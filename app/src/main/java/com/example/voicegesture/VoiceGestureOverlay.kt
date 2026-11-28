package com.example.voicegesture

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun VoiceGestureOverlay(
  status: VoiceGestureStatus,
  onAttachPreview: (PreviewView) -> Unit,
  onTogglePause: () -> Unit,
  onTurnOff: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!status.isEnabled) return

  var showLiveToast by remember { mutableStateOf(false) }
  LaunchedEffect(status.statusToastSeq) {
    if (status.statusToastSeq > 0L && status.statusToast.isNotBlank()) {
      showLiveToast = true
      delay(2200L)
      showLiveToast = false
    }
  }

  val hudBorderColor = when {
    status.isPaused -> Color(0xFFF59E0B).copy(alpha = 0.6f)
    status.isGestureDetected -> Color(0xFF22C55E)
    status.isListening -> Color(0xFF38BDF8)
    else -> Color.White.copy(alpha = 0.16f)
  }

  Column(
    modifier = modifier.widthIn(max = 276.dp),
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xEB0E1420),
      contentColor = Color.White,
      tonalElevation = 8.dp,
      shadowElevation = 10.dp,
      border = BorderStroke(if (status.isListening || status.isGestureDetected) 1.6.dp else 1.dp, hudBorderColor),
      modifier = Modifier.testTag("voice_gesture_hud")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(width = 48.dp, height = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2233))
            .then(
              if (status.isGestureDetected) {
                Modifier.border(1.5.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp))
              } else Modifier
            ),
          contentAlignment = Alignment.Center
        ) {
          if (status.hasCameraPermission && !status.isPaused) {
            AndroidView(
              factory = { ctx ->
                PreviewView(ctx).apply {
                  scaleType = PreviewView.ScaleType.FILL_CENTER
                  implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                  onAttachPreview(this)
                }
              },
              update = { view ->
                onAttachPreview(view)
              },
              modifier = Modifier.matchParentSize()
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.PanTool,
              contentDescription = "Hand gesture camera preview paused",
              tint = Color.White.copy(alpha = 0.7f),
              modifier = Modifier.size(20.dp)
            )
          }
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(4.dp)
              .size(8.dp)
              .clip(CircleShape)
              .background(if (status.isPaused) Color(0xFFF59E0B) else Color(0xFF22C55E))
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Mic,
              contentDescription = null,
              tint = if (status.isListening) Color(0xFF38BDF8) else Color(0xFF60A5FA),
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = when {
                status.isPaused -> "Hands-Free Paused"
                status.isGestureDetected -> "Hand Detected"
                status.isListening -> "Hearing Speech..."
                else -> "Hands-Free Active"
              },
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (status.isGestureDetected) Color(0xFF86EFAC) else Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = status.voiceState,
            style = MaterialTheme.typography.labelSmall,
            color = if (status.isListening) Color(0xFF7DD3FC) else Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = status.gestureState,
            style = MaterialTheme.typography.labelSmall,
            color = if (status.isGestureDetected) Color(0xFF86EFAC) else Color(0xFF93C5FD),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (status.lastCommand.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF2563EB).copy(alpha = 0.35f)
            ) {
              Text(
                text = status.lastCommand,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(
            onClick = onTogglePause,
            modifier = Modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .size(48.dp)
              .testTag("vg_pause_button")
          ) {
            Icon(
              imageVector = if (status.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
              contentDescription = if (status.isPaused) "Resume voice and gesture control" else "Pause voice and gesture control",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(
            onClick = onTurnOff,
            modifier = Modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .size(48.dp)
              .testTag("vg_off_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Turn off voice and gesture control",
              tint = Color(0xFFFCA5A5),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    AnimatedVisibility(
      visible = showLiveToast && status.statusToast.isNotBlank(),
      enter = fadeIn() + slideInVertically { -it / 2 },
      exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
      val toastAccent = when (status.statusToastKind) {
        "gesture" -> Color(0xFF22C55E)
        "warn" -> Color(0xFFF43F5E)
        else -> Color(0xFF38BDF8)
      }
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xF00B1120),
        border = BorderStroke(1.dp, toastAccent.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
        modifier = Modifier.testTag("vg_live_toast")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(toastAccent)
          )
          Text(
            text = status.statusToast,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
