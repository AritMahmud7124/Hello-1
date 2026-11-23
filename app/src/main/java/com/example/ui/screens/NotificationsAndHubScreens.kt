package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.GlobalSettings
import com.example.data.model.OwnerConfig
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.navigation.HelloRoutes
import com.example.ui.viewmodel.CommunityViewModel

@Composable
fun NotificationsScreen(
  viewModel: CommunityViewModel,
  onNotificationSelected: (AppNotification) -> Unit,
  onOpenSettings: () -> Unit = {}
) {
  val context = LocalContext.current
  val notifState by viewModel.notifications.collectAsStateWithLifecycle()
  var unreadOnly by remember { mutableStateOf(false) }
  var hasSystemPerm by remember {
    mutableStateOf(
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    )
  }
  val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasSystemPerm = granted
  }

  val allNotifs = (notifState as? UiState.Success)?.data.orEmpty()
  val unreadCount = allNotifs.count { !it.isRead }
  val visibleNotifs = remember(allNotifs, unreadOnly) {
    if (unreadOnly) allNotifs.filter { !it.isRead } else allNotifs
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
          text = if (unreadCount > 0) "$unreadCount unread alert(s)" else "All caught up across HELLO",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (unreadCount > 0) {
          TextButton(
            onClick = { viewModel.markAllNotificationsRead(allNotifs) },
            modifier = Modifier.testTag("mark_all_notifications_read")
          ) {
            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Mark read")
          }
        }
        if (allNotifs.isNotEmpty()) {
          IconButton(
            onClick = { viewModel.clearAllNotifications(allNotifs) },
            modifier = Modifier.testTag("clear_all_notifications_button")
          ) {
            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all notifications")
          }
        }
        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier.testTag("notification_settings_button")
        ) {
          Icon(Icons.Rounded.Settings, contentDescription = "Notification preferences")
        }
      }
    }

    if (!hasSystemPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Column(modifier = Modifier.weight(1f)) {
            Text("Enable alert notifications", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Turn on system notifications for messages, friends, and SOS.", style = MaterialTheme.typography.bodySmall)
          }
          Button(onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
            Text("Allow")
          }
        }
      }
    }

    if (allNotifs.isNotEmpty()) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !unreadOnly,
          onClick = { unreadOnly = false },
          label = { Text("All (${allNotifs.size})") }
        )
        FilterChip(
          selected = unreadOnly,
          onClick = { unreadOnly = true },
          label = { Text("Unread ($unreadCount)") }
        )
      }
    }

    when (val state = notifState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Rounded.Notifications, "Notifications unavailable", state.message)
      is UiState.Success -> {
        if (visibleNotifs.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Rounded.Notifications,
            title = if (unreadOnly) "No unread notifications" else "All caught up",
            subtitle = "Friend requests, reactions, Secret Love notes, and SOS alerts will appear here."
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visibleNotifs, key = { it.id }) { notif ->
              val (icon, tint) = when (notif.type) {
                "friend_request" -> Icons.Rounded.PersonAdd to MaterialTheme.colorScheme.secondary
                "like" -> Icons.Rounded.ThumbUp to MaterialTheme.colorScheme.primary
                "comment", "reply" -> Icons.AutoMirrored.Rounded.Comment to MaterialTheme.colorScheme.secondary
                "message" -> Icons.Rounded.Notifications to MaterialTheme.colorScheme.primary
                "group_invite", "page_invite" -> Icons.Rounded.GroupAdd to MaterialTheme.colorScheme.secondary
                "secret_love", "crush_match" -> Icons.Rounded.Favorite to MaterialTheme.colorScheme.tertiary
                "mutual_unlock_24h" -> Icons.Rounded.NotificationsActive to MaterialTheme.colorScheme.tertiary
                "sos_alert" -> Icons.Rounded.Warning to MaterialTheme.colorScheme.error
                else -> Icons.Rounded.Notifications to MaterialTheme.colorScheme.primary
              }
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (!notif.isRead) viewModel.markNotificationRead(notif.id)
                    onNotificationSelected(notif)
                  }
                  .testTag("notification_item_${notif.id}"),
                colors = CardDefaults.cardColors(
                  containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                )
              ) {
                Row(
                  modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                  ) {
                    if (notif.type == "message") {
                      Icon(
                        painter = painterResource(R.drawable.ic_messenger_smart),
                        contentDescription = notif.type,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                      )
                    } else {
                      Icon(icon, contentDescription = notif.type, tint = tint)
                    }
                  }
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (!notif.isRead) {
                        Box(
                          modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                        )
                      }
                      Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notif.isRead) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    Text(
                      text = notif.body,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = formatTimestamp(notif.createdAt),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  }
                  IconButton(
                    onClick = { viewModel.deleteNotification(notif.id) },
                    modifier = Modifier.testTag("delete_notification_${notif.id}")
                  ) {
                    Icon(
                      Icons.Rounded.Close,
                      contentDescription = "Remove notification",
                      modifier = Modifier.size(18.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
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

private data class HubTile(
  val title: String,
  val subtitle: String,
  val route: String,
  val icon: ImageVector,
  val tag: String
)

@Composable
fun HubMenuScreen(
  me: PublicUser?,
  ownerConfig: OwnerConfig?,
  isVerifiedOwner: Boolean,
  globalSettings: GlobalSettings,
  onNavigate: (String) -> Unit
) {
  val tiles = buildList {
    add(HubTile("Home Feed", "Posts & reactions", HelloRoutes.HOME, Icons.Rounded.Home, "hub_home"))
    add(HubTile("Friends", "Requests & people", HelloRoutes.FRIENDS, Icons.Rounded.PersonAdd, "hub_friends"))
    add(HubTile("Reels", "Short videos", HelloRoutes.REELS, Icons.Rounded.PlayCircle, "hub_reels"))
    add(HubTile("Stories", "24h highlights", HelloRoutes.STORIES, Icons.Rounded.Collections, "hub_stories"))
    add(HubTile("Messenger", "Direct & group chat", HelloRoutes.MESSENGER, Icons.Rounded.Notifications, "hub_messenger"))
    add(HubTile("Notifications", "All alerts", HelloRoutes.NOTIFICATIONS, Icons.Rounded.Notifications, "hub_notifications"))
    add(HubTile("Groups", "Circles & communities", HelloRoutes.GROUPS, Icons.Rounded.Groups, "hub_groups"))
    if (globalSettings.sosFeatureEnabled) {
      add(HubTile("Emergency SOS", "Live safety circles", HelloRoutes.SOS, Icons.Rounded.HealthAndSafety, "hub_sos"))
    }
    add(HubTile("Pages", "Creators & brands", HelloRoutes.PAGES, Icons.Rounded.Flag, "hub_pages"))
    add(HubTile("Events", "Meetups & RSVPs", HelloRoutes.EVENTS, Icons.Rounded.Event, "hub_events"))
    add(HubTile("Marketplace", "Buy & sell locally", HelloRoutes.MARKETPLACE, Icons.Rounded.Storefront, "hub_marketplace"))
    if (globalSettings.secretLoveEnabled) {
      add(HubTile("Secret Love ❤️", "Anonymous notes", HelloRoutes.SECRET_LOVE, Icons.Rounded.Favorite, "hub_secret_love"))
    }
    add(HubTile("Profile", "Posts & saved", HelloRoutes.PROFILE, Icons.Rounded.Person, "hub_profile"))
    add(HubTile("Search", "Find everything", HelloRoutes.SEARCH, Icons.Rounded.Search, "hub_search"))
    add(HubTile("Settings", "Theme & privacy", HelloRoutes.SETTINGS, Icons.Rounded.Settings, "hub_settings"))
    add(HubTile("Owner Profile", ownerConfig?.ownerName ?: "HELLO Founder", HelloRoutes.OWNER_PROFILE, Icons.Rounded.Verified, "hub_owner_profile"))
    if (isVerifiedOwner) {
      add(HubTile("Owner Dashboard", "Governance & stats", HelloRoutes.OWNER_DASHBOARD, Icons.Rounded.AdminPanelSettings, "hub_owner_dashboard"))
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigate(HelloRoutes.PROFILE) },
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        UserAvatar(
          name = me?.displayName ?: "Member",
          photoUrl = me?.photoUrl ?: "",
          size = 52.dp,
          isOnline = me?.effectiveOnline == true,
          isVerifiedOwner = isVerifiedOwner
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(me?.displayName ?: "Hello Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val hubSub = if (isVerifiedOwner && ownerConfig?.showOwnerHandle == false) {
            "HELLO Account"
          } else {
            me?.handle?.ifBlank { "HELLO Account" } ?: "HELLO Account"
          }
          Text(hubSub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "View Profile",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    Text(
      text = "Explore ${globalSettings.appName}",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    LazyVerticalGrid(
      columns = GridCells.Adaptive(155.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = 16.dp)
    ) {
      items(tiles, key = { it.route }) { tile ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(tile.route) }
            .testTag(tile.tag),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (tile.route == HelloRoutes.MESSENGER) {
              Icon(
                painter = painterResource(R.drawable.ic_messenger_smart),
                contentDescription = tile.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
              )
            } else {
              Icon(tile.icon, contentDescription = tile.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}
