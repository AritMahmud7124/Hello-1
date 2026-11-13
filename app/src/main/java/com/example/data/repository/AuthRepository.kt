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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
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
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      AuthSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed")
    }
  }

  private suspend fun handleCredentialResult(credential: androidx.credentials.Credential): Boolean {
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      auth.signInWithCredential(authCredential).await()
      ensureUserProfile()
      return true
    }
    return false
  }

  suspend fun signOut(context: Context) {
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

  suspend fun ensureUserProfile() {
    val user = auth.currentUser ?: return
    val uid = user.uid
    val publicRef = db.collection("users_public").document(uid)
    val privateRef = db.collection("users").document(uid)

    try {
      val publicSnap = publicRef.get().await()
      val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Hello Member"
      val baseHandle = "@" + (user.email?.substringBefore("@") ?: uid.take(8))
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "")
        .take(24)
        .ifBlank { "hello_${uid.take(6)}" }

      if (!publicSnap.exists()) {
        val publicPayload = mapOf(
          "userId" to uid,
          "displayName" to displayName.take(80),
          "handle" to baseHandle,
          "bio" to "Hey there! I'm on HELLO.",
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "coverUrl" to "",
          "isOnline" to true,
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
        publicRef.update(
          mapOf(
            "isOnline" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      val privateSnap = privateRef.get().await()
      if (!privateSnap.exists()) {
        val privatePayload = mapOf(
          "userId" to uid,
          "email" to (user.email ?: ""),
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
              "ownerName" to displayName.take(80),
              "ownerHandle" to baseHandle,
              "ownerAvatar" to (user.photoUrl?.toString() ?: ""),
              "about" to "Founder & Owner of HELLO Social Network.",
              "contactEmail" to (user.email ?: ""),
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

  suspend fun updatePublicProfile(
    displayName: String,
    bio: String,
    photoUrl: String,
    coverUrl: String,
    profilePrivacy: String,
    acceptSecretLove: Boolean,
    isOnline: Boolean
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users_public").document(uid)
    ref.update(
      mapOf(
        "displayName" to displayName.trim().take(80),
        "bio" to bio.trim().take(300),
        "photoUrl" to photoUrl.trim().take(1000),
        "coverUrl" to coverUrl.trim().take(1000),
        "profilePrivacy" to profilePrivacy,
        "acceptSecretLove" to acceptSecretLove,
        "isOnline" to isOnline,
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
    phone: String,
    isDeactivated: Boolean
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users").document(uid)
    ref.update(
      mapOf(
        "notifLikes" to notifLikes,
        "notifComments" to notifComments,
        "notifFriends" to notifFriends,
        "notifMessages" to notifMessages,
        "notifSecretLove" to notifSecretLove,
        "notifSos" to notifSos,
        "phone" to phone.trim().take(40),
        "isDeactivated" to isDeactivated,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
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
