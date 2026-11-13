package com.example.data.translator

import com.example.data.phonetic.BengaliPhoneticParser

enum class PosTag {
    PRONOUN,
    NOUN,
    VERB,
    ADJ,
    ADV,
    QUESTION,
    OTHER
}

enum class WordOrder {
    SVO,
    SOV
}

data class TranslatedToken(
    val sourceRaw: String,
    val translated: String,
    val leadingPunct: String,
    val trailingPunct: String,
    val pos: PosTag,
    val matched: Boolean
)

object LocalTranslationGrammar {

    private val sovLanguages = setOf(
        "bn", "banglish", "syl", "hi", "ur", "as", "ne", "fa", "ja", "ko", "tr",
        "ta", "te", "gu", "pa", "ml", "kn", "mr", "sa"
    )

    fun wordOrderOf(langCode: String): WordOrder {
        return if (langCode in sovLanguages) WordOrder.SOV else WordOrder.SVO
    }

    fun isWordChar(ch: Char): Boolean {
        if (ch.isLetterOrDigit()) return true
        return when (Character.getType(ch).toByte()) {
            Character.NON_SPACING_MARK,
            Character.COMBINING_SPACING_MARK,
            Character.ENCLOSING_MARK -> true
            else -> false
        }
    }

    fun detectLikelyLanguageCode(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        var bnCount = 0
        var sylNagriCount = 0
        var devanagariCount = 0
        var tamilCount = 0
        var teluguCount = 0
        var kannadaCount = 0
        var malayalamCount = 0
        var gujaratiCount = 0
        var punjabiCount = 0
        var hebrewCount = 0
        var thaiCount = 0
        var arabicCount = 0
        var cyrillicCount = 0
        var hangulCount = 0
        var japaneseCount = 0
        var cjkCount = 0
        var latinCount = 0

        for (ch in trimmed) {
            when (ch.code) {
                in 0x0980..0x09FF -> bnCount++
                in 0xA800..0xA82F -> sylNagriCount++
                in 0x0900..0x097F -> devanagariCount++
                in 0x0B80..0x0BFF -> tamilCount++
                in 0x0C00..0x0C7F -> teluguCount++
                in 0x0C80..0x0CFF -> kannadaCount++
                in 0x0D00..0x0D7F -> malayalamCount++
                in 0x0A80..0x0AFF -> gujaratiCount++
                in 0x0A00..0x0A7F -> punjabiCount++
                in 0x0590..0x05FF -> hebrewCount++
                in 0x0E00..0x0E7F -> thaiCount++
                in 0x0600..0x06FF -> arabicCount++
                in 0x0400..0x04FF -> cyrillicCount++
                in 0xAC00..0xD7AF, in 0x1100..0x11FF -> hangulCount++
                in 0x3040..0x30FF -> japaneseCount++
                in 0x4E00..0x9FFF -> cjkCount++
                in 0x0041..0x005A, in 0x0061..0x007A -> latinCount++
            }
        }

        if (sylNagriCount > 0) return "syl"
        if (bnCount > 0) {
            val sylhetiMarkers = listOf("বালা", "আফনে", "আছইননি", "কিতা", "শুকরিয়া", "হকলতা", "যাইরাম", "খাইরাম", "তুমারে")
            return if (sylhetiMarkers.any { trimmed.contains(it) }) "syl" else "bn"
        }
        if (tamilCount > 0) return "ta"
        if (teluguCount > 0) return "te"
        if (kannadaCount > 0) return "kn"
        if (malayalamCount > 0) return "ml"
        if (gujaratiCount > 0) return "gu"
        if (punjabiCount > 0) return "pa"
        if (hebrewCount > 0) return "he"
        if (thaiCount > 0) return "th"
        if (devanagariCount > 0) return "hi"
        if (arabicCount > 0) {
            val urduMarkers = listOf('ٹ', 'ڈ', 'ڑ', 'ے', 'ں', 'ہ', 'پ', 'چ', 'ژ', 'گ')
            return if (trimmed.any { it in urduMarkers }) "ur" else "ar"
        }
        if (cyrillicCount > 0) {
            val ukrainianMarkers = listOf('і', 'ї', 'є', 'ґ', 'І', 'Ї', 'Є', 'Ґ')
            return if (trimmed.any { it in ukrainianMarkers }) "uk" else "ru"
        }
        if (hangulCount > 0) return "ko"
        if (japaneseCount > 0) return "ja"
        if (cjkCount > 0) return "zh"

        if (latinCount > 0) {
            val tokens = trimmed.lowercase().split(Regex("\\s+"))
            val sylhetiRomanHints = setOf("afne", "bala", "kita", "achoinni", "shukriya", "hokolta", "jairam", "kairam", "tumare")
            if (tokens.any { it in sylhetiRomanHints }) return "syl"

            val banglishHints = setOf(
                "ami", "tumi", "apni", "kemon", "acho", "achen", "bhalo", "valo",
                "valobashi", "bhalobashi", "dhonnobad", "bangla", "bangladesh",
                "bondhu", "shuvo", "sokal", "ratri", "khabar", "pani", "amar", "tomar",
                "emon", "onubad", "obostha", "kothay", "bhalobasha", "valobasha"
            )
            if (tokens.any { it in banglishHints }) return "bn"
            return "en"
        }
        return null
    }

    private val englishContractions = mapOf(
        "i'm" to "i am",
        "im" to "i am",
        "you're" to "you are",
        "youre" to "you are",
        "we're" to "we are",
        "they're" to "they are",
        "theyre" to "they are",
        "he's" to "he is",
        "she's" to "she is",
        "it's" to "it is",
        "what's" to "what is",
        "whats" to "what is",
        "how's" to "how is",
        "where's" to "where is",
        "that's" to "that is",
        "thats" to "that is",
        "don't" to "do not",
        "dont" to "do not",
        "doesn't" to "does not",
        "doesnt" to "does not",
        "didn't" to "did not",
        "didnt" to "did not",
        "can't" to "cannot",
        "cant" to "cannot",
        "won't" to "will not",
        "wont" to "will not",
        "isn't" to "is not",
        "isnt" to "is not",
        "aren't" to "are not",
        "arent" to "are not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "haven't" to "have not",
        "hasn't" to "has not",
        "shouldn't" to "should not",
        "wouldn't" to "would not",
        "couldn't" to "could not",
        "i'll" to "i will",
        "i've" to "i have",
        "ive" to "i have",
        "you've" to "you have",
        "we've" to "we have",
        "they've" to "they have"
    )

    private val sylhetiRomanToScript = mapOf(
        "afne" to "আফনে",
        "bala" to "বালা",
        "achoinni" to "আছইননি",
        "kita" to "কিতা",
        "khobor" to "খবর",
        "kobor" to "খবর",
        "shukriya" to "শুকরিয়া",
        "shukria" to "শুকরিয়া",
        "hokolta" to "হকলতা",
        "tumar" to "তুমার",
        "tumare" to "তুমারে",
        "amar" to "আমার",
        "ami" to "আমি",
        "tumi" to "তুমি",
        "koy" to "কয়",
        "koi" to "কই",
        "bari" to "বাড়ি",
        "barit" to "বাড়িত",
        "maya" to "মায়া",
        "jairam" to "যাইরাম"
    )

    fun normalizeClauseInput(text: String, sourceLangCode: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed

        if (sourceLangCode == "en") {
            val words = trimmed.split(Regex("\\s+"))
            return words.joinToString(" ") { token ->
                val low = token.lowercase()
                englishContractions[low] ?: token
            }
        }

        val isAsciiOnly = trimmed.all { it.code < 128 }
        if (sourceLangCode == "syl" && isAsciiOnly) {
            return trimmed.split(Regex("\\s+")).joinToString(" ") { raw ->
                val clean = raw.trimEnd('?', '!', '.', ',', ' ')
                val punct = raw.removePrefix(clean)
                val mapped = sylhetiRomanToScript[clean.lowercase()]
                    ?: BengaliPhoneticParser.parsePhonetic(clean).firstOrNull()
                    ?: clean
                mapped + punct
            }
        }

        if ((sourceLangCode == "bn" || sourceLangCode == "as" || sourceLangCode == "banglish") && isAsciiOnly) {
            return BengaliPhoneticParser.transliterateSentenceToBengali(trimmed)
        }

        return trimmed
    }

    /**
     * Generates candidate stems for morphological lookup when an inflected form isn't directly in the dictionary.
     */
    fun generateStemCandidates(word: String, langCode: String): List<String> {
        val low = word.lowercase()
        val candidates = mutableListOf(low)

        when (langCode) {
            "en" -> {
                if (low.endsWith("ies") && low.length > 4) candidates.add(low.dropLast(3) + "y")
                if (low.endsWith("ied") && low.length > 4) candidates.add(low.dropLast(3) + "y")
                if (low.endsWith("es") && low.length > 3) candidates.add(low.dropLast(2))
                if (low.endsWith("s") && low.length > 2) candidates.add(low.dropLast(1))
                if (low.endsWith("ing") && low.length > 5) {
                    val stem = low.dropLast(3)
                    candidates.add(stem)
                    candidates.add(stem + "e")
                    if (stem.length >= 3 && stem.last() == stem[stem.length - 2]) {
                        candidates.add(stem.dropLast(1))
                    }
                }
                if (low.endsWith("ed") && low.length > 4) {
                    val stem = low.dropLast(2)
                    candidates.add(stem)
                    candidates.add(stem + "e")
                    if (stem.length >= 3 && stem.last() == stem[stem.length - 2]) {
                        candidates.add(stem.dropLast(1))
                    }
                }
                if (low.endsWith("er") && low.length > 4) candidates.add(low.dropLast(2))
                if (low.endsWith("est") && low.length > 5) candidates.add(low.dropLast(3))
                if (low.endsWith("ly") && low.length > 4) candidates.add(low.dropLast(2))
            }
            "bn", "syl", "as" -> {
                val suffixes = listOf(
                    "গুলো", "গুলা", "দের", "টার", "টির", "টা", "টি", "খান", "খানা",
                    "তেছি", "তেছে", "তেছো", "ছি", "ছো", "ছেন", "ছে", "লাম", "বে", "বেন",
                    "কে", "রে", "তে", "য়", "ে", "র", "এর"
                )
                for (suffix in suffixes) {
                    if (low.endsWith(suffix) && low.length > suffix.length + 1) {
                        candidates.add(low.removeSuffix(suffix))
                    }
                }
            }
            "es", "fr", "pt", "it" -> {
                if (low.endsWith("s") && low.length > 3) candidates.add(low.dropLast(1))
            }
        }
        return candidates.distinct()
    }

    /**
     * Splits multi-sentence text into (clauseText, endingPunctuation) pairs so each sentence can match
     * exact phrases or n-grams independently without failing on multi-sentence inputs.
     */
    fun splitSentences(text: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val buffer = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '.' || ch == '!' || ch == '?' || ch == '।' || ch == '؟' || ch == '\n') {
                val punctBuf = StringBuilder().append(ch)
                while (i + 1 < text.length && text[i + 1] in charArrayOf('.', '!', '?', '।', '؟')) {
                    i++
                    punctBuf.append(text[i])
                }
                val clause = buffer.toString().trim()
                if (clause.isNotEmpty() || punctBuf.toString() == "\n") {
                    result.add(clause to punctBuf.toString())
                }
                buffer.clear()
            } else {
                buffer.append(ch)
            }
            i++
        }
        val tail = buffer.toString().trim()
        if (tail.isNotEmpty()) {
            result.add(tail to "")
        }
        return result
    }

    fun localizePunctuation(punct: String, targetLang: String): String {
        if (punct.isEmpty()) return ""
        return when (targetLang) {
            "bn", "syl", "hi", "as", "ne", "sa" -> punct.replace('.', '।')
            "ar", "ur", "fa" -> punct.replace('?', '؟')
            else -> punct.replace('।', '.').replace('؟', '?')
        }
    }

    /**
     * Lightweight POS-based syntactic transfer between SVO and SOV languages.
     * Reorders simple Subject + Verb + Object <-> Subject + Object + Verb clauses.
     */
    fun applyWordOrderTransfer(
        tokens: List<TranslatedToken>,
        sourceLang: String,
        targetLang: String
    ): List<TranslatedToken> {
        if (tokens.size < 3) return tokens
        val srcOrder = wordOrderOf(sourceLang)
        val tgtOrder = wordOrderOf(targetLang)
        if (srcOrder == tgtOrder) return tokens

        if (srcOrder == WordOrder.SVO && tgtOrder == WordOrder.SOV) {
            // Find first VERB token between index 1 and tokens.size - 2; shift it to end of its clause chunk
            val verbIdx = tokens.indexOfFirst { it.pos == PosTag.VERB }
            if (verbIdx in 1 until tokens.size - 1) {
                val verbToken = tokens[verbIdx]
                val reordered = tokens.toMutableList()
                reordered.removeAt(verbIdx)
                // Preserve trailing punctuation at end of clause cleanly
                val lastTrailing = reordered.last().trailingPunct
                reordered[reordered.lastIndex] = reordered.last().copy(trailingPunct = "")
                reordered.add(verbToken.copy(trailingPunct = if (verbToken.trailingPunct.isNotEmpty()) verbToken.trailingPunct else lastTrailing))
                return reordered
            }
        } else if (srcOrder == WordOrder.SOV && tgtOrder == WordOrder.SVO) {
            // If VERB is at the end (after Subject + Object), move VERB right after Subject (index 0)
            val lastToken = tokens.last()
            if (lastToken.pos == PosTag.VERB && tokens.size >= 3) {
                val reordered = mutableListOf<TranslatedToken>()
                reordered.add(tokens.first())
                reordered.add(lastToken.copy(trailingPunct = ""))
                for (i in 1 until tokens.size - 1) {
                    val mid = tokens[i]
                    reordered.add(
                        if (i == tokens.size - 2 && lastToken.trailingPunct.isNotEmpty() && mid.trailingPunct.isEmpty()) {
                            mid.copy(trailingPunct = lastToken.trailingPunct)
                        } else {
                            mid
                        }
                    )
                }
                return reordered
            }
        }
        return tokens
    }
}
