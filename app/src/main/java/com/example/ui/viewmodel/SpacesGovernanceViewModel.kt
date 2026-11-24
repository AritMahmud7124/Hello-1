package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRecord
import com.example.data.model.DevotionTrial
import com.example.data.model.EventEntity
import com.example.data.model.GlobalSettings
import com.example.data.model.Group
import com.example.data.model.MarketplaceListing
import com.example.data.model.MutualCrushMatch
import com.example.data.model.MutualMatchPhase
import com.example.data.model.MutualSecretLoveEngine
import com.example.data.model.OwnerConfig
import com.example.data.model.PageEntity
import com.example.data.model.PrivateCrushMessage
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.PureLoveTier
import com.example.data.model.RegionalAnalytics
import com.example.data.model.ReportEntity
import com.example.data.model.SecretLoveMessage
import com.example.data.model.SecretLoveSendOutcome
import com.example.data.model.SentSecretLove
import com.example.data.model.SosAlert
import com.example.data.model.UiState
import com.example.data.model.unwrapFirestoreMessage
import com.example.data.repository.GovernanceRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SpacesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpacesGovernanceViewModel(
  private val spacesRepository: SpacesRepository,
  private val governanceRepository: GovernanceRepository,
  private val socialRepository: SocialRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  private val _isSendingSecretLove = MutableStateFlow(false)
  val isSendingSecretLove: StateFlow<Boolean> = _isSendingSecretLove.asStateFlow()

  private val _latestCrushMatch = MutableStateFlow<SecretLoveSendOutcome?>(null)
  val latestCrushMatch: StateFlow<SecretLoveSendOutcome?> = _latestCrushMatch.asStateFlow()

  private val _latestUnlockedMatch = MutableStateFlow<MutualCrushMatch?>(null)
  val latestUnlockedMatch: StateFlow<MutualCrushMatch?> = _latestUnlockedMatch.asStateFlow()

  private val _liveClockMs = MutableStateFlow(System.currentTimeMillis())
  val liveClockMs: StateFlow<Long> = _liveClockMs.asStateFlow()

  private val _latestBadgeUnlocked = MutableStateFlow<PureLoveTier?>(null)
  val latestBadgeUnlocked: StateFlow<PureLoveTier?> = _latestBadgeUnlocked.asStateFlow()

  private val celebratedMatchIds = mutableSetOf<String>()

  val mySecretCrushes: StateFlow<Set<String>> = governanceRepository.observeMySecretCrushes(currentUserId)
    .catch { emit(emptySet()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

  val myMutualMatches: StateFlow<List<MutualCrushMatch>> = governanceRepository.observeMyMutualMatches(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myDevotionTrials: StateFlow<List<DevotionTrial>> = governanceRepository.observeMyDevotionTrials(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  init {
    viewModelScope.launch {
      myMutualMatches.collectLatest { matches ->
        if (matches.isEmpty()) return@collectLatest
        while (true) {
          val now = System.currentTimeMillis()
          _liveClockMs.value = now

          var nearestFutureTransitionMs = Long.MAX_VALUE

          for (match in matches) {
            when (MutualSecretLoveEngine.phaseOf(match, now)) {
              MutualMatchPhase.SEALED_HIDDEN, MutualMatchPhase.COUNTDOWN_24H -> {
                // Complete stealth before countdown officially unlocks: never emit 24h warning or surface match early.
                if (match.revealAtMs > now) {
                  nearestFutureTransitionMs = minOf(nearestFutureTransitionMs, match.revealAtMs - now)
                }
              }
              MutualMatchPhase.UNLOCKED -> {
                if (!match.unlockedNotified) {
                  val unlockedNow = governanceRepository.dispatchMutualUnlockIfNeeded(match).getOrDefault(false)
                  if (unlockedNow && match.id.isNotBlank() && celebratedMatchIds.add(match.id)) {
                    _latestUnlockedMatch.value = match
                  }
                }
              }
            }
          }

          if (nearestFutureTransitionMs < Long.MAX_VALUE) {
            delay(nearestFutureTransitionMs.coerceIn(1_000L, 30_000L))
          } else {
            break
          }
        }
      }
    }
  }

  val publicGroups: StateFlow<List<Group>> = spacesRepository.observePublicGroups()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myGroups: StateFlow<List<Group>> = spacesRepository.observeMyGroups(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val pages: StateFlow<UiState<List<PageEntity>>> = spacesRepository.observePages()
    .map<List<PageEntity>, UiState<List<PageEntity>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load pages"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val events: StateFlow<UiState<List<EventEntity>>> = spacesRepository.observeEvents()
    .map<List<EventEntity>, UiState<List<EventEntity>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load events"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val listings: StateFlow<UiState<List<MarketplaceListing>>> = spacesRepository.observeMarketplaceListings()
    .map<List<MarketplaceListing>, UiState<List<MarketplaceListing>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load marketplace"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val receivedSecretLoves: StateFlow<UiState<List<SecretLoveMessage>>> = governanceRepository.observeReceivedSecretLoves(currentUserId)
    .map<List<SecretLoveMessage>, UiState<List<SecretLoveMessage>>> { UiState.Success(it) }
    .catch { err -> emit(UiState.Error(err.unwrapFirestoreMessage("Failed to load Secret Love"))) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val sentSecretLoves: StateFlow<List<SentSecretLove>> = governanceRepository.observeSentSecretLoves(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val ownerConfig: StateFlow<OwnerConfig?> = governanceRepository.observeOwnerConfig()
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val globalSettings: StateFlow<GlobalSettings> = governanceRepository.observeGlobalSettings()
    .map { it ?: GlobalSettings() }
    .catch { emit(GlobalSettings()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GlobalSettings())

  val isVerifiedOwner: StateFlow<Boolean> = ownerConfig
    .map { cfg -> cfg != null && cfg.ownerUid.isNotBlank() && cfg.ownerUid == currentUserId }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val regionalAnalytics: StateFlow<List<RegionalAnalytics>> = isVerifiedOwner
    .flatMapLatest { verified ->
      if (verified) {
        governanceRepository.observeAllRegionalAnalytics().catch { emit(emptyList()) }
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val ownerCrushSignals: StateFlow<List<GovernanceRepository.CrushSignalAuditItem>> = isVerifiedOwner
    .flatMapLatest { verified ->
      if (verified) {
        governanceRepository.observeAllCrushSignalsForOwner().catch { emit(emptyList()) }
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val ownerMutualMatches: StateFlow<List<MutualCrushMatch>> = isVerifiedOwner
    .flatMapLatest { verified ->
      if (verified) {
        governanceRepository.observeAllMutualMatchesForOwner().catch { emit(emptyList()) }
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val ownerPrivateUsers: StateFlow<List<PrivateUser>> = isVerifiedOwner
    .flatMapLatest { verified ->
      if (verified) {
        governanceRepository.observeAllPrivateUsersForOwner().catch { emit(emptyList()) }
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val admins: StateFlow<List<AdminRecord>> = governanceRepository.observeAdmins()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val reports: StateFlow<List<ReportEntity>> = governanceRepository.observeReports()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  fun dismissCrushMatch() {
    _latestCrushMatch.value = null
  }

  fun openUnlockedMatchCelebration(match: MutualCrushMatch) {
    _latestUnlockedMatch.value = match
  }

  fun dismissUnlockedMatch() {
    _latestUnlockedMatch.value = null
  }

  fun dismissBadgeUnlocked() {
    _latestBadgeUnlocked.value = null
  }

  fun clearFeedback() {
    _feedback.value = null
  }

  // Groups & SOS
  fun createGroup(name: String, description: String, category: String, privacy: String, sosEnabled: Boolean) {
    if (name.trim().length < 2) {
      _feedback.value = "Group name must be at least 2 characters."
      return
    }
    viewModelScope.launch {
      spacesRepository.createGroup(name, description, category, privacy, sosEnabled)
        .onSuccess { _feedback.value = "Group \"$name\" created!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Group creation failed" }
    }
  }

  fun toggleMembership(group: Group) {
    val isMember = currentUserId in group.memberUids
    viewModelScope.launch {
      spacesRepository.joinOrLeaveGroup(group, !isMember)
        .onSuccess { _feedback.value = if (!isMember) "Joined ${group.name}" else "Left ${group.name}" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Membership update failed") }
    }
  }

  fun inviteMember(group: Group, actor: PublicUser, targetUid: String) {
    viewModelScope.launch {
      spacesRepository.inviteMemberToGroup(group, actor, targetUid)
        .onSuccess { _feedback.value = "Member added to ${group.name}" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Only group admins can add members") }
    }
  }

  fun removeMember(group: Group, targetUid: String, targetName: String) {
    viewModelScope.launch {
      spacesRepository.removeMemberFromGroup(group, targetUid)
        .onSuccess { _feedback.value = "Removed $targetName from ${group.name}" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Only group admins can remove members") }
    }
  }

  fun toggleGroupAdmin(group: Group, targetUid: String, targetName: String, makeAdmin: Boolean) {
    viewModelScope.launch {
      spacesRepository.toggleGroupAdmin(group.id, targetUid, makeAdmin)
        .onSuccess {
          _feedback.value = if (makeAdmin) "$targetName is now a Group Admin" else "$targetName removed from Group Admins"
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not update group admin") }
    }
  }

  fun deleteGroup(group: Group, onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      spacesRepository.deleteGroup(group.id)
        .onSuccess {
          _feedback.value = "Group \"${group.name}\" deleted"
          onDeleted()
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not delete group") }
    }
  }

  fun observeGroupSosAlerts(groupId: String): Flow<List<SosAlert>> =
    combine(
      spacesRepository.observeMySentSosAlerts(groupId, currentUserId).catch { emit(emptyList()) },
      spacesRepository.observeIncomingSosAlerts(groupId, currentUserId).catch { emit(emptyList()) }
    ) { sent, incoming ->
      (sent + incoming).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  fun sendSosAlert(
    group: Group,
    sender: PublicUser,
    message: String,
    recipientUids: List<String>,
    shareLiveLocation: Boolean,
    latitude: Double,
    longitude: Double,
    onCreated: (groupId: String, alertId: String) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      spacesRepository.sendGroupSosAlert(group, sender, message, recipientUids, shareLiveLocation, latitude, longitude)
        .onSuccess { alertId ->
          onCreated(group.id, alertId)
          _feedback.value = if (shareLiveLocation) {
            "SOS Alert & live GPS tracking sent to ${recipientUids.size} trusted contact(s)."
          } else {
            "SOS Alert sent privately to ${recipientUids.size} member(s)."
          }
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("SOS dispatch failed") }
    }
  }

  fun updateSosLiveLocation(groupId: String, alertId: String, latitude: Double, longitude: Double) {
    viewModelScope.launch {
      spacesRepository.updateSosLiveLocation(groupId, alertId, latitude, longitude)
    }
  }

  fun stopOrCancelSos(groupId: String, alertId: String, status: String) {
    viewModelScope.launch {
      spacesRepository.stopOrCancelSosAlert(groupId, alertId, status)
        .onSuccess {
          _feedback.value = if (status == "stopped") "Live location sharing stopped." else "Emergency SOS cancelled."
        }
        .onFailure { _feedback.value = it.localizedMessage ?: "Could not update SOS status" }
    }
  }

  // Pages
  fun createPage(
    name: String,
    category: String,
    bio: String,
    context: Context? = null,
    avatarUri: String = "",
    coverUri: String = ""
  ) {
    if (name.trim().length < 2) {
      _feedback.value = "Page name must be at least 2 characters."
      return
    }
    viewModelScope.launch {
      val finalAvatar = uploadPageImageIfLocal(context, avatarUri) ?: return@launch
      val finalCover = uploadPageImageIfLocal(context, coverUri) ?: return@launch
      spacesRepository.createPage(name, category, bio, finalAvatar, finalCover)
        .onSuccess { _feedback.value = "Page created!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Failed to create page" }
    }
  }

  fun updatePage(
    pageId: String,
    name: String,
    category: String,
    bio: String,
    context: Context? = null,
    avatarUri: String = "",
    coverUri: String = "",
    onUpdated: () -> Unit = {}
  ) {
    if (name.trim().length < 2) {
      _feedback.value = "Page name must be at least 2 characters."
      return
    }
    viewModelScope.launch {
      val finalAvatar = uploadPageImageIfLocal(context, avatarUri) ?: return@launch
      val finalCover = uploadPageImageIfLocal(context, coverUri) ?: return@launch
      spacesRepository.updatePage(pageId, name, category, bio, finalAvatar, finalCover)
        .onSuccess {
          _feedback.value = "Page updated!"
          onUpdated()
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Failed to update page") }
    }
  }

  private suspend fun uploadPageImageIfLocal(context: Context?, rawUri: String): String? {
    val clean = rawUri.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("hellomedia://")) {
      return clean
    }
    if (context == null) return clean
    _isUploadingMedia.value = true
    val uploaded = socialRepository.uploadMedia(context, clean, "pages")
    _isUploadingMedia.value = false
    return uploaded.getOrElse { err ->
      _feedback.value = err.localizedMessage ?: "Page photo upload failed"
      null
    }
  }

  fun toggleFollowPage(page: PageEntity) {
    viewModelScope.launch {
      spacesRepository.toggleFollowPage(page)
        .onSuccess { nowFollowing ->
          _feedback.value = if (nowFollowing) "Joined ${page.name}" else "Out / Left ${page.name}"
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Failed to update page membership") }
    }
  }

  fun addPageMember(page: PageEntity, actor: PublicUser, targetUid: String, targetName: String) {
    viewModelScope.launch {
      spacesRepository.addMemberToPage(page, actor, targetUid)
        .onSuccess { _feedback.value = "$targetName added to ${page.name}" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Only Page admins can add members") }
    }
  }

  fun removePageMember(page: PageEntity, targetUid: String, targetName: String) {
    viewModelScope.launch {
      spacesRepository.removeMemberFromPage(page, targetUid)
        .onSuccess { _feedback.value = "Removed $targetName from ${page.name}" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Only Page admins can remove members") }
    }
  }

  fun togglePageAdmin(page: PageEntity, targetUid: String, targetName: String, makeAdmin: Boolean) {
    viewModelScope.launch {
      spacesRepository.togglePageAdmin(page.id, targetUid, makeAdmin)
        .onSuccess {
          _feedback.value = if (makeAdmin) "$targetName promoted to Page Admin" else "$targetName removed from Page Admins"
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not update Page admin") }
    }
  }

  fun deletePage(page: PageEntity) {
    viewModelScope.launch {
      spacesRepository.deletePage(page.id)
        .onSuccess { _feedback.value = "Page \"${page.name}\" deleted" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not delete Page") }
    }
  }

  // Events
  fun createEvent(title: String, desc: String, location: String, dateText: String, timeText: String, creatorName: String) {
    if (title.trim().length < 2 || location.isBlank()) {
      _feedback.value = "Enter an event title and location."
      return
    }
    viewModelScope.launch {
      spacesRepository.createEvent(title, desc, location, dateText, timeText, creatorName)
        .onSuccess { _feedback.value = "Event created!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Event creation failed" }
    }
  }

  fun rsvpEvent(event: EventEntity, mode: String) {
    viewModelScope.launch {
      spacesRepository.toggleEventRsvp(event, mode)
        .onFailure { _feedback.value = it.localizedMessage ?: "RSVP failed" }
    }
  }

  fun inviteToEvent(eventId: String, targetUid: String) {
    viewModelScope.launch {
      spacesRepository.inviteToEvent(eventId, targetUid)
        .onSuccess { _feedback.value = "Invitation sent!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Invite failed" }
    }
  }

  // Marketplace
  fun createListing(
    context: Context,
    seller: PublicUser,
    title: String,
    description: String,
    priceText: String,
    currency: String = "$",
    category: String,
    condition: String,
    locationText: String,
    imageUri: String
  ) {
    val price = priceText.toDoubleOrNull()
    if (title.trim().length < 2 || price == null || price < 0.0) {
      _feedback.value = "Enter a valid title and price."
      return
    }
    viewModelScope.launch {
      val uploadedUrl = if (imageUri.isNotBlank()) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, imageUri, "marketplace")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _feedback.value = err.localizedMessage ?: "Product photo upload failed"
          return@launch
        }
      } else {
        ""
      }
      spacesRepository.createListing(seller, title, description, price, currency, category, condition, locationText, uploadedUrl)
        .onSuccess { _feedback.value = "Listing published to Marketplace!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Listing failed" }
    }
  }

  fun updateListing(
    context: Context,
    listingId: String,
    title: String,
    description: String,
    priceText: String,
    currency: String,
    category: String,
    condition: String,
    locationText: String,
    imageUri: String,
    onUpdated: () -> Unit = {}
  ) {
    val price = priceText.toDoubleOrNull()
    if (title.trim().length < 2 || price == null || price < 0.0) {
      _feedback.value = "Enter a valid title and price."
      return
    }
    viewModelScope.launch {
      val finalImage = if (
        imageUri.isNotBlank() &&
        !imageUri.startsWith("http://") &&
        !imageUri.startsWith("https://") &&
        !imageUri.startsWith("hellomedia://")
      ) {
        _isUploadingMedia.value = true
        val uploadResult = socialRepository.uploadMedia(context, imageUri, "marketplace")
        _isUploadingMedia.value = false
        uploadResult.getOrElse { err ->
          _feedback.value = err.localizedMessage ?: "Product photo upload failed"
          return@launch
        }
      } else {
        imageUri
      }
      spacesRepository.updateListing(listingId, title, description, price, currency, category, condition, locationText, finalImage)
        .onSuccess {
          _feedback.value = "Listing updated!"
          onUpdated()
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Failed to update listing") }
    }
  }

  fun deleteListing(listingId: String) {
    viewModelScope.launch {
      spacesRepository.deleteListing(listingId)
        .onSuccess { _feedback.value = "Listing removed from Marketplace" }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not remove listing") }
    }
  }

  fun toggleListingSold(listing: MarketplaceListing) {
    val nextSold = listing.status != "sold"
    viewModelScope.launch {
      spacesRepository.markListingSold(listing.id, nextSold)
        .onSuccess { _feedback.value = if (nextSold) "Marked as Sold" else "Marked Available" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Failed to update listing" }
    }
  }

  // Secret Love
  fun sendSecretLove(
    sender: PublicUser,
    recipient: PublicUser,
    message: String,
    moodEmoji: String,
    revealNow: Boolean,
    onResult: (Result<SecretLoveSendOutcome>) -> Unit = {}
  ) {
    if (message.isBlank()) {
      _feedback.value = "Write a Secret Love message first."
      return
    }
    viewModelScope.launch {
      _isSendingSecretLove.value = true
      val res = governanceRepository.sendSecretLove(sender, recipient, message, moodEmoji, revealNow)
      _isSendingSecretLove.value = false
      res.onSuccess {
        _feedback.value = if (revealNow) {
          "Love note sent with your name!"
        } else {
          "Anonymous Secret Love sent ❤️"
        }
      }.onFailure {
        _feedback.value = it.unwrapFirestoreMessage("Could not send Secret Love")
      }
      onResult(res)
    }
  }

  fun toggleSecretCrush(recipient: PublicUser) {
    viewModelScope.launch {
      val alreadyAdded = recipient.userId in mySecretCrushes.value
      if (alreadyAdded) {
        governanceRepository.removeSecretCrush(recipient.userId)
          .onSuccess { _feedback.value = "Removed from your private Secret Crush list." }
          .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not update Secret Crush") }
      } else {
        governanceRepository.signalSecretCrush(recipient)
          .onSuccess { _feedback.value = "Added privately to your Secret Crush list 🔒" }
          .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not add Secret Crush") }
      }
    }
  }

  fun revealSecretLove(sent: SentSecretLove, sender: PublicUser) {
    viewModelScope.launch {
      governanceRepository.revealSecretLoveIdentity(sent, sender)
        .onSuccess { _feedback.value = "Your identity has been revealed to ${sent.recipientName}." }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not reveal identity") }
    }
  }

  fun markSecretLoveRead(messageId: String) {
    viewModelScope.launch { governanceRepository.markSecretLoveRead(messageId) }
  }

  fun blockOrReportSecretLove(messageId: String, block: Boolean, reportReason: String?) {
    viewModelScope.launch {
      governanceRepository.blockOrReportSecretLove(messageId, block, reportReason)
        .onSuccess {
          _feedback.value = if (reportReason != null) "Message reported & hidden." else "Message blocked."
        }
        .onFailure { _feedback.value = it.localizedMessage ?: "Action failed" }
    }
  }

  fun observePrivateMatchMessages(matchId: String): Flow<List<PrivateCrushMessage>> =
    governanceRepository.observePrivateMatchMessages(matchId)

  fun sendPrivateMatchMessage(
    matchId: String,
    text: String,
    moodEmoji: String = "💌",
    onSent: () -> Unit = {}
  ) {
    if (text.isBlank()) return
    viewModelScope.launch {
      governanceRepository.sendPrivateMatchMessage(matchId, text, moodEmoji)
        .onSuccess { onSent() }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not send private message") }
    }
  }

  // Pure Love Quest
  fun startOrUpdatePureLoveQuest(recipient: PublicUser, targetTierDays: Int, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      governanceRepository.startOrUpdateDevotionTrialTarget(recipient, targetTierDays)
        .onSuccess { trial ->
          _feedback.value = "Pure Love Quest set to ${trial.targetTierDays} days for ${trial.recipientName}."
          onDone()
        }
        .onFailure { _feedback.value = it.unwrapFirestoreMessage("Could not start Pure Love Quest") }
    }
  }

  fun submitDailyPureLoveCheckIn(
    recipient: PublicUser,
    targetTierDays: Int,
    reflectionText: String,
    onCompleted: (PureLoveTier?) -> Unit = {}
  ) {
    viewModelScope.launch {
      governanceRepository.completeDailyDevotionCheckIn(recipient, targetTierDays, reflectionText)
        .onSuccess { unlockedTier ->
          if (unlockedTier != null) {
            _latestBadgeUnlocked.value = unlockedTier
            _feedback.value = "${unlockedTier.badgeEmoji} Badge Unlocked: ${unlockedTier.badgeTitle} (${unlockedTier.label})!"
          } else {
            _feedback.value = "Daily Pure Love check-in sealed! Discipline verified."
          }
          onCompleted(unlockedTier)
        }
        .onFailure {
          _feedback.value = it.unwrapFirestoreMessage("Check-in failed")
        }
    }
  }

  // Owner Governance (Enforced server-side by firestore.rules)
  fun updateOwnerProfile(
    name: String,
    handle: String,
    avatar: String,
    about: String,
    email: String,
    info: String,
    showContactEmail: Boolean = false,
    showOwnerHandle: Boolean = true
  ) {
    viewModelScope.launch {
      governanceRepository.updateOwnerProfile(name, handle, avatar, about, email, info, showContactEmail, showOwnerHandle)
        .onSuccess { _feedback.value = "Owner profile updated!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Owner access required" }
    }
  }

  fun toggleOwnerHandleVisibility(showPublic: Boolean, onRequiresFreshAuth: () -> Unit = {}) {
    viewModelScope.launch {
      governanceRepository.setOwnerVisibility(showOwnerHandle = showPublic)
        .onSuccess {
          _feedback.value = if (showPublic) {
            "Owner @handle is now Public"
          } else {
            "Owner @handle is now Hidden from other profiles"
          }
        }
        .onFailure { err ->
          val msg = err.localizedMessage ?: "Owner access required"
          if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("recent", ignoreCase = true)) {
            onRequiresFreshAuth()
          }
          _feedback.value = "Verify recent Google sign-in (<15m) to update Owner visibility"
        }
    }
  }

  fun toggleOwnerEmailVisibility(showPublic: Boolean, onRequiresFreshAuth: () -> Unit = {}) {
    viewModelScope.launch {
      governanceRepository.setOwnerVisibility(showContactEmail = showPublic)
        .onSuccess {
          _feedback.value = if (showPublic) {
            "Owner Contact Gmail is now Public"
          } else {
            "Owner Contact Gmail is now Hidden"
          }
        }
        .onFailure { err ->
          val msg = err.localizedMessage ?: "Owner access required"
          if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("recent", ignoreCase = true)) {
            onRequiresFreshAuth()
          }
          _feedback.value = "Verify recent Google sign-in (<15m) to update Owner visibility"
        }
    }
  }

  fun updateGlobalSettings(
    appName: String,
    tagline: String,
    maintenanceBanner: String,
    sosEnabled: Boolean,
    secretLoveEnabled: Boolean,
    allowRegistrations: Boolean
  ) {
    viewModelScope.launch {
      governanceRepository.updateGlobalSettings(
        appName, tagline, maintenanceBanner, sosEnabled, secretLoveEnabled, allowRegistrations
      ).onSuccess { _feedback.value = "Global settings saved!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Owner access required" }
    }
  }

  fun addAdmin(target: PublicUser) {
    viewModelScope.launch {
      governanceRepository.addAdmin(target)
        .onSuccess { _feedback.value = "${target.displayName} promoted to Admin." }
        .onFailure { _feedback.value = it.localizedMessage ?: "Only Owner can add admins" }
    }
  }

  fun removeAdmin(uid: String) {
    viewModelScope.launch {
      governanceRepository.removeAdmin(uid)
        .onSuccess { _feedback.value = "Admin removed." }
        .onFailure { _feedback.value = it.localizedMessage ?: "Only Owner can remove admins" }
    }
  }

  fun resolveReport(reportId: String, status: String) {
    viewModelScope.launch {
      governanceRepository.resolveReport(reportId, status)
        .onSuccess { _feedback.value = "Report marked $status" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Moderation failed" }
    }
  }

  fun deleteReportedPost(postId: String) {
    viewModelScope.launch {
      governanceRepository.removePostAsModerator(postId)
        .onSuccess { _feedback.value = "Reported post removed" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Failed to remove post" }
    }
  }
}
