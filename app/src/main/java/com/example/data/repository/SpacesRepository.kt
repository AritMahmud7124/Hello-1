package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.EventEntity
import com.example.data.model.Group
import com.example.data.model.MarketplaceListing
import com.example.data.model.OperationType
import com.example.data.model.PageEntity
import com.example.data.model.PublicUser
import com.example.data.model.SosAlert
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SpacesRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  // Groups
  fun observePublicGroups(): Flow<List<Group>> =
    db.collection("groups")
      .whereEqualTo("privacy", "public")
      .snapshots()
      .map { snap -> snap.toObjects(Group::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "groups(public)")
        throw e
      }

  fun observeMyGroups(userId: String): Flow<List<Group>> =
    db.collection("groups")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap -> snap.toObjects(Group::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "groups(memberUids)")
        throw e
      }

  suspend fun createGroup(
    name: String,
    description: String,
    category: String,
    privacy: String,
    sosEnabled: Boolean
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("groups").document()
    ref.set(
      mapOf(
        "name" to name.trim().take(100),
        "description" to description.trim().take(600),
        "category" to category.trim().ifBlank { "Community" }.take(60),
        "privacy" to if (privacy in listOf("public", "private")) privacy else "public",
        "creatorId" to uid,
        "adminUids" to listOf(uid),
        "memberUids" to listOf(uid),
        "membersCount" to 1L,
        "sosEnabled" to sosEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "groups")
  }

  suspend fun joinOrLeaveGroup(group: Group, join: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("groups").document(group.id)
    ref.update(
      mapOf(
        "memberUids" to if (join) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid),
        "membersCount" to FieldValue.increment(if (join) 1L else -1L),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "groups/${group.id}")
  }

  suspend fun inviteMemberToGroup(group: Group, actor: PublicUser, targetUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("groups").document(group.id).update(
      mapOf(
        "memberUids" to FieldValue.arrayUnion(targetUid),
        "membersCount" to FieldValue.increment(1L),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    db.collection("users").document(targetUid).collection("notifications").document().set(
      mapOf(
        "recipientId" to targetUid,
        "actorId" to uid,
        "actorName" to actor.displayName.take(80),
        "type" to "group_invite",
        "title" to "Added to ${group.name}",
        "body" to "${actor.displayName} added you to ${group.name}.",
        "targetId" to group.id,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "groups/${group.id}")
  }

  // Group Emergency SOS Alerts (strictly private to sender + selected recipientUids)
  fun observeMySentSosAlerts(groupId: String, userId: String): Flow<List<SosAlert>> =
    db.collection("groups").document(groupId).collection("sos_alerts")
      .whereEqualTo("senderId", userId)
      .snapshots()
      .map { snap -> snap.toObjects(SosAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "groups/$groupId/sos_alerts(sent)")
        throw e
      }

  fun observeIncomingSosAlerts(groupId: String, userId: String): Flow<List<SosAlert>> =
    db.collection("groups").document(groupId).collection("sos_alerts")
      .whereArrayContains("recipientUids", userId)
      .snapshots()
      .map { snap -> snap.toObjects(SosAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "groups/$groupId/sos_alerts(incoming)")
        throw e
      }

  suspend fun sendGroupSosAlert(
    group: Group,
    sender: PublicUser,
    message: String,
    recipientUids: List<String>,
    shareLiveLocation: Boolean,
    latitude: Double,
    longitude: Double
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val cleanRecipients = recipientUids.filter { it != uid }.distinct().take(50)
    require(cleanRecipients.isNotEmpty()) { "Select at least one group member to receive SOS." }
    val ref = db.collection("groups").document(group.id).collection("sos_alerts").document()
    val payload = buildMap<String, Any> {
      put("groupId", group.id)
      put("senderId", uid)
      put("senderName", sender.displayName.ifBlank { "Member" }.take(80))
      put("message", message.trim().ifBlank { "Emergency SOS triggered!" }.take(500))
      put("recipientUids", cleanRecipients)
      put("shareLiveLocation", shareLiveLocation)
      if (shareLiveLocation) {
        put("latitude", latitude)
        put("longitude", longitude)
      } else {
        put("latitude", 0.0)
        put("longitude", 0.0)
      }
      put("status", "active")
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }
    ref.set(payload).await()

    cleanRecipients.forEach { targetUid ->
      runCatching {
        db.collection("users").document(targetUid).collection("notifications").document().set(
          mapOf(
            "recipientId" to targetUid,
            "actorId" to uid,
            "actorName" to sender.displayName.take(80),
            "type" to "sos_alert",
            "title" to "SOS Alert in ${group.name}",
            "body" to message.trim().ifBlank { "Urgent help requested!" }.take(300),
            "targetId" to group.id,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    }
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "groups/${group.id}/sos_alerts")
  }

  suspend fun stopOrCancelSosAlert(groupId: String, alertId: String, newStatus: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("groups").document(groupId).collection("sos_alerts").document(alertId).update(
      mapOf(
        "shareLiveLocation" to false,
        "latitude" to 0.0,
        "longitude" to 0.0,
        "status" to newStatus,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId/sos_alerts/$alertId")
  }

  // Pages
  fun observePages(): Flow<List<PageEntity>> =
    db.collection("pages")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(PageEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "pages")
        throw e
      }

  suspend fun createPage(name: String, category: String, bio: String): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("pages").document()
    val safeCategory = category.trim().let { if (it.length < 2) "Creator" else it }.take(60)
    ref.set(
      mapOf(
        "name" to name.trim().take(100),
        "category" to safeCategory,
        "bio" to bio.trim().take(600),
        "creatorId" to uid,
        "adminUids" to listOf(uid),
        "followerUids" to listOf(uid),
        "followersCount" to 1L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "pages")
  }

  suspend fun toggleFollowPage(page: PageEntity): Result<Unit> = runCatching {
    val uid = requireUserId()
    val isFollowing = uid in page.followerUids
    db.collection("pages").document(page.id).update(
      mapOf(
        "followerUids" to if (isFollowing) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
        "followersCount" to FieldValue.increment(if (isFollowing) -1L else 1L),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "pages/${page.id}")
  }

  // Events
  fun observeEvents(): Flow<List<EventEntity>> =
    db.collection("events")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(EventEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "events")
        throw e
      }

  suspend fun createEvent(
    title: String,
    description: String,
    locationName: String,
    dateText: String,
    timeText: String,
    creatorName: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("events").document()
    ref.set(
      mapOf(
        "title" to title.trim().take(120),
        "description" to description.trim().take(800),
        "locationName" to locationName.trim().ifBlank { "Online" }.take(160),
        "eventDateText" to dateText.trim().ifBlank { "TBD" }.take(40),
        "eventTimeText" to timeText.trim().ifBlank { "TBD" }.take(40),
        "creatorId" to uid,
        "creatorName" to creatorName.trim().ifBlank { "Member" }.take(80),
        "goingUids" to listOf(uid),
        "interestedUids" to emptyList<String>(),
        "invitedUids" to emptyList<String>(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "events")
  }

  suspend fun toggleEventRsvp(event: EventEntity, mode: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("events").document(event.id)
    val updates = when (mode) {
      "going" -> mapOf(
        "goingUids" to if (uid in event.goingUids) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
        "interestedUids" to FieldValue.arrayRemove(uid),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      else -> mapOf(
        "interestedUids" to if (uid in event.interestedUids) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
        "goingUids" to FieldValue.arrayRemove(uid),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    }
    ref.update(updates).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "events/${event.id}")
  }

  suspend fun inviteToEvent(eventId: String, targetUid: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("events").document(eventId).update(
      mapOf(
        "invitedUids" to FieldValue.arrayUnion(targetUid),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "events/$eventId")
  }

  // Marketplace
  fun observeMarketplaceListings(): Flow<List<MarketplaceListing>> =
    db.collection("marketplace_listings")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "marketplace_listings")
        throw e
      }

  suspend fun createListing(
    seller: PublicUser,
    title: String,
    description: String,
    price: Double,
    category: String,
    condition: String,
    locationText: String,
    imageUri: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("marketplace_listings").document()
    ref.set(
      mapOf(
        "sellerId" to uid,
        "sellerName" to seller.displayName.ifBlank { "Seller" }.take(80),
        "sellerAvatar" to seller.photoUrl.take(1000),
        "title" to title.trim().take(120),
        "description" to description.trim().take(1000),
        "price" to price.coerceAtLeast(0.0),
        "currency" to "$",
        "category" to category.trim().ifBlank { "General" }.take(50),
        "condition" to condition.trim().ifBlank { "Good" }.take(40),
        "locationText" to locationText.trim().take(100),
        "imageUri" to imageUri.trim().take(2000),
        "status" to "available",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    ref.id
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "marketplace_listings")
  }

  suspend fun markListingSold(listingId: String, sold: Boolean): Result<Unit> = runCatching {
    requireUserId()
    db.collection("marketplace_listings").document(listingId).update(
      mapOf(
        "status" to if (sold) "sold" else "available",
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "marketplace_listings/$listingId")
  }
}
