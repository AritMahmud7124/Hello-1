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
  fun sendSecretLove_requiresPureLoveDay7QuestGate_andEnforcesTrialStartRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    // Sign back in as Alice and verify direct Secret Love without 7-day Pure Love Quest is blocked
    signInTestUser(ALICE_EMAIL)
    val govRepo = GovernanceRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Secret", acceptSecretLove = true)
    val bob = PublicUser(userId = bobUid, displayName = "Bob Target", acceptSecretLove = true)

    val blockedEarlySend = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.sendSecretLove(
        sender = alice,
        recipient = bob,
        message = "Admiring your creativity ${UUID.randomUUID()}",
        moodEmoji = "❤️",
        revealIdentityNow = false
      )
    }
    assertTrue("Expected Secret Love before Day 7 to be gated", blockedEarlySend.isFailure)

    // Alice starts a Pure Love Quest for Bob -> Day 0 created cleanly
    val trialStart = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.startOrUpdateDevotionTrialTarget(bob, targetTierDays = 7)
    }
    assertTrue("Expected Pure Love trial creation to succeed", trialStart.isSuccess)
    assertEquals(0, trialStart.getOrThrow().completedDays)

    // Complete Day 1 reflection check-in cleanly
    val day1CheckIn = withTimeout(DEFAULT_TIMEOUT_MS) {
      govRepo.completeDailyDevotionCheckIn(
        recipient = bob,
        targetTierDays = 7,
        reflectionText = "Today I noticed how thoughtful and calm they stay under pressure, which makes me respect them even more every single day."
      )
    }
    assertTrue("Expected Day 1 devotion check-in to succeed", day1CheckIn.isSuccess)

    // Bob cannot read Alice's private devotion_trials progress
    signInTestUser(BOB_EMAIL)
    val bobReadTrials = withTimeout(FLOW_TIMEOUT_MS) {
      runCatching { govRepo.observeMyDevotionTrials(aliceUid).first() }
    }
    assertTrue("Recipient must not observe sender private devotion_trials", bobReadTrials.isFailure)
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

  @Test
  fun friendsPosts_observeSharedFriendsPosts_succeedsForNonAdminUsers() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    val charlieUid = signInTestUser(CHARLIE_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val repo = SocialRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Friends")

    val friendsPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        author = alice,
        content = "Friends-only post ${UUID.randomUUID()}",
        privacy = "friends",
        allowedViewerUids = listOf(bobUid)
      ).getOrThrow()
    }
    val onlyMePostId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        author = alice,
        content = "Private diary ${UUID.randomUUID()}",
        privacy = "only_me"
      ).getOrThrow()
    }

    // Bob (allowed viewer, non-admin) receives the friends post cleanly
    signInTestUser(BOB_EMAIL)
    val bobPosts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSharedFriendsPosts(bobUid).first { list -> list.any { it.id == friendsPostId } }
    }
    assertTrue(bobPosts.any { it.id == friendsPostId })
    assertTrue(bobPosts.none { it.id == onlyMePostId })

    // Charlie (unshared non-admin user) queries cleanly without PERMISSION_DENIED
    signInTestUser(CHARLIE_EMAIL)
    val charliePosts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSharedFriendsPosts(charlieUid).first()
    }
    assertTrue(charliePosts.none { it.id == friendsPostId || it.id == onlyMePostId })
  }

  @Test
  fun privateGroupSecurity_onlyAdminCanAddOrRemoveMembers_andPostsStayIsolated() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    val charlieUid = signInTestUser(CHARLIE_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val spacesRepo = SpacesRepository(firestore, auth)
    val socialRepo = SocialRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Admin")

    val groupId = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.createGroup(
        name = "Vault Circle ${UUID.randomUUID()}",
        description = "High security private space",
        category = "Family",
        privacy = "private",
        sosEnabled = true
      ).getOrThrow()
    }
    var group = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeMyGroups(aliceUid).first { list -> list.any { it.id == groupId } }.first { it.id == groupId }
    }

    // Non-admin Charlie cannot self-join a private group (even directly via Firestore)
    signInTestUser(CHARLIE_EMAIL)
    val charlieSelfJoin = withTimeout(DEFAULT_TIMEOUT_MS) {
      runCatching {
        firestore.collection("groups").document(groupId).update(
          mapOf(
            "memberUids" to listOf(aliceUid, charlieUid),
            "membersCount" to 2L
          )
        ).let { com.google.android.gms.tasks.Tasks.await(it) }
      }
    }
    assertTrue("Non-admin self-join on private group must be denied", charlieSelfJoin.isFailure)

    // Group Admin Alice adds Bob to the private group
    signInTestUser(ALICE_EMAIL)
    val addBobRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.inviteMemberToGroup(group, alice, bobUid)
    }
    assertTrue("Admin should add Bob cleanly", addBobRes.isSuccess)
    signInTestUser(BOB_EMAIL)
    group = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeMyGroups(bobUid).first { list ->
        list.any { it.id == groupId && bobUid in it.memberUids }
      }.first { it.id == groupId }
    }

    // Non-admin Bob cannot add Charlie (client check AND Firestore rule block)
    val bobInviteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.inviteMemberToGroup(group, PublicUser(userId = bobUid, displayName = "Bob"), charlieUid)
    }
    assertTrue("Non-admin Bob must not add members", bobInviteRes.isFailure)
    val directRuleBypass = withTimeout(DEFAULT_TIMEOUT_MS) {
      runCatching {
        firestore.collection("groups").document(groupId).update(
          mapOf(
            "memberUids" to listOf(aliceUid, bobUid, charlieUid),
            "membersCount" to 3L
          )
        ).let { com.google.android.gms.tasks.Tasks.await(it) }
      }
    }
    assertTrue("Firestore rule must block non-admin adding another user", directRuleBypass.isFailure)

    // Private group rejects public posts, accepts member-restricted friends posts
    signInTestUser(ALICE_EMAIL)
    val publicGroupPostRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      socialRepo.createPost(
        author = alice,
        content = "Should not be public ${UUID.randomUUID()}",
        privacy = "public",
        groupId = groupId
      )
    }
    assertTrue("Private group must reject public posts", publicGroupPostRes.isFailure)

    val privateGroupPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
      socialRepo.createPost(
        author = alice,
        content = "Vault secret note ${UUID.randomUUID()}",
        privacy = "friends",
        allowedViewerUids = group.memberUids,
        groupId = groupId
      ).getOrThrow()
    }

    // Member Bob reads private group post & cannot share it
    signInTestUser(BOB_EMAIL)
    val bobFeed = withTimeout(FLOW_TIMEOUT_MS) {
      socialRepo.observeSharedFriendsPosts(bobUid).first { list -> list.any { it.id == privateGroupPostId } }
    }
    val groupPost = bobFeed.first { it.id == privateGroupPostId }
    val shareAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      socialRepo.sharePost(groupPost, PublicUser(userId = bobUid, displayName = "Bob"), "Leak attempt")
    }
    assertTrue("Sharing private group post must fail", shareAttempt.isFailure)

    // Non-member Charlie cannot see the private group post
    signInTestUser(CHARLIE_EMAIL)
    val charlieFeed = withTimeout(FLOW_TIMEOUT_MS) {
      socialRepo.observeSharedFriendsPosts(charlieUid).first()
    }
    assertTrue(charlieFeed.none { it.id == privateGroupPostId })

    // Group Admin Alice removes Bob from the private group
    signInTestUser(ALICE_EMAIL)
    val removeBobRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      spacesRepo.removeMemberFromGroup(group, bobUid)
    }
    assertTrue("Admin should remove Bob cleanly", removeBobRes.isSuccess)
    val updatedGroupsForBob = withTimeout(FLOW_TIMEOUT_MS) {
      spacesRepo.observeMyGroups(aliceUid).first { list ->
        list.any { it.id == groupId && bobUid !in it.memberUids }
      }.first { it.id == groupId }
    }
    assertTrue(bobUid !in updatedGroupsForBob.memberUids)
  }

  @Test
  fun reelsFollow_toggleFollowCreator_persistsAndProtectsOwnership() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val repo = SocialRepository(firestore, auth)

    // Alice follows a YouTube Shorts creator
    val followedYt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.toggleFollowCreator("yt_ComposeDaily", "Compose Daily").getOrThrow()
    }
    assertTrue("Expected first toggle to follow creator", followedYt)

    // Alice follows Bob
    val followedBob = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.toggleFollowCreator(bobUid, "Bob Creator").getOrThrow()
    }
    assertTrue("Expected follow on Bob to succeed", followedBob)

    val followingSet = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFollowingIds(aliceUid).first { it.contains("yt_ComposeDaily") && it.contains(bobUid) }
    }
    assertTrue("yt_ComposeDaily" in followingSet)
    assertTrue(bobUid in followingSet)

    // Bob cannot delete Alice's follow document
    signInTestUser(BOB_EMAIL)
    val unauthorizedDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
      runCatching {
        firestore.collection("follows").document("${aliceUid}_$bobUid").delete()
          .let { com.google.android.gms.tasks.Tasks.await(it) }
      }
    }
    assertTrue("Bob must not delete Alice's follow doc", unauthorizedDelete.isFailure)

    // Alice unfollows cleanly
    signInTestUser(ALICE_EMAIL)
    val unfollowedYt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.toggleFollowCreator("yt_ComposeDaily", "Compose Daily").getOrThrow()
    }
    assertEquals(false, unfollowedYt)
    val remainingSet = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFollowingIds(aliceUid).first { "yt_ComposeDaily" !in it }
    }
    assertTrue(bobUid in remainingSet)
  }

  @Test
  fun friendRequest_sendDuplicateAcceptUnfriendAndResend_succeedsAgainstRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val communityRepo = CommunityRepository(firestore, auth)
    val alice = PublicUser(userId = aliceUid, displayName = "Alice Friend")
    val bob = PublicUser(userId = bobUid, displayName = "Bob Friend")

    // First friend request succeeds
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { communityRepo.sendFriendRequest(alice, bob) }.isSuccess)
    // Duplicate tap while pending succeeds idempotently without PERMISSION_DENIED
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { communityRepo.sendFriendRequest(alice, bob) }.isSuccess)

    // Bob sees and accepts request
    signInTestUser(BOB_EMAIL)
    val bobRequests = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeFriendRequests(bobUid).first { list ->
        list.any { it.fromUid == aliceUid && it.toUid == bobUid && it.status == "pending" }
      }
    }
    val req = bobRequests.first { it.fromUid == aliceUid && it.toUid == bobUid }
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { communityRepo.acceptFriendRequest(req) }.isSuccess)

    // Alice unfriends Bob and later re-sends a friend request cleanly
    signInTestUser(ALICE_EMAIL)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { communityRepo.unfriend(bobUid) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { communityRepo.sendFriendRequest(alice, bob) }.isSuccess)
  }

  @Test
  fun notifications_markReadMarkAllReadAndDelete_persistIsReadProperly() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)

    signInTestUser(ALICE_EMAIL)
    val communityRepo = CommunityRepository(firestore, auth)
    communityRepo.sendNotification(
      recipientId = bobUid,
      actorId = aliceUid,
      actorName = "Alice",
      type = "like",
      title = "Alice liked your post",
      body = "Nice post!",
      targetId = "post_1"
    )
    communityRepo.sendNotification(
      recipientId = bobUid,
      actorId = aliceUid,
      actorName = "Alice",
      type = "comment",
      title = "Alice commented",
      body = "Awesome!",
      targetId = "post_1"
    )

    // Bob observes unread notifications (isRead must deserialize cleanly as false, then true after marking)
    signInTestUser(BOB_EMAIL)
    val initialNotifs = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeNotifications(bobUid).first { it.size >= 2 }
    }
    assertTrue(initialNotifs.all { !it.isRead })

    val firstId = initialNotifs.first().id
    communityRepo.markNotificationRead(firstId)
    val afterSingleRead = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeNotifications(bobUid).first { list -> list.any { it.id == firstId && it.isRead } }
    }
    assertTrue(afterSingleRead.first { it.id == firstId }.isRead)

    communityRepo.markAllNotificationsRead(afterSingleRead)
    val afterMarkAll = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeNotifications(bobUid).first { list -> list.all { it.isRead } }
    }
    assertTrue(afterMarkAll.all { it.isRead })

    communityRepo.deleteNotification(firstId)
    val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      communityRepo.observeNotifications(bobUid).first { list -> list.none { it.id == firstId } }
    }
    assertTrue(afterDelete.none { it.id == firstId })
  }

  private companion object {
    const val ALICE_EMAIL = "alice.hello@test.com"
    const val BOB_EMAIL = "bob.hello@test.com"
    const val CHARLIE_EMAIL = "charlie.hello@test.com"
    const val DEFAULT_TIMEOUT_MS = 15000L
    const val FLOW_TIMEOUT_MS = 12000L
  }
}
