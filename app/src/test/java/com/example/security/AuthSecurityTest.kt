package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSecurityTest {

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun rateLimiter_enforcesExponentialCooldownAndResetsOnSuccess() {
    val vault = EncryptedVaultStorage(context)
    val limiter = AuthRateLimiter(vault)
    val baseTime = 1_700_000_000_000L

    // Attempts 1 & 2 allow immediate retry
    val a1 = limiter.recordFailure(baseTime)
    assertFalse(a1.isLockedOut)
    assertEquals(1, a1.failedAttempts)

    val a2 = limiter.recordFailure(baseTime + 1_000L)
    assertFalse(a2.isLockedOut)
    assertEquals(2, a2.failedAttempts)

    // Attempt 3 -> 15s lockout
    val a3 = limiter.recordFailure(baseTime + 2_000L)
    assertTrue(a3.isLockedOut)
    assertEquals(15L, a3.remainingSeconds)

    // Attempt 4 -> 30s, Attempt 5 -> 60s, Attempt 6 -> 300s, Attempt 7 -> 900s
    assertEquals(30L, limiter.recordFailure(baseTime + 3_000L).remainingSeconds)
    assertEquals(60L, limiter.recordFailure(baseTime + 4_000L).remainingSeconds)
    assertEquals(300L, limiter.recordFailure(baseTime + 5_000L).remainingSeconds)
    assertEquals(900L, limiter.recordFailure(baseTime + 6_000L).remainingSeconds)

    // Remote monotonic merge preserves highest lockout
    val synced = limiter.syncRemoteLockout(
      remoteFailedAttempts = 10,
      remoteLockoutUntilMs = baseTime + 1_200_000L,
      nowMs = baseTime + 6_000L
    )
    assertEquals(10, synced.failedAttempts)
    assertTrue(synced.remainingSeconds >= 1_190L)

    // Success clears lockout and failed counter
    limiter.recordSuccess()
    val cleared = limiter.getState(baseTime + 7_000L)
    assertFalse(cleared.isLockedOut)
    assertEquals(0, cleared.failedAttempts)
  }

  @Test
  fun encryptedVault_bindsSha256SessionAndTracksFreshness() {
    val vault = EncryptedVaultStorage(context)
    vault.clearSessionBinding()
    assertFalse(vault.hasFreshSession())

    vault.storeVerifiedSession(
      uid = "uid_123",
      rawIdToken = "sample.jwt.token",
      verifiedAtMs = System.currentTimeMillis()
    )

    assertEquals(EncryptedVaultStorage.sha256Hex("uid_123"), vault.lastVerifiedUidHash)
    assertEquals(EncryptedVaultStorage.sha256Hex("sample.jwt.token"), vault.lastSessionDigest)
    assertEquals(64, vault.lastSessionDigest.length)
    assertTrue(vault.hasFreshSession())

    vault.lastFreshAuthMs = System.currentTimeMillis() - (20L * 60L * 1000L)
    assertFalse(vault.hasFreshSession())

    vault.clearSessionBinding()
    assertEquals("", vault.lastVerifiedUidHash)
    assertEquals("", vault.lastSessionDigest)
  }

  @Test
  fun deviceIntegrityChecker_distinguishesDebugWarningVsStrictBlocking() {
    val reportDefault = DeviceIntegrityChecker.assess(context, strictDebugEnforcement = false)
    assertTrue(reportDefault.isDebugBuild)
    // Debug build never blocks unless strict enforcement simulation is explicitly enabled
    assertFalse(reportDefault.isBlocked)

    val reportStrict = DeviceIntegrityChecker.assess(context, strictDebugEnforcement = true)
    assertTrue(reportStrict.strictEnforcement)
    if (reportStrict.isEmulator || reportStrict.isRooted || reportStrict.isDebuggerAttached) {
      assertTrue(reportStrict.isBlocked)
      assertEquals(SecurityRiskLevel.BLOCKED, reportStrict.riskLevel)
    }
  }

  @Test
  fun pinnedNetworkClient_enforcesTlsAndCertificatePins() {
    val pinner = PinnedNetworkClient.client.certificatePinner
    val specs = PinnedNetworkClient.client.connectionSpecs
    assertTrue(specs.isNotEmpty())
    assertTrue(pinner.findMatchingPins("firestore.googleapis.com").isNotEmpty())
    assertTrue(pinner.findMatchingPins("hello.firebaseio.com").isNotEmpty())
    assertTrue(pinner.findMatchingPins("hello.firebaseapp.com").isNotEmpty())
    assertTrue(pinner.findMatchingPins("www.youtube.com").isNotEmpty())
    assertTrue(pinner.findMatchingPins("i.ytimg.com").isNotEmpty())
  }
}
