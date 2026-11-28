package com.example.voicegesture

sealed interface VoiceGestureAction {
  data class Navigate(val route: String, val label: String) : VoiceGestureAction
  data class Scroll(val direction: Int, val toTop: Boolean = false) : VoiceGestureAction
  data object Back : VoiceGestureAction
  data object MediaNext : VoiceGestureAction
  data object MediaPrevious : VoiceGestureAction
  data object MediaPlayPause : VoiceGestureAction
  data object GesturePalm : VoiceGestureAction
  data object Like : VoiceGestureAction
  data object Select : VoiceGestureAction
  data object Disable : VoiceGestureAction
}

data class VoiceGestureStatus(
  val isEnabled: Boolean = false,
  val isPaused: Boolean = false,
  val isListening: Boolean = false,
  val isGestureDetected: Boolean = false,
  val voiceState: String = "Off",
  val gestureState: String = "Off",
  val lastCommand: String = "",
  val statusToast: String = "",
  val statusToastKind: String = "info",
  val statusToastSeq: Long = 0L,
  val hasMicPermission: Boolean = false,
  val hasCameraPermission: Boolean = false
)
