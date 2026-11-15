package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
          if (mediaType == "image") {
            AsyncImage(
              model = mediaUri,
              contentDescription = "Story photo preview",
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
            OutlinedButton(
              onClick = { pendingCropUri = mediaUri },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Crop Photo")
            }
          } else {
            Text("Media attached ($mediaType) ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    },
    confirmButton = { Button(onClick = { onCreate(caption, mediaUri, mediaType, bgHex) }) { Text("Share Story") } },
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
  var isInputFocused by remember { mutableStateOf(false) }
  var showCommentsList by remember { mutableStateOf(false) }
  var showShareSheet by remember { mutableStateOf(false) }
  val activeStory = stories.getOrNull(currentIndex) ?: initialStory

  val shouldPauseTimer = isPaused || isInputFocused || replyText.isNotEmpty() || showCommentsList || showShareSheet

  LaunchedEffect(activeStory.id) {
    viewModel.markStoryViewed(activeStory.id)
    progress = 0f
    while (progress < 1f) {
      delay(80)
      if (!shouldPauseTimer) {
        progress += 0.015f
      }
    }
    if (currentIndex < stories.lastIndex) {
      currentIndex++
    } else {
      onDismiss()
    }
  }

  val reactions by viewModel.observeStoryReactions(activeStory.id).collectAsStateWithLifecycle(initialValue = emptyList())
  val sortedReactions = remember(reactions) {
    reactions.sortedByDescending { it.createdAt?.seconds ?: 0L }
  }
  val textComments = remember(sortedReactions) {
    sortedReactions.filter { it.replyText.isNotBlank() }
  }

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
      if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "image") {
        AsyncImage(
          model = activeStory.mediaUri,
          contentDescription = activeStory.caption,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
          contentScale = ContentScale.Crop
        )
      } else if (activeStory.mediaUri.isNotBlank() && activeStory.mediaType == "video") {
        Box(modifier = Modifier.align(Alignment.Center)) {
          VideoPlayerBox(videoUri = activeStory.mediaUri, autoPlay = true, isActive = !showCommentsList && !showShareSheet)
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

      // Subtle dark scrim at bottom for high contrast comments & composer
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(260.dp)
          .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
      )

      // Top Progress + Author Bar
      Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp)) {
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
          color = Color.White,
          trackColor = Color.White.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          UserAvatar(name = activeStory.authorName, photoUrl = activeStory.authorAvatar, size = 38.dp)
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(activeStory.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
              text = "${formatTimestamp(activeStory.createdAt)} · ${activeStory.viewersCount} views" + if (shouldPauseTimer) " · Paused" else "",
              color = Color.White.copy(alpha = 0.8f),
              style = MaterialTheme.typography.labelSmall
            )
          }
          IconButton(onClick = { isPaused = !isPaused }) {
            Icon(if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = "Pause story", tint = Color.White)
          }
          IconButton(onClick = { showShareSheet = true }, modifier = Modifier.testTag("story_share_button")) {
            Icon(Icons.Filled.Share, contentDescription = "Share story", tint = Color.White)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
          }
        }
      }

      // Tap areas for prev/next — kept strictly above bottom composer & comments area
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 88.dp, bottom = 240.dp)
      ) {
        Box(modifier = Modifier.weight(1f).fillMaxSize().clickable { if (currentIndex > 0) currentIndex-- })
        Box(modifier = Modifier.weight(1f).fillMaxSize().clickable {
          if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
        })
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
          Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp)) {
            Text(activeStory.caption, color = Color.White, modifier = Modifier.padding(10.dp))
          }
        }

        AnimatedVisibility(visible = showCommentsList && sortedReactions.isNotEmpty()) {
          Surface(
            color = Color.Black.copy(alpha = 0.78f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
          ) {
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
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          listOf("❤️", "🔥", "😂", "👏", "😍").forEach { emoji ->
            Surface(
              color = Color.White.copy(alpha = 0.22f),
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
            placeholder = { Text("Write a story comment...", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
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
              onClick = {
                val trimmed = replyText.trim()
                if (trimmed.isNotEmpty()) {
                  viewModel.reactToStory(activeStory, "💬", trimmed)
                  replyText = ""
                  showCommentsList = true
                }
              },
              modifier = Modifier.testTag("send_story_comment_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send story comment", tint = Color.White)
            }
          }
        }
      }
    }
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
  onOpenStories: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val reelsState by viewModel.reels.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }

  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

  var showCreateReel by remember { mutableStateOf(false) }
  var commentReel by remember { mutableStateOf<Reel?>(null) }
  var shareReelTarget by remember { mutableStateOf<Reel?>(null) }
  var autoAdvanceEnabled by remember { mutableStateOf(true) }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C10))) {
    when (val state = reelsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
      }
      is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        EmptyStateCard(Icons.Filled.PlayCircle, "Reels unavailable", state.message)
      }
      is UiState.Success -> {
        val reels = state.data
        if (reels.isEmpty()) {
          Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
              icon = Icons.Filled.PlayCircle,
              title = "No Reels yet",
              subtitle = "Upload a short clip or create a visual reel — they will play continuously one after another.",
              actionLabel = "Post a Reel",
              onAction = { showCreateReel = true }
            )
          }
        } else {
          val pagerState = rememberPagerState(pageCount = { reels.size })

          VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag("reels_vertical_pager"),
            beyondViewportPageCount = 1
          ) { page ->
            val reel = reels[page]
            val isCurrentPage = pagerState.currentPage == page
            val isSaved = reel.id in savedIds

            fun advanceToNextReel() {
              if (autoAdvanceEnabled && reels.size > 1) {
                val next = (page + 1) % reels.size
                scope.launch {
                  pagerState.animateScrollToPage(next)
                }
              }
            }

            // Auto-advance timer for cover/text-only reels so the continuous feed never gets stuck
            var textReelProgress by remember(reel.id, isCurrentPage) { mutableFloatStateOf(0f) }
            LaunchedEffect(reel.id, isCurrentPage, autoAdvanceEnabled, commentReel, shareReelTarget) {
              if (isCurrentPage && reel.videoUri.isBlank() && autoAdvanceEnabled && reels.size > 1) {
                textReelProgress = 0f
                while (textReelProgress < 1f) {
                  delay(70)
                  if (commentReel == null && shareReelTarget == null && !showCreateReel) {
                    textReelProgress += 0.01f // ~7 seconds per card reel
                  }
                }
                advanceToNextReel()
              }
            }

            Box(modifier = Modifier.fillMaxSize()) {
              if (reel.videoUri.isNotBlank()) {
                VideoPlayerBox(
                  videoUri = reel.videoUri,
                  modifier = Modifier.fillMaxSize(),
                  autoPlay = true,
                  isActive = isCurrentPage && commentReel == null && shareReelTarget == null && !showCreateReel,
                  looping = !autoAdvanceEnabled || reels.size <= 1,
                  defaultHeight = null,
                  cornerRadius = 0.dp,
                  showMediaController = false,
                  onVideoEnded = { advanceToNextReel() }
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
                      text = reel.caption,
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
                  .height(220.dp)
                  .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))))
              )

              // Right-side vertical FB Reels Actions
              Column(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(end = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = { viewModel.likeReel(reel.id) },
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                  ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Like reel", tint = Color(0xFFFF4D6D))
                  }
                  Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = { commentReel = reel },
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                  ) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment on reel", tint = Color.White)
                  }
                  Text("${reel.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = { shareReelTarget = reel },
                    modifier = Modifier
                      .size(48.dp)
                      .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                      .testTag("reel_share_button")
                  ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share reel", tint = Color.White)
                  }
                  Text("${reel.sharesCount}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                  onClick = { viewModel.toggleSave(reel.id, "reel", reel.authorName, reel.caption) },
                  modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                  Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Save reel",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White
                  )
                }

                if (reels.size > 1) {
                  IconButton(
                    onClick = {
                      scope.launch { pagerState.animateScrollToPage((page + 1) % reels.size) }
                    },
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.16f), CircleShape)
                  ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next reel", tint = Color.White)
                  }
                }
              }

              // Bottom-left Author, Caption & Audio pill
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .fillMaxWidth(0.78f)
                  .padding(start = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  UserAvatar(name = reel.authorName, photoUrl = reel.authorAvatar, size = 40.dp)
                  Column {
                    Text(reel.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                      text = "Reel ${page + 1} of ${reels.size}",
                      color = Color.White.copy(alpha = 0.72f),
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                }
                Text(
                  text = reel.caption,
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
                      text = reel.audioTitle,
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }

              if (reel.videoUri.isBlank() && autoAdvanceEnabled && reels.size > 1) {
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

    // Floating Top Controls Bar over Reels
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Reels", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = if (autoAdvanceEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { autoAdvanceEnabled = !autoAdvanceEnabled }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Filled.Repeat, null, tint = Color.White, modifier = Modifier.size(14.dp))
              Text(if (autoAdvanceEnabled) "Auto-Next" else "Loop", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
          }
          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable(onClick = onOpenStories)
          ) {
            Text("Stories", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
          }
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { showCreateReel = true }.testTag("create_reel_button")
          ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(2.dp))
              Text("Create", color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
      if (isUploading) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Uploading reel video to cloud...", style = MaterialTheme.typography.labelSmall, color = Color.White)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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

  shareReelTarget?.let { reel ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Reel by ${reel.authorName} · 🎵 ${reel.audioTitle}",
        body = reel.caption,
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareReelTarget = null },
      onRepostToFeed = { note ->
        val composed = if (note.isBlank()) {
          "Shared Reel by ${reel.authorName}: ${reel.caption}"
        } else {
          "$note\n\n— Reel by ${reel.authorName}: ${reel.caption}"
        }
        viewModel.createPost(context, composed, reel.videoUri, if (reel.videoUri.isNotBlank()) "video" else "none", "public", emptyList())
        viewModel.shareReel(reel.id)
      },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToConversation(conv, sender, msg)
        viewModel.shareReel(reel.id)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToPerson(sender, person, msg)
        viewModel.shareReel(reel.id)
      },
      onExternalShareLaunched = { viewModel.shareReel(reel.id) }
    )
  }
}
