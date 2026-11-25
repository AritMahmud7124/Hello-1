package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallSignal
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.MessengerCall
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // If the recipient already sent a pending request to the current user, accept it directly
    val reverseId = "${recipient.userId}_$uid"
    val reverseSnap = runCatching { db.collection("friend_requests").document(reverseId).get().await() }.getOrNull()
    if (reverseSnap != null && reverseSnap.exists() && reverseSnap.getString("status") == "pending") {
      val reverseReq = reverseSnap.toObject(FriendRequest::class.java)
      if (reverseReq != null) {
        acceptFriendRequest(reverseReq).getOrThrow()
        return@runCatching Unit
      }
    }

    val reqId = "${uid}_${recipient.userId}"
    val reqRef = db.collection("friend_requests").document(reqId)
    val existingSnap = runCatching { reqRef.get().await() }.getOrNull()
    val fromName = sender.displayName.ifBlank { "Hello Member" }.take(80)
    val fromAvatar = sender.photoUrl.take(1000)
    val toName = recipient.displayName.ifBlank { "Hello Member" }.take(80)
    val toAvatar = recipient.photoUrl.take(1000)

    if (existingSnap != null && existingSnap.exists()) {
      if (existingSnap.getString("status") == "pending") {
        return@runCatching Unit
      }
      reqRef.update(
        mapOf(
          "fromName" to fromName,
          "fromAvatar" to fromAvatar,
          "toName" to toName,
          "toAvatar" to toAvatar,
          "status" to "pending",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } else {
      reqRef.set(
        mapOf(
          "fromUid" to uid,
          "toUid" to recipient.userId,
          "fromName" to fromName,
          "fromAvatar" to fromAvatar,
          "toName" to toName,
          "toAvatar" to toAvatar,
          "status" to "pending",
          "participantUids" to listOf(uid, recipient.userId),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }

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
    runCatching { db.collection("friend_requests").document("${uid}_$otherUid").delete().await() }
    runCatching { db.collection("friend_requests").document("${otherUid}_$uid").delete().await() }
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
          .filter { it.id.isNotBlank() }
          .distinctBy { it.id }
          .sortedWith(
            compareByDescending<Conversation> { it.updatedAt?.seconds ?: 0L }
              .thenBy { it.id }
          )
      }
      .distinctUntilChanged()
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
    memberUsers: List<PublicUser>,
    avatarUrl: String = ""
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
        "avatarUrl" to avatarUrl.trim().take(2000),
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

  suspend fun updateGroupAvatar(
    conversationId: String,
    avatarUrl: String
  ): Result<Unit> = runCatching {
    requireUserId()
    db.collection("conversations").document(conversationId).update(
      mapOf(
        "avatarUrl" to avatarUrl.trim().take(2000),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
    db.collection("conversations").document(conversationId).collection("messages")
      .whereArrayContains("participantUids", requireUserId())
      .snapshots()
      .map { snap ->
        val nowMs = System.currentTimeMillis()
        snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { it.id.isNotBlank() && (it.expiresAtMs <= 0L || it.expiresAtMs > nowMs) }
          .distinctBy { it.id }
          .sortedWith(
            compareBy<ChatMessage>(
              { it.createdAt?.seconds ?: Long.MAX_VALUE },
              { it.createdAt?.nanoseconds ?: Int.MAX_VALUE },
              { it.id }
            )
          )
      }
      .distinctUntilChanged()
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
    fileName: String = "",
    expiresAtMs: Long = 0L
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val msgRef = db.collection("conversations").document(conversation.id).collection("messages").document()
    val convRef = db.collection("conversations").document(conversation.id)
    val preview = when {
      expiresAtMs > 0L -> "⏳ Disappearing message"
      text.isNotBlank() -> text.trim().take(200)
      mediaType == "image" -> "📷 Photo"
      mediaType == "video" -> "🎬 Video"
      mediaType == "audio" -> "🎤 Voice note"
      mediaType == "file" -> "📎 ${fileName.ifBlank { "Attachment" }}"
      else -> "Message"
    }
    val safeMediaType = if (mediaType in listOf("none", "image", "video", "file", "audio")) mediaType else "none"
    val payload = buildMap<String, Any> {
      put("conversationId", conversation.id)
      put("senderId", uid)
      put("senderName", sender.displayName.trim().ifBlank { "Member" }.take(80))
      put("text", text.trim().take(2000))
      put("mediaUri", mediaUri.trim().take(2000))
      put("mediaType", safeMediaType)
      put("fileName", fileName.trim().take(200))
      put("reactions", emptyMap<String, String>())
      put("participantUids", (conversation.participantUids + uid).distinct().take(50))
      put("isDeleted", false)
      if (expiresAtMs > 0L) {
        put("expiresAtMs", expiresAtMs)
      }
      put("createdAt", FieldValue.serverTimestamp())
    }
    val batch = db.batch()
    batch.set(msgRef, payload)
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
    val ref = db.collection("conversations").document(conversationId)
    val snap = ref.get().await()
    val existing = snap.toObject(Conversation::class.java)
    val remaining = existing?.participantUids?.filter { it != uid } ?: emptyList()
    if (remaining.size < 2) {
      // Firestore rule requires participantUids.size >= 2 on update; close group/chat cleanly when < 2 remain
      ref.delete().await()
    } else {
      val updatedNames = existing?.participantNames?.toMutableMap()?.apply { remove(uid) } ?: emptyMap()
      ref.update(
        mapOf(
          "participantUids" to remaining,
          "participantNames" to updatedNames,
          "typingUids" to FieldValue.arrayRemove(uid),
          "readByUids" to FieldValue.arrayRemove(uid),
          "lastMessage" to "A member left the group",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
  }

  suspend fun removeGroupParticipant(
    conversation: Conversation,
    targetUid: String,
    targetName: String
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(conversation.isGroup) { "Members can only be removed from group chats." }
    require(uid in conversation.participantUids) { "Not a group participant." }
    if (targetUid == uid) {
      leaveConversation(conversation.id).getOrThrow()
      return@runCatching Unit
    }
    val remaining = conversation.participantUids.filter { it != targetUid }
    val ref = db.collection("conversations").document(conversation.id)
    if (remaining.size < 2) {
      ref.delete().await()
    } else {
      val updatedNames = conversation.participantNames.toMutableMap().apply { remove(targetUid) }
      ref.update(
        mapOf(
          "participantUids" to remaining,
          "participantNames" to updatedNames,
          "typingUids" to FieldValue.arrayRemove(targetUid),
          "readByUids" to FieldValue.arrayRemove(targetUid),
          "lastMessage" to "${targetName.ifBlank { "Member" }} was removed from group",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
  }

  suspend fun addGroupParticipants(
    conversation: Conversation,
    membersToAdd: List<PublicUser>
  ): Result<Unit> = runCatching {
    requireUserId()
    require(conversation.isGroup) { "Only group chats can add members." }
    if (membersToAdd.isEmpty()) return@runCatching Unit
    val mergedUids = (conversation.participantUids + membersToAdd.map { it.userId }).distinct().take(50)
    val mergedNames = conversation.participantNames.toMutableMap().apply {
      membersToAdd.forEach { put(it.userId, it.displayName.ifBlank { "Member" }) }
    }
    db.collection("conversations").document(conversation.id).update(
      mapOf(
        "participantUids" to mergedUids,
        "participantNames" to mergedNames,
        "lastMessage" to "Added ${membersToAdd.joinToString(", ") { it.displayName.ifBlank { "Member" } }}",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
  }

  // --- MESSENGER AUDIO & VIDEO CALL SIGNALING ---
  fun observeActiveCalls(userId: String): Flow<List<MessengerCall>> =
    db.collection("calls")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        val nowMs = System.currentTimeMillis()
        snap.toObjects(MessengerCall::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { call ->
            when (call.status) {
              "ringing" -> kotlin.math.abs(nowMs - call.startedAtMs) < 180_000L
              "active" -> kotlin.math.abs(nowMs - call.startedAtMs) < 14_400_000L
              else -> false
            }
          }
          .sortedByDescending { it.startedAtMs }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "calls")
        throw e
      }

  suspend fun startCall(
    conversation: Conversation,
    caller: PublicUser,
    callType: String
  ): Result<MessengerCall> = runCatching {
    val uid = requireUserId()
    val safeType = if (callType == "video") "video" else "audio"
    val ref = db.collection("calls").document()
    val nowMs = System.currentTimeMillis()
    val participants = (conversation.participantUids + uid).distinct().take(50)
    val safeCallerName = caller.displayName.trim().ifBlank { "Member" }.take(80)
    val convTitle = conversation.title.trim().take(100)
    ref.set(
      mapOf(
        "conversationId" to conversation.id,
        "conversationTitle" to convTitle,
        "callerId" to uid,
        "callerName" to safeCallerName,
        "participantUids" to participants,
        "acceptedUids" to listOf(uid),
        "callType" to safeType,
        "status" to "ringing",
        "startedAtMs" to nowMs,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()

    val icon = if (safeType == "video") "📹" else "📞"
    participants.filter { it != uid }.forEach { otherUid ->
      sendNotification(
        recipientId = otherUid,
        actorId = uid,
        actorName = safeCallerName,
        type = "message",
        title = "$icon Incoming ${if (safeType == "video") "Video" else "Audio"} Call",
        body = "$safeCallerName is calling you on HELLO Messenger",
        targetId = conversation.id
      )
    }

    MessengerCall(
      id = ref.id,
      conversationId = conversation.id,
      conversationTitle = convTitle,
      callerId = uid,
      callerName = safeCallerName,
      participantUids = participants,
      acceptedUids = listOf(uid),
      callType = safeType,
      status = "ringing",
      startedAtMs = nowMs
    )
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "calls")
  }

  suspend fun answerCall(callId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("calls").document(callId).update(
      mapOf(
        "status" to "active",
        "acceptedUids" to FieldValue.arrayUnion(uid),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
  }

  suspend fun endOrDeclineCall(call: MessengerCall, newStatus: String): Result<Unit> = runCatching {
    requireUserId()
    val safeStatus = if (newStatus in listOf("ended", "declined", "missed")) newStatus else "ended"
    db.collection("calls").document(call.id).update(
      mapOf(
        "status" to safeStatus,
        "endedAtMs" to System.currentTimeMillis(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "calls/${call.id}")
  }

  fun observeCallSignals(callId: String): Flow<List<CallSignal>> =
    db.collection("calls").document(callId).collection("signals")
      .snapshots()
      .map { snap ->
        snap.toObjects(CallSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(
            compareBy<CallSignal>(
              { it.createdAt?.seconds ?: Long.MAX_VALUE },
              { it.createdAt?.nanoseconds ?: Int.MAX_VALUE },
              { it.id }
            )
          )
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "calls/$callId/signals")
        throw e
      }

  suspend fun sendCallSignal(
    callId: String,
    recipientId: String = "",
    type: String,
    sdp: String = "",
    sdpMid: String = "",
    sdpMLineIndex: Int = 0,
    candidate: String = ""
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeType = if (type in listOf("offer", "answer", "ice")) type else "ice"
    db.collection("calls").document(callId).collection("signals").document().set(
      mapOf(
        "senderId" to uid,
        "recipientId" to recipientId.trim().take(128),
        "type" to safeType,
        "sdp" to sdp.take(25000),
        "sdpMid" to sdpMid.take(128),
        "sdpMLineIndex" to sdpMLineIndex.coerceIn(0, 64),
        "candidate" to candidate.take(4000),
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "calls/$callId/signals")
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
    if (notifId.isBlank()) return
    runCatching {
      db.collection("users").document(uid).collection("notifications").document(notifId)
        .update("isRead", true)
        .await()
    }.onFailure { e ->
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notifId")
    }
  }

  suspend fun markAllNotificationsRead(notifs: List<AppNotification>) {
    val uid = auth.currentUser?.uid ?: return
    val unread = notifs.filter { !it.isRead && it.id.isNotBlank() }
    if (unread.isEmpty()) return
    runCatching {
      unread.chunked(450).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { n ->
          batch.update(
            db.collection("users").document(uid).collection("notifications").document(n.id),
            "isRead", true
          )
        }
        batch.commit().await()
      }
    }.onFailure { e ->
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications")
    }
  }

  suspend fun deleteNotification(notifId: String) {
    val uid = auth.currentUser?.uid ?: return
    if (notifId.isBlank()) return
    runCatching {
      db.collection("users").document(uid).collection("notifications").document(notifId)
        .delete()
        .await()
    }.onFailure { e ->
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/notifications/$notifId")
    }
  }

  suspend fun clearAllNotifications(notifs: List<AppNotification>) {
    val uid = auth.currentUser?.uid ?: return
    val targets = notifs.filter { it.id.isNotBlank() }
    if (targets.isEmpty()) return
    runCatching {
      targets.chunked(450).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { n ->
          batch.delete(db.collection("users").document(uid).collection("notifications").document(n.id))
        }
        batch.commit().await()
      }
    }.onFailure { e ->
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/notifications")
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
          "actorName" to actorName.trim().take(80),
          "type" to type,
          "title" to title.trim().ifBlank { "HELLO" }.take(120),
          "body" to body.trim().ifBlank { "New notification" }.take(400),
          "targetId" to targetId.trim().take(128),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
  }
}
