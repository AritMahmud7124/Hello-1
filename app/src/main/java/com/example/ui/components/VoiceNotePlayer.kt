package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.FirestoreMediaStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceNotePlayer(
  audioUrl: String,
  fileName: String,
  isMine: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isPlaying by remember(audioUrl) { mutableStateOf(false) }
  var isPreparing by remember(audioUrl) { mutableStateOf(false) }
  var durationMs by remember(audioUrl) { mutableIntStateOf(0) }
  var positionMs by remember(audioUrl) { mutableIntStateOf(0) }
  var mediaPlayer by remember(audioUrl) { mutableStateOf<MediaPlayer?>(null) }

  DisposableEffect(audioUrl) {
    onDispose {
      runCatching {
        mediaPlayer?.stop()
        mediaPlayer?.release()
      }
      mediaPlayer = null
      isPlaying = false
      isPreparing = false
    }
  }

  LaunchedEffect(isPlaying, mediaPlayer) {
    while (isPlaying) {
      val mp = mediaPlayer
      if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
        positionMs = runCatching { mp.currentPosition }.getOrDefault(0)
      }
      delay(160L)
    }
  }

  val fgColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val pillBg = if (isMine) {
    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
  }

  val baseHeights = remember(audioUrl, fileName) {
    val seed = (audioUrl + fileName).hashCode()
    List(18) { idx ->
      val raw = abs((seed * (idx + 7) * 31) % 100)
      (0.28f + (raw / 100f) * 0.72f)
    }
  }
  val progressFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
  val wavePhase = if (isPlaying) {
    val inf = rememberInfiniteTransition(label = "voice_wave_play")
    inf.animateFloat(
      initialValue = 0f,
      targetValue = 6.283f,
      animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
      label = "wave_phase"
    ).value
  } else {
    0f
  }

  Surface(
    color = pillBg,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      IconButton(
        onClick = {
          val current = mediaPlayer
          if (current != null) {
            if (isPlaying) {
              runCatching { current.pause() }
              isPlaying = false
            } else {
              runCatching { current.start() }
              isPlaying = true
            }
          } else if (!isPreparing) {
            isPreparing = true
            scope.launch {
              val sourcePath = if (FirestoreMediaStore.isFirestoreMediaUri(audioUrl)) {
                val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
                runCatching {
                  FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, audioUrl).absolutePath
                }.getOrNull()
              } else {
                audioUrl
              }
              if (sourcePath.isNullOrBlank()) {
                isPreparing = false
                return@launch
              }
              val mp = MediaPlayer().apply {
                setAudioAttributes(
                  AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                )
                setOnPreparedListener { player ->
                  isPreparing = false
                  durationMs = player.duration.coerceAtLeast(0)
                  player.start()
                  isPlaying = true
                }
                setOnCompletionListener {
                  isPlaying = false
                  positionMs = 0
                }
                setOnErrorListener { _, _, _ ->
                  isPreparing = false
                  isPlaying = false
                  true
                }
              }
              mediaPlayer = mp
              runCatching {
                mp.setDataSource(sourcePath)
                mp.prepareAsync()
              }.onFailure {
                isPreparing = false
                runCatching { mp.release() }
                mediaPlayer = null
              }
            }
          }
        },
        modifier = Modifier.size(44.dp).testTag("voice_note_play_button")
      ) {
        if (isPreparing) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = fgColor, strokeWidth = 2.dp)
        } else {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
            tint = fgColor
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          modifier = Modifier.height(22.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
          baseHeights.forEachIndexed { idx, h ->
            val barPos = idx.toFloat() / baseHeights.size.toFloat()
            val played = isPlaying && barPos <= progressFraction
            val pulse = if (isPlaying) (0.78f + 0.25f * sin(wavePhase + idx * 0.6f)) else 1f
            val finalHeight = (20.dp * h * pulse).coerceIn(4.dp, 22.dp)
            Box(
              modifier = Modifier
                .width(3.dp)
                .height(finalHeight)
                .clip(CircleShape)
                .background(
                  if (played) fgColor else fgColor.copy(alpha = 0.42f)
                )
            )
          }
        }
        val labelTime = when {
          durationMs > 0 -> formatSeconds(((if (isPlaying) positionMs else durationMs) / 1000))
          fileName.startsWith("voice_") -> fileName.removePrefix("voice_").removeSuffix(".m4a")
          else -> "Voice note"
        }
        Text(
          text = labelTime,
          color = fgColor.copy(alpha = 0.85f),
          style = MaterialTheme.typography.labelSmall
        )
      }
    }
  }
}

private fun formatSeconds(totalSeconds: Int): String {
  val mins = totalSeconds / 60
  val secs = totalSeconds % 60
  return "%d:%02d".format(mins, secs)
}
