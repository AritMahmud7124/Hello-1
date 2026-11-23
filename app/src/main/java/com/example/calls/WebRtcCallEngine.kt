package com.example.calls

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.CallSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcCallEngine(
  private val context: Context,
  private val isVideoCall: Boolean,
  private val isCaller: Boolean,
  private val onSendSignal: (type: String, sdp: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) -> Unit
) {
  companion object {
    private const val TAG = "WebRtcCallEngine"
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  val eglBase: EglBase = EglBase.create()

  private var factory: PeerConnectionFactory? = null
  private var peerConnection: PeerConnection? = null
  private var audioSource: AudioSource? = null
  private var localAudioTrack: AudioTrack? = null
  private var videoSource: VideoSource? = null
  private var localVideoTrack: VideoTrack? = null
  private var remoteVideoTrack: VideoTrack? = null
  private var videoCapturer: CameraVideoCapturer? = null
  private var surfaceTextureHelper: SurfaceTextureHelper? = null
  private var localRenderer: SurfaceViewRenderer? = null
  private var remoteRenderer: SurfaceViewRenderer? = null

  private val processedSignalIds = mutableSetOf<String>()
  private val pendingRemoteIce = mutableListOf<IceCandidate>()
  private var remoteDescriptionSet = false
  private var started = false
  private var disposed = false
  private var offerSent = false
  private var answerSent = false

  private val _iceState = MutableStateFlow("NEW")
  val iceState: StateFlow<String> = _iceState.asStateFlow()

  private val _hasRemoteVideo = MutableStateFlow(false)
  val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

  fun start() {
    if (started || disposed) return
    started = true
    runCatching {
      PeerConnectionFactory.initialize(
        PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
          .createInitializationOptions()
      )
      val audioDeviceModule = JavaAudioDeviceModule.builder(context.applicationContext)
        .createAudioDeviceModule()

      val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
      val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

      factory = PeerConnectionFactory.builder()
        .setAudioDeviceModule(audioDeviceModule)
        .setVideoEncoderFactory(encoderFactory)
        .setVideoDecoderFactory(decoderFactory)
        .createPeerConnectionFactory()
      audioDeviceModule.release()

      createLocalMedia()
      createPeerConnection()

      if (isCaller && !offerSent) {
        offerSent = true
        createAndSendOffer()
      }
    }.onFailure { err ->
      Log.w(TAG, "Failed to initialize WebRTC engine", err)
    }
  }

  private fun createLocalMedia() {
    val pcf = factory ?: return
    val audioConstraints = MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
    }
    audioSource = pcf.createAudioSource(audioConstraints)
    localAudioTrack = pcf.createAudioTrack("HELLO_AUDIO", audioSource).apply {
      setEnabled(true)
    }

    if (isVideoCall) {
      val enumerator = Camera2Enumerator(context.applicationContext)
      val frontCam = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        ?: enumerator.deviceNames.firstOrNull()
      if (frontCam != null) {
        val capturer = enumerator.createCapturer(frontCam, null)
        if (capturer != null) {
          videoCapturer = capturer
          val helper = SurfaceTextureHelper.create("HelloCaptureThread", eglBase.eglBaseContext)
          surfaceTextureHelper = helper
          val source = pcf.createVideoSource(capturer.isScreencast)
          videoSource = source
          capturer.initialize(helper, context.applicationContext, source.capturerObserver)
          runCatching { capturer.startCapture(720, 1280, 30) }
          localVideoTrack = pcf.createVideoTrack("HELLO_VIDEO", source).apply {
            setEnabled(true)
          }
        }
      }
    }
  }

  private fun createPeerConnection() {
    val pcf = factory ?: return
    val iceServers = listOf(
      PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
      PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
      PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )
    val config = PeerConnection.RTCConfiguration(iceServers).apply {
      sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
    }
    val pc = pcf.createPeerConnection(config, object : PeerConnection.Observer {
      override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}

      override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        _iceState.value = newState?.name ?: "UNKNOWN"
      }

      override fun onIceConnectionReceivingChange(receiving: Boolean) {}

      override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}

      override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate != null && !disposed) {
          onSendSignal(
            "ice",
            "",
            candidate.sdpMid.orEmpty(),
            candidate.sdpMLineIndex,
            candidate.sdp
          )
        }
      }

      override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

      override fun onAddStream(stream: MediaStream?) {
        val track = stream?.videoTracks?.firstOrNull() ?: return
        attachRemoteTrackOnMain(track)
      }

      override fun onRemoveStream(stream: MediaStream?) {}

      override fun onDataChannel(dataChannel: DataChannel?) {}

      override fun onRenegotiationNeeded() {}

      override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        val track = receiver?.track() as? VideoTrack ?: return
        attachRemoteTrackOnMain(track)
      }
    })
    peerConnection = pc
    val streamIds = listOf("HELLO_STREAM")
    localAudioTrack?.let { pc?.addTrack(it, streamIds) }
    localVideoTrack?.let { pc?.addTrack(it, streamIds) }
  }

  private fun attachRemoteTrackOnMain(track: VideoTrack) {
    mainHandler.post {
      if (disposed) return@post
      remoteVideoTrack = track
      track.setEnabled(true)
      remoteRenderer?.let { renderer ->
        runCatching { track.addSink(renderer) }
      }
      _hasRemoteVideo.value = true
    }
  }

  private fun createAndSendOffer() {
    val pc = peerConnection ?: return
    val constraints = MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
    }
    pc.createOffer(object : SdpObserver {
      override fun onCreateSuccess(desc: SessionDescription?) {
        if (desc == null || disposed) return
        pc.setLocalDescription(simpleSdpObserver(), desc)
        onSendSignal("offer", desc.description, "", 0, "")
      }
      override fun onSetSuccess() {}
      override fun onCreateFailure(error: String?) { Log.w(TAG, "createOffer failed: $error") }
      override fun onSetFailure(error: String?) {}
    }, constraints)
  }

  private fun createAndSendAnswer() {
    val pc = peerConnection ?: return
    val constraints = MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
    }
    pc.createAnswer(object : SdpObserver {
      override fun onCreateSuccess(desc: SessionDescription?) {
        if (desc == null || disposed) return
        pc.setLocalDescription(simpleSdpObserver(), desc)
        onSendSignal("answer", desc.description, "", 0, "")
      }
      override fun onSetSuccess() {}
      override fun onCreateFailure(error: String?) { Log.w(TAG, "createAnswer failed: $error") }
      override fun onSetFailure(error: String?) {}
    }, constraints)
  }

  fun onSignalsReceived(signals: List<CallSignal>, currentUserId: String) {
    if (disposed || !started) return
    val pc = peerConnection ?: return
    for (sig in signals) {
      if (sig.id.isBlank() || !processedSignalIds.add(sig.id)) continue
      if (sig.senderId == currentUserId) continue
      if (sig.recipientId.isNotBlank() && sig.recipientId != currentUserId) continue

      when (sig.type) {
        "offer" -> {
          if (sig.sdp.isBlank() || isCaller) continue
          val sdp = SessionDescription(SessionDescription.Type.OFFER, sig.sdp)
          pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
              remoteDescriptionSet = true
              flushPendingIce()
              if (!answerSent) {
                answerSent = true
                createAndSendAnswer()
              }
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) { Log.w(TAG, "setRemote OFFER failed: $err") }
          }, sdp)
        }
        "answer" -> {
          if (sig.sdp.isBlank() || !isCaller) continue
          val sdp = SessionDescription(SessionDescription.Type.ANSWER, sig.sdp)
          pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
              remoteDescriptionSet = true
              flushPendingIce()
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) { Log.w(TAG, "setRemote ANSWER failed: $err") }
          }, sdp)
        }
        "ice" -> {
          if (sig.candidate.isBlank()) continue
          val ice = IceCandidate(sig.sdpMid, sig.sdpMLineIndex, sig.candidate)
          if (remoteDescriptionSet) {
            pc.addIceCandidate(ice)
          } else {
            pendingRemoteIce += ice
          }
        }
      }
    }
  }

  private fun flushPendingIce() {
    val pc = peerConnection ?: return
    pendingRemoteIce.forEach { pc.addIceCandidate(it) }
    pendingRemoteIce.clear()
  }

  fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
    if (disposed) return
    localRenderer = renderer
    runCatching {
      renderer.init(eglBase.eglBaseContext, null)
      renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
      renderer.setMirror(true)
      renderer.setEnableHardwareScaler(true)
      renderer.setZOrderMediaOverlay(true)
      localVideoTrack?.addSink(renderer)
    }
  }

  fun detachLocalRenderer(renderer: SurfaceViewRenderer) {
    runCatching { localVideoTrack?.removeSink(renderer) }
    runCatching { renderer.release() }
    if (localRenderer === renderer) localRenderer = null
  }

  fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
    if (disposed) return
    remoteRenderer = renderer
    runCatching {
      renderer.init(eglBase.eglBaseContext, null)
      renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
      renderer.setEnableHardwareScaler(true)
      remoteVideoTrack?.addSink(renderer)
    }
  }

  fun detachRemoteRenderer(renderer: SurfaceViewRenderer) {
    runCatching { remoteVideoTrack?.removeSink(renderer) }
    runCatching { renderer.release() }
    if (remoteRenderer === renderer) remoteRenderer = null
  }

  fun setMuted(muted: Boolean) {
    runCatching { localAudioTrack?.setEnabled(!muted) }
  }

  fun setCameraEnabled(enabled: Boolean) {
    runCatching { localVideoTrack?.setEnabled(enabled) }
  }

  fun switchCamera(useFront: Boolean) {
    runCatching {
      videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(isFrontCamera: Boolean) {
          mainHandler.post { localRenderer?.setMirror(isFrontCamera) }
        }
        override fun onCameraSwitchError(errorDescription: String?) {}
      })
    }
  }

  fun dispose() {
    if (disposed) return
    disposed = true
    runCatching { localRenderer?.let { localVideoTrack?.removeSink(it) } }
    runCatching { remoteRenderer?.let { remoteVideoTrack?.removeSink(it) } }
    runCatching { videoCapturer?.stopCapture() }
    runCatching { videoCapturer?.dispose() }
    runCatching { surfaceTextureHelper?.dispose() }
    runCatching { localAudioTrack?.dispose() }
    runCatching { audioSource?.dispose() }
    runCatching { localVideoTrack?.dispose() }
    runCatching { videoSource?.dispose() }
    runCatching { peerConnection?.close() }
    runCatching { peerConnection?.dispose() }
    runCatching { factory?.dispose() }
    runCatching { eglBase.release() }
  }

  private fun simpleSdpObserver() = object : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
  }
}
