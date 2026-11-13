package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.VoiceNotePlayer
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun MessengerScreen(
  viewModel: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onOpenThread: (String) -> Unit
) {
  val convState by viewModel.conversations.collectAsStateWithLifecycle()
  val usersState by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  var showNewGroup by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Messenger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Button(onClick = { showNewGroup = true }, modifier = Modifier.testTag("new_group_chat_button")) {
        Icon(Icons.Filled.GroupAdd, null)
        Spacer(Modifier.width(6.dp))
        Text("Group Chat")
      }
    }

    when (val state = convState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.Message, "Chats unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Filled.Message,
            title = "No conversations yet",
            subtitle = "Message a friend from the Friends tab or start a group chat.",
            actionLabel = "Start Group Chat",
            onAction = { showNewGroup = true }
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.data, key = { it.id }) { conv ->
              val displayTitle = if (conv.isGroup) {
                conv.title.ifBlank { "Group Chat" }
              } else {
                val otherUid = conv.participantUids.firstOrNull { it != currentUserId }
                conv.participantNames[otherUid] ?: conv.title.ifBlank { "Direct Chat" }
              }
              val isUnread = currentUserId !in conv.readByUids
              val otherTyping = conv.typingUids.any { it != currentUserId }
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenThread(conv.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  UserAvatar(name = displayTitle, photoUrl = "", size = 46.dp, isOnline = isUnread)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold)
                    Text(
                      text = if (otherTyping) "Typing..." else conv.lastMessage.ifBlank { "Tap to chat" },
                      style = MaterialTheme.typography.bodySmall,
                      color = if (otherTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                  }
                  Text(formatTimestamp(conv.updatedAt), style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showNewGroup) {
    var title by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<PublicUser>() }
    val candidates = ((usersState as? UiState.Success)?.data ?: emptyList()).filter { it.userId != currentUserId }

    AlertDialog(
      onDismissRequest = { showNewGroup = false },
      title = { Text("Create Group Chat") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Group name") }, modifier = Modifier.fillMaxWidth())
          candidates.take(8).forEach { person ->
            val checked = selected.any { it.userId == person.userId }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable {
                if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
              }
            ) {
              Checkbox(checked = checked, onCheckedChange = {
                if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
              })
              Text(person.displayName)
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          if (me != null) {
            viewModel.createGroupChat(title, me, selected.toList()) { convId ->
              showNewGroup = false
              onOpenThread(convId)
            }
          }
        }) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showNewGroup = false }) { Text("Cancel") } }
    )
  }
}

@Composable
fun ChatThreadScreen(
  conversationId: String,
  viewModel: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val convState by viewModel.conversations.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val conversation = (convState as? UiState.Success)?.data?.firstOrNull { it.id == conversationId }
  val messages by viewModel.observeMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
  var input by remember { mutableStateOf("") }

  // Voice recording state
  var isRecording by remember { mutableStateOf(false) }
  var recordingSeconds by remember { mutableIntStateOf(0) }
  var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
  var recordedFile by remember { mutableStateOf<File?>(null) }
  var audioHint by remember { mutableStateOf<String?>(null) }

  fun releaseRecorder(deleteFile: Boolean) {
    runCatching { activeRecorder?.stop() }
    runCatching { activeRecorder?.release() }
    activeRecorder = null
    isRecording = false
    if (deleteFile) {
      recordedFile?.delete()
      recordedFile = null
    }
  }

  fun startVoiceRecording() {
    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      MediaRecorder(context)
    } else {
      @Suppress("DEPRECATION")
      MediaRecorder()
    }
    runCatching {
      recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      recorder.setAudioEncodingBitRate(128000)
      recorder.setAudioSamplingRate(44100)
      recorder.setOutputFile(file.absolutePath)
      recorder.prepare()
      recorder.start()
      recordedFile = file
      activeRecorder = recorder
      recordingSeconds = 0
      audioHint = null
      isRecording = true
    }.onFailure {
      runCatching { recorder.release() }
      file.delete()
      audioHint = "Microphone unavailable on this device."
    }
  }

  fun stopAndSendVoiceNote() {
    val file = recordedFile
    val duration = recordingSeconds.coerceAtLeast(1)
    releaseRecorder(deleteFile = false)
    if (file != null && file.exists() && file.length() > 0L && conversation != null && me != null) {
      val localUri = Uri.fromFile(file).toString()
      viewModel.sendMessage(
        context = context,
        conversation = conversation,
        sender = me,
        text = "",
        mediaUri = localUri,
        mediaType = "audio",
        fileName = "voice_${duration}s.m4a",
        onMediaUploaded = { file.delete() }
      )
      recordedFile = null
    } else {
      file?.delete()
      recordedFile = null
    }
  }

  val recordAudioLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      startVoiceRecording()
    } else {
      audioHint = "Microphone permission is needed to record voice notes."
    }
  }

  LaunchedEffect(isRecording) {
    while (isRecording) {
      delay(1000L)
      recordingSeconds++
      if (recordingSeconds >= 120) {
        stopAndSendVoiceNote()
      }
    }
  }

  LaunchedEffect(conversationId, messages.size) {
    viewModel.markRead(conversationId)
  }

  // Auto-clear typing indicator after 3 seconds of idle input
  LaunchedEffect(conversationId, input) {
    if (input.isNotBlank()) {
      delay(3000L)
      viewModel.setTyping(conversationId, false)
    }
  }

  DisposableEffect(conversationId) {
    onDispose {
      releaseRecorder(deleteFile = true)
      viewModel.setTyping(conversationId, false)
    }
  }

  val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null && conversation != null && me != null) {
      val mime = context.contentResolver.getType(uri).orEmpty()
      val mediaType = if (mime.startsWith("video")) "video" else "image"
      viewModel.sendMessage(context, conversation, me, "", mediaUri = uri.toString(), mediaType = mediaType)
    }
  }
  val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null && conversation != null && me != null) {
      viewModel.sendMessage(context, conversation, me, "", mediaUri = uri.toString(), mediaType = "file", fileName = uri.lastPathSegment ?: "Document")
    }
  }

  val headerTitle = if (conversation == null) {
    "Chat"
  } else if (conversation.isGroup) {
    conversation.title
  } else {
    val otherUid = conversation.participantUids.firstOrNull { it != currentUserId }
    conversation.participantNames[otherUid] ?: conversation.title
  }

  val typingOthers = conversation?.typingUids?.filter { it != currentUserId }.orEmpty()
  val otherReaders = conversation?.readByUids?.filter { it != currentUserId }.orEmpty()
  val lastMyMessageId = remember(messages, currentUserId) {
    messages.lastOrNull { it.senderId == currentUserId && !it.isDeleted }?.id
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          if (typingOthers.isNotEmpty()) {
            val names = typingOthers.mapNotNull { conversation?.participantNames?.get(it) }.ifEmpty { listOf("Someone") }
            Text(
              text = "${names.joinToString(", ")} typing...",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          } else if (otherReaders.isNotEmpty()) {
            val readerLabel = if (conversation?.isGroup == true) {
              val names = otherReaders.mapNotNull { conversation.participantNames[it] }
              if (names.isNotEmpty()) "Seen by ${names.joinToString(", ")}" else "Seen by ${otherReaders.size}"
            } else {
              "Seen"
            }
            Text(readerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        IconButton(onClick = { viewModel.leaveConversation(conversationId, onBack) }) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave chat")
        }
      }
    }

    if (isUploading) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Uploading media to cloud...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }

    audioHint?.let { hint ->
      Text(
        text = hint,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == currentUserId
        val showSeenTick = isMine && msg.id == lastMyMessageId
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
          Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              if (!isMine && conversation?.isGroup == true) {
                Text(msg.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              if (msg.mediaUri.isNotBlank() && !msg.isDeleted) {
                when (msg.mediaType) {
                  "image" -> AsyncImage(
                    model = msg.mediaUri,
                    contentDescription = "Shared photo",
                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                  )
                  "video" -> VideoPlayerBox(videoUri = msg.mediaUri, modifier = Modifier.width(220.dp).height(140.dp))
                  "audio" -> VoiceNotePlayer(audioUrl = msg.mediaUri, fileName = msg.fileName, isMine = isMine)
                  "file" -> Text("📎 ${msg.fileName}", color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
              }
              if (msg.text.isNotBlank()) {
                Text(
                  text = msg.text,
                  color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (msg.reactions.isNotEmpty()) {
                  Text(msg.reactions.values.joinToString(" "), style = MaterialTheme.typography.labelSmall)
                }
                Text(
                  text = "❤️",
                  modifier = Modifier.clickable { viewModel.reactToMessage(conversationId, msg, "❤️") },
                  style = MaterialTheme.typography.labelSmall
                )
                if (isMine && !msg.isDeleted) {
                  Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete message",
                    modifier = Modifier.size(14.dp).clickable { viewModel.deleteMessage(conversationId, msg.id) },
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                  )
                }
                if (showSeenTick) {
                  val seenTint = if (otherReaders.isNotEmpty()) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
                  }
                  Icon(
                    Icons.Filled.DoneAll,
                    contentDescription = if (otherReaders.isNotEmpty()) "Seen" else "Sent",
                    modifier = Modifier.size(15.dp),
                    tint = seenTint
                  )
                }
              }
            }
          }
        }
      }
    }

    if (isRecording) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.errorContainer)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(onClick = { releaseRecorder(deleteFile = true) }, modifier = Modifier.testTag("cancel_voice_note_button")) {
          Icon(Icons.Filled.Close, contentDescription = "Cancel recording", tint = MaterialTheme.colorScheme.onErrorContainer)
        }
        Text(
          text = "Recording voice note... %d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
          fontWeight = FontWeight.SemiBold
        )
        Button(onClick = { stopAndSendVoiceNote() }, modifier = Modifier.testTag("send_voice_note_button")) {
          Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Send")
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
          Icon(Icons.Filled.Image, contentDescription = "Send photo or video")
        }
        IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
          Icon(Icons.Filled.AttachFile, contentDescription = "Send file")
        }
        IconButton(
          onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
              startVoiceRecording()
            } else {
              recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          },
          modifier = Modifier.testTag("record_voice_note_button")
        ) {
          Icon(Icons.Filled.Mic, contentDescription = "Record voice note")
        }
        OutlinedTextField(
          value = input,
          onValueChange = {
            input = it
            viewModel.setTyping(conversationId, it.isNotBlank())
          },
          placeholder = { Text("Message...") },
          modifier = Modifier.weight(1f).testTag("chat_message_input"),
          singleLine = true
        )
        IconButton(
          onClick = {
            if (input.isNotBlank() && conversation != null && me != null) {
              viewModel.sendMessage(context, conversation, me, input)
              input = ""
              viewModel.setTyping(conversationId, false)
            }
          },
          modifier = Modifier.testTag("send_chat_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
  }
}
