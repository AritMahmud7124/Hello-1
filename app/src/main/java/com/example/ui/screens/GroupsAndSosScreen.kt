package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GlobalSettings
import com.example.data.model.Group
import com.example.data.model.Post
import com.example.data.model.PublicUser
import com.example.data.model.SosAlert
import com.example.data.model.UiState
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PostCard
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.formatTimestamp
import com.example.ui.components.openLocationInGoogleMaps
import com.example.ui.components.resolveLiveAuthor
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@Composable
fun GroupsScreen(
  viewModel: SpacesGovernanceViewModel,
  currentUserId: String,
  onOpenGroup: (String) -> Unit,
  onOpenSos: () -> Unit = {}
) {
  val publicGroups by viewModel.publicGroups.collectAsStateWithLifecycle()
  val myGroups by viewModel.myGroups.collectAsStateWithLifecycle()
  var tab by remember { mutableStateOf(0) }
  var showCreateDialog by remember { mutableStateOf(false) }

  val shown = if (tab == 0) myGroups else publicGroups

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Groups & SOS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Private circles with group-only Emergency SOS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreateDialog = true }, modifier = Modifier.testTag("create_group_button")) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Create")
      }
    }

    // Quick Emergency SOS access integrated inside Groups tab
    Card(
      onClick = onOpenSos,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("groups_open_sos_button")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          Icons.Rounded.HealthAndSafety,
          contentDescription = "Emergency SOS",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(26.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
          Text("Emergency SOS Center", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
          Text("Private safety alerts with your group members & friends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        Button(
          onClick = onOpenSos,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text("Open SOS")
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("My Groups (${myGroups.size})") })
      FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Discover (${publicGroups.size})") })
    }

    if (shown.isEmpty()) {
      EmptyStateCard(
        icon = Icons.Rounded.Groups,
        title = if (tab == 0) "You haven't joined a group yet" else "No public groups yet",
        subtitle = "Create a family, neighborhood, or hobby group with optional Emergency SOS alerts.",
        actionLabel = "Create Group",
        onAction = { showCreateDialog = true }
      )
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(shown, key = { it.id }) { group ->
          val isMember = currentUserId in group.memberUids
          val isCreator = currentUserId == group.creatorId
          val isPrivate = group.privacy == "private"
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenGroup(group.id) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isPrivate) {
                      Icon(Icons.Rounded.Lock, contentDescription = "Private group", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  }
                  Text("${group.category} · ${group.privacy} · ${group.membersCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isCreator) {
                  Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                      text = "Admin",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                } else if (isMember) {
                  OutlinedButton(
                    onClick = { viewModel.toggleMembership(group) },
                    modifier = Modifier.testTag("group_out_${group.id}")
                  ) {
                    Icon(Icons.Rounded.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Out")
                  }
                } else if (!isPrivate) {
                  Button(
                    onClick = { viewModel.toggleMembership(group) },
                    modifier = Modifier.testTag("group_join_${group.id}")
                  ) {
                    Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Join")
                  }
                } else {
                  Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Text(
                      text = "Invite Only",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
              if (group.description.isNotBlank()) {
                Text(group.description, style = MaterialTheme.typography.bodyMedium)
              }
              if (group.sosEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Rounded.Shield, contentDescription = "SOS enabled", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                  Text("Private Emergency SOS Enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Neighborhood") }
    var privacy by remember { mutableStateOf("public") }
    var sosEnabled by remember { mutableStateOf(true) }

    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Create Group") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = privacy == "public", onClick = { privacy = "public" }, label = { Text("Public") })
            FilterChip(selected = privacy == "private", onClick = { privacy = "private" }, label = { Text("Private") })
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Emergency SOS", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = sosEnabled, onCheckedChange = { sosEnabled = it })
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.createGroup(name, desc, category, privacy, sosEnabled)
          showCreateDialog = false
        }) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
    )
  }
}

@SuppressLint("MissingPermission")
@Composable
fun GroupDetailScreen(
  groupId: String,
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  globalSettings: GlobalSettings,
  currentUserId: String,
  friendUids: List<String>,
  onBack: () -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val myGroups by spacesVm.myGroups.collectAsStateWithLifecycle()
  val publicGroups by spacesVm.publicGroups.collectAsStateWithLifecycle()
  val group = remember(myGroups, publicGroups, groupId) {
    (myGroups + publicGroups).firstOrNull { it.id == groupId }
  }
  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }
  val feedState by socialVm.publicFeed.collectAsStateWithLifecycle()
  val myPosts by socialVm.myPosts.collectAsStateWithLifecycle()
  val sharedFriendsPosts by socialVm.friendsSharedPosts.collectAsStateWithLifecycle()
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val sosAlerts by spacesVm.observeGroupSosAlerts(groupId).collectAsStateWithLifecycle(initialValue = emptyList())

  val isPrivateGroup = group?.privacy == "private"
  val isGroupAdmin = group != null && (currentUserId == group.creatorId || currentUserId in group.adminUids)

  // Block screenshots & screen capture while viewing a Private Group
  DisposableEffect(context, isPrivateGroup) {
    val activity = context.findActivity()
    if (isPrivateGroup) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    onDispose {
      if (isPrivateGroup) {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }
  }

  var showPostDialog by remember { mutableStateOf(false) }
  var showManageMembersDialog by remember { mutableStateOf(false) }
  var showDeleteGroupConfirm by remember { mutableStateOf(false) }
  var showSosDialog by remember { mutableStateOf(false) }
  var showSosHistory by remember { mutableStateOf(true) }
  var activeCommentPost by remember { mutableStateOf<Post?>(null) }
  var sharePostTarget by remember { mutableStateOf<Post?>(null) }
  var shareGroupSheet by remember { mutableStateOf(false) }

  val groupPosts = remember(feedState, myPosts, sharedFriendsPosts, groupId) {
    val pub = (feedState as? UiState.Success)?.data.orEmpty()
    (pub + myPosts + sharedFriendsPosts).distinctBy { it.id }.filter { it.groupId == groupId }.sortedByDescending { it.createdAt?.seconds ?: 0L }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to groups")
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isPrivateGroup) {
              Icon(Icons.Rounded.Lock, contentDescription = "Private group", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Text(group?.name ?: "Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Text(
            text = "${group?.membersCount ?: 0} members · ${group?.privacy ?: "public"}${if (isGroupAdmin) " · Admin" else ""}",
            style = MaterialTheme.typography.labelSmall
          )
        }
        if (group != null && !isPrivateGroup) {
          IconButton(onClick = { shareGroupSheet = true }, modifier = Modifier.testTag("share_group_button")) {
            Icon(Icons.Rounded.Share, contentDescription = "Share group")
          }
        }
        if (isGroupAdmin) {
          IconButton(
            onClick = { showManageMembersDialog = true },
            modifier = Modifier.testTag("manage_group_members_button")
          ) {
            Icon(Icons.Rounded.GroupAdd, contentDescription = "Manage group members (Admin)")
          }
        }
      }
    }

    if (group == null) {
      EmptyStateCard(Icons.Rounded.Groups, "Group not found", "Return to Groups to pick another circle.", actionLabel = "Go Back", onAction = onBack, modifier = Modifier.padding(16.dp))
      return
    }

    val isMember = currentUserId in group.memberUids
    val activeMyLiveAlert = sosAlerts.firstOrNull { it.senderId == currentUserId && it.status == "active" }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isGroupAdmin) {
            Button(
              onClick = { showManageMembersDialog = true },
              modifier = Modifier.weight(1f).testTag("group_add_out_button")
            ) {
              Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Add / Out Members")
            }
          }
          if (currentUserId == group.creatorId) {
            OutlinedButton(
              onClick = { showDeleteGroupConfirm = true },
              modifier = Modifier.testTag("delete_group_button")
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Delete Group", color = MaterialTheme.colorScheme.error)
            }
          } else if (isMember) {
            OutlinedButton(
              onClick = {
                spacesVm.toggleMembership(group)
                if (isPrivateGroup) onBack()
              },
              modifier = Modifier.weight(1f).testTag("leave_group_button")
            ) {
              Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Out (Leave Group)", color = MaterialTheme.colorScheme.error)
            }
          } else if (!isPrivateGroup) {
            Button(
              onClick = { spacesVm.toggleMembership(group) },
              modifier = Modifier.weight(1f).testTag("join_group_detail_button")
            ) {
              Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Join Group")
            }
          }
        }
      }
      if (isPrivateGroup) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("private_group_security_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              Text(
                text = "High-Security Private Group · Admin-only add/remove · Screenshots & sharing blocked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
      if (activeMyLiveAlert != null) {
        item {
          Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "Active SOS", tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                  text = if (activeMyLiveAlert.shareLiveLocation) "Live Location Sharing ACTIVE (private to ${activeMyLiveAlert.recipientUids.size} member(s))" else "Active Emergency SOS Sent",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onErrorContainer
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { spacesVm.stopOrCancelSos(groupId, activeMyLiveAlert.id, "stopped") },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.testTag("stop_sos_sharing_button")
                ) {
                  Icon(Icons.Rounded.LocationOff, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Stop Sharing")
                }
                OutlinedButton(
                  onClick = { spacesVm.stopOrCancelSos(groupId, activeMyLiveAlert.id, "cancelled") },
                  modifier = Modifier.testTag("cancel_sos_button")
                ) {
                  Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Cancel Emergency")
                }
              }
            }
          }
        }
      }

      if (group.sosEnabled && globalSettings.sosFeatureEnabled && isMember) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Group Emergency SOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Notify selected members privately. Location is never public and requires your explicit consent.", style = MaterialTheme.typography.bodySmall)
              }
              Spacer(Modifier.width(10.dp))
              Button(
                onClick = { showSosDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("trigger_sos_button")
              ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = "SOS")
                Spacer(Modifier.width(4.dp))
                Text("SOS")
              }
            }
          }
        }
      }

      if (sosAlerts.isNotEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sos_history_box")
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .clickable { showSosHistory = !showSosHistory }
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    "Private SOS History (${sosAlerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  if (showSosHistory && sosAlerts.size > 1) {
                    Text(
                      "Scroll",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Icon(
                    imageVector = if (showSosHistory) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (showSosHistory) "Collapse SOS History" else "Expand SOS History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (showSosHistory) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("sos_history_scroll_list"),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  sosAlerts.forEach { alert ->
                    val hasCoords = alert.shareLiveLocation && alert.status == "active" && (alert.latitude != 0.0 || alert.longitude != 0.0)
                    val alertSenderName = resolveLiveAuthor(alert.senderId, alert.senderName, "", usersById, me).displayName
                    Card(
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            "$alertSenderName · ${alert.status.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                          )
                          Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                            Text(
                              text = "🔒 Private · ${alert.recipientUids.size} recipient(s)",
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSecondaryContainer,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                          }
                        }
                        Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                          text = if (hasCoords) {
                            "Live Location (Private — never public): %.4f, %.4f".format(alert.latitude, alert.longitude)
                          } else {
                            "Location sharing: OFF (text alert only)"
                          },
                          style = MaterialTheme.typography.labelMedium,
                          color = if (hasCoords) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = if (hasCoords) {
                            Modifier.clickable {
                              openLocationInGoogleMaps(
                                context = context,
                                latitude = alert.latitude,
                                longitude = alert.longitude,
                                label = "$alertSenderName SOS"
                              )
                            }
                          } else Modifier
                        )
                        if (hasCoords) {
                          OutlinedButton(
                            onClick = {
                              openLocationInGoogleMaps(
                                context = context,
                                latitude = alert.latitude,
                                longitude = alert.longitude,
                                label = "$alertSenderName SOS"
                              )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_sos_map_${alert.id}")
                          ) {
                            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Private Map")
                          }
                        }
                        Text(formatTimestamp(alert.createdAt), style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Group Discussions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          if (isMember) {
            Button(onClick = { showPostDialog = true }) { Text("Post in Group") }
          } else if (!isPrivateGroup) {
            Button(onClick = { spacesVm.toggleMembership(group) }) { Text("Join to Post") }
          }
        }
      }

      if (groupPosts.isEmpty()) {
        item {
          EmptyStateCard(Icons.Rounded.Groups, "No group posts yet", "Start a discussion with ${group.name} members.")
        }
      } else {
        items(groupPosts, key = { it.id }) { post ->
          val reactions by socialVm.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
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
            onReact = { emoji -> socialVm.reactToPost(post, emoji) },
            onCommentClick = { activeCommentPost = post },
            onShare = { note -> if (!isPrivateGroup) socialVm.sharePost(post, note) },
            allowShare = !isPrivateGroup && post.privacy == "public",
            authorOnline = postAuthor.isOnline,
            displayAuthorName = postAuthor.displayName,
            displayAuthorAvatar = postAuthor.avatarUrl,
            onShareClick = { if (!isPrivateGroup) sharePostTarget = post },
            onToggleSave = { socialVm.toggleSave(post.id, "post", postAuthor.displayName, post.content) },
            onEdit = { c, p ->
              val safePrivacy = if (isPrivateGroup && p == "public") "friends" else p
              socialVm.editPost(post.id, c, safePrivacy)
            },
            onDelete = { socialVm.deletePost(post.id) },
            onReport = { r -> socialVm.reportContent("post", post.id, r) },
            onAuthorClick = { onOpenProfile(post.authorId) }
          )
        }
      }
    }

    if (showPostDialog) {
      CreatePostDialog(
        allowPublicPrivacy = !isPrivateGroup,
        onDismiss = { showPostDialog = false },
        onPublish = { content, mediaUri, mediaType, privacy, music ->
          val safePrivacy = if (isPrivateGroup) {
            if (privacy == "only_me") "only_me" else "friends"
          } else {
            privacy
          }
          val viewers = if (isPrivateGroup) group.memberUids else friendUids
          socialVm.createPost(context, content, mediaUri, mediaType, safePrivacy, viewers, groupId = groupId, music = music)
          showPostDialog = false
        },
        authorName = me?.displayName ?: "Hello Member",
        authorAvatar = me?.photoUrl ?: "",
        authorOnline = me?.effectiveOnline == true
      )
    }

    if (showManageMembersDialog && isGroupAdmin) {
      val friendSet = friendUids.toSet()
      val nonMembers = allUsers
        .filter { it.userId != currentUserId && it.userId !in group.memberUids }
        .sortedByDescending { it.userId in friendSet }
      val currentMembers = group.memberUids.map { uid ->
        allUsers.firstOrNull { it.userId == uid } ?: PublicUser(
          userId = uid,
          displayName = if (uid == currentUserId) (me?.displayName ?: "You") else "Member ${uid.take(6)}"
        )
      }
      var inviteQuery by remember { mutableStateOf("") }
      val filteredInvitees = remember(nonMembers, inviteQuery) {
        if (inviteQuery.isBlank()) nonMembers else nonMembers.filter {
          it.displayName.contains(inviteQuery, true) || it.handle.contains(inviteQuery, true)
        }
      }
      AlertDialog(
        onDismissRequest = { showManageMembersDialog = false },
        title = { Text("Group Add & Out (${group.name})") },
        text = {
          Column(
            modifier = Modifier
              .heightIn(max = 420.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Current Members (${currentMembers.size})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            currentMembers.forEach { member ->
              val isMemberCreator = member.userId == group.creatorId
              val isMemberAdmin = isMemberCreator || member.userId in group.adminUids
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(member.displayName, fontWeight = FontWeight.Medium)
                  Text(
                    text = when {
                      isMemberCreator -> "Group Creator"
                      isMemberAdmin -> "Group Admin"
                      else -> member.handle.ifBlank { "Member" }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (!isMemberCreator) {
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (currentUserId == group.creatorId) {
                      TextButton(
                        onClick = { spacesVm.toggleGroupAdmin(group, member.userId, member.displayName, !isMemberAdmin) },
                        modifier = Modifier.testTag("toggle_group_admin_${member.userId}")
                      ) {
                        Text(if (isMemberAdmin) "Admin ✓" else "Make Admin")
                      }
                    }
                    TextButton(
                      onClick = { spacesVm.removeMember(group, member.userId, member.displayName) },
                      modifier = Modifier.testTag("remove_member_${member.userId}")
                    ) {
                      Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Out", color = MaterialTheme.colorScheme.error)
                    }
                  }
                }
              }
            }

            HorizontalDivider()

            Text(
              text = "Add People",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
              value = inviteQuery,
              onValueChange = { inviteQuery = it },
              label = { Text("Search friends & people to add") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            if (filteredInvitees.isEmpty()) {
              Text(
                if (nonMembers.isEmpty()) "All discovered users are already members." else "No matching users found.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              filteredInvitees.take(25).forEach { person ->
                val isFriend = person.userId in friendSet
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable {
                      val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                      spacesVm.inviteMember(group, effectiveMe, person.userId)
                    }
                    .padding(vertical = 6.dp)
                    .testTag("add_member_${person.userId}"),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(person.displayName, fontWeight = FontWeight.Medium)
                    Text(
                      text = listOfNotNull(
                        if (isFriend) "Friend" else null,
                        person.handle.takeIf { it.isNotBlank() }
                      ).joinToString(" · ").ifBlank { "Member" },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text("+ Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { showManageMembersDialog = false }) { Text("Done") } }
      )
    }

    if (showDeleteGroupConfirm) {
      AlertDialog(
        onDismissRequest = { showDeleteGroupConfirm = false },
        title = { Text("Delete Group?") },
        text = { Text("This will permanently remove \"${group.name}\" for all members.") },
        confirmButton = {
          Button(
            onClick = {
              showDeleteGroupConfirm = false
              spacesVm.deleteGroup(group, onDeleted = onBack)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteGroupConfirm = false }) { Text("Cancel") }
        }
      )
    }

    if (showSosDialog) {
      EmergencySosDialog(
        group = group,
        allUsers = allUsers,
        friendUids = friendUids,
        isGroupAdmin = isGroupAdmin,
        currentUserId = currentUserId,
        onDismiss = { showSosDialog = false },
        onQuickAddMember = { person ->
          val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
          spacesVm.inviteMember(group, effectiveMe, person.userId)
        },
        onSendSos = { message, recipients, shareLoc, lat, lon ->
          val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
          spacesVm.sendSosAlert(group, effectiveMe, message, recipients, shareLoc, lat, lon)
          showSosDialog = false
        }
      )
    }

    activeCommentPost?.let { post ->
      val comments by socialVm.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
      CommentsBottomSheet(
        comments = comments,
        usersById = usersById,
        me = me,
        onDismiss = { activeCommentPost = null },
        onSendComment = { text, parentId -> socialVm.addComment(post, text, parentId) }
      )
    }

    if (!isPrivateGroup) {
      sharePostTarget?.let { post ->
        val shareAuthor = resolveLiveAuthor(post.authorId, post.authorName, post.authorAvatar, usersById, me)
        ShareOptionsBottomSheet(
          payload = SharePayload(
            title = "Group Post by ${shareAuthor.displayName}",
            body = post.content.ifBlank { "Shared from ${group.name}" },
            canRepostToFeed = true
          ),
          currentUserId = currentUserId,
          conversations = conversations,
          discoverableUsers = allUsers,
          onDismiss = { sharePostTarget = null },
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

      if (shareGroupSheet) {
        ShareOptionsBottomSheet(
          payload = SharePayload(
            title = "Join ${group.name} on HELLO",
            body = "${group.category} · ${group.membersCount} members\n${group.description}",
            canRepostToFeed = false
          ),
          currentUserId = currentUserId,
          conversations = conversations,
          discoverableUsers = allUsers,
          onDismiss = { shareGroupSheet = false },
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
  }
}

@SuppressLint("MissingPermission")
@Composable
private fun EmergencySosDialog(
  group: Group,
  allUsers: List<PublicUser>,
  friendUids: List<String>,
  isGroupAdmin: Boolean,
  currentUserId: String,
  onDismiss: () -> Unit,
  onQuickAddMember: (PublicUser) -> Unit,
  onSendSos: (message: String, recipients: List<String>, shareLocation: Boolean, lat: Double, lon: Double) -> Unit
) {
  val context = LocalContext.current
  val friendSet = remember(friendUids) { friendUids.toSet() }
  val candidateMembers = remember(group.memberUids, allUsers, currentUserId, friendSet) {
    group.memberUids.filter { it != currentUserId }.map { uid ->
      allUsers.firstOrNull { it.userId == uid } ?: PublicUser(userId = uid, displayName = "Member ${uid.take(6)}")
    }.sortedByDescending { it.userId in friendSet }
  }
  val friendMembers = remember(candidateMembers, friendSet) {
    candidateMembers.filter { it.userId in friendSet }
  }
  val nonMembers = remember(group.memberUids, allUsers, currentUserId, friendSet) {
    allUsers.filter { it.userId != currentUserId && it.userId !in group.memberUids }
      .sortedByDescending { it.userId in friendSet }
  }
  val selectedUids = remember { mutableStateListOf<String>() }
  var hasInitializedSelection by remember { mutableStateOf(false) }
  LaunchedEffect(candidateMembers) {
    val validIds = candidateMembers.map { it.userId }.toSet()
    selectedUids.removeAll { it !in validIds }
    if (!hasInitializedSelection && candidateMembers.isNotEmpty()) {
      selectedUids.addAll(candidateMembers.take(50).map { it.userId })
      hasInitializedSelection = true
    } else if (candidateMembers.size == 1 && candidateMembers.first().userId !in selectedUids) {
      selectedUids.add(candidateMembers.first().userId)
    }
  }

  var message by remember { mutableStateOf("I need urgent assistance. Please check on me.") }
  var consentLocation by remember { mutableStateOf(false) }
  var isFetchingGps by remember { mutableStateOf(false) }
  var lat by remember { mutableDoubleStateOf(0.0) }
  var lon by remember { mutableDoubleStateOf(0.0) }
  var gpsStatus by remember { mutableStateOf("Location sharing is OFF") }

  fun fallbackFromLocationManager(): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    val gps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    val net = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
    val best = gps ?: net ?: return false
    lat = best.latitude
    lon = best.longitude
    gpsStatus = "GPS ready: %.4f, %.4f (private)".format(lat, lon)
    return true
  }

  fun fetchRealLocation() {
    isFetchingGps = true
    gpsStatus = "Fetching GPS fix..."
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
      .addOnSuccessListener { location ->
        isFetchingGps = false
        if (location != null) {
          lat = location.latitude
          lon = location.longitude
          gpsStatus = "GPS ready: %.4f, %.4f (private)".format(lat, lon)
        } else {
          client.lastLocation.addOnSuccessListener { last ->
            if (last != null) {
              lat = last.latitude
              lon = last.longitude
              gpsStatus = "Last GPS ready: %.4f, %.4f (private)".format(lat, lon)
            } else if (!fallbackFromLocationManager()) {
              consentLocation = false
              gpsStatus = "Location unavailable on device; sending without GPS."
            }
          }.addOnFailureListener {
            if (!fallbackFromLocationManager()) {
              consentLocation = false
              gpsStatus = "Location unavailable on device; sending without GPS."
            }
          }
        }
      }
      .addOnFailureListener {
        isFetchingGps = false
        if (!fallbackFromLocationManager()) {
          consentLocation = false
          gpsStatus = "Unable to read GPS; sending alert without coordinates."
        }
      }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    val granted = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
      (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    if (granted) {
      consentLocation = true
      fetchRealLocation()
    } else {
      consentLocation = false
      isFetchingGps = false
      gpsStatus = "Location permission denied; alert can still be sent privately without location."
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Emergency SOS — ${group.name}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
              text = "Strictly Private: Your SOS location is visible ONLY to the selected recipients below — NEVER posted publicly.",
              style = MaterialTheme.typography.labelSmall
            )
          }
        }
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Urgent message") },
          modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Private recipients (${selectedUids.size}/${candidateMembers.size}):", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (friendMembers.isNotEmpty()) {
              TextButton(
                onClick = {
                  selectedUids.clear()
                  selectedUids.addAll(friendMembers.take(50).map { it.userId })
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.testTag("sos_select_friends_only")
              ) {
                Text("Friends")
              }
            }
            if (candidateMembers.isNotEmpty()) {
              TextButton(
                onClick = {
                  if (selectedUids.size == candidateMembers.size) {
                    selectedUids.clear()
                  } else {
                    selectedUids.clear()
                    selectedUids.addAll(candidateMembers.take(50).map { it.userId })
                  }
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(if (selectedUids.size == candidateMembers.size) "Clear" else "Select All")
              }
            }
          }
        }
        if (candidateMembers.isEmpty()) {
          if (isGroupAdmin) {
            Text(
              "Add at least one friend or group member to send a private SOS alert:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
            if (nonMembers.isEmpty()) {
              Text(
                "No other HELLO members discovered yet. Ask a friend to sign in first.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              nonMembers.take(6).forEach { person ->
                val isFriend = person.userId in friendSet
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { onQuickAddMember(person) }
                    .padding(vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(person.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (isFriend) {
                      Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                          "Friend",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                  Text("+ Add to Circle", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            Text(
              "No other members in this group yet. Ask a group admin to add members or create your own Safety Circle.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        } else {
          candidateMembers.take(50).forEach { member ->
            val checked = member.userId in selectedUids
            val isFriend = member.userId in friendSet
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable {
                if (checked) selectedUids.remove(member.userId) else if (selectedUids.size < 50) selectedUids.add(member.userId)
              }
            ) {
              Checkbox(checked = checked, onCheckedChange = {
                if (checked) selectedUids.remove(member.userId) else if (selectedUids.size < 50) selectedUids.add(member.userId)
              })
              Text(member.displayName, modifier = Modifier.weight(1f))
              if (isFriend) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                  Text(
                    "Friend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
          if (selectedUids.isEmpty()) {
            Text(
              "Select at least 1 recipient above to enable Send Private SOS.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Share live GPS location", style = MaterialTheme.typography.titleSmall)
            Text(gpsStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (consentLocation && (lat != 0.0 || lon != 0.0)) {
              TextButton(
                onClick = { openLocationInGoogleMaps(context, lat, lon, "My SOS Location") },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                modifier = Modifier.testTag("preview_sos_google_maps_button")
              ) {
                Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Open in Google Maps")
              }
            }
          }
          Switch(
            checked = consentLocation,
            onCheckedChange = { wantShare ->
              if (!wantShare) {
                consentLocation = false
                isFetchingGps = false
                lat = 0.0
                lon = 0.0
                gpsStatus = "Location sharing is OFF"
              } else {
                val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fineOk || coarseOk) {
                  consentLocation = true
                  fetchRealLocation()
                } else {
                  permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                  )
                }
              }
            },
            modifier = Modifier.testTag("sos_location_consent_switch")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          var finalShare = consentLocation
          if (finalShare && lat == 0.0 && lon == 0.0) {
            if (!fallbackFromLocationManager()) {
              finalShare = false
            }
          }
          onSendSos(
            message.ifBlank { "I need urgent assistance. Please check on me." },
            selectedUids.toList(),
            finalShare,
            if (finalShare) lat else 0.0,
            if (finalShare) lon else 0.0
          )
        },
        enabled = selectedUids.isNotEmpty() && !isFetchingGps,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_send_sos_button")
      ) {
        Text(if (isFetchingGps) "Getting GPS..." else "Send Private SOS")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
fun SosHubScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  globalSettings: GlobalSettings,
  currentUserId: String,
  friendUids: List<String> = emptyList(),
  onOpenGroup: (String) -> Unit,
  onOpenGroups: () -> Unit
) {
  val context = LocalContext.current
  val myGroups by spacesVm.myGroups.collectAsStateWithLifecycle()
  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }

  val sosGroups = remember(myGroups) { myGroups.filter { it.sosEnabled } }
  var selectedSosGroup by remember { mutableStateOf<Group?>(null) }
  var showQuickCreateCircle by remember { mutableStateOf(false) }
  var liveTrackingTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // groupId to alertId
  var panicStatusNote by remember { mutableStateOf<String?>(null) }

  @SuppressLint("MissingPermission")
  fun dispatchOneTapPanicSos(group: Group) {
    val recipients = group.memberUids.filter { it != currentUserId }.take(50)
    if (recipients.isEmpty()) {
      panicStatusNote = "Add at least one trusted contact to ${group.name} first."
      selectedSosGroup = group
      return
    }
    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
    val client = LocationServices.getFusedLocationProviderClient(context)
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    fun sendWithCoords(lat: Double, lon: Double, hasGps: Boolean) {
      val mapLink = if (hasGps) " Live GPS: https://maps.google.com/?q=$lat,$lon" else ""
      spacesVm.sendSosAlert(
        group = group,
        sender = effectiveMe,
        message = "🚨 PANIC SOS: I need immediate help!$mapLink",
        recipientUids = recipients,
        shareLiveLocation = hasGps,
        latitude = lat,
        longitude = lon
      ) { gId, alertId ->
        if (hasGps) liveTrackingTarget = gId to alertId
        panicStatusNote = if (hasGps) {
          "Live GPS tracking active & link shared to ${recipients.size} trusted contact(s)."
        } else {
          "Panic SOS dispatched to ${recipients.size} trusted contact(s)."
        }
      }
    }
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
      .addOnSuccessListener { loc ->
        if (loc != null) {
          sendWithCoords(loc.latitude, loc.longitude, true)
        } else {
          val fallback = runCatching {
            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
              ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
          }.getOrNull()
          if (fallback != null) sendWithCoords(fallback.latitude, fallback.longitude, true)
          else sendWithCoords(0.0, 0.0, false)
        }
      }
      .addOnFailureListener {
        val fallback = runCatching {
          lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        if (fallback != null) sendWithCoords(fallback.latitude, fallback.longitude, true)
        else sendWithCoords(0.0, 0.0, false)
      }
  }

  val panicLocationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    val targetGroup = sosGroups.firstOrNull() ?: return@rememberLauncherForActivityResult
    val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      dispatchOneTapPanicSos(targetGroup)
    } else {
      val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
      val recipients = targetGroup.memberUids.filter { it != currentUserId }.take(50)
      if (recipients.isNotEmpty()) {
        spacesVm.sendSosAlert(
          group = targetGroup,
          sender = effectiveMe,
          message = "🚨 PANIC SOS: Immediate assistance needed!",
          recipientUids = recipients,
          shareLiveLocation = false,
          latitude = 0.0,
          longitude = 0.0
        )
        panicStatusNote = "Location denied — Panic SOS sent without GPS coordinates."
      }
    }
  }

  // Continuous 8-second real-time GPS updates while Live Panic SOS is active
  @SuppressLint("MissingPermission")
  LaunchedEffect(liveTrackingTarget) {
    val active = liveTrackingTarget ?: return@LaunchedEffect
    val client = LocationServices.getFusedLocationProviderClient(context)
    while (liveTrackingTarget == active) {
      val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
      if (fineOk || coarseOk) {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
          .addOnSuccessListener { loc ->
            if (loc != null) {
              spacesVm.updateSosLiveLocation(active.first, active.second, loc.latitude, loc.longitude)
            }
          }
      }
      delay(8000L)
    }
  }

  val panicPulse = rememberInfiniteTransition(label = "panic_sos_pulse")
  val panicScale by panicPulse.animateFloat(
    initialValue = 1f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    label = "panic_btn_scale"
  )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
              Icons.Rounded.HealthAndSafety,
              contentDescription = "Emergency SOS Safety",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text("Emergency SOS Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
              Text(
                "Strictly private alerts sent ONLY to chosen group members — never posted publicly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
          if (sosGroups.isNotEmpty() && globalSettings.sosFeatureEnabled) {
            Button(
              onClick = {
                val primaryCircle = sosGroups.first()
                val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fineOk || coarseOk) {
                  dispatchOneTapPanicSos(primaryCircle)
                } else {
                  panicLocationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier
                .fillMaxWidth()
                .scale(panicScale)
                .testTag("live_panic_sos_button")
            ) {
              Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(8.dp))
              Text("LIVE PANIC SOS · 1-TAP GPS ALERT", fontWeight = FontWeight.ExtraBold)
            }
          }
          panicStatusNote?.let { note ->
            Text(
              text = note,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showQuickCreateCircle = true },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("create_sos_circle_button")
            ) {
              Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("New Safety Circle")
            }
            OutlinedButton(onClick = onOpenGroups) {
              Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("All Groups")
            }
          }
        }
      }
    }

    if (!globalSettings.sosFeatureEnabled) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Shield,
          title = "SOS Currently Paused",
          subtitle = "Emergency SOS alerts are currently disabled by platform governance settings."
        )
      }
      return@LazyColumn
    }

    if (sosGroups.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Rounded.Shield,
          title = "No SOS Circles Yet",
          subtitle = "Create or join a trusted family or neighborhood circle to send private Emergency SOS alerts.",
          actionLabel = "Create Safety Circle",
          onAction = { showQuickCreateCircle = true }
        )
      }
    } else {
      item {
        Text("Your Trusted SOS Circles (${sosGroups.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      items(sosGroups, key = { it.id }) { group ->
        val alerts by spacesVm.observeGroupSosAlerts(group.id).collectAsStateWithLifecycle(initialValue = emptyList())
        val activeAlerts = remember(alerts) { alerts.filter { it.status == "active" } }
        val myActiveAlert = activeAlerts.firstOrNull { it.senderId == currentUserId }

        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().clickable { onOpenGroup(group.id) },
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${group.membersCount} member(s) · ${group.privacy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Button(
                onClick = { selectedSosGroup = group },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("sos_hub_trigger_${group.id}")
              ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("SOS")
              }
            }

            if (myActiveAlert != null) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    if (liveTrackingTarget?.second == myActiveAlert.id) liveTrackingTarget = null
                    spacesVm.stopOrCancelSos(group.id, myActiveAlert.id, "stopped")
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                  Icon(Icons.Rounded.LocationOff, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Stop Sharing")
                }
                OutlinedButton(onClick = {
                  if (liveTrackingTarget?.second == myActiveAlert.id) liveTrackingTarget = null
                  spacesVm.stopOrCancelSos(group.id, myActiveAlert.id, "cancelled")
                }) {
                  Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Cancel")
                }
              }
            }

            activeAlerts.forEach { alert ->
              val hasCoords = alert.shareLiveLocation && (alert.latitude != 0.0 || alert.longitude != 0.0)
              val alertSenderName = resolveLiveAuthor(alert.senderId, alert.senderName, "", usersById, me).displayName
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("$alertSenderName · LIVE PRIVATE SOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                  Text(alert.message, style = MaterialTheme.typography.bodySmall)
                  if (hasCoords) {
                    Text(
                      text = "GPS (private): %.4f, %.4f".format(alert.latitude, alert.longitude),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.clickable {
                        openLocationInGoogleMaps(
                          context = context,
                          latitude = alert.latitude,
                          longitude = alert.longitude,
                          label = "$alertSenderName SOS"
                        )
                      }
                    )
                    OutlinedButton(
                      onClick = {
                        openLocationInGoogleMaps(
                          context = context,
                          latitude = alert.latitude,
                          longitude = alert.longitude,
                          label = "$alertSenderName SOS"
                        )
                      },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.testTag("open_sos_hub_map_${alert.id}")
                    ) {
                      Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Open Private Map")
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

  selectedSosGroup?.let { group ->
    val isAdmin = currentUserId == group.creatorId || currentUserId in group.adminUids
    EmergencySosDialog(
      group = group,
      allUsers = allUsers,
      friendUids = friendUids,
      isGroupAdmin = isAdmin,
      currentUserId = currentUserId,
      onDismiss = { selectedSosGroup = null },
      onQuickAddMember = { person ->
        val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        spacesVm.inviteMember(group, effectiveMe, person.userId)
      },
      onSendSos = { message, recipients, shareLoc, lat, lon ->
        val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        spacesVm.sendSosAlert(group, effectiveMe, message, recipients, shareLoc, lat, lon)
        selectedSosGroup = null
      }
    )
  }

  if (showQuickCreateCircle) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("Trusted emergency safety circle") }
    AlertDialog(
      onDismissRequest = { showQuickCreateCircle = false },
      title = { Text("Create Safety Circle") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle name (e.g. Family Safety)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
      },
      confirmButton = {
        Button(onClick = {
          spacesVm.createGroup(name.ifBlank { "Safety Circle" }, desc, "Safety", "private", true)
          showQuickCreateCircle = false
        }) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showQuickCreateCircle = false }) { Text("Cancel") } }
    )
  }
}
