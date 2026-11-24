package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DevotionTrial
import com.example.data.model.MutualCrushMatch
import com.example.data.model.MutualMatchPhase
import com.example.data.model.MutualSecretLoveEngine
import com.example.data.model.PURE_LOVE_TIERS
import com.example.data.model.PublicUser
import com.example.data.model.PureLoveEngine
import com.example.data.model.PureLoveTier
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun SecretCrushMatchOverlay(
  recipientName: String,
  onDismiss: () -> Unit
) {
  val infinite = rememberInfiniteTransition(label = "crush_match_anim")
  val heartScale by infinite.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.18f,
    animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
    label = "heart_beat_scale"
  )
  val burstProgress by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
    label = "firework_burst"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            listOf(Color(0xFF4A0E2E), Color(0xFF1A0714), Color(0xFF07090F))
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f - 60f
        val palette = listOf(
          Color(0xFFFF4D6D),
          Color(0xFFFFB703),
          Color(0xFFEC4899),
          Color(0xFF38BDF8),
          Color(0xFFA855F7)
        )
        val count = 18
        for (i in 0 until count) {
          val angle = (2.0 * PI * i) / count
          val dist = 60f + burstProgress * (size.minDimension * 0.42f)
          val px = cx + (cos(angle) * dist).toFloat()
          val py = cy + (sin(angle) * dist).toFloat()
          val alpha = (1f - burstProgress).coerceIn(0f, 1f)
          drawCircle(
            color = palette[i % palette.size].copy(alpha = alpha),
            radius = 7.dp.toPx() * (1f - burstProgress * 0.45f),
            center = Offset(px, py)
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF191224).copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, Color(0xFFFF4D6D).copy(alpha = 0.65f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
          .testTag("secret_crush_match_overlay")
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(92.dp)
              .scale(heartScale)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(Color(0xFFFF4D6D), Color(0xFFEC4899)))
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = "Mutual crush match heart",
              tint = Color.White,
              modifier = Modifier.size(48.dp)
            )
          }
          Text(
            text = "Mutual Secret Love Unlocked!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
          )
          Text(
            text = "You and $recipientName mutually chose each other in Secret Love, and your countdown has reached zero.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center
          )
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D6D)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dismiss_crush_match_button")
          ) {
            Text("Celebrate Match", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun BadgeUnlockOverlay(
  tier: PureLoveTier,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(tier.badgeEmoji, style = MaterialTheme.typography.displayMedium)
        Text("Pure Love Milestone Unlocked!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
      }
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = "${tier.badgeTitle} · ${tier.label} (${tier.rarityLabel})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
          )
        }
        Text(
          text = if (tier.days == PureLoveEngine.MIN_HINT_UNLOCK_DAYS) {
            "Day 7 reached! Secret Love Hint messaging for this person is now UNLOCKED, and an anonymous milestone alert was sent."
          } else {
            "Your anonymous rank badge has been upgraded and quietly signaled without revealing your identity."
          },
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().testTag("dismiss_badge_unlock_button")
      ) {
        Text("Continue Quest")
      }
    }
  )
}

@Composable
fun PureLoveQuestTab(
  recipients: List<PublicUser>,
  selectedRecipient: PublicUser?,
  onSelectRecipient: (PublicUser) -> Unit,
  trials: List<DevotionTrial>,
  onSetTargetTier: (PublicUser, Int) -> Unit,
  onSubmitDailyCheckIn: (PublicUser, Int, String, () -> Unit) -> Unit,
  onJumpToSendTab: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedTierDays by remember(selectedRecipient?.userId, trials) {
    val existing = selectedRecipient?.let { r -> trials.firstOrNull { it.recipientId == r.userId } }
    mutableIntStateOf(existing?.targetTierDays ?: 7)
  }
  var showDailyTrialDialog by remember { mutableStateOf(false) }

  val currentTrial = remember(selectedRecipient?.userId, trials) {
    selectedRecipient?.let { r -> trials.firstOrNull { it.recipientId == r.userId } }
  }
  val filteredRecipients = remember(recipients, searchQuery) {
    if (searchQuery.isBlank()) recipients else recipients.filter {
      it.displayName.contains(searchQuery, true) || it.handle.contains(searchQuery, true)
    }
  }
  val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
  val isPeakWindow = currentHour in 20..22
  val cooldownMs = remember(currentTrial?.lastCheckInMs) {
    PureLoveEngine.cooldownRemainingMs(currentTrial?.lastCheckInMs ?: 0L)
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Pure Love Hardcore Quest (Top 1%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Text(
            text = "Only senders who pass Day 7 of consistent daily check-ins, 120-char reflections, and memory trials can send Secret Love Hints. Identity stays 100% hidden until you reveal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
    }

    if (selectedRecipient == null) {
      item {
        Text("Select Someone to Dedicate Your Quest", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search member by name or @handle...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
      if (filteredRecipients.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.Lock,
            title = "No eligible members found",
            subtitle = "Other members who enable Secret Love will appear here."
          )
        }
      } else {
        items(filteredRecipients, key = { "quest_${it.userId}" }) { person ->
          val personTrial = trials.firstOrNull { it.recipientId == person.userId }
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectRecipient(person) }
              .testTag("quest_pick_${person.userId}"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                  text = if (personTrial != null) "Day ${personTrial.completedDays}/${personTrial.targetTierDays} · ${personTrial.successRatePct}% Rate"
                  else "${person.handle.ifBlank { "HELLO Member" }} · Not started",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text("Select", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            UserAvatar(name = selectedRecipient.displayName, photoUrl = selectedRecipient.photoUrl, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text("Dedicated to ${selectedRecipient.displayName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(selectedRecipient.handle.ifBlank { "HELLO Member" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (recipients.size > 1) {
              TextButton(onClick = { searchQuery = ""; onSelectRecipient(selectedRecipient) }) {
                Text("Locked")
              }
            }
          }
        }
      }

      if (recipients.size > 1) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            recipients.forEach { person ->
              FilterChip(
                selected = person.userId == selectedRecipient.userId,
                onClick = { onSelectRecipient(person) },
                label = { Text(person.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
              )
            }
          }
        }
      }

      // Duration Tiers Selector
      item {
        Text("Challenge Duration Tier (Up to 1 Year)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PURE_LOVE_TIERS.forEach { tier ->
            FilterChip(
              selected = selectedTierDays == tier.days,
              onClick = {
                selectedTierDays = tier.days
                onSetTargetTier(selectedRecipient, tier.days)
              },
              label = { Text("${tier.badgeEmoji} ${tier.label}") },
              modifier = Modifier.testTag("tier_chip_${tier.days}")
            )
          }
        }
      }

      // Live Progress & Discipline Dashboard
      item {
        val completedDays = currentTrial?.completedDays ?: 0
        val currentStreak = currentTrial?.currentStreak ?: 0
        val missedDays = currentTrial?.missedDaysCount ?: 0
        val successRate = currentTrial?.successRatePct ?: 100
        val progress = (completedDays.toFloat() / selectedTierDays.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val earnedBadge = PureLoveEngine.badgeForDays(completedDays)

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Day $completedDays / $selectedTierDays",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
              )
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = earnedBadge?.let { "${it.badgeEmoji} ${it.badgeTitle}" } ?: "Unranked · ${completedDays}/7 to Unlock",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }

            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              QuestMetricPill("Success Rate", "$successRate%")
              QuestMetricPill("Streak", "$currentStreak d")
              QuestMetricPill("Missed Penalty", "$missedDays d")
            }

            Surface(
              color = if (isPeakWindow) Color(0xFF064E3B).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                  text = if (isPeakWindow) {
                    "Peak Discipline Window Active (8 PM – 11 PM) · Streak multiplier ready"
                  } else {
                    "Daily Discipline Window: 8 PM – 11 PM (20h server cooldown enforced)"
                  },
                  style = MaterialTheme.typography.labelSmall
                )
              }
            }

            // Exact Next Tasks Checklist
            Text("Today's Exact Trial Tasks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            TaskRow("1. Reality Check Memory Quiz + Cipher Pattern", completed = cooldownMs > 0L)
            TaskRow("2. Write 120+ char Pure Journal Reflection", completed = cooldownMs > 0L)
            TaskRow("3. Complete 10s Continuous Focus Hold", completed = cooldownMs > 0L)
            TaskRow(
              "4. Reach Day 7 (${completedDays.coerceAtMost(7)}/7) to unlock Send ❤️ Hints",
              completed = completedDays >= PureLoveEngine.MIN_HINT_UNLOCK_DAYS
            )

            if (cooldownMs > 0L) {
              val hrs = cooldownMs / 3_600_000L
              val mins = (cooldownMs % 3_600_000L) / 60_000L
              OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().testTag("pure_love_cooldown_button")
              ) {
                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Next Trial Unlocks in ${hrs}h ${mins}m")
              }
            } else {
              Button(
                onClick = { showDailyTrialDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("start_pure_love_trial_button")
              ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start Daily Pure Love Trial (Day ${completedDays + 1})")
              }
            }

            if (completedDays >= PureLoveEngine.MIN_HINT_UNLOCK_DAYS) {
              OutlinedButton(
                onClick = onJumpToSendTab,
                modifier = Modifier.fillMaxWidth().testTag("unlocked_open_send_love_button")
              ) {
                Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Day 7 Unlocked · Send Secret Love Hint")
              }
            }
          }
        }
      }

      // Rank Badges Ladder
      item {
        Text("Pure Love Rank Badges Ladder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        val completedDays = currentTrial?.completedDays ?: 0
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          PURE_LOVE_TIERS.forEach { tier ->
            val unlocked = completedDays >= tier.days
            Surface(
              color = if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
                1.dp,
                if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(tier.badgeEmoji, style = MaterialTheme.typography.titleLarge)
                Column(modifier = Modifier.weight(1f)) {
                  Text("${tier.badgeTitle} · ${tier.label}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  Text(tier.rarityLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                  imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                  contentDescription = if (unlocked) "Unlocked" else "Locked",
                  tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  if (showDailyTrialDialog && selectedRecipient != null) {
    HardcorePureLoveTrialDialog(
      recipient = selectedRecipient,
      nextDayNumber = (currentTrial?.completedDays ?: 0) + 1,
      targetTierDays = selectedTierDays,
      onDismiss = { showDailyTrialDialog = false },
      onSubmit = { reflection ->
        onSubmitDailyCheckIn(selectedRecipient, selectedTierDays, reflection) {
          showDailyTrialDialog = false
        }
      }
    )
  }
}

@Composable
private fun QuestMetricPill(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun TaskRow(text: String, completed: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Icon(
      imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
      contentDescription = null,
      tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(15.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun HardcorePureLoveTrialDialog(
  recipient: PublicUser,
  nextDayNumber: Int,
  targetTierDays: Int,
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit
) {
  var stage by remember { mutableIntStateOf(1) }

  // Stage 1: Reality Check + 4-Node Sequence Cipher
  val handleAnswer = recipient.handle.ifBlank { recipient.displayName.ifBlank { "HELLO Member" } }
  val quizOptions = remember(recipient.userId, handleAnswer) {
    listOf(handleAnswer, "@guest_${recipient.userId.take(4)}", "@member_99").distinct().sorted()
  }
  var selectedQuizOption by remember { mutableStateOf("") }
  var cipherStep by remember { mutableIntStateOf(1) }
  var stage1Error by remember { mutableStateOf<String?>(null) }

  // Stage 2: 120-char Pure Journal Reflection
  var reflection by remember { mutableStateOf("") }
  val journalValidation = remember(reflection) { PureLoveEngine.validateJournalReflection(reflection) }

  // Stage 3: 10-second Continuous Focus Hold
  var isHolding by remember { mutableStateOf(false) }
  var holdProgress by remember { mutableFloatStateOf(0f) }
  var focusCompleted by remember { mutableStateOf(false) }

  LaunchedEffect(isHolding, stage) {
    if (stage == 3 && isHolding && !focusCompleted) {
      while (isHolding && holdProgress < 1f) {
        delay(100L)
        holdProgress = (holdProgress + 0.01f).coerceAtMost(1f)
        if (holdProgress >= 1f) {
          focusCompleted = true
          onSubmit(reflection)
          break
        }
      }
    } else if (!isHolding && !focusCompleted && holdProgress > 0f) {
      holdProgress = 0f
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Day $nextDayNumber Trial · Stage $stage/3", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LinearProgressIndicator(
          progress = { stage / 3f },
          modifier = Modifier.fillMaxWidth().clip(CircleShape)
        )

        when (stage) {
          1 -> {
            Text(
              text = "Reality Check: What is ${recipient.displayName}'s exact HELLO handle?",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            quizOptions.forEach { opt ->
              FilterChip(
                selected = selectedQuizOption == opt,
                onClick = {
                  selectedQuizOption = opt
                  stage1Error = null
                },
                label = { Text(opt) },
                modifier = Modifier.fillMaxWidth()
              )
            }
            Text(
              text = "Cipher Lock: Tap numbers 1 → 2 → 3 → 4 in sequence (${cipherStep - 1}/4 done)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(3, 1, 4, 2).forEach { num ->
                val done = num < cipherStep
                OutlinedButton(
                  onClick = {
                    if (num == cipherStep) {
                      cipherStep++
                      stage1Error = null
                    } else if (num >= cipherStep) {
                      cipherStep = 1
                      stage1Error = "Sequence reset — tap 1, 2, 3, 4 in order."
                    }
                  },
                  modifier = Modifier.weight(1f).testTag("cipher_node_$num")
                ) {
                  Text(if (done) "✓" else "$num", fontWeight = FontWeight.Bold)
                }
              }
            }
            stage1Error?.let { err ->
              Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
          }
          2 -> {
            Text(
              text = "Anonymous Pure Journal (${targetTierDays}D Quest)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Write a genuine gratitude note or observation about ${recipient.displayName} (min 120 characters, 12 thoughtful words).",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
              value = reflection,
              onValueChange = { reflection = it.take(1000) },
              label = { Text("Pure Journal Reflection") },
              supportingText = {
                Text(
                  text = journalValidation ?: "✓ Reflection meets Pure Love Integrity (${reflection.trim().length} chars)",
                  color = if (journalValidation == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
              },
              minLines = 4,
              modifier = Modifier.fillMaxWidth().testTag("pure_love_journal_input")
            )
          }
          3 -> {
            Text(
              text = "10-Second Continuous Focus Vow",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Press and hold the Focus Seal continuously for 10 seconds without releasing to verify your patience.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
              progress = { holdProgress },
              modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
            )
            Surface(
              color = if (isHolding) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                  detectTapGestures(
                    onPress = {
                      isHolding = true
                      tryAwaitRelease()
                      isHolding = false
                    }
                  )
                }
                .testTag("focus_hold_pad")
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  Icons.Rounded.TouchApp,
                  contentDescription = "Press and hold for 10 seconds",
                  tint = if (isHolding) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = when {
                    focusCompleted -> "Vow Sealed!"
                    isHolding -> "Holding Focus... ${(holdProgress * 10).toInt()}s / 10s"
                    else -> "Press & Hold for 10 Seconds"
                  },
                  color = if (isHolding) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      when (stage) {
        1 -> Button(
          onClick = {
            if (selectedQuizOption != handleAnswer) {
              stage1Error = "Check ${recipient.displayName}'s exact profile handle."
            } else if (cipherStep <= 4) {
              stage1Error = "Finish tapping cipher nodes 1 → 2 → 3 → 4."
            } else {
              stage = 2
            }
          },
          modifier = Modifier.testTag("trial_stage1_next")
        ) { Text("Next: Journal") }
        2 -> Button(
          onClick = { if (journalValidation == null) stage = 3 },
          enabled = journalValidation == null,
          modifier = Modifier.testTag("trial_stage2_next")
        ) { Text("Next: Focus Vow") }
        3 -> TextButton(onClick = onDismiss) { Text("Close") }
      }
    },
    dismissButton = {
      if (stage < 3) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}

@Composable
fun MutualMatchVaultCard(
  match: MutualCrushMatch,
  currentUserId: String,
  nowMs: Long,
  partnerProfile: PublicUser?,
  onCelebrate: () -> Unit,
  displayNameOverride: String = "",
  hidePartnerAvatar: Boolean = false,
  onOpenPrivateChat: (() -> Unit)? = null,
  onConfigureMask: (() -> Unit)? = null
) {
  val phase = MutualSecretLoveEngine.phaseOf(match, nowMs)
  when (phase) {
    MutualMatchPhase.SEALED_HIDDEN, MutualMatchPhase.COUNTDOWN_24H -> {
      // Complete stealth before countdown ends: render nothing until officially UNLOCKED.
      return
    }

    MutualMatchPhase.UNLOCKED -> {
      val realName = partnerProfile?.displayName?.ifBlank { "Secret Match" } ?: "Secret Match"
      val shownName = displayNameOverride.trim().ifBlank { realName }
      val isMasked = displayNameOverride.isNotBlank() || hidePartnerAvatar
      val realHandle = partnerProfile?.handle?.trimStart('@')?.ifBlank { "member" } ?: "member"

      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF171124),
        border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFFFF4D6D), Color(0xFF8B5CF6)))),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("mutual_match_unlocked_${match.id}")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (hidePartnerAvatar) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
                  .border(1.5.dp, Color(0xFF94A3B8).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hidden avatar", tint = Color(0xFFCBD5E1), modifier = Modifier.size(22.dp))
              }
            } else {
              UserAvatar(name = shownName, photoUrl = partnerProfile?.photoUrl.orEmpty(), size = 48.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isMasked) "Private Disguise Active 🛡️" else "Mutual Secret Love Unlocked 💖",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF472B6)
              )
              Text(
                text = shownName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = if (isMasked) "Disguised on this device for privacy" else "@$realHandle · Private chat ready",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onOpenPrivateChat != null) {
              Button(
                onClick = onOpenPrivateChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                modifier = Modifier
                  .weight(1f)
                  .testTag("open_private_crush_chat_${match.id}")
              ) {
                Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Private Chat", color = Color.White, fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = onCelebrate,
                modifier = Modifier
                  .weight(1f)
                  .testTag("celebrate_mutual_match_${match.id}")
              ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Celebrate")
              }
            }
            if (onConfigureMask != null) {
              OutlinedButton(
                onClick = onConfigureMask,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                modifier = Modifier.testTag("configure_mask_${match.id}")
              ) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = "Disguise", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Disguise", color = Color.White)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Unique, modern crystalline heart-keyhole vault padlock for Secret Love Mutual matches.
 */
@Composable
fun MutualHeartVaultLockIcon(
  size: Dp = 44.dp,
  isUnlocked: Boolean = false,
  animateGlow: Boolean = false,
  modifier: Modifier = Modifier
) {
  val glowAlpha = if (animateGlow) {
    val transition = rememberInfiniteTransition(label = "mutual_lock_glow")
    val pulse by transition.animateFloat(
      initialValue = 0.22f,
      targetValue = 0.52f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1700, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "mutual_lock_pulse"
    )
    pulse
  } else {
    0.28f
  }

  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    // Outer ambient aura
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFF4D6D).copy(alpha = glowAlpha), Color(0xFF7C3AED).copy(alpha = 0.04f), Color.Transparent),
        center = Offset(w * 0.5f, h * 0.58f),
        radius = w * 0.56f
      ),
      radius = w * 0.56f,
      center = Offset(w * 0.5f, h * 0.58f)
    )

    // Precision metallic gradient shackle
    val shackleStroke = (w * 0.115f).coerceAtLeast(1.8f)
    val shackleLeft = w * 0.27f
    val shackleWidth = w * 0.46f
    val shackleTop = h * 0.11f
    val shackleHeight = h * 0.42f
    val shackleBrush = Brush.linearGradient(
      colors = listOf(Color(0xFFFDA4AF), Color(0xFFC084FC), Color(0xFF818CF8)),
      start = Offset(shackleLeft, shackleTop),
      end = Offset(shackleLeft + shackleWidth, shackleTop + shackleHeight)
    )
    drawArc(
      brush = shackleBrush,
      startAngle = if (isUnlocked) 195f else 180f,
      sweepAngle = if (isUnlocked) 155f else 180f,
      useCenter = false,
      topLeft = Offset(shackleLeft, shackleTop),
      size = Size(shackleWidth, shackleHeight),
      style = Stroke(width = shackleStroke, cap = StrokeCap.Round)
    )
    if (!isUnlocked) {
      drawLine(
        brush = shackleBrush,
        start = Offset(shackleLeft, h * 0.32f),
        end = Offset(shackleLeft, h * 0.44f),
        strokeWidth = shackleStroke,
        cap = StrokeCap.Round
      )
      drawLine(
        brush = shackleBrush,
        start = Offset(shackleLeft + shackleWidth, h * 0.32f),
        end = Offset(shackleLeft + shackleWidth, h * 0.44f),
        strokeWidth = shackleStroke,
        cap = StrokeCap.Round
      )
    } else {
      drawLine(
        brush = shackleBrush,
        start = Offset(shackleLeft, h * 0.31f),
        end = Offset(shackleLeft, h * 0.44f),
        strokeWidth = shackleStroke,
        cap = StrokeCap.Round
      )
    }

    // Modern rounded shield padlock body
    val bodyLeft = w * 0.16f
    val bodyTop = h * 0.40f
    val bodyW = w * 0.68f
    val bodyH = h * 0.48f
    val bodyCorner = CornerRadius(w * 0.22f, w * 0.22f)

    drawRoundRect(
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFF3366), Color(0xFFD946EF), Color(0xFF6366F1)),
        start = Offset(bodyLeft, bodyTop),
        end = Offset(bodyLeft + bodyW, bodyTop + bodyH)
      ),
      topLeft = Offset(bodyLeft, bodyTop),
      size = Size(bodyW, bodyH),
      cornerRadius = bodyCorner
    )

    // Top crystal gloss highlight
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
        startY = bodyTop,
        endY = bodyTop + bodyH * 0.48f
      ),
      topLeft = Offset(bodyLeft + w * 0.03f, bodyTop + h * 0.02f),
      size = Size(bodyW - w * 0.06f, bodyH * 0.44f),
      cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
    )

    // Crisp Heart Keyhole in the center
    val cx = w * 0.5f
    val cy = bodyTop + bodyH * 0.50f
    val hs = w * 0.15f
    val heartPath = Path().apply {
      moveTo(cx, cy + hs * 0.95f)
      cubicTo(
        cx - hs * 1.45f, cy + hs * 0.15f,
        cx - hs * 1.25f, cy - hs * 1.05f,
        cx, cy - hs * 0.35f
      )
      cubicTo(
        cx + hs * 1.25f, cy - hs * 1.05f,
        cx + hs * 1.45f, cy + hs * 0.15f,
        cx, cy + hs * 0.95f
      )
      close()
    }
    drawPath(path = heartPath, color = Color.White.copy(alpha = 0.96f))

    // Keyhole slot stem + subtle specular sparkle dot
    drawLine(
      color = Color.White.copy(alpha = 0.92f),
      start = Offset(cx, cy + hs * 0.72f),
      end = Offset(cx, cy + hs * 1.32f),
      strokeWidth = (w * 0.055f).coerceAtLeast(1.2f),
      cap = StrokeCap.Round
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.85f),
      radius = (w * 0.04f).coerceAtLeast(0.8f),
      center = Offset(bodyLeft + bodyW * 0.82f, bodyTop + bodyH * 0.24f)
    )
  }
}

