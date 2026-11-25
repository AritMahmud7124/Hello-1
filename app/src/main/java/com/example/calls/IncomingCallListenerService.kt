package com.example.calls

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.MessengerCall
import com.example.notifications.HelloFirebaseMessagingService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object HelloCallRuntimeState {
  private val _isAppForeground = MutableStateFlow(false)
  val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

  val handledNotifIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

  fun setForeground(foreground: Boolean) {
    _isAppForeground.value = foreground
  }
}

class CallNotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val callId = intent.getStringExtra(IncomingCallListenerService.EXTRA_CALL_ID).orEmpty()
    if (action == IncomingCallListenerService.ACTION_DECLINE_CALL && callId.isNotBlank()) {
      val stopIntent = Intent(context, IncomingCallListenerService::class.java).apply {
        this.action = IncomingCallListenerService.ACTION_DECLINE_CALL
        putExtra(IncomingCallListenerService.EXTRA_CALL_ID, callId)
      }
      runCatching { context.startService(stopIntent) }
    }
  }
}

class IncomingCallListenerService : Service() {

  companion object {
    private const val TAG = "IncomingCallService"
    const val ACTION_START = "com.example.calls.ACTION_START"
    const val ACTION_STOP = "com.example.calls.ACTION_STOP"
    const val ACTION_DECLINE_CALL = "com.example.calls.ACTION_DECLINE_CALL"
    const val ACTION_ANSWER_CALL = "com.example.calls.ACTION_ANSWER_CALL"
    const val ACTION_CLEAR_RINGING = "com.example.calls.ACTION_CLEAR_RINGING"

    const val EXTRA_UID = "extra_uid"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_ANSWER_CALL_ID = "extra_answer_call_id"

    private const val STATUS_CHANNEL_ID = "hello_service_status"
    const val CALL_CHANNEL_ID = "hello_incoming_calls"
    private const val STATUS_NOTIF_ID = 41001
    const val CALL_NOTIF_ID = 41002

    fun start(context: Context, uid: String) {
      if (uid.isBlank()) return
      val intent = Intent(context, IncomingCallListenerService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_UID, uid)
      }
      runCatching {
        ContextCompat.startForegroundService(context, intent)
      }.onFailure {
        Log.w(TAG, "Unable to start foreground call listener: ${it.message}")
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, IncomingCallListenerService::class.java).apply {
        action = ACTION_STOP
      }
      runCatching { context.stopService(intent) }
    }

    fun clearRingingNotification(context: Context) {
      runCatching { NotificationManagerCompat.from(context).cancel(CALL_NOTIF_ID) }
      val intent = Intent(context, IncomingCallListenerService::class.java).apply {
        action = ACTION_CLEAR_RINGING
      }
      runCatching { context.startService(intent) }
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var currentUid: String = ""
  private var callsRegistration: ListenerRegistration? = null
  private var notifsRegistration: ListenerRegistration? = null
  private var foregroundObserverJob: Job? = null

  private var activeRingingCall: MessengerCall? = null
  private var ringJob: Job? = null
  private var ringtone: Ringtone? = null
  private var toneFallback: ToneGenerator? = null
  private var vibrator: Vibrator? = null
  private var audioFocusRequest: AudioFocusRequest? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var notifBaselineLoaded = false

  private val db: FirebaseFirestore by lazy {
    val dbId = applicationContext.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(dbId)
  }

  override fun onCreate() {
    super.onCreate()
    ensureChannels()
    foregroundObserverJob = serviceScope.launch {
      HelloCallRuntimeState.isAppForeground.collect { inForeground ->
        if (inForeground) {
          stopRingingAndCancelNotif()
        } else {
          val pending = activeRingingCall
          if (pending != null && !HelloCallRuntimeState.isAppForeground.value) {
            triggerBackgroundIncomingCall(pending)
          }
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopRingingAndCancelNotif()
        detachListeners()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }

      ACTION_CLEAR_RINGING -> {
        stopRingingAndCancelNotif()
        return START_STICKY
      }

      ACTION_DECLINE_CALL -> {
        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        activeRingingCall = null
        stopRingingAndCancelNotif()
        if (callId.isNotBlank()) {
          declineCallInFirestore(callId)
        }
        return START_STICKY
      }

      else -> {
        val uid = intent?.getStringExtra(EXTRA_UID)?.takeIf { it.isNotBlank() }
          ?: Firebase.auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
          ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
          stopSelf()
          return START_NOT_STICKY
        }
        promoteToForeground()
        if (uid != currentUid || callsRegistration == null) {
          currentUid = uid
          attachListeners(uid)
        }
        return START_STICKY
      }
    }
  }

  private fun promoteToForeground() {
    ensureChannels()
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingOpen = PendingIntent.getActivity(
      this,
      STATUS_NOTIF_ID,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val statusNotif = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_hello_notif)
      .setContentTitle(getString(R.string.app_name))
      .setContentText("Ready for incoming calls & messages")
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setSilent(true)
      .setOngoing(true)
      .setShowWhen(false)
      .setContentIntent(pendingOpen)
      .build()

    val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
    } else {
      0
    }
    runCatching {
      ServiceCompat.startForeground(this, STATUS_NOTIF_ID, statusNotif, fgsType)
    }.onFailure {
      Log.w(TAG, "startForeground failed: ${it.message}")
    }
  }

  private fun attachListeners(uid: String) {
    detachListeners()
    notifBaselineLoaded = false

    callsRegistration = db.collection("calls")
      .whereArrayContains("participantUids", uid)
      .addSnapshotListener { snap, error ->
        if (error != null || snap == null) return@addSnapshotListener
        val nowMs = System.currentTimeMillis()
        val incoming = snap.toObjects(
          MessengerCall::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
          .filter { call ->
            call.status == "ringing" &&
              call.callerId != uid &&
              uid !in call.acceptedUids &&
              kotlin.math.abs(nowMs - call.startedAtMs) < 180_000L
          }
          .maxByOrNull { it.startedAtMs }

        if (incoming == null) {
          activeRingingCall = null
          stopRingingAndCancelNotif()
        } else {
          val previousId = activeRingingCall?.id
          activeRingingCall = incoming
          if (!HelloCallRuntimeState.isAppForeground.value && previousId != incoming.id) {
            triggerBackgroundIncomingCall(incoming)
          }
        }
      }

    notifsRegistration = db.collection("users").document(uid)
      .collection("notifications")
      .addSnapshotListener { snap, error ->
        if (error != null || snap == null) return@addSnapshotListener
        val list = snap.toObjects(
          AppNotification::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        if (!notifBaselineLoaded) {
          HelloCallRuntimeState.handledNotifIds.addAll(list.mapNotNull { it.id.takeIf(String::isNotBlank) })
          notifBaselineLoaded = true
          return@addSnapshotListener
        }
        list.forEach { notif ->
          if (notif.id.isNotBlank() && notif.id !in HelloCallRuntimeState.handledNotifIds) {
            HelloCallRuntimeState.handledNotifIds.add(notif.id)
            val isCallSyntheticMessage = notif.type == "message" &&
              (notif.title.contains("Incoming Audio Call", ignoreCase = true) ||
                notif.title.contains("Incoming Video Call", ignoreCase = true))
            if (!notif.isRead && !HelloCallRuntimeState.isAppForeground.value && !isCallSyntheticMessage) {
              HelloFirebaseMessagingService.showLocalNotification(
                context = this,
                title = notif.title,
                body = notif.body,
                type = notif.type,
                targetId = notif.targetId,
                notificationKey = notif.id
              )
            }
          }
        }
      }
  }

  private fun detachListeners() {
    callsRegistration?.remove()
    callsRegistration = null
    notifsRegistration?.remove()
    notifsRegistration = null
  }

  private fun triggerBackgroundIncomingCall(call: MessengerCall) {
    ensureChannels()
    postIncomingCallNotification(call)
    startRingingAudioAndVibration(call.id)
  }

  @SuppressLint("MissingPermission")
  private fun postIncomingCallNotification(call: MessengerCall) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val isVideo = call.callType == "video"
    val callerLabel = call.callerName.ifBlank { "HELLO Member" }

    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      this,
      call.id.hashCode() and 0x7fffffff,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val answerIntent = Intent(this, MainActivity::class.java).apply {
      action = ACTION_ANSWER_CALL
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_ANSWER_CALL_ID, call.id)
    }
    val answerPendingIntent = PendingIntent.getActivity(
      this,
      (call.id + "_answer").hashCode() and 0x7fffffff,
      answerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val declineIntent = Intent(this, CallNotificationActionReceiver::class.java).apply {
      action = ACTION_DECLINE_CALL
      putExtra(EXTRA_CALL_ID, call.id)
    }
    val declinePendingIntent = PendingIntent.getBroadcast(
      this,
      (call.id + "_decline").hashCode() and 0x7fffffff,
      declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val callerPerson = Person.Builder()
      .setName(callerLabel)
      .setImportant(true)
      .build()

    val callStyle = NotificationCompat.CallStyle.forIncomingCall(
      callerPerson,
      declinePendingIntent,
      answerPendingIntent
    )

    val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_hello_notif)
      .setContentTitle(callerLabel)
      .setContentText("Incoming ${if (isVideo) "video" else "audio"} call · HELLO")
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setTimeoutAfter(45_000L)
      .setContentIntent(fullScreenPendingIntent)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .setStyle(callStyle)
      .build()

    runCatching {
      NotificationManagerCompat.from(this).notify(CALL_NOTIF_ID, notification)
    }
  }

  @SuppressLint("MissingPermission", "WakelockTimeout")
  private fun startRingingAudioAndVibration(callId: String) {
    stopRingingAudioOnly()

    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
    wakeLock = runCatching {
      pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Hello:IncomingCallRing")?.apply {
        acquire(48_000L)
      }
    }.getOrNull()

    val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (audioManager != null) {
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .build()
          audioFocusRequest = req
          audioManager.requestAudioFocus(req)
        } else {
          @Suppress("DEPRECATION")
          audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_RING,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
          )
        }
        audioManager.mode = AudioManager.MODE_RINGTONE
      }
    }

    vibrator = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    }.getOrNull()

    runCatching {
      if (vibrator?.hasVibrator() == true) {
        val pattern = longArrayOf(0L, 700L, 900L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(pattern, 0)
        }
      }
    }

    ringtone = runCatching {
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      RingtoneManager.getRingtone(this, uri)?.apply {
        audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          isLooping = true
        }
        play()
      }
    }.getOrNull()

    toneFallback = if (ringtone == null) {
      runCatching { ToneGenerator(AudioManager.STREAM_RING, 85) }.getOrNull()
    } else {
      null
    }

    ringJob = serviceScope.launch {
      val startMs = System.currentTimeMillis()
      while (System.currentTimeMillis() - startMs < 45_000L && activeRingingCall?.id == callId) {
        if (ringtone != null) {
          runCatching {
            if (ringtone?.isPlaying == false) {
              ringtone?.play()
            }
          }
        } else {
          runCatching { toneFallback?.startTone(ToneGenerator.TONE_PROP_BEEP2, 850) }
        }
        delay(1400L)
      }
      if (activeRingingCall?.id == callId) {
        stopRingingAndCancelNotif()
      }
    }
  }

  private fun stopRingingAudioOnly() {
    ringJob?.cancel()
    ringJob = null
    runCatching { ringtone?.stop() }
    ringtone = null
    runCatching {
      toneFallback?.stopTone()
      toneFallback?.release()
    }
    toneFallback = null
    runCatching { vibrator?.cancel() }
    vibrator = null
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (audioManager != null && !HelloCallRuntimeState.isAppForeground.value) {
      runCatching {
        if (audioManager.mode == AudioManager.MODE_RINGTONE) {
          audioManager.mode = AudioManager.MODE_NORMAL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
          @Suppress("DEPRECATION")
          audioManager.abandonAudioFocus(null)
        }
      }
    }
    audioFocusRequest = null
    runCatching {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    }
    wakeLock = null
  }

  private fun stopRingingAndCancelNotif() {
    stopRingingAudioOnly()
    runCatching { NotificationManagerCompat.from(this).cancel(CALL_NOTIF_ID) }
  }

  private fun declineCallInFirestore(callId: String) {
    db.collection("calls").document(callId).update(
      mapOf(
        "status" to "declined",
        "endedAtMs" to System.currentTimeMillis(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).addOnFailureListener { e ->
      Log.w(TAG, "Decline call failed: ${e.message}")
    }
  }

  private fun ensureChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (manager.getNotificationChannel(STATUS_CHANNEL_ID) == null) {
      val statusChannel = NotificationChannel(
        STATUS_CHANNEL_ID,
        "HELLO Background Connection",
        NotificationManager.IMPORTANCE_MIN
      ).apply {
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
      }
      manager.createNotificationChannel(statusChannel)
    }

    if (manager.getNotificationChannel(CALL_CHANNEL_ID) == null) {
      val callChannel = NotificationChannel(
        CALL_CHANNEL_ID,
        "Incoming Audio & Video Calls",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts you with ringtone and Answer/Decline controls for incoming HELLO calls"
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setSound(null, null)
        enableVibration(false)
        setBypassDnd(true)
      }
      manager.createNotificationChannel(callChannel)
    }
  }

  override fun onDestroy() {
    stopRingingAndCancelNotif()
    detachListeners()
    foregroundObserverJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
