package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TextShortcutRepository
import com.example.data.local.TypeGeniusDatabase
import com.example.data.translator.OfflineTranslator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
