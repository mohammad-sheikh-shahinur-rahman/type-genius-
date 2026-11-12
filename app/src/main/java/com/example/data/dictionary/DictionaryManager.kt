package com.example.data.dictionary

import android.content.Context
import com.example.data.local.LearnedWord
import com.example.data.local.TypeGeniusDatabase
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.model.LanguageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryManager(context: Context) {

    private val db = TypeGeniusDatabase.getDatabase(context)
    private val wordDao = db.wordDao()

    // Pre-bundled high-frequency offline dictionaries
    private val englishWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "keyboard", "typegenius", "smart", "awesome", "fast", "great", "hello", "thanks"
    )

    private val bengaliWords = listOf(
        "আমি", "তুমি", "আপনি", "সে", "তারা", "আমরা", "তোমরা", "আমার", "তোমার", "আমাদের",
        "কেমন", "আছো", "আছেন", "ভালো", "সুন্দর", "ধন্যবাদ", "বাংলা", "বাংলাদেশ", "ভাষা", "দেশ",
        "সোনার", "বন্ধু", "কথা", "কাজ", "খাবার", "পানি", "জল", "কি", "কী", "কেন",
        "কোথায়", "কবে", "কিভাবে", "ঠিক", "খুব", "একটা", "অনেক", "সব", "মানুষ", "জীবন",
        "নতুন", "ঘর", "বাসা", "শহর", "গ্রাম", "নদী", "পাখি", "গান", "গল্প", "বইঘর",
        "বই", "কলম", "খাতা", "সময়", "দিন", "আজকে", "আজ", "কাল", "সাথে", "পরে"
    )

    private val hindiWords = listOf(
        "नमस्ते", "धन्यवाद", "आप", "कैसे", "हैं", "मैं", "ठीक", "हूँ", "क्या", "कहाँ",
        "क्यों", "कब", "भारत", "हिंदी", "दोस्त", "प्यार", "घर", "अच्छा", "बहुत", "सब",
        "काम", "समय", "आज", "कल", "लोग", "पानी", "खाना", "दिन", "रात", "बात"
    )

    private val arabicWords = listOf(
        "مرحبا", "شكرا", "كيف", "حالك", "أنا", "بخير", "نعم", "لا", "السلام", "عليكم",
        "صباح", "الخير", "مساء", "الله", "جميل", "اليوم", "صديق", "كتاب", "طعام", "ماء"
    )

    suspend fun getPredictions(
        input: String,
        languageId: LanguageId
    ): List<String> = withContext(Dispatchers.Default) {
        if (input.isBlank()) {
            return@withContext getDefaultSuggestions(languageId)
        }

        val clean = input.trim()

        // For Bengali Phonetic mode:
        if (languageId == LanguageId.BANGLA_PHONETIC) {
            val phoneticSuggestions = BengaliPhoneticParser.parsePhonetic(clean)
            val learned = try {
                wordDao.searchPrefix("bn", phoneticSuggestions.firstOrNull() ?: clean, 3)
                    .map { it.word }
            } catch (e: Exception) {
                emptyList()
            }
            return@withContext (learned + phoneticSuggestions).distinct().take(6)
        }

        // For standard languages:
        val results = mutableListOf<String>()

        // 1. Check local learned words from SQLite
        val langCode = languageId.code
        try {
            val dbMatches = wordDao.searchPrefix(langCode, clean, 4).map { it.word }
            results.addAll(dbMatches)
        } catch (_: Exception) {}

        // 2. Check bundled vocabulary
        val dict = when (languageId) {
            LanguageId.BANGLA_NATIONAL -> bengaliWords
            LanguageId.HINDI -> hindiWords
            LanguageId.ARABIC -> arabicWords
            else -> englishWords
        }

        val matches = dict.filter { it.startsWith(clean, ignoreCase = true) && it != clean }
        results.addAll(matches)

        if (results.isEmpty()) {
            results.add(clean)
        }

        return@withContext results.distinct().take(6)
    }

    private fun getDefaultSuggestions(languageId: LanguageId): List<String> {
        return when (languageId) {
            LanguageId.BANGLA_PHONETIC -> listOf("আমি", "কেমন", "ধন্যবাদ", "বাংলা", "ভালো")
            LanguageId.BANGLA_NATIONAL -> listOf("আমি", "তুমি", "কেমন", "ভালো", "ধন্যবাদ")
            LanguageId.HINDI -> listOf("नमस्ते", "धन्यवाद", "आप", "कैसे", "भारत")
            LanguageId.ARABIC -> listOf("مرحبا", "شكرا", "السلام", "اليوم", "نعم")
            LanguageId.ENGLISH -> listOf("Hello", "Thanks", "How", "Good", "Great")
        }
    }

    suspend fun recordWordUsed(word: String, languageId: LanguageId) = withContext(Dispatchers.IO) {
        if (word.isBlank() || word.length < 2) return@withContext
        try {
            wordDao.insertOrUpdate(
                LearnedWord(
                    word = word.trim(),
                    language = languageId.code,
                    frequency = 1,
                    lastUsed = System.currentTimeMillis()
                )
            )
            wordDao.incrementFrequency(word.trim(), languageId.code)
        } catch (_: Exception) {}
    }
}
