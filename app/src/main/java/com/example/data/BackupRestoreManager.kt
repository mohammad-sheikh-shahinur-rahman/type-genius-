package com.example.data

import android.content.Context
import com.example.data.dictionary.FancyFontStyle
import com.example.data.local.LearnedWord
import com.example.data.local.TextShortcut
import com.example.data.local.TypeGeniusDatabase
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
import com.example.model.OneHandedMode
import com.example.model.SoundProfile
import com.example.model.SwipeSensitivity
import com.example.model.VibrationStrength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupStats(
    val learnedWordsCount: Int,
    val shortcutsCount: Int,
    val templatesCount: Int,
    val customPromptsCount: Int,
    val hasNotepadDraft: Boolean
)

data class RestoreSummary(
    val importedWords: Int,
    val importedShortcuts: Int,
    val importedTemplates: Int,
    val importedCustomPrompts: Int,
    val restoredNotepad: Boolean,
    val restoredSettingsCount: Int
)

object BackupRestoreManager {

    private const val BACKUP_FORMAT_VERSION = 1

    suspend fun getLocalStats(context: Context): BackupStats = withContext(Dispatchers.IO) {
        val db = TypeGeniusDatabase.getDatabase(context)
        val settings = SettingsManager.getInstance(context)
        val words = db.wordDao().getAllLearnedWordsList()
        val shortcuts = db.shortcutDao().getAllList()
        val templates = settings.userTemplates.first()
        val prompts = settings.customAiPrompts.first()
        val draft = settings.notepadDraft.first()
        BackupStats(
            learnedWordsCount = words.size,
            shortcutsCount = shortcuts.size,
            templatesCount = templates.size,
            customPromptsCount = prompts.size,
            hasNotepadDraft = draft.isNotBlank()
        )
    }

    suspend fun exportAllToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = TypeGeniusDatabase.getDatabase(context)
        val settings = SettingsManager.getInstance(context)

        val learnedWords = db.wordDao().getAllLearnedWordsList()
        val shortcuts = db.shortcutDao().getAllList()
        val templates = settings.userTemplates.first()
        val notepadDraft = settings.notepadDraft.first()
        val customPrompts = settings.customAiPrompts.first()
        val enabledLangs = settings.enabledLanguages.first()
        val toolbarTools = settings.toolbarTools.first()

        val root = JSONObject()
        root.put("app", "TypeGenius Keyboard")
        root.put("formatVersion", BACKUP_FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Learned Words
        val wordsArray = JSONArray()
        learnedWords.forEach { w ->
            wordsArray.put(JSONObject().apply {
                put("word", w.word)
                put("language", w.language)
                put("frequency", w.frequency)
                put("lastUsed", w.lastUsed)
            })
        }
        root.put("learnedWords", wordsArray)

        // 2. Text Shortcuts
        val shortcutsArray = JSONArray()
        shortcuts.forEach { s ->
            shortcutsArray.put(JSONObject().apply {
                put("trigger", s.trigger)
                put("expansion", s.expansion)
                put("language", s.language)
            })
        }
        root.put("shortcuts", shortcutsArray)

        // 3. Templates
        val templatesArray = JSONArray()
        templates.forEach { templatesArray.put(it) }
        root.put("userTemplates", templatesArray)

        // 4. Notepad Draft
        root.put("notepadDraft", notepadDraft)

        // 5. Custom AI Prompts
        val promptsArray = JSONArray()
        customPrompts.forEach { (title, instruction) ->
            promptsArray.put(JSONObject().apply {
                put("title", title)
                put("instruction", instruction)
            })
        }
        root.put("customAiPrompts", promptsArray)

        // 6. Preferences (offline-only, excludes secrets like Gemini API key for safety)
        val prefsObj = JSONObject().apply {
            put("activeLanguageCode", settings.activeLanguageCode.first())
            put("enabledLanguages", JSONArray(enabledLangs.toList()))
            put("keyboardTheme", settings.keyboardTheme.first().id)
            put("soundOnKeyPress", settings.soundOnKeyPress.first())
            put("soundProfile", settings.soundProfile.first().id)
            put("soundVolume", settings.soundVolume.first().toDouble())
            put("hapticEnabled", settings.isHapticEnabled.first())
            put("vibrationStrength", settings.vibrationStrength.first().id)
            put("numberRowEnabled", settings.numberRowEnabled.first())
            put("autoCapitalize", settings.autoCapitalize.first())
            put("spellCheckEnabled", settings.spellCheckEnabled.first())
            put("glideTypingEnabled", settings.isGlideTypingEnabled.first())
            put("backspaceSwipeEnabled", settings.isBackspaceSwipeEnabled.first())
            put("spacebarSwipeEnabled", settings.isSpacebarSwipeEnabled.first())
            put("swipeSensitivity", settings.swipeSensitivity.first().name)
            put("oneHandedMode", settings.oneHandedMode.first().id)
            put("heightPreset", settings.keyboardHeightPreset.first().id)
            put("heightPortrait", settings.keyboardHeightPortrait.first())
            put("heightLandscape", settings.keyboardHeightLandscape.first())
            put("floatingMode", settings.floatingMode.first())
            put("activeFancyFont", settings.activeFancyFontStyle.first().id)
            put("keyboardFontId", settings.keyboardFontId.first())
            put("otpAutoExpireMinutes", settings.otpAutoExpireMinutes.first())
            put("perAppSmartLanguageEnabled", settings.perAppSmartLanguageEnabled.first())
            put("perAppLanguageMapJson", settings.perAppLanguageMapJson.first())
            put("toolbarTools", JSONArray(toolbarTools.map { it.id }))
        }
        root.put("preferences", prefsObj)

        root.toString(2)
    }

    suspend fun importAllFromJson(context: Context, jsonContent: String): RestoreSummary = withContext(Dispatchers.IO) {
        val clean = jsonContent.trim()
        require(clean.isNotEmpty()) { "ফাইলটি খালি!" }

        val root = JSONObject(clean)
        val db = TypeGeniusDatabase.getDatabase(context)
        val settings = SettingsManager.getInstance(context)

        var wordsImported = 0
        var shortcutsImported = 0
        var templatesImported = 0
        var promptsImported = 0
        var restoredNotepad = false
        var settingsCount = 0

        // 1. Learned Words (merge/upsert)
        root.optJSONArray("learnedWords")?.let { arr ->
            val list = mutableListOf<LearnedWord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val word = obj.optString("word").trim()
                if (word.isNotEmpty()) {
                    list.add(
                        LearnedWord(
                            word = word,
                            language = obj.optString("language", "bn_probhat"),
                            frequency = obj.optInt("frequency", 1).coerceAtLeast(1),
                            lastUsed = obj.optLong("lastUsed", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (list.isNotEmpty()) {
                db.wordDao().insertAll(list)
                wordsImported = list.size
            }
        }

        // 2. Shortcuts (merge without duplicating exact trigger+expansion)
        root.optJSONArray("shortcuts")?.let { arr ->
            val existing = db.shortcutDao().getAllList()
                .map { it.trigger.lowercase() to it.expansion }
                .toHashSet()
            val toAdd = mutableListOf<TextShortcut>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val trigger = obj.optString("trigger").trim()
                val expansion = obj.optString("expansion").trim()
                val lang = obj.optString("language", "all")
                if (trigger.isNotEmpty() && expansion.isNotEmpty()) {
                    if (existing.add(trigger.lowercase() to expansion)) {
                        toAdd.add(TextShortcut(trigger = trigger, expansion = expansion, language = lang))
                    }
                }
            }
            if (toAdd.isNotEmpty()) {
                db.shortcutDao().insertAll(toAdd)
                shortcutsImported = toAdd.size
            }
        }

        // 3. Templates
        root.optJSONArray("userTemplates")?.let { arr ->
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val t = arr.optString(i).trim()
                if (t.isNotEmpty()) list.add(t)
            }
            if (list.isNotEmpty()) {
                val merged = (list + settings.userTemplates.first()).distinct().take(25)
                settings.setUserTemplates(merged)
                templatesImported = list.size
            }
        }

        // 4. Notepad Draft
        if (root.has("notepadDraft")) {
            val draft = root.optString("notepadDraft", "")
            if (draft.isNotBlank()) {
                settings.setNotepadDraft(draft)
                restoredNotepad = true
            }
        }

        // 5. Custom AI Prompts
        root.optJSONArray("customAiPrompts")?.let { arr ->
            val importedPrompts = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                val instruction = obj.optString("instruction").trim()
                if (title.isNotEmpty() && instruction.isNotEmpty()) {
                    importedPrompts.add(title to instruction)
                }
            }
            if (importedPrompts.isNotEmpty()) {
                val existing = settings.customAiPrompts.first()
                val merged = (importedPrompts + existing)
                    .distinctBy { it.first.lowercase() }
                    .take(15)
                settings.setCustomAiPrompts(merged)
                promptsImported = importedPrompts.size
            }
        }

        // 6. Preferences
        root.optJSONObject("preferences")?.let { prefs ->
            if (prefs.has("activeLanguageCode")) {
                settings.setActiveLanguageCode(prefs.optString("activeLanguageCode"))
                settingsCount++
            }
            prefs.optJSONArray("enabledLanguages")?.let { arr ->
                val set = buildSet {
                    for (i in 0 until arr.length()) {
                        val c = arr.optString(i).trim()
                        if (c.isNotEmpty()) add(c)
                    }
                }
                if (set.isNotEmpty()) {
                    settings.setEnabledLanguages(set)
                    settingsCount++
                }
            }
            if (prefs.has("keyboardTheme")) {
                settings.setKeyboardTheme(KeyboardTheme.fromId(prefs.optString("keyboardTheme")))
                settingsCount++
            }
            if (prefs.has("soundOnKeyPress")) {
                settings.setSoundOnKeyPress(prefs.optBoolean("soundOnKeyPress"))
                settingsCount++
            }
            if (prefs.has("soundProfile")) {
                settings.setSoundProfile(SoundProfile.fromId(prefs.optString("soundProfile")))
                settingsCount++
            }
            if (prefs.has("soundVolume")) {
                settings.setSoundVolume(prefs.optDouble("soundVolume", 0.75).toFloat())
                settingsCount++
            }
            if (prefs.has("hapticEnabled")) {
                settings.setHapticEnabled(prefs.optBoolean("hapticEnabled"))
                settingsCount++
            }
            if (prefs.has("vibrationStrength")) {
                settings.setVibrationStrength(VibrationStrength.fromId(prefs.optString("vibrationStrength")))
                settingsCount++
            }
            if (prefs.has("numberRowEnabled")) {
                settings.setNumberRowEnabled(prefs.optBoolean("numberRowEnabled"))
                settingsCount++
            }
            if (prefs.has("autoCapitalize")) {
                settings.setAutoCapitalize(prefs.optBoolean("autoCapitalize"))
                settingsCount++
            }
            if (prefs.has("spellCheckEnabled")) {
                settings.setSpellCheckEnabled(prefs.optBoolean("spellCheckEnabled"))
                settingsCount++
            }
            if (prefs.has("glideTypingEnabled")) {
                settings.setGlideTypingEnabled(prefs.optBoolean("glideTypingEnabled"))
                settingsCount++
            }
            if (prefs.has("backspaceSwipeEnabled")) {
                settings.setBackspaceSwipeEnabled(prefs.optBoolean("backspaceSwipeEnabled"))
                settingsCount++
            }
            if (prefs.has("spacebarSwipeEnabled")) {
                settings.setSpacebarSwipeEnabled(prefs.optBoolean("spacebarSwipeEnabled"))
                settingsCount++
            }
            if (prefs.has("swipeSensitivity")) {
                settings.setSwipeSensitivity(SwipeSensitivity.fromName(prefs.optString("swipeSensitivity")))
                settingsCount++
            }
            if (prefs.has("oneHandedMode")) {
                settings.setOneHandedMode(OneHandedMode.fromId(prefs.optString("oneHandedMode")))
                settingsCount++
            }
            if (prefs.has("heightPreset")) {
                settings.setKeyboardHeightPreset(KeyboardHeightPreset.fromId(prefs.optString("heightPreset")))
                settingsCount++
            }
            if (prefs.has("heightPortrait")) {
                settings.setKeyboardHeightPortrait(prefs.optInt("heightPortrait", 100))
                settingsCount++
            }
            if (prefs.has("heightLandscape")) {
                settings.setKeyboardHeightLandscape(prefs.optInt("heightLandscape", 100))
                settingsCount++
            }
            if (prefs.has("floatingMode")) {
                settings.setFloatingMode(prefs.optBoolean("floatingMode"))
                settingsCount++
            }
            if (prefs.has("activeFancyFont")) {
                settings.setActiveFancyFontStyle(FancyFontStyle.fromId(prefs.optString("activeFancyFont")))
                settingsCount++
            }
            if (prefs.has("keyboardFontId")) {
                settings.setKeyboardFontId(prefs.optString("keyboardFontId"))
                settingsCount++
            }
            if (prefs.has("otpAutoExpireMinutes")) {
                settings.setOtpAutoExpireMinutes(prefs.optInt("otpAutoExpireMinutes", 3))
                settingsCount++
            }
            if (prefs.has("perAppSmartLanguageEnabled")) {
                settings.setPerAppSmartLanguageEnabled(prefs.optBoolean("perAppSmartLanguageEnabled", true))
                settingsCount++
            }
            if (prefs.has("perAppLanguageMapJson")) {
                settings.setPerAppLanguageMapJson(prefs.optString("perAppLanguageMapJson", ""))
                settingsCount++
            }
            prefs.optJSONArray("toolbarTools")?.let { arr ->
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        KeyboardToolId.fromId(arr.optString(i))?.let { add(it) }
                    }
                }
                if (list.isNotEmpty()) {
                    settings.setToolbarTools(list)
                    settingsCount++
                }
            }
        }

        RestoreSummary(
            importedWords = wordsImported,
            importedShortcuts = shortcutsImported,
            importedTemplates = templatesImported,
            importedCustomPrompts = promptsImported,
            restoredNotepad = restoredNotepad,
            restoredSettingsCount = settingsCount
        )
    }
}
