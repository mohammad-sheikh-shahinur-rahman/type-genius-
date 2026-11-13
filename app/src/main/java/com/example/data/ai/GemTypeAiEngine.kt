package com.example.data.ai

import com.example.data.dictionary.OfflineGrammarEngine
import com.example.data.dictionary.OfflineSpellChecker
import com.example.data.phonetic.BengaliPhoneticParser
import com.example.data.translator.OfflineTranslator
import com.example.model.LanguageId
import org.json.JSONArray
import org.json.JSONObject

enum class GemTypeAction(val label: String, val resultLabel: String) {
    CHECK("Check", "Proofread"),
    FIX("Fix", "Suggested fix"),
    IMPROVE("Improve", "Improved"),
    SHORTEN("Shorten", "Shorter"),
    FORMAL("Formal", "More formal"),
    CASUAL("Casual", "More casual"),
    NATIVE("Native", "In native script"),
    ENGLISH("English", "In English")
}

enum class CorrectionCategory {
    GRAMMAR,
    SPELLING,
    PUNCTUATION,
    STYLE;

    companion object {
        fun parse(raw: String?): CorrectionCategory {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: STYLE
        }
    }
}

data class GemTypeCorrection(
    val original: String,
    val replacement: String,
    val explanation: String,
    val category: CorrectionCategory,
    val start: Int = -1,
    val end: Int = -1
)

data class GemTypeResult(
    val action: GemTypeAction,
    val originalText: String,
    val rewrittenText: String,
    val nativeReadAs: String? = null,
    val corrections: List<GemTypeCorrection> = emptyList(),
    val usedGeminiApi: Boolean = false,
    val statusNote: String = ""
)

/**
 * Brings the complete GemType-Keyboard AI feature set into TypeGenius:
 * - Proofreader (`CHECK`) with verbatim span diff + category + explanation + selective apply
 * - One-tap rewrites (`FIX`, `IMPROVE`, `SHORTEN`, `FORMAL`, `CASUAL`) + "Try another" (`avoid`)
 * - Romanized Banglish/Hinglish -> Native Script (`NATIVE`) & Clear English (`ENGLISH`) with "Read as" check
 * - Direct user-key Gemini API calls when a key is provided, plus an immediate on-device fallback when offline/keyless
 */
object GemTypeAiEngine {

    private val client = GeminiDirectClient()

    private const val FULL_STOPS =
        "\"।\" in Bangla, Assamese, Hindi, Nepali, Punjabi, Odia, Sanskrit and Maithili; \"۔\" in Urdu; " +
            "\".\" in English, Marathi, Gujarati, Tamil, Telugu, Kannada, Malayalam, Sinhala, Arabic and most others"

    private const val KEEP_REGISTER =
        "Keep the writer's spoken style and level of formality: casual spoken forms stay spoken, and the familiar or polite \"you\" stays the same."

    private const val NATURAL_RULES =
        "Write it naturally the way a fluent native speaker writes messages or emails. $KEEP_REGISTER " +
            "Use proper punctuation (sentence stops: $FULL_STOPS). Add nothing new. Keep names, URLs, numbers, emoji and line breaks."

    private val checkSchema: JSONObject by lazy {
        fun str() = JSONObject().put("type", "STRING")
        val item = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("original", str())
                    .put("replacement", str())
                    .put("explanation", str())
                    .put("type", str().put("enum", JSONArray(listOf("grammar", "spelling", "punctuation", "style"))))
            )
            .put("required", JSONArray(listOf("original", "replacement", "explanation", "type")))
        JSONObject()
            .put("type", "OBJECT")
            .put("properties", JSONObject().put("corrections", JSONObject().put("type", "ARRAY").put("items", item)))
            .put("required", JSONArray().put("corrections"))
    }

    private val rewriteSchema: JSONObject by lazy {
        JSONObject()
            .put("type", "OBJECT")
            .put("properties", JSONObject().put("rewritten", JSONObject().put("type", "STRING")))
            .put("required", JSONArray().put("rewritten"))
            .put("propertyOrdering", JSONArray().put("rewritten"))
    }

    private val layeredSchema: JSONObject by lazy {
        JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("native", JSONObject().put("type", "STRING"))
                    .put("rewritten", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray().put("native").put("rewritten"))
            .put("propertyOrdering", JSONArray().put("native").put("rewritten"))
    }

    suspend fun runAction(
        text: String,
        action: GemTypeAction,
        activeLanguage: LanguageId,
        apiKey: String,
        avoidPrevious: List<String> = emptyList()
    ): GemTypeResult {
        val clean = text.trim()
        if (clean.isEmpty()) {
            return GemTypeResult(
                action = action,
                originalText = text,
                rewrittenText = "",
                statusNote = "Type or select some text first"
            )
        }

        if (apiKey.isNotBlank()) {
            val remote = when (action) {
                GemTypeAction.CHECK -> runGeminiCheck(clean, activeLanguage, apiKey)
                else -> runGeminiRewrite(clean, action, activeLanguage, apiKey, avoidPrevious)
            }
            if (remote.isSuccess) {
                return remote.getOrThrow()
            }
            val errMsg = remote.exceptionOrNull()?.message ?: "Gemini unavailable"
            return runLocalFallback(clean, action, activeLanguage, "Offline fallback ($errMsg)")
        }

        return runLocalFallback(clean, action, activeLanguage, "On-Device Engine (Add Gemini API key in Settings for cloud AI)")
    }

    private suspend fun runGeminiCheck(
        text: String,
        languageId: LanguageId,
        apiKey: String
    ): Result<GemTypeResult> {
        val langName = languageId.displayName
        val langRule = when {
            languageId.code.startsWith("bn") || languageId == LanguageId.SYLHETI ->
                "Bangla: follow Bangla Academy standard spelling. Avoid mixing sadhu and cholito forms (তাহারা→তারা, করিয়া→করে). Sentences end with \"।\" not \".\"."
            languageId.code.startsWith("hi") ->
                "Hindi: check gender/number agreement, matra, nukta, chandrabindu, and end sentences with \"।\"."
            languageId.code.startsWith("ur") ->
                "Urdu: use Urdu punctuation (۔ ، ؟) and check verb/adjective agreement."
            else ->
                "English/Other: fix grammar, spelling, punctuation (its/it's, their/there/they're, your/you're, then/than, subject-verb, articles)."
        }
        val sys = "Proofread the text written in $langName. Return only real mistakes (never change correct text or invent facts). " +
            "$langRule Keep explanations to 3–8 words. Each 'original' must appear verbatim in the input text."

        val res = client.executeStructuredJsonPrompt(
            apiKey = apiKey,
            userText = "TEXT:\n$text",
            systemInstruction = sys,
            responseSchema = checkSchema,
            temperature = 0.0
        )
        return res.map { json ->
            val rawArray = json.optJSONArray("corrections") ?: JSONArray()
            val located = locateCorrections(text, rawArray)
            val fixedText = applyCorrections(text, located)
            GemTypeResult(
                action = GemTypeAction.CHECK,
                originalText = text,
                rewrittenText = fixedText,
                corrections = located,
                usedGeminiApi = true,
                statusNote = if (located.isEmpty()) "Gemini AI: No mistakes found" else "Gemini AI found ${located.size} fix(es)"
            )
        }
    }

    private suspend fun runGeminiRewrite(
        text: String,
        action: GemTypeAction,
        languageId: LanguageId,
        apiKey: String,
        avoid: List<String>
    ): Result<GemTypeResult> {
        val targetName = when {
            languageId.code.startsWith("bn") || languageId == LanguageId.SYLHETI -> "Bangla"
            languageId.code.startsWith("hi") -> "Hindi"
            languageId.code.startsWith("ur") -> "Urdu"
            else -> languageId.displayName.substringBefore(" (")
        }
        val avoidPrompt = when (avoid.size) {
            0 -> ""
            1 -> " Write a noticeably different version from: ${avoid.first()}."
            else -> " Write a different version from all of these: ${avoid.joinToString(" | ")}."
        }
        val useLayered = (action == GemTypeAction.ENGLISH || action == GemTypeAction.NATIVE) &&
            text.any { it in 'a'..'z' || it in 'A'..'Z' } &&
            targetName != "English"

        val taskPrompt = when (action) {
            GemTypeAction.FIX -> "Fix grammar, spelling, and punctuation with minimal edits. Preserve words, tone, and language. $NATURAL_RULES"
            GemTypeAction.IMPROVE -> "Make the text clearer and more natural at a similar length. Keep tone and language. $NATURAL_RULES"
            GemTypeAction.SHORTEN -> "Make the text concise and direct while keeping every key point and tone. $NATURAL_RULES"
            GemTypeAction.FORMAL -> "Rewrite in a polite, professional tone suited for work messages/emails. $NATURAL_RULES"
            GemTypeAction.CASUAL -> "Rewrite in a friendly, relaxed everyday chat tone. $NATURAL_RULES"
            GemTypeAction.NATIVE -> "If romanized ($targetName typed in English letters like Banglish/Hinglish), convert to correct, natural $targetName script with proper spelling and punctuation (Bangla short forms like tmr=তোমার, k=কে, vai=ভাই). $NATURAL_RULES"
            GemTypeAction.ENGLISH -> "Understand the message (including Banglish/Hinglish/short forms like tmr=your) and express it as fluent, natural English keeping the writer's meaning and tone."
            GemTypeAction.CHECK -> ""
        }

        val systemInstruction = if (useLayered) {
            "First interpret the romanized $targetName text (fixing typos & short forms like tmr=তোমার, vai=ভাই) and fill 'native' in accurate $targetName script. " +
                "Then fill 'rewritten' by doing this task: $taskPrompt$avoidPrompt Return only clean text in each JSON field."
        } else {
            "$taskPrompt$avoidPrompt Return only the resulting text inside JSON 'rewritten'."
        }

        val temp = when {
            avoid.isNotEmpty() -> 0.85
            action == GemTypeAction.FIX || action == GemTypeAction.NATIVE -> 0.05
            else -> 0.4
        }

        val res = client.executeStructuredJsonPrompt(
            apiKey = apiKey,
            userText = "TEXT:\n$text",
            systemInstruction = systemInstruction,
            responseSchema = if (useLayered) layeredSchema else rewriteSchema,
            temperature = temp
        )
        return res.map { json ->
            val rewritten = json.optString("rewritten", text).trim().ifBlank { text }
            val nativeReadAs = if (useLayered) json.optString("native").trim().ifBlank { null } else null
            GemTypeResult(
                action = action,
                originalText = text,
                rewrittenText = rewritten,
                nativeReadAs = nativeReadAs,
                usedGeminiApi = true,
                statusNote = "Gemini AI • ${action.resultLabel}"
            )
        }
    }

    fun applyCorrections(text: String, corrections: List<GemTypeCorrection>): String {
        val out = StringBuilder(text)
        var limit = text.length
        for (c in corrections.sortedByDescending { it.start }) {
            if (c.start < 0 || c.end > limit || c.start > c.end) continue
            if (text.substring(c.start, c.end) != c.original) continue
            out.replace(c.start, c.end, c.replacement)
            limit = c.start
        }
        return out.toString()
    }

    private fun locateCorrections(text: String, array: JSONArray): List<GemTypeCorrection> {
        val list = mutableListOf<GemTypeCorrection>()
        var searchFrom = 0
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val orig = obj.optString("original")
            val repl = obj.optString("replacement")
            val exp = obj.optString("explanation")
            val cat = CorrectionCategory.parse(obj.optString("type"))
            if (orig.isEmpty() || orig == repl) continue
            var pos = text.indexOf(orig, startIndex = searchFrom)
            if (pos < 0) pos = text.indexOf(orig)
            if (pos >= 0) {
                list.add(
                    GemTypeCorrection(
                        original = orig,
                        replacement = repl,
                        explanation = exp.ifBlank { "Suggested fix" },
                        category = cat,
                        start = pos,
                        end = pos + orig.length
                    )
                )
                searchFrom = (pos + orig.length).coerceAtMost(text.length)
            }
        }
        return list
    }

    private fun runLocalFallback(
        text: String,
        action: GemTypeAction,
        languageId: LanguageId,
        note: String
    ): GemTypeResult {
        return when (action) {
            GemTypeAction.CHECK -> {
                val corrections = collectLocalCorrections(text, languageId)
                val fixed = applyCorrections(text, corrections)
                GemTypeResult(
                    action = action,
                    originalText = text,
                    rewrittenText = fixed,
                    corrections = corrections,
                    usedGeminiApi = false,
                    statusNote = if (corrections.isEmpty()) "On-Device Check: Looks clean" else "On-Device Check: ${corrections.size} fix(es)"
                )
            }
            GemTypeAction.FIX, GemTypeAction.IMPROVE -> {
                val corrections = collectLocalCorrections(text, languageId)
                val fixed = applyCorrections(text, corrections).replace(Regex("\\s{2,}"), " ").trim()
                GemTypeResult(
                    action = action,
                    originalText = text,
                    rewrittenText = fixed.ifBlank { text },
                    usedGeminiApi = false,
                    statusNote = note
                )
            }
            GemTypeAction.SHORTEN -> {
                val fillers = setOf("basically", "actually", "literally", "really", "just", "very", "quite", "kind of", "sort of")
                val shortened = text.split(Regex("\\s+"))
                    .filterNot { it.lowercase().trim(',', '.', '!') in fillers }
                    .joinToString(" ")
                    .ifBlank { text }
                GemTypeResult(action, text, shortened, usedGeminiApi = false, statusNote = note)
            }
            GemTypeAction.FORMAL -> {
                val formalMap = listOf(
                    Regex("\\bi'm\\b", RegexOption.IGNORE_CASE) to "I am",
                    Regex("\\bdon't\\b", RegexOption.IGNORE_CASE) to "do not",
                    Regex("\\bdoesn't\\b", RegexOption.IGNORE_CASE) to "does not",
                    Regex("\\bdidn't\\b", RegexOption.IGNORE_CASE) to "did not",
                    Regex("\\bcan't\\b", RegexOption.IGNORE_CASE) to "cannot",
                    Regex("\\bwon't\\b", RegexOption.IGNORE_CASE) to "will not",
                    Regex("\\byou're\\b", RegexOption.IGNORE_CASE) to "you are",
                    Regex("\\bhey\\b", RegexOption.IGNORE_CASE) to "Hello",
                    Regex("\\bthanks\\b", RegexOption.IGNORE_CASE) to "Thank you",
                    Regex("\\bpls\\b|\\bplz\\b", RegexOption.IGNORE_CASE) to "please"
                )
                var result = applyCorrections(text, collectLocalCorrections(text, languageId))
                formalMap.forEach { (rx, rep) -> result = rx.replace(result, rep) }
                GemTypeResult(action, text, result, usedGeminiApi = false, statusNote = note)
            }
            GemTypeAction.CASUAL -> {
                val casualMap = listOf(
                    Regex("\\bI am\\b") to "I'm",
                    Regex("\\bdo not\\b", RegexOption.IGNORE_CASE) to "don't",
                    Regex("\\bdoes not\\b", RegexOption.IGNORE_CASE) to "doesn't",
                    Regex("\\bdid not\\b", RegexOption.IGNORE_CASE) to "didn't",
                    Regex("\\bcannot\\b", RegexOption.IGNORE_CASE) to "can't",
                    Regex("\\bwill not\\b", RegexOption.IGNORE_CASE) to "won't",
                    Regex("\\byou are\\b", RegexOption.IGNORE_CASE) to "you're",
                    Regex("\\bthank you\\b", RegexOption.IGNORE_CASE) to "thanks"
                )
                var result = text
                casualMap.forEach { (rx, rep) -> result = rx.replace(result, rep) }
                GemTypeResult(action, text, result, usedGeminiApi = false, statusNote = note)
            }
            GemTypeAction.NATIVE -> {
                val nativeText = BengaliPhoneticParser.transliterateSentenceToBengali(text)
                GemTypeResult(
                    action = action,
                    originalText = text,
                    rewrittenText = nativeText,
                    usedGeminiApi = false,
                    statusNote = note
                )
            }
            GemTypeAction.ENGLISH -> {
                val src = if (text.any { it.code in 0x0980..0x09FF }) "bn" else "banglish"
                val nativeReadAs = if (src == "banglish") BengaliPhoneticParser.transliterateSentenceToBengali(text) else null
                val translated = OfflineTranslator.translate(text, src, "en").translatedText
                GemTypeResult(
                    action = action,
                    originalText = text,
                    rewrittenText = translated.ifBlank { text },
                    nativeReadAs = nativeReadAs,
                    usedGeminiApi = false,
                    statusNote = note
                )
            }
        }
    }

    /**
     * Collects multiple non-overlapping local grammar/spelling/punctuation fixes across a sentence.
     */
    fun collectLocalCorrections(text: String, languageId: LanguageId): List<GemTypeCorrection> {
        val corrections = mutableListOf<GemTypeCorrection>()
        // Scan word-by-word sliding prefix slices using OfflineGrammarEngine + OfflineSpellChecker
        val tokenRegex = Regex("\\S+")
        val matches = tokenRegex.findAll(text).toList()
        var coveredUntil = 0

        for (i in matches.indices) {
            val match = matches[i]
            if (match.range.first < coveredUntil) continue

            val prefixSlice = text.substring(0, match.range.last + 1)
            val contextFixes = OfflineGrammarEngine.analyzeContext(prefixSlice, languageId)
            val fix = contextFixes.firstOrNull()
            if (fix != null && fix.replaceCharLength <= prefixSlice.length) {
                val start = prefixSlice.length - fix.replaceCharLength
                val end = prefixSlice.length
                val origSpan = text.substring(start, end)
                if (start >= coveredUntil && origSpan != fix.correctedPhrase) {
                    val cat = when {
                        fix.reason.contains("Spell", ignoreCase = true) || fix.reason.contains("বানান") -> CorrectionCategory.SPELLING
                        else -> CorrectionCategory.GRAMMAR
                    }
                    corrections.add(
                        GemTypeCorrection(
                            original = origSpan,
                            replacement = fix.correctedPhrase,
                            explanation = fix.reason,
                            category = cat,
                            start = start,
                            end = end
                        )
                    )
                    coveredUntil = end
                    continue
                }
            }

            // Fallback per-word typo check
            val rawWord = match.value
            val cleanWord = rawWord.trim(',', '.', '!', '?', ';', ':', '।', '"')
            val direct = OfflineSpellChecker.directCorrectionFor(cleanWord)
            if (direct != null && direct != cleanWord && match.range.first >= coveredUntil) {
                val innerOffset = rawWord.indexOf(cleanWord).coerceAtLeast(0)
                val start = match.range.first + innerOffset
                val end = start + cleanWord.length
                corrections.add(
                    GemTypeCorrection(
                        original = cleanWord,
                        replacement = direct,
                        explanation = "Spelling fix",
                        category = CorrectionCategory.SPELLING,
                        start = start,
                        end = end
                    )
                )
                coveredUntil = end
            }
        }
        return corrections
    }
}
