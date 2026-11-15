package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
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
  modifier: Modifier = Modifier,
  autoPlay: Boolean = false,
  isActive: Boolean = true,
  looping: Boolean = false,
  defaultHeight: Dp? = 230.dp,
  cornerRadius: Dp = 16.dp,
  showMediaController: Boolean = true,
  onVideoEnded: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var isPlaying by remember(videoUri) { mutableStateOf(false) }
  var isLoading by remember(videoUri) { mutableStateOf(FirestoreMediaStore.isFirestoreMediaUri(videoUri)) }
  var resolvedUri by remember(videoUri) {
    mutableStateOf<Uri?>(if (FirestoreMediaStore.isFirestoreMediaUri(videoUri)) null else Uri.parse(videoUri))
  }
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
  var prepared by remember(videoUri) { mutableStateOf(false) }

  LaunchedEffect(videoUri) {
    prepared = false
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

  LaunchedEffect(isActive, autoPlay, resolvedUri, prepared, videoViewRef) {
    val vv = videoViewRef ?: return@LaunchedEffect
    if (isActive && autoPlay && resolvedUri != null && prepared) {
      if (!vv.isPlaying) {
        vv.start()
        isPlaying = true
      }
    } else if (!isActive) {
      if (vv.isPlaying) {
        vv.pause()
      }
      isPlaying = false
    }
  }

  val containerModifier = if (defaultHeight != null) {
    modifier
      .fillMaxWidth()
      .height(defaultHeight)
      .clip(RoundedCornerShape(cornerRadius))
      .background(Color.Black)
  } else {
    modifier
      .clip(RoundedCornerShape(cornerRadius))
      .background(Color.Black)
  }

  Box(
    modifier = containerModifier.clickable {
      videoViewRef?.let { vv ->
        if (isPlaying) {
          vv.pause()
          isPlaying = false
        } else {
          vv.start()
          isPlaying = true
        }
      }
    },
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      factory = { ctx ->
        VideoView(ctx).apply {
          if (showMediaController) {
            val controller = MediaController(ctx)
            controller.setAnchorView(this)
            setMediaController(controller)
          }
          setOnPreparedListener { mp ->
            prepared = true
            mp.isLooping = looping && onVideoEnded == null
            if (isActive && autoPlay) {
              start()
              isPlaying = true
            }
          }
          setOnCompletionListener {
            if (onVideoEnded != null) {
              isPlaying = false
              onVideoEnded.invoke()
            } else if (looping) {
              seekTo(0)
              start()
              isPlaying = true
            } else {
              isPlaying = false
            }
          }
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
    } else if (!isPlaying || !autoPlay) {
      Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = Modifier
          .size(56.dp)
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
            modifier = Modifier.size(30.dp)
          )
        }
      }
    }
  }
}

private class YouTubeBridge(
  private val onEndedCallback: () -> Unit
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun onPlayerEnded() {
    mainHandler.post { onEndedCallback() }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayerBox(
  videoId: String,
  isActive: Boolean,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 0.dp,
  onVideoEnded: (() -> Unit)? = null
) {
  val safeId = remember(videoId) { videoId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "") }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val embedHtml = remember(safeId) {
    """
      <!DOCTYPE html>
      <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000000; overflow: hidden; }
          #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
        </style>
      </head>
      <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
          var player;
          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              videoId: '$safeId',
              playerVars: {
                autoplay: ${if (isActive) 1 else 0},
                playsinline: 1,
                rel: 0,
                modestbranding: 1,
                controls: 1,
                enablejsapi: 1
              },
              events: {
                onReady: function(e) {
                  if (${if (isActive) "true" else "false"}) {
                    e.target.playVideo();
                  }
                },
                onStateChange: function(e) {
                  if (e.data === 0 && window.HelloYouTube) {
                    window.HelloYouTube.onPlayerEnded();
                  }
                }
              }
            });
          }
          function setPlaybackActive(active) {
            if (!player || !player.playVideo) return;
            if (active) { player.playVideo(); } else { player.pauseVideo(); }
          }
        </script>
      </body>
      </html>
    """.trimIndent()
  }

  LaunchedEffect(isActive, webViewRef) {
    val wv = webViewRef ?: return@LaunchedEffect
    wv.evaluateJavascript("if (typeof setPlaybackActive === 'function') { setPlaybackActive($isActive); }", null)
  }

  DisposableEffect(safeId) {
    onDispose {
      webViewRef?.apply {
        stopLoading()
        loadUrl("about:blank")
        destroy()
      }
      webViewRef = null
    }
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(cornerRadius))
      .background(Color.Black)
  ) {
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          setBackgroundColor(android.graphics.Color.BLACK)
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.mediaPlaybackRequiresUserGesture = false
          settings.loadWithOverviewMode = true
          settings.useWideViewPort = true
          settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
          webChromeClient = WebChromeClient()
          webViewClient = WebViewClient()
          addJavascriptInterface(
            YouTubeBridge(onEndedCallback = { onVideoEnded?.invoke() }),
            "HelloYouTube"
          )
          loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
          tag = safeId
          webViewRef = this
        }
      },
      update = { wv ->
        if (wv.tag != safeId) {
          wv.tag = safeId
          wv.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
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
  modifier: Modifier = Modifier,
  onShareClick: (() -> Unit)? = null,
  onAuthorClick: (() -> Unit)? = null
) {
  var showMenu by remember { mutableStateOf(false) }
  var showReactionBar by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  val myReaction = reactions.firstOrNull { it.userId == currentUserId }
  val cardContext = LocalContext.current

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
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier)
            .padding(vertical = 2.dp),
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
          onClick = {
            if (onShareClick != null) {
              onShareClick()
            } else {
              showShareDialog = true
            }
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.testTag("post_share_button")
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
      title = { Text("Share Post") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = shareNote,
            onValueChange = { shareNote = it },
            label = { Text("Add a thought to repost on HELLO") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedButton(
            onClick = {
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${post.authorName} on HELLO:\n${post.content}")
              }
              cardContext.startActivity(Intent.createChooser(sendIntent, "Share via"))
              showShareDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share to Other Apps")
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          onShare(shareNote)
          showShareDialog = false
        }) { Text("Share to Feed") }
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
