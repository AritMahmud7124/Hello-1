package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
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
  FREE("Original", null),
  SQUARE("1:1", 1f),
  PORTRAIT("4:5", 4f / 5f),
  STORY("9:16", 9f / 16f),
  COVER("16:9", 16f / 9f)
}

enum class PhotoFilterPreset(val label: String, val accent: Color) {
  ORIGINAL("Original", Color(0xFF94A3B8)),
  VIVID("Vivid", Color(0xFFE11D48)),
  WARM("Warm", Color(0xFFD97706)),
  NOIR("B&W", Color(0xFF64748B)),
  FILM("Film", Color(0xFF059669));

  companion object {
    fun fromName(name: String): PhotoFilterPreset = entries.firstOrNull { it.name == name } ?: ORIGINAL
  }
}

data class PhotoAdjustments(
  val brightness: Float = 0f,
  val contrast: Float = 1f,
  val saturation: Float = 1f,
  val warmth: Float = 0f
) {
  val isDefault: Boolean
    get() = brightness == 0f && contrast == 1f && saturation == 1f && warmth == 0f
}

private enum class PhotoEditorTab(val label: String) {
  CROP("Crop"),
  FILTER("Filters"),
  ADJUST("Adjust")
}

fun buildPhotoFilterColorMatrix(
  preset: PhotoFilterPreset,
  adjustments: PhotoAdjustments = PhotoAdjustments()
): AndroidColorMatrix? {
  val base: AndroidColorMatrix? = when (preset) {
    PhotoFilterPreset.ORIGINAL -> null
    PhotoFilterPreset.VIVID -> AndroidColorMatrix().apply {
      setSaturation(1.38f)
      postConcat(
        AndroidColorMatrix(
          floatArrayOf(
            1.08f, 0f, 0f, 0f, -6f,
            0f, 1.08f, 0f, 0f, -6f,
            0f, 0f, 1.08f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    PhotoFilterPreset.WARM -> AndroidColorMatrix(
      floatArrayOf(
        1.12f, 0f, 0f, 0f, 10f,
        0f, 1.03f, 0f, 0f, 4f,
        0f, 0f, 0.90f, 0f, -6f,
        0f, 0f, 0f, 1f, 0f
      )
    )
    PhotoFilterPreset.NOIR -> AndroidColorMatrix().apply {
      setSaturation(0f)
      postConcat(
        AndroidColorMatrix(
          floatArrayOf(
            1.22f, 0f, 0f, 0f, -16f,
            0f, 1.22f, 0f, 0f, -16f,
            0f, 0f, 1.22f, 0f, -16f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    PhotoFilterPreset.FILM -> AndroidColorMatrix().apply {
      setSaturation(0.88f)
      postConcat(
        AndroidColorMatrix(
          floatArrayOf(
            1.05f, 0f, 0f, 0f, 8f,
            0f, 1.02f, 0f, 0f, 10f,
            0f, 0f, 1.08f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
  }

  if (adjustments.isDefault) return base

  val combined = base ?: AndroidColorMatrix()
  if (adjustments.saturation != 1f) {
    val satMatrix = AndroidColorMatrix().apply { setSaturation(adjustments.saturation.coerceIn(0f, 2f)) }
    combined.postConcat(satMatrix)
  }
  val c = adjustments.contrast.coerceIn(0.5f, 1.8f)
  val bShift = adjustments.brightness.coerceIn(-0.45f, 0.45f) * 255f
  val cOffset = 128f * (1f - c) + bShift
  val w = adjustments.warmth.coerceIn(-0.4f, 0.4f)
  val rScale = (1f + w * 0.35f).coerceAtLeast(0.5f)
  val bScale = (1f - w * 0.35f).coerceAtLeast(0.5f)
  val rShift = w * 18f
  val bShiftWarm = -w * 18f
  combined.postConcat(
    AndroidColorMatrix(
      floatArrayOf(
        c * rScale, 0f, 0f, 0f, cOffset + rShift,
        0f, c, 0f, 0f, cOffset,
        0f, 0f, c * bScale, 0f, cOffset + bShiftWarm,
        0f, 0f, 0f, 1f, 0f
      )
    )
  )
  return combined
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

  var activeTab by remember { mutableStateOf(PhotoEditorTab.CROP) }
  var selectedAspect by remember { mutableStateOf(defaultAspect) }
  var selectedFilter by remember { mutableStateOf(PhotoFilterPreset.ORIGINAL) }
  var adjustments by remember { mutableStateOf(PhotoAdjustments()) }
  var rotationDeg by remember { mutableIntStateOf(0) }
  var flipHorizontal by remember { mutableStateOf(false) }
  var userScale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(imageUri) {
    isLoading = true
    loadError = null
    val bmp = withContext(Dispatchers.IO) {
      loadOrientedSampledBitmap(context, Uri.parse(imageUri), maxDim = 2560)
    }
    if (bmp == null) {
      loadError = "Could not load selected image."
    } else {
      sourceBitmap = bmp
    }
    isLoading = false
  }

  val transformedBitmap = remember(sourceBitmap, rotationDeg, flipHorizontal) {
    val bmp = sourceBitmap ?: return@remember null
    if (rotationDeg % 360 == 0 && !flipHorizontal) {
      bmp
    } else {
      val matrix = Matrix().apply {
        if (flipHorizontal) postScale(-1f, 1f)
        if (rotationDeg % 360 != 0) postRotate(rotationDeg.toFloat())
      }
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
        .background(Color(0xFF0E0F12))
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Clean Photo Editor Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "Cancel crop", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Text(
            text = "Edit Photo",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (onUseOriginal != null) {
            Surface(
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.1f),
              modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onUseOriginal)
                .testTag("use_full_original_button")
            ) {
              Text(
                text = "Original",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
          IconButton(
            onClick = { flipHorizontal = !flipHorizontal },
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(if (flipHorizontal) Color.White.copy(alpha = 0.22f) else Color.Transparent)
          ) {
            Icon(Icons.Rounded.Flip, contentDescription = "Flip photo horizontally", tint = Color.White, modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = {
              rotationDeg = (rotationDeg + 90) % 360
              userScale = 1f
              offsetX = 0f
              offsetY = 0f
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }

      // Main Darkroom Crop Stage
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val bmp = transformedBitmap

        if (isLoading) {
          CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
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

          val outPxW = (frameW / effectiveScale).roundToInt().coerceIn(1, bmp.width)
          val outPxH = (frameH / effectiveScale).roundToInt().coerceIn(1, bmp.height)

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "${selectedAspect.label} · ${outPxW} × ${outPxH}",
              color = Color.White.copy(alpha = 0.7f),
              style = MaterialTheme.typography.labelSmall
            )

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
                .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              val previewColorFilter = remember(selectedFilter, adjustments) {
                buildPhotoFilterColorMatrix(selectedFilter, adjustments)?.let { cm ->
                  ColorFilter.colorMatrix(ColorMatrix(cm.array))
                }
              }
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Crop preview",
                contentScale = ContentScale.Crop,
                colorFilter = previewColorFilter,
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer(
                    scaleX = userScale,
                    scaleY = userScale,
                    translationX = offsetX,
                    translationY = offsetY
                  )
              )
              // Clean white rule-of-thirds + minimal white corner brackets
              Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color.White.copy(alpha = 0.22f)
                val w = size.width
                val h = size.height
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 0.8.dp.toPx())
                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 0.8.dp.toPx())
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 0.8.dp.toPx())
                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 0.8.dp.toPx())

                val cornerLen = min(w, h) * 0.09f
                val cornerStroke = 2.dp.toPx()
                val bracketColor = Color.White
                drawLine(bracketColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(0f, h - cornerLen), Offset(0f, h), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(w, h - cornerLen), Offset(w, h), strokeWidth = cornerStroke, cap = StrokeCap.Square)
                drawLine(bracketColor, Offset(w - cornerLen, h), Offset(w, h), strokeWidth = cornerStroke, cap = StrokeCap.Square)
              }
            }
          }

          LaunchedEffect(userScale, maxOffsetX, maxOffsetY) {
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
          }

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
                    offsetY = offsetY,
                    filterPreset = selectedFilter,
                    adjustments = adjustments
                  )
                }
                isSaving = false
                onCropped(croppedUri ?: imageUri)
              }
            }
          }
          CropApplyHolder.currentSave = saveAction
        }
      }

      // Clean Dark Editor Controls Deck (Instagram-like Crop / Filters / Adjust)
      Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color(0xFF16181D),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Mode Tabs Row (Crop / Filters / Adjust + Reset)
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              PhotoEditorTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Surface(
                  shape = CircleShape,
                  color = if (selected) Color.White else Color.White.copy(alpha = 0.09f),
                  modifier = Modifier
                    .clip(CircleShape)
                    .clickable { activeTab = tab }
                    .testTag("photo_editor_tab_${tab.name.lowercase()}")
                ) {
                  Text(
                    text = tab.label,
                    color = if (selected) Color(0xFF0E0F12) else Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                  )
                }
              }
            }
            IconButton(
              onClick = {
                userScale = 1f
                offsetX = 0f
                offsetY = 0f
                rotationDeg = 0
                flipHorizontal = false
                selectedFilter = PhotoFilterPreset.ORIGINAL
                adjustments = PhotoAdjustments()
              },
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = "Reset all edits", tint = Color.White, modifier = Modifier.size(17.dp))
            }
          }

          when (activeTab) {
            PhotoEditorTab.CROP -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                IconButton(
                  onClick = { userScale = (userScale - 0.3f).coerceAtLeast(1f) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out", tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(17.dp))
                }
                Slider(
                  value = userScale,
                  onValueChange = { userScale = it.coerceIn(1f, 5f) },
                  valueRange = 1f..5f,
                  colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                  ),
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { userScale = (userScale + 0.3f).coerceAtMost(5f) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in", tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(17.dp))
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                CropAspect.entries.forEach { aspect ->
                  val selected = selectedAspect == aspect
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        selectedAspect = aspect
                        userScale = 1f
                        offsetX = 0f
                        offsetY = 0f
                      }
                      .testTag("aspect_ratio_${aspect.name.lowercase()}")
                  ) {
                    Text(
                      text = aspect.label,
                      color = if (selected) Color(0xFF0E0F12) else Color.White.copy(alpha = 0.85f),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                  }
                }
              }
            }

            PhotoEditorTab.FILTER -> {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                PhotoFilterPreset.entries.forEach { preset ->
                  val selected = selectedFilter == preset
                  Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                      .clip(CircleShape)
                      .clickable { selectedFilter = preset }
                      .testTag("photo_filter_${preset.name.lowercase()}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(preset.accent)
                      )
                      Text(
                        text = preset.label,
                        color = if (selected) Color(0xFF0E0F12) else Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }
              }
            }

            PhotoEditorTab.ADJUST -> {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AdjustmentSliderRow(
                  label = "Brightness",
                  valueText = "${(adjustments.brightness * 100).roundToInt()}",
                  value = adjustments.brightness,
                  range = -0.4f..0.4f,
                  onValueChange = { adjustments = adjustments.copy(brightness = it) }
                )
                AdjustmentSliderRow(
                  label = "Contrast",
                  valueText = "${((adjustments.contrast - 1f) * 100).roundToInt()}",
                  value = adjustments.contrast,
                  range = 0.6f..1.6f,
                  onValueChange = { adjustments = adjustments.copy(contrast = it) }
                )
                AdjustmentSliderRow(
                  label = "Saturation",
                  valueText = "${((adjustments.saturation - 1f) * 100).roundToInt()}",
                  value = adjustments.saturation,
                  range = 0f..2f,
                  onValueChange = { adjustments = adjustments.copy(saturation = it) }
                )
                AdjustmentSliderRow(
                  label = "Warmth",
                  valueText = "${(adjustments.warmth * 100).roundToInt()}",
                  value = adjustments.warmth,
                  range = -0.35f..0.35f,
                  onValueChange = { adjustments = adjustments.copy(warmth = it) }
                )
              }
            }
          }

          Button(
            onClick = { CropApplyHolder.currentSave?.invoke() },
            enabled = !isLoading && !isSaving && transformedBitmap != null,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF10B981),
              contentColor = Color.White,
              disabledContainerColor = Color.White.copy(alpha = 0.14f),
              disabledContentColor = Color.White.copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("confirm_crop_button")
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(Modifier.width(8.dp))
              Text("Saving...", color = Color.White, fontWeight = FontWeight.SemiBold)
            } else {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdjustmentSliderRow(
  label: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.85f),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.width(68.dp)
    )
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color(0xFF10B981),
        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
      ),
      modifier = Modifier
        .weight(1f)
        .height(28.dp)
    )
    Text(
      text = valueText,
      color = Color.White.copy(alpha = 0.72f),
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.width(30.dp)
    )
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
  offsetY: Float,
  filterPreset: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
  adjustments: PhotoAdjustments = PhotoAdjustments()
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
  val cm = buildPhotoFilterColorMatrix(filterPreset, adjustments)
  val outputBmp = if (cm != null) {
    Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888).also { dst ->
      val canvas = AndroidCanvas(dst)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(cm)
      }
      canvas.drawBitmap(cropped, 0f, 0f, paint)
    }
  } else {
    cropped
  }
  val outFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
  FileOutputStream(outFile).use { out ->
    outputBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
  }
  if (outputBmp != cropped && outputBmp != bitmap) outputBmp.recycle()
  if (cropped != bitmap) cropped.recycle()
  return Uri.fromFile(outFile).toString()
}
