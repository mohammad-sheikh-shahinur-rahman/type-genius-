package com.example.data.dictionary

import kotlin.math.abs
import kotlin.math.min

/**
 * Hunspell-inspired 100% offline fuzzy spell checker using Damerau-Levenshtein
 * edit distance (substitutions, insertions, deletions, and adjacent transpositions).
 * Runs entirely on-device with zero external network calls.
 */
object OfflineSpellChecker {

    fun correctOrSuggest(
        input: String,
        vocabulary: List<String>,
        maxDistance: Int = 2,
        limit: Int = 4
    ): List<String> {
        val clean = input.trim()
        if (clean.length < 2 || vocabulary.isEmpty()) return emptyList()

        val lowerInput = clean.lowercase()
        // If already an exact match in vocabulary, return exact casing first
        val exactMatch = vocabulary.firstOrNull { it.equals(clean, ignoreCase = true) }

        val scored = vocabulary.asSequence()
            .filter { abs(it.length - clean.length) <= maxDistance }
            .mapNotNull { candidate ->
                val dist = damerauLevenshtein(lowerInput, candidate.lowercase())
                if (dist in 1..maxDistance) {
                    val prefixBonus = if (candidate.startsWith(clean.take(1), ignoreCase = true)) 0 else 1
                    Triple(candidate, dist, prefixBonus)
                } else {
                    null
                }
            }
            .sortedWith(compareBy<Triple<String, Int, Int>> { it.second }.thenBy { it.third }.thenBy { it.first.length })
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()

        return if (exactMatch != null) {
            (listOf(exactMatch) + scored).distinct().take(limit)
        } else {
            scored
        }
    }

    fun damerauLevenshtein(a: String, b: String): Int {
        val lenA = a.length
        val lenB = b.length
        if (lenA == 0) return lenB
        if (lenB == 0) return lenA

        val dp = Array(lenA + 1) { IntArray(lenB + 1) }
        for (i in 0..lenA) dp[i][0] = i
        for (j in 0..lenB) dp[0][j] = j

        for (i in 1..lenA) {
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = value
            }
        }
        return dp[lenA][lenB]
    }
}
