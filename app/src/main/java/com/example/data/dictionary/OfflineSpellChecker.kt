package com.example.data.dictionary

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Hunspell-inspired 100% offline fuzzy spell checker combining:
 * 1. Direct common typo & contraction lookup (0ms exact hits)
 * 2. QWERTY keyboard-proximity weighted edit distance
 * 3. Damerau-Levenshtein edit distance (substitutions, insertions, deletions, adjacent transpositions)
 */
object OfflineSpellChecker {

    val commonMisspellings: Map<String, String> = mapOf(
        // High-frequency keyboard typos
        "teh" to "the",
        "hte" to "the",
        "adn" to "and",
        "nad" to "and",
        "taht" to "that",
        "tath" to "that",
        "tihs" to "this",
        "htis" to "this",
        "waht" to "what",
        "whta" to "what",
        "jsut" to "just",
        "juts" to "just",
        "wich" to "which",
        "whcih" to "which",
        "thier" to "their",
        "theyr" to "their",
        "yuo" to "you",
        "oyu" to "you",
        "yuor" to "your",
        "yrou" to "your",
        "becuase" to "because",
        "beacuse" to "because",
        "becasue" to "because",
        "recieve" to "receive",
        "recieved" to "received",
        "beleive" to "believe",
        "acheive" to "achieve",
        "seperate" to "separate",
        "seperated" to "separated",
        "definately" to "definitely",
        "definatly" to "definitely",
        "definitly" to "definitely",
        "tommorow" to "tomorrow",
        "tomorow" to "tomorrow",
        "tommorrow" to "tomorrow",
        "occured" to "occurred",
        "occuring" to "occurring",
        "untill" to "until",
        "alwyas" to "always",
        "alwasy" to "always",
        "realy" to "really",
        "rellay" to "really",
        "freind" to "friend",
        "frend" to "friend",
        "grammer" to "grammar",
        "languge" to "language",
        "langauge" to "language",
        "keybaord" to "keyboard",
        "keybord" to "keyboard",
        "mesage" to "message",
        "messaeg" to "message",
        "wierd" to "weird",
        "peice" to "piece",
        "neccessary" to "necessary",
        "necesary" to "necessary",
        "accomodate" to "accommodate",
        "recomend" to "recommend",
        "recommand" to "recommend",
        "enviroment" to "environment",
        "goverment" to "government",
        "independant" to "independent",
        "knowlege" to "knowledge",
        "managment" to "management",
        "occassion" to "occasion",
        "posession" to "possession",
        "prefere" to "prefer",
        "publically" to "publicly",
        "rythm" to "rhythm",
        "succesful" to "successful",
        "successfull" to "successful",
        "suprise" to "surprise",
        "truely" to "truly",
        "arguement" to "argument",
        "begining" to "beginning",
        "calender" to "calendar",
        "comming" to "coming",
        "congratulations" to "congratulations",
        "congradulations" to "congratulations",
        "disapoint" to "disappoint",
        "embarass" to "embarrass",
        "existance" to "existence",
        "familar" to "familiar",
        "foriegn" to "foreign",
        "happend" to "happened",
        "immediatly" to "immediately",
        "intresting" to "interesting",
        "maintenence" to "maintenance",
        "noticable" to "noticeable",
        "persue" to "pursue",
        "posible" to "possible",
        "proffessional" to "professional",
        "pronounciation" to "pronunciation",
        "queit" to "quiet",
        "refrence" to "reference",
        "religous" to "religious",
        "similiar" to "similar",
        "sinse" to "since",
        "strenght" to "strength",
        "thnk" to "think",
        "thnks" to "thanks",
        "thanx" to "thanks",
        "plz" to "please",
        "pls" to "please",
        "bcoz" to "because",
        "cuz" to "because",
        "helo" to "hello",
        "hllo" to "hello",
        "writting" to "writing",
        "speling" to "spelling",
        "corect" to "correct",
        "corection" to "correction",
        "translte" to "translate",
        "transalte" to "translate",
        // Missing-apostrophe contractions
        "im" to "I'm",
        "ive" to "I've",
        "dont" to "don't",
        "doesnt" to "doesn't",
        "didnt" to "didn't",
        "cant" to "can't",
        "wont" to "won't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "youre" to "you're",
        "theyre" to "they're",
        "thats" to "that's",
        "whats" to "what's",
        "heres" to "here's",
        "theres" to "there's"
    )

    private val qwertyCoords: Map<Char, Pair<Float, Float>> = buildMap {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val rowOffsets = listOf(0f, 0.35f, 0.85f)
        rows.forEachIndexed { rIdx, row ->
            row.forEachIndexed { cIdx, ch ->
                put(ch, (cIdx + rowOffsets[rIdx]) to rIdx.toFloat())
            }
        }
    }

    fun directCorrectionFor(word: String): String? {
        if (word.isBlank()) return null
        if (word == "i") return "I"
        val lower = word.lowercase()
        val fixed = commonMisspellings[lower] ?: return null
        return preserveCase(word, fixed)
    }

    fun correctOrSuggest(
        input: String,
        vocabulary: List<String>,
        maxDistance: Int = 2,
        limit: Int = 4
    ): List<String> {
        val clean = input.trim()
        if (clean.length < 2 || vocabulary.isEmpty()) return emptyList()

        val lowerInput = clean.lowercase()
        val direct = directCorrectionFor(clean)
        val exactMatch = vocabulary.firstOrNull { it.equals(clean, ignoreCase = true) }

        val scored = vocabulary.asSequence()
            .filter { abs(it.length - clean.length) <= maxDistance }
            .mapNotNull { candidate ->
                val lowerCand = candidate.lowercase()
                if (lowerCand == lowerInput) return@mapNotNull null
                val rawDist = damerauLevenshtein(lowerInput, lowerCand)
                if (rawDist in 1..maxDistance) {
                    val proximityScore = qwertyWeightedDistance(lowerInput, lowerCand)
                    val firstCharPenalty = if (candidate.startsWith(clean.take(1), ignoreCase = true)) 0.0 else 0.45
                    val lastCharBonus = if (clean.length >= 3 && candidate.endsWith(clean.takeLast(1), ignoreCase = true)) -0.15 else 0.0
                    val totalScore = proximityScore + firstCharPenalty + lastCharBonus
                    Triple(candidate, rawDist, totalScore)
                } else {
                    null
                }
            }
            .sortedWith(compareBy<Triple<String, Int, Double>> { it.third }.thenBy { it.second }.thenBy { it.first.length })
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()

        return buildList {
            if (exactMatch != null) add(exactMatch)
            if (direct != null && !contains(direct)) add(direct)
            addAll(scored)
        }.distinct().take(limit)
    }

    fun qwertyWeightedDistance(a: String, b: String): Double {
        val lenA = a.length
        val lenB = b.length
        if (lenA == 0) return lenB.toDouble()
        if (lenB == 0) return lenA.toDouble()

        val dp = Array(lenA + 1) { DoubleArray(lenB + 1) }
        for (i in 0..lenA) dp[i][0] = i.toDouble()
        for (j in 0..lenB) dp[0][j] = j.toDouble()

        for (i in 1..lenA) {
            for (j in 1..lenB) {
                val c1 = a[i - 1]
                val c2 = b[j - 1]
                val subCost = substitutionCost(c1, c2)
                var value = min(
                    min(dp[i - 1][j] + 1.0, dp[i][j - 1] + 1.0),
                    dp[i - 1][j - 1] + subCost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, dp[i - 2][j - 2] + 0.65)
                }
                dp[i][j] = value
            }
        }
        return dp[lenA][lenB]
    }

    private fun substitutionCost(c1: Char, c2: Char): Double {
        if (c1 == c2) return 0.0
        val p1 = qwertyCoords[c1]
        val p2 = qwertyCoords[c2]
        if (p1 != null && p2 != null) {
            val dist = hypot(p1.first - p2.first, p1.second - p2.second)
            if (dist <= 1.15f) return 0.55 // Immediate neighbor key slip
            if (dist <= 1.65f) return 0.78 // Diagonal neighbor key slip
        }
        return 1.0
    }

    private fun preserveCase(source: String, fixed: String): String {
        if (fixed.startsWith("I'")) return fixed
        return when {
            source.all { it.isUpperCase() } && source.length > 1 -> fixed.uppercase()
            source.firstOrNull()?.isUpperCase() == true -> fixed.replaceFirstChar { it.uppercaseChar() }
            else -> fixed
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
