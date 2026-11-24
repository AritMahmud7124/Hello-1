package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MutualCrushMatch
import com.example.data.model.MutualMatchPhase
import com.example.data.model.MutualSecretLoveEngine
import com.example.data.model.PublicUser
import com.example.data.model.PureLoveEngine
import com.example.data.model.UiState
import com.example.security.EncryptedVaultStorage
import com.example.ui.components.BadgeUnlockOverlay
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MutualHeartVaultLockIcon
import com.example.ui.components.MutualMatchVaultCard
import com.example.ui.components.PureLoveQuestTab
import com.example.ui.components.SecretCrushMatchOverlay
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun SecretLoveScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  isFeatureEnabled: Boolean = true,
  vaultStorage: EncryptedVaultStorage? = null
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  val vault = remember(context, vaultStorage) { vaultStorage ?: EncryptedVaultStorage(context) }

  val inboxState by spacesVm.receivedSecretLoves.collectAsStateWithLifecycle()
  val sentList by spacesVm.sentSecretLoves.collectAsStateWithLifecycle()
  val isSendingLove by spacesVm.isSendingSecretLove.collectAsStateWithLifecycle()
  val devotionTrials by spacesVm.myDevotionTrials.collectAsStateWithLifecycle()
  val mySecretCrushes by spacesVm.mySecretCrushes.collectAsStateWithLifecycle()
  val myMutualMatches by spacesVm.myMutualMatches.collectAsStateWithLifecycle()
  val liveClockMs by spacesVm.liveClockMs.collectAsStateWithLifecycle()
  val latestUnlockedMatch by spacesVm.latestUnlockedMatch.collectAsStateWithLifecycle()
  val latestBadgeUnlocked by spacesVm.latestBadgeUnlocked.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val allUsers = (usersState as? UiState.Success)?.data ?: emptyList()
  val recipients = allUsers.filter { it.userId != currentUserId && it.acceptSecretLove }

  // Complete stealth before countdown ends: only UNLOCKED matches are surfaced.
  val visibleMutualMatches = remember(myMutualMatches, liveClockMs) {
    myMutualMatches.filter { MutualSecretLoveEngine.phaseOf(it, liveClockMs) == MutualMatchPhase.UNLOCKED }
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedRecipient by remember { mutableStateOf<PublicUser?>(null) }
  var reportTargetId by remember { mutableStateOf<String?>(null) }
  var activePrivateChatMatch by remember { mutableStateOf<MutualCrushMatch?>(null) }
  var maskEditMatch by remember { mutableStateOf<MutualCrushMatch?>(null) }
  var maskNonce by remember { mutableIntStateOf(0) }

  if (!isFeatureEnabled) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
      EmptyStateCard(
        icon = Icons.Rounded.Favorite,
        title = "Secret Love Paused",
        subtitle = "Secret Love messaging is currently turned off in platform settings."
      )
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(126.dp)
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
          Text("${strings.secretLove} ❤️", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
          Text(
            "Pure Love Quest, anonymous hints & mystery mutual vaults.",
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
              profile.showOnlineStatus
            )
          },
          modifier = Modifier.testTag("secret_love_privacy_switch")
        )
      }
    }

    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
      Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inbox") })
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            MutualHeartVaultLockIcon(size = 16.dp, animateGlow = selectedTab == 1)
            Text(if (visibleMutualMatches.isNotEmpty()) "Mutual (${visibleMutualMatches.size})" else "Mutual")
          }
        },
        modifier = Modifier.testTag("secret_love_mutual_tab")
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Quest 🔥") },
        modifier = Modifier.testTag("secret_love_quest_tab")
      )
      Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Send ❤️") })
      Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Sent (${sentList.size})") })
    }

    when (selectedTab) {
      0 -> {
        when (val state = inboxState) {
          is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
          is UiState.Error -> EmptyStateCard(Icons.Rounded.Favorite, "Inbox unavailable", state.message)
          is UiState.Success -> {
            val visible = state.data.filter { !it.isBlocked }
            if (visible.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Rounded.Favorite,
                title = "No Secret Love messages yet",
                subtitle = "When someone completes Day 7 of the Pure Love Quest for you, their anonymous compliment arrives here."
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
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
                      if (msg.senderBadgeTitle.isNotBlank()) {
                        Surface(
                          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                          shape = RoundedCornerShape(10.dp)
                        ) {
                          Text(
                            text = "${msg.senderBadgeTitle} · Day ${msg.senderTrialDays} Pure Love Dedication",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                          )
                        }
                      }
                      Text(msg.message, style = MaterialTheme.typography.bodyLarge)
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { spacesVm.blockOrReportSecretLove(msg.id, block = true, reportReason = null) }) {
                          Icon(Icons.Rounded.Block, null, modifier = Modifier.padding(end = 4.dp))
                          Text("Block")
                        }
                        OutlinedButton(onClick = { reportTargetId = msg.id }) {
                          Icon(Icons.Rounded.Flag, null, modifier = Modifier.padding(end = 4.dp))
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
        var crushQuery by remember { mutableStateOf("") }
        val filteredCrushPool = remember(recipients, crushQuery) {
          if (crushQuery.isBlank()) recipients else recipients.filter {
            it.displayName.contains(crushQuery, true) || it.handle.contains(crushQuery, true)
          }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Text(
              text = "Mutual Secret Love Vault",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Add your Secret Crush privately below. Mutual seals stay completely hidden until the countdown officially unlocks—and either person can opt out silently anytime.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (visibleMutualMatches.isEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  MutualHeartVaultLockIcon(size = 56.dp, isUnlocked = false, animateGlow = true)
                  Text(
                    text = "Mystery Vault Locked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                  )
                  Text(
                    text = "Select someone privately below. Nothing is revealed until a mutual vault officially unlocks, and you can remove your pick quietly anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          } else {
            items(visibleMutualMatches, key = { "${it.id}_$maskNonce" }) { match ->
              val partnerUid = match.partnerUidFor(currentUserId)
              val phase = MutualSecretLoveEngine.phaseOf(match, liveClockMs)
              val partnerProfile = if (phase == MutualMatchPhase.UNLOCKED) {
                allUsers.firstOrNull { it.userId == partnerUid }
              } else {
                null
              }
              val alias = remember(match.id, maskNonce) { vault.getSecretMatchAlias(match.id) }
              val hideAvatar = remember(match.id, maskNonce) { vault.getSecretMatchHideAvatar(match.id) }
              MutualMatchVaultCard(
                match = match,
                currentUserId = currentUserId,
                nowMs = liveClockMs,
                partnerProfile = partnerProfile,
                onCelebrate = { spacesVm.openUnlockedMatchCelebration(match) },
                displayNameOverride = alias,
                hidePartnerAvatar = hideAvatar,
                onOpenPrivateChat = if (phase == MutualMatchPhase.UNLOCKED) {
                  { activePrivateChatMatch = match }
                } else null,
                onConfigureMask = if (phase == MutualMatchPhase.UNLOCKED) {
                  { maskEditMatch = match }
                } else null
              )
            }
          }
          item {
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Secret Crush Picks (${mySecretCrushes.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
              value = crushQuery,
              onValueChange = { crushQuery = it },
              placeholder = { Text("Find someone to add privately...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
          items(filteredCrushPool, key = { "crush_pick_${it.userId}" }) { person ->
            val added = person.userId in mySecretCrushes
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = if (added) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
              )
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                  Text(person.displayName, fontWeight = FontWeight.SemiBold)
                  Text(
                    text = person.handle.ifBlank { "HELLO Member" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (added) {
                  OutlinedButton(
                    onClick = { spacesVm.toggleSecretCrush(person) },
                    modifier = Modifier.testTag("toggle_secret_crush_${person.userId}")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                      MutualHeartVaultLockIcon(size = 16.dp)
                      Text("Added")
                    }
                  }
                } else {
                  Button(
                    onClick = { spacesVm.toggleSecretCrush(person) },
                    modifier = Modifier.testTag("toggle_secret_crush_${person.userId}")
                  ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Crush")
                  }
                }
              }
            }
          }
        }
      }
      2 -> {
        PureLoveQuestTab(
          recipients = recipients,
          selectedRecipient = selectedRecipient,
          onSelectRecipient = { selectedRecipient = it },
          trials = devotionTrials,
          onSetTargetTier = { person, days -> spacesVm.startOrUpdatePureLoveQuest(person, days) },
          onSubmitDailyCheckIn = { person, days, reflection, onDone ->
            spacesVm.submitDailyPureLoveCheckIn(person, days, reflection) { onDone() }
          },
          onJumpToSendTab = { selectedTab = 3 }
        )
      }
      3 -> {
        var recipientQuery by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var mood by remember { mutableStateOf("❤️") }
        var revealNow by remember { mutableStateOf(false) }
        var inlineStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
        val filteredRecipients = remember(recipients, recipientQuery) {
          if (recipientQuery.isBlank()) recipients else recipients.filter {
            it.displayName.contains(recipientQuery, true) || it.handle.contains(recipientQuery, true)
          }
        }
        val activeTrial = remember(selectedRecipient?.userId, devotionTrials) {
          selectedRecipient?.let { r -> devotionTrials.firstOrNull { it.recipientId == r.userId } }
        }
        val completedDays = activeTrial?.completedDays ?: 0
        val isGateUnlocked = selectedRecipient == null || completedDays >= PureLoveEngine.MIN_HINT_UNLOCK_DAYS

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Text("Choose Recipient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
              value = recipientQuery,
              onValueChange = { recipientQuery = it },
              placeholder = { Text("Search people by name or @handle...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
          if (filteredRecipients.isEmpty()) {
            item {
              EmptyStateCard(
                Icons.Rounded.Lock,
                if (recipients.isEmpty()) "No recipients available" else "No matching people",
                if (recipients.isEmpty()) "Other members who enable Secret Love will show up here." else "Try another name or @handle."
              )
            }
          } else {
            items(filteredRecipients, key = { it.userId }) { person ->
              val isPicked = selectedRecipient?.userId == person.userId
              val personDays = devotionTrials.firstOrNull { it.recipientId == person.userId }?.completedDays ?: 0
              Card(
                modifier = Modifier.fillMaxWidth().clickable {
                  selectedRecipient = person
                  inlineStatus = null
                },
                colors = CardDefaults.cardColors(
                  containerColor = if (isPicked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                )
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  UserAvatar(name = person.displayName, photoUrl = person.photoUrl, size = 38.dp)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(person.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                      text = "${person.handle.ifBlank { "HELLO Member" }} · Pure Love Day $personDays/7",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (personDays >= 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  if (isPicked) Text("Selected ✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }

          if (selectedRecipient != null && !isGateUnlocked) {
            item {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("pure_love_gate_banner")
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                      text = "Day 7 Pure Love Gate Locked ($completedDays/7 days)",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.error
                    )
                  }
                  LinearProgressIndicator(
                    progress = { (completedDays / 7f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
                  )
                  Text(
                    text = "Complete 7 daily Pure Love trials (memory quiz, 120-char reflection & focus hold) for ${selectedRecipient?.displayName} before sending anonymous hints.",
                    style = MaterialTheme.typography.bodySmall
                  )
                  Button(
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.fillMaxWidth().testTag("open_quest_from_gate_button")
                  ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open Pure Love Quest")
                  }
                }
              }
            }
          }

          item {
            inlineStatus?.let { (isOk, msg) ->
              Surface(
                color = if (isOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = msg,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(12.dp)
                )
              }
              Spacer(Modifier.height(8.dp))
            }
            if (selectedRecipient != null && isGateUnlocked) {
              val badge = PureLoveEngine.badgeForDays(completedDays)
              if (badge != null) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "${badge.badgeEmoji} Badge Verified: ${badge.badgeTitle} (Day $completedDays)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                  )
                }
                Spacer(Modifier.height(8.dp))
              }
            }
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
            if (selectedRecipient == null && note.isNotBlank()) {
              Text(
                "Tap a recipient above before sending your private note.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
              )
              Spacer(Modifier.height(4.dp))
            }
            Button(
              onClick = {
                val target = selectedRecipient
                val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                if (target != null && !isSendingLove && isGateUnlocked) {
                  inlineStatus = null
                  spacesVm.sendSecretLove(effectiveMe, target, note, mood, revealNow) { res ->
                    res.onSuccess {
                      note = ""
                      inlineStatus = true to (if (revealNow) "Private love note sent with your name!" else "Anonymous Secret Love sent privately ❤️")
                    }.onFailure { err ->
                      inlineStatus = false to (err.localizedMessage ?: "Failed to send Secret Love")
                    }
                  }
                }
              },
              enabled = selectedRecipient != null && isGateUnlocked && note.isNotBlank() && !isSendingLove,
              modifier = Modifier.fillMaxWidth().testTag("send_secret_love_button")
            ) {
              Text(
                when {
                  isSendingLove -> "Sending Privately..."
                  selectedRecipient != null && !isGateUnlocked -> "Complete Day 7 Quest to Unlock"
                  revealNow -> "Send With My Name"
                  else -> strings.sendSecretLove
                }
              )
            }
          }
        }
      }
      4 -> {
        if (sentList.isEmpty()) {
          EmptyStateCard(Icons.Rounded.Favorite, "No sent Secret Love notes", "Your private sent receipts appear here so you can optionally reveal your identity later.")
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
                    OutlinedButton(onClick = {
                      val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                      spacesVm.revealSecretLove(sent, effectiveMe)
                    }) {
                      Icon(Icons.Rounded.Visibility, null)
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

  activePrivateChatMatch?.let { match ->
    val partnerUid = match.partnerUidFor(currentUserId)
    val partnerProfile = allUsers.firstOrNull { it.userId == partnerUid }
    val alias = remember(match.id, maskNonce) { vault.getSecretMatchAlias(match.id) }
    val hideAvatar = remember(match.id, maskNonce) { vault.getSecretMatchHideAvatar(match.id) }
    PrivateSecretLoveChatDialog(
      match = match,
      currentUserId = currentUserId,
      partnerProfile = partnerProfile,
      aliasOverride = alias,
      hidePartnerAvatar = hideAvatar,
      spacesVm = spacesVm,
      onOpenMaskSettings = { maskEditMatch = match },
      onDismiss = { activePrivateChatMatch = null }
    )
  }

  maskEditMatch?.let { match ->
    val partnerUid = match.partnerUidFor(currentUserId)
    val partnerName = allUsers.firstOrNull { it.userId == partnerUid }?.displayName.orEmpty().ifBlank { "Secret Match" }
    SecretMatchDisguiseDialog(
      realName = partnerName,
      initialAlias = vault.getSecretMatchAlias(match.id),
      initialHideAvatar = vault.getSecretMatchHideAvatar(match.id),
      onSave = { nextAlias, nextHideAvatar ->
        vault.setSecretMatchMask(match.id, nextAlias, nextHideAvatar)
        maskNonce++
        maskEditMatch = null
      },
      onDismiss = { maskEditMatch = null }
    )
  }

  latestUnlockedMatch?.let { match ->
    if (MutualSecretLoveEngine.phaseOf(match, liveClockMs) == MutualMatchPhase.UNLOCKED) {
      val partnerUid = match.partnerUidFor(currentUserId)
      val partnerName = allUsers.firstOrNull { it.userId == partnerUid }?.displayName?.ifBlank { "your Secret Crush" } ?: "your Secret Crush"
      SecretCrushMatchOverlay(
        recipientName = partnerName,
        onDismiss = { spacesVm.dismissUnlockedMatch() }
      )
    }
  }

  latestBadgeUnlocked?.let { tier ->
    BadgeUnlockOverlay(
      tier = tier,
      onDismiss = { spacesVm.dismissBadgeUnlocked() }
    )
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

@Composable
private fun PrivateSecretLoveChatDialog(
  match: MutualCrushMatch,
  currentUserId: String,
  partnerProfile: PublicUser?,
  aliasOverride: String,
  hidePartnerAvatar: Boolean,
  spacesVm: SpacesGovernanceViewModel,
  onOpenMaskSettings: () -> Unit,
  onDismiss: () -> Unit
) {
  val strings = LocalAppStrings.current
  val messages by spacesVm.observePrivateMatchMessages(match.id).collectAsStateWithLifecycle(initialValue = emptyList())
  var draft by remember { mutableStateOf("") }
  var mood by remember { mutableStateOf("💌") }
  val listState = rememberLazyListState()

  val realName = partnerProfile?.displayName?.ifBlank { "Secret Match" } ?: "Secret Match"
  val shownName = aliasOverride.trim().ifBlank { realName }
  val isMasked = aliasOverride.isNotBlank() || hidePartnerAvatar

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  fun send() {
    val clean = draft.trim()
    if (clean.isNotEmpty()) {
      spacesVm.sendPrivateMatchMessage(match.id, clean, mood) {
        draft = ""
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Surface(
      color = Color(0xFF0A0D18),
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
      ) {
        // Masked Private Chat Top Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF1F102E), Color(0xFF121827))
              )
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close private chat", tint = Color.White)
          }
          if (hidePartnerAvatar) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
                .border(1.2.dp, Color(0xFF94A3B8).copy(alpha = 0.55f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hidden photo", tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
            }
          } else {
            UserAvatar(name = shownName, photoUrl = partnerProfile?.photoUrl.orEmpty(), size = 40.dp)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = shownName,
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (isMasked) "Private Disguise Active 🛡️" else "${strings.privateChat} · End-to-End Secret",
              color = Color(0xFFF472B6),
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1
            )
          }
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.45f)),
            modifier = Modifier
              .clip(CircleShape)
              .clickable { onOpenMaskSettings() }
              .testTag("private_chat_disguise_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color(0xFFF472B6), modifier = Modifier.size(15.dp))
              Text("Disguise", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Messages Thread
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (messages.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  MutualHeartVaultLockIcon(size = 44.dp, isUnlocked = true, animateGlow = true)
                  Text(
                    text = "Private Secret Chat Unlocked",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Say hello privately. Tap 'Disguise' above anytime to change the nickname or hide their photo on your screen.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          } else {
            items(messages, key = { it.id }) { msg ->
              val isMine = msg.senderId == currentUserId
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
              ) {
                Surface(
                  shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp
                  ),
                  color = if (isMine) Color(0xFFEC4899) else Color(0xFF1E293B),
                  modifier = Modifier.widthIn(max = 285.dp)
                ) {
                  Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (msg.moodEmoji.isNotBlank()) {
                        Text(msg.moodEmoji, style = MaterialTheme.typography.bodyMedium)
                      }
                      Text(msg.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                      text = formatTimestamp(msg.createdAt),
                      color = Color.White.copy(alpha = 0.7f),
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.align(Alignment.End)
                    )
                  }
                }
              }
            }
          }
        }

        // Composer + Mood Bar
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121827))
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("💌", "❤️", "✨", "🌙", "🤫").forEach { m ->
              val selected = mood == m
              Surface(
                shape = CircleShape,
                color = if (selected) Color(0xFFEC4899).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                border = if (selected) BorderStroke(1.dp, Color(0xFFF472B6)) else null,
                modifier = Modifier.clip(CircleShape).clickable { mood = m }
              ) {
                Text(m, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
              }
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = draft,
              onValueChange = { draft = it.take(1000) },
              placeholder = { Text("Write a private secret message...", color = Color.White.copy(alpha = 0.5f)) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = { send() }),
              shape = RoundedCornerShape(24.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFEC4899),
                focusedBorderColor = Color(0xFFEC4899),
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
              ),
              modifier = Modifier.weight(1f).testTag("private_crush_message_input")
            )
            Surface(
              shape = CircleShape,
              color = if (draft.isNotBlank()) Color(0xFFEC4899) else Color.White.copy(alpha = 0.16f),
              modifier = Modifier.size(48.dp)
            ) {
              IconButton(
                onClick = { send() },
                enabled = draft.isNotBlank(),
                modifier = Modifier.testTag("send_private_crush_message_button")
              ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send private message", tint = Color.White)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SecretMatchDisguiseDialog(
  realName: String,
  initialAlias: String,
  initialHideAvatar: Boolean,
  onSave: (alias: String, hideAvatar: Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  var alias by remember(initialAlias) { mutableStateOf(initialAlias) }
  var hideAvatar by remember(initialHideAvatar) { mutableStateOf(initialHideAvatar) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Disguise Identity for Privacy") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Customize how $realName appears on your device inside Secret Love so nobody near your phone can tell who you are chatting with.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = alias,
          onValueChange = { alias = it.take(40) },
          label = { Text("Custom Nickname / Disguise Name") },
          placeholder = { Text("e.g., Study Buddy, Work Contact") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("secret_disguise_alias_input")
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text("Hide Partner Profile Photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Show a neutral lock icon instead of their photo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Switch(
            checked = hideAvatar,
            onCheckedChange = { hideAvatar = it },
            modifier = Modifier.testTag("secret_disguise_hide_photo_switch")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(alias, hideAvatar) },
        modifier = Modifier.testTag("save_secret_disguise_button")
      ) {
        Text("Save Privacy Mask")
      }
    },
    dismissButton = {
      Row {
        if (alias.isNotBlank() || hideAvatar) {
          TextButton(onClick = { onSave("", false) }) { Text("Reset") }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}
