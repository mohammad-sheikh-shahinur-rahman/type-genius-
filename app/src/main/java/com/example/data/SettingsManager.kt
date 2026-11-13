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
import com.example.data.dictionary.FancyFontStyle
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
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
        val KEY_ACTIVE_FANCY_FONT = stringPreferencesKey("active_fancy_font_style")
        val KEY_TOOLBAR_TOOLS = stringPreferencesKey("toolbar_tools_order")
        val KEY_KEYBOARD_FONT_ID = stringPreferencesKey("keyboard_font_id")
        val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val KEY_CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val KEY_CUSTOM_AI_PROMPTS = stringPreferencesKey("custom_ai_prompts")
        val KEY_NOTEPAD_DRAFT = stringPreferencesKey("notepad_draft")
        val KEY_USER_TEMPLATES = stringPreferencesKey("user_templates")
        val KEY_DOWNLOADED_TRANSLATOR_PACKS = stringSetPreferencesKey("downloaded_translator_packs")

        val DEFAULT_USER_TEMPLATES: List<String> = listOf(
            "আসসালামু আলাইকুম, কেমন আছেন?",
            "ধন্যবাদ, কিছুক্ষণ পর যোগাযোগ করছি।",
            "আফনে বালা আছইননি? সব ঠিকঠাক নি?",
            "Thanks! I will get back to you shortly."
        )

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
        val clean = key.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
            .replace(" ", "")
            .removeSurrounding("\"")
            .removeSurrounding("'")
        context.dataStore.edit { it[KEY_GEMINI_API_KEY] = clean }
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

    val activeFancyFontStyle: Flow<FancyFontStyle> = context.dataStore.data.map { prefs ->
        FancyFontStyle.fromId(prefs[KEY_ACTIVE_FANCY_FONT] ?: FancyFontStyle.NORMAL.id)
    }
    suspend fun setActiveFancyFontStyle(style: FancyFontStyle) {
        context.dataStore.edit { it[KEY_ACTIVE_FANCY_FONT] = style.id }
    }

    val toolbarTools: Flow<List<KeyboardToolId>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_TOOLBAR_TOOLS]
        if (raw.isNullOrBlank()) {
            KeyboardToolId.DEFAULT_TOOLBAR
        } else {
            val parsed = raw.split(",").mapNotNull { KeyboardToolId.fromId(it.trim()) }.distinct()
            parsed.ifEmpty { KeyboardToolId.DEFAULT_TOOLBAR }
        }
    }

    suspend fun setToolbarTools(tools: List<KeyboardToolId>) {
        val clean = tools.distinct().ifEmpty { listOf(KeyboardToolId.TEXT_EDITING) }
        context.dataStore.edit { it[KEY_TOOLBAR_TOOLS] = clean.joinToString(",") { tool -> tool.id } }
    }

    val keyboardFontId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_KEYBOARD_FONT_ID] ?: KeyboardFontPreset.ATMA_HANDWRITING.id
    }

    val customFontPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_FONT_PATH] ?: ""
    }

    val customFontName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_FONT_NAME] ?: ""
    }

    suspend fun setKeyboardFontId(fontId: String) {
        context.dataStore.edit { it[KEY_KEYBOARD_FONT_ID] = fontId }
    }

    suspend fun setCustomFont(path: String, displayName: String) {
        context.dataStore.edit {
            it[KEY_CUSTOM_FONT_PATH] = path
            it[KEY_CUSTOM_FONT_NAME] = displayName
            if (path.isNotBlank()) {
                it[KEY_KEYBOARD_FONT_ID] = KeyboardFontPreset.CUSTOM_IMPORTED.id
            }
        }
    }

    val customAiPrompts: Flow<List<Pair<String, String>>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_AI_PROMPTS].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\u001E").mapNotNull { entry ->
            val parts = entry.split("\u001F", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                parts[0] to parts[1]
            } else null
        }
    }

    suspend fun setCustomAiPrompts(prompts: List<Pair<String, String>>) {
        val encoded = prompts.filter { it.first.isNotBlank() && it.second.isNotBlank() }
            .joinToString("\u001E") { (t, p) ->
                "${t.trim().replace("\u001F", " ").replace("\u001E", " ")}\u001F${p.trim().replace("\u001F", " ").replace("\u001E", " ")}"
            }
        context.dataStore.edit { it[KEY_CUSTOM_AI_PROMPTS] = encoded }
    }

    suspend fun addCustomAiPrompt(title: String, instruction: String) {
        val cleanTitle = title.trim()
        val cleanInstruction = instruction.trim()
        if (cleanTitle.isBlank() || cleanInstruction.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_AI_PROMPTS].orEmpty()
                .split("\u001E")
                .mapNotNull { entry ->
                    val parts = entry.split("\u001F", limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
                }
                .filterNot { it.first.equals(cleanTitle, ignoreCase = true) }
                .toMutableList()
            current.add(0, cleanTitle to cleanInstruction)
            prefs[KEY_CUSTOM_AI_PROMPTS] = current.take(15).joinToString("\u001E") { (t, p) -> "$t\u001F$p" }
        }
    }

    suspend fun removeCustomAiPrompt(title: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_AI_PROMPTS].orEmpty()
                .split("\u001E")
                .mapNotNull { entry ->
                    val parts = entry.split("\u001F", limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
                }
                .filterNot { it.first == title }
            prefs[KEY_CUSTOM_AI_PROMPTS] = current.joinToString("\u001E") { (t, p) -> "$t\u001F$p" }
        }
    }

    val notepadDraft: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTEPAD_DRAFT] ?: ""
    }

    suspend fun setNotepadDraft(text: String) {
        context.dataStore.edit { it[KEY_NOTEPAD_DRAFT] = text.take(4000) }
    }

    val userTemplates: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_USER_TEMPLATES]
        if (raw == null) DEFAULT_USER_TEMPLATES
        else if (raw.isBlank()) emptyList()
        else raw.split("\u001E").filter { it.isNotBlank() }
    }

    suspend fun addUserTemplate(template: String) {
        val clean = template.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_USER_TEMPLATES]?.split("\u001E")?.filter { it.isNotBlank() }
                ?: DEFAULT_USER_TEMPLATES
            val updated = (listOf(clean) + existing.filterNot { it == clean }).take(20)
            prefs[KEY_USER_TEMPLATES] = updated.joinToString("\u001E")
        }
    }

    suspend fun removeUserTemplate(template: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_USER_TEMPLATES]?.split("\u001E")?.filter { it.isNotBlank() }
                ?: DEFAULT_USER_TEMPLATES
            val updated = existing.filterNot { it == template }
            prefs[KEY_USER_TEMPLATES] = updated.joinToString("\u001E")
        }
    }

    val downloadedTranslatorPacks: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] ?: emptySet()
    }

    suspend fun addDownloadedTranslatorPack(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] ?: emptySet()
            prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] = current + code
        }
    }

    suspend fun addDownloadedTranslatorPacks(codes: Set<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] ?: emptySet()
            prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] = current + codes
        }
    }

    suspend fun removeDownloadedTranslatorPack(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] ?: emptySet()
            prefs[KEY_DOWNLOADED_TRANSLATOR_PACKS] = current - code
        }
    }
}
