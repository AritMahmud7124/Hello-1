package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.GlobalSettings
import com.example.data.model.MusicAttachment
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullPhotoImage
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.PostCard
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.SongPickerAndTrimmerDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatSongSec
import com.example.ui.components.resolveLiveAuthor
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
  val feedState by viewModel.homeFeed.collectAsStateWithLifecycle()
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
  val usersById = remember(discoverableUsers) { discoverableUsers.associateBy { it.userId } }

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
            Icon(Icons.Rounded.Campaign, contentDescription = "Announcement", tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
              text = globalSettings.maintenanceBanner,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }
      }
    }

    // Stories strip (circular rings)
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier
                .width(104.dp)
                .height(170.dp)
                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenStories)
                .testTag("add_story_chip")
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                // Top half user background / avatar
                Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.SpaceBetween
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(110.dp)
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                  ) {
                    val myPhoto = me?.photoUrl.orEmpty()
                    if (myPhoto.isNotBlank()) {
                      AsyncImage(
                        model = myPhoto,
                        contentDescription = "My profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(
                            Brush.linearGradient(
                              listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
                              )
                            )
                          ),
                        contentAlignment = Alignment.Center
                      ) {
                        UserAvatar(
                          name = me?.displayName ?: "Me",
                          photoUrl = "",
                          size = 54.dp
                        )
                      }
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                        .border(2.dp, Color.White, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Create story",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  // Bottom text area
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .weight(1f)
                      .background(MaterialTheme.colorScheme.surface)
                      .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "Create story",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      textAlign = TextAlign.Center,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
          items(stories, key = { it.id }) { story ->
            val cardGradient = remember(story.backgroundHex) { storyGradientForHex(story.backgroundHex) }
            val storyAuthor = resolveLiveAuthor(
              authorId = story.authorId,
              fallbackName = story.authorName,
              fallbackAvatar = story.authorAvatar,
              usersById = usersById,
              me = me
            )
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .width(104.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenStoryViewer(story) }
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                // Background image or gradient
                if (story.mediaUri.isNotBlank() && story.mediaType == "image") {
                  AsyncImage(
                    model = story.mediaUri,
                    contentDescription = "${storyAuthor.displayName}'s story",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Brush.linearGradient(cardGradient))
                  ) {
                    if (story.caption.isNotBlank()) {
                      Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                          .align(Alignment.Center)
                          .padding(10.dp),
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }

                // Dark gradient scrim at bottom for author name legibility
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                      Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                      )
                    )
                )

                // Top-left Author Avatar with blue ring
                Box(
                  modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF1877F2), CircleShape)
                    .background(Color.White),
                  contentAlignment = Alignment.Center
                ) {
                  UserAvatar(
                    name = storyAuthor.displayName,
                    photoUrl = storyAuthor.avatarUrl,
                    size = 28.dp
                  )
                }

                // Music Badge if attached
                if (story.musicTitle.isNotBlank()) {
                  Box(
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(8.dp)
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(Color.Black.copy(alpha = 0.6f))
                      .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Rounded.MusicNote,
                      contentDescription = story.musicTitle,
                      tint = Color.White,
                      modifier = Modifier.size(12.dp)
                    )
                  }
                }

                // Bottom Author Name
                Text(
                  text = storyAuthor.displayName.ifBlank { "Story" },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                )
              }
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
          UserAvatar(
            name = me?.displayName ?: "Me",
            photoUrl = me?.photoUrl ?: "",
            size = 42.dp,
            isOnline = me?.effectiveOnline == true
          )
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = com.example.ui.i18n.LocalAppStrings.current.createPostPrompt,
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
            icon = Icons.Rounded.DynamicFeed,
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
              icon = Icons.Rounded.DynamicFeed,
              title = "Your HELLO feed is quiet",
              subtitle = "Share a photo, video, or status update to kick off the conversation.",
              actionLabel = "Write a Post",
              onAction = { showCreatePost = true }
            )
          }
        } else {
          items(combinedPosts, key = { it.id }) { post ->
            val reactions by viewModel.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
            val postAuthor = resolveLiveAuthor(
              authorId = post.authorId,
              fallbackName = post.authorName,
              fallbackAvatar = post.authorAvatar,
              usersById = usersById,
              me = me
            )
            PostCard(
              post = post,
              currentUserId = currentUserId,
              reactions = reactions,
              isSaved = post.id in savedIds,
              onReact = { emoji -> viewModel.reactToPost(post, emoji) },
              onCommentClick = { activeCommentPost = post },
              onShare = { note -> viewModel.sharePost(post, note) },
              allowShare = post.privacy == "public",
              authorOnline = postAuthor.isOnline,
              displayAuthorName = postAuthor.displayName,
              displayAuthorAvatar = postAuthor.avatarUrl,
              onShareClick = { shareTargetPost = post },
              onToggleSave = { viewModel.toggleSave(post.id, "post", postAuthor.displayName, post.content) },
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
      onPublish = { content, mediaUri, mediaType, privacy, music ->
        viewModel.createPost(context, content, mediaUri, mediaType, privacy, friendUids, music = music)
        showCreatePost = false
      },
      authorName = me?.displayName ?: "Hello Member",
      authorAvatar = me?.photoUrl ?: "",
      authorOnline = me?.effectiveOnline == true
    )
  }

  activeCommentPost?.let { post ->
    val comments by viewModel.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      usersById = usersById,
      me = me,
      onDismiss = { activeCommentPost = null },
      onSendComment = { text, parentId -> viewModel.addComment(post, text, parentId) }
    )
  }

  shareTargetPost?.let { post ->
    val shareAuthor = resolveLiveAuthor(
      authorId = post.authorId,
      fallbackName = post.authorName,
      fallbackAvatar = post.authorAvatar,
      usersById = usersById,
      me = me
    )
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${shareAuthor.displayName}",
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
  onPublish: (content: String, mediaUri: String, mediaType: String, privacy: String, music: MusicAttachment?) -> Unit,
  allowPublicPrivacy: Boolean = true,
  authorName: String = "Hello Member",
  authorAvatar: String = "",
  authorOnline: Boolean = true
) {
  var content by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf("") }
  var mediaType by remember { mutableStateOf("none") }
  var privacy by remember(allowPublicPrivacy) { mutableStateOf(if (allowPublicPrivacy) "public" else "friends") }
  var pendingCropUri by remember { mutableStateOf<String?>(null) }
  var selectedSong by remember { mutableStateOf<MusicAttachment?>(null) }
  var showSongPicker by remember { mutableStateOf(false) }

  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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

  val canPublish = content.isNotBlank() || mediaUri.isNotBlank() || selectedSong != null

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
      ) {
        val strings = com.example.ui.i18n.LocalAppStrings.current
        // Clean Top Composer Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close post studio", modifier = Modifier.size(22.dp))
          }
          Text(
            text = strings.createPost,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Button(
            onClick = { onPublish(content, mediaUri, mediaType, privacy, selectedSong) },
            enabled = canPublish,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.testTag("publish_post_button")
          ) {
            Text(strings.publish, fontWeight = FontWeight.SemiBold)
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Scrollable Composer Canvas
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Author + Privacy Pills Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            UserAvatar(
              name = authorName,
              photoUrl = authorAvatar,
              size = 44.dp,
              isOnline = authorOnline
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              val privacyOptions = if (allowPublicPrivacy) {
                listOf("public" to "Public", "friends" to "Friends", "only_me" to "Only Me")
              } else {
                listOf("friends" to "Group Members", "only_me" to "Only Me")
              }
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                privacyOptions.forEach { (key, label) ->
                  val selected = privacy == key
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { privacy = key }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(
                        imageVector = when (key) {
                          "friends" -> Icons.Rounded.Group
                          "only_me" -> Icons.Rounded.Lock
                          else -> Icons.Rounded.Public
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }
            }
          }

          // Clean Editorial Composer Box
          OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth().testTag("post_content_input"),
            minLines = if (mediaUri.isBlank()) 4 else 2,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
          )

          // Clean Attached Song Card (tonal bar)
          selectedSong?.let { song ->
            val clipLen = (song.endSec - song.startSec).coerceAtLeast(1L)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  0.7.dp,
                  MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(12.dp)
                )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = song.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${song.artist} · ${formatSongSec(song.startSec.toInt())}–${formatSongSec(song.endSec.toInt())} (${clipLen}s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                TextButton(
                  onClick = { showSongPicker = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Rounded.ContentCut, contentDescription = "Trim song", modifier = Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Trim", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { selectedSong = null }, modifier = Modifier.size(30.dp)) {
                  Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove song",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                  )
                }
              }
            }
          }

          // Photo / Video Preview with compact modern overlay icons
          if (mediaUri.isNotBlank()) {
            if (mediaType == "image") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
              ) {
                FullPhotoImage(
                  model = mediaUri,
                  contentDescription = "Selected photo",
                  cornerRadius = 14.dp,
                  minHeight = 160.dp,
                  maxHeight = 560.dp,
                  enableFullscreenViewer = true
                )
                Row(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                      .clip(CircleShape)
                      .clickable { pendingCropUri = mediaUri }
                      .testTag("crop_post_photo_button")
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
                  Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .clickable {
                        mediaUri = ""
                        mediaType = "none"
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                  }
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Video attached", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                  }
                  IconButton(
                    onClick = {
                      mediaUri = ""
                      mediaType = "none"
                    },
                    modifier = Modifier.size(30.dp)
                  ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove video", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }

        // Compact Real Action Toolbar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        val dockStrings = com.example.ui.i18n.LocalAppStrings.current
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CreatorDockChip(
            icon = Icons.Rounded.Image,
            label = if (mediaType == "image") "Photo" else dockStrings.photo,
            iconTint = Color(0xFF16A34A),
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.testTag("post_add_photo_chip")
          )
          CreatorDockChip(
            icon = Icons.Rounded.MusicNote,
            label = if (selectedSong == null) dockStrings.song else selectedSong!!.title.take(14),
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = { showSongPicker = true },
            modifier = Modifier.testTag("post_add_song_chip")
          )
          CreatorDockChip(
            icon = Icons.Rounded.Videocam,
            label = dockStrings.video,
            iconTint = Color(0xFFE11D48),
            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
          )
        }
      }
    }
  }

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
private fun CreatorDockChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  iconTint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(17.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
