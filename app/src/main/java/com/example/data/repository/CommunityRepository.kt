package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.OperationType
import com.example.data.model.PublicUser
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

class CommunityRepository(
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

  // Public Users directory (matches rule: existing().profilePrivacy != 'private')
  fun observeDiscoverableUsers(): Flow<List<PublicUser>> =
    db.collection("users_public")
      .whereIn("profilePrivacy", listOf("public", "friends"))
      .snapshots()
      .map { snap -> snap.toObjects(PublicUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users_public")
        throw e
      }

  fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> =
    db.collection("users").document(userId).collection("blocked")
      .snapshots()
      .map { snap -> snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$userId/blocked")
        throw e
      }

  suspend fun blockUser(targetUid: String, targetName: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("users").document(uid).collection("blocked").document(targetUid).set(
      mapOf(
        "blockedUid" to targetUid,
        "blockedName" to targetName.ifBlank { "Member" }.take(80),
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "blocked/$targetUid")
  }

  suspend fun unblockUser(targetUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("users").document(uid).collection("blocked").document(targetUid).delete().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "blocked/$targetUid")
  }

  // Friend Requests & Friendships
  fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> =
    db.collection("friend_requests")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap -> snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "friend_requests")
        throw e
      }

  fun observeFriendships(userId: String): Flow<List<Friendship>> =
    db.collection("friendships")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap -> snap.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "friendships")
        throw e
      }

  suspend fun sendFriendRequest(sender: PublicUser, recipient: PublicUser): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(uid != recipient.userId) { "Cannot send friend request to yourself" }
    val reqId = "${uid}_${recipient.userId}"
    db.collection("friend_requests").document(reqId).set(
      mapOf(
        "fromUid" to uid,
        "toUid" to recipient.userId,
        "fromName" to sender.displayName.ifBlank { "Hello Member" }.take(80),
        "fromAvatar" to sender.photoUrl.take(1000),
        "toName" to recipient.displayName.ifBlank { "Hello Member" }.take(80),
        "toAvatar" to recipient.photoUrl.take(1000),
        "status" to "pending",
        "participantUids" to listOf(uid, recipient.userId),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()

    sendNotification(
      recipientId = recipient.userId,
      actorId = uid,
      actorName = sender.displayName,
      type = "friend_request",
      title = "New Friend Request",
      body = "${sender.displayName} sent you a friend request.",
      targetId = reqId
    )
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "friend_requests")
  }

  suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> = runCatching {
    val uid = requireUserId()
    val sorted = listOf(request.fromUid, request.toUid).sorted()
    val friendshipId = "${sorted[0]}_${sorted[1]}"
    val batch = db.batch()
    batch.update(
      db.collection("friend_requests").document(request.id),
      mapOf(
        "status" to "accepted",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.set(
      db.collection("friendships").document(friendshipId),
      mapOf(
        "participantUids" to sorted,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    sendNotification(
      recipientId = request.fromUid,
      actorId = uid,
      actorName = request.toName,
      type = "friend_request",
      title = "Friend Request Accepted",
      body = "${request.toName.ifBlank { "Your friend" }} accepted your request!",
      targetId = friendshipId
    )
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "friend_requests/${request.id}")
  }

  suspend fun rejectOrCancelFriendRequest(requestId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("friend_requests").document(requestId).delete().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "friend_requests/$requestId")
  }

  suspend fun unfriend(otherUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val sorted = listOf(uid, otherUid).sorted()
    val friendshipId = "${sorted[0]}_${sorted[1]}"
    db.collection("friendships").document(friendshipId).delete().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "friendships")
  }

  // Messenger
  fun observeConversations(userId: String): Flow<List<Conversation>> =
    db.collection("conversations")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "conversations")
        throw e
      }

  suspend fun getOrCreateDirectConversation(me: PublicUser, other: PublicUser): Result<String> = runCatching {
    val uid = requireUserId()
    val sorted = listOf(uid, other.userId).sorted()
    val convId = "dm_${sorted[0]}_${sorted[1]}"
    val ref = db.collection("conversations").document(convId)
    val snap = ref.get().await()
    if (!snap.exists()) {
      ref.set(
        mapOf(
          "isGroup" to false,
          "title" to other.displayName.take(100),
          "participantUids" to sorted,
          "participantNames" to mapOf(
            uid to me.displayName.ifBlank { "Me" },
            other.userId to other.displayName.ifBlank { "Friend" }
          ),
          "lastMessage" to "",
          "lastSenderId" to "",
          "typingUids" to emptyList<String>(),
          "readByUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    convId
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "conversations")
  }

  suspend fun createGroupConversation(
    title: String,
    me: PublicUser,
    memberUsers: List<PublicUser>
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val allParticipants = (listOf(uid) + memberUsers.map { it.userId }).distinct().take(50)
    val namesMap = buildMap {
      put(uid, me.displayName.ifBlank { "Me" })
      memberUsers.forEach { put(it.userId, it.displayName.ifBlank { "Member" }) }
    }
    val ref = db.collection("conversations").document()
    ref.set(
      mapOf(
        "isGroup" to true,
        "title" to title.trim().ifBlank { "Group Chat" }.take(100),
        "participantUids" to allParticipants,
        "participantNames" to namesMap,
        "lastMessage" to "Group created",
        "lastSenderId" to uid,
        "typingUids" to emptyList<String>(),
        "readByUids" to listOf(uid),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "conversations")
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
    db.collection("conversations").document(conversationId).collection("messages")
      .whereArrayContains("participantUids", requireUserId())
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "conversations/$conversationId/messages")
        throw e
      }

  suspend fun sendMessage(
    conversation: Conversation,
    sender: PublicUser,
    text: String,
    mediaUri: String = "",
    mediaType: String = "none",
    fileName: String = ""
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val msgRef = db.collection("conversations").document(conversation.id).collection("messages").document()
    val convRef = db.collection("conversations").document(conversation.id)
    val preview = when {
      text.isNotBlank() -> text.trim().take(200)
      mediaType == "image" -> "📷 Photo"
      mediaType == "video" -> "🎬 Video"
      mediaType == "audio" -> "🎤 Voice note"
      mediaType == "file" -> "📎 ${fileName.ifBlank { "Attachment" }}"
      else -> "Message"
    }
    val batch = db.batch()
    batch.set(
      msgRef,
      mapOf(
        "conversationId" to conversation.id,
        "senderId" to uid,
        "senderName" to sender.displayName.ifBlank { "Member" }.take(80),
        "text" to text.trim().take(2000),
        "mediaUri" to mediaUri.take(2000),
        "mediaType" to mediaType,
        "fileName" to fileName.take(200),
        "reactions" to emptyMap<String, String>(),
        "participantUids" to conversation.participantUids,
        "isDeleted" to false,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.update(
      convRef,
      mapOf(
        "lastMessage" to preview,
        "lastSenderId" to uid,
        "readByUids" to listOf(uid),
        "typingUids" to FieldValue.arrayRemove(uid),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    conversation.participantUids.filter { it != uid }.forEach { otherUid ->
      sendNotification(
        recipientId = otherUid,
        actorId = uid,
        actorName = sender.displayName,
        type = "message",
        title = sender.displayName.ifBlank { "New Message" },
        body = preview,
        targetId = conversation.id
      )
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "conversations/${conversation.id}/messages")
  }

  suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
    val uid = auth.currentUser?.uid ?: return
    runCatching {
      db.collection("conversations").document(conversationId).update(
        mapOf(
          "typingUids" to if (isTyping) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun markConversationRead(conversationId: String) {
    val uid = auth.currentUser?.uid ?: return
    runCatching {
      db.collection("conversations").document(conversationId).update(
        mapOf(
          "readByUids" to FieldValue.arrayUnion(uid),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }

  suspend fun reactToMessage(conversationId: String, message: ChatMessage, emoji: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val updated = message.reactions.toMutableMap()
    if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
    db.collection("conversations").document(conversationId)
      .collection("messages").document(message.id)
      .update("reactions", updated)
      .await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/${message.id}")
  }

  suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("conversations").document(conversationId)
      .collection("messages").document(messageId)
      .update(
        mapOf(
          "isDeleted" to true,
          "text" to "Message deleted"
        )
      ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
  }

  suspend fun leaveConversation(conversationId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("conversations").document(conversationId)
      .update(
        mapOf(
          "participantUids" to FieldValue.arrayRemove(uid),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
  }

  // Notifications
  fun observeNotifications(userId: String): Flow<List<AppNotification>> =
    db.collection("users").document(userId).collection("notifications")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$userId/notifications")
        throw e
      }

  suspend fun markNotificationRead(notifId: String) {
    val uid = auth.currentUser?.uid ?: return
    runCatching {
      db.collection("users").document(uid).collection("notifications").document(notifId)
        .update("isRead", true)
        .await()
    }
  }

  suspend fun markAllNotificationsRead(notifs: List<AppNotification>) {
    val uid = auth.currentUser?.uid ?: return
    val unread = notifs.filter { !it.isRead }
    if (unread.isEmpty()) return
    runCatching {
      val batch = db.batch()
      unread.forEach { n ->
        batch.update(
          db.collection("users").document(uid).collection("notifications").document(n.id),
          "isRead", true
        )
      }
      batch.commit().await()
    }
  }

  suspend fun sendNotification(
    recipientId: String,
    actorId: String,
    actorName: String,
    type: String,
    title: String,
    body: String,
    targetId: String
  ) {
    if (recipientId == actorId) return
    runCatching {
      db.collection("users").document(recipientId).collection("notifications").document().set(
        mapOf(
          "recipientId" to recipientId,
          "actorId" to actorId,
          "actorName" to actorName.take(80),
          "type" to type,
          "title" to title.take(120),
          "body" to body.take(400),
          "targetId" to targetId.take(128),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }
}
