package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.data.model.unwrapFirestoreMessage
import com.example.data.repository.CommunityRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunityViewModel(
  private val communityRepository: CommunityRepository,
  private val socialRepository: SocialRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  val discoverableUsers: StateFlow<UiState<List<PublicUser>>> = communityRepository.observeDiscoverableUsers()
    .map<List<PublicUser>, UiState<List<PublicUser>>> { UiState.Success(it) }
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

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
    communityRepository.observeMessages(conversationId).catch { emit(emptyList()) }

  fun sendMessage(
    context: Context,
    conversation: Conversation,
    sender: PublicUser,
    text: String,
    mediaUri: String = "",
    mediaType: String = "none",
    fileName: String = "",
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
      communityRepository.sendMessage(conversation, sender, text, uploadedUrl, mediaType, fileName)
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Message failed" }
    }
  }

  fun setTyping(conversationId: String, isTyping: Boolean) {
    viewModelScope.launch { communityRepository.setTypingStatus(conversationId, isTyping) }
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
        .onSuccess { onLeft() }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not leave conversation" }
    }
  }

  fun markNotificationRead(notifId: String) {
    viewModelScope.launch { communityRepository.markNotificationRead(notifId) }
  }

  fun markAllNotificationsRead(list: List<AppNotification>) {
    viewModelScope.launch { communityRepository.markAllNotificationsRead(list) }
  }
}
