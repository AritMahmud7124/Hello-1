package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject

enum class StoryFontStyle(
  val id: String,
  val label: String,
  val family: FontFamily,
  val weight: FontWeight
) {
  CLASSIC("classic", "Classic", FontFamily.SansSerif, FontWeight.Bold),
  MODERN("modern", "Modern", FontFamily.SansSerif, FontWeight.ExtraBold),
  SERIF("serif", "Serif", FontFamily.Serif, FontWeight.Bold),
  TYPEWRITER("mono", "Typewriter", FontFamily.Monospace, FontWeight.SemiBold),
  NEON("neon", "Neon", FontFamily.Cursive, FontWeight.Bold);

  companion object {
    fun fromId(id: String): StoryFontStyle = entries.firstOrNull { it.id == id } ?: CLASSIC
  }
}

enum class StoryCaptionBgMode(val id: String, val label: String) {
  NONE("none", "Plain"),
  FILLED("filled", "Box"),
  TRANSLUCENT("soft", "Frost");

  companion object {
    fun fromId(id: String): StoryCaptionBgMode = entries.firstOrNull { it.id == id } ?: NONE
  }
}

enum class StoryLyricStyle(val id: String, val label: String) {
  BOX("box", "Box"),
  KARAOKE("karaoke", "Glow"),
  MINIMAL("minimal", "Clean"),
  CLASSIC("classic", "Serif");

  companion object {
    fun fromId(id: String): StoryLyricStyle = entries.firstOrNull { it.id == id } ?: BOX
  }
}

data class StoryStickerOverlay(
  val id: String,
  val text: String,
  val xNorm: Float = 0.5f,
  val yNorm: Float = 0.32f,
  val scale: Float = 1f
)

data class StoryMentionOverlay(
  val id: String,
  val uid: String,
  val handle: String,
  val xNorm: Float = 0.5f,
  val yNorm: Float = 0.68f,
  val scale: Float = 1f
)

data class StoryBrushStroke(
  val colorHex: String,
  val strokeWidthNorm: Float,
  val points: List<Pair<Float, Float>>
)

data class StoryOverlaySpec(
  val captionXNorm: Float = 0.5f,
  val captionYNorm: Float = 0.5f,
  val captionScale: Float = 1f,
  val captionFont: String = StoryFontStyle.CLASSIC.id,
  val captionColorHex: String = "#FFFFFF",
  val captionBgMode: String = StoryCaptionBgMode.NONE.id,
  val photoFilter: String = PhotoFilterPreset.ORIGINAL.name,
  val lyricXNorm: Float = 0.5f,
  val lyricYNorm: Float = 0.72f,
  val lyricScale: Float = 1f,
  val lyricStyle: String = StoryLyricStyle.BOX.id,
  val isMutualOnly: Boolean = false,
  val stickers: List<StoryStickerOverlay> = emptyList(),
  val mentions: List<StoryMentionOverlay> = emptyList(),
  val strokes: List<StoryBrushStroke> = emptyList()
) {
  fun toJsonString(): String {
    val root = JSONObject().apply {
      put("cx", captionXNorm.toDouble())
      put("cy", captionYNorm.toDouble())
      put("cs", captionScale.toDouble())
      put("cf", captionFont)
      put("cc", captionColorHex)
      put("cb", captionBgMode)
      put("pf", photoFilter)
      put("lx", lyricXNorm.toDouble())
      put("ly", lyricYNorm.toDouble())
      put("ls", lyricScale.toDouble())
      put("lst", lyricStyle)
      if (isMutualOnly) put("mu", true)
      if (stickers.isNotEmpty()) {
        put("st", JSONArray().apply {
          stickers.take(12).forEach { s ->
            put(JSONObject().apply {
              put("id", s.id)
              put("t", s.text.take(40))
              put("x", s.xNorm.toDouble())
              put("y", s.yNorm.toDouble())
              put("s", s.scale.toDouble())
            })
          }
        })
      }
      if (mentions.isNotEmpty()) {
        put("mn", JSONArray().apply {
          mentions.take(8).forEach { m ->
            put(JSONObject().apply {
              put("id", m.id)
              put("u", m.uid.take(80))
              put("h", m.handle.take(50))
              put("x", m.xNorm.toDouble())
              put("y", m.yNorm.toDouble())
              put("s", m.scale.toDouble())
            })
          }
        })
      }
      if (strokes.isNotEmpty()) {
        put("dr", JSONArray().apply {
          strokes.take(24).forEach { stroke ->
            if (stroke.points.size >= 2) {
              put(JSONObject().apply {
                put("c", stroke.colorHex)
                put("w", stroke.strokeWidthNorm.toDouble())
                val pts = JSONArray()
                // Compactly downsample stroke points to stay well under Firestore limit
                val step = (stroke.points.size / 24).coerceAtLeast(1)
                stroke.points.forEachIndexed { idx, p ->
                  if (idx % step == 0 || idx == stroke.points.lastIndex) {
                    pts.put(Math.round(p.first.coerceIn(0f, 1f) * 1000) / 1000.0)
                    pts.put(Math.round(p.second.coerceIn(0f, 1f) * 1000) / 1000.0)
                  }
                }
                put("p", pts)
              })
            }
          }
        })
      }
    }
    return root.toString().take(7800)
  }

  companion object {
    fun parse(raw: String): StoryOverlaySpec {
      if (raw.isBlank()) return StoryOverlaySpec()
      return runCatching {
        val root = JSONObject(raw)
        val stickers = mutableListOf<StoryStickerOverlay>()
        val stArr = root.optJSONArray("st")
        if (stArr != null) {
          for (i in 0 until stArr.length()) {
            val o = stArr.optJSONObject(i) ?: continue
            stickers += StoryStickerOverlay(
              id = o.optString("id", "st_$i"),
              text = o.optString("t", ""),
              xNorm = o.optDouble("x", 0.5).toFloat().coerceIn(0.08f, 0.92f),
              yNorm = o.optDouble("y", 0.35).toFloat().coerceIn(0.08f, 0.92f),
              scale = o.optDouble("s", 1.0).toFloat().coerceIn(0.45f, 2.8f)
            )
          }
        }
        val mentions = mutableListOf<StoryMentionOverlay>()
        val mnArr = root.optJSONArray("mn")
        if (mnArr != null) {
          for (i in 0 until mnArr.length()) {
            val o = mnArr.optJSONObject(i) ?: continue
            mentions += StoryMentionOverlay(
              id = o.optString("id", "mn_$i"),
              uid = o.optString("u", ""),
              handle = o.optString("h", ""),
              xNorm = o.optDouble("x", 0.5).toFloat().coerceIn(0.08f, 0.92f),
              yNorm = o.optDouble("y", 0.65).toFloat().coerceIn(0.08f, 0.92f),
              scale = o.optDouble("s", 1.0).toFloat().coerceIn(0.55f, 2.4f)
            )
          }
        }
        val strokes = mutableListOf<StoryBrushStroke>()
        val drArr = root.optJSONArray("dr")
        if (drArr != null) {
          for (i in 0 until drArr.length()) {
            val o = drArr.optJSONObject(i) ?: continue
            val ptsArr = o.optJSONArray("p") ?: continue
            val pts = mutableListOf<Pair<Float, Float>>()
            var j = 0
            while (j + 1 < ptsArr.length()) {
              pts += ptsArr.optDouble(j, 0.5).toFloat() to ptsArr.optDouble(j + 1, 0.5).toFloat()
              j += 2
            }
            if (pts.size >= 2) {
              strokes += StoryBrushStroke(
                colorHex = o.optString("c", "#FFFFFF"),
                strokeWidthNorm = o.optDouble("w", 0.012).toFloat().coerceIn(0.004f, 0.04f),
                points = pts
              )
            }
          }
        }
        StoryOverlaySpec(
          captionXNorm = root.optDouble("cx", 0.5).toFloat().coerceIn(0.08f, 0.92f),
          captionYNorm = root.optDouble("cy", 0.5).toFloat().coerceIn(0.08f, 0.92f),
          captionScale = root.optDouble("cs", 1.0).toFloat().coerceIn(0.5f, 2.8f),
          captionFont = root.optString("cf", StoryFontStyle.CLASSIC.id),
          captionColorHex = root.optString("cc", "#FFFFFF"),
          captionBgMode = root.optString("cb", StoryCaptionBgMode.NONE.id),
          photoFilter = root.optString("pf", PhotoFilterPreset.ORIGINAL.name),
          lyricXNorm = root.optDouble("lx", 0.5).toFloat().coerceIn(0.08f, 0.92f),
          lyricYNorm = root.optDouble("ly", 0.72).toFloat().coerceIn(0.12f, 0.88f),
          lyricScale = root.optDouble("ls", 1.0).toFloat().coerceIn(0.55f, 2.4f),
          lyricStyle = root.optString("lst", StoryLyricStyle.BOX.id),
          isMutualOnly = root.optBoolean("mu", false),
          stickers = stickers,
          mentions = mentions,
          strokes = strokes
        )
      }.getOrDefault(StoryOverlaySpec())
    }
  }
}

fun parseHexColorSafe(hex: String, fallback: Color = Color.White): Color {
  val clean = hex.trim().removePrefix("#")
  return runCatching {
    val value = clean.toLong(16)
    when (clean.length) {
      6 -> Color((0xFF000000L or value).toInt())
      8 -> Color(value.toInt())
      else -> fallback
    }
  }.getOrDefault(fallback)
}
