package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.YouTubeShortVideo
import com.example.security.PinnedNetworkClient
import java.net.URLEncoder
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

sealed class YouTubeFetchResult {
  data class Success(val items: List<YouTubeShortVideo>) : YouTubeFetchResult()
  data object MissingApiKey : YouTubeFetchResult()
  data class Error(val message: String) : YouTubeFetchResult()
}

object YouTubeShortsClient {
  private const val TAG = "YouTubeShortsClient"
  private const val PLACEHOLDER_KEY = "MY_YOUTUBE_API_KEY"
  private const val YOUTUBE_SEARCH_ENDPOINT = "https://www.googleapis.com/youtube/v3/search"

  fun isApiKeyConfigured(apiKey: String = BuildConfig.YOUTUBE_API_KEY): Boolean {
    val trimmed = apiKey.trim()
    return trimmed.isNotEmpty() && !trimmed.equals(PLACEHOLDER_KEY, ignoreCase = true)
  }

  fun buildEmbedUrl(videoId: String, autoPlay: Boolean = true): String {
    val safeId = videoId.trim().take(32)
    val autoFlag = if (autoPlay) "1" else "0"
    return "https://www.youtube.com/embed/$safeId?autoplay=$autoFlag&playsinline=1&rel=0&modestbranding=1&enablejsapi=1"
  }

  fun extractVideoId(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
      return trimmed
    }
    val patterns = listOf(
      Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
      Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
      Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
      Regex("[?&]v=([a-zA-Z0-9_-]{11})")
    )
    for (pattern in patterns) {
      val match = pattern.find(trimmed)
      if (match != null && match.groupValues.size > 1) {
        return match.groupValues[1]
      }
    }
    return ""
  }

  private val GLOBAL_DIVERSE_REGIONS = listOf("US", "BD", "GB", "JP")

  suspend fun fetchShortsForCategories(
    categories: List<String>,
    regionCode: String = "GLOBAL",
    perCategoryCount: Int = 5,
    apiKey: String = BuildConfig.YOUTUBE_API_KEY
  ): YouTubeFetchResult = withContext(Dispatchers.IO) {
    if (!isApiKeyConfigured(apiKey)) {
      return@withContext YouTubeFetchResult.MissingApiKey
    }
    val safeCategories = categories
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .take(4)
    if (safeCategories.isEmpty()) {
      return@withContext YouTubeFetchResult.Success(emptyList())
    }

    val cleanRegion = regionCode.trim().uppercase().ifBlank { "GLOBAL" }
    val isGlobalGeneral = cleanRegion == "GLOBAL"

    val collected = mutableListOf<YouTubeShortVideo>()
    var lastError: String? = null
    val seenIds = mutableSetOf<String>()

    safeCategories.forEachIndexed { idx, category ->
      val effectiveRegion = if (isGlobalGeneral) {
        GLOBAL_DIVERSE_REGIONS[idx % GLOBAL_DIVERSE_REGIONS.size]
      } else {
        cleanRegion
      }
      val query = URLEncoder.encode("$category shorts", "UTF-8")
      val urlStr = "$YOUTUBE_SEARCH_ENDPOINT" +
        "?part=snippet" +
        "&type=video" +
        "&videoEmbeddable=true" +
        "&videoDuration=short" +
        "&safeSearch=strict" +
        "&order=relevance" +
        "&regionCode=${URLEncoder.encode(effectiveRegion, "UTF-8")}" +
        "&maxResults=${perCategoryCount.coerceIn(2, 10)}" +
        "&q=$query" +
        "&key=${URLEncoder.encode(apiKey.trim(), "UTF-8")}"

      val result = runCatching {
        val request = Request.Builder()
          .url(urlStr)
          .get()
          .header("Accept", "application/json")
          .build()
        PinnedNetworkClient.client.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            val apiMsg = runCatching {
              JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")
            throw IllegalStateException(
              apiMsg.ifBlank { "YouTube Data API returned HTTP ${response.code}" }
            )
          }
          parseSearchResponse(body, category, effectiveRegion)
        }
      }

      result.onSuccess { list ->
        for (item in list) {
          if (seenIds.add(item.videoId)) {
            collected.add(item)
          }
        }
      }.onFailure { err ->
        if (err is SSLPeerUnverifiedException) {
          Log.e(TAG, "SSL Pinning validation failed (possible MitM): ${err.message}")
          lastError = "Secure SSL pinning verification failed. Connection blocked."
        } else {
          Log.w(TAG, "YouTube Shorts fetch failed for category=$category region=$effectiveRegion: ${err.message}")
          lastError = err.message
        }
      }
    }

    if (collected.isEmpty() && lastError != null) {
      YouTubeFetchResult.Error(lastError!!)
    } else {
      YouTubeFetchResult.Success(collected)
    }
  }

  internal fun parseSearchResponse(
    jsonString: String,
    category: String,
    regionCode: String = "GLOBAL"
  ): List<YouTubeShortVideo> {
    if (jsonString.isBlank()) return emptyList()
    val root = JSONObject(jsonString)
    val items = root.optJSONArray("items") ?: return emptyList()
    val nowSec = System.currentTimeMillis() / 1000L
    val out = ArrayList<YouTubeShortVideo>(items.length())
    for (i in 0 until items.length()) {
      val item = items.optJSONObject(i) ?: continue
      val idObj = item.optJSONObject("id") ?: continue
      val videoId = idObj.optString("videoId").trim()
      if (videoId.isBlank()) continue
      val snippet = item.optJSONObject("snippet") ?: continue
      val title = snippet.optString("title").trim().ifBlank { "$category Short" }
      val channel = snippet.optString("channelTitle").trim().ifBlank { "YouTube Creator" }
      val thumbs = snippet.optJSONObject("thumbnails")
      val thumbUrl = thumbs?.optJSONObject("high")?.optString("url")
        ?: thumbs?.optJSONObject("medium")?.optString("url")
        ?: thumbs?.optJSONObject("default")?.optString("url")
        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
      out.add(
        YouTubeShortVideo(
          videoId = videoId,
          title = title,
          channelTitle = channel,
          thumbnailUrl = thumbUrl,
          category = category,
          regionCode = regionCode.ifBlank { "GLOBAL" },
          publishedAtEpochSec = nowSec - i * 60L
        )
      )
    }
    return out
  }
}
