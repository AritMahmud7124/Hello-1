package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.CuratedFeedItem
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
import com.example.data.model.GlobalSettings
import com.example.data.model.MusicAttachment
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
import java.util.Locale

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

  private val _feedMode = MutableStateFlow("general")
  val feedMode: StateFlow<String> = _feedMode.asStateFlow()

  private val _selectedRegion = MutableStateFlow("GLOBAL")
  val selectedRegion: StateFlow<String> = _selectedRegion.asStateFlow()

  private val _reelsAutoAdvance = MutableStateFlow(true)
  val reelsAutoAdvance: StateFlow<Boolean> = _reelsAutoAdvance.asStateFlow()

  private var regionSwitchCount: Long = 0L
  private val autoDeletedStoryIds = mutableSetOf<String>()

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

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val publicFeed: StateFlow<UiState<List<Post>>> = _feedLimit
    .flatMapLatest { limit -> socialRepository.observePublicPosts(limit) }
    .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load feed"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val myPosts: StateFlow<List<Post>> = socialRepository.observeUserPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myProfilePosts: StateFlow<List<Post>> = myPosts
    .map { list -> list.filter { it.groupId.isBlank() && it.pageId.isBlank() } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val friendsSharedPosts: StateFlow<List<Post>> = socialRepository.observeSharedFriendsPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val savedPosts: StateFlow<List<SavedPost>> = socialRepository.observeSavedPosts(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val followingIds: StateFlow<Set<String>> = socialRepository.observeFollowingIds(currentUserId)
    .catch { emit(emptySet()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

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

  val savedHighlightStories: StateFlow<List<Story>> = rawStories
    .map { state ->
      (state as? UiState.Success)?.data
        ?.filter { it.isSavedHighlight }
        ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
        .orEmpty()
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val reels: StateFlow<UiState<List<Reel>>> = socialRepository.observeReels()
    .map<List<Reel>, UiState<List<Reel>>> { list -> UiState.Success(list) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load reels"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val curatedReels: StateFlow<UiState<List<CuratedFeedItem>>> = combine(
    reels,
    _youtubeShorts,
    feedPreferences,
    _selectedReelCategory,
    _selectedRegion
  ) { nativeState, ytList, prefs, selectedCat, region ->
    when (nativeState) {
      is UiState.Loading -> if (ytList.isNotEmpty()) {
        UiState.Success(FeedCurator.rankCuratedReels(emptyList(), ytList, prefs, selectedCat, region))
      } else {
        UiState.Loading
      }
      is UiState.Error -> if (ytList.isNotEmpty()) {
        UiState.Success(FeedCurator.rankCuratedReels(emptyList(), ytList, prefs, selectedCat, region))
      } else {
        nativeState
      }
      is UiState.Success -> {
        UiState.Success(FeedCurator.rankCuratedReels(nativeState.data, ytList, prefs, selectedCat, region))
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  init {
    viewModelScope.launch {
      rawStories.collect { state ->
        val list = (state as? UiState.Success)?.data ?: return@collect
        list.filter { it.authorId == currentUserId && it.id.isNotBlank() && FeedCurator.isStoryExpired(it) }
          .forEach { expired ->
            if (autoDeletedStoryIds.add(expired.id)) {
              socialRepository.deleteStory(expired.id)
            }
          }
      }
    }
  }

  private var lastFetchedCategoriesKey: String = ""

  fun originCountryCode(): String =
    Locale.getDefault().country.trim().uppercase().takeIf { it.length == 2 } ?: "US"

  fun ensureYouTubeShortsLoaded(forceRefresh: Boolean = false) {
    val selected = _selectedReelCategory.value
    val targetCategories = if (!selected.isNullOrBlank()) {
      listOf(selected)
    } else {
      FeedCurator.topActiveCategories(feedPreferences.value, limit = 3)
    }
    val region = _selectedRegion.value
    val key = "${targetCategories.joinToString(",")}|$region"
    if (!forceRefresh && key == lastFetchedCategoriesKey && _youtubeStatus.value != null) {
      return
    }
    if (_isFetchingShorts.value) return
    lastFetchedCategoriesKey = key
    viewModelScope.launch {
      _isFetchingShorts.value = true
      when (val res = YouTubeShortsClient.fetchShortsForCategories(targetCategories, regionCode = region)) {
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
    syncRegionalAnalytics()
    ensureYouTubeShortsLoaded(forceRefresh = true)
  }

  fun setReelsAutoAdvance(enabled: Boolean) {
    _reelsAutoAdvance.value = enabled
  }

  fun restoreReelsPlaybackSettings(autoAdvance: Boolean, regionCode: String) {
    _reelsAutoAdvance.value = autoAdvance
    val cleanRegion = regionCode.trim().uppercase().ifBlank { "GLOBAL" }
    _selectedRegion.value = cleanRegion
    _feedMode.value = if (cleanRegion == "GLOBAL") "general" else "country"
  }

  fun setFeedRegion(mode: String, regionCode: String) {
    val cleanRegion = if (mode == "general") "GLOBAL" else regionCode.trim().uppercase().ifBlank { "US" }
    val cleanMode = if (cleanRegion == "GLOBAL") "general" else "country"
    _feedMode.value = cleanMode
    _selectedRegion.value = cleanRegion
    regionSwitchCount += 1L
    syncRegionalAnalytics()
    ensureYouTubeShortsLoaded(forceRefresh = true)
  }

  fun syncRegionalAnalytics() {
    if (currentUserId.isBlank()) return
    val mode = _feedMode.value
    val region = _selectedRegion.value
    val activeCat = _selectedReelCategory.value ?: "For You"
    val watchMode = if (mode == "general") {
      if (activeCat == "For You") "Global Mix" else "Global · $activeCat"
    } else {
      if (activeCat == "For You") "Country Focus" else "$region · $activeCat"
    }
    viewModelScope.launch {
      socialRepository.logRegionalAnalytics(
        feedMode = mode,
        selectedRegion = region,
        originRegion = originCountryCode(),
        watchMode = watchMode,
        activeCategory = activeCat,
        switchCount = regionSwitchCount
      )
    }
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
    if (targetUserId == currentUserId) return myProfilePosts
    return combine(
      socialRepository.observeUserPublicPosts(targetUserId).catch { emit(emptyList()) },
      friendsSharedPosts.map { list -> list.filter { it.authorId == targetUserId } }
    ) { publicList, friendList ->
      (publicList + friendList)
        .distinctBy { it.id }
        .filter { it.groupId.isBlank() && it.pageId.isBlank() }
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
  }

  fun observeUserStories(targetUserId: String): Flow<List<Story>> =
    socialRepository.observeUserStories(targetUserId)
      .map { list -> list.filter { !FeedCurator.isStoryExpired(it) } }
      .catch { emit(emptyList()) }

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
        .onSuccess { authRepository.setPresenceOnline(true) }
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
    pageId: String = "",
    music: MusicAttachment? = null
  ) {
    if (content.isBlank() && mediaUri.isBlank() && music == null) {
      _actionMessage.value = "Write something, attach media, or add a song first."
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
      val uploadedMusicUrl = if (music != null && music.audioUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val musicResult = socialRepository.uploadMedia(context, music.audioUri, "music")
        _isUploadingMedia.value = false
        musicResult.getOrElse { err ->
          _actionMessage.value = err.localizedMessage ?: "Song upload failed"
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
        pageId = pageId,
        musicTitle = music?.title.orEmpty(),
        musicArtist = music?.artist.orEmpty(),
        musicUri = uploadedMusicUrl,
        musicStartSec = music?.startSec ?: 0L,
        musicEndSec = music?.endSec ?: 0L
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
    youtubeVideoId: String = "",
    isSavedHighlight: Boolean = false,
    autoDelete24h: Boolean = true,
    music: MusicAttachment? = null
  ) {
    val cleanYtId = YouTubeShortsClient.extractVideoId(youtubeVideoId)
    if (caption.isBlank() && mediaUri.isBlank() && cleanYtId.isBlank() && music == null) {
      _actionMessage.value = "Add text, media, song, or a YouTube Short link for your story."
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
      val uploadedMusicUrl = if (music != null && music.audioUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val musicResult = socialRepository.uploadMedia(context, music.audioUri, "music")
        _isUploadingMedia.value = false
        musicResult.getOrElse { err ->
          _actionMessage.value = err.localizedMessage ?: "Story song upload failed"
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
        youtubeVideoId = cleanYtId,
        isSavedHighlight = isSavedHighlight,
        autoDelete24h = autoDelete24h,
        musicTitle = music?.title.orEmpty(),
        musicArtist = music?.artist.orEmpty(),
        musicUri = uploadedMusicUrl,
        musicStartSec = music?.startSec ?: 0L,
        musicEndSec = music?.endSec ?: 0L
      )
        .onSuccess {
          _actionMessage.value = if (isSavedHighlight) "Story live on Home (24h) & saved to Profile Highlights ★" else "Story shared (auto-removes in 24h)!"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Story creation failed") }
    }
  }

  fun toggleStoryHighlight(story: Story) {
    if (story.id.isBlank() || story.authorId != currentUserId) return
    val nextSaved = !story.isSavedHighlight
    viewModelScope.launch {
      socialRepository.setStoryHighlight(story.id, nextSaved)
        .onSuccess {
          _actionMessage.value = if (nextSaved) "Saved to Profile Highlights ★ (Home removes after 24h)" else "Removed from Profile Highlights (24h auto-expire active)"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Could not update highlight") }
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
    youtubeUrlOrId: String = "",
    regionCode: String = _selectedRegion.value,
    @Suppress("UNUSED_PARAMETER") autoDelete24h: Boolean = false
  ) {
    val cleanYtId = YouTubeShortsClient.extractVideoId(youtubeUrlOrId)
    val cleanVideoUri = videoUri.trim()
    val cleanCaption = caption.trim()
    if (cleanCaption.isBlank() && cleanVideoUri.isBlank() && cleanYtId.isBlank()) {
      _actionMessage.value = "Add a caption, video, or YouTube link for your reel."
      return
    }
    val effectiveCaption = cleanCaption.ifBlank {
      if (cleanYtId.isNotBlank()) "Shared Reel" else "New Reel"
    }
    viewModelScope.launch {
      val author = resolveAuthorProfile()
      val uploadedUrl = if (cleanVideoUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, cleanVideoUri, "reels")
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
        caption = effectiveCaption,
        videoUri = uploadedUrl,
        audioTitle = audioTitle,
        coverHex = coverHex,
        category = category,
        regionCode = regionCode,
        autoDelete24h = false,
        youtubeVideoId = cleanYtId
      )
        .onSuccess {
          _actionMessage.value = "Reel posted!"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Reel creation failed") }
    }
  }

  fun deleteReel(reelId: String) {
    if (reelId.isBlank()) return
    viewModelScope.launch {
      socialRepository.deleteReel(reelId)
        .onSuccess { _actionMessage.value = "Reel deleted" }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Failed to delete reel") }
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

  fun toggleFollowCreator(targetId: String, targetName: String) {
    if (targetId.isBlank() || targetId == currentUserId) return
    viewModelScope.launch {
      socialRepository.toggleFollowCreator(targetId, targetName)
        .onSuccess { followed ->
          _actionMessage.value = if (followed) "Following $targetName" else "Unfollowed $targetName"
        }
        .onFailure { err ->
          _actionMessage.value = err.unwrapFirestoreMessage("Follow update failed")
        }
    }
  }

  fun setAppForegroundOnline(isForeground: Boolean) {
    viewModelScope.launch {
      authRepository.setPresenceOnline(isForeground)
    }
  }

  fun toggleShowOnlineStatus(enabled: Boolean) {
    viewModelScope.launch {
      authRepository.setShowOnlineStatus(enabled)
        .onSuccess {
          _actionMessage.value = if (enabled) "Active Status On · Friends see your green dot" else "Active Status Off · You appear offline"
        }
        .onFailure { _actionMessage.value = it.unwrapFirestoreMessage("Could not update online status") }
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
    showOnlineStatus: Boolean
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
        showOnlineStatus = showOnlineStatus
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
    email: String,
    phone: String,
    isDeactivated: Boolean,
    onResult: (Result<Unit>) -> Unit = {}
  ) {
    viewModelScope.launch {
      val result = authRepository.updateNotificationPrefs(
        notifLikes, notifComments, notifFriends, notifMessages, notifSecretLove, notifSos, email, phone, isDeactivated
      )
      result
        .onSuccess { _actionMessage.value = "Account & recovery contact saved" }
        .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save settings" }
      onResult(result)
    }
  }

  fun syncBiometricSetting(
    vault: com.example.security.EncryptedVaultStorage,
    enabled: Boolean,
    integrityReport: com.example.security.DeviceIntegrityReport
  ) {
    vault.isBiometricLockEnabled = enabled
    viewModelScope.launch {
      authRepository.syncSecurityState(
        failedAttempts = vault.failedAuthAttempts,
        lockoutUntilMs = vault.lockoutUntilEpochMs,
        lastAttemptMs = vault.lastAttemptEpochMs,
        biometricEnabled = enabled,
        integrityReport = integrityReport
      )
      authRepository.logSecurityEvent(
        eventType = if (enabled) "biometric_enabled" else "biometric_disabled",
        detail = if (enabled) "Biometric/Passkey 2nd step enabled" else "Biometric/Passkey 2nd step disabled",
        integritySummary = integrityReport.summary
      )
      _actionMessage.value = if (enabled) {
        "Biometric & Passkey 2-Step Unlock enabled"
      } else {
        "Biometric & Passkey 2-Step Unlock disabled"
      }
    }
  }

  fun reauthenticateSession(
    activity: android.app.Activity,
    vault: com.example.security.EncryptedVaultStorage,
    integrityReport: com.example.security.DeviceIntegrityReport,
    onVerified: () -> Unit = {}
  ) {
    viewModelScope.launch {
      when (val res = authRepository.reauthenticateWithGoogle(activity, vault)) {
        is com.example.data.repository.AuthSignInResult.Success -> {
          authRepository.logSecurityEvent(
            eventType = "reauth_verified",
            detail = "Mandatory re-authentication completed",
            integritySummary = integrityReport.summary
          )
          _actionMessage.value = "Session freshly verified via Google"
          onVerified()
        }
        is com.example.data.repository.AuthSignInResult.Cancelled -> {
          _actionMessage.value = "Re-authentication cancelled"
        }
        is com.example.data.repository.AuthSignInResult.Error -> {
          _actionMessage.value = res.message
        }
      }
    }
  }

  fun signOut(context: Context, vault: com.example.security.EncryptedVaultStorage? = null) {
    viewModelScope.launch {
      authRepository.signOut(context, vault)
    }
  }

  fun deleteOwnAccount(
    vault: com.example.security.EncryptedVaultStorage? = null,
    onDeleted: () -> Unit
  ) {
    viewModelScope.launch {
      authRepository.deleteOwnAccountData()
        .onSuccess {
          vault?.clearSessionBinding()
          onDeleted()
        }
        .onFailure {
          _actionMessage.value = it.unwrapFirestoreMessage("Account deletion requires recent authentication (<15m)")
        }
    }
  }
}
