package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calls.WebRtcCallEngine
import com.example.data.model.CallSignal
import com.example.data.model.MessengerCall
import com.example.data.model.PublicUser
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@Composable
fun MessengerCallOverlay(
  call: MessengerCall,
  currentUserId: String,
  allUsers: List<PublicUser>,
  signals: List<CallSignal> = emptyList(),
  onSendSignal: (type: String, sdp: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) -> Unit = { _, _, _, _, _ -> },
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onEnd: () -> Unit,
  onTimeoutMissed: () -> Unit = onEnd
) {
  val context = LocalContext.current
  val isVideoCall = call.callType == "video"
  val isCaller = call.callerId == currentUserId
  val isIncomingRinging = call.status == "ringing" &&
    !isCaller &&
    currentUserId !in call.acceptedUids

  var hasAudioPerm by remember(context) {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasCameraPerm by remember(context) {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var isMuted by remember { mutableStateOf(false) }
  var isSpeakerOn by remember(isVideoCall) { mutableStateOf(isVideoCall) }
  var isCameraOff by remember { mutableStateOf(false) }
  var useFrontCamera by remember { mutableStateOf(true) }
  var elapsedSeconds by remember(call.id) { mutableIntStateOf(0) }

  val latestOnSendSignal by rememberUpdatedState(onSendSignal)
  val latestOnTimeoutMissed by rememberUpdatedState(onTimeoutMissed)

  val permLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    hasAudioPerm = grants[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm
    hasCameraPerm = grants[Manifest.permission.CAMERA] ?: hasCameraPerm
    if (isIncomingRinging && hasAudioPerm) {
      onAccept()
    }
  }

  // Request audio/camera permissions automatically for caller or after notification accept
  LaunchedEffect(call.id, isCaller, isIncomingRinging, isVideoCall) {
    if (isCaller || !isIncomingRinging) {
      val needed = buildList {
        if (!hasAudioPerm) add(Manifest.permission.RECORD_AUDIO)
        if (isVideoCall && !hasCameraPerm) add(Manifest.permission.CAMERA)
      }
      if (needed.isNotEmpty()) {
        permLauncher.launch(needed.toTypedArray())
      }
    }
  }

  // 45s auto-miss timeout for unanswered ringing calls
  LaunchedEffect(call.id, call.status) {
    if (call.status == "ringing") {
      delay(45_000L)
      latestOnTimeoutMissed()
    }
  }

  val audioManager = remember(context) {
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  }

  // Audio focus & call mode lifecycle (kept separate from mute/speaker so toggling never resets MODE_IN_COMMUNICATION)
  DisposableEffect(audioManager, call.id, isIncomingRinging, call.status) {
    var focusRequest: AudioFocusRequest? = null
    if (audioManager != null) {
      runCatching {
        val usage = if (isIncomingRinging) {
          AudioAttributes.USAGE_NOTIFICATION_RINGTONE
        } else {
          AudioAttributes.USAGE_VOICE_COMMUNICATION
        }
        val contentType = if (isIncomingRinging) {
          AudioAttributes.CONTENT_TYPE_SONIFICATION
        } else {
          AudioAttributes.CONTENT_TYPE_SPEECH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .build()
          focusRequest = req
          audioManager.requestAudioFocus(req)
        } else {
          @Suppress("DEPRECATION")
          audioManager.requestAudioFocus(
            null,
            if (isIncomingRinging) AudioManager.STREAM_RING else AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
          )
        }
        audioManager.mode = if (isIncomingRinging) {
          AudioManager.MODE_RINGTONE
        } else {
          AudioManager.MODE_IN_COMMUNICATION
        }
      }
    }
    onDispose {
      if (audioManager != null) {
        runCatching {
          audioManager.isMicrophoneMute = false
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
          }
          @Suppress("DEPRECATION")
          audioManager.isSpeakerphoneOn = false
          audioManager.mode = AudioManager.MODE_NORMAL
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
          } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
          }
        }
      }
    }
  }

  // Dynamic mic mute & earpiece/speaker routing during active/outgoing call
  LaunchedEffect(audioManager, isMuted, isSpeakerOn, isIncomingRinging, call.status) {
    if (audioManager != null && !isIncomingRinging) {
      runCatching {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = isMuted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val preferredType = if (isSpeakerOn) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
          } else {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
          }
          val targetDevice = audioManager.availableCommunicationDevices
            .firstOrNull { it.type == preferredType }
          if (targetDevice != null) {
            audioManager.setCommunicationDevice(targetDevice)
          }
        }
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = isSpeakerOn
      }
    }
  }

  // Incoming ringtone + vibration + keep-alive while ringing
  @SuppressLint("MissingPermission")
  LaunchedEffect(call.id, isIncomingRinging) {
    if (!isIncomingRinging) return@LaunchedEffect
    val vibrator: Vibrator? = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    }.getOrNull()

    val ringtone: Ringtone? = runCatching {
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      RingtoneManager.getRingtone(context, uri)?.apply {
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

    val toneFallback: ToneGenerator? = if (ringtone == null) {
      runCatching { ToneGenerator(AudioManager.STREAM_RING, 85) }.getOrNull()
    } else {
      null
    }

    runCatching {
      if (vibrator?.hasVibrator() == true) {
        val pattern = longArrayOf(0L, 700L, 900L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(pattern, 0)
        }
      }
    }

    try {
      while (true) {
        if (ringtone != null) {
          runCatching {
            if (!ringtone.isPlaying) {
              ringtone.play()
            }
          }
        } else {
          runCatching { toneFallback?.startTone(ToneGenerator.TONE_PROP_BEEP2, 850) }
        }
        delay(1400L)
      }
    } finally {
      runCatching { ringtone?.stop() }
      runCatching { vibrator?.cancel() }
      runCatching {
        toneFallback?.stopTone()
        toneFallback?.release()
      }
    }
  }

  // Outgoing ringback tone while caller waits for answer
  LaunchedEffect(call.id, call.status, isCaller) {
    if (call.status == "ringing" && isCaller) {
      val toneGen = runCatching { ToneGenerator(AudioManager.STREAM_VOICE_CALL, 85) }.getOrNull()
      try {
        while (true) {
          runCatching { toneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400) }
          delay(3500L)
        }
      } finally {
        runCatching {
          toneGen?.stopTone()
          toneGen?.release()
        }
      }
    }
  }

  // Two-way WebRTC Audio/Video Engine (starts once caller initiates OR callee accepts with audio permission)
  val mediaActive = hasAudioPerm && (!isIncomingRinging || call.status == "active")
  val rtcEngine = remember(call.id, isVideoCall, isCaller, mediaActive) {
    if (mediaActive) {
      WebRtcCallEngine(
        context = context.applicationContext,
        isVideoCall = isVideoCall,
        isCaller = isCaller,
        onSendSignal = { type, sdp, sdpMid, sdpMLineIndex, candidate ->
          latestOnSendSignal(type, sdp, sdpMid, sdpMLineIndex, candidate)
        }
      )
    } else {
      null
    }
  }

  DisposableEffect(rtcEngine) {
    rtcEngine?.start()
    onDispose {
      rtcEngine?.dispose()
    }
  }

  LaunchedEffect(rtcEngine, signals) {
    rtcEngine?.onSignalsReceived(signals, currentUserId)
  }

  LaunchedEffect(rtcEngine, isMuted) {
    rtcEngine?.setMuted(isMuted)
  }

  LaunchedEffect(rtcEngine, isCameraOff, hasCameraPerm) {
    rtcEngine?.setCameraEnabled(!isCameraOff && hasCameraPerm)
  }

  var previousFrontCamera by remember(call.id) { mutableStateOf(true) }
  LaunchedEffect(rtcEngine, useFrontCamera) {
    if (rtcEngine != null && useFrontCamera != previousFrontCamera) {
      previousFrontCamera = useFrontCamera
      rtcEngine.switchCamera(useFrontCamera)
    }
  }

  val iceState by (rtcEngine?.iceState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("NEW") })
  val hasRemoteVideo by (rtcEngine?.hasRemoteVideo?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) })
  val iceConnected = iceState == "CONNECTED" || iceState == "COMPLETED"

  // Count call timer only when real WebRTC media ICE is connected
  LaunchedEffect(call.id, call.status, iceConnected) {
    if (call.status == "active" && iceConnected) {
      while (true) {
        delay(1000L)
        elapsedSeconds++
      }
    }
  }

  val peerName = remember(call, currentUserId, allUsers) {
    if (call.callerId != currentUserId) {
      call.callerName.ifBlank { "HELLO Member" }
    } else if (call.conversationTitle.isNotBlank()) {
      call.conversationTitle
    } else {
      val otherId = call.participantUids.firstOrNull { it != currentUserId }
      allUsers.firstOrNull { it.userId == otherId }?.displayName ?: "HELLO Call"
    }
  }
  val peerAvatar = remember(call, currentUserId, allUsers) {
    val targetId = if (call.callerId != currentUserId) call.callerId
    else call.participantUids.firstOrNull { it != currentUserId }
    allUsers.firstOrNull { it.userId == targetId }?.photoUrl.orEmpty()
  }

  val pulseInf = rememberInfiniteTransition(label = "call_pulse")
  val ringScale by pulseInf.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    label = "call_ring_scale"
  )

  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0A0F1D), Color(0xFF10192D), Color(0xFF070A12))
          )
        )
    ) {
      // Remote WebRTC full-screen video when connected
      if (isVideoCall && rtcEngine != null && call.status == "active") {
        AndroidView(
          factory = { ctx ->
            SurfaceViewRenderer(ctx).also { renderer ->
              rtcEngine.attachRemoteRenderer(renderer)
            }
          },
          onRelease = { renderer ->
            rtcEngine.detachRemoteRenderer(renderer)
          },
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent, Color.Black.copy(alpha = 0.78f))
              )
            )
        )
      }

      // Local self-view PiP card for video calls (full-screen during outgoing ring before remote video arrives)
      if (isVideoCall && rtcEngine != null && hasCameraPerm && !isCameraOff && !isIncomingRinging) {
        val showAsMainBackdrop = call.status != "active" || !hasRemoteVideo
        if (showAsMainBackdrop) {
          AndroidView(
            factory = { ctx ->
              SurfaceViewRenderer(ctx).also { renderer ->
                rtcEngine.attachLocalRenderer(renderer)
              }
            },
            onRelease = { renderer ->
              rtcEngine.detachLocalRenderer(renderer)
            },
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent, Color.Black.copy(alpha = 0.78f))
                )
              )
          )
        } else {
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .statusBarsPadding()
              .padding(top = 18.dp, end = 16.dp)
              .size(width = 104.dp, height = 148.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
            shadowElevation = 8.dp
          ) {
            AndroidView(
              factory = { ctx ->
                SurfaceViewRenderer(ctx).also { renderer ->
                  rtcEngine.attachLocalRenderer(renderer)
                }
              },
              onRelease = { renderer ->
                rtcEngine.detachLocalRenderer(renderer)
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      // Top Header Status
      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(top = 28.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = Color.White.copy(alpha = 0.12f),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
              contentDescription = null,
              tint = when {
                iceConnected -> Color(0xFF34D399)
                iceState == "FAILED" || iceState == "DISCONNECTED" -> Color(0xFFF87171)
                else -> Color(0xFF38BDF8)
              },
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = when {
                isIncomingRinging -> if (isVideoCall) "Incoming Video Call" else "Incoming Audio Call"
                call.status == "ringing" -> "Calling..."
                iceConnected -> "HD Live · %02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
                iceState == "FAILED" || iceState == "DISCONNECTED" -> "Connection lost"
                else -> "Connecting audio/video..."
              },
              color = Color.White,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        if (!isVideoCall || isCameraOff || !hasCameraPerm || isIncomingRinging || (call.status == "active" && !hasRemoteVideo)) {
          Spacer(Modifier.height(28.dp))
          Box(
            modifier = Modifier
              .size(126.dp)
              .scale(if (call.status == "ringing") ringScale else 1f)
              .clip(CircleShape)
              .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
          ) {
            UserAvatar(name = peerName, photoUrl = peerAvatar, size = 110.dp)
          }
          Spacer(Modifier.height(8.dp))
        }

        Text(
          text = peerName,
          color = Color.White,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Text(
          text = when {
            call.participantUids.size > 2 -> "Group call · ${call.acceptedUids.size}/${call.participantUids.size} joined"
            call.status == "active" && !iceConnected -> "Linking live audio & video stream..."
            isVideoCall -> "HELLO Encrypted HD Video Call"
            else -> "HELLO Encrypted HD Voice Call"
          },
          color = Color.White.copy(alpha = 0.78f),
          style = MaterialTheme.typography.bodyMedium
        )
        if (call.status == "active" && (iceState == "FAILED" || iceState == "DISCONNECTED")) {
          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.padding(top = 4.dp)
          ) {
            TextButton(
              onClick = { rtcEngine?.restartIce() },
              modifier = Modifier.testTag("call_reconnect_button")
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("Reconnect", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // Bottom Call Controls
      if (isIncomingRinging) {
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 38.dp)
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = onDecline,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = CircleShape,
            modifier = Modifier.size(72.dp).testTag("decline_call_button")
          ) {
            Icon(Icons.Rounded.CallEnd, contentDescription = "Decline call", tint = Color.White, modifier = Modifier.size(30.dp))
          }
          Button(
            onClick = {
              val needed = buildList {
                if (!hasAudioPerm) add(Manifest.permission.RECORD_AUDIO)
                if (isVideoCall && !hasCameraPerm) add(Manifest.permission.CAMERA)
              }
              if (needed.isEmpty()) {
                onAccept()
              } else {
                permLauncher.launch(needed.toTypedArray())
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = CircleShape,
            modifier = Modifier.size(72.dp).testTag("accept_call_button")
          ) {
            Icon(
              imageVector = if (isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
              contentDescription = "Accept call",
              tint = Color.White,
              modifier = Modifier.size(30.dp)
            )
          }
        }
      } else {
        Surface(
          color = Color.Black.copy(alpha = 0.62f),
          shape = RoundedCornerShape(30.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { isMuted = !isMuted },
              modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.16f))
                .testTag("call_mute_button")
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = if (isMuted) "Unmute mic" else "Mute mic",
                tint = if (isMuted) Color.Black else Color.White
              )
            }

            IconButton(
              onClick = { isSpeakerOn = !isSpeakerOn },
              modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.16f))
                .testTag("call_speaker_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Toggle speaker",
                tint = if (isSpeakerOn) Color.Black else Color.White
              )
            }

            if (isVideoCall) {
              IconButton(
                onClick = {
                  if (!hasCameraPerm) {
                    permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                  } else {
                    isCameraOff = !isCameraOff
                  }
                },
                modifier = Modifier
                  .size(50.dp)
                  .clip(CircleShape)
                  .background(if (isCameraOff) Color.White else Color.White.copy(alpha = 0.16f))
              ) {
                Icon(
                  imageVector = if (isCameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                  contentDescription = "Toggle camera",
                  tint = if (isCameraOff) Color.Black else Color.White
                )
              }

              IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier
                  .size(50.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.16f))
              ) {
                Icon(
                  imageVector = Icons.Rounded.Cameraswitch,
                  contentDescription = "Flip camera",
                  tint = Color.White
                )
              }
            }

            IconButton(
              onClick = onEnd,
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .testTag("end_call_button")
            ) {
              Icon(Icons.Rounded.CallEnd, contentDescription = "End call", tint = Color.White)
            }
          }
        }
      }
    }
  }
}
