package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Conversation
import com.example.data.model.PublicUser

data class SharePayload(
  val title: String,
  val body: String,
  val canRepostToFeed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareOptionsBottomSheet(
  payload: SharePayload,
  currentUserId: String,
  conversations: List<Conversation> = emptyList(),
  discoverableUsers: List<PublicUser> = emptyList(),
  onDismiss: () -> Unit,
  onRepostToFeed: ((note: String) -> Unit)? = null,
  onSendToConversation: ((conversation: Conversation, formattedText: String) -> Unit)? = null,
  onSendToPerson: ((person: PublicUser, formattedText: String) -> Unit)? = null,
  onExternalShareLaunched: (() -> Unit)? = null,
  onCopied: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  @Suppress("DEPRECATION")
  val clipboard = LocalClipboardManager.current
  var feedNote by remember { mutableStateOf("") }
  var showFeedComposer by remember { mutableStateOf(false) }
  var feedbackText by remember { mutableStateOf<String?>(null) }

  val shareMessage = remember(payload) {
    buildString {
      append(payload.title)
      if (payload.body.isNotBlank()) {
        append("\n")
        append(payload.body)
      }
      append("\n\nShared via HELLO")
    }
  }

  val recentFriends = remember(discoverableUsers, currentUserId) {
    discoverableUsers.filter { it.userId != currentUserId }.take(12)
  }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Share",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(payload.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          if (payload.body.isNotBlank()) {
            Text(
              payload.body,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      feedbackText?.let { msg ->
        Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }

      if (payload.canRepostToFeed && onRepostToFeed != null) {
        if (!showFeedComposer) {
          Button(
            onClick = { showFeedComposer = true },
            modifier = Modifier.fillMaxWidth().testTag("share_to_feed_option")
          ) {
            Icon(Icons.Rounded.DynamicFeed, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share to Your HELLO Feed")
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = feedNote,
              onValueChange = { feedNote = it },
              label = { Text("Add a thought (optional)") },
              modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(onClick = { showFeedComposer = false }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
              }
              Button(
                onClick = {
                  onRepostToFeed(feedNote)
                  onDismiss()
                },
                modifier = Modifier.weight(1f).testTag("confirm_share_to_feed_button")
              ) {
                Text("Post Now")
              }
            }
          }
        }
      }

      if ((conversations.isNotEmpty() && onSendToConversation != null) || (recentFriends.isNotEmpty() && onSendToPerson != null)) {
        HorizontalDivider()
        Text("Send in HELLO Messenger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        if (conversations.isNotEmpty() && onSendToConversation != null) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(conversations.take(10), key = { it.id }) { conv ->
              val name = if (conv.isGroup) {
                conv.title.ifBlank { "Group" }
              } else {
                val otherUid = conv.participantUids.firstOrNull { it != currentUserId }
                conv.participantNames[otherUid] ?: conv.title.ifBlank { "Friend" }
              }
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .width(68.dp)
                  .clickable {
                    onSendToConversation(conv, shareMessage)
                    onDismiss()
                  }
              ) {
                UserAvatar(name = name, photoUrl = "", size = 48.dp)
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        } else if (recentFriends.isNotEmpty() && onSendToPerson != null) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentFriends, key = { it.userId }) { person ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .width(68.dp)
                  .clickable {
                    onSendToPerson(person, shareMessage)
                    onDismiss()
                  }
              ) {
                UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 48.dp, isOnline = person.effectiveOnline)
                Spacer(Modifier.height(4.dp))
                Text(person.displayName.substringBefore(" "), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }

      HorizontalDivider()

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, payload.title)
              putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            val chooser = Intent.createChooser(sendIntent, "Share via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            onExternalShareLaunched?.invoke()
            onDismiss()
          },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).testTag("share_external_apps_button")
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Other Apps")
        }

        OutlinedButton(
          onClick = {
            clipboard.setText(AnnotatedString(shareMessage))
            feedbackText = "Copied to clipboard!"
            onCopied?.invoke("Copied to clipboard!")
          },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).testTag("copy_share_text_button")
        ) {
          Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Copy Text")
        }
      }

      Spacer(Modifier.height(8.dp))
    }
  }
}
