package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.security.SecureRandom

@IgnoreExtraProperties
data class PublicUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val bio: String = "",
  val photoUrl: String = "",
  val coverUrl: String = "",
  @get:PropertyName("isOnline") @field:PropertyName("isOnline") val isOnline: Boolean = true,
  @get:PropertyName("showOnlineStatus") @field:PropertyName("showOnlineStatus") val showOnlineStatus: Boolean = true,
  val profilePrivacy: String = "public",
  val acceptSecretLove: Boolean = true,
  val friendsCount: Long = 0L,
  val followersCount: Long = 0L,
  @get:PropertyName("isVerifiedOwner") @field:PropertyName("isVerifiedOwner") val isVerifiedOwner: Boolean = false,
  val role: String = "user",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val effectiveOnline: Boolean
    get() = isOnline && showOnlineStatus
}

@IgnoreExtraProperties
data class PrivateUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val phone: String = "",
  val fcmToken: String = "",
  val notifLikes: Boolean = true,
  val notifComments: Boolean = true,
  val notifFriends: Boolean = true,
  val notifMessages: Boolean = true,
  val notifSecretLove: Boolean = true,
  val notifSos: Boolean = true,
  @get:PropertyName("isDeactivated") @field:PropertyName("isDeactivated") val isDeactivated: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MusicAttachment(
  val title: String = "",
  val artist: String = "",
  val audioUri: String = "",
  val startSec: Long = 0L,
  val endSec: Long = 20L
)

@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatar: String = "",
  val content: String = "",
  val mediaUri: String = "",
  val mediaType: String = "none",
  val musicTitle: String = "",
  val musicArtist: String = "",
  val musicUri: String = "",
  val musicStartSec: Long = 0L,
  val musicEndSec: Long = 0L,
  val privacy: String = "public",
  val allowedViewerUids: List<String> = emptyList(),
  val groupId: String = "",
  val pageId: String = "",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val sharedFromPostId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
  @DocumentId val id: String = "",
  val parentCommentId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatar: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostReaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val emoji: String = "👍",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatar: String = "",
  val mediaUri: String = "",
  val mediaType: String = "text",
  val musicTitle: String = "",
  val musicArtist: String = "",
  val musicUri: String = "",
  val musicStartSec: Long = 0L,
  val musicEndSec: Long = 0L,
  val caption: String = "",
  val backgroundHex: String = "#1E1B4B",
  val category: String = "Young",
  val youtubeVideoId: String = "",
  val privacy: String = "public",
  val allowedViewerUids: List<String> = emptyList(),
  val viewersCount: Long = 0L,
  @get:PropertyName("isSavedHighlight") @field:PropertyName("isSavedHighlight") val isSavedHighlight: Boolean = false,
  val autoDelete24h: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoryReaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val emoji: String = "❤️",
  val replyText: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reel(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatar: String = "",
  val caption: String = "",
  val videoUri: String = "",
  val coverHex: String = "#FF5A5F",
  val audioTitle: String = "Original Audio",
  val category: String = "Young",
  val regionCode: String = "GLOBAL",
  val autoDelete24h: Boolean = false,
  val youtubeVideoId: String = "",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendRequest(
  @DocumentId val id: String = "",
  val fromUid: String = "",
  val toUid: String = "",
  val fromName: String = "",
  val fromAvatar: String = "",
  val toName: String = "",
  val toAvatar: String = "",
  val status: String = "pending",
  val participantUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friendship(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  @get:PropertyName("isGroup") @field:PropertyName("isGroup") val isGroup: Boolean = false,
  val title: String = "",
  val participantUids: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val typingUids: List<String> = emptyList(),
  val readByUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val text: String = "",
  val mediaUri: String = "",
  val mediaType: String = "none",
  val fileName: String = "",
  val reactions: Map<String, String> = emptyMap(),
  val participantUids: List<String> = emptyList(),
  @get:PropertyName("isDeleted") @field:PropertyName("isDeleted") val isDeleted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Group(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val category: String = "Community",
  val privacy: String = "public",
  val creatorId: String = "",
  val adminUids: List<String> = emptyList(),
  val memberUids: List<String> = emptyList(),
  val membersCount: Long = 1L,
  val sosEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SosAlert(
  @DocumentId val id: String = "",
  val groupId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val message: String = "",
  val recipientUids: List<String> = emptyList(),
  val shareLiveLocation: Boolean = false,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val status: String = "active",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PageEntity(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = "Creator",
  val bio: String = "",
  val avatarUrl: String = "",
  val coverUrl: String = "",
  val creatorId: String = "",
  val adminUids: List<String> = emptyList(),
  val followerUids: List<String> = emptyList(),
  val followersCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EventEntity(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val locationName: String = "",
  val eventDateText: String = "",
  val eventTimeText: String = "",
  val creatorId: String = "",
  val creatorName: String = "",
  val goingUids: List<String> = emptyList(),
  val interestedUids: List<String> = emptyList(),
  val invitedUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MarketplaceListing(
  @DocumentId val id: String = "",
  val sellerId: String = "",
  val sellerName: String = "",
  val sellerAvatar: String = "",
  val title: String = "",
  val description: String = "",
  val price: Double = 0.0,
  val currency: String = "$",
  val category: String = "Electronics",
  val condition: String = "Like New",
  val locationText: String = "",
  val imageUri: String = "",
  val status: String = "available",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SecretLoveMessage(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val message: String = "",
  val moodEmoji: String = "❤️",
  @get:PropertyName("isRevealed") @field:PropertyName("isRevealed") val isRevealed: Boolean = false,
  val revealedSenderId: String = "",
  val revealedSenderName: String = "",
  @get:PropertyName("isRead") @field:PropertyName("isRead") val isRead: Boolean = false,
  @get:PropertyName("isBlocked") @field:PropertyName("isBlocked") val isBlocked: Boolean = false,
  @get:PropertyName("isReported") @field:PropertyName("isReported") val isReported: Boolean = false,
  val senderBadgeTitle: String = "",
  val senderTrialDays: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SentSecretLove(
  @DocumentId val id: String = "",
  val messageId: String = "",
  val recipientId: String = "",
  val recipientName: String = "",
  val message: String = "",
  val moodEmoji: String = "❤️",
  @get:PropertyName("isRevealed") @field:PropertyName("isRevealed") val isRevealed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val actorId: String = "",
  val actorName: String = "",
  val type: String = "like",
  val title: String = "",
  val body: String = "",
  val targetId: String = "",
  @get:PropertyName("isRead") @field:PropertyName("isRead") val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReportEntity(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetType: String = "post",
  val targetId: String = "",
  val reason: String = "",
  val status: String = "open",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminRecord(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val addedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OwnerConfig(
  @DocumentId val id: String = "",
  val ownerUid: String = "",
  val ownerName: String = "HELLO Founder",
  val ownerHandle: String = "@founder",
  @get:PropertyName("showOwnerHandle") @field:PropertyName("showOwnerHandle") val showOwnerHandle: Boolean = true,
  val ownerAvatar: String = "",
  val about: String = "Founder & Owner of HELLO Social Network.",
  val contactEmail: String = "",
  @get:PropertyName("showContactEmail") @field:PropertyName("showContactEmail") val showContactEmail: Boolean = false,
  val contactInfo: String = "Available for platform governance & community support.",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GlobalSettings(
  @DocumentId val id: String = "",
  val appName: String = "HELLO",
  val tagline: String = "Say Hello to your real circle.",
  val maintenanceBanner: String = "",
  val sosFeatureEnabled: Boolean = true,
  val secretLoveEnabled: Boolean = true,
  val allowNewRegistrations: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedPost(
  @DocumentId val id: String = "",
  val postId: String = "",
  val itemType: String = "post",
  val authorName: String = "",
  val excerpt: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
  @DocumentId val id: String = "",
  val blockedUid: String = "",
  val blockedName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

enum class EngagementSignal(val weightDelta: Long) {
  VIEW(1L),
  LIKE(3L),
  FULL_WATCH(5L),
  NOT_INTERESTED(-6L)
}

@IgnoreExtraProperties
data class FeedPreferences(
  @DocumentId val id: String = "",
  val userId: String = "",
  val categoryScores: Map<String, Long> = DEFAULT_SCORES,
  val dislikedCategories: List<String> = emptyList(),
  val dismissedVideoIds: List<String> = emptyList(),
  val totalViews: Long = 0L,
  val totalLikes: Long = 0L,
  val totalFullWatches: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  companion object {
    val CATEGORIES = listOf("Young", "Tech", "Funny", "Music", "Lifestyle", "Sports")
    val DEFAULT_SCORES = mapOf(
      "Young" to 10L,
      "Tech" to 10L,
      "Funny" to 10L,
      "Music" to 6L,
      "Lifestyle" to 6L,
      "Sports" to 6L
    )
  }
}

data class YouTubeShortVideo(
  val videoId: String,
  val title: String,
  val channelTitle: String,
  val thumbnailUrl: String,
  val category: String,
  val regionCode: String = "GLOBAL",
  val publishedAtEpochSec: Long = 0L
)

data class CuratedFeedItem(
  val uniqueId: String,
  val source: String, // "youtube" or "native"
  val category: String,
  val score: Double,
  val authorId: String,
  val authorName: String,
  val authorAvatar: String,
  val caption: String,
  val audioTitle: String,
  val videoUri: String = "",
  val youtubeVideoId: String = "",
  val thumbnailUrl: String = "",
  val coverHex: String = "#FF5A5F",
  val regionCode: String = "GLOBAL",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val nativeReel: Reel? = null
)

data class FeedRegionOption(
  val code: String,
  val label: String,
  val flag: String,
  val isGeneral: Boolean = false
)

val HELLO_FEED_REGIONS = listOf(
  FeedRegionOption(code = "GLOBAL", label = "General (Global)", flag = "🌐", isGeneral = true),
  FeedRegionOption(code = "US", label = "USA", flag = "🇺🇸"),
  FeedRegionOption(code = "BD", label = "Bangladesh", flag = "🇧🇩"),
  FeedRegionOption(code = "GB", label = "UK", flag = "🇬🇧"),
  FeedRegionOption(code = "IN", label = "India", flag = "🇮🇳"),
  FeedRegionOption(code = "JP", label = "Japan", flag = "🇯🇵"),
  FeedRegionOption(code = "KR", label = "South Korea", flag = "🇰🇷"),
  FeedRegionOption(code = "BR", label = "Brazil", flag = "🇧🇷"),
  FeedRegionOption(code = "DE", label = "Germany", flag = "🇩🇪")
)

@IgnoreExtraProperties
data class RegionalAnalytics(
  @DocumentId val id: String = "",
  val userId: String = "",
  val feedMode: String = "general",
  val selectedRegion: String = "GLOBAL",
  val originRegion: String = "US",
  val watchMode: String = "Global Mix",
  val activeCategory: String = "For You",
  val switchCount: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SecretLoveSendOutcome(
  val messageId: String,
  val recipientId: String,
  val recipientName: String,
  val isMutualMatch: Boolean = false,
  val mutualMatchId: String = ""
)

enum class MutualMatchPhase {
  SEALED_HIDDEN,
  COUNTDOWN_24H,
  UNLOCKED
}

@IgnoreExtraProperties
data class MutualCrushMatch(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val matchedAtMs: Long = 0L,
  val revealAtMs: Long = 0L,
  val alertAtMs: Long = 0L,
  val alert24hDispatched: Boolean = false,
  val unlockedNotified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun partnerUidFor(myUid: String): String =
    participantUids.firstOrNull { it != myUid }.orEmpty()
}

object MutualSecretLoveEngine {
  const val MIN_REVEAL_DAYS = 7L
  const val MAX_REVEAL_DAYS = 14L
  const val DAY_MS = 86_400_000L
  const val MIN_DELAY_MS = MIN_REVEAL_DAYS * DAY_MS // 604_800_000L
  const val MAX_DELAY_MS = MAX_REVEAL_DAYS * DAY_MS // 1_209_600_000L
  const val ALERT_LEAD_MS = DAY_MS // 24 hours before revealAtMs
  const val ALERT_MESSAGE = "Mutual Secret Love unlock has only 24 hours left"

  data class RevealSchedule(
    val matchedAtMs: Long,
    val revealAtMs: Long,
    val alertAtMs: Long
  )

  fun sortedPair(uidA: String, uidB: String): List<String> =
    if (uidA <= uidB) listOf(uidA, uidB) else listOf(uidB, uidA)

  fun sortedPairId(uidA: String, uidB: String): String {
    val pair = sortedPair(uidA, uidB)
    return "${pair[0]}_${pair[1]}"
  }

  fun pickRandomRevealSchedule(
    matchedAtMs: Long = System.currentTimeMillis(),
    randomFraction: Double = SecureRandom().nextDouble()
  ): RevealSchedule {
    val clamped = randomFraction.coerceIn(0.0, 1.0)
    val spanMs = MAX_DELAY_MS - MIN_DELAY_MS
    val delayMs = (MIN_DELAY_MS + (spanMs * clamped).toLong()).coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
    val revealAtMs = matchedAtMs + delayMs
    val alertAtMs = revealAtMs - ALERT_LEAD_MS
    return RevealSchedule(matchedAtMs = matchedAtMs, revealAtMs = revealAtMs, alertAtMs = alertAtMs)
  }

  fun phaseOf(match: MutualCrushMatch, nowMs: Long = System.currentTimeMillis()): MutualMatchPhase = when {
    match.revealAtMs > 0L && nowMs >= match.revealAtMs -> MutualMatchPhase.UNLOCKED
    match.alertAtMs > 0L && nowMs >= match.alertAtMs -> MutualMatchPhase.COUNTDOWN_24H
    else -> MutualMatchPhase.SEALED_HIDDEN
  }

  fun remainingMsUntilReveal(match: MutualCrushMatch, nowMs: Long = System.currentTimeMillis()): Long =
    (match.revealAtMs - nowMs).coerceAtLeast(0L)

  fun formatCountdown24h(remainingMs: Long): String {
    val totalSeconds = remainingMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
  }
}

data class PureLoveTier(
  val days: Int,
  val label: String,
  val badgeTitle: String,
  val badgeEmoji: String,
  val rarityLabel: String
)

val PURE_LOVE_TIERS = listOf(
  PureLoveTier(7, "7 Days", "Spark Initiate", "✨", "Top 18%"),
  PureLoveTier(30, "30 Days", "Steady Flame", "🔥", "Top 9%"),
  PureLoveTier(90, "3 Months", "Devoted Heart", "💎", "Top 5%"),
  PureLoveTier(180, "6 Months", "Patient Soul", "🕊️", "Top 3%"),
  PureLoveTier(210, "7 Months", "Steadfast Guardian", "🛡️", "Top 2%"),
  PureLoveTier(270, "9 Months", "Unbroken Vow", "🌙", "Top 1.5%"),
  PureLoveTier(300, "10 Months", "Eternal Watch", "⭐", "Top 1.2%"),
  PureLoveTier(365, "1 Year", "1% Legend", "👑", "Top 1% Elite")
)

@IgnoreExtraProperties
data class DevotionTrial(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val recipientId: String = "",
  val recipientName: String = "",
  val targetTierDays: Int = 7,
  val completedDays: Int = 0,
  val currentStreak: Int = 0,
  val missedDaysCount: Int = 0,
  val successRatePct: Int = 100,
  val lastCheckInMs: Long = 0L,
  val lastReflection: String = "",
  val unlockedBadges: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MessengerCall(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val conversationTitle: String = "",
  val callerId: String = "",
  val callerName: String = "",
  val participantUids: List<String> = emptyList(),
  val acceptedUids: List<String> = emptyList(),
  val callType: String = "audio",
  val status: String = "ringing",
  val startedAtMs: Long = 0L,
  val endedAtMs: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSignal(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val recipientId: String = "",
  val type: String = "",
  val sdp: String = "",
  val sdpMid: String = "",
  val sdpMLineIndex: Int = 0,
  val candidate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivateCrushMessage(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val text: String = "",
  val moodEmoji: String = "💌",
  @ServerTimestamp val createdAt: Timestamp? = null
)

object PureLoveEngine {
  const val MIN_HINT_UNLOCK_DAYS = 7
  const val COOLDOWN_MS = 72_000_000L // 20 hours (matches firestore.rules)
  const val MISSED_WINDOW_MS = 172_800_000L // 48 hours -> counts as missed day
  const val MIN_REFLECTION_CHARS = 120

  fun badgeForDays(days: Int): PureLoveTier? =
    PURE_LOVE_TIERS.lastOrNull { days >= it.days }

  fun nextTierForDays(days: Int): PureLoveTier? =
    PURE_LOVE_TIERS.firstOrNull { it.days > days }

  fun cooldownRemainingMs(lastCheckInMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
    if (lastCheckInMs <= 0L) return 0L
    return (lastCheckInMs + COOLDOWN_MS - nowMs).coerceAtLeast(0L)
  }

  fun validateJournalReflection(text: String): String? {
    val clean = text.trim()
    if (clean.length < MIN_REFLECTION_CHARS) {
      return "Write at least $MIN_REFLECTION_CHARS characters (${clean.length}/$MIN_REFLECTION_CHARS)."
    }
    val words = clean.split(Regex("\\s+")).filter { it.length >= 2 }
    if (words.size < 12 || words.map { it.lowercase() }.toSet().size < 8) {
      return "Write a genuine reflection with at least 12 thoughtful words."
    }
    val hasSpamRepeat = Regex("(.)\\1{5,}").containsMatchIn(clean)
    if (hasSpamRepeat) {
      return "Repeated placeholder characters are rejected by Pure Love Integrity."
    }
    return null
  }

  data class CheckInComputeResult(
    val completedDays: Int,
    val currentStreak: Int,
    val missedDaysCount: Int,
    val successRatePct: Int,
    val unlockedBadges: List<String>,
    val newlyUnlockedBadge: PureLoveTier?
  )

  fun computeNextCheckIn(existing: DevotionTrial, nowMs: Long): CheckInComputeResult {
    val missedPenaltyDays = if (existing.lastCheckInMs > 0L && nowMs - existing.lastCheckInMs > MISSED_WINDOW_MS) {
      (((nowMs - existing.lastCheckInMs) / 86_400_000L) - 1L).toInt().coerceIn(1, 30)
    } else {
      0
    }
    val nextCompleted = existing.completedDays + 1
    val nextStreak = if (missedPenaltyDays > 0) 1 else existing.currentStreak + 1
    val nextMissed = existing.missedDaysCount + missedPenaltyDays
    val totalOpportunities = nextCompleted + nextMissed
    val basePct = if (totalOpportunities <= 0) 100 else (nextCompleted * 100) / totalOpportunities
    val nextRate = (basePct - (nextMissed * 4)).coerceIn(5, 100)

    val prevBadges = existing.unlockedBadges.toMutableList()
    var newlyUnlocked: PureLoveTier? = null
    PURE_LOVE_TIERS.forEach { tier ->
      val badgeLabel = "${tier.badgeTitle} (${tier.label})"
      if (nextCompleted >= tier.days && badgeLabel !in prevBadges) {
        prevBadges.add(badgeLabel)
        newlyUnlocked = tier
      }
    }
    return CheckInComputeResult(
      completedDays = nextCompleted,
      currentStreak = nextStreak,
      missedDaysCount = nextMissed,
      successRatePct = nextRate,
      unlockedBadges = prevBadges,
      newlyUnlockedBadge = newlyUnlocked
    )
  }
}


