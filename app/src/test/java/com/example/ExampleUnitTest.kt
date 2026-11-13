package com.example

import com.example.data.dictionary.OfflineSpellChecker
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.data.translator.OfflineTranslator
import com.example.model.KeyboardTheme
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import com.example.ui.keyboard.KeyboardLayouts
import com.example.ui.keyboard.KeyboardThemeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAll32LanguagePackages_presentWithValidLayouts() {
        assertEquals(32, LanguageId.entries.size)
        assertTrue(LanguageId.entries.contains(LanguageId.SYLHETI))

        LanguageId.entries.forEach { lang ->
            val unshifted = KeyboardLayouts.getAlphabetRows(lang, isShifted = false)
            val shifted = KeyboardLayouts.getAlphabetRows(lang, isShifted = true)
            val numbers = KeyboardLayouts.getNumberRow(lang)
            val symbols = KeyboardLayouts.getSymbols1(lang)

            assertEquals("Expected 3 alphabet rows for ${lang.name}", 3, unshifted.size)
            assertEquals("Expected 3 shifted rows for ${lang.name}", 3, shifted.size)
            assertEquals("Expected 10 numbers for ${lang.name}", 10, numbers.size)
            assertTrue("Expected non-empty symbols for ${lang.name}", symbols.isNotEmpty())
        }
    }

    @Test
    fun testOfflineTranslator_phrasesWordsAndBanglish() {
        val bnRes = OfflineTranslator.translate("Thank you", "en", "bn")
        assertEquals("ধন্যবাদ", bnRes.translatedText)
        assertTrue(bnRes.isOfflineExactOrPhrase)

        val hiRes = OfflineTranslator.translate("Good morning", "en", "hi")
        assertEquals("सुप्रभात", hiRes.translatedText)

        val arRes = OfflineTranslator.translate("Hello", "en", "ar")
        assertEquals("مرحبا", arRes.translatedText)

        val banglishRes = OfflineTranslator.translate("ami", "bn", "en")
        assertEquals("i", banglishRes.translatedText.lowercase())

        val wordByWord = OfflineTranslator.translate("my good friend", "en", "bn")
        assertEquals("আমার ভালো বন্ধু", wordByWord.translatedText)

        val sylRes = OfflineTranslator.translate("Thank you", "en", "syl")
        assertEquals("শুকরিয়া", sylRes.translatedText)

        val materialYouDark = KeyboardThemeData.getColors(KeyboardTheme.MATERIAL_YOU, context = null, isSystemDark = true)
        val materialYouLight = KeyboardThemeData.getColors(KeyboardTheme.MATERIAL_YOU, context = null, isSystemDark = false)
        assertNotNull(materialYouDark.enterKeyBackground)
        assertTrue(materialYouDark.background != materialYouLight.background)
    }

    @Test
    fun testOfflineSpellChecker_correctsTranspositionsAndTypos() {
        val vocab = listOf("keyboard", "hello", "thanks", "friend", "translate")
        val typo1 = OfflineSpellChecker.correctOrSuggest("keybaord", vocab)
        assertEquals("keyboard", typo1.firstOrNull())

        val typo2 = OfflineSpellChecker.correctOrSuggest("helo", vocab)
        assertEquals("hello", typo2.firstOrNull())
    }

    @Test
    fun testBengaliPhoneticParser_commonWords() {
        val amiResults = BengaliPhoneticParser.parsePhonetic("ami")
        assertTrue("ami should produce 'আমি'", amiResults.contains("আমি"))

        val tumiResults = BengaliPhoneticParser.parsePhonetic("tumi")
        assertTrue("tumi should produce 'তুমি'", tumiResults.contains("তুমি"))

        val banglaResults = BengaliPhoneticParser.parsePhonetic("bangla")
        assertTrue("bangla should produce 'বাংলা'", banglaResults.contains("বাংলা"))

        val dhonnobadResults = BengaliPhoneticParser.parsePhonetic("dhonnobad")
        assertTrue("dhonnobad should produce 'ধন্যবাদ'", dhonnobadResults.contains("ধন্যবাদ"))
    }

    @Test
    fun testBengaliProbhatLayout_primaryAndSecondaryLongPress() {
        val unshiftedRows = KeyboardLayouts.getAlphabetRows(LanguageId.BANGLA_PROBHAT, isShifted = false)
        assertEquals(3, unshiftedRows.size)
        assertEquals(12, unshiftedRows[0].size)
        assertEquals(9, unshiftedRows[1].size)
        assertEquals(11, unshiftedRows[2].size)

        val aaKey = unshiftedRows[2].find { it.label == "আ" }
        assertEquals("ঋ", aaKey?.secondaryLabel)

        assertEquals("দ", unshiftedRows[0][0].label)
        assertEquals("ধ", unshiftedRows[0][0].secondaryLabel)

        val shiftedRows = KeyboardLayouts.getAlphabetRows(LanguageId.BANGLA_PROBHAT, isShifted = true)
        assertEquals("ধ", shiftedRows[0][0].label)
        assertEquals("দ", shiftedRows[0][0].secondaryLabel)
        val riKey = shiftedRows[2].find { it.label == "ঋ" }
        assertEquals("আ", riKey?.secondaryLabel)
    }

    @Test
    fun testLanguageId_cyclingAndRtlFlags() {
        val languages = listOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_PHONETIC,
            LanguageId.ENGLISH,
            LanguageId.HINDI,
            LanguageId.ARABIC
        )

        var currentIndex = languages.indexOf(LanguageId.BANGLA_PROBHAT)
        var nextIndex = (currentIndex + 1) % languages.size
        assertEquals(LanguageId.BANGLA_PHONETIC, languages[nextIndex])

        currentIndex = languages.indexOf(LanguageId.ARABIC)
        nextIndex = (currentIndex + 1) % languages.size
        assertEquals(LanguageId.BANGLA_PROBHAT, languages[nextIndex])

        currentIndex = languages.indexOf(LanguageId.BANGLA_PROBHAT)
        val prevIndex = (currentIndex - 1 + languages.size) % languages.size
        assertEquals(LanguageId.ARABIC, languages[prevIndex])

        assertTrue(LanguageId.ARABIC.isRtl)
        assertTrue(LanguageId.URDU.isRtl)
        assertTrue(LanguageId.PERSIAN.isRtl)
        assertTrue(LanguageId.HEBREW.isRtl)
        assertFalse(LanguageId.ENGLISH.isRtl)
        assertFalse(LanguageId.BANGLA_PROBHAT.isRtl)
        assertEquals("প্রভাত", LanguageId.BANGLA_PROBHAT.spacebarLabel)
        assertEquals("বাংলা", LanguageId.BANGLA_PHONETIC.spacebarLabel)
        assertEquals("EN", LanguageId.ENGLISH.spacebarLabel)
    }

    @Test
    fun testSwipeSensitivityThresholds() {
        assertTrue(SwipeSensitivity.HIGH.thresholdDp < SwipeSensitivity.MEDIUM.thresholdDp)
        assertTrue(SwipeSensitivity.MEDIUM.thresholdDp < SwipeSensitivity.LOW.thresholdDp)
        assertEquals(30f, SwipeSensitivity.HIGH.thresholdDp)
        assertEquals(50f, SwipeSensitivity.MEDIUM.thresholdDp)
        assertEquals(80f, SwipeSensitivity.LOW.thresholdDp)
    }

    @Test
    fun testLocalTranslationEngine_multiSentenceNgramGrammarStemmingAndAutoDetect() {
        // Multi-sentence with localized punctuation (।)
        val multi = OfflineTranslator.translate("Hello! How are you? Thank you.", "en", "bn")
        assertEquals("হ্যালো! আপনি কেমন আছেন? ধন্যবাদ।", multi.translatedText)
        assertTrue(multi.confidence >= 0.99f)

        // Greedy multi-word N-gram inside sentence
        val ngram = OfflineTranslator.translate("hello friend, how are you today?", "en", "bn")
        assertTrue(ngram.translatedText.contains("হ্যালো"))
        assertTrue(ngram.translatedText.contains("বন্ধু"))
        assertTrue(ngram.translatedText.contains("আপনি কেমন আছেন"))
        assertTrue(ngram.translatedText.contains("আজ"))

        // Morphological stemming (plural 'friends' -> 'friend')
        val stemmed = OfflineTranslator.translate("my good friends", "en", "bn")
        assertEquals("আমার ভালো বন্ধু", stemmed.translatedText)

        // SVO -> SOV grammar transfer: [I] [love] [food] -> [আমি] [খাবার] [ভালোবাসা]
        val svoToSov = OfflineTranslator.translate("i love food", "en", "bn")
        assertEquals("আমি খাবার ভালোবাসা", svoToSov.translatedText)
        assertTrue(svoToSov.strategy.contains("Grammar"))

        // SOV -> SVO grammar transfer: [আমি] [খাবার] [ভালোবাসা] -> [i] [love] [food]
        val sovToSvo = OfflineTranslator.translate("আমি খাবার ভালোবাসা", "bn", "en")
        assertEquals("i love food", sovToSvo.translatedText)

        // Sylheti romanized input normalization + auto-detection
        val sylDetected = OfflineTranslator.detectSourceLanguage("afne bala achoinni")
        assertEquals("syl", sylDetected?.code)

        val bnDetected = OfflineTranslator.detectSourceLanguage("ami bangla bhalobashi")
        assertEquals("bn", bnDetected?.code)

        val arDetected = OfflineTranslator.detectSourceLanguage("مرحبا")
        assertEquals("ar", arDetected?.code)

        val zhDetected = OfflineTranslator.detectSourceLanguage("你好")
        assertEquals("zh", zhDetected?.code)
    }

    @Test
    fun testBanglishTranslator_bidirectionalAndMultilingual() {
        // Banglish -> Bengali
        val toBengali = OfflineTranslator.translate("ami bangladesh valobashi", "banglish", "bn")
        assertTrue(toBengali.translatedText.contains("আমি"))
        assertTrue(toBengali.translatedText.contains("বাংলাদেশ"))
        assertTrue(toBengali.isOfflineExactOrPhrase)

        // Banglish -> English
        val toEnglish = OfflineTranslator.translate("ami tomake valobashi", "banglish", "en")
        assertEquals("i love you", toEnglish.translatedText.lowercase())

        // Banglish -> Persian
        val toPersian = OfflineTranslator.translate("ami tomake valobashi", "banglish", "fa")
        assertEquals("دوستت دارم", toPersian.translatedText)

        // Bengali -> Banglish (Roman Bangla)
        val bnToBanglish = OfflineTranslator.translate("আমি তোমাকে ভালোবাসি", "bn", "banglish")
        assertEquals("ami tomake valobashi", bnToBanglish.translatedText.lowercase())

        // English -> Banglish
        val enToBanglish = OfflineTranslator.translate("thank you", "en", "banglish")
        assertEquals("dhonnobad", enToBanglish.translatedText.lowercase())

        // Bengali -> Chinese Simplified
        val bnToZh = OfflineTranslator.translate("হ্যালো", "bn", "zh")
        assertEquals("你好", bnToZh.translatedText)
    }
}
