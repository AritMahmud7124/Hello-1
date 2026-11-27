import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.1.0/firebase-app.js';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged
} from 'https://www.gstatic.com/firebasejs/11.1.0/firebase-auth.js';
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  addDoc,
  deleteDoc,
  onSnapshot,
  query,
  where,
  orderBy,
  limit,
  serverTimestamp,
  increment,
  arrayUnion,
  writeBatch
} from 'https://www.gstatic.com/firebasejs/11.1.0/firebase-firestore.js';

const APK_URL = 'https://github.com/AritMahmud7124/Hello-1/releases/download/v1.8.2/hello-social-v1.8.2.apk';
const RELEASES_URL = 'https://github.com/AritMahmud7124/Hello-1/releases';
const FIRESTORE_DB_ID = 'ai-studio-android-hello-982e615a-60df-4ad0-a009-d7f68361be02';

const firebaseConfig = {
  apiKey: ['AIzaSyDgXgXvcivBHj6', 'PnIZF-NUcc5xz54KX6LU'].join(''),
  authDomain: 'gen-lang-client-0583025546.firebaseapp.com',
  projectId: 'gen-lang-client-0583025546',
  storageBucket: 'gen-lang-client-0583025546.firebasestorage.app',
  messagingSenderId: '1045492054902',
  appId: '1:1045492054902:android:986945a29c7caa20431857'
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DB_ID);

const state = {
  user: null,
  me: null,
  tab: 'feed',
  theme: localStorage.getItem('hello_theme') || 'dark',
  search: '',
  authError: '',
  deferredPrompt: null,
  usersById: {},
  users: [],
  posts: [],
  commentsByPost: {},
  likedByPost: {},
  openComments: {},
  conversations: [],
  activeConvId: null,
  messages: [],
  friendships: [],
  friendRequests: [],
  follows: [],
  groups: [],
  marketplace: [],
  postDraft: '',
  postMediaFile: null,
  msgDraft: '',
  mobileShowChat: false,
  statusBanner: ''
};

if (state.theme === 'light') document.documentElement.classList.add('light');

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./sw.js').catch(() => {});
}

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  state.deferredPrompt = e;
  render();
});

const mediaCache = new Map();
const mediaPending = new Set();

async function resolveMediaUri(uri) {
  if (!uri) return '';
  if (!uri.startsWith('firestore://media/')) return uri;
  if (mediaCache.has(uri)) return mediaCache.get(uri);
  if (mediaPending.has(uri)) return '';
  mediaPending.add(uri);
  try {
    const mediaId = uri.replace('firestore://media/', '').split('?')[0].split('/')[0].trim();
    const blobSnap = await getDoc(doc(db, 'media_blobs', mediaId));
    if (!blobSnap.exists()) return '';
    const blobMeta = blobSnap.data();
    const chunksSnap = await getDocs(collection(db, 'media_blobs', mediaId, 'chunks'));
    const sorted = chunksSnap.docs
      .map((d) => d.data())
      .sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
    const b64 = sorted.map((c) => c.data || '').join('');
    const mime = blobMeta.mimeType || 'image/jpeg';
    const dataUrl = `data:${mime};base64,${b64}`;
    mediaCache.set(uri, dataUrl);
    render();
    return dataUrl;
  } catch (_) {
    return '';
  } finally {
    mediaPending.delete(uri);
  }
}

function mediaSrc(uri) {
  if (!uri) return '';
  if (!uri.startsWith('firestore://media/')) return uri;
  if (mediaCache.has(uri)) return mediaCache.get(uri);
  resolveMediaUri(uri);
  return '';
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function fmtTime(ts) {
  if (!ts) return 'Just now';
  const ms = ts.seconds ? ts.seconds * 1000 : Number(ts) || Date.now();
  const diff = Math.max(0, Math.floor((Date.now() - ms) / 1000));
  if (diff < 60) return 'Just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
  return new Date(ms).toLocaleDateString();
}

function resolveAuthor(uid, fallbackName = 'Hello Member', fallbackAvatar = '') {
  const live = state.usersById[uid] || (state.me && state.me.userId === uid ? state.me : null);
  return {
    name: live?.displayName || fallbackName || 'Hello Member',
    avatar: live?.photoUrl || fallbackAvatar || '',
    online: Boolean(live?.isOnline && live?.showOnlineStatus !== false)
  };
}

function avatarHtml(name, photoUrl, size = 40, isOnline = false) {
  const src = mediaSrc(photoUrl);
  const initials = (name || 'H').trim().slice(0, 1).toUpperCase();
  const dot = isOnline ? `<span class="online-dot"></span>` : '';
  if (src) {
    return `<span class="avatar-wrap" style="width:${size}px;height:${size}px"><img class="avatar" src="${esc(src)}" alt="${esc(name)}" style="width:${size}px;height:${size}px"/>${dot}</span>`;
  }
  return `<span class="avatar-wrap" style="width:${size}px;height:${size}px"><span class="avatar" style="width:${size}px;height:${size}px;font-size:${Math.round(size * 0.42)}px">${esc(initials)}</span>${dot}</span>`;
}

async function installLaptopApp() {
  if (state.deferredPrompt) {
    state.deferredPrompt.prompt();
    const choice = await state.deferredPrompt.userChoice;
    state.deferredPrompt = null;
    if (choice?.outcome === 'accepted') {
      state.statusBanner = 'HELLO is installing as a Desktop App on your laptop!';
    }
    render();
  } else {
    alert('To install HELLO on your laptop:\n\nIn Chrome or Microsoft Edge, click the Install App icon (monitor with arrow) on the right side of the address bar, or open browser Menu (⋮) → Save and share / Apps → Install HELLO.');
  }
}

async function signInGoogle() {
  state.authError = '';
  render();
  try {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  } catch (err) {
    if (err?.code === 'auth/unauthorized-domain') {
      state.authError = `Domain "${window.location.hostname}" is not yet authorized in Firebase Auth. Open Firebase Console → Authentication → Settings → Authorized domains and add "${window.location.hostname}", then click Sign in again.`;
    } else {
      state.authError = err?.message || 'Google Sign-In failed.';
    }
    render();
  }
}

async function ensureWebProfile(firebaseUser) {
  const uid = firebaseUser.uid;
  const pubRef = doc(db, 'users_public', uid);
  const snap = await getDoc(pubRef);
  if (!snap.exists()) {
    const cleanHandle = '@' + (firebaseUser.email?.split('@')[0] || uid.slice(0, 8)).toLowerCase().replace(/[^a-z0-9_]/g, '').slice(0, 24);
    await setDoc(pubRef, {
      userId: uid,
      displayName: (firebaseUser.displayName || 'Hello Member').slice(0, 80),
      handle: cleanHandle,
      bio: "Hey there! I'm on HELLO.",
      photoUrl: firebaseUser.photoURL || '',
      coverUrl: '',
      isOnline: true,
      showOnlineStatus: true,
      profilePrivacy: 'public',
      acceptSecretLove: true,
      friendsCount: 0,
      followersCount: 0,
      isVerifiedOwner: false,
      role: 'user',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  } else {
    const data = snap.data();
    await updateDoc(pubRef, {
      isOnline: data.showOnlineStatus !== false,
      updatedAt: serverTimestamp()
    });
  }
}

let unsubs = [];
function clearSubs() {
  unsubs.forEach((u) => { try { u(); } catch (_) {} });
  unsubs = [];
}

function startListeners(uid) {
  clearSubs();

  unsubs.push(onSnapshot(doc(db, 'users_public', uid), (snap) => {
    if (snap.exists()) {
      state.me = { id: snap.id, ...snap.data() };
      render();
    }
  }));

  unsubs.push(onSnapshot(collection(db, 'users_public'), (snap) => {
    const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    state.users = list;
    const map = {};
    list.forEach((u) => { map[u.userId || u.id] = u; });
    state.usersById = map;
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'posts'), where('privacy', '==', 'public'), limit(60)), (snap) => {
    state.posts = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'conversations'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.conversations = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
    if (!state.activeConvId && state.conversations.length > 0) {
      selectConversation(state.conversations[0].id, false);
    }
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'friendships'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.friendships = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'friend_requests'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.friendRequests = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'follows'), where('followerUid', '==', uid)), (snap) => {
    state.follows = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'groups'), where('privacy', '==', 'public'), limit(40)), (snap) => {
    state.groups = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'marketplace'), limit(40)), (snap) => {
    state.marketplace = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }));
}

let msgUnsub = null;
function selectConversation(convId, openMobile = true) {
  state.activeConvId = convId;
  if (openMobile) state.mobileShowChat = true;
  if (msgUnsub) { msgUnsub(); msgUnsub = null; }
  if (!convId) { state.messages = []; render(); return; }
  msgUnsub = onSnapshot(collection(db, 'conversations', convId, 'messages'), (snap) => {
    state.messages = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    render();
    setTimeout(() => {
      const box = document.getElementById('chatMsgsBox');
      if (box) box.scrollTop = box.scrollHeight;
    }, 30);
  });
  if (state.user) {
    updateDoc(doc(db, 'conversations', convId), {
      readByUids: arrayUnion(state.user.uid)
    }).catch(() => {});
  }
  render();
}

async function uploadImageBlob(file) {
  const buf = await file.arrayBuffer();
  const bytes = new Uint8Array(buf);
  if (bytes.length > 6 * 1024 * 1024) throw new Error('Image must be under 6 MB');
  let binary = '';
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  const b64 = btoa(binary);
  const chunkSize = 450000;
  const chunks = [];
  for (let i = 0; i < b64.length; i += chunkSize) chunks.push(b64.slice(i, i + chunkSize));
  const mediaId = crypto.randomUUID();
  const blobRef = doc(db, 'media_blobs', mediaId);
  await setDoc(blobRef, {
    ownerId: state.user.uid,
    mimeType: file.type || 'image/jpeg',
    ext: (file.name.split('.').pop() || 'jpg').toLowerCase().slice(0, 8),
    chunkCount: chunks.length,
    byteSize: bytes.length,
    createdAt: serverTimestamp()
  });
  const batch = writeBatch(db);
  chunks.forEach((data, index) => {
    batch.set(doc(db, 'media_blobs', mediaId, 'chunks', `chunk_${String(index).padStart(3, '0')}`), {
      ownerId: state.user.uid,
      index,
      data,
      createdAt: serverTimestamp()
    });
  });
  await batch.commit();
  return `firestore://media/${mediaId}`;
}

async function publishPost() {
  if (!state.user) return;
  const text = state.postDraft.trim();
  if (!text && !state.postMediaFile) return;
  try {
    let mediaUri = '';
    let mediaType = 'none';
    if (state.postMediaFile) {
      mediaUri = await uploadImageBlob(state.postMediaFile);
      mediaType = 'image';
    }
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    await addDoc(collection(db, 'posts'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      content: text.slice(0, 5000),
      mediaUri,
      mediaType,
      privacy: 'public',
      allowedViewerUids: [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    state.postDraft = '';
    state.postMediaFile = null;
    render();
  } catch (e) {
    alert(e.message || 'Could not post');
  }
}

async function toggleLike(post) {
  if (!state.user) return;
  const ref = doc(db, 'posts', post.id, 'reactions', state.user.uid);
  const existing = await getDoc(ref);
  if (existing.exists()) {
    await deleteDoc(ref);
    await updateDoc(doc(db, 'posts', post.id), { likesCount: increment(-1) });
    state.likedByPost[post.id] = false;
  } else {
    await setDoc(ref, { userId: state.user.uid, emoji: '👍', createdAt: serverTimestamp() });
    await updateDoc(doc(db, 'posts', post.id), { likesCount: increment(1) });
    state.likedByPost[post.id] = true;
  }
  render();
}

async function toggleComments(postId) {
  state.openComments[postId] = !state.openComments[postId];
  if (state.openComments[postId] && !state.commentsByPost[postId]) {
    const snap = await getDocs(collection(db, 'posts', postId, 'comments'));
    state.commentsByPost[postId] = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
  }
  render();
}

async function submitComment(postId, text) {
  if (!state.user || !text.trim()) return;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
  await addDoc(collection(db, 'posts', postId, 'comments'), {
    authorId: state.user.uid,
    authorName: (me.displayName || 'Hello Member').slice(0, 80),
    authorAvatar: me.photoUrl || '',
    text: text.trim().slice(0, 1000),
    createdAt: serverTimestamp()
  });
  await updateDoc(doc(db, 'posts', postId), { commentsCount: increment(1) });
  const snap = await getDocs(collection(db, 'posts', postId, 'comments'));
  state.commentsByPost[postId] = snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
  render();
}

async function openOrCreateDm(otherUser, initialText = '', initialMediaUri = '', initialMediaType = 'none') {
  if (!state.user || !otherUser) return;
  const uid = state.user.uid;
  const otherId = otherUser.userId || otherUser.id;
  if (!otherId || otherId === uid) return;
  const pair = [uid, otherId].sort();
  const convId = `${pair[0]}_${pair[1]}`;
  const convRef = doc(db, 'conversations', convId);
  const existing = await getDoc(convRef);
  const meName = state.me?.displayName || state.user.displayName || 'Hello Member';
  const otherName = otherUser.displayName || 'Hello Member';
  if (!existing.exists()) {
    await setDoc(convRef, {
      isGroup: false,
      title: otherName.slice(0, 100),
      avatarUrl: '',
      participantUids: pair,
      participantNames: { [uid]: meName, [otherId]: otherName },
      lastMessage: initialText || '',
      lastSenderId: initialText ? uid : '',
      typingUids: [],
      readByUids: [uid],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  }
  state.tab = 'messenger';
  selectConversation(convId, true);
  if (initialText || initialMediaUri) {
    await sendChatMessage(convId, pair, initialText, initialMediaUri, initialMediaType);
  }
}

async function sendChatMessage(convId, participantUids, text, mediaUri = '', mediaType = 'none') {
  if (!state.user || !convId) return;
  if (!text && !mediaUri) return;
  const meName = state.me?.displayName || state.user.displayName || 'Hello Member';
  await addDoc(collection(db, 'conversations', convId, 'messages'), {
    conversationId: convId,
    senderId: state.user.uid,
    senderName: meName.slice(0, 80),
    text: text.slice(0, 2000),
    mediaUri,
    mediaType,
    fileName: '',
    reactions: {},
    participantUids,
    isDeleted: false,
    expiresAtMs: 0,
    createdAt: serverTimestamp()
  });
  await updateDoc(doc(db, 'conversations', convId), {
    lastMessage: (text || (mediaType === 'image' ? '📷 Photo' : 'Attachment')).slice(0, 500),
    lastSenderId: state.user.uid,
    readByUids: [state.user.uid],
    updatedAt: serverTimestamp()
  });
}

async function toggleFollow(targetUser) {
  if (!state.user) return;
  const uid = state.user.uid;
  const targetId = targetUser.userId || targetUser.id;
  const followId = `${uid}_${targetId}`;
  const ref = doc(db, 'follows', followId);
  const snap = await getDoc(ref);
  if (snap.exists()) {
    await deleteDoc(ref);
  } else {
    await setDoc(ref, {
      followerUid: uid,
      targetUid: targetId,
      targetName: (targetUser.displayName || 'Member').slice(0, 80),
      createdAt: serverTimestamp()
    });
  }
}

function renderLanding() {
  return `
    <header class="topbar">
      <div class="brand-row">
        <img class="brand-logo" src="./icon-192.png" alt="HELLO" />
        <span class="brand-name">HELLO</span>
      </div>
      <div class="top-actions">
        <button class="btn btn-secondary" id="installLaptopBtnTop">💻 Install on Laptop</button>
        <a class="btn btn-primary" href="${APK_URL}" target="_blank" rel="noopener">📱 Download Android APK</a>
      </div>
    </header>
    <main class="landing-wrap">
      <section>
        <div class="hero-badge">Web + Desktop App + Android Sync</div>
        <h1 class="hero-title">Use HELLO on your Laptop just like Facebook.</h1>
        <p class="hero-sub">
          Stay connected across your Android phone and laptop in real time. Post photos, chat in Messenger,
          browse Marketplace, join Groups, and install HELLO as a standalone desktop app on Windows, Mac, or Linux.
        </p>
        <div class="hero-cta-row">
          <button class="btn btn-primary" id="installLaptopBtnHero">💻 Install Laptop Desktop App</button>
          <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener">⬇ Download Android APK (v1.8.2)</a>
          <a class="btn btn-secondary" href="${RELEASES_URL}" target="_blank" rel="noopener">All Releases</a>
        </div>
        <div class="notice-box">
          <strong>How to install on Laptop like Facebook:</strong><br/>
          Click <strong>Install Laptop Desktop App</strong> (or click the Install icon in Chrome/Edge's address bar). It adds a <strong>HELLO</strong> app icon right to your Desktop & Start Menu!
        </div>
      </section>
      <section class="card auth-card">
        <div style="display:flex;align-items:center;gap:12px;">
          <img src="./icon-192.png" alt="HELLO" style="width:48px;height:48px;border-radius:14px;" />
          <div>
            <div style="font-weight:800;font-size:20px;">Sign in to HELLO Web</div>
            <div class="muted">Syncs live with your Android app account</div>
          </div>
        </div>
        <button class="btn btn-primary" id="googleSignInBtn" style="padding:12px 18px;font-size:15px;">
          Continue with Google
        </button>
        ${state.authError ? `<div class="error-box">${esc(state.authError)}</div>` : ''}
        <div class="muted" style="line-height:1.5;">
          Uses the same Firebase account, chats, friends, posts, groups, and Marketplace listings as your HELLO Android app.
        </div>
      </section>
    </main>
  `;
}

function renderCenterStage() {
  const uid = state.user.uid;
  const q = state.search.trim().toLowerCase();

  if (state.tab === 'messenger') {
    const convs = state.conversations.filter((c) => {
      if (!q) return true;
      return (c.title || '').toLowerCase().includes(q) || Object.values(c.participantNames || {}).some((n) => String(n).toLowerCase().includes(q));
    });
    const activeConv = state.conversations.find((c) => c.id === state.activeConvId) || convs[0];
    return `
      <div class="messenger-shell">
        <div class="conv-list ${state.mobileShowChat ? 'hidden-mobile' : ''}">
          <div style="padding:8px 10px;font-weight:800;font-size:16px;">Messenger</div>
          ${convs.length === 0 ? `<div class="muted" style="padding:12px;">No conversations yet. Start one from Friends!</div>` : convs.map((c) => {
            const otherId = !c.isGroup ? (c.participantUids || []).find((x) => x !== uid) : null;
            const resolved = otherId ? resolveAuthor(otherId, c.participantNames?.[otherId] || c.title) : { name: c.title || 'Group Chat', avatar: c.avatarUrl || '', online: false };
            const active = activeConv && activeConv.id === c.id;
            const unread = !(c.readByUids || []).includes(uid);
            return `
              <button class="conv-item ${active ? 'active' : ''}" data-conv-id="${esc(c.id)}">
                ${avatarHtml(resolved.name, resolved.avatar, 42, resolved.online)}
                <div style="flex:1;min-width:0;">
                  <div style="font-weight:${unread ? '800' : '600'};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(resolved.name)}</div>
                  <div class="muted" style="font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(c.lastMessage || 'Start chatting')}</div>
                </div>
              </button>
            `;
          }).join('')}
        </div>
        <div class="chat-pane ${!state.mobileShowChat ? 'hidden-mobile' : ''}">
          ${activeConv ? (() => {
            const otherId = !activeConv.isGroup ? (activeConv.participantUids || []).find((x) => x !== uid) : null;
            const resolved = otherId ? resolveAuthor(otherId, activeConv.participantNames?.[otherId] || activeConv.title) : { name: activeConv.title || 'Group Chat', avatar: activeConv.avatarUrl || '', online: false };
            return `
              <div class="chat-head">
                <div style="display:flex;align-items:center;gap:10px;">
                  <button class="btn btn-secondary" id="mobileBackChat" style="padding:6px 10px;">←</button>
                  ${avatarHtml(resolved.name, resolved.avatar, 38, resolved.online)}
                  <div>
                    <div style="font-weight:700;">${esc(resolved.name)}</div>
                    <div class="muted" style="font-size:12px;">${resolved.online ? 'Active now' : (activeConv.isGroup ? `${(activeConv.participantUids || []).length} members` : 'Messenger')}</div>
                  </div>
                </div>
              </div>
              <div class="chat-msgs" id="chatMsgsBox">
                ${state.messages.filter((m) => !m.isDeleted).map((m) => {
                  const mine = m.senderId === uid;
                  const src = mediaSrc(m.mediaUri);
                  return `
                    <div class="msg-row ${mine ? 'mine' : 'theirs'}">
                      ${!mine ? `<div class="muted" style="font-size:11px;margin-bottom:2px;">${esc(resolveAuthor(m.senderId, m.senderName).name)}</div>` : ''}
                      <div class="bubble">
                        ${src && m.mediaType === 'image' ? `<img src="${esc(src)}" style="max-width:260px;border-radius:12px;display:block;margin-bottom:${m.text ? '6px' : '0'};"/>` : ''}
                        ${src && m.mediaType === 'video' ? `<video controls src="${esc(src)}" style="max-width:260px;border-radius:12px;display:block;"></video>` : ''}
                        ${src && m.mediaType === 'audio' ? `<audio controls src="${esc(src)}"></audio>` : ''}
                        ${m.mediaType === 'file' ? `<a href="${esc(src || '#')}" download="${esc(m.fileName || 'file')}" style="text-decoration:underline;">📎 ${esc(m.fileName || 'Attachment')}</a>` : ''}
                        ${m.text ? `<div>${esc(m.text)}</div>` : ''}
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>
              <div class="chat-input-bar">
                <label class="icon-btn" title="Send photo" style="cursor:pointer;">
                  📷
                  <input type="file" accept="image/*" id="chatPhotoInput" style="display:none;" />
                </label>
                <div class="chat-pill">
                  <input id="chatTextInput" type="text" placeholder="Message..." value="${esc(state.msgDraft)}" />
                  <button class="btn btn-secondary" id="chatHeartBtn" style="padding:5px 10px;border:none;background:transparent;font-size:16px;">❤️</button>
                </div>
                <button class="btn btn-primary" id="chatSendBtn">Send</button>
              </div>
            `;
          })() : `<div style="padding:36px;" class="muted">Select a conversation to start messaging.</div>`}
        </div>
      </div>
    `;
  }

  if (state.tab === 'friends') {
    const friendSet = new Set(state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== uid));
    const followingSet = new Set(state.follows.map((f) => f.targetUid));
    const incomingReqs = state.friendRequests.filter((r) => r.toUid === uid && r.status === 'pending');
    const people = state.users.filter((u) => (u.userId || u.id) !== uid && (!q || (u.displayName || '').toLowerCase().includes(q) || (u.handle || '').toLowerCase().includes(q)));
    return `
      <div style="display:flex;flex-direction:column;gap:16px;">
        ${incomingReqs.length ? `
          <div class="card">
            <h3 style="margin:0 0 12px;">Friend Requests (${incomingReqs.length})</h3>
            <div class="grid-cards">
              ${incomingReqs.map((r) => `
                <div class="card" style="padding:14px;">
                  <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px;">
                    ${avatarHtml(r.fromName, r.fromAvatar, 42)}
                    <div><div style="font-weight:700;">${esc(r.fromName)}</div><div class="muted">Sent you a request</div></div>
                  </div>
                  <div style="display:flex;gap:8px;">
                    <button class="btn btn-primary accept-req-btn" data-req-id="${esc(r.id)}">Accept</button>
                    <button class="btn btn-secondary reject-req-btn" data-req-id="${esc(r.id)}">Decline</button>
                  </div>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        <div class="card">
          <h3 style="margin:0 0 12px;">People on HELLO (${people.length})</h3>
          <div class="grid-cards">
            ${people.map((p) => {
              const pid = p.userId || p.id;
              const isFriend = friendSet.has(pid);
              const isFollowing = followingSet.has(pid);
              return `
                <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:10px;">
                  <div style="display:flex;align-items:center;gap:10px;">
                    ${avatarHtml(p.displayName, p.photoUrl, 44, p.isOnline && p.showOnlineStatus !== false)}
                    <div style="min-width:0;">
                      <div style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(p.displayName || 'Member')}</div>
                      <div class="muted">${esc(p.handle || '')}</div>
                    </div>
                  </div>
                  <div style="display:flex;gap:6px;flex-wrap:wrap;">
                    <button class="btn btn-primary msg-person-btn" data-uid="${esc(pid)}">💬 Message</button>
                    <button class="btn btn-secondary follow-btn" data-uid="${esc(pid)}">${isFollowing ? 'Following ✓' : '+ Follow'}</button>
                    ${isFriend ? `<span class="badge">Friend</span>` : `<button class="btn btn-secondary add-friend-btn" data-uid="${esc(pid)}">Add Friend</button>`}
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      </div>
    `;
  }

  if (state.tab === 'groups') {
    const groups = state.groups.filter((g) => !q || (g.name || '').toLowerCase().includes(q));
    return `
      <div class="card">
        <h3 style="margin:0 0 14px;">Public Groups (${groups.length})</h3>
        <div class="grid-cards">
          ${groups.map((g) => {
            const joined = (g.memberUids || []).includes(uid);
            return `
              <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:8px;">
                <div style="font-weight:800;font-size:16px;">${esc(g.name)}</div>
                <div class="muted">${esc(g.category || 'Community')} · ${(g.memberUids || []).length} members</div>
                <div style="font-size:13px;">${esc(g.description || '')}</div>
                <span class="badge">${joined ? 'Member ✓' : 'Public Group'}</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  if (state.tab === 'marketplace') {
    const items = state.marketplace.filter((m) => !q || (m.title || '').toLowerCase().includes(q) || (m.description || '').toLowerCase().includes(q));
    return `
      <div class="card">
        <h3 style="margin:0 0 14px;">HELLO Marketplace (${items.length})</h3>
        <div class="grid-cards">
          ${items.map((item) => {
            const src = mediaSrc(item.imageUri);
            return `
              <div class="card" style="padding:12px;display:flex;flex-direction:column;gap:6px;">
                ${src ? `<img class="market-img" src="${esc(src)}" alt="${esc(item.title)}" />` : `<div class="market-img" style="display:flex;align-items:center;justify-content:center;" class="muted">No photo</div>`}
                <div style="font-weight:800;font-size:16px;color:var(--primary);">${esc(item.currency || '$')}${Number(item.price || 0).toFixed(2)}</div>
                <div style="font-weight:700;">${esc(item.title)}</div>
                <div class="muted">${esc(item.condition || 'Available')} · ${esc(item.locationText || '')}</div>
                ${item.sellerId !== uid ? `<button class="btn btn-primary market-msg-btn" data-item-id="${esc(item.id)}" style="margin-top:6px;">Message Seller</button>` : `<span class="badge">Your Listing</span>`}
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  if (state.tab === 'profile') {
    const me = state.me || {};
    return `
      <div class="card" style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;align-items:center;gap:16px;">
          ${avatarHtml(me.displayName || state.user.displayName, me.photoUrl || state.user.photoURL, 72, true)}
          <div>
            <h2 style="margin:0;">${esc(me.displayName || state.user.displayName || 'Hello Member')}</h2>
            <div class="muted">${esc(me.handle || '')} · ${esc(me.profilePrivacy || 'public')} profile</div>
          </div>
        </div>
        <label style="font-weight:600;font-size:13px;">Display Name
          <input class="field" id="profNameInput" value="${esc(me.displayName || '')}" style="margin-top:6px;" />
        </label>
        <label style="font-weight:600;font-size:13px;">Bio
          <textarea class="field" id="profBioInput" rows="3" style="margin-top:6px;">${esc(me.bio || '')}</textarea>
        </label>
        <div>
          <button class="btn btn-primary" id="saveProfileBtn">Save Profile Changes</button>
        </div>
      </div>
    `;
  }

  // Home Feed
  const posts = state.posts.filter((p) => !q || (p.content || '').toLowerCase().includes(q) || (p.authorName || '').toLowerCase().includes(q));
  return `
    <div style="display:flex;flex-direction:column;gap:16px;">
      <div class="card composer">
        <div style="display:flex;gap:10px;align-items:flex-start;margin-bottom:10px;">
          ${avatarHtml(state.me?.displayName || state.user.displayName, state.me?.photoUrl || state.user.photoURL, 42, true)}
          <textarea id="postDraftInput" rows="2" placeholder="What's on your mind, ${esc((state.me?.displayName || 'friend').split(' ')[0])}?">${esc(state.postDraft)}</textarea>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
          <label class="btn btn-secondary" style="cursor:pointer;">
            📷 ${state.postMediaFile ? esc(state.postMediaFile.name) : 'Add Photo'}
            <input type="file" accept="image/*" id="postPhotoInput" style="display:none;" />
          </label>
          <button class="btn btn-primary" id="publishPostBtn">Post to HELLO</button>
        </div>
      </div>
      <div class="feed-list">
        ${posts.length === 0 ? `<div class="card muted">No public posts yet. Share the first post!</div>` : posts.map((p) => {
          const author = resolveAuthor(p.authorId, p.authorName, p.authorAvatar);
          const media = mediaSrc(p.mediaUri);
          const liked = Boolean(state.likedByPost[p.id]);
          const commentsOpen = Boolean(state.openComments[p.id]);
          const comments = state.commentsByPost[p.id] || [];
          return `
            <article class="card">
              <div class="post-header">
                ${avatarHtml(author.name, author.avatar, 42, author.online)}
                <div>
                  <div class="post-author">${esc(author.name)}</div>
                  <div class="post-meta">${fmtTime(p.createdAt)} · Public</div>
                </div>
              </div>
              ${p.content ? `<div class="post-body">${esc(p.content)}</div>` : ''}
              ${media && p.mediaType === 'image' ? `<img class="post-media" src="${esc(media)}" alt="Post media" />` : ''}
              ${media && p.mediaType === 'video' ? `<video class="post-media" controls src="${esc(media)}"></video>` : ''}
              <div class="post-actions">
                <button class="action-pill like-btn ${liked ? 'liked' : ''}" data-post-id="${esc(p.id)}">👍 Like (${p.likesCount || 0})</button>
                <button class="action-pill comment-toggle-btn" data-post-id="${esc(p.id)}">💬 Comment (${p.commentsCount || 0})</button>
                <button class="action-pill share-post-btn" data-post-id="${esc(p.id)}">↗ Share</button>
              </div>
              ${commentsOpen ? `
                <div class="comments-box">
                  ${comments.map((c) => {
                    const ca = resolveAuthor(c.authorId, c.authorName, c.authorAvatar);
                    return `<div class="comment-item"><strong>${esc(ca.name)}</strong>: ${esc(c.text)}</div>`;
                  }).join('')}
                  <div style="display:flex;gap:8px;margin-top:4px;">
                    <input class="field comment-input" data-post-id="${esc(p.id)}" placeholder="Write a comment..." style="padding:8px 12px;" />
                    <button class="btn btn-primary comment-send-btn" data-post-id="${esc(p.id)}">Send</button>
                  </div>
                </div>
              ` : ''}
            </article>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

function renderShell() {
  const uid = state.user.uid;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
  const activePeers = state.users.filter((u) => (u.userId || u.id) !== uid && u.isOnline && u.showOnlineStatus !== false);
  const unreadChats = state.conversations.filter((c) => !(c.readByUids || []).includes(uid)).length;

  return `
    <header class="topbar">
      <div class="brand-row">
        <img class="brand-logo" src="./icon-192.png" alt="HELLO" />
        <span class="brand-name">HELLO</span>
        <div class="search-pill">
          <span>🔍</span>
          <input id="globalSearchInput" type="search" placeholder="Search HELLO..." value="${esc(state.search)}" />
        </div>
      </div>
      <nav class="nav-tabs">
        <button class="nav-tab ${state.tab === 'feed' ? 'active' : ''}" data-tab="feed">🏠 <span>Feed</span></button>
        <button class="nav-tab ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger">💬 <span>Messenger${unreadChats ? ` (${unreadChats})` : ''}</span></button>
        <button class="nav-tab ${state.tab === 'friends' ? 'active' : ''}" data-tab="friends">👥 <span>Friends</span></button>
        <button class="nav-tab ${state.tab === 'groups' ? 'active' : ''}" data-tab="groups">🛡 <span>Groups</span></button>
        <button class="nav-tab ${state.tab === 'marketplace' ? 'active' : ''}" data-tab="marketplace">🛍 <span>Shop</span></button>
      </nav>
      <div class="top-actions">
        <button class="btn btn-secondary" id="installLaptopBtnTop" title="Install HELLO on Laptop">💻 App</button>
        <button class="icon-btn" id="themeToggleBtn" title="Toggle theme">${state.theme === 'dark' ? '☀️' : '🌙'}</button>
        <button class="btn btn-secondary" id="signOutBtn">Sign Out</button>
      </div>
    </header>
    ${state.statusBanner ? `<div style="max-width:1340px;margin:10px auto 0;padding:0 18px;"><div class="notice-box">${esc(state.statusBanner)}</div></div>` : ''}
    <div class="app-grid">
      <aside class="sticky-col left-col">
        <div class="card" style="display:flex;align-items:center;gap:12px;">
          ${avatarHtml(me.displayName, me.photoUrl, 46, true)}
          <div style="min-width:0;">
            <div style="font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(me.displayName)}</div>
            <div class="muted">${esc(me.handle || '')}</div>
          </div>
        </div>
        <div class="card side-menu">
          <button class="side-item ${state.tab === 'feed' ? 'active' : ''}" data-tab="feed">🏠 Home Feed</button>
          <button class="side-item ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger">💬 Messenger ${unreadChats ? `<span class="badge">${unreadChats}</span>` : ''}</button>
          <button class="side-item ${state.tab === 'friends' ? 'active' : ''}" data-tab="friends">👥 Friends & Follow</button>
          <button class="side-item ${state.tab === 'groups' ? 'active' : ''}" data-tab="groups">🛡 Groups</button>
          <button class="side-item ${state.tab === 'marketplace' ? 'active' : ''}" data-tab="marketplace">🛍 Marketplace</button>
          <button class="side-item ${state.tab === 'profile' ? 'active' : ''}" data-tab="profile">👤 Edit Profile</button>
        </div>
        <div class="card" style="display:flex;flex-direction:column;gap:8px;">
          <div style="font-weight:800;font-size:14px;">Get HELLO Apps</div>
          <button class="btn btn-primary" id="installLaptopBtnSide">💻 Install on Laptop</button>
          <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener">📱 Download Android APK</a>
        </div>
      </aside>
      <main>${renderCenterStage()}</main>
      <aside class="sticky-col right-col">
        <div class="card">
          <div style="font-weight:800;margin-bottom:10px;">Active Friends (${activePeers.length})</div>
          <div style="display:flex;flex-direction:column;gap:6px;">
            ${activePeers.length === 0 ? `<div class="muted">No friends active right now.</div>` : activePeers.slice(0, 15).map((u) => `
              <button class="side-item msg-person-btn" data-uid="${esc(u.userId || u.id)}">
                ${avatarHtml(u.displayName, u.photoUrl, 34, true)}
                <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(u.displayName || 'Member')}</span>
              </button>
            `).join('')}
          </div>
        </div>
      </aside>
    </div>
  `;
}

function render() {
  const root = document.getElementById('root');
  if (!root) return;
  root.innerHTML = state.user ? renderShell() : renderLanding();
  bindEvents();
}

function bindEvents() {
  document.getElementById('googleSignInBtn')?.addEventListener('click', signInGoogle);
  document.getElementById('installLaptopBtnTop')?.addEventListener('click', installLaptopApp);
  document.getElementById('installLaptopBtnHero')?.addEventListener('click', installLaptopApp);
  document.getElementById('installLaptopBtnSide')?.addEventListener('click', installLaptopApp);
  document.getElementById('signOutBtn')?.addEventListener('click', () => signOut(auth));
  document.getElementById('themeToggleBtn')?.addEventListener('click', () => {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    localStorage.setItem('hello_theme', state.theme);
    document.documentElement.classList.toggle('light', state.theme === 'light');
    render();
  });

  document.getElementById('globalSearchInput')?.addEventListener('input', (e) => {
    state.search = e.target.value;
    render();
    const el = document.getElementById('globalSearchInput');
    if (el) { el.focus(); el.setSelectionRange(el.value.length, el.value.length); }
  });

  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.tab = btn.getAttribute('data-tab');
      state.mobileShowChat = false;
      render();
    });
  });

  document.getElementById('postDraftInput')?.addEventListener('input', (e) => { state.postDraft = e.target.value; });
  document.getElementById('postPhotoInput')?.addEventListener('change', (e) => {
    state.postMediaFile = e.target.files?.[0] || null;
    render();
  });
  document.getElementById('publishPostBtn')?.addEventListener('click', publishPost);

  document.querySelectorAll('.like-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const post = state.posts.find((p) => p.id === btn.getAttribute('data-post-id'));
      if (post) toggleLike(post);
    });
  });
  document.querySelectorAll('.comment-toggle-btn').forEach((btn) => {
    btn.addEventListener('click', () => toggleComments(btn.getAttribute('data-post-id')));
  });
  document.querySelectorAll('.comment-send-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const pid = btn.getAttribute('data-post-id');
      const input = document.querySelector(`.comment-input[data-post-id="${pid}"]`);
      if (input) submitComment(pid, input.value);
    });
  });
  document.querySelectorAll('.share-post-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const pid = btn.getAttribute('data-post-id');
      navigator.clipboard?.writeText(`${window.location.href}#post-${pid}`);
      alert('Post link copied!');
    });
  });

  document.querySelectorAll('[data-conv-id]').forEach((btn) => {
    btn.addEventListener('click', () => selectConversation(btn.getAttribute('data-conv-id'), true));
  });
  document.getElementById('mobileBackChat')?.addEventListener('click', () => {
    state.mobileShowChat = false;
    render();
  });

  const chatInput = document.getElementById('chatTextInput');
  chatInput?.addEventListener('input', (e) => { state.msgDraft = e.target.value; });
  chatInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && state.msgDraft.trim()) {
      const conv = state.conversations.find((c) => c.id === state.activeConvId);
      const t = state.msgDraft.trim();
      state.msgDraft = '';
      if (conv) sendChatMessage(conv.id, conv.participantUids || [], t);
    }
  });
  document.getElementById('chatSendBtn')?.addEventListener('click', () => {
    const conv = state.conversations.find((c) => c.id === state.activeConvId);
    const t = state.msgDraft.trim();
    if (conv && t) {
      state.msgDraft = '';
      sendChatMessage(conv.id, conv.participantUids || [], t);
    }
  });
  document.getElementById('chatHeartBtn')?.addEventListener('click', () => {
    const conv = state.conversations.find((c) => c.id === state.activeConvId);
    if (conv) sendChatMessage(conv.id, conv.participantUids || [], '❤️');
  });
  document.getElementById('chatPhotoInput')?.addEventListener('change', async (e) => {
    const file = e.target.files?.[0];
    const conv = state.conversations.find((c) => c.id === state.activeConvId);
    if (file && conv) {
      const uri = await uploadImageBlob(file);
      await sendChatMessage(conv.id, conv.participantUids || [], '', uri, 'image');
    }
  });

  document.querySelectorAll('.msg-person-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const peer = state.usersById[btn.getAttribute('data-uid')];
      if (peer) openOrCreateDm(peer);
    });
  });
  document.querySelectorAll('.follow-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const peer = state.usersById[btn.getAttribute('data-uid')];
      if (peer) toggleFollow(peer);
    });
  });
  document.querySelectorAll('.add-friend-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const peer = state.usersById[btn.getAttribute('data-uid')];
      if (!peer || !state.user) return;
      const uid = state.user.uid;
      const toUid = peer.userId || peer.id;
      const reqId = `${uid}_${toUid}`;
      await setDoc(doc(db, 'friend_requests', reqId), {
        fromUid: uid,
        toUid,
        fromName: state.me?.displayName || state.user.displayName || 'Hello Member',
        fromAvatar: state.me?.photoUrl || '',
        toName: peer.displayName || 'Member',
        toAvatar: peer.photoUrl || '',
        status: 'pending',
        participantUids: [uid, toUid].sort(),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      alert(`Friend request sent to ${peer.displayName}!`);
    });
  });
  document.querySelectorAll('.accept-req-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const req = state.friendRequests.find((r) => r.id === btn.getAttribute('data-req-id'));
      if (!req) return;
      const pair = [req.fromUid, req.toUid].sort();
      const batch = writeBatch(db);
      batch.update(doc(db, 'friend_requests', req.id), { status: 'accepted', updatedAt: serverTimestamp() });
      batch.set(doc(db, 'friendships', `${pair[0]}_${pair[1]}`), { participantUids: pair, createdAt: serverTimestamp() });
      await batch.commit();
    });
  });
  document.querySelectorAll('.reject-req-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      await updateDoc(doc(db, 'friend_requests', btn.getAttribute('data-req-id')), { status: 'rejected', updatedAt: serverTimestamp() });
    });
  });
  document.querySelectorAll('.market-msg-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = state.marketplace.find((m) => m.id === btn.getAttribute('data-item-id'));
      if (!item) return;
      const seller = state.usersById[item.sellerId] || { userId: item.sellerId, displayName: item.sellerName || 'Seller' };
      openOrCreateDm(
        seller,
        `Is this available? (${item.title} · ${item.currency || '$'}${Number(item.price || 0).toFixed(2)})`,
        item.imageUri || '',
        item.imageUri ? 'image' : 'none'
      );
    });
  });
  document.getElementById('saveProfileBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const name = document.getElementById('profNameInput')?.value?.trim();
    const bio = document.getElementById('profBioInput')?.value?.trim() || '';
    if (!name) return;
    await updateDoc(doc(db, 'users_public', state.user.uid), {
      displayName: name.slice(0, 80),
      bio: bio.slice(0, 300),
      updatedAt: serverTimestamp()
    });
    state.statusBanner = 'Profile updated across laptop & mobile!';
    render();
  });
}

onAuthStateChanged(auth, async (u) => {
  state.user = u;
  if (u) {
    try {
      await ensureWebProfile(u);
      startListeners(u.uid);
    } catch (e) {
      state.authError = e?.message || 'Unable to load profile';
    }
  } else {
    clearSubs();
    state.me = null;
  }
  render();
});

render();
