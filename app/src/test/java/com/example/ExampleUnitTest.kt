package com.example

import com.example.data.KeyboardFontPreset
import com.example.data.ai.GemTypeAction
import com.example.data.ai.GemTypeAiEngine
import com.example.data.dictionary.FancyFontStyle
import com.example.data.dictionary.OfflineGlideEngine
import com.example.data.dictionary.OfflineGrammarEngine
import com.example.data.dictionary.OfflineMathEvaluator
import com.example.data.dictionary.OfflineSpellChecker
import com.example.data.dictionary.UnicodeTextStyler
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.data.translator.OfflineTranslator
import com.example.model.KeyboardTheme
import com.example.model.KeyboardToolId
import com.example.model.LanguageId
import com.example.model.SwipeSensitivity
import com.example.ui.keyboard.KeyboardLayouts
import com.example.ui.keyboard.KeyboardThemeData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testBengaliLayouts_containAllVowelsKarsConsonantsAndPholas() {
        val requiredCharsAndSigns = listOf(
            // Vowels
            "অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ",
            // Kars
            "া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ",
            // Consonants & modifiers
            "ক", "খ", "গ", "ঘ", "ঙ",
            "চ", "ছ", "জ", "ঝ", "ঞ",
            "ট", "ঠ", "ড", "ঢ", "ণ",
            "ত", "থ", "দ", "ধ", "ন",
            "প", "ফ", "ব", "ভ", "ম",
            "য", "র", "ল",
            "শ", "ষ", "স", "হ",
            "ড়", "ঢ়", "য়", "ৎ", "ং", "ঃ", "ঁ", "্",
            // Pholas & Reph
            "\u09CD\u09B0", // র-ফলা
            "\u09CD\u09AF", // য-ফলা
            "\u09B0\u09CD", // রেফ
            "\u09CD\u09AC", // ব-ফলা
            "\u09CD\u09AE", // ম-ফলা
            "\u09CD\u09A8", // ন-ফলা
            "\u09CD\u09B2"  // ল-ফলা
        )

        listOf(LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_NATIONAL).forEach { lang ->
            val allTokens = (KeyboardLayouts.getAlphabetRows(lang, false) + KeyboardLayouts.getAlphabetRows(lang, true))
                .flatten()
                .flatMap { key ->
                    buildList {
                        add(key.label)
                        key.secondaryLabel?.let { add(it) }
                        addAll(key.extraVariants)
                    }
                }
                .toSet()

            requiredCharsAndSigns.forEach { sign ->
                assertTrue("Missing '$sign' in $lang layout", allTokens.contains(sign))
            }
        }

        assertTrue(com.example.model.KeyboardHeightPreset.NORMAL.rowHeightDp >= 56)
        assertTrue(BengaliPhoneticParser.parsePhonetic("krishi").contains("কৃষি"))
        assertTrue(BengaliPhoneticParser.parsePhonetic("prothom").contains("প্রথম"))
        assertTrue(BengaliPhoneticParser.parsePhonetic("NG").contains("ঞ"))
        assertTrue(BengaliPhoneticParser.parsePhonetic("prri").firstOrNull() == "পৃ")
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

    @Test
    fun testOfflineMathEvaluator_englishAndBengaliDigits() {
        val add = OfflineMathEvaluator.evaluateIfTriggered("12+8=")
        assertEquals("20", add?.resultString)
        assertEquals("12+8=20", add?.equationString)

        val parens = OfflineMathEvaluator.evaluateIfTriggered("(25*4)-10=")
        assertEquals("90", parens?.resultString)

        val decimal = OfflineMathEvaluator.evaluateIfTriggered("10/4=")
        assertEquals("2.5", decimal?.resultString)

        val bnAdd = OfflineMathEvaluator.evaluateIfTriggered("১২+৮=")
        assertEquals("২০", bnAdd?.resultString)

        assertNull(OfflineMathEvaluator.evaluateIfTriggered("hello="))
        assertNull(OfflineMathEvaluator.evaluateIfTriggered("12+8"))
    }

    @Test
    fun testUnicodeTextStyler_transformsLatinAndDecorators() {
        val bold = UnicodeTextStyler.styleText("Hi 1", FancyFontStyle.BOLD_SANS)
        assertEquals("𝗛𝗶 𝟭", bold)

        val cursive = UnicodeTextStyler.styleText("Abc", FancyFontStyle.SCRIPT_BOLD)
        assertEquals("𝓐𝓫𝓬", cursive)

        val gothic = UnicodeTextStyler.styleText("Type", FancyFontStyle.FRAKTUR_BOLD)
        assertEquals("𝕿𝖞𝖕𝖊", gothic)

        val squared = UnicodeTextStyler.styleText("hi", FancyFontStyle.SQUARED)
        assertEquals("🄷🄸", squared)

        val circled = UnicodeTextStyler.styleText("abc", FancyFontStyle.CIRCLED)
        assertEquals("ⓐⓑⓒ", circled)

        val banglaDecor = UnicodeTextStyler.styleText("বাংলা", FancyFontStyle.DECORATED_STAR)
        assertEquals("✦ বাংলা ✦", banglaDecor)

        val royalDecor = UnicodeTextStyler.styleText("সিলেট", FancyFontStyle.DECORATED_ROYAL)
        assertEquals("꧁ সিলেট ꧂", royalDecor)

        // Ensure combining underline does not insert mark before Bengali vowel sign া
        val banglaUnderline = UnicodeTextStyler.styleText("বা", FancyFontStyle.UNDERLINE)
        assertEquals("বা\u0332", banglaUnderline)

        // Ensure unstyleToPlain cleanly converts styled text back before restyling
        assertEquals("Abc", UnicodeTextStyler.unstyleToPlain(cursive))
        assertEquals("Type", UnicodeTextStyler.unstyleToPlain(gothic))
        assertEquals("বা", UnicodeTextStyler.unstyleToPlain(banglaUnderline))
        assertEquals("বাংলা", UnicodeTextStyler.unstyleToPlain(banglaDecor))
        assertEquals("Abc", UnicodeTextStyler.styleText(cursive, FancyFontStyle.NORMAL))
    }

    @Test
    fun testOfflineGlideEngine_resolvesEnglishAndBanglishPhonetic() {
        val centers = mapOf(
            "q" to (50f to 50f), "w" to (150f to 50f), "e" to (250f to 50f), "r" to (350f to 50f),
            "t" to (450f to 50f), "y" to (550f to 50f), "u" to (650f to 50f), "i" to (750f to 50f),
            "o" to (850f to 50f), "p" to (950f to 50f),
            "a" to (100f to 150f), "s" to (200f to 150f), "d" to (300f to 150f), "f" to (400f to 150f),
            "g" to (500f to 150f), "h" to (600f to 150f), "j" to (700f to 150f), "k" to (800f to 150f),
            "l" to (900f to 150f),
            "z" to (150f to 250f), "x" to (250f to 250f), "c" to (350f to 250f), "v" to (450f to 250f),
            "b" to (550f to 250f), "n" to (650f to 250f), "m" to (750f to 250f)
        )
        val englishVocab = listOf("hello", "world", "friend", "happy", "thanks")
        val helloTrail = listOf(
            600f to 150f, // h
            250f to 50f,  // e
            900f to 150f, // l
            880f to 140f, // l
            850f to 50f   // o
        )
        val enResult = OfflineGlideEngine.resolveGlideCandidates(
            trail = helloTrail,
            keyCenters = centers,
            vocabulary = englishVocab,
            languageId = LanguageId.ENGLISH
        )
        assertTrue("Expected 'hello' in glide results: $enResult", enResult.contains("hello"))

        val amiTrail = listOf(
            100f to 150f, // a
            750f to 250f, // m
            750f to 50f   // i
        )
        val bnResult = OfflineGlideEngine.resolveGlideCandidates(
            trail = amiTrail,
            keyCenters = centers,
            vocabulary = englishVocab,
            languageId = LanguageId.BANGLA_PHONETIC
        )
        assertTrue("Expected 'আমি' in Banglish glide results: $bnResult", bnResult.contains("আমি"))
    }

    @Test
    fun testOfflineGrammarEngine_englishAndBengaliCorrections() {
        // English subject-verb agreement
        val sv1 = OfflineGrammarEngine.analyzeContext("i is happy", LanguageId.ENGLISH)
        // Trailing "i is" check
        val sv2 = OfflineGrammarEngine.analyzeContext("i is", LanguageId.ENGLISH)
        assertTrue(sv2.any { it.correctedPhrase == "I am" })
        assertEquals("she doesn't", OfflineGrammarEngine.analyzeContext("she don't", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)
        assertEquals("they were", OfflineGrammarEngine.analyzeContext("they was", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)

        // Article agreement (a vs an)
        assertEquals("an apple", OfflineGrammarEngine.analyzeContext("a apple", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)
        assertEquals("a university", OfflineGrammarEngine.analyzeContext("an university", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)
        assertEquals("an hour", OfflineGrammarEngine.analyzeContext("a hour", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)

        // Auxiliary + past verb tense
        assertEquals("didn't go", OfflineGrammarEngine.analyzeContext("I didn't went", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)
        assertEquals("better", OfflineGrammarEngine.analyzeContext("more better", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)
        assertEquals("better than", OfflineGrammarEngine.analyzeContext("better then", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)

        // Duplicate English word removal
        assertEquals("the", OfflineGrammarEngine.analyzeContext("the the", LanguageId.ENGLISH).firstOrNull()?.correctedPhrase)

        // Fast safe Spacebar auto-corrections
        assertEquals("I", OfflineGrammarEngine.autoCorrectWordOnSpace("i", LanguageId.ENGLISH))
        assertEquals("the", OfflineGrammarEngine.autoCorrectWordOnSpace("teh", LanguageId.ENGLISH))
        assertEquals("I'm", OfflineGrammarEngine.autoCorrectWordOnSpace("im", LanguageId.ENGLISH))
        assertEquals("don't", OfflineGrammarEngine.autoCorrectWordOnSpace("dont", LanguageId.ENGLISH))

        // Bengali grammar & Bangla Academy spelling fixes
        assertEquals("ভালো", OfflineGrammarEngine.analyzeContext("ভাল", LanguageId.BANGLA_PROBHAT).firstOrNull()?.correctedPhrase)
        assertEquals("করছি", OfflineGrammarEngine.analyzeContext("আমি করতেছি", LanguageId.BANGLA_PROBHAT).firstOrNull()?.correctedPhrase)
        assertEquals("আমি আছি", OfflineGrammarEngine.analyzeContext("আমি আছো", LanguageId.BANGLA_PROBHAT).firstOrNull()?.correctedPhrase)
        assertEquals("আপনি আছেন", OfflineGrammarEngine.analyzeContext("আপনি আছো", LanguageId.BANGLA_PROBHAT).firstOrNull()?.correctedPhrase)
        assertEquals("একসাথে", OfflineGrammarEngine.analyzeContext("এক সাথে", LanguageId.BANGLA_PROBHAT).firstOrNull()?.correctedPhrase)

        // Reduplication in Bengali ("ভালো ভালো") is natural and should NOT be collapsed as duplicate error
        assertTrue(OfflineGrammarEngine.analyzeContext("ভালো ভালো", LanguageId.BANGLA_PROBHAT).none { it.correctedPhrase == "ভালো" })

        // Enhanced spell checker direct + proximity lookup
        assertEquals("receive", OfflineSpellChecker.directCorrectionFor("recieve"))
        assertEquals("definitely", OfflineSpellChecker.directCorrectionFor("definately"))
        assertTrue(sv1.isEmpty())
    }

    @Test
    fun testKeyboardToolId_defaultToolbarContainsTextEditingAndGemTypeAi() {
        val defaults = KeyboardToolId.DEFAULT_TOOLBAR
        assertTrue(defaults.contains(KeyboardToolId.TEXT_EDITING))
        assertTrue(defaults.contains(KeyboardToolId.GEMINI_AI))
        assertEquals(KeyboardToolId.TEXT_EDITING, KeyboardToolId.fromId("text_editing"))
        assertEquals(KeyboardToolId.GEMINI_AI, KeyboardToolId.fromId("gemini_ai"))
        assertNull(KeyboardToolId.fromId("unknown_tool_id"))
    }

    @Test
    fun testGemTypeAiEngine_offlineProofreadRewriteAndNativeConversion() = runBlocking {
        // Multi-fix CHECK with selective applyCorrections
        val checkRes = GemTypeAiEngine.runAction(
            text = "she don't recieve a apple",
            action = GemTypeAction.CHECK,
            activeLanguage = LanguageId.ENGLISH,
            apiKey = ""
        )
        assertTrue("Expected multiple local fixes: ${checkRes.corrections}", checkRes.corrections.size >= 3)
        assertTrue(checkRes.rewrittenText.contains("she doesn't"))
        assertTrue(checkRes.rewrittenText.contains("receive"))
        assertTrue(checkRes.rewrittenText.contains("an apple"))

        // Selective keep/skip: keep only the first and third fixes
        val selective = GemTypeAiEngine.applyCorrections(
            text = "she don't recieve a apple",
            corrections = listOf(checkRes.corrections.first(), checkRes.corrections.last())
        )
        assertTrue(selective.contains("she doesn't"))
        assertTrue(selective.contains("recieve"))
        assertTrue(selective.contains("an apple"))

        // Shorten removes filler words
        val shortenRes = GemTypeAiEngine.runAction(
            text = "I am basically really very happy",
            action = GemTypeAction.SHORTEN,
            activeLanguage = LanguageId.ENGLISH,
            apiKey = ""
        )
        assertEquals("I am happy", shortenRes.rewrittenText)

        // Formal & Casual register transforms
        val formalRes = GemTypeAiEngine.runAction(
            text = "hey i'm late pls wait",
            action = GemTypeAction.FORMAL,
            activeLanguage = LanguageId.ENGLISH,
            apiKey = ""
        )
        assertTrue(formalRes.rewrittenText.contains("Hello"))
        assertTrue(formalRes.rewrittenText.contains("I am"))
        assertTrue(formalRes.rewrittenText.contains("please"))

        val casualRes = GemTypeAiEngine.runAction(
            text = "I am sorry, do not worry, thank you",
            action = GemTypeAction.CASUAL,
            activeLanguage = LanguageId.ENGLISH,
            apiKey = ""
        )
        assertTrue(casualRes.rewrittenText.contains("I'm"))
        assertTrue(casualRes.rewrittenText.contains("don't"))
        assertTrue(casualRes.rewrittenText.contains("thanks"))

        // Romanized Banglish -> Native Bengali script & English with Read As back-translation
        val nativeRes = GemTypeAiEngine.runAction(
            text = "ami bangla valobashi",
            action = GemTypeAction.NATIVE,
            activeLanguage = LanguageId.BANGLA_PHONETIC,
            apiKey = ""
        )
        assertTrue("Expected Bengali script: ${nativeRes.rewrittenText}", nativeRes.rewrittenText.contains("আমি"))

        val englishRes = GemTypeAiEngine.runAction(
            text = "ami tomake valobashi",
            action = GemTypeAction.ENGLISH,
            activeLanguage = LanguageId.BANGLA_PHONETIC,
            apiKey = ""
        )
        assertEquals("i love you", englishRes.rewrittenText.lowercase())
        assertTrue(englishRes.nativeReadAs?.contains("আমি") == true)
    }

    @Test
    fun testKeyboardFontPreset_andLongPressCopyPasteLayout() {
        assertEquals(KeyboardFontPreset.ATMA_HANDWRITING, KeyboardFontPreset.fromId("unknown"))
        assertEquals(KeyboardFontPreset.ATMA_HANDWRITING, KeyboardFontPreset.fromId("atma"))
        assertEquals(KeyboardFontPreset.GALADA_CALLIGRAPHY, KeyboardFontPreset.fromId("galada"))
        assertEquals(KeyboardFontPreset.CUSTOM_IMPORTED, KeyboardFontPreset.fromId("custom"))
        assertEquals(KeyboardFontPreset.SYSTEM, KeyboardFontPreset.fromId("system"))

        val unshiftedRow3 = KeyboardLayouts.getAlphabetRows(LanguageId.ENGLISH, isShifted = false)[2]
        val cKey = unshiftedRow3.firstOrNull { it.label == "c" }
        val vKey = unshiftedRow3.firstOrNull { it.label == "v" }
        assertEquals("Copy", cKey?.secondaryLabel)
        assertEquals("Paste", vKey?.secondaryLabel)
        assertTrue(cKey?.extraVariants?.contains("Copy") == true)
        assertTrue(vKey?.extraVariants?.contains("Paste") == true)

        val shiftedRow3 = KeyboardLayouts.getAlphabetRows(LanguageId.ENGLISH, isShifted = true)[2]
        assertEquals("Copy", shiftedRow3.firstOrNull { it.label == "C" }?.secondaryLabel)
        assertEquals("Paste", shiftedRow3.firstOrNull { it.label == "V" }?.secondaryLabel)
    }
}
