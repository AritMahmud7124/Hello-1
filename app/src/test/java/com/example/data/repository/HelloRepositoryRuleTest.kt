package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.EngagementSignal
import com.example.data.model.FeedPreferences
import com.example.data.model.PublicUser
import com.example.data.model.Reel
import com.example.data.model.YouTubeShortVideo
import com.example.data.network.YouTubeShortsClient
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class HelloRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createPost_andObservePublicFeed_succeedsForAuthenticatedUser() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SocialRepository(firestore, auth)
    val author = PublicUser(userId = uid, displayName = "Alice Hello")
    val uniqueText = "Hello World ${UUID.randomUUID()}"

    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(author = author, content = uniqueText, privacy = "public")
    }
    assertTrue("Expected post creation to succeed", createRes.isSuccess)
    val postId = createRes.getOrThrow()

    val posts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePublicPosts(30L).first { list -> list.any { it.id == postId } }
    }
    assertTrue(posts.any { it.id == postId && it.content == uniqueText })
  }

  @Test
  fun observePublicPosts_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SocialRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePublicPosts(10L).first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated caller")
    } catch (e: Exception) {
      val firestoreError = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError?.code)
    }
  }

  @Test
  fun sendSecretLove_hidesSenderIdentityFromRecipientInbox() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    // Sign back in as Alice to send Secret Love to Bob
    signInTestUser(ALICE_EMAIL)
    val govRepo = GovernanceRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Secret", acceptSecretLove = true)
    val bob = PublicUser(userId = bobUid, displayName = "Bob Target", acceptSecretLove = true)

    val sendRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.sendSecretLove(
        sender = alice,
        recipient = bob,
        message = "Admiring your creativity ${UUID.randomUUID()}",
        moodEmoji = "❤️",
        revealIdentityNow = false
      )
    }
    assertTrue("Expected Secret Love batch write to succeed", sendRes.isSuccess)
    val msgId = sendRes.getOrThrow()

    // Sign in as Bob and verify sender identity is strictly hidden
    signInTestUser(BOB_EMAIL)
    val inbox = withTimeout(FLOW_TIMEOUT_MS) {
      govRepo.observeReceivedSecretLoves(bobUid).first { list -> list.any { it.id == msgId } }
    }
    val received = inbox.first { it.id == msgId }
    assertEquals(false, received.isRevealed)
    assertEquals("", received.revealedSenderId)
    assertEquals("", received.revealedSenderName)
  }

  @Test
  fun groupSosAlert_isRestrictedToSelectedRecipientsOnly() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    val charlieUid = signInTestUser(CHARLIE_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val spacesRepo = SpacesRepository(firestore, auth)
    val groupId = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.createGroup("Safety Circle ${UUID.randomUUID()}", "Local", "Safety", "public", true).getOrThrow()
    }
    val group = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeMyGroups(aliceUid).first { list -> list.any { it.id == groupId } }.first { it.id == groupId }
    }

    val alertId = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.sendGroupSosAlert(
        group = group,
        sender = PublicUser(userId = aliceUid, displayName = "Alice"),
        message = "Need quick help!",
        recipientUids = listOf(bobUid),
        shareLiveLocation = true,
        latitude = 37.7749,
        longitude = -122.4194
      ).getOrThrow()
    }

    // Bob (selected recipient) receives the alert
    signInTestUser(BOB_EMAIL)
    val bobAlerts = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeIncomingSosAlerts(groupId, bobUid).first { list -> list.any { it.id == alertId } }
    }
    assertTrue(bobAlerts.any { it.id == alertId && it.shareLiveLocation })

    // Charlie (unselected) receives nothing
    signInTestUser(CHARLIE_EMAIL)
    val charlieAlerts = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeIncomingSosAlerts(groupId, charlieUid).first()
    }
    assertTrue(charlieAlerts.none { it.id == alertId })
  }

  @Test
  fun messenger_voiceNoteTypingAndReadReceipts_succeedAgainstRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val communityRepo = CommunityRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice")
    val bob = PublicUser(userId = bobUid, displayName = "Bob")

    val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
      communityRepo.getOrCreateDirectConversation(alice, bob).getOrThrow()
    }
    val conv = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeConversations(aliceUid).first { list -> list.any { it.id == convId } }.first { it.id == convId }
    }

    communityRepo.setTypingStatus(convId, true)
    val sendVoice = withTimeout(DEFAULT_TIMEOUT_MS) {
      communityRepo.sendMessage(
        conversation = conv,
        sender = alice,
        text = "",
        mediaUri = "https://firebasestorage.googleapis.com/v0/b/demo/o/voice.m4a",
        mediaType = "audio",
        fileName = "voice_4s.m4a"
      )
    }
    assertTrue("Expected voice note audio message to succeed", sendVoice.isSuccess)

    // Bob marks conversation read and observes voice note
    signInTestUser(BOB_EMAIL)
    communityRepo.markConversationRead(convId)
    val bobMsgs = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeMessages(convId).first { list -> list.any { it.mediaType == "audio" } }
    }
    assertTrue(bobMsgs.any { it.mediaType == "audio" && it.fileName == "voice_4s.m4a" })

    val updatedConv = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeConversations(bobUid).first { list ->
        list.any { it.id == convId && bobUid in it.readByUids }
      }.first { it.id == convId }
    }
    assertTrue(bobUid in updatedConv.readByUids)
    assertTrue(aliceUid in updatedConv.readByUids)
  }

  @Test
  fun ownerGovernance_nonOwnerCannotWriteOwnerConfigOrGlobalSettings() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val govRepo = GovernanceRepository(firestore, auth)

    val updateOwnerRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.updateOwnerProfile(
        ownerName = "Spoof Owner",
        ownerHandle = "@spoof",
        ownerAvatar = "",
        about = "Unauthorized",
        contactEmail = ALICE_EMAIL,
        contactInfo = ""
      )
    }
    assertTrue("Expected non-owner updateOwnerProfile to fail", updateOwnerRes.isFailure)

    val updateSettingsRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.updateGlobalSettings(
        appName = "Hijacked",
        tagline = "Bad",
        maintenanceBanner = "Bad",
        sosEnabled = false,
        secretLoveEnabled = false,
        allowRegistrations = false
      )
    }
    assertTrue("Expected non-owner updateGlobalSettings to fail", updateSettingsRes.isFailure)
  }

  @Test
  fun firestoreMediaStore_uploadBytesAndResolve_worksWithoutStorageBucket() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val samplePayload = "HELLO_SPARK_FREE_CLOUD_MEDIA_BYTES".toByteArray()

    val mediaUri = withTimeout(DEFAULT_TIMEOUT_MS) {
      com.example.data.media.FirestoreMediaStore.uploadBytes(
        context = context,
        db = firestore,
        uid = aliceUid,
        bytes = samplePayload,
        mimeType = "audio/mp4",
        ext = "m4a"
      )
    }
    assertTrue("Expected firestore://media/ URI, got $mediaUri", mediaUri.startsWith("firestore://media/"))

    val repo = SocialRepository(firestore, auth)
    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        author = PublicUser(userId = aliceUid, displayName = "Alice"),
        content = "Media post on Spark plan",
        mediaUri = mediaUri,
        mediaType = "image",
        privacy = "public"
      ).getOrThrow()
    }
    assertTrue(postId.isNotBlank())

    // Delete local cache so Bob forces a live Firestore fetch
    java.io.File(context.cacheDir, "hello_media").deleteRecursively()

    signInTestUser(BOB_EMAIL)
    val resolvedFile = withTimeout(DEFAULT_TIMEOUT_MS) {
      com.example.data.media.FirestoreMediaStore.resolveToLocalFile(context, firestore, mediaUri)
    }
    assertTrue("Expected resolved local file to exist", resolvedFile.exists())
    assertEquals("HELLO_SPARK_FREE_CLOUD_MEDIA_BYTES", resolvedFile.readText())
  }

  @Test
  fun storyLifecycle_friendsPrivacyCommentsAndDelete_enforcesRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    val charlieUid = signInTestUser(CHARLIE_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val repo = SocialRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Story")
    val bob = PublicUser(userId = bobUid, displayName = "Bob Viewer")

    val storyId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createStory(
        author = alice,
        caption = "Friends only sunset ${UUID.randomUUID()}",
        mediaType = "text",
        backgroundHex = "#BE123C",
        privacy = "friends",
        allowedViewerUids = listOf(bobUid)
      ).getOrThrow()
    }

    // Bob (allowed friend) receives story and posts a story comment
    signInTestUser(BOB_EMAIL)
    val bobStories = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFriendsStories(bobUid).first { list -> list.any { it.id == storyId } }
    }
    val bobStory = bobStories.first { it.id == storyId }
    assertEquals("#BE123C", bobStory.backgroundHex)
    val reactRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.reactToStory(bobStory, bob, "🔥", "Looking amazing!")
    }
    assertTrue("Expected Bob to react/comment on Alice's friend story", reactRes.isSuccess)

    // Charlie (not in allowedViewerUids) cannot see Alice's friends-only story
    signInTestUser(CHARLIE_EMAIL)
    val charlieStories = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFriendsStories(charlieUid).first()
    }
    assertTrue(charlieStories.none { it.id == storyId })

    // Bob cannot delete Alice's story
    signInTestUser(BOB_EMAIL)
    val bobDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteStory(storyId)
    }
    assertTrue("Non-author Bob must not delete Alice's story", bobDelete.isFailure)

    // Alice (author) can delete her own story cleanly
    signInTestUser(ALICE_EMAIL)
    val aliceDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteStory(storyId)
    }
    assertTrue("Author Alice should be able to delete her story", aliceDelete.isSuccess)
  }

  @Test
  fun userProfile_observeUserPublicPosts_succeedsAndRespectsPrivacy() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    signInTestUser(BOB_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val repo = SocialRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Profile")

    val publicPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(author = alice, content = "Public bio update ${UUID.randomUUID()}", privacy = "public").getOrThrow()
    }
    val privatePostId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(author = alice, content = "Secret diary ${UUID.randomUUID()}", privacy = "only_me").getOrThrow()
    }

    // Bob visits Alice's profile and queries her public posts cleanly
    signInTestUser(BOB_EMAIL)
    val visiblePosts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserPublicPosts(aliceUid).first { list -> list.any { it.id == publicPostId } }
    }
    assertTrue(visiblePosts.any { it.id == publicPostId })
    assertTrue(visiblePosts.none { it.id == privatePostId })
  }

  @Test
  fun feedCuration_engagementScoresNotInterestedFilteringAndPrivacyIsolation_workEndToEnd() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val curationRepo = FeedCurationRepository(firestore, auth)

    // Initial preferences
    var prefs = withTimeout(FLOW_TIMEOUT_MS) {
      curationRepo.observePreferences(aliceUid).first()
    }
    assertEquals(10L, prefs.categoryScores["Tech"])

    // Simulate Views (+1), Likes (+3), Full Watches (+5) on Tech
    prefs = withTimeout(DEFAULT_TIMEOUT_MS) {
      curationRepo.recordEngagement(prefs, "Tech", EngagementSignal.VIEW).getOrThrow()
    }
    prefs = withTimeout(DEFAULT_TIMEOUT_MS) {
      curationRepo.recordEngagement(prefs, "Tech", EngagementSignal.LIKE).getOrThrow()
    }
    prefs = withTimeout(DEFAULT_TIMEOUT_MS) {
      curationRepo.recordEngagement(prefs, "Tech", EngagementSignal.FULL_WATCH).getOrThrow()
    }
    assertEquals(19L, prefs.categoryScores["Tech"])

    // Mark a Funny YouTube Short as "Not Interested" twice so Funny drops <= 0 and gets hidden
    prefs = withTimeout(DEFAULT_TIMEOUT_MS) {
      curationRepo.recordEngagement(prefs, "Funny", EngagementSignal.NOT_INTERESTED, videoId = "yt_bad123").getOrThrow()
    }
    prefs = withTimeout(DEFAULT_TIMEOUT_MS) {
      curationRepo.recordEngagement(prefs, "Funny", EngagementSignal.NOT_INTERESTED, videoId = "yt_bad456").getOrThrow()
    }
    assertTrue("yt_bad123" in prefs.dismissedVideoIds)
    assertTrue("Funny" in prefs.dislikedCategories)

    // Verify dynamic ranking puts Tech above Young, and filters out yt_bad123 & Funny category
    val sampleShorts = listOf(
      YouTubeShortVideo("yt_bad123", "Filtered Short", "ChanA", "", "Tech"),
      YouTubeShortVideo("yt_funny1", "Disliked Cat Short", "ChanB", "", "Funny"),
      YouTubeShortVideo("yt_young1", "Youth Vlog", "ChanC", "", "Young"),
      YouTubeShortVideo("yt_tech1", "Kotlin Compose Tips", "ChanD", "", "Tech")
    )
    val ranked = FeedCurator.rankCuratedReels(emptyList<Reel>(), sampleShorts, prefs)
    assertEquals(listOf("yt_yt_tech1", "yt_yt_young1"), ranked.map { it.uniqueId })

    // Verify YouTube ID extraction & embed URL generation
    assertEquals("dQw4w9WgXcQ", YouTubeShortsClient.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    assertTrue(YouTubeShortsClient.buildEmbedUrl("dQw4w9WgXcQ").startsWith("https://www.youtube.com/embed/dQw4w9WgXcQ"))

    // Cross-user isolation: Bob cannot read or overwrite Alice's feed_preferences
    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        curationRepo.observePreferences(aliceUid).first()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice feed_preferences")
    } catch (e: Exception) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice.hello@test.com"
    const val BOB_EMAIL = "bob.hello@test.com"
    const val CHARLIE_EMAIL = "charlie.hello@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
