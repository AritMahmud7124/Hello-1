package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.EventEntity
import com.example.data.model.MarketplaceListing
import com.example.data.model.PageEntity
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.ui.components.CropAspect
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullPhotoImage
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.ShareOptionsBottomSheet
import com.example.ui.components.SharePayload
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.components.resolveLiveAuthor
import com.example.ui.i18n.LocalAppStrings
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
  val strings = LocalAppStrings.current
  val pagesState by spacesVm.pages.collectAsStateWithLifecycle()
  val isUploading by spacesVm.isUploadingMedia.collectAsStateWithLifecycle()
  val feedState by socialVm.publicFeed.collectAsStateWithLifecycle()
  val myPosts by socialVm.myPosts.collectAsStateWithLifecycle()
  val allPublicPosts = remember(feedState, myPosts) {
    ((feedState as? UiState.Success)?.data.orEmpty() + myPosts).distinctBy { it.id }
  }
  val conversationsState = communityVm?.conversations?.collectAsStateWithLifecycle()
  val usersState = communityVm?.discoverableUsers?.collectAsStateWithLifecycle()
  val conversations = (conversationsState?.value as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState?.value as? UiState.Success)?.data.orEmpty()

  var pageQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var showCreate by remember { mutableStateOf(false) }
  var editPageTarget by remember { mutableStateOf<PageEntity?>(null) }
  var selectedPageId by remember { mutableStateOf<String?>(null) }
  var postPageId by remember { mutableStateOf<PageEntity?>(null) }
  var managePageTarget by remember { mutableStateOf<PageEntity?>(null) }
  var confirmDeletePage by remember { mutableStateOf<PageEntity?>(null) }
  var sharePageTarget by remember { mutableStateOf<PageEntity?>(null) }

  val categoryFilters = remember {
    listOf("All", "Creator", "Community", "Business", "Music", "Education", "Tech", "Sports")
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(strings.pages, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Creators, communities & brands on HELLO", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreate = true }, modifier = Modifier.testTag("create_page_button")) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(4.dp))
        Text(strings.createPage)
      }
    }

    if (isUploading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape))
    }

    OutlinedTextField(
      value = pageQuery,
      onValueChange = { pageQuery = it },
      placeholder = { Text("Search pages or categories...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (pageQuery.isNotBlank()) {
          IconButton(onClick = { pageQuery = "" }) {
            Icon(Icons.Rounded.Close, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth().testTag("pages_search_input")
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categoryFilters) { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text(cat) }
        )
      }
    }

    when (val state = pagesState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Rounded.Flag, "Pages unavailable", state.message)
      is UiState.Success -> {
        val filteredPages = remember(state.data, pageQuery, selectedCategory) {
          val q = pageQuery.trim()
          state.data.filter { page ->
            val catMatch = selectedCategory == "All" || page.category.contains(selectedCategory, ignoreCase = true)
            val queryMatch = q.isBlank() ||
              page.name.contains(q, ignoreCase = true) ||
              page.category.contains(q, ignoreCase = true) ||
              page.bio.contains(q, ignoreCase = true)
            catMatch && queryMatch
          }
        }
        if (filteredPages.isEmpty()) {
          EmptyStateCard(
            Icons.Rounded.Flag,
            if (state.data.isEmpty()) "No Pages yet" else "No matching Pages",
            if (state.data.isEmpty()) "Launch a creator or brand Page." else "Try another search or category.",
            actionLabel = strings.createPage,
            onAction = { showCreate = true }
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filteredPages, key = { it.id }) { page ->
              val isFollowing = currentUserId in page.followerUids
              val isCreator = currentUserId == page.creatorId
              val isAdmin = isCreator || currentUserId in page.adminUids
              val pagePosts = allPublicPosts.filter { it.pageId == page.id }

              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(20.dp))
                  .clickable { selectedPageId = page.id }
                  .testTag("page_card_${page.id}")
              ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  // Modern Cover Banner + Overlapping Page Avatar
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(108.dp)
                  ) {
                    if (page.coverUrl.isNotBlank()) {
                      AsyncImage(
                        model = page.coverUrl,
                        contentDescription = "${page.name} cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(
                            Brush.linearGradient(
                              listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.68f)
                              )
                            )
                          )
                      )
                    }
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))))
                    )
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = Color.Black.copy(alpha = 0.52f),
                      modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    ) {
                      Text(
                        text = page.category,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp)
                      .offset(y = (-22).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                      shadowElevation = 2.dp
                    ) {
                      UserAvatar(name = page.name, photoUrl = page.avatarUrl, size = 56.dp)
                    }
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(top = 24.dp)
                    ) {
                      IconButton(onClick = { sharePageTarget = page }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share page", modifier = Modifier.size(18.dp))
                      }
                      if (!isCreator) {
                        if (isFollowing) {
                          OutlinedButton(
                            onClick = { spacesVm.toggleFollowPage(page) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("page_out_${page.id}")
                          ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${strings.following} / Out")
                          }
                        } else {
                          Button(
                            onClick = { spacesVm.toggleFollowPage(page) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("page_follow_${page.id}")
                          ) {
                            Text(strings.follow)
                          }
                        }
                      } else {
                        OutlinedButton(
                          onClick = { editPageTarget = page },
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                          modifier = Modifier.testTag("edit_page_${page.id}")
                        ) {
                          Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("Edit")
                        }
                      }
                    }
                  }

                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                      .offset(y = (-12).dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(page.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                      text = "${page.followersCount} followers · ${pagePosts.size} posts" + when {
                        isCreator -> " · Your Page"
                        isAdmin -> " · Admin"
                        else -> ""
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (page.bio.isNotBlank()) {
                      Text(
                        text = page.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    if (isAdmin) {
                      Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        OutlinedButton(
                          onClick = { postPageId = page },
                          modifier = Modifier.testTag("publish_page_post_${page.id}")
                        ) {
                          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("Publish Page Post")
                        }
                        OutlinedButton(
                          onClick = { managePageTarget = page },
                          modifier = Modifier.testTag("manage_page_members_${page.id}")
                        ) {
                          Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("Add / Out")
                        }
                        if (!isCreator) {
                          OutlinedButton(onClick = { editPageTarget = page }) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                          }
                        }
                        if (isCreator) {
                          IconButton(
                            onClick = { confirmDeletePage = page },
                            modifier = Modifier.testTag("delete_page_${page.id}")
                          ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete page", tint = MaterialTheme.colorScheme.error)
                          }
                        }
                      }
                    }
                    pagePosts.take(2).forEach { p ->
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text(p.content.ifBlank { "Shared media post" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                          Text("${p.likesCount} likes · ${p.commentsCount} comments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

  // Dedicated Page Detail Hub Dialog
  selectedPageId?.let { pid ->
    val livePage = ((pagesState as? UiState.Success)?.data.orEmpty()).firstOrNull { it.id == pid }
    if (livePage == null) {
      selectedPageId = null
    } else {
      val isFollowing = currentUserId in livePage.followerUids
      val isCreator = currentUserId == livePage.creatorId
      val isAdmin = isCreator || currentUserId in livePage.adminUids
      val pagePosts = allPublicPosts.filter { it.pageId == livePage.id }

      Dialog(
        onDismissRequest = { selectedPageId = null },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
      ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              IconButton(onClick = { selectedPageId = null }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close page")
              }
              Text(livePage.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
              IconButton(onClick = { sharePageTarget = livePage }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share page")
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = 28.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              item {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                    if (livePage.coverUrl.isNotBlank()) {
                      AsyncImage(
                        model = livePage.coverUrl,
                        contentDescription = "${livePage.name} cover banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(
                            Brush.linearGradient(
                              listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                          )
                      )
                    }
                  }
                  Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-28).dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.Bottom
                    ) {
                      Surface(
                        shape = CircleShape,
                        border = BorderStroke(4.dp, MaterialTheme.colorScheme.background),
                        shadowElevation = 3.dp
                      ) {
                        UserAvatar(name = livePage.name, photoUrl = livePage.avatarUrl, size = 76.dp)
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isCreator) {
                          if (isFollowing) {
                            OutlinedButton(onClick = { spacesVm.toggleFollowPage(livePage) }) {
                              Text("${strings.following} / Out")
                            }
                          } else {
                            Button(onClick = { spacesVm.toggleFollowPage(livePage) }) {
                              Text(strings.follow)
                            }
                          }
                        }
                        if (isAdmin) {
                          OutlinedButton(onClick = { editPageTarget = livePage }) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Page")
                          }
                        }
                      }
                    }

                    Text(livePage.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                      text = "${livePage.category} · ${livePage.followersCount} followers · ${pagePosts.size} posts",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary
                    )
                    if (livePage.bio.isNotBlank()) {
                      Text(livePage.bio, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (isAdmin) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { postPageId = livePage }, modifier = Modifier.weight(1f)) {
                          Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("Publish Post")
                        }
                        OutlinedButton(onClick = { managePageTarget = livePage }, modifier = Modifier.weight(1f)) {
                          Icon(Icons.Rounded.GroupAdd, null, modifier = Modifier.size(16.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("Members")
                        }
                      }
                    }
                  }
                }
              }

              if (pagePosts.isEmpty()) {
                item {
                  Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyStateCard(
                      icon = Icons.Rounded.Flag,
                      title = "No Page posts yet",
                      subtitle = if (isAdmin) "Publish an update or media post to engage your Page followers." else "Follow this Page to see upcoming posts."
                    )
                  }
                }
              } else {
                items(pagePosts, key = { "page_post_${it.id}" }) { p ->
                  Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                  ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UserAvatar(name = livePage.name, photoUrl = livePage.avatarUrl.ifBlank { p.authorAvatar }, size = 36.dp)
                        Column {
                          Text(livePage.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                          Text(formatTimestamp(p.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      }
                      if (p.content.isNotBlank()) {
                        Text(p.content, style = MaterialTheme.typography.bodyMedium)
                      }
                      if (p.mediaUri.isNotBlank() && p.mediaType == "image") {
                        FullPhotoImage(model = p.mediaUri, contentDescription = "Page post image", cornerRadius = 12.dp)
                      }
                      Text(
                        text = "${p.likesCount} reactions · ${p.commentsCount} comments",
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
    }
  }

  if (showCreate || editPageTarget != null) {
    PageEditorDialog(
      existingPage = editPageTarget,
      onDismiss = {
        showCreate = false
        editPageTarget = null
      },
      onSave = { name, category, bio, avatarUri, coverUri ->
        val editing = editPageTarget
        if (editing == null) {
          spacesVm.createPage(name, category, bio, context = context, avatarUri = avatarUri, coverUri = coverUri)
          showCreate = false
        } else {
          spacesVm.updatePage(editing.id, name, category, bio, context = context, avatarUri = avatarUri, coverUri = coverUri) {
            editPageTarget = null
          }
        }
      }
    )
  }

  postPageId?.let { page ->
    CreatePostDialog(
      onDismiss = { postPageId = null },
      onPublish = { content, mediaUri, mediaType, _, music ->
        socialVm.createPost(context, content, mediaUri, mediaType, "public", friendUids, pageId = page.id, music = music)
        postPageId = null
      },
      authorName = page.name.ifBlank { me?.displayName ?: "Hello Member" },
      authorAvatar = page.avatarUrl.ifBlank { me?.photoUrl ?: "" },
      authorOnline = me?.effectiveOnline == true
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
        socialVm.createPost(context, composed, page.coverUrl.ifBlank { page.avatarUrl }, if (page.coverUrl.isNotBlank() || page.avatarUrl.isNotBlank()) "image" else "none", "public", friendUids)
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

  managePageTarget?.let { targetSnapshot ->
    val livePage = ((pagesState as? UiState.Success)?.data.orEmpty()).firstOrNull { it.id == targetSnapshot.id } ?: targetSnapshot
    val friendSet = friendUids.toSet()
    val nonMembers = discoverableUsers
      .filter { it.userId != currentUserId && it.userId !in livePage.followerUids }
      .sortedByDescending { it.userId in friendSet }
    val currentMembers = (livePage.followerUids + livePage.adminUids).distinct().map { uid ->
      discoverableUsers.firstOrNull { it.userId == uid } ?: PublicUser(
        userId = uid,
        displayName = if (uid == currentUserId) (me?.displayName ?: "You") else "Member ${uid.take(6)}"
      )
    }
    var searchPeople by remember { mutableStateOf("") }
    val filteredNonMembers = remember(nonMembers, searchPeople) {
      if (searchPeople.isBlank()) nonMembers else nonMembers.filter {
        it.displayName.contains(searchPeople, true) || it.handle.contains(searchPeople, true)
      }
    }
    AlertDialog(
      onDismissRequest = { managePageTarget = null },
      title = { Text("Page Add & Out (${livePage.name})") },
      text = {
        Column(
          modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Current Members & Followers (${currentMembers.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          currentMembers.forEach { member ->
            val isMemberCreator = member.userId == livePage.creatorId
            val isMemberAdmin = isMemberCreator || member.userId in livePage.adminUids
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName, fontWeight = FontWeight.Medium)
                Text(
                  text = when {
                    isMemberCreator -> "Page Creator"
                    isMemberAdmin -> "Page Admin"
                    else -> member.handle.ifBlank { "Follower" }
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (!isMemberCreator) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                  if (currentUserId == livePage.creatorId) {
                    TextButton(
                      onClick = { spacesVm.togglePageAdmin(livePage, member.userId, member.displayName, !isMemberAdmin) },
                      modifier = Modifier.testTag("toggle_page_admin_${member.userId}")
                    ) {
                      Text(if (isMemberAdmin) "Admin ✓" else "Make Admin")
                    }
                  }
                  TextButton(
                    onClick = { spacesVm.removePageMember(livePage, member.userId, member.displayName) },
                    modifier = Modifier.testTag("remove_page_member_${member.userId}")
                  ) {
                    Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Out", color = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }

          HorizontalDivider()

          Text(
            text = "Add People to Page",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          OutlinedTextField(
            value = searchPeople,
            onValueChange = { searchPeople = it },
            label = { Text("Search friends & people to add") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (filteredNonMembers.isEmpty()) {
            Text(
              if (nonMembers.isEmpty()) "All discovered users are already on this Page." else "No matching users found.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            filteredNonMembers.take(25).forEach { person ->
              val isFriend = person.userId in friendSet
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .clickable {
                    val effectiveMe = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
                    spacesVm.addPageMember(livePage, effectiveMe, person.userId, person.displayName)
                  }
                  .padding(vertical = 6.dp)
                  .testTag("add_page_member_${person.userId}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(person.displayName, fontWeight = FontWeight.Medium)
                  Text(
                    text = listOfNotNull(
                      if (isFriend) "Friend" else null,
                      person.handle.takeIf { it.isNotBlank() }
                    ).joinToString(" · ").ifBlank { "Member" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Text("+ Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { managePageTarget = null }) { Text("Done") } }
    )
  }

  confirmDeletePage?.let { targetPage ->
    AlertDialog(
      onDismissRequest = { confirmDeletePage = null },
      title = { Text("Delete Page?") },
      text = { Text("This will permanently remove \"${targetPage.name}\" and its follower list.") },
      confirmButton = {
        Button(
          onClick = {
            spacesVm.deletePage(targetPage)
            confirmDeletePage = null
            if (selectedPageId == targetPage.id) selectedPageId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeletePage = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun PageEditorDialog(
  existingPage: PageEntity?,
  onDismiss: () -> Unit,
  onSave: (name: String, category: String, bio: String, avatarUri: String, coverUri: String) -> Unit
) {
  var name by remember(existingPage?.id) { mutableStateOf(existingPage?.name ?: "") }
  var category by remember(existingPage?.id) { mutableStateOf(existingPage?.category ?: "Digital Creator") }
  var bio by remember(existingPage?.id) { mutableStateOf(existingPage?.bio ?: "") }
  var avatarUri by remember(existingPage?.id) { mutableStateOf(existingPage?.avatarUrl ?: "") }
  var coverUri by remember(existingPage?.id) { mutableStateOf(existingPage?.coverUrl ?: "") }

  var cropTargetType by remember { mutableStateOf<String?>(null) } // "avatar" or "cover"
  var pendingCropUri by remember { mutableStateOf<String?>(null) }

  val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      cropTargetType = "avatar"
      pendingCropUri = uri.toString()
    }
  }
  val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      cropTargetType = "cover"
      pendingCropUri = uri.toString()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingPage == null) "Create HELLO Page" else "Edit Page") },
    text = {
      Column(
        modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Cover Preview & Picker
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
              )
            )
            .clickable { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
          contentAlignment = Alignment.Center
        ) {
          if (coverUri.isNotBlank()) {
            AsyncImage(
              model = coverUri,
              contentDescription = "Page cover preview",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          }
          Surface(color = Color.Black.copy(alpha = 0.56f), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Image, null, tint = Color.White, modifier = Modifier.size(16.dp))
              Text(if (coverUri.isBlank()) "Add Cover Photo" else "Change Cover", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
          }
        }

        // Avatar Picker Row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          UserAvatar(name = name.ifBlank { "Page" }, photoUrl = avatarUri, size = 54.dp)
          OutlinedButton(
            onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Rounded.Crop, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (avatarUri.isBlank()) "Pick Page Logo" else "Change Page Logo")
          }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Page name") }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Digital Creator", "Community", "Business", "Music", "Education", "Tech", "Sports").forEach { preset ->
            FilterChip(selected = category == preset, onClick = { category = preset }, label = { Text(preset) })
          }
        }
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("About / Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
      }
    },
    confirmButton = {
      Button(
        enabled = name.trim().length >= 2,
        onClick = { onSave(name.trim(), category.trim().ifBlank { "Creator" }, bio.trim(), avatarUri, coverUri) }
      ) {
        Text(if (existingPage == null) "Create" else "Save Changes")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )

  pendingCropUri?.let { raw ->
    val isCover = cropTargetType == "cover"
    ImageCropDialog(
      imageUri = raw,
      defaultAspect = if (isCover) CropAspect.COVER else CropAspect.SQUARE,
      onDismiss = { pendingCropUri = null },
      onUseOriginal = {
        if (isCover) coverUri = raw else avatarUri = raw
        pendingCropUri = null
      },
      onCropped = { cropped ->
        if (isCover) coverUri = cropped else avatarUri = cropped
        pendingCropUri = null
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
  val strings = LocalAppStrings.current
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
        Text(strings.events, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Discover meetups & invite friends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(onClick = { showCreate = true }, modifier = Modifier.testTag("create_event_button")) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(4.dp))
        Text("Create")
      }
    }

    when (val state = eventsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Rounded.Event, "Events unavailable", state.message)
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          EmptyStateCard(Icons.Rounded.Event, "No upcoming events", "Host a local meetup or online stream.", actionLabel = "Create Event", onAction = { showCreate = true })
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
                    Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ev.locationName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  if (ev.description.isNotBlank()) Text(ev.description, style = MaterialTheme.typography.bodyMedium)
                  Text("${ev.goingUids.size} Going · ${ev.interestedUids.size} Interested", style = MaterialTheme.typography.labelSmall)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = isGoing, onClick = { spacesVm.rsvpEvent(ev, "going") }, label = { Text("Going") })
                    FilterChip(selected = isInterested, onClick = { spacesVm.rsvpEvent(ev, "interested") }, label = { Text("Interested") })
                    TextButton(onClick = { inviteEvent = ev }) { Text("Invite") }
                    IconButton(onClick = { shareEventTarget = ev }) {
                      Icon(Icons.Rounded.Share, contentDescription = "Share event")
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
  val strings = LocalAppStrings.current
  val listingsState by spacesVm.listings.collectAsStateWithLifecycle()
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val usersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val discoverableUsers = (usersState as? UiState.Success)?.data.orEmpty()
  val usersById = remember(discoverableUsers, me) {
    val map = discoverableUsers.associateBy { it.userId }.toMutableMap()
    if (me != null) map[me.userId] = me
    map
  }
  val isUploading by spacesVm.isUploadingMedia.collectAsStateWithLifecycle()

  var query by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var statusFilter by remember { mutableStateOf("All") } // All, Available, Sold, Mine
  var sortMode by remember { mutableStateOf("Newest") } // Newest, PriceAsc, PriceDesc
  var showCreate by remember { mutableStateOf(false) }
  var editListingTarget by remember { mutableStateOf<MarketplaceListing?>(null) }
  var confirmDeleteTarget by remember { mutableStateOf<MarketplaceListing?>(null) }
  var detailListing by remember { mutableStateOf<MarketplaceListing?>(null) }
  var shareListingTarget by remember { mutableStateOf<MarketplaceListing?>(null) }

  val categories = remember { listOf("All", "Electronics", "Home", "Fashion", "Vehicles", "Books", "Services") }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(strings.marketplace, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
          .clip(RoundedCornerShape(18.dp))
          .clickable { showCreate = true }
          .testTag("create_listing_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(17.dp))
          Text(strings.sellItem, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
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
      placeholder = { Text("Search ${strings.marketplace}") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
      trailingIcon = {
        if (query.isNotBlank()) {
          IconButton(onClick = { query = "" }) {
            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
          }
        }
      },
      shape = RoundedCornerShape(22.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
      ),
      modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input"),
      singleLine = true
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(categories) { cat ->
        val selected = selectedCategory == cat
        Surface(
          shape = CircleShape,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.clip(CircleShape).clickable { selectedCategory = cat }
        ) {
          Text(
            text = cat,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf(
        "All" to "All",
        "Available" to strings.available,
        "Sold" to strings.sold,
        "Mine" to "My Listings"
      ).forEach { (key, label) ->
        val selected = statusFilter == key
        Surface(
          shape = CircleShape,
          color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.clip(CircleShape).clickable { statusFilter = key }
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
      Surface(
        shape = CircleShape,
        color = if (sortMode != "Newest") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
          .clip(CircleShape)
          .clickable {
            sortMode = when (sortMode) {
              "Newest" -> "PriceAsc"
              "PriceAsc" -> "PriceDesc"
              else -> "Newest"
            }
          }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(14.dp))
          Text(
            text = when (sortMode) {
              "PriceAsc" -> "Price ↑"
              "PriceDesc" -> "Price ↓"
              else -> "Newest"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    when (val state = listingsState) {
      is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is UiState.Error -> EmptyStateCard(Icons.Rounded.Storefront, "Marketplace unavailable", state.message)
      is UiState.Success -> {
        val filtered = remember(state.data, selectedCategory, query, statusFilter, sortMode, currentUserId) {
          val base = state.data.filter { item ->
            val catOk = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val qOk = query.isBlank() ||
              item.title.contains(query, true) ||
              item.description.contains(query, true) ||
              item.locationText.contains(query, true)
            val statusOk = when (statusFilter) {
              "Available" -> item.status != "sold"
              "Sold" -> item.status == "sold"
              "Mine" -> item.sellerId == currentUserId
              else -> true
            }
            catOk && qOk && statusOk
          }
          when (sortMode) {
            "PriceAsc" -> base.sortedBy { it.price }
            "PriceDesc" -> base.sortedByDescending { it.price }
            else -> base
          }
        }
        if (filtered.isEmpty()) {
          EmptyStateCard(Icons.Rounded.Storefront, "No listings match", "Post an item or adjust your filters.", actionLabel = strings.sellItem, onAction = { showCreate = true })
        } else {
          val rows = remember(filtered) { filtered.chunked(2) }
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
          ) {
            items(rows, key = { row -> row.first().id }) { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                pair.forEach { item ->
                  val isSold = item.status == "sold"
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { detailListing = item }
                      .testTag("marketplace_card_${item.id}")
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                      if (item.imageUri.isNotBlank()) {
                        AsyncImage(
                          model = item.imageUri,
                          contentDescription = item.title,
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                      } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                          Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = item.title,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                          )
                        }
                      }
                      if (isSold) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = Color.Black.copy(alpha = 0.7f),
                          modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        ) {
                          Text(
                            text = strings.sold,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                    Column(
                      modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp, bottom = 4.dp),
                      verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                      Text(
                        text = "${item.currency}${if (item.price % 1.0 == 0.0) item.price.toLong().toString() else "%.2f".format(item.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                      )
                      Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = item.locationText.ifBlank { item.category },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreate || editListingTarget != null) {
    MarketplaceEditorDialog(
      existingListing = editListingTarget,
      editTitle = strings.editListing,
      onDismiss = {
        showCreate = false
        editListingTarget = null
      },
      onSave = { title, price, currency, category, condition, locationText, desc, imageUri ->
        val editing = editListingTarget
        if (editing == null) {
          val seller = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
          spacesVm.createListing(context, seller, title, desc, price, currency, category, condition, locationText, imageUri)
          showCreate = false
        } else {
          spacesVm.updateListing(context, editing.id, title, desc, price, currency, category, condition, locationText, imageUri) {
            editListingTarget = null
          }
        }
      }
    )
  }

  detailListing?.let { item ->
    var showReport by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Suspicious or prohibited item") }
    val isMine = item.sellerId == currentUserId
    AlertDialog(
      onDismissRequest = { detailListing = null },
      title = { Text(item.title, fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (item.imageUri.isNotBlank()) {
            FullPhotoImage(
              model = item.imageUri,
              contentDescription = item.title,
              cornerRadius = 12.dp,
              minHeight = 140.dp,
              maxHeight = 250.dp
            )
          }
          Text(
            "${item.currency}${"%.2f".format(item.price)} · ${if (item.status == "sold") strings.sold else strings.available}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text("${item.category} · ${item.condition}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodyMedium)
          Text("Location: ${item.locationText.ifBlank { "Local pickup" }}", style = MaterialTheme.typography.bodySmall)
          val resolvedSeller = resolveLiveAuthor(item.sellerId, item.sellerName, item.sellerAvatar, usersById, me)
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserAvatar(name = resolvedSeller.displayName, photoUrl = resolvedSeller.avatarUrl, size = 32.dp)
            Text("Seller: ${resolvedSeller.displayName}", style = MaterialTheme.typography.labelLarge)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                shareListingTarget = item
                detailListing = null
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Share")
            }
            if (isMine) {
              OutlinedButton(
                onClick = {
                  editListingTarget = item
                  detailListing = null
                },
                modifier = Modifier.weight(1f).testTag("edit_listing_button_${item.id}")
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.editListing)
              }
            }
          }
          if (isMine) {
            OutlinedButton(
              onClick = {
                confirmDeleteTarget = item
                detailListing = null
              },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth().testTag("delete_listing_button_${item.id}")
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.deleteListing)
            }
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
        if (isMine) {
          Button(onClick = {
            spacesVm.toggleListingSold(item)
            detailListing = null
          }) { Text(if (item.status == "sold") "Mark ${strings.available}" else "Mark ${strings.sold}") }
        } else {
          Button(
            onClick = {
              val buyer = me ?: PublicUser(userId = currentUserId, displayName = "Hello Member")
              val seller = PublicUser(userId = item.sellerId, displayName = item.sellerName, photoUrl = item.sellerAvatar)
              communityVm.sendMarketplaceInquiry(
                context = context,
                buyer = buyer,
                seller = seller,
                listingTitle = item.title,
                listingPriceText = "${item.currency}${"%.2f".format(item.price)}",
                listingImageUri = item.imageUri,
                inquiryText = "Is this available?"
              ) { convId ->
                detailListing = null
                onOpenChat(convId)
              }
            },
            modifier = Modifier.testTag("marketplace_message_seller_button")
          ) { Text(strings.messageSeller) }
        }
      },
      dismissButton = {
        Row {
          if (!isMine && !showReport) {
            TextButton(onClick = { showReport = true }) { Text("Report") }
          }
          TextButton(onClick = { detailListing = null }) { Text("Close") }
        }
      }
    )
  }

  confirmDeleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { confirmDeleteTarget = null },
      title = { Text("Delete Listing?") },
      text = { Text("Remove \"${target.title}\" permanently from Marketplace?") },
      confirmButton = {
        Button(
          onClick = {
            spacesVm.deleteListing(target.id)
            confirmDeleteTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(strings.deleteListing)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeleteTarget = null }) { Text("Cancel") }
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

@Composable
private fun MarketplaceEditorDialog(
  existingListing: MarketplaceListing?,
  editTitle: String,
  onDismiss: () -> Unit,
  onSave: (title: String, price: String, currency: String, category: String, condition: String, locationText: String, desc: String, imageUri: String) -> Unit
) {
  var title by remember(existingListing?.id) { mutableStateOf(existingListing?.title ?: "") }
  var price by remember(existingListing?.id) {
    mutableStateOf(if (existingListing != null && existingListing.price > 0.0) existingListing.price.toString() else "")
  }
  var currency by remember(existingListing?.id) { mutableStateOf(existingListing?.currency?.ifBlank { "$" } ?: "$") }
  var category by remember(existingListing?.id) { mutableStateOf(existingListing?.category?.ifBlank { "Electronics" } ?: "Electronics") }
  var condition by remember(existingListing?.id) { mutableStateOf(existingListing?.condition?.ifBlank { "Like New" } ?: "Like New") }
  var locationText by remember(existingListing?.id) { mutableStateOf(existingListing?.locationText ?: "") }
  var desc by remember(existingListing?.id) { mutableStateOf(existingListing?.description ?: "") }
  var imageUri by remember(existingListing?.id) { mutableStateOf(existingListing?.imageUri ?: "") }
  var pendingCropUri by remember { mutableStateOf<String?>(null) }

  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      pendingCropUri = uri.toString()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingListing == null) "New Listing" else editTitle) },
    text = {
      Column(
        modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item title") }, modifier = Modifier.fillMaxWidth())

        Text("Currency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("$", "৳", "₹", "€", "£").forEach { sym ->
            FilterChip(selected = currency == sym, onClick = { currency = sym }, label = { Text(sym) })
          }
        }

        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price ($currency)") }, modifier = Modifier.fillMaxWidth())

        Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Electronics", "Home", "Fashion", "Vehicles", "Books", "Services").forEach { cat ->
            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
          }
        }

        Text("Condition", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("New", "Like New", "Good", "Fair").forEach { cond ->
            FilterChip(selected = condition == cond, onClick = { condition = cond }, label = { Text(cond) })
          }
        }

        OutlinedTextField(value = locationText, onValueChange = { locationText = it }, label = { Text("Pickup area") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.Image, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (imageUri.isBlank()) "Add Photo" else "Change Photo")
        }
        if (imageUri.isNotBlank()) {
          FullPhotoImage(
            model = imageUri,
            contentDescription = "Listing photo preview",
            cornerRadius = 10.dp,
            minHeight = 130.dp,
            maxHeight = 220.dp
          )
          OutlinedButton(onClick = { pendingCropUri = imageUri }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit / Crop Photo")
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = title.trim().length >= 2 && price.toDoubleOrNull() != null,
        onClick = { onSave(title.trim(), price.trim(), currency, category, condition, locationText.trim(), desc.trim(), imageUri) }
      ) {
        Text(if (existingListing == null) "Publish" else "Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
