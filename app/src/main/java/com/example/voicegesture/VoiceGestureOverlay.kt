package com.example.voicegesture

import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VoiceGestureOverlay(
  status: VoiceGestureStatus,
  onAttachPreview: (PreviewView) -> Unit,
  onTogglePause: () -> Unit,
  onTurnOff: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!status.isEnabled) return

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xEB0E1420),
    contentColor = Color.White,
    tonalElevation = 8.dp,
    shadowElevation = 10.dp,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    modifier = modifier
      .widthIn(max = 264.dp)
      .testTag("voice_gesture_hud")
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
          .background(Color(0xFF1A2233)),
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
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (status.isPaused) "Hands-Free Paused" else "Hands-Free Active",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Text(
          text = status.voiceState,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = status.gestureState,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF93C5FD),
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
}
