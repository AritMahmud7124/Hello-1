package com.example.data.model

import com.example.data.repository.FeedCurator
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PureLoveAndFeedCurationTest {

  private val validReflection =
    "Today I noticed how calm and thoughtful they stayed even when the schedule shifted unexpectedly, and it reminded me why patience matters deeply."

  @Test
  fun pureLove_validateJournalReflection_enforcesLengthWordVarietyAndSpamFilter() {
    assertNotNull(PureLoveEngine.validateJournalReflection("Too short"))
    assertNotNull(
      PureLoveEngine.validateJournalReflection(
        "aaaaaa ".repeat(25)
      )
    )
    assertNull(PureLoveEngine.validateJournalReflection(validReflection))
  }

  @Test
  fun pureLove_cooldownAndProgression_calculatesStreakMissedPenaltyAndBadges() {
    assertEquals(0L, PureLoveEngine.cooldownRemainingMs(lastCheckInMs = 0L, nowMs = 1_000_000L))
    val last = 1_000_000L
    assertTrue(PureLoveEngine.cooldownRemainingMs(last, nowMs = last + 1_000L) > 0L)
    assertEquals(0L, PureLoveEngine.cooldownRemainingMs(last, nowMs = last + PureLoveEngine.COOLDOWN_MS))

    // Day 6 -> Day 7 unlocks Spark Initiate badge
    val day6Trial = DevotionTrial(
      senderId = "u1",
      recipientId = "u2",
      targetTierDays = 30,
      completedDays = 6,
      currentStreak = 6,
      missedDaysCount = 0,
      successRatePct = 100,
      lastCheckInMs = last
    )
    val day7Result = PureLoveEngine.computeNextCheckIn(
      existing = day6Trial,
      nowMs = last + PureLoveEngine.COOLDOWN_MS + 60_000L
    )
    assertEquals(7, day7Result.completedDays)
    assertEquals(7, day7Result.currentStreak)
    assertEquals(0, day7Result.missedDaysCount)
    assertEquals(100, day7Result.successRatePct)
    assertEquals("Spark Initiate", day7Result.newlyUnlockedBadge?.badgeTitle)

    // Missing > 48h resets streak to 1 and reduces successRatePct
    val missedResult = PureLoveEngine.computeNextCheckIn(
      existing = day6Trial,
      nowMs = last + PureLoveEngine.MISSED_WINDOW_MS + 86_400_000L
    )
    assertEquals(1, missedResult.currentStreak)
    assertTrue(missedResult.missedDaysCount > 0)
    assertTrue(missedResult.successRatePct < 100)

    // Day 364 -> Day 365 unlocks 1% Legend
    val day364 = DevotionTrial(
      completedDays = 364,
      currentStreak = 364,
      lastCheckInMs = last,
      unlockedBadges = PURE_LOVE_TIERS.dropLast(1).map { "${it.badgeTitle} (${it.label})" }
    )
    val day365 = PureLoveEngine.computeNextCheckIn(day364, last + PureLoveEngine.COOLDOWN_MS)
    assertEquals("1% Legend", day365.newlyUnlockedBadge?.badgeTitle)
  }

  @Test
  fun feedCurator_24hExpiryAndHighlights_respectsSavedFlagAndTtl() {
    val nowSec = 2_000_000L
    val old25h = Timestamp(nowSec - 90_000L, 0)
    val fresh2h = Timestamp(nowSec - 7_200L, 0)

    val expiredStory = Story(id = "s1", createdAt = old25h, isSavedHighlight = false, autoDelete24h = true)
    val highlightedOldStory = Story(id = "s2", createdAt = old25h, isSavedHighlight = true, autoDelete24h = false)
    val freshStory = Story(id = "s3", createdAt = fresh2h, isSavedHighlight = false, autoDelete24h = true)

    assertTrue(FeedCurator.isStoryExpired(expiredStory, nowSec))
    assertFalse(FeedCurator.isStoryExpired(highlightedOldStory, nowSec))
    assertFalse(FeedCurator.isStoryExpired(freshStory, nowSec))

    // Home Stories tray always auto-outs after 24h even if saved to Profile Highlights
    assertTrue(FeedCurator.isExpiredFromHomeTray(expiredStory, nowSec))
    assertTrue(FeedCurator.isExpiredFromHomeTray(highlightedOldStory, nowSec))
    assertFalse(FeedCurator.isExpiredFromHomeTray(freshStory, nowSec))

    val homeTrayRanked = FeedCurator.rankCuratedStories(
      stories = listOf(expiredStory, highlightedOldStory, freshStory),
      prefs = FeedPreferences(userId = "u1"),
      currentUserId = "u1",
      nowEpochSec = nowSec
    )
    assertEquals(listOf("s3"), homeTrayRanked.map { it.id })

    val legacyFlaggedReel = Reel(id = "r1", createdAt = old25h, autoDelete24h = true)
    val permanentOldReel = Reel(id = "r2", createdAt = old25h, autoDelete24h = false)
    assertFalse(FeedCurator.isReelExpired(legacyFlaggedReel, nowSec))
    assertFalse(FeedCurator.isReelExpired(permanentOldReel, nowSec))
  }

  @Test
  fun publicUser_effectiveOnline_respectsActiveStatusSwitch() {
    assertTrue(PublicUser(userId = "u1", isOnline = true, showOnlineStatus = true).effectiveOnline)
    assertFalse(PublicUser(userId = "u1", isOnline = true, showOnlineStatus = false).effectiveOnline)
    assertFalse(PublicUser(userId = "u1", isOnline = false, showOnlineStatus = true).effectiveOnline)
    assertFalse(PublicUser(userId = "u1", isOnline = false, showOnlineStatus = false).effectiveOnline)
  }

  @Test
  fun feedCurator_globalAndCountrySwitching_interleavesGlobalAndFiltersCountry() {
    val nowSec = 2_000_000L
    val prefs = FeedPreferences(userId = "u1")
    val reels = listOf(
      Reel(id = "r_bd", caption = "Dhaka street food", category = "Lifestyle", regionCode = "BD", createdAt = Timestamp(nowSec, 0)),
      Reel(id = "r_us", caption = "NYC tech demo", category = "Tech", regionCode = "US", createdAt = Timestamp(nowSec, 0)),
      Reel(id = "r_gl", caption = "Global music jam", category = "Music", regionCode = "GLOBAL", createdAt = Timestamp(nowSec, 0))
    )

    val globalFeed = FeedCurator.rankCuratedReels(
      nativeReels = reels,
      youtubeShorts = emptyList(),
      prefs = prefs,
      selectedCategory = null,
      selectedRegion = "GLOBAL",
      nowEpochSec = nowSec
    )
    assertEquals(3, globalFeed.size)

    val bdOnlyFeed = FeedCurator.rankCuratedReels(
      nativeReels = reels,
      youtubeShorts = emptyList(),
      prefs = prefs,
      selectedCategory = null,
      selectedRegion = "BD",
      nowEpochSec = nowSec
    )
    assertTrue(bdOnlyFeed.any { it.uniqueId == "native_r_bd" })
    assertTrue(bdOnlyFeed.none { it.uniqueId == "native_r_us" })
    assertEquals("native_r_bd", bdOnlyFeed.first().uniqueId)
  }

  @Test
  fun mutualSecretLove_sortedPairScheduleAndCountdownPhases_behaveDeterministically() {
    // Lexicographical sorting is invariant to who added first
    assertEquals("alice_bob", MutualSecretLoveEngine.sortedPairId("alice", "bob"))
    assertEquals("alice_bob", MutualSecretLoveEngine.sortedPairId("bob", "alice"))

    val matchedAt = 1_700_000_000_000L
    val minSchedule = MutualSecretLoveEngine.pickRandomRevealSchedule(matchedAt, randomFraction = 0.0)
    val midSchedule = MutualSecretLoveEngine.pickRandomRevealSchedule(matchedAt, randomFraction = 0.5)
    val maxSchedule = MutualSecretLoveEngine.pickRandomRevealSchedule(matchedAt, randomFraction = 1.0)

    assertEquals(matchedAt + MutualSecretLoveEngine.MIN_DELAY_MS, minSchedule.revealAtMs)
    assertEquals(matchedAt + MutualSecretLoveEngine.MAX_DELAY_MS, maxSchedule.revealAtMs)
    assertEquals(minSchedule.revealAtMs - MutualSecretLoveEngine.ALERT_LEAD_MS, minSchedule.alertAtMs)
    assertEquals(midSchedule.revealAtMs - MutualSecretLoveEngine.ALERT_LEAD_MS, midSchedule.alertAtMs)
    assertEquals(maxSchedule.revealAtMs - MutualSecretLoveEngine.ALERT_LEAD_MS, maxSchedule.alertAtMs)
    assertTrue(midSchedule.revealAtMs in (matchedAt + 7L * 86_400_000L)..(matchedAt + 14L * 86_400_000L))

    val match = MutualCrushMatch(
      id = "alice_bob",
      participantUids = listOf("alice", "bob"),
      matchedAtMs = midSchedule.matchedAtMs,
      revealAtMs = midSchedule.revealAtMs,
      alertAtMs = midSchedule.alertAtMs
    )
    assertEquals("bob", match.partnerUidFor("alice"))
    assertEquals("alice", match.partnerUidFor("bob"))

    // 1. Sealed hidden until exactly 24h before revealAt
    assertEquals(MutualMatchPhase.SEALED_HIDDEN, MutualSecretLoveEngine.phaseOf(match, matchedAt))
    assertEquals(MutualMatchPhase.SEALED_HIDDEN, MutualSecretLoveEngine.phaseOf(match, match.alertAtMs - 1L))

    // 2. Sudden 24h alert & live countdown starts at alertAtMs
    assertEquals(MutualMatchPhase.COUNTDOWN_24H, MutualSecretLoveEngine.phaseOf(match, match.alertAtMs))
    assertEquals(MutualMatchPhase.COUNTDOWN_24H, MutualSecretLoveEngine.phaseOf(match, match.revealAtMs - 1_000L))
    assertEquals("24:00:00", MutualSecretLoveEngine.formatCountdown24h(MutualSecretLoveEngine.remainingMsUntilReveal(match, match.alertAtMs)))
    assertEquals("23:59:59", MutualSecretLoveEngine.formatCountdown24h(86_399_000L))
    assertEquals("00:00:01", MutualSecretLoveEngine.formatCountdown24h(1_000L))

    // 3. Unlocks simultaneously once timer hits zero
    assertEquals(MutualMatchPhase.UNLOCKED, MutualSecretLoveEngine.phaseOf(match, match.revealAtMs))
    assertEquals("00:00:00", MutualSecretLoveEngine.formatCountdown24h(MutualSecretLoveEngine.remainingMsUntilReveal(match, match.revealAtMs + 5000L)))
    assertEquals("Mutual Secret Love unlock has only 24 hours left", MutualSecretLoveEngine.ALERT_MESSAGE)
  }

  @Test
  fun ownerEmailVisibility_and_musicAttachmentModels_haveSafeDefaultsAndFields() {
    val defaultOwner = OwnerConfig()
    assertFalse(defaultOwner.showContactEmail)
    assertTrue(defaultOwner.showOwnerHandle)

    val visibleOwner = OwnerConfig(
      ownerHandle = "@hello123",
      showOwnerHandle = false,
      contactEmail = "helloarit123@gmail.com",
      showContactEmail = true
    )
    assertTrue(visibleOwner.showContactEmail)
    assertFalse(visibleOwner.showOwnerHandle)
    assertEquals("helloarit123@gmail.com", visibleOwner.contactEmail)

    val music = MusicAttachment(
      title = "Sunlit Dreams",
      artist = "HELLO Studio",
      audioUri = "firestore://media/music_123",
      startSec = 8L,
      endSec = 28L
    )
    val post = Post(
      authorId = "u1",
      content = "Photo with song",
      musicTitle = music.title,
      musicArtist = music.artist,
      musicUri = music.audioUri,
      musicStartSec = music.startSec,
      musicEndSec = music.endSec
    )
    val story = Story(
      authorId = "u1",
      mediaType = "image",
      musicTitle = music.title,
      musicArtist = music.artist,
      musicUri = music.audioUri,
      musicStartSec = music.startSec,
      musicEndSec = music.endSec
    )
    assertEquals("Sunlit Dreams", post.musicTitle)
    assertEquals(8L, post.musicStartSec)
    assertEquals(28L, post.musicEndSec)
    assertEquals("Sunlit Dreams", story.musicTitle)
    assertEquals("HELLO Studio", story.musicArtist)
    assertEquals(28L, story.musicEndSec)
  }
}

