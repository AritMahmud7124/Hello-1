package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.resolveLiveAuthor
import com.example.ui.viewmodel.CommunityViewModel

@Composable
fun FriendsScreen(
  viewModel: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onOpenChat: (String) -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  val usersState by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  val requests by viewModel.friendRequests.collectAsStateWithLifecycle()
  val friendships by viewModel.friendships.collectAsStateWithLifecycle()
  val blocked by viewModel.blockedUsers.collectAsStateWithLifecycle()
  val usersById = remember(usersState, me) {
    val map = (usersState as? UiState.Success)?.data.orEmpty().associateBy { it.userId }.toMutableMap()
    if (me != null) map[me.userId] = me
    map
  }

  var query by remember { mutableStateOf("") }
  val blockedUids = remember(blocked) { blocked.map { it.blockedUid }.toSet() }
  val friendUids = remember(friendships, currentUserId) {
    friendships.flatMap { it.participantUids }.filter { it != currentUserId }.toSet()
  }

  val incomingRequests = remember(requests, currentUserId) {
    requests.filter { it.toUid == currentUserId && it.status == "pending" }
  }
  val incomingByUid = remember(incomingRequests) {
    incomingRequests.associateBy { it.fromUid }
  }
  val outgoingByUid = remember(requests, currentUserId) {
    requests.filter { it.fromUid == currentUserId && it.status == "pending" }.associateBy { it.toUid }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text("Friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        placeholder = { Text("Search people by name or @handle") },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("friends_search_input"),
        singleLine = true
      )
    }

    if (incomingRequests.isNotEmpty()) {
      item {
        Text("Friend Requests (${incomingRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      items(incomingRequests, key = { it.id }) { req ->
        val resolvedSender = resolveLiveAuthor(req.fromUid, req.fromName, req.fromAvatar, usersById, me)
        Card(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .clickable { onOpenProfile(req.fromUid) }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            UserAvatar(name = resolvedSender.displayName, photoUrl = resolvedSender.avatarUrl, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(resolvedSender.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
              Text("Wants to connect · Tap to view profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
              onClick = { viewModel.acceptFriendRequest(req) },
              contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
              Icon(Icons.Rounded.Check, null)
              Spacer(Modifier.width(4.dp))
              Text("Accept")
            }
            IconButton(onClick = { viewModel.rejectOrCancelRequest(req.id) }) {
              Icon(Icons.Rounded.Close, contentDescription = "Reject")
            }
          }
        }
      }
    }

    when (val state = usersState) {
      is UiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
      is UiState.Error -> item { EmptyStateCard(Icons.Rounded.People, "Could not load directory", state.message) }
      is UiState.Success -> {
        val people = state.data.filter {
          it.userId != currentUserId &&
            it.userId !in blockedUids &&
            (query.isBlank() || it.displayName.contains(query, true) || it.handle.contains(query, true))
        }
        val friendsList = people.filter { it.userId in friendUids }
        val suggestions = people.filter { it.userId !in friendUids }

        if (friendsList.isNotEmpty()) {
          item {
            Text("Your Friends (${friendsList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          items(friendsList, key = { "f_${it.userId}" }) { user ->
            FriendPersonCard(
              user = user,
              onClick = { onOpenProfile(user.userId) },
              statusContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  IconButton(onClick = {
                    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                    viewModel.startDirectChat(effectiveMe, user, onOpenChat)
                  }) {
                    Icon(
                      painter = painterResource(R.drawable.ic_messenger_smart),
                      contentDescription = "Message ${user.displayName}",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(21.dp)
                    )
                  }
                  OutlinedButton(onClick = { viewModel.unfriend(user.userId) }) { Text("Unfriend") }
                  IconButton(onClick = { viewModel.blockUser(user.userId, user.displayName) }) {
                    Icon(Icons.Rounded.Block, contentDescription = "Block")
                  }
                }
              }
            )
          }
        }

        item {
          Text("Discover People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (suggestions.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.Rounded.People,
              title = "No matching people",
              subtitle = "Invite friends to sign into HELLO with Google to connect."
            )
          }
        } else {
          items(suggestions, key = { "s_${it.userId}" }) { user ->
            val pendingOut = outgoingByUid[user.userId]
            val pendingIn = incomingByUid[user.userId]
            FriendPersonCard(
              user = user,
              onClick = { onOpenProfile(user.userId) },
              statusContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  when {
                    pendingIn != null -> {
                      Button(onClick = { viewModel.acceptFriendRequest(pendingIn) }) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                      }
                    }
                    pendingOut != null -> {
                      OutlinedButton(onClick = { viewModel.rejectOrCancelRequest(pendingOut.id) }) {
                        Text("Cancel")
                      }
                    }
                    else -> {
                      Button(onClick = {
                        val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                        viewModel.sendFriendRequest(effectiveMe, user)
                      }) {
                        Icon(Icons.Rounded.PersonAdd, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                      }
                    }
                  }
                  IconButton(onClick = { viewModel.blockUser(user.userId, user.displayName) }) {
                    Icon(Icons.Rounded.Block, contentDescription = "Block")
                  }
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FriendPersonCard(
  user: PublicUser,
  onClick: () -> Unit,
  statusContent: @Composable () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("friend_card_${user.userId}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      UserAvatar(
        name = user.displayName,
        photoUrl = user.photoUrl,
        size = 46.dp,
        isOnline = user.effectiveOnline,
        isVerifiedOwner = user.isVerifiedOwner
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
          text = if (user.handle.isNotBlank()) {
            "${user.handle} · ${if (user.effectiveOnline) "Online" else "Offline"}"
          } else {
            if (user.effectiveOnline) "Online" else "Offline"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      statusContent()
    }
  }
}
