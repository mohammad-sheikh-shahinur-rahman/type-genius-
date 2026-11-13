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
        "am", "is", "are", "was", "were", "been", "being", "has", "had", "does",
        "did", "doing", "done", "should", "could", "would", "must", "might", "may", "shall",
        "very", "much", "many", "more", "most", "less", "better", "best", "worse", "worst",
        "always", "never", "often", "sometimes", "usually", "really", "actually", "probably",
        "definitely", "perhaps", "maybe", "almost", "already", "still", "yet", "soon",
        "here", "where", "why", "while", "before", "during", "until", "since", "through",
        "between", "around", "again", "away", "together", "everything", "everyone", "someone",
        "anyone", "nothing", "anything", "something", "different", "important", "possible",
        "necessary", "interesting", "beautiful", "amazing", "wonderful", "perfect", "awesome",
        "great", "happy", "sorry", "thank", "thanks", "please", "welcome", "hello", "goodbye",
        "morning", "afternoon", "evening", "night", "today", "tomorrow", "yesterday", "week",
        "month", "hour", "minute", "second", "friend", "family", "mother", "father", "brother",
        "sister", "home", "house", "school", "college", "office", "city", "country", "world",
        "water", "food", "coffee", "tea", "book", "phone", "computer", "screen", "keyboard",
        "typegenius", "smart", "fast", "message", "typing", "offline", "translate", "translation",
        "language", "grammar", "spelling", "correct", "correction", "sentence", "word", "letter",
        "private", "secure", "question", "answer", "problem", "solution", "idea", "reason",
        "example", "experience", "remember", "forget", "understand", "believe", "receive",
        "separate", "achieve", "occurred", "recommend", "environment", "government", "independent",
        "knowledge", "management", "occasion", "surprise", "truly", "argument", "beginning",
        "calendar", "coming", "congratulations", "disappoint", "existence", "familiar", "foreign",
        "happened", "immediately", "maintenance", "noticeable", "professional", "quiet", "reference",
        "similar", "strength", "writing", "speak", "talk", "listen", "read", "write", "learn",
        "study", "teach", "help", "need", "love", "feel", "agree", "start", "finish", "try",
        "keep", "send", "call", "wait", "live", "stay", "play", "watch", "eat", "drink", "sleep",
        "apple", "orange", "umbrella", "university", "useful", "honest", "elephant", "email"
    )

    private val bengaliWords = listOf(
        "আমি", "তুমি", "আপনি", "সে", "তারা", "আমরা", "তোমরা", "আমার", "তোমার", "আপনার", "আমাদের",
        "কেমন", "আছি", "আছো", "আছেন", "আছে", "নেই", "নাই", "ভালো", "সুন্দর", "খুব", "অনেক",
        "ধন্যবাদ", "স্বাগতম", "অভিনন্দন", "দুঃখিত", "শুকরিয়া", "বাংলা", "বাংলাদেশ", "ভাষা", "দেশ",
        "সোনার", "বন্ধু", "কথা", "কাজ", "খাবার", "পানি", "জল", "চা", "ভাত", "কী", "কি", "কেন",
        "কোথায়", "কবে", "কীভাবে", "কিভাবে", "কখন", "কারণ", "জন্য", "সাথে", "একসাথে", "পরে", "আগে",
        "ঠিক", "ভুল", "সঠিক", "শুদ্ধ", "নিশ্চয়", "অবশ্যই", "আসলে", "এখন", "এখনো", "তাহলে", "কিন্তু",
        "এবং", "অথবা", "যদি", "একটা", "একটি", "অনেকগুলো", "সব", "সবাই", "মানুষ", "জীবন", "মন",
        "হৃদয়", "ভালোবাসা", "ভালোবাসি", "নতুন", "পুরাতন", "ঘর", "বাড়ি", "বাসা", "শহর", "গ্রাম",
        "নদী", "পাখি", "আকাশ", "বৃষ্টি", "গান", "গল্প", "বইঘর", "বই", "কলম", "খাতা", "স্কুল", "কলেজ",
        "সময়", "দিন", "আজকে", "আজ", "কাল", "সকাল", "দুপুর", "বিকাল", "সন্ধ্যা", "রাত",
        "মা", "বাবা", "ভাই", "বোন", "দাদা", "দিদি", "পরিবার", "শিক্ষক", "ছাত্র",
        "করি", "করো", "করুন", "করছি", "করছে", "করবো", "যাই", "যাও", "যান", "যাচ্ছি", "যাবো",
        "আসি", "আসো", "আসুন", "দেখি", "দেখো", "দেখুন", "বলি", "বলো", "বলুন", "লিখি", "লিখছি",
        "পড়ি", "পড়ছি", "শুরু", "শেষ", "সত্যি", "পরীক্ষা", "বুদ্ধি", "সাধারণ", "ব্যবহার", "সমস্যা", "সমাধান"
    )

    private val assameseWords = listOf(
        "মই", "তুমি", "আপুনি", "নমস্কাৰ", "ধন্যবাদ", "ভাল", "অসম", "অসমীয়া", "বন্ধু", "কথা", "পানী", "ভাত", "দিন", "ৰাতি"
    )

    private val sylhetiWords = listOf(
        "ভালা", "বালা", "কিতা", "খবর", "হকলতা", "আছইননি", "সিলেট", "সিলেটি", "আফনে", "তুমরা",
        "আমি", "ধন্যবাদ", "শুকরিয়া", "কইরা", "যাইমু", "খাইমু", "বাড়িত", "ꠍꠤꠟꠐꠤ", "ꠛꠣꠟꠣ", "ꠇꠤꠔꠣ", "ꠀꠍꠂꠘ꠆ꠘꠤ", "ꠢꠇꠟꠔꠣ"
    )

    private val hindiWords = listOf(
        "नमस्ते", "धन्यवाद", "आप", "कैसे", "हैं", "मैं", "ठीक", "हूँ", "क्या", "कहाँ",
        "क्यों", "कब", "भारत", "हिंदी", "दोस्त", "प्यार", "घर", "अच्छा", "बहुत", "सब",
        "काम", "समय", "आज", "कल", "लोग", "पानी", "खाना", "दिन", "रात", "बात", "शुभ"
    )

    private val nepaliWords = listOf(
        "नमस्ते", "धन्यवाद", "तपाईं", "कस्तो", "हुनुहुन्छ", "म", "ठीक", "नेपाल", "नेपाली", "साथी", "घर", "पानी", "आज", "भोलि"
    )

    private val marathiWords = listOf(
        "नमस्कार", "धन्यवाद", "तुम्ही", "कसे", "आहात", "मी", "बरा", "मराठी", "मित्र", "घर", "पाणी", "आज", "उद्या"
    )

    private val sanskritWords = listOf(
        "नमस्ते", "धन्यवादः", "कथम्", "अस्ति", "अहम्", "कुशलम्", "संस्कृतम्", "मित्रम्", "गृहम्", "जलम्", "शुभम्"
    )

    private val arabicWords = listOf(
        "مرحبا", "شكرا", "كيف", "حالك", "أنا", "بخير", "نعم", "لا", "السلام", "عليكم",
        "صباح", "الخير", "مساء", "الله", "جميل", "اليوم", "صديق", "كتاب", "طعام", "ماء"
    )

    private val urduWords = listOf(
        "السلام", "علیکم", "شکریہ", "کیسے", "ہیں", "میں", "ٹھیک", "ہوں", "دوست", "گھر", "پانی", "آج", "کل", "اردو"
    )

    private val persianWords = listOf(
        "سلام", "ممنون", "خوب", "هستم", "شما", "چطور", "امروز", "فردا", "دوست", "خانه", "آب", "فارسی"
    )

    private val hebrewWords = listOf(
        "שלום", "תודה", "מה", "נשמע", "אני", "טוב", "כן", "לא", "היום", "מחר", "חבר", "בית", "מים"
    )

    private val tamilWords = listOf("வணக்கம்", "நன்றி", "நலம்", "தமிழ்", "நண்பன்", "வீடு", "தண்ணீர்", "இன்று", "நாளை")
    private val teluguWords = listOf("నమస్కారం", "ధన్యవాదాలు", "బాగున్నాను", "తెలుగు", "స్నేహితుడు", "ఇల్లు", "నీరు", "ఈరోజు")
    private val gujaratiWords = listOf("નમસ્તે", "આભાર", "કેમ", "છો", "મજામાં", "ગુજરાતી", "મિત્ર", "ઘર", "પાણી", "આજે")
    private val punjabiWords = listOf("ਸਤ", "ਸ੍ਰੀ", "ਅਕਾਲ", "ਧੰਨਵਾਦ", "ਕਿਵੇਂ", "ਠੀਕ", "ਪੰਜਾਬੀ", "ਦੋਸਤ", "ਘਰ", "ਪਾਣੀ")
    private val malayalamWords = listOf("നമസ്കാരം", "നന്ദി", "സുഖം", "മലയാളം", "സുഹൃത്ത്", "വീട്", "വെള്ളം", "ഇന്ന്")
    private val kannadaWords = listOf("ನಮಸ್ಕಾರ", "ಧನ್ಯವಾದ", "ಚೆನ್ನಾಗಿದೆ", "ಕನ್ನಡ", "ಸ್ನೇಹಿತ", "ಮನೆ", "ನೀರು", "ಇಂದು")

    private val spanishWords = listOf("hola", "gracias", "buenos", "días", "cómo", "estás", "bien", "amigo", "casa", "agua", "hoy", "mañana", "español")
    private val frenchWords = listOf("bonjour", "merci", "comment", "allez", "vous", "bien", "ami", "maison", "aujourd'hui", "demain", "français")
    private val germanWords = listOf("hallo", "danke", "guten", "morgen", "wie", "geht", "freund", "haus", "wasser", "heute", "deutsch")
    private val portugueseWords = listOf("olá", "obrigado", "bom", "dia", "tudo", "bem", "amigo", "casa", "água", "hoje", "português")
    private val italianWords = listOf("ciao", "grazie", "buongiorno", "come", "stai", "bene", "amico", "casa", "acqua", "oggi", "italiano")
    private val turkishWords = listOf("merhaba", "teşekkürler", "nasılsın", "iyiyim", "günaydın", "arkadaş", "ev", "su", "bugün", "türkçe")
    private val russianWords = listOf("привет", "спасибо", "как", "дела", "хорошо", "друг", "дом", "вода", "сегодня", "завтра", "русский")
    private val indonesianWords = listOf("halo", "terima", "kasih", "apa", "kabar", "baik", "selamat", "pagi", "teman", "rumah", "hari")
    private val malayWords = listOf("hai", "terima", "kasih", "apa", "khabar", "baik", "selamat", "pagi", "kawan", "rumah", "hari")
    private val koreanWords = listOf("안녕하세요", "감사합니다", "잘", "지내요", "친구", "오늘", "내일", "집", "물", "사랑해요")
    private val japaneseWords = listOf("こんにちは", "ありがとう", "ございます", "元気", "です", "友達", "今日", "明日", "日本", "日本語")

    fun getVocabulary(languageId: LanguageId): List<String> {
        return when (languageId) {
            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL, LanguageId.BANGLA_UNIBIJOY -> bengaliWords
            LanguageId.SYLHETI -> sylhetiWords
            LanguageId.ASSAMESE -> assameseWords
            LanguageId.HINDI -> hindiWords
            LanguageId.NEPALI -> nepaliWords
            LanguageId.MARATHI -> marathiWords
            LanguageId.SANSKRIT -> sanskritWords
            LanguageId.ARABIC -> arabicWords
            LanguageId.URDU -> urduWords
            LanguageId.PERSIAN -> persianWords
            LanguageId.HEBREW -> hebrewWords
            LanguageId.TAMIL -> tamilWords
            LanguageId.TELUGU -> teluguWords
            LanguageId.GUJARATI -> gujaratiWords
            LanguageId.PUNJABI -> punjabiWords
            LanguageId.MALAYALAM -> malayalamWords
            LanguageId.KANNADA -> kannadaWords
            LanguageId.SPANISH -> spanishWords
            LanguageId.FRENCH -> frenchWords
            LanguageId.GERMAN -> germanWords
            LanguageId.PORTUGUESE -> portugueseWords
            LanguageId.ITALIAN -> italianWords
            LanguageId.TURKISH -> turkishWords
            LanguageId.RUSSIAN -> russianWords
            LanguageId.INDONESIAN -> indonesianWords
            LanguageId.MALAY -> malayWords
            LanguageId.KOREAN -> koreanWords
            LanguageId.JAPANESE -> japaneseWords
            LanguageId.ENGLISH, LanguageId.ENGLISH_UK -> englishWords
        }
    }

    suspend fun getPredictions(
        input: String,
        languageId: LanguageId,
        spellCheckEnabled: Boolean = true
    ): List<String> = withContext(Dispatchers.Default) {
        if (input.isBlank()) {
            return@withContext getDefaultSuggestions(languageId)
        }

        val clean = input.trim()

        // For Bengali Phonetic mode: always keep instant Avro Bengali output ahead of raw ASCII
        if (languageId == LanguageId.BANGLA_PHONETIC) {
            val phoneticSuggestions = BengaliPhoneticParser.parsePhonetic(clean)
            val learnedBengali = try {
                wordDao.searchPrefix("bn_phonetic", phoneticSuggestions.firstOrNull() ?: clean, 3)
                    .map { it.word }
                    .filter { word -> word.any { it in '\u0980'..'\u09FF' } }
            } catch (_: Exception) {
                emptyList()
            }
            val primaryPhonetic = phoneticSuggestions.firstOrNull { word -> word.any { it in '\u0980'..'\u09FF' } }
            return@withContext buildList {
                if (primaryPhonetic != null) add(primaryPhonetic)
                addAll(learnedBengali)
                addAll(phoneticSuggestions)
            }.distinct().take(6)
        }

        val results = mutableListOf<String>()

        // 0. Instant exact typo/contraction fix first (e.g. "teh" -> "the", "dont" -> "don't", "im" -> "I'm")
        val directFix = if (spellCheckEnabled) OfflineSpellChecker.directCorrectionFor(clean) else null
        if (directFix != null) {
            results.add(directFix)
        }

        // 1. Check local learned words from Room SQLite
        val langCode = languageId.code
        try {
            val dbMatches = wordDao.searchPrefix(langCode, clean, 4).map { it.word }
            results.addAll(dbMatches)
        } catch (_: Exception) {}

        // 2. Check bundled offline vocabulary prefix matches
        val dict = getVocabulary(languageId)
        val prefixMatches = dict.filter { it.startsWith(clean, ignoreCase = true) && !it.equals(clean, ignoreCase = true) }
        results.addAll(prefixMatches)

        // 3. Hunspell-style proximity fuzzy spell correction when no/few prefix matches found
        if (spellCheckEnabled && clean.length >= 2) {
            val spellCorrections = OfflineSpellChecker.correctOrSuggest(clean, dict, maxDistance = 2, limit = 3)
            results.addAll(spellCorrections)
        }

        if (results.isEmpty()) {
            results.add(clean)
        }

        return@withContext results.distinct().take(6)
    }

    fun getDefaultSuggestions(languageId: LanguageId): List<String> {
        return getVocabulary(languageId).take(5)
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

    val learnedWordsFlow: kotlinx.coroutines.flow.Flow<List<LearnedWord>> = wordDao.getAllLearnedWords()

    suspend fun addCustomWord(word: String, languageCode: String) = withContext(Dispatchers.IO) {
        val clean = word.trim()
        if (clean.isBlank()) return@withContext
        try {
            wordDao.insertOrUpdate(
                LearnedWord(
                    word = clean,
                    language = languageCode,
                    frequency = 5,
                    lastUsed = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun removeLearnedWord(word: String) = withContext(Dispatchers.IO) {
        try {
            wordDao.deleteWord(word.trim())
        } catch (_: Exception) {}
    }

    suspend fun clearAllLearnedWords() = withContext(Dispatchers.IO) {
        try {
            wordDao.clearAllLearnedWords()
        } catch (_: Exception) {}
    }
}
