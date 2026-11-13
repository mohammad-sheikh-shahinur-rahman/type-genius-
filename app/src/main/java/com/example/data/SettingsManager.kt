package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.LanguageId
import com.example.model.OneHandedMode
import com.example.model.SoundProfile
import com.example.model.SwipeSensitivity
import com.example.model.VibrationStrength
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
        val KEY_AUTO_CAPITALIZE = booleanPreferencesKey("auto_capitalize")
        val KEY_SPELL_CHECK = booleanPreferencesKey("spell_check_enabled")
        val KEY_ONE_HANDED_MODE = stringPreferencesKey("one_handed_mode")
        val KEY_KEYBOARD_HEIGHT = stringPreferencesKey("keyboard_height_preset")
        val KEY_FLOATING_MODE = booleanPreferencesKey("floating_mode_enabled")
        val KEY_TRANSLATOR_SRC = stringPreferencesKey("translator_source_lang")
        val KEY_TRANSLATOR_TGT = stringPreferencesKey("translator_target_lang")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_WALLPAPER_PATH = stringPreferencesKey("keyboard_wallpaper_path")
        val KEY_WALLPAPER_DIM_ALPHA = floatPreferencesKey("wallpaper_dim_alpha")
        val KEY_KEY_BG_ALPHA = floatPreferencesKey("key_bg_alpha")
        val KEY_INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val KEY_VIBRATION_STRENGTH = stringPreferencesKey("vibration_strength")
        val KEY_SOUND_PROFILE = stringPreferencesKey("sound_profile")
        val KEY_GLIDE_TYPING = booleanPreferencesKey("glide_typing_enabled")
        val KEY_BACKSPACE_SWIPE = booleanPreferencesKey("backspace_swipe_delete")

        val KEY_LARGE_NUMBER_ROW = booleanPreferencesKey("large_number_row")
        val KEY_HIDE_LONG_PRESS_HINTS = booleanPreferencesKey("hide_long_press_hints")
        val KEY_HEIGHT_PORTRAIT = intPreferencesKey("keyboard_height_portrait")
        val KEY_HEIGHT_LANDSCAPE = intPreferencesKey("keyboard_height_landscape")
        val KEY_ONE_HANDED_PORTRAIT = intPreferencesKey("one_handed_width_portrait")
        val KEY_ONE_HANDED_LANDSCAPE = intPreferencesKey("one_handed_width_landscape")
        val KEY_SPLIT_FOLDABLE = booleanPreferencesKey("split_keyboard_foldable")
        val KEY_ALWAYS_ENTER = booleanPreferencesKey("always_enter_button")
        val KEY_HAS_SEEN_SWIPE_TUTORIAL = booleanPreferencesKey("has_seen_swipe_tutorial")

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
            LanguageId.SYLHETI.code,
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
        val raw = prefs[KEY_THEME] ?: KeyboardTheme.MATERIAL_YOU.id
        KeyboardTheme.fromId(raw)
    }

    val numberRowEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NUMBER_ROW] ?: false
    }

    val soundOnKeyPress: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ON_KEY] ?: false
    }

    val autoCapitalize: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CAPITALIZE] ?: true
    }

    val spellCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPELL_CHECK] ?: true
    }

    val oneHandedMode: Flow<OneHandedMode> = context.dataStore.data.map { prefs ->
        OneHandedMode.fromId(prefs[KEY_ONE_HANDED_MODE] ?: OneHandedMode.FULL.id)
    }

    val keyboardHeightPreset: Flow<KeyboardHeightPreset> = context.dataStore.data.map { prefs ->
        KeyboardHeightPreset.fromId(prefs[KEY_KEYBOARD_HEIGHT] ?: KeyboardHeightPreset.NORMAL.id)
    }

    val floatingMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLOATING_MODE] ?: false
    }

    val translatorSourceCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRANSLATOR_SRC] ?: "bn"
    }

    val translatorTargetCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRANSLATOR_TGT] ?: "en"
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    val wallpaperPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_PATH] ?: ""
    }

    val wallpaperDimAlpha: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[KEY_WALLPAPER_DIM_ALPHA] ?: 0.42f).coerceIn(0f, 0.85f)
    }

    val keyBackgroundAlpha: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[KEY_KEY_BG_ALPHA] ?: 0.78f).coerceIn(0.15f, 1.0f)
    }

    val isIncognitoMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_INCOGNITO_MODE] ?: false
    }

    val vibrationStrength: Flow<VibrationStrength> = context.dataStore.data.map { prefs ->
        VibrationStrength.fromId(prefs[KEY_VIBRATION_STRENGTH] ?: VibrationStrength.MEDIUM.id)
    }

    val soundProfile: Flow<SoundProfile> = context.dataStore.data.map { prefs ->
        SoundProfile.fromId(prefs[KEY_SOUND_PROFILE] ?: SoundProfile.SYSTEM.id)
    }

    val isGlideTypingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLIDE_TYPING] ?: true
    }

    val isBackspaceSwipeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKSPACE_SWIPE] ?: true
    }

    val largeNumberRow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_LARGE_NUMBER_ROW] ?: false }
    val hideLongPressHints: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_HIDE_LONG_PRESS_HINTS] ?: false }
    val keyboardHeightPortrait: Flow<Int> = context.dataStore.data.map { prefs -> prefs[KEY_HEIGHT_PORTRAIT] ?: 100 }
    val keyboardHeightLandscape: Flow<Int> = context.dataStore.data.map { prefs -> prefs[KEY_HEIGHT_LANDSCAPE] ?: 100 }
    val oneHandedWidthPortrait: Flow<Int> = context.dataStore.data.map { prefs -> prefs[KEY_ONE_HANDED_PORTRAIT] ?: 85 }
    val oneHandedWidthLandscape: Flow<Int> = context.dataStore.data.map { prefs -> prefs[KEY_ONE_HANDED_LANDSCAPE] ?: 40 }
    val splitKeyboardFoldable: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_SPLIT_FOLDABLE] ?: true }
    val alwaysEnterButton: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_ALWAYS_ENTER] ?: true }

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

    suspend fun setAutoCapitalize(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CAPITALIZE] = enabled }
    }

    suspend fun setSpellCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPELL_CHECK] = enabled }
    }

    suspend fun setOneHandedMode(mode: OneHandedMode) {
        context.dataStore.edit { it[KEY_ONE_HANDED_MODE] = mode.id }
    }

    suspend fun setKeyboardHeightPreset(preset: KeyboardHeightPreset) {
        context.dataStore.edit { it[KEY_KEYBOARD_HEIGHT] = preset.id }
    }

    suspend fun setFloatingMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_MODE] = enabled }
    }

    suspend fun setTranslatorLanguages(sourceCode: String, targetCode: String) {
        context.dataStore.edit {
            it[KEY_TRANSLATOR_SRC] = sourceCode
            it[KEY_TRANSLATOR_TGT] = targetCode
        }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[KEY_GEMINI_API_KEY] = key.trim() }
    }

    suspend fun setWallpaperPath(path: String) {
        context.dataStore.edit { it[KEY_WALLPAPER_PATH] = path }
    }

    suspend fun setWallpaperDimAlpha(alpha: Float) {
        context.dataStore.edit { it[KEY_WALLPAPER_DIM_ALPHA] = alpha.coerceIn(0f, 0.85f) }
    }

    suspend fun setKeyBackgroundAlpha(alpha: Float) {
        context.dataStore.edit { it[KEY_KEY_BG_ALPHA] = alpha.coerceIn(0.15f, 1.0f) }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_INCOGNITO_MODE] = enabled }
    }

    suspend fun setVibrationStrength(strength: VibrationStrength) {
        context.dataStore.edit { it[KEY_VIBRATION_STRENGTH] = strength.id }
    }

    suspend fun setSoundProfile(profile: SoundProfile) {
        context.dataStore.edit { it[KEY_SOUND_PROFILE] = profile.id }
    }

    suspend fun setGlideTypingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GLIDE_TYPING] = enabled }
    }

    suspend fun setBackspaceSwipeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BACKSPACE_SWIPE] = enabled }
    }

    suspend fun setLargeNumberRow(enabled: Boolean) { context.dataStore.edit { it[KEY_LARGE_NUMBER_ROW] = enabled } }
    suspend fun setHideLongPressHints(hide: Boolean) { context.dataStore.edit { it[KEY_HIDE_LONG_PRESS_HINTS] = hide } }
    suspend fun setKeyboardHeightPortrait(pct: Int) { context.dataStore.edit { it[KEY_HEIGHT_PORTRAIT] = pct.coerceIn(50, 150) } }
    suspend fun setKeyboardHeightLandscape(pct: Int) { context.dataStore.edit { it[KEY_HEIGHT_LANDSCAPE] = pct.coerceIn(50, 150) } }
    suspend fun setOneHandedWidthPortrait(pct: Int) { context.dataStore.edit { it[KEY_ONE_HANDED_PORTRAIT] = pct.coerceIn(50, 100) } }
    suspend fun setOneHandedWidthLandscape(pct: Int) { context.dataStore.edit { it[KEY_ONE_HANDED_LANDSCAPE] = pct.coerceIn(30, 90) } }
    suspend fun setSplitKeyboardFoldable(enabled: Boolean) { context.dataStore.edit { it[KEY_SPLIT_FOLDABLE] = enabled } }
    suspend fun setAlwaysEnterButton(enabled: Boolean) { context.dataStore.edit { it[KEY_ALWAYS_ENTER] = enabled } }

    val hasSeenSwipeTutorial: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_HAS_SEEN_SWIPE_TUTORIAL] ?: false }
    suspend fun setHasSeenSwipeTutorial(seen: Boolean) { context.dataStore.edit { it[KEY_HAS_SEEN_SWIPE_TUTORIAL] = seen } }
}
