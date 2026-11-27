package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.model.FeedPreferences
import com.example.data.model.HELLO_FEED_REGIONS
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.network.YouTubeFetchResult
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ThemeMode
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricAvailability
import com.example.security.DeviceIntegrityChecker
import com.example.security.DeviceIntegrityReport
import com.example.security.EncryptedVaultStorage
import com.example.security.SecurityRiskLevel
import com.example.security.findFragmentActivity
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullPhotoImage
import com.example.ui.components.HelloWebsiteAndLaptopCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.PostCard
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.formatTimestamp
import com.example.ui.components.resolveLiveAuthor
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ProfileScreen(
  socialVm: MainSocialViewModel,
  currentUserId: String,
  friendCount: Int,
  isVerifiedOwner: Boolean,
  communityVm: CommunityViewModel? = null,
  spacesVm: SpacesGovernanceViewModel? = null,
  vaultStorage: EncryptedVaultStorage? = null,
  integrityReport: DeviceIntegrityReport? = null,
  onOpenStoryViewer: (Story) -> Unit = {},
  onOpenSettings: () -> Unit
) {
  val context = LocalContext.current
  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val ownerCfg by (spacesVm?.ownerConfig?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
  val canManageOwnerPrivacy = isVerifiedOwner &&
    spacesVm != null &&
    currentUserId.isNotBlank() &&
    ownerCfg?.ownerUid == currentUserId
  val myPosts by socialVm.myProfilePosts.collectAsStateWithLifecycle()
  val reelsState by socialVm.reels.collectAsStateWithLifecycle()
  val myReels = remember(reelsState, currentUserId) {
    ((reelsState as? UiState.Success)?.data.orEmpty()).filter {
      currentUserId.isNotBlank() && it.authorId == currentUserId
    }
  }
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val highlightStories by socialVm.savedHighlightStories.collectAsStateWithLifecycle()
  val myHighlights = remember(highlightStories, currentUserId) {
    highlightStories.filter { it.authorId == currentUserId }
  }
  val isUploading by socialVm.isUploadingMedia.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()
  val usersById = remember(discoverableUsers) { discoverableUsers.associateBy { it.userId } }
  val liveFollowerCount by socialVm.observeFollowerCount(currentUserId).collectAsStateWithLifecycle()
  val displayedFollowerCount = maxOf(me?.followersCount ?: 0L, liveFollowerCount)
  var tab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }
  var commentPost by remember { mutableStateOf<Post?>(null) }
  var shareTargetPost by remember { mutableStateOf<Post?>(null) }
  var deleteReelTarget by remember { mutableStateOf<Reel?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (isUploading) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Uploading profile media...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    }
    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
              )
          ) {
            if (!me?.coverUrl.isNullOrBlank()) {
              AsyncImage(
                model = me!!.coverUrl,
                contentDescription = "Profile cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            }
          }
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              UserAvatar(
                name = me?.displayName ?: "Member",
                photoUrl = me?.photoUrl ?: "",
                size = 64.dp,
                isOnline = me?.effectiveOnline == true,
                isVerifiedOwner = isVerifiedOwner
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(me?.displayName ?: "Hello Member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val hideOwnHandle = canManageOwnerPrivacy && ownerCfg?.showOwnerHandle == false
                val profileMeta = if (hideOwnHandle) {
                  "${me?.profilePrivacy ?: "public"} profile"
                } else {
                  "${me?.handle ?: "@hello"} · ${me?.profilePrivacy ?: "public"} profile"
                }
                Text(profileMeta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            if (!me?.bio.isNullOrBlank()) {
              Text(me!!.bio, style = MaterialTheme.typography.bodyMedium)
            }
            Text("$friendCount friends · $displayedFollowerCount followers · ${myPosts.size} posts · ${myReels.size} reels", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f).testTag("edit_profile_button")) {
                Icon(Icons.Rounded.Edit, null)
                Spacer(Modifier.width(4.dp))
                Text("Edit Profile")
              }
              OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, null)
                Spacer(Modifier.width(4.dp))
                Text("Settings")
              }
            }
          }
        }
      }
    }

    if (myHighlights.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Story Highlights (${myHighlights.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(myHighlights, key = { "hl_${it.id}" }) { story ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .width(64.dp)
                  .clickable { onOpenStoryViewer(story) }
                  .testTag("profile_highlight_${story.id}")
              ) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .border(
                      2.dp,
                      Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                      CircleShape
                    )
                    .padding(3.dp),
                  contentAlignment = Alignment.Center
                ) {
                  if (story.mediaUri.isNotBlank() && story.mediaType == "image") {
                    AsyncImage(
                      model = story.mediaUri,
                      contentDescription = story.caption.ifBlank { "Story highlight" },
                      modifier = Modifier.fillMaxSize().clip(CircleShape),
                      contentScale = ContentScale.Crop
                    )
                  } else {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.fillMaxSize()
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Star, contentDescription = "Story highlight", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                      }
                    }
                  }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                  text = story.caption.ifBlank { "Highlight" },
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
      }
    }

    item {
      TabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Posts", maxLines = 1, overflow = TextOverflow.Ellipsis) })
        Tab(
          selected = tab == 1,
          onClick = { tab = 1 },
          text = { Text("Reels (${myReels.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.testTag("profile_tab_reels")
        )
        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Media", maxLines = 1, overflow = TextOverflow.Ellipsis) })
        Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Saved (${savedPosts.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) })
      }
    }

    when (tab) {
      0 -> {
        if (myPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Rounded.Person, "No posts yet", "Your posts, shared items, and privacy-controlled updates live here.") }
        } else {
          items(myPosts, key = { it.id }) { post ->
            val reactions by socialVm.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
            val liveName = me?.displayName?.ifBlank { null } ?: post.authorName
            val liveAvatar = me?.photoUrl?.ifBlank { null } ?: post.authorAvatar
            PostCard(
              post = post,
              currentUserId = currentUserId,
              reactions = reactions,
              isSaved = post.id in savedIds,
              allowShare = post.privacy == "public",
              authorOnline = me?.effectiveOnline == true,
              displayAuthorName = liveName,
              displayAuthorAvatar = liveAvatar,
              onReact = { e -> socialVm.reactToPost(post, e) },
              onCommentClick = { commentPost = post },
              onShare = { n -> socialVm.sharePost(post, n) },
              onShareClick = { shareTargetPost = post },
              onToggleSave = { socialVm.toggleSave(post.id, "post", liveName, post.content) },
              onEdit = { c, p -> socialVm.editPost(post.id, c, p) },
              onDelete = { socialVm.deletePost(post.id) },
              onReport = { r -> socialVm.reportContent("post", post.id, r) }
            )
          }
        }
      }
      1 -> {
        if (myReels.isEmpty()) {
          item {
            EmptyStateCard(
              Icons.Rounded.SlowMotionVideo,
              "No reels uploaded yet",
              "Reels you upload appear here, and you can delete any of your reels anytime."
            )
          }
        } else {
          items(myReels, key = { "my_reel_${it.id}" }) { reel ->
            val liveMyName = me?.displayName?.ifBlank { null } ?: reel.authorName.ifBlank { "Hello Member" }
            val liveMyAvatar = me?.photoUrl?.ifBlank { null } ?: reel.authorAvatar
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  UserAvatar(
                    name = liveMyName,
                    photoUrl = liveMyAvatar,
                    size = 38.dp,
                    isOnline = me?.effectiveOnline == true,
                    isVerifiedOwner = isVerifiedOwner
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = liveMyName,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "${reel.category} · ${formatTimestamp(reel.createdAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  OutlinedButton(
                    onClick = { deleteReelTarget = reel },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("profile_delete_reel_${reel.id}")
                  ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete reel", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                  }
                }

                if (reel.caption.isNotBlank()) {
                  Text(reel.caption, style = MaterialTheme.typography.bodyMedium)
                }

                if (reel.videoUri.isNotBlank()) {
                  VideoPlayerBox(
                    videoUri = reel.videoUri,
                    autoPlay = false,
                    isActive = false,
                    showMediaController = true
                  )
                } else if (reel.youtubeVideoId.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Text(
                        text = "YouTube Shorts · ${reel.youtubeVideoId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                      text = reel.audioTitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Text(
                    text = "${reel.likesCount} likes · ${reel.commentsCount} comments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
      2 -> {
        val mediaPosts = myPosts.filter { it.mediaUri.isNotBlank() }
        if (mediaPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Rounded.Person, "No photos or videos yet", "Photos and videos you share in posts appear here.") }
        } else {
          items(mediaPosts, key = { it.id }) { post ->
            if (post.mediaType == "video") {
              VideoPlayerBox(videoUri = post.mediaUri)
            } else {
              FullPhotoImage(
                model = post.mediaUri,
                contentDescription = post.content.ifBlank { "Profile photo" },
                cornerRadius = 16.dp,
                minHeight = 180.dp,
                maxHeight = 720.dp
              )
            }
          }
        }
      }
      3 -> {
        if (savedPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Rounded.Bookmark, "Nothing saved yet", "Tap the bookmark icon on any post or reel to keep it handy.") }
        } else {
          items(savedPosts, key = { it.id }) { saved ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${saved.itemType.uppercase()} · ${saved.authorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                  Text(saved.excerpt.ifBlank { "Saved item" }, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { socialVm.toggleSave(saved.postId, saved.itemType, saved.authorName, saved.excerpt) }) {
                  Text("Remove")
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditDialog) {
    val cur = me ?: com.example.data.model.PublicUser(
      userId = currentUserId,
      displayName = "Hello Member",
      bio = "Hey there! I'm on HELLO."
    )
    var name by remember { mutableStateOf(cur.displayName) }
    var bio by remember { mutableStateOf(cur.bio) }
    var photo by remember { mutableStateOf(cur.photoUrl) }
    var cover by remember { mutableStateOf(cur.coverUrl) }
    var privacy by remember { mutableStateOf(cur.profilePrivacy) }
    var acceptLove by remember { mutableStateOf(cur.acceptSecretLove) }
    var cropAvatarUri by remember { mutableStateOf<String?>(null) }
    var cropCoverUri by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) cropAvatarUri = uri.toString()
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) cropCoverUri = uri.toString()
    }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Edit Profile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Avatar (Crop)") }
            OutlinedButton(onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Cover (Crop)") }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("public", "friends", "private").forEach { p ->
              FilterChip(selected = privacy == p, onClick = { privacy = p }, label = { Text(p) })
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          socialVm.updateProfile(context, name, bio, photo, cover, privacy, acceptLove, cur.showOnlineStatus)
          showEditDialog = false
        }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
    )

    cropAvatarUri?.let { raw ->
      ImageCropDialog(
        imageUri = raw,
        defaultAspect = CropAspect.SQUARE,
        onDismiss = { cropAvatarUri = null },
        onUseOriginal = {
          photo = raw
          cropAvatarUri = null
        },
        onCropped = { cropped ->
          photo = cropped
          cropAvatarUri = null
        }
      )
    }

    cropCoverUri?.let { raw ->
      ImageCropDialog(
        imageUri = raw,
        defaultAspect = CropAspect.COVER,
        onDismiss = { cropCoverUri = null },
        onUseOriginal = {
          cover = raw
          cropCoverUri = null
        },
        onCropped = { cropped ->
          cover = cropped
          cropCoverUri = null
        }
      )
    }
  }

  commentPost?.let { post ->
    val comments by socialVm.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      usersById = usersById,
      me = me,
      onDismiss = { commentPost = null },
      onSendComment = { t, p -> socialVm.addComment(post, t, p) }
    )
  }

  shareTargetPost?.let { post ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${me?.displayName?.ifBlank { post.authorName } ?: post.authorName}",
        body = post.content.ifBlank { "Shared post on HELLO" },
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareTargetPost = null },
      onRepostToFeed = { note -> socialVm.sharePost(post, note) },
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

  deleteReelTarget?.let { reel ->
    AlertDialog(
      onDismissRequest = { deleteReelTarget = null },
      title = { Text("Delete Reel?") },
      text = { Text("Are you sure you want to delete this reel? It will be permanently removed for everyone.") },
      confirmButton = {
        Button(
          onClick = {
            socialVm.deleteReel(reel.id)
            deleteReelTarget = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("profile_confirm_delete_reel_button")
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

@Composable
fun UserProfileScreen(
  targetUserId: String,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  currentUserId: String,
  friendUids: List<String>,
  blockedUids: Set<String>,
  spacesVm: SpacesGovernanceViewModel? = null,
  vaultStorage: EncryptedVaultStorage? = null,
  integrityReport: DeviceIntegrityReport? = null,
  incognitoPrivateViewing: Boolean = false,
  onBack: () -> Unit,
  onOpenChat: (String) -> Unit,
  onOpenStoryViewer: (Story) -> Unit,
  onOpenSettings: () -> Unit
) {
  if (targetUserId == currentUserId) {
    ProfileScreen(
      socialVm = socialVm,
      currentUserId = currentUserId,
      friendCount = friendUids.size,
      isVerifiedOwner = spacesVm?.isVerifiedOwner?.collectAsStateWithLifecycle()?.value == true,
      communityVm = communityVm,
      spacesVm = spacesVm,
      vaultStorage = vaultStorage,
      integrityReport = integrityReport,
      onOpenStoryViewer = onOpenStoryViewer,
      onOpenSettings = onOpenSettings
    )
    return
  }

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val ownerCfg by (spacesVm?.ownerConfig?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
  val profileState by socialVm.observePublicUserProfile(targetUserId).collectAsStateWithLifecycle(initialValue = UiState.Loading)
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val discoverableUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val usersById = remember(discoverableUsers) { discoverableUsers.associateBy { it.userId } }
  val cachedUser = remember(discoverableUsers, targetUserId) {
    discoverableUsers.firstOrNull { it.userId == targetUserId }
  }
  val followingIds by socialVm.followingIds.collectAsStateWithLifecycle()
  val liveFollowerCount by socialVm.observeFollowerCount(targetUserId).collectAsStateWithLifecycle()
  val isFollowingTarget = targetUserId in followingIds
  val profilePosts by socialVm.observeProfilePosts(targetUserId).collectAsStateWithLifecycle(initialValue = emptyList())
  val storiesState by socialVm.stories.collectAsStateWithLifecycle()
  val savedHighlightsState by socialVm.savedHighlightStories.collectAsStateWithLifecycle()
  val activeUserStories = remember(storiesState, targetUserId) {
    ((storiesState as? UiState.Success)?.data.orEmpty()).filter { it.authorId == targetUserId && isStoryFresh24h(it) }
  }
  val userHighlights = remember(savedHighlightsState, targetUserId) {
    savedHighlightsState.filter { it.authorId == targetUserId }
  }
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val friendRequests by communityVm.friendRequests.collectAsStateWithLifecycle()

  val isBlocked = targetUserId in blockedUids
  val isFriend = targetUserId in friendUids
  val incomingRequest = remember(friendRequests, targetUserId, currentUserId) {
    friendRequests.firstOrNull { it.fromUid == targetUserId && it.toUid == currentUserId && it.status == "pending" }
  }
  val outgoingRequest = remember(friendRequests, targetUserId, currentUserId) {
    friendRequests.firstOrNull { it.fromUid == currentUserId && it.toUid == targetUserId && it.status == "pending" }
  }

  val targetUser: PublicUser? = when (val s = profileState) {
    is UiState.Success -> s.data ?: cachedUser
    is UiState.Loading -> cachedUser
    is UiState.Error -> cachedUser
  }

  var tab by remember { mutableIntStateOf(0) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var reportReason by remember { mutableStateOf("") }
  var commentPost by remember { mutableStateOf<Post?>(null) }
  var shareTargetPost by remember { mutableStateOf<Post?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("user_profile_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        val hideTargetOwnerHandle = ownerCfg?.showOwnerHandle == false && targetUser != null &&
          (targetUser.isVerifiedOwner || (ownerCfg?.ownerUid?.isNotBlank() == true && targetUser.userId == ownerCfg?.ownerUid))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = targetUser?.displayName ?: "Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (targetUser != null && !hideTargetOwnerHandle && targetUser.handle.isNotBlank()) {
            Text(
              text = targetUser.handle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Box {
          IconButton(onClick = { showOverflowMenu = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Profile options")
          }
          DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
            if (isBlocked) {
              DropdownMenuItem(
                text = { Text("Unblock user") },
                leadingIcon = { Icon(Icons.Rounded.Block, null) },
                onClick = {
                  showOverflowMenu = false
                  communityVm.unblockUser(targetUserId)
                }
              )
            } else {
              DropdownMenuItem(
                text = { Text("Block user") },
                leadingIcon = { Icon(Icons.Rounded.Block, null) },
                onClick = {
                  showOverflowMenu = false
                  communityVm.blockUser(targetUserId, targetUser?.displayName ?: "Member")
                }
              )
            }
            DropdownMenuItem(
              text = { Text("Report profile") },
              leadingIcon = { Icon(Icons.Rounded.Flag, null) },
              onClick = {
                showOverflowMenu = false
                showReportDialog = true
              }
            )
          }
        }
      }
    }

    if (profileState is UiState.Loading && targetUser == null) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
      return@Column
    }

    if (targetUser == null) {
      Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val msg = (profileState as? UiState.Error)?.message ?: "This user's profile is private or not available."
        EmptyStateCard(
          icon = Icons.Rounded.Lock,
          title = "Profile Unavailable",
          subtitle = msg,
          actionLabel = "Go Back",
          onAction = onBack
        )
      }
      return@Column
    }

    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
    val isRestrictedFriendsOnly = targetUser.profilePrivacy == "friends" && !isFriend

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                  Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                )
            ) {
              if (targetUser.coverUrl.isNotBlank()) {
                AsyncImage(
                  model = targetUser.coverUrl,
                  contentDescription = "${targetUser.displayName} cover",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              }
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UserAvatar(
                  name = targetUser.displayName,
                  photoUrl = targetUser.photoUrl,
                  size = 64.dp,
                  isOnline = targetUser.effectiveOnline,
                  isVerifiedOwner = targetUser.isVerifiedOwner
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(targetUser.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                      imageVector = when (targetUser.profilePrivacy) {
                        "friends" -> Icons.Rounded.Group
                        "private" -> Icons.Rounded.Lock
                        else -> Icons.Rounded.Public
                      },
                      contentDescription = targetUser.profilePrivacy,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val hideHandleInHeader = ownerCfg?.showOwnerHandle == false &&
                      (targetUser.isVerifiedOwner || (ownerCfg?.ownerUid?.isNotBlank() == true && targetUser.userId == ownerCfg?.ownerUid))
                    val headerText = if (hideHandleInHeader || targetUser.handle.isBlank()) {
                      "${targetUser.profilePrivacy} profile · ${if (targetUser.effectiveOnline) "Online" else "Offline"}"
                    } else {
                      "${targetUser.handle} · ${targetUser.profilePrivacy} profile · ${if (targetUser.effectiveOnline) "Online" else "Offline"}"
                    }
                    Text(
                      text = headerText,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              if (targetUser.bio.isNotBlank()) {
                Text(targetUser.bio, style = MaterialTheme.typography.bodyMedium)
              }

              val displayedFollowerCount = maxOf(targetUser.followersCount, liveFollowerCount)
              Text(
                text = "$displayedFollowerCount followers · ${profilePosts.size} posts · ${activeUserStories.size} stories",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )

              if (isBlocked) {
                OutlinedButton(
                  onClick = { communityVm.unblockUser(targetUserId) },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Rounded.Block, null)
                  Spacer(Modifier.width(6.dp))
                  Text("Blocked · Tap to Unblock")
                }
              } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  when {
                    isFriend -> {
                      OutlinedButton(
                        onClick = { communityVm.unfriend(targetUserId) },
                        modifier = Modifier.weight(1f).testTag("profile_unfriend_button")
                      ) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Friends")
                      }
                    }
                    incomingRequest != null -> {
                      Button(
                        onClick = { communityVm.acceptFriendRequest(incomingRequest) },
                        modifier = Modifier.weight(1f).testTag("profile_accept_friend_button")
                      ) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                      }
                      OutlinedButton(
                        onClick = { communityVm.rejectOrCancelRequest(incomingRequest.id) }
                      ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Decline")
                      }
                    }
                    outgoingRequest != null -> {
                      OutlinedButton(
                        onClick = { communityVm.rejectOrCancelRequest(outgoingRequest.id) },
                        modifier = Modifier.weight(1f).testTag("profile_cancel_request_button")
                      ) {
                        Text("Requested")
                      }
                    }
                    else -> {
                      Button(
                        onClick = { communityVm.sendFriendRequest(effectiveMe, targetUser) },
                        modifier = Modifier.weight(1f).testTag("profile_add_friend_button")
                      ) {
                        Icon(Icons.Rounded.PersonAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Friend")
                      }
                    }
                  }

                  Button(
                    onClick = { communityVm.startDirectChat(effectiveMe, targetUser, onOpenChat) },
                    modifier = Modifier.weight(1f).testTag("profile_message_button")
                  ) {
                    Icon(
                      painter = painterResource(R.drawable.ic_messenger_smart),
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Message")
                  }
                }
                if (isFollowingTarget) {
                  OutlinedButton(
                    onClick = { socialVm.toggleFollowCreator(targetUserId, targetUser.displayName) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_follow_button")
                  ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Following · $displayedFollowerCount")
                  }
                } else {
                  Button(
                    onClick = { socialVm.toggleFollowCreator(targetUserId, targetUser.displayName) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer,
                      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_follow_button")
                  ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Follow")
                  }
                }
              }
            }
          }
        }
      }

      if (isBlocked) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.Block,
            title = "You blocked ${targetUser.displayName}",
            subtitle = "Unblock them to view their posts, stories, and send messages."
          )
        }
      } else if (isRestrictedFriendsOnly) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.Group,
            title = "Friends-Only Profile",
            subtitle = "Add ${targetUser.displayName} as a friend to see their posts, photos, and stories."
          )
        }
      } else {
        if (userHighlights.isNotEmpty()) {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Story Highlights (${userHighlights.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(userHighlights, key = { "uhl_${it.id}" }) { story ->
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp).clickable { onOpenStoryViewer(story) }
                  ) {
                    Box(
                      modifier = Modifier
                        .size(54.dp)
                        .border(2.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)), CircleShape)
                        .padding(3.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      if (story.mediaUri.isNotBlank() && story.mediaType == "image") {
                        AsyncImage(
                          model = story.mediaUri,
                          contentDescription = story.caption.ifBlank { "Highlight" },
                          modifier = Modifier.fillMaxSize().clip(CircleShape),
                          contentScale = ContentScale.Crop
                        )
                      } else {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize()) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                          }
                        }
                      }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(story.caption.ifBlank { "Highlight" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }
        item {
          TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Posts (${profilePosts.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Media") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Stories (${activeUserStories.size})") })
          }
        }

        when (tab) {
          0 -> {
            if (profilePosts.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Rounded.Person,
                  title = "No visible posts yet",
                  subtitle = "${targetUser.displayName} hasn't shared public or friends-shared posts with you yet."
                )
              }
            } else {
              items(profilePosts, key = { it.id }) { post ->
                val reactions by socialVm.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
                PostCard(
                  post = post,
                  currentUserId = currentUserId,
                  reactions = reactions,
                  isSaved = post.id in savedIds,
                  allowShare = post.privacy == "public",
                  authorOnline = targetUser.effectiveOnline,
                  displayAuthorName = targetUser.displayName,
                  displayAuthorAvatar = targetUser.photoUrl,
                  onReact = { e -> socialVm.reactToPost(post, e) },
                  onCommentClick = { commentPost = post },
                  onShare = { n -> socialVm.sharePost(post, n) },
                  onShareClick = { shareTargetPost = post },
                  onToggleSave = { socialVm.toggleSave(post.id, "post", targetUser.displayName, post.content) },
                  onEdit = { c, p -> socialVm.editPost(post.id, c, p) },
                  onDelete = { socialVm.deletePost(post.id) },
                  onReport = { r -> socialVm.reportContent("post", post.id, r) }
                )
              }
            }
          }
          1 -> {
            val mediaPosts = profilePosts.filter { it.mediaUri.isNotBlank() }
            if (mediaPosts.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Rounded.Person,
                  title = "No shared media yet",
                  subtitle = "Photos and videos shared by ${targetUser.displayName} will appear here."
                )
              }
            } else {
              items(mediaPosts, key = { it.id }) { post ->
                if (post.mediaType == "video") {
                  VideoPlayerBox(videoUri = post.mediaUri)
                } else {
                  FullPhotoImage(
                    model = post.mediaUri,
                    contentDescription = post.content.ifBlank { "Shared photo" },
                    cornerRadius = 16.dp,
                    minHeight = 180.dp,
                    maxHeight = 540.dp
                  )
                }
              }
            }
          }
          2 -> {
            if (activeUserStories.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Rounded.Collections,
                  title = "No active stories",
                  subtitle = "${targetUser.displayName} has no visible 24h stories right now."
                )
              }
            } else {
              items(activeUserStories, key = { it.id }) { story ->
                val gradient = remember(story.backgroundHex) { storyGradientForHex(story.backgroundHex) }
                Card(
                  modifier = Modifier.fillMaxWidth().clickable { onOpenStoryViewer(story) },
                  shape = RoundedCornerShape(18.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(150.dp)
                      .background(Brush.linearGradient(gradient))
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
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(14.dp)
                    ) {
                      if (story.mediaType == "video") {
                        Icon(
                          Icons.Rounded.PlayArrow,
                          contentDescription = "Video story",
                          tint = Color.White,
                          modifier = Modifier.size(36.dp).align(Alignment.Center)
                        )
                      }
                      Text(
                        text = story.caption.ifBlank { "Tap to watch story" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.BottomStart)
                      )
                      Text(
                        text = formatTimestamp(story.createdAt),
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd)
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
  }

  if (showReportDialog) {
    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      title = { Text("Report ${targetUser?.displayName ?: "Member"}") },
      text = {
        OutlinedTextField(
          value = reportReason,
          onValueChange = { reportReason = it },
          label = { Text("Reason") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(onClick = {
          socialVm.reportContent("user", targetUserId, reportReason)
          reportReason = ""
          showReportDialog = false
        }) { Text("Submit") }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
      }
    )
  }

  commentPost?.let { post ->
    val comments by socialVm.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      usersById = usersById,
      me = me,
      onDismiss = { commentPost = null },
      onSendComment = { t, p -> socialVm.addComment(post, t, p) }
    )
  }

  shareTargetPost?.let { post ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${targetUser?.displayName ?: post.authorName}",
        body = post.content.ifBlank { "Shared post on HELLO" },
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareTargetPost = null },
      onRepostToFeed = { note -> socialVm.sharePost(post, note) },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToConversation(conv, sender, msg)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToPerson(sender, person, msg)
      }
    )
  }
}

@Composable
fun SearchScreen(
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  spacesVm: SpacesGovernanceViewModel,
  searchHistory: List<String> = emptyList(),
  onSaveSearchHistory: (String) -> Unit = {},
  onRemoveSearchHistory: (String) -> Unit = {},
  onClearSearchHistory: () -> Unit = {},
  onOpenGroup: (String) -> Unit,
  onOpenPages: () -> Unit,
  onOpenEvents: () -> Unit,
  onOpenMarketplace: () -> Unit,
  onOpenChat: (String) -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  var query by remember { mutableStateOf("") }
  var scopeTab by remember { mutableStateOf("All") }
  val categories = listOf("All", "People", "Posts", "Groups", "Pages", "Events", "Marketplace")
  val focusManager = LocalFocusManager.current

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val users = ((communityVm.discoverableUsers.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val usersById = remember(users, me) {
    val map = users.associateBy { it.userId }.toMutableMap()
    if (me != null) map[me!!.userId] = me!!
    map
  }
  val posts = ((socialVm.publicFeed.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val groups by spacesVm.publicGroups.collectAsStateWithLifecycle()
  val pages = ((spacesVm.pages.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val events = ((spacesVm.events.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val listings = ((spacesVm.listings.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()

  fun commitQueryIfNeeded() {
    if (query.isNotBlank()) {
      onSaveSearchHistory(query)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    val strings = LocalAppStrings.current
    // HELLO clean pill search bar header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("${strings.search} HELLO") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
        trailingIcon = {
          if (query.isNotBlank()) {
            IconButton(
              onClick = { query = "" },
              modifier = Modifier.testTag("clear_search_query_button")
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Clear search")
            }
          }
        },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            commitQueryIfNeeded()
            focusManager.clearFocus()
          }
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("global_search_input"),
        singleLine = true
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { c ->
          FilterChip(selected = scopeTab == c, onClick = { scopeTab = c }, label = { Text(c) })
        }
      }
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

    val trimmed = query.trim()
    val mUsers = users.filter { trimmed.isBlank() || it.displayName.contains(trimmed, true) || it.handle.contains(trimmed, true) }
    val mPosts = posts.filter { trimmed.isBlank() || it.content.contains(trimmed, true) || it.authorName.contains(trimmed, true) }
    val mGroups = groups.filter { trimmed.isBlank() || it.name.contains(trimmed, true) || it.description.contains(trimmed, true) }
    val mPages = pages.filter { trimmed.isBlank() || it.name.contains(trimmed, true) || it.category.contains(trimmed, true) }
    val mEvents = events.filter { trimmed.isBlank() || it.title.contains(trimmed, true) || it.locationName.contains(trimmed, true) }
    val mListings = listings.filter { trimmed.isBlank() || it.title.contains(trimmed, true) || it.category.contains(trimmed, true) }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      // Recent Searches section
      if (trimmed.isBlank() && searchHistory.isNotEmpty()) {
        item(key = "recent_header") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Recent searches",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            TextButton(
              onClick = onClearSearchHistory,
              modifier = Modifier.testTag("clear_all_search_history_button")
            ) {
              Text("Clear all")
            }
          }
        }
        items(searchHistory, key = { "hist_$it" }) { hist ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .clickable {
                query = hist
                onSaveSearchHistory(hist)
              }
              .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              modifier = Modifier.size(38.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.History,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Text(
              text = hist,
              style = MaterialTheme.typography.bodyLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = { onRemoveSearchHistory(hist) },
              modifier = Modifier.testTag("remove_history_$hist")
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove $hist from history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
        item(key = "recent_divider") {
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            thickness = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
          )
        }
      }

      if (scopeTab in listOf("All", "People") && mUsers.isNotEmpty()) {
        item(key = "sec_people") {
          HelloSearchSectionHeader(if (trimmed.isBlank()) "Suggested People" else "People")
        }
        items(mUsers.take(10), key = { "u_${it.userId}" }) { u ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 56.dp)
              .clickable {
                commitQueryIfNeeded()
                onOpenProfile(u.userId)
              }
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            UserAvatar(
              name = u.displayName,
              photoUrl = u.photoUrl,
              size = 44.dp,
              isOnline = u.effectiveOnline,
              isVerifiedOwner = u.isVerifiedOwner
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = u.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = u.handle.ifBlank { "HELLO Member" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            IconButton(
              onClick = {
                commitQueryIfNeeded()
                if (me != null) communityVm.startDirectChat(me!!, u, onOpenChat)
              }
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_messenger_smart),
                contentDescription = "Message ${u.displayName}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
          )
        }
      }

      if (scopeTab in listOf("All", "Groups") && mGroups.isNotEmpty()) {
        item(key = "sec_groups") { HelloSearchSectionHeader("Groups") }
        items(mGroups.take(8), key = { "g_${it.id}" }) { g ->
          HelloSearchRow(
            icon = Icons.Rounded.Groups,
            title = g.name,
            subtitle = "${g.category} · ${g.membersCount} members · ${g.privacy}",
            onClick = {
              commitQueryIfNeeded()
              onOpenGroup(g.id)
            }
          )
        }
      }

      if (scopeTab in listOf("All", "Posts") && mPosts.isNotEmpty()) {
        item(key = "sec_posts") { HelloSearchSectionHeader("Posts") }
        items(mPosts.take(10), key = { "p_${it.id}" }) { p ->
          val resolvedAuthor = resolveLiveAuthor(p.authorId, p.authorName, p.authorAvatar, usersById, me)
          HelloSearchRow(
            icon = Icons.Rounded.Article,
            title = p.content.ifBlank { "Shared media post" },
            subtitle = "Posted by ${resolvedAuthor.displayName}",
            onClick = {
              commitQueryIfNeeded()
              onOpenProfile(p.authorId)
            }
          )
        }
      }

      if (scopeTab in listOf("All", "Pages") && mPages.isNotEmpty()) {
        item(key = "sec_pages") { HelloSearchSectionHeader("Pages") }
        items(mPages.take(8), key = { "pg_${it.id}" }) { pg ->
          HelloSearchRow(
            icon = Icons.Rounded.Flag,
            title = pg.name,
            subtitle = "Page · ${pg.category} · ${pg.followersCount} followers",
            onClick = {
              commitQueryIfNeeded()
              onOpenPages()
            }
          )
        }
      }

      if (scopeTab in listOf("All", "Events") && mEvents.isNotEmpty()) {
        item(key = "sec_events") { HelloSearchSectionHeader("Events") }
        items(mEvents.take(8), key = { "ev_${it.id}" }) { ev ->
          HelloSearchRow(
            icon = Icons.Rounded.Event,
            title = ev.title,
            subtitle = "${ev.eventDateText} · ${ev.locationName}",
            onClick = {
              commitQueryIfNeeded()
              onOpenEvents()
            }
          )
        }
      }

      if (scopeTab in listOf("All", "Marketplace") && mListings.isNotEmpty()) {
        item(key = "sec_market") { HelloSearchSectionHeader("Marketplace") }
        items(mListings.take(8), key = { "mk_${it.id}" }) { mk ->
          HelloSearchRow(
            icon = Icons.Rounded.Storefront,
            title = "${mk.title} · ${mk.currency}${mk.price}",
            subtitle = "${mk.category} · ${mk.condition}",
            onClick = {
              commitQueryIfNeeded()
              onOpenMarketplace()
            }
          )
        }
      }

      val noMatches = trimmed.isNotBlank() &&
        ((scopeTab == "All" && mUsers.isEmpty() && mPosts.isEmpty() && mGroups.isEmpty() && mPages.isEmpty() && mEvents.isEmpty() && mListings.isEmpty()) ||
          (scopeTab == "People" && mUsers.isEmpty()) ||
          (scopeTab == "Posts" && mPosts.isEmpty()) ||
          (scopeTab == "Groups" && mGroups.isEmpty()) ||
          (scopeTab == "Pages" && mPages.isEmpty()) ||
          (scopeTab == "Events" && mEvents.isEmpty()) ||
          (scopeTab == "Marketplace" && mListings.isEmpty()))
      if (noMatches) {
        item(key = "search_empty") {
          Box(modifier = Modifier.padding(24.dp)) {
            EmptyStateCard(
              icon = Icons.Rounded.Search,
              title = "No results for \"$trimmed\"",
              subtitle = "Try searching another name, group, post keyword, or marketplace item."
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HelloSearchSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
  )
}

@Composable
private fun HelloSearchRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.size(20.dp)
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(start = 70.dp, end = 16.dp),
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
  }
}

@Composable
fun SettingsScreen(
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  currentThemeMode: ThemeMode,
  currentLanguage: AppLanguage,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  spacesVm: SpacesGovernanceViewModel? = null,
  isVerifiedOwner: Boolean = false,
  vaultStorage: EncryptedVaultStorage? = null,
  integrityReport: DeviceIntegrityReport? = null,
  strictDebugMode: Boolean = false,
  onStrictDebugModeChange: (Boolean) -> Unit = {},
  onReelsAutoAdvancePersist: (Boolean) -> Unit = {},
  onReelsRegionPersist: (String) -> Unit = {},
  incognitoHideStatus: Boolean = false,
  incognitoPrivateViewing: Boolean = false,
  disappearingChat24h: Boolean = false,
  onIncognitoMasterChange: (Boolean) -> Unit = {},
  onIncognitoHideStatusChange: (Boolean) -> Unit = {},
  onIncognitoPrivateViewingChange: (Boolean) -> Unit = {},
  onDisappearingChat24hChange: (Boolean) -> Unit = {}
) {
  val context = LocalContext.current
  val vault = remember(context, vaultStorage) { vaultStorage ?: EncryptedVaultStorage(context) }
  val report = remember(context, integrityReport, strictDebugMode) {
    integrityReport ?: DeviceIntegrityChecker.assess(context, strictDebugMode)
  }
  val biometricAvailability = remember(context) { BiometricAuthHelper.checkAvailability(context) }
  var biometricEnabled by remember(vault) { mutableStateOf(vault.isBiometricLockEnabled) }
  var hasFreshSession by remember(vault) { mutableStateOf(vault.hasFreshSession()) }
  var securityStatusNotice by remember { mutableStateOf<String?>(null) }

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val priv by socialVm.privateUser.collectAsStateWithLifecycle()
  val blocked by communityVm.blockedUsers.collectAsStateWithLifecycle()
  val feedPrefs by socialVm.feedPreferences.collectAsStateWithLifecycle()
  val selectedReelCategory by socialVm.selectedReelCategory.collectAsStateWithLifecycle()
  val feedMode by socialVm.feedMode.collectAsStateWithLifecycle()
  val selectedRegion by socialVm.selectedRegion.collectAsStateWithLifecycle()
  val reelsAutoAdvance by socialVm.reelsAutoAdvance.collectAsStateWithLifecycle()
  val youtubeStatus by socialVm.youtubeStatus.collectAsStateWithLifecycle()
  val isFetchingShorts by socialVm.isFetchingShorts.collectAsStateWithLifecycle()

  val strings = LocalAppStrings.current
  var phone by remember(priv?.phone) { mutableStateOf(priv?.phone ?: "") }
  var recoveryEmail by remember(priv?.email) { mutableStateOf(priv?.email ?: "") }
  var recoveryStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
  var isSavingRecovery by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showTuningDetails by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Text(strings.settings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

    item {
      HelloWebsiteAndLaptopCard()
    }

    item {
      val showOnline = me?.showOnlineStatus != false
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                  if (showOnline) Color(0xFF22C55E).copy(alpha = 0.16f)
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(14.dp)
                  .clip(CircleShape)
                  .background(if (showOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
              )
            }
            Column {
              Text(
                text = "Active Status (Green Online Icon)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (showOnline) {
                  "Auto-turns ON when you're online in HELLO · Switch OFF anytime to hide your green dot."
                } else {
                  "Turned OFF — your green online dot is hidden from everyone."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Switch(
            checked = showOnline,
            onCheckedChange = { socialVm.toggleShowOnlineStatus(it) },
            modifier = Modifier.testTag("settings_online_status_switch")
          )
        }
      }
    }

    item {
      val incognitoAny = incognitoHideStatus || incognitoPrivateViewing || disappearingChat24h
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f).padding(end = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = if (incognitoAny) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = "Incognito and privacy mode",
                    tint = if (incognitoAny) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "Incognito & Privacy Mode",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (incognitoAny) {
                    "Stealth protections active across chat, stories & presence."
                  } else {
                    "Hide typing/presence, view stories quietly, or auto-expire messages in 24h."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Switch(
              checked = incognitoAny,
              onCheckedChange = onIncognitoMasterChange,
              modifier = Modifier.testTag("settings_incognito_master_switch")
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text("Hide Online & Typing Status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(
                "Keep your green dot offline and pause outgoing typing indicators.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = incognitoHideStatus,
              onCheckedChange = onIncognitoHideStatusChange,
              modifier = Modifier.testTag("settings_incognito_hide_status_switch")
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text("Private Story & Profile Viewing", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(
                "Browse stories without incrementing view counts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = incognitoPrivateViewing,
              onCheckedChange = onIncognitoPrivateViewingChange,
              modifier = Modifier.testTag("settings_incognito_private_viewing_switch")
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Disappearing Chat (24h Default)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              Text(
                "New messages sent in Messenger automatically expire after 24 hours.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = disappearingChat24h,
              onCheckedChange = onDisappearingChat24hChange,
              modifier = Modifier.testTag("settings_incognito_disappearing_chat_switch")
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Safety guardrail: Blocking & reporting remain active inside Incognito mode so privacy options cannot be used for harassment or spam.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
          }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Enterprise Security Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          SecurityStatusLine(
            label = "Encrypted Vault",
            value = if (vault.isHardwareBacked) "Hardware Keystore (AES-256 GCM)" else "Ephemeral Memory Vault",
            isHealthy = true
          )
          SecurityStatusLine(
            label = "Network & SSL Pinning",
            value = "TLS 1.2/1.3 + GTS Root Pins + CT",
            isHealthy = true
          )
          SecurityStatusLine(
            label = "Device Integrity",
            value = when (report.riskLevel) {
              SecurityRiskLevel.CLEAN -> "Verified Clean"
              SecurityRiskLevel.PREVIEW_WARNING -> "Debug Preview Emulator"
              SecurityRiskLevel.BLOCKED -> "Blocked"
            },
            isHealthy = report.riskLevel != SecurityRiskLevel.BLOCKED
          )
          SecurityStatusLine(
            label = "Session Verification",
            value = if (hasFreshSession) "Freshly Verified (<15m)" else "Re-auth Needed for Critical Actions",
            isHealthy = hasFreshSession
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Biometric & Passkey 2-Step", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              Text(
                text = when (biometricAvailability) {
                  BiometricAvailability.AVAILABLE -> "Require Fingerprint, Face Unlock, or Passkey after login."
                  BiometricAvailability.NONE_ENROLLED -> "Set up a screen lock, fingerprint, or face unlock in Android Settings first."
                  BiometricAvailability.UNAVAILABLE -> "Biometric hardware unavailable on this device."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = biometricEnabled,
              modifier = Modifier.testTag("biometric_lock_switch"),
              onCheckedChange = { targetState ->
                if (biometricAvailability != BiometricAvailability.AVAILABLE && targetState) {
                  securityStatusNotice = "Enroll a device screen lock or biometric credential in Android Settings to enable 2-step lock."
                } else {
                  val fragActivity = context.findFragmentActivity()
                  if (fragActivity != null && biometricAvailability == BiometricAvailability.AVAILABLE) {
                    BiometricAuthHelper.authenticate(
                      activity = fragActivity,
                      title = if (targetState) "Enable Biometric Vault" else "Disable Biometric Vault",
                      subtitle = "Verify identity to update enterprise vault lock",
                      onSuccess = {
                        biometricEnabled = targetState
                        securityStatusNotice = null
                        socialVm.syncBiometricSetting(vault, targetState, report)
                      },
                      onError = { err -> securityStatusNotice = err }
                    )
                  } else if (!targetState) {
                    biometricEnabled = false
                    socialVm.syncBiometricSetting(vault, false, report)
                  } else {
                    securityStatusNotice = "FragmentActivity unavailable to launch BiometricPrompt."
                  }
                }
              }
            )
          }

          if (BuildConfig.DEBUG) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                  Text("Strict Tamper Lock (Preview)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                  text = "Release builds block root, emulator, and debuggers automatically. Toggle to test block gate in debug.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = strictDebugMode,
                modifier = Modifier.testTag("strict_tamper_switch"),
                onCheckedChange = onStrictDebugModeChange
              )
            }
          }

          securityStatusNotice?.let { msg ->
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }

          OutlinedButton(
            onClick = {
              val act = context.findFragmentActivity()
              if (act == null) {
                securityStatusNotice = "Activity unavailable for Google re-authentication."
              } else {
                socialVm.reauthenticateSession(act, vault, report) {
                  hasFreshSession = vault.hasFreshSession()
                  securityStatusNotice = null
                }
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("reauthenticate_button")
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Re-authenticate Google Session")
          }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.recoveryTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            "Signed in email: ${priv?.email?.ifBlank { "Google Account" } ?: "Google Account"}",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            strings.recoverySubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = recoveryEmail,
            onValueChange = {
              recoveryEmail = it
              recoveryStatus = null
            },
            label = { Text(strings.recoveryEmailLabel) },
            placeholder = { Text("name@gmail.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_email_input")
          )
          OutlinedTextField(
            value = phone,
            onValueChange = {
              phone = it
              recoveryStatus = null
            },
            label = { Text(strings.recoveryPhoneLabel) },
            placeholder = { Text("+8801XXXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_phone_input")
          )
          recoveryStatus?.let { (ok, message) ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().testTag("recovery_save_status")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = if (ok) Icons.Rounded.Check else Icons.Rounded.Close,
                  contentDescription = null,
                  tint = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = message,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
              }
            }
          }
          Button(
            onClick = {
              isSavingRecovery = true
              recoveryStatus = null
              val p = priv
              socialVm.updateAccountSettings(
                notifLikes = p?.notifLikes ?: true,
                notifComments = p?.notifComments ?: true,
                notifFriends = p?.notifFriends ?: true,
                notifMessages = p?.notifMessages ?: true,
                notifSecretLove = p?.notifSecretLove ?: true,
                notifSos = p?.notifSos ?: true,
                email = recoveryEmail,
                phone = phone,
                isDeactivated = p?.isDeactivated ?: false
              ) { res ->
                isSavingRecovery = false
                recoveryStatus = res.fold(
                  onSuccess = { true to strings.recoverySaved },
                  onFailure = { false to (it.localizedMessage ?: "Failed to save recovery contact") }
                )
              }
            },
            enabled = !isSavingRecovery,
            modifier = Modifier.fillMaxWidth().testTag("save_recovery_contact_button")
          ) {
            if (isSavingRecovery) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Spacer(Modifier.width(8.dp))
            }
            Text(strings.saveRecovery)
          }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(selected = currentThemeMode == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.label.substringBefore(" ")) })
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppLanguage.entries) { lang ->
              FilterChip(
                selected = currentLanguage == lang,
                onClick = { onLanguageChange(lang) },
                label = { Text(lang.label) },
                modifier = Modifier.testTag("language_chip_${lang.name.lowercase()}")
              )
            }
          }
        }
      }
    }

    item {
      val currentRegionObj = HELLO_FEED_REGIONS.firstOrNull {
        it.code.equals(selectedRegion, ignoreCase = true)
      } ?: HELLO_FEED_REGIONS.first()
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.testTag("settings_reels_section")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.SlowMotionVideo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column {
                Text("Reels & Smart Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  text = if (feedMode == "general") "Global Mix · Personalized curation" else "${currentRegionObj.flag} ${currentRegionObj.label} Focus",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            ) {
              Text(
                text = selectedReelCategory ?: "For You",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text("Auto-Advance Reels", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (reelsAutoAdvance) "Automatically moves to the next clip when playback finishes" else "Loops the current clip continuously",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = reelsAutoAdvance,
              onCheckedChange = { enabled ->
                socialVm.setReelsAutoAdvance(enabled)
                onReelsAutoAdvancePersist(enabled)
              },
              modifier = Modifier.testTag("settings_reels_auto_advance_switch")
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Text("Feed Region & Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(HELLO_FEED_REGIONS) { opt ->
                val isSelected = selectedRegion.equals(opt.code, ignoreCase = true)
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    val mode = if (opt.isGeneral) "general" else "country"
                    socialVm.setFeedRegion(mode = mode, regionCode = opt.code)
                    onReelsRegionPersist(if (opt.isGeneral) "GLOBAL" else opt.code.uppercase())
                  },
                  label = {
                    Text("${opt.flag} ${if (opt.isGeneral) "General (Global)" else opt.label}")
                  },
                  modifier = Modifier.testTag("settings_reels_region_${opt.code.lowercase()}")
                )
              }
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Text("Feed Category Focus", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            val activeCategories = remember(feedPrefs) {
              FeedPreferences.CATEGORIES.filter { cat ->
                feedPrefs.dislikedCategories.none { it.equals(cat, ignoreCase = true) }
              }.sortedByDescending { feedPrefs.categoryScores[it] ?: 6L }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                FilterChip(
                  selected = selectedReelCategory == null,
                  onClick = { socialVm.selectReelCategory(null) },
                  label = { Text("For You") },
                  modifier = Modifier.testTag("settings_reels_category_for_you")
                )
              }
              items(activeCategories) { cat ->
                FilterChip(
                  selected = selectedReelCategory.equals(cat, ignoreCase = true),
                  onClick = { socialVm.selectReelCategory(cat) },
                  label = { Text(cat) },
                  modifier = Modifier.testTag("settings_reels_category_${cat.lowercase()}")
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { showTuningDetails = !showTuningDetails }
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
              Column {
                Text("Interest Curation & Signals", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                  text = "${feedPrefs.totalViews} views · ${feedPrefs.totalLikes} likes · ${feedPrefs.totalFullWatches} full watches",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Icon(
              imageVector = if (showTuningDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription = if (showTuningDetails) "Collapse interest details" else "Expand interest details",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          AnimatedVisibility(visible = showTuningDetails) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              val maxScore = remember(feedPrefs) {
                (feedPrefs.categoryScores.values.maxOrNull() ?: 10L).coerceAtLeast(10L).toFloat()
              }
              FeedPreferences.CATEGORIES.forEach { cat ->
                val score = feedPrefs.categoryScores[cat] ?: 10L
                val isMuted = feedPrefs.dislikedCategories.any { it.equals(cat, ignoreCase = true) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (isMuted) "$cat (Hidden)" else cat,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (isMuted) FontWeight.Normal else FontWeight.SemiBold,
                      color = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (isMuted) {
                      TextButton(
                        onClick = { socialVm.restoreCategoryPreference(cat) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                      ) {
                        Text("Restore", style = MaterialTheme.typography.labelSmall)
                      }
                    } else {
                      Text(
                        text = "$score pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                  }
                  LinearProgressIndicator(
                    progress = { if (isMuted) 0f else (score.toFloat() / maxScore).coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
                  )
                }
              }

              if (youtubeStatus is YouTubeFetchResult.MissingApiKey) {
                Text(
                  text = "Configure YOUTUBE_API_KEY in AI Studio Secrets to stream fresh category YouTube Shorts.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          if (isFetchingShorts) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = { socialVm.ensureYouTubeShortsLoaded(forceRefresh = true) },
              enabled = !isFetchingShorts,
              modifier = Modifier.weight(1f).testTag("settings_refresh_shorts_button")
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Refresh Clips", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
              onClick = { socialVm.resetFeedPreferences() },
              modifier = Modifier.weight(1f).testTag("settings_reset_feed_weights_button")
            ) {
              Text("Reset Interests", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    priv?.let { p ->
      item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Notification Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            PrefSwitch("Likes & Reactions", p.notifLikes) { socialVm.updateAccountSettings(it, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, recoveryEmail, phone, p.isDeactivated) }
            PrefSwitch("Comments & Replies", p.notifComments) { socialVm.updateAccountSettings(p.notifLikes, it, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, recoveryEmail, phone, p.isDeactivated) }
            PrefSwitch("Friend Requests", p.notifFriends) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, it, p.notifMessages, p.notifSecretLove, p.notifSos, recoveryEmail, phone, p.isDeactivated) }
            PrefSwitch("Messenger Chats", p.notifMessages) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, it, p.notifSecretLove, p.notifSos, recoveryEmail, phone, p.isDeactivated) }
            PrefSwitch("Secret Love Notes", p.notifSecretLove) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, it, p.notifSos, recoveryEmail, phone, p.isDeactivated) }
            PrefSwitch("Emergency SOS Alerts", p.notifSos) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, it, recoveryEmail, phone, p.isDeactivated) }
          }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Blocked Users (${blocked.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          if (blocked.isEmpty()) {
            Text("You haven't blocked anyone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            blocked.forEach { b ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(b.blockedName)
                TextButton(onClick = { communityVm.unblockUser(b.blockedUid) }) { Text("Unblock") }
              }
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        priv?.let { p ->
          OutlinedButton(
            onClick = {
              socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, recoveryEmail, phone, !p.isDeactivated)
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(if (p.isDeactivated) "Reactivate Account" else "Deactivate Account Temporarily")
          }
        }
        OutlinedButton(
          onClick = { socialVm.signOut(context, vault) },
          modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Rounded.ExitToApp, null)
          Spacer(Modifier.width(6.dp))
          Text("Sign Out")
        }
        Button(
          onClick = { showDeleteConfirm = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
        ) {
          Icon(Icons.Rounded.DeleteForever, null)
          Spacer(Modifier.width(6.dp))
          Text("Delete Account")
        }
      }
    }
  }

  if (showDeleteConfirm) {
    val needsReauth = !vault.hasFreshSession()
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Your Account?") },
      text = {
        Text(
          if (needsReauth) {
            "For maximum security, deleting your account requires fresh Google re-authentication (within 15 minutes) before removing your data."
          } else {
            "Your session is freshly verified. Confirming will permanently delete your private account settings and Firebase authentication profile."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            val act = context.findFragmentActivity()
            if (needsReauth && act != null) {
              socialVm.reauthenticateSession(act, vault, report) {
                hasFreshSession = vault.hasFreshSession()
                socialVm.deleteOwnAccount(vault) {}
              }
            } else if (act != null && biometricAvailability == BiometricAvailability.AVAILABLE && biometricEnabled) {
              BiometricAuthHelper.authenticate(
                activity = act,
                title = "Confirm Account Deletion",
                subtitle = "Biometric verification required before deleting account",
                onSuccess = { socialVm.deleteOwnAccount(vault) {} },
                onError = { err -> securityStatusNotice = err }
              )
            } else {
              socialVm.deleteOwnAccount(vault) {}
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text(if (needsReauth) "Verify & Delete" else "Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
    )
  }
}

@Composable
private fun SecurityStatusLine(label: String, value: String, isHealthy: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )
  }
}

@Composable
private fun PrefSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Switch(checked = checked, onCheckedChange = onChange)
  }
}
