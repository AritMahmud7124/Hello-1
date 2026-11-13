package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.MainSocialViewModel
import kotlinx.coroutines.delay

@Composable
fun StoriesScreen(
  viewModel: MainSocialViewModel,
  onOpenViewer: (Story) -> Unit
) {
  val context = LocalContext.current
  val storiesState by viewModel.stories.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  var showCreateStory by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Stories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Share moments that shine all day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreateStory = true }, modifier = Modifier.testTag("create_story_button")) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(6.dp))
        Text("Add Story")
      }
    }
    if (isUploading) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Uploading story media to cloud...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }

    when (val state = storiesState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.AutoAwesome, "Stories unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Filled.AutoAwesome,
            title = "No active stories yet",
            subtitle = "Create a photo, video, or gradient text story for your friends.",
            actionLabel = "Create Story",
            onAction = { showCreateStory = true }
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(155.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(state.data, key = { it.id }) { story ->
              Card(
                modifier = Modifier
                  .height(230.dp)
                  .clickable { onOpenViewer(story) },
                shape = RoundedCornerShape(20.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                      )
                    )
                ) {
                  if (story.mediaUri.isNotBlank() && story.mediaType == "image") {
                    AsyncImage(
                      model = story.mediaUri,
                      contentDescription = story.caption,
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop
                    )
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color.Black.copy(alpha = 0.28f))
                      .padding(12.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.TopStart)) {
                      UserAvatar(name = story.authorName, photoUrl = story.authorAvatar, size = 32.dp)
                      Spacer(Modifier.width(8.dp))
                      Text(story.authorName, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                    Text(
                      text = story.caption.ifBlank { "Tap to view story" },
                      color = Color.White,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 4,
                      modifier = Modifier.align(Alignment.BottomStart)
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

  if (showCreateStory) {
    CreateStoryDialog(
      onDismiss = { showCreateStory = false },
      onCreate = { caption, mediaUri, mediaType, bgHex ->
        viewModel.createStory(context, caption, mediaUri, mediaType, bgHex)
        showCreateStory = false
      }
    )
  }
}

@Composable
private fun CreateStoryDialog(
  onDismiss: () -> Unit,
  onCreate: (caption: String, mediaUri: String, mediaType: String, bgHex: String) -> Unit
) {
  var caption by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf("") }
  var mediaType by remember { mutableStateOf("text") }
  var bgHex by remember { mutableStateOf("#1E1B4B") }

  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      mediaUri = uri.toString()
      mediaType = "image"
    }
  }
  val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      mediaUri = uri.toString()
      mediaType = "video"
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Story") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it },
          label = { Text("Story text or caption") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.Image, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Photo")
          }
          OutlinedButton(
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.VideoLibrary, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Video")
          }
        }
        if (mediaUri.isNotBlank()) {
          Text("Media attached ($mediaType) ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
      }
    },
    confirmButton = { Button(onClick = { onCreate(caption, mediaUri, mediaType, bgHex) }) { Text("Share Story") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
fun StoryViewerDialog(
  stories: List<Story>,
  initialStory: Story,
  viewModel: MainSocialViewModel,
  onDismiss: () -> Unit
) {
  val startIdx = remember(stories, initialStory) {
    stories.indexOfFirst { it.id == initialStory.id }.coerceAtLeast(0)
  }
  var currentIndex by remember { mutableIntStateOf(startIdx) }
  var progress by remember { mutableFloatStateOf(0f) }
  var replyText by remember { mutableStateOf("") }
  val activeStory = stories.getOrNull(currentIndex) ?: initialStory

  LaunchedEffect(activeStory.id) {
    viewModel.markStoryViewed(activeStory.id)
    progress = 0f
    while (progress < 1f) {
      delay(80)
      progress += 0.015f
    }
    if (currentIndex < stories.lastIndex) {
      currentIndex++
    } else {
      onDismiss()
    }
  }

  val reactions by viewModel.observeStoryReactions(activeStory.id).collectAsStateWithLifecycle(initialValue = emptyList())

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(16.dp)
    ) {
      if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "image") {
        AsyncImage(
          model = activeStory.mediaUri,
          contentDescription = activeStory.caption,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
          contentScale = ContentScale.Crop
        )
      } else if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "video") {
        Box(modifier = Modifier.align(Alignment.Center)) {
          VideoPlayerBox(videoUri = activeStory.mediaUri)
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = activeStory.caption,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(24.dp)
          )
        }
      }

      Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp)) {
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          UserAvatar(name = activeStory.authorName, photoUrl = activeStory.authorAvatar, size = 38.dp)
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(activeStory.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${formatTimestamp(activeStory.createdAt)} · ${activeStory.viewersCount} views", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
          }
        }
      }

      // Tap areas for prev/next
      Row(modifier = Modifier.fillMaxSize().padding(vertical = 96.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxSize().clickable { if (currentIndex > 0) currentIndex-- })
        Box(modifier = Modifier.weight(1f).fillMaxSize().clickable {
          if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
        })
      }

      Column(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (activeStory.caption.isNotBlank() && activeStory.mediaType != "text") {
          Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
            Text(activeStory.caption, color = Color.White, modifier = Modifier.padding(10.dp))
          }
        }
        if (reactions.isNotEmpty()) {
          Text(
            text = "${reactions.size} reactions/replies",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          listOf("❤️", "🔥", "😂", "👏", "😍").forEach { emoji ->
            Surface(
              color = Color.White.copy(alpha = 0.2f),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.clickable { viewModel.reactToStory(activeStory, emoji) }
            ) {
              Text(emoji, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            placeholder = { Text("Reply to story...", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = {
            if (replyText.isNotBlank()) {
              viewModel.reactToStory(activeStory, "💬", replyText)
              replyText = ""
            }
          }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Reply", tint = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun ReelsScreen(
  viewModel: MainSocialViewModel,
  onOpenStories: () -> Unit
) {
  val context = LocalContext.current
  val reelsState by viewModel.reels.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  var showCreateReel by remember { mutableStateOf(false) }
  var commentReel by remember { mutableStateOf<Reel?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Reels", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Short videos from your HELLO circle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpenStories) { Text("Stories") }
        Button(onClick = { showCreateReel = true }, modifier = Modifier.testTag("create_reel_button")) {
          Icon(Icons.Filled.Add, null)
          Spacer(Modifier.width(4.dp))
          Text("Create")
        }
      }
    }
    if (isUploading) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Uploading reel video to cloud...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }

    when (val state = reelsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.PlayCircle, "Reels unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Filled.PlayCircle,
            title = "No Reels yet",
            subtitle = "Upload a short clip or create a visual reel to share.",
            actionLabel = "Post a Reel",
            onAction = { showCreateReel = true }
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(state.data, key = { it.id }) { reel ->
              val isSaved = reel.id in savedIds
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = reel.authorName, photoUrl = reel.authorAvatar, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(reel.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(reel.audioTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                    IconButton(onClick = { viewModel.toggleSave(reel.id, "reel", reel.authorName, reel.caption) }) {
                      Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = "Save reel")
                    }
                  }

                  if (reel.videoUri.isNotBlank()) {
                    VideoPlayerBox(videoUri = reel.videoUri, modifier = Modifier.height(270.dp))
                  } else {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                          Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                          )
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(reel.caption, color = Color.White, style = MaterialTheme.typography.titleMedium)
                      }
                    }
                  }

                  Text(reel.caption, style = MaterialTheme.typography.bodyMedium)

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { viewModel.likeReel(reel.id) }) {
                      Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                      Spacer(Modifier.width(4.dp))
                      Text("${reel.likesCount}")
                    }
                    TextButton(onClick = { commentReel = reel }) {
                      Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("${reel.commentsCount}")
                    }
                    TextButton(onClick = { viewModel.shareReel(reel.id) }) {
                      Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("${reel.sharesCount}")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateReel) {
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("Original HELLO Mix") }
    var videoUri by remember { mutableStateOf("") }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) videoUri = uri.toString()
    }
    AlertDialog(
      onDismissRequest = { showCreateReel = false },
      title = { Text("Create Reel") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Reel caption") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = audioTitle, onValueChange = { audioTitle = it }, label = { Text("Audio track") }, modifier = Modifier.fillMaxWidth())
          OutlinedButton(
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.VideoLibrary, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (videoUri.isBlank()) "Pick Video (Photo Picker)" else "Video Selected ✓")
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.createReel(context, caption, videoUri, audioTitle, "#FF5A5F")
          showCreateReel = false
        }) { Text("Post Reel") }
      },
      dismissButton = { TextButton(onClick = { showCreateReel = false }) { Text("Cancel") } }
    )
  }

  commentReel?.let { reel ->
    val comments by viewModel.observeReelComments(reel.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      onDismiss = { commentReel = null },
      onSendComment = { text, _ -> viewModel.addReelComment(reel.id, text) }
    )
  }
}
