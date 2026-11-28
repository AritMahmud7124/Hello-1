export const REACTIONS = ['👍', '❤️', '😂', '😮', '😢'];
export const STORY_REACTIONS = ['❤️', '😂', '😮', '😢', '👏', '🔥'];
export const REEL_CATEGORIES = ['All', 'Trending', 'Comedy', 'Music', 'Gaming', 'Tech', 'Sports', 'Young', 'Funny', 'Lifestyle'];
export const PURE_LOVE_TIERS = [7, 30, 90, 180, 210, 270, 300, 365];

let ctx = null;
let matchChatUnsub = null;

export function initFeatures(context) {
  ctx = context;
}

export function isVerifiedOwnerUser() {
  const { state } = ctx;
  const uid = state.user?.uid;
  if (!uid) return false;
  if (state.ownerConfig?.ownerUid) return state.ownerConfig.ownerUid === uid;
  return state.user?.email === 'helloarit123@gmail.com' && Boolean(state.user?.emailVerified);
}

export function isAdminUser() {
  const { state } = ctx;
  const uid = state.user?.uid;
  if (!uid) return false;
  if (isVerifiedOwnerUser()) return true;
  if (state.me?.role === 'owner' || state.me?.role === 'admin') return true;
  return (state.admins || []).some((a) => (a.uid || a.id) === uid);
}

export function stopFeatureSubListeners() {
  if (matchChatUnsub) {
    try { matchChatUnsub(); } catch (_) {}
    matchChatUnsub = null;
  }
}

export async function reauthOwnerSession() {
  const { auth, GoogleAuthProvider, reauthenticateWithPopup, helpers, state } = ctx;
  if (!auth.currentUser) return;
  try {
    await reauthenticateWithPopup(auth.currentUser, new GoogleAuthProvider());
    state.lastReauthAt = Date.now();
    helpers.showBanner('Session verified (<15 min). Owner security actions unlocked.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Re-authentication failed.', true);
  }
}

export async function upsertPrivateUser(patch) {
  const { state, db, fs } = ctx;
  const uid = state.user?.uid;
  if (!uid) return;
  const ref = fs.doc(db, 'users', uid);
  const snap = await fs.getDoc(ref);
  if (!snap.exists()) {
    await fs.setDoc(ref, {
      userId: uid,
      email: state.user.email || '',
      phone: '',
      fcmToken: '',
      notifLikes: true,
      notifComments: true,
      notifFriends: true,
      notifMessages: true,
      notifSecretLove: true,
      notifSos: true,
      isDeactivated: false,
      createdAt: fs.serverTimestamp(),
      updatedAt: fs.serverTimestamp(),
      ...patch
    });
  } else {
    await fs.updateDoc(ref, {
      ...patch,
      updatedAt: fs.serverTimestamp()
    });
  }
}

export async function setPostReaction(post, emoji) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !post) return;
  try {
    const ref = fs.doc(db, 'posts', post.id, 'reactions', uid);
    const snap = await fs.getDoc(ref);
    const currentEmoji = snap.exists() ? snap.data()?.emoji : '';
    if (currentEmoji === emoji) {
      await fs.deleteDoc(ref);
      if ((post.likesCount || 0) > 0) {
        await fs.updateDoc(fs.doc(db, 'posts', post.id), {
          likesCount: fs.increment(-1),
          updatedAt: fs.serverTimestamp()
        });
      }
      state.likedByPost[post.id] = false;
      delete state.myReactionByPost[post.id];
    } else {
      await fs.setDoc(ref, {
        userId: uid,
        emoji,
        createdAt: fs.serverTimestamp()
      });
      if (!currentEmoji) {
        await fs.updateDoc(fs.doc(db, 'posts', post.id), {
          likesCount: fs.increment(1),
          updatedAt: fs.serverTimestamp()
        });
      }
      state.likedByPost[post.id] = true;
      state.myReactionByPost[post.id] = emoji;
    }
    state.reactionPickerPostId = null;
    helpers.render();
  } catch (e) {
    helpers.showBanner(e?.message || 'Reaction failed.', true);
  }
}

export async function saveEditedPost(postId, newText, newPrivacy) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !postId) return;
  const post = state.posts.find((p) => p.id === postId);
  if (!post) return;
  try {
    const friendUids = state.friendships
      .flatMap((f) => f.participantUids || [])
      .filter((x) => x !== state.user.uid);
    const allowedViewerUids = newPrivacy === 'friends' ? friendUids.slice(0, 200) : [];
    await fs.updateDoc(fs.doc(db, 'posts', postId), {
      content: (newText || '').trim().slice(0, 5000),
      mediaUri: post.mediaUri || '',
      mediaType: post.mediaType || 'none',
      privacy: ['public', 'friends', 'only_me'].includes(newPrivacy) ? newPrivacy : 'public',
      allowedViewerUids,
      updatedAt: fs.serverTimestamp()
    });
    state.editPostTarget = null;
    helpers.showBanner('Post updated.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update post.', true);
  }
}

export async function deletePostItem(postId) {
  const { db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'posts', postId));
    helpers.showBanner('Post deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete post.', true);
  }
}

export async function deleteCommentItem(postId, commentId) {
  const { state, db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'posts', postId, 'comments', commentId));
    await fs.updateDoc(fs.doc(db, 'posts', postId), {
      commentsCount: fs.increment(-1),
      updatedAt: fs.serverTimestamp()
    }).catch(() => {});
    if (state.commentsByPost[postId]) {
      state.commentsByPost[postId] = state.commentsByPost[postId].filter((c) => c.id !== commentId);
    }
    helpers.render();
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete comment.', true);
  }
}

export async function sharePostToFeed(post, noteText) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !post) return;
  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    const body = noteText?.trim()
      ? `${noteText.trim()}\n\n— Shared from ${post.authorName}: ${(post.content || '').slice(0, 300)}`
      : `Shared from ${post.authorName}:\n${(post.content || '').slice(0, 400)}`;
    await fs.addDoc(fs.collection(db, 'posts'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      content: body.slice(0, 5000),
      mediaUri: post.mediaUri || '',
      mediaType: post.mediaType || 'none',
      privacy: 'public',
      allowedViewerUids: [],
      groupId: '',
      pageId: '',
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      sharedFromPostId: post.id,
      createdAt: fs.serverTimestamp(),
      updatedAt: fs.serverTimestamp()
    });
    await fs.updateDoc(fs.doc(db, 'posts', post.id), {
      sharesCount: fs.increment(1),
      updatedAt: fs.serverTimestamp()
    }).catch(() => {});
    state.shareTarget = null;
    helpers.showBanner('Shared to your HELLO feed!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not share post.', true);
  }
}

export async function openStoryWithTracking(story) {
  const { state, db, fs, helpers } = ctx;
  state.activeStory = story;
  helpers.render();
  try {
    const snap = await fs.getDocs(fs.collection(db, 'stories', story.id, 'reactions'));
    state.storyReactionsById[story.id] = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    if (story.authorId !== state.user?.uid && !state.incognitoPrivateViewing) {
      await fs.updateDoc(fs.doc(db, 'stories', story.id), {
        viewersCount: fs.increment(1)
      }).catch(() => {});
    }
    helpers.render();
  } catch (_) {}
}

export async function reactToStory(storyId, emoji, replyText = '') {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !storyId) return;
  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
    const ref = fs.doc(db, 'stories', storyId, 'reactions', uid);
    const existing = await fs.getDoc(ref);
    if (existing.exists()) await fs.deleteDoc(ref);
    await fs.setDoc(ref, {
      userId: uid,
      userName: (me.displayName || 'Hello Member').slice(0, 80),
      emoji,
      replyText: (replyText || '').trim().slice(0, 300),
      createdAt: fs.serverTimestamp()
    });
    const snap = await fs.getDocs(fs.collection(db, 'stories', storyId, 'reactions'));
    state.storyReactionsById[storyId] = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    helpers.showBanner(`Reacted ${emoji} to story.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Story reaction failed.', true);
  }
}

export async function toggleStoryHighlight(story) {
  const { db, fs, helpers } = ctx;
  try {
    const next = !story.isSavedHighlight;
    await fs.updateDoc(fs.doc(db, 'stories', story.id), {
      isSavedHighlight: next
    });
    helpers.showBanner(next ? 'Saved to Story Highlights.' : 'Removed from Highlights.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update Highlight.', true);
  }
}

export async function deleteStoryItem(storyId) {
  const { state, db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'stories', storyId));
    state.activeStory = null;
    helpers.showBanner('Story deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete story.', true);
  }
}

export async function loadReelComments(reelId) {
  const { state, db, fs, helpers } = ctx;
  state.openReelCommentsId = reelId;
  try {
    const snap = await fs.getDocs(fs.collection(db, 'reels', reelId, 'comments'));
    state.reelCommentsById[reelId] = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
  } catch (_) {}
  helpers.render();
}

export async function submitReelComment(reelId, text) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !text.trim()) return;
  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    await fs.addDoc(fs.collection(db, 'reels', reelId, 'comments'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      text: text.trim().slice(0, 1000),
      createdAt: fs.serverTimestamp()
    });
    await fs.updateDoc(fs.doc(db, 'reels', reelId), { commentsCount: fs.increment(1) }).catch(() => {});
    await loadReelComments(reelId);
  } catch (e) {
    helpers.showBanner(e?.message || 'Reel comment failed.', true);
  }
}

export async function toggleSaveReel(reel) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !reel) return;
  try {
    const ref = fs.doc(db, 'users', state.user.uid, 'saved_posts', reel.id);
    if (state.savedPostIds.has(reel.id)) {
      await fs.deleteDoc(ref);
      helpers.showBanner('Removed Reel from Saved.');
    } else {
      await fs.setDoc(ref, {
        postId: reel.id,
        itemType: 'reel',
        authorName: (reel.authorName || 'Creator').slice(0, 80),
        excerpt: (reel.caption || 'Reel video').slice(0, 280),
        createdAt: fs.serverTimestamp()
      });
      helpers.showBanner('Reel bookmarked to Saved!');
    }
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not save Reel.', true);
  }
}

export async function deleteReelItem(reelId) {
  const { state, db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'reels', reelId));
    if (state.openReelCommentsId === reelId) state.openReelCommentsId = null;
    helpers.showBanner('Reel deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete Reel.', true);
  }
}

export async function submitReportItem(targetType, targetId, reason) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user) return;
  const cleanReason = (reason || '').trim();
  if (cleanReason.length < 2) {
    helpers.showBanner('Please describe why you are reporting this.', true);
    return;
  }
  try {
    await fs.addDoc(fs.collection(db, 'reports'), {
      reporterId: state.user.uid,
      targetType,
      targetId,
      reason: cleanReason.slice(0, 500),
      status: 'open',
      createdAt: fs.serverTimestamp(),
      updatedAt: fs.serverTimestamp()
    });
    state.reportTarget = null;
    helpers.showBanner('Report submitted to HELLO Moderation.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Report failed.', true);
  }
}

export async function blockTargetUser(targetUid, targetName = 'User') {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !helpers.isValidId(targetUid) || targetUid === state.user.uid) return;
  try {
    await fs.setDoc(fs.doc(db, 'users', state.user.uid, 'blocked', targetUid), {
      blockedUid: targetUid,
      blockedName: (targetName || 'User').slice(0, 80),
      createdAt: fs.serverTimestamp()
    });
    state.blockedNamesById[targetUid] = targetName;
    if (state.profileViewUid === targetUid) {
      state.profileViewUid = null;
      state.tab = 'feed';
    }
    helpers.showBanner(`Blocked ${targetName}.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not block user.', true);
  }
}

export async function unblockTargetUser(targetUid) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !targetUid) return;
  try {
    await fs.deleteDoc(fs.doc(db, 'users', state.user.uid, 'blocked', targetUid));
    helpers.showBanner('User unblocked.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not unblock user.', true);
  }
}

// --- Groups & Private Emergency SOS ---
export async function sendSosAlert(group, customMessage, shareLocation) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !group) return;
  const recipients = (group.memberUids || []).filter((id) => id !== uid).slice(0, 50);
  if (recipients.length === 0) {
    helpers.showBanner('Add at least one other member to this group before triggering SOS.', true);
    return;
  }

  let lat = 0.0;
  let lng = 0.0;
  let actualShare = Boolean(shareLocation);
  if (actualShare && navigator.geolocation) {
    try {
      const pos = await new Promise((resolve, reject) =>
        navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 7000 })
      );
      lat = Number(pos.coords.latitude) || 0.0;
      lng = Number(pos.coords.longitude) || 0.0;
    } catch (_) {
      actualShare = false;
      lat = 0.0;
      lng = 0.0;
    }
  } else {
    actualShare = false;
  }

  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
    const senderName = (me.displayName || 'Hello Member').slice(0, 80);
    const msg = (customMessage || 'Emergency SOS! Please check on me immediately.').trim().slice(0, 500);

    await fs.addDoc(fs.collection(db, 'groups', group.id, 'sos_alerts'), {
      groupId: group.id,
      senderId: uid,
      senderName,
      message: msg,
      recipientUids: recipients,
      shareLiveLocation: actualShare,
      latitude: actualShare ? lat : 0.0,
      longitude: actualShare ? lng : 0.0,
      status: 'active',
      createdAt: fs.serverTimestamp(),
      updatedAt: fs.serverTimestamp()
    });

    await Promise.all(recipients.slice(0, 20).map((rid) =>
      fs.addDoc(fs.collection(db, 'users', rid, 'notifications'), {
        recipientId: rid,
        actorId: uid,
        actorName: senderName,
        type: 'sos_alert',
        title: `🚨 SOS Alert in ${group.name}`.slice(0, 120),
        body: msg.slice(0, 400),
        targetId: group.id,
        isRead: false,
        createdAt: fs.serverTimestamp()
      }).catch(() => {})
    ));

    state.sosModalGroup = null;
    helpers.showBanner(`Emergency SOS sent privately to ${recipients.length} member(s) in ${group.name}.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not send SOS alert.', true);
  }
}

export async function stopSosAlert(groupId, alertId, nextStatus = 'stopped') {
  const { db, fs, helpers } = ctx;
  try {
    await fs.updateDoc(fs.doc(db, 'groups', groupId, 'sos_alerts', alertId), {
      shareLiveLocation: false,
      latitude: 0.0,
      longitude: 0.0,
      status: nextStatus,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(`SOS marked as ${nextStatus}.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update SOS alert.', true);
  }
}

export async function addGroupMember(group, memberUid) {
  const { db, fs, helpers } = ctx;
  if (!group || !memberUid) return;
  try {
    if ((group.memberUids || []).includes(memberUid)) return;
    await fs.updateDoc(fs.doc(db, 'groups', group.id), {
      memberUids: fs.arrayUnion(memberUid),
      membersCount: (group.memberUids || []).length + 1,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner('Member added to group.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not add member.', true);
  }
}

export async function removeGroupMember(group, memberUid) {
  const { db, fs, helpers } = ctx;
  if (!group || !memberUid || memberUid === group.creatorId) return;
  try {
    await fs.updateDoc(fs.doc(db, 'groups', group.id), {
      memberUids: fs.arrayRemove(memberUid),
      adminUids: fs.arrayRemove(memberUid),
      membersCount: Math.max(1, (group.memberUids || []).length - 1),
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner('Member removed.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not remove member.', true);
  }
}

export async function deleteGroupItem(groupId) {
  const { state, db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'groups', groupId));
    state.activeGroupId = null;
    helpers.showBanner('Group deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete group.', true);
  }
}

export async function deleteMarketplaceItem(id) {
  const { db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'marketplace_listings', id));
    helpers.showBanner('Listing deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete listing.', true);
  }
}

export async function deletePageItem(id) {
  const { state, db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'pages', id));
    if (state.activePageId === id) state.activePageId = null;
    helpers.showBanner('Page deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete page.', true);
  }
}

export async function toggleFollowPage(pg) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !pg) return;
  try {
    const existing = Array.isArray(pg.followerUids) ? pg.followerUids : [];
    const isFollowing = existing.includes(uid);
    const nextFollowers = isFollowing
      ? existing.filter((x) => x !== uid)
      : Array.from(new Set([...existing, uid]));
    await fs.updateDoc(fs.doc(db, 'pages', pg.id), {
      followerUids: nextFollowers,
      followersCount: nextFollowers.length,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(isFollowing ? `Unfollowed ${pg.name}` : `Following ${pg.name}`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update Page follow state.', true);
  }
}

export async function addPageFollower(pg, targetUid) {
  const { db, fs, helpers } = ctx;
  if (!pg || !targetUid) return;
  try {
    const existing = Array.isArray(pg.followerUids) ? pg.followerUids : [];
    if (existing.includes(targetUid)) return;
    const next = [...existing, targetUid];
    await fs.updateDoc(fs.doc(db, 'pages', pg.id), {
      followerUids: next,
      followersCount: next.length,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner('Member added to Page.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not add Page member.', true);
  }
}

export async function removePageFollower(pg, targetUid) {
  const { db, fs, helpers } = ctx;
  if (!pg || !targetUid || targetUid === pg.creatorId) return;
  try {
    const existing = Array.isArray(pg.followerUids) ? pg.followerUids : [];
    const next = existing.filter((x) => x !== targetUid);
    const nextAdmins = (Array.isArray(pg.adminUids) ? pg.adminUids : [pg.creatorId])
      .filter((x) => x !== targetUid || x === pg.creatorId);
    if (!nextAdmins.includes(pg.creatorId)) nextAdmins.push(pg.creatorId);
    await fs.updateDoc(fs.doc(db, 'pages', pg.id), {
      followerUids: next,
      followersCount: next.length,
      adminUids: nextAdmins,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner('Member removed from Page.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not remove Page member.', true);
  }
}

export async function togglePageAdmin(pg, targetUid) {
  const { db, fs, helpers } = ctx;
  if (!pg || !targetUid || targetUid === pg.creatorId) return;
  try {
    const admins = Array.isArray(pg.adminUids) ? [...pg.adminUids] : [pg.creatorId];
    const isAd = admins.includes(targetUid);
    const nextAdmins = isAd ? admins.filter((x) => x !== targetUid) : Array.from(new Set([...admins, targetUid]));
    if (!nextAdmins.includes(pg.creatorId)) nextAdmins.push(pg.creatorId);
    const followers = Array.isArray(pg.followerUids) ? pg.followerUids : [];
    const nextFollowers = !isAd && !followers.includes(targetUid) ? [...followers, targetUid] : followers;
    await fs.updateDoc(fs.doc(db, 'pages', pg.id), {
      adminUids: nextAdmins,
      followerUids: nextFollowers,
      followersCount: nextFollowers.length,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(isAd ? 'Removed Page Admin role.' : 'Promoted to Page Admin!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update Page Admin.', true);
  }
}

export async function saveEditedPage(pageId, form) {
  const { state, db, fs, helpers } = ctx;
  const pg = state.pages.find((x) => x.id === pageId);
  if (!pg || !state.user) return;
  const name = (form.name || '').trim();
  if (name.length < 2) {
    helpers.showBanner('Page name must be at least 2 characters.', true);
    return;
  }
  try {
    let avatarUrl = pg.avatarUrl || '';
    let coverUrl = pg.coverUrl || '';
    if (state.pageAvatarFile && helpers.uploadMediaBlob) {
      avatarUrl = await helpers.uploadMediaBlob(state.pageAvatarFile, '', 'Uploading Page avatar...');
    }
    if (state.pageCoverFile && helpers.uploadMediaBlob) {
      coverUrl = await helpers.uploadMediaBlob(state.pageCoverFile, '', 'Uploading Page cover...');
    }
    await fs.updateDoc(fs.doc(db, 'pages', pageId), {
      name: name.slice(0, 100),
      category: (form.category || pg.category || 'Creator').slice(0, 40),
      bio: (form.bio || '').trim().slice(0, 600),
      avatarUrl,
      coverUrl,
      updatedAt: fs.serverTimestamp()
    });
    state.editPageTarget = null;
    if (helpers.setPreviewFile) {
      helpers.setPreviewFile('pageAvatarFile', 'pageAvatarPreview', null);
      helpers.setPreviewFile('pageCoverFile', 'pageCoverPreview', null);
    }
    helpers.showBanner('Page updated!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update Page.', true);
  }
}

export async function saveEditedListing(listingId, form) {
  const { state, db, fs, helpers } = ctx;
  const item = state.marketplace.find((x) => x.id === listingId);
  if (!item || !state.user) return;
  const title = (form.title || '').trim();
  const price = parseFloat(form.price ?? '0');
  if (title.length < 2 || Number.isNaN(price) || price < 0) {
    helpers.showBanner('Enter a valid product title and price.', true);
    return;
  }
  try {
    let imageUri = item.imageUri || '';
    if (state.mktMediaFile && helpers.uploadMediaBlob) {
      imageUri = await helpers.uploadMediaBlob(state.mktMediaFile, '', 'Updating listing photo...');
    }
    await fs.updateDoc(fs.doc(db, 'marketplace_listings', listingId), {
      title: title.slice(0, 120),
      description: (form.description || '').trim().slice(0, 1000),
      price,
      currency: (form.currency || '$').slice(0, 6),
      category: (form.category || 'Other').slice(0, 40),
      condition: (form.condition || 'Good').slice(0, 40),
      locationText: (form.locationText || 'Local').trim().slice(0, 100) || 'Local',
      imageUri,
      updatedAt: fs.serverTimestamp()
    });
    state.editListingTarget = null;
    if (helpers.setPreviewFile) {
      helpers.setPreviewFile('mktMediaFile', 'mktMediaPreview', null);
    }
    helpers.showBanner('Marketplace listing updated!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update listing.', true);
  }
}

export async function saveProfileModalChanges(form) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user) return;
  const name = (form.displayName || '').trim();
  if (!name) {
    helpers.showBanner('Display name cannot be empty.', true);
    return;
  }
  try {
    await fs.updateDoc(fs.doc(db, 'users_public', state.user.uid), {
      displayName: name.slice(0, 80),
      bio: (form.bio || '').trim().slice(0, 300),
      profilePrivacy: ['public', 'friends', 'private'].includes(form.profilePrivacy) ? form.profilePrivacy : (state.me?.profilePrivacy || 'public'),
      acceptSecretLove: Boolean(form.acceptSecretLove),
      updatedAt: fs.serverTimestamp()
    });
    state.showEditProfileModal = false;
    helpers.showBanner('Profile saved across phone & web!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update profile.', true);
  }
}

export async function deleteEventItem(id) {
  const { db, fs, helpers } = ctx;
  try {
    await fs.deleteDoc(fs.doc(db, 'events', id));
    helpers.showBanner('Event deleted.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not delete event.', true);
  }
}

export async function leaveGroupConversation(conv) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !conv || !conv.isGroup) return;
  if ((conv.participantUids || []).length <= 2) {
    helpers.showBanner('Group conversations must have at least 2 remaining participants.', true);
    return;
  }
  try {
    await fs.updateDoc(fs.doc(db, 'conversations', conv.id), {
      participantUids: fs.arrayRemove(uid),
      updatedAt: fs.serverTimestamp()
    });
    state.activeConvId = null;
    state.mobileShowChat = false;
    helpers.showBanner('Left group chat.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not leave chat.', true);
  }
}

let typingTimer = null;
export function setTypingState(convId, isTyping) {
  const { state, db, fs } = ctx;
  const uid = state.user?.uid;
  if (!uid || !convId || state.incognitoHideStatus) return;
  clearTimeout(typingTimer);
  fs.updateDoc(fs.doc(db, 'conversations', convId), {
    typingUids: isTyping ? fs.arrayUnion(uid) : fs.arrayRemove(uid),
    updatedAt: fs.serverTimestamp()
  }).catch(() => {});
  if (isTyping) {
    typingTimer = setTimeout(() => {
      fs.updateDoc(fs.doc(db, 'conversations', convId), {
        typingUids: fs.arrayRemove(uid),
        updatedAt: fs.serverTimestamp()
      }).catch(() => {});
    }, 3500);
  }
}

// --- Secret Love A-to-Z (Pure Love Quest, Stealth Mutual Vault, Private Chat, Send & Reveal) ---
export async function toggleSecretCrush(targetUser) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  const targetUid = targetUser?.userId || targetUser?.id;
  if (!uid || !targetUid || uid === targetUid) return;

  const isSelected = state.secretCrushes.some((c) => c.id === targetUid || c.targetUid === targetUid);
  const pair = [uid, targetUid].sort();
  const matchId = `${pair[0]}_${pair[1]}`;

  try {
    if (isSelected) {
      const batch = fs.writeBatch(db);
      batch.delete(fs.doc(db, 'users', uid, 'secret_crushes', targetUid));
      batch.delete(fs.doc(db, 'crush_signals', `${uid}_${targetUid}`));
      batch.delete(fs.doc(db, 'mutual_crush_matches', matchId));
      await batch.commit();
      helpers.showBanner('Removed from your private Crush Picks.');
    } else {
      const batch = fs.writeBatch(db);
      batch.set(fs.doc(db, 'users', uid, 'secret_crushes', targetUid), {
        targetUid,
        targetName: (targetUser.displayName || 'Member').slice(0, 80),
        createdAt: fs.serverTimestamp()
      });
      batch.set(fs.doc(db, 'crush_signals', `${uid}_${targetUid}`), {
        fromUid: uid,
        toUid: targetUid,
        createdAt: fs.serverTimestamp()
      });
      await batch.commit();

      // Silently check mutual match without notifying immediately (7-14d vault)
      const reverseSnap = await fs.getDoc(fs.doc(db, 'crush_signals', `${targetUid}_${uid}`)).catch(() => null);
      if (reverseSnap?.exists()) {
        const existingMatch = await fs.getDoc(fs.doc(db, 'mutual_crush_matches', matchId)).catch(() => null);
        if (!existingMatch?.exists()) {
          const nowMs = Date.now();
          const randDaysMs = (7 + Math.floor(Math.random() * 8)) * 86400000;
          const revealAtMs = Math.min(nowMs + 1209600000, Math.max(nowMs + 604800000, nowMs + randDaysMs));
          const alertAtMs = revealAtMs - 86400000;
          await fs.setDoc(fs.doc(db, 'mutual_crush_matches', matchId), {
            participantUids: pair,
            matchedAtMs: nowMs,
            revealAtMs,
            alertAtMs,
            alert24hDispatched: false,
            unlockedNotified: false,
            createdAt: fs.serverTimestamp(),
            updatedAt: fs.serverTimestamp()
          }).catch(() => {});
        }
      }
      helpers.showBanner('Secret Crush saved privately (strictly hidden unless mutual & unlocked).');
    }
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update Secret Crush.', true);
  }
}

export async function startDevotionTrial(recipientUser, targetTierDays = 7) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  const rid = recipientUser?.userId || recipientUser?.id;
  if (!uid || !rid || uid === rid) return;
  try {
    const trialId = `${uid}_${rid}`;
    await fs.setDoc(fs.doc(db, 'devotion_trials', trialId), {
      senderId: uid,
      recipientId: rid,
      recipientName: (recipientUser.displayName || 'Special Someone').slice(0, 80),
      targetTierDays: PURE_LOVE_TIERS.includes(Number(targetTierDays)) ? Number(targetTierDays) : 7,
      completedDays: 0,
      currentStreak: 0,
      missedDaysCount: 0,
      successRatePct: 100,
      lastCheckInMs: 0,
      lastReflection: '',
      unlockedBadges: [],
      createdAt: fs.serverTimestamp(),
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(`Pure Love Quest started for ${recipientUser.displayName}! Complete 7 daily check-ins to unlock Secret Notes.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not start Pure Love Quest.', true);
  }
}

export async function submitDevotionCheckIn(trial, reflection) {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !trial) return;
  const text = (reflection || '').trim();
  if (text.length < 120) {
    helpers.showBanner(`Write at least 120 characters for your Pure Love reflection (${text.length}/120).`, true);
    return;
  }
  const nowMs = Date.now();
  if (trial.lastCheckInMs > 0 && nowMs - trial.lastCheckInMs < 72000000) {
    const hrsLeft = Math.ceil((72000000 - (nowMs - trial.lastCheckInMs)) / 3600000);
    helpers.showBanner(`Next daily check-in unlocks in ~${hrsLeft} hour(s) (20h cooldown).`, true);
    return;
  }
  try {
    const nextDays = (trial.completedDays || 0) + 1;
    const badges = Array.from(new Set([
      ...(trial.unlockedBadges || []),
      ...PURE_LOVE_TIERS.filter((t) => nextDays >= t).map((t) => `Day ${t} Devotion`)
    ])).slice(0, 16);
    await fs.updateDoc(fs.doc(db, 'devotion_trials', trial.id), {
      completedDays: nextDays,
      currentStreak: (trial.currentStreak || 0) + 1,
      lastCheckInMs: nowMs,
      lastReflection: text.slice(0, 1000),
      unlockedBadges: badges,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(`Day ${nextDays} Pure Love Check-In recorded!`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Check-in rejected by server cooldown.', true);
  }
}

export async function sendSecretLoveNote(recipientUser, message, moodEmoji = '❤️') {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  const rid = recipientUser?.userId || recipientUser?.id;
  if (!uid || !rid || !message.trim()) return;
  const trial = state.devotionTrials.find((t) => t.recipientId === rid);
  if (!trial || (trial.completedDays || 0) < 7) {
    helpers.showBanner('Complete Day 7 of the Pure Love Quest for this recipient before sending.', true);
    return;
  }
  try {
    const msgId = crypto.randomUUID ? crypto.randomUUID().replace(/-/g, '') : `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
    const cleanMsg = message.trim().slice(0, 1000);
    const badge = (trial.unlockedBadges || []).slice(-1)[0] || 'Pure Love Day 7';

    const batch = fs.writeBatch(db);
    batch.set(fs.doc(db, 'users', uid, 'sent_secret_loves', msgId), {
      messageId: msgId,
      recipientId: rid,
      recipientName: (recipientUser.displayName || 'Admirer').slice(0, 80),
      message: cleanMsg,
      moodEmoji: moodEmoji || '❤️',
      isRevealed: false,
      createdAt: fs.serverTimestamp()
    });
    batch.set(fs.doc(db, 'secret_loves', msgId), {
      recipientId: rid,
      message: cleanMsg,
      moodEmoji: moodEmoji || '❤️',
      isRevealed: false,
      revealedSenderId: '',
      revealedSenderName: '',
      isRead: false,
      isBlocked: false,
      isReported: false,
      senderBadgeTitle: badge.slice(0, 60),
      senderTrialDays: trial.completedDays || 7,
      createdAt: fs.serverTimestamp()
    });
    batch.set(fs.doc(fs.collection(db, 'users', rid, 'notifications')), {
      recipientId: rid,
      actorId: uid,
      actorName: 'Anonymous Admirer',
      type: 'secret_love',
      title: 'Someone sent you a Secret Love ❤️',
      body: 'Open Secret Love Inbox to read your anonymous note.',
      targetId: msgId,
      isRead: false,
      createdAt: fs.serverTimestamp()
    });
    await batch.commit();
    state.secretNoteDraft = '';
    state.secretTab = 'sent';
    helpers.showBanner('Anonymous Secret Love delivered!');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not send Secret Love note.', true);
  }
}

export async function revealSecretLoveNote(sentItem) {
  const { state, db, fs, helpers } = ctx;
  const uid = state.user?.uid;
  if (!uid || !sentItem) return;
  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
    const msgId = sentItem.messageId || sentItem.id;
    const batch = fs.writeBatch(db);
    batch.update(fs.doc(db, 'secret_loves', msgId), {
      isRevealed: true,
      revealedSenderId: uid,
      revealedSenderName: (me.displayName || 'Hello Member').slice(0, 80)
    });
    batch.update(fs.doc(db, 'users', uid, 'sent_secret_loves', msgId), {
      isRevealed: true
    });
    await batch.commit();
    helpers.showBanner('Your identity has been revealed to the recipient.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not reveal identity.', true);
  }
}

export function openPrivateMatchChat(matchId) {
  const { state, db, fs, helpers } = ctx;
  if (matchChatUnsub) {
    try { matchChatUnsub(); } catch (_) {}
    matchChatUnsub = null;
  }
  state.privateChatMatchId = matchId;
  state.privateMatchMsgs = [];
  const match = state.mutualMatches.find((m) => m.id === matchId);
  if (!match || Date.now() < (match.revealAtMs || Infinity)) {
    helpers.showBanner('This mutual vault has not unlocked yet.', true);
    state.privateChatMatchId = null;
    helpers.render();
    return;
  }
  matchChatUnsub = fs.onSnapshot(fs.collection(db, 'mutual_crush_matches', matchId, 'private_messages'), (snap) => {
    state.privateMatchMsgs = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    helpers.render();
  });
  helpers.render();
}

export async function sendPrivateMatchMessage(matchId, text, moodEmoji = '❤️') {
  const { state, db, fs, helpers } = ctx;
  if (!state.user || !text.trim()) return;
  try {
    await fs.addDoc(fs.collection(db, 'mutual_crush_matches', matchId, 'private_messages'), {
      senderId: state.user.uid,
      text: text.trim().slice(0, 2000),
      moodEmoji,
      createdAt: fs.serverTimestamp()
    });
    state.privateMatchDraft = '';
    helpers.render();
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not send private message.', true);
  }
}

export function getMatchDisplay(match) {
  const { state, helpers } = ctx;
  const uid = state.user?.uid;
  const peerUid = (match.participantUids || []).find((x) => x !== uid) || '';
  const peer = helpers.resolveAuthor(peerUid, 'Secret Match');
  try {
    const saved = JSON.parse(localStorage.getItem(`hello_mask_${match.id}`) || 'null');
    if (saved && (saved.alias || saved.hideAvatar)) {
      return {
        peerUid,
        name: saved.alias?.trim() ? saved.alias.trim() : peer.name,
        avatar: saved.hideAvatar ? '' : peer.avatar,
        isMasked: true,
        alias: saved.alias || '',
        hideAvatar: Boolean(saved.hideAvatar)
      };
    }
  } catch (_) {}
  return { peerUid, name: peer.name, avatar: peer.avatar, isMasked: false, alias: '', hideAvatar: false };
}

// --- Owner & Admin Governance ---
export async function saveGlobalSettings(form) {
  const { state, db, fs, helpers } = ctx;
  if (!isVerifiedOwnerUser()) return;
  try {
    const ref = fs.doc(db, 'app_config', 'global_settings');
    const payload = {
      appName: (form.appName || 'HELLO').trim().slice(0, 40) || 'HELLO',
      tagline: (form.tagline || '').trim().slice(0, 160),
      maintenanceBanner: (form.maintenanceBanner || '').trim().slice(0, 300),
      sosFeatureEnabled: Boolean(form.sosFeatureEnabled),
      secretLoveEnabled: Boolean(form.secretLoveEnabled),
      allowNewRegistrations: Boolean(form.allowNewRegistrations),
      updatedAt: fs.serverTimestamp()
    };
    const existing = await fs.getDoc(ref);
    if (!existing.exists()) await fs.setDoc(ref, payload);
    else await fs.updateDoc(ref, payload);
    state.globalSettings = { ...payload };
    helpers.showBanner('Global app settings saved!');
  } catch (e) {
    if (e?.code === 'permission-denied') {
      helpers.showBanner('Recent Google verification (<15 min) required. Click "Verify Session (<15m)" first.', true);
    } else {
      helpers.showBanner(e?.message || 'Could not save settings.', true);
    }
  }
}

export async function saveOwnerConfig(form) {
  const { state, db, fs, helpers } = ctx;
  if (!isVerifiedOwnerUser()) return;
  try {
    const ref = fs.doc(db, 'app_config', 'owner');
    const payload = {
      ownerUid: state.ownerConfig?.ownerUid || state.user.uid,
      ownerName: (form.ownerName || 'HELLO Founder').trim().slice(0, 80) || 'HELLO Founder',
      ownerHandle: (form.ownerHandle || '@hello_owner').trim().slice(0, 40),
      showOwnerHandle: Boolean(form.showOwnerHandle),
      ownerAvatar: state.ownerConfig?.ownerAvatar || state.me?.photoUrl || state.user.photoURL || '',
      about: (form.about || 'Founder & Architect of HELLO Social Network.').trim().slice(0, 600),
      contactEmail: (form.contactEmail || state.user.email || '').trim().slice(0, 160),
      showContactEmail: Boolean(form.showContactEmail),
      contactInfo: (form.contactInfo || '').trim().slice(0, 200),
      updatedAt: fs.serverTimestamp()
    };
    const existing = await fs.getDoc(ref);
    if (!existing.exists()) await fs.setDoc(ref, payload);
    else await fs.updateDoc(ref, payload);
    state.ownerConfig = { ...payload };
    helpers.showBanner('Owner identity & profile updated!');
  } catch (e) {
    if (e?.code === 'permission-denied') {
      helpers.showBanner('Recent Google verification (<15 min) required. Click "Verify Session (<15m)" first.', true);
    } else {
      helpers.showBanner(e?.message || 'Could not save Owner config.', true);
    }
  }
}

export async function addAdminRecord(targetInput) {
  const { state, db, fs, helpers } = ctx;
  if (!isVerifiedOwnerUser()) return;
  const queryText = (targetInput || '').trim();
  if (!queryText) return;
  const matched = state.users.find((u) =>
    (u.userId || u.id) === queryText ||
    (u.handle || '').toLowerCase() === queryText.toLowerCase() ||
    (u.displayName || '').toLowerCase() === queryText.toLowerCase()
  );
  const targetUid = matched ? (matched.userId || matched.id) : (helpers.isValidId(queryText) ? queryText : '');
  if (!targetUid) {
    helpers.showBanner('Enter a valid User UID or @handle from HELLO users.', true);
    return;
  }
  try {
    await fs.setDoc(fs.doc(db, 'admins', targetUid), {
      uid: targetUid,
      email: '',
      displayName: (matched?.displayName || 'HELLO Admin').slice(0, 80),
      addedByUid: state.user.uid,
      createdAt: fs.serverTimestamp()
    });
    helpers.showBanner('Admin granted!');
  } catch (e) {
    if (e?.code === 'permission-denied') {
      helpers.showBanner('Click "Verify Session (<15m)" first to manage admins.', true);
    } else {
      helpers.showBanner(e?.message || 'Could not add admin.', true);
    }
  }
}

export async function removeAdminRecord(targetUid) {
  const { db, fs, helpers } = ctx;
  if (!isVerifiedOwnerUser() || !targetUid) return;
  try {
    await fs.deleteDoc(fs.doc(db, 'admins', targetUid));
    helpers.showBanner('Admin removed.');
  } catch (e) {
    helpers.showBanner(e?.message || 'Click "Verify Session (<15m)" first.', true);
  }
}

export async function updateReportStatus(reportId, status) {
  const { db, fs, helpers } = ctx;
  if (!isAdminUser()) return;
  try {
    await fs.updateDoc(fs.doc(db, 'reports', reportId), {
      status,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner(`Report marked ${status}.`);
  } catch (e) {
    helpers.showBanner(e?.message || 'Could not update report.', true);
  }
}

// --- A-to-Z Screen Renderers ---

export function renderSosTab() {
  const { state, helpers } = ctx;
  const { esc, fmtTime, iconSvg } = helpers;
  const uid = state.user.uid;
  const sosGroups = state.groupsMine.filter((g) => g.sosEnabled !== false);
  const allAlerts = Object.values(state.sosAlertsByGroup).flat()
    .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  const activeIncoming = allAlerts.filter((a) => a.status === 'active' && a.senderId !== uid);
  const myActive = allAlerts.filter((a) => a.status === 'active' && a.senderId === uid);

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <div class="sos-hero">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
          <div>
            <div style="font-size:21px;font-weight:800;">🚨 Emergency SOS Hub</div>
            <div style="font-size:13.5px;opacity:0.92;margin-top:4px;">Private safety alerts sent strictly to your trusted group circles — never public.</div>
          </div>
          <span class="badge" style="background:rgba(255,255,255,0.18);color:#fff;">Private Circle Safety</span>
        </div>
      </div>

      ${activeIncoming.length ? `
        <div class="card sos-alert-card">
          <div style="font-weight:800;color:#FB7185;margin-bottom:8px;">LIVE INCOMING SOS ALERTS (${activeIncoming.length})</div>
          <div style="display:flex;flex-direction:column;gap:8px;">
            ${activeIncoming.map((al) => {
              const g = state.groups.find((x) => x.id === al.groupId);
              const hasCoords = al.shareLiveLocation && (al.latitude || al.longitude);
              return `
                <div style="padding:12px;border-radius:14px;background:rgba(244,63,94,0.1);border:1px solid rgba(244,63,94,0.35);">
                  <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
                    <strong>${esc(al.senderName)} in ${esc(g?.name || 'Group')}</strong>
                    <span class="muted">${fmtTime(al.createdAt)}</span>
                  </div>
                  <div style="margin:6px 0;font-size:14px;">${esc(al.message)}</div>
                  <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px;">
                    ${hasCoords ? `<a class="btn btn-primary" href="https://maps.google.com/?q=${Number(al.latitude)},${Number(al.longitude)}" target="_blank" rel="noopener noreferrer">Open Map (${Number(al.latitude).toFixed(4)}, ${Number(al.longitude).toFixed(4)})</a>` : `<span class="muted">Location not attached</span>`}
                    <button class="btn btn-secondary msg-person-btn" data-uid="${esc(al.senderId)}">${iconSvg('chat', 15)}<span>Message</span></button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      ` : ''}

      ${myActive.length ? `
        <div class="card">
          <div style="font-weight:800;margin-bottom:8px;">Your Active SOS Broadcasts (${myActive.length})</div>
          <div style="display:flex;flex-direction:column;gap:8px;">
            ${myActive.map((al) => {
              const g = state.groups.find((x) => x.id === al.groupId);
              return `
                <div style="padding:12px;border-radius:14px;background:var(--elevated);display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
                  <div>
                    <div style="font-weight:700;">${esc(g?.name || 'Group')} · ${fmtTime(al.createdAt)}</div>
                    <div class="muted">${esc(al.message)}</div>
                  </div>
                  <div style="display:flex;gap:8px;">
                    <button class="btn btn-secondary stop-sos-btn" data-group-id="${esc(al.groupId)}" data-alert-id="${esc(al.id)}" data-status="stopped">I'm Safe (Stop)</button>
                    <button class="btn btn-danger stop-sos-btn" data-group-id="${esc(al.groupId)}" data-alert-id="${esc(al.id)}" data-status="cancelled">Cancel</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      ` : ''}

      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:12px;">
          <div>
            <h3 style="margin:0;">Your Trusted SOS Circles (${sosGroups.length})</h3>
            <div class="muted">Pick any joined group with multiple members to trigger an instant private SOS alert.</div>
          </div>
          <button class="btn btn-secondary" data-tab="groups">Manage Groups</button>
        </div>
        ${sosGroups.length === 0 ? `<div class="muted">Join or create a group first to set up a private SOS circle.</div>` : `
          <div class="grid-cards">
            ${sosGroups.map((g) => `
              <div class="card" style="background:var(--elevated);padding:14px;display:flex;flex-direction:column;gap:8px;">
                <div style="font-weight:800;font-size:15.5px;">${esc(g.name)}</div>
                <div class="muted">${esc(g.privacy)} · ${(g.memberUids || []).length} members</div>
                <div style="display:flex;gap:8px;margin-top:auto;">
                  <button class="btn btn-danger trigger-sos-open-btn" data-group-id="${esc(g.id)}" style="flex:1;">🚨 Trigger SOS</button>
                  <button class="btn btn-secondary open-group-btn" data-group-id="${esc(g.id)}">Open</button>
                </div>
              </div>
            `).join('')}
          </div>
        `}
      </div>
    </div>
  `;
}

export function renderSecretLoveFull() {
  const { state, helpers } = ctx;
  const { esc, fmtTime, avatarHtml } = helpers;
  const uid = state.user.uid;
  const allowLove = state.me?.acceptSecretLove !== false;
  const now = Date.now();
  const peers = state.users.filter((u) => (u.userId || u.id) !== uid && !state.blockedUids.has(u.userId || u.id));
  const selectedCrushIds = new Set(state.secretCrushes.map((c) => c.targetUid || c.id));
  const unlockedMatches = state.mutualMatches.filter((m) => now >= (m.revealAtMs || Infinity));
  const selectedRecUid = state.secretSendRecipientUid || peers[0]?.userId || peers[0]?.id || '';
  const activeTrial = state.devotionTrials.find((t) => t.recipientId === selectedRecUid);

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <div class="love-hero">
        <div style="font-size:22px;font-weight:800;">Secret Love ❤️ & Mutual Vault</div>
        <div style="font-size:13.5px;opacity:0.92;margin-top:4px;">Anonymous notes, 7–365 Day Pure Love Quest, stealth Crush Picks & Mutual Vault synced with Android.</div>
      </div>

      <div class="card" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
        <div>
          <div style="font-weight:700;">Allow Secret Love messages</div>
          <div class="muted">Let admirers send verified anonymous notes to your Inbox</div>
        </div>
        <button class="btn ${allowLove ? 'btn-primary' : 'btn-secondary'}" id="toggleAcceptLoveBtn">${allowLove ? 'Enabled ✓' : 'Disabled'}</button>
      </div>

      <div class="chip-row">
        <button class="filter-chip ${state.secretTab === 'inbox' ? 'active' : ''}" data-secret-tab="inbox">Inbox (${state.secretInbox.length})</button>
        <button class="filter-chip ${state.secretTab === 'sent' ? 'active' : ''}" data-secret-tab="sent">Sent (${state.secretSent.length})</button>
        <button class="filter-chip ${state.secretTab === 'send' ? 'active' : ''}" data-secret-tab="send">Pure Love & Send</button>
        <button class="filter-chip ${state.secretTab === 'crushes' ? 'active' : ''}" data-secret-tab="crushes">Crush Vault (${unlockedMatches.length})</button>
        <button class="filter-chip ${state.secretTab === 'guide' ? 'active' : ''}" data-secret-tab="guide">How It Works</button>
      </div>

      ${state.secretTab === 'inbox' ? `
        <div style="display:flex;flex-direction:column;gap:10px;">
          ${state.secretInbox.length === 0 ? `<div class="card muted">No Secret Love messages yet.</div>` : state.secretInbox.map((m) => `
            <div class="card" style="display:flex;flex-direction:column;gap:8px;${m.isRead ? '' : 'border-color:var(--rose);'}">
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-weight:800;font-size:15px;">${esc(m.moodEmoji || '❤️')} ${m.isRevealed ? `From ${esc(m.revealedSenderName)}` : 'Anonymous Admirer'}</span>
                <span class="muted" style="font-size:11px;">${fmtTime(m.createdAt)}</span>
              </div>
              ${m.senderBadgeTitle ? `<span class="badge">${esc(m.senderBadgeTitle)} · Day ${m.senderTrialDays || 0}</span>` : ''}
              <div style="font-size:14.5px;line-height:1.5;">${esc(m.message)}</div>
              <div style="display:flex;gap:8px;flex-wrap:wrap;">
                ${!m.isRead ? `<button class="btn btn-secondary mark-love-read-btn" data-love-id="${esc(m.id)}">Mark Read</button>` : ''}
                ${m.isRevealed && m.revealedSenderId ? `<button class="btn btn-primary msg-person-btn" data-uid="${esc(m.revealedSenderId)}">Message ${esc(m.revealedSenderName)}</button>` : ''}
                <button class="btn btn-secondary report-open-btn" data-rtype="secret_love" data-rid="${esc(m.id)}">Report</button>
                <button class="btn btn-secondary block-love-btn" data-love-id="${esc(m.id)}">Block</button>
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}

      ${state.secretTab === 'sent' ? `
        <div style="display:flex;flex-direction:column;gap:10px;">
          ${state.secretSent.length === 0 ? `<div class="card muted">No sent Secret Love notes yet. Start a Pure Love Quest under "Pure Love & Send".</div>` : state.secretSent.map((s) => `
            <div class="card" style="display:flex;flex-direction:column;gap:8px;">
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <div style="font-weight:700;">To ${esc(s.recipientName || 'Recipient')} ${esc(s.moodEmoji || '❤️')}</div>
                <span class="muted" style="font-size:11px;">${fmtTime(s.createdAt)}</span>
              </div>
              <div style="font-size:14px;">${esc(s.message)}</div>
              <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
                <span class="muted">${s.isRevealed ? 'Identity revealed ✓' : 'Sent anonymously'}</span>
                ${!s.isRevealed ? `<button class="btn btn-secondary reveal-love-btn" data-sent-id="${esc(s.id)}">Reveal My Identity</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}

      ${state.secretTab === 'send' ? `
        <div class="card" style="display:flex;flex-direction:column;gap:12px;">
          <h3 style="margin:0;">Pure Love Quest & Send Anonymous Note</h3>
          <div class="muted">Select a HELLO member. Firestore rules verify 7 completed Pure Love reflection days before delivering an anonymous note.</div>
          <select class="select-field" id="secretRecipientSelect">
            ${peers.map((p) => `<option value="${esc(p.userId || p.id)}" ${selectedRecUid === (p.userId || p.id) ? 'selected' : ''}>${esc(p.displayName)} (${esc(p.handle || '')})</option>`).join('')}
          </select>

          ${!selectedRecUid ? `<div class="muted">No discoverable recipients yet.</div>` : (!activeTrial ? `
            <div style="padding:14px;border-radius:14px;background:var(--elevated);display:flex;flex-direction:column;gap:10px;">
              <div style="font-weight:700;">Start Pure Love Quest</div>
              <div class="muted">Pick a devotion target (minimum 7 days to unlock Secret Note delivery):</div>
              <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                <select class="select-field" id="questTierSelect" style="width:auto;">
                  ${PURE_LOVE_TIERS.map((d) => `<option value="${d}" ${(state.questTierDays || 7) === d ? 'selected' : ''}>${d} Days</option>`).join('')}
                </select>
                <button class="btn btn-primary" id="startDevotionBtn">Start Quest</button>
              </div>
            </div>
          ` : `
            <div style="padding:14px;border-radius:14px;background:var(--elevated);display:flex;flex-direction:column;gap:10px;">
              <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:6px;">
                <strong>Quest for ${esc(activeTrial.recipientName)}: Day ${activeTrial.completedDays || 0} / ${activeTrial.targetTierDays || 7}</strong>
                <span class="badge">${(activeTrial.completedDays || 0) >= 7 ? 'Day 7+ Unlocked ✓' : `${Math.max(0, 7 - (activeTrial.completedDays || 0))} days to unlock`}</span>
              </div>
              <div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, Math.round(((activeTrial.completedDays || 0) / (activeTrial.targetTierDays || 7)) * 100))}%"></div></div>
              ${(activeTrial.unlockedBadges || []).length ? `<div class="chip-row">${activeTrial.unlockedBadges.map((b) => `<span class="badge">${esc(b)}</span>`).join('')}</div>` : ''}
              <textarea class="field" id="devotionReflectionInput" rows="3" placeholder="Write today's sincere reflection (minimum 120 characters)...">${esc(state.devotionReflectionDraft || '')}</textarea>
              <button class="btn btn-violet" id="submitDevotionBtn">Complete Daily Check-In</button>
            </div>

            ${(activeTrial.completedDays || 0) >= 7 ? `
              <div style="padding:14px;border-radius:14px;border:1px solid var(--rose);display:flex;flex-direction:column;gap:10px;">
                <div style="font-weight:800;">Send Anonymous Secret Love Note ❤️</div>
                <div class="chip-row">
                  ${['❤️', '🌹', '✨', '🥰', '💫'].map((em) => `<button type="button" class="filter-chip ${state.secretMoodEmoji === em ? 'active' : ''}" data-love-mood="${em}">${em}</button>`).join('')}
                </div>
                <textarea class="field" id="secretNoteInput" rows="3" placeholder="Write your anonymous compliment or confession...">${esc(state.secretNoteDraft || '')}</textarea>
                <button class="btn btn-primary" id="sendSecretLoveBtn">Send Secret Love Anonymously</button>
              </div>
            ` : ''}
          `)}
        </div>
      ` : ''}

      ${state.secretTab === 'crushes' ? `
        <div style="display:flex;flex-direction:column;gap:14px;">
          <div class="card">
            <h3 style="margin:0 0 6px;">Unlocked Mutual Vault (${unlockedMatches.length})</h3>
            <div class="muted" style="margin-bottom:10px;">Stealth vault stays strictly hidden until a mutual 7–14 day timer unlocks for both people.</div>
            ${unlockedMatches.length === 0 ? `<div class="muted">No unlocked mutual matches yet. Add your Crush Picks below silently.</div>` : unlockedMatches.map((m) => {
              const disp = getMatchDisplay(m);
              return `
                <div style="padding:12px;border-radius:14px;background:var(--elevated);display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:8px;">
                  <div style="display:flex;align-items:center;gap:10px;">
                    ${avatarHtml(disp.name, disp.avatar, 42, false)}
                    <div>
                      <div style="font-weight:800;">${esc(disp.name)} ${disp.isMasked ? '<span class="badge">Masked</span>' : ''}</div>
                      <div class="muted">Mutual Secret Crush Unlocked ✨</div>
                    </div>
                  </div>
                  <div style="display:flex;gap:8px;">
                    <button class="btn btn-secondary open-mask-modal-btn" data-match-id="${esc(m.id)}">Disguise</button>
                    <button class="btn btn-primary open-vault-chat-btn" data-match-id="${esc(m.id)}">Private Chat</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>

          <div class="card">
            <h3 style="margin:0 0 6px;">Secret Crush Picks (${state.secretCrushes.length})</h3>
            <div class="muted" style="margin-bottom:12px;">Zero-knowledge privacy: nobody ever knows unless they pick you too and the delayed vault unlocks.</div>
            <div class="grid-cards">
              ${peers.map((p) => {
                const pid = p.userId || p.id;
                const picked = selectedCrushIds.has(pid);
                return `
                  <div class="card" style="padding:12px;background:var(--elevated);display:flex;align-items:center;justify-content:space-between;gap:8px;">
                    <div style="display:flex;align-items:center;gap:8px;min-width:0;">
                      ${avatarHtml(p.displayName, p.photoUrl, 34, false)}
                      <span style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(p.displayName)}</span>
                    </div>
                    <button class="btn ${picked ? 'btn-primary' : 'btn-secondary'} toggle-crush-btn" data-uid="${esc(pid)}">${picked ? 'Picked ❤️' : 'Pick'}</button>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        </div>
      ` : ''}

      ${state.secretTab === 'guide' ? `
        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <h3 style="margin:0;">How Secret Love & Mutual Vault Work</h3>
          <div>1. <strong>Zero-Leak Crush Picks:</strong> When you pick someone as a Secret Crush, only you can read your pick list. They are never alerted unless they also pick you.</div>
          <div>2. <strong>Stealth 7–14 Day Vault:</strong> Mutual matches do not reveal immediately so no one can guess who picked them via timing. When unlocked, both get a private Vault Chat with optional Disguise Alias mode.</div>
          <div>3. <strong>Pure Love Quest:</strong> To send anonymous text notes, complete 7 daily reflections (min 120 chars, 20h apart) enforced by Firebase security rules.</div>
          <div>4. <strong>Self-Reveal Anytime:</strong> From Sent Notes, reveal your identity anytime when you're ready.</div>
        </div>
      ` : ''}
    </div>
  `;
}

export function renderSettingsTab() {
  const { state, helpers } = ctx;
  const { esc, iconSvg } = helpers;
  const me = state.me || {};
  const priv = state.privateUser || {};
  const blockedIds = [...state.blockedUids];

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <div class="card">
        <h2 style="margin:0;">Settings & Privacy</h2>
        <div class="muted">Account, Incognito mode, Notifications, Blocked users & App controls</div>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <h3 style="margin:0;">Appearance & Online Presence</h3>
        <div class="setting-row">
          <div>
            <div style="font-weight:700;">Dark / Light Theme</div>
            <div class="muted">Currently ${state.theme === 'dark' ? 'Night Dark' : 'Day Light'}</div>
          </div>
          <button class="btn btn-secondary" id="settingsThemeBtn">${iconSvg(state.theme === 'dark' ? 'sun' : 'moon', 16)}<span>Switch Theme</span></button>
        </div>
        <div class="setting-row">
          <div>
            <div style="font-weight:700;">Show Active Green Dot</div>
            <div class="muted">Let friends see when you are online</div>
          </div>
          <button class="btn ${me.showOnlineStatus !== false ? 'btn-primary' : 'btn-secondary'}" id="toggleOnlineDotBtn">${me.showOnlineStatus !== false ? 'On' : 'Off'}</button>
        </div>
        <div class="setting-row">
          <div>
            <div style="font-weight:700;">Incognito: Hide Typing & Status</div>
            <div class="muted">Browse Messenger quietly without broadcasting typing indicators</div>
          </div>
          <button class="btn ${state.incognitoHideStatus ? 'btn-violet' : 'btn-secondary'}" id="toggleIncognitoStatusBtn">${state.incognitoHideStatus ? 'Active' : 'Off'}</button>
        </div>
        <div class="setting-row">
          <div>
            <div style="font-weight:700;">Incognito: Private Story Viewing</div>
            <div class="muted">View public stories without incrementing viewer count</div>
          </div>
          <button class="btn ${state.incognitoPrivateViewing ? 'btn-violet' : 'btn-secondary'}" id="toggleIncognitoViewBtn">${state.incognitoPrivateViewing ? 'Active' : 'Off'}</button>
        </div>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <h3 style="margin:0;">Voice &amp; Gesture Control (Hands-Free)</h3>
        <div class="setting-row">
          <div style="flex:1;min-width:0;">
            <div style="font-weight:700;">Voice &amp; Gesture Control</div>
            <div class="muted">Off by default to save battery. Turn on to navigate &amp; control HELLO by voice ("Home", "Reels", "Marketplace", "Next", "Like", "Back") or front-camera hand gestures (Swipe, Open Palm, Thumbs Up / Pinch).</div>
          </div>
          <button class="btn ${state.vgEnabled ? 'btn-primary' : 'btn-secondary'}" id="toggleVoiceGestureBtn" type="button">${state.vgEnabled ? 'On ✓' : 'Off'}</button>
        </div>
        ${state.vgEnabled ? `
          <div class="notice-box" style="font-size:12.5px;">
            Active: Say "Reels", "Marketplace", "Profile", "Scroll down", "Next", "Previous", "Pause", "Like", or "Back". Show hand swipe / palm / thumbs-up to front camera.
          </div>
        ` : ''}
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <h3 style="margin:0;">Profile Privacy & Recovery</h3>
        <div>
          <label class="muted">Who can view your full profile?</label>
          <select class="select-field" id="settingsProfilePrivacy" style="margin-top:6px;">
            <option value="public" ${(me.profilePrivacy || 'public') === 'public' ? 'selected' : ''}>Public (Discoverable in People & Search)</option>
            <option value="friends" ${me.profilePrivacy === 'friends' ? 'selected' : ''}>Friends Only</option>
            <option value="private" ${me.profilePrivacy === 'private' ? 'selected' : ''}>Private (Hidden from Search)</option>
          </select>
        </div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:10px;">
          <input class="field" id="recoveryEmailInput" placeholder="Recovery email" value="${esc(priv.email || state.user?.email || '')}" />
          <input class="field" id="recoveryPhoneInput" placeholder="Recovery phone" value="${esc(priv.phone || '')}" />
        </div>
        <button class="btn btn-primary" id="saveRecoveryBtn" style="align-self:flex-start;">Save Recovery Info</button>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:10px;">
        <h3 style="margin:0;">Notification Preferences</h3>
        ${[
          ['notifMessages', 'Messenger & Calls'],
          ['notifFriends', 'Friend Requests'],
          ['notifLikes', 'Post Likes & Reactions'],
          ['notifComments', 'Post Comments'],
          ['notifSecretLove', 'Secret Love Alerts'],
          ['notifSos', 'Emergency SOS Circle Alerts']
        ].map(([k, lbl]) => {
          const on = priv[k] !== false;
          return `
            <div class="setting-row">
              <span>${lbl}</span>
              <button class="btn ${on ? 'btn-primary' : 'btn-secondary'} toggle-notif-pref-btn" data-pref-key="${k}">${on ? 'Enabled' : 'Muted'}</button>
            </div>
          `;
        }).join('')}
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:10px;">
        <h3 style="margin:0;">Blocked Users (${blockedIds.length})</h3>
        ${blockedIds.length === 0 ? `<div class="muted">No blocked users.</div>` : blockedIds.map((bid) => `
          <div class="setting-row">
            <span>${esc(state.blockedNamesById[bid] || state.usersById[bid]?.displayName || bid)}</span>
            <button class="btn btn-secondary unblock-user-btn" data-uid="${esc(bid)}">Unblock</button>
          </div>
        `).join('')}
      </div>

      <div class="card" style="display:flex;gap:10px;flex-wrap:wrap;justify-content:space-between;align-items:center;">
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <button class="btn btn-violet" id="installLaptopBtnSettings">${iconSvg('laptop', 16)}<span>Website &amp; Laptop App</span></button>
          <a class="btn btn-secondary" href="${esc(helpers.apkUrl || '')}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 16)}<span>Android APK</span></a>
        </div>
        <button class="btn btn-danger" id="signOutBtnSettings">${iconSvg('logout', 16)}<span>Sign Out</span></button>
      </div>
    </div>
  `;
}

export function renderOwnerProfileTab() {
  const { state, helpers } = ctx;
  const { esc, avatarHtml, iconSvg } = helpers;
  const cfg = state.ownerConfig || {};
  const ownerName = cfg.ownerName || 'HELLO Founder';
  const showHandle = cfg.showOwnerHandle !== false;
  const showEmail = Boolean(cfg.showContactEmail);
  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <div class="card" style="padding:22px;display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;align-items:center;gap:14px;flex-wrap:wrap;">
          ${avatarHtml(ownerName, cfg.ownerAvatar || '', 72, false)}
          <div>
            <h2 style="margin:0;display:flex;align-items:center;gap:6px;">
              <span>${esc(ownerName)}</span>
              <span class="verified-badge">${iconSvg('verified', 18)}</span>
            </h2>
            ${showHandle && cfg.ownerHandle ? `<div class="muted">${esc(cfg.ownerHandle)}</div>` : ''}
            <span class="badge" style="margin-top:4px;">Verified App Owner</span>
          </div>
        </div>
        <div style="font-size:14.5px;line-height:1.55;">${esc(cfg.about || 'Founder & Lead Architect of HELLO Social Network.')}</div>
        ${showEmail && cfg.contactEmail ? `<div class="muted">Contact Email: <strong>${esc(cfg.contactEmail)}</strong></div>` : ''}
        ${cfg.contactInfo ? `<div class="muted">${esc(cfg.contactInfo)}</div>` : ''}
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          ${cfg.ownerUid && cfg.ownerUid !== state.user.uid ? `<button class="btn btn-primary msg-person-btn" data-uid="${esc(cfg.ownerUid)}">${iconSvg('chat', 16)}<span>Message Founder</span></button>` : ''}
          ${isVerifiedOwnerUser() ? `<button class="btn btn-violet" data-tab="owner_dashboard">Open Owner Command Center</button>` : ''}
        </div>
      </div>
    </div>
  `;
}

export function renderOwnerDashboardTab() {
  const { state, helpers } = ctx;
  const { esc, fmtTime, iconSvg } = helpers;
  const isOwner = isVerifiedOwnerUser();
  const isAdmin = isAdminUser();
  if (!isAdmin) {
    return `<div class="card error-box">Owner / Admin privileges required to view Governance Dashboard.</div>`;
  }
  const gs = state.globalSettings || {
    appName: 'HELLO',
    tagline: 'Connect Real-Time',
    maintenanceBanner: '',
    sosFeatureEnabled: true,
    secretLoveEnabled: true,
    allowNewRegistrations: true
  };
  const oc = state.ownerConfig || {
    ownerName: state.me?.displayName || 'HELLO Founder',
    ownerHandle: '@hello_owner',
    showOwnerHandle: true,
    about: 'Founder & Lead Architect of HELLO Social Network.',
    contactEmail: state.user?.email || '',
    showContactEmail: false,
    contactInfo: ''
  };
  const dTab = state.ownerSubTab || 'overview';

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <div class="card" style="background:linear-gradient(135deg,#1E1B4B,#312E81);color:#fff;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
          <div>
            <h2 style="margin:0;color:#fff;">👑 HELLO Owner Command Center</h2>
            <div style="font-size:13px;opacity:0.9;">Zero-Trust Governance · Live Reports · App Controls · Owner Audit</div>
          </div>
          ${isOwner ? `<button class="btn btn-emerald" id="reauthOwnerBtn">${iconSvg('shield', 16)}<span>Verify Session (&lt;15m)</span></button>` : ''}
        </div>
      </div>

      <div class="chip-row">
        <button class="filter-chip ${dTab === 'overview' ? 'active' : ''}" data-owner-tab="overview">Overview & Controls</button>
        <button class="filter-chip ${dTab === 'reports' ? 'active' : ''}" data-owner-tab="reports">Reports (${state.reports.length})</button>
        ${isOwner ? `<button class="filter-chip ${dTab === 'admins' ? 'active' : ''}" data-owner-tab="admins">Admins (${state.admins.length})</button>` : ''}
        ${isOwner ? `<button class="filter-chip ${dTab === 'audit' ? 'active' : ''}" data-owner-tab="audit">Vault Audit</button>` : ''}
      </div>

      ${dTab === 'overview' ? `
        <div class="grid-cards">
          <div class="card"><strong>${state.users.length}</strong><div class="muted">Public Members</div></div>
          <div class="card"><strong>${state.posts.length}</strong><div class="muted">Active Posts</div></div>
          <div class="card"><strong>${state.reports.filter((r) => r.status === 'open').length}</strong><div class="muted">Open Reports</div></div>
          <div class="card"><strong>${state.groups.length}</strong><div class="muted">Groups</div></div>
        </div>
        ${isOwner ? `
          <div class="card" style="display:flex;flex-direction:column;gap:10px;">
            <h3 style="margin:0;">Global App Settings</h3>
            <input class="field" id="gsAppName" placeholder="App Name" value="${esc(gs.appName)}" />
            <input class="field" id="gsTagline" placeholder="Tagline" value="${esc(gs.tagline || '')}" />
            <input class="field" id="gsBanner" placeholder="Live Maintenance / Announcement Banner (blank = hidden)" value="${esc(gs.maintenanceBanner || '')}" />
            <label class="setting-row"><span>Emergency SOS Enabled</span><input type="checkbox" id="gsSosEnabled" ${gs.sosFeatureEnabled !== false ? 'checked' : ''} /></label>
            <label class="setting-row"><span>Secret Love Enabled</span><input type="checkbox" id="gsLoveEnabled" ${gs.secretLoveEnabled !== false ? 'checked' : ''} /></label>
            <label class="setting-row"><span>Allow New Registrations</span><input type="checkbox" id="gsRegEnabled" ${gs.allowNewRegistrations !== false ? 'checked' : ''} /></label>
            <button class="btn btn-primary" id="saveGlobalSettingsBtn" style="align-self:flex-start;">Save Global Settings</button>
          </div>

          <div class="card" style="display:flex;flex-direction:column;gap:10px;">
            <h3 style="margin:0;">Owner Verified Card</h3>
            <input class="field" id="ocOwnerName" placeholder="Owner Display Name" value="${esc(oc.ownerName)}" />
            <input class="field" id="ocOwnerHandle" placeholder="@handle" value="${esc(oc.ownerHandle || '')}" />
            <label class="setting-row"><span>Show Owner Handle Publicly</span><input type="checkbox" id="ocShowHandle" ${oc.showOwnerHandle !== false ? 'checked' : ''} /></label>
            <textarea class="field" id="ocAbout" rows="2" placeholder="About Founder">${esc(oc.about || '')}</textarea>
            <input class="field" id="ocEmail" placeholder="Public Contact Email" value="${esc(oc.contactEmail || '')}" />
            <label class="setting-row"><span>Show Contact Email Publicly</span><input type="checkbox" id="ocShowEmail" ${oc.showContactEmail ? 'checked' : ''} /></label>
            <input class="field" id="ocContactInfo" placeholder="Extra Contact Info" value="${esc(oc.contactInfo || '')}" />
            <button class="btn btn-primary" id="saveOwnerConfigBtn" style="align-self:flex-start;">Save Owner Card</button>
          </div>
        ` : ''}
      ` : ''}

      ${dTab === 'reports' ? `
        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <h3 style="margin:0;">Moderation Reports (${state.reports.length})</h3>
          ${state.reports.length === 0 ? `<div class="muted">No reports filed.</div>` : state.reports.map((r) => `
            <div style="padding:12px;border-radius:12px;background:var(--elevated);display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
              <div>
                <div style="font-weight:700;">[${esc(r.targetType)}] ${esc(r.targetId)} · <span class="badge">${esc(r.status)}</span></div>
                <div style="font-size:13px;margin-top:4px;">${esc(r.reason)}</div>
                <div class="muted" style="font-size:11px;">${fmtTime(r.createdAt)}</div>
              </div>
              <div style="display:flex;gap:6px;">
                <button class="btn btn-primary update-report-btn" data-report-id="${esc(r.id)}" data-status="resolved">Resolve</button>
                <button class="btn btn-secondary update-report-btn" data-report-id="${esc(r.id)}" data-status="dismissed">Dismiss</button>
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}

      ${dTab === 'admins' && isOwner ? `
        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <h3 style="margin:0;">Platform Admins (${state.admins.length})</h3>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <input class="field" id="addAdminInput" placeholder="Enter User UID or @handle" style="flex:1;" />
            <button class="btn btn-primary" id="addAdminBtn">Add Admin</button>
          </div>
          ${state.admins.map((a) => `
            <div class="setting-row">
              <div><strong>${esc(a.displayName)}</strong> <span class="muted">${esc(a.uid)}</span></div>
              <button class="btn btn-danger remove-admin-btn" data-uid="${esc(a.uid || a.id)}">Remove</button>
            </div>
          `).join('')}
        </div>
      ` : ''}

      ${dTab === 'audit' && isOwner ? `
        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <h3 style="margin:0;">Owner Read-Only Security Audit</h3>
          <div class="muted">Crush Signals: ${state.ownerSignals.length} · Mutual Vault Matches: ${state.ownerMatches.length} · Registered Private Users: ${state.ownerUsers.length}</div>
          ${state.ownerMatches.slice(0, 20).map((m) => `
            <div style="padding:10px;border-radius:10px;background:var(--elevated);font-size:13px;">
              Match <strong>${esc(m.id)}</strong> · Unlocked: ${Date.now() >= (m.revealAtMs || 0) ? 'Yes' : 'Locked until ' + new Date(m.revealAtMs || 0).toLocaleDateString()}
            </div>
          `).join('')}
        </div>
      ` : ''}
    </div>
  `;
}

export function renderFeatureModals() {
  const { state, helpers } = ctx;
  const { esc, fmtTime, avatarHtml, iconSvg } = helpers;
  const uid = state.user?.uid;

  if (state.editPostTarget) {
    const p = state.editPostTarget;
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Edit Post</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <textarea class="field" id="editPostContentInput" rows="4">${esc(p.content || '')}</textarea>
          <select class="select-field" id="editPostPrivacySelect">
            <option value="public" ${p.privacy === 'public' ? 'selected' : ''}>Public</option>
            <option value="friends" ${p.privacy === 'friends' ? 'selected' : ''}>Friends</option>
            <option value="only_me" ${p.privacy === 'only_me' ? 'selected' : ''}>Only Me</option>
          </select>
          <button class="btn btn-primary" id="saveEditPostBtn" data-post-id="${esc(p.id)}">Save Changes</button>
        </div>
      </div>
    `;
  }

  if (state.reportTarget) {
    const rt = state.reportTarget;
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Report Content</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div class="muted">Reporting ${esc(rt.type)} (${esc(rt.id)})</div>
          <textarea class="field" id="reportReasonInput" rows="3" placeholder="Tell moderators what violates HELLO safety rules..."></textarea>
          <button class="btn btn-danger" id="submitReportBtn">Submit Report</button>
        </div>
      </div>
    `;
  }

  if (state.shareTarget) {
    const p = state.shareTarget;
    const peers = state.users.filter((u) => (u.userId || u.id) !== uid && !state.blockedUids.has(u.userId || u.id)).slice(0, 10);
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Share Post</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="repostNoteInput" placeholder="Add a comment for your feed (optional)..." />
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-primary" id="shareRepostFeedBtn" style="flex:1;">Repost to My Feed</button>
            <button class="btn btn-secondary" id="shareCopyLinkBtn">Copy Link</button>
          </div>
          <div class="muted" style="margin-top:4px;">Or send directly in Messenger:</div>
          <div style="max-height:190px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${peers.map((u) => `
              <button class="side-item share-send-dm-btn" data-uid="${esc(u.userId || u.id)}">
                <span class="side-item-left">${avatarHtml(u.displayName, u.photoUrl, 30, false)}<span>${esc(u.displayName)}</span></span>
                <span class="badge">Send</span>
              </button>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }

  if (state.openReelCommentsId) {
    const rid = state.openReelCommentsId;
    const list = state.reelCommentsById[rid] || [];
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Reel Comments (${list.length})</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="max-height:260px;overflow-y:auto;display:flex;flex-direction:column;gap:8px;">
            ${list.length === 0 ? `<div class="muted">No comments yet. Be the first!</div>` : list.map((c) => `
              <div class="comment-item"><strong>${esc(c.authorName)}</strong>: ${esc(c.text)}</div>
            `).join('')}
          </div>
          <div style="display:flex;gap:8px;">
            <input class="field" id="reelCommentInput" placeholder="Add a comment..." style="flex:1;" />
            <button class="btn btn-primary" id="submitReelCommentBtn" data-reel-id="${esc(rid)}">Send</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.manageGroupModal) {
    const grp = state.groups.find((g) => g.id === state.manageGroupModal);
    if (!grp) return '';
    const memberSet = new Set(grp.memberUids || []);
    const nonMembers = state.users.filter((u) => !memberSet.has(u.userId || u.id));
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Manage ${esc(grp.name)} Members</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="font-weight:700;font-size:13px;">Current Members (${(grp.memberUids || []).length})</div>
          <div style="max-height:160px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${(grp.memberUids || []).map((mid) => {
              const u = helpers.resolveAuthor(mid, 'Member');
              const isCreator = mid === grp.creatorId;
              return `
                <div class="setting-row">
                  <span>${esc(u.name)} ${isCreator ? '<span class="badge">Creator</span>' : ''}</span>
                  ${!isCreator ? `<button class="btn btn-danger remove-grp-mem-btn" data-group-id="${esc(grp.id)}" data-uid="${esc(mid)}">Remove</button>` : ''}
                </div>
              `;
            }).join('')}
          </div>
          <div style="font-weight:700;font-size:13px;margin-top:6px;">Add Friends to Circle</div>
          <div style="max-height:160px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${nonMembers.length === 0 ? `<div class="muted">All visible members are already in this group.</div>` : nonMembers.map((u) => `
              <div class="setting-row">
                <span>${esc(u.displayName)}</span>
                <button class="btn btn-primary add-grp-mem-btn" data-group-id="${esc(grp.id)}" data-uid="${esc(u.userId || u.id)}">Add</button>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }

  if (state.sosModalGroup) {
    const grp = state.groups.find((g) => g.id === state.sosModalGroup);
    if (!grp) return '';
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;color:#FB7185;">🚨 Trigger Private SOS — ${esc(grp.name)}</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div class="muted">Sent privately to ${(grp.memberUids || []).filter((x) => x !== uid).length} member(s) in this group only.</div>
          <textarea class="field" id="sosMessageInput" rows="3" placeholder="Emergency note...">Emergency SOS! Please check on me immediately.</textarea>
          <label class="setting-row">
            <span>Attach Current Location (GPS)</span>
            <input type="checkbox" id="sosShareLocChk" checked />
          </label>
          <button class="btn btn-danger" id="confirmSendSosBtn" data-group-id="${esc(grp.id)}">Send SOS Alert Now</button>
        </div>
      </div>
    `;
  }

  if (state.privateChatMatchId) {
    const match = state.mutualMatches.find((m) => m.id === state.privateChatMatchId);
    const disp = match ? getMatchDisplay(match) : { name: 'Mutual Match', avatar: '' };
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div style="display:flex;align-items:center;gap:8px;">
              ${avatarHtml(disp.name, disp.avatar, 34, false)}
              <h3 style="margin:0;">${esc(disp.name)} Vault Chat</h3>
            </div>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="max-height:280px;overflow-y:auto;display:flex;flex-direction:column;gap:8px;padding:6px;background:var(--bg);border-radius:12px;">
            ${(state.privateMatchMsgs || []).length === 0 ? `<div class="muted" style="padding:12px;">Say hello inside your private vault...</div>` : state.privateMatchMsgs.map((m) => `
              <div class="msg-row ${m.senderId === uid ? 'mine' : 'theirs'}">
                <div class="bubble">${esc(m.text)}</div>
                <div class="msg-meta-row">${fmtTime(m.createdAt)}</div>
              </div>
            `).join('')}
          </div>
          <div style="display:flex;gap:8px;">
            <input class="field" id="privateVaultInput" placeholder="Write privately..." value="${esc(state.privateMatchDraft || '')}" style="flex:1;" />
            <button class="btn btn-primary" id="sendPrivateVaultBtn">Send</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.maskModalMatchId) {
    const match = state.mutualMatches.find((m) => m.id === state.maskModalMatchId);
    const disp = match ? getMatchDisplay(match) : { alias: '', hideAvatar: false };
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Disguise Match Alias</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="maskAliasInput" placeholder="Local nickname (e.g. Work Friend)" value="${esc(disp.alias || '')}" />
          <label class="setting-row">
            <span>Hide Profile Photo in Vault</span>
            <input type="checkbox" id="maskHideAvatarChk" ${disp.hideAvatar ? 'checked' : ''} />
          </label>
          <button class="btn btn-primary" id="saveMaskBtn" data-match-id="${esc(state.maskModalMatchId)}">Save Disguise</button>
        </div>
      </div>
    `;
  }

  if (state.detailListingId) {
    const item = state.marketplace.find((x) => x.id === state.detailListingId);
    if (item) {
      const seller = helpers.resolveAuthor(item.sellerId, item.sellerName, item.sellerAvatar);
      const isMine = item.sellerId === uid || isAdminUser();
      return `
        <div class="modal-backdrop" id="modalBackdrop">
          <div class="modal-card" style="max-width:560px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <h3 style="margin:0;">${esc(item.title)}</h3>
              <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
            </div>
            ${item.imageUri ? helpers.mediaImgHtml(item.imageUri, 'mkt-detail-img open-lightbox', item.title, `data-lightbox-uri="${esc(item.imageUri)}"`) : ''}
            <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
              <div style="font-size:22px;font-weight:800;color:var(--primary);">${esc(item.currency || '$')}${Number(item.price || 0).toFixed(2)}</div>
              <div style="display:flex;gap:6px;flex-wrap:wrap;">
                <span class="badge">${esc(item.category || 'Other')}</span>
                <span class="badge">${esc(item.condition || 'Good')}</span>
                <span class="badge">${esc(item.status || 'available')}</span>
              </div>
            </div>
            <div class="muted">📍 ${esc(item.locationText || 'Local')} · Posted ${fmtTime(item.createdAt)}</div>
            ${item.description ? `<div style="font-size:14px;line-height:1.5;white-space:pre-wrap;">${esc(item.description)}</div>` : ''}
            <div class="side-item open-user-profile" data-uid="${esc(item.sellerId)}" style="background:var(--elevated);cursor:pointer;">
              <span class="side-item-left">
                ${avatarHtml(seller.name, seller.avatar, 36, seller.online)}
                <div>
                  <div style="font-weight:700;">${esc(seller.name)}</div>
                  <div class="muted" style="font-size:11.5px;">View seller profile</div>
                </div>
              </span>
              <span class="badge">Seller</span>
            </div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;">
              ${isMine ? `
                <button class="btn btn-secondary open-edit-listing-btn" data-item-id="${esc(item.id)}">Edit Listing</button>
                <button class="btn btn-primary toggle-sold-btn" data-item-id="${esc(item.id)}">${item.status === 'sold' ? 'Mark Available' : 'Mark Sold'}</button>
                <button class="btn btn-danger delete-mkt-btn" data-item-id="${esc(item.id)}">Delete</button>
              ` : `
                <button class="btn btn-primary market-msg-btn" data-item-id="${esc(item.id)}" style="flex:1;">${iconSvg('chat', 16)}<span>Message Seller</span></button>
                <button class="btn btn-secondary report-open-btn" data-rtype="listing" data-rid="${esc(item.id)}">Report</button>
              `}
            </div>
          </div>
        </div>
      `;
    }
  }

  if (state.editListingTarget) {
    const it = state.editListingTarget;
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Edit Marketplace Listing</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="editMktTitle" placeholder="Title" value="${esc(it.title || '')}" />
          <div style="display:grid;grid-template-columns:100px 1fr;gap:8px;">
            <select class="select-field" id="editMktCurrency">
              ${['$', '৳', '₹', '€', '£'].map((cur) => `<option ${it.currency === cur ? 'selected' : ''}>${cur}</option>`).join('')}
            </select>
            <input class="field" id="editMktPrice" type="number" min="0" step="0.01" placeholder="Price" value="${esc(String(it.price ?? '0'))}" />
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <select class="select-field" id="editMktCat">
              ${['Electronics', 'Vehicles', 'Home', 'Fashion', 'Books', 'Services', 'Other'].map((c) => `<option ${it.category === c ? 'selected' : ''}>${c}</option>`).join('')}
            </select>
            <select class="select-field" id="editMktCond">
              ${['New', 'Like New', 'Good', 'Fair'].map((c) => `<option ${it.condition === c ? 'selected' : ''}>${c}</option>`).join('')}
            </select>
          </div>
          <input class="field" id="editMktLoc" placeholder="Location" value="${esc(it.locationText || '')}" />
          <textarea class="field" id="editMktDesc" rows="3" placeholder="Description">${esc(it.description || '')}</textarea>
          ${state.mktMediaPreview ? `
            <div class="preview-wrap"><img src="${esc(state.mktMediaPreview)}" alt="New photo" /><button class="preview-remove-btn" id="clearMktMediaBtn" type="button">${iconSvg('close', 16)}</button></div>
          ` : (it.imageUri ? helpers.mediaImgHtml(it.imageUri, 'market-img', 'Current photo') : '')}
          <button class="btn btn-secondary" id="pickMktMediaBtn" type="button">${iconSvg('photo', 16)}<span>${state.mktMediaFile ? 'Change Selected Photo' : 'Replace Product Photo'}</span></button>
          <button class="btn btn-primary" id="saveEditListingBtn" data-item-id="${esc(it.id)}" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Saving...' : 'Save Changes'}</button>
        </div>
      </div>
    `;
  }

  if (state.editPageTarget) {
    const pg = state.editPageTarget;
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Edit Page</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="editPageName" placeholder="Page name" value="${esc(pg.name || '')}" />
          <select class="select-field" id="editPageCat">
            ${['Digital Creator', 'Creator', 'Community', 'Business', 'Music', 'Education', 'Tech', 'Sports'].map((c) => `<option ${pg.category === c ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
          <textarea class="field" id="editPageBio" rows="3" placeholder="Page bio">${esc(pg.bio || '')}</textarea>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-secondary" id="pickPageAvatarBtn" type="button">${iconSvg('photo', 15)}<span>${state.pageAvatarFile ? 'Avatar Ready ✓' : 'Page Avatar'}</span></button>
            <button class="btn btn-secondary" id="pickPageCoverBtn" type="button">${iconSvg('photo', 15)}<span>${state.pageCoverFile ? 'Cover Ready ✓' : 'Cover Banner'}</span></button>
          </div>
          <button class="btn btn-primary" id="saveEditPageBtn" data-page-id="${esc(pg.id)}" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Saving...' : 'Save Page Changes'}</button>
        </div>
      </div>
    `;
  }

  if (state.managePageModal) {
    const pg = state.pages.find((p) => p.id === state.managePageModal);
    if (!pg) return '';
    const followerSet = new Set(pg.followerUids || []);
    const adminSet = new Set(pg.adminUids || [pg.creatorId]);
    const isCreator = pg.creatorId === uid;
    const candidates = state.users.filter((u) => !followerSet.has(u.userId || u.id));
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Manage ${esc(pg.name)}</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="font-weight:700;font-size:13px;">Followers &amp; Admins (${(pg.followerUids || []).length})</div>
          <div style="max-height:175px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${(pg.followerUids || []).map((fid) => {
              const u = helpers.resolveAuthor(fid, 'Member');
              const isCr = fid === pg.creatorId;
              const isAd = adminSet.has(fid);
              return `
                <div class="setting-row">
                  <span>${esc(u.name)} ${isCr ? '<span class="badge">Owner</span>' : (isAd ? '<span class="badge">Admin</span>' : '')}</span>
                  ${!isCr ? `
                    <div style="display:flex;gap:6px;">
                      ${isCreator ? `<button class="btn btn-secondary toggle-page-admin-btn" data-page-id="${esc(pg.id)}" data-uid="${esc(fid)}" style="min-height:30px;padding:3px 9px;font-size:11.5px;">${isAd ? 'Remove Admin' : 'Make Admin'}</button>` : ''}
                      <button class="btn btn-danger remove-page-mem-btn" data-page-id="${esc(pg.id)}" data-uid="${esc(fid)}" style="min-height:30px;padding:3px 9px;font-size:11.5px;">Remove</button>
                    </div>
                  ` : ''}
                </div>
              `;
            }).join('')}
          </div>
          <div style="font-weight:700;font-size:13px;margin-top:6px;">Add People to Page</div>
          <div style="max-height:150px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${candidates.length === 0 ? `<div class="muted">Everyone discoverable already follows this Page.</div>` : candidates.map((u) => `
              <div class="setting-row">
                <span>${esc(u.displayName)}</span>
                <button class="btn btn-primary add-page-mem-btn" data-page-id="${esc(pg.id)}" data-uid="${esc(u.userId || u.id)}" style="min-height:30px;padding:3px 10px;font-size:11.5px;">Add</button>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }

  if (state.showEditProfileModal) {
    const me = state.me || {};
    return `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Edit Profile</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-secondary" id="pickProfAvatarBtn" type="button" ${state.isUploading ? 'disabled' : ''}>${iconSvg('photo', 15)}<span>Change Avatar</span></button>
            <button class="btn btn-secondary" id="pickProfCoverBtn" type="button" ${state.isUploading ? 'disabled' : ''}>${iconSvg('photo', 15)}<span>Change Cover</span></button>
          </div>
          <input class="field" id="editProfNameInput" placeholder="Display Name" value="${esc(me.displayName || state.user?.displayName || '')}" />
          <textarea class="field" id="editProfBioInput" rows="3" placeholder="Bio">${esc(me.bio || '')}</textarea>
          <div>
            <label class="muted">Profile Privacy</label>
            <select class="select-field" id="editProfPrivacySelect" style="margin-top:4px;">
              <option value="public" ${(me.profilePrivacy || 'public') === 'public' ? 'selected' : ''}>Public</option>
              <option value="friends" ${me.profilePrivacy === 'friends' ? 'selected' : ''}>Friends Only</option>
              <option value="private" ${me.profilePrivacy === 'private' ? 'selected' : ''}>Private</option>
            </select>
          </div>
          <label class="setting-row">
            <span>Allow Secret Love Messages</span>
            <input type="checkbox" id="editProfLoveChk" ${me.acceptSecretLove !== false ? 'checked' : ''} />
          </label>
          <button class="btn btn-primary" id="saveEditProfileModalBtn" ${state.isUploading ? 'disabled' : ''}>Save Changes</button>
        </div>
      </div>
    `;
  }

  return '';
}

export function bindFeatureEvents() {
  const { state, db, fs, helpers } = ctx;

  // Post Reactions Picker & Multi-Reaction
  document.querySelectorAll('.toggle-react-picker-btn').forEach((b) => {
    b.addEventListener('click', (e) => {
      e.stopPropagation();
      const pid = b.getAttribute('data-post-id');
      state.reactionPickerPostId = state.reactionPickerPostId === pid ? null : pid;
      helpers.render();
    });
  });
  document.querySelectorAll('.pick-post-react-btn').forEach((b) => {
    b.addEventListener('click', (e) => {
      e.stopPropagation();
      const pid = b.getAttribute('data-post-id');
      const emoji = b.getAttribute('data-emoji');
      const post = state.posts.find((p) => p.id === pid);
      if (post && emoji) setPostReaction(post, emoji);
    });
  });

  // Post Edit, Delete, Comment Delete, Share Modal
  document.querySelectorAll('.edit-post-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.editPostTarget = state.posts.find((p) => p.id === b.getAttribute('data-post-id')) || null;
      helpers.render();
    });
  });
  document.getElementById('saveEditPostBtn')?.addEventListener('click', (e) => {
    const pid = e.currentTarget.getAttribute('data-post-id');
    const txt = document.getElementById('editPostContentInput')?.value ?? '';
    const priv = document.getElementById('editPostPrivacySelect')?.value || 'public';
    saveEditedPost(pid, txt, priv);
  });
  document.querySelectorAll('.delete-post-btn').forEach((b) => {
    b.addEventListener('click', () => deletePostItem(b.getAttribute('data-post-id')));
  });
  document.querySelectorAll('.delete-comment-btn').forEach((b) => {
    b.addEventListener('click', () => deleteCommentItem(b.getAttribute('data-post-id'), b.getAttribute('data-comment-id')));
  });
  document.querySelectorAll('.open-share-modal-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.shareTarget = state.posts.find((p) => p.id === b.getAttribute('data-post-id')) || null;
      helpers.render();
    });
  });
  document.getElementById('shareRepostFeedBtn')?.addEventListener('click', () => {
    if (state.shareTarget) sharePostToFeed(state.shareTarget, document.getElementById('repostNoteInput')?.value || '');
  });
  document.getElementById('shareCopyLinkBtn')?.addEventListener('click', () => {
    if (!state.shareTarget) return;
    navigator.clipboard?.writeText(`${window.location.origin}${window.location.pathname}#post-${state.shareTarget.id}`);
    fs.updateDoc(fs.doc(db, 'posts', state.shareTarget.id), { sharesCount: fs.increment(1), updatedAt: fs.serverTimestamp() }).catch(() => {});
    state.shareTarget = null;
    helpers.showBanner('Post link copied!');
  });
  document.querySelectorAll('.share-send-dm-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const pid = b.getAttribute('data-uid');
      const peer = state.usersById[pid];
      const post = state.shareTarget;
      state.shareTarget = null;
      if (peer && post) {
        helpers.openOrCreateDm(peer, `Shared a post by ${post.authorName}:\n${(post.content || '').slice(0, 300)}`, post.mediaUri || '', post.mediaType === 'image' || post.mediaType === 'video' ? post.mediaType : 'none');
      }
    });
  });

  // Stories Filters, Reactions, Highlight, Delete
  document.querySelectorAll('[data-story-filter]').forEach((b) => {
    b.addEventListener('click', () => { state.storyFilter = b.getAttribute('data-story-filter'); helpers.render(); });
  });
  document.querySelectorAll('.react-story-btn').forEach((b) => {
    b.addEventListener('click', () => {
      if (state.activeStory) reactToStory(state.activeStory.id, b.getAttribute('data-emoji'));
    });
  });
  document.getElementById('sendStoryReplyBtn')?.addEventListener('click', () => {
    const val = document.getElementById('storyReplyInput')?.value?.trim();
    if (state.activeStory && val) {
      reactToStory(state.activeStory.id, '💬', val);
      const inp = document.getElementById('storyReplyInput');
      if (inp) inp.value = '';
    }
  });
  document.getElementById('toggleStoryHighlightBtn')?.addEventListener('click', () => {
    if (state.activeStory) toggleStoryHighlight(state.activeStory);
  });
  document.getElementById('deleteStoryBtn')?.addEventListener('click', () => {
    if (state.activeStory) deleteStoryItem(state.activeStory.id);
  });

  // Reels Filters, Comments, Save, Delete
  document.querySelectorAll('[data-reel-cat]').forEach((b) => {
    b.addEventListener('click', () => { state.reelFilter = b.getAttribute('data-reel-cat'); helpers.render(); });
  });
  document.querySelectorAll('.open-reel-comments-btn').forEach((b) => {
    b.addEventListener('click', () => loadReelComments(b.getAttribute('data-reel-id')));
  });
  document.getElementById('submitReelCommentBtn')?.addEventListener('click', (e) => {
    const rid = e.currentTarget.getAttribute('data-reel-id');
    const inp = document.getElementById('reelCommentInput');
    if (inp && inp.value.trim()) {
      const txt = inp.value;
      inp.value = '';
      submitReelComment(rid, txt);
    }
  });
  document.querySelectorAll('.save-reel-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const r = state.reels.find((x) => x.id === b.getAttribute('data-reel-id'));
      if (r) toggleSaveReel(r);
    });
  });
  document.querySelectorAll('.delete-reel-btn').forEach((b) => {
    b.addEventListener('click', () => deleteReelItem(b.getAttribute('data-reel-id')));
  });

  // Report & Block
  document.querySelectorAll('.report-open-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.reportTarget = { type: b.getAttribute('data-rtype'), id: b.getAttribute('data-rid') };
      helpers.render();
    });
  });
  document.getElementById('submitReportBtn')?.addEventListener('click', () => {
    if (state.reportTarget) submitReportItem(state.reportTarget.type, state.reportTarget.id, document.getElementById('reportReasonInput')?.value || '');
  });
  document.querySelectorAll('.block-user-btn').forEach((b) => {
    b.addEventListener('click', () => blockTargetUser(b.getAttribute('data-uid'), b.getAttribute('data-name') || 'Member'));
  });
  document.querySelectorAll('.unblock-user-btn').forEach((b) => {
    b.addEventListener('click', () => unblockTargetUser(b.getAttribute('data-uid')));
  });

  // Groups Management & SOS
  document.querySelectorAll('.manage-group-btn').forEach((b) => {
    b.addEventListener('click', () => { state.manageGroupModal = b.getAttribute('data-group-id'); helpers.render(); });
  });
  document.querySelectorAll('.add-grp-mem-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const g = state.groups.find((x) => x.id === b.getAttribute('data-group-id'));
      if (g) addGroupMember(g, b.getAttribute('data-uid'));
    });
  });
  document.querySelectorAll('.remove-grp-mem-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const g = state.groups.find((x) => x.id === b.getAttribute('data-group-id'));
      if (g) removeGroupMember(g, b.getAttribute('data-uid'));
    });
  });
  document.querySelectorAll('.delete-group-btn').forEach((b) => {
    b.addEventListener('click', () => deleteGroupItem(b.getAttribute('data-group-id')));
  });
  document.querySelectorAll('.trigger-sos-open-btn').forEach((b) => {
    b.addEventListener('click', () => { state.sosModalGroup = b.getAttribute('data-group-id'); helpers.render(); });
  });
  document.getElementById('confirmSendSosBtn')?.addEventListener('click', (e) => {
    const g = state.groups.find((x) => x.id === e.currentTarget.getAttribute('data-group-id'));
    const msg = document.getElementById('sosMessageInput')?.value || 'Emergency SOS!';
    const shareLoc = Boolean(document.getElementById('sosShareLocChk')?.checked);
    if (g) sendSosAlert(g, msg, shareLoc);
  });
  document.querySelectorAll('.stop-sos-btn').forEach((b) => {
    b.addEventListener('click', () => stopSosAlert(b.getAttribute('data-group-id'), b.getAttribute('data-alert-id'), b.getAttribute('data-status')));
  });

  // Marketplace / Page / Event Delete & Group Chat Leave
  document.querySelectorAll('.delete-mkt-btn').forEach((b) => b.addEventListener('click', () => deleteMarketplaceItem(b.getAttribute('data-item-id'))));
  document.querySelectorAll('.delete-page-btn').forEach((b) => b.addEventListener('click', () => deletePageItem(b.getAttribute('data-page-id'))));
  document.querySelectorAll('.delete-event-btn').forEach((b) => b.addEventListener('click', () => deleteEventItem(b.getAttribute('data-event-id'))));
  document.getElementById('leaveGroupChatBtn')?.addEventListener('click', () => {
    const c = state.conversations.find((x) => x.id === state.activeConvId);
    if (c) leaveGroupConversation(c);
  });

  // Secret Love A-to-Z
  document.getElementById('secretRecipientSelect')?.addEventListener('change', (e) => {
    state.secretSendRecipientUid = e.target.value;
    helpers.render();
  });
  document.getElementById('questTierSelect')?.addEventListener('change', (e) => {
    state.questTierDays = Number(e.target.value) || 7;
  });
  document.getElementById('startDevotionBtn')?.addEventListener('click', () => {
    const uid = state.user?.uid;
    const peers = state.users.filter((u) => (u.userId || u.id) !== uid);
    const recUid = state.secretSendRecipientUid || peers[0]?.userId || peers[0]?.id;
    const rec = state.usersById[recUid] || peers[0];
    if (rec) startDevotionTrial(rec, state.questTierDays || 7);
  });
  document.getElementById('devotionReflectionInput')?.addEventListener('input', (e) => {
    state.devotionReflectionDraft = e.target.value;
  });
  document.getElementById('submitDevotionBtn')?.addEventListener('click', () => {
    const uid = state.user?.uid;
    const peers = state.users.filter((u) => (u.userId || u.id) !== uid);
    const recUid = state.secretSendRecipientUid || peers[0]?.userId || peers[0]?.id;
    const trial = state.devotionTrials.find((t) => t.recipientId === recUid);
    if (trial) submitDevotionCheckIn(trial, state.devotionReflectionDraft || '');
  });
  document.querySelectorAll('[data-love-mood]').forEach((b) => {
    b.addEventListener('click', () => { state.secretMoodEmoji = b.getAttribute('data-love-mood'); helpers.render(); });
  });
  document.getElementById('secretNoteInput')?.addEventListener('input', (e) => {
    state.secretNoteDraft = e.target.value;
  });
  document.getElementById('sendSecretLoveBtn')?.addEventListener('click', () => {
    const uid = state.user?.uid;
    const peers = state.users.filter((u) => (u.userId || u.id) !== uid);
    const recUid = state.secretSendRecipientUid || peers[0]?.userId || peers[0]?.id;
    const rec = state.usersById[recUid] || peers[0];
    if (rec) sendSecretLoveNote(rec, state.secretNoteDraft || '', state.secretMoodEmoji || '❤️');
  });
  document.querySelectorAll('.reveal-love-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const s = state.secretSent.find((x) => x.id === b.getAttribute('data-sent-id'));
      if (s) revealSecretLoveNote(s);
    });
  });
  document.querySelectorAll('.toggle-crush-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const target = state.usersById[b.getAttribute('data-uid')];
      if (target) toggleSecretCrush(target);
    });
  });
  document.querySelectorAll('.open-vault-chat-btn').forEach((b) => {
    b.addEventListener('click', () => openPrivateMatchChat(b.getAttribute('data-match-id')));
  });
  document.getElementById('privateVaultInput')?.addEventListener('input', (e) => {
    state.privateMatchDraft = e.target.value;
  });
  document.getElementById('sendPrivateVaultBtn')?.addEventListener('click', () => {
    if (state.privateChatMatchId) sendPrivateMatchMessage(state.privateChatMatchId, state.privateMatchDraft || '');
  });
  document.querySelectorAll('.open-mask-modal-btn').forEach((b) => {
    b.addEventListener('click', () => { state.maskModalMatchId = b.getAttribute('data-match-id'); helpers.render(); });
  });
  document.getElementById('saveMaskBtn')?.addEventListener('click', (e) => {
    const mid = e.currentTarget.getAttribute('data-match-id');
    const alias = document.getElementById('maskAliasInput')?.value || '';
    const hideAvatar = Boolean(document.getElementById('maskHideAvatarChk')?.checked);
    try { localStorage.setItem(`hello_mask_${mid}`, JSON.stringify({ alias, hideAvatar })); } catch (_) {}
    state.maskModalMatchId = null;
    helpers.showBanner('Disguise settings updated.');
  });

  // Settings & Privacy
  document.getElementById('settingsThemeBtn')?.addEventListener('click', () => document.getElementById('themeToggleBtn')?.click());
  document.getElementById('toggleOnlineDotBtn')?.addEventListener('click', async () => {
    if (!state.user || !state.me) return;
    const next = state.me.showOnlineStatus === false;
    await fs.updateDoc(fs.doc(db, 'users_public', state.user.uid), {
      showOnlineStatus: next,
      isOnline: next && !state.incognitoHideStatus,
      updatedAt: fs.serverTimestamp()
    });
  });
  document.getElementById('toggleIncognitoStatusBtn')?.addEventListener('click', async () => {
    state.incognitoHideStatus = !state.incognitoHideStatus;
    localStorage.setItem('hello_incog_status', state.incognitoHideStatus ? '1' : '0');
    if (state.user) {
      await fs.updateDoc(fs.doc(db, 'users_public', state.user.uid), {
        isOnline: !state.incognitoHideStatus && state.me?.showOnlineStatus !== false,
        updatedAt: fs.serverTimestamp()
      }).catch(() => {});
    }
    helpers.render();
  });
  document.getElementById('toggleIncognitoViewBtn')?.addEventListener('click', () => {
    state.incognitoPrivateViewing = !state.incognitoPrivateViewing;
    localStorage.setItem('hello_incog_view', state.incognitoPrivateViewing ? '1' : '0');
    helpers.render();
  });
  document.getElementById('toggleVoiceGestureBtn')?.addEventListener('click', () => {
    helpers.toggleVoiceGesture?.();
  });
  document.getElementById('settingsProfilePrivacy')?.addEventListener('change', async (e) => {
    if (!state.user) return;
    await fs.updateDoc(fs.doc(db, 'users_public', state.user.uid), {
      profilePrivacy: e.target.value,
      updatedAt: fs.serverTimestamp()
    });
    helpers.showBanner('Profile privacy updated.');
  });
  document.getElementById('saveRecoveryBtn')?.addEventListener('click', async () => {
    const email = (document.getElementById('recoveryEmailInput')?.value || '').trim().slice(0, 160);
    const phone = (document.getElementById('recoveryPhoneInput')?.value || '').trim().slice(0, 40);
    try {
      await upsertPrivateUser({ email, phone });
      helpers.showBanner('Recovery contact info saved securely.');
    } catch (e) {
      helpers.showBanner(e?.message || 'Could not save recovery info.', true);
    }
  });
  document.querySelectorAll('.toggle-notif-pref-btn').forEach((b) => {
    b.addEventListener('click', async () => {
      const k = b.getAttribute('data-pref-key');
      const cur = (state.privateUser || {})[k] !== false;
      await upsertPrivateUser({ [k]: !cur }).catch(() => {});
    });
  });
  document.getElementById('installLaptopBtnSettings')?.addEventListener('click', () => helpers.openGetAppModal?.());
  document.getElementById('signOutBtnSettings')?.addEventListener('click', () => ctx.auth.signOut());

  // Marketplace Detail / Edit, Page Detail / Edit / Members, Edit Profile Modal
  document.querySelectorAll('.open-listing-detail-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.detailListingId = b.getAttribute('data-item-id');
      helpers.render();
    });
  });
  document.querySelectorAll('.open-edit-listing-btn').forEach((b) => {
    b.addEventListener('click', (e) => {
      e.stopPropagation();
      const it = state.marketplace.find((x) => x.id === b.getAttribute('data-item-id'));
      if (it) {
        state.detailListingId = null;
        state.editListingTarget = it;
        helpers.setPreviewFile?.('mktMediaFile', 'mktMediaPreview', null);
        helpers.render();
      }
    });
  });
  document.getElementById('saveEditListingBtn')?.addEventListener('click', (e) => {
    const lid = e.currentTarget.getAttribute('data-item-id');
    saveEditedListing(lid, {
      title: document.getElementById('editMktTitle')?.value,
      price: document.getElementById('editMktPrice')?.value,
      currency: document.getElementById('editMktCurrency')?.value,
      category: document.getElementById('editMktCat')?.value,
      condition: document.getElementById('editMktCond')?.value,
      locationText: document.getElementById('editMktLoc')?.value,
      description: document.getElementById('editMktDesc')?.value
    });
  });

  document.querySelectorAll('.open-page-detail-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.activePageId = b.getAttribute('data-page-id');
      helpers.render();
    });
  });
  document.getElementById('backToPagesBtn')?.addEventListener('click', () => {
    state.activePageId = null;
    helpers.render();
  });
  document.querySelectorAll('.open-edit-page-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const pg = state.pages.find((p) => p.id === b.getAttribute('data-page-id'));
      if (pg) {
        state.editPageTarget = pg;
        helpers.render();
      }
    });
  });
  document.getElementById('saveEditPageBtn')?.addEventListener('click', (e) => {
    const pid = e.currentTarget.getAttribute('data-page-id');
    saveEditedPage(pid, {
      name: document.getElementById('editPageName')?.value,
      category: document.getElementById('editPageCat')?.value,
      bio: document.getElementById('editPageBio')?.value
    });
  });
  document.querySelectorAll('.manage-page-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.managePageModal = b.getAttribute('data-page-id');
      helpers.render();
    });
  });
  document.querySelectorAll('.add-page-mem-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const pg = state.pages.find((p) => p.id === b.getAttribute('data-page-id'));
      if (pg) addPageFollower(pg, b.getAttribute('data-uid'));
    });
  });
  document.querySelectorAll('.remove-page-mem-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const pg = state.pages.find((p) => p.id === b.getAttribute('data-page-id'));
      if (pg) removePageFollower(pg, b.getAttribute('data-uid'));
    });
  });
  document.querySelectorAll('.toggle-page-admin-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const pg = state.pages.find((p) => p.id === b.getAttribute('data-page-id'));
      if (pg) togglePageAdmin(pg, b.getAttribute('data-uid'));
    });
  });
  document.getElementById('openEditProfileModalBtn')?.addEventListener('click', () => {
    state.showEditProfileModal = true;
    helpers.render();
  });
  document.getElementById('saveEditProfileModalBtn')?.addEventListener('click', () => {
    saveProfileModalChanges({
      displayName: document.getElementById('editProfNameInput')?.value,
      bio: document.getElementById('editProfBioInput')?.value,
      profilePrivacy: document.getElementById('editProfPrivacySelect')?.value,
      acceptSecretLove: document.getElementById('editProfLoveChk')?.checked
    });
  });

  // Owner Dashboard
  document.querySelectorAll('[data-owner-tab]').forEach((b) => {
    b.addEventListener('click', () => { state.ownerSubTab = b.getAttribute('data-owner-tab'); helpers.render(); });
  });
  document.getElementById('reauthOwnerBtn')?.addEventListener('click', reauthOwnerSession);
  document.getElementById('saveGlobalSettingsBtn')?.addEventListener('click', () => {
    saveGlobalSettings({
      appName: document.getElementById('gsAppName')?.value,
      tagline: document.getElementById('gsTagline')?.value,
      maintenanceBanner: document.getElementById('gsBanner')?.value,
      sosFeatureEnabled: document.getElementById('gsSosEnabled')?.checked,
      secretLoveEnabled: document.getElementById('gsLoveEnabled')?.checked,
      allowNewRegistrations: document.getElementById('gsRegEnabled')?.checked
    });
  });
  document.getElementById('saveOwnerConfigBtn')?.addEventListener('click', () => {
    saveOwnerConfig({
      ownerName: document.getElementById('ocOwnerName')?.value,
      ownerHandle: document.getElementById('ocOwnerHandle')?.value,
      showOwnerHandle: document.getElementById('ocShowHandle')?.checked,
      about: document.getElementById('ocAbout')?.value,
      contactEmail: document.getElementById('ocEmail')?.value,
      showContactEmail: document.getElementById('ocShowEmail')?.checked,
      contactInfo: document.getElementById('ocContactInfo')?.value
    });
  });
  document.getElementById('addAdminBtn')?.addEventListener('click', () => {
    addAdminRecord(document.getElementById('addAdminInput')?.value || '');
  });
  document.querySelectorAll('.remove-admin-btn').forEach((b) => {
    b.addEventListener('click', () => removeAdminRecord(b.getAttribute('data-uid')));
  });
  document.querySelectorAll('.update-report-btn').forEach((b) => {
    b.addEventListener('click', () => updateReportStatus(b.getAttribute('data-report-id'), b.getAttribute('data-status')));
  });
}
