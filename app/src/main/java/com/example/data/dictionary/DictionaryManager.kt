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
        "keyboard", "typegenius", "smart", "awesome", "fast", "great", "hello", "thanks",
        "please", "morning", "night", "today", "tomorrow", "friend", "happy", "welcome",
        "message", "typing", "offline", "translate", "language", "private", "secure"
    )

    private val bengaliWords = listOf(
        "আমি", "তুমি", "আপনি", "সে", "তারা", "আমরা", "তোমরা", "আমার", "তোমার", "আমাদের",
        "কেমন", "আছো", "আছেন", "ভালো", "সুন্দর", "ধন্যবাদ", "বাংলা", "বাংলাদেশ", "ভাষা", "দেশ",
        "সোনার", "বন্ধু", "কথা", "কাজ", "খাবার", "পানি", "জল", "কি", "কী", "কেন",
        "কোথায়", "কবে", "কিভাবে", "ঠিক", "খুব", "একটা", "অনেক", "সব", "মানুষ", "জীবন",
        "নতুন", "ঘর", "বাসা", "শহর", "গ্রাম", "নদী", "পাখি", "গান", "গল্প", "বইঘর",
        "বই", "কলম", "খাতা", "সময়", "দিন", "আজকে", "আজ", "কাল", "সাথে", "পরে",
        "সকাল", "রাত", "মা", "বাবা", "ভাই", "বোন", "ভালোবাসা", "শুভ", "স্বাগতম", "অভিনন্দন"
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
            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL -> bengaliWords
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

        // For Bengali Phonetic mode:
        if (languageId == LanguageId.BANGLA_PHONETIC) {
            val phoneticSuggestions = BengaliPhoneticParser.parsePhonetic(clean)
            val learned = try {
                wordDao.searchPrefix("bn_phonetic", phoneticSuggestions.firstOrNull() ?: clean, 3)
                    .map { it.word }
            } catch (_: Exception) {
                emptyList()
            }
            return@withContext (learned + phoneticSuggestions).distinct().take(6)
        }

        val results = mutableListOf<String>()

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

        // 3. Hunspell-style offline fuzzy spell correction when no/few prefix matches found
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
}
