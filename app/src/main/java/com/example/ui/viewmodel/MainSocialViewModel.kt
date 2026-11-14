package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.GlobalSettings
import com.example.data.model.OwnerConfig
import com.example.data.model.Post
import com.example.data.model.PostReaction
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.example.data.model.StoryReaction
import com.example.data.model.UiState
import com.example.data.model.unwrapFirestoreMessage
import com.example.data.repository.AuthRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeedSortMode(val label: String) {
  LATEST("Latest"),
  TOP("Top"),
  FRIENDS("Friends")
}

class MainSocialViewModel(
  private val authRepository: AuthRepository,
  private val socialRepository: SocialRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _feedLimit = MutableStateFlow(20L)
  val feedLimit: StateFlow<Long> = _feedLimit.asStateFlow()

  private val _sortMode = MutableStateFlow(FeedSortMode.LATEST)
  val sortMode: StateFlow<FeedSortMode> = _sortMode.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  val currentUser: StateFlow<PublicUser?> = authRepository.observePublicUser(currentUserId)
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val privateUser: StateFlow<PrivateUser?> = authRepository.observePrivateUser(currentUserId)
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val publicFeed: StateFlow<UiState<List<Post>>> = _feedLimit
    .flatMapLatest { limit -> socialRepository.observePublicPosts(limit) }
    .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load feed"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val myPosts: StateFlow<List<Post>> = socialRepository.observeUserPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val friendsSharedPosts: StateFlow<List<Post>> = socialRepository.observeSharedFriendsPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val savedPosts: StateFlow<List<SavedPost>> = socialRepository.observeSavedPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val stories: StateFlow<UiState<List<Story>>> = socialRepository.observePublicStories()
    .map<List<Story>, UiState<List<Story>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load stories"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val reels: StateFlow<UiState<List<Reel>>> = socialRepository.observeReels()
    .map<List<Reel>, UiState<List<Reel>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load reels"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  fun setSortMode(mode: FeedSortMode) {
    _sortMode.value = mode
  }

  fun loadMorePosts() {
    _feedLimit.value = (_feedLimit.value + 20L).coerceAtMost(200L)
  }

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun bootstrapProfile() {
    viewModelScope.launch {
      authRepository.ensureProfileReady()
        .onFailure { err ->
          _actionMessage.value = err.unwrapFirestoreMessage("Could not initialize your profile in Firestore.")
        }
    }
  }

  private suspend fun resolveAuthorProfile(): PublicUser {
    currentUser.value?.let { return it }
    authRepository.ensureProfileReady()
    currentUser.value?.let { return it }
    val fbUser = authRepository.currentUser()
    return PublicUser(
      userId = currentUserId,
      displayName = fbUser?.displayName?.trim()?.takeIf { it.isNotBlank() }?.take(80) ?: "Hello Member",
      photoUrl = (fbUser?.photoUrl?.toString() ?: "").trim().take(1000)
    )
  }

  fun createPost(
    context: Context,
    content: String,
    mediaUri: String,
    mediaType: String,
    privacy: String,
    friendUids: List<String>,
    groupId: String = "",
    pageId: String = ""
  ) {
    if (content.isBlank() && mediaUri.isBlank()) {
      _actionMessage.value = "Write something or attach media first."
      return
    }
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      val uploadedUrl = if (mediaUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, mediaUri, "posts")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _actionMessage.value = err.localizedMessage ?: "Media upload failed"
          return@launch
        }
      } else {
        ""
      }
      val viewers = if (privacy == "friends") friendUids else emptyList()
      socialRepository.createPost(
        author = author,
        content = content,
        mediaUri = uploadedUrl,
        mediaType = mediaType,
        privacy = privacy,
        allowedViewerUids = viewers,
        groupId = groupId,
        pageId = pageId
      ).onSuccess {
        _actionMessage.value = "Post published!"
      }.onFailure {
        _actionMessage.value = it.unwrapFirestoreMessage("Failed to publish post")
      }
    }
  }

  fun editPost(postId: String, content: String, privacy: String) {
    if (content.isBlank()) return
    viewModelScope.launch {
      socialRepository.updatePost(postId, content, privacy)
        .onSuccess { _actionMessage.value = "Post updated" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Failed to update post") }
    }
  }

  fun deletePost(postId: String) {
    viewModelScope.launch {
      socialRepository.deletePost(postId)
        .onSuccess { _actionMessage.value = "Post deleted" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Failed to delete post") }
    }
  }

  fun reactToPost(post: Post, emoji: String) {
    viewModelScope.launch {
      val actorName = resolveAuthorProfile().displayName
      socialRepository.togglePostReaction(post, actorName, emoji)
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reaction failed") }
    }
  }

  fun observePostReactions(postId: String): Flow<List<PostReaction>> =
    socialRepository.observePostReactions(postId).catch { emit(emptyList()) }

  fun observePostComments(postId: String): Flow<List<Comment>> =
    socialRepository.observeComments(postId).catch { emit(emptyList()) }

  fun addComment(post: Post, text: String, parentCommentId: String = "") {
    if (text.isBlank()) return
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      socialRepository.addComment(post, author, text, parentCommentId)
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Failed to comment") }
    }
  }

  fun sharePost(post: Post, note: String) {
    viewModelScope.launch {
      val sharer = resolveAuthorProfile()
      socialRepository.sharePost(post, sharer, note)
        .onSuccess { _actionMessage.value = "Shared to your feed!" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Share failed") }
    }
  }

  fun toggleSave(itemId: String, itemType: String, authorName: String, excerpt: String) {
    viewModelScope.launch {
      socialRepository.toggleSaveItem(itemId, itemType, authorName, excerpt)
        .onSuccess { saved ->
          _actionMessage.value = if (saved) "Saved to bookmarks" else "Removed from bookmarks"
        }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Could not save item" }
    }
  }

  fun reportContent(targetType: String, targetId: String, reason: String) {
    if (reason.isBlank()) return
    viewModelScope.launch {
      socialRepository.reportContent(targetType, targetId, reason)
        .onSuccess { _actionMessage.value = "Report submitted for review" }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Report failed" }
    }
  }

  fun createStory(context: Context, caption: String, mediaUri: String, mediaType: String, backgroundHex: String) {
    if (caption.isBlank() && mediaUri.isBlank()) {
      _actionMessage.value = "Add text or media for your story."
      return
    }
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      val uploadedUrl = if (mediaUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, mediaUri, "stories")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _actionMessage.value = err.localizedMessage ?: "Story media upload failed"
          return@launch
        }
      } else {
        ""
      }
      socialRepository.createStory(author, caption, uploadedUrl, mediaType, backgroundHex)
        .onSuccess { _actionMessage.value = "Story shared!" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Story creation failed") }
    }
  }

  fun markStoryViewed(storyId: String) {
    viewModelScope.launch { socialRepository.incrementStoryView(storyId) }
  }

  fun observeStoryReactions(storyId: String): Flow<List<StoryReaction>> =
    socialRepository.observeStoryReactions(storyId).catch { emit(emptyList()) }

  fun reactToStory(story: Story, emoji: String, replyText: String = "") {
    viewModelScope.launch {
      val actor = resolveAuthorProfile()
      socialRepository.reactToStory(story, actor, emoji, replyText)
        .onSuccess {
          if (replyText.isNotBlank()) _actionMessage.value = "Reply sent to ${story.authorName}"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reaction failed") }
    }
  }

  fun createReel(context: Context, caption: String, videoUri: String, audioTitle: String, coverHex: String) {
    if (caption.isBlank()) {
      _actionMessage.value = "Add a caption for your reel."
      return
    }
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      val uploadedUrl = if (videoUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, videoUri, "reels")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _actionMessage.value = err.localizedMessage ?: "Reel video upload failed"
          return@launch
        }
      } else {
        ""
      }
      socialRepository.createReel(author, caption, uploadedUrl, audioTitle, coverHex)
        .onSuccess { _actionMessage.value = "Reel posted!" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reel creation failed") }
    }
  }

  fun likeReel(reelId: String) {
    viewModelScope.launch { socialRepository.likeReel(reelId) }
  }

  fun shareReel(reelId: String) {
    viewModelScope.launch {
      socialRepository.shareReel(reelId)
        .onSuccess { _actionMessage.value = "Reel shared!" }
    }
  }

  fun observeReelComments(reelId: String): Flow<List<Comment>> =
    socialRepository.observeReelComments(reelId).catch { emit(emptyList()) }

  fun addReelComment(reelId: String, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      socialRepository.addReelComment(reelId, author, text)
    }
  }

  fun updateProfile(
    context: Context?,
    displayName: String,
    bio: String,
    photoUrl: String,
    coverUrl: String,
    profilePrivacy: String,
    acceptSecretLove: Boolean,
    isOnline: Boolean
  ) {
    if (displayName.isBlank()) {
      _actionMessage.value = "Display name cannot be empty."
      return
    }
    viewModelScope.launch {
      var finalPhoto = photoUrl
      var finalCover = coverUrl
      if (context != null) {
        if (photoUrl.isNotBlank() && !photoUrl.startsWith("http") && !photoUrl.startsWith("firestore://")) {
          _isUploadingMedia.value = true
          val photoResult = socialRepository.uploadMedia(context, photoUrl, "profiles")
          _isUploadingMedia.value = false
          finalPhoto = photoResult.getOrElse { err ->
            _actionMessage.value = err.localizedMessage ?: "Avatar upload failed"
            return@launch
          }
        }
        if (coverUrl.isNotBlank() && !coverUrl.startsWith("http") && !coverUrl.startsWith("firestore://")) {
          _isUploadingMedia.value = true
          val coverResult = socialRepository.uploadMedia(context, coverUrl, "profiles")
          _isUploadingMedia.value = false
          finalCover = coverResult.getOrElse { err ->
            _actionMessage.value = err.localizedMessage ?: "Cover upload failed"
            return@launch
          }
        }
      }
      authRepository.updatePublicProfile(
        displayName = displayName,
        bio = bio,
        photoUrl = finalPhoto,
        coverUrl = finalCover,
        profilePrivacy = profilePrivacy,
        acceptSecretLove = acceptSecretLove,
        isOnline = isOnline
      ).onSuccess { _actionMessage.value = "Profile saved" }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update profile" }
    }
  }

  fun updateAccountSettings(
    notifLikes: Boolean,
    notifComments: Boolean,
    notifFriends: Boolean,
    notifMessages: Boolean,
    notifSecretLove: Boolean,
    notifSos: Boolean,
    phone: String,
    isDeactivated: Boolean
  ) {
    viewModelScope.launch {
      authRepository.updateNotificationPrefs(
        notifLikes, notifComments, notifFriends, notifMessages, notifSecretLove, notifSos, phone, isDeactivated
      ).onSuccess { _actionMessage.value = "Settings updated" }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save settings" }
    }
  }

  fun signOut(context: android.content.Context) {
    viewModelScope.launch {
      authRepository.signOut(context)
    }
  }

  fun deleteOwnAccount(onDeleted: () -> Unit) {
    viewModelScope.launch {
      authRepository.deleteOwnAccountData()
        .onSuccess { onDeleted() }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Account deletion may require recent sign-in" }
    }
  }
}
