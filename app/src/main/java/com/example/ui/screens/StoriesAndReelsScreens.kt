package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.media.AudioTrackInfo
import com.example.data.media.AudioTrimmerAndStudio
import com.example.data.model.CuratedFeedItem
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
import com.example.data.model.HELLO_FEED_REGIONS
import com.example.data.model.MusicAttachment
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
import com.example.ui.components.SongPickerAndTrimmerDialog
import com.example.ui.components.StoryMusicPlayerPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.YouTubeEmbedPlayerBox
import com.example.ui.components.formatSongSec
import com.example.ui.components.formatTimestamp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
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
        Icon(Icons.Rounded.Add, null)
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
      is UiState.Error -> EmptyStateCard(Icons.Rounded.Collections, "Stories unavailable", state.message)
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
            icon = Icons.Rounded.Collections,
            title = if (selectedTab == StoryFilterTab.ALL) "No active stories yet" else "No ${selectedTab.label.lowercase()} yet",
            subtitle = "Create a photo, video, or text story for friends or everyone.",
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
                            imageVector = if (story.privacy == "friends") Icons.Rounded.Group else Icons.Rounded.Public,
                            contentDescription = story.privacy,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                          )
                          Text(
                            text = if (isFresh) "24h · ${formatTimestamp(story.createdAt)}" else "24h",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                          )
                        }
                      }
                      if (story.authorId == currentUserId && currentUserId.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          Surface(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = CircleShape,
                            modifier = Modifier
                              .size(28.dp)
                              .clickable { viewModel.toggleStoryHighlight(story) }
                              .testTag("toggle_highlight_card_${story.id}")
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(
                                imageVector = if (story.isSavedHighlight) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (story.isSavedHighlight) "Remove from Highlights" else "Save to Highlights",
                                tint = if (story.isSavedHighlight) Color(0xFFFBBF24) else Color.White,
                                modifier = Modifier.size(16.dp)
                              )
                            }
                          }
                          Surface(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = CircleShape,
                            modifier = Modifier
                              .size(28.dp)
                              .clickable { deleteTarget = story }
                              .testTag("delete_story_card_${story.id}")
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete story", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
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
                          Icon(Icons.Rounded.PlayArrow, contentDescription = "Video story", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                      }
                    }

                    Column(
                      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      if (story.musicTitle.isNotBlank()) {
                        Surface(
                          color = Color(0xFF141416).copy(alpha = 0.84f),
                          shape = RoundedCornerShape(8.dp),
                          border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                          ) {
                            Box(
                              modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.14f)),
                              contentAlignment = Alignment.Center
                            ) {
                              Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                            Column {
                              Text(
                                text = story.musicTitle,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                              )
                              if (story.musicArtist.isNotBlank()) {
                                Text(
                                  text = "♪ ${story.musicArtist}",
                                  color = Color.White.copy(alpha = 0.74f),
                                  style = MaterialTheme.typography.labelSmall,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                                )
                              }
                            }
                          }
                        }
                      }
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
                        Icon(Icons.Rounded.Visibility, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(12.dp))
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
      onCreate = { caption, mediaUri, mediaType, bgHex, privacy, category, ytInput, isSavedHighlight, music ->
        viewModel.createStory(
          context = context,
          caption = caption,
          mediaUri = mediaUri,
          mediaType = mediaType,
          backgroundHex = bgHex,
          privacy = privacy,
          friendUids = friendUids,
          category = category,
          youtubeVideoId = ytInput,
          isSavedHighlight = isSavedHighlight,
          autoDelete24h = !isSavedHighlight,
          music = music
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
  onCreate: (
    caption: String,
    mediaUri: String,
    mediaType: String,
    bgHex: String,
    privacy: String,
    category: String,
    ytInput: String,
    isSavedHighlight: Boolean,
    music: MusicAttachment?
  ) -> Unit
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  var caption by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf("") }
  var mediaType by remember { mutableStateOf("text") }
  var bgHex by remember { mutableStateOf(STORY_THEME_OPTIONS.first().hex) }
  var privacy by remember { mutableStateOf("public") }
  var category by remember { mutableStateOf("Young") }
  var ytInput by remember { mutableStateOf("") }
  var isSavedHighlight by remember { mutableStateOf(false) }
  var pendingCropUri by remember { mutableStateOf<String?>(null) }
  var selectedSong by remember { mutableStateOf<MusicAttachment?>(null) }
  var showSongPicker by remember { mutableStateOf(false) }
  var studioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }

  LaunchedEffect(Unit) {
    studioTracks = AudioTrimmerAndStudio.getBuiltInStudioTracks(context)
  }

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

  val extractedYtId = remember(ytInput) { YouTubeShortsClient.extractVideoId(ytInput) }
  val canShare = caption.isNotBlank() || mediaUri.isNotBlank() || extractedYtId.isNotBlank() || selectedSong != null
  val previewGradient = remember(bgHex) { storyGradientForHex(bgHex) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Surface(
      color = Color(0xFF121316),
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
      ) {
        // Clean Story Creator Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close story creator", tint = Color.White, modifier = Modifier.size(22.dp))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Create Story", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = if (isSavedHighlight) "Profile Highlight + 24h" else "Visible for 24 hours",
              color = Color.White.copy(alpha = 0.65f),
              style = MaterialTheme.typography.labelSmall
            )
          }
          Button(
            onClick = { onCreate(caption, mediaUri, mediaType, bgHex, privacy, category, extractedYtId, isSavedHighlight, selectedSong) },
            enabled = canShare,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = Color.White.copy(alpha = 0.14f)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.testTag("submit_story_button")
          ) {
            Text("Share", color = Color.White, fontWeight = FontWeight.SemiBold)
          }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Story Preview Canvas
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(250.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Brush.linearGradient(previewGradient))
              .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
          ) {
            if (mediaUri.isNotBlank() && mediaType == "image") {
              AsyncImage(
                model = mediaUri,
                contentDescription = "Story photo preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
              )
              Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
            }
            if (mediaUri.isNotBlank() && mediaType == "video") {
              Surface(
                color = Color.Black.copy(alpha = 0.56f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Rounded.PlayCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                  Text("Video", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
            if (mediaUri.isNotBlank()) {
              Row(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (mediaType == "image") {
                  Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = CircleShape,
                    modifier = Modifier
                      .clip(CircleShape)
                      .clickable { pendingCropUri = mediaUri }
                      .testTag("story_crop_studio_button")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Rounded.Crop, contentDescription = "Edit photo", tint = Color.White, modifier = Modifier.size(14.dp))
                      Text("Edit", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                  }
                }
                Surface(
                  color = Color.Black.copy(alpha = 0.62f),
                  shape = CircleShape,
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                      mediaUri = ""
                      mediaType = "text"
                    }
                    .testTag("clear_story_media_button")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove media", tint = Color.White, modifier = Modifier.size(15.dp))
                  }
                }
              }
            }
            Text(
              text = caption.ifBlank { if (mediaUri.isBlank()) "Start typing..." else "" },
              color = Color.White.copy(alpha = if (caption.isBlank()) 0.6f else 1f),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Clean Frosted Music Sticker Pill on Canvas
            selectedSong?.let { song ->
              val clipLen = (song.endSec - song.startSec).coerceAtLeast(1L)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.56f),
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(10.dp)
                  .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                  .clickable { showSongPicker = true }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                  Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(15.dp))
                  Text(
                    text = "${song.title} · ${formatSongSec(song.startSec.toInt())}-${formatSongSec(song.endSec.toInt())} (${clipLen}s)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                  Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove song",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                      .size(15.dp)
                      .clickable { selectedSong = null }
                  )
                }
              }
            }
          }

          // Compact Real Action Pills (Photo, Music, Video)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.White.copy(alpha = 0.09f),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .testTag("pick_story_photo_button")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Rounded.Image, contentDescription = strings.photo, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.photo, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selectedSong != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.09f),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { showSongPicker = true }
                .testTag("story_add_song_button")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (selectedSong == null) Icons.Rounded.MusicNote else Icons.Rounded.ContentCut,
                  contentDescription = strings.song,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = if (selectedSong == null) strings.song else "Trim",
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.White.copy(alpha = 0.09f),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                .testTag("pick_story_video_button")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Rounded.Videocam, contentDescription = strings.video, tint = Color(0xFFFB7185), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.video, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
              }
            }
          }

          // Compact Quick Soundtrack Bar
          if (studioTracks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(15.dp))
                  Text("Quick Soundtracks", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                  text = if (selectedSong == null) "Browse all" else "Change / Device",
                  color = Color.White.copy(alpha = 0.75f),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showSongPicker = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                studioTracks.forEach { track ->
                  val isPicked = selectedSong?.title == track.title
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPicked) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                      0.8.dp,
                      if (isPicked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        val maxSec = (track.durationMs / 1000L).coerceAtLeast(3L)
                        selectedSong = MusicAttachment(
                          title = track.title,
                          artist = track.artist,
                          audioUri = track.uriString,
                          startSec = 0L,
                          endSec = minOf(20L, maxSec)
                        )
                      }
                      .testTag("story_quick_track_${track.id}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                      Icon(
                        imageVector = if (isPicked) Icons.Rounded.Check else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                      )
                      Column {
                        Text(track.title, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(track.artist, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                      }
                    }
                  }
                }
              }
            }
          }

          // Background Theme Swatches
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Background", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              STORY_THEME_OPTIONS.forEach { option ->
                val selected = bgHex.equals(option.hex, ignoreCase = true)
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(option.colors))
                    .border(
                      width = if (selected) 2.dp else 0.8.dp,
                      color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                      shape = CircleShape
                    )
                    .clickable { bgHex = option.hex }
                    .testTag("story_theme_${option.label.lowercase()}"),
                  contentAlignment = Alignment.Center
                ) {
                  if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = option.label, tint = Color.White, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }

          OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(500) },
            label = { Text("Story caption", color = Color.White.copy(alpha = 0.7f)) },
            placeholder = { Text("Add text...", color = Color.White.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth().testTag("story_caption_input"),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color.White.copy(alpha = 0.55f),
              unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
              cursorColor = Color.White
            )
          )

          // Privacy Selector
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (privacy == "public") MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(0.8.dp, if (privacy == "public") MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                .clickable { privacy = "public" }
                .testTag("story_privacy_public")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(Icons.Rounded.Public, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Public", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
              }
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (privacy == "friends") MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(0.8.dp, if (privacy == "friends") MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                .clickable { privacy = "friends" }
                .testTag("story_privacy_friends")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(Icons.Rounded.Group, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Friends ($friendCount)", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          // Story Category Tag
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Category", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FeedPreferences.CATEGORIES.forEach { cat ->
                val selected = category == cat
                Surface(
                  shape = CircleShape,
                  color = if (selected) Color.White else Color.White.copy(alpha = 0.1f),
                  modifier = Modifier.clip(CircleShape).clickable { category = cat }
                ) {
                  Text(
                    cat,
                    color = if (selected) Color(0xFF121316) else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = ytInput,
            onValueChange = { ytInput = it },
            label = { Text("Optional YouTube Short Link", color = Color.White.copy(alpha = 0.7f)) },
            placeholder = { Text("https://youtube.com/shorts/...", color = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color.White.copy(alpha = 0.55f),
              unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
              cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { isSavedHighlight = !isSavedHighlight }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                  "Save to Profile Highlights",
                  color = Color.White,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  "Keep on Profile after 24h",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.65f)
                )
              }
              Switch(
                checked = isSavedHighlight,
                onCheckedChange = { isSavedHighlight = it },
                modifier = Modifier.testTag("story_save_highlight_switch")
              )
            }
          }
        }
      }
    }
  }

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

  if (showSongPicker) {
    SongPickerAndTrimmerDialog(
      initialSelection = selectedSong,
      onDismiss = { showSongPicker = false },
      onSongAttached = { song ->
        selectedSong = song
        showSongPicker = false
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
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
          contentScale = ContentScale.Fit
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
                  imageVector = if (activeStory.privacy == "friends") Icons.Rounded.Group else Icons.Rounded.Public,
                  contentDescription = activeStory.privacy,
                  tint = Color.White.copy(alpha = 0.8f),
                  modifier = Modifier.size(13.dp)
                )
              }
              val effectiveHighlight = remember(activeStory.id, activeStory.isSavedHighlight) {
                mutableStateOf(activeStory.isSavedHighlight)
              }
              Text(
                text = "${if (effectiveHighlight.value) "Saved on Profile ★" else "24h"} · ${formatTimestamp(activeStory.createdAt)} · ${activeStory.viewersCount} views" + if (shouldPauseTimer) " · Paused" else "",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall
              )
            }
          }
          IconButton(onClick = { isPaused = !isPaused }) {
            Icon(if (shouldPauseTimer) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = "Pause story", tint = Color.White)
          }
          if (activeStory.authorId != currentUserId) {
            IconButton(
              onClick = {
                viewModel.markNotInterested("story_${activeStory.id}", activeStory.youtubeVideoId, activeStory.category)
                goToNextStory()
              },
              modifier = Modifier.testTag("story_not_interested_button")
            ) {
              Icon(Icons.Rounded.ThumbDown, contentDescription = "Not interested in story", tint = Color.White)
            }
          }
          if (activeStory.authorId == currentUserId && currentUserId.isNotBlank()) {
            var isHighlightLocal by remember(activeStory.id, activeStory.isSavedHighlight) {
              mutableStateOf(activeStory.isSavedHighlight)
            }
            IconButton(
              onClick = {
                isHighlightLocal = !isHighlightLocal
                viewModel.toggleStoryHighlight(activeStory.copy(isSavedHighlight = !isHighlightLocal))
              },
              modifier = Modifier.testTag("story_highlight_toggle_button")
            ) {
              Icon(
                imageVector = if (isHighlightLocal) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (isHighlightLocal) "Remove from Highlights" else "Save to Highlights",
                tint = if (isHighlightLocal) Color(0xFFFBBF24) else Color.White
              )
            }
            IconButton(
              onClick = { showDeleteConfirm = true },
              modifier = Modifier.testTag("story_delete_button")
            ) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete story", tint = Color.White)
            }
          }
          IconButton(onClick = { showShareSheet = true }, modifier = Modifier.testTag("story_share_button")) {
            Icon(Icons.Rounded.Share, contentDescription = "Share story", tint = Color.White)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close story", tint = Color.White)
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

      // Bottom Caption + Story Music + Comments Drawer + Quick Reactions + Comment Input
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (activeStory.musicTitle.isNotBlank() || activeStory.musicUri.isNotBlank()) {
          StoryMusicPlayerPill(
            storyId = activeStory.id,
            title = activeStory.musicTitle,
            artist = activeStory.musicArtist,
            audioUri = activeStory.musicUri,
            startSec = activeStory.musicStartSec,
            endSec = activeStory.musicEndSec,
            shouldPause = shouldPauseTimer,
            modifier = Modifier.align(Alignment.Start)
          )
        }
        if (activeStory.caption.isNotBlank() && activeStory.mediaType != "text") {
          Surface(color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(12.dp)) {
            Text(activeStory.caption, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
          }
        }

        AnimatedVisibility(visible = showCommentsList) {
          Surface(
            color = Color(0xFF0E1320).copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 380.dp)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "All Comments (${textComments.size}) · ${sortedReactions.size} total",
                  color = Color.White,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showCommentsList = false }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Rounded.Close, contentDescription = "Close comments", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
              }
              HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
              if (sortedReactions.isEmpty()) {
                Text(
                  text = "No comments or reactions yet. Be the first to reply!",
                  color = Color.White.copy(alpha = 0.8f),
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(14.dp)
                )
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                  contentPadding = PaddingValues(12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  items(sortedReactions, key = { it.id }) { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Text(r.emoji, style = MaterialTheme.typography.titleMedium)
                      Column(modifier = Modifier.weight(1f)) {
                        Text(r.userName.ifBlank { "Member" }, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (r.replyText.isNotBlank()) {
                          Text(r.replyText, color = Color.White.copy(alpha = 0.94f), style = MaterialTheme.typography.bodySmall)
                        }
                      }
                      Text(formatTimestamp(r.createdAt), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
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
              Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send story comment", tint = Color.White)
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
private fun ReelsRailAction(
  onClick: () -> Unit,
  contentDescription: String,
  label: String? = null,
  containerTint: Color = Color.Black.copy(alpha = 0.24f),
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
    modifier = modifier
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(containerTint)
    ) {
      icon()
    }
    if (!label.isNullOrBlank()) {
      Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.72f),
            offset = Offset(0f, 1.5f),
            blurRadius = 6f
          )
        ),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun ReelsTopActionButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  containerColor: Color = Color.White.copy(alpha = 0.14f),
  icon: @Composable () -> Unit
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.size(48.dp)
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(containerColor),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
  }
}

@Composable
private fun ReelsScrubProgressBar(
  progressFraction: Float,
  durationSec: Float,
  positionSec: Float,
  onScrubStart: () -> Unit,
  onScrubFractionChange: (Float) -> Unit,
  onScrubCommit: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var isDragging by remember { mutableStateOf(false) }
  var localDragFraction by remember { mutableFloatStateOf(progressFraction) }
  val displayFraction = (if (isDragging) localDragFraction else progressFraction).coerceIn(0f, 1f)
  val density = LocalDensity.current

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    AnimatedVisibility(
      visible = isDragging && durationSec > 0f,
      enter = fadeIn() + scaleIn(initialScale = 0.9f),
      exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
      val targetSec = (displayFraction * durationSec).toInt()
      val totalSec = durationSec.toInt().coerceAtLeast(1)
      Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xCC0D1018),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        Text(
          text = "${formatReelTime(targetSec)} / ${formatReelTime(totalSec)}",
          color = Color.White,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
      }
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .height(22.dp)
        .testTag("reel_progress_bar")
        .pointerInput(durationSec) {
          detectTapGestures { offset ->
            val w = size.width.toFloat().coerceAtLeast(1f)
            val f = (offset.x / w).coerceIn(0f, 1f)
            onScrubCommit(f)
          }
        }
        .pointerInput(durationSec) {
          detectHorizontalDragGestures(
            onDragStart = { offset ->
              isDragging = true
              val w = size.width.toFloat().coerceAtLeast(1f)
              val f = (offset.x / w).coerceIn(0f, 1f)
              localDragFraction = f
              onScrubStart()
              onScrubFractionChange(f)
            },
            onHorizontalDrag = { change, _ ->
              change.consume()
              val w = size.width.toFloat().coerceAtLeast(1f)
              val f = (change.position.x / w).coerceIn(0f, 1f)
              localDragFraction = f
              onScrubFractionChange(f)
            },
            onDragEnd = {
              isDragging = false
              onScrubCommit(localDragFraction)
            },
            onDragCancel = {
              isDragging = false
              onScrubCommit(localDragFraction)
            }
          )
        },
      contentAlignment = Alignment.BottomCenter
    ) {
      val barWidthPx = with(density) { maxWidth.toPx() }
      val trackHeight = if (isDragging) 5.dp else 2.5.dp
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(trackHeight)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.White.copy(alpha = 0.24f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(displayFraction)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.92f))
        )
      }
      if (isDragging) {
        val thumbSize = 12.dp
        val thumbOffsetX = with(density) {
          ((barWidthPx * displayFraction) - (thumbSize.toPx() / 2f))
            .coerceIn(0f, (barWidthPx - thumbSize.toPx()).coerceAtLeast(0f))
            .toDp()
        }
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = thumbOffsetX, y = -(trackHeight / 2) + (thumbSize / 2))
            .size(thumbSize)
            .clip(CircleShape)
            .background(Color.White)
        )
      }
    }
  }
}

private fun formatReelTime(totalSec: Int): String {
  val m = (totalSec / 60).coerceAtLeast(0)
  val s = (totalSec % 60).coerceAtLeast(0)
  return "%d:%02d".format(m, s)
}

@Composable
fun ReelsScreen(
  viewModel: MainSocialViewModel,
  communityVm: CommunityViewModel? = null,
  me: PublicUser? = null,
  currentUserId: String = "",
  onOpenStories: () -> Unit,
  onOpenProfile: (String) -> Unit = {},
  onOpenSettings: () -> Unit = {}
) {
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val curatedState by viewModel.curatedReels.collectAsStateWithLifecycle()
  val feedPrefs by viewModel.feedPreferences.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedReelCategory.collectAsStateWithLifecycle()
  val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
  val autoAdvanceEnabled by viewModel.reelsAutoAdvance.collectAsStateWithLifecycle()
  val youtubeStatus by viewModel.youtubeStatus.collectAsStateWithLifecycle()
  val isFetchingShorts by viewModel.isFetchingShorts.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
  val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
  val localLikedIds by viewModel.localLikedItemIds.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }

  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

  var showCreateReel by remember { mutableStateOf(false) }
  var showCategoryMenu by remember { mutableStateOf(false) }
  var commentReel by remember { mutableStateOf<Reel?>(null) }
  var shareItemTarget by remember { mutableStateOf<CuratedFeedItem?>(null) }
  var deleteReelTarget by remember { mutableStateOf<Reel?>(null) }
  val effectiveCurrentUid = remember(currentUserId, me?.userId) {
    currentUserId.ifBlank { me?.userId.orEmpty() }
  }

  val textShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.72f),
      offset = Offset(0f, 1.5f),
      blurRadius = 8f
    )
  }

  LaunchedEffect(Unit) {
    viewModel.syncRegionalAnalytics()
    viewModel.ensureYouTubeShortsLoaded()
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07090E))) {
    when (val state = curatedState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
      }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        EmptyStateCard(Icons.Rounded.PlayCircle, "Reels unavailable", state.message)
      }
      is UiState.Success -> {
        val items = state.data
        if (items.isEmpty()) {
          Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
              icon = Icons.Rounded.PlayCircle,
              title = "No Reels yet",
              subtitle = when (val yt = youtubeStatus) {
                is YouTubeFetchResult.Error ->
                  "Couldn't load YouTube clips right now (${yt.message}). Post your own Reel or check your connection."
                else ->
                  "Share a short video from your phone or paste a YouTube Shorts link to start the Reels feed."
              },
              actionLabel = "Post a Reel",
              onAction = { showCreateReel = true }
            )
          }
        } else {
          val pagerState = rememberPagerState(pageCount = { items.size })

          LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
              .drop(1)
              .collect {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
          }

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

            var heartBurstNonce by remember(item.uniqueId) { mutableIntStateOf(0) }
            var showHeartBurst by remember(item.uniqueId) { mutableStateOf(false) }
            var showMoreMenu by remember(item.uniqueId) { mutableStateOf(false) }

            var isCaptionExpanded by remember(item.uniqueId) { mutableStateOf(false) }
            var isCaptionTruncated by remember(item.uniqueId) { mutableStateOf(false) }

            var durationSec by remember(item.uniqueId) {
              mutableFloatStateOf(if (item.youtubeVideoId.isBlank() && item.videoUri.isBlank()) 10f else 0f)
            }
            var positionSec by remember(item.uniqueId) { mutableFloatStateOf(0f) }
            var isScrubbing by remember(item.uniqueId) { mutableStateOf(false) }
            var scrubFraction by remember(item.uniqueId) { mutableFloatStateOf(0f) }
            var seekNonce by remember(item.uniqueId) { mutableIntStateOf(0) }
            var ytSeekRequest by remember(item.uniqueId) { mutableStateOf<Pair<Int, Float>?>(null) }
            var videoSeekRequestMs by remember(item.uniqueId) { mutableStateOf<Pair<Int, Long>?>(null) }

            fun advanceToNextReel() {
              if (autoAdvanceEnabled && items.size > 1) {
                val next = (page + 1) % items.size
                scope.launch {
                  pagerState.animateScrollToPage(next)
                }
              }
            }

            fun triggerLike(forceLikeOnly: Boolean) {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              if (forceLikeOnly) {
                heartBurstNonce++
                showHeartBurst = true
                if (!isLiked) {
                  if (item.nativeReel != null) {
                    viewModel.likeReel(item.nativeReel.id, item.category)
                  } else {
                    viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.LIKE)
                  }
                }
              } else {
                if (item.nativeReel != null) {
                  viewModel.likeReel(item.nativeReel.id, item.category)
                } else {
                  viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.LIKE)
                }
              }
            }

            LaunchedEffect(heartBurstNonce) {
              if (heartBurstNonce > 0) {
                delay(780L)
                showHeartBurst = false
              }
            }

            LaunchedEffect(item.uniqueId, isCurrentPage) {
              if (isCurrentPage) {
                delay(2000)
                viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.VIEW)
              }
            }

            var textReelProgress by remember(item.uniqueId, isCurrentPage) { mutableFloatStateOf(0f) }
            LaunchedEffect(item.uniqueId, isCurrentPage, autoAdvanceEnabled, commentReel, shareItemTarget, showCreateReel, deleteReelTarget) {
              if (isCurrentPage && item.youtubeVideoId.isBlank() && item.videoUri.isBlank() && autoAdvanceEnabled && items.size > 1) {
                textReelProgress = 0f
                durationSec = 10f
                while (textReelProgress < 1f) {
                  delay(70)
                  if (!isScrubbing && commentReel == null && shareItemTarget == null && !showCreateReel && deleteReelTarget == null) {
                    textReelProgress = (textReelProgress + 0.01f).coerceAtMost(1f)
                    positionSec = textReelProgress * 10f
                  }
                }
                viewModel.trackEngagement(item.uniqueId, item.category, EngagementSignal.FULL_WATCH)
                advanceToNextReel()
              }
            }

            val playerIsActive = isCurrentPage && commentReel == null && shareItemTarget == null && !showCreateReel && deleteReelTarget == null
            val ownNativeReel = item.nativeReel?.takeIf {
              effectiveCurrentUid.isNotBlank() && (it.authorId == effectiveCurrentUid || item.authorId == effectiveCurrentUid)
            }

            Box(modifier = Modifier.fillMaxSize()) {
              if (item.youtubeVideoId.isNotBlank()) {
                YouTubeEmbedPlayerBox(
                  videoId = item.youtubeVideoId,
                  isActive = playerIsActive,
                  modifier = Modifier.fillMaxSize(),
                  cornerRadius = 0.dp,
                  showControls = false,
                  seekRequestSec = ytSeekRequest,
                  onProgressUpdate = { cur, dur ->
                    if (!isScrubbing) {
                      if (dur > 0f) durationSec = dur
                      positionSec = cur.coerceAtLeast(0f)
                    }
                  },
                  onDoubleTap = { triggerLike(forceLikeOnly = true) },
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
                  seekRequestMs = videoSeekRequestMs,
                  onProgressUpdate = { posMs, durMs ->
                    if (!isScrubbing) {
                      if (durMs > 0L) durationSec = durMs / 1000f
                      positionSec = posMs / 1000f
                    }
                  },
                  onDoubleTap = { triggerLike(forceLikeOnly = true) },
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
                          Color(0xFF0E1324),
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                          Color(0xFF141129)
                        )
                      )
                    )
                    .pointerInput(item.uniqueId) {
                      detectTapGestures(
                        onDoubleTap = { triggerLike(forceLikeOnly = true) }
                      )
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                  ) {
                    Icon(
                      Icons.Rounded.PlayCircle,
                      contentDescription = null,
                      tint = Color.White.copy(alpha = 0.88f),
                      modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                      text = item.caption,
                      color = Color.White,
                      style = MaterialTheme.typography.headlineSmall,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }

              // Soft bottom cinema scrim for contrast without boxed cards
              Box(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .height(230.dp)
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.36f),
                        Color.Black.copy(alpha = 0.86f)
                      )
                    )
                  )
              )

              // Double-tap animated floating Heart
              AnimatedVisibility(
                visible = showHeartBurst,
                enter = scaleIn(
                  initialScale = 0.3f,
                  animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                  )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 1.28f) + fadeOut(),
                modifier = Modifier
                  .align(Alignment.Center)
                  .testTag("reel_double_tap_heart")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Box(
                    modifier = Modifier
                      .size(120.dp)
                      .clip(CircleShape)
                      .background(
                        Brush.radialGradient(
                          listOf(Color(0xFFFF2D55).copy(alpha = 0.42f), Color.Transparent)
                        )
                      )
                  )
                  Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Double tap heart",
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(92.dp)
                  )
                }
              }

              // Sleek minimalist Right-Side Action Rail
              Column(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(end = 10.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                ReelsRailAction(
                  onClick = { triggerLike(forceLikeOnly = false) },
                  contentDescription = "Like reel",
                  label = if (item.nativeReel != null) "${item.likesCount}" else if (isLiked) "Liked" else "Like",
                  containerTint = if (isLiked) Color(0xFFFF2D55).copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.24f),
                  modifier = Modifier.testTag("reel_like_button")
                ) {
                  Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like reel",
                    tint = if (isLiked) Color(0xFFFF3B6B) else Color.White,
                    modifier = Modifier.size(26.dp)
                  )
                }

                if (item.nativeReel != null) {
                  ReelsRailAction(
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      commentReel = item.nativeReel
                    },
                    contentDescription = "Comment on reel",
                    label = "${item.commentsCount}"
                  ) {
                    Icon(
                      Icons.AutoMirrored.Rounded.Comment,
                      contentDescription = "Comment on reel",
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }

                ReelsRailAction(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    shareItemTarget = item
                  },
                  contentDescription = "Share reel",
                  label = if (item.nativeReel != null) "${item.sharesCount}" else "Share",
                  modifier = Modifier.testTag("reel_share_button")
                ) {
                  Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Share reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }

                ReelsRailAction(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleSave(bookmarkKey, "reel", item.authorName, item.caption)
                  },
                  contentDescription = "Save reel",
                  label = if (isSaved) "Saved" else null
                ) {
                  Icon(
                    if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Save reel",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }

                if (ownNativeReel != null) {
                  ReelsRailAction(
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                      deleteReelTarget = ownNativeReel
                    },
                    contentDescription = "Delete reel",
                    label = "Delete",
                    containerTint = Color(0xFFD32F2F).copy(alpha = 0.48f),
                    modifier = Modifier.testTag("reel_rail_delete_button")
                  ) {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete reel",
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }

                Box {
                  ReelsRailAction(
                    onClick = { showMoreMenu = true },
                    contentDescription = "More reel options",
                    modifier = Modifier.testTag("reel_more_button")
                  ) {
                    Icon(
                      Icons.Rounded.MoreHoriz,
                      contentDescription = "More reel options",
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                  ) {
                    DropdownMenuItem(
                      text = { Text("Not interested") },
                      leadingIcon = { Icon(Icons.Rounded.NotInterested, contentDescription = null) },
                      onClick = {
                        showMoreMenu = false
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.markNotInterested(item.uniqueId, item.youtubeVideoId, item.category)
                      },
                      modifier = Modifier.testTag("reel_not_interested_button")
                    )
                    DropdownMenuItem(
                      text = { Text("View Stories") },
                      leadingIcon = { Icon(Icons.Rounded.Collections, contentDescription = null) },
                      onClick = {
                        showMoreMenu = false
                        onOpenStories()
                      }
                    )
                    DropdownMenuItem(
                      text = { Text("Reels & Feed Settings") },
                      leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                      onClick = {
                        showMoreMenu = false
                        onOpenSettings()
                      }
                    )
                    if (ownNativeReel != null) {
                      DropdownMenuItem(
                        text = { Text("Delete Reel", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                          showMoreMenu = false
                          deleteReelTarget = ownNativeReel
                        },
                        modifier = Modifier.testTag("reel_delete_button")
                      )
                    }
                  }
                }
              }

              // Clean Minimal Bottom-Left Creator Info, Caption & Audio Footer (no glass card boxes or score soup)
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .fillMaxWidth(0.78f)
                  .padding(start = 16.dp, bottom = 26.dp)
                  .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val followTargetId = remember(item.authorId, item.authorName) {
                  item.authorId.ifBlank {
                    "yt_" + item.authorName.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(40).trim('_').ifBlank { "creator" }
                  }
                }
                val isOwnReel = effectiveCurrentUid.isNotBlank() && item.authorId == effectiveCurrentUid
                val isFollowingCreator = followTargetId in followingIds

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                      .weight(1f, fill = false)
                      .clip(RoundedCornerShape(12.dp))
                      .then(
                        if (item.authorId.isNotBlank()) Modifier.clickable { onOpenProfile(item.authorId) } else Modifier
                      )
                  ) {
                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.72f), CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      UserAvatar(name = item.authorName, photoUrl = item.authorAvatar, size = 36.dp)
                    }
                    Text(
                      text = item.authorName,
                      color = Color.White,
                      style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  if (!isOwnReel && followTargetId.isNotBlank()) {
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = if (isFollowingCreator) Color.White.copy(alpha = 0.16f) else Color.White,
                      border = if (isFollowingCreator) BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)) else null,
                      modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          viewModel.toggleFollowCreator(followTargetId, item.authorName)
                        }
                        .testTag("reel_follow_button")
                    ) {
                      Text(
                        text = if (isFollowingCreator) "Following" else "Follow",
                        color = if (isFollowingCreator) Color.White else Color.Black,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                      )
                    }
                  }
                }

                if (item.caption.isNotBlank()) {
                  val captionScrollState = rememberScrollState()
                  Text(
                    text = item.caption,
                    color = Color.White.copy(alpha = 0.96f),
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                    maxLines = if (isCaptionExpanded) 12 else 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout ->
                      if (!isCaptionExpanded) {
                        isCaptionTruncated = layout.hasVisualOverflow
                      }
                    },
                    modifier = if (isCaptionExpanded) {
                      Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .verticalScroll(captionScrollState)
                    } else {
                      Modifier.fillMaxWidth()
                    }
                  )
                  if (isCaptionTruncated || isCaptionExpanded) {
                    Text(
                      text = if (isCaptionExpanded) "Show less" else "more",
                      color = Color.White.copy(alpha = 0.78f),
                      style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          isCaptionExpanded = !isCaptionExpanded
                        }
                        .padding(vertical = 2.dp)
                        .testTag("reel_caption_more_button")
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.84f)
                  )
                  Text(
                    text = "${item.audioTitle} · ${item.category}",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              // Sleek Bottom Scrubbable Progress Bar
              val currentProgress = when {
                isScrubbing -> scrubFraction
                durationSec > 0f -> (positionSec / durationSec).coerceIn(0f, 1f)
                item.youtubeVideoId.isBlank() && item.videoUri.isBlank() -> textReelProgress.coerceIn(0f, 1f)
                else -> 0f
              }
              ReelsScrubProgressBar(
                progressFraction = currentProgress,
                durationSec = if (durationSec > 0f) durationSec else 30f,
                positionSec = positionSec,
                onScrubStart = {
                  isScrubbing = true
                  scrubFraction = currentProgress
                },
                onScrubFractionChange = { nextFrac ->
                  scrubFraction = nextFrac
                },
                onScrubCommit = { targetFrac ->
                  isScrubbing = false
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  val effDuration = if (durationSec > 0f) durationSec else 30f
                  val targetSec = (targetFrac * effDuration).coerceIn(0f, effDuration)
                  positionSec = targetSec
                  seekNonce++
                  if (item.youtubeVideoId.isNotBlank()) {
                    ytSeekRequest = seekNonce to targetSec
                  } else if (item.videoUri.isNotBlank()) {
                    videoSeekRequestMs = seekNonce to (targetSec * 1000f).toLong()
                  } else {
                    textReelProgress = targetFrac.coerceIn(0f, 1f)
                  }
                },
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    // Minimalist Floating Top Header (All tuning/region/loop settings moved cleanly to Settings)
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.56f), Color.Transparent)))
        .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Reels",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(shadow = textShadow),
            fontWeight = FontWeight.Bold
          )

          Box {
            Surface(
              shape = RoundedCornerShape(50),
              color = Color.White.copy(alpha = 0.14f),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { showCategoryMenu = true }
                .testTag("reels_category_dropdown")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = selectedCategory ?: "For You",
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Icon(
                  Icons.Rounded.KeyboardArrowDown,
                  contentDescription = "Choose category",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            val activeCats = remember(feedPrefs) {
              FeedPreferences.CATEGORIES.filter { cat ->
                feedPrefs.dislikedCategories.none { it.equals(cat, ignoreCase = true) }
              }.sortedByDescending { feedPrefs.categoryScores[it] ?: 6L }
            }
            DropdownMenu(
              expanded = showCategoryMenu,
              onDismissRequest = { showCategoryMenu = false }
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    "For You",
                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                  )
                },
                trailingIcon = if (selectedCategory == null) {
                  { Icon(Icons.Rounded.Check, contentDescription = "Selected") }
                } else null,
                onClick = {
                  showCategoryMenu = false
                  viewModel.selectReelCategory(null)
                }
              )
              activeCats.forEach { cat ->
                val isSel = selectedCategory.equals(cat, ignoreCase = true)
                DropdownMenuItem(
                  text = { Text(cat, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  trailingIcon = if (isSel) {
                    { Icon(Icons.Rounded.Check, contentDescription = "Selected") }
                  } else null,
                  onClick = {
                    showCategoryMenu = false
                    viewModel.selectReelCategory(cat)
                  }
                )
              }
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ReelsTopActionButton(
            onClick = { showCreateReel = true },
            contentDescription = "Create Reel",
            containerColor = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.testTag("create_reel_button")
          ) {
            Icon(
              Icons.Rounded.Add,
              contentDescription = "Create Reel",
              tint = Color.Black,
              modifier = Modifier.size(20.dp)
            )
          }
          ReelsTopActionButton(
            onClick = onOpenSettings,
            contentDescription = "Reels settings",
            modifier = Modifier.testTag("reels_settings_button")
          ) {
            Icon(
              Icons.Rounded.Settings,
              contentDescription = "Reels settings",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      if (isUploading || isFetchingShorts) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(2.dp)),
          color = Color.White,
          trackColor = Color.White.copy(alpha = 0.22f)
        )
      }
    }
  }

  if (showCreateReel) {
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("Original HELLO Mix") }
    var category by remember { mutableStateOf("Young") }
    var reelRegion by remember(selectedRegion) { mutableStateOf(selectedRegion) }
    var ytLinkInput by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf("") }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) videoUri = uri.toString()
    }
    val canPostReel = caption.isNotBlank() || videoUri.isNotBlank() || ytLinkInput.isNotBlank()
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
          Text("Audience / Region", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HELLO_FEED_REGIONS.forEach { reg ->
              FilterChip(
                selected = reelRegion.equals(reg.code, ignoreCase = true),
                onClick = { reelRegion = reg.code },
                label = { Text("${reg.flag} ${if (reg.isGeneral) "Global" else reg.code}", style = MaterialTheme.typography.labelSmall) }
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
            Icon(Icons.Rounded.Videocam, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (videoUri.isBlank()) "Pick Device Video (Photo Picker)" else "Video Selected ✓")
          }
        }
      },
      confirmButton = {
        Button(
          enabled = canPostReel,
          onClick = {
            viewModel.createReel(
              context = context,
              caption = caption,
              videoUri = videoUri,
              audioTitle = audioTitle,
              coverHex = "#FF5A5F",
              category = category,
              youtubeUrlOrId = ytLinkInput,
              regionCode = reelRegion,
              autoDelete24h = false
            )
            showCreateReel = false
          }
        ) { Text("Post Reel") }
      },
      dismissButton = { TextButton(onClick = { showCreateReel = false }) { Text("Cancel") } }
    )
  }

  commentReel?.let { reel ->
    val comments by viewModel.observeReelComments(reel.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      onDismiss = { commentReel = null },
      onSendComment = { text, _ ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.addReelComment(reel.id, text)
      }
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

  deleteReelTarget?.let { reel ->
    AlertDialog(
      onDismissRequest = { deleteReelTarget = null },
      title = { Text("Delete Reel?") },
      text = { Text("Are you sure you want to delete this reel? It will be permanently removed for everyone.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteReel(reel.id)
            deleteReelTarget = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("confirm_delete_reel_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteReelTarget = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
