// Voice & Hand Gesture Control for HELLO Web App
// Off by default; cleanly manages Web Speech API + front-camera MediaPipe HandLandmarker (with Canvas fallback).

const STORAGE_KEY = 'hello_voice_gesture_enabled';
const MP_VISION_URL = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/vision_bundle.mjs';
const MP_WASM_URL = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm';
const MP_MODEL_URL = 'https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task';

export function isVoiceGestureStoredEnabled() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function setVoiceGestureStoredEnabled(enabled) {
  try {
    localStorage.setItem(STORAGE_KEY, enabled ? 'true' : 'false');
  } catch {
    // Ignore storage quota errors
  }
}

export function createVoiceGestureController({ onCommand, onDisableRequest, showToast }) {
  let enabled = false;
  let paused = false;
  let hudEl = null;
  let videoEl = null;
  let mediaStream = null;
  let recognition = null;
  let restartTimer = null;
  let rafId = null;
  let handLandmarker = null;
  let landmarkerLoading = false;
  let detectorMode = 'idle'; // 'mediapipe' | 'motion' | 'none' | 'idle'
  let voiceStatus = 'Off';
  let gestureStatus = 'Off';
  let lastCommandText = '';
  let lastVoiceExecAt = 0;
  let lastVoiceNormalized = '';
  let lastGestureAt = 0;
  let palmHoldStartAt = 0;
  let prevFrameData = null;
  let offscreenCanvas = null;
  let offscreenCtx = null;

  // Motion history for swipe detection: { x, y, t }
  const motionHistory = [];

  function notify(msg) {
    lastCommandText = msg;
    updateHud();
    if (typeof showToast === 'function') {
      showToast(msg);
    }
  }

  function ensureHud() {
    if (hudEl && document.body.contains(hudEl)) return hudEl;
    hudEl = document.createElement('div');
    hudEl.id = 'hello-vg-hud';
    hudEl.className = 'vg-hud';
    hudEl.innerHTML = `
      <div class="vg-hud-head">
        <span class="vg-hud-badge"><span class="vg-dot"></span> Voice &amp; Gesture</span>
        <div class="vg-hud-actions">
          <button type="button" class="vg-hud-btn" data-vg-action="pause" title="Pause or Resume">Pause</button>
          <button type="button" class="vg-hud-btn vg-hud-off" data-vg-action="off" title="Turn Off">Off</button>
        </div>
      </div>
      <div class="vg-hud-body">
        <div class="vg-cam-wrap">
          <video class="vg-cam-video" autoplay playsinline muted></video>
          <span class="vg-cam-mode">Cam</span>
        </div>
        <div class="vg-hud-info">
          <div class="vg-line"><b>Mic:</b> <span class="vg-voice-stat">Starting...</span></div>
          <div class="vg-line"><b>Hand:</b> <span class="vg-gesture-stat">Starting...</span></div>
          <div class="vg-cmd">Say "Reels", "Next", "Like", "Back" or swipe hand</div>
        </div>
      </div>
    `;
    document.body.appendChild(hudEl);
    videoEl = hudEl.querySelector('.vg-cam-video');
    hudEl.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-vg-action]');
      if (!btn) return;
      const action = btn.dataset.vgAction;
      if (action === 'off') {
        stop();
        setVoiceGestureStoredEnabled(false);
        if (typeof onDisableRequest === 'function') onDisableRequest();
      } else if (action === 'pause') {
        togglePause();
      }
    });
    return hudEl;
  }

  function removeHud() {
    if (hudEl && hudEl.parentNode) {
      hudEl.parentNode.removeChild(hudEl);
    }
    hudEl = null;
    videoEl = null;
  }

  function updateHud() {
    if (!hudEl) return;
    const vStat = hudEl.querySelector('.vg-voice-stat');
    const gStat = hudEl.querySelector('.vg-gesture-stat');
    const cmdEl = hudEl.querySelector('.vg-cmd');
    const modeEl = hudEl.querySelector('.vg-cam-mode');
    const pauseBtn = hudEl.querySelector('[data-vg-action="pause"]');
    const dot = hudEl.querySelector('.vg-dot');

    if (vStat) vStat.textContent = paused ? 'Paused' : voiceStatus;
    if (gStat) gStat.textContent = paused ? 'Paused' : gestureStatus;
    if (modeEl) modeEl.textContent = detectorMode === 'mediapipe' ? 'AI Hand' : detectorMode === 'motion' ? 'Motion' : 'Cam';
    if (cmdEl) {
      cmdEl.textContent = lastCommandText || 'Say "Home", "Reels", "Next", "Like", "Back" or use hand gestures';
    }
    if (pauseBtn) pauseBtn.textContent = paused ? 'Resume' : 'Pause';
    if (dot) dot.classList.toggle('is-paused', paused);
  }

  function dispatchVoiceText(rawText) {
    const clean = String(rawText || '').trim().toLowerCase();
    if (!clean) return;
    const now = Date.now();
    if (clean === lastVoiceNormalized && now - lastVoiceExecAt < 1400) return;

    const match = parseVoiceCommand(clean);
    if (!match) return;
    lastVoiceNormalized = clean;
    lastVoiceExecAt = now;
    notify(`Voice: ${match.label}`);
    if (match.type === 'disable') {
      stop();
      setVoiceGestureStoredEnabled(false);
      if (typeof onDisableRequest === 'function') onDisableRequest();
      return;
    }
    if (typeof onCommand === 'function') {
      onCommand(match);
    }
  }

  function parseVoiceCommand(text) {
    if (!text) return null;
    if (/(turn off|disable|stop)\s+(voice|gesture|control)/.test(text)) {
      return { type: 'disable', label: 'Turn Off Control' };
    }
    if (/(scroll\s*down|move\s*down|go\s*down|page\s*down|\bdown\b|নিচে)/.test(text)) {
      return { type: 'scroll', delta: 420, label: 'Scroll Down' };
    }
    if (/(scroll\s*up|move\s*up|go\s*up|page\s*up|\bup\b|উপরে)/.test(text)) {
      return { type: 'scroll', delta: -420, label: 'Scroll Up' };
    }
    if (/(scroll\s*top|back\s*to\s*top|\btop\b)/.test(text)) {
      return { type: 'scrollTop', label: 'Top of Page' };
    }
    if (/(\bgo\s*back\b|\bback\b|পিছনে|ফিরে যাও)/.test(text)) {
      return { type: 'back', label: 'Back' };
    }
    if (/(\bnext\b|\bskip\b|পরের)/.test(text)) {
      return { type: 'media', action: 'next', label: 'Next' };
    }
    if (/(\bprevious\b|\bprev\b|আগের)/.test(text)) {
      return { type: 'media', action: 'prev', label: 'Previous' };
    }
    if (/(\bpause\b|\bstop video\b|থামাও)/.test(text)) {
      return { type: 'media', action: 'pause', label: 'Pause' };
    }
    if (/(\bplay\b|\bresume\b|চালাও)/.test(text)) {
      return { type: 'media', action: 'play', label: 'Play' };
    }
    if (/(\blike\b|\blove\b|লাইক)/.test(text)) {
      return { type: 'interact', action: 'like', label: 'Like' };
    }
    if (/(\bselect\b|\bopen\b|ওপেন)/.test(text)) {
      return { type: 'interact', action: 'select', label: 'Select' };
    }

    const tabRoutes = [
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bhome\b|\bfeed\b|হোম)/, tab: 'home', label: 'Open Home' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\breels?\b|\bvideos?\b|রিল)/, tab: 'reels', label: 'Open Reels' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bfriends?\b|ফ্রেন্ড)/, tab: 'friends', label: 'Open Friends' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bgroups?\b|গ্রুপ)/, tab: 'groups', label: 'Open Groups' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bmarketplace\b|\bmarket\b|\bshop\b|মার্কেট)/, tab: 'marketplace', label: 'Open Marketplace' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bpages?\b|পেজ)/, tab: 'pages', label: 'Open Pages' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bevents?\b|ইভেন্ট)/, tab: 'events', label: 'Open Events' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bmessenger\b|\bmessages?\b|\bchat\b|মেসেজ)/, tab: 'messenger', label: 'Open Messenger' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bnotifications?\b|\balerts?\b)/, tab: 'notifications', label: 'Open Notifications' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bsearch\b|সার্চ)/, tab: 'search', label: 'Open Search' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bprofile\b|\bmy account\b|প্রোফাইল)/, tab: 'profile', label: 'Open Profile' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bsettings?\b|সেটিংস)/, tab: 'settings', label: 'Open Settings' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(secret\s*love)/, tab: 'secretlove', label: 'Open Secret Love' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bsos\b|emergency)/, tab: 'sos', label: 'Open SOS' },
      { re: /(open\s+|go\s+to\s+|show\s+)?(\bhub\b|explore)/, tab: 'hub', label: 'Open Hub' }
    ];
    for (const r of tabRoutes) {
      if (r.re.test(text)) {
        return { type: 'navigate', tab: r.tab, label: r.label };
      }
    }
    return null;
  }

  function startVoice() {
    const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRec) {
      voiceStatus = 'Unsupported browser';
      updateHud();
      return;
    }
    stopVoice();
    try {
      recognition = new SpeechRec();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        voiceStatus = 'Listening';
        updateHud();
      };

      recognition.onresult = (event) => {
        if (!enabled || paused) return;
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const res = event.results[i];
          const transcript = res[0]?.transcript || '';
          if (res.isFinal) {
            dispatchVoiceText(transcript);
          } else if (transcript.trim().split(/\s+/).length <= 3) {
            dispatchVoiceText(transcript);
          }
        }
      };

      recognition.onerror = (event) => {
        const err = event?.error || 'error';
        if (err === 'not-allowed' || err === 'service-not-allowed') {
          voiceStatus = 'Mic denied';
          updateHud();
          return;
        }
        if (err === 'no-speech' || err === 'aborted') {
          return;
        }
        voiceStatus = `Mic: ${err}`;
        updateHud();
      };

      recognition.onend = () => {
        if (enabled && !paused && document.visibilityState === 'visible' && voiceStatus !== 'Mic denied') {
          clearTimeout(restartTimer);
          restartTimer = setTimeout(() => {
            if (enabled && !paused) startVoice();
          }, 450);
        } else if (!enabled || paused) {
          voiceStatus = paused ? 'Paused' : 'Off';
          updateHud();
        }
      };

      recognition.start();
    } catch {
      voiceStatus = 'Mic unavailable';
      updateHud();
    }
  }

  function stopVoice() {
    clearTimeout(restartTimer);
    restartTimer = null;
    if (recognition) {
      try { recognition.onend = null; recognition.onresult = null; recognition.onerror = null; recognition.abort(); } catch {}
      recognition = null;
    }
    voiceStatus = 'Off';
  }

  async function initMediaPipeLandmarker() {
    if (handLandmarker || landmarkerLoading) return handLandmarker;
    landmarkerLoading = true;
    try {
      const vision = await import(/* @vite-ignore */ MP_VISION_URL);
      const fileset = await vision.FilesetResolver.forVisionTasks(MP_WASM_URL);
      handLandmarker = await vision.HandLandmarker.createFromOptions(fileset, {
        baseOptions: {
          modelAssetPath: MP_MODEL_URL,
          delegate: 'GPU'
        },
        runningMode: 'VIDEO',
        numHands: 1,
        minHandDetectionConfidence: 0.55,
        minHandPresenceConfidence: 0.55,
        minTrackingConfidence: 0.55
      });
      detectorMode = 'mediapipe';
      gestureStatus = 'MediaPipe Ready';
      updateHud();
      return handLandmarker;
    } catch {
      detectorMode = 'motion';
      gestureStatus = 'Motion Fallback';
      updateHud();
      return null;
    } finally {
      landmarkerLoading = false;
    }
  }

  async function startCameraAndGestures() {
    if (!navigator.mediaDevices?.getUserMedia) {
      gestureStatus = 'Camera unsupported';
      detectorMode = 'none';
      updateHud();
      return;
    }
    stopCamera();
    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 320 }, height: { ideal: 240 } },
        audio: false
      });
      if (!enabled) {
        stopCamera();
        return;
      }
      ensureHud();
      if (videoEl) {
        videoEl.srcObject = mediaStream;
        await videoEl.play().catch(() => {});
      }
      gestureStatus = 'Active (loading AI...)';
      detectorMode = 'motion';
      updateHud();

      // Load MediaPipe in background while motion fallback works immediately
      initMediaPipeLandmarker();
      runDetectionLoop();
    } catch (err) {
      gestureStatus = err?.name === 'NotAllowedError' ? 'Camera denied' : 'Camera unavailable';
      detectorMode = 'none';
      updateHud();
    }
  }

  function stopCamera() {
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (mediaStream) {
      try {
        mediaStream.getTracks().forEach((t) => { try { t.stop(); } catch {} });
      } catch {}
      mediaStream = null;
    }
    if (videoEl) {
      try { videoEl.pause(); videoEl.srcObject = null; } catch {}
    }
    motionHistory.length = 0;
    palmHoldStartAt = 0;
    prevFrameData = null;
    gestureStatus = 'Off';
  }

  function emitGesture(type, label) {
    const now = Date.now();
    if (now - lastGestureAt < 1300) return;
    lastGestureAt = now;
    motionHistory.length = 0;
    palmHoldStartAt = 0;
    notify(`Gesture: ${label}`);
    if (typeof onCommand === 'function') {
      if (type === 'swipe_left') {
        onCommand({ type: 'gesture', gesture: 'swipe_left', label });
      } else if (type === 'swipe_right') {
        onCommand({ type: 'gesture', gesture: 'swipe_right', label });
      } else if (type === 'open_palm') {
        onCommand({ type: 'gesture', gesture: 'open_palm', label });
      } else if (type === 'thumbs_pinch') {
        onCommand({ type: 'gesture', gesture: 'thumbs_pinch', label });
      }
    }
  }

  function dist(a, b) {
    return Math.hypot((a.x || 0) - (b.x || 0), (a.y || 0) - (b.y || 0));
  }

  function analyzeLandmarks(landmarks, now) {
    if (!landmarks || !landmarks.length) {
      palmHoldStartAt = 0;
      return;
    }
    const lm = landmarks[0];
    const wrist = lm[0];
    const indexMcp = lm[5];
    const palmScale = Math.max(0.05, dist(wrist, indexMcp));

    // Finger extension checks (tip higher [smaller y] than PIP joint)
    const indexExt = lm[8].y < lm[6].y - 0.02;
    const middleExt = lm[12].y < lm[10].y - 0.02;
    const ringExt = lm[16].y < lm[14].y - 0.02;
    const pinkyExt = lm[20].y < lm[18].y - 0.02;
    const thumbUp = lm[4].y < lm[3].y - 0.03 && lm[4].y < lm[5].y - 0.03;

    // 1. Pinch detection (thumb tip close to index tip, while other fingers not all open)
    const pinchDist = dist(lm[4], lm[8]) / palmScale;
    if (pinchDist < 0.38 && (!middleExt || !ringExt)) {
      emitGesture('thumbs_pinch', 'Pinch -> Like/Select');
      return;
    }

    // 2. Thumbs-up detection
    if (thumbUp && !indexExt && !middleExt && !ringExt && !pinkyExt) {
      emitGesture('thumbs_pinch', 'Thumbs Up -> Like/Select');
      return;
    }

    // 3. Swipe left / right tracking on wrist/palm center
    const cx = (lm[0].x + lm[9].x) * 0.5;
    motionHistory.push({ x: cx, t: now });
    while (motionHistory.length > 0 && now - motionHistory[0].t > 550) {
      motionHistory.shift();
    }
    if (motionHistory.length >= 4) {
      const dx = cx - motionHistory[0].x;
      // Front camera image is mirrored for natural UX: moving hand right-to-left or left-to-right
      if (dx > 0.24) {
        emitGesture('swipe_left', 'Swipe -> Next / Down');
        return;
      }
      if (dx < -0.24) {
        emitGesture('swipe_right', 'Swipe -> Prev / Up');
        return;
      }
    }

    // 4. Open palm hold (~700ms steady)
    if (indexExt && middleExt && ringExt && pinkyExt) {
      if (!palmHoldStartAt) palmHoldStartAt = now;
      if (now - palmHoldStartAt > 720) {
        emitGesture('open_palm', 'Open Palm -> Pause / Back');
      }
    } else {
      palmHoldStartAt = 0;
    }
  }

  function analyzeMotionFallback(now) {
    if (!videoEl || videoEl.readyState < 2) return;
    if (!offscreenCanvas) {
      offscreenCanvas = document.createElement('canvas');
      offscreenCanvas.width = 64;
      offscreenCanvas.height = 48;
      offscreenCtx = offscreenCanvas.getContext('2d', { willReadFrequently: true });
    }
    if (!offscreenCtx) return;
    offscreenCtx.drawImage(videoEl, 0, 0, 64, 48);
    const frame = offscreenCtx.getImageData(0, 0, 64, 48).data;
    if (!prevFrameData) {
      prevFrameData = new Uint8ClampedArray(frame);
      return;
    }
    let sumX = 0;
    let count = 0;
    for (let y = 0; y < 48; y++) {
      for (let x = 0; x < 64; x++) {
        const idx = (y * 64 + x) * 4;
        const diff =
          Math.abs(frame[idx] - prevFrameData[idx]) +
          Math.abs(frame[idx + 1] - prevFrameData[idx + 1]) +
          Math.abs(frame[idx + 2] - prevFrameData[idx + 2]);
        if (diff > 95) {
          sumX += x / 64;
          count++;
        }
      }
    }
    prevFrameData.set(frame);

    if (count > 85 && count < 1800) {
      const cx = sumX / count;
      motionHistory.push({ x: cx, t: now });
      while (motionHistory.length > 0 && now - motionHistory[0].t > 550) {
        motionHistory.shift();
      }
      if (motionHistory.length >= 4) {
        const dx = cx - motionHistory[0].x;
        if (dx > 0.22) emitGesture('swipe_left', 'Swipe -> Next / Down');
        else if (dx < -0.22) emitGesture('swipe_right', 'Swipe -> Prev / Up');
      }
    }
  }

  let lastDetectFrameAt = 0;
  function runDetectionLoop() {
    if (!enabled || paused || !mediaStream) return;
    const now = performance.now();
    if (now - lastDetectFrameAt >= 90 && videoEl && videoEl.readyState >= 2) {
      lastDetectFrameAt = now;
      if (handLandmarker && detectorMode === 'mediapipe') {
        try {
          const result = handLandmarker.detectForVideo(videoEl, now);
          analyzeLandmarks(result?.landmarks, now);
        } catch {
          detectorMode = 'motion';
          gestureStatus = 'Motion Fallback';
          updateHud();
        }
      } else {
        analyzeMotionFallback(now);
      }
    }
    rafId = requestAnimationFrame(runDetectionLoop);
  }

  function handleVisibilityChange() {
    if (!enabled) return;
    if (document.visibilityState === 'hidden') {
      stopVoice();
      stopCamera();
      updateHud();
    } else if (document.visibilityState === 'visible' && !paused) {
      startVoice();
      startCameraAndGestures();
    }
  }

  function togglePause() {
    if (!enabled) return;
    paused = !paused;
    if (paused) {
      stopVoice();
      stopCamera();
    } else {
      startVoice();
      startCameraAndGestures();
    }
    updateHud();
  }

  async function start() {
    if (enabled) return;
    enabled = true;
    paused = false;
    ensureHud();
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('pagehide', stop);
    startVoice();
    await startCameraAndGestures();
    updateHud();
  }

  function stop() {
    enabled = false;
    paused = false;
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener('pagehide', stop);
    stopVoice();
    stopCamera();
    if (handLandmarker) {
      try { handLandmarker.close(); } catch {}
      handLandmarker = null;
    }
    detectorMode = 'idle';
    removeHud();
  }

  return {
    isEnabled: () => enabled,
    start,
    stop,
    setEnabled(next) {
      setVoiceGestureStoredEnabled(!!next);
      if (next) return start();
      stop();
      return Promise.resolve();
    }
  };
}
