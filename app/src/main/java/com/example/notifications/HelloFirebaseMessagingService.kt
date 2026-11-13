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

    val channelId = ensureChannel(this, type)
    val route = routeForNotification(type, targetId)
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_ROUTE, route)
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      (type + targetId).hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val notification = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(
        if (type in listOf("message", "secret_love", "sos_alert")) {
          NotificationCompat.PRIORITY_HIGH
        } else {
          NotificationCompat.PRIORITY_DEFAULT
        }
      )
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(this).notify((System.currentTimeMillis() and 0x7fffffff).toInt(), notification)
  }

  private fun defaultTitleForType(type: String): String = when (type) {
    "message" -> "New Messenger Message"
    "friend_request" -> "Friend Request"
    "secret_love" -> "New Secret Love ❤️"
    "sos_alert" -> "Emergency SOS Alert"
    else -> "HELLO Notification"
  }

  private fun routeForNotification(type: String, targetId: String): String = when (type) {
    "message" -> if (targetId.isNotBlank()) HelloRoutes.chatThread(targetId) else HelloRoutes.MESSENGER
    "friend_request" -> HelloRoutes.FRIENDS
    "secret_love" -> HelloRoutes.SECRET_LOVE
    "group_invite", "sos_alert" -> if (targetId.isNotBlank()) HelloRoutes.groupDetail(targetId) else HelloRoutes.GROUPS
    else -> HelloRoutes.NOTIFICATIONS
  }

  private fun ensureChannel(context: Context, type: String): String {
    val (channelId, name, importance) = when (type) {
      "message" -> Triple("hello_messages", "Messenger Chats", NotificationManager.IMPORTANCE_HIGH)
      "friend_request" -> Triple("hello_friends", "Friend Requests", NotificationManager.IMPORTANCE_DEFAULT)
      "secret_love" -> Triple("hello_secret_love", "Secret Love", NotificationManager.IMPORTANCE_HIGH)
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

  companion object {
    private const val TAG = "HelloFcmService"
    const val EXTRA_ROUTE = "extra_route"
  }
}
