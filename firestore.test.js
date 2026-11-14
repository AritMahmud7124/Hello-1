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

test("Secret Love hides sender identity completely from recipient", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

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

  // Valid anonymous Secret Love atomic batch succeeds
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
    createdAt: ts(),
  });
  await assertSucceeds(goodBatch.commit());

  // Bob can query his Secret Love inbox and cannot read Alice's sent_secret_loves
  const bobInbox = await assertSucceeds(
    bobDb.collection("secret_loves").where("recipientId", "==", BOB_UID).get()
  );
  assert.strictEqual(bobInbox.docs.length, 1);
  assert.strictEqual(bobInbox.docs[0].data().revealedSenderId, "");
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sent_secret_loves").doc("sl_good").get());
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
