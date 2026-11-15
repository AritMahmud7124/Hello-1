package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CuratedFeedItem
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
import com.example.data.model.OperationType
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.YouTubeShortVideo
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Privacy-safe preference & curation engine.
 * Category weights & dismissed video IDs live strictly inside `/users/{uid}/feed_preferences/curation`.
 */
object FeedCurator {

  fun normalizeCategory(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "Young"
    return FeedPreferences.CATEGORIES.firstOrNull { it.equals(trimmed, ignoreCase = true) }
      ?: trimmed.take(32)
  }

  fun applyEngagement(
    current: FeedPreferences,
    userId: String,
    category: String,
    signal: EngagementSignal,
    videoId: String = ""
  ): FeedPreferences {
    val cleanCat = normalizeCategory(category)
    val scores = current.categoryScores
      .ifEmpty { FeedPreferences.DEFAULT_SCORES }
      .toMutableMap()
    val prev = scores[cleanCat] ?: 10L
    val nextScore = (prev + signal.weightDelta).coerceIn(-12L, 250L)
    scores[cleanCat] = nextScore

    val disliked = current.dislikedCategories.toMutableList()
    if (signal == EngagementSignal.NOT_INTERESTED) {
      // Automatically demote/remove disliked categories if weight reaches <= 0
      if (nextScore <= 0L && cleanCat !in disliked) {
        disliked.add(cleanCat)
      }
    } else if (nextScore > 0L) {
      disliked.removeAll { it.equals(cleanCat, ignoreCase = true) }
    }

    val dismissed = current.dismissedVideoIds.toMutableList()
    if (signal == EngagementSignal.NOT_INTERESTED && videoId.isNotBlank()) {
      dismissed.remove(videoId)
      dismissed.add(0, videoId)
    }

    return current.copy(
      userId = userId,
      categoryScores = scores.entries.take(20).associate { it.key to it.value },
      dislikedCategories = disliked.distinct().take(20),
      dismissedVideoIds = dismissed.distinct().take(200),
      totalViews = current.totalViews + if (signal == EngagementSignal.VIEW) 1L else 0L,
      totalLikes = current.totalLikes + if (signal == EngagementSignal.LIKE) 1L else 0L,
      totalFullWatches = current.totalFullWatches + if (signal == EngagementSignal.FULL_WATCH) 1L else 0L
    )
  }

  fun restoreCategory(
    current: FeedPreferences,
    userId: String,
    category: String
  ): FeedPreferences {
    val cleanCat = normalizeCategory(category)
    val scores = current.categoryScores.ifEmpty { FeedPreferences.DEFAULT_SCORES }.toMutableMap()
    scores[cleanCat] = 10L
    return current.copy(
      userId = userId,
      categoryScores = scores,
      dislikedCategories = current.dislikedCategories.filterNot { it.equals(cleanCat, ignoreCase = true) }
    )
  }

  fun topActiveCategories(prefs: FeedPreferences, limit: Int = 3): List<String> {
    val scores = prefs.categoryScores.ifEmpty { FeedPreferences.DEFAULT_SCORES }
    val dislikedSet = prefs.dislikedCategories.map { it.lowercase() }.toSet()
    val active = FeedPreferences.CATEGORIES
      .filter { it.lowercase() !in dislikedSet }
      .sortedByDescending { scores[it] ?: 6L }
      .take(limit)
    return active.ifEmpty { listOf("Young", "Tech", "Funny") }
  }

  fun rankCuratedReels(
    nativeReels: List<Reel>,
    youtubeShorts: List<YouTubeShortVideo>,
    prefs: FeedPreferences,
    selectedCategory: String? = null
  ): List<CuratedFeedItem> {
    val scores = prefs.categoryScores.ifEmpty { FeedPreferences.DEFAULT_SCORES }
    val dismissedSet = prefs.dismissedVideoIds.toSet()
    val dislikedSet = prefs.dislikedCategories.map { it.lowercase() }.toSet()
    val filterCat = selectedCategory?.takeIf { it.isNotBlank() && !it.equals("For You", ignoreCase = true) }

    val nativeItems = nativeReels.mapNotNull { reel ->
      val cat = normalizeCategory(reel.category)
      val ytId = reel.youtubeVideoId.trim()
      if (reel.id in dismissedSet || (ytId.isNotBlank() && ytId in dismissedSet)) return@mapNotNull null
      if (filterCat == null && cat.lowercase() in dislikedSet) return@mapNotNull null
      if (filterCat != null && !cat.equals(filterCat, ignoreCase = true)) return@mapNotNull null

      val prefWeight = (scores[cat] ?: 10L).toDouble()
      val engageBoost = (reel.likesCount * 0.4 + reel.commentsCount * 0.6 + reel.sharesCount * 0.5).coerceAtMost(25.0)
      val recencyBoost = ((reel.createdAt?.seconds ?: 0L) / 3600.0).coerceAtLeast(0.0) * 0.001
      CuratedFeedItem(
        uniqueId = "native_${reel.id}",
        source = "native",
        category = cat,
        score = prefWeight * 1.5 + engageBoost + recencyBoost,
        authorId = reel.authorId,
        authorName = reel.authorName,
        authorAvatar = reel.authorAvatar,
        caption = reel.caption,
        audioTitle = reel.audioTitle,
        videoUri = reel.videoUri,
        youtubeVideoId = ytId,
        coverHex = reel.coverHex,
        likesCount = reel.likesCount,
        commentsCount = reel.commentsCount,
        sharesCount = reel.sharesCount,
        nativeReel = reel
      )
    }

    val ytItems = youtubeShorts.mapNotNull { yt ->
      val cat = normalizeCategory(yt.category)
      val ytUid = "yt_${yt.videoId}"
      if (yt.videoId in dismissedSet || ytUid in dismissedSet) return@mapNotNull null
      if (filterCat == null && cat.lowercase() in dislikedSet) return@mapNotNull null
      if (filterCat != null && !cat.equals(filterCat, ignoreCase = true)) return@mapNotNull null

      val prefWeight = (scores[cat] ?: 10L).toDouble()
      CuratedFeedItem(
        uniqueId = ytUid,
        source = "youtube",
        category = cat,
        score = prefWeight * 1.55 + 2.0,
        authorId = "",
        authorName = yt.channelTitle,
        authorAvatar = yt.thumbnailUrl,
        caption = yt.title,
        audioTitle = "YouTube Shorts · $cat",
        youtubeVideoId = yt.videoId,
        thumbnailUrl = yt.thumbnailUrl,
        coverHex = "#111827"
      )
    }

    // Interleave & sort cleanly by dynamic recommendation score
    return (nativeItems + ytItems).sortedByDescending { it.score }
  }

  fun rankCuratedStories(
    stories: List<Story>,
    prefs: FeedPreferences,
    currentUserId: String
  ): List<Story> {
    val scores = prefs.categoryScores.ifEmpty { FeedPreferences.DEFAULT_SCORES }
    val dismissedSet = prefs.dismissedVideoIds.toSet()
    val dislikedSet = prefs.dislikedCategories.map { it.lowercase() }.toSet()

    return stories
      .filter { story ->
        val isOwn = story.authorId == currentUserId && currentUserId.isNotBlank()
        if (isOwn) return@filter true
        val cat = normalizeCategory(story.category)
        val storyKey = "story_${story.id}"
        story.id !in dismissedSet &&
          storyKey !in dismissedSet &&
          (story.youtubeVideoId.isBlank() || story.youtubeVideoId !in dismissedSet) &&
          cat.lowercase() !in dislikedSet
      }
      .sortedByDescending { story ->
        val cat = normalizeCategory(story.category)
        val prefScore = (scores[cat] ?: 10L).toDouble()
        val timeBonus = ((story.createdAt?.seconds ?: 0L) / 1_000_000.0)
        prefScore * 1.2 + timeBonus
      }
  }
}

class FeedCurationRepository(
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

  fun observePreferences(userId: String): Flow<FeedPreferences> =
    db.collection("users")
      .document(userId)
      .collection("feed_preferences")
      .document(DOC_ID)
      .snapshots()
      .map { snap ->
        if (!snap.exists()) {
          FeedPreferences(id = DOC_ID, userId = userId)
        } else {
          val parsed = snap.toObject(FeedPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          parsed ?: FeedPreferences(id = DOC_ID, userId = userId)
        }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.GET, "users/$userId/feed_preferences/$DOC_ID")
        throw e
      }

  suspend fun savePreferences(prefs: FeedPreferences): Result<FeedPreferences> = runCatching {
    val uid = requireUserId()
    val normalized = prefs.copy(
      userId = uid,
      categoryScores = prefs.categoryScores.ifEmpty { FeedPreferences.DEFAULT_SCORES }.entries.take(20).associate { it.key to it.value },
      dislikedCategories = prefs.dislikedCategories.distinct().take(20),
      dismissedVideoIds = prefs.dismissedVideoIds.distinct().take(200)
    )
    val payload = mapOf(
      "userId" to uid,
      "categoryScores" to normalized.categoryScores,
      "dislikedCategories" to normalized.dislikedCategories,
      "dismissedVideoIds" to normalized.dismissedVideoIds,
      "totalViews" to normalized.totalViews.coerceAtLeast(0L),
      "totalLikes" to normalized.totalLikes.coerceAtLeast(0L),
      "totalFullWatches" to normalized.totalFullWatches.coerceAtLeast(0L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    db.collection("users")
      .document(uid)
      .collection("feed_preferences")
      .document(DOC_ID)
      .set(payload)
      .await()
    normalized
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/feed_preferences/$DOC_ID")
  }

  suspend fun recordEngagement(
    current: FeedPreferences,
    category: String,
    signal: EngagementSignal,
    videoId: String = ""
  ): Result<FeedPreferences> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val updated = FeedCurator.applyEngagement(current, uid, category, signal, videoId)
    return savePreferences(updated)
  }

  suspend fun restoreCategory(current: FeedPreferences, category: String): Result<FeedPreferences> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val updated = FeedCurator.restoreCategory(current, uid, category)
    return savePreferences(updated)
  }

  suspend fun resetPreferences(): Result<FeedPreferences> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val fresh = FeedPreferences(
      id = DOC_ID,
      userId = uid,
      categoryScores = FeedPreferences.DEFAULT_SCORES,
      dislikedCategories = emptyList(),
      dismissedVideoIds = emptyList(),
      totalViews = 0L,
      totalLikes = 0L,
      totalFullWatches = 0L
    )
    return savePreferences(fresh)
  }

  companion object {
    const val DOC_ID = "curation"
  }
}
