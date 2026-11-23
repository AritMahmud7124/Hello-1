package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRecord
import com.example.data.model.DevotionTrial
import com.example.data.model.GlobalSettings
import com.example.data.model.MutualCrushMatch
import com.example.data.model.MutualSecretLoveEngine
import com.example.data.model.OperationType
import com.example.data.model.OwnerConfig
import com.example.data.model.PrivateCrushMessage
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.PureLoveEngine
import com.example.data.model.PureLoveTier
import com.example.data.model.RegionalAnalytics
import com.example.data.model.ReportEntity
import com.example.data.model.SecretLoveMessage
import com.example.data.model.SecretLoveSendOutcome
import com.example.data.model.SentSecretLove
import com.example.data.model.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GovernanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  // --- SECRET LOVE ---
  // Strictly scoped to recipientId == currentUserId; sender identity is NEVER stored on /secret_loves unless revealed.
  fun observeReceivedSecretLoves(userId: String): Flow<List<SecretLoveMessage>> =
    db.collection("secret_loves")
      .whereEqualTo("recipientId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(SecretLoveMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "secret_loves")
        throw e
      }

  fun observeSentSecretLoves(userId: String): Flow<List<SentSecretLove>> =
    db.collection("users").document(userId).collection("sent_secret_loves")
      .snapshots()
      .map { snap ->
        snap.toObjects(SentSecretLove::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$userId/sent_secret_loves")
        throw e
      }

  suspend fun sendSecretLove(
    sender: PublicUser,
    recipient: PublicUser,
    message: String,
    moodEmoji: String,
    revealIdentityNow: Boolean
  ): Result<SecretLoveSendOutcome> = runCatching {
    val uid = requireUserId()
    require(uid != recipient.userId) { "Choose someone else to send a Secret Love note." }
    require(recipient.acceptSecretLove) { "${recipient.displayName} has disabled Secret Love messages." }

    // Verify Pure Love Quest 7-day gate before creating the message
    val trialRef = db.collection("devotion_trials").document("${uid}_${recipient.userId}")
    val trialSnap = trialRef.get().await()
    val completedDays = (trialSnap.getLong("completedDays") ?: 0L).toInt()
    require(trialSnap.exists() && completedDays >= PureLoveEngine.MIN_HINT_UNLOCK_DAYS) {
      "Complete Day ${PureLoveEngine.MIN_HINT_UNLOCK_DAYS} of the Pure Love Quest for ${recipient.displayName} to unlock Secret Love Hints ($completedDays/${PureLoveEngine.MIN_HINT_UNLOCK_DAYS} days)."
    }
    val earnedBadge = PureLoveEngine.badgeForDays(completedDays)
    val badgeTitle = earnedBadge?.let { "${it.badgeEmoji} ${it.badgeTitle}" } ?: ""

    val messageRef = db.collection("secret_loves").document()
    val messageId = messageRef.id
    val receiptRef = db.collection("users").document(uid).collection("sent_secret_loves").document(messageId)
    val signalRef = db.collection("crush_signals").document("${uid}_${recipient.userId}")
    val privateCrushRef = db.collection("users").document(uid).collection("secret_crushes").document(recipient.userId)
    val cleanMsg = message.trim().take(1000)
    val cleanEmoji = moodEmoji.ifBlank { "❤️" }.take(16)
    val safeRecipientName = recipient.displayName.ifBlank { "Secret Admirer" }.take(80)

    val batch = db.batch()
    batch.set(
      receiptRef,
      mapOf(
        "messageId" to messageId,
        "recipientId" to recipient.userId,
        "recipientName" to recipient.displayName.ifBlank { "Member" }.take(80),
        "message" to cleanMsg,
        "moodEmoji" to cleanEmoji,
        "isRevealed" to revealIdentityNow,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.set(
      messageRef,
      mapOf(
        "recipientId" to recipient.userId,
        "message" to cleanMsg,
        "moodEmoji" to cleanEmoji,
        "isRevealed" to revealIdentityNow,
        "revealedSenderId" to if (revealIdentityNow) uid else "",
        "revealedSenderName" to if (revealIdentityNow) sender.displayName.ifBlank { "Admirer" }.take(80) else "",
        "isRead" to false,
        "isBlocked" to false,
        "isReported" to false,
        "senderBadgeTitle" to badgeTitle.take(60),
        "senderTrialDays" to completedDays,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.set(
      signalRef,
      mapOf(
        "fromUid" to uid,
        "toUid" to recipient.userId,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.set(
      privateCrushRef,
      mapOf(
        "targetUid" to recipient.userId,
        "targetName" to safeRecipientName,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    // Check reciprocal zero-trust crush signal (only succeeds if both users signaled each other)
    val isMutualMatch = runCatching {
      db.collection("crush_signals").document("${recipient.userId}_$uid").get().await().exists()
    }.getOrDefault(false)

    val safeSenderName = sender.displayName.ifBlank { "Admirer" }.take(80)

    // Send incoming Secret Love notification to recipient (no mutual reveal!)
    runCatching {
      db.collection("users").document(recipient.userId).collection("notifications").document().set(
        mapOf(
          "recipientId" to recipient.userId,
          "actorId" to uid,
          "actorName" to if (revealIdentityNow) safeSenderName else "Secret Admirer",
          "type" to "secret_love",
          "title" to (if (revealIdentityNow) "$safeSenderName sent you Love $cleanEmoji" else "New Secret Love $cleanEmoji").take(120),
          "body" to "Tap to open your Secret Love inbox.",
          "targetId" to messageId,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }

    // Never reveal mutual crush immediately or store who added first:
    // Create a sealed delayed match with a hidden random revealAt between 7 and 14 days.
    val mutualMatchId = if (isMutualMatch) {
      ensureMutualMatchRecord(uid, recipient.userId)
    } else {
      ""
    }

    SecretLoveSendOutcome(
      messageId = messageId,
      recipientId = recipient.userId,
      recipientName = safeRecipientName,
      isMutualMatch = isMutualMatch,
      mutualMatchId = mutualMatchId
    )
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "secret_loves")
  }

  // --- DELAYED MUTUAL SECRET LOVE (7-14d hidden random revealAt + 24h warning + simultaneous unlock) ---
  fun observeMySecretCrushes(userId: String): Flow<Set<String>> =
    db.collection("users").document(userId).collection("secret_crushes")
      .snapshots()
      .map { snap ->
        snap.documents.mapNotNull { it.getString("targetUid") ?: it.id }.toSet()
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$userId/secret_crushes")
        throw e
      }

  fun observeMyMutualMatches(userId: String): Flow<List<MutualCrushMatch>> =
    db.collection("mutual_crush_matches")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(MutualCrushMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.revealAtMs }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "mutual_crush_matches")
        throw e
      }

  suspend fun signalSecretCrush(recipient: PublicUser): Result<Boolean> = runCatching {
    val uid = requireUserId()
    require(uid != recipient.userId) { "Choose someone else to add to Secret Crush." }
    require(recipient.acceptSecretLove) { "${recipient.displayName} has disabled Secret Love." }

    val safeRecipientName = recipient.displayName.ifBlank { "Member" }.take(80)
    val batch = db.batch()
    batch.set(
      db.collection("crush_signals").document("${uid}_${recipient.userId}"),
      mapOf(
        "fromUid" to uid,
        "toUid" to recipient.userId,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.set(
      db.collection("users").document(uid).collection("secret_crushes").document(recipient.userId),
      mapOf(
        "targetUid" to recipient.userId,
        "targetName" to safeRecipientName,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    val isMutualMatch = runCatching {
      db.collection("crush_signals").document("${recipient.userId}_$uid").get().await().exists()
    }.getOrDefault(false)

    if (isMutualMatch) {
      ensureMutualMatchRecord(uid, recipient.userId)
    }
    isMutualMatch
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "crush_signals")
  }

  suspend fun removeSecretCrush(recipientId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val matchId = MutualSecretLoveEngine.sortedPairId(uid, recipientId)
    val existingMatch = runCatching {
      db.collection("mutual_crush_matches").document(matchId).get().await()
    }.getOrNull()
    if (existingMatch != null && existingMatch.exists()) {
      val revealAtMs = existingMatch.getLong("revealAtMs") ?: Long.MAX_VALUE
      require(System.currentTimeMillis() >= revealAtMs) {
        "Mutual Secret Love is sealed until its countdown unlocks."
      }
    }
    val batch = db.batch()
    batch.delete(db.collection("crush_signals").document("${uid}_$recipientId"))
    batch.delete(db.collection("users").document(uid).collection("secret_crushes").document(recipientId))
    batch.commit().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "crush_signals")
  }

  private suspend fun ensureMutualMatchRecord(uidA: String, uidB: String): String {
    val sorted = MutualSecretLoveEngine.sortedPair(uidA, uidB)
    val matchId = "${sorted[0]}_${sorted[1]}"
    val ref = db.collection("mutual_crush_matches").document(matchId)
    val existing = runCatching { ref.get().await() }.getOrNull()
    if (existing == null || !existing.exists()) {
      val schedule = MutualSecretLoveEngine.pickRandomRevealSchedule(System.currentTimeMillis())
      // Only creates if non-existent; ignore race if both users confirm at exact same instant
      runCatching {
        ref.set(
          mapOf(
            "participantUids" to sorted,
            "matchedAtMs" to schedule.matchedAtMs,
            "revealAtMs" to schedule.revealAtMs,
            "alertAtMs" to schedule.alertAtMs,
            "alert24hDispatched" to false,
            "unlockedNotified" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    }
    return matchId
  }

  suspend fun dispatchMutual24hAlertIfNeeded(match: MutualCrushMatch): Result<Boolean> = runCatching {
    val uid = requireUserId()
    if (match.id.isBlank() || match.alertAtMs <= 0L) return@runCatching false
    if (System.currentTimeMillis() < match.alertAtMs) return@runCatching false

    val matchRef = db.collection("mutual_crush_matches").document(match.id)
    val flippedHere = db.runTransaction { tx ->
      val snap = tx.get(matchRef)
      if (!snap.exists()) return@runTransaction false
      val already = snap.getBoolean("alert24hDispatched") == true
      if (already) return@runTransaction false
      tx.update(
        matchRef,
        mapOf(
          "alert24hDispatched" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      true
    }.await()

    val targets = if (flippedHere) match.participantUids else listOf(uid)
    val notifId = "mutual_24h_${match.id}"
    targets.distinct().forEach { targetUid ->
      runCatching {
        val notifRef = db.collection("users").document(targetUid).collection("notifications").document(notifId)
        val shouldCreate = targetUid != uid || !notifRef.get().await().exists()
        if (shouldCreate) {
          notifRef.set(
            mapOf(
              "recipientId" to targetUid,
              "actorId" to uid,
              "actorName" to "Secret Love",
              "type" to "mutual_unlock_24h",
              "title" to "Secret Love 24h Alert ⏳",
              "body" to MutualSecretLoveEngine.ALERT_MESSAGE,
              "targetId" to match.id,
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
    }
    flippedHere
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "mutual_crush_matches/${match.id}")
  }

  suspend fun dispatchMutualUnlockIfNeeded(match: MutualCrushMatch): Result<Boolean> = runCatching {
    val uid = requireUserId()
    if (match.id.isBlank() || match.revealAtMs <= 0L) return@runCatching false
    if (System.currentTimeMillis() < match.revealAtMs) return@runCatching false

    val matchRef = db.collection("mutual_crush_matches").document(match.id)
    val flippedHere = db.runTransaction { tx ->
      val snap = tx.get(matchRef)
      if (!snap.exists()) return@runTransaction false
      val already = snap.getBoolean("unlockedNotified") == true
      if (already) return@runTransaction false
      tx.update(
        matchRef,
        mapOf(
          "alert24hDispatched" to true,
          "unlockedNotified" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      true
    }.await()

    val targets = if (flippedHere) match.participantUids else listOf(uid)
    val notifId = "mutual_unlocked_${match.id}"
    targets.distinct().forEach { targetUid ->
      runCatching {
        val notifRef = db.collection("users").document(targetUid).collection("notifications").document(notifId)
        val shouldCreate = targetUid != uid || !notifRef.get().await().exists()
        if (shouldCreate) {
          notifRef.set(
            mapOf(
              "recipientId" to targetUid,
              "actorId" to uid,
              "actorName" to "Secret Love",
              "type" to "crush_match",
              "title" to "Mutual Secret Love Unlocked! 💖",
              "body" to "Your 24-hour countdown reached zero. Tap to reveal your mutual Secret Love!",
              "targetId" to match.id,
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
    }
    flippedHere
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "mutual_crush_matches/${match.id}")
  }

  // --- PRIVATE MASKED CHAT FOR UNLOCKED MUTUAL SECRET LOVE ---
  fun observePrivateMatchMessages(matchId: String): Flow<List<PrivateCrushMessage>> =
    db.collection("mutual_crush_matches").document(matchId).collection("private_messages")
      .snapshots()
      .map { snap ->
        snap.toObjects(PrivateCrushMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(
            compareBy<PrivateCrushMessage>(
              { it.createdAt?.seconds ?: Long.MAX_VALUE },
              { it.createdAt?.nanoseconds ?: Int.MAX_VALUE },
              { it.id }
            )
          )
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "mutual_crush_matches/$matchId/private_messages")
        throw e
      }

  suspend fun sendPrivateMatchMessage(
    matchId: String,
    text: String,
    moodEmoji: String = "💌"
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val clean = text.trim().take(2000)
    require(clean.isNotBlank()) { "Write a message first." }
    db.collection("mutual_crush_matches").document(matchId).collection("private_messages").document().set(
      mapOf(
        "senderId" to uid,
        "text" to clean,
        "moodEmoji" to moodEmoji.trim().ifBlank { "💌" }.take(16),
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "mutual_crush_matches/$matchId/private_messages")
  }

  suspend fun revealSecretLoveIdentity(sent: SentSecretLove, sender: PublicUser): Result<Unit> = runCatching {
    val uid = requireUserId()
    val targetMessageId = sent.messageId.ifBlank { sent.id }
    val batch = db.batch()
    batch.update(
      db.collection("secret_loves").document(targetMessageId),
      mapOf(
        "isRevealed" to true,
        "revealedSenderId" to uid,
        "revealedSenderName" to sender.displayName.ifBlank { "Admirer" }.take(80)
      )
    )
    batch.update(
      db.collection("users").document(uid).collection("sent_secret_loves").document(targetMessageId),
      "isRevealed", true
    )
    batch.commit().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "secret_loves/${sent.messageId}")
  }

  suspend fun markSecretLoveRead(messageId: String) {
    runCatching {
      db.collection("secret_loves").document(messageId).update("isRead", true).await()
    }
  }

  suspend fun blockOrReportSecretLove(messageId: String, block: Boolean, reportReason: String?): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("secret_loves").document(messageId).update(
      mapOf(
        "isBlocked" to block,
        "isReported" to (reportReason != null)
      )
    ).await()
    if (!reportReason.isNullOrBlank()) {
      db.collection("reports").document().set(
        mapOf(
          "reporterId" to uid,
          "targetType" to "secret_love",
          "targetId" to messageId,
          "reason" to reportReason.trim().take(500),
          "status" to "open",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "secret_loves/$messageId")
  }

  // --- PURE LOVE DEVOTION TRIALS (Sender-Private Progress & Milestone Badges) ---
  fun observeMyDevotionTrials(userId: String): Flow<List<DevotionTrial>> =
    db.collection("devotion_trials")
      .whereEqualTo("senderId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(DevotionTrial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.completedDays }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "devotion_trials")
        throw e
      }

  suspend fun startOrUpdateDevotionTrialTarget(
    recipient: PublicUser,
    targetTierDays: Int
  ): Result<DevotionTrial> = runCatching {
    val uid = requireUserId()
    require(uid != recipient.userId) { "Choose someone else to start a Pure Love Quest." }
    require(targetTierDays in listOf(7, 30, 90, 180, 210, 270, 300, 365)) { "Select a valid devotion duration." }
    val trialId = "${uid}_${recipient.userId}"
    val ref = db.collection("devotion_trials").document(trialId)
    val existing = ref.get().await()
    val safeName = recipient.displayName.ifBlank { "Member" }.take(80)
    if (!existing.exists()) {
      ref.set(
        mapOf(
          "senderId" to uid,
          "recipientId" to recipient.userId,
          "recipientName" to safeName,
          "targetTierDays" to targetTierDays,
          "completedDays" to 0,
          "currentStreak" to 0,
          "missedDaysCount" to 0,
          "successRatePct" to 100,
          "lastCheckInMs" to 0L,
          "lastReflection" to "",
          "unlockedBadges" to emptyList<String>(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } else {
      ref.update(
        mapOf(
          "recipientName" to safeName,
          "targetTierDays" to targetTierDays,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    ref.get().await().toObject(DevotionTrial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      ?: DevotionTrial(
        id = trialId,
        senderId = uid,
        recipientId = recipient.userId,
        recipientName = safeName,
        targetTierDays = targetTierDays
      )
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "devotion_trials")
  }

  suspend fun completeDailyDevotionCheckIn(
    recipient: PublicUser,
    targetTierDays: Int,
    reflectionText: String
  ): Result<PureLoveTier?> = runCatching {
    val uid = requireUserId()
    val validationErr = PureLoveEngine.validateJournalReflection(reflectionText)
    require(validationErr == null) { validationErr ?: "Reflection invalid" }

    val trialId = "${uid}_${recipient.userId}"
    val ref = db.collection("devotion_trials").document(trialId)
    val snap = ref.get().await()
    val current = if (snap.exists()) {
      snap.toObject(DevotionTrial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: DevotionTrial(id = trialId, senderId = uid, recipientId = recipient.userId)
    } else {
      startOrUpdateDevotionTrialTarget(recipient, targetTierDays).getOrThrow()
    }

    val nowMs = System.currentTimeMillis()
    val cooldownLeft = PureLoveEngine.cooldownRemainingMs(current.lastCheckInMs, nowMs)
    require(cooldownLeft == 0L) {
      val hrs = (cooldownLeft / 3_600_000L)
      val mins = ((cooldownLeft % 3_600_000L) / 60_000L)
      "Daily Pure Love cooldown active: return in ${hrs}h ${mins}m."
    }

    val next = PureLoveEngine.computeNextCheckIn(current, nowMs)
    ref.update(
      mapOf(
        "targetTierDays" to targetTierDays,
        "completedDays" to next.completedDays,
        "currentStreak" to next.currentStreak,
        "missedDaysCount" to next.missedDaysCount,
        "successRatePct" to next.successRatePct,
        "lastCheckInMs" to nowMs,
        "lastReflection" to reflectionText.trim().take(1000),
        "unlockedBadges" to next.unlockedBadges,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()

    // If a new milestone badge unlocked, send an anonymous alert to recipient (never exposing sender identity!)
    val badge = next.newlyUnlockedBadge
    if (badge != null) {
      runCatching {
        db.collection("users").document(recipient.userId).collection("notifications").document().set(
          mapOf(
            "recipientId" to recipient.userId,
            "actorId" to uid,
            "actorName" to "Secret Admirer",
            "type" to "secret_love",
            "title" to "${badge.badgeEmoji} Anonymous Pure Love Milestone: ${badge.badgeTitle}!",
            "body" to "Someone silently dedicated ${badge.label} (${badge.rarityLabel}) of the Pure Love Challenge to you!".take(400),
            "targetId" to trialId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    }
    badge
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "devotion_trials")
  }

  // --- OWNER PROFILE & GLOBAL SETTINGS ---
  fun observeOwnerConfig(): Flow<OwnerConfig?> =
    db.collection("app_config").document("owner")
      .snapshots()
      .map { snap -> snap.toObject(OwnerConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.GET, "app_config/owner")
        throw e
      }

  fun observeGlobalSettings(): Flow<GlobalSettings?> =
    db.collection("app_config").document("global_settings")
      .snapshots()
      .map { snap -> snap.toObject(GlobalSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.GET, "app_config/global_settings")
        throw e
      }

  suspend fun updateOwnerProfile(
    ownerName: String,
    ownerHandle: String,
    ownerAvatar: String,
    about: String,
    contactEmail: String,
    contactInfo: String,
    showContactEmail: Boolean = false,
    showOwnerHandle: Boolean = true
  ): Result<Unit> = runCatching {
    requireUserId()
    db.collection("app_config").document("owner").update(
      mapOf(
        "ownerName" to ownerName.trim().take(80),
        "ownerHandle" to ownerHandle.trim().take(40),
        "showOwnerHandle" to showOwnerHandle,
        "ownerAvatar" to ownerAvatar.trim().take(1000),
        "about" to about.trim().take(600),
        "contactEmail" to contactEmail.trim().take(160),
        "showContactEmail" to showContactEmail,
        "contactInfo" to contactInfo.trim().take(200),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "app_config/owner")
  }

  suspend fun setOwnerVisibility(
    showOwnerHandle: Boolean? = null,
    showContactEmail: Boolean? = null
  ): Result<Unit> = runCatching {
    requireUserId()
    val updates = mutableMapOf<String, Any>(
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (showOwnerHandle != null) updates["showOwnerHandle"] = showOwnerHandle
    if (showContactEmail != null) updates["showContactEmail"] = showContactEmail
    if (updates.size > 1) {
      db.collection("app_config").document("owner").update(updates).await()
    }
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "app_config/owner")
  }

  suspend fun updateGlobalSettings(
    appName: String,
    tagline: String,
    maintenanceBanner: String,
    sosEnabled: Boolean,
    secretLoveEnabled: Boolean,
    allowRegistrations: Boolean
  ): Result<Unit> = runCatching {
    requireUserId()
    db.collection("app_config").document("global_settings").set(
      mapOf(
        "appName" to appName.trim().ifBlank { "HELLO" }.take(40),
        "tagline" to tagline.trim().take(160),
        "maintenanceBanner" to maintenanceBanner.trim().take(300),
        "sosFeatureEnabled" to sosEnabled,
        "secretLoveEnabled" to secretLoveEnabled,
        "allowNewRegistrations" to allowRegistrations,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "app_config/global_settings")
  }

  // --- OWNER DASHBOARD GOVERNANCE (Admins, Reports, Users) ---
  fun observeAdmins(): Flow<List<AdminRecord>> =
    db.collection("admins")
      .snapshots()
      .map { snap -> snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "admins")
        throw e
      }

  suspend fun addAdmin(targetUser: PublicUser): Result<Unit> = runCatching {
    val ownerUid = requireUserId()
    val batch = db.batch()
    batch.set(
      db.collection("admins").document(targetUser.userId),
      mapOf(
        "uid" to targetUser.userId,
        "email" to "",
        "displayName" to targetUser.displayName.ifBlank { "Admin" }.take(80),
        "addedByUid" to ownerUid,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.update(
      db.collection("users_public").document(targetUser.userId),
      mapOf(
        "role" to "admin",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "admins/${targetUser.userId}")
  }

  suspend fun removeAdmin(adminUid: String): Result<Unit> = runCatching {
    requireUserId()
    val batch = db.batch()
    batch.delete(db.collection("admins").document(adminUid))
    batch.update(
      db.collection("users_public").document(adminUid),
      mapOf(
        "role" to "user",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "admins/$adminUid")
  }

  fun observeReports(): Flow<List<ReportEntity>> =
    db.collection("reports")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(ReportEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "reports")
        throw e
      }

  suspend fun resolveReport(reportId: String, status: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("reports").document(reportId).update(
      mapOf(
        "status" to status,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
  }

  suspend fun removePostAsModerator(postId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("posts").document(postId).delete().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
  }

  fun observeAllRegionalAnalytics(): Flow<List<RegionalAnalytics>> =
    db.collection("regional_analytics")
      .snapshots()
      .map { snap ->
        snap.toObjects(RegionalAnalytics::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "regional_analytics")
        throw e
      }

  // Strictly READ-ONLY Owner Audit Streams (Owner can inspect A-to-Z but never alter mutual crush selections)
  data class CrushSignalAuditItem(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val createdAt: Timestamp? = null
  )

  fun observeAllCrushSignalsForOwner(): Flow<List<CrushSignalAuditItem>> =
    db.collection("crush_signals")
      .snapshots()
      .map { snap ->
        snap.documents.map { doc ->
          CrushSignalAuditItem(
            id = doc.id,
            fromUid = doc.getString("fromUid").orEmpty(),
            toUid = doc.getString("toUid").orEmpty(),
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          )
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "crush_signals")
        throw e
      }

  fun observeAllMutualMatchesForOwner(): Flow<List<MutualCrushMatch>> =
    db.collection("mutual_crush_matches")
      .snapshots()
      .map { snap ->
        snap.toObjects(MutualCrushMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.matchedAtMs }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "mutual_crush_matches")
        throw e
      }

  fun observeAllPrivateUsersForOwner(): Flow<List<PrivateUser>> =
    db.collection("users")
      .snapshots()
      .map { snap ->
        snap.toObjects(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users")
        throw e
      }
}
