import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.1.0/firebase-app.js';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  reauthenticateWithPopup,
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
import {
  REACTIONS,
  STORY_REACTIONS,
  REEL_CATEGORIES,
  initFeatures,
  isVerifiedOwnerUser,
  isAdminUser,
  stopFeatureSubListeners,
  setTypingState,
  openStoryWithTracking,
  renderSosTab,
  renderSecretLoveFull,
  renderSettingsTab,
  renderOwnerProfileTab,
  renderOwnerDashboardTab,
  renderFeatureModals,
  bindFeatureEvents
} from './features.js';
import {
  createVoiceGestureController,
  isVoiceGestureStoredEnabled,
  setVoiceGestureStoredEnabled
} from './voiceGesture.js';

const WEBSITE_URL = 'https://aritmahmud7124.github.io/Hello-1/';
const APK_URL = 'https://github.com/AritMahmud7124/Hello-1/releases/download/v1.8.7/hello-social-v1.8.7.apk';
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
  callEnd: '<path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08a.956.956 0 0 1-.29-.7c0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.11-.7-.28a11.27 11.27 0 0 0-2.67-1.85.996.996 0 0 1-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z"/>',
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
  logout: '<path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>',
  flag: '<path d="M14.4 6 14 4H5v17h2v-7h5.6l.4 2h7V6h-5.6z"/>',
  event: '<path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z"/>',
  reply: '<path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>'
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
  blockedUids: new Set(),
  postsPublic: [],
  postsMine: [],
  postsShared: [],
  posts: [],
  storiesPublic: [],
  storiesMine: [],
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
  replyToMsg: null,
  activeCalls: [],
  friendships: [],
  friendRequests: [],
  follows: [],
  groupsPublic: [],
  groupsMine: [],
  groups: [],
  activeGroupId: null,
  pages: [],
  events: [],
  marketplace: [],
  marketCat: 'All',
  notifications: [],
  notifFilter: 'all',
  secretInbox: [],
  secretSent: [],
  secretTab: 'inbox',
  postDraft: '',
  postPrivacy: 'public',
  postMediaFile: null,
  postMediaPreview: '',
  storyDraftCaption: '',
  storyBgHex: '#1E1B4B',
  storyMediaFile: null,
  storyMediaPreview: '',
  reelDraftCaption: '',
  reelDraftCategory: 'Trending',
  reelCoverHex: '#1E1B4B',
  reelMediaFile: null,
  reelMediaPreview: '',
  mktTitle: '',
  mktPrice: '',
  mktCat: 'Electronics',
  mktLoc: '',
  mktDesc: '',
  mktMediaFile: null,
  mktMediaPreview: '',
  grpName: '',
  grpCat: 'Community',
  grpPrivacy: 'public',
  grpDesc: '',
  pageName: '',
  pageCat: 'Creator',
  pageBio: '',
  evtTitle: '',
  evtLocation: '',
  evtDate: '',
  evtTime: '',
  evtDesc: '',
  msgDraft: '',
  showAttachMenu: false,
  isRecordingVoice: false,
  recordingSec: 0,
  mobileShowChat: false,
  activeStory: null,
  lightboxUri: '',
  profileViewUid: null,
  showCreateStoryModal: false,
  showCreateReelModal: false,
  showCreateListingModal: false,
  showCreateGroupModal: false,
  showCreatePageModal: false,
  showCreateEventModal: false,
  showNewGroupChatModal: false,
  showGetAppModal: false,
  isUploading: false,
  uploadLabel: '',
  statusBanner: '',
  statusIsError: false,
  privateUser: null,
  blockedNamesById: {},
  globalSettings: null,
  ownerConfig: null,
  ownerSubTab: 'overview',
  lastReauthAt: 0,
  reports: [],
  admins: [],
  ownerUsers: [],
  ownerSignals: [],
  ownerMatches: [],
  sosAlertsByGroup: {},
  secretCrushes: [],
  mutualMatches: [],
  devotionTrials: [],
  secretSendRecipientUid: '',
  questTierDays: 7,
  devotionReflectionDraft: '',
  secretMoodEmoji: '❤️',
  secretNoteDraft: '',
  privateChatMatchId: null,
  privateMatchMsgs: [],
  privateMatchDraft: '',
  maskModalMatchId: null,
  reelFilter: 'All',
  reelCommentsById: {},
  openReelCommentsId: null,
  storyFilter: 'all',
  storyPrivacy: 'public',
  storyReactionsById: {},
  reactionsByPost: {},
  myReactionByPost: {},
  reactionPickerPostId: null,
  disappearing24h: false,
  incognitoHideStatus: localStorage.getItem('hello_incog_status') === '1',
  incognitoPrivateViewing: localStorage.getItem('hello_incog_view') === '1',
  recentSearches: (() => { try { return JSON.parse(localStorage.getItem('hello_recent_searches') || '[]'); } catch (_) { return []; } })(),
  searchScope: 'all',
  profileTab: 'posts',
  editPostTarget: null,
  reportTarget: null,
  shareTarget: null,
  manageGroupModal: null,
  sosModalGroup: null,
  grpSosEnabled: true,
  activeMsgEmojiId: null,
  reelAutoNext: true,
  reelActiveIndex: 0,
  marketStatus: 'All',
  marketSort: 'Newest',
  mktCurrency: '$',
  mktCondition: 'Like New',
  detailListingId: null,
  editListingTarget: null,
  activePageId: null,
  pageCatFilter: 'All',
  editPageTarget: null,
  managePageModal: null,
  pageAvatarFile: null,
  pageAvatarPreview: '',
  pageCoverFile: null,
  pageCoverPreview: '',
  showEditProfileModal: false,
  vgEnabled: isVoiceGestureStoredEnabled()
};

if (state.theme === 'light') document.documentElement.classList.add('light');

if ('serviceWorker' in navigator) {
  let swReloaded = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!swReloaded) {
      swReloaded = true;
      window.location.reload();
    }
  });
  navigator.serviceWorker.register('./sw.js', { updateViaCache: 'none' }).then((reg) => {
    reg.update().catch(() => {});
  }).catch(() => {});
}

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  state.deferredPrompt = e;
  render();
});

window.addEventListener('appinstalled', () => {
  state.deferredPrompt = null;
  state.showGetAppModal = false;
  showBanner('HELLO Desktop / Laptop App installed! Launch it anytime from your Start Menu or Applications.');
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
    if (/^data:image\/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=\s]+$/i.test(trimmed)) return trimmed;
    if (/^data:video\/(mp4|webm|quicktime);base64,[A-Za-z0-9+/=\s]+$/i.test(trimmed)) return trimmed;
    if (/^data:audio\/(webm|mp4|mpeg|ogg|wav|x-m4a);base64,[A-Za-z0-9+/=\s]+$/i.test(trimmed)) return trimmed;
  }
  return '';
}

function base64ToBlobUrl(b64, mimeType) {
  try {
    const safeMime = (mimeType && !/html|svg|javascript|script/i.test(mimeType)) ? mimeType : 'application/octet-stream';
    let clean = String(b64 || '').replace(/\s+/g, '').replace(/-/g, '+').replace(/_/g, '/');
    while (clean.length % 4 !== 0) clean += '=';
    const bin = atob(clean);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return URL.createObjectURL(new Blob([bytes], { type: safeMime }));
  } catch (_) {
    return '';
  }
}

function hydrateMediaElements(uri, objectUrl) {
  if (!objectUrl) return;
  document.querySelectorAll('[data-media-uri]').forEach((el) => {
    if (el.getAttribute('data-media-uri') !== uri) return;
    if (el.tagName === 'IMG' || el.tagName === 'VIDEO' || el.tagName === 'AUDIO') {
      if (el.getAttribute('src') !== objectUrl) {
        el.setAttribute('src', objectUrl);
        el.style.display = '';
      }
      const fallback = el.nextElementSibling;
      if (fallback && fallback.hasAttribute('data-media-fallback')) {
        fallback.style.display = 'none';
      }
    } else if (el.tagName === 'A') {
      el.setAttribute('href', objectUrl);
    }
  });
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
    if (!b64) return '';
    const mime = blobMeta.mimeType || 'image/jpeg';
    const objectUrl = base64ToBlobUrl(b64, mime);
    if (objectUrl) {
      mediaCache.set(uri, objectUrl);
      hydrateMediaElements(uri, objectUrl);
    }
    return objectUrl;
  } catch (err) {
    console.warn('Media resolve failed:', uri, err);
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

function showBanner(msg, isError = false) {
  state.statusBanner = msg;
  state.statusIsError = isError;
  render();
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
  const isCloud = typeof photoUrl === 'string' && photoUrl.startsWith('firestore://media/');
  const initials = (name || 'H').trim().slice(0, 1).toUpperCase();
  const dot = isOnline ? '<span class="online-dot"></span>' : '';
  if (src || isCloud) {
    return `
      <span class="avatar-wrap" style="width:${size}px;height:${size}px">
        <img class="avatar" referrerpolicy="no-referrer" data-media-uri="${esc(photoUrl)}" src="${esc(src)}" alt="${esc(name)}" style="width:${size}px;height:${size}px;${src ? '' : 'display:none;'}" />
        <span class="avatar" data-media-fallback="1" style="width:${size}px;height:${size}px;font-size:${Math.round(size * 0.42)}px;${src ? 'display:none;' : ''}">${esc(initials)}</span>
        ${dot}
      </span>
    `;
  }
  return `<span class="avatar-wrap" style="width:${size}px;height:${size}px"><span class="avatar" style="width:${size}px;height:${size}px;font-size:${Math.round(size * 0.42)}px">${esc(initials)}</span>${dot}</span>`;
}

function mediaImgHtml(uri, cls, alt = 'Photo', attrs = '') {
  if (!uri) return '';
  const src = mediaSrc(uri);
  const isCloud = uri.startsWith('firestore://media/');
  if (!src && !isCloud) return '';
  return `
    <img class="${cls}" referrerpolicy="no-referrer" data-media-uri="${esc(uri)}" src="${esc(src)}" alt="${esc(alt)}" style="${src ? '' : 'display:none;'}" ${attrs} />
    <div class="media-loading-box" data-media-fallback="1" style="${src ? 'display:none;' : ''}">Loading photo...</div>
  `;
}

function mediaVideoHtml(uri, cls, extraAttrs = 'controls playsinline') {
  if (!uri) return '';
  const src = mediaSrc(uri);
  const isCloud = uri.startsWith('firestore://media/');
  if (!src && !isCloud) return '';
  return `
    <video class="${cls}" ${extraAttrs} data-media-uri="${esc(uri)}" src="${esc(src)}" style="${src ? '' : 'display:none;'}"></video>
    <div class="media-loading-box" data-media-fallback="1" style="${src ? 'display:none;' : ''}">Loading video...</div>
  `;
}

function setPreviewFile(targetKey, previewKey, file) {
  if (state[previewKey]) {
    try { URL.revokeObjectURL(state[previewKey]); } catch (_) {}
  }
  state[targetKey] = file || null;
  state[previewKey] = file ? URL.createObjectURL(file) : '';
  render();
}

async function compressImageFile(file) {
  if (!file || !file.type?.startsWith('image/') || file.type === 'image/gif') return file;
  if (file.size <= 950 * 1024) return file;
  try {
    const imgUrl = URL.createObjectURL(file);
    const img = await new Promise((resolve, reject) => {
      const i = new Image();
      i.onload = () => resolve(i);
      i.onerror = reject;
      i.src = imgUrl;
    });
    const maxDim = 1920;
    let w = img.naturalWidth || img.width;
    let h = img.naturalHeight || img.height;
    if (w > maxDim || h > maxDim) {
      const ratio = maxDim / Math.max(w, h);
      w = Math.max(1, Math.round(w * ratio));
      h = Math.max(1, Math.round(h * ratio));
    }
    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0, w, h);
    URL.revokeObjectURL(imgUrl);

    for (const q of [0.86, 0.74, 0.62, 0.50]) {
      const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', q));
      if (blob && (blob.size <= 1250000 || q === 0.50)) {
        const baseName = (file.name || 'photo').replace(/\.[^.]+$/, '');
        return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg' });
      }
    }
  } catch (err) {
    console.warn('Image compression fallback to raw:', err);
  }
  return file;
}

function bytesToBase64(bytes) {
  let binary = '';
  const step = 4096;
  for (let i = 0; i < bytes.length; i += step) {
    const slice = bytes.subarray(i, i + step);
    let part = '';
    for (let j = 0; j < slice.length; j++) part += String.fromCharCode(slice[j]);
    binary += part;
  }
  return btoa(binary);
}

async function uploadMediaBlob(rawFile, forcedMime = '', label = 'Uploading media...') {
  if (!state.user) throw new Error('Please sign in first.');
  state.isUploading = true;
  state.uploadLabel = label;
  render();
  try {
    const file = await compressImageFile(rawFile);
    const buf = await file.arrayBuffer();
    const bytes = new Uint8Array(buf);
    if (bytes.length === 0) throw new Error('Selected file is empty.');
    if (bytes.length > 8 * 1024 * 1024) {
      throw new Error('File must be under 8 MB (try a shorter video or smaller photo).');
    }
    const b64 = bytesToBase64(bytes);
    const chunkSize = 450000;
    const chunks = [];
    for (let i = 0; i < b64.length; i += chunkSize) chunks.push(b64.slice(i, i + chunkSize));
    if (chunks.length > 30) throw new Error('Encoded file exceeds chunk limit.');

    const mediaId = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
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

    // Commit in batches of max 6 chunks (matches FirestoreMediaStore.kt MAX_CHUNKS_PER_BATCH)
    const MAX_CHUNKS_PER_BATCH = 6;
    for (let start = 0; start < chunks.length; start += MAX_CHUNKS_PER_BATCH) {
      const end = Math.min(start + MAX_CHUNKS_PER_BATCH, chunks.length);
      const batch = writeBatch(db);
      for (let idx = start; idx < end; idx++) {
        batch.set(doc(db, 'media_blobs', mediaId, 'chunks', `chunk_${String(idx).padStart(3, '0')}`), {
          ownerId: state.user.uid,
          index: idx,
          data: chunks[idx],
          createdAt: serverTimestamp()
        });
      }
      await batch.commit();
    }

    const uri = `firestore://media/${mediaId}`;
    mediaCache.set(uri, URL.createObjectURL(new Blob([bytes], { type: safeMime })));
    return uri;
  } finally {
    state.isUploading = false;
    state.uploadLabel = '';
    render();
  }
}

function mergePosts() {
  const map = new Map();
  [...state.postsPublic, ...state.postsMine, ...state.postsShared].forEach((p) => {
    if (!state.blockedUids.has(p.authorId)) map.set(p.id, p);
  });
  state.posts = [...map.values()].sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

function mergeStories() {
  const cutoff = Date.now() - 24 * 60 * 60 * 1000;
  const map = new Map();
  [...state.storiesPublic, ...state.storiesMine].forEach((s) => {
    const ms = s.createdAt?.seconds ? s.createdAt.seconds * 1000 : Date.now();
    if (!state.blockedUids.has(s.authorId) && (s.isSavedHighlight || ms >= cutoff)) {
      map.set(s.id, s);
    }
  });
  state.stories = [...map.values()].sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
}

function mergeGroups() {
  const map = new Map();
  [...state.groupsPublic, ...state.groupsMine].forEach((g) => map.set(g.id, g));
  state.groups = [...map.values()].sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
}

// Web Audio subtle ringtone for incoming call
let ringTimer = null;
function updateRingtone(ringing) {
  if (ringing && !ringTimer) {
    const playTone = () => {
      try {
        const Ctx = window.AudioContext || window.webkitAudioContext;
        if (!Ctx) return;
        const ctx = new Ctx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(523.25, ctx.currentTime);
        osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.16);
        gain.gain.setValueAtTime(0.08, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.52);
        setTimeout(() => ctx.close().catch(() => {}), 700);
      } catch (_) {}
    };
    playTone();
    ringTimer = setInterval(playTone, 2400);
  } else if (!ringing && ringTimer) {
    clearInterval(ringTimer);
    ringTimer = null;
  }
}

function isStandaloneApp() {
  return Boolean(window.matchMedia?.('(display-mode: standalone)')?.matches || window.navigator?.standalone === true);
}

async function copyTextSafe(text) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (_) {}
  try {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.setAttribute('readonly', '');
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(ta);
    return ok;
  } catch (_) {
    return false;
  }
}

function downloadLaptopShortcutFile() {
  const html = `<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta http-equiv="refresh" content="0; url=${WEBSITE_URL}"><title>HELLO App Launcher</title></head><body style="font-family:sans-serif;background:#0B0E1E;color:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;"><p>Opening HELLO... <a href="${WEBSITE_URL}" style="color:#FF5A5F;">Click here if not redirected</a></p><script>window.location.replace("${WEBSITE_URL}");</script></body></html>`;
  const blob = new Blob([html], { type: 'text/html' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'HELLO-Laptop-Shortcut.html';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1500);
  showBanner('Downloaded HELLO-Laptop-Shortcut.html — double-click it on your laptop to open HELLO anytime!');
}

function openGetAppModal() {
  state.showGetAppModal = true;
  render();
}

async function triggerNativeDesktopInstall() {
  if (state.deferredPrompt) {
    state.deferredPrompt.prompt();
    const choice = await state.deferredPrompt.userChoice;
    state.deferredPrompt = null;
    if (choice?.outcome === 'accepted') {
      state.showGetAppModal = false;
      showBanner('HELLO Desktop / Laptop App is installing!');
    } else {
      render();
    }
  } else {
    state.showGetAppModal = true;
    render();
  }
}

async function installLaptopApp() {
  if (state.deferredPrompt) {
    await triggerNativeDesktopInstall();
  } else {
    openGetAppModal();
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
      handle: cleanHandle.length >= 2 ? cleanHandle : '@hello_user',
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
    }).catch(() => {});
  }
}

let unsubs = [];
let sosUnsubs = {};
let adminUnsubs = [];

function clearSosSubs() {
  Object.values(sosUnsubs).forEach((arr) => arr.forEach((u) => { try { u(); } catch (_) {} }));
  sosUnsubs = {};
}

function clearAdminSubs() {
  adminUnsubs.forEach((u) => { try { u(); } catch (_) {} });
  adminUnsubs = [];
}

function clearSubs() {
  unsubs.forEach((u) => { try { u(); } catch (_) {} });
  unsubs = [];
  clearSosSubs();
  clearAdminSubs();
  stopFeatureSubListeners();
  updateRingtone(false);
}

function logSubErr(label) {
  return (err) => console.warn(`Listener [${label}]:`, err?.code || err?.message);
}

function refreshGovernanceListeners(uid) {
  clearAdminSubs();
  const isOwner = isVerifiedOwnerUser();
  const isAdmin = isAdminUser();
  if (isAdmin) {
    adminUnsubs.push(onSnapshot(query(collection(db, 'reports'), limit(80)), (snap) => {
      state.reports = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      render();
    }, logSubErr('reports')));
  }
  if (isOwner) {
    adminUnsubs.push(onSnapshot(collection(db, 'admins'), (snap) => {
      state.admins = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      render();
    }, logSubErr('admins')));
    adminUnsubs.push(onSnapshot(query(collection(db, 'users'), limit(80)), (snap) => {
      state.ownerUsers = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      render();
    }, logSubErr('owner_users')));
    adminUnsubs.push(onSnapshot(query(collection(db, 'crush_signals'), limit(80)), (snap) => {
      state.ownerSignals = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      render();
    }, logSubErr('owner_signals')));
    adminUnsubs.push(onSnapshot(query(collection(db, 'mutual_crush_matches'), limit(80)), (snap) => {
      state.ownerMatches = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      render();
    }, logSubErr('owner_matches')));
  } else if (uid) {
    adminUnsubs.push(onSnapshot(doc(db, 'admins', uid), (snap) => {
      if (snap.exists()) {
        state.admins = [{ id: snap.id, ...snap.data() }];
        render();
      }
    }, logSubErr('my_admin_doc')));
  }
}

function syncSosGroupListeners(uid) {
  const joinedIds = new Set(state.groupsMine.map((g) => g.id));
  Object.keys(sosUnsubs).forEach((gid) => {
    if (!joinedIds.has(gid)) {
      sosUnsubs[gid].forEach((u) => { try { u(); } catch (_) {} });
      delete sosUnsubs[gid];
      delete state.sosAlertsByGroup[gid];
    }
  });
  joinedIds.forEach((gid) => {
    if (sosUnsubs[gid]) return;
    const bySent = {};
    const byRec = {};
    const rebuild = () => {
      state.sosAlertsByGroup[gid] = Object.values({ ...bySent, ...byRec })
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      render();
    };
    const u1 = onSnapshot(query(collection(db, 'groups', gid, 'sos_alerts'), where('senderId', '==', uid)), (snap) => {
      Object.keys(bySent).forEach((k) => delete bySent[k]);
      snap.docs.forEach((d) => { bySent[d.id] = { id: d.id, ...d.data() }; });
      rebuild();
    }, logSubErr(`sos_sent_${gid}`));
    const u2 = onSnapshot(query(collection(db, 'groups', gid, 'sos_alerts'), where('recipientUids', 'array-contains', uid)), (snap) => {
      Object.keys(byRec).forEach((k) => delete byRec[k]);
      snap.docs.forEach((d) => { byRec[d.id] = { id: d.id, ...d.data() }; });
      rebuild();
    }, logSubErr(`sos_rec_${gid}`));
    sosUnsubs[gid] = [u1, u2];
  });
}

function startListeners(uid) {
  clearSubs();

  unsubs.push(onSnapshot(doc(db, 'users_public', uid), (snap) => {
    if (snap.exists()) {
      state.me = { id: snap.id, ...snap.data() };
      state.usersById[uid] = state.me;
      refreshGovernanceListeners(uid);
      render();
    }
  }, logSubErr('me')));

  unsubs.push(onSnapshot(doc(db, 'users', uid), (snap) => {
    if (snap.exists()) {
      state.privateUser = { id: snap.id, ...snap.data() };
      render();
    }
  }, logSubErr('private_user')));

  unsubs.push(onSnapshot(doc(db, 'app_config', 'owner'), (snap) => {
    state.ownerConfig = snap.exists() ? snap.data() : null;
    refreshGovernanceListeners(uid);
    render();
  }, logSubErr('owner_config')));

  unsubs.push(onSnapshot(doc(db, 'app_config', 'global_settings'), (snap) => {
    state.globalSettings = snap.exists() ? snap.data() : null;
    render();
  }, logSubErr('global_settings')));

  // Required by firestore.rules line 658: existing().profilePrivacy != 'private'
  unsubs.push(onSnapshot(query(collection(db, 'users_public'), where('profilePrivacy', 'in', ['public', 'friends'])), (snap) => {
    const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    state.users = list;
    const map = { ...state.usersById };
    list.forEach((u) => { map[u.userId || u.id] = u; });
    state.usersById = map;
    render();
  }, logSubErr('users_public')));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'blocked'), (snap) => {
    state.blockedUids = new Set(snap.docs.map((d) => d.id));
    snap.docs.forEach((d) => {
      const n = d.data()?.blockedName;
      if (n) state.blockedNamesById[d.id] = n;
    });
    mergePosts();
    mergeStories();
    render();
  }, logSubErr('blocked')));

  unsubs.push(onSnapshot(query(collection(db, 'posts'), where('privacy', '==', 'public'), limit(60)), (snap) => {
    state.postsPublic = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergePosts();
    render();
  }, logSubErr('posts_public')));

  unsubs.push(onSnapshot(query(collection(db, 'posts'), where('authorId', '==', uid), limit(40)), (snap) => {
    state.postsMine = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergePosts();
    render();
  }, logSubErr('posts_mine')));

  unsubs.push(onSnapshot(query(collection(db, 'posts'), where('privacy', '==', 'friends'), where('allowedViewerUids', 'array-contains', uid), limit(40)), (snap) => {
    state.postsShared = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergePosts();
    render();
  }, logSubErr('posts_shared')));

  unsubs.push(onSnapshot(query(collection(db, 'stories'), where('privacy', '==', 'public'), limit(35)), (snap) => {
    state.storiesPublic = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergeStories();
    render();
  }, logSubErr('stories_public')));

  unsubs.push(onSnapshot(query(collection(db, 'stories'), where('authorId', '==', uid), limit(25)), (snap) => {
    state.storiesMine = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergeStories();
    render();
  }, logSubErr('stories_mine')));

  unsubs.push(onSnapshot(query(collection(db, 'reels'), limit(40)), (snap) => {
    state.reels = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('reels')));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'saved_posts'), (snap) => {
    state.savedPostIds = new Set(snap.docs.map((d) => d.id));
    render();
  }, logSubErr('saved_posts')));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'notifications'), (snap) => {
    state.notifications = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('notifications')));

  unsubs.push(onSnapshot(query(collection(db, 'conversations'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.conversations = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
    if (!state.activeConvId && state.conversations.length > 0) {
      selectConversation(state.conversations[0].id, false);
    }
    render();
  }, logSubErr('conversations')));

  unsubs.push(onSnapshot(query(collection(db, 'calls'), where('participantUids', 'array-contains', uid)), (snap) => {
    const now = Date.now();
    state.activeCalls = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .filter((c) => (c.status === 'ringing' || c.status === 'active') && (now - (c.startedAtMs || now)) < 180000);
    const hasIncoming = state.activeCalls.some((c) => c.status === 'ringing' && c.callerId !== uid);
    updateRingtone(hasIncoming);
    render();
  }, logSubErr('calls')));

  unsubs.push(onSnapshot(query(collection(db, 'friendships'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.friendships = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('friendships')));

  unsubs.push(onSnapshot(query(collection(db, 'friend_requests'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.friendRequests = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('friend_requests')));

  unsubs.push(onSnapshot(query(collection(db, 'follows'), where('followerUid', '==', uid)), (snap) => {
    state.follows = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('follows')));

  unsubs.push(onSnapshot(query(collection(db, 'groups'), where('privacy', '==', 'public'), limit(40)), (snap) => {
    state.groupsPublic = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergeGroups();
    render();
  }, logSubErr('groups_public')));

  unsubs.push(onSnapshot(query(collection(db, 'groups'), where('memberUids', 'array-contains', uid), limit(40)), (snap) => {
    state.groupsMine = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    mergeGroups();
    syncSosGroupListeners(uid);
    render();
  }, logSubErr('groups_mine')));

  unsubs.push(onSnapshot(query(collection(db, 'pages'), limit(40)), (snap) => {
    state.pages = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.followersCount || 0) - (a.followersCount || 0));
    render();
  }, logSubErr('pages')));

  unsubs.push(onSnapshot(query(collection(db, 'events'), limit(40)), (snap) => {
    state.events = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('events')));

  unsubs.push(onSnapshot(query(collection(db, 'marketplace_listings'), limit(50)), (snap) => {
    state.marketplace = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('marketplace_listings')));

  unsubs.push(onSnapshot(query(collection(db, 'secret_loves'), where('recipientId', '==', uid)), (snap) => {
    state.secretInbox = snap.docs.map((d) => ({ id: d.id, ...d.data() })).filter((m) => !m.isBlocked).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('secret_loves')));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'sent_secret_loves'), (snap) => {
    state.secretSent = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    render();
  }, logSubErr('sent_secret_loves')));

  unsubs.push(onSnapshot(collection(db, 'users', uid, 'secret_crushes'), (snap) => {
    state.secretCrushes = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('secret_crushes')));

  unsubs.push(onSnapshot(query(collection(db, 'mutual_crush_matches'), where('participantUids', 'array-contains', uid)), (snap) => {
    state.mutualMatches = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('mutual_matches')));

  unsubs.push(onSnapshot(query(collection(db, 'devotion_trials'), where('senderId', '==', uid)), (snap) => {
    state.devotionTrials = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    render();
  }, logSubErr('devotion_trials')));

  refreshGovernanceListeners(uid);
}

let msgUnsub = null;
function selectConversation(convId, openMobile = true) {
  state.activeConvId = convId;
  state.replyToMsg = null;
  if (openMobile) state.mobileShowChat = true;
  if (msgUnsub) { msgUnsub(); msgUnsub = null; }
  if (!convId || !isValidId(convId)) { state.messages = []; render(); return; }
  msgUnsub = onSnapshot(collection(db, 'conversations', convId, 'messages'), (snap) => {
    state.messages = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    render();
    setTimeout(() => {
      const box = document.getElementById('chatMsgsBox');
      if (box) box.scrollTop = box.scrollHeight;
    }, 30);
  }, logSubErr('messages'));
  if (state.user) {
    updateDoc(doc(db, 'conversations', convId), { readByUids: arrayUnion(state.user.uid) }).catch(() => {});
  }
  render();
}

async function publishPost(groupId = '', pageId = '') {
  if (!state.user || state.isUploading) return;
  const text = state.postDraft.trim();
  if (!text && !state.postMediaFile) {
    showBanner('Write something or attach a photo/video first.', true);
    return;
  }
  try {
    let mediaUri = '';
    let mediaType = 'none';
    if (state.postMediaFile) {
      mediaUri = await uploadMediaBlob(state.postMediaFile, '', 'Uploading post media...');
      mediaType = state.postMediaFile.type?.startsWith('video/') ? 'video' : 'image';
    }
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    const friendUids = state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== state.user.uid);
    const activeGroup = groupId ? state.groups.find((g) => g.id === groupId) : null;
    const effectivePrivacy = activeGroup?.privacy === 'private' ? 'friends' : (pageId ? 'public' : state.postPrivacy);
    const allowedViewers = activeGroup?.privacy === 'private'
      ? (activeGroup.memberUids || []).slice(0, 200)
      : (effectivePrivacy === 'friends' ? friendUids.slice(0, 200) : []);

    await addDoc(collection(db, 'posts'), {
      authorId: state.user.uid,
      authorName: (me.displayName || 'Hello Member').slice(0, 80),
      authorAvatar: me.photoUrl || '',
      content: text.slice(0, 5000),
      mediaUri,
      mediaType,
      privacy: effectivePrivacy,
      allowedViewerUids: allowedViewers,
      groupId: groupId || '',
      pageId: pageId || '',
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    state.postDraft = '';
    setPreviewFile('postMediaFile', 'postMediaPreview', null);
    showBanner(pageId ? 'Posted to Page!' : (groupId ? 'Posted to Group!' : 'Post published to HELLO!'));
  } catch (e) {
    showBanner(e?.message || 'Could not publish post.', true);
  }
}

async function toggleLike(post) {
  if (!state.user) return;
  try {
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
  } catch (e) {
    showBanner(e?.message || 'Reaction failed.', true);
  }
}

async function toggleSavePost(post) {
  if (!state.user) return;
  try {
    const ref = doc(db, 'users', state.user.uid, 'saved_posts', post.id);
    if (state.savedPostIds.has(post.id)) {
      await deleteDoc(ref);
      showBanner('Removed from Saved.');
    } else {
      await setDoc(ref, {
        postId: post.id,
        itemType: 'post',
        authorName: (post.authorName || 'Member').slice(0, 80),
        excerpt: (post.content || '').slice(0, 280),
        createdAt: serverTimestamp()
      });
      showBanner('Saved to your HELLO bookmarks.');
    }
  } catch (e) {
    showBanner(e?.message || 'Could not save post.', true);
  }
}

async function toggleComments(postId) {
  state.openComments[postId] = !state.openComments[postId];
  if (state.openComments[postId]) {
    try {
      const snap = await getDocs(collection(db, 'posts', postId, 'comments'));
      state.commentsByPost[postId] = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    } catch (_) {}
  }
  render();
}

async function submitComment(postId, text) {
  if (!state.user || !text.trim()) return;
  try {
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
    state.commentsByPost[postId] = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    render();
  } catch (e) {
    showBanner(e?.message || 'Comment failed.', true);
  }
}

async function openOrCreateDm(targetUser, initialText = '', initialMediaUri = '', initialMediaType = 'none') {
  if (!state.user || !targetUser) return;
  const uid = state.user.uid;
  const targetId = targetUser.userId || targetUser.id;
  if (!isValidId(targetId)) return;
  try {
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
  } catch (e) {
    showBanner(e?.message || 'Could not open chat.', true);
  }
}

async function sendChatMessage(convId, participantUids, text, mediaUri = '', mediaType = 'none', fileName = '') {
  if (!state.user || !isValidId(convId)) return;
  if (!text && !mediaUri) return;
  try {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member' };
    const finalText = state.replyToMsg && text
      ? `↩ ${state.replyToMsg.senderName}: "${(state.replyToMsg.text || state.replyToMsg.mediaType).slice(0, 60)}"\n${text}`
      : text;
    state.replyToMsg = null;
    const expiresAtMs = state.disappearing24h ? Date.now() + 86400000 : 0;

    await addDoc(collection(db, 'conversations', convId, 'messages'), {
      conversationId: convId,
      senderId: state.user.uid,
      senderName: (me.displayName || 'Hello Member').slice(0, 80),
      text: (finalText || '').slice(0, 2000),
      mediaUri: mediaUri || '',
      mediaType,
      fileName: (fileName || '').slice(0, 200),
      reactions: {},
      participantUids,
      isDeleted: false,
      expiresAtMs,
      createdAt: serverTimestamp()
    });
    const preview = text || (mediaType === 'image' ? 'Photo' : mediaType === 'video' ? 'Video' : mediaType === 'audio' ? 'Voice message' : `File: ${fileName || 'Attachment'}`);
    await updateDoc(doc(db, 'conversations', convId), {
      lastMessage: preview.slice(0, 500),
      lastSenderId: state.user.uid,
      typingUids: arrayRemove(state.user.uid),
      readByUids: [state.user.uid],
      updatedAt: serverTimestamp()
    });
  } catch (e) {
    showBanner(e?.message || 'Message failed to send.', true);
  }
}

async function startCallSignal(conv, callType) {
  if (!state.user || !conv) return;
  try {
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
    showBanner(`Ringing ${title} (${callType} call)...`);
  } catch (e) {
    showBanner(e?.message || 'Could not start call.', true);
  }
}

async function updateCallStatus(call, nextStatus) {
  if (!state.user || !call) return;
  try {
    const patch = {
      status: nextStatus,
      updatedAt: serverTimestamp()
    };
    if (nextStatus === 'active') {
      patch.acceptedUids = arrayUnion(state.user.uid);
    } else {
      patch.endedAtMs = Date.now();
    }
    await updateDoc(doc(db, 'calls', call.id), patch);
    if (nextStatus === 'active') {
      state.tab = 'messenger';
      selectConversation(call.conversationId, true);
    }
  } catch (e) {
    showBanner(e?.message || 'Could not update call.', true);
  }
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
    recTimer = setInterval(() => { state.recordingSec += 1; render(); }, 1000);
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) recChunks.push(e.data); };
    mediaRecorder.onstop = async () => {
      clearInterval(recTimer);
      state.isRecordingVoice = false;
      state.recordingSec = 0;
      stream.getTracks().forEach((t) => t.stop());
      const blob = new Blob(recChunks, { type: mime });
      if (blob.size > 0) {
        try {
          const file = new File([blob], `voice-${Date.now()}.webm`, { type: mime });
          const uri = await uploadMediaBlob(file, mime, 'Sending voice note...');
          await sendChatMessage(conv.id, conv.participantUids || [], '', uri, 'audio', file.name);
        } catch (err) {
          showBanner(err?.message || 'Voice note upload failed.', true);
        }
      }
      render();
    };
    mediaRecorder.start();
    render();
  } catch (_) {
    state.isRecordingVoice = false;
    showBanner('Microphone permission is required to record voice notes.', true);
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
  try {
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
  } catch (e) {
    showBanner(e?.message || 'Could not update group membership.', true);
  }
}

async function toggleFollowPage(page) {
  if (!state.user || !page) return;
  const uid = state.user.uid;
  const ref = doc(db, 'pages', page.id);
  const followers = Array.isArray(page.followerUids) ? page.followerUids : [];
  const isFollowing = followers.includes(uid);
  const nextFollowers = isFollowing
    ? followers.filter((x) => x !== uid)
    : Array.from(new Set([...followers, uid]));
  try {
    await updateDoc(ref, {
      followerUids: nextFollowers,
      followersCount: nextFollowers.length,
      updatedAt: serverTimestamp()
    });
    showBanner(isFollowing ? `Unfollowed ${page.name}` : `Following ${page.name}`);
  } catch (e) {
    showBanner(e?.message || 'Could not update page follow.', true);
  }
}

function renderBrandTile(size = 34) {
  return `<span class="brand-tile" style="width:${size}px;height:${size}px;border-radius:${Math.round(size * 0.28)}px"><img src="./hello-logo.png" alt="HELLO"/></span>`;
}

function renderLanding() {
  return `
    <header class="topbar">
      <div class="brand-btn" style="cursor:default;">
        ${renderBrandTile(34)}
        <span class="brand-name">HELLO</span>
      </div>
      <div class="top-actions">
        <button class="icon-btn" id="themeToggleBtn" title="Toggle Theme">${iconSvg(state.theme === 'dark' ? 'sun' : 'moon', 18)}</button>
        <button class="btn btn-secondary open-get-app-btn" id="installLaptopBtnTop">${iconSvg('laptop', 17)}<span>Website &amp; Laptop App</span></button>
        <a class="btn btn-primary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 17)}<span>Android APK</span></a>
      </div>
    </header>
    <main class="auth-page">
      <div class="auth-shell">
        <section style="display:flex;flex-direction:column;gap:14px;">
          <div class="security-pill">${iconSvg('shield', 15)}<span>Verified Firebase Auth & Encrypted Cloud Sync</span></div>
          <h1 class="auth-headline">HELLO on Phone, Website &amp; Laptop.</h1>
          <p class="auth-desc">
            Use HELLO directly on this official website, install it as a native Desktop / Laptop App on Windows, Mac &amp; Linux, or download the Android APK — everything stays synced in real time.
          </p>
          <div class="feature-list">
            <div class="feature-chip">${iconSvg('chat', 18)}<span>Live Messenger & Calls</span></div>
            <div class="feature-chip">${iconSvg('reels', 18)}<span>Stories, Posts & Reels</span></div>
            <div class="feature-chip">${iconSvg('heart', 18)}<span>Secret Love Inbox</span></div>
            <div class="feature-chip">${iconSvg('shop', 18)}<span>Marketplace & Groups</span></div>
          </div>
          <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:6px;">
            <button class="btn btn-violet open-get-app-btn" id="installLaptopBtnHero">${iconSvg('laptop', 17)}<span>Install on Desktop / Laptop</span></button>
            <button class="btn btn-secondary copy-website-url-btn" type="button">${iconSvg('share', 16)}<span>Copy Website Link</span></button>
            <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 17)}<span>Download Android APK (v1.8.7)</span></a>
          </div>
        </section>

        <section class="card" style="padding:26px;display:flex;flex-direction:column;gap:14px;">
          <div style="display:flex;align-items:center;gap:12px;">
            ${renderBrandTile(48)}
            <div>
              <div style="font-weight:800;font-size:20px;">Sign in to HELLO</div>
              <div class="muted">Use the same Google account as your HELLO Android app</div>
            </div>
          </div>
          <button class="btn btn-primary" id="googleSignInBtn" style="min-height:46px;font-size:15px;">
            Continue with Google
          </button>
          ${state.statusBanner ? `<div class="${state.statusIsError ? 'error-box' : 'notice-box'}">${esc(state.statusBanner)}</div>` : ''}
          ${state.authError ? `
            <div class="error-box">
              <div>${esc(state.authError)}</div>
              ${state.unauthorizedDomain ? `
                <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;">
                  <a class="btn btn-primary" href="${AUTH_SETTINGS_URL}" target="_blank" rel="noopener noreferrer">Open Firebase Authorized Domains</a>
                  <button class="btn btn-secondary" id="copyDomainBtn" type="button">Copy "${esc(state.unauthorizedDomain)}"</button>
                </div>
              ` : ''}
            </div>
          ` : ''}
          <div class="muted" style="font-size:12.5px;line-height:1.5;">
            Protected by Google Sign-In and strict server-side Firebase security rules.
          </div>
        </section>
      </div>
    </main>
    ${renderModals()}
  `;
}

function renderStoriesStrip() {
  const uid = state.user.uid;
  const me = state.me || { displayName: state.user.displayName, photoUrl: state.user.photoURL };
  const myPhoto = mediaSrc(me.photoUrl);
  const friendSet = new Set(state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== uid));
  let filtered = state.stories;
  if (state.storyFilter === 'mine') filtered = filtered.filter((s) => s.authorId === uid);
  else if (state.storyFilter === 'friends') filtered = filtered.filter((s) => friendSet.has(s.authorId));
  else if (state.storyFilter === 'highlights') filtered = filtered.filter((s) => Boolean(s.isSavedHighlight));

  return `
    <div class="card" style="padding:14px;">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:10px;">
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
          <span style="font-weight:700;font-size:15px;">Stories</span>
          <div class="chip-row">
            ${[['all', 'All'], ['mine', 'My Stories'], ['friends', 'Friends'], ['highlights', 'Highlights']].map(([k, lbl]) => `
              <button class="filter-chip ${state.storyFilter === k ? 'active' : ''}" data-story-filter="${k}" style="padding:3px 10px;font-size:11.5px;">${lbl}</button>
            `).join('')}
          </div>
        </div>
        <button class="btn btn-secondary" id="openCreateStoryBtn" style="padding:5px 12px;min-height:30px;font-size:12px;">${iconSvg('plus', 15)}<span>Add Story</span></button>
      </div>
      <div class="stories-row">
        <button class="story-card story-card-create" id="createStoryCardBtn">
          <div class="story-create-top">
            ${myPhoto ? `<img referrerpolicy="no-referrer" src="${esc(myPhoto)}" alt="Your story" />` : `<div style="width:100%;height:100%;background:linear-gradient(135deg,#1E1B4B,#7C3AED);"></div>`}
          </div>
          <div class="story-create-plus">${iconSvg('plus', 16)}</div>
          <div class="story-create-label">Create Story</div>
        </button>
        ${filtered.map((s) => {
          const author = resolveAuthor(s.authorId, s.authorName, s.authorAvatar);
          const bgSrc = mediaSrc(s.mediaUri);
          const isCloud = typeof s.mediaUri === 'string' && s.mediaUri.startsWith('firestore://media/');
          const hex = /^#[0-9A-Fa-f]{6}$/.test(s.backgroundHex || '') ? s.backgroundHex : '#1E1B4B';
          return `
            <button class="story-card view-story-btn" data-story-id="${esc(s.id)}" style="background:${hex};">
              ${(bgSrc || isCloud) ? `<img class="story-bg" referrerpolicy="no-referrer" data-media-uri="${esc(s.mediaUri)}" src="${esc(bgSrc)}" alt="${esc(author.name)}" style="${bgSrc ? '' : 'display:none;'}" />` : ''}
              <div class="story-scrim">
                <span class="story-ring">${avatarHtml(author.name, author.avatar, 30, false)}</span>
                <div>
                  ${s.isSavedHighlight ? `<span class="badge" style="font-size:9px;padding:1px 6px;">★ Highlight</span>` : ''}
                  ${s.caption ? `<div style="font-size:11px;margin-bottom:4px;opacity:0.95;line-height:1.25;">${esc(s.caption.slice(0, 46))}</div>` : ''}
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

function renderPostCard(p) {
  const uid = state.user?.uid;
  const author = resolveAuthor(p.authorId, p.authorName, p.authorAvatar);
  const myEmoji = state.myReactionByPost[p.id] || (state.likedByPost[p.id] ? '👍' : '');
  const liked = Boolean(myEmoji);
  const saved = state.savedPostIds.has(p.id);
  const commentsOpen = Boolean(state.openComments[p.id]);
  const comments = state.commentsByPost[p.id] || [];
  const isOwnerOrAuthor = p.authorId === uid || isAdminUser();
  const pickerOpen = state.reactionPickerPostId === p.id;
  return `
    <article class="card" style="position:relative;">
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
        <div style="display:flex;align-items:center;gap:4px;">
          ${isOwnerOrAuthor ? `
            <button class="btn btn-secondary edit-post-btn" data-post-id="${esc(p.id)}" style="padding:4px 9px;min-height:28px;font-size:11.5px;">Edit</button>
            <button class="icon-btn delete-post-btn" data-post-id="${esc(p.id)}" title="Delete post" style="width:30px;height:30px;color:var(--rose);">${iconSvg('trash', 15)}</button>
          ` : `
            <button class="btn btn-secondary report-open-btn" data-rtype="post" data-rid="${esc(p.id)}" style="padding:4px 9px;min-height:28px;font-size:11.5px;">Report</button>
            <button class="btn btn-secondary block-user-btn" data-uid="${esc(p.authorId)}" data-name="${esc(author.name)}" style="padding:4px 9px;min-height:28px;font-size:11.5px;">Block</button>
          `}
          <button class="icon-btn save-post-btn" data-post-id="${esc(p.id)}" title="Save post" style="width:32px;height:32px;color:${saved ? 'var(--violet-light)' : 'var(--muted)'};">${iconSvg('bookmark', 17)}</button>
        </div>
      </div>
      ${p.content ? `<div class="post-body">${esc(p.content)}</div>` : ''}
      ${p.mediaType === 'image' && p.mediaUri ? mediaImgHtml(p.mediaUri, 'post-media open-lightbox', 'Post photo', `data-lightbox-uri="${esc(p.mediaUri)}"`) : ''}
      ${p.mediaType === 'video' && p.mediaUri ? mediaVideoHtml(p.mediaUri, 'post-media') : ''}
      <div class="post-stats">
        <span>${p.likesCount || 0} reactions ${myEmoji ? `(${myEmoji})` : ''}</span>
        <span>${p.commentsCount || 0} comments · ${p.sharesCount || 0} shares</span>
      </div>
      ${pickerOpen ? `
        <div class="reaction-picker-row">
          ${REACTIONS.map((em) => `<button type="button" class="reaction-emoji-btn pick-post-react-btn" data-post-id="${esc(p.id)}" data-emoji="${em}">${em}</button>`).join('')}
        </div>
      ` : ''}
      <div class="post-actions">
        <button class="action-pill like-btn ${liked ? 'liked' : ''}" data-post-id="${esc(p.id)}">${liked ? `<span>${myEmoji}</span>` : iconSvg('like', 17)}<span>${liked ? 'Reacted' : 'Like'}</span></button>
        <button class="action-pill toggle-react-picker-btn" data-post-id="${esc(p.id)}" title="More reactions" style="flex:0 0 auto;padding:8px 12px;">😊</button>
        <button class="action-pill comment-toggle-btn" data-post-id="${esc(p.id)}">${iconSvg('comment', 17)}<span>Comment</span></button>
        <button class="action-pill open-share-modal-btn" data-post-id="${esc(p.id)}">${iconSvg('share', 17)}<span>Share</span></button>
      </div>
      ${commentsOpen ? `
        <div class="comments-box">
          ${comments.map((c) => {
            const ca = resolveAuthor(c.authorId, c.authorName, c.authorAvatar);
            const canDelC = c.authorId === uid || p.authorId === uid || isAdminUser();
            return `
              <div class="comment-item" style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
                <div><strong>${esc(ca.name)}</strong>: ${esc(c.text)}</div>
                ${canDelC ? `<button class="msg-mini-btn delete-comment-btn" data-post-id="${esc(p.id)}" data-comment-id="${esc(c.id)}" title="Delete comment">${iconSvg('close', 13)}</button>` : ''}
              </div>
            `;
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

function renderCenterStage() {
  const uid = state.user.uid;
  const q = state.search.trim().toLowerCase();

  if (state.tab === 'messenger') {
    const activePeople = state.users.filter((u) => (u.userId || u.id) !== uid && !state.blockedUids.has(u.userId || u.id)).sort((a, b) => Number(b.isOnline) - Number(a.isOnline));
    let convs = state.conversations.filter((c) => {
      if (!q) return true;
      return (c.title || '').toLowerCase().includes(q) || Object.values(c.participantNames || {}).some((n) => String(n).toLowerCase().includes(q));
    });
    if (state.inboxFilter === 'unread') convs = convs.filter((c) => !(c.readByUids || []).includes(uid));
    if (state.inboxFilter === 'groups') convs = convs.filter((c) => Boolean(c.isGroup));
    if (state.inboxFilter === 'active') convs = convs.filter((c) => (c.participantUids || []).some((p) => p !== uid && state.usersById[p]?.isOnline));

    const activeConv = state.conversations.find((c) => c.id === state.activeConvId) || convs[0];
    const hasDraft = state.msgDraft.trim().length > 0;

    return `
      <div class="messenger-shell">
        <div class="conv-list ${state.mobileShowChat ? 'hidden-mobile' : ''}">
          <div class="conv-list-head">
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <div>
                <div style="font-weight:800;font-size:18px;">Messages</div>
                <div class="muted" style="font-size:11.5px;">${activePeople.filter((u) => u.isOnline).length} active now</div>
              </div>
              <button class="icon-btn" id="openNewGroupChatBtn" title="New Group Chat" style="width:36px;height:36px;">${iconSvg('plus', 18)}</button>
            </div>
            ${activePeople.length ? `
              <div class="active-people-strip">
                ${activePeople.slice(0, 14).map((p) => `
                  <button class="active-person-chip msg-person-btn" data-uid="${esc(p.userId || p.id)}" title="Message ${esc(p.displayName)}">
                    ${avatarHtml(p.displayName, p.photoUrl, 44, Boolean(p.isOnline && p.showOnlineStatus !== false))}
                    <span>${esc((p.displayName || 'User').split(' ')[0])}</span>
                  </button>
                `).join('')}
              </div>
            ` : ''}
            <div class="chip-row">
              ${[['all', 'Primary'], ['unread', 'Unread'], ['active', 'Active'], ['groups', 'Groups']].map(([k, lbl]) => `
                <button class="filter-chip ${state.inboxFilter === k ? 'active' : ''}" data-inbox-filter="${k}" style="padding:4px 10px;font-size:12px;">${lbl}</button>
              `).join('')}
            </div>
          </div>
          <div class="conv-scroll">
            ${convs.length === 0 ? `<div class="muted" style="padding:14px;">No conversations here yet. Tap any person above to start chatting!</div>` : convs.map((c) => {
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
            const othersTyping = (activeConv.typingUids || []).filter((id) => id !== uid);
            const nowMs = Date.now();
            return `
              <div class="chat-head">
                <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                  <button class="icon-btn" id="mobileBackChat" title="Back" style="width:34px;height:34px;">${iconSvg('back', 18)}</button>
                  <div class="open-user-profile" data-uid="${esc(otherId || '')}" style="display:flex;align-items:center;gap:10px;min-width:0;cursor:${otherId ? 'pointer' : 'default'};">
                    ${avatarHtml(resolved.name, resolved.avatar, 38, resolved.online)}
                    <div style="min-width:0;">
                      <div style="font-weight:700;display:flex;align-items:center;gap:5px;">
                        <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(resolved.name)}</span>
                        ${resolved.verified ? `<span class="verified-badge">${iconSvg('verified', 14)}</span>` : ''}
                      </div>
                      <div class="muted" style="font-size:12px;">${othersTyping.length ? 'Typing...' : (resolved.online ? 'Active now' : (activeConv.isGroup ? `${(activeConv.participantUids || []).length} members` : 'HELLO Messenger'))}</div>
                    </div>
                  </div>
                </div>
                <div style="display:flex;gap:6px;align-items:center;">
                  <button class="filter-chip ${state.disappearing24h ? 'active' : ''}" id="toggle24hBtn" title="24h Disappearing Messages" style="padding:4px 9px;font-size:11px;">⏱ 24h ${state.disappearing24h ? 'On' : 'Off'}</button>
                  <button class="icon-btn" id="startAudioCallBtn" title="Audio Call">${iconSvg('call', 18)}</button>
                  <button class="icon-btn" id="startVideoCallBtn" title="Video Call">${iconSvg('video', 18)}</button>
                  ${activeConv.isGroup ? `<button class="btn btn-secondary" id="leaveGroupChatBtn" style="padding:4px 9px;min-height:30px;font-size:11.5px;">Leave</button>` : (otherId ? `<button class="btn btn-secondary block-user-btn" data-uid="${esc(otherId)}" data-name="${esc(resolved.name)}" style="padding:4px 9px;min-height:30px;font-size:11.5px;">Block</button>` : '')}
                </div>
              </div>

              <div class="chat-msgs" id="chatMsgsBox">
                ${state.messages.filter((m) => !m.isDeleted && !(m.expiresAtMs > 0 && nowMs > m.expiresAtMs)).map((m) => {
                  const mine = m.senderId === uid;
                  const reactionVals = Object.values(m.reactions || {});
                  const dlName = (m.fileName || 'file').replace(/[^a-zA-Z0-9._-]/g, '_');
                  const cloudSrc = m.mediaUri ? mediaSrc(m.mediaUri) : '';
                  const emojiTrayOpen = state.activeMsgEmojiId === m.id;
                  return `
                    <div class="msg-row ${mine ? 'mine' : 'theirs'}">
                      ${!mine ? `<div class="muted" style="font-size:11px;margin-bottom:2px;">${esc(resolveAuthor(m.senderId, m.senderName).name)}</div>` : ''}
                      <div class="bubble">
                        ${m.mediaType === 'image' && m.mediaUri ? mediaImgHtml(m.mediaUri, 'open-lightbox', 'Photo', `style="max-width:260px;border-radius:12px;display:block;cursor:pointer;margin-bottom:${m.text ? '6px' : '0'};" data-lightbox-uri="${esc(m.mediaUri)}"`) : ''}
                        ${m.mediaType === 'video' && m.mediaUri ? mediaVideoHtml(m.mediaUri, '', 'controls playsinline style="max-width:260px;border-radius:12px;display:block;"') : ''}
                        ${m.mediaType === 'audio' && m.mediaUri ? `<audio controls data-media-uri="${esc(m.mediaUri)}" src="${esc(cloudSrc)}" style="max-width:240px;"></audio>` : ''}
                        ${m.mediaType === 'file' ? `<a data-media-uri="${esc(m.mediaUri || '')}" href="${esc(cloudSrc || '#')}" download="${esc(dlName)}" rel="noopener noreferrer" style="display:inline-flex;align-items:center;gap:6px;text-decoration:underline;">${iconSvg('file', 16)}<span>${esc(m.fileName || 'Attachment')}</span></a>` : ''}
                        ${m.text ? `<div style="white-space:pre-wrap;">${esc(m.text)}</div>` : ''}
                      </div>
                      ${emojiTrayOpen ? `
                        <div class="reaction-picker-row" style="margin-top:4px;">
                          ${['❤️', '😂', '👍', '😮', '😢'].map((em) => `<button type="button" class="reaction-emoji-btn react-msg-emoji-btn" data-msg-id="${esc(m.id)}" data-emoji="${em}">${em}</button>`).join('')}
                        </div>
                      ` : ''}
                      <div class="msg-meta-row">
                        <span>${fmtTime(m.createdAt)}${m.expiresAtMs > 0 ? ' · ⏱24h' : ''}</span>
                        ${reactionVals.length ? `<span>${esc(reactionVals.join(' '))}</span>` : ''}
                        <button class="msg-mini-btn reply-msg-btn" data-msg-id="${esc(m.id)}" title="Reply">${iconSvg('reply', 12)}</button>
                        <button class="msg-mini-btn toggle-msg-emoji-btn" data-msg-id="${esc(m.id)}" title="React">${iconSvg('heart', 12)}</button>
                        ${mine ? `<button class="msg-mini-btn unsend-msg-btn" data-msg-id="${esc(m.id)}" title="Unsend">${iconSvg('trash', 12)}</button>` : ''}
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>

              <div class="composer-dock">
                <input type="file" accept="image/*,video/*" capture="environment" id="chatCameraInput" class="sr-only-input" />
                <input type="file" accept="image/*,video/*" id="chatMediaInput" class="sr-only-input" />
                <input type="file" id="chatFileInput" class="sr-only-input" />
                ${state.replyToMsg ? `
                  <div class="reply-banner">
                    <span>Replying to <strong>${esc(state.replyToMsg.senderName)}</strong>: ${esc((state.replyToMsg.text || state.replyToMsg.mediaType || '').slice(0, 60))}</span>
                    <button class="msg-mini-btn" id="cancelReplyBtn">${iconSvg('close', 14)}</button>
                  </div>
                ` : ''}
                ${state.showAttachMenu ? `
                  <div class="attach-popover">
                    <button class="attach-menu-item" id="pickChatMediaBtn" type="button">${iconSvg('photo', 17)}<span>Photo / Video Gallery</span></button>
                    <button class="attach-menu-item" id="pickChatFileBtn" type="button">${iconSvg('file', 17)}<span>Document / File</span></button>
                  </div>
                ` : ''}
                <div class="screenshot-pill">
                  <button class="cam-circle" id="pickChatCameraBtn" type="button" title="Camera Photo or Video">${iconSvg('camera', 19)}</button>
                  <input id="chatTextInput" type="text" placeholder="${state.isUploading ? (state.uploadLabel || 'Uploading...') : (state.isRecordingVoice ? `Recording voice (${state.recordingSec}s)...` : (state.disappearing24h ? '24h disappearing message...' : 'Message...'))}" value="${esc(state.msgDraft)}" />
                  <button class="pill-icon-btn ${state.isRecordingVoice ? 'recording' : ''}" id="chatMicBtn" type="button" title="Voice message">${iconSvg(state.isRecordingVoice ? 'stop' : 'mic', 19)}</button>
                  <button class="pill-icon-btn" id="chatAttachToggleBtn" type="button" title="Attach">${iconSvg('attach', 19)}</button>
                  ${hasDraft ? `
                    <button class="text-send-btn" id="chatSendBtn" type="button" title="Send">${iconSvg('send', 18)}</button>
                  ` : `
                    <button class="heart-send-btn" id="chatHeartBtn" type="button" title="Send heart">${iconSvg('heart', 20)}</button>
                  `}
                </div>
              </div>
            `;
          })() : `<div style="padding:36px;" class="muted">Select a chat or tap an active friend to start messaging.</div>`}
        </div>
      </div>
    `;
  }

  if (state.tab === 'reels') {
    const reels = state.reels.filter((r) => {
      if (state.blockedUids.has(r.authorId)) return false;
      const catOk = state.reelFilter === 'All' || (r.category || '').toLowerCase() === state.reelFilter.toLowerCase();
      const qOk = !q || (r.caption || '').toLowerCase().includes(q) || (r.authorName || '').toLowerCase().includes(q);
      return catOk && qOk;
    });
    return `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div class="card" style="padding:14px;">
          <div class="reels-toolbar">
            <div>
              <h3 style="margin:0;">HELLO Reels</h3>
              <div class="muted">Vertical feed · plays sequentially ${state.reelAutoNext ? 'automatically' : 'manually'}</div>
            </div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
              <button class="filter-chip ${state.reelAutoNext ? 'active' : ''}" id="toggleReelAutoNextBtn" type="button">Auto-Next: ${state.reelAutoNext ? 'On' : 'Off'}</button>
              <button class="btn btn-secondary" id="reelPrevBtn" type="button" style="min-height:34px;padding:6px 12px;">↑ Prev</button>
              <button class="btn btn-secondary" id="reelNextBtn" type="button" style="min-height:34px;padding:6px 12px;">↓ Next</button>
              <button class="btn btn-primary" id="openCreateReelBtn">${iconSvg('plus', 16)}<span>Create Reel</span></button>
            </div>
          </div>
          <div class="chip-row" style="margin-top:10px;">
            ${REEL_CATEGORIES.map((c) => `<button class="filter-chip ${state.reelFilter === c ? 'active' : ''}" data-reel-cat="${c}">${c}</button>`).join('')}
          </div>
        </div>

        ${reels.length === 0 ? `<div class="card muted">No Reels match this category yet.</div>` : `
          <div class="reels-snap-feed" id="reelsSnapFeed">
            ${reels.map((r, idx) => {
              const author = resolveAuthor(r.authorId, r.authorName, r.authorAvatar);
              const hex = /^#[0-9A-Fa-f]{6}$/.test(r.coverHex || '') ? r.coverHex : '#1E1B4B';
              const isMine = r.authorId === uid || isAdminUser();
              const isSaved = state.savedPostIds.has(r.id);
              const src = r.videoUri ? mediaSrc(r.videoUri) : '';
              const isCloud = typeof r.videoUri === 'string' && r.videoUri.startsWith('firestore://media/');
              return `
                <section class="reel-slide" data-reel-idx="${idx}" data-reel-id="${esc(r.id)}">
                  ${r.videoUri ? `
                    <video class="reel-slide-video" playsinline controls preload="metadata" data-media-uri="${esc(r.videoUri)}" src="${esc(src)}" style="${src || !isCloud ? '' : 'display:none;'}"></video>
                    ${isCloud ? `<div class="media-loading-box" data-media-fallback="1" style="${src ? 'display:none;' : ''}">Loading Reel video...</div>` : ''}
                  ` : `
                    <div class="reel-slide-text-card" style="background:linear-gradient(155deg,${hex},#0B0E1E);">
                      ${esc(r.caption)}
                    </div>
                  `}
                  <div class="reel-slide-gradient"></div>
                  <div class="reel-slide-meta">
                    <div class="open-user-profile" data-uid="${esc(r.authorId)}" style="display:flex;align-items:center;gap:10px;cursor:pointer;width:fit-content;">
                      ${avatarHtml(author.name, author.avatar, 38, author.online)}
                      <div>
                        <div style="font-weight:800;font-size:14px;">${esc(author.name)}</div>
                        <div style="font-size:11.5px;opacity:0.85;">${esc(r.category || 'Trending')} · Reel ${idx + 1} / ${reels.length}</div>
                      </div>
                    </div>
                    <div style="font-size:13.5px;line-height:1.4;">${esc(r.caption)}</div>
                  </div>
                  <div class="reel-side-rail">
                    <button class="reel-rail-btn like-reel-btn" data-reel-id="${esc(r.id)}" title="Like">${iconSvg('like', 19)}</button>
                    <span class="reel-rail-count">${r.likesCount || 0}</span>
                    <button class="reel-rail-btn open-reel-comments-btn" data-reel-id="${esc(r.id)}" title="Comments">${iconSvg('comment', 19)}</button>
                    <span class="reel-rail-count">${r.commentsCount || 0}</span>
                    <button class="reel-rail-btn save-reel-btn" data-reel-id="${esc(r.id)}" title="Save" style="color:${isSaved ? '#A78BFA' : '#fff'};">${iconSvg('bookmark', 18)}</button>
                    <button class="reel-rail-btn msg-person-btn" data-uid="${esc(r.authorId)}" title="Message creator">${iconSvg('chat', 18)}</button>
                    ${isMine ? `<button class="reel-rail-btn delete-reel-btn" data-reel-id="${esc(r.id)}" title="Delete Reel" style="color:#FB7185;">${iconSvg('trash', 17)}</button>` : ''}
                  </div>
                </section>
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
    const people = state.users.filter((u) => (u.userId || u.id) !== uid && !state.blockedUids.has(u.userId || u.id) && (!q || (u.displayName || '').toLowerCase().includes(q) || (u.handle || '').toLowerCase().includes(q)));

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
          ${people.length === 0 ? `<div class="muted">No other discoverable members loaded yet.</div>` : `
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
                      <button class="btn btn-secondary follow-btn" data-uid="${esc(pid)}">${iconSvg('follow', 15)}<span>${isFollowing ? 'Following' : 'Follow'}</span></button>
                      ${isFriend ? `<span class="badge">Friend</span>` : (isPending ? `<span class="badge">Requested</span>` : `<button class="btn btn-secondary add-friend-btn" data-uid="${esc(pid)}">Add Friend</button>`)}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      </div>
    `;
  }

  if (state.tab === 'sos') return renderSosTab();
  if (state.tab === 'settings') return renderSettingsTab();
  if (state.tab === 'owner_profile') return renderOwnerProfileTab();
  if (state.tab === 'owner_dashboard') return renderOwnerDashboardTab();

  if (state.tab === 'groups') {
    if (state.activeGroupId) {
      const grp = state.groups.find((g) => g.id === state.activeGroupId);
      if (grp) {
        const joined = (grp.memberUids || []).includes(uid);
        const isGroupAdmin = grp.creatorId === uid || (grp.adminUids || []).includes(uid) || isAdminUser();
        const grpPosts = state.posts.filter((p) => p.groupId === grp.id);
        const grpAlerts = (state.sosAlertsByGroup[grp.id] || []).slice(0, 6);
        return `
          <div style="display:flex;flex-direction:column;gap:14px;">
            <div class="card">
              <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
                <div style="display:flex;align-items:center;gap:10px;">
                  <button class="icon-btn" id="backToGroupsBtn">${iconSvg('back', 18)}</button>
                  <div>
                    <h2 style="margin:0;">${esc(grp.name)}</h2>
                    <div class="muted">${esc(grp.category || 'Community')} · ${esc(grp.privacy)} · ${(grp.memberUids || []).length} members ${grp.sosEnabled ? '· 🚨 SOS Circle' : ''}</div>
                  </div>
                </div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                  ${joined && grp.sosEnabled !== false ? `<button class="btn btn-danger trigger-sos-open-btn" data-group-id="${esc(grp.id)}">🚨 SOS</button>` : ''}
                  ${isGroupAdmin ? `<button class="btn btn-secondary manage-group-btn" data-group-id="${esc(grp.id)}">Manage Members</button>` : ''}
                  ${grp.creatorId === uid || isAdminUser() ? `<button class="btn btn-secondary delete-group-btn" data-group-id="${esc(grp.id)}" style="color:var(--rose);">Delete</button>` : ''}
                  ${grp.creatorId !== uid ? `<button class="btn ${joined ? 'btn-secondary' : 'btn-primary'} join-group-btn" data-group-id="${esc(grp.id)}">${joined ? 'Leave' : 'Join Group'}</button>` : ''}
                </div>
              </div>
              ${grp.description ? `<div style="margin-top:10px;font-size:14px;">${esc(grp.description)}</div>` : ''}
            </div>
            ${grpAlerts.length ? `
              <div class="card sos-alert-card">
                <div style="font-weight:800;margin-bottom:8px;">Private SOS History (${grpAlerts.length})</div>
                ${grpAlerts.map((al) => `
                  <div style="padding:8px 10px;border-radius:10px;background:rgba(244,63,94,0.1);margin-bottom:6px;display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
                    <div><strong>${esc(al.senderName)}</strong> (${esc(al.status)}): ${esc(al.message)}</div>
                    ${al.status === 'active' && al.senderId === uid ? `<button class="btn btn-secondary stop-sos-btn" data-group-id="${esc(grp.id)}" data-alert-id="${esc(al.id)}" data-status="stopped">Stop</button>` : `<span class="muted">${fmtTime(al.createdAt)}</span>`}
                  </div>
                `).join('')}
              </div>
            ` : ''}
            ${joined ? `
              <div class="card composer">
                <textarea id="postDraftInput" rows="2" placeholder="Share something with ${esc(grp.name)}...">${esc(state.postDraft)}</textarea>
                ${state.postMediaPreview ? `
                  <div class="preview-wrap">
                    ${state.postMediaFile?.type?.startsWith('video/') ? `<video controls src="${esc(state.postMediaPreview)}"></video>` : `<img src="${esc(state.postMediaPreview)}" alt="Preview" />`}
                    <button class="preview-remove-btn" id="clearPostMediaBtn" type="button">${iconSvg('close', 16)}</button>
                  </div>
                ` : ''}
                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px;gap:8px;">
                  <button class="btn btn-secondary" id="pickPostMediaBtn" type="button">${iconSvg('photo', 16)}<span>${state.postMediaFile ? 'Change Photo/Video' : 'Photo / Video'}</span></button>
                  <button class="btn btn-primary" id="publishGroupPostBtn" data-group-id="${esc(grp.id)}" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Uploading...' : 'Post to Group'}</button>
                </div>
              </div>
            ` : ''}
            <div class="feed-list">
              ${grpPosts.length === 0 ? `<div class="card muted">No posts in this group yet.</div>` : grpPosts.map((p) => renderPostCard(p)).join('')}
            </div>
          </div>
        `;
      }
    }

    const groups = state.groups.filter((g) => !q || (g.name || '').toLowerCase().includes(q));
    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card" style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;background:linear-gradient(135deg,rgba(244,63,94,0.16),rgba(124,58,237,0.12));border-color:rgba(244,63,94,0.35);">
          <div>
            <div style="font-weight:800;">🚨 Emergency SOS Hub</div>
            <div class="muted">Alert your private group circles with optional live location.</div>
          </div>
          <button class="btn btn-danger" data-tab="sos">Open SOS Center</button>
        </div>
        <div class="card">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:14px;">
            <div>
              <h3 style="margin:0;">Groups (${groups.length})</h3>
              <div class="muted">Communities & private circles</div>
            </div>
            <button class="btn btn-primary" id="openCreateGroupBtn">${iconSvg('plus', 16)}<span>Create Group</span></button>
          </div>
          ${groups.length === 0 ? `<div class="muted">No groups found. Create the first one!</div>` : `
            <div class="grid-cards">
              ${groups.map((g) => {
                const joined = (g.memberUids || []).includes(uid);
                const isCreator = g.creatorId === uid;
                return `
                  <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:8px;background:var(--elevated);">
                    <div class="open-group-btn" data-group-id="${esc(g.id)}" style="cursor:pointer;">
                      <div style="font-weight:800;font-size:15.5px;">${esc(g.name)} ${g.sosEnabled ? '<span class="badge" style="color:#FB7185;">SOS</span>' : ''}</div>
                      <div class="muted">${esc(g.category || 'Community')} · ${esc(g.privacy)} · ${(g.memberUids || []).length} members</div>
                      <div style="font-size:13px;line-height:1.45;margin-top:6px;">${esc(g.description || '')}</div>
                    </div>
                    <div style="margin-top:auto;padding-top:6px;display:flex;gap:8px;">
                      <button class="btn btn-secondary open-group-btn" data-group-id="${esc(g.id)}" style="flex:1;">Open</button>
                      ${isCreator ? `<span class="badge">Admin</span>` : `<button class="btn ${joined ? 'btn-secondary' : 'btn-primary'} join-group-btn" data-group-id="${esc(g.id)}">${joined ? 'Joined' : 'Join'}</button>`}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      </div>
    `;
  }

  if (state.tab === 'secret_love') return renderSecretLoveFull();

  if (state.tab === 'hub') {
    const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
    const tiles = [
      ['feed', 'home', 'Home Feed', 'Posts & stories'],
      ['friends', 'friends', 'Friends', 'Requests & people'],
      ['groups', 'groups', 'Groups', 'Circles & communities'],
      ['sos', 'shield', 'Emergency SOS', 'Private circle alerts'],
      ['reels', 'reels', 'Reels', 'Short creator clips'],
      ['messenger', 'chat', 'Messenger', 'Direct & group chats'],
      ['notifications', 'bell', 'Notifications', 'All alerts'],
      ['marketplace', 'shop', 'Marketplace', 'Buy & sell locally'],
      ['pages', 'flag', 'Pages', 'Creators & brands'],
      ['events', 'event', 'Events', 'Meetups & RSVPs'],
      ['secret_love', 'heart', 'Secret Love ❤️', 'Vault & Pure Love Quest'],
      ['saved', 'bookmark', 'Saved Items', `${state.savedPostIds.size} bookmarked`],
      ['search', 'search', 'Search', 'Find everything on HELLO'],
      ['settings', 'user', 'Settings & Privacy', 'Incognito, blocked & alerts'],
      ['owner_profile', 'verified', 'HELLO Founder', 'Verified creator card']
    ];
    if (isAdminUser()) {
      tiles.push(['owner_dashboard', 'shield', 'Owner Command', 'Governance, admins & reports']);
    }
    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card open-my-profile" style="display:flex;align-items:center;justify-content:space-between;gap:12px;cursor:pointer;">
          <div style="display:flex;align-items:center;gap:12px;min-width:0;">
            ${avatarHtml(me.displayName, me.photoUrl, 52, true)}
            <div style="min-width:0;">
              <div style="font-weight:800;font-size:17px;">${esc(me.displayName)}</div>
              <div class="muted">${esc(me.handle || 'HELLO Account')}</div>
            </div>
          </div>
          <span class="badge">View Profile</span>
        </div>

        <div class="card" style="display:flex;flex-wrap:wrap;gap:10px;align-items:center;justify-content:space-between;">
          <div>
            <div style="font-weight:700;">HELLO Website, Laptop &amp; Android App</div>
            <div class="muted">Install on your laptop, copy the website link, or download Android APK v1.8.7</div>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-violet open-get-app-btn" id="installLaptopBtnHub">${iconSvg('laptop', 16)}<span>Website &amp; Laptop App</span></button>
            <button class="btn btn-secondary copy-website-url-btn" type="button">${iconSvg('share', 15)}<span>Copy Website</span></button>
            <a class="btn btn-secondary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 16)}<span>Android APK</span></a>
            <button class="btn btn-secondary" id="signOutBtnHub">${iconSvg('logout', 16)}<span>Sign Out</span></button>
          </div>
        </div>

        <h3 style="margin:4px 0 0;">Explore HELLO (A to Z)</h3>
        <div class="hub-grid">
          ${tiles.map(([tKey, ic, title, sub]) => `
            <button class="hub-tile" data-tab="${tKey}">
              ${iconSvg(ic, 24)}
              <div style="font-weight:800;font-size:14.5px;margin-top:2px;">${title}</div>
              <div class="muted" style="font-size:12px;">${sub}</div>
            </button>
          `).join('')}
        </div>
      </div>
    `;
  }

  if (state.tab === 'marketplace') {
    const cats = ['All', 'Electronics', 'Vehicles', 'Home', 'Fashion', 'Books', 'Services', 'Other'];
    let items = state.marketplace.filter((m) => {
      if (state.blockedUids.has(m.sellerId)) return false;
      const catOk = state.marketCat === 'All' || (m.category || '').toLowerCase() === state.marketCat.toLowerCase();
      const statusOk =
        state.marketStatus === 'All' ||
        (state.marketStatus === 'Available' && m.status !== 'sold') ||
        (state.marketStatus === 'Sold' && m.status === 'sold') ||
        (state.marketStatus === 'Mine' && m.sellerId === uid);
      const qOk = !q || (m.title || '').toLowerCase().includes(q) || (m.description || '').toLowerCase().includes(q) || (m.locationText || '').toLowerCase().includes(q);
      return catOk && statusOk && qOk;
    });
    if (state.marketSort === 'Price ↑') {
      items = [...items].sort((a, b) => Number(a.price || 0) - Number(b.price || 0));
    } else if (state.marketSort === 'Price ↓') {
      items = [...items].sort((a, b) => Number(b.price || 0) - Number(a.price || 0));
    }

    return `
      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
          <div>
            <h3 style="margin:0;">HELLO Marketplace (${items.length})</h3>
            <div class="muted">Buy &amp; sell locally — click any listing for full details or Message Seller</div>
          </div>
          <button class="btn btn-primary" id="openCreateListingBtn">${iconSvg('plus', 16)}<span>Sell Item</span></button>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          <input class="field" id="tabLocalSearchInput" type="search" placeholder="Search items, locations..." value="${esc(state.search)}" style="flex:1;min-width:180px;" />
          <select class="select-field" id="marketSortSelect" style="width:auto;">
            ${['Newest', 'Price ↑', 'Price ↓'].map((s) => `<option ${state.marketSort === s ? 'selected' : ''}>${s}</option>`).join('')}
          </select>
        </div>
        <div class="chip-row">
          ${cats.map((c) => `<button class="filter-chip ${state.marketCat === c ? 'active' : ''}" data-market-cat="${c}">${c}</button>`).join('')}
        </div>
        <div class="chip-row">
          ${['All', 'Available', 'Sold', 'Mine'].map((st) => `<button class="filter-chip ${state.marketStatus === st ? 'active' : ''}" data-market-status="${st}">${st}</button>`).join('')}
        </div>
        ${items.length === 0 ? `<div class="muted">No Marketplace listings match your filters.</div>` : `
          <div class="grid-cards">
            ${items.map((item) => {
              const isMine = item.sellerId === uid || isAdminUser();
              return `
                <div class="card" style="padding:12px;display:flex;flex-direction:column;gap:6px;background:var(--elevated);">
                  <div class="open-listing-detail-btn" data-item-id="${esc(item.id)}" style="cursor:pointer;">
                    ${item.imageUri ? mediaImgHtml(item.imageUri, 'market-img', item.title) : `<div class="market-img" style="display:flex;align-items:center;justify-content:center;color:var(--muted);">No photo</div>`}
                    <div style="display:flex;justify-content:space-between;align-items:center;">
                      <span style="font-weight:800;font-size:16px;color:var(--primary);font-variant-numeric:tabular-nums;">${esc(item.currency || '$')}${Number(item.price || 0).toFixed(2)}</span>
                      <span class="badge">${esc(item.status || 'available')}</span>
                    </div>
                    <div style="font-weight:700;margin-top:4px;">${esc(item.title)}</div>
                    <div class="muted" style="font-size:12px;">${esc(item.category || 'Other')} · ${esc(item.condition || 'Good')} · ${esc(item.locationText || 'Local')}</div>
                  </div>
                  ${isMine ? `
                    <div style="display:flex;gap:6px;margin-top:auto;padding-top:6px;">
                      <button class="btn btn-secondary open-edit-listing-btn" data-item-id="${esc(item.id)}" style="padding:6px 10px;">Edit</button>
                      <button class="btn btn-secondary toggle-sold-btn" data-item-id="${esc(item.id)}" style="flex:1;">${item.status === 'sold' ? 'Available' : 'Sold'}</button>
                      <button class="btn btn-danger delete-mkt-btn" data-item-id="${esc(item.id)}">${iconSvg('trash', 15)}</button>
                    </div>
                  ` : `
                    <div style="display:flex;gap:6px;margin-top:auto;padding-top:6px;">
                      <button class="btn btn-primary market-msg-btn" data-item-id="${esc(item.id)}" style="flex:1;">${iconSvg('chat', 15)}<span>Message</span></button>
                      <button class="btn btn-secondary open-listing-detail-btn" data-item-id="${esc(item.id)}">View</button>
                    </div>
                  `}
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (state.tab === 'pages') {
    if (state.activePageId) {
      const pg = state.pages.find((p) => p.id === state.activePageId);
      if (pg) {
        const following = (pg.followerUids || []).includes(uid);
        const isPageAdmin = pg.creatorId === uid || (pg.adminUids || []).includes(uid) || isAdminUser();
        const pagePosts = state.posts.filter((p) => p.pageId === pg.id);
        return `
          <div style="display:flex;flex-direction:column;gap:14px;">
            <div class="card profile-clean-card">
              <div class="profile-clean-cover" style="height:165px;">
                ${pg.coverUrl ? mediaImgHtml(pg.coverUrl, '', 'Page cover') : ''}
                <button class="icon-btn" id="backToPagesBtn" title="Back to Pages" style="position:absolute;top:12px;left:12px;background:rgba(11,14,30,0.75);color:#fff;">${iconSvg('back', 18)}</button>
              </div>
              <div class="profile-clean-body">
                <div class="profile-clean-avatar-wrap">${avatarHtml(pg.name, pg.avatarUrl, 82, false)}</div>
                <h2 style="margin:8px 0 2px;">${esc(pg.name)}</h2>
                <div class="muted">${esc(pg.category || 'Creator')} · ${pg.followersCount ?? (pg.followerUids || []).length} followers</div>
                ${pg.bio ? `<div style="margin-top:8px;font-size:14px;max-width:520px;">${esc(pg.bio)}</div>` : ''}
                <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:center;margin-top:12px;">
                  <button class="btn ${following ? 'btn-secondary' : 'btn-primary'} follow-page-btn" data-page-id="${esc(pg.id)}">${following ? 'Following ✓' : 'Follow Page'}</button>
                  ${isPageAdmin ? `<button class="btn btn-secondary open-edit-page-btn" data-page-id="${esc(pg.id)}">Edit Page</button>` : ''}
                  ${isPageAdmin ? `<button class="btn btn-secondary manage-page-btn" data-page-id="${esc(pg.id)}">Manage Members</button>` : ''}
                  ${(pg.creatorId === uid || isAdminUser()) ? `<button class="btn btn-danger delete-page-btn" data-page-id="${esc(pg.id)}">${iconSvg('trash', 15)}<span>Delete</span></button>` : ''}
                </div>
              </div>
            </div>

            ${isPageAdmin ? `
              <div class="card composer">
                <textarea id="postDraftInput" rows="2" placeholder="Publish an update as ${esc(pg.name)}...">${esc(state.postDraft)}</textarea>
                ${state.postMediaPreview ? `
                  <div class="preview-wrap">
                    ${state.postMediaFile?.type?.startsWith('video/') ? `<video controls src="${esc(state.postMediaPreview)}"></video>` : `<img src="${esc(state.postMediaPreview)}" alt="Preview" />`}
                    <button class="preview-remove-btn" id="clearPostMediaBtn" type="button">${iconSvg('close', 16)}</button>
                  </div>
                ` : ''}
                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:10px;gap:8px;">
                  <button class="btn btn-secondary" id="pickPostMediaBtn" type="button">${iconSvg('photo', 16)}<span>${state.postMediaFile ? 'Change Media' : 'Photo / Video'}</span></button>
                  <button class="btn btn-primary" id="publishPagePostBtn" data-page-id="${esc(pg.id)}" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Uploading...' : 'Post to Page'}</button>
                </div>
              </div>
            ` : ''}

            <div class="feed-list">
              ${pagePosts.length === 0 ? `<div class="card muted">No posts published on ${esc(pg.name)} yet.</div>` : pagePosts.map((p) => renderPostCard(p)).join('')}
            </div>
          </div>
        `;
      }
    }

    const pageCats = ['All', 'Digital Creator', 'Creator', 'Community', 'Business', 'Music', 'Education', 'Tech', 'Sports'];
    const pages = state.pages.filter((pg) => {
      const catOk = state.pageCatFilter === 'All' || (pg.category || '').toLowerCase() === state.pageCatFilter.toLowerCase();
      const qOk = !q || (pg.name || '').toLowerCase().includes(q) || (pg.category || '').toLowerCase().includes(q) || (pg.bio || '').toLowerCase().includes(q);
      return catOk && qOk;
    });
    return `
      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
          <div>
            <h3 style="margin:0;">HELLO Pages (${pages.length})</h3>
            <div class="muted">Creators, communities &amp; brands</div>
          </div>
          <button class="btn btn-primary" id="openCreatePageBtn">${iconSvg('plus', 16)}<span>Create Page</span></button>
        </div>
        <input class="field" id="tabLocalSearchInput" type="search" placeholder="Search Pages by name or bio..." value="${esc(state.search)}" />
        <div class="chip-row">
          ${pageCats.map((c) => `<button class="filter-chip ${state.pageCatFilter === c ? 'active' : ''}" data-page-cat="${c}">${c}</button>`).join('')}
        </div>
        ${pages.length === 0 ? `<div class="muted">No Pages match your filter yet.</div>` : `
          <div class="grid-cards">
            ${pages.map((pg) => {
              const following = (pg.followerUids || []).includes(uid);
              const isMine = pg.creatorId === uid || (pg.adminUids || []).includes(uid) || isAdminUser();
              return `
                <div class="card" style="padding:12px;display:flex;flex-direction:column;gap:8px;background:var(--elevated);">
                  <div class="open-page-detail-btn" data-page-id="${esc(pg.id)}" style="cursor:pointer;">
                    <div class="page-card-banner">${pg.coverUrl ? mediaImgHtml(pg.coverUrl, '', pg.name) : ''}</div>
                    <div style="display:flex;align-items:center;gap:10px;margin-top:8px;">
                      ${avatarHtml(pg.name, pg.avatarUrl, 40, false)}
                      <div style="min-width:0;">
                        <div style="font-weight:800;font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(pg.name)}</div>
                        <div class="muted" style="font-size:12px;">${esc(pg.category)} · ${pg.followersCount ?? (pg.followerUids || []).length} followers</div>
                      </div>
                    </div>
                    ${pg.bio ? `<div style="font-size:13px;margin-top:6px;line-height:1.4;">${esc(pg.bio.slice(0, 120))}</div>` : ''}
                  </div>
                  <div style="display:flex;gap:6px;margin-top:auto;padding-top:4px;">
                    <button class="btn ${following ? 'btn-secondary' : 'btn-primary'} follow-page-btn" data-page-id="${esc(pg.id)}" style="flex:1;">${following ? 'Following ✓' : 'Follow'}</button>
                    <button class="btn btn-secondary open-page-detail-btn" data-page-id="${esc(pg.id)}">Open</button>
                    ${isMine ? `<button class="btn btn-secondary open-edit-page-btn" data-page-id="${esc(pg.id)}">Edit</button>` : ''}
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (state.tab === 'events') {
    const events = state.events.filter((ev) => !q || (ev.title || '').toLowerCase().includes(q) || (ev.locationName || '').toLowerCase().includes(q));
    return `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:14px;">
          <div>
            <h3 style="margin:0;">HELLO Events (${events.length})</h3>
            <div class="muted">Meetups & live happenings</div>
          </div>
          <button class="btn btn-primary" id="openCreateEventBtn">${iconSvg('plus', 16)}<span>Create Event</span></button>
        </div>
        ${events.length === 0 ? `<div class="muted">No Events posted yet.</div>` : `
          <div class="grid-cards">
            ${events.map((ev) => {
              const going = (ev.goingUids || []).includes(uid);
              const interested = (ev.interestedUids || []).includes(uid);
              const isMine = ev.creatorId === uid || isAdminUser();
              return `
                <div class="card" style="padding:14px;display:flex;flex-direction:column;gap:8px;background:var(--elevated);">
                  <div style="display:flex;justify-content:space-between;align-items:center;">
                    <div style="font-weight:800;font-size:15.5px;">${esc(ev.title)}</div>
                    ${isMine ? `<button class="icon-btn delete-event-btn" data-event-id="${esc(ev.id)}" style="width:28px;height:28px;color:var(--rose);">${iconSvg('trash', 15)}</button>` : ''}
                  </div>
                  <div class="badge">${esc(ev.eventDateText)} · ${esc(ev.eventTimeText)}</div>
                  <div class="muted">${esc(ev.locationName)}</div>
                  ${ev.description ? `<div style="font-size:13px;">${esc(ev.description)}</div>` : ''}
                  <div style="display:flex;gap:8px;margin-top:auto;">
                    <button class="btn ${going ? 'btn-primary' : 'btn-secondary'} rsvp-going-btn" data-event-id="${esc(ev.id)}" style="flex:1;">Going (${(ev.goingUids || []).length})</button>
                    <button class="btn ${interested ? 'btn-violet' : 'btn-secondary'} rsvp-int-btn" data-event-id="${esc(ev.id)}" style="flex:1;">Interested (${(ev.interestedUids || []).length})</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (state.tab === 'saved') {
    const savedPosts = state.posts.filter((p) => state.savedPostIds.has(p.id));
    const savedReels = state.reels.filter((r) => state.savedPostIds.has(r.id));
    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card">
          <h3 style="margin:0;">Saved Items (${savedPosts.length + savedReels.length})</h3>
          <div class="muted">Posts and Reels you bookmarked across HELLO Android & Desktop</div>
        </div>
        ${savedReels.length ? `
          <div class="card">
            <h4 style="margin:0 0 10px;">Saved Reels (${savedReels.length})</h4>
            <div class="grid-cards">
              ${savedReels.map((r) => `
                <div class="card" style="padding:12px;background:var(--elevated);">
                  ${r.videoUri ? mediaVideoHtml(r.videoUri, '', 'controls playsinline style="width:100%;height:200px;border-radius:10px;background:#000;"') : ''}
                  <div style="font-weight:700;margin-top:6px;">${esc(r.authorName)}</div>
                  <div style="font-size:13px;">${esc(r.caption)}</div>
                  <button class="btn btn-secondary save-reel-btn" data-reel-id="${esc(r.id)}" style="margin-top:8px;">Remove Bookmark</button>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        ${savedPosts.length === 0 && savedReels.length === 0 ? `<div class="card muted">No saved items yet. Tap the bookmark icon on any post or Reel to save it here.</div>` : savedPosts.map((p) => renderPostCard(p)).join('')}
      </div>
    `;
  }

  if (state.tab === 'notifications') {
    const unreadCount = state.notifications.filter((n) => !n.isRead).length;
    const list = state.notifFilter === 'unread' ? state.notifications.filter((n) => !n.isRead) : state.notifications;
    return `
      <div class="card" style="display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
          <div>
            <h3 style="margin:0;">Notifications</h3>
            <div class="muted">${unreadCount ? `${unreadCount} unread alerts` : 'All caught up across HELLO'}</div>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            ${unreadCount ? `<button class="btn btn-secondary" id="markAllReadBtn">${iconSvg('check', 16)}<span>Mark all read</span></button>` : ''}
            ${state.notifications.length ? `<button class="btn btn-secondary" id="clearAllNotifsBtn">${iconSvg('trash', 15)}<span>Clear All</span></button>` : ''}
          </div>
        </div>
        <div class="chip-row">
          <button class="filter-chip ${state.notifFilter === 'all' ? 'active' : ''}" data-notif-filter="all">All (${state.notifications.length})</button>
          <button class="filter-chip ${state.notifFilter === 'unread' ? 'active' : ''}" data-notif-filter="unread">Unread (${unreadCount})</button>
        </div>
        ${list.length === 0 ? `<div class="muted">No notifications here.</div>` : list.slice(0, 40).map((n) => `
          <div class="open-notif-item" data-notif-id="${esc(n.id)}" data-notif-type="${esc(n.type || '')}" data-target-id="${esc(n.targetId || '')}" style="padding:12px;border-radius:14px;background:${n.isRead ? 'var(--elevated)' : 'var(--primary-soft)'};border:1px solid var(--border);cursor:pointer;display:flex;justify-content:space-between;align-items:center;gap:10px;">
            <div>
              <div style="font-weight:700;font-size:14px;">${esc(n.title)}</div>
              <div style="font-size:13px;margin-top:2px;">${esc(n.body)}</div>
              <div class="muted" style="font-size:11px;margin-top:4px;">${fmtTime(n.createdAt)}</div>
            </div>
            <button class="icon-btn del-notif-btn" data-notif-id="${esc(n.id)}" style="width:32px;height:32px;" title="Remove">${iconSvg('close', 16)}</button>
          </div>
        `).join('')}
      </div>
    `;
  }

  if (state.tab === 'search') {
    const peopleHits = state.users.filter((u) => !state.blockedUids.has(u.userId || u.id) && (!q || (u.displayName || '').toLowerCase().includes(q) || (u.handle || '').toLowerCase().includes(q))).slice(0, 12);
    const postHits = state.posts.filter((p) => !q || (p.content || '').toLowerCase().includes(q) || (p.authorName || '').toLowerCase().includes(q)).slice(0, 12);
    const groupHits = state.groups.filter((g) => !q || (g.name || '').toLowerCase().includes(q)).slice(0, 8);
    const mktHits = state.marketplace.filter((m) => !q || (m.title || '').toLowerCase().includes(q) || (m.description || '').toLowerCase().includes(q)).slice(0, 8);
    const showAll = state.searchScope === 'all';

    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <div style="display:flex;align-items:center;gap:10px;background:var(--elevated);border:1px solid var(--border);border-radius:999px;padding:8px 16px;">
            ${iconSvg('search', 18)}
            <input id="globalSearchInput" type="search" placeholder="Search people, posts, groups, marketplace..." value="${esc(state.search)}" style="flex:1;background:transparent;border:none;outline:none;color:var(--text);font-size:14.5px;" />
            ${q ? `<button class="btn btn-secondary" id="saveRecentSearchBtn" style="padding:4px 10px;min-height:28px;font-size:11.5px;">Save</button>` : ''}
          </div>
          <div class="chip-row">
            ${[['all', 'All'], ['people', 'People'], ['posts', 'Posts'], ['groups', 'Groups'], ['marketplace', 'Marketplace']].map(([k, lbl]) => `
              <button class="filter-chip ${state.searchScope === k ? 'active' : ''}" data-search-scope="${k}">${lbl}</button>
            `).join('')}
          </div>
          ${state.recentSearches.length ? `
            <div style="display:flex;gap:6px;flex-wrap:wrap;align-items:center;">
              <span class="muted" style="font-size:11.5px;">Recent:</span>
              ${state.recentSearches.map((term) => `<button class="filter-chip recent-search-pill" data-term="${esc(term)}" style="padding:3px 10px;font-size:11.5px;">${esc(term)}</button>`).join('')}
              <button class="msg-mini-btn" id="clearRecentSearchBtn">Clear</button>
            </div>
          ` : ''}
        </div>
        ${(showAll || state.searchScope === 'people') && peopleHits.length ? `
          <div class="card">
            <h4 style="margin:0 0 10px;">People (${peopleHits.length})</h4>
            <div style="display:flex;flex-direction:column;gap:6px;">
              ${peopleHits.map((u) => `
                <div class="side-item open-user-profile" data-uid="${esc(u.userId || u.id)}">
                  <span class="side-item-left">${avatarHtml(u.displayName, u.photoUrl, 36, u.isOnline)}<span>${esc(u.displayName)} <span class="muted">${esc(u.handle || '')}</span></span></span>
                  <button class="btn btn-primary msg-person-btn" data-uid="${esc(u.userId || u.id)}" style="min-height:32px;padding:4px 12px;">Message</button>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        ${(showAll || state.searchScope === 'groups') && groupHits.length ? `
          <div class="card">
            <h4 style="margin:0 0 10px;">Groups (${groupHits.length})</h4>
            <div style="display:flex;flex-direction:column;gap:6px;">
              ${groupHits.map((g) => `
                <button class="side-item open-group-btn" data-group-id="${esc(g.id)}">
                  <span>${esc(g.name)}</span>
                  <span class="muted">${(g.memberUids || []).length} members</span>
                </button>
              `).join('')}
            </div>
          </div>
        ` : ''}
        ${(showAll || state.searchScope === 'marketplace') && mktHits.length ? `
          <div class="card">
            <h4 style="margin:0 0 10px;">Marketplace (${mktHits.length})</h4>
            <div class="grid-cards">
              ${mktHits.map((m) => `
                <div class="card" style="padding:10px;background:var(--elevated);">
                  <div style="font-weight:700;">${esc(m.title)}</div>
                  <div class="muted">${esc(m.currency || '$')}${Number(m.price || 0).toFixed(2)} · ${esc(m.locationText || '')}</div>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        ${(showAll || state.searchScope === 'posts') ? postHits.map((p) => renderPostCard(p)).join('') : ''}
      </div>
    `;
  }

  if (state.tab === 'profile') {
    const viewUid = state.profileViewUid || uid;
    const isMe = viewUid === uid;
    const person = isMe ? (state.me || {}) : (state.usersById[viewUid] || {});
    const myPosts = state.posts.filter((p) => p.authorId === viewUid);
    const myReels = state.reels.filter((r) => r.authorId === viewUid);
    const myPhotos = myPosts.filter((p) => p.mediaType === 'image' && p.mediaUri);
    const myHighlights = state.stories.filter((s) => s.authorId === viewUid && s.isSavedHighlight);
    const savedPosts = isMe ? state.posts.filter((p) => state.savedPostIds.has(p.id)) : [];
    const friendSet = new Set(state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== uid));
    const isFriend = friendSet.has(viewUid);
    const isFollowing = state.follows.some((f) => f.targetUid === viewUid);
    const isPending = state.friendRequests.some((r) => r.fromUid === uid && r.toUid === viewUid && r.status === 'pending');
    const pTab = state.profileTab || 'posts';

    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="card profile-clean-card">
          <div class="profile-clean-cover">
            ${person.coverUrl ? mediaImgHtml(person.coverUrl, 'open-lightbox', 'Cover', `data-lightbox-uri="${esc(person.coverUrl)}"`) : ''}
          </div>
          <div class="profile-clean-body">
            <div class="profile-clean-avatar-wrap">
              ${avatarHtml(person.displayName || state.user.displayName, person.photoUrl || (isMe ? state.user.photoURL : ''), 92, Boolean(person.isOnline && person.showOnlineStatus !== false))}
            </div>
            <h2 style="margin:10px 0 2px;display:flex;align-items:center;justify-content:center;gap:6px;">
              <span>${esc(person.displayName || 'Hello Member')}</span>
              ${person.isVerifiedOwner ? `<span class="verified-badge">${iconSvg('verified', 18)}</span>` : ''}
            </h2>
            ${person.handle ? `<div class="muted" style="font-size:13px;">${esc(person.handle)}</div>` : ''}
            ${person.bio ? `<div style="margin-top:8px;font-size:14px;max-width:480px;line-height:1.45;">${esc(person.bio)}</div>` : ''}

            <div class="profile-stats-bar">
              <div class="profile-stat-pill"><strong>${person.friendsCount || 0}</strong><span>Friends</span></div>
              <div class="profile-stat-pill"><strong>${person.followersCount || 0}</strong><span>Followers</span></div>
              <div class="profile-stat-pill"><strong>${myPosts.length}</strong><span>Posts</span></div>
              <div class="profile-stat-pill"><strong>${myReels.length}</strong><span>Reels</span></div>
            </div>

            <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:center;margin-top:14px;width:100%;">
              ${isMe ? `
                <button class="btn btn-primary" id="openEditProfileModalBtn" style="flex:1;max-width:220px;">Edit Profile</button>
                <button class="btn btn-secondary" data-tab="settings" style="flex:1;max-width:220px;">Settings &amp; Privacy</button>
              ` : `
                <button class="btn btn-primary msg-person-btn" data-uid="${esc(viewUid)}">${iconSvg('chat', 16)}<span>Message</span></button>
                ${isFriend ? `<span class="badge">Friends ✓</span>` : (isPending ? `<span class="badge">Requested</span>` : `<button class="btn btn-secondary add-friend-btn" data-uid="${esc(viewUid)}">Add Friend</button>`)}
                <button class="btn btn-secondary follow-btn" data-uid="${esc(viewUid)}">${isFollowing ? 'Following ✓' : 'Follow'}</button>
                <button class="btn btn-secondary report-open-btn" data-rtype="user" data-rid="${esc(viewUid)}">Report</button>
                <button class="btn btn-danger block-user-btn" data-uid="${esc(viewUid)}" data-name="${esc(person.displayName || 'User')}">Block</button>
              `}
            </div>
          </div>
        </div>

        ${myHighlights.length ? `
          <div class="card" style="padding:12px 14px;">
            <div style="font-weight:700;font-size:13px;margin-bottom:8px;">Story Highlights</div>
            <div class="highlights-row">
              ${myHighlights.map((st) => `
                <button class="highlight-chip view-story-btn" data-story-id="${esc(st.id)}">
                  <div class="highlight-ring">
                    <div class="highlight-inner" style="background:${esc(st.backgroundHex || '#1E1B4B')};">
                      ${st.mediaUri && st.mediaType !== 'video' ? `<img referrerpolicy="no-referrer" data-media-uri="${esc(st.mediaUri)}" src="${esc(mediaSrc(st.mediaUri))}" alt="Highlight" />` : esc((st.caption || 'Story').slice(0, 8))}
                    </div>
                  </div>
                  <span style="font-size:11px;max-width:70px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(st.caption || 'Highlight')}</span>
                </button>
              `).join('')}
            </div>
          </div>
        ` : ''}

        <div class="chip-row">
          <button class="filter-chip ${pTab === 'posts' ? 'active' : ''}" data-prof-tab="posts">Posts (${myPosts.length})</button>
          <button class="filter-chip ${pTab === 'reels' ? 'active' : ''}" data-prof-tab="reels">Reels (${myReels.length})</button>
          <button class="filter-chip ${pTab === 'photos' ? 'active' : ''}" data-prof-tab="photos">Photos (${myPhotos.length})</button>
          ${isMe ? `<button class="filter-chip ${pTab === 'saved' ? 'active' : ''}" data-prof-tab="saved">Saved (${savedPosts.length})</button>` : ''}
        </div>
        ${pTab === 'posts' ? (myPosts.length ? myPosts.map((p) => renderPostCard(p)).join('') : `<div class="card muted">No posts yet.</div>`) : ''}
        ${pTab === 'reels' ? (myReels.length ? `<div class="grid-cards">${myReels.map((r) => `<div class="card" style="padding:10px;">${r.videoUri ? mediaVideoHtml(r.videoUri, '', 'controls playsinline style="width:100%;height:210px;border-radius:10px;background:#000;"') : ''}<div style="margin-top:6px;font-size:13px;">${esc(r.caption)}</div></div>`).join('')}</div>` : `<div class="card muted">No Reels yet.</div>`) : ''}
        ${pTab === 'photos' ? (myPhotos.length ? `<div class="card"><div class="photos-square-grid">${myPhotos.map((p) => `<div class="photo-square-tile open-lightbox" data-lightbox-uri="${esc(p.mediaUri)}">${mediaImgHtml(p.mediaUri, '', 'Photo')}</div>`).join('')}</div></div>` : `<div class="card muted">No photos yet.</div>`) : ''}
        ${pTab === 'saved' && isMe ? (savedPosts.length ? savedPosts.map((p) => renderPostCard(p)).join('') : `<div class="card muted">No saved posts yet.</div>`) : ''}
      </div>
    `;
  }

  // Home Feed (keeps Group and Page posts inside their own spaces)
  let posts = state.posts.filter((p) => !p.groupId && !p.pageId && (!q || (p.content || '').toLowerCase().includes(q) || (p.authorName || '').toLowerCase().includes(q)));
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
        <div style="display:flex;gap:10px;align-items:flex-start;margin-bottom:8px;">
          ${avatarHtml(state.me?.displayName || state.user.displayName, state.me?.photoUrl || state.user.photoURL, 42, true)}
          <textarea id="postDraftInput" rows="2" placeholder="What's on your mind, ${esc((state.me?.displayName || 'friend').split(' ')[0])}?">${esc(state.postDraft)}</textarea>
        </div>
        ${state.postMediaPreview ? `
          <div class="preview-wrap">
            ${state.postMediaFile?.type?.startsWith('video/') ? `<video controls src="${esc(state.postMediaPreview)}"></video>` : `<img src="${esc(state.postMediaPreview)}" alt="Selected media preview" />`}
            <button class="preview-remove-btn" id="clearPostMediaBtn" type="button" title="Remove">${iconSvg('close', 16)}</button>
          </div>
        ` : ''}
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;margin-top:6px;">
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
            <button class="btn btn-secondary" id="pickPostMediaBtn" type="button" ${state.isUploading ? 'disabled' : ''}>
              ${iconSvg('photo', 16)}
              <span>${state.postMediaFile ? 'Change Photo / Video' : 'Photo / Video'}</span>
            </button>
            <select class="select-field" id="postPrivacySelect" style="width:auto;padding:7px 12px;border-radius:999px;font-size:12.5px;">
              <option value="public" ${state.postPrivacy === 'public' ? 'selected' : ''}>Public</option>
              <option value="friends" ${state.postPrivacy === 'friends' ? 'selected' : ''}>Friends</option>
              <option value="only_me" ${state.postPrivacy === 'only_me' ? 'selected' : ''}>Only Me</option>
            </select>
          </div>
          <button class="btn btn-primary" id="publishPostBtn" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? (state.uploadLabel || 'Uploading...') : 'Post'}</button>
        </div>
      </div>

      <div class="chip-row">
        ${[['latest', 'Latest'], ['top', 'Top Posts'], ['friends', 'Friends']].map(([k, lbl]) => `
          <button class="filter-chip ${state.feedSort === k ? 'active' : ''}" data-feed-sort="${k}">${lbl}</button>
        `).join('')}
      </div>

      <div class="feed-list">
        ${posts.length === 0 ? `<div class="card muted">No posts match this feed filter yet.</div>` : posts.map((p) => renderPostCard(p)).join('')}
      </div>
    </div>
  `;
}

function renderModals() {
  const uid = state.user?.uid;
  const liveCall = uid ? state.activeCalls[0] : null;
  let html = '';

  if (state.lightboxUri) {
    const src = mediaSrc(state.lightboxUri);
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div style="position:relative;max-width:92vw;max-height:90vh;">
          <button class="icon-btn close-modals-btn" style="position:absolute;top:10px;right:10px;z-index:2;background:rgba(11,14,30,0.85);color:#fff;">${iconSvg('close', 20)}</button>
          <img referrerpolicy="no-referrer" data-media-uri="${esc(state.lightboxUri)}" src="${esc(src)}" alt="Full photo" style="max-width:92vw;max-height:88vh;border-radius:16px;display:block;object-fit:contain;background:#000;" />
        </div>
      </div>
    `;
  } else if (liveCall) {
    const incoming = liveCall.callerId !== uid && liveCall.status === 'ringing';
    html += `
      <div class="modal-backdrop">
        <div class="modal-card" style="text-align:center;align-items:center;padding:28px 22px;background:linear-gradient(160deg,#13152A,#1E1B4B);">
          <span class="badge">${liveCall.callType === 'video' ? 'HELLO Video Call' : 'HELLO Audio Call'}</span>
          <div style="margin:10px 0;">${avatarHtml(liveCall.callerId === uid ? (liveCall.conversationTitle || 'Call') : liveCall.callerName, '', 74, true)}</div>
          <h2 style="margin:0;color:#fff;">${esc(liveCall.callerId === uid ? (liveCall.conversationTitle || 'Calling...') : liveCall.callerName)}</h2>
          <div class="muted" style="color:#CBD5E1;">${incoming ? 'Incoming call — ringing on HELLO...' : (liveCall.status === 'active' ? 'Call connected (audio/video active in mobile app)' : 'Ringing...')}</div>
          <div style="display:flex;gap:14px;margin-top:14px;">
            ${incoming ? `
              <button class="btn btn-emerald answer-call-btn" data-call-id="${esc(liveCall.id)}" style="min-height:46px;padding:10px 22px;">${iconSvg('call', 18)}<span>Answer</span></button>
              <button class="btn btn-danger decline-call-btn" data-call-id="${esc(liveCall.id)}" style="min-height:46px;padding:10px 22px;">${iconSvg('callEnd', 18)}<span>Decline</span></button>
            ` : `
              <button class="btn btn-danger end-call-btn" data-call-id="${esc(liveCall.id)}" style="min-height:46px;padding:10px 24px;">${iconSvg('callEnd', 18)}<span>End Call</span></button>
            `}
          </div>
        </div>
      </div>
    `;
  } else if (state.activeStory) {
    const s = state.activeStory;
    const author = resolveAuthor(s.authorId, s.authorName, s.authorAvatar);
    const hex = /^#[0-9A-Fa-f]{6}$/.test(s.backgroundHex || '') ? s.backgroundHex : '#1E1B4B';
    const storyReacts = state.storyReactionsById[s.id] || [];
    const isMyStory = s.authorId === uid || isAdminUser();
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card story-viewer-card">
          <div style="padding:14px;display:flex;justify-content:space-between;align-items:center;background:rgba(11,14,30,0.88);">
            <div style="display:flex;align-items:center;gap:10px;">
              ${avatarHtml(author.name, author.avatar, 36, false)}
              <div><div style="font-weight:700;">${esc(author.name)}</div><div class="muted" style="font-size:11px;">${fmtTime(s.createdAt)} · ${s.viewersCount || 0} views</div></div>
            </div>
            <div style="display:flex;gap:6px;align-items:center;">
              ${s.authorId === uid ? `<button class="btn btn-secondary" id="toggleStoryHighlightBtn" style="padding:4px 9px;min-height:28px;font-size:11px;">${s.isSavedHighlight ? '★ Highlight' : 'Save Highlight'}</button>` : ''}
              ${isMyStory ? `<button class="icon-btn" id="deleteStoryBtn" style="width:30px;height:30px;color:#FB7185;">${iconSvg('trash', 15)}</button>` : ''}
              <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
            </div>
          </div>
          <div style="min-height:340px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:${hex};padding:18px;text-align:center;">
            ${s.mediaUri && s.mediaType === 'video' ? mediaVideoHtml(s.mediaUri, '', 'controls autoplay playsinline style="max-width:100%;max-height:380px;border-radius:12px;"') : ''}
            ${s.mediaUri && s.mediaType !== 'video' ? mediaImgHtml(s.mediaUri, '', 'Story', 'style="max-width:100%;max-height:380px;border-radius:12px;"') : ''}
            ${s.caption ? `<div style="margin-top:12px;font-size:16px;font-weight:700;color:#fff;">${esc(s.caption)}</div>` : ''}
          </div>
          <div style="padding:12px;background:#111428;display:flex;flex-direction:column;gap:8px;">
            ${storyReacts.length ? `<div style="font-size:12px;color:#CBD5E1;max-height:70px;overflow-y:auto;">${storyReacts.map((sr) => `<div><strong>${esc(sr.userName)}</strong> ${esc(sr.emoji)} ${sr.replyText ? esc(sr.replyText) : ''}</div>`).join('')}</div>` : ''}
            <div style="display:flex;justify-content:space-around;gap:6px;">
              ${STORY_REACTIONS.map((em) => `<button type="button" class="reaction-emoji-btn react-story-btn" data-emoji="${em}">${em}</button>`).join('')}
            </div>
            <div style="display:flex;gap:6px;">
              <input class="field" id="storyReplyInput" placeholder="Reply to story..." style="background:#1B203B;color:#fff;" />
              <button class="btn btn-primary" id="sendStoryReplyBtn">Send</button>
            </div>
          </div>
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
          <input class="field" id="storyCaptionInput" placeholder="Write a story caption..." value="${esc(state.storyDraftCaption)}" />
          <select class="select-field" id="storyPrivacySelect">
            <option value="public" ${state.storyPrivacy === 'public' ? 'selected' : ''}>Public Story</option>
            <option value="friends" ${state.storyPrivacy === 'friends' ? 'selected' : ''}>Friends Only</option>
          </select>
          <div style="display:flex;gap:8px;align-items:center;">
            <span class="muted" style="font-size:12px;">Theme:</span>
            ${['#1E1B4B', '#FF5A5F', '#7C3AED', '#0F766E', '#E11D48'].map((hex) => `
              <button type="button" class="story-bg-pick" data-hex="${hex}" style="width:26px;height:26px;border-radius:999px;border:${state.storyBgHex === hex ? '2.5px solid #fff' : '1px solid var(--border)'};background:${hex};cursor:pointer;"></button>
            `).join('')}
          </div>
          ${state.storyMediaPreview ? `
            <div class="preview-wrap">
              ${state.storyMediaFile?.type?.startsWith('video/') ? `<video controls src="${esc(state.storyMediaPreview)}"></video>` : `<img src="${esc(state.storyMediaPreview)}" alt="Story preview" />`}
              <button class="preview-remove-btn" id="clearStoryMediaBtn" type="button">${iconSvg('close', 16)}</button>
            </div>
          ` : ''}
          <button class="btn btn-secondary" id="pickStoryMediaBtn" type="button">${iconSvg('photo', 16)}<span>${state.storyMediaFile ? 'Change Photo / Video' : 'Choose Photo / Video (Optional)'}</span></button>
          <button class="btn btn-primary" id="submitStoryBtn" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Publishing...' : 'Publish Story'}</button>
        </div>
      </div>
    `;
  } else if (state.showCreateReelModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Create Reel</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <textarea class="field" id="reelCaptionInput" rows="2" placeholder="Write a caption for your Reel...">${esc(state.reelDraftCaption)}</textarea>
          <select class="select-field" id="reelCatSelect">
            ${['Trending', 'Comedy', 'Music', 'Gaming', 'Tech', 'Sports'].map((c) => `<option ${state.reelDraftCategory === c ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
          ${state.reelMediaPreview ? `
            <div class="preview-wrap">
              <video controls src="${esc(state.reelMediaPreview)}"></video>
              <button class="preview-remove-btn" id="clearReelMediaBtn" type="button">${iconSvg('close', 16)}</button>
            </div>
          ` : ''}
          <button class="btn btn-secondary" id="pickReelMediaBtn" type="button">${iconSvg('video', 16)}<span>${state.reelMediaFile ? 'Change Video (under 8 MB)' : 'Choose Short Video (Optional)'}</span></button>
          <button class="btn btn-primary" id="submitReelBtn" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Publishing...' : 'Publish Reel'}</button>
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
          <input class="field" id="mktTitleInput" placeholder="Product title" value="${esc(state.mktTitle)}" />
          <div style="display:grid;grid-template-columns:100px 1fr;gap:8px;">
            <select class="select-field" id="mktCurrencyInput">
              ${['$', '৳', '₹', '€', '£'].map((cur) => `<option ${state.mktCurrency === cur ? 'selected' : ''}>${cur}</option>`).join('')}
            </select>
            <input class="field" id="mktPriceInput" type="number" min="0" step="0.01" placeholder="Price (e.g. 25)" value="${esc(state.mktPrice)}" />
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <select class="select-field" id="mktCatInput">
              ${['Electronics', 'Vehicles', 'Home', 'Fashion', 'Books', 'Services', 'Other'].map((c) => `<option ${state.mktCat === c ? 'selected' : ''}>${c}</option>`).join('')}
            </select>
            <select class="select-field" id="mktCondInput">
              ${['New', 'Like New', 'Good', 'Fair'].map((c) => `<option ${state.mktCondition === c ? 'selected' : ''}>${c}</option>`).join('')}
            </select>
          </div>
          <input class="field" id="mktLocInput" placeholder="Location (e.g. Dhaka)" value="${esc(state.mktLoc)}" />
          <textarea class="field" id="mktDescInput" rows="2" placeholder="Description">${esc(state.mktDesc)}</textarea>
          ${state.mktMediaPreview ? `
            <div class="preview-wrap">
              <img src="${esc(state.mktMediaPreview)}" alt="Product preview" />
              <button class="preview-remove-btn" id="clearMktMediaBtn" type="button">${iconSvg('close', 16)}</button>
            </div>
          ` : ''}
          <button class="btn btn-secondary" id="pickMktMediaBtn" type="button">${iconSvg('photo', 16)}<span>${state.mktMediaFile ? 'Change Product Photo' : 'Choose Product Photo'}</span></button>
          <button class="btn btn-primary" id="submitListingBtn" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Publishing...' : 'Publish Listing'}</button>
        </div>
      </div>
    `;
  } else if (state.showCreateGroupModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Create Group</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="grpNameInput" placeholder="Group name" value="${esc(state.grpName)}" />
          <select class="select-field" id="grpCatInput">
            ${['Community', 'Friends & Family', 'Tech', 'Music', 'Sports', 'Gaming'].map((c) => `<option ${state.grpCat === c ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
          <select class="select-field" id="grpPrivacyInput">
            <option value="public" ${state.grpPrivacy === 'public' ? 'selected' : ''}>Public Group</option>
            <option value="private" ${state.grpPrivacy === 'private' ? 'selected' : ''}>Private Circle</option>
          </select>
          <textarea class="field" id="grpDescInput" rows="2" placeholder="Description">${esc(state.grpDesc)}</textarea>
          <label class="setting-row">
            <span>Enable Emergency SOS Circle</span>
            <input type="checkbox" id="grpSosChk" ${state.grpSosEnabled ? 'checked' : ''} />
          </label>
          <button class="btn btn-primary" id="submitGroupBtn">Create Group</button>
        </div>
      </div>
    `;
  } else if (state.showCreatePageModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Create Page</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="pageNameInput" placeholder="Page name" value="${esc(state.pageName)}" />
          <select class="select-field" id="pageCatInput">
            ${['Digital Creator', 'Creator', 'Community', 'Business', 'Music', 'Education', 'Tech', 'Sports'].map((c) => `<option ${state.pageCat === c ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
          <textarea class="field" id="pageBioInput" rows="2" placeholder="Page bio / description">${esc(state.pageBio)}</textarea>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-secondary" id="pickPageAvatarBtn" type="button">${iconSvg('photo', 15)}<span>${state.pageAvatarFile ? 'Avatar Ready ✓' : 'Page Avatar'}</span></button>
            <button class="btn btn-secondary" id="pickPageCoverBtn" type="button">${iconSvg('photo', 15)}<span>${state.pageCoverFile ? 'Cover Ready ✓' : 'Cover Banner'}</span></button>
          </div>
          <button class="btn btn-primary" id="submitPageBtn" ${state.isUploading ? 'disabled' : ''}>${state.isUploading ? 'Creating...' : 'Create Page'}</button>
        </div>
      </div>
    `;
  } else if (state.showCreateEventModal) {
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">Create Event</h3>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          <input class="field" id="evtTitleInput" placeholder="Event title" value="${esc(state.evtTitle)}" />
          <input class="field" id="evtLocInput" placeholder="Location or Online link" value="${esc(state.evtLocation)}" />
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <input class="field" id="evtDateInput" placeholder="Date (e.g. Dec 15)" value="${esc(state.evtDate)}" />
            <input class="field" id="evtTimeInput" placeholder="Time (e.g. 7:00 PM)" value="${esc(state.evtTime)}" />
          </div>
          <textarea class="field" id="evtDescInput" rows="2" placeholder="Event details">${esc(state.evtDesc)}</textarea>
          <button class="btn btn-primary" id="submitEventBtn">Publish Event</button>
        </div>
      </div>
    `;
  } else if (state.showNewGroupChatModal) {
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
  } else if (state.showGetAppModal) {
    const installed = isStandaloneApp();
    const ready = Boolean(state.deferredPrompt);
    html += `
      <div class="modal-backdrop" id="modalBackdrop">
        <div class="modal-card" style="max-width:540px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div style="display:flex;align-items:center;gap:10px;">
              ${renderBrandTile(34)}
              <div>
                <h3 style="margin:0;">HELLO Website, Laptop &amp; Android</h3>
                <div class="muted" style="font-size:12px;">Use HELLO on any device with real-time sync</div>
              </div>
            </div>
            <button class="icon-btn close-modals-btn" style="width:34px;height:34px;">${iconSvg('close', 18)}</button>
          </div>
          ${state.statusBanner ? `<div class="${state.statusIsError ? 'error-box' : 'notice-box'}">${esc(state.statusBanner)}</div>` : ''}

          <div style="padding:14px;border-radius:14px;background:var(--elevated);border:1px solid var(--border);display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
              <strong>💻 Laptop / Desktop App (Windows · Mac · Linux)</strong>
              <span class="badge">${installed ? 'Installed ✓' : (ready ? '1-Click Ready' : 'Browser Install')}</span>
            </div>
            <div class="muted" style="font-size:13px;line-height:1.45;">
              ${installed
                ? 'You are already running the installed HELLO Desktop App.'
                : (ready
                  ? 'Click below to install HELLO as a standalone app on your laptop Start Menu / Dock.'
                  : 'In Chrome or Edge on laptop: click the Install icon in the right of the address bar, or open ⋮ Menu → Cast, save, and share → Install page as app. On Mac Safari: File → Add to Dock.')}
            </div>
            ${!installed ? `
              <div style="display:flex;gap:8px;flex-wrap:wrap;">
                ${ready ? `<button class="btn btn-violet" id="triggerNativePwaBtn">${iconSvg('laptop', 16)}<span>Install Desktop / Laptop App Now</span></button>` : ''}
                <button class="btn btn-primary" id="downloadLaptopShortcutBtn" type="button">${iconSvg('laptop', 16)}<span>Download Laptop Shortcut</span></button>
                <button class="btn btn-secondary copy-website-url-btn" type="button">${iconSvg('share', 15)}<span>Copy Website Link</span></button>
              </div>
            ` : ''}
          </div>

          <div style="padding:14px;border-radius:14px;background:var(--elevated);border:1px solid var(--border);display:flex;flex-direction:column;gap:8px;">
            <strong>🌐 Official HELLO Website</strong>
            <div style="padding:8px 10px;border-radius:10px;background:var(--bg);font-size:12.5px;word-break:break-all;border:1px solid var(--border);">${esc(WEBSITE_URL)}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;">
              <button class="btn btn-primary copy-website-url-btn" type="button">${iconSvg('check', 15)}<span>Copy Website Link</span></button>
              <button class="btn btn-secondary" id="shareWebsiteModalBtn" type="button">${iconSvg('share', 15)}<span>Share Link</span></button>
              <a class="btn btn-secondary" href="${WEBSITE_URL}" target="_blank" rel="noopener noreferrer">Open Tab</a>
            </div>
          </div>

          <div style="padding:14px;border-radius:14px;background:var(--elevated);border:1px solid var(--border);display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;">
            <div>
              <strong>📱 Android Phone App (APK)</strong>
              <div class="muted" style="font-size:12px;">Latest Android release v1.8.7</div>
            </div>
            <a class="btn btn-primary" href="${APK_URL}" target="_blank" rel="noopener noreferrer">${iconSvg('phone', 16)}<span>Download APK</span></a>
          </div>
        </div>
      </div>
    `;
  } else {
    html += renderFeatureModals();
  }
  return html;
}

function renderShell() {
  const uid = state.user.uid;
  const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
  const activePeers = state.users.filter((u) => (u.userId || u.id) !== uid && !state.blockedUids.has(u.userId || u.id) && u.isOnline && u.showOnlineStatus !== false);
  const unreadChats = state.conversations.filter((c) => !(c.readByUids || []).includes(uid)).length;
  const unreadNotifs = state.notifications.filter((n) => !n.isRead).length;
  const isMessenger = state.tab === 'messenger';

  const primaryNav = [
    ['feed', 'home', 'Home'],
    ['friends', 'friends', 'Friends'],
    ['groups', 'groups', 'Groups'],
    ['reels', 'reels', 'Reels'],
    ['secret_love', 'heart', 'Love']
  ];

  return `
    <div class="app-frame">
      <aside class="nav-rail">
        <button class="rail-logo-btn" id="railHubBtn" title="HELLO Hub Menu">
          ${renderBrandTile(34)}
        </button>
        ${primaryNav.map(([k, ic, lbl]) => `
          <button class="rail-item ${state.tab === k ? 'active' : ''}" data-tab="${k}">
            <span class="rail-pill">${iconSvg(ic, 20)}</span>
            <span>${lbl}</span>
          </button>
        `).join('')}
        <button class="rail-item ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger">
          <span class="rail-pill">${iconSvg('chat', 20)}</span>
          <span>Chats</span>
          ${unreadChats ? `<span class="tab-badge">${unreadChats}</span>` : ''}
        </button>
      </aside>

      <div class="main-column">
        <header class="topbar">
          <button class="brand-btn" id="topHubBtn" title="Open HELLO Hub">
            ${renderBrandTile(32)}
            <div style="text-align:left;">
              <div class="brand-name" style="line-height:1.1;">HELLO</div>
              <div class="brand-sub">${state.tab === 'hub' ? 'Hub Open' : 'Tap for Menu'}</div>
            </div>
          </button>

          <div class="top-actions">
            <button class="btn btn-secondary open-get-app-btn" id="openGetAppTopBtn" title="Website, Laptop & Android Download" style="padding:6px 12px;min-height:36px;font-size:12.5px;">${iconSvg('laptop', 16)}<span>App &amp; Laptop</span></button>
            <button class="icon-btn" id="themeToggleBtn" title="Day / Night Mode">${iconSvg(state.theme === 'dark' ? 'sun' : 'moon', 19)}</button>
            <button class="icon-btn ${state.tab === 'search' ? 'active' : ''}" data-tab="search" title="Search HELLO">${iconSvg('search', 19)}</button>
            <button class="icon-btn ${state.tab === 'messenger' ? 'active' : ''}" data-tab="messenger" title="Messenger">
              ${iconSvg('chat', 19)}
              ${unreadChats ? `<span class="tab-badge">${unreadChats}</span>` : ''}
            </button>
            <button class="icon-btn ${state.tab === 'notifications' ? 'active' : ''}" data-tab="notifications" title="Notifications">
              ${iconSvg('bell', 19)}
              ${unreadNotifs ? `<span class="tab-badge">${unreadNotifs}</span>` : ''}
            </button>
            <button class="top-avatar-btn open-my-profile" title="My Profile">
              ${avatarHtml(me.displayName, me.photoUrl, 36, me.isOnline !== false)}
            </button>
          </div>
        </header>

        ${state.globalSettings?.maintenanceBanner ? `
          <div style="max-width:1180px;width:100%;margin:10px auto 0;padding:0 16px;">
            <div class="notice-box" style="font-weight:600;">📢 ${esc(state.globalSettings.maintenanceBanner)}</div>
          </div>
        ` : ''}

        ${state.statusBanner ? `
          <div style="max-width:1180px;width:100%;margin:10px auto 0;padding:0 16px;">
            <div class="${state.statusIsError ? 'error-box' : 'notice-box'}" style="display:flex;justify-content:space-between;align-items:center;gap:10px;">
              <span>${esc(state.statusBanner)}</span>
              <button class="msg-mini-btn" id="dismissBannerBtn">${iconSvg('close', 16)}</button>
            </div>
          </div>
        ` : ''}

        <div class="workspace-body ${isMessenger ? 'full-width' : ''}">
          <main class="center-stage">${renderCenterStage()}</main>

          ${!isMessenger ? `
            <aside class="right-panel">
              <div class="card">
                <div style="font-weight:800;margin-bottom:8px;">Active Friends (${activePeers.length})</div>
                <div style="display:flex;flex-direction:column;gap:3px;">
                  ${activePeers.length === 0 ? `<div class="muted">No friends active right now.</div>` : activePeers.slice(0, 12).map((u) => `
                    <button class="side-item msg-person-btn" data-uid="${esc(u.userId || u.id)}">
                      <span class="side-item-left">
                        ${avatarHtml(u.displayName, u.photoUrl, 34, true)}
                        <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(u.displayName || 'Member')}</span>
                      </span>
                    </button>
                  `).join('')}
                </div>
              </div>
              <div class="card" style="display:flex;flex-direction:column;gap:6px;padding:12px;">
                <button class="side-item" data-tab="sos"><span class="side-item-left">${iconSvg('shield', 18)}<span>Emergency SOS</span></span></button>
                <button class="side-item" data-tab="marketplace"><span class="side-item-left">${iconSvg('shop', 18)}<span>Marketplace</span></span></button>
                <button class="side-item" data-tab="pages"><span class="side-item-left">${iconSvg('flag', 18)}<span>Pages</span></span></button>
                <button class="side-item" data-tab="events"><span class="side-item-left">${iconSvg('event', 18)}<span>Events</span></span></button>
                <button class="side-item" data-tab="saved"><span class="side-item-left">${iconSvg('bookmark', 18)}<span>Saved Items</span></span></button>
                <button class="side-item" data-tab="settings"><span class="side-item-left">${iconSvg('user', 18)}<span>Settings & Privacy</span></span></button>
                <button class="side-item open-get-app-btn" id="openGetAppSideBtn"><span class="side-item-left">${iconSvg('laptop', 18)}<span>Website &amp; Laptop App</span></span></button>
                ${isAdminUser() ? `<button class="side-item" data-tab="owner_dashboard"><span class="side-item-left">${iconSvg('verified', 18)}<span>Owner Dashboard</span></span></button>` : ''}
              </div>
            </aside>
          ` : ''}
        </div>
      </div>

      <nav class="mobile-bottom-nav">
        ${primaryNav.map(([k, ic, lbl]) => `
          <button class="mobile-nav-btn ${state.tab === k ? 'active' : ''}" data-tab="${k}">
            <span class="m-pill">${iconSvg(ic, 20)}</span>
            <span>${lbl}</span>
          </button>
        `).join('')}
      </nav>
    </div>

    ${renderModals()}
  `;
}

// Persistent file inputs inside root need to be kept intact or safely wired on every render.
// Even better: mount persistent shell file inputs OUTSIDE #root once at startup!
function ensureGlobalFileInputs() {
  if (document.getElementById('hello-global-pickers')) return;
  const host = document.createElement('div');
  host.id = 'hello-global-pickers';
  host.innerHTML = `
    <input type="file" accept="image/*,video/*" id="gPostPhotoInput" class="sr-only-input" />
    <input type="file" accept="image/*,video/*" id="gStoryFileInput" class="sr-only-input" />
    <input type="file" accept="video/*" id="gReelFileInput" class="sr-only-input" />
    <input type="file" accept="image/*" id="gMktFileInput" class="sr-only-input" />
    <input type="file" accept="image/*" id="gProfAvatarInput" class="sr-only-input" />
    <input type="file" accept="image/*" id="gProfCoverInput" class="sr-only-input" />
    <input type="file" accept="image/*" id="gPageAvatarInput" class="sr-only-input" />
    <input type="file" accept="image/*" id="gPageCoverInput" class="sr-only-input" />
    <input type="file" accept="image/*,video/*" capture="environment" id="gChatCameraInput" class="sr-only-input" />
    <input type="file" accept="image/*,video/*" id="gChatMediaInput" class="sr-only-input" />
    <input type="file" id="gChatFileInput" class="sr-only-input" />
  `;
  document.body.appendChild(host);

  document.getElementById('gPostPhotoInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('postMediaFile', 'postMediaPreview', f);
  });
  document.getElementById('gStoryFileInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('storyMediaFile', 'storyMediaPreview', f);
  });
  document.getElementById('gReelFileInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('reelMediaFile', 'reelMediaPreview', f);
  });
  document.getElementById('gMktFileInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('mktMediaFile', 'mktMediaPreview', f);
  });
  document.getElementById('gProfAvatarInput').addEventListener('change', async (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f || !state.user) return;
    try {
      const uri = await uploadMediaBlob(f, '', 'Updating profile photo...');
      await updateDoc(doc(db, 'users_public', state.user.uid), { photoUrl: uri, updatedAt: serverTimestamp() });
      showBanner('Profile photo updated.');
    } catch (err) {
      showBanner(err?.message || 'Could not upload avatar.', true);
    }
  });
  document.getElementById('gProfCoverInput').addEventListener('change', async (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f || !state.user) return;
    try {
      const uri = await uploadMediaBlob(f, '', 'Updating cover photo...');
      await updateDoc(doc(db, 'users_public', state.user.uid), { coverUrl: uri, updatedAt: serverTimestamp() });
      showBanner('Cover photo updated.');
    } catch (err) {
      showBanner(err?.message || 'Could not upload cover photo.', true);
    }
  });
  document.getElementById('gPageAvatarInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('pageAvatarFile', 'pageAvatarPreview', f);
  });
  document.getElementById('gPageCoverInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) setPreviewFile('pageCoverFile', 'pageCoverPreview', f);
  });

  const uploadChatFile = async (file, forcedType = '') => {
    const activeConv = state.conversations.find((c) => c.id === state.activeConvId);
    if (!file || !activeConv) return;
    state.showAttachMenu = false;
    try {
      const uri = await uploadMediaBlob(file, '', 'Sending attachment...');
      const mType = forcedType || (file.type?.startsWith('video/') ? 'video' : file.type?.startsWith('image/') ? 'image' : 'file');
      await sendChatMessage(activeConv.id, activeConv.participantUids || [], '', uri, mType, file.name);
    } catch (err) {
      showBanner(err?.message || 'Could not send file.', true);
    }
  };
  document.getElementById('gChatCameraInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) uploadChatFile(f);
  });
  document.getElementById('gChatMediaInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) uploadChatFile(f);
  });
  document.getElementById('gChatFileInput').addEventListener('change', (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (f) uploadChatFile(f, 'file');
  });
}

let reelsObserver = null;
let reelTextTimer = null;

function scrollToReelSlide(targetIdx) {
  const feed = document.getElementById('reelsSnapFeed');
  if (!feed) return;
  const slides = feed.querySelectorAll('.reel-slide');
  if (!slides.length) return;
  const clamped = Math.max(0, Math.min(slides.length - 1, targetIdx));
  state.reelActiveIndex = clamped;
  slides[clamped].scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function setupReelsSequentialObserver() {
  if (reelsObserver) {
    try { reelsObserver.disconnect(); } catch (_) {}
    reelsObserver = null;
  }
  if (reelTextTimer) {
    clearTimeout(reelTextTimer);
    reelTextTimer = null;
  }
  if (!state.user || state.tab !== 'reels') return;
  const feed = document.getElementById('reelsSnapFeed');
  if (!feed) return;
  const slides = [...feed.querySelectorAll('.reel-slide')];
  if (!slides.length) return;

  const activateSlide = (activeIdx) => {
    state.reelActiveIndex = activeIdx;
    clearTimeout(reelTextTimer);
    slides.forEach((slide, idx) => {
      const vid = slide.querySelector('video');
      if (vid) {
        vid.loop = !state.reelAutoNext;
        if (idx === activeIdx) {
          vid.onended = () => {
            if (state.reelAutoNext && idx + 1 < slides.length) {
              scrollToReelSlide(idx + 1);
            }
          };
          vid.play().catch(() => {
            vid.muted = true;
            vid.play().catch(() => {});
          });
        } else {
          vid.onended = null;
          if (!vid.paused) vid.pause();
        }
      } else if (idx === activeIdx && state.reelAutoNext && idx + 1 < slides.length) {
        reelTextTimer = setTimeout(() => {
          if (state.tab === 'reels' && state.reelAutoNext && state.reelActiveIndex === idx) {
            scrollToReelSlide(idx + 1);
          }
        }, 6500);
      }
    });
  };

  reelsObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting && entry.intersectionRatio >= 0.6) {
        const idx = Number(entry.target.getAttribute('data-reel-idx') || 0);
        activateSlide(idx);
      }
    });
  }, { root: feed, threshold: 0.62 });

  slides.forEach((s) => reelsObserver.observe(s));
  const initialIdx = Math.max(0, Math.min(slides.length - 1, state.reelActiveIndex || 0));
  if (initialIdx > 0) {
    slides[initialIdx].scrollIntoView({ behavior: 'auto', block: 'start' });
  }
  activateSlide(initialIdx);
}

function render() {
  ensureGlobalFileInputs();
  const root = document.getElementById('root');
  if (!root) return;
  const activeId = document.activeElement?.id || '';
  const selStart = document.activeElement?.selectionStart ?? null;

  root.innerHTML = state.user ? renderShell() : renderLanding();
  bindEvents();
  setupReelsSequentialObserver();

  if (activeId) {
    const el = document.getElementById(activeId);
    if (el && typeof el.focus === 'function') {
      el.focus();
      if (selStart !== null && typeof el.setSelectionRange === 'function') {
        try { el.setSelectionRange(selStart, selStart); } catch (_) {}
      }
    }
  }
}

function openPicker(inputId) {
  const input = document.getElementById(inputId);
  if (input) input.click();
}

function closeAllModals() {
  state.activeStory = null;
  state.lightboxUri = '';
  state.showCreateStoryModal = false;
  state.showCreateReelModal = false;
  state.showCreateListingModal = false;
  state.showCreateGroupModal = false;
  state.showCreatePageModal = false;
  state.showCreateEventModal = false;
  state.showNewGroupChatModal = false;
  state.showGetAppModal = false;
  state.editPostTarget = null;
  state.reportTarget = null;
  state.shareTarget = null;
  state.openReelCommentsId = null;
  state.manageGroupModal = null;
  state.sosModalGroup = null;
  state.privateChatMatchId = null;
  state.maskModalMatchId = null;
  state.detailListingId = null;
  state.editListingTarget = null;
  state.editPageTarget = null;
  state.managePageModal = null;
  state.showEditProfileModal = false;
  stopFeatureSubListeners();
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
  document.querySelectorAll('.open-get-app-btn').forEach((btn) => {
    btn.addEventListener('click', openGetAppModal);
  });
  document.getElementById('triggerNativePwaBtn')?.addEventListener('click', triggerNativeDesktopInstall);
  document.getElementById('downloadLaptopShortcutBtn')?.addEventListener('click', downloadLaptopShortcutFile);
  document.querySelectorAll('.copy-website-url-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const ok = await copyTextSafe(WEBSITE_URL);
      showBanner(ok ? `Official Website link copied: ${WEBSITE_URL}` : `Website URL: ${WEBSITE_URL}`);
    });
  });
  document.getElementById('shareWebsiteModalBtn')?.addEventListener('click', async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'HELLO Social App — Website, Laptop & Android',
          text: 'Open HELLO on Website or install it on your Laptop & Phone:',
          url: WEBSITE_URL
        });
        return;
      } catch (_) {}
    }
    await copyTextSafe(WEBSITE_URL);
    showBanner(`Official Website link copied: ${WEBSITE_URL}`);
  });
  document.getElementById('signOutBtnHub')?.addEventListener('click', () => signOut(auth));
  document.getElementById('dismissBannerBtn')?.addEventListener('click', () => { state.statusBanner = ''; render(); });

  document.getElementById('themeToggleBtn')?.addEventListener('click', () => {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    localStorage.setItem('hello_theme', state.theme);
    document.documentElement.classList.toggle('light', state.theme === 'light');
    render();
  });

  const toggleHub = () => {
    state.tab = state.tab === 'hub' ? 'feed' : 'hub';
    render();
  };
  document.getElementById('topHubBtn')?.addEventListener('click', toggleHub);
  document.getElementById('railHubBtn')?.addEventListener('click', toggleHub);

  document.getElementById('globalSearchInput')?.addEventListener('input', (e) => {
    state.search = e.target.value;
    render();
  });
  document.getElementById('tabLocalSearchInput')?.addEventListener('input', (e) => {
    state.search = e.target.value;
    render();
  });

  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.tab = btn.getAttribute('data-tab');
      if (state.tab === 'profile') state.profileViewUid = null;
      if (state.tab === 'reels') state.reelActiveIndex = 0;
      state.activeGroupId = null;
      state.activePageId = null;
      state.mobileShowChat = false;
      state.showAttachMenu = false;
      render();
    });
  });

  document.querySelectorAll('.open-my-profile').forEach((el) => {
    el.addEventListener('click', () => { state.profileViewUid = null; state.tab = 'profile'; render(); });
  });
  document.querySelectorAll('.open-user-profile').forEach((el) => {
    el.addEventListener('click', () => {
      const target = el.getAttribute('data-uid');
      if (target) { closeAllModals(); state.profileViewUid = target; state.tab = 'profile'; render(); }
    });
  });

  document.querySelectorAll('.open-lightbox').forEach((el) => {
    el.addEventListener('click', () => {
      const u = el.getAttribute('data-lightbox-uri');
      if (u) { state.lightboxUri = u; render(); }
    });
  });

  document.querySelectorAll('[data-feed-sort]').forEach((btn) => {
    btn.addEventListener('click', () => { state.feedSort = btn.getAttribute('data-feed-sort'); render(); });
  });

  // Post Composer & Persistent File Pickers
  document.getElementById('postDraftInput')?.addEventListener('input', (e) => { state.postDraft = e.target.value; });
  document.getElementById('postPrivacySelect')?.addEventListener('change', (e) => { state.postPrivacy = e.target.value; });
  document.getElementById('pickPostMediaBtn')?.addEventListener('click', () => openPicker('gPostPhotoInput'));
  document.getElementById('clearPostMediaBtn')?.addEventListener('click', () => setPreviewFile('postMediaFile', 'postMediaPreview', null));
  document.getElementById('publishPostBtn')?.addEventListener('click', () => publishPost('', ''));
  document.getElementById('publishGroupPostBtn')?.addEventListener('click', (e) => publishPost(e.currentTarget.getAttribute('data-group-id') || '', ''));
  document.getElementById('publishPagePostBtn')?.addEventListener('click', (e) => publishPost('', e.currentTarget.getAttribute('data-page-id') || ''));

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
      showBanner('Post link copied to clipboard.');
    });
  });

  // Stories Modal & Pickers
  document.getElementById('openCreateStoryBtn')?.addEventListener('click', () => { state.showCreateStoryModal = true; render(); });
  document.getElementById('createStoryCardBtn')?.addEventListener('click', () => { state.showCreateStoryModal = true; render(); });
  document.getElementById('storyCaptionInput')?.addEventListener('input', (e) => { state.storyDraftCaption = e.target.value; });
  document.getElementById('storyPrivacySelect')?.addEventListener('change', (e) => { state.storyPrivacy = e.target.value; });
  document.querySelectorAll('.story-bg-pick').forEach((b) => {
    b.addEventListener('click', () => { state.storyBgHex = b.getAttribute('data-hex'); render(); });
  });
  document.getElementById('pickStoryMediaBtn')?.addEventListener('click', () => openPicker('gStoryFileInput'));
  document.getElementById('clearStoryMediaBtn')?.addEventListener('click', () => setPreviewFile('storyMediaFile', 'storyMediaPreview', null));
  document.querySelectorAll('.view-story-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const s = state.stories.find((x) => x.id === btn.getAttribute('data-story-id'));
      if (s) openStoryWithTracking(s);
    });
  });
  document.getElementById('submitStoryBtn')?.addEventListener('click', async () => {
    if (!state.user || state.isUploading) return;
    const cap = state.storyDraftCaption.trim();
    const file = state.storyMediaFile;
    if (!cap && !file) { showBanner('Add a caption or choose a photo/video.', true); return; }
    try {
      const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
      const uri = file ? await uploadMediaBlob(file, '', 'Publishing story...') : '';
      const mType = file ? (file.type?.startsWith('video/') ? 'video' : 'image') : 'text';
      const friendUids = state.friendships.flatMap((f) => f.participantUids || []).filter((x) => x !== state.user.uid);
      const priv = state.storyPrivacy === 'friends' ? 'friends' : 'public';
      await addDoc(collection(db, 'stories'), {
        authorId: state.user.uid,
        authorName: (me.displayName || 'Hello Member').slice(0, 80),
        authorAvatar: me.photoUrl || '',
        mediaUri: uri,
        mediaType: mType,
        caption: cap.slice(0, 500),
        backgroundHex: state.storyBgHex || '#1E1B4B',
        privacy: priv,
        allowedViewerUids: priv === 'friends' ? friendUids.slice(0, 200) : [],
        viewersCount: 0,
        isSavedHighlight: false,
        autoDelete24h: true,
        createdAt: serverTimestamp()
      });
      state.storyDraftCaption = '';
      setPreviewFile('storyMediaFile', 'storyMediaPreview', null);
      state.showCreateStoryModal = false;
      showBanner('Story published!');
    } catch (e) {
      showBanner(e?.message || 'Failed to publish story.', true);
    }
  });

  // Reels Modal & Pickers
  document.getElementById('openCreateReelBtn')?.addEventListener('click', () => { state.showCreateReelModal = true; render(); });
  document.getElementById('reelCaptionInput')?.addEventListener('input', (e) => { state.reelDraftCaption = e.target.value; });
  document.getElementById('reelCatSelect')?.addEventListener('change', (e) => { state.reelDraftCategory = e.target.value; });
  document.getElementById('pickReelMediaBtn')?.addEventListener('click', () => openPicker('gReelFileInput'));
  document.getElementById('clearReelMediaBtn')?.addEventListener('click', () => setPreviewFile('reelMediaFile', 'reelMediaPreview', null));
  document.getElementById('submitReelBtn')?.addEventListener('click', async () => {
    if (!state.user || state.isUploading) return;
    const cap = state.reelDraftCaption.trim();
    if (!cap) { showBanner('Write a Reel caption first.', true); return; }
    try {
      const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
      const videoUri = state.reelMediaFile ? await uploadMediaBlob(state.reelMediaFile, '', 'Uploading Reel video...') : '';
      await addDoc(collection(db, 'reels'), {
        authorId: state.user.uid,
        authorName: (me.displayName || 'Hello Member').slice(0, 80),
        authorAvatar: me.photoUrl || '',
        caption: cap.slice(0, 1000),
        videoUri,
        coverHex: state.reelCoverHex || '#1E1B4B',
        category: state.reelDraftCategory || 'Trending',
        likesCount: 0,
        commentsCount: 0,
        sharesCount: 0,
        createdAt: serverTimestamp()
      });
      state.reelDraftCaption = '';
      setPreviewFile('reelMediaFile', 'reelMediaPreview', null);
      state.showCreateReelModal = false;
      showBanner('Reel published!');
    } catch (e) {
      showBanner(e?.message || 'Could not publish Reel.', true);
    }
  });
  document.getElementById('toggleReelAutoNextBtn')?.addEventListener('click', () => {
    state.reelAutoNext = !state.reelAutoNext;
    render();
  });
  document.getElementById('reelPrevBtn')?.addEventListener('click', () => scrollToReelSlide((state.reelActiveIndex || 0) - 1));
  document.getElementById('reelNextBtn')?.addEventListener('click', () => scrollToReelSlide((state.reelActiveIndex || 0) + 1));
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

  const activeConv = state.conversations.find((c) => c.id === state.activeConvId);
  document.getElementById('startAudioCallBtn')?.addEventListener('click', () => startCallSignal(activeConv, 'audio'));
  document.getElementById('startVideoCallBtn')?.addEventListener('click', () => startCallSignal(activeConv, 'video'));

  document.querySelectorAll('.answer-call-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const c = state.activeCalls.find((x) => x.id === b.getAttribute('data-call-id'));
      if (c) updateCallStatus(c, 'active');
    });
  });
  document.querySelectorAll('.decline-call-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const c = state.activeCalls.find((x) => x.id === b.getAttribute('data-call-id'));
      if (c) updateCallStatus(c, 'declined');
    });
  });
  document.querySelectorAll('.end-call-btn').forEach((b) => {
    b.addEventListener('click', () => {
      const c = state.activeCalls.find((x) => x.id === b.getAttribute('data-call-id'));
      if (c) updateCallStatus(c, 'ended');
    });
  });

  const chatInput = document.getElementById('chatTextInput');
  chatInput?.addEventListener('input', (e) => {
    const hadText = state.msgDraft.trim().length > 0;
    state.msgDraft = e.target.value;
    if (activeConv) setTypingState(activeConv.id, state.msgDraft.trim().length > 0);
    if (hadText !== (state.msgDraft.trim().length > 0)) render();
  });
  chatInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && state.msgDraft.trim() && activeConv) {
      const t = state.msgDraft.trim();
      state.msgDraft = '';
      sendChatMessage(activeConv.id, activeConv.participantUids || [], t);
    }
  });
  document.getElementById('toggle24hBtn')?.addEventListener('click', () => { state.disappearing24h = !state.disappearing24h; render(); });
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
  document.getElementById('pickChatCameraBtn')?.addEventListener('click', () => openPicker('gChatCameraInput'));
  document.getElementById('pickChatMediaBtn')?.addEventListener('click', () => openPicker('gChatMediaInput'));
  document.getElementById('pickChatFileBtn')?.addEventListener('click', () => openPicker('gChatFileInput'));
  document.getElementById('cancelReplyBtn')?.addEventListener('click', () => { state.replyToMsg = null; render(); });

  document.querySelectorAll('.reply-msg-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const msg = state.messages.find((m) => m.id === btn.getAttribute('data-msg-id'));
      if (msg) { state.replyToMsg = msg; render(); }
    });
  });
  document.querySelectorAll('.toggle-msg-emoji-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const mid = btn.getAttribute('data-msg-id');
      state.activeMsgEmojiId = state.activeMsgEmojiId === mid ? null : mid;
      render();
    });
  });
  document.querySelectorAll('.react-msg-emoji-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!activeConv || !state.user) return;
      const mid = btn.getAttribute('data-msg-id');
      const em = btn.getAttribute('data-emoji') || '❤️';
      state.activeMsgEmojiId = null;
      await updateDoc(doc(db, 'conversations', activeConv.id, 'messages', mid), { [`reactions.${state.user.uid}`]: em }).catch(() => {});
      render();
    });
  });
  document.querySelectorAll('.unsend-msg-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!activeConv) return;
      const mid = btn.getAttribute('data-msg-id');
      await updateDoc(doc(db, 'conversations', activeConv.id, 'messages', mid), { isDeleted: true, text: '' }).catch(() => {});
    });
  });

  document.getElementById('openNewGroupChatBtn')?.addEventListener('click', () => { state.showNewGroupChatModal = true; render(); });
  document.getElementById('createGrpChatBtn')?.addEventListener('click', async () => {
    const title = document.getElementById('grpChatTitleInput')?.value?.trim();
    const picked = [...document.querySelectorAll('.grp-member-chk:checked')].map((c) => c.value);
    if (!title || picked.length === 0) { showBanner('Enter a name and choose members.', true); return; }
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
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const targetUid = btn.getAttribute('data-uid');
      const peer = state.usersById[targetUid] || { userId: targetUid, displayName: 'Member' };
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
      showBanner(`Friend request sent to ${peer.displayName}.`);
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

  // Groups
  document.querySelectorAll('.open-group-btn').forEach((btn) => {
    btn.addEventListener('click', () => { state.activeGroupId = btn.getAttribute('data-group-id'); state.tab = 'groups'; render(); });
  });
  document.getElementById('backToGroupsBtn')?.addEventListener('click', () => { state.activeGroupId = null; render(); });
  document.querySelectorAll('.join-group-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const g = state.groups.find((x) => x.id === btn.getAttribute('data-group-id'));
      if (g) toggleJoinGroup(g);
    });
  });
  document.getElementById('openCreateGroupBtn')?.addEventListener('click', () => { state.showCreateGroupModal = true; render(); });
  document.getElementById('grpNameInput')?.addEventListener('input', (e) => { state.grpName = e.target.value; });
  document.getElementById('grpCatInput')?.addEventListener('change', (e) => { state.grpCat = e.target.value; });
  document.getElementById('grpPrivacyInput')?.addEventListener('change', (e) => { state.grpPrivacy = e.target.value; });
  document.getElementById('grpDescInput')?.addEventListener('input', (e) => { state.grpDesc = e.target.value; });
  document.getElementById('grpSosChk')?.addEventListener('change', (e) => { state.grpSosEnabled = e.target.checked; });
  document.getElementById('submitGroupBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const name = state.grpName.trim();
    if (name.length < 2) { showBanner('Group name must be at least 2 characters.', true); return; }
    try {
      const uid = state.user.uid;
      await addDoc(collection(db, 'groups'), {
        name: name.slice(0, 100),
        description: state.grpDesc.trim().slice(0, 600),
        category: state.grpCat || 'Community',
        privacy: state.grpPrivacy === 'private' ? 'private' : 'public',
        creatorId: uid,
        adminUids: [uid],
        memberUids: [uid],
        membersCount: 1,
        sosEnabled: Boolean(state.grpSosEnabled),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      state.grpName = ''; state.grpDesc = ''; state.showCreateGroupModal = false;
      showBanner('Group created!');
    } catch (e) {
      showBanner(e?.message || 'Could not create group.', true);
    }
  });

  // Marketplace
  document.querySelectorAll('[data-market-cat]').forEach((btn) => {
    btn.addEventListener('click', () => { state.marketCat = btn.getAttribute('data-market-cat'); render(); });
  });
  document.querySelectorAll('[data-market-status]').forEach((btn) => {
    btn.addEventListener('click', () => { state.marketStatus = btn.getAttribute('data-market-status'); render(); });
  });
  document.getElementById('marketSortSelect')?.addEventListener('change', (e) => {
    state.marketSort = e.target.value;
    render();
  });
  document.getElementById('openCreateListingBtn')?.addEventListener('click', () => { state.showCreateListingModal = true; render(); });
  document.getElementById('mktTitleInput')?.addEventListener('input', (e) => { state.mktTitle = e.target.value; });
  document.getElementById('mktCurrencyInput')?.addEventListener('change', (e) => { state.mktCurrency = e.target.value; });
  document.getElementById('mktPriceInput')?.addEventListener('input', (e) => { state.mktPrice = e.target.value; });
  document.getElementById('mktCatInput')?.addEventListener('change', (e) => { state.mktCat = e.target.value; });
  document.getElementById('mktCondInput')?.addEventListener('change', (e) => { state.mktCondition = e.target.value; });
  document.getElementById('mktLocInput')?.addEventListener('input', (e) => { state.mktLoc = e.target.value; });
  document.getElementById('mktDescInput')?.addEventListener('input', (e) => { state.mktDesc = e.target.value; });
  document.getElementById('pickMktMediaBtn')?.addEventListener('click', () => openPicker('gMktFileInput'));
  document.getElementById('clearMktMediaBtn')?.addEventListener('click', () => setPreviewFile('mktMediaFile', 'mktMediaPreview', null));
  document.getElementById('submitListingBtn')?.addEventListener('click', async () => {
    if (!state.user || state.isUploading) return;
    const title = state.mktTitle.trim();
    const price = parseFloat(state.mktPrice || '0');
    if (title.length < 2 || Number.isNaN(price) || price < 0) {
      showBanner('Enter a valid product title and price.', true);
      return;
    }
    try {
      const imageUri = state.mktMediaFile ? await uploadMediaBlob(state.mktMediaFile, '', 'Uploading product photo...') : '';
      const me = state.me || { displayName: state.user.displayName || 'Hello Member', photoUrl: state.user.photoURL || '' };
      await addDoc(collection(db, 'marketplace_listings'), {
        sellerId: state.user.uid,
        sellerName: (me.displayName || 'Hello Member').slice(0, 80),
        sellerAvatar: me.photoUrl || '',
        title: title.slice(0, 120),
        description: state.mktDesc.trim().slice(0, 1000),
        price,
        currency: state.mktCurrency || '$',
        category: state.mktCat || 'Other',
        condition: state.mktCondition || 'Like New',
        locationText: state.mktLoc.trim().slice(0, 100) || 'Local',
        imageUri,
        status: 'available',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      state.mktTitle = ''; state.mktPrice = ''; state.mktLoc = ''; state.mktDesc = '';
      setPreviewFile('mktMediaFile', 'mktMediaPreview', null);
      state.showCreateListingModal = false;
      showBanner('Listing published to HELLO Marketplace!');
    } catch (e) {
      showBanner(e?.message || 'Could not publish listing.', true);
    }
  });
  document.querySelectorAll('.toggle-sold-btn').forEach((btn) => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const item = state.marketplace.find((m) => m.id === btn.getAttribute('data-item-id'));
      if (!item) return;
      try {
        const nextStatus = item.status === 'sold' ? 'available' : 'sold';
        await updateDoc(doc(db, 'marketplace_listings', item.id), { status: nextStatus, updatedAt: serverTimestamp() });
        showBanner(nextStatus === 'sold' ? 'Listing marked as Sold.' : 'Listing marked Available.');
      } catch (err) {
        showBanner(err?.message || 'Could not update listing status.', true);
      }
    });
  });
  document.querySelectorAll('.market-msg-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const item = state.marketplace.find((m) => m.id === btn.getAttribute('data-item-id'));
      if (!item) return;
      closeAllModals();
      const seller = state.usersById[item.sellerId] || { userId: item.sellerId, displayName: item.sellerName || 'Seller' };
      openOrCreateDm(seller, `Is this available? (${item.title} · ${item.currency || '$'}${Number(item.price || 0).toFixed(2)})`, item.imageUri || '', item.imageUri ? 'image' : 'none');
    });
  });

  // Pages & Events
  document.querySelectorAll('[data-page-cat]').forEach((btn) => {
    btn.addEventListener('click', () => { state.pageCatFilter = btn.getAttribute('data-page-cat'); render(); });
  });
  document.getElementById('openCreatePageBtn')?.addEventListener('click', () => { state.showCreatePageModal = true; render(); });
  document.getElementById('pageNameInput')?.addEventListener('input', (e) => { state.pageName = e.target.value; });
  document.getElementById('pageCatInput')?.addEventListener('change', (e) => { state.pageCat = e.target.value; });
  document.getElementById('pageBioInput')?.addEventListener('input', (e) => { state.pageBio = e.target.value; });
  document.getElementById('pickPageAvatarBtn')?.addEventListener('click', () => openPicker('gPageAvatarInput'));
  document.getElementById('pickPageCoverBtn')?.addEventListener('click', () => openPicker('gPageCoverInput'));
  document.getElementById('submitPageBtn')?.addEventListener('click', async () => {
    if (!state.user || state.isUploading) return;
    const name = state.pageName.trim();
    if (name.length < 2) { showBanner('Page name must be at least 2 characters.', true); return; }
    try {
      const uid = state.user.uid;
      const avatarUrl = state.pageAvatarFile ? await uploadMediaBlob(state.pageAvatarFile, '', 'Uploading Page avatar...') : '';
      const coverUrl = state.pageCoverFile ? await uploadMediaBlob(state.pageCoverFile, '', 'Uploading Page cover...') : '';
      await addDoc(collection(db, 'pages'), {
        name: name.slice(0, 100),
        category: state.pageCat || 'Creator',
        bio: state.pageBio.trim().slice(0, 600),
        avatarUrl,
        coverUrl,
        creatorId: uid,
        adminUids: [uid],
        followerUids: [uid],
        followersCount: 1,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      state.pageName = ''; state.pageBio = ''; state.showCreatePageModal = false;
      setPreviewFile('pageAvatarFile', 'pageAvatarPreview', null);
      setPreviewFile('pageCoverFile', 'pageCoverPreview', null);
      showBanner('Page created!');
    } catch (e) {
      showBanner(e?.message || 'Could not create page.', true);
    }
  });
  document.querySelectorAll('.follow-page-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const pg = state.pages.find((p) => p.id === btn.getAttribute('data-page-id'));
      if (pg) toggleFollowPage(pg);
    });
  });

  document.getElementById('openCreateEventBtn')?.addEventListener('click', () => { state.showCreateEventModal = true; render(); });
  document.getElementById('evtTitleInput')?.addEventListener('input', (e) => { state.evtTitle = e.target.value; });
  document.getElementById('evtLocInput')?.addEventListener('input', (e) => { state.evtLocation = e.target.value; });
  document.getElementById('evtDateInput')?.addEventListener('input', (e) => { state.evtDate = e.target.value; });
  document.getElementById('evtTimeInput')?.addEventListener('input', (e) => { state.evtTime = e.target.value; });
  document.getElementById('evtDescInput')?.addEventListener('input', (e) => { state.evtDesc = e.target.value; });
  document.getElementById('submitEventBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const title = state.evtTitle.trim();
    const loc = state.evtLocation.trim();
    const dt = state.evtDate.trim() || 'Upcoming';
    const tm = state.evtTime.trim() || '7:00 PM';
    if (title.length < 2 || !loc) { showBanner('Enter event title and location.', true); return; }
    try {
      const uid = state.user.uid;
      await addDoc(collection(db, 'events'), {
        title: title.slice(0, 120),
        description: state.evtDesc.trim().slice(0, 800),
        locationName: loc.slice(0, 160),
        eventDateText: dt.slice(0, 40),
        eventTimeText: tm.slice(0, 40),
        creatorId: uid,
        creatorName: (state.me?.displayName || state.user.displayName || 'Member').slice(0, 80),
        goingUids: [uid],
        interestedUids: [],
        invitedUids: [],
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      state.evtTitle = ''; state.evtLocation = ''; state.evtDate = ''; state.evtTime = ''; state.evtDesc = '';
      state.showCreateEventModal = false;
      showBanner('Event published!');
    } catch (e) {
      showBanner(e?.message || 'Could not create event.', true);
    }
  });
  document.querySelectorAll('.rsvp-going-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const ev = state.events.find((x) => x.id === btn.getAttribute('data-event-id'));
      if (!ev || !state.user) return;
      const uid = state.user.uid;
      const going = (ev.goingUids || []).includes(uid);
      await updateDoc(doc(db, 'events', ev.id), {
        goingUids: going ? arrayRemove(uid) : arrayUnion(uid),
        updatedAt: serverTimestamp()
      });
    });
  });
  document.querySelectorAll('.rsvp-int-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const ev = state.events.find((x) => x.id === btn.getAttribute('data-event-id'));
      if (!ev || !state.user) return;
      const uid = state.user.uid;
      const intr = (ev.interestedUids || []).includes(uid);
      await updateDoc(doc(db, 'events', ev.id), {
        interestedUids: intr ? arrayRemove(uid) : arrayUnion(uid),
        updatedAt: serverTimestamp()
      });
    });
  });

  // Secret Love
  document.querySelectorAll('[data-secret-tab]').forEach((btn) => {
    btn.addEventListener('click', () => { state.secretTab = btn.getAttribute('data-secret-tab'); render(); });
  });
  document.getElementById('toggleAcceptLoveBtn')?.addEventListener('click', async () => {
    if (!state.user || !state.me) return;
    await updateDoc(doc(db, 'users_public', state.user.uid), {
      acceptSecretLove: state.me.acceptSecretLove === false,
      updatedAt: serverTimestamp()
    });
  });
  document.querySelectorAll('.mark-love-read-btn').forEach((btn) => {
    btn.addEventListener('click', () => updateDoc(doc(db, 'secret_loves', btn.getAttribute('data-love-id')), { isRead: true }));
  });
  document.querySelectorAll('.block-love-btn').forEach((btn) => {
    btn.addEventListener('click', () => updateDoc(doc(db, 'secret_loves', btn.getAttribute('data-love-id')), { isBlocked: true }));
  });

  // Profile Pickers & Save
  document.getElementById('pickProfAvatarBtn')?.addEventListener('click', () => openPicker('gProfAvatarInput'));
  document.getElementById('pickProfCoverBtn')?.addEventListener('click', () => openPicker('gProfCoverInput'));
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
    showBanner('Profile saved across phone & desktop.');
  });

  // Notifications
  document.querySelectorAll('[data-notif-filter]').forEach((btn) => {
    btn.addEventListener('click', () => { state.notifFilter = btn.getAttribute('data-notif-filter'); render(); });
  });
  document.getElementById('markAllReadBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const batch = writeBatch(db);
    state.notifications.filter((n) => !n.isRead).forEach((n) => {
      batch.update(doc(db, 'users', state.user.uid, 'notifications', n.id), { isRead: true });
    });
    await batch.commit();
  });
  document.querySelectorAll('.del-notif-btn').forEach((btn) => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      if (state.user) await deleteDoc(doc(db, 'users', state.user.uid, 'notifications', btn.getAttribute('data-notif-id')));
    });
  });
  document.querySelectorAll('.open-notif-item').forEach((el) => {
    el.addEventListener('click', () => {
      const nid = el.getAttribute('data-notif-id');
      const typ = el.getAttribute('data-notif-type');
      const tid = el.getAttribute('data-target-id');
      if (state.user && nid) updateDoc(doc(db, 'users', state.user.uid, 'notifications', nid), { isRead: true }).catch(() => {});
      if (typ === 'friend_request') state.tab = 'friends';
      else if (typ === 'message') { state.tab = 'messenger'; if (tid) selectConversation(tid, true); }
      else if (typ === 'group_invite' || typ === 'sos_alert') { state.tab = 'groups'; state.activeGroupId = tid || null; }
      else if (typ === 'page_invite') state.tab = 'pages';
      else if (typ === 'secret_love' || typ === 'crush_match' || typ === 'mutual_unlock_24h') state.tab = 'secret_love';
      else state.tab = 'feed';
      render();
    });
  });

  // Search scopes, Recent Searches, Profile Tabs, Clear All Notifs
  document.querySelectorAll('[data-search-scope]').forEach((b) => {
    b.addEventListener('click', () => { state.searchScope = b.getAttribute('data-search-scope'); render(); });
  });
  document.getElementById('saveRecentSearchBtn')?.addEventListener('click', () => {
    const t = state.search.trim();
    if (!t) return;
    state.recentSearches = Array.from(new Set([t, ...state.recentSearches])).slice(0, 8);
    localStorage.setItem('hello_recent_searches', JSON.stringify(state.recentSearches));
    render();
  });
  document.querySelectorAll('.recent-search-pill').forEach((b) => {
    b.addEventListener('click', () => { state.search = b.getAttribute('data-term') || ''; render(); });
  });
  document.getElementById('clearRecentSearchBtn')?.addEventListener('click', () => {
    state.recentSearches = [];
    localStorage.removeItem('hello_recent_searches');
    render();
  });
  document.querySelectorAll('[data-prof-tab]').forEach((b) => {
    b.addEventListener('click', () => { state.profileTab = b.getAttribute('data-prof-tab'); render(); });
  });
  document.getElementById('clearAllNotifsBtn')?.addEventListener('click', async () => {
    if (!state.user) return;
    const batch = writeBatch(db);
    state.notifications.slice(0, 100).forEach((n) => batch.delete(doc(db, 'users', state.user.uid, 'notifications', n.id)));
    await batch.commit();
  });

  // Close modals
  document.querySelectorAll('.close-modals-btn').forEach((b) => {
    b.addEventListener('click', () => { closeAllModals(); render(); });
  });
  document.getElementById('modalBackdrop')?.addEventListener('click', (e) => {
    if (e.target.id === 'modalBackdrop') { closeAllModals(); render(); }
  });

  bindFeatureEvents();
}

async function toggleVoiceGesture() {
  const next = !state.vgEnabled;
  state.vgEnabled = next;
  setVoiceGestureStoredEnabled(next);
  if (next) {
    await vgController.start();
    showBanner('Voice & Gesture Control enabled.');
  } else {
    vgController.stop();
    showBanner('Voice & Gesture Control disabled.');
  }
  render();
}

const vgController = createVoiceGestureController({
  onCommand(cmd) {
    if (!state.user) return;
    if (cmd.type === 'navigate') {
      closeAllModals();
      const targetTab = cmd.tab === 'home' ? 'feed' : (cmd.tab === 'secretlove' ? 'secret_love' : cmd.tab);
      state.tab = targetTab;
      if (targetTab === 'profile') state.profileViewUid = null;
      if (targetTab === 'reels') state.reelActiveIndex = 0;
      render();
      return;
    }
    if (cmd.type === 'scroll') {
      if (state.tab === 'reels') {
        scrollToReelSlide((state.reelActiveIndex || 0) + (cmd.delta > 0 ? 1 : -1));
      } else {
        window.scrollBy({ top: cmd.delta || 400, behavior: 'smooth' });
      }
      return;
    }
    if (cmd.type === 'scrollTop') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    if (cmd.type === 'back') {
      if (state.activeStory || state.lightboxUri || state.detailListingId || state.editListingTarget || state.editPageTarget || state.managePageModal || state.showEditProfileModal) {
        closeAllModals();
      } else if (state.activeGroupId) {
        state.activeGroupId = null;
      } else if (state.activePageId) {
        state.activePageId = null;
      } else if (state.tab !== 'feed') {
        state.tab = 'feed';
      }
      render();
      return;
    }
    if (cmd.type === 'media') {
      if (state.tab === 'reels') {
        if (cmd.action === 'next') scrollToReelSlide((state.reelActiveIndex || 0) + 1);
        else if (cmd.action === 'prev') scrollToReelSlide((state.reelActiveIndex || 0) - 1);
        else if (cmd.action === 'pause' || cmd.action === 'play') {
          const feed = document.getElementById('reelsSnapFeed');
          const slide = feed?.querySelectorAll('.reel-slide')?.[state.reelActiveIndex || 0];
          const vid = slide?.querySelector('video');
          if (vid) {
            if (cmd.action === 'pause') vid.pause();
            else vid.play().catch(() => {});
          }
        }
      }
      return;
    }
    if (cmd.type === 'interact') {
      if (cmd.action === 'like') {
        if (state.tab === 'reels') {
          const feed = document.getElementById('reelsSnapFeed');
          const slide = feed?.querySelectorAll('.reel-slide')?.[state.reelActiveIndex || 0];
          const rid = slide?.getAttribute('data-reel-id');
          if (rid) updateDoc(doc(db, 'reels', rid), { likesCount: increment(1) }).catch(() => {});
        } else {
          const firstPost = state.posts.find((p) => !p.groupId && !p.pageId);
          if (firstPost) toggleLike(firstPost);
        }
      } else if (cmd.action === 'select') {
        if (state.tab === 'feed' && state.posts[0]) {
          toggleComments(state.posts[0].id);
        } else if (state.tab === 'marketplace' && state.marketplace[0]) {
          state.detailListingId = state.marketplace[0].id;
          render();
        } else if (state.tab === 'pages' && state.pages[0]) {
          state.activePageId = state.pages[0].id;
          render();
        }
      }
      return;
    }
    if (cmd.type === 'gesture') {
      if (cmd.gesture === 'swipe_left') {
        if (state.tab === 'reels') scrollToReelSlide((state.reelActiveIndex || 0) + 1);
        else window.scrollBy({ top: 420, behavior: 'smooth' });
      } else if (cmd.gesture === 'swipe_right') {
        if (state.tab === 'reels') scrollToReelSlide((state.reelActiveIndex || 0) - 1);
        else window.scrollBy({ top: -420, behavior: 'smooth' });
      } else if (cmd.gesture === 'open_palm') {
        if (state.tab === 'reels') {
          const feed = document.getElementById('reelsSnapFeed');
          const slide = feed?.querySelectorAll('.reel-slide')?.[state.reelActiveIndex || 0];
          const vid = slide?.querySelector('video');
          if (vid) {
            if (vid.paused) vid.play().catch(() => {});
            else vid.pause();
          }
        } else if (state.tab !== 'feed') {
          state.tab = 'feed';
          render();
        }
      } else if (cmd.gesture === 'thumbs_pinch') {
        if (state.tab === 'reels') {
          const feed = document.getElementById('reelsSnapFeed');
          const slide = feed?.querySelectorAll('.reel-slide')?.[state.reelActiveIndex || 0];
          const rid = slide?.getAttribute('data-reel-id');
          if (rid) updateDoc(doc(db, 'reels', rid), { likesCount: increment(1) }).catch(() => {});
        } else {
          const firstPost = state.posts.find((p) => !p.groupId && !p.pageId);
          if (firstPost) toggleLike(firstPost);
        }
      }
    }
  },
  onDisableRequest() {
    state.vgEnabled = false;
    setVoiceGestureStoredEnabled(false);
    render();
  },
  showToast(msg) {
    state.statusBanner = msg;
    state.statusIsError = false;
  }
});

initFeatures({
  state,
  db,
  auth,
  GoogleAuthProvider,
  reauthenticateWithPopup,
  fs: {
    collection, doc, getDoc, getDocs, setDoc, updateDoc, addDoc, deleteDoc,
    onSnapshot, query, where, limit, serverTimestamp, increment, arrayUnion, arrayRemove, writeBatch
  },
  helpers: {
    esc, fmtTime, showBanner, avatarHtml, mediaImgHtml, mediaVideoHtml, mediaSrc,
    iconSvg, resolveAuthor, isValidId, render, openOrCreateDm, selectConversation,
    openGetAppModal, uploadMediaBlob, setPreviewFile, toggleVoiceGesture,
    websiteUrl: WEBSITE_URL, apkUrl: APK_URL
  }
});

onAuthStateChanged(auth, async (u) => {
  state.user = u;
  if (u) {
    try {
      await ensureWebProfile(u);
      startListeners(u.uid);
      if (isVoiceGestureStoredEnabled()) {
        state.vgEnabled = true;
        vgController.start();
      }
    } catch (e) {
      state.authError = e?.message || 'Unable to load profile';
    }
  } else {
    clearSubs();
    vgController.stop();
    state.me = null;
  }
  render();
});

render();
