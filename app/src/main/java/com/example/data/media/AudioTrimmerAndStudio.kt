package com.example.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTrackInfo(
  val id: String,
  val title: String,
  val artist: String,
  val uriString: String,
  val durationMs: Long,
  val moodTag: String = "Device",
  val mediaAffinity: String = "all",
  val lyrics: List<String> = emptyList()
)

data class PresetVibeSpec(
  val id: String,
  val title: String,
  val artist: String,
  val moodTag: String,
  val mediaAffinity: String,
  val baseFreqHz: Double,
  val chordSteps: DoubleArray,
  val bpm: Double,
  val durationSec: Int = 36,
  val lyrics: List<String> = emptyList()
)

object AudioTrimmerAndStudio {

  private val builtInVibes = listOf(
    PresetVibeSpec(
      id = "vibe_dhaka_chill",
      title = "Dhaka Midnight Lo-Fi",
      artist = "HELLO Studio",
      moodTag = "Chill",
      mediaAffinity = "photo",
      baseFreqHz = 220.0,
      chordSteps = doubleArrayOf(1.0, 1.25, 1.5, 1.875),
      bpm = 84.0,
      lyrics = listOf(
        "City lights shimmer through the midnight rain",
        "Quiet streets humming an old refrain",
        "Rooftop breeze carries memories bright",
        "Hold this moment under neon night"
      )
    ),
    PresetVibeSpec(
      id = "vibe_golden_sunset",
      title = "Golden Hour Acoustic",
      artist = "HELLO Vibes",
      moodTag = "Romantic",
      mediaAffinity = "photo",
      baseFreqHz = 261.63,
      chordSteps = doubleArrayOf(1.0, 1.25, 1.5, 2.0),
      bpm = 96.0,
      lyrics = listOf(
        "Amber sky fades into soft warm gold",
        "Every silent glance tells a story untold",
        "Slow down time while the sun dips low",
        "Chasing horizons in the evening glow"
      )
    ),
    PresetVibeSpec(
      id = "vibe_neon_pop",
      title = "Neon Skyline Beat",
      artist = "HELLO Pulse",
      moodTag = "Upbeat",
      mediaAffinity = "video",
      baseFreqHz = 293.66,
      chordSteps = doubleArrayOf(1.0, 1.189, 1.498, 1.782),
      bpm = 114.0,
      lyrics = listOf(
        "Turn the tempo up, feel the city move",
        "Flashing skyline dancing to the groove",
        "Every frame alive with electric spark",
        "Lighting up the rhythm after dark"
      )
    ),
    PresetVibeSpec(
      id = "vibe_pure_love",
      title = "Pure Love Serenade",
      artist = "HELLO Acoustic",
      moodTag = "Love",
      mediaAffinity = "photo",
      baseFreqHz = 196.0,
      chordSteps = doubleArrayOf(1.0, 1.25, 1.5, 1.667),
      bpm = 78.0,
      lyrics = listOf(
        "In a crowded world, only you stand clear",
        "Every gentle heartbeat draws you near",
        "Softly spoken promises meant to stay",
        "Walking hand in hand till the break of day"
      )
    ),
    PresetVibeSpec(
      id = "vibe_morning_joy",
      title = "Sunshine Spark",
      artist = "HELLO Radio",
      moodTag = "Happy",
      mediaAffinity = "video",
      baseFreqHz = 329.63,
      chordSteps = doubleArrayOf(1.0, 1.25, 1.5, 1.875),
      bpm = 108.0,
      lyrics = listOf(
        "Open up the morning, let the daylight in",
        "Ready for a brand new chapter to begin",
        "Laughing in the sun with the windows down",
        "Good vibes spinning all across the town"
      )
    )
  )

  fun suggestedQuickTracks(tracks: List<AudioTrackInfo>, mediaType: String): List<AudioTrackInfo> {
    if (tracks.isEmpty()) return emptyList()
    return when (mediaType) {
      "video" -> tracks.sortedByDescending {
        when (it.mediaAffinity) {
          "video" -> 2
          "all" -> 1
          else -> 0
        }
      }
      "image" -> tracks.sortedByDescending {
        when (it.mediaAffinity) {
          "photo" -> 2
          "all" -> 1
          else -> 0
        }
      }
      else -> tracks
    }
  }

  suspend fun getBuiltInStudioTracks(context: Context): List<AudioTrackInfo> = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "hello_music_studio").apply { mkdirs() }
    builtInVibes.map { spec ->
      val outFile = File(dir, "${spec.id}.wav")
      if (!outFile.exists() || outFile.length() < 4096L) {
        generateSynthesizedWav(outFile, spec)
      }
      AudioTrackInfo(
        id = spec.id,
        title = spec.title,
        artist = spec.artist,
        uriString = Uri.fromFile(outFile).toString(),
        durationMs = spec.durationSec * 1000L,
        moodTag = spec.moodTag,
        mediaAffinity = spec.mediaAffinity,
        lyrics = spec.lyrics
      )
    }
  }

  suspend fun inspectAudioUri(context: Context, uri: Uri): AudioTrackInfo = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var fileNameTitle = ""
    runCatching {
      resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) {
            fileNameTitle = cursor.getString(idx) ?: ""
          }
        }
      }
    }
    val cleanFileName = fileNameTitle
      .substringBeforeLast('.')
      .replace('_', ' ')
      .trim()

    val mmr = MediaMetadataRetriever()
    var title = ""
    var artist = ""
    var durationMs = 30_000L
    try {
      mmr.setDataSource(context, uri)
      title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim().orEmpty()
      artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim().orEmpty()
      durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30_000L
    } catch (_: Exception) {
    } finally {
      runCatching { mmr.release() }
    }

    AudioTrackInfo(
      id = "device_${uri.toString().hashCode()}",
      title = title.ifBlank { cleanFileName.ifBlank { "My Song" } }.take(100),
      artist = artist.ifBlank { "Device Audio" }.take(70),
      uriString = uri.toString(),
      durationMs = durationMs.coerceAtLeast(3_000L),
      moodTag = "Phone Song"
    )
  }

  /**
   * Trims any selected audio URI (WAV, MP3, M4A, AAC, OGG) into a compact 22.05kHz mono WAV clip
   * containing only `[startMs, endMs]`, so uploading & friend playback matches the exact cut window.
   */
  suspend fun trimAudioSegmentToWav(
    context: Context,
    sourceUriString: String,
    startMs: Long,
    endMs: Long
  ): String = withContext(Dispatchers.IO) {
    val safeStartMs = startMs.coerceAtLeast(0L)
    val safeEndMs = endMs.coerceAtLeast(safeStartMs + 3_000L).coerceAtMost(safeStartMs + 35_000L)
    val uri = Uri.parse(sourceUriString)

    // Fast path for local studio WAV files or device WAV files
    val directWav = runCatching { trySliceWavUri(context, uri, safeStartMs, safeEndMs) }.getOrNull()
    if (directWav != null) return@withContext directWav

    // General MediaExtractor + MediaCodec decode-and-slice path for MP3 / M4A / AAC / OGG
    val decodedWav = runCatching { decodeAndWriteWavSlice(context, uri, safeStartMs, safeEndMs) }.getOrNull()
    if (decodedWav != null) return@withContext decodedWav

    // Fallback to original source URI (start/end bounds still stored & honored during playback)
    sourceUriString
  }

  private fun trySliceWavUri(context: Context, uri: Uri, startMs: Long, endMs: Long): String? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.size < 44) return null
    val headerTag = String(bytes, 0, 4, Charsets.US_ASCII)
    val waveTag = String(bytes, 8, 4, Charsets.US_ASCII)
    if (headerTag != "RIFF" || waveTag != "WAVE") return null

    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val channels = buf.getShort(22).toInt().coerceAtLeast(1)
    val sampleRate = buf.getInt(24).coerceAtLeast(8000)
    val bitsPerSample = buf.getShort(34).toInt().coerceAtLeast(8)
    val bytesPerFrame = (channels * (bitsPerSample / 8)).coerceAtLeast(1)

    // Locate 'data' chunk offset safely
    var pos = 12
    var dataStart = -1
    var dataSize = 0
    while (pos + 8 <= bytes.size) {
      val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
      val chunkLen = buf.getInt(pos + 4).coerceAtLeast(0)
      if (chunkId == "data") {
        dataStart = pos + 8
        dataSize = minOf(chunkLen, bytes.size - dataStart)
        break
      }
      pos += 8 + chunkLen
    }
    if (dataStart < 0 || dataSize <= 0) return null

    val startFrame = ((startMs / 1000.0) * sampleRate).toInt().coerceAtLeast(0)
    val endFrame = ((endMs / 1000.0) * sampleRate).toInt().coerceAtLeast(startFrame + sampleRate)
    val startByte = (startFrame * bytesPerFrame).coerceIn(0, dataSize)
    val endByte = (endFrame * bytesPerFrame).coerceIn(startByte, dataSize)
    val sliceLen = (endByte - startByte).coerceAtLeast(0)
    if (sliceLen <= 0) return null

    val outFile = File(context.cacheDir, "cut_song_${System.currentTimeMillis()}.wav")
    FileOutputStream(outFile).use { fos ->
      fos.write(buildWavHeader(sliceLen, sampleRate, channels, bitsPerSample))
      fos.write(bytes, dataStart + startByte, sliceLen)
    }
    return Uri.fromFile(outFile).toString()
  }

  private fun decodeAndWriteWavSlice(context: Context, uri: Uri, startMs: Long, endMs: Long): String? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
      extractor.setDataSource(context, uri, null)
      var trackIndex = -1
      var trackFormat: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
          trackIndex = i
          trackFormat = fmt
          break
        }
      }
      if (trackIndex < 0 || trackFormat == null) return null
      extractor.selectTrack(trackIndex)
      val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
      var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

      val startUs = startMs * 1000L
      val endUs = endMs * 1000L
      extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

      codec = MediaCodec.createDecoderByType(mime)
      codec.configure(trackFormat, null, null, 0)
      codec.start()

      val targetRate = 22050
      val pcmOut = ByteArrayOutputStream()
      val bufferInfo = MediaCodec.BufferInfo()
      var sawInputEOS = false
      var sawOutputEOS = false
      val maxOutBytes = ((endMs - startMs).coerceAtLeast(3000L) / 1000.0 * targetRate * 2).toInt() + 8192

      while (!sawOutputEOS && pcmOut.size() < maxOutBytes) {
        if (!sawInputEOS) {
          val inIdx = codec.dequeueInputBuffer(10_000L)
          if (inIdx >= 0) {
            val inBuf = codec.getInputBuffer(inIdx)!!
            val sampleSize = extractor.readSampleData(inBuf, 0)
            val pts = extractor.sampleTime
            if (sampleSize < 0 || (pts > endUs + 500_000L)) {
              codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              sawInputEOS = true
            } else {
              codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
              extractor.advance()
            }
          }
        }

        val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val outFmt = codec.outputFormat
          if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
        } else if (outIdx >= 0) {
          val outBuf = codec.getOutputBuffer(outIdx)
          if (outBuf != null && bufferInfo.size > 0 && bufferInfo.presentationTimeUs >= startUs - 50_000L) {
            outBuf.position(bufferInfo.offset)
            outBuf.limit(bufferInfo.offset + bufferInfo.size)
            val chunk = ByteArray(bufferInfo.size)
            outBuf.get(chunk)
            appendDownsampledMono16(chunk, channelCount, sampleRate, targetRate, pcmOut)
          }
          codec.releaseOutputBuffer(outIdx, false)
          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 ||
            bufferInfo.presentationTimeUs >= endUs
          ) {
            sawOutputEOS = true
          }
        }
      }

      val pcmBytes = pcmOut.toByteArray()
      if (pcmBytes.size < 2048) return null

      val outFile = File(context.cacheDir, "cut_song_${System.currentTimeMillis()}.wav")
      FileOutputStream(outFile).use { fos ->
        fos.write(buildWavHeader(pcmBytes.size, targetRate, 1, 16))
        fos.write(pcmBytes)
      }
      return Uri.fromFile(outFile).toString()
    } finally {
      runCatching { codec?.stop() }
      runCatching { codec?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun appendDownsampledMono16(
    pcmBytes: ByteArray,
    channels: Int,
    srcRate: Int,
    dstRate: Int,
    out: ByteArrayOutputStream
  ) {
    val safeChannels = channels.coerceAtLeast(1)
    val shortCount = pcmBytes.size / 2
    val frameCount = shortCount / safeChannels
    if (frameCount <= 0) return

    val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
    val step = (srcRate.toDouble() / dstRate.toDouble()).coerceAtLeast(1.0)
    var framePos = 0.0
    while (framePos < frameCount) {
      val fIdx = framePos.toInt()
      var sum = 0
      for (c in 0 until safeChannels) {
        sum += bb.getShort((fIdx * safeChannels + c) * 2).toInt()
      }
      val mono = (sum / safeChannels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
      out.write(mono and 0xFF)
      out.write((mono shr 8) and 0xFF)
      framePos += step
    }
  }

  private fun generateSynthesizedWav(file: File, spec: PresetVibeSpec) {
    val sampleRate = 22050
    val totalSamples = sampleRate * spec.durationSec
    val pcm = ByteArray(totalSamples * 2)
    val beatSec = 60.0 / spec.bpm
    val steps = spec.chordSteps

    for (i in 0 until totalSamples) {
      val t = i.toDouble() / sampleRate
      val beatNum = (t / beatSec).toInt()
      val beatFrac = (t % beatSec) / beatSec
      val noteIdx = beatNum % steps.size
      val chordShift = when ((beatNum / 4) % 4) {
        1 -> 1.12246
        2 -> 0.8909
        3 -> 1.3348
        else -> 1.0
      }
      val freq = spec.baseFreqHz * steps[noteIdx] * chordShift
      val subFreq = spec.baseFreqHz * 0.5 * chordShift
      val env = (1.0 - beatFrac * 0.72) * kotlin.math.min(1.0, beatFrac * 24.0)
      val melody = sin(2.0 * PI * freq * t) * 0.48 + sin(2.0 * PI * freq * 2.0 * t) * 0.14
      val pad = sin(2.0 * PI * subFreq * t) * 0.28
      val fade = when {
        t < 0.25 -> t / 0.25
        t > spec.durationSec - 0.5 -> ((spec.durationSec - t) / 0.5).coerceAtLeast(0.0)
        else -> 1.0
      }
      val sample = ((melody * env + pad) * fade * 15500.0).roundToInt().coerceIn(-32767, 32767)
      pcm[i * 2] = (sample and 0xFF).toByte()
      pcm[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
    }

    FileOutputStream(file).use { fos ->
      fos.write(buildWavHeader(pcm.size, sampleRate, 1, 16))
      fos.write(pcm)
    }
  }

  private fun buildWavHeader(dataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
    val byteRate = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign = channels * (bitsPerSample / 8)
    val totalDataLen = dataSize + 36
    return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
      put("RIFF".toByteArray(Charsets.US_ASCII))
      putInt(totalDataLen)
      put("WAVE".toByteArray(Charsets.US_ASCII))
      put("fmt ".toByteArray(Charsets.US_ASCII))
      putInt(16)
      putShort(1.toShort()) // PCM
      putShort(channels.toShort())
      putInt(sampleRate)
      putInt(byteRate)
      putShort(blockAlign.toShort())
      putShort(bitsPerSample.toShort())
      put("data".toByteArray(Charsets.US_ASCII))
      putInt(dataSize)
    }.array()
  }
}
