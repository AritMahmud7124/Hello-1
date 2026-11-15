package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CropAspect(val label: String, val ratio: Float?) {
  FREE("Free", null),
  SQUARE("1:1", 1f),
  PORTRAIT("4:5", 4f / 5f),
  STORY("9:16", 9f / 16f),
  COVER("16:9", 16f / 9f)
}

@Composable
fun ImageCropDialog(
  imageUri: String,
  defaultAspect: CropAspect = CropAspect.FREE,
  onDismiss: () -> Unit,
  onUseOriginal: (() -> Unit)? = null,
  onCropped: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
  var isLoading by remember(imageUri) { mutableStateOf(true) }
  var isSaving by remember { mutableStateOf(false) }
  var loadError by remember { mutableStateOf<String?>(null) }

  var selectedAspect by remember { mutableStateOf(defaultAspect) }
  var rotationDeg by remember { mutableIntStateOf(0) }
  var userScale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(imageUri) {
    isLoading = true
    loadError = null
    val bmp = withContext(Dispatchers.IO) {
      loadOrientedSampledBitmap(context, Uri.parse(imageUri), maxDim = 1600)
    }
    if (bmp == null) {
      loadError = "Could not load selected image."
    } else {
      sourceBitmap = bmp
    }
    isLoading = false
  }

  val rotatedBitmap = remember(sourceBitmap, rotationDeg) {
    val bmp = sourceBitmap ?: return@remember null
    if (rotationDeg % 360 == 0) {
      bmp
    } else {
      val matrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
      Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0E1015))
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Top bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Cancel crop", tint = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Filled.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Crop Photo", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onUseOriginal != null) {
            TextButton(onClick = onUseOriginal) {
              Text("Original", color = Color.White.copy(alpha = 0.85f))
            }
          }
          IconButton(
            onClick = {
              rotationDeg = (rotationDeg + 90) % 360
              userScale = 1f
              offsetX = 0f
              offsetY = 0f
            }
          ) {
            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White)
          }
        }
      }

      // Main crop stage
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val bmp = rotatedBitmap

        if (isLoading) {
          CircularProgressIndicator(color = Color.White)
        } else if (loadError != null || bmp == null) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(loadError ?: "Image error", color = Color.White)
            OutlinedButton(onClick = { onCropped(imageUri) }) {
              Text("Use Image As-Is", color = Color.White)
            }
          }
        } else {
          val imgRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
          val frameRatio = selectedAspect.ratio ?: imgRatio
          val frameW: Float
          val frameH: Float
          if (maxW / maxH > frameRatio) {
            frameH = maxH
            frameW = maxH * frameRatio
          } else {
            frameW = maxW
            frameH = maxW / frameRatio
          }

          val baseScale = max(frameW / bmp.width.toFloat(), frameH / bmp.height.toFloat())
          val effectiveScale = baseScale * userScale
          val maxOffsetX = max(0f, (bmp.width * effectiveScale - frameW) / 2f)
          val maxOffsetY = max(0f, (bmp.height * effectiveScale - frameH) / 2f)

          val frameWdp = with(density) { frameW.toDp() }
          val frameHdp = with(density) { frameH.toDp() }

          Box(
            modifier = Modifier
              .size(frameWdp, frameHdp)
              .clip(RoundedCornerShape(8.dp))
              .clipToBounds()
              .background(Color.Black)
              .pointerInput(bmp, selectedAspect) {
                detectTransformGestures { _, pan, zoom, _ ->
                  val nextScale = (userScale * zoom).coerceIn(1f, 5f)
                  val nextEff = baseScale * nextScale
                  val nxMax = max(0f, (bmp.width * nextEff - frameW) / 2f)
                  val nyMax = max(0f, (bmp.height * nextEff - frameH) / 2f)
                  userScale = nextScale
                  offsetX = (offsetX + pan.x).coerceIn(-nxMax, nxMax)
                  offsetY = (offsetY + pan.y).coerceIn(-nyMax, nyMax)
                }
              }
              .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "Crop preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                  scaleX = userScale,
                  scaleY = userScale,
                  translationX = offsetX,
                  translationY = offsetY
                )
            )
            // Rule of thirds grid overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
              val gridColor = Color.White.copy(alpha = 0.35f)
              val w = size.width
              val h = size.height
              drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
              drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
              drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
              drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())
            }
          }

          // Hidden trigger holder inside composition scope so Apply button accesses exact geometry
          LaunchedEffect(userScale, maxOffsetX, maxOffsetY) {
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
          }

          // Bottom controls overlaid safely inside parent Column below BoxWithConstraints
          Surface(
            color = Color.Transparent,
            modifier = Modifier.align(Alignment.BottomCenter)
          ) {
            Box(Modifier.size(0.dp))
          }

          // Store current crop save action via remembered holder
          val saveAction: () -> Unit = {
            if (!isSaving) {
              isSaving = true
              scope.launch {
                val croppedUri = withContext(Dispatchers.IO) {
                  cropAndSaveBitmap(
                    context = context,
                    bitmap = bmp,
                    frameWidthPx = frameW,
                    frameHeightPx = frameH,
                    userScale = userScale,
                    offsetX = offsetX,
                    offsetY = offsetY
                  )
                }
                isSaving = false
                if (croppedUri != null) {
                  onCropped(croppedUri)
                } else {
                  onCropped(imageUri)
                }
              }
            }
          }

          // Floating Apply button at top-right helper slot or bottom bar via state
          CropApplyHolder.currentSave = saveAction
        }
      }

      // Zoom & Aspect controls
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF171A21))
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Pinch or drag to adjust", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { userScale = (userScale - 0.35f).coerceAtLeast(1f) },
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
            }
            Text("${"%.1f".format(userScale)}x", color = Color.White, style = MaterialTheme.typography.labelMedium)
            IconButton(
              onClick = { userScale = (userScale + 0.35f).coerceAtMost(5f) },
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
            }
            IconButton(
              onClick = {
                userScale = 1f
                offsetX = 0f
                offsetY = 0f
                rotationDeg = 0
              },
              modifier = Modifier.size(40.dp)
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = "Reset crop", tint = Color.White)
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CropAspect.entries.forEach { aspect ->
            FilterChip(
              selected = selectedAspect == aspect,
              onClick = {
                selectedAspect = aspect
                userScale = 1f
                offsetX = 0f
                offsetY = 0f
              },
              label = { Text(aspect.label) },
              colors = FilterChipDefaults.filterChipColors(
                labelColor = Color.White.copy(alpha = 0.85f),
                selectedLabelColor = Color.White,
                selectedContainerColor = MaterialTheme.colorScheme.primary
              )
            )
          }
        }

        Button(
          onClick = { CropApplyHolder.currentSave?.invoke() },
          enabled = !isLoading && !isSaving && rotatedBitmap != null,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("confirm_crop_button")
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Cropping...")
          } else {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Apply Crop")
          }
        }
      }
    }
  }
}

private object CropApplyHolder {
  var currentSave: (() -> Unit)? = null
}

private fun loadOrientedSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
  val resolver = context.contentResolver
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

  var sample = 1
  val maxSide = max(bounds.outWidth, bounds.outHeight)
  while (maxSide / sample > maxDim) {
    sample *= 2
  }
  val opts = BitmapFactory.Options().apply { inSampleSize = sample }
  val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

  val exifRotation = runCatching {
    resolver.openInputStream(uri)?.use { stream ->
      when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
      }
    } ?: 0f
  }.getOrDefault(0f)

  if (exifRotation == 0f) return raw
  val m = Matrix().apply { postRotate(exifRotation) }
  return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also {
    if (it != raw) raw.recycle()
  }
}

private fun cropAndSaveBitmap(
  context: Context,
  bitmap: Bitmap,
  frameWidthPx: Float,
  frameHeightPx: Float,
  userScale: Float,
  offsetX: Float,
  offsetY: Float
): String? {
  if (frameWidthPx <= 1f || frameHeightPx <= 1f) return null
  val baseScale = max(frameWidthPx / bitmap.width.toFloat(), frameHeightPx / bitmap.height.toFloat())
  val effScale = baseScale * userScale

  val visibleW = frameWidthPx / effScale
  val visibleH = frameHeightPx / effScale
  val centerX = (bitmap.width / 2f) - (offsetX / effScale)
  val centerY = (bitmap.height / 2f) - (offsetY / effScale)

  val left = (centerX - visibleW / 2f).roundToInt().coerceIn(0, bitmap.width - 1)
  val top = (centerY - visibleH / 2f).roundToInt().coerceIn(0, bitmap.height - 1)
  val cropW = visibleW.roundToInt().coerceIn(1, bitmap.width - left)
  val cropH = visibleH.roundToInt().coerceIn(1, bitmap.height - top)

  val cropped = Bitmap.createBitmap(bitmap, left, top, min(cropW, bitmap.width - left), min(cropH, bitmap.height - top))
  val outFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
  FileOutputStream(outFile).use { out ->
    cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
  }
  if (cropped != bitmap) cropped.recycle()
  return Uri.fromFile(outFile).toString()
}
