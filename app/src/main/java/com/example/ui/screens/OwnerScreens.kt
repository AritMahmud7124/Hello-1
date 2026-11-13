package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.UserAvatar
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun OwnerProfileScreen(
  spacesVm: SpacesGovernanceViewModel,
  isVerifiedOwner: Boolean,
  onOpenDashboard: () -> Unit
) {
  val owner by spacesVm.ownerConfig.collectAsStateWithLifecycle()

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
              Icon(Icons.Filled.Verified, contentDescription = "Verified Owner Badge", tint = MaterialTheme.colorScheme.primary)
            }
            Text(owner?.ownerHandle ?: "@founder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Owner ID: ${owner?.ownerUid?.ifBlank { "Configured via verified owner bootstrap" } ?: "Not initialized"}", style = MaterialTheme.typography.labelSmall)
          }
        }
        Text(
          text = owner?.about ?: "Founder & Verified Owner of HELLO Social Network.",
          style = MaterialTheme.typography.bodyLarge
        )
        Text(
          text = "Contact: ${owner?.contactEmail?.ifBlank { "helloarit123@gmail.com" } ?: "helloarit123@gmail.com"}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = owner?.contactInfo ?: "Available for platform governance & safety.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isVerifiedOwner) {
          Button(
            onClick = onOpenDashboard,
            modifier = Modifier.fillMaxWidth().testTag("open_owner_dashboard_button")
          ) {
            Icon(Icons.Filled.AdminPanelSettings, null)
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
  isVerifiedOwner: Boolean
) {
  if (!isVerifiedOwner) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
      EmptyStateCard(
        icon = Icons.Filled.Lock,
        title = "Owner Authorization Required",
        subtitle = "Only the verified platform OWNER_UID enforced by Firestore Security Rules can manage global settings, owner profile, and admin accounts."
      )
    }
    return
  }

  val owner by spacesVm.ownerConfig.collectAsStateWithLifecycle()
  val globals by spacesVm.globalSettings.collectAsStateWithLifecycle()
  val admins by spacesVm.admins.collectAsStateWithLifecycle()
  val reports by spacesVm.reports.collectAsStateWithLifecycle()
  val users = ((communityVm.discoverableUsers.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val posts = ((socialVm.publicFeed.collectAsStateWithLifecycle().value as? UiState.Success)?.data).orEmpty()
  val groups by spacesVm.publicGroups.collectAsStateWithLifecycle()

  var tab by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Owner Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

    TabRow(selectedTabIndex = tab) {
      Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Stats & App") })
      Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Owner Info") })
      Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Admins") })
      Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Reports (${reports.count { it.status == "open" }})") })
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
        var about by remember(owner?.about) { mutableStateOf(owner?.about ?: "") }
        var email by remember(owner?.contactEmail) { mutableStateOf(owner?.contactEmail ?: "") }
        var info by remember(owner?.contactInfo) { mutableStateOf(owner?.contactInfo ?: "") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("Owner Handle") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("About") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = info, onValueChange = { info = it }, label = { Text("Contact Info") }, modifier = Modifier.fillMaxWidth())
            Button(
              onClick = { spacesVm.updateOwnerProfile(name, handle, owner?.ownerAvatar ?: "", about, email, info) },
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Update Owner Profile") }
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
          EmptyStateCard(Icons.Filled.AdminPanelSettings, "No reports", "Community safety reports appear here for Owner & Admin moderation.")
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
                        Icon(Icons.Filled.Delete, null)
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
