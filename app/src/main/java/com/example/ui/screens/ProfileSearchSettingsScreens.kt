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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ThemeMode
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.PostCard
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size

@Composable
fun ProfileScreen(
  socialVm: MainSocialViewModel,
  currentUserId: String,
  friendCount: Int,
  isVerifiedOwner: Boolean,
  communityVm: CommunityViewModel? = null,
  onOpenSettings: () -> Unit
) {
  val context = LocalContext.current
  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val myPosts by socialVm.myPosts.collectAsStateWithLifecycle()
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val isUploading by socialVm.isUploadingMedia.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()
  var tab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }
  var commentPost by remember { mutableStateOf<Post?>(null) }
  var shareTargetPost by remember { mutableStateOf<Post?>(null) }

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
                isOnline = me?.isOnline ?: true,
                isVerifiedOwner = isVerifiedOwner
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(me?.displayName ?: "Hello Member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${me?.handle ?: "@hello"} · ${me?.profilePrivacy ?: "public"} profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            if (!me?.bio.isNullOrBlank()) {
              Text(me!!.bio, style = MaterialTheme.typography.bodyMedium)
            }
            Text("$friendCount friends · ${me?.followersCount ?: 0} followers · ${myPosts.size} posts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f).testTag("edit_profile_button")) {
                Icon(Icons.Filled.Edit, null)
                Spacer(Modifier.width(4.dp))
                Text("Edit Profile")
              }
              OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, null)
                Spacer(Modifier.width(4.dp))
                Text("Settings")
              }
            }
          }
        }
      }
    }

    item {
      TabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Posts") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Media") })
        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Saved (${savedPosts.size})") })
      }
    }

    when (tab) {
      0 -> {
        if (myPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Filled.Person, "No posts yet", "Your posts, shared items, and privacy-controlled updates live here.") }
        } else {
          items(myPosts, key = { it.id }) { post ->
            val reactions by socialVm.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
            PostCard(
              post = post,
              currentUserId = currentUserId,
              reactions = reactions,
              isSaved = post.id in savedIds,
              onReact = { e -> socialVm.reactToPost(post, e) },
              onCommentClick = { commentPost = post },
              onShare = { n -> socialVm.sharePost(post, n) },
              onShareClick = { shareTargetPost = post },
              onToggleSave = { socialVm.toggleSave(post.id, "post", post.authorName, post.content) },
              onEdit = { c, p -> socialVm.editPost(post.id, c, p) },
              onDelete = { socialVm.deletePost(post.id) },
              onReport = { r -> socialVm.reportContent("post", post.id, r) }
            )
          }
        }
      }
      1 -> {
        val mediaPosts = myPosts.filter { it.mediaUri.isNotBlank() }
        if (mediaPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Filled.Person, "No photos or videos yet", "Photos and videos you share in posts appear here.") }
        } else {
          items(mediaPosts, key = { it.id }) { post ->
            AsyncImage(
              model = post.mediaUri,
              contentDescription = post.content,
              modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
              contentScale = ContentScale.Crop
            )
          }
        }
      }
      2 -> {
        if (savedPosts.isEmpty()) {
          item { EmptyStateCard(Icons.Filled.Bookmark, "Nothing saved yet", "Tap the bookmark icon on any post or reel to keep it handy.") }
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
    var online by remember { mutableStateOf(cur.isOnline) }
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
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show Online Status")
            Switch(checked = online, onCheckedChange = { online = it })
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          socialVm.updateProfile(context, name, bio, photo, cover, privacy, acceptLove, online)
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
    CommentsBottomSheet(comments = comments, onDismiss = { commentPost = null }, onSendComment = { t, p -> socialVm.addComment(post, t, p) })
  }

  shareTargetPost?.let { post ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${post.authorName}",
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
}

@Composable
fun UserProfileScreen(
  targetUserId: String,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  currentUserId: String,
  friendUids: List<String>,
  blockedUids: Set<String>,
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
      isVerifiedOwner = socialVm.currentUser.collectAsStateWithLifecycle().value?.isVerifiedOwner == true,
      communityVm = communityVm,
      onOpenSettings = onOpenSettings
    )
    return
  }

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val profileState by socialVm.observePublicUserProfile(targetUserId).collectAsStateWithLifecycle(initialValue = UiState.Loading)
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val discoverableUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val cachedUser = remember(discoverableUsers, targetUserId) {
    discoverableUsers.firstOrNull { it.userId == targetUserId }
  }
  val profilePosts by socialVm.observeProfilePosts(targetUserId).collectAsStateWithLifecycle(initialValue = emptyList())
  val storiesState by socialVm.stories.collectAsStateWithLifecycle()
  val userStories = remember(storiesState, targetUserId) {
    ((storiesState as? UiState.Success)?.data.orEmpty()).filter { it.authorId == targetUserId }
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
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = targetUser?.displayName ?: "Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (targetUser != null) {
            Text(
              text = targetUser.handle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Box {
          IconButton(onClick = { showOverflowMenu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Profile options")
          }
          DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
            if (isBlocked) {
              DropdownMenuItem(
                text = { Text("Unblock user") },
                leadingIcon = { Icon(Icons.Filled.Block, null) },
                onClick = {
                  showOverflowMenu = false
                  communityVm.unblockUser(targetUserId)
                }
              )
            } else {
              DropdownMenuItem(
                text = { Text("Block user") },
                leadingIcon = { Icon(Icons.Filled.Block, null) },
                onClick = {
                  showOverflowMenu = false
                  communityVm.blockUser(targetUserId, targetUser?.displayName ?: "Member")
                }
              )
            }
            DropdownMenuItem(
              text = { Text("Report profile") },
              leadingIcon = { Icon(Icons.Filled.Flag, null) },
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
          icon = Icons.Filled.Lock,
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
                  isOnline = targetUser.isOnline,
                  isVerifiedOwner = targetUser.isVerifiedOwner
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(targetUser.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                      imageVector = when (targetUser.profilePrivacy) {
                        "friends" -> Icons.Filled.Group
                        "private" -> Icons.Filled.Lock
                        else -> Icons.Filled.Public
                      },
                      contentDescription = targetUser.profilePrivacy,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "${targetUser.handle} · ${targetUser.profilePrivacy} profile · ${if (targetUser.isOnline) "Online" else "Offline"}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              if (targetUser.bio.isNotBlank()) {
                Text(targetUser.bio, style = MaterialTheme.typography.bodyMedium)
              }

              Text(
                text = "${targetUser.followersCount} followers · ${profilePosts.size} posts · ${userStories.size} stories",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )

              if (isBlocked) {
                OutlinedButton(
                  onClick = { communityVm.unblockUser(targetUserId) },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Filled.Block, null)
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
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Friends")
                      }
                    }
                    incomingRequest != null -> {
                      Button(
                        onClick = { communityVm.acceptFriendRequest(incomingRequest) },
                        modifier = Modifier.weight(1f).testTag("profile_accept_friend_button")
                      ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                      }
                      OutlinedButton(
                        onClick = { communityVm.rejectOrCancelRequest(incomingRequest.id) }
                      ) {
                        Icon(Icons.Filled.Close, contentDescription = "Decline")
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
                        Icon(Icons.Filled.PersonAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Friend")
                      }
                    }
                  }

                  Button(
                    onClick = { communityVm.startDirectChat(effectiveMe, targetUser, onOpenChat) },
                    modifier = Modifier.weight(1f).testTag("profile_message_button")
                  ) {
                    Icon(Icons.Filled.Message, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Message")
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
            icon = Icons.Filled.Block,
            title = "You blocked ${targetUser.displayName}",
            subtitle = "Unblock them to view their posts, stories, and send messages."
          )
        }
      } else if (isRestrictedFriendsOnly) {
        item {
          EmptyStateCard(
            icon = Icons.Filled.Group,
            title = "Friends-Only Profile",
            subtitle = "Add ${targetUser.displayName} as a friend to see their posts, photos, and stories."
          )
        }
      } else {
        item {
          TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Posts (${profilePosts.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Media") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Stories (${userStories.size})") })
          }
        }

        when (tab) {
          0 -> {
            if (profilePosts.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Filled.Person,
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
                  onReact = { e -> socialVm.reactToPost(post, e) },
                  onCommentClick = { commentPost = post },
                  onShare = { n -> socialVm.sharePost(post, n) },
                  onShareClick = { shareTargetPost = post },
                  onToggleSave = { socialVm.toggleSave(post.id, "post", post.authorName, post.content) },
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
                  icon = Icons.Filled.Person,
                  title = "No shared media yet",
                  subtitle = "Photos and videos shared by ${targetUser.displayName} will appear here."
                )
              }
            } else {
              items(mediaPosts, key = { it.id }) { post ->
                if (post.mediaType == "video") {
                  VideoPlayerBox(videoUri = post.mediaUri)
                } else {
                  AsyncImage(
                    model = post.mediaUri,
                    contentDescription = post.content.ifBlank { "Shared photo" },
                    modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                  )
                }
              }
            }
          }
          2 -> {
            if (userStories.isEmpty()) {
              item {
                EmptyStateCard(
                  icon = Icons.Filled.AutoAwesome,
                  title = "No active stories",
                  subtitle = "${targetUser.displayName} has no visible stories right now."
                )
              }
            } else {
              items(userStories, key = { it.id }) { story ->
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
                          Icons.Filled.PlayArrow,
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
    CommentsBottomSheet(comments = comments, onDismiss = { commentPost = null }, onSendComment = { t, p -> socialVm.addComment(post, t, p) })
  }

  shareTargetPost?.let { post ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Post by ${post.authorName}",
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

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val users = ((communityVm.discoverableUsers.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val posts = ((socialVm.publicFeed.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val groups by spacesVm.publicGroups.collectAsStateWithLifecycle()
  val pages = ((spacesVm.pages.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val events = ((spacesVm.events.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val listings = ((spacesVm.listings.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Search HELLO", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Search people, posts, groups, pages, events, market...") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
      singleLine = true
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categories) { c -> FilterChip(selected = scopeTab == c, onClick = { scopeTab = c }, label = { Text(c) }) }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (scopeTab in listOf("All", "People")) {
        val mUsers = users.filter { query.isBlank() || it.displayName.contains(query, true) || it.handle.contains(query, true) }
        items(mUsers.take(10), key = { "u_${it.userId}" }) { u ->
          Card(modifier = Modifier.fillMaxWidth().clickable {
            onOpenProfile(u.userId)
          }) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              UserAvatar(name = u.displayName, photoUrl = u.photoUrl, size = 38.dp, isOnline = u.isOnline, isVerifiedOwner = u.isVerifiedOwner)
              Column(modifier = Modifier.weight(1f)) {
                Text("Person · ${u.displayName}", fontWeight = FontWeight.SemiBold)
                Text(u.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = {
                if (me != null) communityVm.startDirectChat(me!!, u, onOpenChat)
              }) {
                Icon(Icons.Filled.Message, contentDescription = "Message ${u.displayName}")
              }
            }
          }
        }
      }
      if (scopeTab in listOf("All", "Posts")) {
        val mPosts = posts.filter { query.isBlank() || it.content.contains(query, true) || it.authorName.contains(query, true) }
        items(mPosts.take(10), key = { "p_${it.id}" }) { p ->
          Card(modifier = Modifier.fillMaxWidth().clickable { onOpenProfile(p.authorId) }) {
            Column(Modifier.padding(12.dp)) {
              Text("Post by ${p.authorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Text(p.content, maxLines = 2)
            }
          }
        }
      }
      if (scopeTab in listOf("All", "Groups")) {
        val mGroups = groups.filter { query.isBlank() || it.name.contains(query, true) }
        items(mGroups.take(8), key = { "g_${it.id}" }) { g ->
          Card(modifier = Modifier.fillMaxWidth().clickable { onOpenGroup(g.id) }) {
            Column(Modifier.padding(12.dp)) { Text("Group · ${g.name}", fontWeight = FontWeight.SemiBold); Text(g.description, style = MaterialTheme.typography.bodySmall) }
          }
        }
      }
      if (scopeTab in listOf("All", "Pages")) {
        val mPages = pages.filter { query.isBlank() || it.name.contains(query, true) }
        items(mPages.take(8), key = { "pg_${it.id}" }) { pg ->
          Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPages)) {
            Column(Modifier.padding(12.dp)) { Text("Page · ${pg.name}", fontWeight = FontWeight.SemiBold); Text(pg.category, style = MaterialTheme.typography.bodySmall) }
          }
        }
      }
      if (scopeTab in listOf("All", "Events")) {
        val mEvents = events.filter { query.isBlank() || it.title.contains(query, true) }
        items(mEvents.take(8), key = { "ev_${it.id}" }) { ev ->
          Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenEvents)) {
            Column(Modifier.padding(12.dp)) { Text("Event · ${ev.title}", fontWeight = FontWeight.SemiBold); Text("${ev.eventDateText} · ${ev.locationName}", style = MaterialTheme.typography.bodySmall) }
          }
        }
      }
      if (scopeTab in listOf("All", "Marketplace")) {
        val mListings = listings.filter { query.isBlank() || it.title.contains(query, true) }
        items(mListings.take(8), key = { "mk_${it.id}" }) { mk ->
          Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMarketplace)) {
            Column(Modifier.padding(12.dp)) { Text("Marketplace · ${mk.title} (${mk.currency}${mk.price})", fontWeight = FontWeight.SemiBold); Text(mk.category, style = MaterialTheme.typography.bodySmall) }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsScreen(
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  currentThemeMode: ThemeMode,
  currentLanguage: AppLanguage,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit
) {
  val context = LocalContext.current
  val priv by socialVm.privateUser.collectAsStateWithLifecycle()
  val blocked by communityVm.blockedUsers.collectAsStateWithLifecycle()
  var phone by remember(priv?.phone) { mutableStateOf(priv?.phone ?: "") }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Account & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("Signed in email: ${priv?.email ?: "Google Account"}", style = MaterialTheme.typography.bodyMedium)
          Text("Password & 2-Step Verification are protected through your Google Account security settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Recovery / contact phone") }, modifier = Modifier.fillMaxWidth())
          Button(onClick = {
            val p = priv ?: return@Button
            socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, phone, p.isDeactivated)
          }) { Text("Save Account Contact") }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Theme & Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(selected = currentThemeMode == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.label.substringBefore(" ")) })
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppLanguage.entries) { lang ->
              FilterChip(selected = currentLanguage == lang, onClick = { onLanguageChange(lang) }, label = { Text(lang.label) })
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
            PrefSwitch("Likes & Reactions", p.notifLikes) { socialVm.updateAccountSettings(it, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, phone, p.isDeactivated) }
            PrefSwitch("Comments & Replies", p.notifComments) { socialVm.updateAccountSettings(p.notifLikes, it, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, phone, p.isDeactivated) }
            PrefSwitch("Friend Requests", p.notifFriends) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, it, p.notifMessages, p.notifSecretLove, p.notifSos, phone, p.isDeactivated) }
            PrefSwitch("Messenger Chats", p.notifMessages) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, it, p.notifSecretLove, p.notifSos, phone, p.isDeactivated) }
            PrefSwitch("Secret Love Notes", p.notifSecretLove) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, it, p.notifSos, phone, p.isDeactivated) }
            PrefSwitch("Emergency SOS Alerts", p.notifSos) { socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, it, phone, p.isDeactivated) }
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
              socialVm.updateAccountSettings(p.notifLikes, p.notifComments, p.notifFriends, p.notifMessages, p.notifSecretLove, p.notifSos, phone, !p.isDeactivated)
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(if (p.isDeactivated) "Reactivate Account" else "Deactivate Account Temporarily")
          }
        }
        OutlinedButton(
          onClick = { socialVm.signOut(context) },
          modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
          Spacer(Modifier.width(6.dp))
          Text("Sign Out")
        }
        Button(
          onClick = { showDeleteConfirm = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.DeleteForever, null)
          Spacer(Modifier.width(6.dp))
          Text("Delete Account")
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Your Account?") },
      text = { Text("This deletes your private account settings and Firebase authentication profile.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            socialVm.deleteOwnAccount {}
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
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
