package com.example.data.dictionary

import com.example.model.LanguageId

data class GrammarSuggestion(
    val displayLabel: String,
    val correctedPhrase: String,
    val replaceCharLength: Int,
    val trailingSpace: Boolean,
    val reason: String
)

/**
 * 100% Offline, rule-based contextual Grammar & Proofreading Engine for English and Bengali/Banglish.
 * Evaluates multi-word windows before the cursor for subject-verb agreement, articles (a/an),
 * verb tense/auxiliaries, contractions, duplicate words, and Bangla Academy standard spelling & verb harmony.
 */
object OfflineGrammarEngine {

    private val vowelSoundExceptionsAn = setOf(
        "hour", "hours", "honest", "honestly", "honor", "honour", "honorable",
        "heir", "heirloom", "herb"
    )
    private val consonantSoundStartsWithVowel = listOf(
        "uni", "use", "user", "usual", "useful", "useless", "util", "uk", "eu", "one", "once"
    )

    private val irregularPastToBase = mapOf(
        "went" to "go",
        "saw" to "see",
        "came" to "come",
        "did" to "do",
        "ate" to "eat",
        "took" to "take",
        "gave" to "give",
        "wrote" to "write",
        "spoke" to "speak",
        "knew" to "know",
        "bought" to "buy",
        "made" to "make",
        "got" to "get",
        "felt" to "feel",
        "found" to "find",
        "told" to "tell",
        "thought" to "think",
        "ran" to "run",
        "slept" to "sleep",
        "drank" to "drink",
        "brought" to "bring",
        "broken" to "break",
        "gone" to "go"
    )

    private val doubleComparatives = mapOf(
        "better" to "better",
        "worse" to "worse",
        "faster" to "faster",
        "slower" to "slower",
        "bigger" to "bigger",
        "smaller" to "smaller",
        "easier" to "easier",
        "harder" to "harder",
        "stronger" to "stronger",
        "smarter" to "smarter",
        "cheaper" to "cheaper",
        "cleaner" to "cleaner",
        "higher" to "higher",
        "lower" to "lower"
    )

    private val comparativeThenTriggers = setOf(
        "better", "worse", "more", "less", "bigger", "smaller",
        "faster", "slower", "easier", "harder", "higher", "lower",
        "older", "younger", "stronger", "rather", "other"
    )

    private val englishMultiWordCorrections = mapOf(
        "alot" to "a lot",
        "thankyou" to "thank you",
        "infront" to "in front",
        "everytime" to "every time",
        "aswell" to "as well",
        "ofcourse" to "of course",
        "eachother" to "each other",
        "atleast" to "at least",
        "incase" to "in case",
        "inspite" to "in spite",
        "highschool" to "high school",
        "allright" to "alright"
    )

    private val bengaliSpellingFixes = mapOf(
        "ভাল" to "ভালো",
        "কেনো" to "কেন",
        "সাতে" to "সাথে",
        "হইতেছে" to "হচ্ছে",
        "করতেছি" to "করছি",
        "করতেছে" to "করছে",
        "করতেছো" to "করছো",
        "যাইতেছি" to "যাচ্ছি",
        "যাইতেছে" to "যাচ্ছে",
        "খাইতেছি" to "খাচ্ছি",
        "আসতেছি" to "আসছি",
        "বলতেছি" to "বলছি",
        "দেখতেছি" to "দেখছি",
        "লিখতেছি" to "লিখছি",
        "পড়তেছি" to "পড়ছি",
        "ভূল" to "ভুল",
        "পূর্ন" to "পূর্ণ",
        "দুখিত" to "দুঃখিত",
        "নিশ্চই" to "নিশ্চয়",
        "সাগতম" to "স্বাগতম",
        "ধন্যবাধ" to "ধন্যবাদ",
        "তাহালে" to "তাহলে",
        "ইতমধ্যে" to "ইতিমধ্যে",
        "অবশ্যয়" to "অবশ্যই",
        "আশলে" to "আসলে",
        "সুরু" to "শুরু",
        "শুন্দর" to "সুন্দর",
        "মানুস" to "মানুষ",
        "পরিক্ষা" to "পরীক্ষা",
        "বুদ্দি" to "বুদ্ধি",
        "জন্ন" to "জন্য",
        "সত্তি" to "সত্যি",
        "বাঙলাদেশ" to "বাংলাদেশ",
        "কারন" to "কারণ",
        "সাধারন" to "সাধারণ",
        "ব্যার্থ" to "ব্যর্থ",
        "স্বরন" to "স্মরণ",
        "শরন" to "স্মরণ",
        "প্রতিযোগীতা" to "প্রতিযোগিতা",
        "সহযোগীতা" to "সহযোগিতা",
        "উপকারিতা" to "উপকারিতা",
        "শান্তি" to "শান্তি",
        "ভালোবাসা" to "ভালোবাসা",
        "ভালবাসা" to "ভালোবাসা",
        "ভালবাসি" to "ভালোবাসি",
        "ভালবেসে" to "ভালোবেসে",
        "একসাতে" to "একসাথে",
        "দোকানদারী" to "দোকানদারি",
        "ব্যায়" to "ব্যয়",
        "ব্যাবহার" to "ব্যবহার",
        "ব্যাস্ত" to "ব্যস্ত",
        "সন্মান" to "সম্মান",
        "উদ্দেশো" to "উদ্দেশ্য"
    )

    private val englishDuplicateSkip = setOf("had", "that", "bye", "ha")

    /**
     * Analyzes text immediately before the cursor and returns ordered contextual grammar corrections.
     */
    fun analyzeContext(
        textBeforeCursor: String,
        languageId: LanguageId
    ): List<GrammarSuggestion> {
        if (textBeforeCursor.isEmpty()) return emptyList()

        val isBengaliFamily = languageId in listOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_PHONETIC,
            LanguageId.BANGLA_NATIONAL,
            LanguageId.ASSAMESE
        )

        val hasTrailingSpace = textBeforeCursor.last().isWhitespace()
        val trimmed = textBeforeCursor.trimEnd()
        if (trimmed.isEmpty()) return emptyList()
        val trailingWsLen = textBeforeCursor.length - trimmed.length

        // Extract up to last 4 whitespace-separated tokens with their exact character span from trimmed end
        val rawTokens = extractLastTokens(trimmed, maxTokens = 4)
        if (rawTokens.isEmpty()) return emptyList()

        val results = mutableListOf<GrammarSuggestion>()

        if (isBengaliFamily || containsBengali(trimmed)) {
            analyzeBengaliTokens(rawTokens, trailingWsLen, hasTrailingSpace, results)
        }

        if (!isBengaliFamily || rawTokens.last().core.all { it.code < 128 }) {
            analyzeEnglishTokens(rawTokens, trailingWsLen, hasTrailingSpace, results)
        }

        return results.distinctBy { it.correctedPhrase }
    }

    /**
     * Fast single-word safe auto-correction when pressing Space (avoids false positives).
     */
    fun autoCorrectWordOnSpace(word: String, languageId: LanguageId): String? {
        if (word.isBlank()) return null
        val isEnglish = languageId == LanguageId.ENGLISH || languageId == LanguageId.ENGLISH_UK
        if (isEnglish) {
            if (word == "i") return "I"
            val multi = englishMultiWordCorrections[word.lowercase()]
            if (multi != null) return preserveLeadingCase(word, multi)
            return OfflineSpellChecker.directCorrectionFor(word)
        }
        val isBengali = languageId in listOf(
            LanguageId.BANGLA_PROBHAT,
            LanguageId.BANGLA_NATIONAL,
            LanguageId.BANGLA_PHONETIC
        )
        if (isBengali) {
            // Only high-confidence unambiguous spelling corrections on Space
            val bnFix = bengaliSpellingFixes[word]
            if (bnFix != null && word != "ভাল") return bnFix
        }
        return null
    }

    private data class SpanToken(
        val raw: String,
        val leadingPunct: String,
        val core: String,
        val trailingPunct: String,
        val startInTrimmed: Int,
        val endInTrimmed: Int
    )

    private fun extractLastTokens(trimmed: String, maxTokens: Int): List<SpanToken> {
        val list = mutableListOf<SpanToken>()
        var idx = trimmed.length
        while (idx > 0 && list.size < maxTokens) {
            while (idx > 0 && trimmed[idx - 1].isWhitespace()) idx--
            if (idx <= 0) break
            val end = idx
            while (idx > 0 && !trimmed[idx - 1].isWhitespace()) idx--
            val start = idx
            val raw = trimmed.substring(start, end)
            val lead = raw.takeWhile { isPunctChar(it) }
            val trail = raw.drop(lead.length).takeLastWhile { isPunctChar(it) }
            val core = raw.substring(lead.length, raw.length - trail.length)
            list.add(0, SpanToken(raw, lead, core, trail, start, end))
        }
        return list
    }

    private fun isPunctChar(ch: Char): Boolean {
        return ch in charArrayOf(',', '.', '!', '?', ';', ':', '।', '"', '(', ')', '[', ']')
    }

    private fun analyzeEnglishTokens(
        tokens: List<SpanToken>,
        trailingWsLen: Int,
        hasTrailingSpace: Boolean,
        out: MutableList<GrammarSuggestion>
    ) {
        val totalLen = tokens.last().endInTrimmed + trailingWsLen
        val last = tokens.last()
        val lastLow = last.core.lowercase()

        // 1. Standalone single-word English fixes & spelling/contractions
        if (last.core == "i") {
            addFix(out, "I", "${last.leadingPunct}I${last.trailingPunct}", totalLen - last.startInTrimmed, hasTrailingSpace, "Capitalize pronoun 'I'")
        }

        englishMultiWordCorrections[lastLow]?.let { phrase ->
            val fixed = "${last.leadingPunct}${preserveLeadingCase(last.core, phrase)}${last.trailingPunct}"
            addFix(out, fixed, fixed, totalLen - last.startInTrimmed, hasTrailingSpace, "Spacing fix")
        }

        OfflineSpellChecker.directCorrectionFor(last.core)?.let { direct ->
            if (direct != last.core) {
                val fixed = "${last.leadingPunct}$direct${last.trailingPunct}"
                addFix(out, fixed, fixed, totalLen - last.startInTrimmed, hasTrailingSpace, "Spelling & contraction")
            }
        }

        if (tokens.size < 2) return
        val prev = tokens[tokens.size - 2]
        val prevLow = prev.core.lowercase()
        val twoWordSpanLen = totalLen - prev.startInTrimmed

        // Only evaluate multi-word pairs when there is no sentence break between prev and last
        val sentenceTerminated = prev.trailingPunct.any { it in charArrayOf('.', '!', '?', '।') }
        if (sentenceTerminated) return

        // 2. Duplicate consecutive word ("the the" -> "the")
        if (prevLow.isNotEmpty() && prevLow == lastLow && prevLow !in englishDuplicateSkip) {
            val fixed = "${prev.raw}${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Remove repeated word")
        }

        // 3. Article agreement: "a" vs "an"
        if (prevLow == "a" && lastLow.length >= 2 && lastLow.all { it.isLetter() } && needsAnArticle(lastLow)) {
            val article = if (prev.core.first().isUpperCase()) "An" else "an"
            val fixed = "${prev.leadingPunct}$article ${last.raw}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use 'an' before vowel sound")
        } else if (prevLow == "an" && lastLow.length >= 2 && lastLow.all { it.isLetter() } && !needsAnArticle(lastLow)) {
            val article = if (prev.core.first().isUpperCase()) "A" else "a"
            val fixed = "${prev.leadingPunct}$article ${last.raw}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use 'a' before consonant sound")
        }

        // 4. Subject-Verb agreement
        when {
            prevLow == "i" && (lastLow == "is" || lastLow == "are") -> {
                val fixed = "${prev.leadingPunct}I am${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow == "i" && lastLow == "has" -> {
                val fixed = "${prev.leadingPunct}I have${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("you", "we", "they") && lastLow == "is" -> {
                val fixed = "${prev.raw} are${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("you", "we", "they") && lastLow == "was" -> {
                val fixed = "${prev.raw} were${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("you", "we", "they") && lastLow == "has" -> {
                val fixed = "${prev.raw} have${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("he", "she", "it") && lastLow == "are" -> {
                val fixed = "${prev.raw} is${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("he", "she", "it") && lastLow == "were" -> {
                val fixed = "${prev.raw} was${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("he", "she", "it") && (lastLow == "don't" || lastLow == "dont") -> {
                val fixed = "${prev.raw} doesn't${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use 'doesn't' with $prevLow")
            }
            prevLow in setOf("he", "she", "it") && lastLow == "have" -> {
                val fixed = "${prev.raw} has${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("he", "she", "it") && lastLow == "go" -> {
                val fixed = "${prev.raw} goes${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
            prevLow in setOf("he", "she", "it") && lastLow == "do" -> {
                val fixed = "${prev.raw} does${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Subject-verb agreement")
            }
        }

        // 5. Tense & Auxiliary fixes ("didn't went" -> "didn't go", "to went" -> "to go")
        if (prevLow in setOf("didn't", "didnt", "can't", "cant", "won't", "wont", "shouldn't", "wouldn't", "couldn't", "to", "will", "can", "could", "would", "should", "must", "may", "might")) {
            val baseVerb = irregularPastToBase[lastLow]
            if (baseVerb != null) {
                val fixedAux = OfflineSpellChecker.directCorrectionFor(prev.core) ?: prev.core
                val fixed = "${prev.leadingPunct}$fixedAux $baseVerb${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use base verb after '$fixedAux'")
            } else if (lastLow == "goes" && prevLow != "to") {
                val fixedAux = OfflineSpellChecker.directCorrectionFor(prev.core) ?: prev.core
                val fixed = "${prev.leadingPunct}$fixedAux go${last.trailingPunct}"
                addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use base verb after '$fixedAux'")
            }
        }

        // 6. Double comparatives ("more better" -> "better") & "better then" -> "better than"
        if (prevLow == "more" && lastLow in doubleComparatives) {
            val fixed = "${prev.leadingPunct}${preserveLeadingCase(prev.core, last.core)}${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Avoid double comparative")
        }
        if (prevLow in comparativeThenTriggers && lastLow == "then") {
            val fixed = "${prev.raw} than${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use 'than' for comparisons")
        }
        if (prevLow == "your" && lastLow == "welcome") {
            val prefix = if (prev.core.first().isUpperCase()) "You're" else "you're"
            val fixed = "${prev.leadingPunct}$prefix ${last.raw}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use contraction \"you're\"")
        }
        if (prevLow == "their" && lastLow in setOf("is", "are", "was", "were")) {
            val prefix = if (prev.core.first().isUpperCase()) "There" else "there"
            val fixed = "${prev.leadingPunct}$prefix ${last.raw}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "Use 'there' here")
        }

        // 7. 3-word rules ("did not went" -> "did not go", "there is many" -> "there are many", "i am agree" -> "I agree")
        if (tokens.size >= 3) {
            val prev2 = tokens[tokens.size - 3]
            val prev2Low = prev2.core.lowercase()
            val threeWordSpanLen = totalLen - prev2.startInTrimmed
            val midTerminated = prev2.trailingPunct.any { it in charArrayOf('.', '!', '?', '।') }
            if (!midTerminated) {
                if (prev2Low in setOf("did", "do", "does", "will", "would", "could", "should") && prevLow == "not") {
                    irregularPastToBase[lastLow]?.let { baseVerb ->
                        val fixed = "${prev2.raw} not $baseVerb${last.trailingPunct}"
                        addFix(out, fixed, fixed, threeWordSpanLen, hasTrailingSpace, "Use base verb")
                    }
                }
                if (prev2Low == "there" && prevLow == "is" && lastLow in setOf("many", "several", "two", "three", "four", "lots", "some", "people")) {
                    val thereWord = if (prev2.core.first().isUpperCase()) "There" else "there"
                    val fixed = "${prev2.leadingPunct}$thereWord are ${last.raw}"
                    addFix(out, fixed, fixed, threeWordSpanLen, hasTrailingSpace, "Plural agreement")
                }
                if (prev2Low == "i" && prevLow == "am" && lastLow == "agree") {
                    val fixed = "${prev2.leadingPunct}I agree${last.trailingPunct}"
                    addFix(out, fixed, fixed, threeWordSpanLen, hasTrailingSpace, "Grammar fix")
                }
            }
        }
    }

    private fun analyzeBengaliTokens(
        tokens: List<SpanToken>,
        trailingWsLen: Int,
        hasTrailingSpace: Boolean,
        out: MutableList<GrammarSuggestion>
    ) {
        val totalLen = tokens.last().endInTrimmed + trailingWsLen
        val last = tokens.last()

        // 1. Standard Bangla Academy spelling improvements
        bengaliSpellingFixes[last.core]?.let { fixedWord ->
            if (fixedWord != last.core) {
                val fixed = "${last.leadingPunct}$fixedWord${last.trailingPunct}"
                addFix(out, fixed, fixed, totalLen - last.startInTrimmed, hasTrailingSpace, "শুদ্ধ বাংলা বানান")
            }
        }

        if (tokens.size < 2) return
        val prev = tokens[tokens.size - 2]
        val twoWordSpanLen = totalLen - prev.startInTrimmed
        val sentenceTerminated = prev.trailingPunct.any { it in charArrayOf('.', '!', '?', '।') }
        if (sentenceTerminated) return

        // 2. Bengali phrase compounds ("এক সাথে" -> "একসাথে", "অনেক গুলো" -> "অনেকগুলো")
        if (prev.core == "এক" && (last.core == "সাথে" || last.core == "সাতে")) {
            val fixed = "${prev.leadingPunct}একসাথে${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "একসাথে যুক্ত শব্দ")
        }
        if (prev.core == "অনেক" && (last.core == "গুলো" || last.core == "গুলা")) {
            val fixed = "${prev.leadingPunct}অনেকগুলো${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "শুদ্ধ রূপ")
        }

        // 3. Bengali "কি" vs "কী" context check
        if (prev.core == "কি" && last.core in setOf("ভাবে", "জন্য", "কারণে", "রকম", "ধরনের")) {
            val fixed = "${prev.leadingPunct}কী${last.core}${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "প্রশ্নবোধক 'কী'")
        }
        if (prev.core in setOf("নাম", "খবর", "অবস্থা", "ব্যাপার") && last.core == "কি") {
            val fixed = "${prev.raw} কী${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "বর্ণনামূলক প্রশ্ন 'কী'")
        }

        // 4. Bengali Subject-Verb honorific/person agreement (আমি / তুমি / আপনি / সে)
        val bnPairFix: String? = when (prev.core to last.core) {
            "আমি" to "আছো", "আমি" to "আছেন", "আমি" to "আছে" -> "আমি আছি"
            "আমি" to "যাও", "আমি" to "যান", "আমি" to "যাবেন" -> "আমি যাবো"
            "আমি" to "করো", "আমি" to "করুন", "আমি" to "করেন" -> "আমি করি"
            "তুমি" to "আছি", "তুমি" to "আছেন" -> "তুমি আছো"
            "তুমি" to "যান", "তুমি" to "যাবেন" -> "তুমি যাও"
            "তুমি" to "করুন", "তুমি" to "করেন" -> "তুমি করো"
            "আপনি" to "আছো", "আপনি" to "আছি", "আপনি" to "আছে" -> "আপনি আছেন"
            "আপনি" to "যাও", "আপনি" to "যাবি" -> "আপনি যান"
            "আপনি" to "করো", "আপনি" to "করি" -> "আপনি করুন"
            "সে" to "আছো", "সে" to "আছি" -> "সে আছে"
            "তারা" to "আছো", "তারা" to "আছি" -> "তারা আছে"
            else -> null
        }
        if (bnPairFix != null) {
            val fixed = "${prev.leadingPunct}$bnPairFix${last.trailingPunct}"
            addFix(out, fixed, fixed, twoWordSpanLen, hasTrailingSpace, "সঠিক ক্রিয়া ও সর্বনাম")
        }
    }

    private fun needsAnArticle(wordLower: String): Boolean {
        if (wordLower in vowelSoundExceptionsAn) return true
        if (consonantSoundStartsWithVowel.any { wordLower.startsWith(it) }) return false
        return wordLower.firstOrNull() in setOf('a', 'e', 'i', 'o', 'u')
    }

    private fun containsBengali(text: String): Boolean {
        return text.any { it.code in 0x0980..0x09FF }
    }

    private fun preserveLeadingCase(source: String, replacement: String): String {
        return if (source.firstOrNull()?.isUpperCase() == true) {
            replacement.replaceFirstChar { it.uppercaseChar() }
        } else replacement
    }

    private fun addFix(
        out: MutableList<GrammarSuggestion>,
        displayLabel: String,
        correctedPhrase: String,
        replaceCharLength: Int,
        trailingSpace: Boolean,
        reason: String
    ) {
        out.add(
            GrammarSuggestion(
                displayLabel = "✓ $displayLabel",
                correctedPhrase = correctedPhrase,
                replaceCharLength = replaceCharLength,
                trailingSpace = trailingSpace,
                reason = reason
            )
        )
    }
}
