package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class EncryptedVaultStorage(context: Context) {
  private val appContext = context.applicationContext
  private val memoryFallback = ConcurrentHashMap<String, String>()

  private val initOutcome: Pair<SharedPreferences?, Boolean> by lazy {
    try {
      val masterKey = buildHardwareMasterKey(appContext)
      val prefs = EncryptedSharedPreferences.create(
        appContext,
        VAULT_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
      prefs to true
    } catch (e: Exception) {
      Log.w(TAG, "Hardware Keystore vault unavailable in current runtime; using ephemeral memory vault: ${e.message}")
      null to false
    }
  }

  private val prefs: SharedPreferences?
    get() = initOutcome.first

  val isHardwareBacked: Boolean
    get() = initOutcome.second

  var isBiometricLockEnabled: Boolean
    get() = getBoolean(KEY_BIOMETRIC_LOCK, defaultValue = false)
    set(value) = putBoolean(KEY_BIOMETRIC_LOCK, value)

  var isStrictTamperModeEnabled: Boolean
    get() = getBoolean(KEY_STRICT_TAMPER_MODE, defaultValue = false)
    set(value) = putBoolean(KEY_STRICT_TAMPER_MODE, value)

  var failedAuthAttempts: Int
    get() = getInt(KEY_FAILED_ATTEMPTS, defaultValue = 0)
    set(value) = putInt(KEY_FAILED_ATTEMPTS, value.coerceAtLeast(0))

  var lockoutUntilEpochMs: Long
    get() = getLong(KEY_LOCKOUT_UNTIL_MS, defaultValue = 0L)
    set(value) = putLong(KEY_LOCKOUT_UNTIL_MS, value.coerceAtLeast(0L))

  var lastAttemptEpochMs: Long
    get() = getLong(KEY_LAST_ATTEMPT_MS, defaultValue = 0L)
    set(value) = putLong(KEY_LAST_ATTEMPT_MS, value.coerceAtLeast(0L))

  var lastFreshAuthMs: Long
    get() = getLong(KEY_LAST_FRESH_AUTH_MS, defaultValue = 0L)
    set(value) = putLong(KEY_LAST_FRESH_AUTH_MS, value.coerceAtLeast(0L))

  var lastVerifiedUidHash: String
    get() = getString(KEY_UID_HASH)
    set(value) = putString(KEY_UID_HASH, value)

  var lastSessionDigest: String
    get() = getString(KEY_SESSION_DIGEST)
    set(value) = putString(KEY_SESSION_DIGEST, value)

  fun storeVerifiedSession(uid: String, rawIdToken: String, verifiedAtMs: Long = System.currentTimeMillis()) {
    lastVerifiedUidHash = sha256Hex(uid)
    lastSessionDigest = sha256Hex(rawIdToken)
    lastFreshAuthMs = verifiedAtMs
  }

  fun clearSessionBinding() {
    remove(KEY_UID_HASH)
    remove(KEY_SESSION_DIGEST)
    remove(KEY_LAST_FRESH_AUTH_MS)
  }

  fun hasFreshSession(maxAgeMs: Long = DEFAULT_FRESH_SESSION_WINDOW_MS): Boolean {
    val last = lastFreshAuthMs
    return last > 0L && (System.currentTimeMillis() - last) in 0..maxAgeMs
  }

  fun getSecretMatchAlias(matchId: String): String {
    if (matchId.isBlank()) return ""
    return getString("secret_alias_${sha256Hex(matchId).take(24)}")
  }

  fun getSecretMatchHideAvatar(matchId: String): Boolean {
    if (matchId.isBlank()) return false
    return getBoolean("secret_hide_avatar_${sha256Hex(matchId).take(24)}", defaultValue = false)
  }

  fun setSecretMatchMask(matchId: String, alias: String, hideAvatar: Boolean) {
    if (matchId.isBlank()) return
    val keySuffix = sha256Hex(matchId).take(24)
    val cleanAlias = alias.trim().take(40)
    if (cleanAlias.isBlank()) {
      remove("secret_alias_$keySuffix")
    } else {
      putString("secret_alias_$keySuffix", cleanAlias)
    }
    putBoolean("secret_hide_avatar_$keySuffix", hideAvatar)
  }

  private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
    val p = prefs ?: return memoryFallback[key]?.toBooleanStrictOrNull() ?: defaultValue
    return p.getBoolean(key, defaultValue)
  }

  private fun putBoolean(key: String, value: Boolean) {
    val p = prefs
    if (p == null) {
      memoryFallback[key] = value.toString()
      return
    }
    p.edit().putBoolean(key, value).apply()
  }

  private fun getInt(key: String, defaultValue: Int): Int {
    val p = prefs ?: return memoryFallback[key]?.toIntOrNull() ?: defaultValue
    return p.getInt(key, defaultValue)
  }

  private fun putInt(key: String, value: Int) {
    val p = prefs
    if (p == null) {
      memoryFallback[key] = value.toString()
      return
    }
    p.edit().putInt(key, value).apply()
  }

  private fun getLong(key: String, defaultValue: Long): Long {
    val p = prefs ?: return memoryFallback[key]?.toLongOrNull() ?: defaultValue
    return p.getLong(key, defaultValue)
  }

  private fun putLong(key: String, value: Long) {
    val p = prefs
    if (p == null) {
      memoryFallback[key] = value.toString()
      return
    }
    p.edit().putLong(key, value).apply()
  }

  private fun getString(key: String): String {
    val p = prefs ?: return memoryFallback[key].orEmpty()
    return p.getString(key, "").orEmpty()
  }

  private fun putString(key: String, value: String) {
    val p = prefs
    if (p == null) {
      memoryFallback[key] = value
      return
    }
    p.edit().putString(key, value).apply()
  }

  private fun remove(key: String) {
    val p = prefs
    if (p == null) {
      memoryFallback.remove(key)
      return
    }
    p.edit().remove(key).apply()
  }

  companion object {
    private const val TAG = "EncryptedVaultStorage"
    const val VAULT_PREFS_NAME = "hello_secure_vault"
    const val DEFAULT_FRESH_SESSION_WINDOW_MS = 15L * 60L * 1000L

    private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
    private const val KEY_STRICT_TAMPER_MODE = "strict_tamper_mode"
    private const val KEY_FAILED_ATTEMPTS = "auth_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL_MS = "auth_lockout_until_ms"
    private const val KEY_LAST_ATTEMPT_MS = "auth_last_attempt_ms"
    private const val KEY_LAST_FRESH_AUTH_MS = "last_fresh_auth_ms"
    private const val KEY_UID_HASH = "verified_uid_hash"
    private const val KEY_SESSION_DIGEST = "session_token_sha256"

    fun sha256Hex(input: String): String {
      if (input.isBlank()) return ""
      val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
      return digest.joinToString("") { "%02x".format(it) }
    }

    private fun buildHardwareMasterKey(context: Context): MasterKey {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
          return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
        } catch (_: Exception) {
          // StrongBox HSM absent on device; fall through to TEE hardware Keystore
        }
      }
      return MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    }
  }
}
