package com.example.data.translator

import android.content.Context
import com.example.data.local.TranslationMemoryEntry
import kotlinx.coroutines.flow.Flow

data class TranslationLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val isBundledCore: Boolean = true
)

object OfflineTranslator {

    val supportedLanguages = listOf(
        // 22 Bundled Core Languages (pre-installed offline)
        TranslationLanguage("bn", "Bengali", "বাংলা", isBundledCore = true),
        TranslationLanguage("banglish", "Banglish (Roman Bangla)", "বাংলিশ", isBundledCore = true),
        TranslationLanguage("syl", "Sylheti", "সিলেটি", isBundledCore = true),
        TranslationLanguage("en", "English", "English", isBundledCore = true),
        TranslationLanguage("hi", "Hindi", "हिन्दी", isBundledCore = true),
        TranslationLanguage("zh", "Chinese (Simplified)", "中文", isBundledCore = true),
        TranslationLanguage("ar", "Arabic", "العربية", isBundledCore = true),
        TranslationLanguage("fa", "Persian", "فارسی", isBundledCore = true),
        TranslationLanguage("ur", "Urdu", "اردو", isBundledCore = true),
        TranslationLanguage("as", "Assamese", "অসমীয়া", isBundledCore = true),
        TranslationLanguage("ne", "Nepali", "नेपाली", isBundledCore = true),
        TranslationLanguage("es", "Spanish", "Español", isBundledCore = true),
        TranslationLanguage("fr", "French", "Français", isBundledCore = true),
        TranslationLanguage("de", "German", "Deutsch", isBundledCore = true),
        TranslationLanguage("pt", "Portuguese", "Português", isBundledCore = true),
        TranslationLanguage("it", "Italian", "Italiano", isBundledCore = true),
        TranslationLanguage("tr", "Turkish", "Türkçe", isBundledCore = true),
        TranslationLanguage("id", "Indonesian", "Indonesia", isBundledCore = true),
        TranslationLanguage("ms", "Malay", "Melayu", isBundledCore = true),
        TranslationLanguage("ru", "Russian", "Русский", isBundledCore = true),
        TranslationLanguage("ja", "Japanese", "日本語", isBundledCore = true),
        TranslationLanguage("ko", "Korean", "한국어", isBundledCore = true),

        // 16 Additional Downloadable Offline Language Packs
        TranslationLanguage("ta", "Tamil", "தமிழ்", isBundledCore = false),
        TranslationLanguage("te", "Telugu", "తెలుగు", isBundledCore = false),
        TranslationLanguage("gu", "Gujarati", "ગુજરાતી", isBundledCore = false),
        TranslationLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", isBundledCore = false),
        TranslationLanguage("ml", "Malayalam", "മലയാളം", isBundledCore = false),
        TranslationLanguage("kn", "Kannada", "ಕನ್ನಡ", isBundledCore = false),
        TranslationLanguage("mr", "Marathi", "मराठी", isBundledCore = false),
        TranslationLanguage("sa", "Sanskrit", "संस्कृतम्", isBundledCore = false),
        TranslationLanguage("he", "Hebrew", "עברית", isBundledCore = false),
        TranslationLanguage("th", "Thai", "ไทย", isBundledCore = false),
        TranslationLanguage("vi", "Vietnamese", "Tiếng Việt", isBundledCore = false),
        TranslationLanguage("nl", "Dutch", "Nederlands", isBundledCore = false),
        TranslationLanguage("pl", "Polish", "Polski", isBundledCore = false),
        TranslationLanguage("uk", "Ukrainian", "Українська", isBundledCore = false),
        TranslationLanguage("sw", "Swahili", "Kiswahili", isBundledCore = false),
        TranslationLanguage("tl", "Filipino", "Filipino", isBundledCore = false)
    )

    suspend fun initialize(context: Context) {
        LocalTranslationEngine.ensureLoaded(context)
    }

    fun isPackReady(context: Context?, langCode: String, downloadedSet: Set<String> = emptySet()): Boolean =
        TranslationPackManager.isPackReady(context, langCode, downloadedSet)

    fun getPackSizeLabel(context: Context, langCode: String): String =
        TranslationPackManager.getPackSizeLabel(context, langCode)

    suspend fun downloadLanguagePack(context: Context, langCode: String): TranslationPackInstallResult =
        TranslationPackManager.downloadLanguagePack(context, langCode)

    suspend fun downloadAllLanguagePacks(context: Context): List<TranslationPackInstallResult> =
        TranslationPackManager.downloadAllExtraPacks(context)

    suspend fun removeLanguagePack(context: Context, langCode: String) =
        TranslationPackManager.removeLanguagePack(context, langCode)

    fun translate(
        input: String,
        sourceLangCode: String,
        targetLangCode: String
    ): OfflineTranslationResult {
        return LocalTranslationEngine.translate(input, sourceLangCode, targetLangCode)
    }

    suspend fun translateWithMemory(
        context: Context,
        input: String,
        sourceLangCode: String,
        targetLangCode: String
    ): OfflineTranslationResult {
        return LocalTranslationEngine.translateWithMemory(context, input, sourceLangCode, targetLangCode)
    }

    fun detectSourceLanguage(input: String): TranslationLanguage? {
        val code = LocalTranslationGrammar.detectLikelyLanguageCode(input) ?: return null
        return supportedLanguages.find { it.code == code }
    }

    fun getQuickPhrases(): List<String> = listOf(
        "Hello",
        "How are you",
        "I am fine",
        "Thank you",
        "Good morning",
        "Good night",
        "Nice to meet you",
        "I love Bangladesh",
        "See you later",
        "No problem"
    )

    fun observeHistory(context: Context): Flow<List<TranslationMemoryEntry>> =
        LocalTranslationEngine.observeHistory(context)

    suspend fun saveCustomPhrase(
        context: Context,
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    ) = LocalTranslationEngine.saveCustomTranslation(context, sourceText, translatedText, sourceLang, targetLang)

    suspend fun togglePinHistory(context: Context, entry: TranslationMemoryEntry) =
        LocalTranslationEngine.togglePinHistory(context, entry)

    suspend fun deleteHistoryEntry(context: Context, entry: TranslationMemoryEntry) =
        LocalTranslationEngine.deleteHistoryEntry(context, entry)

    fun getModelStats(): Pair<Int, Int> = LocalTranslationEngine.getModelStats()

    fun isPackLoadedFromAssets(): Boolean = LocalTranslationEngine.isPackLoadedFromAssets()
}
