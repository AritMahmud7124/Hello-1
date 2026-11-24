package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.media.FirestoreMediaStore
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PostReaction
import com.example.data.model.PublicUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.ui.theme.HelloCoral
import com.example.ui.theme.HelloEmerald
import com.example.ui.theme.HelloRose
import com.example.ui.theme.HelloViolet
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

data class ResolvedAuthor(
  val displayName: String,
  val avatarUrl: String,
  val isOnline: Boolean = false
)

fun resolveLiveAuthor(
  authorId: String,
  fallbackName: String,
  fallbackAvatar: String,
  usersById: Map<String, PublicUser>,
  me: PublicUser? = null
): ResolvedAuthor {
  val liveUser = when {
    authorId.isNotBlank() && me != null && authorId == me.userId -> me
    authorId.isNotBlank() -> usersById[authorId]
    else -> null
  }
  val resolvedName = liveUser?.displayName?.ifBlank { null }
    ?: fallbackName.ifBlank { null }
    ?: "Hello Member"
  val resolvedAvatar = liveUser?.photoUrl?.ifBlank { null }
    ?: fallbackAvatar
  return ResolvedAuthor(
    displayName = resolvedName,
    avatarUrl = resolvedAvatar,
    isOnline = liveUser?.effectiveOnline == true
  )
}

fun openLocationInGoogleMaps(
  context: Context,
  latitude: Double,
  longitude: Double,
  label: String = "HELLO SOS"
) {
  if (latitude == 0.0 && longitude == 0.0) {
    Toast.makeText(context, "No GPS coordinates shared for this alert.", Toast.LENGTH_SHORT).show()
    return
  }
  val encodedLabel = Uri.encode(label.ifBlank { "HELLO SOS" })
  val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
  val pm = context.packageManager

  val googleMapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
    setPackage("com.google.android.apps.maps")
  }
  if (googleMapsIntent.resolveActivity(pm) != null) {
    val launched = runCatching { context.startActivity(googleMapsIntent) }.isSuccess
    if (launched) return
  }

  val genericGeoIntent = Intent(Intent.ACTION_VIEW, geoUri)
  if (genericGeoIntent.resolveActivity(pm) != null) {
    val launched = runCatching { context.startActivity(genericGeoIntent) }.isSuccess
    if (launched) return
  }

  val webMapsUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
  val webIntent = Intent(Intent.ACTION_VIEW, webMapsUri)
  if (webIntent.resolveActivity(pm) != null) {
    val launched = runCatching { context.startActivity(webIntent) }.isSuccess
    if (launched) return
  }

  Toast.makeText(context, "Unable to open Google Maps or browser on this device.", Toast.LENGTH_SHORT).show()
}

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
      val dotSize = (size * 0.28f).coerceAtLeast(9.dp)
      Box(
        modifier = Modifier
          .size(dotSize)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(Color(0xFF22C55E))
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
    if (isVerifiedOwner) {
      Icon(
        imageVector = Icons.Rounded.Verified,
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

@Composable
fun FullPhotoImage(
  model: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 16.dp,
  minHeight: Dp = 180.dp,
  maxHeight: Dp = 540.dp,
  enableFullscreenViewer: Boolean = true
) {
  var naturalAspect by remember(model) { mutableStateOf<Float?>(null) }
  var showFullscreen by remember { mutableStateOf(false) }

  val shape = RoundedCornerShape(cornerRadius)
  val frameModifier = modifier
    .fillMaxWidth()
    .then(
      if (naturalAspect != null) {
        Modifier
          .heightIn(min = 120.dp, max = maxHeight)
          .aspectRatio(naturalAspect!!.coerceIn(0.5f, 2.2f))
      } else {
        Modifier.heightIn(min = minHeight, max = maxHeight)
      }
    )
    .clip(shape)
    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
    .then(if (enableFullscreenViewer) Modifier.clickable { showFullscreen = true } else Modifier)

  Box(modifier = frameModifier, contentAlignment = Alignment.Center) {
    AsyncImage(
      model = model,
      contentDescription = contentDescription,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
      onSuccess = { state ->
        val size = state.painter.intrinsicSize
        if (size.width > 0f && size.height > 0f && size.width.isFinite() && size.height.isFinite()) {
          naturalAspect = size.width / size.height
        }
      }
    )
  }

  if (showFullscreen && enableFullscreenViewer) {
    FullScreenPhotoViewerDialog(
      model = model,
      contentDescription = contentDescription,
      onDismiss = { showFullscreen = false }
    )
  }
}

@Composable
fun FullScreenPhotoViewerDialog(
  model: String,
  contentDescription: String?,
  onDismiss: () -> Unit
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .statusBarsPadding()
        .navigationBarsPadding()
        .clipToBounds()
        .pointerInput(model) {
          detectTransformGestures { _, pan, zoom, _ ->
            val next = (scale * zoom).coerceIn(1f, 4f)
            scale = next
            if (next <= 1.02f) {
              offsetX = 0f
              offsetY = 0f
            } else {
              offsetX += pan.x
              offsetY += pan.y
            }
          }
        }
    ) {
      AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offsetX,
            translationY = offsetY
          ),
        contentScale = ContentScale.Fit
      )
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp)
          .background(Color.Black.copy(alpha = 0.55f), CircleShape)
      ) {
        Icon(Icons.Rounded.Close, contentDescription = "Close photo viewer", tint = Color.White)
      }
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
  seekRequestMs: Pair<Int, Long>? = null,
  onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
  onDoubleTap: (() -> Unit)? = null,
  onVideoEnded: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var isPlaying by remember(videoUri) { mutableStateOf(false) }
  var isLoading by remember(videoUri) { mutableStateOf(FirestoreMediaStore.isFirestoreMediaUri(videoUri)) }
  var hasError by remember(videoUri) { mutableStateOf(false) }
  var retryNonce by remember(videoUri) { mutableIntStateOf(0) }
  var resolvedUri by remember(videoUri) {
    mutableStateOf<Uri?>(if (FirestoreMediaStore.isFirestoreMediaUri(videoUri)) null else Uri.parse(videoUri))
  }
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
  var prepared by remember(videoUri) { mutableStateOf(false) }
  val latestOnProgressState = rememberUpdatedState(onProgressUpdate)
  val latestOnDoubleTapState = rememberUpdatedState(onDoubleTap)
  val latestOnEndedState = rememberUpdatedState(onVideoEnded)
  val latestLoopingState = rememberUpdatedState(looping)
  val latestIsActiveState = rememberUpdatedState(isActive)
  val latestAutoPlayState = rememberUpdatedState(autoPlay)

  LaunchedEffect(videoUri, retryNonce) {
    prepared = false
    hasError = false
    if (FirestoreMediaStore.isFirestoreMediaUri(videoUri)) {
      isLoading = true
      val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
      val resolved = runCatching {
        Uri.fromFile(FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, videoUri))
      }.getOrNull()
      resolvedUri = resolved
      hasError = resolved == null
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

  LaunchedEffect(seekRequestMs, prepared, videoViewRef) {
    val req = seekRequestMs ?: return@LaunchedEffect
    val vv = videoViewRef ?: return@LaunchedEffect
    if (!prepared) return@LaunchedEffect
    val target = req.second.coerceAtLeast(0L)
    runCatching {
      vv.seekTo(target.toInt())
      val dur = vv.duration.toLong().coerceAtLeast(0L)
      latestOnProgressState.value?.invoke(target, dur)
    }
  }

  LaunchedEffect(isActive, isPlaying, prepared, videoViewRef) {
    val vv = videoViewRef ?: return@LaunchedEffect
    if (!prepared || !isActive) return@LaunchedEffect
    while (true) {
      latestOnProgressState.value?.let { callback ->
        val pos = runCatching { vv.currentPosition.toLong().coerceAtLeast(0L) }.getOrDefault(0L)
        val dur = runCatching { vv.duration.toLong().coerceAtLeast(0L) }.getOrDefault(0L)
        callback(pos, dur)
      }
      delay(250L)
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
    modifier = containerModifier.pointerInput(videoUri) {
      detectTapGestures(
        onDoubleTap = if (latestOnDoubleTapState.value != null) {
          { latestOnDoubleTapState.value?.invoke() }
        } else null,
        onTap = {
          if (hasError) {
            retryNonce++
          } else {
            videoViewRef?.let { vv ->
              if (isPlaying) {
                vv.pause()
                isPlaying = false
              } else if (prepared) {
                vv.start()
                isPlaying = true
              }
            }
          }
        }
      )
    },
    contentAlignment = Alignment.Center
  ) {
    AndroidView(
      factory = { ctx ->
        prepared = false
        VideoView(ctx).apply {
          if (showMediaController) {
            val controller = MediaController(ctx)
            controller.setAnchorView(this)
            setMediaController(controller)
          }
          setOnPreparedListener { mp ->
            prepared = true
            hasError = false
            runCatching {
              mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            }
            val shouldNativeLoop = latestLoopingState.value && latestOnEndedState.value == null
            mp.isLooping = shouldNativeLoop
            latestOnProgressState.value?.invoke(0L, duration.toLong().coerceAtLeast(0L))
            if (latestIsActiveState.value && latestAutoPlayState.value) {
              start()
              isPlaying = true
            }
          }
          setOnErrorListener { _, _, _ ->
            prepared = false
            isLoading = false
            isPlaying = false
            hasError = true
            true
          }
          setOnCompletionListener {
            val ended = latestOnEndedState.value
            val shouldLoop = latestLoopingState.value
            if (shouldLoop) {
              seekTo(0)
              start()
              isPlaying = true
              ended?.invoke()
            } else if (ended != null) {
              isPlaying = false
              ended.invoke()
            } else {
              isPlaying = false
            }
          }
          resolvedUri?.let {
            tag = "${it}_$retryNonce"
            setVideoURI(it)
          }
          videoViewRef = this
        }
      },
      update = { vv ->
        val next = resolvedUri
        val nextTag = next?.let { "${it}_$retryNonce" }
        if (next != null && nextTag != null && vv.tag != nextTag) {
          prepared = false
          vv.tag = nextTag
          vv.setVideoURI(next)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
    if (isLoading) {
      CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
    } else if (hasError) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier
          .clickable { retryNonce++ }
          .padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = "Retry video", tint = Color.White, modifier = Modifier.size(28.dp))
          Text("Couldn't play video · Tap to retry", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
      }
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
              } else if (prepared) {
                vv.start()
                isPlaying = true
              }
            }
          }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
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
  private val onEndedCallback: () -> Unit,
  private val onProgressCallback: (Float, Float) -> Unit,
  private val onErrorCallback: (Int) -> Unit = {}
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun onPlayerEnded() {
    mainHandler.post { onEndedCallback() }
  }

  @JavascriptInterface
  fun onProgress(currentSec: Float, durationSec: Float) {
    mainHandler.post { onProgressCallback(currentSec, durationSec) }
  }

  @JavascriptInterface
  fun onPlayerError(code: Int) {
    mainHandler.post { onErrorCallback(code) }
  }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun YouTubeEmbedPlayerBox(
  videoId: String,
  isActive: Boolean,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 0.dp,
  showControls: Boolean = true,
  seekRequestSec: Pair<Int, Float>? = null,
  onProgressUpdate: ((currentSec: Float, durationSec: Float) -> Unit)? = null,
  onDoubleTap: (() -> Unit)? = null,
  onVideoEnded: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val safeId = remember(videoId) { videoId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "") }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var embedErrorCode by remember(safeId) { mutableStateOf<Int?>(null) }
  var reloadNonce by remember(safeId) { mutableIntStateOf(0) }
  val latestOnEndedState = rememberUpdatedState(onVideoEnded)
  val latestOnProgressState = rememberUpdatedState(onProgressUpdate)
  val latestOnDoubleTapState = rememberUpdatedState(onDoubleTap)
  val controlsFlag = if (showControls) 1 else 0

  val embedHtml = remember(safeId, controlsFlag) {
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
          var desiredActive = ${if (isActive) "true" else "false"};
          var progressTimer = null;
          function startProgressTimer() {
            if (progressTimer) clearInterval(progressTimer);
            progressTimer = setInterval(function() {
              if (player && player.getCurrentTime && player.getDuration && window.HelloYouTube) {
                var cur = player.getCurrentTime() || 0;
                var dur = player.getDuration() || 0;
                window.HelloYouTube.onProgress(cur, dur);
              }
            }, 350);
          }
          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              videoId: '$safeId',
              playerVars: {
                autoplay: desiredActive ? 1 : 0,
                playsinline: 1,
                rel: 0,
                modestbranding: 1,
                iv_load_policy: 3,
                controls: $controlsFlag,
                origin: 'https://www.youtube.com',
                enablejsapi: 1
              },
              events: {
                onReady: function(e) {
                  startProgressTimer();
                  if (desiredActive) {
                    e.target.playVideo();
                  }
                },
                onStateChange: function(e) {
                  if (e.data === 0 && window.HelloYouTube) {
                    window.HelloYouTube.onPlayerEnded();
                  }
                },
                onError: function(e) {
                  if (window.HelloYouTube) {
                    window.HelloYouTube.onPlayerError(e && e.data ? e.data : -1);
                  }
                }
              }
            });
          }
          function setPlaybackActive(active) {
            desiredActive = !!active;
            if (!player || !player.playVideo) return;
            if (desiredActive) { player.playVideo(); } else { player.pauseVideo(); }
          }
          function togglePlayback() {
            if (!player || !player.getPlayerState) return;
            var st = player.getPlayerState();
            if (st === 1) { player.pauseVideo(); } else { player.playVideo(); }
          }
          function seekToSeconds(sec) {
            if (!player || !player.seekTo) return;
            player.seekTo(sec, true);
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

  LaunchedEffect(seekRequestSec, webViewRef) {
    val req = seekRequestSec ?: return@LaunchedEffect
    val wv = webViewRef ?: return@LaunchedEffect
    val sec = req.second.coerceAtLeast(0f)
    wv.evaluateJavascript("if (typeof seekToSeconds === 'function') { seekToSeconds($sec); }", null)
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
            YouTubeBridge(
              onEndedCallback = { latestOnEndedState.value?.invoke() },
              onProgressCallback = { cur, dur -> latestOnProgressState.value?.invoke(cur, dur) },
              onErrorCallback = { code -> embedErrorCode = code }
            ),
            "HelloYouTube"
          )
          val gestureDetector = GestureDetector(
            ctx,
            object : GestureDetector.SimpleOnGestureListener() {
              override fun onDoubleTap(e: MotionEvent): Boolean {
                latestOnDoubleTapState.value?.invoke()
                return false
              }
              override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!showControls) {
                  evaluateJavascript("if (typeof togglePlayback === 'function') { togglePlayback(); }", null)
                }
                return false
              }
            }
          )
          setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
          }
          loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
          tag = "${safeId}_${controlsFlag}_$reloadNonce"
          webViewRef = this
        }
      },
      update = { wv ->
        val nextTag = "${safeId}_${controlsFlag}_$reloadNonce"
        if (wv.tag != nextTag) {
          wv.tag = nextTag
          wv.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
    if (embedErrorCode != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(24.dp)
        ) {
          Text(
            text = "Clip couldn't load in embedded player",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
              embedErrorCode = null
              reloadNonce++
            }) {
              Text("Retry", color = Color.White)
            }
            Button(onClick = {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$safeId")))
              }
            }) {
              Text("Watch on YouTube")
            }
          }
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
  modifier: Modifier = Modifier,
  allowShare: Boolean = true,
  authorOnline: Boolean = false,
  displayAuthorName: String = post.authorName,
  displayAuthorAvatar: String = post.authorAvatar,
  onShareClick: (() -> Unit)? = null,
  onAuthorClick: (() -> Unit)? = null
) {
  val resolvedAuthorName = displayAuthorName.ifBlank { post.authorName }.ifBlank { "Hello Member" }
  val resolvedAuthorAvatar = displayAuthorAvatar.ifBlank { post.authorAvatar }
  var showMenu by remember { mutableStateOf(false) }
  var showReactionBar by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  val myReaction = reactions.firstOrNull { it.userId == currentUserId }
  val cardContext = LocalContext.current
  val postAudioCtrl = if (post.musicUri.isNotBlank()) {
    rememberPostAudioController(
      audioUri = post.musicUri,
      startSec = post.musicStartSec,
      endSec = post.musicEndSec
    )
  } else null

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
          UserAvatar(name = resolvedAuthorName, photoUrl = resolvedAuthorAvatar, size = 44.dp, isOnline = authorOnline)
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = resolvedAuthorName,
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
              if (post.musicTitle.isNotBlank()) {
                val songLabel = if (post.musicArtist.isNotBlank()) {
                  "${post.musicTitle} · ${post.musicArtist}"
                } else {
                  post.musicTitle
                }
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                  shape = CircleShape,
                  color = Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                  modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(CircleShape)
                    .then(if (postAudioCtrl != null) Modifier.clickable { postAudioCtrl.toggle() } else Modifier)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Icon(
                      imageVector = if (postAudioCtrl?.isPlaying == true) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.MusicNote,
                      contentDescription = "Attached song",
                      modifier = Modifier.size(12.dp),
                      tint = Color(0xFF1877F2)
                    )
                    Text(
                      text = songLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFF0F172A),
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
              Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Icon(
                imageVector = when (post.privacy) {
                  "friends" -> Icons.Rounded.Group
                  "only_me" -> Icons.Rounded.Lock
                  else -> Icons.Rounded.Public
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
            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = "Save post",
            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Post options")
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (post.authorId == currentUserId) {
              DropdownMenuItem(
                text = { Text("Edit post") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = {
                  showMenu = false
                  showEditDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Delete post") },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                onClick = {
                  showMenu = false
                  onDelete()
                }
              )
            }
            DropdownMenuItem(
              text = { Text("Report post") },
              leadingIcon = { Icon(Icons.Rounded.Flag, null) },
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
          Box(modifier = Modifier.fillMaxWidth()) {
            FullPhotoImage(
              model = post.mediaUri,
              contentDescription = "Post photo",
              cornerRadius = 16.dp,
              minHeight = 180.dp,
              maxHeight = 560.dp
            )
            if (postAudioCtrl != null) {
              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(4.dp)
                  .size(48.dp)
                  .clip(CircleShape)
                  .clickable { postAudioCtrl.toggle() }
                  .testTag("post_music_play_button"),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f)),
                  contentAlignment = Alignment.Center
                ) {
                  if (postAudioCtrl.isPreparing) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(15.dp),
                      strokeWidth = 1.8.dp,
                      color = Color.White
                    )
                  } else {
                    Icon(
                      imageVector = if (postAudioCtrl.isPlaying) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                      contentDescription = if (postAudioCtrl.isPlaying) "Mute post music" else "Play post music",
                      tint = Color.White,
                      modifier = Modifier.size(17.dp)
                    )
                  }
                }
              }
            }
          }
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
            Icon(Icons.Rounded.ThumbUp, contentDescription = "React", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("React")
          }
        }

        TextButton(
          onClick = onCommentClick,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = "Comment", modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Comment")
        }

        if (allowShare) {
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
            Icon(Icons.Rounded.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Share")
          }
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
                putExtra(Intent.EXTRA_TEXT, "$resolvedAuthorName on HELLO:\n${post.content}")
              }
              cardContext.startActivity(Intent.createChooser(sendIntent, "Share via"))
              showShareDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
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
  usersById: Map<String, PublicUser> = emptyMap(),
  me: PublicUser? = null,
  onDismiss: () -> Unit,
  onSendComment: (text: String, parentCommentId: String) -> Unit
) {
  val strings = com.example.ui.i18n.LocalAppStrings.current
  var commentInput by remember { mutableStateOf("") }
  var replyingTo by remember { mutableStateOf<Comment?>(null) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val listState = rememberLazyListState()

  val topLevelIds = remember(comments) {
    comments.filter { it.parentCommentId.isBlank() }.map { it.id }.toSet()
  }
  val displayRoots = remember(comments, topLevelIds) {
    comments.filter { it.parentCommentId.isBlank() || it.parentCommentId !in topLevelIds }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.86f)
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${strings.allComments} (${comments.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${comments.size} total",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      if (comments.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            "No comments yet. Start the conversation!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 4.dp)
        ) {
          items(displayRoots, key = { it.id }) { comment ->
            val replies = comments.filter { it.parentCommentId == comment.id && comment.parentCommentId.isBlank() }
            val commentAuthor = resolveLiveAuthor(
              authorId = comment.authorId,
              fallbackName = comment.authorName,
              fallbackAvatar = comment.authorAvatar,
              usersById = usersById,
              me = me
            )
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                  name = commentAuthor.displayName,
                  photoUrl = commentAuthor.avatarUrl,
                  size = 30.dp,
                  isOnline = commentAuthor.isOnline
                )
                Spacer(Modifier.width(8.dp))
                Text(commentAuthor.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  replies.forEach { rep ->
                    val replyAuthor = resolveLiveAuthor(
                      authorId = rep.authorId,
                      fallbackName = rep.authorName,
                      fallbackAvatar = rep.authorAvatar,
                      usersById = usersById,
                      me = me
                    )
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        UserAvatar(name = replyAuthor.displayName, photoUrl = replyAuthor.avatarUrl, size = 20.dp)
                        Text(
                          text = "${replyAuthor.displayName}: ${rep.text}",
                          style = MaterialTheme.typography.bodySmall
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (replyingTo != null) {
        val targetAuthor = resolveLiveAuthor(
          authorId = replyingTo!!.authorId,
          fallbackName = replyingTo!!.authorName,
          fallbackAvatar = replyingTo!!.authorAvatar,
          usersById = usersById,
          me = me
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Replying to ${targetAuthor.displayName}",
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
          placeholder = { Text(strings.writeComment) },
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
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send comment", tint = MaterialTheme.colorScheme.primary)
        }
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}
