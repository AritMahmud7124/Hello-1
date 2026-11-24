package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallSignal
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.MessengerCall
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.data.model.unwrapFirestoreMessage
import kotlinx.coroutines.flow.Flow
import com.example.data.repository.CommunityRepository
import com.example.data.repository.GovernanceRepository
import com.example.data.repository.SocialRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunityViewModel(
  private val communityRepository: CommunityRepository,
  private val socialRepository: SocialRepository,
  private val currentUserId: String,
  private val governanceRepository: GovernanceRepository? = null
) : ViewModel() {

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  private val ownerConfigFlow = (governanceRepository?.observeOwnerConfig() ?: flowOf(null))
    .catch { emit(null) }

  val discoverableUsers: StateFlow<UiState<List<PublicUser>>> = combine(
    communityRepository.observeDiscoverableUsers(),
    ownerConfigFlow
  ) { users, ownerCfg ->
    val hideOwnerHandle = ownerCfg != null && !ownerCfg.showOwnerHandle
    val sanitized = if (!hideOwnerHandle) {
      users
    } else {
      val ownerUid = ownerCfg?.ownerUid.orEmpty()
      users.map { u ->
        val isOwnerAccount = u.isVerifiedOwner || (ownerUid.isNotBlank() && u.userId == ownerUid)
        if (isOwnerAccount && u.userId != currentUserId) {
          u.copy(handle = "")
        } else {
          u
        }
      }
    }
    UiState.Success(sanitized) as UiState<List<PublicUser>>
  }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load people"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val friendRequests: StateFlow<List<FriendRequest>> = communityRepository.observeFriendRequests(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val friendships: StateFlow<List<Friendship>> = communityRepository.observeFriendships(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val blockedUsers: StateFlow<List<BlockedUser>> = communityRepository.observeBlockedUsers(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val conversations: StateFlow<UiState<List<Conversation>>> = communityRepository.observeConversations(currentUserId)
    .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load chats"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val notifications: StateFlow<UiState<List<AppNotification>>> = communityRepository.observeNotifications(currentUserId)
    .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load notifications"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val activeCalls: StateFlow<List<MessengerCall>> = communityRepository.observeActiveCalls(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  fun clearMessage() {
    _statusMessage.value = null
  }

  fun sendFriendRequest(me: PublicUser, recipient: PublicUser) {
    viewModelScope.launch {
      communityRepository.sendFriendRequest(me, recipient)
        .onSuccess { _statusMessage.value = "Friend request sent to ${recipient.displayName}" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to send request" }
    }
  }

  fun acceptFriendRequest(request: FriendRequest) {
    viewModelScope.launch {
      communityRepository.acceptFriendRequest(request)
        .onSuccess { _statusMessage.value = "You are now friends!" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to accept" }
    }
  }

  fun rejectOrCancelRequest(requestId: String) {
    viewModelScope.launch {
      communityRepository.rejectOrCancelFriendRequest(requestId)
        .onSuccess { _statusMessage.value = "Request removed" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update request" }
    }
  }

  fun unfriend(otherUid: String) {
    viewModelScope.launch {
      communityRepository.unfriend(otherUid)
        .onSuccess { _statusMessage.value = "Unfriended" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to unfriend" }
    }
  }

  fun blockUser(targetUid: String, targetName: String) {
    viewModelScope.launch {
      communityRepository.blockUser(targetUid, targetName)
        .onSuccess { _statusMessage.value = "$targetName blocked" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Block failed" }
    }
  }

  fun unblockUser(targetUid: String) {
    viewModelScope.launch {
      communityRepository.unblockUser(targetUid)
        .onSuccess { _statusMessage.value = "User unblocked" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Unblock failed" }
    }
  }

  fun startDirectChat(me: PublicUser, other: PublicUser, onOpen: (String) -> Unit) {
    viewModelScope.launch {
      communityRepository.getOrCreateDirectConversation(me, other)
        .onSuccess(onOpen)
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not open chat" }
    }
  }

  fun shareTextToConversation(conversation: Conversation, sender: PublicUser, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      communityRepository.sendMessage(conversation, sender, text)
        .onSuccess {
          val label = if (conversation.isGroup) conversation.title else "Messenger"
          _statusMessage.value = "Shared in $label"
        }
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not share in Messenger") }
    }
  }

  fun shareTextToPerson(sender: PublicUser, recipient: PublicUser, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      communityRepository.getOrCreateDirectConversation(sender, recipient)
        .onSuccess { convId ->
          val conv = Conversation(
            id = convId,
            isGroup = false,
            title = recipient.displayName,
            participantUids = listOf(currentUserId, recipient.userId).sorted(),
            participantNames = mapOf(
              currentUserId to sender.displayName.ifBlank { "Me" },
              recipient.userId to recipient.displayName.ifBlank { "Friend" }
            )
          )
          communityRepository.sendMessage(conv, sender, text)
            .onSuccess { _statusMessage.value = "Sent to ${recipient.displayName} in Messenger" }
            .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Failed to send message") }
        }
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not open Messenger chat") }
    }
  }

  fun createGroupChat(title: String, me: PublicUser, members: List<PublicUser>, onOpen: (String) -> Unit) {
    if (title.isBlank() || members.isEmpty()) {
      _statusMessage.value = "Add a group name and at least one member."
      return
    }
    viewModelScope.launch {
      communityRepository.createGroupConversation(title, me, members)
        .onSuccess(onOpen)
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to create group chat" }
    }
  }

  private val messageFlows = ConcurrentHashMap<String, StateFlow<List<ChatMessage>>>()

  fun observeMessages(conversationId: String): StateFlow<List<ChatMessage>> =
    messageFlows.getOrPut(conversationId) {
      communityRepository.observeMessages(conversationId)
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    }

  fun sendMessage(
    context: Context,
    conversation: Conversation,
    sender: PublicUser,
    text: String,
    mediaUri: String = "",
    mediaType: String = "none",
    fileName: String = "",
    expiresAfterMs: Long = 0L,
    onMediaUploaded: () -> Unit = {}
  ) {
    if (text.isBlank() && mediaUri.isBlank()) return
    viewModelScope.launch {
      val uploadedUrl = if (mediaUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, mediaUri, "messages")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _statusMessage.value = err.localizedMessage ?: "Attachment upload failed"
          return@launch
        }
      } else {
        ""
      }
      onMediaUploaded()
      val expiresAtMs = if (expiresAfterMs > 0L) System.currentTimeMillis() + expiresAfterMs else 0L
      communityRepository.sendMessage(conversation, sender, text, uploadedUrl, mediaType, fileName, expiresAtMs)
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Message failed" }
    }
  }

  fun setTyping(conversationId: String, isTyping: Boolean, stealthHideStatus: Boolean = false) {
    viewModelScope.launch {
      communityRepository.setTypingStatus(conversationId, isTyping && !stealthHideStatus)
    }
  }

  fun markRead(conversationId: String) {
    viewModelScope.launch { communityRepository.markConversationRead(conversationId) }
  }

  fun reactToMessage(conversationId: String, message: ChatMessage, emoji: String) {
    viewModelScope.launch { communityRepository.reactToMessage(conversationId, message, emoji) }
  }

  fun deleteMessage(conversationId: String, messageId: String) {
    viewModelScope.launch { communityRepository.deleteMessage(conversationId, messageId) }
  }

  fun leaveConversation(conversationId: String, onLeft: () -> Unit) {
    viewModelScope.launch {
      communityRepository.leaveConversation(conversationId)
        .onSuccess {
          _statusMessage.value = "You left the group chat."
          onLeft()
        }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not leave conversation" }
    }
  }

  fun removeGroupMember(conversation: Conversation, targetUid: String, targetName: String, onClosedIfEmpty: () -> Unit = {}) {
    viewModelScope.launch {
      communityRepository.removeGroupParticipant(conversation, targetUid, targetName)
        .onSuccess {
          _statusMessage.value = "$targetName removed from ${conversation.title}"
          if (conversation.participantUids.count { it != targetUid } < 2) {
            onClosedIfEmpty()
          }
        }
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not remove member") }
    }
  }

  fun addGroupMembers(conversation: Conversation, membersToAdd: List<PublicUser>) {
    viewModelScope.launch {
      communityRepository.addGroupParticipants(conversation, membersToAdd)
        .onSuccess { _statusMessage.value = "Added ${membersToAdd.size} member(s) to ${conversation.title}" }
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not add members") }
    }
  }

  fun startMessengerCall(conversation: Conversation, caller: PublicUser, callType: String) {
    if (activeCalls.value.isNotEmpty()) {
      _statusMessage.value = "A call is already in progress."
      return
    }
    viewModelScope.launch {
      communityRepository.startCall(conversation, caller, callType)
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not start call") }
    }
  }

  fun answerMessengerCall(callId: String) {
    viewModelScope.launch {
      communityRepository.answerCall(callId)
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Could not answer call") }
    }
  }

  fun endMessengerCall(call: MessengerCall, newStatus: String) {
    viewModelScope.launch {
      communityRepository.endOrDeclineCall(call, newStatus)
        .onFailure { _statusMessage.value = it.unwrapFirestoreMessage("Call update failed") }
    }
  }

  fun observeCallSignals(callId: String): Flow<List<CallSignal>> =
    communityRepository.observeCallSignals(callId).catch { emit(emptyList()) }

  fun sendCallSignal(
    callId: String,
    recipientId: String = "",
    type: String,
    sdp: String = "",
    sdpMid: String = "",
    sdpMLineIndex: Int = 0,
    candidate: String = ""
  ) {
    viewModelScope.launch {
      communityRepository.sendCallSignal(callId, recipientId, type, sdp, sdpMid, sdpMLineIndex, candidate)
    }
  }

  fun markNotificationRead(notifId: String) {
    viewModelScope.launch { communityRepository.markNotificationRead(notifId) }
  }

  fun markAllNotificationsRead(list: List<AppNotification>) {
    viewModelScope.launch { communityRepository.markAllNotificationsRead(list) }
  }

  fun deleteNotification(notifId: String) {
    viewModelScope.launch { communityRepository.deleteNotification(notifId) }
  }

  fun clearAllNotifications(list: List<AppNotification>) {
    viewModelScope.launch { communityRepository.clearAllNotifications(list) }
  }
}
