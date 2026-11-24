package com.example.security

import kotlin.math.ceil

data class RateLimitState(
  val failedAttempts: Int,
  val lockoutUntilEpochMs: Long,
  val remainingSeconds: Long,
  val isLockedOut: Boolean
)

class AuthRateLimiter(private val vault: EncryptedVaultStorage) {

  fun getState(nowMs: Long = System.currentTimeMillis()): RateLimitState {
    val until = vault.lockoutUntilEpochMs
    val remainingMs = (until - nowMs).coerceAtLeast(0L)
    val remainingSec = if (remainingMs > 0L) ceil(remainingMs / 1000.0).toLong() else 0L
    return RateLimitState(
      failedAttempts = vault.failedAuthAttempts,
      lockoutUntilEpochMs = until,
      remainingSeconds = remainingSec,
      isLockedOut = remainingMs > 0L
    )
  }

  fun syncRemoteLockout(remoteFailedAttempts: Int, remoteLockoutUntilMs: Long, nowMs: Long = System.currentTimeMillis()): RateLimitState {
    if (remoteFailedAttempts > vault.failedAuthAttempts) {
      vault.failedAuthAttempts = remoteFailedAttempts
    }
    if (remoteLockoutUntilMs > vault.lockoutUntilEpochMs && remoteLockoutUntilMs > nowMs) {
      vault.lockoutUntilEpochMs = remoteLockoutUntilMs
    }
    return getState(nowMs)
  }

  fun recordFailure(nowMs: Long = System.currentTimeMillis()): RateLimitState {
    val nextAttempts = vault.failedAuthAttempts + 1
    vault.failedAuthAttempts = nextAttempts
    vault.lastAttemptEpochMs = nowMs

    val cooldownSec = cooldownSecondsForAttempt(nextAttempts)
    if (cooldownSec > 0L) {
      val nextLockout = nowMs + cooldownSec * 1000L
      if (nextLockout > vault.lockoutUntilEpochMs) {
        vault.lockoutUntilEpochMs = nextLockout
      }
    }
    return getState(nowMs)
  }

  fun recordSuccess() {
    vault.failedAuthAttempts = 0
    vault.lockoutUntilEpochMs = 0L
    vault.lastAttemptEpochMs = System.currentTimeMillis()
  }

  companion object {
    fun cooldownSecondsForAttempt(attemptCount: Int): Long = when {
      attemptCount <= 2 -> 0L
      attemptCount == 3 -> 15L
      attemptCount == 4 -> 30L
      attemptCount == 5 -> 60L
      attemptCount == 6 -> 300L
      else -> 900L
    }
  }
}
