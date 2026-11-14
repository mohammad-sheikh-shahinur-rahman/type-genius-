package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.data.TypingStatsManager
import com.example.data.dictionary.DictionaryManager
import com.example.data.local.TextShortcutRepository
import com.example.data.local.TypeGeniusDatabase
import com.example.data.translator.OfflineTranslator
import com.example.data.translator.TranslationPackManager
import com.example.model.LanguageId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TypeGenius Keyboard", appName)
    }

    @Test
    fun `local translation engine loads bundled asset pack and caches in Room SQLite offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        OfflineTranslator.initialize(context)
        assertTrue("Bundled translation asset pack should load locally", OfflineTranslator.isPackLoadedFromAssets())

        val (phrases, lex) = OfflineTranslator.getModelStats()
        assertTrue("Expected >= 45 bundled phrases, was $phrases", phrases >= 45)
        assertTrue("Expected >= 60 bundled lexicon entries, was $lex", lex >= 60)

        // Translate phrase exclusively present in assets/typegenius_translation_pack.json
        val sylResult = OfflineTranslator.translateWithMemory(context, "Nice to meet you", "en", "syl")
        assertEquals("আফনার লগে দেখা অইয়া বালা লাগলো", sylResult.translatedText)
        assertTrue(sylResult.isOfflineExactOrPhrase)

        // Save a custom pinned phrase in local Room translation memory
        OfflineTranslator.saveCustomPhrase(
            context = context,
            sourceText = "typegenius rocks",
            translatedText = "টাইপজিনিয়াস দারুণ",
            sourceLang = "en",
            targetLang = "bn"
        )

        val memResult = OfflineTranslator.translateWithMemory(context, "typegenius rocks", "en", "bn")
        assertEquals("টাইপজিনিয়াস দারুণ", memResult.translatedText)
        assertEquals("Room Translation Memory", memResult.strategy)

        val dbCount = TypeGeniusDatabase.getDatabase(context).translationMemoryDao().countAll()
        assertTrue("Translation memory should persist locally in SQLite", dbCount >= 2)

        val banglishToBn = OfflineTranslator.translateWithMemory(context, "tumi kemon acho", "banglish", "bn")
        assertTrue(banglishToBn.translatedText.contains("তুমি"))

        val banglishToFa = OfflineTranslator.translateWithMemory(context, "dhonnobad", "banglish", "fa")
        assertTrue(banglishToFa.translatedText.isNotBlank())
    }

    @Test
    fun `room v3 personal text shortcuts seed, add, query, and delete offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = TextShortcutRepository(context)

        repo.ensureSeededDefaults()
        val gmMatches = repo.findMatchingExpansions("gm")
        assertTrue("Expected seeded shortcut for 'gm'", gmMatches.any { it.contains("শুভ সকাল") })

        repo.addOrUpdateShortcut("addr", "Zindabazar, Sylhet")
        val prefixMatches = repo.findMatchingExpansions("add")
        assertTrue("Expected prefix match for 'add'", prefixMatches.contains("Zindabazar, Sylhet"))

        val all = repo.allShortcuts.first()
        val added = all.find { it.trigger == "addr" }
        assertTrue("Expected persisted shortcut in Room DB", added != null)
        if (added != null) {
            repo.deleteShortcut(added)
        }
    }

    @Test
    fun `personal dictionary custom word add, remove, and clear offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dict = DictionaryManager(context)

        dict.addCustomWord("SylhetTech", "en")
        dict.addCustomWord("টাইপজিনিয়াস", "bn_probhat")

        val initialWords = dict.learnedWordsFlow.first()
        assertTrue(initialWords.any { it.word == "SylhetTech" })
        assertTrue(initialWords.any { it.word == "টাইপজিনিয়াস" })

        dict.removeLearnedWord("SylhetTech")
        val afterRemove = dict.learnedWordsFlow.first()
        assertTrue(afterRemove.none { it.word == "SylhetTech" })
        assertTrue(afterRemove.any { it.word == "টাইপজিনিয়াস" })

        dict.clearAllLearnedWords()
        val afterClear = dict.learnedWordsFlow.first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun `settings manager custom ai prompts, notepad draft, and templates persist offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsManager.getInstance(context)

        settings.addCustomAiPrompt("Meeting Tone", "Rewrite politely for a manager")
        val prompts = settings.customAiPrompts.first()
        assertTrue(prompts.any { it.first == "Meeting Tone" && it.second.contains("manager") })

        settings.removeCustomAiPrompt("Meeting Tone")
        val afterRemove = settings.customAiPrompts.first()
        assertTrue(afterRemove.none { it.first == "Meeting Tone" })

        settings.setNotepadDraft("Quick reminder note")
        assertEquals("Quick reminder note", settings.notepadDraft.first())

        settings.addUserTemplate("InshaAllah see you soon")
        assertTrue(settings.userTemplates.first().contains("InshaAllah see you soon"))
    }

    @Test
    fun `typing stats manager tracks words, script ratio, wpm and resets offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val statsManager = TypingStatsManager.getInstance(context)
        statsManager.resetStats()

        statsManager.recordCommittedWord("আমি", LanguageId.BANGLA_UNIBIJOY, 1200L)
        statsManager.recordCommittedWord("বাংলাদেশ", LanguageId.BANGLA_PHONETIC, 1400L)
        statsManager.recordCommittedWord("hello", LanguageId.ENGLISH, 1000L)

        val stats = statsManager.statsFlow.first()
        assertEquals(3, stats.totalWords)
        assertEquals(2, stats.banglaWords)
        assertEquals(1, stats.englishWords)
        assertTrue(stats.averageWpm > 0)
        assertTrue(stats.banglaPercent in 60..70)

        statsManager.resetStats()
        assertEquals(0, statsManager.statsFlow.first().totalWords)
    }

    @Test
    fun `downloadable offline language pack installs to disk, translates locally, and removes cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsManager.getInstance(context)

        val installRes = OfflineTranslator.downloadLanguagePack(context, "ta")
        assertEquals("ta", installRes.langCode)
        assertTrue("Expected installed Tamil pack entries", installRes.entryCount > 0)

        val packFile = File(File(context.filesDir, "translation_packs"), "ta.json")
        assertTrue("Expected persisted translation pack file on disk", packFile.exists())
        assertTrue(settings.downloadedTranslatorPacks.first().contains("ta"))
        assertTrue(TranslationPackManager.isPackReady(context, "ta", settings.downloadedTranslatorPacks.first()))

        val taResult = OfflineTranslator.translateWithMemory(context, "Thank you", "en", "ta")
        assertEquals("நன்றி", taResult.translatedText)
        assertTrue(taResult.isOfflineExactOrPhrase)

        OfflineTranslator.removeLanguagePack(context, "ta")
        assertFalse("Expected pack file removed after delete", packFile.exists())
        assertFalse(settings.downloadedTranslatorPacks.first().contains("ta"))
    }

    @Test
    fun `key sound engine singleton plays without crash and voice controller defaults to continuous 1-tap mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundEngine = com.example.service.KeySoundEngine.getInstance(context)
        com.example.model.SoundProfile.entries.forEach { profile ->
            soundEngine.play(profile, com.example.model.KeyType.CHARACTER)
            soundEngine.play(profile, com.example.model.KeyType.SPACEBAR)
            soundEngine.play(profile, com.example.model.KeyType.ENTER)
        }

        val voiceController = com.example.service.VoiceInputController(context)
        assertTrue("Continuous mode should be ON by default for 1-tap voice typing", voiceController.continuousMode)
        assertEquals(com.example.service.VoiceRoutingMode.AUTO, voiceController.routingMode)
        assertFalse("AUTO mode with network should not force offline", voiceController.preferOffline)
        voiceController.cycleRoutingMode()
        assertEquals(com.example.service.VoiceRoutingMode.OFFLINE_ONLY, voiceController.routingMode)
        assertTrue(voiceController.preferOffline)
        voiceController.cycleRoutingMode()
        assertEquals(com.example.service.VoiceRoutingMode.ONLINE_ONLY, voiceController.routingMode)
        voiceController.cycleRoutingMode()
        assertEquals(com.example.service.VoiceRoutingMode.AUTO, voiceController.routingMode)
        assertEquals("bn-BD", voiceController.resolveLanguageTag("bn"))
        assertEquals("bn-BD", voiceController.resolveLanguageTag("bn_phonetic"))
        assertEquals("en-US", voiceController.resolveLanguageTag("en"))
        voiceController.toggleBanglaEnglishVoice()
        assertEquals("EN", voiceController.activeVoiceBadgeLabel())
        voiceController.toggleBanglaEnglishVoice()
        assertEquals("বাংলা", voiceController.activeVoiceBadgeLabel())
        assertFalse(voiceController.isSessionActive)
        voiceController.destroy()
    }

    @Test
    fun `offline json backup export and import restores words, shortcuts, templates, notepad and prefs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dict = DictionaryManager(context)
        val shortcuts = TextShortcutRepository(context)
        val settings = SettingsManager.getInstance(context)

        dict.addCustomWord("বাংলাবিদ", "bn_probhat")
        shortcuts.addOrUpdateShortcut("bk", "ব্যাকআপ পরীক্ষা")
        settings.setNotepadDraft("অফলাইন নোট খসড়া")
        settings.addUserTemplate("শুভ জন্মদিন ভাই!")
        settings.addCustomAiPrompt("Bangla Clean", "শুদ্ধ বাংলায় লেখো")
        settings.setSoundVolume(0.55f)
        settings.setOtpAutoExpireMinutes(5)

        val json = com.example.data.BackupRestoreManager.exportAllToJson(context)
        assertTrue(json.contains("বাংলাবিদ"))
        assertTrue(json.contains("ব্যাকআপ পরীক্ষা"))
        assertTrue(json.contains("অফলাইন নোট খসড়া"))

        // Clear/change values before restore
        dict.clearAllLearnedWords()
        settings.setNotepadDraft("")
        settings.setSoundVolume(0.9f)
        settings.setOtpAutoExpireMinutes(3)

        val summary = com.example.data.BackupRestoreManager.importAllFromJson(context, json)
        assertTrue(summary.importedWords >= 1)
        assertTrue(summary.restoredNotepad)
        assertTrue(summary.restoredSettingsCount >= 5)

        assertTrue(dict.learnedWordsFlow.first().any { it.word == "বাংলাবিদ" })
        assertEquals("অফলাইন নোট খসড়া", settings.notepadDraft.first())
        assertEquals(5, settings.otpAutoExpireMinutes.first())
        assertEquals(0.55f, settings.soundVolume.first(), 0.02f)
    }

    @Test
    fun `real animated GIF89a encoder and custom bangla PNG sticker generator produce valid files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preset = com.example.ui.keyboard.AnimatedGifBuilder.presets.first()
        val gifFile = com.example.ui.keyboard.AnimatedGifBuilder.getOrCreatePresetGif(context, preset)
        assertTrue("Preset GIF should exist", gifFile.exists() && gifFile.length() > 500)
        val gifBytes = gifFile.readBytes()
        assertEquals("GIF89a", String(gifBytes.copyOfRange(0, 6)))
        assertEquals(0x3B.toByte(), gifBytes.last())

        val customGif = com.example.ui.keyboard.AnimatedGifBuilder.createCustomTextGif(context, "অস্থির!")
        assertTrue("Custom GIF should exist", customGif.exists() && customGif.length() > 500)

        val stickerFile = com.example.ui.keyboard.StickerImageGenerator.generateStickerPngFile(
            context = context,
            text = "অস্থির হইছে বস!",
            emoji = "🔥",
            gradient = com.example.ui.keyboard.StickerImageGenerator.gradientPresets.first(),
            fontStyle = com.example.ui.keyboard.StickerFontStyle.BOLD
        )
        assertTrue("Sticker PNG should exist", stickerFile.exists() && stickerFile.length() > 500)
        val pngHeader = stickerFile.readBytes().copyOfRange(0, 4)
        assertEquals(0x89.toByte(), pngHeader[0])
        assertEquals('P'.code.toByte(), pngHeader[1])
        assertEquals('N'.code.toByte(), pngHeader[2])
        assertEquals('G'.code.toByte(), pngHeader[3])
    }

    @Test
    fun `otp auto expire deletes expired unpinned items and per-app smart language resolves properly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = TypeGeniusDatabase.getDatabase(context)
        val settings = SettingsManager.getInstance(context)
        val now = System.currentTimeMillis()

        db.clipboardDao().insert(
            com.example.data.local.ClipboardItem(
                content = "OTP 998877",
                timestamp = now - 200_000L,
                isSensitive = true,
                expiresAt = now - 10_000L,
                isPinned = false
            )
        )
        db.clipboardDao().insert(
            com.example.data.local.ClipboardItem(
                content = "Pinned OTP 112233",
                timestamp = now - 200_000L,
                isSensitive = true,
                expiresAt = now - 10_000L,
                isPinned = true
            )
        )
        db.clipboardDao().deleteExpired(now)
        val remaining = db.clipboardDao().getAllItems().first()
        assertFalse(remaining.any { it.content == "OTP 998877" })
        assertTrue(remaining.any { it.content == "Pinned OTP 112233" })

        val enabled = LanguageId.entries
        assertEquals(
            LanguageId.BANGLA_PHONETIC,
            settings.resolveSmartLanguageForPackage("com.whatsapp", emptyMap(), enabled, LanguageId.ENGLISH)
        )
        assertEquals(
            LanguageId.ENGLISH,
            settings.resolveSmartLanguageForPackage("com.android.chrome", emptyMap(), enabled, LanguageId.BANGLA_PROBHAT)
        )

        settings.saveLanguageForPackage("com.whatsapp", LanguageId.BANGLA_UNIBIJOY.code)
        val savedMap = settings.perAppLanguageMap.first()
        assertEquals(
            LanguageId.BANGLA_UNIBIJOY,
            settings.resolveSmartLanguageForPackage("com.whatsapp", savedMap, enabled, LanguageId.ENGLISH)
        )
    }

    @Test
    fun `handwriting output generator creates valid PNGs and clipboard dao deduplicates and clears cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hwFile = com.example.ui.keyboard.HandwritingOutputGenerator.generateHandwritingPngFile(
            context = context,
            rawText = "আমার সোনার বাংলা • Hello TypeGenius",
            style = com.example.data.dictionary.FancyFontStyle.HANDWRITING_ATMA,
            customFontPath = "",
            paperTheme = com.example.ui.keyboard.HandwritingPaperTheme.NOTEBOOK
        )
        assertTrue("Handwriting PNG should exist", hwFile.exists() && hwFile.length() > 500)
        val header = hwFile.readBytes().copyOfRange(0, 4)
        assertEquals(0x89.toByte(), header[0])
        assertEquals('P'.code.toByte(), header[1])
        assertEquals('N'.code.toByte(), header[2])
        assertEquals('G'.code.toByte(), header[3])

        val db = TypeGeniusDatabase.getDatabase(context)
        val dao = db.clipboardDao()
        dao.clearAll()

        val t1 = System.currentTimeMillis()
        dao.insert(
            com.example.data.local.ClipboardItem(
                content = "https://typegenius.dev",
                timestamp = t1,
                isPinned = false
            )
        )
        val found = dao.findByContent("https://typegenius.dev")
        assertTrue(found != null)
        dao.update(found!!.copy(isPinned = true, expiresAt = null))

        val pinnedFound = dao.findByContent("https://typegenius.dev")
        assertEquals(true, pinnedFound?.isPinned)

        dao.clearUnpinned()
        assertTrue(dao.getAllItems().first().any { it.content == "https://typegenius.dev" })

        dao.clearAll()
        assertTrue(dao.getAllItems().first().isEmpty())
    }

    @Test
    fun `method xml declares all 33 subtypes including Avro, CrashDiagnosticLogger persists offline, and Portrait Letter PNG renders with signature`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify res/xml/method.xml has all 33 subtypes and Avro naming
        val subtypeLabels = mutableListOf<String>()
        val parser = context.resources.getXml(R.xml.method)
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "subtype") {
                val labelRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "label", 0)
                val rawLabel = if (labelRes != 0) {
                    context.getString(labelRes)
                } else {
                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "label").orEmpty()
                }
                subtypeLabels.add(rawLabel)
            }
            event = parser.next()
        }
        assertEquals(33, subtypeLabels.size)
        assertTrue("Expected Avro subtype label 'অভ্র'", subtypeLabels.contains("অভ্র"))

        // 2. Verify CrashDiagnosticLogger writes, reads, and clears cleanly offline
        com.example.data.CrashDiagnosticLogger.clearLogs(context)
        com.example.data.CrashDiagnosticLogger.logInfo(context, "IME", "Diagnostic test entry")
        com.example.data.CrashDiagnosticLogger.logError(context, "TestTag", "Simulated error", IllegalStateException("TestCause"))
        val logs = com.example.data.CrashDiagnosticLogger.readLogs(context)
        assertTrue(logs.any { it.contains("Diagnostic test entry") })
        assertTrue(logs.any { it.contains("Simulated error") && it.contains("IllegalStateException") })
        com.example.data.CrashDiagnosticLogger.clearLogs(context)
        assertTrue(com.example.data.CrashDiagnosticLogger.readLogs(context).isEmpty())

        // 3. Verify HandwritingOutputGenerator PORTRAIT_LETTER renders tall PNG with signature
        val letterFile = com.example.ui.keyboard.HandwritingOutputGenerator.generateHandwritingPngFile(
            context = context,
            rawText = "প্রিয় বন্ধু,\nকেমন আছো? হাতের লেখার নতুন চিঠি ফরম্যাট পরীক্ষা করা হচ্ছে।",
            style = com.example.data.dictionary.FancyFontStyle.HANDWRITING_GALADA,
            customFontPath = "",
            paperTheme = com.example.ui.keyboard.HandwritingPaperTheme.NOTEBOOK,
            format = com.example.ui.keyboard.HandwritingCardFormat.PORTRAIT_LETTER,
            signature = "শাহিনুর"
        )
        assertTrue("Portrait letter PNG should exist", letterFile.exists() && letterFile.length() > 500)
        val decoded = android.graphics.BitmapFactory.decodeFile(letterFile.absolutePath)
        assertTrue("Expected tall portrait letter height >= 1240, got ${decoded.height}", decoded.height >= 1240)
        decoded.recycle()
    }

    @Test
    fun `autofill profile vault and quick todo checklist persist cleanly and round-trip in offline json backup`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsManager.getInstance(context)

        settings.setAutofillProfileMap(
            mapOf(
                "name" to "Shahinur Islam",
                "phone" to "+8801700000000",
                "bkash" to "01700000000"
            )
        )
        settings.addQuickTodo("অ্যাপ আপডেট রিলিজ করা")
        settings.addQuickTodo("টিম মিটিং ৪টায়")

        val profile = settings.autofillProfile.first()
        assertEquals("Shahinur Islam", profile["name"])
        assertEquals("+8801700000000", profile["phone"])

        val todosBefore = settings.quickTodoItems.first()
        assertTrue(todosBefore.any { it.text == "অ্যাপ আপডেট রিলিজ করা" })
        val targetId = todosBefore.first { it.text == "অ্যাপ আপডেট রিলিজ করা" }.id
        settings.toggleQuickTodo(targetId)
        assertTrue(settings.quickTodoItems.first().first { it.id == targetId }.done)

        val backupJson = com.example.data.BackupRestoreManager.exportAllToJson(context)
        assertTrue(backupJson.contains("Shahinur Islam"))
        assertTrue(backupJson.contains("অ্যাপ আপডেট রিলিজ করা"))

        settings.setAutofillProfileMap(emptyMap())
        settings.setQuickTodoItems(emptyList())
        assertTrue(settings.autofillProfile.first().isEmpty())
        assertTrue(settings.quickTodoItems.first().isEmpty())

        com.example.data.BackupRestoreManager.importAllFromJson(context, backupJson)
        assertEquals("Shahinur Islam", settings.autofillProfile.first()["name"])
        assertTrue(settings.quickTodoItems.first().any { it.text == "অ্যাপ আপডেট রিলিজ করা" })
    }

    @Test
    fun `digital tasbih counter and dhikr selection persist in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsManager.getInstance(context)

        settings.resetTasbih()
        assertEquals(0, settings.tasbihCount.first())

        settings.incrementTasbih()
        settings.incrementTasbih()
        assertEquals(2, settings.tasbihCount.first())

        settings.cycleTasbihDhikr(com.example.data.IslamicToolkit.tasbihPresets.size)
        assertEquals(1, settings.tasbihDhikrIndex.first())
        assertEquals(0, settings.tasbihCount.first())
    }
}
