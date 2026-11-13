package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.OperationType
import com.example.data.model.Post
import com.example.data.model.PostReaction
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.example.data.model.StoryReaction
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  suspend fun uploadMedia(
    context: Context,
    localUriString: String,
    folder: String
  ): Result<String> = runCatching {
    val trimmed = localUriString.trim()
    if (trimmed.isBlank()) return@runCatching ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return@runCatching trimmed
    }

    val uid = requireUserId()
    val uri = Uri.parse(trimmed)
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    val extFromPath = uri.lastPathSegment
      ?.substringAfterLast('.', "")
      ?.lowercase()
      ?.takeIf { it.length in 1..6 && it.all(Char::isLetterOrDigit) }
    val defaultExt = when (folder) {
      "reels" -> "mp4"
      "messages" -> "bin"
      else -> "jpg"
    }
    val ext = extFromMime ?: extFromPath ?: defaultExt
    val objectPath = "hello_media/$folder/$uid/${UUID.randomUUID()}.$ext"
    val ref = storage.reference.child(objectPath)

    if (mimeType != null) {
      val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
      ref.putFile(uri, metadata).await()
    } else {
      ref.putFile(uri).await()
    }
    ref.downloadUrl.await().toString()
  }.onFailure { e ->
    if (e is CancellationException) throw e
  }

  // Public Feed (matches rule: existing().privacy == 'public')
  fun observePublicPosts(limitCount: Long = 30L): Flow<List<Post>> =
    db.collection("posts")
      .whereEqualTo("privacy", "public")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(limitCount)
      .snapshots()
      .map { snap -> snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "posts")
        throw e
      }

  // Own Posts (includes public, friends, only_me; matches rule: existing().authorId == request.auth.uid)
  fun observeUserPosts(userId: String): Flow<List<Post>> =
    db.collection("posts")
      .whereEqualTo("authorId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "posts(authorId=$userId)")
        throw e
      }

  // Friends-only Posts shared with current user (matches rule: request.auth.uid in existing().allowedViewerUids)
  fun observeSharedFriendsPosts(currentUserId: String): Flow<List<Post>> =
    db.collection("posts")
      .whereArrayContains("allowedViewerUids", currentUserId)
      .snapshots()
      .map { snap -> snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "posts(allowedViewerUids)")
        throw e
      }

  suspend fun createPost(
    author: PublicUser,
    content: String,
    mediaUri: String = "",
    mediaType: String = "none",
    privacy: String = "public",
    allowedViewerUids: List<String> = emptyList(),
    groupId: String = "",
    pageId: String = "",
    sharedFromPostId: String = ""
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("posts").document()
    val payload = mapOf(
      "authorId" to uid,
      "authorName" to author.displayName.ifBlank { "Hello Member" }.take(80),
      "authorAvatar" to author.photoUrl.take(1000),
      "content" to content.trim().take(5000),
      "mediaUri" to mediaUri.trim().take(2000),
      "mediaType" to mediaType,
      "privacy" to privacy,
      "allowedViewerUids" to allowedViewerUids.distinct().take(200),
      "groupId" to groupId,
      "pageId" to pageId,
      "likesCount" to 0L,
      "commentsCount" to 0L,
      "sharesCount" to 0L,
      "sharedFromPostId" to sharedFromPostId,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
    docRef.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "posts")
  }

  suspend fun updatePost(postId: String, content: String, privacy: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("posts").document(postId).update(
      mapOf(
        "content" to content.trim().take(5000),
        "privacy" to privacy,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
  }

  suspend fun deletePost(postId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("posts").document(postId).delete().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
  }

  fun observePostReactions(postId: String): Flow<List<PostReaction>> =
    db.collection("posts").document(postId).collection("reactions")
      .snapshots()
      .map { snap -> snap.toObjects(PostReaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "posts/$postId/reactions")
        throw e
      }

  suspend fun togglePostReaction(post: Post, actorName: String, emoji: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reactionRef = db.collection("posts").document(post.id).collection("reactions").document(uid)
    val postRef = db.collection("posts").document(post.id)
    val existing = reactionRef.get().await()
    val batch = db.batch()
    if (existing.exists() && existing.getString("emoji") == emoji) {
      batch.delete(reactionRef)
      batch.update(
        postRef,
        mapOf(
          "likesCount" to FieldValue.increment(-1L),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    } else {
      batch.set(
        reactionRef,
        mapOf(
          "userId" to uid,
          "emoji" to emoji.take(16),
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      if (!existing.exists()) {
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
    }
    batch.commit().await()

    if (post.authorId != uid && !existing.exists()) {
      sendNotificationInternal(
        recipientId = post.authorId,
        actorId = uid,
        actorName = actorName,
        type = "like",
        title = "$actorName reacted $emoji",
        body = post.content.take(80).ifBlank { "Reacted to your post" },
        targetId = post.id
      )
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/reactions")
  }

  fun observeComments(postId: String): Flow<List<Comment>> =
    db.collection("posts").document(postId).collection("comments")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "posts/$postId/comments")
        throw e
      }

  suspend fun addComment(
    post: Post,
    author: PublicUser,
    text: String,
    parentCommentId: String = ""
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val commentRef = db.collection("posts").document(post.id).collection("comments").document()
    val postRef = db.collection("posts").document(post.id)
    val batch = db.batch()
    batch.set(
      commentRef,
      mapOf(
        "parentCommentId" to parentCommentId,
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Hello Member" }.take(80),
        "authorAvatar" to author.photoUrl.take(1000),
        "text" to text.trim().take(1000),
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.update(
      postRef,
      mapOf(
        "commentsCount" to FieldValue.increment(1L),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()

    if (post.authorId != uid) {
      sendNotificationInternal(
        recipientId = post.authorId,
        actorId = uid,
        actorName = author.displayName,
        type = if (parentCommentId.isBlank()) "comment" else "reply",
        title = if (parentCommentId.isBlank()) "${author.displayName} commented" else "${author.displayName} replied",
        body = text.trim().take(120),
        targetId = post.id
      )
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
  }

  suspend fun sharePost(original: Post, sharer: PublicUser, note: String): Result<String> = runCatching {
    val uid = requireUserId()
    val newRef = db.collection("posts").document()
    val origRef = db.collection("posts").document(original.id)
    val combined = if (note.isBlank()) {
      "Shared from ${original.authorName}: ${original.content}"
    } else {
      "${note.trim()}\n\n— Shared from ${original.authorName}: ${original.content}"
    }
    val batch = db.batch()
    batch.set(
      newRef,
      mapOf(
        "authorId" to uid,
        "authorName" to sharer.displayName.ifBlank { "Hello Member" }.take(80),
        "authorAvatar" to sharer.photoUrl.take(1000),
        "content" to combined.take(5000),
        "mediaUri" to original.mediaUri,
        "mediaType" to original.mediaType,
        "privacy" to "public",
        "allowedViewerUids" to emptyList<String>(),
        "groupId" to "",
        "pageId" to "",
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "sharesCount" to 0L,
        "sharedFromPostId" to original.id,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.update(
      origRef,
      mapOf(
        "sharesCount" to FieldValue.increment(1L),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().await()
    newRef.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "posts/share")
  }

  fun observeSavedPosts(userId: String): Flow<List<SavedPost>> =
    db.collection("users").document(userId).collection("saved_posts")
      .snapshots()
      .map { snap -> snap.toObjects(SavedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$userId/saved_posts")
        throw e
      }

  suspend fun toggleSaveItem(
    itemId: String,
    itemType: String,
    authorName: String,
    excerpt: String
  ): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users").document(uid).collection("saved_posts").document(itemId)
    val snap = ref.get().await()
    if (snap.exists()) {
      ref.delete().await()
      false
    } else {
      ref.set(
        mapOf(
          "postId" to itemId,
          "itemType" to itemType,
          "authorName" to authorName.take(80),
          "excerpt" to excerpt.take(300),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      true
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.WRITE, "saved_posts/$itemId")
  }

  suspend fun reportContent(targetType: String, targetId: String, reason: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("reports").document()
    ref.set(
      mapOf(
        "reporterId" to uid,
        "targetType" to targetType,
        "targetId" to targetId,
        "reason" to reason.trim().take(500),
        "status" to "open",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "reports")
  }

  // Stories
  fun observePublicStories(): Flow<List<Story>> =
    db.collection("stories")
      .whereEqualTo("privacy", "public")
      .snapshots()
      .map { snap ->
        snap.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "stories")
        throw e
      }

  suspend fun createStory(
    author: PublicUser,
    caption: String,
    mediaUri: String = "",
    mediaType: String = "text",
    backgroundHex: String = "#1E1B4B"
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("stories").document()
    ref.set(
      mapOf(
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Hello Member" }.take(80),
        "authorAvatar" to author.photoUrl.take(1000),
        "mediaUri" to mediaUri.take(2000),
        "mediaType" to mediaType,
        "caption" to caption.trim().take(500),
        "backgroundHex" to backgroundHex.take(16),
        "privacy" to "public",
        "allowedViewerUids" to emptyList<String>(),
        "viewersCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "stories")
  }

  suspend fun incrementStoryView(storyId: String) {
    runCatching {
      db.collection("stories").document(storyId).update(
        "viewersCount", FieldValue.increment(1L)
      ).await()
    }
  }

  fun observeStoryReactions(storyId: String): Flow<List<StoryReaction>> =
    db.collection("stories").document(storyId).collection("reactions")
      .snapshots()
      .map { snap -> snap.toObjects(StoryReaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "stories/$storyId/reactions")
        throw e
      }

  suspend fun reactToStory(story: Story, actor: PublicUser, emoji: String, replyText: String = ""): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("stories").document(story.id).collection("reactions").document()
    ref.set(
      mapOf(
        "userId" to uid,
        "userName" to actor.displayName.ifBlank { "Hello Member" }.take(80),
        "emoji" to emoji.take(16),
        "replyText" to replyText.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    if (story.authorId != uid) {
      sendNotificationInternal(
        recipientId = story.authorId,
        actorId = uid,
        actorName = actor.displayName,
        type = "reply",
        title = "${actor.displayName} reacted $emoji to your story",
        body = replyText.ifBlank { "Story reaction" },
        targetId = story.id
      )
    }
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "stories/${story.id}/reactions")
  }

  // Reels
  fun observeReels(): Flow<List<Reel>> =
    db.collection("reels")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(Reel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "reels")
        throw e
      }

  suspend fun createReel(
    author: PublicUser,
    caption: String,
    videoUri: String,
    audioTitle: String,
    coverHex: String = "#FF5A5F"
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("reels").document()
    ref.set(
      mapOf(
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Hello Member" }.take(80),
        "authorAvatar" to author.photoUrl.take(1000),
        "caption" to caption.trim().take(1000),
        "videoUri" to videoUri.trim().take(2000),
        "coverHex" to coverHex.take(16),
        "audioTitle" to audioTitle.trim().ifBlank { "Original HELLO Audio" }.take(120),
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "sharesCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "reels")
  }

  suspend fun likeReel(reelId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("reels").document(reelId).update(
      "likesCount", FieldValue.increment(1L)
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "reels/$reelId")
  }

  suspend fun shareReel(reelId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("reels").document(reelId).update(
      "sharesCount", FieldValue.increment(1L)
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "reels/$reelId")
  }

  fun observeReelComments(reelId: String): Flow<List<Comment>> =
    db.collection("reels").document(reelId).collection("comments")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "reels/$reelId/comments")
        throw e
      }

  suspend fun addReelComment(reelId: String, author: PublicUser, text: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val commentRef = db.collection("reels").document(reelId).collection("comments").document()
    val reelRef = db.collection("reels").document(reelId)
    val batch = db.batch()
    batch.set(
      commentRef,
      mapOf(
        "parentCommentId" to "",
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Hello Member" }.take(80),
        "authorAvatar" to author.photoUrl.take(1000),
        "text" to text.trim().take(1000),
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.update(reelRef, "commentsCount", FieldValue.increment(1L))
    batch.commit().await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "reels/$reelId/comments")
  }

  internal suspend fun sendNotificationInternal(
    recipientId: String,
    actorId: String,
    actorName: String,
    type: String,
    title: String,
    body: String,
    targetId: String
  ) {
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
