package com.example.service

import java.util.Locale

/**
 * Enhances raw ASR output (especially on-device/offline SpeechRecognizer results):
 * 1. N-best candidate reranking using script match, confidence, and user vocabulary.
 * 2. Spoken Bengali & English punctuation / newline voice commands.
 * 3. Punctuation spacing normalization and English sentence-start capitalization.
 */
object OfflineVoicePostProcessor {

    private val voiceCommands = listOf(
        // Longer multi-word phrases first
        "exclamation point" to "!",
        "exclamation mark" to "!",
        "question mark" to "?",
        "প্রশ্নবোধক চিহ্ন" to "?",
        "বিস্ময়বোধক চিহ্ন" to "!",
        "বিস্ময়বোধক চিহ্ন" to "!",
        "নতুন লাইন" to "\n",
        "নেক্সট লাইন" to "\n",
        "next line" to "\n",
        "new line" to "\n",
        "ফুল স্টপ" to "।",
        "পূর্ণচ্ছেদ" to "।",
        "প্রশ্নবোধক" to "?",
        "প্রশ্নচিহ্ন" to "?",
        "বিস্ময়চিহ্ন" to "!",
        "বিস্ময়চিহ্ন" to "!",
        "full stop" to ".",
        "সেমিকোলন" to ";",
        "semicolon" to ";",
        "হাইফেন" to "-",
        "hyphen" to "-",
        "period" to ".",
        "দাঁড়ি" to "।",
        "দাড়ি" to "।",
        "দাঁড়ি" to "।",
        "কোলন" to ":",
        "colon" to ":",
        "কমা" to ",",
        "comma" to ","
    )

    fun pickBestCandidate(
        candidates: List<String>,
        confidences: FloatArray?,
        languageTag: String,
        preferredWords: Set<String> = emptySet()
    ): String {
        val cleaned = candidates.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return ""
        if (cleaned.size == 1) return cleaned.first()

        val wantsBangla = languageTag.startsWith("bn", ignoreCase = true)
        var bestText = cleaned.first()
        var bestScore = Float.NEGATIVE_INFINITY

        cleaned.forEachIndexed { idx, cand ->
            val baseConf = confidences?.getOrNull(idx)?.takeIf { it >= 0f }
                ?: (1.0f - idx * 0.12f)
            var score = baseConf

            if (wantsBangla) {
                val banglaChars = cand.count { it in '\u0980'..'\u09FF' }
                val ratio = banglaChars.toFloat() / cand.length.coerceAtLeast(1)
                if (banglaChars > 0) score += 0.25f + ratio * 0.2f
            } else if (languageTag.startsWith("en", ignoreCase = true)) {
                val latinChars = cand.count { it in 'a'..'z' || it in 'A'..'Z' }
                val ratio = latinChars.toFloat() / cand.length.coerceAtLeast(1)
                if (latinChars > 0) score += 0.2f * ratio
            }

            if (preferredWords.isNotEmpty()) {
                val tokens = cand.split(Regex("\\s+"))
                val hits = tokens.count { tok -> preferredWords.contains(tok.lowercase(Locale.ROOT)) }
                score += (hits * 0.08f).coerceAtMost(0.24f)
            }

            if (score > bestScore) {
                bestScore = score
                bestText = cand
            }
        }
        return bestText
    }

    fun processSpokenText(
        rawText: String,
        languageTag: String,
        previousCommittedTail: String = ""
    ): String {
        var text = rawText.trim()
        if (text.isEmpty()) return ""

        // 1. Replace spoken punctuation / newline commands
        for ((phrase, symbol) in voiceCommands) {
            val isAsciiPhrase = phrase.all { it.code < 128 }
            val pattern = if (isAsciiPhrase) {
                Regex("(?i)(^|\\s)${Regex.escape(phrase)}(?=\\s|$|[.,!?।])")
            } else {
                Regex("(^|\\s)${Regex.escape(phrase)}(?=\\s|$|[.,!?।])")
            }
            text = text.replace(pattern) { match ->
                if (symbol == "\n") "\n" else symbol
            }
        }

        // 2. Clean whitespace around punctuation marks & newlines
        text = text
            .replace(Regex("[ \\t]+([,।.?!:;])"), "$1")
            .replace(Regex("([,।.?!:;])([^\\s\\n\"')\\]])"), "$1 $2")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()

        if (text.isEmpty()) return ""

        // 3. Smart English capitalization at start of field or after sentence end
        if (languageTag.startsWith("en", ignoreCase = true)) {
            val trimmedPrev = previousCommittedTail.trimEnd()
            val shouldCapStart = trimmedPrev.isEmpty() ||
                trimmedPrev.endsWith(".") ||
                trimmedPrev.endsWith("!") ||
                trimmedPrev.endsWith("?") ||
                trimmedPrev.endsWith("।") ||
                previousCommittedTail.endsWith("\n")

            if (shouldCapStart) {
                text = capitalizeFirstLetter(text)
            }
            // Also capitalize " i " standalone pronoun and letters after [.!?]
            text = text.replace(Regex("\\bi\\b"), "I")
            text = capitalizeAfterSentenceBoundary(text)
        }

        return text
    }

    /**
     * Prevents continuous ASR restart from re-committing the exact duplicate trailing phrase
     * within a short time window.
     */
    fun isDuplicateEcho(newText: String, lastCommittedText: String, elapsedMs: Long): Boolean {
        if (elapsedMs > 2500L) return false
        val a = newText.trim().lowercase(Locale.ROOT)
        val b = lastCommittedText.trim().lowercase(Locale.ROOT)
        return a.isNotEmpty() && a == b
    }

    private fun capitalizeFirstLetter(input: String): String {
        val idx = input.indexOfFirst { it.isLetter() }
        if (idx < 0) return input
        val ch = input[idx]
        if (!ch.isLowerCase()) return input
        return input.substring(0, idx) + ch.uppercaseChar() + input.substring(idx + 1)
    }

    private fun capitalizeAfterSentenceBoundary(input: String): String {
        val sb = StringBuilder(input.length)
        var capNext = false
        for (c in input) {
            if (capNext && c.isLetter()) {
                sb.append(c.uppercaseChar())
                capNext = false
            } else {
                sb.append(c)
                if (c == '.' || c == '!' || c == '?' || c == '\n') {
                    capNext = true
                } else if (!c.isWhitespace()) {
                    capNext = false
                }
            }
        }
        return sb.toString()
    }
}
