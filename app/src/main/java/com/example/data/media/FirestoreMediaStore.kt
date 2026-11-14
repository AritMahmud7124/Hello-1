package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FirestoreMediaStore {
  const val SCHEME = "firestore"
  const val HOST = "media"
  const val PREFIX = "firestore://media/"
  private const val CACHE_DIR_NAME = "hello_media"
  private const val MAX_IMAGE_DIMENSION = 1280
  private const val TARGET_IMAGE_BYTES = 450_000
  private const val MAX_SPARK_RAW_BYTES = 8L * 1024L * 1024L // 8 MB safe cap (~11 MB Base64 <= 25 chunks)
  private const val CHUNK_CHAR_SIZE = 450_000
  private const val MAX_CHUNKS_PER_BATCH = 6

  private val fetchMutexes = ConcurrentHashMap<String, Mutex>()

  fun isFirestoreMediaUri(uri: String?): Boolean =
    !uri.isNullOrBlank() && uri.trim().startsWith(PREFIX)

  fun extractMediaId(uri: String): String =
    uri.trim().removePrefix(PREFIX).substringBefore('?').substringBefore('/').trim()

  suspend fun uploadUri(
    context: Context,
    db: FirebaseFirestore,
    uid: String,
    uri: Uri,
    resolvedMime: String,
    ext: String
  ): String = withContext(Dispatchers.IO) {
    val safeMime = resolvedMime.trim().ifBlank { "application/octet-stream" }.take(100)
    val safeExt = sanitizeExt(ext)
    val payloadBytes: ByteArray = if (safeMime.startsWith("image/")) {
      compressImageOrReadRaw(context, uri)
    } else {
      readBoundedBytes(context, uri)
    }
    if (payloadBytes.isEmpty()) {
      throw IllegalArgumentException("Selected file is empty.")
    }
    uploadBytes(context, db, uid, payloadBytes, safeMime, safeExt)
  }

  suspend fun uploadBytes(
    context: Context,
    db: FirebaseFirestore,
    uid: String,
    bytes: ByteArray,
    mimeType: String,
    ext: String
  ): String = withContext(Dispatchers.IO) {
    if (bytes.isEmpty()) {
      throw IllegalArgumentException("Cannot upload empty media.")
    }
    if (bytes.size.toLong() > MAX_SPARK_RAW_BYTES) {
      throw IllegalArgumentException(
        "File is too large (${bytes.size / (1024 * 1024)} MB). Free cloud mode supports files/videos up to 8 MB."
      )
    }

    val safeMime = mimeType.trim().ifBlank { "application/octet-stream" }.take(100)
    val safeExt = sanitizeExt(ext)
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    val chunks = base64.chunked(CHUNK_CHAR_SIZE)
    require(chunks.size in 1..30) {
      "Encoded file exceeds chunk limit (${chunks.size}/30). Try a shorter video or smaller file."
    }

    val mediaId = UUID.randomUUID().toString()
    val blobRef = db.collection("media_blobs").document(mediaId)
    blobRef.set(
      mapOf(
        "ownerId" to uid,
        "mimeType" to safeMime,
        "ext" to safeExt,
        "chunkCount" to chunks.size,
        "byteSize" to bytes.size,
        "createdAt" to FieldValue.serverTimestamp()
      )
    ).await()

    var startIndex = 0
    while (startIndex < chunks.size) {
      val endIndex = minOf(startIndex + MAX_CHUNKS_PER_BATCH, chunks.size)
      val batch = db.batch()
      for (idx in startIndex until endIndex) {
        val chunkId = "chunk_%03d".format(idx)
        batch.set(
          blobRef.collection("chunks").document(chunkId),
          mapOf(
            "ownerId" to uid,
            "index" to idx,
            "data" to chunks[idx],
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      startIndex = endIndex
    }

    // Cache locally immediately so author sees instant playback/rendering
    runCatching { writeCacheFile(context, mediaId, safeExt, bytes) }
    "$PREFIX$mediaId"
  }

  suspend fun resolveToLocalFile(
    context: Context,
    db: FirebaseFirestore,
    uriString: String
  ): File {
    val mediaId = extractMediaId(uriString)
    require(mediaId.isNotBlank()) { "Invalid cloud media URI: $uriString" }

    findCachedFile(context, mediaId)?.let { return it }

    val mutex = fetchMutexes.getOrPut(mediaId) { Mutex() }
    return mutex.withLock {
      findCachedFile(context, mediaId)?.let { return@withLock it }
      withContext(Dispatchers.IO) {
        try {
          val blobDoc = db.collection("media_blobs").document(mediaId).get().await()
          if (!blobDoc.exists()) {
            throw IllegalStateException("Cloud media not found ($mediaId).")
          }
          val ext = sanitizeExt(blobDoc.getString("ext") ?: "bin")
          val chunksSnap = db.collection("media_blobs").document(mediaId)
            .collection("chunks")
            .orderBy("index", Query.Direction.ASCENDING)
            .get()
            .await()
          if (chunksSnap.isEmpty) {
            throw IllegalStateException("Cloud media data is missing chunks ($mediaId).")
          }
          val encoded = buildString {
            chunksSnap.documents.forEach { doc ->
              append(doc.getString("data").orEmpty())
            }
          }
          val decoded = Base64.decode(encoded, Base64.DEFAULT)
          if (decoded.isEmpty()) {
            throw IllegalStateException("Cloud media decoded to 0 bytes ($mediaId).")
          }
          writeCacheFile(context, mediaId, ext, decoded)
        } catch (e: CancellationException) {
          throw e
        }
      }
    }
  }

  private fun compressImageOrReadRaw(context: Context, uri: Uri): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
      return readBoundedBytes(context, uri)
    }

    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_DIMENSION)
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: return readBoundedBytes(context, uri)

    val scaled = scaleDownIfNeeded(decoded, MAX_IMAGE_DIMENSION)
    if (scaled !== decoded) {
      decoded.recycle()
    }

    return try {
      val out = ByteArrayOutputStream()
      var quality = 85
      scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
      while (out.size() > TARGET_IMAGE_BYTES && quality > 45) {
        quality -= 12
        out.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
      }
      out.toByteArray()
    } finally {
      scaled.recycle()
    }
  }

  private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var inSampleSize = 1
    var w = width
    var h = height
    while (w / 2 >= maxDim || h / 2 >= maxDim) {
      inSampleSize *= 2
      w /= 2
      h /= 2
    }
    return inSampleSize
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val maxSide = maxOf(bitmap.width, bitmap.height)
    if (maxSide <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / maxSide.toFloat()
    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun readBoundedBytes(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
      ?: throw IllegalStateException("Unable to read selected file.")
    return input.use { stream ->
      val out = ByteArrayOutputStream()
      val buffer = ByteArray(16 * 1024)
      var total = 0L
      while (true) {
        val read = stream.read(buffer)
        if (read == -1) break
        total += read
        if (total > MAX_SPARK_RAW_BYTES) {
          throw IllegalArgumentException(
            "Selected file exceeds the 8 MB limit for free cloud uploads. Please pick a shorter clip or smaller file."
          )
        }
        out.write(buffer, 0, read)
      }
      out.toByteArray()
    }
  }

  private fun cacheDir(context: Context): File =
    File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }

  private fun findCachedFile(context: Context, mediaId: String): File? {
    val dir = cacheDir(context)
    return dir.listFiles()?.firstOrNull { file ->
      file.isFile && file.name.startsWith("$mediaId.") && !file.name.endsWith(".tmp") && file.length() > 0L
    }
  }

  private fun writeCacheFile(context: Context, mediaId: String, ext: String, bytes: ByteArray): File {
    val dir = cacheDir(context)
    val target = File(dir, "$mediaId.$ext")
    val tmp = File(dir, "$mediaId.$ext.tmp")
    FileOutputStream(tmp).use { it.write(bytes) }
    if (!tmp.renameTo(target)) {
      FileOutputStream(target).use { it.write(bytes) }
      tmp.delete()
    }
    return target
  }

  private fun sanitizeExt(ext: String): String =
    ext.trim().lowercase().filter(Char::isLetterOrDigit).take(10).ifBlank { "bin" }
}
