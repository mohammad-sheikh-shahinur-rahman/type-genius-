package com.example.data.translator

import android.content.Context
import com.example.data.local.TranslationMemoryEntry
import kotlinx.coroutines.flow.Flow

data class TranslationLanguage(
    val code: String,
    val name: String,
    val nativeName: String
)

object OfflineTranslator {

    val supportedLanguages = listOf(
        TranslationLanguage("bn", "Bengali", "বাংলা"),
        TranslationLanguage("banglish", "Banglish (Roman Bangla)", "বাংলিশ"),
        TranslationLanguage("syl", "Sylheti", "সিলেটি"),
        TranslationLanguage("en", "English", "English"),
        TranslationLanguage("hi", "Hindi", "हिन्दी"),
        TranslationLanguage("zh", "Chinese (Simplified)", "中文"),
        TranslationLanguage("ar", "Arabic", "العربية"),
        TranslationLanguage("fa", "Persian", "فارسی"),
        TranslationLanguage("ur", "Urdu", "اردو"),
        TranslationLanguage("as", "Assamese", "অসমীয়া"),
        TranslationLanguage("ne", "Nepali", "नेपाली"),
        TranslationLanguage("es", "Spanish", "Español"),
        TranslationLanguage("fr", "French", "Français"),
        TranslationLanguage("de", "German", "Deutsch"),
        TranslationLanguage("pt", "Portuguese", "Português"),
        TranslationLanguage("it", "Italian", "Italiano"),
        TranslationLanguage("tr", "Turkish", "Türkçe"),
        TranslationLanguage("id", "Indonesian", "Indonesia"),
        TranslationLanguage("ms", "Malay", "Melayu"),
        TranslationLanguage("ru", "Russian", "Русский"),
        TranslationLanguage("ja", "Japanese", "日本語"),
        TranslationLanguage("ko", "Korean", "한국어")
    )

    suspend fun initialize(context: Context) {
        LocalTranslationEngine.ensureLoaded(context)
    }

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
