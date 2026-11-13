package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

  val themeMode: Flow<ThemeMode> = context.helloPrefsDataStore.data.map { prefs ->
    ThemeMode.fromValue(prefs[themeKey])
  }

  val language: Flow<AppLanguage> = context.helloPrefsDataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[langKey])
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.helloPrefsDataStore.edit { it[themeKey] = mode.value }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.helloPrefsDataStore.edit { it[langKey] = language.code }
  }
}
