package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.model.LanguageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

data class TypingStats(
    val totalWords: Int = 0,
    val totalChars: Int = 0,
    val banglaWords: Int = 0,
    val englishWords: Int = 0,
    val activeTypingMs: Long = 0L,
    val peakWpm: Int = 0
) {
    val averageWpm: Int
        get() {
            if (totalWords <= 0 || activeTypingMs < 2000L) return 0
            val minutes = activeTypingMs / 60_000.0
            return (totalWords / minutes).roundToInt().coerceIn(1, 220)
        }

    val banglaPercent: Int
        get() {
            val totalLang = banglaWords + englishWords
            if (totalLang <= 0) return 0
            return ((banglaWords * 100f) / totalLang).roundToInt().coerceIn(0, 100)
        }

    val englishPercent: Int
        get() {
            val totalLang = banglaWords + englishWords
            if (totalLang <= 0) return 0
            return (100 - banglaPercent).coerceIn(0, 100)
        }
}

class TypingStatsManager(private val context: Context) {

    companion object {
        val KEY_TOTAL_WORDS = intPreferencesKey("stats_total_words")
        val KEY_TOTAL_CHARS = intPreferencesKey("stats_total_chars")
        val KEY_BANGLA_WORDS = intPreferencesKey("stats_bangla_words")
        val KEY_ENGLISH_WORDS = intPreferencesKey("stats_english_words")
        val KEY_ACTIVE_TYPING_MS = longPreferencesKey("stats_active_typing_ms")
        val KEY_PEAK_WPM = intPreferencesKey("stats_peak_wpm")

        @Volatile
        private var instance: TypingStatsManager? = null

        fun getInstance(context: Context): TypingStatsManager {
            return instance ?: synchronized(this) {
                instance ?: TypingStatsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val statsFlow: Flow<TypingStats> = context.dataStore.data.map { prefs ->
        TypingStats(
            totalWords = prefs[KEY_TOTAL_WORDS] ?: 0,
            totalChars = prefs[KEY_TOTAL_CHARS] ?: 0,
            banglaWords = prefs[KEY_BANGLA_WORDS] ?: 0,
            englishWords = prefs[KEY_ENGLISH_WORDS] ?: 0,
            activeTypingMs = prefs[KEY_ACTIVE_TYPING_MS] ?: 0L,
            peakWpm = prefs[KEY_PEAK_WPM] ?: 0
        )
    }

    suspend fun recordCommittedWord(
        word: String,
        languageId: LanguageId,
        elapsedMsSinceLastWord: Long
    ) {
        val clean = word.trim()
        if (clean.isEmpty()) return

        val boundedElapsed = if (elapsedMsSinceLastWord in 150L..8_000L) elapsedMsSinceLastWord else 1_200L
        val isBanglaScript = clean.any { it.code in 0x0980..0x09FF || it.code in 0xA800..0xA82F } ||
            languageId in setOf(
                LanguageId.BANGLA_PROBHAT,
                LanguageId.BANGLA_PHONETIC,
                LanguageId.BANGLA_NATIONAL,
                LanguageId.BANGLA_UNIBIJOY,
                LanguageId.SYLHETI
            )

        context.dataStore.edit { prefs ->
            val newWords = (prefs[KEY_TOTAL_WORDS] ?: 0) + 1
            val newChars = (prefs[KEY_TOTAL_CHARS] ?: 0) + clean.length
            val newActiveMs = (prefs[KEY_ACTIVE_TYPING_MS] ?: 0L) + boundedElapsed

            prefs[KEY_TOTAL_WORDS] = newWords
            prefs[KEY_TOTAL_CHARS] = newChars
            prefs[KEY_ACTIVE_TYPING_MS] = newActiveMs

            if (isBanglaScript) {
                prefs[KEY_BANGLA_WORDS] = (prefs[KEY_BANGLA_WORDS] ?: 0) + 1
            } else {
                prefs[KEY_ENGLISH_WORDS] = (prefs[KEY_ENGLISH_WORDS] ?: 0) + 1
            }

            val instantWpm = (60_000.0 / boundedElapsed.coerceAtLeast(350L)).roundToInt().coerceAtMost(180)
            val prevPeak = prefs[KEY_PEAK_WPM] ?: 0
            if (instantWpm > prevPeak) {
                prefs[KEY_PEAK_WPM] = instantWpm
            }
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOTAL_WORDS] = 0
            prefs[KEY_TOTAL_CHARS] = 0
            prefs[KEY_BANGLA_WORDS] = 0
            prefs[KEY_ENGLISH_WORDS] = 0
            prefs[KEY_ACTIVE_TYPING_MS] = 0L
            prefs[KEY_PEAK_WPM] = 0
        }
    }
}
