package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.EventEntity
import com.example.data.model.MarketplaceListing
import com.example.data.model.PageEntity
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel

@Composable
fun PagesScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  currentUserId: String,
  friendUids: List<String>,
  communityVm: CommunityViewModel? = null,
  me: PublicUser? = null
) {
  val context = LocalContext.current
  val pagesState by spacesVm.pages.collectAsStateWithLifecycle()
  val feedState by socialVm.publicFeed.collectAsStateWithLifecycle()
  val allPublicPosts = (feedState as? UiState.Success)?.data.orEmpty()
  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()
  var showCreate by remember { mutableStateOf(false) }
  var postPageId by remember { mutableStateOf<PageEntity?>(null) }
  var sharePageTarget by remember { mutableStateOf<PageEntity?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Pages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Creators, communities & brands on HELLO", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreate = true }, modifier = Modifier.testTag("create_page_button")) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Create")
      }
    }

    when (val state = pagesState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.Flag, "Pages unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(Icons.Filled.Flag, "No Pages yet", "Launch a creator or brand Page.", actionLabel = "Create Page", onAction = { showCreate = true })
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.data, key = { it.id }) { page ->
              val isFollowing = currentUserId in page.followerUids
              val isAdmin = currentUserId in page.adminUids
              val pagePosts = allPublicPosts.filter { it.pageId == page.id }
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(page.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                      Text("${page.category} · ${page.followersCount} followers" + if (isAdmin) " · Admin" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { sharePageTarget = page }) {
                      Icon(Icons.Filled.Share, contentDescription = "Share page")
                    }
                    OutlinedButton(onClick = { spacesVm.toggleFollowPage(page) }) {
                      Text(if (isFollowing) "Following" else "Follow")
                    }
                  }
                  if (page.bio.isNotBlank()) Text(page.bio, style = MaterialTheme.typography.bodyMedium)
                  if (isAdmin) {
                    TextButton(onClick = { postPageId = page }, contentPadding = PaddingValues(0.dp)) {
                      Text("Publish Page Post")
                    }
                  }
                  pagePosts.take(2).forEach { p ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                      Text(p.content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
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

  if (showCreate) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Digital Creator") }
    var bio by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCreate = false },
      title = { Text("Create Page") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Page name") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("About / Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
      },
      confirmButton = {
        Button(onClick = {
          spacesVm.createPage(name, category, bio)
          showCreate = false
        }) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
    )
  }

  postPageId?.let { page ->
    CreatePostDialog(
      onDismiss = { postPageId = null },
      onPublish = { content, mediaUri, mediaType, _ ->
        socialVm.createPost(context, content, mediaUri, mediaType, "public", friendUids, pageId = page.id)
        postPageId = null
      }
    )
  }

  sharePageTarget?.let { page ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "${page.name} (${page.category}) on HELLO",
        body = page.bio.ifBlank { "${page.followersCount} followers" },
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { sharePageTarget = null },
      onRepostToFeed = { note ->
        val composed = if (note.isBlank()) "Check out ${page.name} (${page.category}): ${page.bio}" else "$note\n\n— Page: ${page.name}"
        socialVm.createPost(context, composed, "", "none", "public", friendUids)
      },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToConversation(conv, sender, msg)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm?.shareTextToPerson(sender, person, msg)
      }
    )
  }
}

@Composable
fun EventsScreen(
  spacesVm: SpacesGovernanceViewModel,
  communityVm: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String
) {
  val eventsState by spacesVm.events.collectAsStateWithLifecycle()
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val allUsers = discoverableUsers.filter { it.userId != currentUserId }
  var showCreate by remember { mutableStateOf(false) }
  var inviteEvent by remember { mutableStateOf<EventEntity?>(null) }
  var shareEventTarget by remember { mutableStateOf<EventEntity?>(null) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Events", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Discover meetups & invite friends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreate = true }, modifier = Modifier.testTag("create_event_button")) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Create")
      }
    }

    when (val state = eventsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.Event, "Events unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(Icons.Filled.Event, "No upcoming events", "Host a local meetup or online stream.", actionLabel = "Create Event", onAction = { showCreate = true })
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.data, key = { it.id }) { ev ->
              val isGoing = currentUserId in ev.goingUids
              val isInterested = currentUserId in ev.interestedUids
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text("${ev.eventDateText} at ${ev.eventTimeText}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ev.locationName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  if (ev.description.isNotBlank()) Text(ev.description, style = MaterialTheme.typography.bodyMedium)
                  Text("${ev.goingUids.size} Going · ${ev.interestedUids.size} Interested", style = MaterialTheme.typography.labelSmall)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = isGoing, onClick = { spacesVm.rsvpEvent(ev, "going") }, label = { Text("Going") })
                    FilterChip(selected = isInterested, onClick = { spacesVm.rsvpEvent(ev, "interested") }, label = { Text("Interested") })
                    TextButton(onClick = { inviteEvent = ev }) { Text("Invite") }
                    IconButton(onClick = { shareEventTarget = ev }) {
                      Icon(Icons.Filled.Share, contentDescription = "Share event")
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

  if (showCreate) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-06-15") }
    var time by remember { mutableStateOf("6:00 PM") }
    AlertDialog(
      onDismissRequest = { showCreate = false },
      title = { Text("Create Event") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event title") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, modifier = Modifier.weight(1f))
          }
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(onClick = {
          spacesVm.createEvent(title, desc, loc, date, time, me?.displayName ?: "Host")
          showCreate = false
        }) { Text("Publish") }
      },
      dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
    )
  }

  inviteEvent?.let { ev ->
    AlertDialog(
      onDismissRequest = { inviteEvent = null },
      title = { Text("Invite to ${ev.title}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (allUsers.isEmpty()) Text("No people available to invite.")
          allUsers.take(8).forEach { person ->
            Row(
              modifier = Modifier.fillMaxWidth().clickable {
                spacesVm.inviteToEvent(ev.id, person.userId)
                inviteEvent = null
              }.padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(person.displayName)
              Text(if (person.userId in ev.invitedUids) "Invited" else "Invite", color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { inviteEvent = null }) { Text("Done") } }
    )
  }

  shareEventTarget?.let { ev ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "Event: ${ev.title}",
        body = "${ev.eventDateText} at ${ev.eventTimeText} · ${ev.locationName}\n${ev.description}",
        canRepostToFeed = false
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareEventTarget = null },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToConversation(conv, sender, msg)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToPerson(sender, person, msg)
      }
    )
  }
}

@Composable
fun MarketplaceScreen(
  spacesVm: SpacesGovernanceViewModel,
  socialVm: MainSocialViewModel,
  communityVm: CommunityViewModel,
  me: PublicUser?,
  currentUserId: String,
  onOpenChat: (String) -> Unit
) {
  val context = LocalContext.current
  val listingsState by spacesVm.listings.collectAsStateWithLifecycle()
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val isUploading by spacesVm.isUploadingMedia.collectAsStateWithLifecycle()
  var query by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var showCreate by remember { mutableStateOf(false) }
  var detailListing by remember { mutableStateOf<MarketplaceListing?>(null) }
  var shareListingTarget by remember { mutableStateOf<MarketplaceListing?>(null) }
  val categories = listOf("All", "Electronics", "Home", "Fashion", "Vehicles", "Books")

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Marketplace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Button(onClick = { showCreate = true }, modifier = Modifier.testTag("create_listing_button")) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Sell")
      }
    }
    if (isUploading) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Uploading product photo...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }

    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Search Marketplace...") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categories) { cat ->
        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
      }
    }

    when (val state = listingsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Filled.Storefront, "Marketplace unavailable", state.message)
      is UiState.Success -> {
        val filtered = state.data.filter { item ->
          (selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)) &&
            (query.isBlank() || item.title.contains(query, true) || item.description.contains(query, true))
        }
        if (filtered.isEmpty()) {
          EmptyStateCard(Icons.Filled.Storefront, "No listings match", "Post an item or adjust your filters.", actionLabel = "List an Item", onAction = { showCreate = true })
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { it.id }) { item ->
              Card(
                modifier = Modifier.fillMaxWidth().clickable { detailListing = item },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  if (item.imageUri.isNotBlank()) {
                    AsyncImage(
                      model = item.imageUri,
                      contentDescription = item.title,
                      modifier = Modifier.size(86.dp).clip(RoundedCornerShape(14.dp)),
                      contentScale = ContentScale.Crop
                    )
                  } else {
                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier.size(86.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Storefront, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "${item.currency}${"%.2f".format(item.price)}",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${item.category} · ${item.condition} · ${item.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreate) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var condition by remember { mutableStateOf("Like New") }
    var locationText by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }
    var pendingCropUri by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pendingCropUri = uri.toString()
    }
    AlertDialog(
      onDismissRequest = { showCreate = false },
      title = { Text("New Marketplace Listing") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item title") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price ($)") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = locationText, onValueChange = { locationText = it }, label = { Text("Pickup area") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
          OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Image, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (imageUri.isBlank()) "Pick Photo" else "Change Photo")
          }
          if (imageUri.isNotBlank()) {
            AsyncImage(
              model = imageUri,
              contentDescription = "Listing photo preview",
              modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp)),
              contentScale = ContentScale.Crop
            )
            OutlinedButton(onClick = { pendingCropUri = imageUri }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Crop Product Photo")
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          val seller = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
          spacesVm.createListing(context, seller, title, desc, price, category, condition, locationText, imageUri)
          showCreate = false
        }) { Text("List Item") }
      },
      dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
    )

    pendingCropUri?.let { raw ->
      ImageCropDialog(
        imageUri = raw,
        defaultAspect = CropAspect.SQUARE,
        onDismiss = { pendingCropUri = null },
        onUseOriginal = {
          imageUri = raw
          pendingCropUri = null
        },
        onCropped = { cropped ->
          imageUri = cropped
          pendingCropUri = null
        }
      )
    }
  }

  detailListing?.let { item ->
    var showReport by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Suspicious or prohibited item") }
    AlertDialog(
      onDismissRequest = { detailListing = null },
      title = { Text(item.title) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${item.currency}${"%.2f".format(item.price)} · ${item.status.uppercase()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodyMedium)
          Text("Location: ${item.locationText.ifBlank { "Local pickup" }}", style = MaterialTheme.typography.bodySmall)
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserAvatar(name = item.sellerName, photoUrl = item.sellerAvatar, size = 32.dp)
            Text("Seller: ${item.sellerName}", style = MaterialTheme.typography.labelLarge)
          }
          OutlinedButton(
            onClick = {
              shareListingTarget = item
              detailListing = null
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share Listing")
          }
          if (showReport) {
            OutlinedTextField(value = reportReason, onValueChange = { reportReason = it }, label = { Text("Report reason") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
              socialVm.reportContent("listing", item.id, reportReason)
              detailListing = null
            }) { Text("Submit Report") }
          }
        }
      },
      confirmButton = {
        if (item.sellerId == currentUserId) {
          Button(onClick = {
            spacesVm.toggleListingSold(item)
            detailListing = null
          }) { Text(if (item.status == "sold") "Mark Available" else "Mark Sold") }
        } else {
          Button(onClick = {
            val buyer = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
            val seller = PublicUser(userId = item.sellerId, displayName = item.sellerName, photoUrl = item.sellerAvatar)
            communityVm.startDirectChat(buyer, seller) { convId ->
              detailListing = null
              onOpenChat(convId)
            }
          }) { Text("Contact Seller") }
        }
      },
      dismissButton = {
        Row {
          if (item.sellerId != currentUserId && !showReport) {
            TextButton(onClick = { showReport = true }) { Text("Report") }
          }
          TextButton(onClick = { detailListing = null }) { Text("Close") }
        }
      }
    )
  }

  shareListingTarget?.let { item ->
    ShareOptionsBottomSheet(
      payload = SharePayload(
        title = "${item.title} — ${item.currency}${"%.2f".format(item.price)}",
        body = "${item.condition} · ${item.category}\n${item.description}",
        canRepostToFeed = true
      ),
      currentUserId = currentUserId,
      conversations = conversations,
      discoverableUsers = discoverableUsers,
      onDismiss = { shareListingTarget = null },
      onRepostToFeed = { note ->
        val composed = if (note.isBlank()) {
          "Check out ${item.title} (${item.currency}${"%.2f".format(item.price)}) on HELLO Marketplace!\n${item.description}"
        } else {
          "$note\n\n— Marketplace: ${item.title} (${item.currency}${"%.2f".format(item.price)})"
        }
        socialVm.createPost(context, composed, item.imageUri, if (item.imageUri.isNotBlank()) "image" else "none", "public", emptyList())
      },
      onSendToConversation = { conv, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToConversation(conv, sender, msg)
      },
      onSendToPerson = { person, msg ->
        val sender = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
        communityVm.shareTextToPerson(sender, person, msg)
      }
    )
  }
}
