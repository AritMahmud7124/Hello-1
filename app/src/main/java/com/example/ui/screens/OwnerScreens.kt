package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MutualMatchPhase
import com.example.data.model.MutualSecretLoveEngine
import com.example.data.model.UiState
import com.example.security.DeviceIntegrityChecker
import com.example.security.DeviceIntegrityReport
import com.example.security.EncryptedVaultStorage
import com.example.security.findFragmentActivity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun OwnerProfileScreen(
  spacesVm: SpacesGovernanceViewModel,
  isVerifiedOwner: Boolean,
  socialVm: MainSocialViewModel? = null,
  vaultStorage: EncryptedVaultStorage? = null,
  integrityReport: DeviceIntegrityReport? = null,
  onOpenDashboard: () -> Unit
) {
  val context = LocalContext.current
  val owner by spacesVm.ownerConfig.collectAsStateWithLifecycle()
  val handleVisible = owner?.showOwnerHandle != false

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          UserAvatar(
            name = owner?.ownerName ?: "HELLO Founder",
            photoUrl = owner?.ownerAvatar ?: "",
            size = 68.dp,
            isOnline = true,
            isVerifiedOwner = true
          )
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(owner?.ownerName ?: "HELLO Founder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
              Icon(Icons.Rounded.Verified, contentDescription = "Verified Owner Badge", tint = MaterialTheme.colorScheme.primary)
            }
            if (handleVisible) {
              Text(owner?.ownerHandle ?: "@founder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (isVerifiedOwner) {
              Text(
                text = "${owner?.ownerHandle ?: "@founder"} · Hidden from users",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(14.dp))
              Text(
                text = "Verified Platform Founder · Protected Account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
        Text(
          text = owner?.about ?: "Founder & Verified Owner of HELLO Social Network.",
          style = MaterialTheme.typography.bodyLarge
        )
        Text(
          text = owner?.contactInfo?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: "Official governance & community support via verified in-app channels only.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val publicEmail = owner?.contactEmail?.trim().orEmpty()
        if (owner?.showContactEmail == true && publicEmail.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                runCatching {
                  context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$publicEmail")))
                }
              }
              .testTag("owner_public_email_card")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Rounded.Email, contentDescription = "Contact Gmail", tint = MaterialTheme.colorScheme.primary)
              Column(modifier = Modifier.weight(1f)) {
                Text("Official Contact Gmail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(publicEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
        if (isVerifiedOwner) {
          Button(
            onClick = onOpenDashboard,
            modifier = Modifier.fillMaxWidth().testTag("open_owner_dashboard_button")
          ) {
            Icon(Icons.Rounded.AdminPanelSettings, null)
            Spacer(Modifier.width(6.dp))
            Text("Open Owner Dashboard")
          }
        }
      }
    }
  }
}

@Composable
fun OwnerDashboardScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  isVerifiedOwner: Boolean,
  vaultStorage: EncryptedVaultStorage? = null,
  integrityReport: DeviceIntegrityReport? = null
) {
  if (!isVerifiedOwner) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
      EmptyStateCard(
        icon = Icons.Rounded.Lock,
        title = "Owner Authorization Required",
        subtitle = "Only the verified platform OWNER_UID enforced by Firestore Security Rules can manage global settings, owner profile, and admin accounts."
      )
    }
    return
  }

  val context = LocalContext.current
  val vault = remember(context, vaultStorage) { vaultStorage ?: EncryptedVaultStorage(context) }
  val report = remember(context, integrityReport) { integrityReport ?: DeviceIntegrityChecker.assess(context, vault.isStrictTamperModeEnabled) }
  var hasFreshSession by remember(vault) { mutableStateOf(vault.hasFreshSession()) }

  val strings = LocalAppStrings.current
  val owner by spacesVm.ownerConfig.collectAsStateWithLifecycle()
  val globals by spacesVm.globalSettings.collectAsStateWithLifecycle()
  val admins by spacesVm.admins.collectAsStateWithLifecycle()
  val reports by spacesVm.reports.collectAsStateWithLifecycle()
  val users = ((communityVm.discoverableUsers.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val posts = ((socialVm.publicFeed.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val groups by spacesVm.publicGroups.collectAsStateWithLifecycle()
  val regionalAnalytics by spacesVm.regionalAnalytics.collectAsStateWithLifecycle()
  val ownerCrushSignals by spacesVm.ownerCrushSignals.collectAsStateWithLifecycle()
  val ownerMutualMatches by spacesVm.ownerMutualMatches.collectAsStateWithLifecycle()
  val ownerPrivateUsers by spacesVm.ownerPrivateUsers.collectAsStateWithLifecycle()
  val liveClockMs by spacesVm.liveClockMs.collectAsStateWithLifecycle()

  var tab by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(strings.ownerDashboard, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

    if (!hasFreshSession) {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = "Governance rules require fresh sign-in (<15m) before modifying owner or admin settings.",
              style = MaterialTheme.typography.bodySmall
            )
          }
          Button(onClick = {
            context.findFragmentActivity()?.let { act ->
              socialVm.reauthenticateSession(act, vault, report) {
                hasFreshSession = vault.hasFreshSession()
              }
            }
          }) {
            Text("Verify")
          }
        }
      }
    }

    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
      Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Stats & App") })
      Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Owner Info") })
      Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Admins") })
      Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Reports (${reports.count { it.status == "open" }})") })
      Tab(
        selected = tab == 4,
        onClick = { tab = 4 },
        text = { Text(strings.ownerAuditTab) },
        modifier = Modifier.testTag("owner_audit_tab")
      )
    }

    when (tab) {
      0 -> {
        var appName by remember(globals.appName) { mutableStateOf(globals.appName) }
        var tagline by remember(globals.tagline) { mutableStateOf(globals.tagline) }
        var banner by remember(globals.maintenanceBanner) { mutableStateOf(globals.maintenanceBanner) }
        var sosOn by remember(globals.sosFeatureEnabled) { mutableStateOf(globals.sosFeatureEnabled) }
        var loveOn by remember(globals.secretLoveEnabled) { mutableStateOf(globals.secretLoveEnabled) }
        var regOn by remember(globals.allowNewRegistrations) { mutableStateOf(globals.allowNewRegistrations) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
          item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("Users", "${users.size}")
                StatCell("Posts", "${posts.size}")
                StatCell("Groups", "${groups.size}")
                StatCell("Reports", "${reports.size}")
              }
            }
          }
          item {
            val generalCount = regionalAnalytics.count { it.feedMode == "general" }
            val countryCount = regionalAnalytics.count { it.feedMode == "country" }
            val totalSwitches = regionalAnalytics.sumOf { it.switchCount }
            val topRegions = regionalAnalytics
              .groupingBy { it.selectedRegion.ifBlank { "GLOBAL" } }
              .eachCount()
              .entries
              .sortedByDescending { it.value }
              .take(4)
            val originCountries = regionalAnalytics
              .groupingBy { it.originRegion.ifBlank { "US" } }
              .eachCount()
              .entries
              .sortedByDescending { it.value }
              .take(4)

            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth().testTag("owner_regional_analytics_card")
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Text("Global Reach & Regional Feed Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (regionalAnalytics.isEmpty()) {
                  Text(
                    text = "No regional feed telemetry recorded yet. Counts sync automatically as members switch between General (Global) and Country feeds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                } else {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCell("Active Users", "${regionalAnalytics.size}")
                    StatCell("General", "$generalCount")
                    StatCell("Country", "$countryCount")
                    StatCell("Switches", "$totalSwitches")
                  }
                  if (topRegions.isNotEmpty()) {
                    Text(
                      text = "Watched Feeds: ${topRegions.joinToString(" · ") { "${it.key} (${it.value})" }}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  if (originCountries.isNotEmpty()) {
                    Text(
                      text = "Origin Locations: ${originCountries.joinToString(" · ") { "${it.key} (${it.value})" }}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  regionalAnalytics.take(4).forEach { entry ->
                    Text(
                      text = "• ${entry.originRegion} → ${entry.watchMode} (${entry.selectedRegion}, ${entry.switchCount} switches)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
          item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Global App Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App display name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = banner, onValueChange = { banner = it }, label = { Text("Global announcement banner") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("Emergency SOS Feature")
                  Switch(checked = sosOn, onCheckedChange = { sosOn = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("Secret Love Feature")
                  Switch(checked = loveOn, onCheckedChange = { loveOn = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("Allow New Registrations")
                  Switch(checked = regOn, onCheckedChange = { regOn = it })
                }
                Button(
                  onClick = { spacesVm.updateGlobalSettings(appName, tagline, banner, sosOn, loveOn, regOn) },
                  modifier = Modifier.fillMaxWidth().testTag("save_global_settings_button")
                ) { Text("Save Global Settings") }
              }
            }
          }
        }
      }
      1 -> {
        var name by remember(owner?.ownerName) { mutableStateOf(owner?.ownerName ?: "HELLO Founder") }
        var handle by remember(owner?.ownerHandle) { mutableStateOf(owner?.ownerHandle ?: "@founder") }
        var showOwnerHandle by remember(owner?.showOwnerHandle) { mutableStateOf(owner?.showOwnerHandle != false) }
        var about by remember(owner?.about) { mutableStateOf(owner?.about ?: "") }
        var email by remember(owner?.contactEmail) { mutableStateOf(owner?.contactEmail ?: "") }
        var info by remember(owner?.contactInfo) { mutableStateOf(owner?.contactInfo ?: "") }
        var showContactEmail by remember(owner?.showContactEmail) { mutableStateOf(owner?.showContactEmail == true) }

        fun applyHandleVisibility(next: Boolean) {
          showOwnerHandle = next
          val runToggle = { spacesVm.toggleOwnerHandleVisibility(next) { hasFreshSession = false } }
          if (hasFreshSession) {
            runToggle()
          } else {
            val act = context.findFragmentActivity()
            if (act != null) {
              socialVm.reauthenticateSession(act, vault, report) {
                hasFreshSession = vault.hasFreshSession()
                runToggle()
              }
            } else {
              runToggle()
            }
          }
        }

        fun applyEmailVisibility(next: Boolean) {
          showContactEmail = next
          val runToggle = { spacesVm.toggleOwnerEmailVisibility(next) { hasFreshSession = false } }
          if (hasFreshSession) {
            runToggle()
          } else {
            val act = context.findFragmentActivity()
            if (act != null) {
              socialVm.reauthenticateSession(act, vault, report) {
                hasFreshSession = vault.hasFreshSession()
                runToggle()
              }
            } else {
              runToggle()
            }
          }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
              OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("Owner Handle") }, modifier = Modifier.fillMaxWidth())

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (showOwnerHandle) Color(0xFF22C55E).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { applyHandleVisibility(!showOwnerHandle) }
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Icon(
                      imageVector = if (showOwnerHandle) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                      contentDescription = null,
                      tint = if (showOwnerHandle) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Public Owner @Handle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                          text = if (showOwnerHandle) "PUBLIC" else "HIDDEN",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = if (showOwnerHandle) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Text(
                        text = if (showOwnerHandle) {
                          "Everyone can see $handle across the app."
                        } else {
                          "Hidden — $handle is removed from every screen without any notice to users."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Switch(
                    checked = showOwnerHandle,
                    onCheckedChange = { applyHandleVisibility(it) },
                    modifier = Modifier.testTag("owner_dashboard_handle_visibility_switch")
                  )
                }
              }

              OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("About") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
              OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Owner Contact Gmail") },
                placeholder = { Text("helloarit123@gmail.com") },
                modifier = Modifier.fillMaxWidth()
              )

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (showContactEmail) Color(0xFF22C55E).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { applyEmailVisibility(!showContactEmail) }
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Icon(
                      imageVector = if (showContactEmail) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                      contentDescription = null,
                      tint = if (showContactEmail) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Public Contact Gmail", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                          text = if (showContactEmail) "PUBLIC" else "HIDDEN",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = if (showContactEmail) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Text(
                        text = if (showContactEmail) {
                          "Everyone viewing Owner Profile can see & tap your Contact Gmail."
                        } else {
                          "Hidden — Contact Gmail is not shown anywhere to users."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Switch(
                    checked = showContactEmail,
                    onCheckedChange = { applyEmailVisibility(it) },
                    modifier = Modifier.testTag("owner_email_visibility_switch")
                  )
                }
              }

              OutlinedTextField(value = info, onValueChange = { info = it }, label = { Text("Contact Info") }, modifier = Modifier.fillMaxWidth())
              Button(
                onClick = {
                  spacesVm.updateOwnerProfile(name, handle, owner?.ownerAvatar ?: "", about, email, info, showContactEmail, showOwnerHandle)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("update_owner_profile_button")
              ) { Text("Update Owner Profile") }
            }
          }
        }
      }
      2 -> {
        val adminIds = admins.map { it.uid }.toSet()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item { Text("Current Admins (${admins.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
          items(admins, key = { it.uid }) { adm ->
            Card {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(adm.displayName, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { spacesVm.removeAdmin(adm.uid) }) { Text("Revoke") }
              }
            }
          }
          item { Text("Promote Member to Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
          items(users.filter { it.userId !in adminIds && !it.isVerifiedOwner }, key = { it.userId }) { u ->
            Card {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${u.displayName} (${u.handle})")
                Button(onClick = { spacesVm.addAdmin(u) }) { Text("Make Admin") }
              }
            }
          }
        }
      }
      3 -> {
        if (reports.isEmpty()) {
          EmptyStateCard(Icons.Rounded.AdminPanelSettings, "No reports", "Community safety reports appear here for Owner & Admin moderation.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reports, key = { it.id }) { r ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("${r.targetType.uppercase()} · ${r.status.uppercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  Text(r.reason, style = MaterialTheme.typography.bodyMedium)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { spacesVm.resolveReport(r.id, "resolved") }) { Text("Resolve") }
                    TextButton(onClick = { spacesVm.resolveReport(r.id, "dismissed") }) { Text("Dismiss") }
                    if (r.targetType == "post") {
                      TextButton(onClick = { spacesVm.deleteReportedPost(r.targetId) }) {
                        Icon(Icons.Rounded.DeleteOutline, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Post")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      4 -> {
        var auditQuery by remember { mutableStateOf("") }
        val userByUid = remember(users) { users.associateBy { it.userId } }
        val privateByUid = remember(ownerPrivateUsers) { ownerPrivateUsers.associateBy { it.userId.ifBlank { it.id } } }
        fun labelFor(uid: String): String {
          val u = userByUid[uid]
          return if (u != null) {
            val h = u.handle.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            "${u.displayName.ifBlank { uid }}$h"
          } else {
            uid
          }
        }
        val q = auditQuery.trim()
        val sortedUsers = remember(users, ownerPrivateUsers, q) {
          val allUids = (users.map { it.userId } + ownerPrivateUsers.map { it.userId.ifBlank { it.id } }).filter { it.isNotBlank() }.distinct()
          allUids.sortedBy { uid -> userByUid[uid]?.displayName?.lowercase() ?: uid.lowercase() }
            .filter { uid ->
              if (q.isBlank()) true else {
                val pub = userByUid[uid]
                val priv = privateByUid[uid]
                uid.contains(q, true) ||
                  (pub?.displayName?.contains(q, true) == true) ||
                  (pub?.handle?.contains(q, true) == true) ||
                  (priv?.email?.contains(q, true) == true) ||
                  (priv?.phone?.contains(q, true) == true)
              }
            }
        }
        val filteredSignals = remember(ownerCrushSignals, userByUid, q) {
          if (q.isBlank()) ownerCrushSignals else ownerCrushSignals.filter { sig ->
            sig.fromUid.contains(q, true) || sig.toUid.contains(q, true) ||
              labelFor(sig.fromUid).contains(q, true) || labelFor(sig.toUid).contains(q, true)
          }
        }
        val filteredMatches = remember(ownerMutualMatches, userByUid, q) {
          if (q.isBlank()) ownerMutualMatches else ownerMutualMatches.filter { m ->
            m.participantUids.any { uid -> uid.contains(q, true) || labelFor(uid).contains(q, true) }
          }
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
          modifier = Modifier.testTag("owner_audit_list")
        ) {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Read-Only Platform Audit (A–Z)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Inspect who selected whom in Secret Love, active Mutual Matches, and A–Z account status. Owner cannot modify or delete any user's Secret Love selection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
              }
            }
          }

          item {
            OutlinedTextField(
              value = auditQuery,
              onValueChange = { auditQuery = it },
              placeholder = { Text("Search user name, @handle, email, or UID...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(22.dp),
              modifier = Modifier.fillMaxWidth().testTag("owner_audit_search_input")
            )
          }

          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
              StatCell("Accounts", "${sortedUsers.size}")
              StatCell("Crush Picks", "${filteredSignals.size}")
              StatCell("Mutual", "${filteredMatches.size}")
            }
          }

          item {
            Text(
              text = "Mutual Love Matches (${filteredMatches.size}) · Read-Only",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          if (filteredMatches.isEmpty()) {
            item {
              Text(
                text = "No mutual matches recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            items(filteredMatches, key = { "audit_match_${it.id}" }) { m ->
              val pA = m.participantUids.getOrNull(0).orEmpty()
              val pB = m.participantUids.getOrNull(1).orEmpty()
              val phase = MutualSecretLoveEngine.phaseOf(m, liveClockMs)
              val phaseLabel = when (phase) {
                MutualMatchPhase.SEALED_HIDDEN -> "SEALED (Hidden to users)"
                MutualMatchPhase.COUNTDOWN_24H -> "24H COUNTDOWN (${MutualSecretLoveEngine.formatCountdown24h(MutualSecretLoveEngine.remainingMsUntilReveal(m, liveClockMs))})"
                MutualMatchPhase.UNLOCKED -> "UNLOCKED"
              }
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                    Text("${labelFor(pA)}  ↔  ${labelFor(pB)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  }
                  Text("Phase: $phaseLabel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                  Text(
                    text = "Matched at ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US).format(java.util.Date(m.matchedAtMs.coerceAtLeast(0L)))} · Reveal at ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US).format(java.util.Date(m.revealAtMs.coerceAtLeast(0L)))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          item {
            Spacer(Modifier.height(4.dp))
            Text(
              text = "All Secret Love Selections (${filteredSignals.size}) · Read-Only",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          if (filteredSignals.isEmpty()) {
            item {
              Text(
                text = "No Secret Love crush selections recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            items(filteredSignals, key = { "audit_sig_${it.id}" }) { sig ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "${labelFor(sig.fromUid)}  →  ${labelFor(sig.toUid)}",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = "Selected on ${formatTimestamp(sig.createdAt)} · Read-only audit",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          item {
            Spacer(Modifier.height(4.dp))
            Text(
              text = "All User Accounts A–Z (${sortedUsers.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          items(sortedUsers, key = { "audit_user_$it" }) { uid ->
            val pub = userByUid[uid]
            val priv = privateByUid[uid]
            val displayName = pub?.displayName?.ifBlank { uid } ?: uid
            val handle = pub?.handle?.ifBlank { "@member" } ?: "@member"
            val email = priv?.email?.ifBlank { "No recovery email" } ?: "Not loaded"
            val phone = priv?.phone?.ifBlank { "No recovery phone" } ?: "—"
            val statusBadge = when {
              priv?.isDeactivated == true -> "DEACTIVATED"
              pub?.effectiveOnline == true -> "ONLINE"
              else -> "ACTIVE (Offline)"
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    UserAvatar(name = displayName, photoUrl = pub?.photoUrl.orEmpty(), size = 34.dp, isOnline = pub?.effectiveOnline == true)
                    Column {
                      Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                      Text("$handle · Role: ${pub?.role ?: "user"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                  Surface(
                    shape = CircleShape,
                    color = if (priv?.isDeactivated == true) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                  ) {
                    Text(
                      text = statusBadge,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (priv?.isDeactivated == true) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
                Text("UID: $uid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Recovery Gmail: $email · Phone: $phone", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = "Privacy: ${pub?.profilePrivacy ?: "public"} · Green dot: ${if (pub?.showOnlineStatus != false) "ON" else "OFF"} · Secret Love: ${if (pub?.acceptSecretLove != false) "Enabled" else "Off"} · Friends: ${pub?.friendsCount ?: 0}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatCell(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
