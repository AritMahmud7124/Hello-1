package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
  onNotificationSelected: (AppNotification) -> Unit
) {
  val notifState by viewModel.notifications.collectAsStateWithLifecycle()

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
      Column {
        Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Activity across your HELLO circle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      val list = (notifState as? UiState.Success)?.data.orEmpty()
      if (list.any { !it.isRead }) {
        TextButton(
          onClick = { viewModel.markAllNotificationsRead(list) },
          modifier = Modifier.testTag("mark_all_notifications_read")
        ) {
          Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Mark all read")
        }
      }
    }

    when (val state = notifState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.Notifications, "Notifications unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Filled.Notifications,
            title = "All caught up",
            subtitle = "Friend requests, reactions, Secret Love notes, and SOS alerts will appear here."
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.data, key = { it.id }) { notif ->
              val (icon, tint) = when (notif.type) {
                "friend_request" -> Icons.Filled.PersonAdd to MaterialTheme.colorScheme.secondary
                "like" -> Icons.Filled.ThumbUp to MaterialTheme.colorScheme.primary
                "comment", "reply" -> Icons.AutoMirrored.Filled.Comment to MaterialTheme.colorScheme.secondary
                "message" -> Icons.AutoMirrored.Filled.Message to MaterialTheme.colorScheme.primary
                "group_invite" -> Icons.Filled.GroupAdd to MaterialTheme.colorScheme.secondary
                "secret_love" -> Icons.Filled.Favorite to MaterialTheme.colorScheme.tertiary
                "sos_alert" -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
                else -> Icons.Filled.Notifications to MaterialTheme.colorScheme.primary
              }
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (!notif.isRead) viewModel.markNotificationRead(notif.id)
                    onNotificationSelected(notif)
                  },
                colors = CardDefaults.cardColors(
                  containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                )
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
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
                    Icon(icon, contentDescription = notif.type, tint = tint)
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = notif.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = if (notif.isRead) FontWeight.Medium else FontWeight.Bold
                    )
                    Text(
                      text = notif.body,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Text(formatTimestamp(notif.createdAt), style = MaterialTheme.typography.labelSmall)
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
    add(HubTile("Home Feed", "Posts & reactions", HelloRoutes.HOME, Icons.Filled.Home, "hub_home"))
    add(HubTile("Friends", "Requests & people", HelloRoutes.FRIENDS, Icons.Filled.PersonAdd, "hub_friends"))
    add(HubTile("Reels", "Short videos", HelloRoutes.REELS, Icons.Filled.PlayCircle, "hub_reels"))
    add(HubTile("Stories", "24h highlights", HelloRoutes.STORIES, Icons.Filled.AutoAwesome, "hub_stories"))
    add(HubTile("Messenger", "Direct & group chat", HelloRoutes.MESSENGER, Icons.AutoMirrored.Filled.Message, "hub_messenger"))
    add(HubTile("Notifications", "All alerts", HelloRoutes.NOTIFICATIONS, Icons.Filled.Notifications, "hub_notifications"))
    add(HubTile("Groups & SOS", "Circles & safety", HelloRoutes.GROUPS, Icons.Filled.Groups, "hub_groups"))
    add(HubTile("Pages", "Creators & brands", HelloRoutes.PAGES, Icons.Filled.Flag, "hub_pages"))
    add(HubTile("Events", "Meetups & RSVPs", HelloRoutes.EVENTS, Icons.Filled.Event, "hub_events"))
    add(HubTile("Marketplace", "Buy & sell locally", HelloRoutes.MARKETPLACE, Icons.Filled.Storefront, "hub_marketplace"))
    if (globalSettings.secretLoveEnabled) {
      add(HubTile("Secret Love ❤️", "Anonymous notes", HelloRoutes.SECRET_LOVE, Icons.Filled.Favorite, "hub_secret_love"))
    }
    add(HubTile("Profile", "Posts & saved", HelloRoutes.PROFILE, Icons.Filled.Person, "hub_profile"))
    add(HubTile("Search", "Find everything", HelloRoutes.SEARCH, Icons.Filled.Search, "hub_search"))
    add(HubTile("Settings", "Theme & privacy", HelloRoutes.SETTINGS, Icons.Filled.Settings, "hub_settings"))
    add(HubTile("Owner Profile", ownerConfig?.ownerName ?: "HELLO Founder", HelloRoutes.OWNER_PROFILE, Icons.Filled.Verified, "hub_owner_profile"))
    if (isVerifiedOwner) {
      add(HubTile("Owner Dashboard", "Governance & stats", HelloRoutes.OWNER_DASHBOARD, Icons.Filled.AdminPanelSettings, "hub_owner_dashboard"))
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
          isOnline = true,
          isVerifiedOwner = isVerifiedOwner
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(me?.displayName ?: "Hello Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(me?.handle ?: "@hello", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Icon(tile.icon, contentDescription = tile.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(2.dp))
            Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}
