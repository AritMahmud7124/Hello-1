package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRecord
import com.example.data.model.GlobalSettings
import com.example.data.model.OperationType
import com.example.data.model.OwnerConfig
import com.example.data.model.PublicUser
import com.example.data.model.ReportEntity
import com.example.data.model.SecretLoveMessage
import com.example.data.model.SentSecretLove
import com.example.data.model.handleFirestoreError
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
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(uid != recipient.userId) { "Choose someone else to send a Secret Love note." }
    require(recipient.acceptSecretLove) { "${recipient.displayName} has disabled Secret Love messages." }

    val messageRef = db.collection("secret_loves").document()
    val messageId = messageRef.id
    val receiptRef = db.collection("users").document(uid).collection("sent_secret_loves").document(messageId)
    val cleanMsg = message.trim().take(1000)
    val cleanEmoji = moodEmoji.ifBlank { "❤️" }.take(16)

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
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    // Send anonymous notification to recipient (never reveal sender unless revealIdentityNow == true)
    runCatching {
      val safeSenderName = sender.displayName.ifBlank { "Admirer" }.take(80)
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
    messageId
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "secret_loves")
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
    contactInfo: String
  ): Result<Unit> = runCatching {
    requireUserId()
    db.collection("app_config").document("owner").update(
      mapOf(
        "ownerName" to ownerName.trim().take(80),
        "ownerHandle" to ownerHandle.trim().take(40),
        "ownerAvatar" to ownerAvatar.trim().take(1000),
        "about" to about.trim().take(600),
        "contactEmail" to contactEmail.trim().take(160),
        "contactInfo" to contactInfo.trim().take(200),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
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
}
