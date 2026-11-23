const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";
const OWNER_UID = "owner_001";
const OWNER_EMAIL = "helloarit123@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("users_public").get());
  await assertFails(unauthDb.collection("secret_loves").get());
  await assertFails(unauthDb.collection("app_config").doc("owner").get());
});

test("PII Isolation: Bob cannot read Alice private user doc", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      isDeactivated: false,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Public user creation blocks self-escalation to admin or owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users_public").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      profilePrivacy: "public",
      acceptSecretLove: true,
      role: "admin",
      isVerifiedOwner: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      profilePrivacy: "public",
      acceptSecretLove: true,
      role: "user",
      isVerifiedOwner: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Post privacy & Shadow Update protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_private_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Private note",
      mediaType: "none",
      privacy: "only_me",
      allowedViewerUids: [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob cannot read Alice's only_me post
  await assertFails(bobDb.collection("posts").doc("post_private_1").get());

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("posts").doc("post_private_1").update({
      content: "Updated",
      isPromoted: true,
      updatedAt: ts(),
    })
  );
});

test("Secret Love enforces 7-day Pure Love quest gate and hides sender identity completely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Seed an incomplete trial (day 2 < 7) first: sending a hint message must be DENIED
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("devotion_trials").doc(`${ALICE_UID}_${BOB_UID}`).set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      recipientName: "Bob",
      targetTierDays: 7,
      completedDays: 2,
      currentStreak: 2,
      missedDaysCount: 0,
      successRatePct: 100,
      lastCheckInMs: Date.now() - 80000000,
      lastReflection: "Daily Pure Love reflection entry recorded in good faith and verified across the trial.",
      unlockedBadges: [],
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const lockedBatch = aliceDb.batch();
  lockedBatch.set(aliceDb.collection("users").doc(ALICE_UID).collection("sent_secret_loves").doc("sl_locked"), {
    messageId: "sl_locked",
    recipientId: BOB_UID,
    recipientName: "Bob",
    message: "Too early hint!",
    moodEmoji: "❤️",
    isRevealed: false,
    createdAt: ts(),
  });
  lockedBatch.set(aliceDb.collection("secret_loves").doc("sl_locked"), {
    recipientId: BOB_UID,
    message: "Too early hint!",
    moodEmoji: "❤️",
    isRevealed: false,
    revealedSenderId: "",
    revealedSenderName: "",
    isRead: false,
    isBlocked: false,
    isReported: false,
    createdAt: ts(),
  });
  await assertFails(lockedBatch.commit());

  // Advance trial to 7 completed days
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("devotion_trials").doc(`${ALICE_UID}_${BOB_UID}`).update({
      completedDays: 7,
      currentStreak: 7,
      unlockedBadges: ["Spark Initiate (7D)"],
    });
  });

  // Attempting to smuggle senderId into revealedSenderId while isRevealed=false fails
  const badBatch = aliceDb.batch();
  badBatch.set(aliceDb.collection("users").doc(ALICE_UID).collection("sent_secret_loves").doc("sl_bad"), {
    messageId: "sl_bad",
    recipientId: BOB_UID,
    message: "You are amazing!",
    isRevealed: false,
    createdAt: ts(),
  });
  badBatch.set(aliceDb.collection("secret_loves").doc("sl_bad"), {
    recipientId: BOB_UID,
    message: "You are amazing!",
    moodEmoji: "❤️",
    isRevealed: false,
    revealedSenderId: ALICE_UID,
    revealedSenderName: "Alice",
    isRead: false,
    isBlocked: false,
    isReported: false,
    createdAt: ts(),
  });
  await assertFails(badBatch.commit());

  // Valid anonymous Secret Love atomic batch succeeds once 7-day Pure Love milestone is unlocked
  const goodBatch = aliceDb.batch();
  goodBatch.set(aliceDb.collection("users").doc(ALICE_UID).collection("sent_secret_loves").doc("sl_good"), {
    messageId: "sl_good",
    recipientId: BOB_UID,
    recipientName: "Bob",
    message: "Secretly admiring your kindness!",
    moodEmoji: "❤️",
    isRevealed: false,
    createdAt: ts(),
  });
  goodBatch.set(aliceDb.collection("secret_loves").doc("sl_good"), {
    recipientId: BOB_UID,
    message: "Secretly admiring your kindness!",
    moodEmoji: "❤️",
    isRevealed: false,
    revealedSenderId: "",
    revealedSenderName: "",
    isRead: false,
    isBlocked: false,
    isReported: false,
    senderBadgeTitle: "Spark Initiate (7D)",
    senderTrialDays: 7,
    createdAt: ts(),
  });
  await assertSucceeds(goodBatch.commit());

  // Bob can query his Secret Love inbox and cannot read Alice's sent_secret_loves or devotion_trials
  const bobInbox = await assertSucceeds(
    bobDb.collection("secret_loves").where("recipientId", "==", BOB_UID).get()
  );
  assert.strictEqual(bobInbox.docs.length, 1);
  assert.strictEqual(bobInbox.docs[0].data().revealedSenderId, "");
  assert.strictEqual(bobInbox.docs[0].data().senderBadgeTitle, "Spark Initiate (7D)");
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sent_secret_loves").doc("sl_good").get());
  await assertFails(bobDb.collection("devotion_trials").doc(`${ALICE_UID}_${BOB_UID}`).get());
});

test("Emergency SOS restricts live location to explicitly selected group recipients", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("groups").doc("grp_1").set({
      name: "Neighborhood Watch",
      privacy: "public",
      creatorId: ALICE_UID,
      adminUids: [ALICE_UID],
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      membersCount: 3,
      sosEnabled: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice sends SOS only to Bob (not Charlie)
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("sos_alerts").doc("sos_1").set({
      groupId: "grp_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      message: "Need urgent pickup!",
      recipientUids: [BOB_UID],
      shareLiveLocation: true,
      latitude: 37.7749,
      longitude: -122.4194,
      status: "active",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(bobDb.collection("groups").doc("grp_1").collection("sos_alerts").doc("sos_1").get());
  await assertFails(charlieDb.collection("groups").doc("grp_1").collection("sos_alerts").doc("sos_1").get());

  // Alice stops sharing location
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").collection("sos_alerts").doc("sos_1").update({
      shareLiveLocation: false,
      latitude: 0.0,
      longitude: 0.0,
      status: "stopped",
      updatedAt: ts(),
    })
  );
});

test("Owner Governance: only verified owner can claim owner config, admins cannot escalate or edit owner", async () => {
  // Spoofed unverified email must fail
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("app_config").doc("owner").set({
      ownerUid: "spoof_uid",
      ownerName: "Spoofer",
      about: "Fake",
      updatedAt: ts(),
    })
  );

  // Verified owner claims owner config
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    auth_time: Math.floor(Date.now() / 1000) - 10,
  }).firestore();
  await assertSucceeds(
    ownerDb.collection("app_config").doc("owner").set({
      ownerUid: OWNER_UID,
      ownerName: "HELLO Founder",
      ownerHandle: "@founder",
      about: "Building HELLO safely.",
      contactEmail: OWNER_EMAIL,
      updatedAt: ts(),
    })
  );

  // Owner adds Bob as an ordinary admin
  await assertSucceeds(
    ownerDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob Mod",
      addedByUid: OWNER_UID,
      createdAt: ts(),
    })
  );

  // Ordinary admin Bob CANNOT modify owner config, global settings, or promote Charlie to admin
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("app_config").doc("owner").update({
      ownerName: "Hijacked",
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobDb.collection("app_config").doc("global_settings").set({
      appName: "Hijacked",
      sosFeatureEnabled: false,
      secretLoveEnabled: false,
      allowNewRegistrations: true,
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobDb.collection("admins").doc(CHARLIE_UID).set({
      uid: CHARLIE_UID,
      displayName: "Charlie",
      addedByUid: BOB_UID,
      createdAt: ts(),
    })
  );

  // Owner cannot even change ownerUid once set
  await assertFails(
    ownerDb.collection("app_config").doc("owner").update({
      ownerUid: BOB_UID,
      updatedAt: ts(),
    })
  );
});

test("Messenger voice notes, typing indicators, read receipts, and FCM token rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fcmToken: "fcm_sample_token_123",
      isDeactivated: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      isGroup: false,
      title: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      typingUids: [ALICE_UID],
      readByUids: [ALICE_UID],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_voice_1").set({
      conversationId: "conv_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "",
      mediaUri: "https://firebasestorage.googleapis.com/voice.m4a",
      mediaType: "audio",
      fileName: "Voice Note (4s)",
      participantUids: [ALICE_UID, BOB_UID],
      isDeleted: false,
      createdAt: ts(),
    })
  );

  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").update({
      readByUids: [ALICE_UID, BOB_UID],
      typingUids: [],
      updatedAt: ts(),
    })
  );
});

test("Firestore cloud media_blobs enforce owner writes and authenticated get", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(unauthDb.collection("media_blobs").doc("media_1").get());

  // Spoofed ownerId rejected
  await assertFails(
    bobDb.collection("media_blobs").doc("media_1").set({
      ownerId: ALICE_UID,
      mimeType: "image/jpeg",
      ext: "jpg",
      chunkCount: 1,
      byteSize: 1200,
      createdAt: ts(),
    })
  );

  // Alice writes valid metadata + chunk
  await assertSucceeds(
    aliceDb.collection("media_blobs").doc("media_1").set({
      ownerId: ALICE_UID,
      mimeType: "image/jpeg",
      ext: "jpg",
      chunkCount: 1,
      byteSize: 12,
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("media_blobs").doc("media_1").collection("chunks").doc("chunk_000").set({
      ownerId: ALICE_UID,
      index: 0,
      data: "SGVsbG9Xb3JsZA==",
      createdAt: ts(),
    })
  );

  // Bob (signed in) can fetch metadata + chunks to view Alice's post/story, but cannot overwrite/list-all/delete
  await assertSucceeds(bobDb.collection("media_blobs").doc("media_1").get());
  await assertSucceeds(bobDb.collection("media_blobs").doc("media_1").collection("chunks").get());
  await assertFails(bobDb.collection("media_blobs").get());
  await assertFails(bobDb.collection("media_blobs").doc("media_1").delete());
});

test("Feed preferences enforce strict owner-only privacy and category fields on reels/stories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice writes her private curation preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("feed_preferences").doc("curation").set({
      userId: ALICE_UID,
      categoryScores: { Tech: 18, Funny: 12, Young: 10 },
      dislikedCategories: ["Politics"],
      dismissedVideoIds: ["yt_abc123"],
      totalViews: 5,
      totalLikes: 2,
      totalFullWatches: 1,
      updatedAt: ts(),
    })
  );

  // Bob cannot read or overwrite Alice's feed preferences
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("feed_preferences").doc("curation").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("feed_preferences").doc("curation").set({
      userId: ALICE_UID,
      categoryScores: { Tech: 1 },
      dislikedCategories: [],
      dismissedVideoIds: [],
      totalViews: 0,
      totalLikes: 0,
      totalFullWatches: 0,
      updatedAt: ts(),
    })
  );

  // Reels and Stories accept category & youtubeVideoId safely
  await assertSucceeds(
    aliceDb.collection("reels").doc("reel_yt_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      caption: "Cool Compose tip!",
      videoUri: "",
      coverHex: "#4F46E5",
      audioTitle: "Tech Mix",
      category: "Tech",
      youtubeVideoId: "dQw4w9WgXcQ",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
    })
  );
});

test("Friends-only posts list query requires privacy=='friends' filter alongside allowedViewerUids", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_friends_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Friends update",
      mediaType: "none",
      privacy: "friends",
      allowedViewerUids: [BOB_UID],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Unfiltered query on allowedViewerUids alone fails for non-admin users
  await assertFails(
    bobDb.collection("posts").where("allowedViewerUids", "array-contains", BOB_UID).get()
  );

  // Aligned query with privacy == 'friends' succeeds for Bob and returns empty for Charlie
  const bobSnap = await assertSucceeds(
    bobDb.collection("posts")
      .where("privacy", "==", "friends")
      .where("allowedViewerUids", "array-contains", BOB_UID)
      .get()
  );
  assert.strictEqual(bobSnap.docs.length, 1);

  const charlieSnap = await assertSucceeds(
    charlieDb.collection("posts")
      .where("privacy", "==", "friends")
      .where("allowedViewerUids", "array-contains", CHARLIE_UID)
      .get()
  );
  assert.strictEqual(charlieSnap.docs.length, 0);
});

test("Enterprise Auth Rules: reject anonymous provider, enforce recent auth (<15m), monotonic rate-limit, and immutable security_events", async () => {
  const nowSec = Math.floor(Date.now() / 1000);

  // 1. Anonymous sign-in provider must be denied everywhere
  const anonDb = testEnv.authenticatedContext("anon_user", {
    firebase: { sign_in_provider: "anonymous" },
    auth_time: nowSec,
  }).firestore();
  await assertFails(anonDb.collection("posts").get());
  await assertFails(anonDb.collection("users_public").doc("anon_user").set({
    userId: "anon_user",
    displayName: "Anon",
    handle: "@anon",
    profilePrivacy: "public",
    acceptSecretLove: true,
    role: "user",
    isVerifiedOwner: false,
    createdAt: ts(),
    updatedAt: ts(),
  }));

  // 2. Stale auth_time (> 15 min ago) is denied on destructive/governance actions, fresh auth_time succeeds
  const staleOwnerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 1800, // 30 minutes ago
  }).firestore();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      isDeactivated: false,
      createdAt: ts(),
      updatedAt: ts(),
    });
    await ctx.firestore().collection("app_config").doc("owner").set({
      ownerUid: OWNER_UID,
      ownerName: "HELLO Founder",
      ownerHandle: "@founder",
      about: "Founder",
      updatedAt: ts(),
    });
  });

  await assertFails(
    staleOwnerDb.collection("app_config").doc("owner").update({
      about: "Edited with stale token",
      updatedAt: ts(),
    })
  );
  await assertFails(
    staleOwnerDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      addedByUid: OWNER_UID,
      createdAt: ts(),
    })
  );

  const staleAliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 1800,
  }).firestore();
  await assertFails(staleAliceDb.collection("users").doc(ALICE_UID).delete());

  const freshOwnerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 30,
  }).firestore();
  await assertSucceeds(
    freshOwnerDb.collection("app_config").doc("owner").update({
      about: "Verified fresh owner",
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    freshOwnerDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      addedByUid: OWNER_UID,
      createdAt: ts(),
    })
  );

  const freshAliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 30,
  }).firestore();
  await assertSucceeds(freshAliceDb.collection("users").doc(ALICE_UID).delete());

  // 3. Monotonic backend rate-limiting (security_state) cannot be reset early during active lockout
  const futureLockoutMs = Date.now() + 300000;
  await assertSucceeds(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_state").doc("auth_guard").set({
      userId: ALICE_UID,
      failedAttempts: 5,
      lockoutUntilMs: futureLockoutMs,
      lastAttemptMs: Date.now() - 1000,
      biometricEnabled: true,
      integrityRiskLevel: "CLEAN",
      integritySummary: "Verified",
      updatedAt: ts(),
    })
  );
  await assertFails(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_state").doc("auth_guard").update({
      failedAttempts: 0,
      lockoutUntilMs: 0,
      updatedAt: ts(),
    })
  );
  await assertFails(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_state").doc("auth_guard").delete()
  );

  // 4. Immutable security_events audit log (owner create/read only, no update/delete, no spoofing)
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  await assertSucceeds(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_events").doc("evt_1").set({
      userId: ALICE_UID,
      eventType: "biometric_unlocked",
      detail: "Hardware biometric prompt unlocked session",
      integritySummary: "Clean",
      createdAt: ts(),
    })
  );
  await assertFails(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_events").doc("evt_1").update({
      detail: "Tampered",
    })
  );
  await assertFails(
    freshAliceDb.collection("users").doc(ALICE_UID).collection("security_events").doc("evt_1").delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("security_events").doc("evt_1").get()
  );
});

test("Secret Crush zero-trust signals, crush_match notifications, regional_analytics, and Story/Reel fields", async () => {
  const nowSec = Math.floor(Date.now() / 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();

  // 1. Alice signals crush on Bob; Bob hasn't signaled Alice yet
  await assertSucceeds(
    aliceDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      createdAt: ts(),
    })
  );
  // Listing crush_signals is denied for everyone
  await assertFails(bobDb.collection("crush_signals").get());
  await assertFails(aliceDb.collection("crush_signals").get());
  // Bob CANNOT inspect Alice's unrequited signal until Bob signals Alice too
  await assertFails(bobDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).get());
  // Charlie cannot spoof a signal from Alice
  await assertFails(
    charlieDb.collection("crush_signals").doc(`${ALICE_UID}_${CHARLIE_UID}`).set({
      fromUid: ALICE_UID,
      toUid: CHARLIE_UID,
      createdAt: ts(),
    })
  );

  // 2. Bob also signals crush on Alice -> mutual match!
  await assertSucceeds(
    bobDb.collection("crush_signals").doc(`${BOB_UID}_${ALICE_UID}`).set({
      fromUid: BOB_UID,
      toUid: ALICE_UID,
      createdAt: ts(),
    })
  );
  await assertSucceeds(bobDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).get());
  await assertSucceeds(aliceDb.collection("crush_signals").doc(`${BOB_UID}_${ALICE_UID}`).get());
  // Charlie still cannot read Alice <-> Bob signal
  await assertFails(charlieDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).get());

  // Both users can receive crush_match notifications from Bob
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("match_1").set({
      recipientId: ALICE_UID,
      actorId: BOB_UID,
      actorName: "HELLO",
      type: "crush_match",
      title: "It's a Secret Crush Match!",
      body: "You both liked each other!",
      targetId: ALICE_UID,
      isRead: false,
      createdAt: ts(),
    })
  );

  // 3. Regional Analytics: user writes own doc; non-admin cannot list; owner can list
  await assertSucceeds(
    aliceDb.collection("regional_analytics").doc(ALICE_UID).set({
      userId: ALICE_UID,
      feedMode: "country",
      selectedRegion: "BD",
      originRegion: "US",
      watchMode: "Country Focus",
      activeCategory: "Music",
      switchCount: 3,
      updatedAt: ts(),
    })
  );
  await assertFails(bobDb.collection("regional_analytics").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("regional_analytics").get());
  await assertSucceeds(ownerDb.collection("regional_analytics").get());

  // 4. Story highlight & Reel regional/autoDelete fields
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_hl_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      mediaType: "text",
      caption: "Saved forever",
      privacy: "public",
      allowedViewerUids: [],
      viewersCount: 0,
      isSavedHighlight: true,
      autoDelete24h: true,
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("reels").doc("reel_bd_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      caption: "Dhaka street food",
      category: "Lifestyle",
      regionCode: "BD",
      autoDelete24h: true,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
    })
  );
});

test("Pure Love devotion_trials enforce 0-start, 120-char reflection, 20h cooldown, and Messenger calls restrict participants", async () => {
  const nowSec = Math.floor(Date.now() / 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();

  const trialId = `${ALICE_UID}_${BOB_UID}`;
  const longReflection = "I noticed how thoughtfully you encouraged everyone in the community group today, and I am keeping my promise patiently every single evening.";

  // Creating a pre-filled trial (>0 days) must fail
  await assertFails(
    aliceDb.collection("devotion_trials").doc(trialId).set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      recipientName: "Bob",
      targetTierDays: 30,
      completedDays: 7,
      currentStreak: 7,
      missedDaysCount: 0,
      successRatePct: 100,
      lastCheckInMs: 0,
      lastReflection: "",
      unlockedBadges: [],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Starting cleanly at Day 0 succeeds
  await assertSucceeds(
    aliceDb.collection("devotion_trials").doc(trialId).set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      recipientName: "Bob",
      targetTierDays: 30,
      completedDays: 0,
      currentStreak: 0,
      missedDaysCount: 0,
      successRatePct: 100,
      lastCheckInMs: 0,
      lastReflection: "",
      unlockedBadges: [],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Short reflection (<120 chars) must fail
  await assertFails(
    aliceDb.collection("devotion_trials").doc(trialId).update({
      completedDays: 1,
      currentStreak: 1,
      lastCheckInMs: Date.now(),
      lastReflection: "Too short",
      updatedAt: ts(),
    })
  );

  // Valid Day 1 check-in succeeds
  await assertSucceeds(
    aliceDb.collection("devotion_trials").doc(trialId).update({
      completedDays: 1,
      currentStreak: 1,
      lastCheckInMs: Date.now(),
      lastReflection: longReflection,
      updatedAt: ts(),
    })
  );

  // Immediate Day 2 check-in (bypassing 20h cooldown) MUST FAIL
  await assertFails(
    aliceDb.collection("devotion_trials").doc(trialId).update({
      completedDays: 2,
      currentStreak: 2,
      lastCheckInMs: Date.now() + 1000,
      lastReflection: longReflection,
      updatedAt: ts(),
    })
  );

  // Jumping multiple days (+5) also fails
  await assertFails(
    aliceDb.collection("devotion_trials").doc(trialId).update({
      completedDays: 7,
      currentStreak: 7,
      lastCheckInMs: Date.now() + 1000,
      lastReflection: longReflection,
      updatedAt: ts(),
    })
  );

  // Messenger Call signaling: Alice starts a video call with Bob; Charlie cannot read or tamper
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      conversationId: "conv_1",
      conversationTitle: "Bob",
      callerId: ALICE_UID,
      callerName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      acceptedUids: [ALICE_UID],
      callType: "video",
      status: "ringing",
      startedAtMs: Date.now(),
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertSucceeds(bobDb.collection("calls").doc("call_1").get());
  await assertFails(charlieDb.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "active",
      acceptedUids: [ALICE_UID, BOB_UID],
      updatedAt: ts(),
    })
  );
  await assertFails(
    charlieDb.collection("calls").doc("call_1").update({
      status: "ended",
      updatedAt: ts(),
    })
  );
});

test("Mutual Secret Love matches enforce sorted pair, 7-14d revealAtMs, 24h alert window, and zero premature reveal", async () => {
  const nowMs = Date.now();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const sortedPair = [ALICE_UID, BOB_UID].sort();
  const matchId = `${sortedPair[0]}_${sortedPair[1]}`;
  const validRevealAtMs = nowMs + 10 * 86400000;
  const validAlertAtMs = validRevealAtMs - 86400000;

  // Cannot create mutual_crush_matches before both crush_signals exist
  await assertSucceeds(
    aliceDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      createdAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("mutual_crush_matches").doc(matchId).set({
      participantUids: sortedPair,
      matchedAtMs: nowMs,
      revealAtMs: validRevealAtMs,
      alertAtMs: validAlertAtMs,
      alert24hDispatched: false,
      unlockedNotified: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob adds reciprocal crush signal + creates private secret_crushes doc
  await assertSucceeds(
    bobDb.collection("crush_signals").doc(`${BOB_UID}_${ALICE_UID}`).set({
      fromUid: BOB_UID,
      toUid: ALICE_UID,
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("secret_crushes").doc(ALICE_UID).set({
      targetUid: ALICE_UID,
      targetName: "Alice",
      createdAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("secret_crushes").doc(ALICE_UID).get()
  );

  // Unsorted participantUids fails
  await assertFails(
    bobDb.collection("mutual_crush_matches").doc(`${sortedPair[1]}_${sortedPair[0]}`).set({
      participantUids: [sortedPair[1], sortedPair[0]],
      matchedAtMs: nowMs,
      revealAtMs: validRevealAtMs,
      alertAtMs: validAlertAtMs,
      alert24hDispatched: false,
      unlockedNotified: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // revealAtMs < 7 days or > 14 days fails
  await assertFails(
    bobDb.collection("mutual_crush_matches").doc(matchId).set({
      participantUids: sortedPair,
      matchedAtMs: nowMs,
      revealAtMs: nowMs + 3 * 86400000,
      alertAtMs: nowMs + 2 * 86400000,
      alert24hDispatched: false,
      unlockedNotified: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Valid 10-day mutual match creation succeeds
  await assertSucceeds(
    bobDb.collection("mutual_crush_matches").doc(matchId).set({
      participantUids: sortedPair,
      matchedAtMs: nowMs,
      revealAtMs: validRevealAtMs,
      alertAtMs: validAlertAtMs,
      alert24hDispatched: false,
      unlockedNotified: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Charlie cannot read Alice <-> Bob mutual match
  await assertFails(charlieDb.collection("mutual_crush_matches").doc(matchId).get());
  await assertSucceeds(aliceDb.collection("mutual_crush_matches").doc(matchId).get());

  // Premature 24h alert dispatch or premature unlock MUST FAIL while now < alertAtMs
  await assertFails(
    aliceDb.collection("mutual_crush_matches").doc(matchId).update({
      alert24hDispatched: true,
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobDb.collection("mutual_crush_matches").doc(matchId).update({
      unlockedNotified: true,
      updatedAt: ts(),
    })
  );

  // mutual_unlock_24h notification type succeeds cleanly
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc(`mutual_24h_${matchId}`).set({
      recipientId: ALICE_UID,
      actorId: BOB_UID,
      actorName: "Secret Love",
      type: "mutual_unlock_24h",
      title: "Secret Love 24h Alert",
      body: "Mutual Secret Love unlock has only 24 hours left",
      targetId: matchId,
      isRead: false,
      createdAt: ts(),
    })
  );
});

test("Groups and Pages enforce secure Add/Out for creator, admins, and self join/leave", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // 1. Public Group: Alice creates, Bob self-joins, Alice adds Charlie, Alice removes ("Out") Charlie, Bob self-leaves ("Out")
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_add_out").set({
      name: "Campus Builders",
      description: "Open community",
      category: "Tech",
      privacy: "public",
      creatorId: ALICE_UID,
      adminUids: [ALICE_UID],
      memberUids: [ALICE_UID],
      membersCount: 1,
      sosEnabled: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Non-admin Bob cannot add Charlie directly
  await assertFails(
    bobDb.collection("groups").doc("grp_add_out").update({
      memberUids: [ALICE_UID, CHARLIE_UID],
      membersCount: 2,
      updatedAt: ts(),
    })
  );

  // Bob can self-join public group
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_add_out").update({
      memberUids: [ALICE_UID, BOB_UID],
      membersCount: 2,
      updatedAt: ts(),
    })
  );

  // Admin Alice adds Charlie and removes ("Out") Charlie
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_add_out").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      membersCount: 3,
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_add_out").update({
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      membersCount: 2,
      updatedAt: ts(),
    })
  );

  // Bob can self-leave ("Out")
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_add_out").update({
      memberUids: [ALICE_UID],
      membersCount: 1,
      updatedAt: ts(),
    })
  );

  // 2. Page: Alice creates, Alice adds Bob, Bob cannot remove Alice, Charlie can self-follow and self-leave ("Out"), page_invite allowed
  await assertSucceeds(
    aliceDb.collection("pages").doc("page_add_out").set({
      name: "HELLO Official",
      category: "Community",
      bio: "Official page",
      creatorId: ALICE_UID,
      adminUids: [ALICE_UID],
      followerUids: [ALICE_UID],
      followersCount: 1,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("pages").doc("page_add_out").update({
      followerUids: [ALICE_UID, BOB_UID],
      followersCount: 2,
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("page_inv_1").set({
      recipientId: BOB_UID,
      actorId: ALICE_UID,
      actorName: "Alice",
      type: "page_invite",
      title: "Added to Page: HELLO Official",
      body: "Alice added you to HELLO Official.",
      targetId: "page_add_out",
      isRead: false,
      createdAt: ts(),
    })
  );

  // Non-admin Bob cannot add or remove other users on the page
  await assertFails(
    bobDb.collection("pages").doc("page_add_out").update({
      followerUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      followersCount: 3,
      updatedAt: ts(),
    })
  );

  // Charlie can self-follow and self-leave ("Out")
  await assertSucceeds(
    charlieDb.collection("pages").doc("page_add_out").update({
      followerUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      followersCount: 3,
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("pages").doc("page_add_out").update({
      followerUids: [ALICE_UID, BOB_UID],
      followersCount: 2,
      updatedAt: ts(),
    })
  );

  // Admin Alice removes ("Out") Bob cleanly
  await assertSucceeds(
    aliceDb.collection("pages").doc("page_add_out").update({
      followerUids: [ALICE_UID],
      adminUids: [ALICE_UID],
      followersCount: 1,
      updatedAt: ts(),
    })
  );
});

test("Public user showOnlineStatus allows boolean toggle by owner and rejects invalid types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      profilePrivacy: "public",
      acceptSecretLove: true,
      isOnline: true,
      showOnlineStatus: true,
      role: "user",
      isVerifiedOwner: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice can turn off her green Active Status indicator
  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      showOnlineStatus: false,
      updatedAt: ts(),
    })
  );

  // Non-boolean showOnlineStatus is rejected
  await assertFails(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      showOnlineStatus: "no",
      updatedAt: ts(),
    })
  );

  // Bob cannot toggle Alice's Active Status
  await assertFails(
    bobDb.collection("users_public").doc(ALICE_UID).update({
      showOnlineStatus: true,
      updatedAt: ts(),
    })
  );
});

test("Posts & Stories allow valid cut music fields and Owner can toggle showContactEmail visibility", async () => {
  const nowSec = Math.floor(Date.now() / 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();

  // Valid Post with cut song clip succeeds
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_music_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Golden hour vibes",
      mediaUri: "firestore://media/photo_1",
      mediaType: "image",
      musicTitle: "Golden Hour Vibe",
      musicArtist: "HELLO Studio",
      musicUri: "firestore://media/music_1",
      musicStartSec: 12,
      musicEndSec: 27,
      privacy: "public",
      allowedViewerUids: [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Valid Story with cut song clip, lyrics, and overlaySpec succeeds
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_music_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      mediaUri: "firestore://media/photo_2",
      mediaType: "image",
      musicTitle: "Midnight Breeze",
      musicArtist: "HELLO Studio",
      musicUri: "firestore://media/music_2",
      musicStartSec: 5,
      musicEndSec: 20,
      musicLyrics: "Neon lights softly glow\nWalking down the quiet road",
      overlaySpec: "{\"cs\":1.2,\"ls\":1.1,\"lStyle\":\"GLOW\"}",
      backgroundHex: "#1E1B4B,#6366F1",
      caption: "Late night drive",
      privacy: "public",
      allowedViewerUids: [],
      viewersCount: 0,
      isSavedHighlight: false,
      autoDelete24h: true,
      createdAt: ts(),
    })
  );

  // Oversized musicLyrics (>1200) or overlaySpec (>8000) is rejected
  await assertFails(
    aliceDb.collection("stories").doc("story_lyrics_too_long").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      mediaType: "text",
      musicLyrics: "a".repeat(1201),
      privacy: "public",
      allowedViewerUids: [],
      viewersCount: 0,
      createdAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("stories").doc("story_overlay_too_long").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      mediaType: "text",
      overlaySpec: "x".repeat(8001),
      privacy: "public",
      allowedViewerUids: [],
      viewersCount: 0,
      createdAt: ts(),
    })
  );

  // Negative musicStartSec is rejected on both posts and stories
  await assertFails(
    aliceDb.collection("posts").doc("post_music_bad").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Bad range",
      mediaType: "none",
      musicTitle: "Track",
      musicStartSec: -4,
      musicEndSec: 15,
      privacy: "public",
      allowedViewerUids: [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("stories").doc("story_music_bad").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      mediaType: "text",
      musicTitle: "Track",
      musicStartSec: -1,
      musicEndSec: 10,
      privacy: "public",
      allowedViewerUids: [],
      viewersCount: 0,
      createdAt: ts(),
    })
  );

  // Owner can set and toggle showContactEmail & showOwnerHandle Public <-> Hidden
  await assertSucceeds(
    ownerDb.collection("app_config").doc("owner").set({
      ownerUid: OWNER_UID,
      ownerName: "HELLO Founder",
      ownerHandle: "@hello123",
      showOwnerHandle: true,
      about: "Verified platform owner.",
      contactEmail: OWNER_EMAIL,
      showContactEmail: false,
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_config").doc("owner").update({
      showContactEmail: true,
      showOwnerHandle: false,
      updatedAt: ts(),
    })
  );

  // Add Bob as Admin and confirm neither Admin nor regular user can modify showOwnerHandle/showContactEmail
  await assertSucceeds(
    ownerDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob Admin",
      addedByUid: OWNER_UID,
      createdAt: ts(),
    })
  );
  const bobAdminDb = testEnv.authenticatedContext(BOB_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();

  await assertFails(
    bobAdminDb.collection("app_config").doc("owner").update({
      showOwnerHandle: true,
      updatedAt: ts(),
    })
  );
  await assertFails(
    bobAdminDb.collection("app_config").doc("owner").update({
      showContactEmail: false,
      updatedAt: ts(),
    })
  );

  // Non-boolean showContactEmail / showOwnerHandle and non-owner updates are rejected
  await assertFails(
    ownerDb.collection("app_config").doc("owner").update({
      showContactEmail: "public",
      updatedAt: ts(),
    })
  );
  await assertFails(
    ownerDb.collection("app_config").doc("owner").update({
      showOwnerHandle: "hidden",
      updatedAt: ts(),
    })
  );
  await assertFails(
    aliceDb.collection("app_config").doc("owner").update({
      showContactEmail: true,
      showOwnerHandle: false,
      updatedAt: ts(),
    })
  );
});

test("WebRTC call signals, Page cover/avatar, Owner read-only Secret Love audit, and Unlocked Private Secret Love chat rules", async () => {
  const nowSec = Math.floor(Date.now() / 1000);
  const nowMs = Date.now();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, {
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
    firebase: { sign_in_provider: "google.com" },
    auth_time: nowSec - 10,
  }).firestore();

  // 1. Page avatarUrl & coverUrl update
  await assertSucceeds(
    aliceDb.collection("pages").doc("page_media_1").set({
      name: "Creative Studio",
      category: "Design",
      bio: "Modern visual showcase",
      avatarUrl: "firestore://media/pg_av_1",
      coverUrl: "firestore://media/pg_cv_1",
      creatorId: ALICE_UID,
      adminUids: [ALICE_UID],
      followerUids: [ALICE_UID],
      followersCount: 1,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // 2. WebRTC call & /calls/{callId}/signals/{signalId} participant-only access
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_rtc_1").set({
      conversationId: "conv_rtc_1",
      conversationTitle: "Bob",
      callerId: ALICE_UID,
      callerName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      acceptedUids: [ALICE_UID],
      callType: "video",
      status: "ringing",
      startedAtMs: nowMs,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_rtc_1").collection("signals").doc("sig_offer").set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      type: "offer",
      sdp: "v=0\r\no=- 123 2 IN IP4 127.0.0.1\r\n",
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_rtc_1").collection("signals").doc("sig_answer").set({
      senderId: BOB_UID,
      recipientId: ALICE_UID,
      type: "answer",
      sdp: "v=0\r\no=- 456 2 IN IP4 127.0.0.1\r\n",
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_rtc_1").collection("signals").get()
  );
  await assertFails(
    charlieDb.collection("calls").doc("call_rtc_1").collection("signals").get()
  );
  await assertFails(
    charlieDb.collection("calls").doc("call_rtc_1").collection("signals").doc("sig_spoof").set({
      senderId: CHARLIE_UID,
      recipientId: ALICE_UID,
      type: "ice",
      candidate: "candidate:1 1 UDP 123 127.0.0.1 9999 typ host",
      sdpMid: "0",
      sdpMLineIndex: 0,
      createdAt: ts(),
    })
  );

  // 3. Owner read-only audit of crush_signals, mutual_crush_matches, and users (cannot mutate other users' crush data or read unlocked private_messages)
  const sortedPair = [ALICE_UID, BOB_UID].sort();
  const matchId = `${sortedPair[0]}_${sortedPair[1]}`;
  const pastMatchedMs = nowMs - 10 * 86400000;
  const pastRevealMs = nowMs - 60000;

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phone: "+8801700000001",
      isDeactivated: false,
      createdAt: ts(),
      updatedAt: ts(),
    });
    await ctx.firestore().collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      createdAt: ts(),
    });
    await ctx.firestore().collection("crush_signals").doc(`${BOB_UID}_${ALICE_UID}`).set({
      fromUid: BOB_UID,
      toUid: ALICE_UID,
      createdAt: ts(),
    });
    await ctx.firestore().collection("mutual_crush_matches").doc(matchId).set({
      participantUids: sortedPair,
      matchedAtMs: pastMatchedMs,
      revealAtMs: pastRevealMs,
      alertAtMs: pastRevealMs - 86400000,
      alert24hDispatched: true,
      unlockedNotified: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
    await ctx.firestore().collection("mutual_crush_matches").doc("locked_match").set({
      participantUids: sortedPair,
      matchedAtMs: nowMs,
      revealAtMs: nowMs + 8 * 86400000,
      alertAtMs: nowMs + 7 * 86400000,
      alert24hDispatched: false,
      unlockedNotified: false,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  // Owner CAN list crush_signals, mutual_crush_matches, and users
  await assertSucceeds(ownerDb.collection("crush_signals").get());
  await assertSucceeds(ownerDb.collection("mutual_crush_matches").get());
  await assertSucceeds(ownerDb.collection("users").get());

  // Owner CANNOT update or delete other users' crush_signals or mutual_crush_matches
  await assertFails(
    ownerDb.collection("crush_signals").doc(`${ALICE_UID}_${BOB_UID}`).delete()
  );
  await assertFails(
    ownerDb.collection("mutual_crush_matches").doc(matchId).update({
      alert24hDispatched: false,
      updatedAt: ts(),
    })
  );
  await assertFails(
    ownerDb.collection("mutual_crush_matches").doc(matchId).delete()
  );

  // 4. Unlocked private_messages allowed for participants ONLY after revealAtMs; denied before revealAtMs, denied for Charlie & Owner
  await assertFails(
    aliceDb.collection("mutual_crush_matches").doc("locked_match").collection("private_messages").doc("pm_early").set({
      senderId: ALICE_UID,
      text: "Too early secret note",
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("mutual_crush_matches").doc(matchId).collection("private_messages").doc("pm_1").set({
      senderId: ALICE_UID,
      text: "Unlocked private note for you",
      moodEmoji: "💖",
      createdAt: ts(),
    })
  );
  await assertSucceeds(
    bobDb.collection("mutual_crush_matches").doc(matchId).collection("private_messages").get()
  );
  await assertFails(
    charlieDb.collection("mutual_crush_matches").doc(matchId).collection("private_messages").get()
  );
  await assertFails(
    ownerDb.collection("mutual_crush_matches").doc(matchId).collection("private_messages").get()
  );
});


