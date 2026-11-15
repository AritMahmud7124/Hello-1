package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.media.FirestoreMediaStore
import com.example.data.model.Conversation
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.VoiceNotePlayer
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MessengerBlue = Color(0xFF0084FF)
private val MessengerBlueDark = Color(0xFF006BFF)
private val MessengerViolet = Color(0xFF8B5CF6)

@Composable
fun MessengerScreen(
  viewModel: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onOpenThread: (String) -> Unit
) {
  val convState by viewModel.conversations.collectAsStateWithLifecycle()
  val usersState by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = remember(usersState) {
    ((usersState as? UiState.Success)?.data ?: emptyList())
  }
  val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }
  val activePeople = remember(allUsers, currentUserId) {
    allUsers
      .filter { it.userId != currentUserId }
      .sortedByDescending { it.isOnline }
  }

  var searchQuery by remember { mutableStateOf("") }
  var showNewGroup by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Messenger Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MessengerBlue, MessengerViolet))),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Filled.Message, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Text("Chats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      }
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .clickable { showNewGroup = true }
          .testTag("new_group_chat_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Filled.GroupAdd, contentDescription = "New group chat", modifier = Modifier.size(21.dp))
        }
      }
    }

    // Pill Search Bar (FB Messenger style)
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search Messenger") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search chats") },
      shape = RoundedCornerShape(28.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("messenger_search_input")
    )

    Spacer(Modifier.height(10.dp))

    // Horizontal Active Now / Quick People Strip (FB Messenger signature)
    val filteredPeople = remember(activePeople, searchQuery) {
      if (searchQuery.isBlank()) activePeople else activePeople.filter { it.displayName.contains(searchQuery, true) }
    }
    if (filteredPeople.isNotEmpty()) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(filteredPeople, key = { it.userId }) { person ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .width(64.dp)
              .clickable {
                val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                viewModel.startDirectChat(effectiveMe, person, onOpenThread)
              }
          ) {
            UserAvatar(
              name = person.displayName,
              photoUrl = person.photoUrl,
              size = 54.dp,
              isOnline = person.isOnline
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = person.displayName.substringBefore(" ").ifBlank { "Friend" },
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
      Spacer(Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    when (val state = convState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> Box(Modifier.padding(16.dp)) {
        EmptyStateCard(Icons.Filled.Message, "Chats unavailable", state.message)
      }
      is UiState.Success -> {
        val filteredChats = remember(state.data, searchQuery, currentUserId) {
          if (searchQuery.isBlank()) {
            state.data
          } else {
            state.data.filter { conv ->
              val otherUid = conv.participantUids.firstOrNull { it != currentUserId }
              val title = if (conv.isGroup) conv.title else (conv.participantNames[otherUid] ?: conv.title)
              title.contains(searchQuery, true) || conv.lastMessage.contains(searchQuery, true)
            }
          }
        }

        if (filteredChats.isEmpty()) {
          Box(Modifier.padding(16.dp)) {
            EmptyStateCard(
              icon = Icons.Filled.Message,
              title = if (searchQuery.isBlank()) "No conversations yet" else "No matching chats",
              subtitle = "Tap any friend above to start chatting on HELLO Messenger or start a group chat.",
              actionLabel = "Start Group Chat",
              onAction = { showNewGroup = true }
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            items(filteredChats, key = { it.id }) { conv ->
              val otherUid = if (conv.isGroup) null else conv.participantUids.firstOrNull { it != currentUserId }
              val otherUser = otherUid?.let { usersById[it] }
              val displayTitle = if (conv.isGroup) {
                conv.title.ifBlank { "Group Chat" }
              } else {
                conv.participantNames[otherUid] ?: otherUser?.displayName ?: conv.title.ifBlank { "Direct Chat" }
              }
              val isUnread = currentUserId !in conv.readByUids
              val otherTyping = conv.typingUids.any { it != currentUserId }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenThread(conv.id) }
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                UserAvatar(
                  name = displayTitle,
                  photoUrl = otherUser?.photoUrl ?: "",
                  size = 54.dp,
                  isOnline = otherUser?.isOnline == true
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = when {
                        otherTyping -> "Typing..."
                        conv.lastMessage.isNotBlank() -> {
                          val prefix = if (conv.lastSenderId == currentUserId) "You: " else ""
                          "$prefix${conv.lastMessage}"
                        }
                        else -> "Say hi on Messenger"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (isUnread || otherTyping) FontWeight.SemiBold else FontWeight.Normal,
                      color = when {
                        otherTyping -> MessengerBlue
                        isUnread -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                      text = "· ${formatTimestamp(conv.updatedAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                if (isUnread) {
                  Box(
                    modifier = Modifier
                      .size(11.dp)
                      .clip(CircleShape)
                      .background(MessengerBlue)
                  )
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
    val candidates = activePeople

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
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
                }
            ) {
              Checkbox(checked = checked, onCheckedChange = {
                if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
              })
              UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 30.dp, isOnline = person.isOnline)
              Spacer(Modifier.width(8.dp))
              Text(person.displayName)
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
          viewModel.createGroupChat(title, effectiveMe, selected.toList()) { convId ->
            showNewGroup = false
            onOpenThread(convId)
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
  val scope = rememberCoroutineScope()
  val convState by viewModel.conversations.collectAsStateWithLifecycle()
  val usersState by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val conversation = (convState as? UiState.Success)?.data?.firstOrNull { it.id == conversationId }
  val messages by viewModel.observeMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
  var input by remember { mutableStateOf("") }
  var pendingCropImageUri by remember { mutableStateOf<String?>(null) }
  var reactionTargetMsgId by remember { mutableStateOf<String?>(null) }

  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

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
    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
    if (file != null && file.exists() && file.length() > 0L && conversation != null) {
      val localUri = Uri.fromFile(file).toString()
      viewModel.sendMessage(
        context = context,
        conversation = conversation,
        sender = effectiveMe,
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
    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
    if (uri != null && conversation != null) {
      val mime = context.contentResolver.getType(uri).orEmpty()
      if (mime.startsWith("video")) {
        viewModel.sendMessage(context, conversation, effectiveMe, "", mediaUri = uri.toString(), mediaType = "video")
      } else {
        // Open Crop dialog before sending photo
        pendingCropImageUri = uri.toString()
      }
    }
  }
  val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
    if (uri != null && conversation != null) {
      viewModel.sendMessage(context, conversation, effectiveMe, "", mediaUri = uri.toString(), mediaType = "file", fileName = uri.lastPathSegment ?: "Document")
    }
  }

  val otherUid = if (conversation?.isGroup == false) conversation.participantUids.firstOrNull { it != currentUserId } else null
  val otherUser = otherUid?.let { id -> allUsers.firstOrNull { it.userId == id } }
  val headerTitle = if (conversation == null) {
    "Chat"
  } else if (conversation.isGroup) {
    conversation.title
  } else {
    conversation.participantNames[otherUid] ?: otherUser?.displayName ?: conversation.title
  }

  val typingOthers = conversation?.typingUids?.filter { it != currentUserId }.orEmpty()
  val otherReaders = conversation?.readByUids?.filter { it != currentUserId }.orEmpty()
  val lastMyMessageId = remember(messages, currentUserId) {
    messages.lastOrNull { it.senderId == currentUserId && !it.isDeleted }?.id
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .imePadding()
  ) {
    // Messenger Top App Bar
    Surface(tonalElevation = 2.dp, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MessengerBlue)
        }
        UserAvatar(
          name = headerTitle,
          photoUrl = otherUser?.photoUrl ?: "",
          size = 40.dp,
          isOnline = otherUser?.isOnline == true
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          val subtitle = when {
            typingOthers.isNotEmpty() -> {
              val names = typingOthers.mapNotNull { conversation?.participantNames?.get(it) }.ifEmpty { listOf("Someone") }
              "${names.joinToString(", ")} typing..."
            }
            otherUser?.isOnline == true -> "Active now"
            otherReaders.isNotEmpty() -> {
              if (conversation?.isGroup == true) {
                val names = otherReaders.mapNotNull { conversation.participantNames[it] }
                if (names.isNotEmpty()) "Seen by ${names.joinToString(", ")}" else "Seen by ${otherReaders.size}"
              } else {
                "Seen"
              }
            }
            else -> if (conversation?.isGroup == true) "${conversation.participantUids.size} members" else "HELLO Messenger"
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (typingOthers.isNotEmpty() || otherUser?.isOnline == true) MessengerBlue else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = { viewModel.leaveConversation(conversationId, onBack) }) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave chat", tint = MessengerBlue)
        }
      }
    }

    if (isUploading) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Sending media on Messenger...", style = MaterialTheme.typography.labelSmall, color = MessengerBlue)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MessengerBlue)
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
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == currentUserId
        val showSeenTick = isMine && msg.id == lastMyMessageId
        val bubbleShape = if (isMine) {
          RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
        } else {
          RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
        }
        val senderUser = if (isMine) null else allUsers.firstOrNull { it.userId == msg.senderId }

        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
          ) {
            if (!isMine) {
              UserAvatar(
                name = msg.senderName,
                photoUrl = senderUser?.photoUrl ?: "",
                size = 28.dp,
                modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
              )
            }
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
              if (!isMine && conversation?.isGroup == true) {
                Text(
                  text = msg.senderName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
              }

              Box(
                modifier = Modifier
                  .widthIn(max = 285.dp)
                  .clip(bubbleShape)
                  .then(
                    if (isMine) {
                      Modifier.background(Brush.linearGradient(listOf(MessengerBlue, MessengerBlueDark)))
                    } else {
                      Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    }
                  )
                  .clickable {
                    reactionTargetMsgId = if (reactionTargetMsgId == msg.id) null else msg.id
                  }
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  if (msg.mediaUri.isNotBlank() && !msg.isDeleted) {
                    when (msg.mediaType) {
                      "image" -> AsyncImage(
                        model = msg.mediaUri,
                        contentDescription = "Shared photo",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                      )
                      "video" -> VideoPlayerBox(videoUri = msg.mediaUri, modifier = Modifier.width(230.dp).height(150.dp), defaultHeight = null)
                      "audio" -> VoiceNotePlayer(audioUrl = msg.mediaUri, fileName = msg.fileName, isMine = isMine)
                      "file" -> Surface(
                        color = if (isMine) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable {
                          scope.launch {
                            val savedMsg = withContext(Dispatchers.IO) {
                              runCatching {
                                val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
                                val source = if (FirestoreMediaStore.isFirestoreMediaUri(msg.mediaUri)) {
                                  FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, msg.mediaUri)
                                } else null
                                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                                val cleanName = msg.fileName.ifBlank { "hello_file" }.replace("/", "_")
                                if (source != null && source.exists()) {
                                  val dest = File(targetDir, cleanName)
                                  source.copyTo(dest, overwrite = true)
                                  "Saved $cleanName (${dest.length() / 1024 + 1} KB)"
                                } else {
                                  "File: $cleanName"
                                }
                              }.getOrElse { "Unable to save attachment" }
                            }
                            Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                          }
                        }
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Icon(
                            Icons.Filled.Description,
                            contentDescription = "Download file",
                            tint = if (isMine) Color.White else MessengerBlue,
                            modifier = Modifier.size(18.dp)
                          )
                          Text(
                            text = msg.fileName.ifBlank { "Document" },
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                          )
                        }
                      }
                    }
                  }
                  if (msg.text.isNotBlank()) {
                    Text(
                      text = msg.text,
                      color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                      style = MaterialTheme.typography.bodyMedium
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = formatTimestamp(msg.createdAt),
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isMine) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (msg.reactions.isNotEmpty()) {
                      Surface(
                        color = if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text(
                          text = msg.reactions.values.joinToString(""),
                          style = MaterialTheme.typography.labelSmall,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                      }
                    }
                    Icon(
                      Icons.Filled.AddReaction,
                      contentDescription = "React",
                      modifier = Modifier.size(14.dp).clickable {
                        reactionTargetMsgId = if (reactionTargetMsgId == msg.id) null else msg.id
                      },
                      tint = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMine && !msg.isDeleted) {
                      Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete message",
                        modifier = Modifier.size(14.dp).clickable { viewModel.deleteMessage(conversationId, msg.id) },
                        tint = Color.White.copy(alpha = 0.75f)
                      )
                    }
                    if (showSeenTick) {
                      Icon(
                        Icons.Filled.DoneAll,
                        contentDescription = if (otherReaders.isNotEmpty()) "Seen" else "Sent",
                        modifier = Modifier.size(15.dp),
                        tint = if (otherReaders.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.55f)
                      )
                    }
                  }
                }
              }
            }
          }

          AnimatedVisibility(visible = reactionTargetMsgId == msg.id) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              tonalElevation = 4.dp,
              shadowElevation = 2.dp,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("👍", "❤️", "😂", "😮", "😢", "🔥").forEach { emoji ->
                  Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                      viewModel.reactToMessage(conversationId, msg, emoji)
                      reactionTargetMsgId = null
                    }
                  )
                }
              }
            }
          }
        }
      }

      if (typingOthers.isNotEmpty()) {
        item {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(18.dp)
            ) {
              Text(
                text = "● ● ●  Typing...",
                style = MaterialTheme.typography.labelMedium,
                color = MessengerBlue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
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
      Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
            Icon(Icons.Filled.Image, contentDescription = "Send photo or video", tint = MessengerBlue)
          }
          IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
            Icon(Icons.Filled.AttachFile, contentDescription = "Send file", tint = MessengerBlue)
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
            Icon(Icons.Filled.Mic, contentDescription = "Record voice note", tint = MessengerBlue)
          }
          OutlinedTextField(
            value = input,
            onValueChange = {
              input = it
              viewModel.setTyping(conversationId, it.isNotBlank())
            },
            placeholder = { Text("Aa") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              focusedBorderColor = Color.Transparent,
              unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f).testTag("chat_message_input"),
            singleLine = true
          )
          IconButton(
            onClick = {
              val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
              if (conversation != null) {
                if (input.isNotBlank()) {
                  viewModel.sendMessage(context, conversation, effectiveMe, input)
                  input = ""
                  viewModel.setTyping(conversationId, false)
                } else {
                  viewModel.sendMessage(context, conversation, effectiveMe, "👍")
                }
              }
            },
            modifier = Modifier.testTag("send_chat_button")
          ) {
            Icon(
              imageVector = if (input.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Filled.ThumbUp,
              contentDescription = if (input.isNotBlank()) "Send" else "Send thumbs up",
              tint = MessengerBlue
            )
          }
        }
      }
    }
  }

  pendingCropImageUri?.let { uriStr ->
    ImageCropDialog(
      imageUri = uriStr,
      defaultAspect = CropAspect.FREE,
      onDismiss = { pendingCropImageUri = null },
      onUseOriginal = {
        val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        if (conversation != null) {
          viewModel.sendMessage(context, conversation, effectiveMe, "", mediaUri = uriStr, mediaType = "image")
        }
        pendingCropImageUri = null
      },
      onCropped = { cropped ->
        val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        if (conversation != null) {
          viewModel.sendMessage(context, conversation, effectiveMe, "", mediaUri = cropped, mediaType = "image")
        }
        pendingCropImageUri = null
      }
    )
  }
}
