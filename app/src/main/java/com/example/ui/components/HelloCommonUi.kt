package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import com.example.R
import com.example.data.media.FirestoreMediaStore
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PostReaction
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ui.theme.HelloCoral
import com.example.ui.theme.HelloEmerald
import com.example.ui.theme.HelloRose
import com.example.ui.theme.HelloViolet
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HelloBrandLogo(modifier: Modifier = Modifier, size: Dp = 36.dp) {
  Surface(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.26f))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(size * 0.26f)),
    color = Color.White,
    shadowElevation = 2.dp
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hello_logo),
      contentDescription = "HELLO app logo",
      modifier = Modifier
        .fillMaxSize()
        .padding(size * 0.08f),
      contentScale = ContentScale.Fit
    )
  }
}

@Composable
fun UserAvatar(
  name: String,
  photoUrl: String,
  size: Dp = 44.dp,
  isOnline: Boolean = false,
  isVerifiedOwner: Boolean = false,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.size(size)) {
    if (photoUrl.isNotBlank()) {
      AsyncImage(
        model = photoUrl,
        contentDescription = "$name avatar",
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
        contentScale = ContentScale.Crop
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = name.trim().take(1).uppercase().ifBlank { "H" },
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
      }
    }
    if (isOnline) {
      Box(
        modifier = Modifier
          .size(size * 0.28f)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(HelloEmerald)
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
    if (isVerifiedOwner) {
      Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified Owner",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .size(size * 0.34f)
          .align(Alignment.TopEnd)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
  }
}

fun formatTimestamp(ts: Timestamp?): String {
  if (ts == null) return "Just now"
  val diffSec = (Timestamp.now().seconds - ts.seconds).coerceAtLeast(0L)
  return when {
    diffSec < 60 -> "Just now"
    diffSec < 3600 -> "${diffSec / 60}m ago"
    diffSec < 86400 -> "${diffSec / 3600}h ago"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(ts.toDate())
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(4.dp))
        Button(onClick = onAction) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun VideoPlayerBox(
  videoUri: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isPlaying by remember(videoUri) { mutableStateOf(false) }
  var isLoading by remember(videoUri) { mutableStateOf(FirestoreMediaStore.isFirestoreMediaUri(videoUri)) }
  var resolvedUri by remember(videoUri) {
    mutableStateOf<Uri?>(if (FirestoreMediaStore.isFirestoreMediaUri(videoUri)) null else Uri.parse(videoUri))
  }
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

  LaunchedEffect(videoUri) {
    if (FirestoreMediaStore.isFirestoreMediaUri(videoUri)) {
      isLoading = true
      val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
      resolvedUri = runCatching {
        Uri.fromFile(FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, videoUri))
      }.getOrNull()
      isLoading = false
    } else {
      resolvedUri = Uri.parse(videoUri)
      isLoading = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(230.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      factory = { ctx ->
        VideoView(ctx).apply {
          val controller = MediaController(ctx)
          controller.setAnchorView(this)
          setMediaController(controller)
          setOnCompletionListener { isPlaying = false }
          resolvedUri?.let {
            tag = it.toString()
            setVideoURI(it)
          }
          videoViewRef = this
        }
      },
      update = { vv ->
        val next = resolvedUri
        if (next != null && vv.tag != next.toString()) {
          vv.tag = next.toString()
          vv.setVideoURI(next)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
    if (isLoading) {
      CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
    } else {
      Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = Modifier
          .size(52.dp)
          .clickable {
            videoViewRef?.let { vv ->
              if (isPlaying) {
                vv.pause()
                isPlaying = false
              } else {
                vv.start()
                isPlaying = true
              }
            }
          }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause video" else "Play video",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
  post: Post,
  currentUserId: String,
  reactions: List<PostReaction>,
  isSaved: Boolean,
  onReact: (String) -> Unit,
  onCommentClick: () -> Unit,
  onShare: (String) -> Unit,
  onToggleSave: () -> Unit,
  onEdit: (String, String) -> Unit,
  onDelete: () -> Unit,
  onReport: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  var showReactionBar by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  val myReaction = reactions.firstOrNull { it.userId == currentUserId }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        UserAvatar(name = post.authorName, photoUrl = post.authorAvatar, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = post.authorName.ifBlank { "Hello Member" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = formatTimestamp(post.createdAt),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
              imageVector = when (post.privacy) {
                "friends" -> Icons.Filled.Group
                "only_me" -> Icons.Filled.Lock
                else -> Icons.Filled.Public
              },
              contentDescription = post.privacy,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        IconButton(onClick = onToggleSave) {
          Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = "Save post",
            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (post.authorId == currentUserId) {
              DropdownMenuItem(
                text = { Text("Edit post") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = {
                  showMenu = false
                  showEditDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Delete post") },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = {
                  showMenu = false
                  onDelete()
                }
              )
            }
            DropdownMenuItem(
              text = { Text("Report post") },
              leadingIcon = { Icon(Icons.Filled.Flag, null) },
              onClick = {
                showMenu = false
                showReportDialog = true
              }
            )
          }
        }
      }

      if (post.content.isNotBlank()) {
        Text(
          text = post.content,
          style = MaterialTheme.typography.bodyLarge
        )
      }

      if (post.mediaUri.isNotBlank()) {
        if (post.mediaType == "video") {
          VideoPlayerBox(videoUri = post.mediaUri)
        } else {
          AsyncImage(
            model = post.mediaUri,
            contentDescription = "Post photo",
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp)
              .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
          )
        }
      }

      AnimatedVisibility(visible = showReactionBar) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf("👍", "❤️", "😂", "😮", "😢", "🔥").forEach { emoji ->
            Text(
              text = emoji,
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier
                .clip(CircleShape)
                .clickable {
                  showReactionBar = false
                  onReact(emoji)
                }
                .padding(6.dp)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${post.likesCount} reactions",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${post.commentsCount} comments · ${post.sharesCount} shares",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        TextButton(
          onClick = {
            if (myReaction != null) onReact(myReaction.emoji) else showReactionBar = !showReactionBar
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          if (myReaction != null) {
            Text(myReaction.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(4.dp))
            Text("Reacted", color = MaterialTheme.colorScheme.primary)
          } else {
            Icon(Icons.Filled.ThumbUp, contentDescription = "React", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("React")
          }
        }

        TextButton(
          onClick = onCommentClick,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Comment")
        }

        TextButton(
          onClick = { showShareDialog = true },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Share")
        }
      }
    }
  }

  if (showEditDialog) {
    var editedContent by remember { mutableStateOf(post.content) }
    var editedPrivacy by remember { mutableStateOf(post.privacy) }
    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Edit Post") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = editedContent,
            onValueChange = { editedContent = it },
            label = { Text("Post text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("public" to "Public", "friends" to "Friends", "only_me" to "Only Me").forEach { (key, label) ->
              OutlinedButton(
                onClick = { editedPrivacy = key },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (editedPrivacy == key) "✓ $label" else label,
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          onEdit(editedContent, editedPrivacy)
          showEditDialog = false
        }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
    )
  }

  if (showShareDialog) {
    var shareNote by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showShareDialog = false },
      title = { Text("Share to Your Feed") },
      text = {
        OutlinedTextField(
          value = shareNote,
          onValueChange = { shareNote = it },
          label = { Text("Add a thought (optional)") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(onClick = {
          onShare(shareNote)
          showShareDialog = false
        }) { Text("Share Now") }
      },
      dismissButton = { TextButton(onClick = { showShareDialog = false }) { Text("Cancel") } }
    )
  }

  if (showReportDialog) {
    var reason by remember { mutableStateOf("Spam or misleading") }
    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      title = { Text("Report Content") },
      text = {
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(onClick = {
          onReport(reason)
          showReportDialog = false
        }) { Text("Submit Report") }
      },
      dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
  comments: List<Comment>,
  onDismiss: () -> Unit,
  onSendComment: (text: String, parentCommentId: String) -> Unit
) {
  var commentInput by remember { mutableStateOf("") }
  var replyingTo by remember { mutableStateOf<Comment?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("Comments & Replies", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      if (comments.isEmpty()) {
        Text(
          "No comments yet. Start the conversation!",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val topLevel = comments.filter { it.parentCommentId.isBlank() }
          topLevel.forEach { comment ->
            val replies = comments.filter { it.parentCommentId == comment.id }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = comment.authorName, photoUrl = comment.authorAvatar, size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(formatTimestamp(comment.createdAt), style = MaterialTheme.typography.labelSmall)
              }
              Spacer(Modifier.height(4.dp))
              Text(comment.text, style = MaterialTheme.typography.bodyMedium)
              TextButton(
                onClick = { replyingTo = comment },
                contentPadding = PaddingValues(0.dp)
              ) {
                Text("Reply", style = MaterialTheme.typography.labelMedium)
              }
              if (replies.isNotEmpty()) {
                Column(
                  modifier = Modifier.padding(start = 18.dp, top = 4.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  replies.forEach { rep ->
                    Text(
                      text = "${rep.authorName}: ${rep.text}",
                      style = MaterialTheme.typography.bodySmall
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (replyingTo != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Replying to ${replyingTo!!.authorName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = { replyingTo = null }) { Text("Cancel") }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = commentInput,
          onValueChange = { commentInput = it },
          placeholder = { Text("Write a comment...") },
          modifier = Modifier
            .weight(1f)
            .testTag("comment_input")
        )
        IconButton(
          onClick = {
            if (commentInput.isNotBlank()) {
              onSendComment(commentInput, replyingTo?.id ?: "")
              commentInput = ""
              replyingTo = null
            }
          },
          modifier = Modifier.testTag("send_comment_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment", tint = MaterialTheme.colorScheme.primary)
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}
