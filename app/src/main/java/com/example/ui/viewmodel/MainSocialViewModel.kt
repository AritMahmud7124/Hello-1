package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.CuratedFeedItem
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
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
import com.example.data.model.YouTubeShortVideo
import com.example.data.model.unwrapFirestoreMessage
import com.example.data.network.YouTubeFetchResult
import com.example.data.network.YouTubeShortsClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.FeedCurationRepository
import com.example.data.repository.FeedCurator
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

  private val curationRepository: FeedCurationRepository = socialRepository.createFeedCurationRepository()

  private val _feedLimit = MutableStateFlow(20L)
  val feedLimit: StateFlow<Long> = _feedLimit.asStateFlow()

  private val _sortMode = MutableStateFlow(FeedSortMode.LATEST)
  val sortMode: StateFlow<FeedSortMode> = _sortMode.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  private val _optimisticPrefs = MutableStateFlow<FeedPreferences?>(null)
  private val _selectedReelCategory = MutableStateFlow<String?>(null)
  val selectedReelCategory: StateFlow<String?> = _selectedReelCategory.asStateFlow()

  private val _youtubeShorts = MutableStateFlow<List<YouTubeShortVideo>>(emptyList())
  private val _youtubeStatus = MutableStateFlow<YouTubeFetchResult?>(null)
  val youtubeStatus: StateFlow<YouTubeFetchResult?> = _youtubeStatus.asStateFlow()

  private val _isFetchingShorts = MutableStateFlow(false)
  val isFetchingShorts: StateFlow<Boolean> = _isFetchingShorts.asStateFlow()

  private val _localLikedItemIds = MutableStateFlow<Set<String>>(emptySet())
  val localLikedItemIds: StateFlow<Set<String>> = _localLikedItemIds.asStateFlow()

  val currentUser: StateFlow<PublicUser?> = authRepository.observePublicUser(currentUserId)
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val privateUser: StateFlow<PrivateUser?> = authRepository.observePrivateUser(currentUserId)
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val feedPreferences: StateFlow<FeedPreferences> = combine(
    curationRepository.observePreferences(currentUserId).catch { emit(FeedPreferences(userId = currentUserId)) },
    _optimisticPrefs
  ) { remote, optimistic ->
    optimistic ?: remote
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedPreferences(userId = currentUserId))

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

  private val rawStories: StateFlow<UiState<List<Story>>> = combine(
    socialRepository.observePublicStories(),
    socialRepository.observeUserStories(currentUserId).catch { emit(emptyList()) },
    socialRepository.observeFriendsStories(currentUserId).catch { emit(emptyList()) }
  ) { publicStories, myStories, friendStories ->
    val merged = (myStories + friendStories + publicStories)
      .distinctBy { it.id }
      .sortedByDescending { it.createdAt?.seconds ?: 0L }
    UiState.Success(merged) as UiState<List<Story>>
  }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load stories"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val stories: StateFlow<UiState<List<Story>>> = combine(rawStories, feedPreferences) { state, prefs ->
    when (state) {
      is UiState.Success -> UiState.Success(FeedCurator.rankCuratedStories(state.data, prefs, currentUserId))
      else -> state
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val reels: StateFlow<UiState<List<Reel>>> = socialRepository.observeReels()
    .map<List<Reel>, UiState<List<Reel>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load reels"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val curatedReels: StateFlow<UiState<List<CuratedFeedItem>>> = combine(
    reels,
    _youtubeShorts,
    feedPreferences,
    _selectedReelCategory
  ) { nativeState, ytList, prefs, selectedCat ->
    when (nativeState) {
      is UiState.Loading -> if (ytList.isNotEmpty()) {
        UiState.Success(FeedCurator.rankCuratedReels(emptyList(), ytList, prefs, selectedCat))
      } else {
        UiState.Loading
      }
      is UiState.Error -> if (ytList.isNotEmpty()) {
        UiState.Success(FeedCurator.rankCuratedReels(emptyList(), ytList, prefs, selectedCat))
      } else {
        nativeState
      }
      is UiState.Success -> {
        UiState.Success(FeedCurator.rankCuratedReels(nativeState.data, ytList, prefs, selectedCat))
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private var lastFetchedCategoriesKey: String = ""

  fun ensureYouTubeShortsLoaded(forceRefresh: Boolean = false) {
    val selected = _selectedReelCategory.value
    val targetCategories = if (!selected.isNullOrBlank()) {
      listOf(selected)
    } else {
      FeedCurator.topActiveCategories(feedPreferences.value, limit = 3)
    }
    val key = targetCategories.joinToString(",")
    if (!forceRefresh && key == lastFetchedCategoriesKey && _youtubeStatus.value != null) {
      return
    }
    if (_isFetchingShorts.value) return
    lastFetchedCategoriesKey = key
    viewModelScope.launch {
      _isFetchingShorts.value = true
      when (val res = YouTubeShortsClient.fetchShortsForCategories(targetCategories)) {
        is YouTubeFetchResult.Success -> {
          _youtubeShorts.value = res.items
          _youtubeStatus.value = res
        }
        is YouTubeFetchResult.MissingApiKey -> {
          _youtubeStatus.value = res
        }
        is YouTubeFetchResult.Error -> {
          _youtubeStatus.value = res
        }
      }
      _isFetchingShorts.value = false
    }
  }

  fun selectReelCategory(category: String?) {
    val next = category?.takeIf { it.isNotBlank() && !it.equals("For You", ignoreCase = true) }
    _selectedReelCategory.value = next
    ensureYouTubeShortsLoaded(forceRefresh = true)
  }

  fun trackEngagement(itemKey: String, category: String, signal: EngagementSignal) {
    val cleanCat = FeedCurator.normalizeCategory(category)
    val current = feedPreferences.value
    if (signal == EngagementSignal.LIKE && itemKey.isNotBlank()) {
      _localLikedItemIds.value = _localLikedItemIds.value + itemKey
    }
    val updated = FeedCurator.applyEngagement(
      current = current,
      userId = currentUserId,
      category = cleanCat,
      signal = signal,
      videoId = if (signal == EngagementSignal.NOT_INTERESTED) itemKey else ""
    )
    _optimisticPrefs.value = updated
    viewModelScope.launch {
      curationRepository.savePreferences(updated).onSuccess {
        _optimisticPrefs.value = null
      }
    }
  }

  fun markNotInterested(itemKey: String, youtubeVideoId: String, category: String) {
    val cleanCat = FeedCurator.normalizeCategory(category)
    val prevDisliked = feedPreferences.value.dislikedCategories.toSet()
    var updated = FeedCurator.applyEngagement(
      current = feedPreferences.value,
      userId = currentUserId,
      category = cleanCat,
      signal = EngagementSignal.NOT_INTERESTED,
      videoId = itemKey
    )
    if (youtubeVideoId.isNotBlank()) {
      updated = updated.copy(
        dismissedVideoIds = (listOf(youtubeVideoId) + updated.dismissedVideoIds).distinct().take(200)
      )
    }
    _optimisticPrefs.value = updated
    viewModelScope.launch {
      curationRepository.savePreferences(updated).onSuccess {
        _optimisticPrefs.value = null
      }
    }
    if (updated.dislikedCategories.toSet() != prevDisliked) {
      _actionMessage.value = "Filtered out & muted $cleanCat category based on your feedback"
      ensureYouTubeShortsLoaded(forceRefresh = true)
    } else {
      _actionMessage.value = "Video removed · adjusting $cleanCat score"
    }
  }

  fun restoreCategoryPreference(category: String) {
    val updated = FeedCurator.restoreCategory(feedPreferences.value, currentUserId, category)
    _optimisticPrefs.value = updated
    viewModelScope.launch {
      curationRepository.savePreferences(updated).onSuccess {
        _optimisticPrefs.value = null
        ensureYouTubeShortsLoaded(forceRefresh = true)
      }
    }
    _actionMessage.value = "$category restored to your personalized feed"
  }

  fun resetFeedPreferences() {
    viewModelScope.launch {
      curationRepository.resetPreferences()
        .onSuccess { fresh ->
          _optimisticPrefs.value = fresh
          _actionMessage.value = "Feed recommendations reset to default interests"
          ensureYouTubeShortsLoaded(forceRefresh = true)
        }
        .onFailure {
          _actionMessage.value = it.unwrapFirestoreMessage("Could not reset preferences")
        }
    }
  }

  fun observePublicUserProfile(userId: String): Flow<UiState<PublicUser?>> =
    authRepository.observePublicUser(userId)
      .map<PublicUser?, UiState<PublicUser?>> { UiState.Success(it) }
      .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Profile is private or unavailable"))) }

  fun observeProfilePosts(targetUserId: String): Flow<List<Post>> {
    if (targetUserId == currentUserId) return myPosts
    return combine(
      socialRepository.observeUserPublicPosts(targetUserId).catch { emit(emptyList()) },
      friendsSharedPosts.map { list -> list.filter { it.authorId == targetUserId } }
    ) { publicList, friendList ->
      (publicList + friendList)
        .distinctBy { it.id }
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
  }

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

  fun createStory(
    context: Context,
    caption: String,
    mediaUri: String,
    mediaType: String,
    backgroundHex: String,
    privacy: String = "public",
    friendUids: List<String> = emptyList(),
    category: String = "Young",
    youtubeVideoId: String = ""
  ) {
    val cleanYtId = YouTubeShortsClient.extractVideoId(youtubeVideoId)
    if (caption.isBlank() && mediaUri.isBlank() && cleanYtId.isBlank()) {
      _actionMessage.value = "Add text, media, or a YouTube Short link for your story."
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
      val viewers = if (privacy == "friends") friendUids else emptyList()
      socialRepository.createStory(
        author = author,
        caption = caption,
        mediaUri = uploadedUrl,
        mediaType = mediaType,
        backgroundHex = backgroundHex,
        privacy = privacy,
        allowedViewerUids = viewers,
        category = category,
        youtubeVideoId = cleanYtId
      )
        .onSuccess { _actionMessage.value = "Story shared!" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Story creation failed") }
    }
  }

  fun deleteStory(storyId: String) {
    if (storyId.isBlank()) return
    viewModelScope.launch {
      socialRepository.deleteStory(storyId)
        .onSuccess { _actionMessage.value = "Story deleted" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Failed to delete story") }
    }
  }

  fun markStoryViewed(storyId: String, category: String = "Young") {
    viewModelScope.launch { socialRepository.incrementStoryView(storyId) }
    trackEngagement(storyId, category, EngagementSignal.VIEW)
  }

  fun observeStoryReactions(storyId: String): Flow<List<StoryReaction>> =
    socialRepository.observeStoryReactions(storyId).catch { emit(emptyList()) }

  fun reactToStory(story: Story, emoji: String, replyText: String = "") {
    viewModelScope.launch {
      val actor = resolveAuthorProfile()
      socialRepository.reactToStory(story, actor, emoji, replyText)
        .onSuccess {
          trackEngagement(story.id, story.category, EngagementSignal.LIKE)
          if (replyText.isNotBlank()) _actionMessage.value = "Reply sent to ${story.authorName}"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reaction failed") }
    }
  }

  fun createReel(
    context: Context,
    caption: String,
    videoUri: String,
    audioTitle: String,
    coverHex: String,
    category: String = "Young",
    youtubeUrlOrId: String = ""
  ) {
    val cleanYtId = YouTubeShortsClient.extractVideoId(youtubeUrlOrId)
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
      socialRepository.createReel(
        author = author,
        caption = caption,
        videoUri = uploadedUrl,
        audioTitle = audioTitle,
        coverHex = coverHex,
        category = category,
        youtubeVideoId = cleanYtId
      )
        .onSuccess { _actionMessage.value = "Reel posted!" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reel creation failed") }
    }
  }

  fun likeReel(reelId: String, category: String = "Young") {
    viewModelScope.launch { socialRepository.likeReel(reelId) }
    trackEngagement("native_$reelId", category, EngagementSignal.LIKE)
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
