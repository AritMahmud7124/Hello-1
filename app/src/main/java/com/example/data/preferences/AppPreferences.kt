package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.helloPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hello_settings")

enum class ThemeMode(val value: String, val label: String) {
  SYSTEM("system", "System Default"),
  LIGHT("light", "Light Mode"),
  DARK("dark", "Dark Mode");

  companion object {
    fun fromValue(v: String?): ThemeMode = entries.firstOrNull { it.value == v } ?: SYSTEM
  }
}

enum class AppLanguage(val code: String, val label: String, val greetingWord: String) {
  EN("en", "English", "Hello"),
  ES("es", "Español", "Hola"),
  FR("fr", "Français", "Bonjour"),
  HI("hi", "हिन्दी", "नमस्ते"),
  BN("bn", "বাংলা", "হ্যালো");

  companion object {
    fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: EN
  }
}

class AppPreferences(private val context: Context) {
  private val themeKey = stringPreferencesKey("theme_mode")
  private val langKey = stringPreferencesKey("app_language")
  private val searchHistoryKey = stringPreferencesKey("search_history_json")
  private val reelsAutoAdvanceKey = booleanPreferencesKey("reels_auto_advance")
  private val reelsRegionKey = stringPreferencesKey("reels_feed_region")

  val themeMode: Flow<ThemeMode> = context.helloPrefsDataStore.data.map { prefs ->
    ThemeMode.fromValue(prefs[themeKey])
  }

  val language: Flow<AppLanguage> = context.helloPrefsDataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[langKey])
  }

  val searchHistory: Flow<List<String>> = context.helloPrefsDataStore.data.map { prefs ->
    decodeHistory(prefs[searchHistoryKey])
  }

  val reelsAutoAdvance: Flow<Boolean> = context.helloPrefsDataStore.data.map { prefs ->
    prefs[reelsAutoAdvanceKey] ?: true
  }

  val reelsRegion: Flow<String> = context.helloPrefsDataStore.data.map { prefs ->
    prefs[reelsRegionKey] ?: "GLOBAL"
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.helloPrefsDataStore.edit { it[themeKey] = mode.value }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.helloPrefsDataStore.edit { it[langKey] = language.code }
  }

  suspend fun setReelsAutoAdvance(enabled: Boolean) {
    context.helloPrefsDataStore.edit { it[reelsAutoAdvanceKey] = enabled }
  }

  suspend fun setReelsRegion(regionCode: String) {
    context.helloPrefsDataStore.edit { it[reelsRegionKey] = regionCode.trim().uppercase().ifBlank { "GLOBAL" } }
  }

  suspend fun addSearchHistory(query: String) {
    val clean = query.trim().take(80)
    if (clean.isBlank()) return
    context.helloPrefsDataStore.edit { prefs ->
      val current = decodeHistory(prefs[searchHistoryKey])
      val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(12)
      prefs[searchHistoryKey] = encodeHistory(updated)
    }
  }

  suspend fun removeSearchHistoryItem(query: String) {
    context.helloPrefsDataStore.edit { prefs ->
      val current = decodeHistory(prefs[searchHistoryKey])
      val updated = current.filterNot { it.equals(query, ignoreCase = true) }
      prefs[searchHistoryKey] = encodeHistory(updated)
    }
  }

  suspend fun clearSearchHistory() {
    context.helloPrefsDataStore.edit { prefs ->
      prefs.remove(searchHistoryKey)
    }
  }

  private fun decodeHistory(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val item = arr.optString(i).trim()
          if (item.isNotBlank()) add(item)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeHistory(items: List<String>): String {
    val arr = JSONArray()
    items.forEach { arr.put(it) }
    return arr.toString()
  }
}
