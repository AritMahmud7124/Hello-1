package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.MutualCrushMatch
import com.example.data.model.MutualSecretLoveEngine

class SecretLoveCountdownReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val matchId = intent.getStringExtra(EXTRA_MATCH_ID).orEmpty()
    when (intent.action) {
      ACTION_24H_ALERT -> {
        HelloFirebaseMessagingService.showLocalNotification(
          context = context,
          title = "Secret Love 24h Alert ⏳",
          body = MutualSecretLoveEngine.ALERT_MESSAGE,
          type = "mutual_unlock_24h",
          targetId = matchId,
          notificationKey = "mutual_24h_$matchId"
        )
      }
      ACTION_UNLOCK -> {
        HelloFirebaseMessagingService.showLocalNotification(
          context = context,
          title = "Mutual Secret Love Unlocked! 💖",
          body = "The 24-hour countdown reached zero. Tap to reveal your match!",
          type = "crush_match",
          targetId = matchId,
          notificationKey = "mutual_unlocked_$matchId"
        )
      }
    }
  }

  companion object {
    const val ACTION_24H_ALERT = "com.example.notifications.ACTION_MUTUAL_24H_ALERT"
    const val ACTION_UNLOCK = "com.example.notifications.ACTION_MUTUAL_UNLOCK"
    const val EXTRA_MATCH_ID = "extra_match_id"
  }
}

object SecretLoveAlarmScheduler {
  private const val TAG = "SecretLoveAlarms"

  fun syncAlarms(context: Context, matches: List<MutualCrushMatch>, nowMs: Long = System.currentTimeMillis()) {
    val appCtx = context.applicationContext
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    matches.forEach { match ->
      if (match.id.isBlank()) return@forEach

      if (!match.alert24hDispatched && match.alertAtMs > nowMs) {
        scheduleIdleSafe(
          alarmManager = alarmManager,
          context = appCtx,
          triggerAtMs = match.alertAtMs,
          action = SecretLoveCountdownReceiver.ACTION_24H_ALERT,
          matchId = match.id,
          requestCode = ("alert_" + match.id).hashCode() and 0x7fffffff
        )
      }

      if (!match.unlockedNotified && match.revealAtMs > nowMs) {
        scheduleIdleSafe(
          alarmManager = alarmManager,
          context = appCtx,
          triggerAtMs = match.revealAtMs,
          action = SecretLoveCountdownReceiver.ACTION_UNLOCK,
          matchId = match.id,
          requestCode = ("unlock_" + match.id).hashCode() and 0x7fffffff
        )
      }
    }
  }

  private fun scheduleIdleSafe(
    alarmManager: AlarmManager,
    context: Context,
    triggerAtMs: Long,
    action: String,
    matchId: String,
    requestCode: Int
  ) {
    runCatching {
      val intent = Intent(context, SecretLoveCountdownReceiver::class.java).apply {
        this.action = action
        putExtra(SecretLoveCountdownReceiver.EXTRA_MATCH_ID, matchId)
      }
      val pending = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
      }
    }.onFailure { err ->
      Log.w(TAG, "Unable to schedule mutual countdown alarm: ${err.message}")
    }
  }
}
