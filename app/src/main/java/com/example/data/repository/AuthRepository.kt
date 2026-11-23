package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.OperationType
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.handleFirestoreError
import com.example.security.DeviceIntegrityReport
import com.example.security.EncryptedVaultStorage
import com.example.security.SecurityRiskLevel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  private val profileMutex = Mutex()

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun currentUser(): FirebaseUser? = auth.currentUser

  fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  suspend fun attemptAutoSignIn(context: Context): Boolean {
    if (auth.currentUser != null) return true
    val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
      ?: return false
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()
    return try {
      val result = credentialManager.getCredential(context, request)
      handleCredentialResult(result.credential)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
      false
    }
  }

  suspend fun signInWithGoogle(activity: Activity): AuthSignInResult {
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return AuthSignInResult.Error("Google Sign-In configuration missing: default_web_client_id not found")
    }
    val credentialManager = CredentialManager.create(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()
    return try {
      val result = credentialManager.getCredential(activity, request)
      if (handleCredentialResult(result.credential)) {
        AuthSignInResult.Success
      } else {
        AuthSignInResult.Error("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      AuthSignInResult.Cancelled
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      AuthSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed")
    }
  }

  private suspend fun handleCredentialResult(credential: androidx.credentials.Credential): Boolean {
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      return withContext(NonCancellable) {
        auth.signInWithCredential(authCredential).await()
        ensureUserProfile()
        true
      }
    }
    return false
  }

  suspend fun refreshAndVerifyIdToken(vault: EncryptedVaultStorage? = null): Result<Long> = runCatching {
    val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
    val tokenResult = user.getIdToken(true).await()
    val token = tokenResult.token.orEmpty()
    val signInProvider = tokenResult.signInProvider.orEmpty()
    if (signInProvider == "anonymous") {
      throw IllegalStateException("Anonymous sessions are prohibited.")
    }
    val authTimestampSec = tokenResult.authTimestamp
    val verifiedAtMs = System.currentTimeMillis()
    if (token.isNotBlank()) {
      vault?.storeVerifiedSession(
        uid = user.uid,
        rawIdToken = token,
        verifiedAtMs = verifiedAtMs
      )
    }
    authTimestampSec
  }

  suspend fun reauthenticateWithGoogle(
    activity: Activity,
    vault: EncryptedVaultStorage? = null
  ): AuthSignInResult {
    val user = auth.currentUser ?: return AuthSignInResult.Error("No active session to re-authenticate.")
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      return AuthSignInResult.Error("Google Sign-In configuration missing: default_web_client_id not found")
    }
    val credentialManager = CredentialManager.create(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()
    return try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        withContext(NonCancellable) {
          user.reauthenticate(authCredential).await()
          refreshAndVerifyIdToken(vault)
        }
        AuthSignInResult.Success
      } else {
        AuthSignInResult.Error("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Re-authentication cancelled: ${e.message}")
      AuthSignInResult.Cancelled
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Google re-authentication failed", e)
      AuthSignInResult.Error(e.localizedMessage ?: "Re-authentication failed")
    }
  }

  suspend fun syncSecurityState(
    failedAttempts: Int,
    lockoutUntilMs: Long,
    lastAttemptMs: Long,
    biometricEnabled: Boolean,
    integrityReport: DeviceIntegrityReport
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("security_state").document("auth_guard")
    docRef.set(
      mapOf(
        "userId" to uid,
        "failedAttempts" to failedAttempts.coerceIn(0, 1000).toLong(),
        "lockoutUntilMs" to lockoutUntilMs.coerceAtLeast(0L),
        "lastAttemptMs" to lastAttemptMs.coerceAtLeast(0L),
        "biometricEnabled" to biometricEnabled,
        "integrityRiskLevel" to integrityReport.riskLevel.name,
        "integritySummary" to integrityReport.summary.take(400),
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/security_state/auth_guard")
  }

  suspend fun fetchBackendLockout(): Pair<Int, Long>? = runCatching {
    val uid = auth.currentUser?.uid ?: return@runCatching null
    val snap = db.collection("users").document(uid).collection("security_state").document("auth_guard").get().await()
    if (!snap.exists()) return@runCatching null
    val attempts = (snap.get("failedAttempts") as? Number)?.toInt() ?: 0
    val lockoutUntil = (snap.get("lockoutUntilMs") as? Number)?.toLong() ?: 0L
    attempts to lockoutUntil
  }.getOrNull()

  suspend fun logSecurityEvent(
    eventType: String,
    detail: String,
    integritySummary: String
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val eventRef = db.collection("users").document(uid).collection("security_events").document()
    eventRef.set(
      mapOf(
        "userId" to uid,
        "eventType" to eventType,
        "detail" to detail.trim().ifBlank { "Security checkpoint" }.take(300),
        "integritySummary" to integritySummary.trim().ifBlank { "Verified" }.take(400),
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/security_events")
  }

  suspend fun signOut(context: Context, vault: EncryptedVaultStorage? = null) {
    withContext(NonCancellable) {
      val uid = auth.currentUser?.uid
      if (uid != null) {
        runCatching {
          db.collection("users_public").document(uid).update(
            mapOf(
              "isOnline" to false,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
      vault?.clearSessionBinding()
      val credentialManager = CredentialManager.create(context)
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        auth.signOut()
      }
    }
  }

  suspend fun ensureProfileReady(): Result<Unit> = runCatching {
    ensureUserProfile()
  }

  suspend fun ensureUserProfile() = profileMutex.withLock {
    val user = auth.currentUser ?: return@withLock
    val uid = user.uid
    val publicRef = db.collection("users_public").document(uid)
    val privateRef = db.collection("users").document(uid)

    try {
      val publicSnap = publicRef.get().await()
      val displayName = user.displayName?.trim()?.takeIf { it.isNotBlank() }?.take(80) ?: "Hello Member"
      val baseHandle = "@" + (user.email?.substringBefore("@") ?: uid.take(8))
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "")
        .take(24)
        .ifBlank { "hello_${uid.take(6)}" }
      val safePhotoUrl = (user.photoUrl?.toString() ?: "").trim().take(1000)
      val safeEmail = (user.email ?: "").trim().take(160)

      if (!publicSnap.exists()) {
        val publicPayload = mapOf(
          "userId" to uid,
          "displayName" to displayName,
          "handle" to baseHandle.take(40),
          "bio" to "Hey there! I'm on HELLO.",
          "photoUrl" to safePhotoUrl,
          "coverUrl" to "",
          "isOnline" to true,
          "showOnlineStatus" to true,
          "profilePrivacy" to "public",
          "acceptSecretLove" to true,
          "friendsCount" to 0L,
          "followersCount" to 0L,
          "isVerifiedOwner" to false,
          "role" to "user",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        publicRef.set(publicPayload).await()
      } else {
        val showOnline = publicSnap.getBoolean("showOnlineStatus") ?: true
        publicRef.update(
          mapOf(
            "isOnline" to showOnline,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      val privateSnap = privateRef.get().await()
      if (!privateSnap.exists()) {
        val privatePayload = mapOf(
          "userId" to uid,
          "email" to safeEmail,
          "phone" to "",
          "fcmToken" to "",
          "notifLikes" to true,
          "notifComments" to true,
          "notifFriends" to true,
          "notifMessages" to true,
          "notifSecretLove" to true,
          "notifSos" to true,
          "isDeactivated" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        privateRef.set(privatePayload).await()
      }
      syncFcmToken()

      // Bootstrap owner profile if this verified Google account is the configured owner email
      // and app_config/owner has not yet been claimed.
      if (user.email == BOOTSTRAP_OWNER_EMAIL && user.isEmailVerified) {
        val ownerRef = db.collection("app_config").document("owner")
        val ownerSnap = ownerRef.get().await()
        if (!ownerSnap.exists()) {
          ownerRef.set(
            mapOf(
              "ownerUid" to uid,
              "ownerName" to displayName,
              "ownerHandle" to baseHandle.take(40),
              "showOwnerHandle" to true,
              "ownerAvatar" to safePhotoUrl,
              "about" to "Founder & Owner of HELLO Social Network.",
              "contactEmail" to safeEmail,
              "showContactEmail" to false,
              "contactInfo" to "Verified platform owner account.",
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
          db.collection("app_config").document("global_settings").set(
            mapOf(
              "appName" to "HELLO",
              "tagline" to "Say Hello to your real circle.",
              "maintenanceBanner" to "",
              "sosFeatureEnabled" to true,
              "secretLoveEnabled" to true,
              "allowNewRegistrations" to true,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
          publicRef.update(
            mapOf(
              "isVerifiedOwner" to true,
              "role" to "owner",
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users_public/$uid")
      throw e
    }
  }

  suspend fun syncFcmToken() {
    val uid = auth.currentUser?.uid ?: return
    runCatching {
      val token = FirebaseMessaging.getInstance().token.await()
      if (token.isNotBlank()) {
        db.collection("users").document(uid).update(
          mapOf(
            "fcmToken" to token.take(512),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    }
  }

  fun observePublicUser(uid: String): Flow<PublicUser?> =
    db.collection("users_public").document(uid)
      .snapshots()
      .map { snap -> snap.toObject(PublicUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

  fun observePrivateUser(uid: String): Flow<PrivateUser?> =
    db.collection("users").document(uid)
      .snapshots()
      .map { snap -> snap.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

  suspend fun setPresenceOnline(isForeground: Boolean): Result<Unit> = runCatching {
    val uid = auth.currentUser?.uid ?: return@runCatching
    val ref = db.collection("users_public").document(uid)
    val snap = ref.get().await()
    if (!snap.exists()) return@runCatching
    val showOnline = snap.getBoolean("showOnlineStatus") ?: true
    val targetOnline = isForeground && showOnline
    val currentOnline = snap.getBoolean("isOnline")
    if (currentOnline == targetOnline) return@runCatching
    ref.update(
      mapOf(
        "isOnline" to targetOnline,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "users_public/${auth.currentUser?.uid}")
  }

  suspend fun setShowOnlineStatus(enabled: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users_public").document(uid)
    if (!ref.get().await().exists()) {
      ensureUserProfile()
    }
    ref.update(
      mapOf(
        "showOnlineStatus" to enabled,
        "isOnline" to enabled,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "users_public/${auth.currentUser?.uid}")
  }

  suspend fun updatePublicProfile(
    displayName: String,
    bio: String,
    photoUrl: String,
    coverUrl: String,
    profilePrivacy: String,
    acceptSecretLove: Boolean,
    showOnlineStatus: Boolean
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users_public").document(uid)
    if (!ref.get().await().exists()) {
      ensureUserProfile()
    }
    val safeName = displayName.trim().ifBlank { "Hello Member" }.take(80)
    val safePrivacy = if (profilePrivacy in listOf("public", "friends", "private")) profilePrivacy else "public"
    ref.update(
      mapOf(
        "displayName" to safeName,
        "bio" to bio.trim().take(300),
        "photoUrl" to photoUrl.trim().take(1000),
        "coverUrl" to coverUrl.trim().take(1000),
        "profilePrivacy" to safePrivacy,
        "acceptSecretLove" to acceptSecretLove,
        "showOnlineStatus" to showOnlineStatus,
        "isOnline" to showOnlineStatus,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "users_public/${auth.currentUser?.uid}")
  }

  suspend fun updateNotificationPrefs(
    notifLikes: Boolean,
    notifComments: Boolean,
    notifFriends: Boolean,
    notifMessages: Boolean,
    notifSecretLove: Boolean,
    notifSos: Boolean,
    email: String,
    phone: String,
    isDeactivated: Boolean
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanEmail = email.trim().take(160)
    val cleanPhone = phone.trim().take(40)
    if (cleanEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      throw IllegalArgumentException("Enter a valid recovery email address.")
    }
    if (cleanPhone.isNotBlank() && cleanPhone.count { it.isDigit() } < 6) {
      throw IllegalArgumentException("Enter a valid phone number with at least 6 digits.")
    }

    val ref = db.collection("users").document(uid)
    val existing = runCatching { ref.get().await() }.getOrNull()
    if (existing == null || !existing.exists()) {
      ref.set(
        mapOf(
          "userId" to uid,
          "email" to cleanEmail,
          "phone" to cleanPhone,
          "fcmToken" to "",
          "notifLikes" to notifLikes,
          "notifComments" to notifComments,
          "notifFriends" to notifFriends,
          "notifMessages" to notifMessages,
          "notifSecretLove" to notifSecretLove,
          "notifSos" to notifSos,
          "isDeactivated" to isDeactivated,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } else {
      ref.update(
        mapOf(
          "notifLikes" to notifLikes,
          "notifComments" to notifComments,
          "notifFriends" to notifFriends,
          "notifMessages" to notifMessages,
          "notifSecretLove" to notifSecretLove,
          "notifSos" to notifSos,
          "email" to cleanEmail,
          "phone" to cleanPhone,
          "isDeactivated" to isDeactivated,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
  }

  suspend fun deleteOwnAccountData(): Result<Unit> = runCatching {
    val uid = requireUserId()
    db.collection("users").document(uid).delete().await()
    auth.currentUser?.delete()?.await()
    Unit
  }.onFailure { e ->
    if (e is CancellationException) throw e
    handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}")
  }

  companion object {
    private const val TAG = "AuthRepository"
    private const val BOOTSTRAP_OWNER_EMAIL = "helloarit123@gmail.com"
  }
}

sealed interface AuthSignInResult {
  data object Success : AuthSignInResult
  data object Cancelled : AuthSignInResult
  data class Error(val message: String) : AuthSignInResult
}
