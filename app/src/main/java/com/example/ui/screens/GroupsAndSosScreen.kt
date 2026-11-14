package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun GroupsScreen(
  viewModel: SpacesGovernanceViewModel,
  currentUserId: String,
  onOpenGroup: (String) -> Unit
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
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Create")
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("My Groups (${myGroups.size})") })
      FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Discover (${publicGroups.size})") })
    }

    if (shown.isEmpty()) {
      EmptyStateCard(
        icon = Icons.Filled.Groups,
        title = if (tab == 0) "You haven't joined a group yet" else "No public groups yet",
        subtitle = "Create a family, neighborhood, or hobby group with optional Emergency SOS alerts.",
        actionLabel = "Create Group",
        onAction = { showCreateDialog = true }
      )
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(shown, key = { it.id }) { group ->
          val isMember = currentUserId in group.memberUids
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenGroup(group.id) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text("${group.category} · ${group.privacy} · ${group.membersCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.toggleMembership(group) }) {
                  Text(if (isMember) "Joined" else "Join")
                }
              }
              if (group.description.isNotBlank()) {
                Text(group.description, style = MaterialTheme.typography.bodyMedium)
              }
              if (group.sosEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Filled.Shield, contentDescription = "SOS enabled", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
  onBack: () -> Unit
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
  val feedState by socialVm.publicFeed.collectAsStateWithLifecycle()
  val myPosts by socialVm.myPosts.collectAsStateWithLifecycle()
  val savedPosts by socialVm.savedPosts.collectAsStateWithLifecycle()
  val savedIds = remember(savedPosts) { savedPosts.map { it.postId }.toSet() }
  val sosAlerts by spacesVm.observeGroupSosAlerts(groupId).collectAsStateWithLifecycle(initialValue = emptyList())

  var showPostDialog by remember { mutableStateOf(false) }
  var showInviteDialog by remember { mutableStateOf(false) }
  var showSosDialog by remember { mutableStateOf(false) }
  var activeCommentPost by remember { mutableStateOf<Post?>(null) }

  val groupPosts = remember(feedState, myPosts, groupId) {
    val pub = (feedState as? UiState.Success)?.data.orEmpty()
    (pub + myPosts).distinctBy { it.id }.filter { it.groupId == groupId }.sortedByDescending { it.createdAt?.seconds ?: 0L }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to groups")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(group?.name ?: "Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("${group?.membersCount ?: 0} members · ${group?.privacy ?: "public"}", style = MaterialTheme.typography.labelSmall)
        }
        if (group != null && currentUserId in group.memberUids) {
          IconButton(onClick = { showInviteDialog = true }) {
            Icon(Icons.Filled.GroupAdd, contentDescription = "Invite member")
          }
        }
      }
    }

    if (group == null) {
      EmptyStateCard(Icons.Filled.Groups, "Group not found", "Return to Groups to pick another circle.", actionLabel = "Go Back", onAction = onBack, modifier = Modifier.padding(16.dp))
      return
    }

    val isMember = currentUserId in group.memberUids
    val activeMyLiveAlert = sosAlerts.firstOrNull { it.senderId == currentUserId && it.status == "active" }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (activeMyLiveAlert != null) {
        item {
          Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.GpsFixed, contentDescription = "Active SOS", tint = MaterialTheme.colorScheme.onErrorContainer)
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
                  Icon(Icons.Filled.LocationOff, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Stop Sharing")
                }
                OutlinedButton(
                  onClick = { spacesVm.stopOrCancelSos(groupId, activeMyLiveAlert.id, "cancelled") },
                  modifier = Modifier.testTag("cancel_sos_button")
                ) {
                  Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp))
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
                Icon(Icons.Filled.Warning, contentDescription = "SOS")
                Spacer(Modifier.width(4.dp))
                Text("SOS")
              }
            }
          }
        }
      }

      if (sosAlerts.isNotEmpty()) {
        item {
          Text("Private SOS History (${sosAlerts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(sosAlerts, key = { it.id }) { alert ->
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("${alert.senderName} · ${alert.status.uppercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
              Text(alert.message, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = if (alert.shareLiveLocation && alert.status == "active") {
                  "Live Location Shared privately: %.4f, %.4f".format(alert.latitude, alert.longitude)
                } else {
                  "Location sharing: OFF"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(formatTimestamp(alert.createdAt), style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }

      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Group Discussions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          if (isMember) {
            Button(onClick = { showPostDialog = true }) { Text("Post in Group") }
          } else {
            Button(onClick = { spacesVm.toggleMembership(group) }) { Text("Join to Post") }
          }
        }
      }

      if (groupPosts.isEmpty()) {
        item {
          EmptyStateCard(Icons.Filled.Groups, "No group posts yet", "Start a discussion with ${group.name} members.")
        }
      } else {
        items(groupPosts, key = { it.id }) { post ->
          val reactions by socialVm.observePostReactions(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
          PostCard(
            post = post,
            currentUserId = currentUserId,
            reactions = reactions,
            isSaved = post.id in savedIds,
            onReact = { emoji -> socialVm.reactToPost(post, emoji) },
            onCommentClick = { activeCommentPost = post },
            onShare = { note -> socialVm.sharePost(post, note) },
            onToggleSave = { socialVm.toggleSave(post.id, "post", post.authorName, post.content) },
            onEdit = { c, p -> socialVm.editPost(post.id, c, p) },
            onDelete = { socialVm.deletePost(post.id) },
            onReport = { r -> socialVm.reportContent("post", post.id, r) }
          )
        }
      }
    }

    if (showPostDialog) {
      CreatePostDialog(
        onDismiss = { showPostDialog = false },
        onPublish = { content, mediaUri, mediaType, privacy ->
          socialVm.createPost(context, content, mediaUri, mediaType, privacy, friendUids, groupId = groupId)
          showPostDialog = false
        }
      )
    }

    if (showInviteDialog) {
      val nonMembers = allUsers.filter { it.userId != currentUserId && it.userId !in group.memberUids }
      AlertDialog(
        onDismissRequest = { showInviteDialog = false },
        title = { Text("Add Group Member") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (nonMembers.isEmpty()) {
              Text("All visible users are already members.")
            } else {
              nonMembers.take(8).forEach { person ->
                Row(
                  modifier = Modifier.fillMaxWidth().clickable {
                    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                    spacesVm.inviteMember(group, effectiveMe, person.userId)
                    showInviteDialog = false
                  }.padding(vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(person.displayName)
                  Text("Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Close") } }
      )
    }

    if (showSosDialog) {
      EmergencySosDialog(
        group = group,
        allUsers = allUsers,
        currentUserId = currentUserId,
        onDismiss = { showSosDialog = false },
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
        onDismiss = { activeCommentPost = null },
        onSendComment = { text, parentId -> socialVm.addComment(post, text, parentId) }
      )
    }
  }
}

@SuppressLint("MissingPermission")
@Composable
private fun EmergencySosDialog(
  group: Group,
  allUsers: List<PublicUser>,
  currentUserId: String,
  onDismiss: () -> Unit,
  onSendSos: (message: String, recipients: List<String>, shareLocation: Boolean, lat: Double, lon: Double) -> Unit
) {
  val context = LocalContext.current
  val candidateMembers = remember(group.memberUids, allUsers, currentUserId) {
    group.memberUids.filter { it != currentUserId }.map { uid ->
      allUsers.firstOrNull { it.userId == uid } ?: PublicUser(userId = uid, displayName = "Member ${uid.take(6)}")
    }
  }
  val selectedUids = remember { mutableStateListOf<String>() }
  var message by remember { mutableStateOf("I need urgent assistance. Please check on me.") }
  var consentLocation by remember { mutableStateOf(false) }
  var lat by remember { mutableDoubleStateOf(0.0) }
  var lon by remember { mutableDoubleStateOf(0.0) }
  var gpsStatus by remember { mutableStateOf("Location sharing is OFF") }

  fun fetchRealLocation() {
    gpsStatus = "Fetching GPS fix..."
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
      .addOnSuccessListener { location ->
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
            } else {
              consentLocation = false
              gpsStatus = "Location unavailable on device; sending without GPS."
            }
          }
        }
      }
      .addOnFailureListener {
        consentLocation = false
        gpsStatus = "Unable to read GPS; sending alert without coordinates."
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
      gpsStatus = "Location permission denied; alert can still be sent privately without location."
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Emergency SOS — ${group.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Urgent message") },
          modifier = Modifier.fillMaxWidth()
        )
        Text("Select recipients (never public):", style = MaterialTheme.typography.labelLarge)
        if (candidateMembers.isEmpty()) {
          Text("Add another member to this group first before sending an SOS alert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else {
          candidateMembers.take(6).forEach { member ->
            val checked = member.userId in selectedUids
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable {
                if (checked) selectedUids.remove(member.userId) else selectedUids.add(member.userId)
              }
            ) {
              Checkbox(checked = checked, onCheckedChange = {
                if (checked) selectedUids.remove(member.userId) else selectedUids.add(member.userId)
              })
              Text(member.displayName)
            }
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
          }
          Switch(
            checked = consentLocation,
            onCheckedChange = { wantShare ->
              if (!wantShare) {
                consentLocation = false
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
        onClick = { onSendSos(message, selectedUids.toList(), consentLocation, lat, lon) },
        enabled = selectedUids.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_send_sos_button")
      ) { Text("Send Private SOS") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
