package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CuratedFeedItem
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.network.YouTubeFetchResult
import com.example.data.network.YouTubeShortsClient
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.YouTubeEmbedPlayerBox
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class StoryThemeOption(
  val hex: String,
  val label: String,
  val colors: List<Color>
)

internal val STORY_THEME_OPTIONS = listOf(
  StoryThemeOption("#1E1B4B", "Midnight", listOf(Color(0xFF1E1B4B), Color(0xFF4338CA))),
  StoryThemeOption("#BE123C", "Sunset", listOf(Color(0xFFBE123C), Color(0xFFFB7185))),
  StoryThemeOption("#0F766E", "Ocean", listOf(Color(0xFF0F766E), Color(0xFF06B6D4))),
  StoryThemeOption("#833AB4", "Glow", listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))),
  StoryThemeOption("#065F46", "Forest", listOf(Color(0xFF065F46), Color(0xFF10B981))),
  StoryThemeOption("#111827", "Obsidian", listOf(Color(0xFF111827), Color(0xFF374151)))
)

internal fun storyGradientForHex(hex: String): List<Color> =
  STORY_THEME_OPTIONS.firstOrNull { it.hex.equals(hex.trim(), ignoreCase = true) }?.colors
    ?: STORY_THEME_OPTIONS.first().colors

internal fun isStoryFresh24h(story: Story): Boolean {
  val ts = story.createdAt ?: return true
  return (Timestamp.now().seconds - ts.seconds) < 86_400L
}

private enum class StoryFilterTab(val label: String) {
  ALL("All"),
  MINE("My Stories"),
  FRIENDS("Friends"),
  PHOTOS("Photos"),
  VIDEOS("Videos")
}

@Composable
fun StoriesScreen(
  viewModel: MainSocialViewModel,
  currentUserId: String = "",
  friendUids: List<String> = emptyList(),
  blockedUids: Set<String> = emptySet(),
  onOpenViewer: (Story) -> Unit
) {
  val context = LocalContext.current
  val storiesState by viewModel.stories.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  var showCreateStory by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(StoryFilterTab.ALL) }
  var deleteTarget by remember { mutableStateOf<Story?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Stories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Moments, clips & mood cards · auto-organized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreateStory = true }, modifier = Modifier.testTag("create_story_button")) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(6.dp))
        Text("Add Story")
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StoryFilterTab.entries.forEach { tab ->
        FilterChip(
          selected = selectedTab == tab,
          onClick = { selectedTab = tab },
          label = { Text(tab.label) },
          modifier = Modifier.testTag("story_filter_${tab.name.lowercase()}")
        )
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
        val visibleStories = remember(state.data, selectedTab, currentUserId, friendUids, blockedUids) {
          val unblocked = state.data.filter { it.authorId !in blockedUids }
          when (selectedTab) {
            StoryFilterTab.ALL -> unblocked
            StoryFilterTab.MINE -> unblocked.filter { it.authorId == currentUserId }
            StoryFilterTab.FRIENDS -> unblocked.filter { it.privacy == "friends" || it.authorId in friendUids }
            StoryFilterTab.PHOTOS -> unblocked.filter { it.mediaType == "image" && it.mediaUri.isNotBlank() }
            StoryFilterTab.VIDEOS -> unblocked.filter { it.mediaType == "video" && it.mediaUri.isNotBlank() }
          }
        }
        if (visibleStories.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Filled.AutoAwesome,
            title = if (selectedTab == StoryFilterTab.ALL) "No active stories yet" else "No ${selectedTab.label.lowercase()} yet",
            subtitle = "Create a photo, video, or gradient mood story for friends or everyone.",
            actionLabel = "Create Story",
            onAction = { showCreateStory = true }
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(155.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(visibleStories, key = { it.id }) { story ->
              val storyGradient = remember(story.backgroundHex) { storyGradientForHex(story.backgroundHex) }
              val isFresh = remember(story.createdAt) { isStoryFresh24h(story) }
              Card(
                modifier = Modifier
                  .height(238.dp)
                  .clickable { onOpenViewer(story) }
                  .testTag("story_card_${story.id}"),
                shape = RoundedCornerShape(20.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(storyGradient))
                ) {
                  if (story.mediaUri.isNotBlank() && story.mediaType == "image") {
                    AsyncImage(
                      model = story.mediaUri,
                      contentDescription = story.caption.ifBlank { "${story.authorName}'s story photo" },
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop
                    )
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(
                        Brush.verticalGradient(
                          listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.14f),
                            Color.Black.copy(alpha = 0.72f)
                          )
                        )
                      )
                      .padding(10.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                    ) {
                      UserAvatar(name = story.authorName, photoUrl = story.authorAvatar, size = 30.dp)
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          story.authorName,
                          color = Color.White,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          Icon(
                            imageVector = if (story.privacy == "friends") Icons.Filled.Group else Icons.Filled.Public,
                            contentDescription = story.privacy,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                          )
                          Text(
                            text = if (isFresh) formatTimestamp(story.createdAt) else "Saved",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                          )
                        }
                      }
                      if (story.authorId == currentUserId && currentUserId.isNotBlank()) {
                        Surface(
                          color = Color.Black.copy(alpha = 0.45f),
                          shape = CircleShape,
                          modifier = Modifier
                            .size(28.dp)
                            .clickable { deleteTarget = story }
                            .testTag("delete_story_card_${story.id}")
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete story", tint = Color.White, modifier = Modifier.size(16.dp))
                          }
                        }
                      }
                    }

                    if (story.mediaType == "video" && story.mediaUri.isNotBlank()) {
                      Surface(
                        color = Color.Black.copy(alpha = 0.52f),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp).align(Alignment.Center)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(Icons.Filled.PlayArrow, contentDescription = "Video story", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                      }
                    }

                    Column(
                      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Text(
                        text = story.caption.ifBlank {
                          if (story.mediaType == "video") "Video Story" else "Tap to view story"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                      )
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Visibility, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(12.dp))
                        Text("${story.viewersCount}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
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
  }

  if (showCreateStory) {
    CreateStoryDialog(
      friendCount = friendUids.size,
      onDismiss = { showCreateStory = false },
      onCreate = { caption, mediaUri, mediaType, bgHex, privacy, category, ytInput ->
        viewModel.createStory(
          context = context,
          caption = caption,
          mediaUri = mediaUri,
          mediaType = mediaType,
          backgroundHex = bgHex,
          privacy = privacy,
          friendUids = friendUids,
          category = category,
          youtubeVideoId = ytInput
        )
        showCreateStory = false
      }
    )
  }

  deleteTarget?.let { story ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Delete Story?") },
      text = { Text("This story and its reactions will be removed permanently.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteStory(story.id)
            deleteTarget = null
          }
        ) { Text("Delete") }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun CreateStoryDialog(
  friendCount: Int,
  onDismiss: () -> Unit,
  onCreate: (caption: String, mediaUri: String, mediaType: String, bgHex: String, privacy: String, category: String, ytInput: String) -> Unit
) {
  var caption by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf("") }
  var mediaType by remember { mutableStateOf("text") }
  var bgHex by remember { mutableStateOf(STORY_THEME_OPTIONS.first().hex) }
  var privacy by remember { mutableStateOf("public") }
  var category by remember { mutableStateOf("Young") }
  var ytInput by remember { mutableStateOf("") }
  var pendingCropUri by remember { mutableStateOf<String?>(null) }

  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      pendingCropUri = uri.toString()
    }
  }
  val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      mediaUri = uri.toString()
      mediaType = "video"
    }
  }

  val extractedYtId = remember(ytInput) { YouTubeShortsClient.extractVideoId(ytInput) }
  val canShare = caption.isNotBlank() || mediaUri.isNotBlank() || extractedYtId.isNotBlank()
  val previewGradient = remember(bgHex) { storyGradientForHex(bgHex) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Story Studio", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Live Story Preview Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(previewGradient)),
          contentAlignment = Alignment.Center
        ) {
          if (mediaUri.isNotBlank() && mediaType == "image") {
            AsyncImage(
              model = mediaUri,
              contentDescription = "Story photo preview",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
          }
          if (mediaUri.isNotBlank() && mediaType == "video") {
            Surface(
              color = Color.Black.copy(alpha = 0.55f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Video attached", color = Color.White, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          if (mediaUri.isNotBlank()) {
            Surface(
              color = Color.Black.copy(alpha = 0.62f),
              shape = CircleShape,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clickable {
                  mediaUri = ""
                  mediaType = "text"
                }
                .testTag("clear_story_media_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Remove media", tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
          }
          Text(
            text = caption.ifBlank { "Your live story preview..." },
            color = Color.White.copy(alpha = if (caption.isBlank()) 0.68f else 1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }

        // Gradient Theme Swatches
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Background Theme", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            STORY_THEME_OPTIONS.forEach { option ->
              val selected = bgHex.equals(option.hex, ignoreCase = true)
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(option.colors))
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                  )
                  .clickable { bgHex = option.hex }
                  .testTag("story_theme_${option.label.lowercase()}"),
                contentAlignment = Alignment.Center
              ) {
                if (selected) {
                  Icon(Icons.Filled.Check, contentDescription = option.label, tint = Color.White, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it.take(500) },
          label = { Text("Story text or caption") },
          placeholder = { Text("Share what's happening right now...") },
          modifier = Modifier.fillMaxWidth().testTag("story_caption_input"),
          minLines = 2,
          maxLines = 4
        )

        // Quick Mood Chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("✨ Feeling blessed", "🔥 Big mood", "☕ Coffee break", "🎵 Vibing").forEach { mood ->
            FilterChip(
              selected = false,
              onClick = {
                caption = if (caption.isBlank()) mood else "$caption $mood"
              },
              label = { Text(mood, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        // Privacy Selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          FilterChip(
            selected = privacy == "public",
            onClick = { privacy = "public" },
            leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(16.dp)) },
            label = { Text("Public") },
            modifier = Modifier.weight(1f).testTag("story_privacy_public")
          )
          FilterChip(
            selected = privacy == "friends",
            onClick = { privacy = "friends" },
            leadingIcon = { Icon(Icons.Filled.Group, null, Modifier.size(16.dp)) },
            label = { Text("Friends ($friendCount)") },
            modifier = Modifier.weight(1f).testTag("story_privacy_friends")
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f).testTag("pick_story_photo_button")
          ) {
            Icon(Icons.Filled.Image, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Photo")
          }
          OutlinedButton(
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.weight(1f).testTag("pick_story_video_button")
          ) {
            Icon(Icons.Filled.VideoLibrary, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Video")
          }
        }

        if (mediaUri.isNotBlank() && mediaType == "image") {
          OutlinedButton(
            onClick = { pendingCropUri = mediaUri },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Adjust 9:16 Crop")
          }
        }
        // Story Category Tag
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Interest Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FeedPreferences.CATEGORIES.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }

        OutlinedTextField(
          value = ytInput,
          onValueChange = { ytInput = it },
          label = { Text("Optional YouTube Short Link") },
          placeholder = { Text("https://youtube.com/shorts/...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (!canShare) {
          Text(
            "Add story text, a photo, a video clip, or a YouTube Short link.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(caption, mediaUri, mediaType, bgHex, privacy, category, extractedYtId) },
        enabled = canShare,
        modifier = Modifier.testTag("submit_story_button")
      ) { Text("Share Story") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )

  pendingCropUri?.let { rawUri ->
    ImageCropDialog(
      imageUri = rawUri,
      defaultAspect = CropAspect.STORY,
      onDismiss = { pendingCropUri = null },
      onUseOriginal = {
        mediaUri = rawUri
        mediaType = "image"
        pendingCropUri = null
      },
      onCropped = { cropped ->
        mediaUri = cropped
        mediaType = "image"
        pendingCropUri = null
      }
    )
  }
}

@Composable
fun StoryViewerDialog(
  stories: List<Story>,
  initialStory: Story,
  viewModel: MainSocialViewModel,
  communityVm: CommunityViewModel? = null,
  me: PublicUser? = null,
  currentUserId: String = "",
  onOpenProfile: ((String) -> Unit)? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val startIdx = remember(stories, initialStory) {
    stories.indexOfFirst { it.id == initialStory.id }.coerceAtLeast(0)
  }
  var currentIndex by remember { mutableIntStateOf(startIdx) }
  var progress by remember { mutableFloatStateOf(0f) }
  var replyText by remember { mutableStateOf("") }
  var isPaused by remember { mutableStateOf(false) }
  var isHoldingPause by remember { mutableStateOf(false) }
  var isInputFocused by remember { mutableStateOf(false) }
  var showCommentsList by remember { mutableStateOf(false) }
  var showShareSheet by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var burstEmoji by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(stories.size) {
    if (stories.isEmpty()) {
      onDismiss()
    } else if (currentIndex > stories.lastIndex) {
      currentIndex = stories.lastIndex
    }
  }

  val activeStory = stories.getOrNull(currentIndex) ?: initialStory
  val shouldPauseTimer = isPaused || isHoldingPause || isInputFocused || replyText.isNotEmpty() || showCommentsList || showShareSheet || showDeleteConfirm

  fun goToNextStory() {
    if (currentIndex < stories.lastIndex) {
      currentIndex++
    } else {
      onDismiss()
    }
  }

  fun goToPrevStory() {
    if (currentIndex > 0) {
      currentIndex--
    } else {
      progress = 0f
    }
  }

  LaunchedEffect(activeStory.id) {
    if (activeStory.authorId != currentUserId && currentUserId.isNotBlank()) {
      viewModel.markStoryViewed(activeStory.id, activeStory.category)
    }
    progress = 0f
    replyText = ""
    showCommentsList = false
    val isVideoOrYt = (activeStory.mediaType == "video" && activeStory.mediaUri.isNotBlank()) || activeStory.youtubeVideoId.isNotBlank()
    val stepPerTick = if (isVideoOrYt) 0.004f else 0.0125f
    while (progress < 1f) {
      delay(80)
      if (!shouldPauseTimer) {
        progress += stepPerTick
      }
    }
    if (activeStory.authorId != currentUserId) {
      viewModel.trackEngagement(activeStory.id, activeStory.category, EngagementSignal.FULL_WATCH)
    }
    goToNextStory()
  }

  LaunchedEffect(burstEmoji) {
    if (burstEmoji != null) {
      delay(950)
      burstEmoji = null
    }
  }

  val reactions by viewModel.observeStoryReactions(activeStory.id).collectAsStateWithLifecycle(initialValue = emptyList())
  val sortedReactions = remember(reactions) {
    reactions.sortedByDescending { it.createdAt?.seconds ?: 0L }
  }
  val textComments = remember(sortedReactions) {
    sortedReactions.filter { it.replyText.isNotBlank() }
  }
  val storyGradient = remember(activeStory.backgroundHex) { storyGradientForHex(activeStory.backgroundHex) }

  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

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
        .imePadding()
        .padding(12.dp)
    ) {
      if (activeStory.youtubeVideoId.isNotBlank()) {
        YouTubeEmbedPlayerBox(
          videoId = activeStory.youtubeVideoId,
          isActive = !shouldPauseTimer,
          modifier = Modifier.fillMaxSize(),
          cornerRadius = 24.dp,
          onVideoEnded = {
            viewModel.trackEngagement(activeStory.id, activeStory.category, EngagementSignal.FULL_WATCH)
            goToNextStory()
          }
        )
      } else if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "image") {
        AsyncImage(
          model = activeStory.mediaUri,
          contentDescription = activeStory.caption.ifBlank { "Story photo" },
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
          contentScale = ContentScale.Crop
        )
      } else if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "video") {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
          contentAlignment = Alignment.Center
        ) {
          VideoPlayerBox(
            videoUri = activeStory.mediaUri,
            modifier = Modifier.fillMaxSize(),
            autoPlay = true,
            isActive = !shouldPauseTimer,
            defaultHeight = null,
            cornerRadius = 24.dp,
            showMediaController = false,
            onVideoEnded = {
              viewModel.trackEngagement(activeStory.id, activeStory.category, EngagementSignal.FULL_WATCH)
              goToNextStory()
            }
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(storyGradient)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = activeStory.caption,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.padding(28.dp)
          )
        }
      }

      // Top & bottom contrast scrims
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .height(140.dp)
          .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
          .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
      )
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(270.dp)
          .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
      )

      // Top Segmented Progress + Author Bar
      Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp)) {
        val totalSegments = stories.size.coerceAtLeast(1)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          for (idx in 0 until totalSegments) {
            val segmentProgress = when {
              idx < currentIndex -> 1f
              idx == currentIndex -> progress.coerceIn(0f, 1f)
              else -> 0f
            }
            LinearProgressIndicator(
              progress = { segmentProgress },
              modifier = Modifier
                .weight(1f)
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = Color.White,
              trackColor = Color.White.copy(alpha = 0.3f)
            )
          }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .then(
                if (onOpenProfile != null) Modifier.clickable {
                  onDismiss()
                  onOpenProfile(activeStory.authorId)
                } else Modifier
              )
              .padding(vertical = 2.dp)
          ) {
            UserAvatar(name = activeStory.authorName, photoUrl = activeStory.authorAvatar, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(activeStory.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(
                  imageVector = if (activeStory.privacy == "friends") Icons.Filled.Group else Icons.Filled.Public,
                  contentDescription = activeStory.privacy,
                  tint = Color.White.copy(alpha = 0.8f),
                  modifier = Modifier.size(13.dp)
                )
              }
              Text(
                text = "${formatTimestamp(activeStory.createdAt)} · ${activeStory.viewersCount} views" + if (shouldPauseTimer) " · Paused" else "",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall
              )
            }
          }
          IconButton(onClick = { isPaused = !isPaused }) {
            Icon(if (shouldPauseTimer) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = "Pause story", tint = Color.White)
          }
          if (activeStory.authorId != currentUserId) {
            IconButton(
              onClick = {
                viewModel.markNotInterested("story_${activeStory.id}", activeStory.youtubeVideoId, activeStory.category)
                goToNextStory()
              },
              modifier = Modifier.testTag("story_not_interested_button")
            ) {
              Icon(Icons.Filled.ThumbDown, contentDescription = "Not interested in story", tint = Color.White)
            }
          }
          if (activeStory.authorId == currentUserId && currentUserId.isNotBlank()) {
            IconButton(
              onClick = { showDeleteConfirm = true },
              modifier = Modifier.testTag("story_delete_button")
            ) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete story", tint = Color.White)
            }
          }
          IconButton(onClick = { showShareSheet = true }, modifier = Modifier.testTag("story_share_button")) {
            Icon(Icons.Filled.Share, contentDescription = "Share story", tint = Color.White)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
          }
        }
      }

      // Middle Gesture Zone: tap left/right for Prev/Next, press-and-hold anywhere in middle to Pause
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 92.dp, bottom = 245.dp)
          .pointerInput(activeStory.id, stories.size, currentIndex) {
            detectTapGestures(
              onPress = {
                isHoldingPause = true
                tryAwaitRelease()
                isHoldingPause = false
              },
              onTap = { offset ->
                if (offset.x < size.width * 0.35f) {
                  goToPrevStory()
                } else {
                  goToNextStory()
                }
              }
            )
          }
      )

      // Floating Emoji Burst Feedback
      AnimatedVisibility(
        visible = burstEmoji != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = Modifier.align(Alignment.Center)
      ) {
        Surface(
          color = Color.Black.copy(alpha = 0.55f),
          shape = CircleShape
        ) {
          Text(
            text = burstEmoji.orEmpty(),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
          )
        }
      }

      // Bottom Caption + Comments Drawer + Quick Reactions + Comment Input
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (activeStory.caption.isNotBlank() && activeStory.mediaType != "text") {
          Surface(color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(12.dp)) {
            Text(activeStory.caption, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
          }
        }

        AnimatedVisibility(visible = showCommentsList) {
          Surface(
            color = Color.Black.copy(alpha = 0.82f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
          ) {
            if (sortedReactions.isEmpty()) {
              Text(
                text = "No comments or reactions yet. Be the first to reply!",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(14.dp)
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(sortedReactions, key = { it.id }) { r ->
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.emoji, style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                      Text(r.userName.ifBlank { "Member" }, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      if (r.replyText.isNotBlank()) {
                        Text(r.replyText, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall)
                      }
                    }
                    Text(formatTimestamp(r.createdAt), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.18f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .clickable { showCommentsList = !showCommentsList }
              .testTag("toggle_story_comments_button")
          ) {
            Text(
              text = if (textComments.isNotEmpty()) {
                "💬 ${textComments.size} comment(s) · ${sortedReactions.size} total " + if (showCommentsList) "▼" else "▲"
              } else {
                "💬 ${sortedReactions.size} reactions " + if (showCommentsList) "▼" else "▲"
              },
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
          Text(
            text = "${currentIndex + 1} / ${stories.size.coerceAtLeast(1)}",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelSmall
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          listOf("❤️", "🔥", "😂", "👏", "😍").forEach { emoji ->
            Surface(
              color = Color.White.copy(alpha = 0.22f),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.clickable {
                burstEmoji = emoji
                viewModel.reactToStory(activeStory, emoji)
              }
            ) {
              Text(emoji, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
          }
        }

        fun submitComment() {
          val trimmed = replyText.trim()
          if (trimmed.isNotEmpty()) {
            viewModel.reactToStory(activeStory, "💬", trimmed)
            replyText = ""
            showCommentsList = true
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it.take(300) },
            placeholder = { Text("Write a story comment...", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submitComment() }),
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              cursorColor = Color.White,
              focusedContainerColor = Color.Black.copy(alpha = 0.68f),
              unfocusedContainerColor = Color.Black.copy(alpha = 0.56f),
              focusedBorderColor = Color.White.copy(alpha = 0.9f),
              unfocusedBorderColor = Color.White.copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .weight(1f)
              .onFocusChanged { focusState -> isInputFocused = focusState.isFocused }
              .testTag("story_comment_input")
          )
          Surface(
            color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.22f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
          ) {
            IconButton(
              onClick = { submitComment() },
              modifier = Modifier.testTag("send_story_comment_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send story comment", tint = Color.White)
            }
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete this Story?") },
      text = { Text("It will disappear for everyone immediately.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteStory(activeStory.id)
            showDeleteConfirm = false
            if (stories.size <= 1) onDismiss()
          }
        ) { Text("Delete") }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
      }
    )
  }

  if (showShareSheet) {
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "${activeStory.authorName}'s HELLO Story",
        body = activeStory.caption.ifBlank { "Check out this Story on HELLO!" },
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { showShareSheet = false },
      onRepostToFeed = { note ->
        val composed = if (note.isBlank()) {
          "Shared ${activeStory.authorName}'s story: ${activeStory.caption}"
        } else {
          "$note\n\n— Story by ${activeStory.authorName}: ${activeStory.caption}"
        }
        viewModel.createPost(context, composed, activeStory.mediaUri, activeStory.mediaType.takeIf { it in listOf("image", "video") } ?: "none", "public", emptyList())
      },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToConversation(conv, sender, msg)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToPerson(sender, person, msg)
      }
    )
  }
}

@Composable
fun ReelsScreen(
  viewModel: MainSocialViewModel,
  communityVm: CommunityViewModel? = null,
  me: PublicUser? = null,
  currentUserId: String = "",
  onOpenStories: () -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val curatedState by viewModel.curatedReels.collectAsStateWithLifecycle()
  val feedPrefs by viewModel.feedPreferences.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedReelCategory.collectAsStateWithLifecycle()
  val youtubeStatus by viewModel.youtubeStatus.collectAsStateWithLifecycle()
  val isFetchingShorts by viewModel.isFetchingShorts.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
  val localLikedIds by viewModel.localLikedItemIds.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }

  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

  var showCreateReel by remember { mutableStateOf(false) }
  var showTuningDialog by remember { mutableStateOf(false) }
  var commentReel by remember { mutableStateOf<Reel?>(null) }
  var shareItemTarget by remember { mutableStateOf<CuratedFeedItem?>(null) }
  var autoAdvanceEnabled by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    viewModel.ensureYouTubeShortsLoaded()
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C10))) {
    when (val state = curatedState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
      }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        EmptyStateCard(Icons.Filled.PlayCircle, "Reels unavailable", state.message)
      }
      is UiState.Success -> {
        val items = state.data
        if (items.isEmpty()) {
          Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            val missingKey = youtubeStatus is YouTubeFetchResult.MissingApiKey
            EmptyStateCard(
              icon = Icons.Filled.PlayCircle,
              title = if (missingKey) "Add YOUTUBE_API_KEY or Post a Reel" else "No matching Reels right now",
              subtitle = when (val yt = youtubeStatus) {
                is YouTubeFetchResult.MissingApiKey ->
                  "Configure YOUTUBE_API_KEY in AI Studio Secrets for automatic YouTube Shorts discovery, or post your own Reel or YouTube link."
                is YouTubeFetchResult.Error ->
                  "YouTube discovery note: ${yt.message}. You can refresh or post your own Reel."
                else ->
                  "All clips in this category were filtered or none uploaded yet. Refresh or reset your feed preferences."
              },
              actionLabel = "Post a Reel",
              onAction = { showCreateReel = true }
            )
          }
        } else {
          val pagerState = rememberPagerState(pageCount = { items.size })

          VerticalPager(
            state = pagerState,
            key = { idx -> items.getOrNull(idx)?.uniqueId ?: idx },
            modifier = Modifier.fillMaxSize().testTag("reels_vertical_pager"),
            beyondViewportPageCount = 1
          ) { page ->
            val item = items[page]
            val isCurrentPage = pagerState.currentPage == page
            val bookmarkKey = item.nativeReel?.id ?: item.uniqueId
            val isSaved = bookmarkKey in savedIds
            val isLiked = item.uniqueId in localLikedIds

            fun advanceToNextReel() {
              if (autoAdvanceEnabled && items.size > 1) {
                val next = (page + 1) % items.size
                scope.launch {
                  pagerState.animateScrollToPage(next)
                }
              }
            }

            // Record engagement impression automatically after watching for 2s
            LaunchedEffect(item.uniqueId, isCurrentPage) {
              if (isCurrentPage) {
                delay(2000)
                viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.VIEW)
              }
            }

            // Auto-advance timer for cover/text-only reels
            var textReelProgress by remember(item.uniqueId, isCurrentPage) { mutableFloatStateOf(0f) }
            LaunchedEffect(item.uniqueId, isCurrentPage, autoAdvanceEnabled, commentReel, shareItemTarget) {
              if (isCurrentPage && item.youtubeVideoId.isBlank() && item.videoUri.isBlank() && autoAdvanceEnabled && items.size > 1) {
                textReelProgress = 0f
                while (textReelProgress < 1f) {
                  delay(70)
                  if (commentReel == null && shareItemTarget == null && !showCreateReel && !showTuningDialog) {
                    textReelProgress += 0.01f
                  }
                }
                viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.FULL_WATCH)
                advanceToNextReel()
              }
            }

            val playerIsActive = isCurrentPage && commentReel == null && shareItemTarget == null && !showCreateReel && !showTuningDialog

            Box(modifier = Modifier.fillMaxSize()) {
              if (item.youtubeVideoId.isNotBlank()) {
                YouTubeEmbedPlayerBox(
                  videoId = item.youtubeVideoId,
                  isActive = playerIsActive,
                  modifier = Modifier.fillMaxSize(),
                  cornerRadius = 0.dp,
                  onVideoEnded = {
                    viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.FULL_WATCH)
                    advanceToNextReel()
                  }
                )
              } else if (item.videoUri.isNotBlank()) {
                VideoPlayerBox(
                  videoUri = item.videoUri,
                  modifier = Modifier.fillMaxSize(),
                  autoPlay = true,
                  isActive = playerIsActive,
                  looping = !autoAdvanceEnabled || items.size <= 1,
                  defaultHeight = null,
                  cornerRadius = 0.dp,
                  showMediaController = false,
                  onVideoEnded = {
                    viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.FULL_WATCH)
                    advanceToNextReel()
                  }
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.linearGradient(
                        listOf(
                          MaterialTheme.colorScheme.primary,
                          Color(0xFF1F1638),
                          MaterialTheme.colorScheme.secondary
                        )
                      )
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                      text = item.caption,
                      color = Color.White,
                      style = MaterialTheme.typography.headlineSmall,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              // Bottom scrim for readability
              Box(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .height(235.dp)
                  .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
              )

              // Right-side vertical FB/Shorts Actions
              Column(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(end = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = {
                      if (item.nativeReel != null) {
                        viewModel.likeReel(item.nativeReel.id, item.category)
                      } else {
                        viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.LIKE)
                      }
                    },
                    modifier = Modifier
                      .size(48.dp)
                      .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                      .testTag("reel_like_button")
                  ) {
                    Icon(
                      Icons.Filled.Favorite,
                      contentDescription = "Like reel",
                      tint = if (isLiked) Color(0xFFFF2D55) else Color(0xFFFF758F)
                    )
                  }
                  Text(
                    text = if (item.nativeReel != null) "${item.likesCount}" else if (isLiked) "Liked" else "Like",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                }

                if (item.nativeReel != null) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                      onClick = { commentReel = item.nativeReel },
                      modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                      Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment on reel", tint = Color.White)
                    }
                    Text("${item.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                  }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = { shareItemTarget = item },
                    modifier = Modifier
                      .size(48.dp)
                      .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                      .testTag("reel_share_button")
                  ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share reel", tint = Color.White)
                  }
                  Text(
                    text = if (item.nativeReel != null) "${item.sharesCount}" else "Share",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                }

                IconButton(
                  onClick = { viewModel.toggleSave(bookmarkKey, "reel", item.authorName, item.caption) },
                  modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                  Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Save reel",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White
                  )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = {
                      viewModel.markNotInterested(item.uniqueId, item.youtubeVideoId, item.category)
                    },
                    modifier = Modifier
                      .size(48.dp)
                      .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                      .testTag("reel_not_interested_button")
                  ) {
                    Icon(
                      Icons.Filled.ThumbDown,
                      contentDescription = "Not Interested",
                      tint = Color(0xFFFCA5A5)
                    )
                  }
                  Text("Hide", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                }

                if (items.size > 1) {
                  IconButton(
                    onClick = {
                      scope.launch { pagerState.animateScrollToPage((page + 1) % items.size) }
                    },
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.16f), CircleShape)
                  ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next reel", tint = Color.White)
                  }
                }
              }

              // Bottom-left Author, Category Tag, Caption & Source pill
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .fillMaxWidth(0.78f)
                  .padding(start = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text(
                      text = "${item.category} · Score ${(feedPrefs.categoryScores[item.category] ?: 10L)}",
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                  if (item.source == "youtube" || item.youtubeVideoId.isNotBlank()) {
                    Surface(
                      color = Color(0xFFDC2626).copy(alpha = 0.9f),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Text(
                        text = "YouTube Embed",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                      if (item.authorId.isNotBlank()) Modifier.clickable { onOpenProfile(item.authorId) } else Modifier
                    )
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                  UserAvatar(name = item.authorName, photoUrl = item.authorAvatar, size = 40.dp)
                  Column {
                    Text(item.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                      text = "Clip ${page + 1} of ${items.size}",
                      color = Color.White.copy(alpha = 0.72f),
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                }
                Text(
                  text = item.caption,
                  color = Color.White,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis
                )
                Surface(
                  color = Color.Black.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(16.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Text(
                      text = item.audioTitle,
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }

              if (item.youtubeVideoId.isBlank() && item.videoUri.isBlank() && autoAdvanceEnabled && items.size > 1) {
                LinearProgressIndicator(
                  progress = { textReelProgress.coerceIn(0f, 1f) },
                  modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                  color = Color.White,
                  trackColor = Color.White.copy(alpha = 0.25f)
                )
              }
            }
          }
        }
      }
    }

    // Floating Top Controls + Dynamic Interest Category Bar over Reels
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Reels", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showTuningDialog = true },
            modifier = Modifier
              .size(34.dp)
              .background(Color.White.copy(alpha = 0.18f), CircleShape)
              .testTag("tune_feed_button")
          ) {
            Icon(Icons.Filled.Tune, contentDescription = "Feed preference scores", tint = Color.White, modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = { viewModel.ensureYouTubeShortsLoaded(forceRefresh = true) },
            modifier = Modifier
              .size(34.dp)
              .background(Color.White.copy(alpha = 0.18f), CircleShape)
              .testTag("refresh_youtube_shorts_button")
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh YouTube Shorts", tint = Color.White, modifier = Modifier.size(18.dp))
          }
          Surface(
            color = if (autoAdvanceEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { autoAdvanceEnabled = !autoAdvanceEnabled }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Filled.Repeat, null, tint = Color.White, modifier = Modifier.size(13.dp))
              Text(if (autoAdvanceEnabled) "Auto" else "Loop", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
          }
          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable(onClick = onOpenStories)
          ) {
            Text("Stories", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
          }
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { showCreateReel = true }.testTag("create_reel_button")
          ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Add, null, tint = Color.Black, modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(2.dp))
              Text("Post", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Dynamic Interest Filter Pills (hides muted/disliked categories automatically in For You)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val activeCats = remember(feedPrefs) {
          FeedPreferences.CATEGORIES.filter { cat ->
            feedPrefs.dislikedCategories.none { it.equals(cat, ignoreCase = true) }
          }.sortedByDescending { feedPrefs.categoryScores[it] ?: 6L }
        }
        Surface(
          color = if (selectedCategory == null) Color.White else Color.White.copy(alpha = 0.16f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.clickable { viewModel.selectReelCategory(null) }
        ) {
          Text(
            text = "✨ For You",
            color = if (selectedCategory == null) Color.Black else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
        activeCats.forEach { cat ->
          val isSel = selectedCategory.equals(cat, ignoreCase = true)
          val score = feedPrefs.categoryScores[cat] ?: 10L
          Surface(
            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.clickable { viewModel.selectReelCategory(cat) }
          ) {
            Text(
              text = "$cat ($score)",
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      if (isUploading || isFetchingShorts) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = if (isUploading) "Uploading reel video to cloud..." else "Fetching fresh YouTube Shorts for your interests...",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
          )
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }

  if (showTuningDialog) {
    AlertDialog(
      onDismissRequest = { showTuningDialog = false },
      title = { Text("Smart Feed Preferences", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = "Private auto-curation tracks views (+1), likes (+3), full watches (+5), and Not Interested (-6) securely under your account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Signals: ${feedPrefs.totalViews} views · ${feedPrefs.totalLikes} likes · ${feedPrefs.totalFullWatches} full watches",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          FeedPreferences.CATEGORIES.forEach { cat ->
            val score = feedPrefs.categoryScores[cat] ?: 10L
            val isMuted = feedPrefs.dislikedCategories.any { it.equals(cat, ignoreCase = true) }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isMuted) "$cat (Hidden · $score)" else "$cat · Score $score",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMuted) FontWeight.Normal else FontWeight.SemiBold
              )
              if (isMuted) {
                TextButton(onClick = { viewModel.restoreCategoryPreference(cat) }) {
                  Text("Restore")
                }
              }
            }
          }
          if (youtubeStatus is YouTubeFetchResult.MissingApiKey) {
            Text(
              text = "Note: Add YOUTUBE_API_KEY in AI Studio Secrets to stream fresh category YouTube Shorts.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        Button(onClick = { showTuningDialog = false }) { Text("Done") }
      },
      dismissButton = {
        TextButton(onClick = {
          viewModel.resetFeedPreferences()
          showTuningDialog = false
        }) { Text("Reset Weights") }
      }
    )
  }

  if (showCreateReel) {
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("Original HELLO Mix") }
    var category by remember { mutableStateOf("Young") }
    var ytLinkInput by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf("") }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) videoUri = uri.toString()
    }
    AlertDialog(
      onDismissRequest = { showCreateReel = false },
      title = { Text("Create Reel") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
          OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Reel caption") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = audioTitle, onValueChange = { audioTitle = it }, label = { Text("Audio track") }, modifier = Modifier.fillMaxWidth())
          Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FeedPreferences.CATEGORIES.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
          OutlinedTextField(
            value = ytLinkInput,
            onValueChange = { ytLinkInput = it },
            label = { Text("YouTube Shorts Link (optional)") },
            placeholder = { Text("https://youtube.com/shorts/...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedButton(
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.VideoLibrary, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (videoUri.isBlank()) "Pick Device Video (Photo Picker)" else "Video Selected ✓")
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.createReel(context, caption, videoUri, audioTitle, "#FF5A5F", category, ytLinkInput)
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

  shareItemTarget?.let { item ->
    val shareLink = if (item.youtubeVideoId.isNotBlank()) "\nhttps://youtube.com/shorts/${item.youtubeVideoId}" else ""
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "${item.category} Reel by ${item.authorName}",
        body = "${item.caption}$shareLink",
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareItemTarget = null },
      onRepostToFeed = { note ->
        val composed = if (note.isBlank()) {
          "Shared ${item.category} Reel by ${item.authorName}: ${item.caption}$shareLink"
        } else {
          "$note\n\n— ${item.category} Reel by ${item.authorName}: ${item.caption}$shareLink"
        }
        viewModel.createPost(context, composed, item.videoUri, if (item.videoUri.isNotBlank()) "video" else "none", "public", emptyList())
        item.nativeReel?.let { viewModel.shareReel(it.id) }
      },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToConversation(conv, sender, msg)
        item.nativeReel?.let { viewModel.shareReel(it.id) }
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToPerson(sender, person, msg)
        item.nativeReel?.let { viewModel.shareReel(it.id) }
      },
      onExternalShareLaunched = { item.nativeReel?.let { viewModel.shareReel(it.id) } }
    )
  }
}
