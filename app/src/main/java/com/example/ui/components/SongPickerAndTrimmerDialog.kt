package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.media.AudioTrackInfo
import com.example.data.media.AudioTrimmerAndStudio
import com.example.data.media.FirestoreMediaStore
import com.example.data.model.MusicAttachment
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SongPickerAndTrimmerDialog(
  initialSelection: MusicAttachment? = null,
  onDismiss: () -> Unit,
  onSongAttached: (MusicAttachment) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var studioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
  var selectedTrack by remember { mutableStateOf<AudioTrackInfo?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var isLoadingTracks by remember { mutableStateOf(true) }
  var isTrimming by remember { mutableStateOf(false) }

  // Cut range in seconds
  var cutRange by remember { mutableStateOf(0f..20f) }
  var isPreviewPlaying by remember { mutableStateOf(false) }
  var previewPositionMs by remember { mutableIntStateOf(0) }
  var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

  LaunchedEffect(Unit) {
    isLoadingTracks = true
    val loaded = AudioTrimmerAndStudio.getBuiltInStudioTracks(context)
    studioTracks = loaded
    if (initialSelection != null && initialSelection.title.isNotBlank()) {
      val matched = loaded.firstOrNull { it.title == initialSelection.title }
      if (matched != null) {
        selectedTrack = matched
        val maxSec = (matched.durationMs / 1000f).coerceAtLeast(5f)
        val s = initialSelection.startSec.toFloat().coerceIn(0f, (maxSec - 3f).coerceAtLeast(0f))
        val e = initialSelection.endSec.toFloat().coerceIn(s + 3f, maxSec)
        cutRange = s..e
      }
    }
    isLoadingTracks = false
  }

  fun stopPreview() {
    runCatching {
      previewPlayer?.stop()
      previewPlayer?.release()
    }
    previewPlayer = null
    isPreviewPlaying = false
  }

  DisposableEffect(Unit) {
    onDispose { stopPreview() }
  }

  fun startPreview(track: AudioTrackInfo, startSec: Float, endSec: Float) {
    stopPreview()
    val mp = MediaPlayer()
    previewPlayer = mp
    runCatching {
      mp.setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .build()
      )
      mp.setDataSource(context, Uri.parse(track.uriString))
      mp.setOnPreparedListener { player ->
        val startMs = (startSec * 1000f).toInt().coerceIn(0, (player.duration - 1000).coerceAtLeast(0))
        player.seekTo(startMs)
        player.start()
        previewPositionMs = startMs
        isPreviewPlaying = true
      }
      mp.setOnCompletionListener {
        val startMs = (cutRange.start * 1000f).toInt()
        runCatching {
          mp.seekTo(startMs)
          mp.start()
        }
      }
      mp.setOnErrorListener { _, _, _ ->
        stopPreview()
        true
      }
      mp.prepareAsync()
    }.onFailure {
      stopPreview()
    }
  }

  // Keep preview looping cleanly inside [cutRange.start, cutRange.endInclusive]
  LaunchedEffect(isPreviewPlaying, previewPlayer, cutRange) {
    while (isPreviewPlaying) {
      val mp = previewPlayer
      if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
        val pos = runCatching { mp.currentPosition }.getOrDefault(0)
        previewPositionMs = pos
        val endMs = (cutRange.endInclusive * 1000f).toInt()
        if (pos >= endMs) {
          val startMs = (cutRange.start * 1000f).toInt()
          runCatching { mp.seekTo(startMs) }
        }
      }
      delay(120L)
    }
  }

  val deviceAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      scope.launch {
        stopPreview()
        val info = AudioTrimmerAndStudio.inspectAudioUri(context, uri)
        selectedTrack = info
        val totalSec = (info.durationMs / 1000f).coerceAtLeast(5f)
        val endSec = minOf(totalSec, 20f).coerceAtLeast(minOf(totalSec, 3f))
        cutRange = 0f..endSec
        startPreview(info, 0f, endSec)
      }
    }
  }

  val filteredTracks = remember(studioTracks, searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) studioTracks
    else studioTracks.filter {
      it.title.contains(q, ignoreCase = true) ||
        it.artist.contains(q, ignoreCase = true) ||
        it.moodTag.contains(q, ignoreCase = true)
    }
  }

  Dialog(
    onDismissRequest = {
      stopPreview()
      onDismiss()
    },
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0A0D16), Color(0xFF121827), Color(0xFF0A0D16))
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
    ) {
      // Top Glass Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(onClick = {
          stopPreview()
          onDismiss()
        }) {
          Icon(Icons.Rounded.Close, contentDescription = "Close music studio", tint = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          }
          Text(
            text = "Music & Song Cutter",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(Modifier.width(48.dp))
      }

      // Phone Song Picker Card + Search
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color(0xFF1B2336),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              Brush.horizontalGradient(listOf(Color(0xFF38BDF8).copy(alpha = 0.6f), Color(0xFFEC4899).copy(alpha = 0.6f))),
              RoundedCornerShape(18.dp)
            )
            .clickable { deviceAudioPicker.launch(arrayOf("audio/*")) }
            .testTag("pick_device_song_button")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Pick Any Song From Phone",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "MP3, M4A, AAC, WAV — select & cut your favorite part",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
              )
            }
            Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = Color(0xFF38BDF8))
          }
        }

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search studio vibes or mood...", color = Color.White.copy(alpha = 0.55f)) },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
            focusedContainerColor = Color(0xFF141A29),
            unfocusedContainerColor = Color(0xFF141A29)
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(Modifier.height(10.dp))

      // Tracks list
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
      ) {
        item {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
            Text(
              text = "HELLO Vibe Library · Ready to Cut",
              color = Color.White.copy(alpha = 0.85f),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        if (isLoadingTracks) {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = Color(0xFFEC4899))
            }
          }
        } else {
          items(filteredTracks, key = { it.id }) { track ->
            val isSelected = selectedTrack?.id == track.id
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) Color(0xFF231B3D) else Color(0xFF141926),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                  selectedTrack = track
                  val totalSec = (track.durationMs / 1000f).coerceAtLeast(5f)
                  val endSec = minOf(totalSec, 20f).coerceAtLeast(minOf(totalSec, 3f))
                  cutRange = 0f..endSec
                  startPreview(track, 0f, endSec)
                }
                .testTag("studio_song_${track.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                      if (isSelected) Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
                      else SolidColor(Color(0xFF22293D))
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isSelected && isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${track.artist} · ${track.moodTag} · ${formatSongSec((track.durationMs / 1000L).toInt())}",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                if (isSelected) {
                  Surface(
                    shape = CircleShape,
                    color = Color(0xFFEC4899).copy(alpha = 0.2f)
                  ) {
                    Icon(
                      Icons.Rounded.Check,
                      contentDescription = "Selected track",
                      tint = Color(0xFFEC4899),
                      modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Interactive Song Cutter Panel (shows once any track is selected)
      selectedTrack?.let { track ->
        val maxSec = (track.durationMs / 1000f).coerceAtLeast(5f)
        val minClip = 3f
        val maxClip = minOf(30f, maxSec)
        val startSecInt = cutRange.start.roundToLong().coerceAtLeast(0L)
        val endSecInt = cutRange.endInclusive.roundToLong().coerceAtLeast(startSecInt + 3L)
        val clipDuration = (endSecInt - startSecInt).coerceAtLeast(1L)

        Surface(
          shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
          color = Color(0xFF161C2C),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.title,
                  color = Color.White,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Cut: ${formatSongSec(startSecInt.toInt())} – ${formatSongSec(endSecInt.toInt())} (${clipDuration}s clip)",
                  color = Color(0xFFF472B6),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold
                )
              }
              IconButton(
                onClick = {
                  if (isPreviewPlaying) {
                    runCatching { previewPlayer?.pause() }
                    isPreviewPlaying = false
                  } else if (previewPlayer != null) {
                    runCatching {
                      previewPlayer?.seekTo((cutRange.start * 1000f).toInt())
                      previewPlayer?.start()
                    }
                    isPreviewPlaying = true
                  } else {
                    startPreview(track, cutRange.start, cutRange.endInclusive)
                  }
                },
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))))
                  .testTag("song_cut_preview_button")
              ) {
                Icon(
                  imageVector = if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (isPreviewPlaying) "Pause preview" else "Play cut preview",
                  tint = Color.White
                )
              }
            }

            // Visual Waveform reflecting active cut selection
            val waveSeed = remember(track.id) { abs(track.title.hashCode()) }
            val startFrac = (cutRange.start / maxSec).coerceIn(0f, 1f)
            val endFrac = (cutRange.endInclusive / maxSec).coerceIn(startFrac, 1f)
            val playFrac = ((previewPositionMs / 1000f) / maxSec).coerceIn(0f, 1f)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0E1320))
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
              val barCount = 36
              for (i in 0 until barCount) {
                val frac = i.toFloat() / barCount.toFloat()
                val inCut = frac in startFrac..endFrac
                val passedPlayhead = isPreviewPlaying && frac in startFrac..playFrac
                val amp = 0.25f + (abs((waveSeed * (i + 5) * 19) % 75) / 100f)
                val barColor = when {
                  passedPlayhead -> Color(0xFF38BDF8)
                  inCut -> Color(0xFFEC4899)
                  else -> Color.White.copy(alpha = 0.22f)
                }
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height((28.dp * amp).coerceIn(5.dp, 28.dp))
                    .clip(CircleShape)
                    .background(barColor)
                )
              }
            }

            // Drag Start & End Handles to Cut Song
            RangeSlider(
              value = cutRange,
              onValueChange = { next ->
                var s = next.start.coerceIn(0f, maxSec - minClip)
                var e = next.endInclusive.coerceIn(s + minClip, maxSec)
                if (e - s > maxClip) {
                  if (abs(s - cutRange.start) > 0.05f) {
                    e = (s + maxClip).coerceAtMost(maxSec)
                  } else {
                    s = (e - maxClip).coerceAtLeast(0f)
                  }
                }
                cutRange = s..e
              },
              onValueChangeFinished = {
                if (isPreviewPlaying) {
                  runCatching { previewPlayer?.seekTo((cutRange.start * 1000f).toInt()) }
                }
              },
              valueRange = 0f..maxSec,
              colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFEC4899),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
              ),
              modifier = Modifier.testTag("song_cut_range_slider")
            )

            Button(
              onClick = {
                if (!isTrimming) {
                  stopPreview()
                  isTrimming = true
                  scope.launch {
                    val trimmedUri = AudioTrimmerAndStudio.trimAudioSegmentToWav(
                      context = context,
                      sourceUriString = track.uriString,
                      startMs = startSecInt * 1000L,
                      endMs = endSecInt * 1000L
                    )
                    isTrimming = false
                    onSongAttached(
                      MusicAttachment(
                        title = track.title,
                        artist = track.artist,
                        audioUri = trimmedUri,
                        startSec = startSecInt,
                        endSec = endSecInt
                      )
                    )
                  }
                }
              },
              enabled = !isTrimming,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("attach_cut_song_button")
            ) {
              if (isTrimming) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Cutting Song Clip...", fontWeight = FontWeight.Bold, color = Color.White)
              } else {
                Icon(Icons.Rounded.ContentCut, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Cut & Add Song (${clipDuration}s)", fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun PostMusicBanner(
  title: String,
  artist: String,
  audioUri: String,
  startSec: Long,
  endSec: Long,
  modifier: Modifier = Modifier
) {
  if (title.isBlank() && audioUri.isBlank()) return
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isPlaying by remember(audioUri) { mutableStateOf(false) }
  var isPreparing by remember(audioUri) { mutableStateOf(false) }
  var mediaPlayer by remember(audioUri) { mutableStateOf<MediaPlayer?>(null) }
  var clipProgress by remember(audioUri) { mutableFloatStateOf(0f) }

  DisposableEffect(audioUri) {
    onDispose {
      runCatching {
        mediaPlayer?.stop()
        mediaPlayer?.release()
      }
      mediaPlayer = null
      isPlaying = false
    }
  }

  val inf = rememberInfiniteTransition(label = "post_music_anim")
  val animPhase by inf.animateFloat(
    initialValue = 0f,
    targetValue = 6.283f,
    animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
    label = "eq"
  )
  val discRotation by inf.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
    label = "disc_spin"
  )

  // Keep playback progress synced and loop trimmed segment cleanly
  LaunchedEffect(isPlaying, mediaPlayer, startSec, endSec) {
    val mp = mediaPlayer ?: return@LaunchedEffect
    while (isPlaying) {
      val totalDur = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(1)
      val pos = runCatching { mp.currentPosition }.getOrDefault(0).coerceAtLeast(0)
      val hasSegment = endSec > startSec
      val expectedSegMs = ((endSec - startSec) * 1000L).toInt()
      if (hasSegment && totalDur > expectedSegMs + 2500) {
        val segStartMs = (startSec * 1000L).toInt().coerceIn(0, totalDur - 500)
        val segEndMs = (endSec * 1000L).toInt().coerceIn(segStartMs + 500, totalDur)
        if (pos >= segEndMs || pos < segStartMs - 300) {
          runCatching { mp.seekTo(segStartMs) }
        }
        clipProgress = ((pos - segStartMs).toFloat() / (segEndMs - segStartMs).toFloat()).coerceIn(0f, 1f)
      } else {
        clipProgress = (pos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
      }
      delay(120L)
    }
  }

  val rangeLabel = if (endSec > startSec) {
    "${formatSongSec(startSec.toInt())}–${formatSongSec(endSec.toInt())}"
  } else {
    "Studio Track"
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.Transparent,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.horizontalGradient(
          listOf(Color(0xFF151226), Color(0xFF21173B), Color(0xFF131B2E))
        )
      )
      .border(
        1.dp,
        Brush.horizontalGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.65f), Color(0xFF38BDF8).copy(alpha = 0.55f))),
        RoundedCornerShape(18.dp)
      )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
      ) {
        // Spinning Vinyl / Disc Medallion
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
              Brush.sweepGradient(
                listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFEC4899))
              )
            )
            .padding(2.5.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clip(CircleShape)
              .background(Color(0xFF0E111A)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = Color(0xFFF472B6),
              modifier = Modifier.size(20.dp)
            )
            // Subtle rotating shine dot
            val angle = if (isPlaying) discRotation else 0f
            val rad = Math.toRadians(angle.toDouble())
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(4.dp)
                  .clip(CircleShape)
                  .background(
                    if (isPlaying) Color(0xFF38BDF8).copy(alpha = (0.4f + 0.5f * abs(sin(rad))).toFloat())
                    else Color.Transparent
                  )
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = title.ifBlank { "Attached Song" },
              color = Color.White,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
              shape = CircleShape,
              color = Color(0xFFEC4899).copy(alpha = 0.22f)
            ) {
              Text(
                text = rangeLabel,
                color = Color(0xFFF9A8D4),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = artist.ifBlank { "HELLO Studio Audio" },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (isPlaying) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(18.dp)
          ) {
            for (i in 0..4) {
              val h = (5f + 12f * abs(sin(animPhase + i * 0.85f))).dp
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(h)
                  .clip(CircleShape)
                  .background(Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFFEC4899))))
              )
            }
          }
        }

        if (audioUri.isNotBlank()) {
          IconButton(
            onClick = {
              val cur = mediaPlayer
              if (cur != null) {
                if (isPlaying) {
                  runCatching { cur.pause() }
                  isPlaying = false
                } else {
                  runCatching { cur.start() }
                  isPlaying = true
                }
              } else if (!isPreparing) {
                isPreparing = true
                scope.launch {
                  val resolvedPath = if (FirestoreMediaStore.isFirestoreMediaUri(audioUri)) {
                    val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
                    runCatching {
                      FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, audioUri).absolutePath
                    }.getOrNull()
                  } else {
                    audioUri
                  }
                  if (resolvedPath.isNullOrBlank()) {
                    isPreparing = false
                    return@launch
                  }
                  val mp = MediaPlayer().apply {
                    setAudioAttributes(
                      AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                    )
                    setOnPreparedListener { p ->
                      isPreparing = false
                      p.isLooping = true
                      val expectedSegMs = ((endSec - startSec) * 1000L).toInt()
                      if (endSec > startSec && p.duration > expectedSegMs + 2500) {
                        val startMs = (startSec * 1000L).toInt().coerceIn(0, (p.duration - 500).coerceAtLeast(0))
                        p.seekTo(startMs)
                      }
                      p.start()
                      isPlaying = true
                    }
                    setOnErrorListener { _, _, _ ->
                      isPreparing = false
                      isPlaying = false
                      true
                    }
                  }
                  mediaPlayer = mp
                  runCatching {
                    if (resolvedPath.startsWith("/")) mp.setDataSource(resolvedPath)
                    else mp.setDataSource(context, Uri.parse(resolvedPath))
                    mp.prepareAsync()
                  }.onFailure {
                    isPreparing = false
                    runCatching { mp.release() }
                    mediaPlayer = null
                  }
                }
              }
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))))
              .testTag("post_music_play_button")
          ) {
            if (isPreparing) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
              Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause song" else "Play song",
                tint = Color.White
              )
            }
          }
        }
      }

      // Live playback scrubber underline
      if (isPlaying || clipProgress > 0f) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(clipProgress.coerceIn(0.02f, 1f))
              .height(2.5.dp)
              .background(Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF38BDF8))))
          )
        }
      }
    }
  }
}

@Composable
fun StoryMusicPlayerPill(
  storyId: String,
  title: String,
  artist: String,
  audioUri: String,
  startSec: Long,
  endSec: Long,
  shouldPause: Boolean,
  modifier: Modifier = Modifier
) {
  if (title.isBlank() && audioUri.isBlank()) return
  val context = LocalContext.current
  var isMuted by remember { mutableStateOf(false) }
  var isReady by remember(storyId, audioUri) { mutableStateOf(false) }
  var mediaPlayer by remember(storyId, audioUri) { mutableStateOf<MediaPlayer?>(null) }
  var clipProgress by remember(storyId, audioUri) { mutableFloatStateOf(0f) }

  DisposableEffect(storyId, audioUri) {
    onDispose {
      runCatching {
        mediaPlayer?.stop()
        mediaPlayer?.release()
      }
      mediaPlayer = null
      isReady = false
    }
  }

  val inf = rememberInfiniteTransition(label = "story_music_eq")
  val eqPhase by inf.animateFloat(
    initialValue = 0f,
    targetValue = 6.283f,
    animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart),
    label = "story_eq_phase"
  )

  LaunchedEffect(storyId, audioUri) {
    if (audioUri.isBlank()) return@LaunchedEffect
    val resolvedPath = if (FirestoreMediaStore.isFirestoreMediaUri(audioUri)) {
      val db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
      runCatching {
        FirestoreMediaStore.resolveToLocalFile(context.applicationContext, db, audioUri).absolutePath
      }.getOrNull()
    } else {
      audioUri
    }
    if (resolvedPath.isNullOrBlank()) return@LaunchedEffect
    val mp = MediaPlayer().apply {
      setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .build()
      )
      setOnPreparedListener { p ->
        isReady = true
        p.isLooping = true
        val expectedSegMs = ((endSec - startSec) * 1000L).toInt()
        if (endSec > startSec && p.duration > expectedSegMs + 2500) {
          val startMs = (startSec * 1000L).toInt().coerceIn(0, (p.duration - 500).coerceAtLeast(0))
          p.seekTo(startMs)
        }
        val vol = if (isMuted) 0f else 1f
        p.setVolume(vol, vol)
        if (!shouldPause) p.start()
      }
      setOnErrorListener { _, _, _ -> true }
    }
    mediaPlayer = mp
    runCatching {
      if (resolvedPath.startsWith("/")) mp.setDataSource(resolvedPath)
      else mp.setDataSource(context, Uri.parse(resolvedPath))
      mp.prepareAsync()
    }.onFailure {
      runCatching { mp.release() }
      mediaPlayer = null
    }
  }

  LaunchedEffect(shouldPause, isMuted, isReady, mediaPlayer) {
    val mp = mediaPlayer ?: return@LaunchedEffect
    if (!isReady) return@LaunchedEffect
    val vol = if (isMuted) 0f else 1f
    runCatching {
      mp.setVolume(vol, vol)
      if (shouldPause && mp.isPlaying) {
        mp.pause()
      } else if (!shouldPause && !mp.isPlaying) {
        mp.start()
      }
    }
  }

  LaunchedEffect(isReady, shouldPause, mediaPlayer, startSec, endSec) {
    val mp = mediaPlayer ?: return@LaunchedEffect
    if (!isReady) return@LaunchedEffect
    while (!shouldPause) {
      val totalDur = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(1)
      val pos = runCatching { mp.currentPosition }.getOrDefault(0).coerceAtLeast(0)
      val expectedSegMs = ((endSec - startSec) * 1000L).toInt()
      if (endSec > startSec && totalDur > expectedSegMs + 2500) {
        val segStartMs = (startSec * 1000L).toInt().coerceIn(0, totalDur - 500)
        val segEndMs = (endSec * 1000L).toInt().coerceIn(segStartMs + 500, totalDur)
        if (pos >= segEndMs || pos < segStartMs - 300) {
          runCatching { mp.seekTo(segStartMs) }
        }
        clipProgress = ((pos - segStartMs).toFloat() / (segEndMs - segStartMs).toFloat()).coerceIn(0f, 1f)
      } else {
        clipProgress = (pos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
      }
      delay(140L)
    }
  }

  val rangeStr = if (endSec > startSec) " · ${formatSongSec(startSec.toInt())}–${formatSongSec(endSec.toInt())}" else ""

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.Transparent,
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.horizontalGradient(listOf(Color(0xD9111428), Color(0xD925143B)))
      )
      .border(
        1.dp,
        Brush.horizontalGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.65f), Color(0xFF38BDF8).copy(alpha = 0.55f))),
        RoundedCornerShape(18.dp)
      )
  ) {
    Column {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
          Text(
            text = title.ifBlank { "Story Song" },
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${artist.ifBlank { "HELLO Music" }}$rangeStr",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (!shouldPause && !isMuted && audioUri.isNotBlank()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(14.dp)
          ) {
            for (i in 0..3) {
              val h = (4f + 9f * abs(sin(eqPhase + i * 0.9f))).dp
              Box(
                modifier = Modifier
                  .width(2.5.dp)
                  .height(h)
                  .clip(CircleShape)
                  .background(Color(0xFFF472B6))
              )
            }
          }
        }
        if (audioUri.isNotBlank()) {
          IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = if (isMuted) "Unmute story music" else "Mute story music",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
      if (isReady && !shouldPause) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.12f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(clipProgress.coerceIn(0.03f, 1f))
              .height(2.dp)
              .background(Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF38BDF8))))
          )
        }
      }
    }
  }
}

fun formatSongSec(totalSec: Int): String {
  val safe = totalSec.coerceAtLeast(0)
  val m = safe / 60
  val s = safe % 60
  return "%d:%02d".format(m, s)
}
