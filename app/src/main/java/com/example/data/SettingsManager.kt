package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.KeyboardTheme
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "typegenius_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_SPACEBAR_SWIPE = booleanPreferencesKey("spacebar_swipe_enabled")
        val KEY_SWIPE_SENSITIVITY = stringPreferencesKey("swipe_sensitivity")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_SHOW_LANG_ON_SPACE = booleanPreferencesKey("show_lang_on_spacebar")
        val KEY_LONG_PRESS_LANG_PICKER = booleanPreferencesKey("long_press_lang_picker")
        val KEY_ENABLED_LANGUAGES = stringSetPreferencesKey("enabled_languages")
        val KEY_ACTIVE_LANGUAGE = stringPreferencesKey("active_language")
        val KEY_SHOW_ALT_LANG_KEY = booleanPreferencesKey("show_alt_lang_key")
        val KEY_THEME = stringPreferencesKey("keyboard_theme")
        val KEY_NUMBER_ROW = booleanPreferencesKey("number_row_enabled")
        val KEY_SOUND_ON_KEY = booleanPreferencesKey("sound_on_keypress")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val isSpacebarSwipeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPACEBAR_SWIPE] ?: true
    }

    val swipeSensitivity: Flow<SwipeSensitivity> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SWIPE_SENSITIVITY] ?: SwipeSensitivity.MEDIUM.name
        SwipeSensitivity.fromName(raw)
    }

    val isHapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK] ?: true
    }

    val showActiveLanguageOnSpacebar: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_LANG_ON_SPACE] ?: true
    }

    val longPressLanguageSelector: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LONG_PRESS_LANG_PICKER] ?: true
    }

    val enabledLanguages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_LANGUAGES] ?: setOf(
            LanguageId.BANGLA_PROBHAT.code,
            LanguageId.BANGLA_PHONETIC.code,
            LanguageId.ENGLISH.code,
            LanguageId.HINDI.code,
            LanguageId.ARABIC.code
        )
    }

    val activeLanguageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_LANGUAGE] ?: LanguageId.BANGLA_PROBHAT.code
    }

    val showAlternativeLanguageKey: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_ALT_LANG_KEY] ?: true
    }

    val keyboardTheme: Flow<KeyboardTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: KeyboardTheme.TYPEGENIUS_PURPLE.id
        KeyboardTheme.fromId(raw)
    }

    val numberRowEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NUMBER_ROW] ?: true
    }

    val soundOnKeyPress: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ON_KEY] ?: false
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    suspend fun setSpacebarSwipeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPACEBAR_SWIPE] = enabled }
    }

    suspend fun setSwipeSensitivity(sensitivity: SwipeSensitivity) {
        context.dataStore.edit { it[KEY_SWIPE_SENSITIVITY] = sensitivity.name }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setShowActiveLanguageOnSpacebar(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_LANG_ON_SPACE] = show }
    }

    suspend fun setLongPressLanguageSelector(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LONG_PRESS_LANG_PICKER] = enabled }
    }

    suspend fun setEnabledLanguages(languages: Set<String>) {
        context.dataStore.edit { it[KEY_ENABLED_LANGUAGES] = languages }
    }

    suspend fun setActiveLanguageCode(code: String) {
        context.dataStore.edit { it[KEY_ACTIVE_LANGUAGE] = code }
    }

    suspend fun setShowAlternativeLanguageKey(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_ALT_LANG_KEY] = show }
    }

    suspend fun setKeyboardTheme(theme: KeyboardTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.id }
    }

    suspend fun setNumberRowEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NUMBER_ROW] = enabled }
    }

    suspend fun setSoundOnKeyPress(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ON_KEY] = enabled }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[KEY_GEMINI_API_KEY] = key.trim() }
    }
}
