package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PublicUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val bio: String = "",
  val photoUrl: String = "",
  val coverUrl: String = "",
  val isOnline: Boolean = true,
  val profilePrivacy: String = "public",
  val acceptSecretLove: Boolean = true,
  val friendsCount: Long = 0L,
  val followersCount: Long = 0L,
  val isVerifiedOwner: Boolean = false,
  val role: String = "user",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

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
  val isDeactivated: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
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
  val caption: String = "",
  val backgroundHex: String = "#1E1B4B",
  val privacy: String = "public",
  val allowedViewerUids: List<String> = emptyList(),
  val viewersCount: Long = 0L,
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
  val isGroup: Boolean = false,
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
  val isDeleted: Boolean = false,
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
  val isRevealed: Boolean = false,
  val revealedSenderId: String = "",
  val revealedSenderName: String = "",
  val isRead: Boolean = false,
  val isBlocked: Boolean = false,
  val isReported: Boolean = false,
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
  val isRevealed: Boolean = false,
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
  val isRead: Boolean = false,
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
  val ownerAvatar: String = "",
  val about: String = "Founder & Owner of HELLO Social Network.",
  val contactEmail: String = "",
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
