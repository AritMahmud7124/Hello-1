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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullPhotoImage
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.VoiceNotePlayer
import com.example.ui.components.formatTimestamp
import com.example.ui.viewmodel.CommunityViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HelloIndigo = Color(0xFF3B82F6)
private val HelloViolet = Color(0xFF6366F1)
private val HelloMagenta = Color(0xFF4F46E5)
private val HelloRose = Color(0xFFE11D48)
private val HelloAmber = Color(0xFF0EA5E9)
private val HelloBlue = Color(0xFF2563EB)

private val HelloRingBrush = Brush.linearGradient(listOf(HelloBlue, HelloViolet))
private val HelloBubbleBrush = Brush.linearGradient(listOf(HelloBlue, HelloIndigo))

private enum class DmInboxFilter(val label: String) {
  ALL("Primary"),
  UNREAD("Unread"),
  ACTIVE("Active"),
  GROUPS("Groups")
}

private enum class ThreadMediaFilter(val label: String) {
  ALL("All"),
  MEDIA("Photos & Video"),
  VOICE("Voice"),
  FILES("Files")
}

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
      .sortedByDescending { it.effectiveOnline }
  }

  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(DmInboxFilter.ALL) }
  var showNewGroup by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // HELLO Chat Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = me?.handle?.takeIf { it.isNotBlank() } ?: me?.displayName?.ifBlank { "HELLO Chat" } ?: "HELLO Chat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = HelloMagenta.copy(alpha = 0.14f)
          ) {
            Text(
              text = "HELLO",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = HelloMagenta,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = "${activePeople.count { it.effectiveOnline }} active now",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { showNewGroup = true }
            .testTag("new_group_chat_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.GroupAdd, contentDescription = "New group chat", modifier = Modifier.size(21.dp))
          }
        }
      }
    }

    // HELLO Search Pill
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search messages or friends") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search chats") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
          }
        }
      },
      shape = RoundedCornerShape(24.dp),
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

    Spacer(Modifier.height(12.dp))

    // HELLO Notes & Active Stories Ring Strip
    val filteredPeople = remember(activePeople, searchQuery) {
      if (searchQuery.isBlank()) activePeople else activePeople.filter {
        it.displayName.contains(searchQuery, true) || it.handle.contains(searchQuery, true)
      }
    }
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item(key = "my_note") {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(68.dp)
            .clickable { showNewGroup = true }
        ) {
          Box(contentAlignment = Alignment.TopCenter) {
            Box(
              modifier = Modifier
                .padding(top = 12.dp)
                .size(58.dp)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                .padding(3.dp)
            ) {
              UserAvatar(
                name = me?.displayName ?: "You",
                photoUrl = me?.photoUrl ?: "",
                size = 52.dp,
                isOnline = me?.effectiveOnline == true
              )
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              shadowElevation = 2.dp
            ) {
              Text(
                text = "Group +",
                style = MaterialTheme.typography.labelSmall,
                color = HelloViolet,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Your circle",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      items(filteredPeople, key = { it.userId }) { person ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(68.dp)
            .clickable {
              val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
              viewModel.startDirectChat(effectiveMe, person, onOpenThread)
            }
        ) {
          Box(contentAlignment = Alignment.TopCenter) {
            Box(
              modifier = Modifier
                .padding(top = 12.dp)
                .size(58.dp)
                .then(
                  if (person.effectiveOnline) {
                    Modifier.border(2.dp, HelloRingBrush, CircleShape)
                  } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                  }
                )
                .padding(3.dp)
            ) {
              UserAvatar(
                name = person.displayName,
                photoUrl = person.photoUrl,
                size = 52.dp,
                isOnline = person.effectiveOnline
              )
            }
            if (person.effectiveOnline) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
              ) {
                Text(
                  text = "Active",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
              }
            }
          }
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

    Spacer(Modifier.height(10.dp))

    when (val state = convState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> Box(Modifier.padding(16.dp)) {
        EmptyStateCard(Icons.Rounded.Forum, "Direct messages unavailable", state.message)
      }
      is UiState.Success -> {
        val allChats = state.data
        val unreadCount = remember(allChats, currentUserId) {
          allChats.count { currentUserId !in it.readByUids }
        }
        val activeChatCount = remember(allChats, usersById, currentUserId) {
          allChats.count { conv ->
            !conv.isGroup && conv.participantUids.any { it != currentUserId && usersById[it]?.effectiveOnline == true }
          }
        }
        val groupCount = remember(allChats) { allChats.count { it.isGroup } }

        // Smart Structured Filter Bar (HELLO All / Unread / Active / Groups)
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DmInboxFilter.entries) { filter ->
            val countSuffix = when (filter) {
              DmInboxFilter.ALL -> if (allChats.isNotEmpty()) " (${allChats.size})" else ""
              DmInboxFilter.UNREAD -> if (unreadCount > 0) " ($unreadCount)" else ""
              DmInboxFilter.ACTIVE -> if (activeChatCount > 0) " ($activeChatCount)" else ""
              DmInboxFilter.GROUPS -> if (groupCount > 0) " ($groupCount)" else ""
            }
            val selected = selectedFilter == filter
            FilterChip(
              selected = selected,
              onClick = { selectedFilter = filter },
              label = {
                Text(
                  text = "${filter.label}$countSuffix",
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HelloIndigo.copy(alpha = 0.14f),
                selectedLabelColor = HelloIndigo
              )
            )
          }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

        val filteredChats = remember(allChats, searchQuery, selectedFilter, usersById, currentUserId) {
          allChats.filter { conv ->
            val otherUid = if (conv.isGroup) null else conv.participantUids.firstOrNull { it != currentUserId }
            val title = if (conv.isGroup) conv.title else (conv.participantNames[otherUid] ?: usersById[otherUid]?.displayName ?: conv.title)
            val matchesQuery = searchQuery.isBlank() ||
              title.contains(searchQuery, true) ||
              conv.lastMessage.contains(searchQuery, true)
            val matchesFilter = when (selectedFilter) {
              DmInboxFilter.ALL -> true
              DmInboxFilter.UNREAD -> currentUserId !in conv.readByUids
              DmInboxFilter.ACTIVE -> !conv.isGroup && otherUid != null && usersById[otherUid]?.effectiveOnline == true
              DmInboxFilter.GROUPS -> conv.isGroup
            }
            matchesQuery && matchesFilter
          }
        }

        if (filteredChats.isEmpty()) {
          Box(Modifier.padding(16.dp)) {
            EmptyStateCard(
              icon = Icons.Rounded.Forum,
              title = when {
                searchQuery.isNotBlank() -> "No matching messages"
                selectedFilter == DmInboxFilter.UNREAD -> "All caught up"
                selectedFilter == DmInboxFilter.GROUPS -> "No group chats yet"
                else -> "Your HELLO inbox is quiet"
              },
              subtitle = "Tap any friend above to start a HELLO private chat, or create a group.",
              actionLabel = "New Group Chat",
              onAction = { showNewGroup = true }
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp)
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
              val seenByOther = conv.lastSenderId == currentUserId &&
                conv.readByUids.any { it != currentUserId }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenThread(conv.id) }
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(58.dp)
                    .then(
                      if (isUnread) Modifier.border(2.dp, HelloRingBrush, CircleShape).padding(3.dp)
                      else Modifier
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  UserAvatar(
                    name = displayTitle,
                    photoUrl = otherUser?.photoUrl ?: "",
                    size = if (isUnread) 52.dp else 56.dp,
                    isOnline = !conv.isGroup && otherUser?.effectiveOnline == true
                  )
                  if (conv.isGroup) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(HelloViolet)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                  }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val previewText = when {
                      otherTyping -> "Typing..."
                      conv.lastMessage.isNotBlank() -> {
                        val cleanPreview = conv.lastMessage.replace("\n", " ")
                        val prefix = if (conv.lastSenderId == currentUserId) "You: " else ""
                        "$prefix$cleanPreview"
                      }
                      else -> "Say hi on HELLO Direct"
                    }
                    Text(
                      text = previewText,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (isUnread || otherTyping) FontWeight.SemiBold else FontWeight.Normal,
                      color = when {
                        otherTyping -> HelloMagenta
                        isUnread -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                      text = if (seenByOther && !isUnread) "· Seen · ${formatTimestamp(conv.updatedAt)}" else "· ${formatTimestamp(conv.updatedAt)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (isUnread) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(HelloBlue)
                  )
                } else {
                  Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = "Open chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
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
    var memberQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<PublicUser>() }
    val candidates = remember(activePeople, memberQuery) {
      if (memberQuery.isBlank()) activePeople else activePeople.filter {
        it.displayName.contains(memberQuery, true) || it.handle.contains(memberQuery, true)
      }
    }

    AlertDialog(
      onDismissRequest = { showNewGroup = false },
      title = { Text("New Group Chat") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Group name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = memberQuery,
            onValueChange = { memberQuery = it },
            label = { Text("Search friends (${selected.size} selected)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          LazyColumn(modifier = Modifier.height(210.dp)) {
            items(candidates.take(25), key = { it.userId }) { person ->
              val checked = selected.any { it.userId == person.userId }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
                  }
                  .padding(vertical = 4.dp)
              ) {
                Checkbox(checked = checked, onCheckedChange = {
                  if (checked) selected.removeAll { it.userId == person.userId } else selected.add(person)
                })
                UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 32.dp, isOnline = person.effectiveOnline)
                Spacer(Modifier.width(8.dp))
                Text(person.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatThreadScreen(
  conversationId: String,
  viewModel: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onBack: () -> Unit,
  onOpenProfile: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val convState by viewModel.conversations.collectAsStateWithLifecycle()
  val usersState by viewModel.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val conversation = (convState as? UiState.Success)?.data?.firstOrNull { it.id == conversationId }
  val messageFlow = remember(viewModel, conversationId) { viewModel.observeMessages(conversationId) }
  val rawMessages by messageFlow.collectAsStateWithLifecycle()
  val messages = remember(rawMessages) { rawMessages.distinctBy { it.id } }

  var input by remember { mutableStateOf("") }
  var lastHeartSendMs by remember { mutableLongStateOf(0L) }
  var pendingCropImageUri by remember { mutableStateOf<String?>(null) }
  var reactionTargetMsgId by remember { mutableStateOf<String?>(null) }
  var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
  var showThreadSearch by remember { mutableStateOf(false) }
  var threadSearchQuery by remember { mutableStateOf("") }
  var mediaFilter by remember { mutableStateOf(ThreadMediaFilter.ALL) }

  val displayedMessages = remember(messages, threadSearchQuery, mediaFilter, showThreadSearch) {
    messages.filter { msg ->
      val matchesFilter = when (mediaFilter) {
        ThreadMediaFilter.ALL -> true
        ThreadMediaFilter.MEDIA -> msg.mediaType in listOf("image", "video")
        ThreadMediaFilter.VOICE -> msg.mediaType == "audio"
        ThreadMediaFilter.FILES -> msg.mediaType == "file"
      }
      val matchesSearch = !showThreadSearch || threadSearchQuery.isBlank() ||
        msg.text.contains(threadSearchQuery, true) ||
        msg.fileName.contains(threadSearchQuery, true) ||
        msg.senderName.contains(threadSearchQuery, true)
      matchesFilter && matchesSearch
    }
  }

  val listState = rememberLazyListState()
  LaunchedEffect(displayedMessages.size) {
    if (displayedMessages.isNotEmpty() && !showThreadSearch && mediaFilter == ThreadMediaFilter.ALL) {
      listState.animateScrollToItem(displayedMessages.lastIndex)
    }
  }

  // Voice recording + Call + Group Out state
  var isRecording by remember { mutableStateOf(false) }
  var isRecordingLocked by remember { mutableStateOf(false) }
  var recordingSeconds by remember { mutableIntStateOf(0) }
  var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
  var recordedFile by remember { mutableStateOf<File?>(null) }
  var audioHint by remember { mutableStateOf<String?>(null) }
  val liveAmplitudes = remember { mutableStateListOf<Float>() }
  var pendingCallType by remember { mutableStateOf<String?>(null) }
  var showGroupMembersDialog by remember { mutableStateOf(false) }

  fun releaseRecorder(deleteFile: Boolean) {
    runCatching { activeRecorder?.stop() }
    runCatching { activeRecorder?.release() }
    activeRecorder = null
    isRecording = false
    isRecordingLocked = false
    liveAmplitudes.clear()
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

  val callPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    val type = pendingCallType ?: return@rememberLauncherForActivityResult
    pendingCallType = null
    val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = type != "video" ||
      grants[Manifest.permission.CAMERA] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (audioGranted && cameraGranted && conversation != null) {
      val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
      viewModel.startMessengerCall(conversation, effectiveMe, type)
    } else {
      audioHint = if (type == "video") {
        "Microphone & Camera permissions are needed for video calls."
      } else {
        "Microphone permission is needed for audio calls."
      }
    }
  }

  fun initiateMessengerCall(type: String) {
    val conv = conversation ?: return
    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val needed = buildList {
      if (!hasAudio) add(Manifest.permission.RECORD_AUDIO)
      if (type == "video" && !hasCam) add(Manifest.permission.CAMERA)
    }
    if (needed.isEmpty()) {
      val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
      viewModel.startMessengerCall(conv, effectiveMe, type)
    } else {
      pendingCallType = type
      callPermLauncher.launch(needed.toTypedArray())
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

  LaunchedEffect(isRecording, activeRecorder) {
    if (isRecording) {
      liveAmplitudes.clear()
      repeat(22) { liveAmplitudes.add(0.22f) }
      while (isRecording) {
        val rawAmp = runCatching { activeRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        val normalized = if (rawAmp > 0) {
          ((rawAmp.toFloat() / 16000f).coerceIn(0.18f, 1f))
        } else {
          0.22f
        }
        if (liveAmplitudes.size >= 22) liveAmplitudes.removeAt(0)
        liveAmplitudes.add(normalized)
        delay(65L)
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
        viewModel.sendMessage(context, conversation, effectiveMe, "", mediaUri = uri.toString(), mediaType = "image")
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
      .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
  ) {
    // HELLO Chat Top Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
    ) {
      Column {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .then(if (otherUid != null) Modifier.clickable { onOpenProfile(otherUid) } else Modifier)
              .padding(vertical = 2.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .then(
                  if (otherUser?.effectiveOnline == true) Modifier.border(1.8.dp, HelloRingBrush, CircleShape).padding(2.dp)
                  else Modifier
                )
            ) {
              UserAvatar(
                name = headerTitle,
                photoUrl = otherUser?.photoUrl ?: "",
                size = 38.dp,
                isOnline = otherUser?.effectiveOnline == true
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              val subtitle = when {
                typingOthers.isNotEmpty() -> {
                  val names = typingOthers.mapNotNull { conversation?.participantNames?.get(it) }.ifEmpty { listOf("Someone") }
                  "${names.joinToString(", ")} typing..."
                }
                otherUser?.effectiveOnline == true -> if (otherUser.handle.isNotBlank()) "${otherUser.handle} · Active now" else "Active now"
                otherUser?.handle?.isNotBlank() == true -> otherUser.handle
                conversation?.isGroup == true -> "${conversation.participantUids.size} members"
                else -> "HELLO Chat"
              }
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (typingOthers.isNotEmpty()) HelloMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          if (conversation != null) {
            IconButton(
              onClick = { initiateMessengerCall("audio") },
              modifier = Modifier.testTag("chat_audio_call_button")
            ) {
              Icon(Icons.Rounded.Call, contentDescription = "Audio call")
            }
            IconButton(
              onClick = { initiateMessengerCall("video") },
              modifier = Modifier.testTag("chat_video_call_button")
            ) {
              Icon(Icons.Rounded.Videocam, contentDescription = "Video call")
            }
          }
          if (conversation?.isGroup == true) {
            IconButton(
              onClick = { showGroupMembersDialog = true },
              modifier = Modifier.testTag("chat_group_members_button")
            ) {
              Icon(Icons.Rounded.Groups, contentDescription = "Group members & out")
            }
          } else if (otherUid != null) {
            IconButton(
              onClick = { onOpenProfile(otherUid) },
              modifier = Modifier.testTag("chat_view_profile_button")
            ) {
              Icon(Icons.Rounded.Person, contentDescription = "View profile")
            }
          }
          IconButton(onClick = { showThreadSearch = !showThreadSearch }) {
            Icon(Icons.Rounded.Search, contentDescription = "Filter messages")
          }
          IconButton(
            onClick = {
              if (conversation?.isGroup == true) showGroupMembersDialog = true
              else viewModel.leaveConversation(conversationId, onBack)
            },
            modifier = Modifier.testTag("chat_leave_button")
          ) {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = if (conversation?.isGroup == true) "Group Out" else "Leave chat")
          }
        }

        AnimatedVisibility(visible = showThreadSearch) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = threadSearchQuery,
              onValueChange = { threadSearchQuery = it },
              placeholder = { Text("Search inside conversation...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(ThreadMediaFilter.entries) { filter ->
                FilterChip(
                  selected = mediaFilter == filter,
                  onClick = { mediaFilter = filter },
                  label = { Text(filter.label) }
                )
              }
            }
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
      }
    }

    if (isUploading) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Sending media...", style = MaterialTheme.typography.labelSmall, color = HelloMagenta)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = HelloMagenta)
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
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
      itemsIndexed(displayedMessages, key = { _, msg -> msg.id }) { index, msg ->
        val prevMsg = displayedMessages.getOrNull(index - 1)
        val nextMsg = displayedMessages.getOrNull(index + 1)
        val isMine = msg.senderId == currentUserId

        val showSessionHeader = shouldShowSessionDivider(prevMsg?.createdAt?.toDate(), msg.createdAt?.toDate())
        val isFirstInGroup = prevMsg == null || prevMsg.senderId != msg.senderId || showSessionHeader
        val isLastInGroup = nextMsg == null || nextMsg.senderId != msg.senderId ||
          shouldShowSessionDivider(msg.createdAt?.toDate(), nextMsg.createdAt?.toDate())

        val topGap = if (isFirstInGroup) 10.dp else 2.dp
        val bubbleShape = if (isMine) {
          RoundedCornerShape(
            topStart = 20.dp,
            bottomStart = 20.dp,
            topEnd = if (isFirstInGroup) 20.dp else 6.dp,
            bottomEnd = if (isLastInGroup) 20.dp else 6.dp
          )
        } else {
          RoundedCornerShape(
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            topStart = if (isFirstInGroup) 20.dp else 6.dp,
            bottomStart = if (isLastInGroup) 20.dp else 6.dp
          )
        }

        val senderUser = if (isMine) null else allUsers.firstOrNull { it.userId == msg.senderId }
        val (replyQuote, bodyText) = remember(msg.text) { parseQuotedMessage(msg.text) }

        if (showSessionHeader) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = formatSessionDivider(msg.createdAt?.toDate()),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = topGap),
          horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
          ) {
            if (!isMine) {
              if (isLastInGroup) {
                UserAvatar(
                  name = msg.senderName,
                  photoUrl = senderUser?.photoUrl ?: "",
                  size = 28.dp,
                  modifier = Modifier.padding(end = 8.dp, bottom = 2.dp)
                )
              } else {
                Spacer(Modifier.width(36.dp))
              }
            }

            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
              if (!isMine && conversation?.isGroup == true && isFirstInGroup) {
                Text(
                  text = msg.senderName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
              }

              Box(
                modifier = Modifier
                  .widthIn(max = 285.dp)
                  .clip(bubbleShape)
                  .then(
                    if (msg.isDeleted) {
                      Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), bubbleShape)
                        .background(MaterialTheme.colorScheme.surface)
                    } else if (isMine) {
                      Modifier.background(HelloBubbleBrush)
                    } else {
                      Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    }
                  )
                  .combinedClickable(
                    onClick = {
                      reactionTargetMsgId = if (reactionTargetMsgId == msg.id) null else msg.id
                    },
                    onDoubleClick = {
                      if (!msg.isDeleted) {
                        viewModel.reactToMessage(conversationId, msg, "❤️")
                      }
                    }
                  )
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                  verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  if (!replyQuote.isNullOrBlank() && !msg.isDeleted) {
                    Surface(
                      color = if (isMine) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text(
                        text = replyQuote,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                      )
                    }
                  }

                  if (msg.mediaUri.isNotBlank() && !msg.isDeleted) {
                    when (msg.mediaType) {
                      "image" -> FullPhotoImage(
                        model = msg.mediaUri,
                        contentDescription = "Shared photo",
                        modifier = Modifier.widthIn(min = 150.dp, max = 250.dp),
                        cornerRadius = 14.dp,
                        minHeight = 140.dp,
                        maxHeight = 360.dp,
                        enableFullscreenViewer = true
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
                            Icons.Rounded.Description,
                            contentDescription = "Download file",
                            tint = if (isMine) Color.White else HelloIndigo,
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

                  if (bodyText.isNotBlank()) {
                    Text(
                      text = bodyText,
                      color = when {
                        msg.isDeleted -> MaterialTheme.colorScheme.onSurfaceVariant
                        isMine -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                      },
                      style = MaterialTheme.typography.bodyMedium
                    )
                  }
                }
              }

              // Floating IG Reactions Pill under bubble corner
              if (msg.reactions.isNotEmpty()) {
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  tonalElevation = 2.dp,
                  shadowElevation = 2.dp,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  Text(
                    text = msg.reactions.values.joinToString(""),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }

              // Seen / Sent status under last outgoing bubble
              if (isMine && msg.id == lastMyMessageId) {
                Text(
                  text = if (otherReaders.isNotEmpty()) "Seen" else "Sent · ${formatTimestamp(msg.createdAt)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 3.dp, end = 4.dp)
                )
              }
            }
          }

          // Tap-to-react + Reply + Unsend bar
          AnimatedVisibility(visible = reactionTargetMsgId == msg.id) {
            Surface(
              shape = RoundedCornerShape(24.dp),
              tonalElevation = 4.dp,
              shadowElevation = 3.dp,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("❤️", "😂", "😮", "😢", "🔥", "👍").forEach { emoji ->
                  Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                      viewModel.reactToMessage(conversationId, msg, emoji)
                      reactionTargetMsgId = null
                    }
                  )
                }
                if (!msg.isDeleted) {
                  IconButton(
                    onClick = {
                      replyingTo = msg
                      reactionTargetMsgId = null
                    },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "Reply", modifier = Modifier.size(18.dp))
                  }
                }
                if (isMine && !msg.isDeleted) {
                  IconButton(
                    onClick = {
                      viewModel.deleteMessage(conversationId, msg.id)
                      reactionTargetMsgId = null
                    },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Unsend", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }

      if (typingOthers.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            UserAvatar(
              name = headerTitle,
              photoUrl = otherUser?.photoUrl ?: "",
              size = 28.dp
            )
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(20.dp)
            ) {
              Text(
                text = "● ● ●",
                style = MaterialTheme.typography.labelMedium,
                color = HelloMagenta,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
              )
            }
          }
        }
      }
    }

    // Replying banner (HELLO quote reply)
    AnimatedVisibility(visible = replyingTo != null) {
      replyingTo?.let { target ->
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Replying to ${if (target.senderId == currentUserId) "yourself" else target.senderName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HelloIndigo
              )
              Text(
                text = target.text.ifBlank { target.fileName.ifBlank { target.mediaType } },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    if (isRecording) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = { releaseRecorder(deleteFile = true) },
              modifier = Modifier.testTag("cancel_voice_note_button")
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Cancel recording", tint = MaterialTheme.colorScheme.onErrorContainer)
            }

            // Live Waveform Visualizer Strip
            Row(
              modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.error)
              )
              Text(
                text = "%d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
              Row(
                modifier = Modifier.weight(1f).height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                liveAmplitudes.forEach { level ->
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height((22.dp * level).coerceIn(4.dp, 22.dp))
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.error)
                  )
                }
              }
            }

            IconButton(
              onClick = { isRecordingLocked = !isRecordingLocked },
              modifier = Modifier
                .clip(CircleShape)
                .background(
                  if (isRecordingLocked) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                )
                .testTag("lock_voice_note_button")
            ) {
              Icon(
                imageVector = if (isRecordingLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = if (isRecordingLocked) "Voice recording locked" else "Lock voice recording",
                tint = if (isRecordingLocked) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
              )
            }

            Button(
              onClick = { stopAndSendVoiceNote() },
              modifier = Modifier.testTag("send_voice_note_button")
            ) {
              Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Send")
            }
          }
          if (isRecordingLocked) {
            Text(
              text = "🔒 Hands-free locked recording active · Speak freely and tap Send when ready",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 8.dp)
            )
          }
        }
      }
    } else {
      // HELLO Pill Composer - flush directly above IME keyboard
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(HelloBubbleBrush)
                .clickable { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.PhotoCamera, contentDescription = "Send photo or video", tint = Color.White, modifier = Modifier.size(19.dp))
            }

            OutlinedTextField(
              value = input,
              onValueChange = {
                input = it
                viewModel.setTyping(conversationId, it.isNotBlank())
              },
              placeholder = { Text("Message...") },
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
              ),
              modifier = Modifier.weight(1f).testTag("chat_message_input"),
              singleLine = true
            )

            if (input.isBlank()) {
              IconButton(
                onClick = {
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecording()
                  } else {
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                },
                modifier = Modifier.size(38.dp).testTag("record_voice_note_button")
              ) {
                Icon(Icons.Rounded.Mic, contentDescription = "Record voice note")
              }
              IconButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier.size(38.dp)
              ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Send file")
              }
              IconButton(
                onClick = {
                  val now = System.currentTimeMillis()
                  val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                  if (conversation != null && now - lastHeartSendMs > 450L) {
                    lastHeartSendMs = now
                    viewModel.sendMessage(context, conversation, effectiveMe, "❤️")
                  }
                },
                modifier = Modifier.size(40.dp).testTag("send_chat_button")
              ) {
                Icon(Icons.Rounded.Favorite, contentDescription = "Send heart", tint = HelloRose)
              }
            } else {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = HelloBlue,
                modifier = Modifier
                  .padding(end = 4.dp)
                  .clip(RoundedCornerShape(20.dp))
                  .clickable {
                    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                    val textToSend = input
                    if (conversation != null && textToSend.isNotBlank()) {
                      input = ""
                      val quoteTarget = replyingTo
                      replyingTo = null
                      val payloadText = quoteTarget?.let { target ->
                        val who = if (target.senderId == currentUserId) "You" else target.senderName
                        val snippet = target.text.ifBlank { target.fileName.ifBlank { target.mediaType } }
                          .replace("\n", " ")
                          .take(65)
                        "↩ Replying to $who: \"$snippet\"\n${textToSend.trim()}"
                      } ?: textToSend
                      viewModel.sendMessage(context, conversation, effectiveMe, payloadText)
                      viewModel.setTyping(conversationId, false)
                    }
                  }
                  .testTag("send_chat_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text("Send", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(15.dp))
                }
              }
            }
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

  if (showGroupMembersDialog && conversation != null && conversation.isGroup) {
    val nonMembers = remember(allUsers, conversation.participantUids) {
      allUsers.filter { it.userId !in conversation.participantUids }.take(20)
    }
    AlertDialog(
      onDismissRequest = { showGroupMembersDialog = false },
      title = { Text("${conversation.title} · Group Members") },
      text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            Text(
              "Members (${conversation.participantUids.size}) · Tap Out to remove or leave group:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          items(conversation.participantUids, key = { it }) { memberUid ->
            val userObj = allUsers.firstOrNull { it.userId == memberUid }
            val memberName = conversation.participantNames[memberUid] ?: userObj?.displayName ?: "Member"
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              UserAvatar(name = memberName, photoUrl = userObj?.photoUrl ?: "", size = 32.dp)
              Text(
                text = if (memberUid == currentUserId) "$memberName (You)" else memberName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              TextButton(
                onClick = {
                  if (memberUid == currentUserId) {
                    showGroupMembersDialog = false
                    viewModel.leaveConversation(conversationId, onBack)
                  } else {
                    viewModel.removeGroupMember(conversation, memberUid, memberName) {
                      showGroupMembersDialog = false
                      onBack()
                    }
                  }
                },
                modifier = Modifier.testTag("group_out_member_$memberUid")
              ) {
                Icon(Icons.Rounded.PersonRemove, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text(if (memberUid == currentUserId) "Leave" else "Out", color = MaterialTheme.colorScheme.error)
              }
            }
          }
          if (nonMembers.isNotEmpty()) {
            item {
              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
              Text("Add Friend to Group", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            items(nonMembers, key = { "add_${it.userId}" }) { person ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { viewModel.addGroupMembers(conversation, listOf(person)) }
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 30.dp)
                Text(person.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("+ Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showGroupMembersDialog = false
            viewModel.leaveConversation(conversationId, onBack)
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("group_leave_out_button")
        ) {
          Icon(Icons.AutoMirrored.Rounded.ExitToApp, null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Leave Group (Out)")
        }
      },
      dismissButton = {
        TextButton(onClick = { showGroupMembersDialog = false }) { Text("Close") }
      }
    )
  }
}

private fun parseQuotedMessage(raw: String): Pair<String?, String> {
  if (!raw.startsWith("↩ Replying to ")) return null to raw
  val splitIdx = raw.indexOf('\n')
  if (splitIdx <= 0) return null to raw
  val header = raw.substring(2, splitIdx).trim()
  val body = raw.substring(splitIdx + 1).trim()
  return header to body
}

private fun shouldShowSessionDivider(prev: Date?, curr: Date?): Boolean {
  if (curr == null) return false
  if (prev == null) return true
  val prevCal = Calendar.getInstance().apply { time = prev }
  val currCal = Calendar.getInstance().apply { time = curr }
  val differentDay = prevCal.get(Calendar.YEAR) != currCal.get(Calendar.YEAR) ||
    prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR)
  return differentDay || (curr.time - prev.time) > 60 * 60 * 1000L
}

private fun formatSessionDivider(date: Date?): String {
  if (date == null) return "Today"
  val now = Calendar.getInstance()
  val target = Calendar.getInstance().apply { time = date }
  val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
  return when {
    now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
      now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "Today · $timeFmt"
    now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
      now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday · $timeFmt"
    else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
  }
}
