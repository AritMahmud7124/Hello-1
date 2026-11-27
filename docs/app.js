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
  limit,
  serverTimestamp,
  increment,
  arrayUnion,
  arrayRemove,
  writeBatch
} from 'https://www.gstatic.com/firebasejs/11.1.0/firebase-firestore.js';

const APK_URL = 'https://github.com/AritMahmud7124/Hello-1/releases/download/v1.8.3/hello-social-v1.8.3.apk';
const RELEASES_URL = 'https://github.com/AritMahmud7124/Hello-1/releases';
const AUTH_SETTINGS_URL = 'https://console.firebase.google.com/project/gen-lang-client-0583025546/authentication/settings';
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

const SVG_PATHS = {
  home: '<path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>',
  reels: '<path d="M10 8v8l6-4-6-4zm9-5H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z"/>',
  friends: '<path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>',
  groups: '<path d="M12 12.75c1.63 0 3.07.39 4.24.9 1.08.48 1.76 1.56 1.76 2.73V18H6v-1.61c0-1.18.68-2.26 1.76-2.73 1.17-.52 2.61-.91 4.24-.91zM4 13c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm1.13 1.1c-.37-.06-.74-.1-1.13-.1-.99 0-1.93.21-2.78.58A2.01 2.01 0 0 0 0 16.43V18h4.5v-1.61c0-.83.23-1.61.63-2.29zM20 13c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm4 3.43c0-.81-.48-1.53-1.22-1.85-.85-.37-1.79-.58-2.78-.58-.39 0-.76.04-1.13.1.4.68.63 1.46.63 2.29V18H24v-1.57zM12 6c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3z"/>',
  chat: '<path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>',
  shop: '<path d="M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z"/>',
  bell: '<path d="M12 22c1.1 0 2-.9 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>',
  search: '<path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>',
  camera: '<circle cx="12" cy="12" r="3.2"/><path d="M9 2 7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/>',
  photo: '<path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>',
  mic: '<path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z"/>',
  stop: '<path d="M6 6h12v12H6z"/>',
  attach: '<path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/>',
  heart: '<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>',
  send: '<path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>',
  like: '<path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/>',
  comment: '<path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>',
  share: '<path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z"/>',
  bookmark: '<path d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>',
  call: '<path d="M6.62 10.79a15.05 15.05 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.01-.24c1.12.37 2.33.57 3.58.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.58.11.35.03.74-.25 1.01l-2.2 2.2z"/>',
  video: '<path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>',
  verified: '<path d="m23 12-2.44-2.79.34-3.69-3.61-.82-1.89-3.2L12 2.96 8.6 1.5 6.71 4.69 3.1 5.5l.34 3.7L1 12l2.44 2.79-.34 3.7 3.61.82L8.6 22.5l3.4-1.47 3.4 1.46 1.89-3.19 3.61-.82-.34-3.69L23 12zm-12.91 4.72-3.8-3.81 1.48-1.48 2.32 2.33 5.85-5.87 1.48 1.48-7.33 7.35z"/>',
  shield: '<path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>',
  laptop: '<path d="M20 18c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z"/>',
  phone: '<path d="M17 1.01 7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/>',
  sun: '<path d="M6.76 4.84 4.96 3.05 3.55 4.46l1.79 1.79 1.42-1.41zM4 10.5H1v2h3v-2zm9-9.95h-2V3.5h2V.55zm7.45 3.91-1.41-1.41-1.79 1.79 1.41 1.41 1.79-1.79zm-3.21 13.7 1.79 1.8 1.41-1.41-1.8-1.79-1.4 1.4zM20 10.5v2h3v-2h-3zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm-1 16.95h2V19.5h-2v2.95zm-7.45-3.91 1.41 1.41 1.79-1.8-1.41-1.41-1.79 1.8z"/>',
  moon: '<path d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/>',
  plus: '<path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>',
  back: '<path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>',
  close: '<path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>',
  file: '<path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>',
  follow: '<path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>',
  check: '<path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>',
  trash: '<path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>',
  user: '<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>',
  logout: '<path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>'
};

function iconSvg(name, size = 19, cls = '') {
  const path = SVG_PATHS[name] || '';
  return `<svg class="svg-icon ${cls}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" style="width:${size}px;height:${size}px">${path}</svg>`;
}

const state = {
  user: null,
  me: null,
  tab: 'feed',
  feedSort: 'latest',
  theme: localStorage.getItem('hello_theme') || 'dark',
  search: '',
  authError: '',
  unauthorizedDomain: '',
  deferredPrompt: null,
  usersById: {},
  users: [],
  posts: [],
  stories: [],
  reels: [],
  savedPostIds: new Set(),
  commentsByPost: {},
  likedByPost: {},
  openComments: {},
  conversations: [],
  inboxFilter: 'all',
  activeConvId: null,
  messages: [],
  activeCalls: [],
  friendships: [],
  friendRequests: [],
  follows: [],
  groups: [],
  marketplace: [],
  marketCat: 'All',
  notifications: [],
  postDraft: '',
  postPrivacy: 'public',
  postMediaFile: null,
  msgDraft: '',
  showAttachMenu: false,
  isRecordingVoice: false,
  recordingSec: 0,
  mobileShowChat: false,
  activeStory: null,
  profileViewUid: null,
  showNotificationsModal: false,
  showCreateStoryModal: false,
  showCreateListingModal: false,
  showNewGroupChatModal: false,
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

function isValidId(id) {
  return typeof id === 'string' && /^[a-zA-Z0-9_-]{1,128}$/.test(id);
}

function safeUrl(raw, allowDataMedia = true) {
  if (!raw || typeof raw !== 'string') return '';
  const trimmed = raw.trim();
  if (trimmed.startsWith('https://') || trimmed.startsWith('blob:')) return trimmed;
  if (allowDataMedia) {
    if (/^data:image\/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=]+$/i.test(trimmed)) return trimmed;
    if (/^data:video\/(mp4|webm|quicktime);base64,[A-Za-z0-9+/=]+$/i.test(trimmed)) return trimmed;
    if (/^data:audio\/(webm|mp4|mpeg|ogg|wav|x-m4a);base64,[A-Za-z0-9+/=]+$/i.test(trimmed)) return trimmed;
  }
  return '';
}

function base64ToBlobUrl(b64, mimeType) {
  try {
    const safeMime = (mimeType && !/html|svg|javascript|script/i.test(mimeType)) ? mimeType : 'application/octet-stream';
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return URL.createObjectURL(new Blob([bytes], { type: safeMime }));
  } catch (_) {
    return '';
  }
}

async function resolveMediaUri(uri) {
  if (!uri || typeof uri !== 'string') return '';
  if (!uri.startsWith('firestore://media/')) return safeUrl(uri, true);
  if (mediaCache.has(uri)) return mediaCache.get(uri);
  if (mediaPending.has(uri)) return '';
  mediaPending.add(uri);
  try {
    const mediaId = uri.replace('firestore://media/', '').split('?')[0].split('/')[0].trim();
    if (!isValidId(mediaId)) return '';
    const blobSnap = await getDoc(doc(db, 'media_blobs', mediaId));
    if (!blobSnap.exists()) return '';
    const blobMeta = blobSnap.data();
    const chunksSnap = await getDocs(collection(db, 'media_blobs', mediaId, 'chunks'));
    const sorted = chunksSnap.docs.map((d) => d.data()).sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
    const b64 = sorted.map((c) => c.data || '').join('');
    const mime = blobMeta.mimeType || 'image/jpeg';
    const objectUrl = base64ToBlobUrl(b64, mime);
    if (objectUrl) {
      mediaCache.set(uri, objectUrl);
      render();
    }
    return objectUrl;
  } catch (_) {
    return '';
  } finally {
    mediaPending.delete(uri);
  }
}

function mediaSrc(uri) {
  if (!uri) return '';
  if (!uri.startsWith('firestore://media/')) return safeUrl(uri, true);
  if (mediaCache.has(uri)) return mediaCache.get(uri);
  resolveMediaUri(uri);
  return '';
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
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
    online: Boolean(live?.isOnline && live?.showOnlineStatus !== false),
    verified: Boolean(live?.isVerifiedOwner)
  };
}

function avatarHtml(name, photoUrl, size = 40, isOnline = false) {
  const src = mediaSrc(photoUrl);
  const initials = (name || 'H').trim().slice(0, 1).toUpperCase();
  const dot = isOnline ? '<span class="online-dot"></span>' : '';
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
      state.statusBanner = 'HELLO Desktop App is installing on your device.';
    }
    render();
  } else {
    state.statusBanner = 'In Chrome or Edge, click the Install App icon on the right side of the address bar (or Menu → Apps → Install HELLO).';
    render();
  }
}

async function signInGoogle() {
  state.authError = '';
  state.unauthorizedDomain = '';
  render();
  try {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  } catch (err) {
    if (err?.code === 'auth/unauthorized-domain') {
      state.unauthorizedDomain = window.location.hostname;
      state.authError = `Domain "${window.location.hostname}" needs to be added once to Firebase Auth Authorized domains.`;
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

  unsubs.push(onSnapshot(query(collection(db, 'stories'), where('privacy', '==', 'public'), limit(35)), (snap) => {
    const cutoff = Date.now() - 24 * 60 * 60 * 1000;
    state.stories = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .filter((s) => s.isSavedHighlight || ((s.createdAt?.seconds ? s.createdAt.seconds * 1000 : Date.now()) >= cutoff))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }));

  unsubs.push(onSnapshot(query(collection(db, 'reels'), limit(40)), (snap) => {
    state.reels = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'saved_posts'), (snap) => {
    state.savedPostIds = new Set(snap.docs.map((d) => d.id));
    render();
  }));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'notifications'), (snap) => {
    state.notifications = snap.docs
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

  unsubs.push(onSnapshot(query(collection(db, 'calls'), where('participantUids', 'array-contains', uid)), (snap) => {
    const now = Date.now();
    state.activeCalls = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .filter((c) => (c.status === 'ringing' || c.status === 'active') && (now - (c.startedAtMs || now)) < 180000);
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

  // Matches SpacesRepository.kt real Firestore collection: marketplace_listings
  unsubs.push(onSnapshot(query(collection(db, 'marketplace_listings'), limit(50)), (snap) => {
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
  if (!convId || !isValidId(convId)) { state.messages = []; render(); return; }
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

async function uploadMediaBlob(file, forcedMime = '') {
  const buf = await file.arrayBuffer();
  const bytes = new Uint8Array(buf);
  if (bytes.length === 0) throw new Error('Empty file cannot be uploaded.');
  if (bytes.length > 9 * 1024 * 1024) throw new Error('File must be under 9 MB.');
  let binary = '';
  const step = 8192;
  for (let i = 0; i < bytes.length; i += step) {
    binary += String.fromCharCode(...bytes.subarray(i, i + step));
  }
  const b64 = btoa(binary);
  const chunkSize = 450000;
  const chunks = [];
  for (let i = 0; i < b64.length; i += chunkSize) chunks.push(b64.slice(i, i + chunkSize));
  if (chunks.length > 28) throw new Error('File exceeds maximum allowed chunks.');

  const mediaId = crypto.randomUUID();
  const rawMime = forcedMime || file.type || 'application/octet-stream';
  const safeMime = /svg|html|javascript/i.test(rawMime) ? 'application/octet-stream' : rawMime.slice(0, 100);
  const ext = ((file.name || 'bin').split('.').pop() || 'bin').toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 8) || 'bin';

  await setDoc(doc(db, 'media_blobs', mediaId), {
    ownerId: state.user.uid,
    mimeType: safeMime,
    ext,
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
      mediaUri = await uploadMediaBlob(state.postMediaFile);
      mediaType = state.postMediaFile.type?.startsWith('video/') ? 'video' : 'image';
    }
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    const friendUids = state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== state.user.uid);
    await addDoc(collection(db, 'posts'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      content: text.slice(0, 5000),
      mediaUri,
      mediaType,
      privacy: state.postPrivacy,
      allowedViewerUids: state.postPrivacy === 'friends' ? friendUids.slice(0, 200) : [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    state.postDraft = '';
    state.postMediaFile = null;
    state.statusBanner = 'Post published to HELLO.';
    render();
  } catch (e) {
    state.statusBanner = e?.message || 'Could not publish post.';
    render();
  }
}

async function toggleLike(post) {
  if (!state.user) return;
  const ref = doc(db, 'posts', post.id, 'reactions', state.user.uid);
  const existing = await getDoc(ref);
  if (existing.exists()) {
    await deleteDoc(ref);
    if ((post.likesCount || 0) > 0) await updateDoc(doc(db, 'posts', post.id), { likesCount: increment(-1), updatedAt: serverTimestamp() });
    state.likedByPost[post.id] = false;
  } else {
    await setDoc(ref, { userId: state.user.uid, emoji: '👍', createdAt: serverTimestamp() });
    await updateDoc(doc(db, 'posts', post.id), { likesCount: increment(1), updatedAt: serverTimestamp() });
    state.likedByPost[post.id] = true;
  }
  render();
}

async function toggleSavePost(post) {
  if (!state.user) return;
  const ref = doc(db, 'users', state.user.uid, 'saved_posts', post.id);
  if (state.savedPostIds.has(post.id)) {
    await deleteDoc(ref);
  } else {
    await setDoc(ref, {
      postId: post.id,
      itemType: 'post',
      authorName: (post.authorName || 'Member').slice(0, 80),
      excerpt: (post.content || '').slice(0, 280),
      createdAt: serverTimestamp()
    });
  }
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
  await updateDoc(doc(db, 'posts', postId), { commentsCount: increment(1), updatedAt: serverTimestamp() });
  const snap = await getDocs(collection(db, 'posts', postId, 'comments'));
  state.commentsByPost[postId] = snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
  render();
}

async function openOrCreateDm(targetUser, initialText = '', initialMediaUri = '', initialMediaType = 'none') {
  if (!state.user || !targetUser) return;
  const uid = state.user.uid;
  const targetId = targetUser.userId || targetUser.id;
  if (!isValidId(targetId)) return;
  const pair = [uid, targetId].sort();
  const convId = `${pair[0]}_${pair[1]}`;
  const convRef = doc(db, 'conversations', convId);
  const existing = await getDoc(convRef);
  const myName = state.me?.displayName || state.user.displayName || 'Hello Member';
  if (!existing.exists()) {
    await setDoc(convRef, {
      isGroup: false,
      title: '',
      avatarUrl: '',
      participantUids: pair,
      participantNames: { [uid]: myName, [targetId]: targetUser.displayName || 'Member' },
      lastMessage: '',
      lastSenderId: '',
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
  render();
}

async function sendChatMessage(convId, participantUids, text, mediaUri = '', mediaType = 'none', fileName = '') {
  if (!state.user || !isValidId(convId)) return;
  if (!text && !mediaUri) return;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
  await addDoc(collection(db, 'conversations', convId, 'messages'), {
    conversationId: convId,
    senderId: state.user.uid,
    senderName: (me.displayName || 'Hello Member').slice(0, 80),
    text: (text || '').slice(0, 2000),
    mediaUri: mediaUri || '',
    mediaType,
    fileName: (fileName || '').slice(0, 200),
    reactions: {},
    participantUids,
    isDeleted: false,
    expiresAtMs: 0,
    createdAt: serverTimestamp()
  });
  const preview = text || (mediaType === 'image' ? 'Photo' : mediaType === 'video' ? 'Video' : mediaType === 'audio' ? 'Voice message' : `File: ${fileName || 'Attachment'}`);
  await updateDoc(doc(db, 'conversations', convId), {
    lastMessage: preview.slice(0, 500),
    lastSenderId: state.user.uid,
    readByUids: [state.user.uid],
    updatedAt: serverTimestamp()
  });
}

async function startCallSignal(conv, callType) {
  if (!state.user || !conv) return;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
  const otherId = !conv.isGroup ? (conv.participantUids || []).find((x) => x !== state.user.uid) : null;
  const title = otherId ? resolveAuthor(otherId, conv.participantNames?.[otherId] || conv.title).name : (conv.title || 'Group Call');
  await addDoc(collection(db, 'calls'), {
    conversationId: conv.id,
    conversationTitle: title.slice(0, 100),
    callerId: state.user.uid,
    callerName: (me.displayName || 'Hello Member').slice(0, 80),
    participantUids: conv.participantUids || [state.user.uid],
    acceptedUids: [state.user.uid],
    callType,
    status: 'ringing',
    startedAtMs: Date.now(),
    endedAtMs: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
  state.statusBanner = `Ringing ${title} on HELLO (${callType} call)...`;
  render();
}

let mediaRecorder = null;
let recChunks = [];
let recTimer = null;

async function toggleVoiceRecord(conv) {
  if (!conv || !state.user) return;
  if (state.isRecordingVoice && mediaRecorder) {
    mediaRecorder.stop();
    return;
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    recChunks = [];
    const mime = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4';
    mediaRecorder = new MediaRecorder(stream, { mimeType: mime });
    state.isRecordingVoice = true;
    state.recordingSec = 0;
    recTimer = setInterval(() => {
      state.recordingSec += 1;
      render();
    }, 1000);
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) recChunks.push(e.data); };
    mediaRecorder.onstop = async () => {
      clearInterval(recTimer);
      state.isRecordingVoice = false;
      state.recordingSec = 0;
      stream.getTracks().forEach((t) => t.stop());
      const blob = new Blob(recChunks, { type: mime });
      if (blob.size > 0) {
        const file = new File([blob], `voice-${Date.now()}.webm`, { type: mime });
        const uri = await uploadMediaBlob(file, mime);
        await sendChatMessage(conv.id, conv.participantUids || [], '', uri, 'audio', file.name);
      }
      render();
    };
    mediaRecorder.start();
    render();
  } catch (_) {
    state.isRecordingVoice = false;
    state.statusBanner = 'Microphone permission is required to send voice notes.';
    render();
  }
}

async function toggleFollow(targetUser) {
  if (!state.user) return;
  const uid = state.user.uid;
  const targetId = targetUser.userId || targetUser.id;
  if (!isValidId(targetId)) return;
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

async function toggleJoinGroup(group) {
  if (!state.user) return;
  const uid = state.user.uid;
  const ref = doc(db, 'groups', group.id);
  const members = group.memberUids || [];
  if (members.includes(uid)) {
    if (group.creatorId === uid) return;
    await updateDoc(ref, {
      memberUids: arrayRemove(uid),
      membersCount: Math.max(0, (group.membersCount || members.length) - 1),
      updatedAt: serverTimestamp()
    });
  } else {
    await updateDoc(ref, {
      memberUids: arrayUnion(uid),
      membersCount: (group.membersCount || members.length) + 1,
      updatedAt: serverTimestamp()
    });
  }
}

function renderBrandTile(size = 36) {
  return `<span class="brand-tile" style="width:${size}px;height:${size}px;border-radius:${Math.round(size * 0.26)}px"><img src="./hello-logo.png" alt="HELLO"/></span>`;
}

function renderLanding() {
  return `
    <header class="topbar">
      <div class="brand-row">
        ${renderBrandTile(36)}
        <span class="brand-name">HELLO</span>
      </div>
      <div class="top-actions">
        <button class="icon-btn" id="themeToggleBtn" title="Toggle Theme">${iconSvg(state.theme === 'dark' ? 'sun' : 'moon', 18)}</button>
        <button class="btn btn-secondary" id="installLaptopBtnTop">${iconSvg('laptop', 17)}<span>Install Desktop App</span></button>
        <a class="btn btn-primary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 17)}<span>Android APK</span></a>
      </div>
    </header>
    <main class="auth-page">
      <div class="auth-shell">
        <section class="auth-intro">
          <div class="security-pill">${iconSvg('shield', 15)}<span>Verified Firebase Auth & Encrypted Sync</span></div>
          <h1 class="auth-headline">HELLO for Web, Desktop & Android.</h1>
          <p class="auth-desc">
            Connect with friends, share Stories & Reels, chat in real-time Messenger, join Groups, and shop Marketplace across your phone and laptop with live cloud sync.
          </p>
          <div class="feature-list">
            <div class="feature-chip">${iconSvg('chat', 18)}<span>Live Messenger & Voice</span></div>
            <div class="feature-chip">${iconSvg('reels', 18)}<span>Stories, Posts & Reels</span></div>
            <div class="feature-chip">${iconSvg('shop', 18)}<span>Marketplace & Groups</span></div>
            <div class="feature-chip">${iconSvg('shield', 18)}<span>Protected Account Data</span></div>
          </div>
          <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:6px;">
            <button class="btn btn-violet" id="installLaptopBtnHero">${iconSvg('laptop', 17)}<span>Install HELLO on Laptop</span></button>
            <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 17)}<span>Download Android APK (v1.8.3)</span></a>
          </div>
        </section>

        <section class="card auth-card">
          <div style="display:flex;align-items:center;gap:12px;">
            ${renderBrandTile(48)}
            <div>
              <div style="font-weight:800;font-size:20px;">Sign in to HELLO</div>
              <div class="muted">Same Google account as your HELLO Android app</div>
            </div>
          </div>
          <button class="btn btn-primary" id="googleSignInBtn" style="min-height:46px;font-size:15px;">
            Continue with Google
          </button>
          ${state.statusBanner ? `<div class="notice-box">${esc(state.statusBanner)}</div>` : ''}
          ${state.authError ? `
            <div class="error-box">
              <div>${esc(state.authError)}</div>
              ${state.unauthorizedDomain ? `
                <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;">
                  <a class="btn btn-primary" href="${AUTH_SETTINGS_URL}" target="_blank" rel="noopener noreferrer">Open Firebase Authorized Domains</a>
                  <button class="btn btn-secondary" id="copyDomainBtn" type="button">Copy "${esc(state.unauthorizedDomain)}"</button>
                </div>
                <div style="margin-top:8px;font-size:12px;opacity:0.9;">
                  Authentication → Settings → Authorized domains → Add <code>${esc(state.unauthorizedDomain)}</code>
                </div>
              ` : ''}
            </div>
          ` : ''}
          <div class="muted" style="line-height:1.5;font-size:12.5px;">
            Protected by Google Sign-In, strict server-side Firestore rules, and Content Security Policy. No unauthorized account access.
          </div>
        </section>
      </div>
    </main>
  `;
}

function renderStoriesStrip() {
  const me = state.me || { displayName: state.user.displayName, photoUrl: state.user.photoURL };
  const myPhoto = mediaSrc(me.photoUrl);
  return `
    <div class="card" style="padding:14px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
        <span style="font-weight:700;font-size:15px;">Stories</span>
        <button class="btn btn-secondary" id="openCreateStoryBtn" style="padding:5px 12px;min-height:30px;font-size:12px;">${iconSvg('plus', 15)}<span>Add Story</span></button>
      </div>
      <div class="stories-row">
        <button class="story-card story-card-create" id="createStoryCardBtn">
          <div class="story-create-top">
            ${myPhoto ? `<img src="${esc(myPhoto)}" alt="Your story" />` : `<div style="width:100%;height:100%;background:linear-gradient(135deg,#1E1B4B,#7C3AED);"></div>`}
          </div>
          <div class="story-create-plus">${iconSvg('plus', 16)}</div>
          <div class="story-create-label">Create Story</div>
        </button>
        ${state.stories.map((s) => {
          const author = resolveAuthor(s.authorId, s.authorName, s.authorAvatar);
          const bgSrc = mediaSrc(s.mediaUri);
          const hex = /^#[0-9A-Fa-f]{6}$/.test(s.backgroundHex || '') ? s.backgroundHex : '#1E1B4B';
          return `
            <button class="story-card view-story-btn" data-story-id="${esc(s.id)}" style="background:${hex};">
              ${bgSrc ? `<img class="story-bg" src="${esc(bgSrc)}" alt="${esc(author.name)}" />` : ''}
              <div class="story-scrim">
                <span class="story-ring">${avatarHtml(author.name, author.avatar, 30, false)}</span>
                <div>
                  ${!bgSrc && s.caption ? `<div style="font-size:11px;margin-bottom:6px;opacity:0.95;line-height:1.25;">${esc(s.caption.slice(0, 48))}</div>` : ''}
                  <div class="story-name">${esc(author.name)}</div>
                </div>
              </div>
            </button>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

function renderCenterStage() {
  const uid = state.user.uid;
  const q = state.search.trim().toLowerCase();

  if (state.tab === 'messenger') {
    let convs = state.conversations.filter((c) => {
      if (!q) return true;
      return (c.title || '').toLowerCase().includes(q) || Object.values(c.participantNames || {}).some((n) => String(n).toLowerCase().includes(q));
    });
    if (state.inboxFilter === 'unread') convs = convs.filter((c) => !(c.readByUids || []).includes(uid));
    if (state.inboxFilter === 'groups') convs = convs.filter((c) => Boolean(c.isGroup));
    if (state.inboxFilter === 'active') {
      convs = convs.filter((c) => (c.participantUids || []).some((p) => p !== uid && state.usersById[p]?.isOnline));
    }

    const activeConv = state.conversations.find((c) => c.id === state.activeConvId) || convs[0];
    const hasDraft = state.msgDraft.trim().length > 0;

    return `
      <div class="messenger-shell">
        <div class="conv-list ${state.mobileShowChat ? 'hidden-mobile' : ''}">
          <div class="conv-list-head">
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <span style="font-weight:800;font-size:17px;">Messages</span>
              <button class="icon-btn" id="openNewGroupChatBtn" title="New Group Chat" style="width:34px;height:34px;">${iconSvg('plus', 18)}</button>
            </div>
            <div class="chip-row">
              ${[
                ['all', 'Primary'],
                ['unread', 'Unread'],
                ['active', 'Active'],
                ['groups', 'Groups']
              ].map(([k, lbl]) => `<button class="filter-chip ${state.inboxFilter === k ? 'active' : ''}" data-inbox-filter="${k}" style="padding:4px 10px;font-size:12px;">${lbl}</button>`).join('')}
            </div>
          </div>
          <div class="conv-scroll">
            ${convs.length === 0 ? `<div class="muted" style="padding:14px;">No conversations in this filter.</div>` : convs.map((c) => {
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
        </div>

        <div class="chat-pane ${!state.mobileShowChat ? 'hidden-mobile' : ''}">
          ${activeConv ? (() => {
            const otherId = !activeConv.isGroup ? (activeConv.participantUids || []).find((x) => x !== uid) : null;
            const resolved = otherId ? resolveAuthor(otherId, activeConv.participantNames?.[otherId] || activeConv.title) : { name: activeConv.title || 'Group Chat', avatar: activeConv.avatarUrl || '', online: false, verified: false };
            return `
              <div class="chat-head">
                <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                  <button class="icon-btn" id="mobileBackChat" title="Back" style="width:34px;height:34px;">${iconSvg('back', 18)}</button>
                  ${avatarHtml(resolved.name, resolved.avatar, 38, resolved.online)}
                  <div style="min-width:0;">
                    <div style="font-weight:700;display:flex;align-items:center;gap:5px;">
                      <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(resolved.name)}</span>
                      ${resolved.verified ? `<span class="verified-badge">${iconSvg('verified', 14)}</span>` : ''}
                    </div>
                    <div class="muted" style="font-size:12px;">${resolved.online ? 'Active now' : (activeConv.isGroup ? `${(activeConv.participantUids || []).length} members` : 'HELLO Messenger')}</div>
                  </div>
                </div>
                <div style="display:flex;gap:6px;">
                  <button class="icon-btn" id="startAudioCallBtn" title="Audio Call">${iconSvg('call', 18)}</button>
                  <button class="icon-btn" id="startVideoCallBtn" title="Video Call">${iconSvg('video', 18)}</button>
                </div>
              </div>

              <div class="chat-msgs" id="chatMsgsBox">
                ${state.messages.filter((m) => !m.isDeleted).map((m) => {
                  const mine = m.senderId === uid;
                  const src = mediaSrc(m.mediaUri);
                  const reactionVals = Object.values(m.reactions || {});
                  return `
                    <div class="msg-row ${mine ? 'mine' : 'theirs'}">
                      ${!mine ? `<div class="muted" style="font-size:11px;margin-bottom:2px;">${esc(resolveAuthor(m.senderId, m.senderName).name)}</div>` : ''}
                      <div class="bubble">
                        ${src && m.mediaType === 'image' ? `<img src="${esc(src)}" alt="Photo" style="max-width:260px;border-radius:12px;display:block;margin-bottom:${m.text ? '6px' : '0'};"/>` : ''}
                        ${src && m.mediaType === 'video' ? `<video controls src="${esc(src)}" style="max-width:260px;border-radius:12px;display:block;"></video>` : ''}
                        ${src && m.mediaType === 'audio' ? `<audio controls src="${esc(src)}" style="max-width:240px;"></audio>` : ''}
                        ${m.mediaType === 'file' ? `<a href="${esc(src || '#')}" download="${esc((m.fileName || 'file').replace(/[^a-zA-Z0-9._-]/g, '_'))}" rel="noopener noreferrer" style="display:inline-flex;align-items:center;gap:6px;text-decoration:underline;">${iconSvg('file', 16)}<span>${esc(m.fileName || 'Attachment')}</span></a>` : ''}
                        ${m.text ? `<div>${esc(m.text)}</div>` : ''}
                      </div>
                      <div class="msg-meta-row">
                        <span>${fmtTime(m.createdAt)}</span>
                        ${reactionVals.length ? `<span>${esc(reactionVals.join(' '))}</span>` : ''}
                        <button class="msg-mini-btn react-msg-btn" data-msg-id="${esc(m.id)}" title="React with heart">${iconSvg('heart', 12)}</button>
                        ${mine ? `<button class="msg-mini-btn unsend-msg-btn" data-msg-id="${esc(m.id)}" title="Unsend">${iconSvg('trash', 12)}</button>` : ''}
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>

              <div class="composer-dock">
                ${state.showAttachMenu ? `
                  <div class="attach-popover">
                    <label class="attach-menu-item" style="cursor:pointer;">
                      ${iconSvg('photo', 17)}<span>Photo / Video</span>
                      <input type="file" accept="image/*,video/*" id="chatMediaInput" style="display:none;" />
                    </label>
                    <label class="attach-menu-item" style="cursor:pointer;">
                      ${iconSvg('file', 17)}<span>Document / File</span>
                      <input type="file" id="chatFileInput" style="display:none;" />
                    </label>
                  </div>
                ` : ''}
                <div class="screenshot-pill">
                  <label class="cam-circle" title="Send camera photo or video">
                    ${iconSvg('camera', 19)}
                    <input type="file" accept="image/*,video/*" id="chatCameraInput" style="display:none;" />
                  </label>
                  <input id="chatTextInput" type="text" placeholder="${state.isRecordingVoice ? `Recording voice note (${state.recordingSec}s)...` : 'Message...'}" value="${esc(state.msgDraft)}" />
                  <button class="pill-icon-btn ${state.isRecordingVoice ? 'recording' : ''}" id="chatMicBtn" type="button" title="Record voice message">
                    ${iconSvg(state.isRecordingVoice ? 'stop' : 'mic', 19)}
                  </button>
                  <button class="pill-icon-btn" id="chatAttachToggleBtn" type="button" title="Attach photo, video or file">
                    ${iconSvg('attach', 19)}
                  </button>
                  ${hasDraft ? `
                    <button class="text-send-btn" id="chatSendBtn" type="button" title="Send">${iconSvg('send', 18)}</button>
                  ` : `
                    <button class="heart-send-btn" id="chatHeartBtn" type="button" title="Send heart">${iconSvg('heart', 20)}</button>
                  `}
                </div>
              </div>
            `;
          })() : `<div style="padding:36px;" class="muted">Select a conversation to start messaging.</div>`}
        </div>
      </div>
    `;
  }

  if (state.tab === 'reels') {
    const reels = state.reels.filter((r) => !q || (r.caption || '').toLowerCase().includes(q) || (r.authorName || '').toLowerCase().includes(q));
    return `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;">
          <div>
            <h3 style="margin:0;">HELLO Reels</h3>
            <div class="muted">Short videos from creators across HELLO</div>
          </div>
        </div>
        ${reels.length === 0 ? `<div class="muted">No Reels posted yet.</div>` : `
          <div class="grid-cards">
            ${reels.map((r) => {
              const author = resolveAuthor(r.authorId, r.authorName, r.authorAvatar);
              const vSrc = mediaSrc(r.videoUri);
              return `
                <div class="card" style="padding:12px;display:flex;flex-direction:column;gap:8px;background:var(--elevated);">
                  <div style="display:flex;align-items:center;gap:8px;">
                    ${avatarHtml(author.name, author.avatar, 32, author.online)}
                    <div style="min-width:0;">
                      <div style="font-weight:700;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(author.name)}</div>
                      <div class="muted" style="font-size:11px;">${esc(r.category || 'Reel')}</div>
                    </div>
                  </div>
                  ${vSrc ? `<video controls src="${esc(vSrc)}" style="width:100%;height:280px;object-fit:cover;border-radius:12px;background:#000;"></video>` : `<div style="height:200px;border-radius:12px;background:${/^#[0-9A-Fa-f]{6}$/.test(r.coverHex || '') ? r.coverHex : '#1E1B4B'};display:flex;align-items:center;justify-content:center;padding:14px;text-align:center;font-weight:700;">${esc(r.caption)}</div>`}
                  <div style="font-size:13px;line-height:1.4;">${esc(r.caption)}</div>
                  <div style="display:flex;gap:8px;">
                    <button class="action-pill like-reel-btn" data-reel-id="${esc(r.id)}" style="background:var(--card);">${iconSvg('like', 16)}<span>${r.likesCount || 0}</span></button>
                    <button class="action-pill msg-person-btn" data-uid="${esc(r.authorId)}" style="background:var(--card);">${iconSvg('chat', 16)}<span>Message</span></button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (state.tab === 'friends') {
    const friendSet = new Set(state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== uid));
    const followingSet = new Set(state.follows.map((f) => f.targetUid));
    const incomingReqs = state.friendRequests.filter((r) => r.toUid === uid && r.status === 'pending');
    const outgoingSet = new Set(state.friendRequests.filter((r) => r.fromUid === uid && r.status === 'pending').map((r) => r.toUid));
    const people = state.users.filter((u) => (u.userId || u.id) !== uid && (!q || (u.displayName || '').toLowerCase().includes(q) || (u.handle || '').toLowerCase().includes(q)));

    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        ${incomingReqs.length ? `
          <div class="card">
            <h3 style="margin:0 0 12px;">Friend Requests (${incomingReqs.length})</h3>
            <div class="grid-cards">
              ${incomingReqs.map((r) => {
                const sender = resolveAuthor(r.fromUid, r.fromName, r.fromAvatar);
                return `
                  <div class="card" style="padding:14px;background:var(--elevated);">
                    <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px;">
                      ${avatarHtml(sender.name, sender.avatar, 42, sender.online)}
                      <div><div style="font-weight:700;">${esc(sender.name)}</div><div class="muted">Sent a friend request</div></div>
                    </div>
                    <div style="display:flex;gap:8px;">
                      <button class="btn btn-primary accept-req-btn" data-req-id="${esc(r.id)}">${iconSvg('check', 16)}<span>Accept</span></button>
                      <button class="btn btn-secondary reject-req-btn" data-req-id="${esc(r.id)}">Decline</button>
                    </div>
                  </div>
                `;
              }).join('')}
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
              const isPending = outgoingSet.has(pid);
              return `
                <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:10px;background:var(--elevated);">
                  <div style="display:flex;align-items:center;gap:10px;cursor:pointer;" class="open-user-profile" data-uid="${esc(pid)}">
                    ${avatarHtml(p.displayName, p.photoUrl, 44, p.isOnline && p.showOnlineStatus !== false)}
                    <div style="min-width:0;">
                      <div style="font-weight:700;display:flex;align-items:center;gap:4px;">
                        <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(p.displayName || 'Member')}</span>
                        ${p.isVerifiedOwner ? `<span class="verified-badge">${iconSvg('verified', 14)}</span>` : ''}
                      </div>
                      <div class="muted">${esc(p.handle || '')}</div>
                    </div>
                  </div>
                  <div style="display:flex;gap:6px;flex-wrap:wrap;">
                    <button class="btn btn-primary msg-person-btn" data-uid="${esc(pid)}">${iconSvg('chat', 15)}<span>Message</span></button>
                    <button class="btn btn-secondary follow-btn" data-uid="${esc(pid)}" title="Follow">${iconSvg('follow', 15)}<span>${isFollowing ? 'Following' : 'Follow'}</span></button>
                    ${isFriend ? `<span class="badge">Friend</span>` : (isPending ? `<span class="badge">Requested</span>` : `<button class="btn btn-secondary add-friend-btn" data-uid="${esc(pid)}">Add Friend</button>`)}
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
            const isCreator = g.creatorId === uid;
            return `
              <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:8px;background:var(--elevated);">
                <div style="font-weight:800;font-size:15.5px;">${esc(g.name)}</div>
                <div class="muted">${esc(g.category || 'Community')} · ${(g.memberUids || []).length} members</div>
                <div style="font-size:13px;line-height:1.45;">${esc(g.description || '')}</div>
                <div style="margin-top:auto;padding-top:6px;">
                  ${isCreator ? `<span class="badge">Group Admin</span>` : `<button class="btn ${joined ? 'btn-secondary' : 'btn-primary'} join-group-btn" data-group-id="${esc(g.id)}">${joined ? 'Joined (Leave)' : 'Join Group'}</button>`}
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  if (state.tab === 'marketplace') {
    const cats = ['All', 'Electronics', 'Vehicles', 'Home', 'Fashion', 'Services', 'Other'];
    const items = state.marketplace.filter((m) => {
      const catOk = state.marketCat === 'All' || (m.category || '').toLowerCase() === state.marketCat.toLowerCase();
      const qOk = !q || (m.title || '').toLowerCase().includes(q) || (m.description || '').toLowerCase().includes(q);
      return catOk && qOk;
    });
    return `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:12px;">
          <div>
            <h3 style="margin:0;">HELLO Marketplace (${items.length})</h3>
            <div class="muted">Buy & sell locally — tap Message Seller to send product photo</div>
          </div>
          <button class="btn btn-primary" id="openCreateListingBtn">${iconSvg('plus', 16)}<span>Sell Item</span></button>
        </div>
        <div class="chip-row" style="margin-bottom:14px;">
          ${cats.map((c) => `<button class="filter-chip ${state.marketCat === c ? 'active' : ''}" data-market-cat="${c}">${c}</button>`).join('')}
        </div>
        ${items.length === 0 ? `<div class="muted">No Marketplace listings yet.</div>` : `
          <div class="grid-cards">
            ${items.map((item) => {
              const src = mediaSrc(item.imageUri);
              const isMine = item.sellerId === uid;
              return `
                <div class="card" style="padding:12px;display:flex;flex-direction:column;gap:6px;background:var(--elevated);">
                  ${src ? `<img class="market-img" src="${esc(src)}" alt="${esc(item.title)}" />` : `<div class="market-img" style="display:flex;align-items:center;justify-content:center;color:var(--muted);">No photo</div>`}
                  <div style="display:flex;justify-content:space-between;align-items:center;">
                    <span style="font-weight:800;font-size:16px;color:var(--primary);font-variant-numeric:tabular-nums;">${esc(item.currency || '$')}${Number(item.price || 0).toFixed(2)}</span>
                    <span class="badge">${esc(item.status || 'available')}</span>
                  </div>
                  <div style="font-weight:700;">${esc(item.title)}</div>
                  <div class="muted" style="font-size:12px;">${esc(item.condition || 'Available')} · ${esc(item.locationText || 'Local')}</div>
                  ${isMine ? `
                    <div style="display:flex;gap:6px;margin-top:6px;">
                      <button class="btn btn-secondary toggle-sold-btn" data-item-id="${esc(item.id)}" style="flex:1;">${item.status === 'sold' ? 'Mark Available' : 'Mark Sold'}</button>
                    </div>
                  ` : `
                    <button class="btn btn-primary market-msg-btn" data-item-id="${esc(item.id)}" style="margin-top:6px;">${iconSvg('chat', 15)}<span>Message Seller</span></button>
                  `}
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (state.tab === 'profile') {
    const viewUid = state.profileViewUid || uid;
    const isMe = viewUid === uid;
    const person = isMe ? (state.me || {}) : (state.usersById[viewUid] || {});
    const coverSrc = mediaSrc(person.coverUrl);
    const myPosts = state.posts.filter((p) => p.authorId === viewUid);
    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card" style="padding:14px;">
          <div class="profile-cover">
            ${coverSrc ? `<img src="${esc(coverSrc)}" alt="Cover" />` : ''}
          </div>
          <div class="profile-head-overlap">
            ${avatarHtml(person.displayName || state.user.displayName, person.photoUrl || (isMe ? state.user.photoURL : ''), 78, person.isOnline)}
            <div style="flex:1;padding-bottom:6px;">
              <h2 style="margin:0;display:flex;align-items:center;gap:6px;">
                <span>${esc(person.displayName || 'Hello Member')}</span>
                ${person.isVerifiedOwner ? `<span class="verified-badge">${iconSvg('verified', 18)}</span>` : ''}
              </h2>
              <div class="muted">${esc(person.handle || '')} · ${person.friendsCount || 0} friends · ${person.followersCount || 0} followers</div>
            </div>
          </div>
          ${person.bio ? `<div style="padding:12px 14px 4px;font-size:14px;">${esc(person.bio)}</div>` : ''}
          ${isMe ? `
            <div style="padding:12px 14px 4px;display:flex;flex-direction:column;gap:10px;border-top:1px solid var(--border);margin-top:12px;">
              <div style="font-weight:700;font-size:14px;">Edit Profile</div>
              <input class="field" id="profNameInput" placeholder="Display Name" value="${esc(person.displayName || '')}" />
              <textarea class="field" id="profBioInput" rows="2" placeholder="Bio">${esc(person.bio || '')}</textarea>
              <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                <label class="btn btn-secondary" style="cursor:pointer;">${iconSvg('photo', 16)}<span>Change Avatar</span><input type="file" accept="image/*" id="profAvatarInput" style="display:none;" /></label>
                <label class="btn btn-secondary" style="cursor:pointer;">${iconSvg('photo', 16)}<span>Change Cover</span><input type="file" accept="image/*" id="profCoverInput" style="display:none;" /></label>
                <button class="btn btn-primary" id="saveProfileBtn">Save Changes</button>
              </div>
            </div>
          ` : `
            <div style="padding:12px 14px 4px;display:flex;gap:8px;">
              <button class="btn btn-primary msg-person-btn" data-uid="${esc(viewUid)}">${iconSvg('chat', 16)}<span>Message</span></button>
              <button class="btn btn-secondary follow-btn" data-uid="${esc(viewUid)}">${state.follows.some((f) => f.targetUid === viewUid) ? 'Following' : 'Follow'}</button>
            </div>
          `}
        </div>
        ${myPosts.map((p) => renderPostCard(p)).join('')}
      </div>
    `;
  }

  // Home Feed
  let posts = state.posts.filter((p) => !q || (p.content || '').toLowerCase().includes(q) || (p.authorName || '').toLowerCase().includes(q));
  const friendSet = new Set(state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== uid));
  if (state.feedSort === 'top') {
    posts = [...posts].sort((a, b) => ((b.likesCount || 0) * 2 + (b.commentsCount || 0) + (b.sharesCount || 0)) - ((a.likesCount || 0) * 2 + (a.commentsCount || 0) + (a.sharesCount || 0)));
  } else if (state.feedSort === 'friends') {
    posts = posts.filter((p) => p.authorId === uid || friendSet.has(p.authorId));
  }

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      ${renderStoriesStrip()}

      <div class="card composer">
        <div style="display:flex;gap:10px;align-items:flex-start;margin-bottom:10px;">
          ${avatarHtml(state.me?.displayName || state.user.displayName, state.me?.photoUrl || state.user.photoURL, 42, true)}
          <textarea id="postDraftInput" rows="2" placeholder="What's on your mind, ${esc((state.me?.displayName || 'friend').split(' ')[0])}?">${esc(state.postDraft)}</textarea>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
            <label class="btn btn-secondary" style="cursor:pointer;">
              ${iconSvg('photo', 16)}
              <span>${state.postMediaFile ? esc(state.postMediaFile.name) : 'Photo / Video'}</span>
              <input type="file" accept="image/*,video/*" id="postPhotoInput" style="display:none;" />
            </label>
            <select class="select-field" id="postPrivacySelect" style="width:auto;padding:7px 12px;border-radius:999px;font-size:12.5px;">
              <option value="public" ${state.postPrivacy === 'public' ? 'selected' : ''}>Public</option>
              <option value="friends" ${state.postPrivacy === 'friends' ? 'selected' : ''}>Friends</option>
              <option value="only_me" ${state.postPrivacy === 'only_me' ? 'selected' : ''}>Only Me</option>
            </select>
          </div>
          <button class="btn btn-primary" id="publishPostBtn">Post to HELLO</button>
        </div>
      </div>

      <div class="chip-row">
        ${[['latest', 'Latest'], ['top', 'Top Posts'], ['friends', 'Friends Only']].map(([k, lbl]) => `
          <button class="filter-chip ${state.feedSort === k ? 'active' : ''}" data-feed-sort="${k}">${lbl}</button>
        `).join('')}
      </div>

      <div class="feed-list">
        ${posts.length === 0 ? `<div class="card muted">No posts match this feed filter yet.</div>` : posts.map((p) => renderPostCard(p)).join('')}
      </div>
    </div>
  `;
}

function renderPostCard(p) {
  const author = resolveAuthor(p.authorId, p.authorName, p.authorAvatar);
  const media = mediaSrc(p.mediaUri);
  const liked = Boolean(state.likedByPost[p.id]);
  const saved = state.savedPostIds.has(p.id);
  const commentsOpen = Boolean(state.openComments[p.id]);
  const comments = state.commentsByPost[p.id] || [];
  return `
    <article class="card">
      <div class="post-header">
        <div class="post-author-row open-user-profile" style="cursor:pointer;" data-uid="${esc(p.authorId)}">
          ${avatarHtml(author.name, author.avatar, 42, author.online)}
          <div>
            <div class="post-author">
              <span>${esc(author.name)}</span>
              ${author.verified ? `<span class="verified-badge">${iconSvg('verified', 15)}</span>` : ''}
            </div>
            <div class="post-meta">${fmtTime(p.createdAt)} · ${esc(p.privacy || 'public')}</div>
          </div>
        </div>
        <button class="icon-btn save-post-btn" data-post-id="${esc(p.id)}" title="Save post" style="width:34px;height:34px;color:${saved ? 'var(--violet-light)' : 'var(--muted)'};">${iconSvg('bookmark', 17)}</button>
      </div>
      ${p.content ? `<div class="post-body">${esc(p.content)}</div>` : ''}
      ${media && p.mediaType === 'image' ? `<img class="post-media" src="${esc(media)}" alt="Post media" />` : ''}
      ${media && p.mediaType === 'video' ? `<video class="post-media" controls src="${esc(media)}"></video>` : ''}
      <div class="post-stats">
        <span>${p.likesCount || 0} reactions</span>
        <span>${p.commentsCount || 0} comments · ${p.sharesCount || 0} shares</span>
      </div>
      <div class="post-actions">
        <button class="action-pill like-btn ${liked ? 'liked' : ''}" data-post-id="${esc(p.id)}">${iconSvg('like', 17)}<span>${liked ? 'Liked' : 'Like'}</span></button>
        <button class="action-pill comment-toggle-btn" data-post-id="${esc(p.id)}">${iconSvg('comment', 17)}<span>Comment</span></button>
        <button class="action-pill share-post-btn" data-post-id="${esc(p.id)}">${iconSvg('share', 17)}<span>Share</span></button>
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
}

function renderModals() {
  let html = '';
  if (state.activeStory) {
    const s = state.activeStory;
    const author = resolveAuthor(s.authorId, s.authorName, s.authorAvatar);
    const media = mediaSrc(s.mediaUri);
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card story-viewer-card">
          <div style="padding:14px;display:flex;justify-content:space-between;align-items:center;background:rgba(11,14,30,0.85);">
            <div style="display:flex;align-items:center;gap:10px;">
              ${avatarHtml(author.name, author.avatar, 36, false)}
              <div><div style="font-weight:700;">${esc(author.name)}</div><div class="muted" style="font-size:11px;">${fmtTime(s.createdAt)}</div></div>
            </div>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <div style="min-height:380px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:${/^#[0-9A-Fa-f]{6}$/.test(s.backgroundHex || '') ? s.backgroundHex : '#1E1B4B'};padding:18px;text-align:center;">
            ${media && s.mediaType === 'video' ? `<video controls autoplay src="${esc(media)}" style="max-width:100%;max-height:420px;border-radius:12px;"></video>` : ''}
            ${media && s.mediaType !== 'video' ? `<img src="${esc(media)}" alt="Story" style="max-width:100%;max-height:420px;border-radius:12px;" />` : ''}
            ${s.caption ? `<div style="margin-top:12px;font-size:16px;font-weight:700;">${esc(s.caption)}</div>` : ''}
          </div>
        </div>
      </div>
    `;
  } else if (state.showNotificationsModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Notifications</h3>
            <div style="display:flex;gap:8px;">
              <button class="btn btn-secondary" id="markAllReadBtn" style="padding:4px 12px;min-height:30px;font-size:12px;">Mark read</button>
              <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
            </div>
          </div>
          ${state.notifications.length === 0 ? `<div class="muted">No notifications yet.</div>` : state.notifications.slice(0, 25).map((n) => `
            <div style="padding:10px;border-radius:12px;background:${n.isRead ? 'var(--elevated)' : 'rgba(255,90,95,0.14)'};border:1px solid var(--border);">
              <div style="font-weight:700;font-size:13.5px;">${esc(n.title)}</div>
              <div style="font-size:13px;margin-top:2px;">${esc(n.body)}</div>
              <div class="muted" style="font-size:11px;margin-top:4px;">${fmtTime(n.createdAt)}</div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  } else if (state.showCreateStoryModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Create Story</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="storyCaptionInput" placeholder="Write a story caption..." />
          <label class="btn btn-secondary" style="cursor:pointer;">${iconSvg('photo', 16)}<span>Choose Photo / Video (Optional)</span><input type="file" accept="image/*,video/*" id="storyFileInput" style="display:none;" /></label>
          <button class="btn btn-primary" id="submitStoryBtn">Publish Story</button>
        </div>
      </div>
    `;
  } else if (state.showCreateListingModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">New Marketplace Listing</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="mktTitleInput" placeholder="Product title" />
          <input class="field" id="mktPriceInput" type="number" min="0" step="0.01" placeholder="Price (e.g. 25)" />
          <select class="select-field" id="mktCatInput">
            <option>Electronics</option><option>Vehicles</option><option>Home</option><option>Fashion</option><option>Services</option><option>Other</option>
          </select>
          <input class="field" id="mktLocInput" placeholder="Location (e.g. Dhaka)" />
          <textarea class="field" id="mktDescInput" rows="2" placeholder="Description"></textarea>
          <label class="btn btn-secondary" style="cursor:pointer;">${iconSvg('photo', 16)}<span>Product Photo</span><input type="file" accept="image/*" id="mktFileInput" style="display:none;" /></label>
          <button class="btn btn-primary" id="submitListingBtn">Publish Listing</button>
        </div>
      </div>
    `;
  } else if (state.showNewGroupChatModal) {
    const uid = state.user.uid;
    const peers = state.users.filter((u) => (u.userId || u.id) !== uid);
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">New Group Chat</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="grpChatTitleInput" placeholder="Group chat name" />
          <div style="max-height:220px;overflow-y:auto;display:flex;flex-direction:column;gap:6px;">
            ${peers.map((p) => `
              <label style="display:flex;align-items:center;gap:10px;padding:6px 8px;border-radius:10px;background:var(--elevated);cursor:pointer;">
                <input type="checkbox" class="grp-member-chk" value="${esc(p.userId || p.id)}" />
                ${avatarHtml(p.displayName, p.photoUrl, 30, false)}
                <span>${esc(p.displayName)}</span>
              </label>
            `).join('')}
          </div>
          <button class="btn btn-primary" id="createGrpChatBtn">Create Group Chat</button>
        </div>
      </div>
    `;
  }
  return html;
}

function renderShell() {
  const uid = state.user.uid;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
  const activePeers = state.users.filter((u) => (u.userId || u.id) !== uid && u.isOnline && u.showOnlineStatus !== false);
  const unreadChats = state.conversations.filter((c) => !(c.readByUids || []).includes(uid)).length;
  const unreadNotifs = state.notifications.filter((n) => !n.isRead).length;
  const incomingCall = state.activeCalls.find((c) => c.status === 'ringing' && c.callerId !== uid);

  return `
    <header class="topbar">
      <div class="brand-row">
        ${renderBrandTile(36)}
        <span class="brand-name">HELLO</span>
        <div class="search-pill">
          ${iconSvg('search', 17)}
          <input id="globalSearchInput" type="search" placeholder="Search HELLO..." value="${esc(state.search)}" />
        </div>
      </div>

      <nav class="nav-tabs">
        <button class="nav-tab ${state.tab === 'feed' ? 'active' : ''}" data-tab="feed">${iconSvg('home', 18)}<span>Home</span></button>
        <button class="nav-tab ${state.tab === 'reels' ? 'active' : ''}" data-tab="reels">${iconSvg('reels', 18)}<span>Reels</span></button>
        <button class="nav-tab ${state.tab === 'friends' ? 'active' : ''}" data-tab="friends">${iconSvg('friends', 18)}<span>Friends</span></button>
        <button class="nav-tab ${state.tab === 'groups' ? 'active' : ''}" data-tab="groups">${iconSvg('groups', 18)}<span>Groups</span></button>
        <button class="nav-tab ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger">${iconSvg('chat', 18)}<span>Chats</span>${unreadChats ? `<span class="tab-badge">${unreadChats}</span>` : ''}</button>
        <button class="nav-tab ${state.tab === 'marketplace' ? 'active' : ''}" data-tab="marketplace">${iconSvg('shop', 18)}<span>Shop</span></button>
      </nav>

      <div class="top-actions">
        <button class="icon-btn" id="openNotifsBtn" title="Notifications">${iconSvg('bell', 18)}${unreadNotifs ? `<span class="tab-badge">${unreadNotifs}</span>` : ''}</button>
        <button class="icon-btn" id="installLaptopBtnTop" title="Install Desktop App">${iconSvg('laptop', 18)}</button>
        <button class="icon-btn" id="themeToggleBtn" title="Toggle Theme">${iconSvg(state.theme === 'dark' ? 'sun' : 'moon', 18)}</button>
        <button class="icon-btn" id="signOutBtn" title="Sign Out">${iconSvg('logout', 18)}</button>
      </div>
    </header>

    ${incomingCall ? `
      <div style="max-width:1340px;margin:10px auto 0;padding:0 18px;">
        <div class="notice-box" style="display:flex;justify-content:space-between;align-items:center;gap:10px;border-color:var(--emerald);background:rgba(16,185,129,0.16);">
          <span><strong>Incoming ${esc(incomingCall.callType)} call</strong> from ${esc(incomingCall.callerName)} (answer on your HELLO Android app or chat below)</span>
          <button class="btn btn-secondary" id="openCallChatBtn" data-conv-id="${esc(incomingCall.conversationId)}">Open Chat</button>
        </div>
      </div>
    ` : ''}

    ${state.statusBanner ? `<div style="max-width:1340px;margin:10px auto 0;padding:0 18px;"><div class="notice-box" style="display:flex;justify-content:space-between;align-items:center;"><span>${esc(state.statusBanner)}</span><button class="msg-mini-btn" id="dismissBannerBtn">${iconSvg('close', 16)}</button></div></div>` : ''}

    <div class="app-grid">
      <aside class="sticky-col left-col">
        <div class="card open-my-profile" style="display:flex;align-items:center;gap:12px;cursor:pointer;">
          ${avatarHtml(me.displayName, me.photoUrl, 44, true)}
          <div style="min-width:0;">
            <div style="font-weight:800;display:flex;align-items:center;gap:4px;">
              <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(me.displayName)}</span>
              ${me.isVerifiedOwner ? `<span class="verified-badge">${iconSvg('verified', 14)}</span>` : ''}
            </div>
            <div class="muted">${esc(me.handle || '')}</div>
          </div>
        </div>

        <div class="card side-menu">
          ${[
            ['feed', 'home', 'Home Feed', 0],
            ['reels', 'reels', 'Reels', 0],
            ['messenger', 'chat', 'Messenger', unreadChats],
            ['friends', 'friends', 'Friends & Follow', 0],
            ['groups', 'groups', 'Groups', 0],
            ['marketplace', 'shop', 'Marketplace', 0],
            ['profile', 'user', 'Profile', 0]
          ].map(([tabKey, ic, label, badge]) => `
            <button class="side-item ${state.tab === tabKey ? 'active' : ''}" data-tab="${tabKey}">
              <span class="side-item-left">${iconSvg(ic, 18)}<span>${label}</span></span>
              ${badge ? `<span class="tab-badge">${badge}</span>` : ''}
            </button>
          `).join('')}
        </div>

        <div class="card" style="display:flex;flex-direction:column;gap:8px;">
          <div style="display:flex;align-items:center;gap:6px;font-weight:700;font-size:13.5px;color:var(--emerald);">
            ${iconSvg('shield', 16)}<span>Protected Apps</span>
          </div>
          <button class="btn btn-primary" id="installLaptopBtnSide">${iconSvg('laptop', 16)}<span>Install on Laptop</span></button>
          <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 16)}<span>Android APK (v1.8.3)</span></a>
        </div>
      </aside>

      <main>${renderCenterStage()}</main>

      <aside class="sticky-col right-col">
        <div class="card">
          <div style="font-weight:800;margin-bottom:10px;">Active Friends (${activePeers.length})</div>
          <div style="display:flex;flex-direction:column;gap:4px;">
            ${activePeers.length === 0 ? `<div class="muted">No friends active right now.</div>` : activePeers.slice(0, 15).map((u) => `
              <button class="side-item msg-person-btn" data-uid="${esc(u.userId || u.id)}">
                <span class="side-item-left">
                  ${avatarHtml(u.displayName, u.photoUrl, 34, true)}
                  <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(u.displayName || 'Member')}</span>
                </span>
              </button>
            `).join('')}
          </div>
        </div>
      </aside>
    </div>

    <nav class="mobile-bottom-nav">
      <button class="mobile-nav-btn ${state.tab === 'feed' ? 'active' : ''}" data-tab="feed">${iconSvg('home', 20)}<span>Home</span></button>
      <button class="mobile-nav-btn ${state.tab === 'reels' ? 'active' : ''}" data-tab="reels">${iconSvg('reels', 20)}<span>Reels</span></button>
      <button class="mobile-nav-btn ${state.tab === 'friends' ? 'active' : ''}" data-tab="friends">${iconSvg('friends', 20)}<span>Friends</span></button>
      <button class="mobile-nav-btn ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger">${iconSvg('chat', 20)}<span>Chats</span>${unreadChats ? `<span class="tab-badge" style="position:absolute;top:2px;right:6px;">${unreadChats}</span>` : ''}</button>
      <button class="mobile-nav-btn ${state.tab === 'marketplace' ? 'active' : ''}" data-tab="marketplace">${iconSvg('shop', 20)}<span>Shop</span></button>
    </nav>

    ${renderModals()}
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
  document.getElementById('copyDomainBtn')?.addEventListener('click', () => {
    if (state.unauthorizedDomain) {
      navigator.clipboard?.writeText(state.unauthorizedDomain);
      const b = document.getElementById('copyDomainBtn');
      if (b) b.textContent = `Copied "${state.unauthorizedDomain}"`;
    }
  });
  document.getElementById('installLaptopBtnTop')?.addEventListener('click', installLaptopApp);
  document.getElementById('installLaptopBtnHero')?.addEventListener('click', installLaptopApp);
  document.getElementById('installLaptopBtnSide')?.addEventListener('click', installLaptopApp);
  document.getElementById('signOutBtn')?.addEventListener('click', () => signOut(auth));
  document.getElementById('dismissBannerBtn')?.addEventListener('click', () => { state.statusBanner = ''; render(); });

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
      if (state.tab === 'profile') state.profileViewUid = null;
      state.mobileShowChat = false;
      state.showAttachMenu = false;
      render();
    });
  });

  document.querySelectorAll('.open-my-profile').forEach((el) => {
    el.addEventListener('click', () => { state.profileViewUid = null; state.tab = 'profile'; render(); });
  });
  document.querySelectorAll('.open-user-profile').forEach((el) => {
    el.addEventListener('click', () => { state.profileViewUid = el.getAttribute('data-uid'); state.tab = 'profile'; render(); });
  });

  document.querySelectorAll('[data-feed-sort]').forEach((btn) => {
    btn.addEventListener('click', () => { state.feedSort = btn.getAttribute('data-feed-sort'); render(); });
  });

  document.getElementById('postDraftInput')?.addEventListener('input', (e) => { state.postDraft = e.target.value; });
  document.getElementById('postPrivacySelect')?.addEventListener('change', (e) => { state.postPrivacy = e.target.value; });
  document.getElementById('postPhotoInput')?.addEventListener('change', (e) => { state.postMediaFile = e.target.files?.[0] || null; render(); });
  document.getElementById('publishPostBtn')?.addEventListener('click', publishPost);

  document.querySelectorAll('.like-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const post = state.posts.find((p) => p.id === btn.getAttribute('data-post-id'));
      if (post) toggleLike(post);
    });
  });
  document.querySelectorAll('.save-post-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const post = state.posts.find((p) => p.id === btn.getAttribute('data-post-id'));
      if (post) toggleSavePost(post);
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
    btn.addEventListener('click', async () => {
      const pid = btn.getAttribute('data-post-id');
      navigator.clipboard?.writeText(`${window.location.origin}${window.location.pathname}#post-${pid}`);
      await updateDoc(doc(db, 'posts', pid), { sharesCount: increment(1), updatedAt: serverTimestamp() }).catch(() => {});
      state.statusBanner = 'Post link copied to clipboard.';
      render();
    });
  });

  // Stories
  document.getElementById('openCreateStoryBtn')?.addEventListener('click', () => { state.showCreateStoryModal = true; render(); });
  document.getElementById('createStoryCardBtn')?.addEventListener('click', () => { state.showCreateStoryModal = true; render(); });
  document.querySelectorAll('.view-story-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const s = state.stories.find((x) => x.id === btn.getAttribute('data-story-id'));
      if (s) { state.activeStory = s; render(); }
    });
  });
  document.getElementById('submitStoryBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const cap = document.getElementById('storyCaptionInput')?.value?.trim() || '';
    const file = document.getElementById('storyFileInput')?.files?.[0];
    if (!cap && !file) return;
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    const uri = file ? await uploadMediaBlob(file) : '';
    const mType = file ? (file.type?.startsWith('video/') ? 'video' : 'image') : 'text';
    await addDoc(collection(db, 'stories'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      mediaUri: uri,
      mediaType: mType,
      caption: cap.slice(0, 500),
      backgroundHex: '#1E1B4B',
      privacy: 'public',
      allowedViewerUids: [],
      viewersCount: 0,
      isSavedHighlight: false,
      autoDelete24h: true,
      createdAt: serverTimestamp()
    });
    state.showCreateStoryModal = false;
    state.statusBanner = 'Story published.';
    render();
  });

  // Reels
  document.querySelectorAll('.like-reel-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const rid = btn.getAttribute('data-reel-id');
      if (rid) await updateDoc(doc(db, 'reels', rid), { likesCount: increment(1) }).catch(() => {});
    });
  });

  // Messenger
  document.querySelectorAll('[data-inbox-filter]').forEach((btn) => {
    btn.addEventListener('click', () => { state.inboxFilter = btn.getAttribute('data-inbox-filter'); render(); });
  });
  document.querySelectorAll('[data-conv-id]').forEach((btn) => {
    btn.addEventListener('click', () => selectConversation(btn.getAttribute('data-conv-id'), true));
  });
  document.getElementById('mobileBackChat')?.addEventListener('click', () => { state.mobileShowChat = false; render(); });
  document.getElementById('openCallChatBtn')?.addEventListener('click', (e) => {
    state.tab = 'messenger';
    selectConversation(e.currentTarget.getAttribute('data-conv-id'), true);
  });

  const activeConv = state.conversations.find((c) => c.id === state.activeConvId);
  document.getElementById('startAudioCallBtn')?.addEventListener('click', () => startCallSignal(activeConv, 'audio'));
  document.getElementById('startVideoCallBtn')?.addEventListener('click', () => startCallSignal(activeConv, 'video'));

  const chatInput = document.getElementById('chatTextInput');
  chatInput?.addEventListener('input', (e) => {
    const hadText = state.msgDraft.trim().length > 0;
    state.msgDraft = e.target.value;
    const hasTextNow = state.msgDraft.trim().length > 0;
    if (hadText !== hasTextNow) {
      render();
      const el = document.getElementById('chatTextInput');
      if (el) { el.focus(); el.setSelectionRange(el.value.length, el.value.length); }
    }
  });
  chatInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && state.msgDraft.trim() && activeConv) {
      const t = state.msgDraft.trim();
      state.msgDraft = '';
      sendChatMessage(activeConv.id, activeConv.participantUids || [], t);
    }
  });
  document.getElementById('chatSendBtn')?.addEventListener('click', () => {
    const t = state.msgDraft.trim();
    if (activeConv && t) {
      state.msgDraft = '';
      sendChatMessage(activeConv.id, activeConv.participantUids || [], t);
    }
  });
  document.getElementById('chatHeartBtn')?.addEventListener('click', () => {
    if (activeConv) sendChatMessage(activeConv.id, activeConv.participantUids || [], '❤️');
  });
  document.getElementById('chatMicBtn')?.addEventListener('click', () => toggleVoiceRecord(activeConv));
  document.getElementById('chatAttachToggleBtn')?.addEventListener('click', () => { state.showAttachMenu = !state.showAttachMenu; render(); });

  const handleChatMediaUpload = async (file) => {
    if (!file || !activeConv) return;
    state.showAttachMenu = false;
    const uri = await uploadMediaBlob(file);
    const mType = file.type?.startsWith('video/') ? 'video' : file.type?.startsWith('image/') ? 'image' : 'file';
    await sendChatMessage(activeConv.id, activeConv.participantUids || [], '', uri, mType, file.name);
  };
  document.getElementById('chatCameraInput')?.addEventListener('change', (e) => handleChatMediaUpload(e.target.files?.[0]));
  document.getElementById('chatMediaInput')?.addEventListener('change', (e) => handleChatMediaUpload(e.target.files?.[0]));
  document.getElementById('chatFileInput')?.addEventListener('change', async (e) => {
    const file = e.target.files?.[0];
    if (!file || !activeConv) return;
    state.showAttachMenu = false;
    const uri = await uploadMediaBlob(file);
    await sendChatMessage(activeConv.id, activeConv.participantUids || [], '', uri, 'file', file.name);
  });

  document.querySelectorAll('.react-msg-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!activeConv || !state.user) return;
      const mid = btn.getAttribute('data-msg-id');
      await updateDoc(doc(db, 'conversations', activeConv.id, 'messages', mid), {
        [`reactions.${state.user.uid}`]: '❤️'
      }).catch(() => {});
    });
  });
  document.querySelectorAll('.unsend-msg-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!activeConv) return;
      const mid = btn.getAttribute('data-msg-id');
      await updateDoc(doc(db, 'conversations', activeConv.id, 'messages', mid), {
        isDeleted: true,
        text: ''
      }).catch(() => {});
    });
  });

  document.getElementById('openNewGroupChatBtn')?.addEventListener('click', () => { state.showNewGroupChatModal = true; render(); });
  document.getElementById('createGrpChatBtn')?.addEventListener('click', async () => {
    const title = document.getElementById('grpChatTitleInput')?.value?.trim();
    const picked = [...document.querySelectorAll('.grp-member-chk:checked')].map((c) => c.value);
    if (!title || picked.length === 0) return;
    const uid = state.user.uid;
    const participants = Array.from(new Set([uid, ...picked]));
    const names = { [uid]: state.me?.displayName || state.user.displayName || 'Member' };
    picked.forEach((id) => { names[id] = state.usersById[id]?.displayName || 'Member'; });
    const ref = await addDoc(collection(db, 'conversations'), {
      isGroup: true,
      title: title.slice(0, 100),
      avatarUrl: '',
      participantUids: participants,
      participantNames: names,
      lastMessage: 'Group created',
      lastSenderId: uid,
      typingUids: [],
      readByUids: [uid],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    state.showNewGroupChatModal = false;
    selectConversation(ref.id, true);
  });

  // Friends & Follow
  document.querySelectorAll('.msg-person-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const peer = state.usersById[btn.getAttribute('data-uid')] || { userId: btn.getAttribute('data-uid'), displayName: 'Member' };
      openOrCreateDm(peer);
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
      await setDoc(doc(db, 'friend_requests', `${uid}_${toUid}`), {
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
      state.statusBanner = `Friend request sent to ${peer.displayName}.`;
      render();
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

  // Groups & Marketplace
  document.querySelectorAll('.join-group-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const g = state.groups.find((x) => x.id === btn.getAttribute('data-group-id'));
      if (g) toggleJoinGroup(g);
    });
  });
  document.querySelectorAll('[data-market-cat]').forEach((btn) => {
    btn.addEventListener('click', () => { state.marketCat = btn.getAttribute('data-market-cat'); render(); });
  });
  document.getElementById('openCreateListingBtn')?.addEventListener('click', () => { state.showCreateListingModal = true; render(); });
  document.getElementById('submitListingBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const title = document.getElementById('mktTitleInput')?.value?.trim();
    const price = parseFloat(document.getElementById('mktPriceInput')?.value || '0');
    const category = document.getElementById('mktCatInput')?.value || 'Other';
    const locationText = document.getElementById('mktLocInput')?.value?.trim() || 'Local';
    const description = document.getElementById('mktDescInput')?.value?.trim() || '';
    const file = document.getElementById('mktFileInput')?.files?.[0];
    if (!title || Number.isNaN(price) || price < 0) return;
    const imageUri = file ? await uploadMediaBlob(file) : '';
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    await addDoc(collection(db, 'marketplace_listings'), {
      sellerId: state.user.uid,
      sellerName: (me.displayName || 'Hello Member').slice(0, 80),
      sellerAvatar: me.photoUrl || '',
      title: title.slice(0, 120),
      description: description.slice(0, 1000),
      price,
      currency: '$',
      category,
      condition: 'Good',
      locationText: locationText.slice(0, 100),
      imageUri,
      status: 'available',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    state.showCreateListingModal = false;
    state.statusBanner = 'Listing published to HELLO Marketplace.';
    render();
  });
  document.querySelectorAll('.toggle-sold-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const item = state.marketplace.find((m) => m.id === btn.getAttribute('data-item-id'));
      if (!item) return;
      await updateDoc(doc(db, 'marketplace_listings', item.id), {
        status: item.status === 'sold' ? 'available' : 'sold',
        updatedAt: serverTimestamp()
      });
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

  // Profile updates
  document.getElementById('profAvatarInput')?.addEventListener('change', async (e) => {
    const file = e.target.files?.[0];
    if (!file || !state.user) return;
    const uri = await uploadMediaBlob(file);
    await updateDoc(doc(db, 'users_public', state.user.uid), { photoUrl: uri, updatedAt: serverTimestamp() });
    state.statusBanner = 'Profile photo updated.';
    render();
  });
  document.getElementById('profCoverInput')?.addEventListener('change', async (e) => {
    const file = e.target.files?.[0];
    if (!file || !state.user) return;
    const uri = await uploadMediaBlob(file);
    await updateDoc(doc(db, 'users_public', state.user.uid), { coverUrl: uri, updatedAt: serverTimestamp() });
    state.statusBanner = 'Cover photo updated.';
    render();
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
    state.statusBanner = 'Profile saved across desktop & mobile.';
    render();
  });

  // Notifications & Modals
  document.getElementById('openNotifsBtn')?.addEventListener('click', () => { state.showNotificationsModal = true; render(); });
  document.getElementById('markAllReadBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const batch = writeBatch(db);
    state.notifications.filter((n) => !n.isRead).forEach((n) => {
      batch.update(doc(db, 'users', state.user.uid, 'notifications', n.id), { isRead: true });
    });
    await batch.commit();
  });
  document.querySelectorAll('.close-modals-btn').forEach((b) => {
    b.addEventListener('click', () => {
      state.activeStory = null;
      state.showNotificationsModal = false;
      state.showCreateStoryModal = false;
      state.showCreateListingModal = false;
      state.showNewGroupChatModal = false;
      render();
    });
  });
  document.getElementById('modalBackdrop')?.addEventListener('click', (e) => {
    if (e.target.id === 'modalBackdrop') {
      state.activeStory = null;
      state.showNotificationsModal = false;
      state.showCreateStoryModal = false;
      state.showCreateListingModal = false;
      state.showNewGroupChatModal = false;
      render();
    }
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
