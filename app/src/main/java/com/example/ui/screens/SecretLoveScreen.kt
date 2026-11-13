package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun SecretLoveScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String
) {
  val inboxState by spacesVm.receivedSecretLoves.collectAsStateWithLifecycle()
  val sentList by spacesVm.sentSecretLoves.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val recipients = ((usersState as? UiState.Success)?.data ?: emptyList()).filter { it.userId != currentUserId && it.acceptSecretLove }

  var selectedTab by remember { mutableIntStateOf(0) }
  var reportTargetId by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .clip(RoundedCornerShape(22.dp))
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_secret_love_art),
        contentDescription = "Secret Love banner",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
          .padding(16.dp)
      ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
          Text("Secret Love ❤️", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
          Text(
            "Anonymous notes. Zero sender leakage unless you choose to reveal.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
          )
        }
      }
    }

    me?.let { profile ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Allow Secret Love messages", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Text("Privacy control for incoming anonymous notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
          checked = profile.acceptSecretLove,
          onCheckedChange = { enabled ->
            socialVm.updateProfile(
              null,
              profile.displayName,
              profile.bio,
              profile.photoUrl,
              profile.coverUrl,
              profile.profilePrivacy,
              enabled,
              profile.isOnline
            )
          },
          modifier = Modifier.testTag("secret_love_privacy_switch")
        )
      }
    }

    TabRow(selectedTabIndex = selectedTab) {
      Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inbox") })
      Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Send ❤️") })
      Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Sent (${sentList.size})") })
    }

    when (selectedTab) {
      0 -> {
        when (val state = inboxState) {
          is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
          is UiState.Error -> EmptyStateCard(Icons.Filled.Favorite, "Inbox unavailable", state.message)
          is UiState.Success -> {
            val visible = state.data.filter { !it.isBlocked }
            if (visible.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Filled.Favorite,
                title = "No Secret Love messages yet",
                subtitle = "When someone sends you an anonymous compliment or love note, it appears privately here."
              )
            } else {
              LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { msg ->
                  Card(
                    modifier = Modifier.fillMaxWidth().clickable { if (!msg.isRead) spacesVm.markSecretLoveRead(msg.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                  ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(msg.moodEmoji, style = MaterialTheme.typography.titleLarge)
                          Text(
                            text = if (msg.isRevealed) "From ${msg.revealedSenderName}" else "Anonymous Admirer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                          )
                        }
                        Text(formatTimestamp(msg.createdAt), style = MaterialTheme.typography.labelSmall)
                      }
                      Text(msg.message, style = MaterialTheme.typography.bodyLarge)
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { spacesVm.blockOrReportSecretLove(msg.id, block = true, reportReason = null) }) {
                          Icon(Icons.Filled.Block, null, modifier = Modifier.padding(end = 4.dp))
                          Text("Block")
                        }
                        OutlinedButton(onClick = { reportTargetId = msg.id }) {
                          Icon(Icons.Filled.Flag, null, modifier = Modifier.padding(end = 4.dp))
                          Text("Report")
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
      1 -> {
        var selectedRecipient by remember { mutableStateOf<PublicUser?>(null) }
        var note by remember { mutableStateOf("") }
        var mood by remember { mutableStateOf("❤️") }
        var revealNow by remember { mutableStateOf(false) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Text("Choose Recipient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          if (recipients.isEmpty()) {
            item {
              EmptyStateCard(Icons.Filled.Lock, "No recipients available", "Other members who enable Secret Love will show up here.")
            }
          } else {
            items(recipients.take(8), key = { it.userId }) { person ->
              val isPicked = selectedRecipient?.userId == person.userId
              Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedRecipient = person },
                colors = CardDefaults.cardColors(
                  containerColor = if (isPicked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                )
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 38.dp)
                  Text(person.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                  if (isPicked) Text("Selected ✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }
          item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("❤️", "💌", "🌹", "✨", "🥰").forEach { emoji ->
                FilterChip(selected = mood == emoji, onClick = { mood = emoji }, label = { Text(emoji) })
              }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = note,
              onValueChange = { note = it },
              label = { Text("Write your Secret Love message") },
              modifier = Modifier.fillMaxWidth().testTag("secret_love_input"),
              minLines = 3
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text("Reveal my identity immediately", style = MaterialTheme.typography.bodyMedium)
              Switch(checked = revealNow, onCheckedChange = { revealNow = it })
            }
            Button(
              onClick = {
                val target = selectedRecipient
                if (me != null && target != null) {
                  spacesVm.sendSecretLove(me, target, note, mood, revealNow)
                  note = ""
                }
              },
              enabled = selectedRecipient != null && note.isNotBlank(),
              modifier = Modifier.fillMaxWidth().testTag("send_secret_love_button")
            ) {
              Text(if (revealNow) "Send With My Name" else "Send Anonymously ❤️")
            }
          }
        }
      }
      2 -> {
        if (sentList.isEmpty()) {
          EmptyStateCard(Icons.Filled.Favorite, "No sent Secret Love notes", "Your private sent receipts appear here so you can optionally reveal your identity later.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sentList, key = { it.id }) { sent ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("To ${sent.recipientName} ${sent.moodEmoji}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(sent.message, style = MaterialTheme.typography.bodyMedium)
                  if (sent.isRevealed) {
                    Text("Identity revealed ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                  } else {
                    OutlinedButton(onClick = { if (me != null) spacesVm.revealSecretLove(sent, me) }) {
                      Icon(Icons.Filled.Visibility, null)
                      Spacer(Modifier.width(6.dp))
                      Text("Reveal My Identity")
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

  reportTargetId?.let { msgId ->
    var reason by remember { mutableStateOf("Harassment or unwanted message") }
    AlertDialog(
      onDismissRequest = { reportTargetId = null },
      title = { Text("Report Secret Love Message") },
      text = {
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
      },
      confirmButton = {
        Button(onClick = {
          spacesVm.blockOrReportSecretLove(msgId, block = true, reportReason = reason)
          reportTargetId = null
        }) { Text("Block & Report") }
      },
      dismissButton = { TextButton(onClick = { reportTargetId = null }) { Text("Cancel") } }
    )
  }
}
