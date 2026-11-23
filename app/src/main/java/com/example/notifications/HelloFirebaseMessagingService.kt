package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrivateUser
import com.example.ui.navigation.HelloRoutes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HelloFirebaseMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val uid = Firebase.auth.currentUser?.uid ?: return
    val dbId = applicationContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(dbId)
    db.collection("users").document(uid).update(
      mapOf(
        "fcmToken" to token.take(512),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).addOnFailureListener { e ->
      Log.w(TAG, "Failed to update refreshed FCM token: ${e.message}")
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val data = remoteMessage.data
    val type = data["type"] ?: "social"
    val targetId = data["targetId"] ?: ""
    val title = data["title"]
      ?: remoteMessage.notification?.title
      ?: defaultTitleForType(type)
    val body = data["body"]
      ?: remoteMessage.notification?.body
      ?: "Tap to open HELLO."

    showLocalNotification(
      context = this,
      title = title,
      body = body,
      type = type,
      targetId = targetId,
      notificationKey = remoteMessage.messageId ?: (type + targetId + System.currentTimeMillis())
    )
  }

  companion object {
    private const val TAG = "HelloFcmService"
    const val EXTRA_ROUTE = "extra_route"

    fun isTypeEnabled(prefs: PrivateUser?, type: String): Boolean {
      if (prefs == null) return true
      return when (type) {
        "like" -> prefs.notifLikes
        "comment", "reply" -> prefs.notifComments
        "friend_request", "group_invite", "page_invite" -> prefs.notifFriends
        "message" -> prefs.notifMessages
        "secret_love", "crush_match", "mutual_unlock_24h" -> prefs.notifSecretLove
        "sos_alert" -> prefs.notifSos
        else -> true
      }
    }

    fun showLocalNotification(
      context: Context,
      title: String,
      body: String,
      type: String,
      targetId: String,
      notificationKey: String = type + targetId
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }

      val channelId = ensureChannel(context, type)
      val route = routeForNotification(type, targetId)
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_ROUTE, route)
      }
      val notifId = notificationKey.hashCode() and 0x7fffffff
      val pendingIntent = PendingIntent.getActivity(
        context,
        notifId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_hello_notif)
        .setContentTitle(title.ifBlank { defaultTitleForType(type) })
        .setContentText(body.ifBlank { "Tap to open HELLO." })
        .setStyle(NotificationCompat.BigTextStyle().bigText(body.ifBlank { "Tap to open HELLO." }))
        .setPriority(
          if (type in listOf("message", "secret_love", "crush_match", "mutual_unlock_24h", "sos_alert")) {
            NotificationCompat.PRIORITY_HIGH
          } else {
            NotificationCompat.PRIORITY_DEFAULT
          }
        )
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    private fun defaultTitleForType(type: String): String = when (type) {
      "message" -> "New Messenger Message"
      "friend_request" -> "Friend Request"
      "secret_love" -> "New Secret Love ❤️"
      "crush_match" -> "Mutual Secret Love Unlocked! 💖"
      "mutual_unlock_24h" -> "Secret Love 24h Alert ⏳"
      "sos_alert" -> "Emergency SOS Alert"
      "group_invite" -> "Group Update"
      "page_invite" -> "Page Update"
      else -> "HELLO Notification"
    }

    fun routeForNotification(type: String, targetId: String): String = when (type) {
      "message" -> if (targetId.isNotBlank()) HelloRoutes.chatThread(targetId) else HelloRoutes.MESSENGER
      "friend_request" -> HelloRoutes.FRIENDS
      "secret_love", "crush_match", "mutual_unlock_24h" -> HelloRoutes.SECRET_LOVE
      "group_invite", "sos_alert" -> if (targetId.isNotBlank()) HelloRoutes.groupDetail(targetId) else HelloRoutes.GROUPS
      "page_invite" -> HelloRoutes.PAGES
      "like", "comment", "reply" -> HelloRoutes.HOME
      else -> HelloRoutes.NOTIFICATIONS
    }

    private fun ensureChannel(context: Context, type: String): String {
      val (channelId, name, importance) = when (type) {
        "message" -> Triple("hello_messages", "Messenger Chats", NotificationManager.IMPORTANCE_HIGH)
        "friend_request", "group_invite", "page_invite" -> Triple("hello_friends", "Friend Requests, Groups & Pages", NotificationManager.IMPORTANCE_DEFAULT)
        "secret_love", "crush_match", "mutual_unlock_24h" -> Triple("hello_secret_love", "Secret Love", NotificationManager.IMPORTANCE_HIGH)
        "sos_alert" -> Triple("hello_sos", "Emergency SOS", NotificationManager.IMPORTANCE_HIGH)
        else -> Triple("hello_social", "Social Activity", NotificationManager.IMPORTANCE_DEFAULT)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
          manager.createNotificationChannel(NotificationChannel(channelId, name, importance))
        }
      }
      return channelId
    }
  }
}
