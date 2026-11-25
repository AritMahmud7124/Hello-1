package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.MutualCrushMatch

class SecretLoveCountdownReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val matchId = intent.getStringExtra(EXTRA_MATCH_ID).orEmpty()
    when (intent.action) {
      ACTION_UNLOCK -> {
        HelloFirebaseMessagingService.showLocalNotification(
          context = context,
          title = "Mutual Secret Love Unlocked! 💖",
          body = "Your Mutual Secret Love has officially unlocked. Tap to reveal your match!",
          type = "crush_match",
          targetId = matchId,
          notificationKey = "mutual_unlocked_$matchId"
        )
      }
      // ACTION_24H_ALERT intentionally ignored to preserve complete stealth before countdown ends
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
  private const val PREFS_NAME = "hello_secret_love_alarms"
  private const val KEY_ACTIVE_MATCH_IDS = "active_match_ids"

  fun syncAlarms(context: Context, matches: List<MutualCrushMatch>, nowMs: Long = System.currentTimeMillis()) {
    val appCtx = context.applicationContext
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val previousIds = prefs.getStringSet(KEY_ACTIVE_MATCH_IDS, emptySet()).orEmpty()
    val currentIds = matches.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }.toSet()

    // Cancel any alarms for matches that were silently opted out or removed
    (previousIds - currentIds).forEach { removedId ->
      cancelMatchAlarms(alarmManager, appCtx, removedId)
    }

    matches.forEach { match ->
      if (match.id.isBlank()) return@forEach

      // Ensure no pre-unlock 24h warning alarm ever exists
      cancelActionAlarm(
        alarmManager = alarmManager,
        context = appCtx,
        action = SecretLoveCountdownReceiver.ACTION_24H_ALERT,
        matchId = match.id,
        requestCode = ("alert_" + match.id).hashCode() and 0x7fffffff
      )

      if (!match.unlockedNotified && match.revealAtMs > nowMs) {
        scheduleIdleSafe(
          alarmManager = alarmManager,
          context = appCtx,
          triggerAtMs = match.revealAtMs,
          action = SecretLoveCountdownReceiver.ACTION_UNLOCK,
          matchId = match.id,
          requestCode = ("unlock_" + match.id).hashCode() and 0x7fffffff
        )
      } else {
        cancelActionAlarm(
          alarmManager = alarmManager,
          context = appCtx,
          action = SecretLoveCountdownReceiver.ACTION_UNLOCK,
          matchId = match.id,
          requestCode = ("unlock_" + match.id).hashCode() and 0x7fffffff
        )
      }
    }

    prefs.edit().putStringSet(KEY_ACTIVE_MATCH_IDS, currentIds).apply()
  }

  private fun cancelMatchAlarms(alarmManager: AlarmManager, context: Context, matchId: String) {
    cancelActionAlarm(
      alarmManager = alarmManager,
      context = context,
      action = SecretLoveCountdownReceiver.ACTION_24H_ALERT,
      matchId = matchId,
      requestCode = ("alert_$matchId").hashCode() and 0x7fffffff
    )
    cancelActionAlarm(
      alarmManager = alarmManager,
      context = context,
      action = SecretLoveCountdownReceiver.ACTION_UNLOCK,
      matchId = matchId,
      requestCode = ("unlock_$matchId").hashCode() and 0x7fffffff
    )
  }

  private fun cancelActionAlarm(
    alarmManager: AlarmManager,
    context: Context,
    action: String,
    matchId: String,
    requestCode: Int
  ) {
    runCatching {
      val intent = Intent(context, SecretLoveCountdownReceiver::class.java).apply {
        this.action = action
        putExtra(SecretLoveCountdownReceiver.EXTRA_MATCH_ID, matchId)
      }
      val existing = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      if (existing != null) {
        alarmManager.cancel(existing)
        existing.cancel()
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
      Log.w(TAG, "Unable to schedule mutual unlock alarm: ${err.message}")
    }
  }
}
