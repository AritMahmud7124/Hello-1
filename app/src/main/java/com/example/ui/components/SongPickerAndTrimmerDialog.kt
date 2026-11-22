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
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
      ) {
        // Clean Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(onClick = {
            stopPreview()
            onDismiss()
          }) {
            Icon(
              Icons.Rounded.Close,
              contentDescription = "Close music picker",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Music",
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(Modifier.width(48.dp))
        }

        // Device Audio Picker + Search
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                0.8.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
              )
              .clip(RoundedCornerShape(14.dp))
              .clickable { deviceAudioPicker.launch(arrayOf("audio/*")) }
              .testTag("pick_device_song_button")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Rounded.AudioFile,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Choose from device",
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "MP3, M4A, AAC, WAV · Trim your clip",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.labelSmall
                )
              }
              Icon(
                Icons.Rounded.ContentCut,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
              Text(
                "Search songs or mood",
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            leadingIcon = {
              Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Audio Library",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (isLoadingTracks) {
            item {
              Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
              }
            }
          } else {
            items(filteredTracks, key = { it.id }) { track ->
              val isSelected = selectedTrack?.id == track.id
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    width = if (isSelected) 1.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clip(RoundedCornerShape(12.dp))
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (isSelected && isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.MusicNote,
                      contentDescription = null,
                      tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = track.title,
                      color = MaterialTheme.colorScheme.onSurface,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${track.artist} · ${track.moodTag} · ${formatSongSec((track.durationMs / 1000L).toInt())}",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  if (isSelected) {
                    Icon(
                      Icons.Rounded.Check,
                      contentDescription = "Selected track",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // Clean Interactive Song Cutter Panel
        selectedTrack?.let { track ->
          val maxSec = (track.durationMs / 1000f).coerceAtLeast(5f)
          val minClip = 3f
          val maxClip = minOf(30f, maxSec)
          val startSecInt = cutRange.start.roundToLong().coerceAtLeast(0L)
          val endSecInt = cutRange.endInclusive.roundToLong().coerceAtLeast(startSecInt + 3L)
          val clipDuration = (endSecInt - startSecInt).coerceAtLeast(1L)

          Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
              .fillMaxWidth()
              .border(
                0.8.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
              )
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${formatSongSec(startSecInt.toInt())} – ${formatSongSec(endSecInt.toInt())} (${clipDuration}s clip)",
                    color = MaterialTheme.colorScheme.primary,
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("song_cut_preview_button")
                ) {
                  Icon(
                    imageVector = if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPreviewPlaying) "Pause preview" else "Play cut preview",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              // Clean Minimal Waveform reflecting active cut selection
              val waveSeed = remember(track.id) { abs(track.title.hashCode()) }
              val startFrac = (cutRange.start / maxSec).coerceIn(0f, 1f)
              val endFrac = (cutRange.endInclusive / maxSec).coerceIn(startFrac, 1f)
              val playFrac = ((previewPositionMs / 1000f) / maxSec).coerceIn(0f, 1f)

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(30.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
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
                    passedPlayhead -> MaterialTheme.colorScheme.primary
                    inCut -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                  }
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height((24.dp * amp).coerceIn(4.dp, 24.dp))
                      .clip(CircleShape)
                      .background(barColor)
                  )
                }
              }

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
                  thumbColor = MaterialTheme.colorScheme.primary,
                  activeTrackColor = MaterialTheme.colorScheme.primary,
                  inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("attach_cut_song_button")
              ) {
                if (isTrimming) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                  Spacer(Modifier.width(8.dp))
                  Text("Trimming clip...", fontWeight = FontWeight.SemiBold)
                } else {
                  Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Use Song (${clipDuration}s)", fontWeight = FontWeight.SemiBold)
                }
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
    ""
  }

  // Clean authentic social post music bar
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(
        0.7.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        RoundedCornerShape(12.dp)
      )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = title.ifBlank { "Attached Song" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          val artistDisplay = artist.ifBlank { "Music" }
          Text(
            text = if (rangeLabel.isNotBlank()) "$artistDisplay · $rangeLabel" else artistDisplay,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (isPlaying) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(14.dp)
          ) {
            for (i in 0..2) {
              val h = (4f + 9f * abs(sin(animPhase + i * 0.9f))).dp
              Box(
                modifier = Modifier
                  .width(2.5.dp)
                  .height(h)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
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
              .size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
              .testTag("post_music_play_button")
          ) {
            if (isPreparing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause song" else "Play song",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
              )
            }
          }
        }
      }

      if (isPlaying || clipProgress > 0f) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(clipProgress.coerceIn(0.02f, 1f))
              .height(2.dp)
              .background(MaterialTheme.colorScheme.primary)
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

  // Clean frosted Story Music Sticker
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color.Black.copy(alpha = 0.52f),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .border(
        0.6.dp,
        Color.White.copy(alpha = 0.18f),
        RoundedCornerShape(12.dp)
      )
  ) {
    Column {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
          Text(
            text = title.ifBlank { "Story Music" },
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${artist.ifBlank { "Audio" }}$rangeStr",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (!shouldPause && !isMuted && audioUri.isNotBlank()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(12.dp)
          ) {
            for (i in 0..2) {
              val h = (3.5f + 7.5f * abs(sin(eqPhase + i * 0.9f))).dp
              Box(
                modifier = Modifier
                  .width(2.dp)
                  .height(h)
                  .clip(CircleShape)
                  .background(Color.White)
              )
            }
          }
        }
        if (audioUri.isNotBlank()) {
          IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = if (isMuted) "Unmute story music" else "Mute story music",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
      if (isReady && !shouldPause) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Color.White.copy(alpha = 0.14f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(clipProgress.coerceIn(0.03f, 1f))
              .height(1.5.dp)
              .background(Color.White)
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
