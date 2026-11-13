package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRecord
import com.example.data.model.EventEntity
import com.example.data.model.GlobalSettings
import com.example.data.model.Group
import com.example.data.model.MarketplaceListing
import com.example.data.model.OwnerConfig
import com.example.data.model.PageEntity
import com.example.data.model.PublicUser
import com.example.data.model.ReportEntity
import com.example.data.model.SecretLoveMessage
import com.example.data.model.SentSecretLove
import com.example.data.model.SosAlert
import com.example.data.model.UiState
import com.example.data.model.unwrapFirestoreMessage
import com.example.data.repository.GovernanceRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SpacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    .map { cfg -> cfg != null && cfg.ownerUid == currentUserId }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

  val admins: StateFlow<List<AdminRecord>> = governanceRepository.observeAdmins()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val reports: StateFlow<List<ReportEntity>> = governanceRepository.observeReports()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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
        .onFailure { _feedback.value = it.localizedMessage ?: "Membership update failed" }
    }
  }

  fun inviteMember(group: Group, actor: PublicUser, targetUid: String) {
    viewModelScope.launch {
      spacesRepository.inviteMemberToGroup(group, actor, targetUid)
        .onSuccess { _feedback.value = "Member added to ${group.name}" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Invite failed" }
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
    longitude: Double
  ) {
    viewModelScope.launch {
      spacesRepository.sendGroupSosAlert(group, sender, message, recipientUids, shareLiveLocation, latitude, longitude)
        .onSuccess {
          _feedback.value = if (shareLiveLocation) {
            "SOS Alert & private live location sent to ${recipientUids.size} member(s)."
          } else {
            "SOS Alert sent to ${recipientUids.size} member(s) without location."
          }
        }
        .onFailure { _feedback.value = it.localizedMessage ?: "SOS dispatch failed" }
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
  fun createPage(name: String, category: String, bio: String) {
    if (name.trim().length < 2) {
      _feedback.value = "Page name must be at least 2 characters."
      return
    }
    viewModelScope.launch {
      spacesRepository.createPage(name, category, bio)
        .onSuccess { _feedback.value = "Page created!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Failed to create page" }
    }
  }

  fun toggleFollowPage(page: PageEntity) {
    viewModelScope.launch {
      spacesRepository.toggleFollowPage(page)
        .onFailure { _feedback.value = it.localizedMessage ?: "Failed to update follow" }
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
      spacesRepository.createListing(seller, title, description, price, category, condition, locationText, uploadedUrl)
        .onSuccess { _feedback.value = "Listing published to Marketplace!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Listing failed" }
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
    revealNow: Boolean
  ) {
    if (message.isBlank()) {
      _feedback.value = "Write a Secret Love message first."
      return
    }
    viewModelScope.launch {
      governanceRepository.sendSecretLove(sender, recipient, message, moodEmoji, revealNow)
        .onSuccess {
          _feedback.value = if (revealNow) {
            "Love note sent with your name!"
          } else {
            "Anonymous Secret Love sent ❤️"
          }
        }
        .onFailure { _feedback.value = it.localizedMessage ?: "Could not send Secret Love" }
    }
  }

  fun revealSecretLove(sent: SentSecretLove, sender: PublicUser) {
    viewModelScope.launch {
      governanceRepository.revealSecretLoveIdentity(sent, sender)
        .onSuccess { _feedback.value = "Your identity has been revealed to ${sent.recipientName}." }
        .onFailure { _feedback.value = it.localizedMessage ?: "Could not reveal identity" }
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

  // Owner Governance (Enforced server-side by firestore.rules)
  fun updateOwnerProfile(name: String, handle: String, avatar: String, about: String, email: String, info: String) {
    viewModelScope.launch {
      governanceRepository.updateOwnerProfile(name, handle, avatar, about, email, info)
        .onSuccess { _feedback.value = "Owner profile updated!" }
        .onFailure { _feedback.value = it.localizedMessage ?: "Owner access required" }
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
