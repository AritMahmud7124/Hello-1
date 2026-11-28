package com.example.voicegesture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.ui.navigation.HelloRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class VoiceGestureEngine(
  context: Context,
  private val onAction: (VoiceGestureAction) -> Unit
) {
  private val appContext = context.applicationContext
  private val mainHandler = Handler(Looper.getMainLooper())
  private var analysisExecutor: ExecutorService? = null

  private val _status = MutableStateFlow(VoiceGestureStatus())
  val status: StateFlow<VoiceGestureStatus> = _status.asStateFlow()

  private var speechRecognizer: SpeechRecognizer? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var boundLifecycleOwner: LifecycleOwner? = null
  private var boundPreviewView: PreviewView? = null

  private var isStarted = false
  private var isPaused = false
  private var lastSpeechActionMs = 0L
  private var lastSpeechPhrase = ""
  private var lastGestureMs = 0L

  // Motion grid state for lightweight front-camera hand gesture detection
  private var prevGrid: IntArray? = null
  private var motionWindowStartMs = 0L
  private var motionOriginX = 0f
  private var motionOriginY = 0f
  private var motionSumDx = 0f
  private var motionSumDy = 0f
  private var largeSpreadHoldStartMs = 0L
  private var compactPulseCount = 0

  fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
    boundLifecycleOwner = lifecycleOwner
    boundPreviewView = previewView
    val micOk = hasPermission(Manifest.permission.RECORD_AUDIO)
    val camOk = hasPermission(Manifest.permission.CAMERA)

    isStarted = true
    _status.update {
      it.copy(
        isEnabled = true,
        isPaused = isPaused,
        hasMicPermission = micOk,
        hasCameraPermission = camOk
      )
    }

    if (isPaused) {
      _status.update { it.copy(voiceState = "Paused", gestureState = "Paused") }
      return
    }

    if (micOk) {
      startSpeechLoop()
    } else {
      _status.update { it.copy(voiceState = "Mic permission needed") }
    }

    if (camOk) {
      startCameraAnalysis(lifecycleOwner, previewView)
    } else {
      _status.update { it.copy(gestureState = "Camera permission needed") }
    }
  }

  fun attachPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
    val changed = boundPreviewView !== previewView || boundLifecycleOwner !== lifecycleOwner
    boundLifecycleOwner = lifecycleOwner
    boundPreviewView = previewView
    if (isStarted && !isPaused && hasPermission(Manifest.permission.CAMERA) && changed) {
      startCameraAnalysis(lifecycleOwner, previewView)
    }
  }

  fun setPaused(paused: Boolean) {
    isPaused = paused
    if (paused) {
      stopHardwareOnly()
      _status.update {
        it.copy(
          isPaused = true,
          voiceState = "Paused",
          gestureState = "Paused"
        )
      }
    } else {
      _status.update { it.copy(isPaused = false) }
      val owner = boundLifecycleOwner
      if (isStarted && owner != null) {
        start(owner, boundPreviewView)
      }
    }
  }

  fun stop() {
    isStarted = false
    isPaused = false
    boundLifecycleOwner = null
    boundPreviewView = null
    stopHardwareOnly()
    _status.value = VoiceGestureStatus()
  }

  private fun stopHardwareOnly() {
    mainHandler.removeCallbacksAndMessages(null)
    mainHandler.post {
      runCatching { speechRecognizer?.stopListening() }
      runCatching { speechRecognizer?.cancel() }
      runCatching { speechRecognizer?.destroy() }
      speechRecognizer = null
    }
    runCatching {
      cameraProvider?.unbindAll()
    }
    runCatching {
      analysisExecutor?.shutdownNow()
    }
    analysisExecutor = null
    resetMotionTracker()
  }

  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
  }

  private fun startSpeechLoop() {
    mainHandler.post {
      if (!isStarted || isPaused || !hasPermission(Manifest.permission.RECORD_AUDIO)) return@post
      if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
        _status.update { it.copy(voiceState = "Speech unavailable on device") }
        return@post
      }
      runCatching {
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer.setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            if (isStarted && !isPaused) {
              _status.update { it.copy(voiceState = "Listening...") }
            }
          }

          override fun onBeginningOfSpeech() {}
          override fun onRmsChanged(rmsdB: Float) {}
          override fun onBufferReceived(buffer: ByteArray?) {}
          override fun onEndOfSpeech() {}

          override fun onError(error: Int) {
            if (!isStarted || isPaused) return
            when (error) {
              SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                _status.update { it.copy(hasMicPermission = false, voiceState = "Mic permission denied") }
              }
              SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
              SpeechRecognizer.ERROR_CLIENT -> {
                _status.update { it.copy(voiceState = "Reconnecting mic...") }
                scheduleSpeechRestart(700L)
              }
              SpeechRecognizer.ERROR_NO_MATCH,
              SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                _status.update { it.copy(voiceState = "Listening...") }
                scheduleSpeechRestart(250L)
              }
              else -> {
                _status.update { it.copy(voiceState = "Listening...") }
                scheduleSpeechRestart(650L)
              }
            }
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            handleSpokenMatches(matches, isPartial = false)
            if (isStarted && !isPaused) {
              scheduleSpeechRestart(220L)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            handleSpokenMatches(matches, isPartial = true)
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
          putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        recognizer.startListening(intent)
        _status.update { it.copy(voiceState = "Listening...") }
      }.onFailure {
        _status.update { it.copy(voiceState = "Speech unavailable") }
      }
    }
  }

  private fun scheduleSpeechRestart(delayMs: Long) {
    mainHandler.removeCallbacksAndMessages(null)
    mainHandler.postDelayed({
      if (!isStarted || isPaused || !hasPermission(Manifest.permission.RECORD_AUDIO)) return@postDelayed
      runCatching {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
          putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        speechRecognizer?.startListening(intent) ?: startSpeechLoop()
      }.onFailure {
        startSpeechLoop()
      }
    }, delayMs)
  }

  private fun handleSpokenMatches(matches: List<String>, isPartial: Boolean) {
    val raw = matches.firstOrNull()?.trim()?.lowercase().orEmpty()
    if (raw.isBlank()) return
    val parsed = parseVoiceCommand(raw) ?: return
    val now = System.currentTimeMillis()
    if (now - lastSpeechActionMs < 950L && (isPartial || raw == lastSpeechPhrase)) {
      return
    }
    lastSpeechActionMs = now
    lastSpeechPhrase = raw
    val label = describeAction(parsed)
    _status.update { it.copy(lastCommand = "Voice: $label") }
    onAction(parsed)
  }

  private fun startCameraAnalysis(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
    val providerFuture = ProcessCameraProvider.getInstance(appContext)
    providerFuture.addListener({
      if (!isStarted || isPaused || !hasPermission(Manifest.permission.CAMERA)) return@addListener
      runCatching {
        val provider = providerFuture.get()
        cameraProvider = provider
        provider.unbindAll()

        val executor = analysisExecutor?.takeIf { !it.isShutdown }
          ?: Executors.newSingleThreadExecutor().also { analysisExecutor = it }

        val analysis = ImageAnalysis.Builder()
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .build()
        analysis.setAnalyzer(executor) { imageProxy ->
          analyzeCameraFrame(imageProxy)
        }

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        if (previewView != null) {
          val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }
          provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        } else {
          provider.bindToLifecycle(lifecycleOwner, selector, analysis)
        }
        _status.update { it.copy(gestureState = "Hand Camera Active") }
      }.onFailure {
        _status.update { it.copy(gestureState = "Camera unavailable") }
      }
    }, ContextCompat.getMainExecutor(appContext))
  }

  private fun analyzeCameraFrame(imageProxy: ImageProxy) {
    try {
      if (!isStarted || isPaused) return
      val plane = imageProxy.planes.firstOrNull() ?: return
      val width = imageProxy.width
      val height = imageProxy.height
      if (width <= 0 || height <= 0) return

      val buffer = plane.buffer
      val rowStride = plane.rowStride
      val pixelStride = plane.pixelStride

      val cols = 20
      val rows = 15
      val currentGrid = IntArray(cols * rows)
      val stepX = (width / cols).coerceAtLeast(1)
      val stepY = (height / rows).coerceAtLeast(1)

      for (r in 0 until rows) {
        val py = (r * stepY).coerceIn(0, height - 1)
        val rowOffset = py * rowStride
        for (c in 0 until cols) {
          val px = (c * stepX).coerceIn(0, width - 1)
          val idx = rowOffset + px * pixelStride
          currentGrid[r * cols + c] = if (idx in 0 until buffer.limit()) {
            buffer.get(idx).toInt() and 0xFF
          } else 0
        }
      }

      val previous = prevGrid
      prevGrid = currentGrid
      if (previous == null) return

      var activeCount = 0
      var sumX = 0f
      var sumY = 0f
      var minX = cols
      var maxX = 0
      var minY = rows
      var maxY = 0

      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val diff = abs(currentGrid[r * cols + c] - previous[r * cols + c])
          if (diff > 22) {
            activeCount++
            sumX += c.toFloat() / cols.toFloat()
            sumY += r.toFloat() / rows.toFloat()
            if (c < minX) minX = c
            if (c > maxX) maxX = c
            if (r < minY) minY = r
            if (r > maxY) maxY = r
          }
        }
      }

      val now = System.currentTimeMillis()
      val totalCells = cols * rows
      val motionFraction = activeCount.toFloat() / totalCells.toFloat()

      if (now - lastGestureMs < 1300L) {
        motionWindowStartMs = 0L
        largeSpreadHoldStartMs = 0L
        compactPulseCount = 0
        return
      }

      if (motionFraction < 0.04f) {
        if (now - motionWindowStartMs > 450L) {
          motionWindowStartMs = 0L
          largeSpreadHoldStartMs = 0L
          compactPulseCount = 0
        }
        return
      }

      val cx = sumX / activeCount.toFloat()
      val cy = sumY / activeCount.toFloat()
      val spreadX = (maxX - minX).toFloat() / cols.toFloat()
      val spreadY = (maxY - minY).toFloat() / rows.toFloat()
      val area = spreadX * spreadY

      if (motionWindowStartMs == 0L || now - motionWindowStartMs > 520L) {
        motionWindowStartMs = now
        motionOriginX = cx
        motionOriginY = cy
        motionSumDx = 0f
        motionSumDy = 0f
        compactPulseCount = 0
      } else {
        motionSumDx = cx - motionOriginX
        motionSumDy = cy - motionOriginY
      }

      // Open Palm / Wave Hold: large spread held briefly with little net drift
      if (area > 0.32f && motionFraction > 0.16f && abs(motionSumDx) < 0.14f) {
        if (largeSpreadHoldStartMs == 0L) {
          largeSpreadHoldStartMs = now
        } else if (now - largeSpreadHoldStartMs >= 360L) {
          emitGesture(VoiceGestureAction.GesturePalm, "Open Palm (Pause/Back)", now)
          return
        }
      } else {
        largeSpreadHoldStartMs = 0L
      }

      // Swipe Left / Right (front camera mirrored -> invert horizontal interpretation for natural viewer direction)
      if (abs(motionSumDx) > 0.21f && abs(motionSumDx) > abs(motionSumDy) * 1.35f) {
        val action = if (motionSumDx < 0f) {
          VoiceGestureAction.MediaNext to "Swipe Left (Next)"
        } else {
          VoiceGestureAction.MediaPrevious to "Swipe Right (Previous)"
        }
        emitGesture(action.first, action.second, now)
        return
      }

      // Vertical Swipe Up / Down
      if (abs(motionSumDy) > 0.22f && abs(motionSumDy) > abs(motionSumDx) * 1.35f) {
        val action = if (motionSumDy < 0f) {
          VoiceGestureAction.MediaNext to "Wave Up (Next)"
        } else {
          VoiceGestureAction.MediaPrevious to "Wave Down (Previous)"
        }
        emitGesture(action.first, action.second, now)
        return
      }

      // Compact pinch / thumbs-up pulse near center
      if (motionFraction in 0.045f..0.14f && area in 0.015f..0.16f && cx in 0.22f..0.78f && cy in 0.22f..0.85f) {
        compactPulseCount++
        if (compactPulseCount >= 4) {
          emitGesture(VoiceGestureAction.Like, "Pinch / Thumbs Up (Like)", now)
        }
      }
    } finally {
      imageProxy.close()
    }
  }

  private fun emitGesture(action: VoiceGestureAction, label: String, nowMs: Long) {
    lastGestureMs = nowMs
    resetMotionTracker()
    mainHandler.post {
      if (!isStarted || isPaused) return@post
      _status.update { it.copy(lastCommand = "Gesture: $label") }
      onAction(action)
    }
  }

  private fun resetMotionTracker() {
    prevGrid = null
    motionWindowStartMs = 0L
    motionSumDx = 0f
    motionSumDy = 0f
    largeSpreadHoldStartMs = 0L
    compactPulseCount = 0
  }

  companion object {
    fun parseVoiceCommand(rawText: String): VoiceGestureAction? {
      val t = rawText.trim().lowercase()
      if (t.isBlank()) return null

      return when {
        hasAny(t, "stop voice", "disable voice", "turn off voice", "voice off", "stop hands free", "বন্ধ করো") ->
          VoiceGestureAction.Disable
        hasAny(t, "scroll to top", "go to top", "back to top") ->
          VoiceGestureAction.Scroll(direction = -1, toTop = true)
        hasAny(t, "scroll down", "move down", "page down", "নিচে") ->
          VoiceGestureAction.Scroll(direction = 1)
        hasAny(t, "scroll up", "move up", "page up", "উপরে") ->
          VoiceGestureAction.Scroll(direction = -1)
        hasAny(t, "next reel", "next video", "skip reel", "skip", "next", "পরের") ->
          VoiceGestureAction.MediaNext
        hasAny(t, "previous reel", "prev reel", "last reel", "previous", "prev", "আগের") ->
          VoiceGestureAction.MediaPrevious
        hasAny(t, "pause", "play video", "resume video", "play", "resume", "পজ", "চালাও") ->
          VoiceGestureAction.MediaPlayPause
        hasAny(t, "go back", "back", "পিছনে") ->
          VoiceGestureAction.Back
        hasAny(t, "like", "love this", "heart", "লাইক") ->
          VoiceGestureAction.Like
        hasAny(t, "select", "open post", "open item", "click") ->
          VoiceGestureAction.Select
        hasAny(t, "open home", "home", "feed", "হোম", "বাড়ি") ->
          VoiceGestureAction.Navigate(HelloRoutes.HOME, "Home")
        hasAny(t, "open reels", "reels", "reel", "shorts", "রিলস", "রিল") ->
          VoiceGestureAction.Navigate(HelloRoutes.REELS, "Reels")
        hasAny(t, "open marketplace", "marketplace", "market", "shop", "মার্কেট", "বাজার") ->
          VoiceGestureAction.Navigate(HelloRoutes.MARKETPLACE, "Marketplace")
        hasAny(t, "open pages", "pages", "page", "পেজ") ->
          VoiceGestureAction.Navigate(HelloRoutes.PAGES, "Pages")
        hasAny(t, "open groups", "groups", "group", "গ্রুপ") ->
          VoiceGestureAction.Navigate(HelloRoutes.GROUPS, "Groups")
        hasAny(t, "open friends", "friends", "friend", "বন্ধু") ->
          VoiceGestureAction.Navigate(HelloRoutes.FRIENDS, "Friends")
        hasAny(t, "open messenger", "messenger", "messages", "chat", "মেসেঞ্জার", "চ্যাট") ->
          VoiceGestureAction.Navigate(HelloRoutes.MESSENGER, "Messenger")
        hasAny(t, "open profile", "profile", "my profile", "প্রোফাইল") ->
          VoiceGestureAction.Navigate(HelloRoutes.PROFILE, "Profile")
        hasAny(t, "open settings", "settings", "setting", "সেটিংস") ->
          VoiceGestureAction.Navigate(HelloRoutes.SETTINGS, "Settings")
        hasAny(t, "open stories", "stories", "story", "স্টোরি") ->
          VoiceGestureAction.Navigate(HelloRoutes.STORIES, "Stories")
        hasAny(t, "open events", "events", "event") ->
          VoiceGestureAction.Navigate(HelloRoutes.EVENTS, "Events")
        hasAny(t, "open search", "search", "খুঁজুন") ->
          VoiceGestureAction.Navigate(HelloRoutes.SEARCH, "Search")
        hasAny(t, "notifications", "alerts") ->
          VoiceGestureAction.Navigate(HelloRoutes.NOTIFICATIONS, "Notifications")
        hasAny(t, "secret love", "crush") ->
          VoiceGestureAction.Navigate(HelloRoutes.SECRET_LOVE, "Secret Love")
        hasAny(t, "sos", "emergency") ->
          VoiceGestureAction.Navigate(HelloRoutes.SOS, "SOS")
        hasAny(t, "hub", "menu") ->
          VoiceGestureAction.Navigate(HelloRoutes.HUB, "Hub Menu")
        else -> null
      }
    }

    private fun hasAny(text: String, vararg phrases: String): Boolean {
      return phrases.any { phrase ->
        if (phrase.any { it.code > 127 }) {
          text.contains(phrase)
        } else {
          Regex("\\b${Regex.escape(phrase)}\\b").containsMatchIn(text) || text == phrase
        }
      }
    }

    private fun describeAction(action: VoiceGestureAction): String = when (action) {
      is VoiceGestureAction.Navigate -> "Open ${action.label}"
      is VoiceGestureAction.Scroll -> when {
        action.toTop -> "Top"
        action.direction > 0 -> "Scroll Down"
        else -> "Scroll Up"
      }
      VoiceGestureAction.Back -> "Go Back"
      VoiceGestureAction.MediaNext -> "Next"
      VoiceGestureAction.MediaPrevious -> "Previous"
      VoiceGestureAction.MediaPlayPause -> "Play / Pause"
      VoiceGestureAction.GesturePalm -> "Pause / Back"
      VoiceGestureAction.Like -> "Like"
      VoiceGestureAction.Select -> "Select"
      VoiceGestureAction.Disable -> "Turn Off"
    }
  }
}
