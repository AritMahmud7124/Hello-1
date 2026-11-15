package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.GlobalSettings
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.PostCard
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.FeedSortMode
import com.example.ui.viewmodel.MainSocialViewModel

@Composable
fun HomeScreen(
  viewModel: MainSocialViewModel,
  currentUserId: String,
  friendUids: List<String>,
  blockedUids: Set<String>,
  globalSettings: GlobalSettings,
  communityVm: CommunityViewModel? = null,
  onOpenStories: () -> Unit,
  onOpenStoryViewer: (Story) -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val me by viewModel.currentUser.collectAsStateWithLifecycle()
  val feedState by viewModel.publicFeed.collectAsStateWithLifecycle()
  val myPosts by viewModel.myPosts.collectAsStateWithLifecycle()
  val friendsPosts by viewModel.friendsSharedPosts.collectAsStateWithLifecycle()
  val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
  val storiesState by viewModel.stories.collectAsStateWithLifecycle()
  val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()

  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

  var showCreatePost by remember { mutableStateOf(false) }
  var activeCommentPost by remember { mutableStateOf<Post?>(null) }
  var shareTargetPost by remember { mutableStateOf<Post?>(null) }

  val listState = rememberLazyListState()
  val shouldLoadMore by remember {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val total = listState.layoutInfo.totalItemsCount
      total > 5 && lastVisible >= total - 2
    }
  }
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) viewModel.loadMorePosts()
  }

  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }

  val combinedPosts = remember(feedState, myPosts, friendsPosts, sortMode, blockedUids, friendUids) {
    val pub = (feedState as? UiState.Success)?.data ?: emptyList()
    val merged = (pub + myPosts + friendsPosts)
      .distinctBy { it.id }
      .filter { it.authorId !in blockedUids }
    when (sortMode) {
      FeedSortMode.LATEST -> merged.sortedByDescending { it.createdAt?.seconds ?: 0L }
      FeedSortMode.TOP -> merged.sortedByDescending { it.likesCount * 2 + it.commentsCount + it.sharesCount }
      FeedSortMode.FRIENDS -> merged
        .filter { it.authorId == currentUserId || it.authorId in friendUids }
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
  }

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    if (globalSettings.maintenanceBanner.isNotBlank()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Filled.Campaign, contentDescription = "Announcement", tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
              text = globalSettings.maintenanceBanner,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }
      }
    }

    // Stories strip
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Stories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          TextButton(onClick = onOpenStories) { Text("See all") }
        }
        val stories = remember(storiesState, blockedUids) {
          ((storiesState as? UiState.Success)?.data.orEmpty()).filter { it.authorId !in blockedUids }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(72.dp)
                .clickable(onClick = onOpenStories)
                .testTag("add_story_chip")
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Create story", tint = MaterialTheme.colorScheme.primary)
              }
              Spacer(Modifier.height(4.dp))
              Text("Your Story", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          items(stories, key = { it.id }) { story ->
            val ringColors = remember(story.backgroundHex) { storyGradientForHex(story.backgroundHex) }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(72.dp)
                .clickable { onOpenStoryViewer(story) }
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .border(
                    2.5.dp,
                    Brush.linearGradient(ringColors),
                    CircleShape
                  )
                  .padding(3.dp)
              ) {
                UserAvatar(name = story.authorName, photoUrl = story.authorAvatar, size = 58.dp)
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = story.authorName.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    // Composer card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showCreatePost = true }
          .testTag("open_post_composer_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          UserAvatar(name = me?.displayName ?: "Me", photoUrl = me?.photoUrl ?: "", size = 42.dp, isOnline = true)
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "Say Hello! What's on your mind?",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
          }
        }
      }
    }

    // Upload progress banner
    if (isUploading) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Uploading post media to cloud...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    }

    // Sort chips
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FeedSortMode.entries.forEach { mode ->
          FilterChip(
            selected = sortMode == mode,
            onClick = { viewModel.setSortMode(mode) },
            label = { Text(mode.label) }
          )
        }
      }
    }

    when (val state = feedState) {
      is UiState.Loading -> {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      }
      is UiState.Error -> {
        item {
          EmptyStateCard(
            icon = Icons.Filled.DynamicFeed,
            title = "Couldn't load feed",
            subtitle = state.message,
            actionLabel = "Create First Post",
            onAction = { showCreatePost = true }
          )
        }
      }
      is UiState.Success -> {
        if (combinedPosts.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Filled.DynamicFeed,
              title = "Your HELLO feed is quiet",
              subtitle = "Share a photo, video, or status update to kick off the conversation.",
              actionLabel = "Write a Post",
              onAction = { showCreatePost = true }
            )
          }
        } else {
          items(combinedPosts, key = { it.id }) { post ->
            val reactions by viewModel.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
            PostCard(
              post = post,
              currentUserId = currentUserId,
              reactions = reactions,
              isSaved = post.id in savedIds,
              onReact = { emoji -> viewModel.reactToPost(post, emoji) },
              onCommentClick = { activeCommentPost = post },
              onShare = { note -> viewModel.sharePost(post, note) },
              onShareClick = { shareTargetPost = post },
              onToggleSave = { viewModel.toggleSave(post.id, "post", post.authorName, post.content) },
              onEdit = { content, privacy -> viewModel.editPost(post.id, content, privacy) },
              onDelete = { viewModel.deletePost(post.id) },
              onReport = { reason -> viewModel.reportContent("post", post.id, reason) },
              onAuthorClick = { onOpenProfile(post.authorId) }
            )
          }
          item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              TextButton(onClick = { viewModel.loadMorePosts() }) {
                Text("Load more posts")
              }
            }
          }
        }
      }
    }
  }

  if (showCreatePost) {
    CreatePostDialog(
      onDismiss = { showCreatePost = false },
      onPublish = { content, mediaUri, mediaType, privacy ->
        viewModel.createPost(context, content, mediaUri, mediaType, privacy, friendUids)
        showCreatePost = false
      }
    )
  }

  activeCommentPost?.let { post ->
    val comments by viewModel.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      onDismiss = { activeCommentPost = null },
      onSendComment = { text, parentId -> viewModel.addComment(post, text, parentId) }
    )
  }

  shareTargetPost?.let { post ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${post.authorName}",
        body = post.content.ifBlank { "Shared media post on HELLO" },
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareTargetPost = null },
      onRepostToFeed = { note -> viewModel.sharePost(post, note) },
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
fun CreatePostDialog(
  onDismiss: () -> Unit,
  onPublish: (content: String, mediaUri: String, mediaType: String, privacy: String) -> Unit
) {
  var content by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf("") }
  var mediaType by remember { mutableStateOf("none") }
  var privacy by remember { mutableStateOf("public") }
  var pendingCropUri by remember { mutableStateOf<String?>(null) }

  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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
    title = { Text("Create Post") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("What's new on HELLO?") },
          modifier = Modifier.fillMaxWidth().testTag("post_content_input"),
          minLines = 3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Photo")
          }
          OutlinedButton(
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Video")
          }
        }
        if (mediaUri.isNotBlank()) {
          if (mediaType == "image") {
            AsyncImage(
              model = mediaUri,
              contentDescription = "Selected photo",
              modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              OutlinedButton(
                onClick = { pendingCropUri = mediaUri },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("crop_post_photo_button")
              ) {
                Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Crop Photo")
              }
              TextButton(
                onClick = {
                  mediaUri = ""
                  mediaType = "none"
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
              }
            }
          } else {
            Text("Video attached ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            TextButton(
              onClick = {
                mediaUri = ""
                mediaType = "none"
              },
              contentPadding = PaddingValues(0.dp)
            ) {
              Text("Remove attachment", color = MaterialTheme.colorScheme.error)
            }
          }
        }
        Text("Privacy", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("public" to "Public", "friends" to "Friends", "only_me" to "Only Me").forEach { (key, label) ->
            FilterChip(
              selected = privacy == key,
              onClick = { privacy = key },
              label = { Text(label) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onPublish(content, mediaUri, mediaType, privacy) },
        enabled = content.isNotBlank() || mediaUri.isNotBlank(),
        modifier = Modifier.testTag("publish_post_button")
      ) { Text("Post") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )

  pendingCropUri?.let { rawUri ->
    ImageCropDialog(
      imageUri = rawUri,
      defaultAspect = CropAspect.FREE,
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
