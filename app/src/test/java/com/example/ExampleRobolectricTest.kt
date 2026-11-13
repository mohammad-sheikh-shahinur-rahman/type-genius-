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
        assertFalse(voiceController.isSessionActive)
        voiceController.destroy()
    }
}
