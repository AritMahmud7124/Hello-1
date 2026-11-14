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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.UiState
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ThemeMode
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun ProfileScreen(
  socialVm: MainSocialViewModel,
  currentUserId: String,
  friendCount: Int,
  isVerifiedOwner: Boolean,
  onOpenSettings: () -> Unit
) {
  val context = LocalContext.current
  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val myPosts by socialVm.myPosts.collectAsStateWithLifecycle()
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val isUploading by socialVm.isUploadingMedia.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  var tab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }
  var commentPost by remember { mutableStateOf<Post?>(null) }

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

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) photo = uri.toString()
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) cover = uri.toString()
    }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Edit Profile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Avatar") }
            OutlinedButton(onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Cover") }
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
  }

  commentPost?.let { post ->
    val comments by socialVm.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(comments = comments, onDismiss = { commentPost = null }, onSendComment = { t, p -> socialVm.addComment(post, t, p) })
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
  onOpenChat: (String) -> Unit
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
            if (me != null) communityVm.startDirectChat(me!!, u, onOpenChat)
          }) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              UserAvatar(name = u.displayName, photoUrl = u.photoUrl, size = 38.dp, isOnline = u.isOnline)
              Column {
                Text("Person · ${u.displayName}", fontWeight = FontWeight.SemiBold)
                Text(u.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
      if (scopeTab in listOf("All", "Posts")) {
        val mPosts = posts.filter { query.isBlank() || it.content.contains(query, true) || it.authorName.contains(query, true) }
        items(mPosts.take(10), key = { "p_${it.id}" }) { p ->
          Card { Column(Modifier.padding(12.dp)) { Text("Post by ${p.authorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(p.content, maxLines = 2) } }
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
