package com.example.ui.keyboard

import com.example.model.KeyAction
import com.example.model.KeyType
import com.example.model.LanguageId

object KeyboardLayouts {

    val numberRow = listOf(
        KeyAction("1", null, KeyType.CHARACTER),
        KeyAction("2", null, KeyType.CHARACTER),
        KeyAction("3", null, KeyType.CHARACTER),
        KeyAction("4", null, KeyType.CHARACTER),
        KeyAction("5", null, KeyType.CHARACTER),
        KeyAction("6", null, KeyType.CHARACTER),
        KeyAction("7", null, KeyType.CHARACTER),
        KeyAction("8", null, KeyType.CHARACTER),
        KeyAction("9", null, KeyType.CHARACTER),
        KeyAction("0", null, KeyType.CHARACTER)
    )

    fun getNumberRow(language: LanguageId): List<KeyAction> {
        return when (language) {
            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL, LanguageId.BANGLA_UNIBIJOY, LanguageId.SYLHETI, LanguageId.ASSAMESE ->
                listOf("১" to "1", "২" to "2", "৩" to "3", "৪" to "4", "৫" to "5", "৬" to "6", "৭" to "7", "৮" to "8", "৯" to "9", "০" to "0")
                    .map { (bn, en) -> KeyAction(bn, en, KeyType.CHARACTER) }
            LanguageId.HINDI, LanguageId.NEPALI, LanguageId.MARATHI, LanguageId.SANSKRIT ->
                listOf("१" to "1", "२" to "2", "३" to "3", "४" to "4", "५" to "5", "६" to "6", "७" to "7", "८" to "8", "९" to "9", "०" to "0")
                    .map { (hi, en) -> KeyAction(hi, en, KeyType.CHARACTER) }
            LanguageId.ARABIC, LanguageId.URDU, LanguageId.PERSIAN ->
                listOf("١" to "1", "٢" to "2", "٣" to "3", "٤" to "4", "٥" to "5", "٦" to "6", "٧" to "7", "٨" to "8", "٩" to "9", "٠" to "0")
                    .map { (ar, en) -> KeyAction(ar, en, KeyType.CHARACTER) }
            else -> numberRow
        }
    }

    fun getAlphabetRows(language: LanguageId, isShifted: Boolean): List<List<KeyAction>> {
        return when (language) {
            LanguageId.BANGLA_PROBHAT -> getBengaliProbhatRows(isShifted)
            LanguageId.BANGLA_NATIONAL -> getBengaliNationalRows(isShifted)
            LanguageId.BANGLA_UNIBIJOY -> getBengaliUnibijoyRows(isShifted)
            LanguageId.SYLHETI -> getSylhetiRows(isShifted)
            LanguageId.ASSAMESE -> getAssameseRows(isShifted)
            LanguageId.BANGLA_PHONETIC, LanguageId.ENGLISH, LanguageId.ENGLISH_UK,
            LanguageId.INDONESIAN, LanguageId.MALAY -> getLatinQwertyRows(isShifted)
            LanguageId.SPANISH -> getSpanishRows(isShifted)
            LanguageId.FRENCH -> getFrenchRows(isShifted)
            LanguageId.GERMAN -> getGermanRows(isShifted)
            LanguageId.PORTUGUESE -> getPortugueseRows(isShifted)
            LanguageId.ITALIAN -> getItalianRows(isShifted)
            LanguageId.TURKISH -> getTurkishRows(isShifted)
            LanguageId.HINDI, LanguageId.NEPALI, LanguageId.MARATHI, LanguageId.SANSKRIT -> getDevanagariRows(isShifted)
            LanguageId.ARABIC -> getArabicRows(isShifted)
            LanguageId.URDU -> getUrduRows(isShifted)
            LanguageId.PERSIAN -> getPersianRows(isShifted)
            LanguageId.HEBREW -> getHebrewRows(isShifted)
            LanguageId.TAMIL -> getTamilRows(isShifted)
            LanguageId.TELUGU -> getTeluguRows(isShifted)
            LanguageId.GUJARATI -> getGujaratiRows(isShifted)
            LanguageId.PUNJABI -> getPunjabiRows(isShifted)
            LanguageId.MALAYALAM -> getMalayalamRows(isShifted)
            LanguageId.KANNADA -> getKannadaRows(isShifted)
            LanguageId.RUSSIAN -> getRussianRows(isShifted)
            LanguageId.KOREAN -> getKoreanRows(isShifted)
            LanguageId.JAPANESE -> getJapaneseKanaRows(isShifted)
        }
    }

    private fun buildPairedLayout(
        r1: List<Pair<String, String>>,
        r2: List<Pair<String, String>>,
        r3: List<Pair<String, String>>,
        isShifted: Boolean,
        shiftWeight: Float = 1.25f
    ): List<List<KeyAction>> {
        fun mapPair(p: Pair<String, String>) =
            if (isShifted) KeyAction(p.second, p.first, KeyType.CHARACTER)
            else KeyAction(p.first, p.second, KeyType.CHARACTER)

        return listOf(
            r1.map(::mapPair),
            r2.map(::mapPair),
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = shiftWeight))
                addAll(r3.map(::mapPair))
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = shiftWeight))
            }
        )
    }

    private val bengaliExtraVariants: Map<String, List<String>> = mapOf(
        "র" to listOf("ড়", "\u09CD\u09B0", "\u09B0\u09CD", "ৃ", "ঋ"),
        "ড়" to listOf("র", "\u09CD\u09B0", "\u09B0\u09CD", "ঢ়"),
        "য়" to listOf("য", "\u09CD\u09AF", "\u09CD\u09AF\u09BE"),
        "য" to listOf("য়", "\u09CD\u09AF", "\u09CD\u09AF\u09BE"),
        "া" to listOf("অ", "আ", "\u09CD\u09AF\u09BE"),
        "আ" to listOf("ঋ", "ৃ", "\u0985\u09CD\u09AF\u09BE"),
        "ত" to listOf("থ", "ৎ", "\u09A4\u09CD\u09B0", "\u09A4\u09CD\u09AC"),
        "জ" to listOf("ঝ", "ঞ", "\u099C\u09CD\u099E"),
        "চ" to listOf("ছ", "ঞ", "\u099A\u09CD\u099B"),
        "ন" to listOf("ণ", "ঞ", "\u09CD\u09A8", "\u09A8\u09CD\u09A4", "\u09A8\u09CD\u09A6"),
        "ক" to listOf("খ", "\u0995\u09CD\u09B7", "\u0995\u09CD\u09B0", "\u0995\u09CD\u09A4"),
        "ব" to listOf("ভ", "\u09CD\u09AC"),
        "ম" to listOf("ঙ", "\u09CD\u09AE"),
        "ল" to listOf("ং", "\u09CD\u09B2", "ৎ"),
        "শ" to listOf("ঢ়", "\u09B6\u09CD\u09B0", "\u09B6\u09CD\u09AC"),
        "স" to listOf("ষ", "\u09B8\u09CD\u09A4", "\u09B8\u09CD\u09AC", "\u09B8\u09CD\u09B0"),
        "হ" to listOf("ঃ", "\u09B9\u09CD\u09AE", "\u09B9\u09CD\u09AF", "\u09B9\u09CD\u09B0"),
        "ে" to listOf("ৈ", "এ", "ঐ"),
        "ো" to listOf("ৌ", "ও", "ঔ"),
        "ু" to listOf("উ", "ূ", "ৃ"),
        "ূ" to listOf("ঊ", "ু", "ৃ"),
        "ি" to listOf("ই", "ী"),
        "ী" to listOf("ঈ", "ি"),
        "ৃ" to listOf("\u09CD\u09B0", "\u09CD\u09AF", "\u09B0\u09CD", "ঋ", "ঞ", "ৎ", ","),
        "\u09CD\u09B0" to listOf("ৃ", "\u09CD\u09AF", "\u09B0\u09CD", "ঋ", "ঞ", "ৎ"),
        "্" to listOf("ঁ", "\u09CD\u09B0", "\u09CD\u09AF", "\u09B0\u09CD", "\u09CD\u09AC", "\u09CD\u09AE", "\u09CD\u09A8", "\u09CD\u09B2"),
        "ফ" to listOf("প", "ঞ", "\u09AB\u09CD\u09B0"),
        "ৈ" to listOf("ৌ", "ঐ", "ঔ"),
        "ং" to listOf("ৎ", "ঃ", "ঁ")
    )

    private fun buildBengaliPairedLayout(
        r1: List<Pair<String, String>>,
        r2: List<Pair<String, String>>,
        r3: List<Pair<String, String>>,
        isShifted: Boolean,
        shiftWeight: Float = 1.25f
    ): List<List<KeyAction>> {
        fun mapPair(p: Pair<String, String>): KeyAction {
            val primary = if (isShifted) p.second else p.first
            val secondary = if (isShifted) p.first else p.second
            val variants = buildList {
                add(secondary)
                bengaliExtraVariants[primary]?.forEach { if (!contains(it)) add(it) }
            }
            return KeyAction(
                label = primary,
                secondaryLabel = secondary,
                type = KeyType.CHARACTER,
                extraVariants = if (variants.size > 1) variants else emptyList()
            )
        }

        return listOf(
            r1.map(::mapPair),
            r2.map(::mapPair),
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = shiftWeight))
                addAll(r3.map(::mapPair))
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = shiftWeight))
            }
        )
    }

    private fun getBengaliProbhatRows(isShifted: Boolean) = buildBengaliPairedLayout(
        r1 = listOf("দ" to "ধ", "ূ" to "ঊ", "ী" to "ঈ", "র" to "ড়", "ট" to "ঠ", "এ" to "ঐ", "ু" to "উ", "ি" to "ই", "ও" to "ঔ", "প" to "ফ", "ে" to "ৈ", "ো" to "ৌ"),
        r2 = listOf("া" to "অ", "স" to "ষ", "ড" to "ঢ", "ত" to "থ", "গ" to "ঘ", "হ" to "ঃ", "জ" to "ঝ", "ক" to "খ", "ল" to "ং"),
        r3 = listOf("য়" to "য", "শ" to "ঢ়", "চ" to "ছ", "আ" to "ঋ", "ব" to "ভ", "ন" to "ণ", "ম" to "ঙ", "ৃ" to "\u09CD\u09B0", "্" to "ঁ"),
        isShifted = isShifted
    )

    private fun getAssameseRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("দ" to "ধ", "ূ" to "ঊ", "ী" to "ঈ", "ৰ" to "ড়", "ট" to "ঠ", "এ" to "ঐ", "ু" to "উ", "ি" to "ই", "ও" to "ঔ", "প" to "ফ", "ে" to "ৈ", "ো" to "ৌ"),
        r2 = listOf("া" to "অ", "স" to "ষ", "ড" to "ঢ", "ত" to "থ", "গ" to "ঘ", "হ" to "ঃ", "জ" to "ঝ", "ক" to "খ", "ল" to "ং"),
        r3 = listOf("য়" to "য", "শ" to "ঢ়", "চ" to "ছ", "আ" to "ঋ", "ব" to "ৱ", "ন" to "ণ", "ম" to "ঙ", "ৃ" to "\u09CD\u09B0", "্" to "ঁ"),
        isShifted = isShifted
    )

    // Sylheti keyboard: Syloti Nagri (ꠍꠤꠟꠐꠤ ꠘꠣꠉꠞꠤ) paired with Bengali-script Sylheti equivalents on long-press / shift
    private fun getSylhetiRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ꠖ" to "দ", "ꠗ" to "ধ", "ꠞ" to "র", "ꠐ" to "ট", "ꠑ" to "ঠ", "ꠄ" to "এ", "ꠥ" to "ু", "ꠤ" to "ি", "ꠅ" to "ও", "ꠙ" to "প", "ꠦ" to "ে", "ꠧ" to "ো"),
        r2 = listOf("ꠣ" to "া", "ꠡ" to "স", "ꠒ" to "ড", "ꠔ" to "ত", "ꠕ" to "থ", "ꠉ" to "গ", "ꠢ" to "হ", "ꠎ" to "জ", "ꠇ" to "ক", "ꠟ" to "ল"),
        r3 = listOf("ꠀ" to "আ", "ꠍ" to "ছ", "ꠌ" to "চ", "ꠛ" to "ব", "ꠜ" to "ভ", "ꠘ" to "ন", "ꠝ" to "ম", "ꠈ" to "খ", "ꠚ" to "ফ"),
        isShifted = isShifted
    )

    private fun getBengaliNationalRows(isShifted: Boolean) = buildBengaliPairedLayout(
        r1 = listOf("ড়" to "ঢ়", "ই" to "ঈ", "উ" to "ঊ", "র" to "ঋ", "ত" to "থ", "য" to "য়", "ব" to "ভ", "হ" to "ঙ", "গ" to "ঘ", "দ" to "ধ", "ড" to "ঢ"),
        r2 = listOf("া" to "অ", "ি" to "আ", "ে" to "এ", "ো" to "ও", "ু" to "ী", "প" to "ূ", "ক" to "খ", "জ" to "ঝ", "ট" to "ঠ", "চ" to "ছ"),
        r3 = listOf("ং" to "ৎ", "্" to "ঃ", "ম" to "ঁ", "ন" to "ণ", "স" to "ষ", "ল" to "শ", "ফ" to "ঞ", "ৃ" to "\u09CD\u09B0", "ৈ" to "ৌ"),
        isShifted = isShifted,
        shiftWeight = 1.25f
    )

    // UniBijoy classic mobile-adapted layout: full vowels, kars, consonants, pholas, signs
    private fun getBengaliUnibijoyRows(isShifted: Boolean) = buildBengaliPairedLayout(
        r1 = listOf("ং" to "ঙ", "য়" to "য", "ড" to "ঢ", "প" to "ফ", "ট" to "ঠ", "চ" to "ছ", "জ" to "ঝ", "হ" to "ঞ", "ো" to "ৌ", "ৃ" to "\u09CD\u09B0"),
        r2 = listOf("া" to "অ", "ু" to "উ", "ি" to "ই", "র" to "ড়", "্" to "আ", "ত" to "থ", "ক" to "খ", "ল" to "ঃ", "ে" to "ঐ", "এ" to "ও"),
        r3 = listOf("ঁ" to "ঢ়", "স" to "ষ", "দ" to "ধ", "ন" to "ণ", "ব" to "ভ", "ম" to "শ", "গ" to "ঘ", "ী" to "ঈ", "ূ" to "ঊ", "ৈ" to "ঔ", "ঋ" to "ৎ"),
        isShifted = isShifted,
        shiftWeight = 1.2f
    )

    private fun getLatinQwertyRows(
        isShifted: Boolean,
        extraAccents: Map<String, String> = emptyMap()
    ): List<List<KeyAction>> {
        val row1 = listOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0")
        val row2 = listOf("a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&", "h" to "-", "j" to "+", "k" to "(", "l" to ")")
        val row3 = listOf("z" to "*", "x" to "\"", "c" to "Copy", "v" to "Paste", "b" to ";", "n" to "!", "m" to "?")

        fun toKey(char: String, fallbackSec: String): KeyAction {
            val text = if (isShifted) char.uppercase() else char
            if (char == "c" && extraAccents.containsKey("c")) {
                val accent = extraAccents["c"]!!.let { if (isShifted) it.uppercase() else it }
                return KeyAction(text, "Copy", KeyType.CHARACTER, extraVariants = listOf("Copy", accent))
            }
            if (char == "c") {
                return KeyAction(text, "Copy", KeyType.CHARACTER, extraVariants = listOf("Copy", "'"))
            }
            if (char == "v") {
                return KeyAction(text, "Paste", KeyType.CHARACTER, extraVariants = listOf("Paste", ":"))
            }
            val sec = extraAccents[char]?.let { if (isShifted) it.uppercase() else it } ?: fallbackSec
            return KeyAction(text, sec, KeyType.CHARACTER)
        }

        return listOf(
            row1.map { (c, s) -> toKey(c, s) },
            row2.map { (c, s) -> toKey(c, s) },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.3f))
                addAll(row3.map { (c, s) -> toKey(c, s) })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
            }
        )
    }

    private fun getSpanishRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("a" to "á", "e" to "é", "i" to "í", "o" to "ó", "u" to "ú", "n" to "ñ")
    )

    private fun getFrenchRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("a" to "à", "e" to "é", "i" to "î", "o" to "ô", "u" to "ù", "c" to "ç")
    )

    private fun getGermanRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("a" to "ä", "o" to "ö", "u" to "ü", "s" to "ß")
    )

    private fun getPortugueseRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("a" to "ã", "e" to "ê", "i" to "í", "o" to "õ", "u" to "ú", "c" to "ç")
    )

    private fun getItalianRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("a" to "à", "e" to "è", "i" to "ì", "o" to "ò", "u" to "ù")
    )

    private fun getTurkishRows(isShifted: Boolean) = getLatinQwertyRows(
        isShifted,
        mapOf("g" to "ğ", "u" to "ü", "s" to "ş", "i" to "ı", "o" to "ö", "c" to "ç")
    )

    private fun getDevanagariRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ौ" to "औ", "ै" to "ऐ", "ा" to "आ", "ी" to "ई", "ू" to "ऊ", "ब" to "भ", "ह" to "ङ", "ग" to "घ", "द" to "ध", "ज" to "झ"),
        r2 = listOf("ो" to "ओ", "े" to "ए", "्" to "अ", "ि" to "इ", "ु" to "उ", "प" to "फ", "र" to "ख", "क" to "थ", "त" to "छ", "च" to "ठ", "ट" to "ढ"),
        r3 = listOf("ं" to "ँ", "म" to "ण", "न" to "ष", "व" to "श", "ल" to "ळ", "स" to "ज्ञ", "य" to "क्ष"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getArabicRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ض" to "َ", "ص" to "ً", "ث" to "ُ", "ق" to "ٌ", "ف" to "لإ", "غ" to "إ", "ع" to "‘", "ه" to "÷", "خ" to "×", "ح" to "؛", "ج" to "<"),
        r2 = listOf("ش" to "ِ", "س" to "ٍ", "ي" to "]", "ب" to "[", "ل" to "لأ", "ا" to "أ", "ت" to "ـ", "ن" to "،", "م" to "/", "ك" to ":", "ط" to "\""),
        r3 = listOf("ئ" to "~", "ء" to "ْ", "ؤ" to "}", "ر" to "{", "لا" to "لآ", "ى" to "آ", "ة" to "’", "و" to ",", "ز" to ".", "ظ" to "؟"),
        isShifted = isShifted,
        shiftWeight = 1.15f
    )

    private fun getUrduRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ق" to "ْ", "و" to "ؤ", "ع" to "ء", "ر" to "ڑ", "ت" to "ٹ", "ے" to "ۓ", "ی" to "ئ", "ہ" to "ھ", "پ" to "ُ", "ج" to "چ"),
        r2 = listOf("ا" to "آ", "س" to "ش", "د" to "ڈ", "ف" to "گ", "غ" to "خ", "ح" to "ج", "ک" to "خ", "ل" to "ض", "م" to "ظ"),
        r3 = listOf("ز" to "ژ", "ط" to "ذ", "ص" to "ث", "ب" to "پ", "ن" to "ں", "ت" to "ط", "۔" to "؟"),
        isShifted = isShifted,
        shiftWeight = 1.25f
    )

    private fun getPersianRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ض" to "ْ", "ص" to "ٌ", "ث" to "ٍ", "ق" to "ً", "ف" to "ُ", "غ" to "ِ", "ع" to "َ", "ه" to "ّ", "خ" to "]", "ح" to "[", "ج" to "چ"),
        r2 = listOf("ش" to "ؤ", "س" to "ئ", "ی" to "ي", "ب" to "إ", "ل" to "أ", "ا" to "آ", "ت" to "ة", "ن" to "»", "م" to "«", "ک" to "؛", "گ" to "پ"),
        r3 = listOf("ظ" to "ژ", "ط" to "ذ", "ز" to "ر", "ر" to "ز", "ذ" to "ژ", "د" to "و", "پ" to "؟", "و" to "،"),
        isShifted = isShifted,
        shiftWeight = 1.2f
    )

    private fun getHebrewRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ק" to "1", "ר" to "2", "א" to "3", "ט" to "4", "ו" to "5", "ן" to "6", "ם" to "7", "פ" to "8"),
        r2 = listOf("ש" to "9", "ד" to "0", "ג" to "@", "כ" to "#", "ע" to "$", "י" to "%", "ח" to "&", "ל" to "-", "ך" to "+"),
        r3 = listOf("ז" to "*", "ס" to "\"", "ב" to "'", "ה" to ":", "נ" to ";", "מ" to "!", "צ" to "?", "ת" to ",", "ץ" to "."),
        isShifted = isShifted,
        shiftWeight = 1.15f
    )

    private fun getTamilRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ஆ" to "அ", "ஈ" to "இ", "ஊ" to "உ", "ஐ" to "எ", "ஏ" to "ஒ", "ஓ" to "ஔ", "ப" to "ஃ", "ம" to "ற", "த" to "ந", "ன" to "ண"),
        r2 = listOf("ா" to "க", "ி" to "ச", "ீ" to "ட", "ு" to "த", "ூ" to "ப", "ெ" to "ய", "ே" to "ர", "ை" to "ல", "ொ" to "வ", "ோ" to "ழ"),
        r3 = listOf("்" to "ள", "ஞ" to "ங", "ஷ" to "ஸ", "ஜ" to "ஹ", "ஶ" to "க்ஷ", "ழ" to "ன"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getTeluguRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ా" to "ఆ", "ి" to "ఇ", "ీ" to "ఈ", "ు" to "ఉ", "ూ" to "ఊ", "బ" to "భ", "హ" to "ఙ", "గ" to "ఘ", "ద" to "ధ", "జ" to "ఝ"),
        r2 = listOf("ో" to "ఓ", "ే" to "ఏ", "్" to "అ", "ె" to "ఎ", "ొ" to "ఒ", "ప" to "ఫ", "ర" to "ఖ", "క" to "థ", "త" to "ఛ", "చ" to "ఠ"),
        r3 = listOf("ం" to "ః", "మ" to "ణ", "న" to "ష", "వ" to "శ", "ల" to "ళ", "స" to "య", "ట" to "డ"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getGujaratiRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ૌ" to "ઔ", "ૈ" to "ઐ", "ા" to "આ", "ી" to "ઈ", "ૂ" to "ઊ", "બ" to "ભ", "હ" to "ઙ", "ગ" to "ઘ", "દ" to "ધ", "જ" to "ઝ"),
        r2 = listOf("ો" to "ઓ", "ે" to "એ", "્" to "અ", "િ" to "ઇ", "ુ" to "ઉ", "પ" to "ફ", "ર" to "ખ", "ક" to "થ", "ત" to "છ", "ચ" to "ઠ"),
        r3 = listOf("ં" to "ઁ", "મ" to "ણ", "ન" to "ષ", "વ" to "શ", "લ" to "ળ", "સ" to "જ્ઞ", "ય" to "ક્ષ"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getPunjabiRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ੌ" to "ਔ", "ੈ" to "ਐ", "ਾ" to "ਆ", "ੀ" to "ਈ", "ੂ" to "ਊ", "ਬ" to "ਭ", "ਹ" to "ਙ", "ਗ" to "ਘ", "ਦ" to "ਧ", "ਜ" to "ਝ"),
        r2 = listOf("ੋ" to "ਓ", "ੇ" to "ਏ", "੍" to "ਅ", "ਿ" to "ਇ", "ੁ" to "ਉ", "ਪ" to "ਫ", "ਰ" to "ਖ", "ਕ" to "ਥ", "ਤ" to "ਛ", "ਚ" to "ਠ"),
        r3 = listOf("ਂ" to "ੰ", "ਮ" to "ਣ", "ਨ" to "ਸ਼", "ਵ" to "ਸ", "ਲ" to "ਲ਼", "ਟ" to "ਠ", "ਯ" to "ੜ"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getMalayalamRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ൗ" to "ഔ", "ൈ" to "ഐ", "ാ" to "ആ", "ീ" to "ഈ", "ൂ" to "ഊ", "ബ" to "ഭ", "ഹ" to "ങ", "ഗ" to "ഘ", "ദ" to "ധ", "ജ" to "ഝ"),
        r2 = listOf("ോ" to "ഓ", "േ" to "ഏ", "്" to "അ", "ി" to "ഇ", "ു" to "ഉ", "പ" to "ഫ", "ര" to "ഖ", "ക" to "ഥ", "ത" to "ഛ", "ച" to "ഠ"),
        r3 = listOf("ം" to "ഃ", "മ" to "ണ", "ന" to "ഷ", "വ" to "ശ", "ല" to "ള", "സ" to "ഴ", "യ" to "റ"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getKannadaRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ೌ" to "ಔ", "ೈ" to "ಐ", "ಾ" to "ಆ", "ೀ" to "ಈ", "ೂ" to "ಊ", "ಬ" to "ಭ", "ಹ" to "ಙ", "ಗ" to "ಘ", "ದ" to "ಧ", "ಜ" to "ಝ"),
        r2 = listOf("ೋ" to "ಓ", "ೇ" to "ಏ", "್" to "ಅ", "ಿ" to "ಇ", "ು" to "ಉ", "ಪ" to "ಫ", "ರ" to "ಖ", "ಕ" to "ಥ", "ತ" to "ಛ", "ಚ" to "ಠ"),
        r3 = listOf("ಂ" to "ಃ", "ಮ" to "ಣ", "ನ" to "ಷ", "ವ" to "ಶ", "ಲ" to "ಳ", "ಸ" to "ಯ", "ಟ" to "ಡ"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getRussianRows(isShifted: Boolean): List<List<KeyAction>> {
        val r1 = listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х")
        val r2 = listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э")
        val r3 = listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю")
        fun mapK(c: String, i: Int): KeyAction {
            val t = if (isShifted) c.uppercase() else c
            return KeyAction(t, if (i < 10) ((i + 1) % 10).toString() else null, KeyType.CHARACTER)
        }
        return listOf(
            r1.mapIndexed { idx, c -> mapK(c, idx) },
            r2.mapIndexed { idx, c -> KeyAction(if (isShifted) c.uppercase() else c, null, KeyType.CHARACTER) },
            buildList {
                add(KeyAction("⇧", null, KeyType.SHIFT, weight = 1.2f))
                addAll(r3.map { c -> KeyAction(if (isShifted) c.uppercase() else c, null, KeyType.CHARACTER) })
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.2f))
            }
        )
    }

    private fun getKoreanRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("ㅂ" to "ㅃ", "ㅈ" to "ㅉ", "ㄷ" to "ㄸ", "ㄱ" to "ㄲ", "ㅅ" to "ㅆ", "ㅛ" to "1", "ㅕ" to "2", "ㅑ" to "3", "ㅐ" to "ㅒ", "ㅔ" to "ㅖ"),
        r2 = listOf("ㅁ" to "4", "ㄴ" to "5", "ㅇ" to "6", "ㄹ" to "7", "ㅎ" to "8", "ㅗ" to "9", "ㅓ" to "0", "ㅏ" to "@", "ㅣ" to "#"),
        r3 = listOf("ㅋ" to "*", "ㅌ" to "-", "ㅊ" to "+", "ㅍ" to "/", "ㅠ" to ":", "ㅜ" to "!", "ㅡ" to "?"),
        isShifted = isShifted,
        shiftWeight = 1.3f
    )

    private fun getJapaneseKanaRows(isShifted: Boolean) = buildPairedLayout(
        r1 = listOf("あ" to "ぁ", "か" to "が", "さ" to "ざ", "た" to "だ", "な" to "に", "は" to "ば", "ま" to "み", "や" to "ゃ", "ら" to "り", "わ" to "を"),
        r2 = listOf("い" to "ぃ", "き" to "ぎ", "し" to "じ", "ち" to "ぢ", "ぬ" to "ね", "ひ" to "び", "む" to "め", "ゆ" to "ゅ", "る" to "れ", "ん" to "ー"),
        r3 = listOf("う" to "ぅ", "く" to "ぐ", "す" to "ず", "つ" to "っ", "の" to "え", "ふ" to "ぷ", "も" to "お", "よ" to "ょ", "ろ" to "、"),
        isShifted = isShifted,
        shiftWeight = 1.25f
    )

    fun getSymbols1(language: LanguageId): List<List<KeyAction>> {
        val isBengaliFamily = language in listOf(
            LanguageId.BANGLA_PROBHAT, LanguageId.BANGLA_PHONETIC, LanguageId.BANGLA_NATIONAL, LanguageId.BANGLA_UNIBIJOY, LanguageId.SYLHETI, LanguageId.ASSAMESE
        )
        val topRow = if (isBengaliFamily) {
            listOf("১" to "1", "২" to "2", "৩" to "3", "৪" to "4", "৫" to "5", "৬" to "6", "৭" to "7", "৮" to "8", "৯" to "9", "০" to "0")
                .map { (bn, en) -> KeyAction(bn, en, KeyType.CHARACTER) }
        } else {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeyAction(it, null, KeyType.CHARACTER) }
        }
        val row2 = if (isBengaliFamily) {
            listOf(
                KeyAction("@", null, KeyType.CHARACTER),
                KeyAction("#", null, KeyType.CHARACTER),
                KeyAction("৳", "₹", KeyType.CHARACTER, extraVariants = listOf("₹", "$", "€", "£")),
                KeyAction("%", null, KeyType.CHARACTER),
                KeyAction("&", null, KeyType.CHARACTER),
                KeyAction("-", "—", KeyType.CHARACTER, extraVariants = listOf("—", "–", "_")),
                KeyAction("+", null, KeyType.CHARACTER),
                KeyAction("(", "[", KeyType.CHARACTER),
                KeyAction(")", "]", KeyType.CHARACTER),
                KeyAction("/", "\\", KeyType.CHARACTER)
            )
        } else {
            listOf("@", "#", "৳", "%", "&", "-", "+", "(", ")", "/").map { KeyAction(it, null, KeyType.CHARACTER) }
        }
        return listOf(
            topRow,
            row2,
            buildList {
                add(KeyAction("1/2", null, KeyType.MODE_SWITCH, weight = 1.3f))
                if (isBengaliFamily) {
                    addAll(listOf(
                        KeyAction("*", null, KeyType.CHARACTER),
                        KeyAction("\"", "“", KeyType.CHARACTER, extraVariants = listOf("“", "”")),
                        KeyAction("'", "‘", KeyType.CHARACTER, extraVariants = listOf("‘", "’")),
                        KeyAction(":", "ঃ", KeyType.CHARACTER),
                        KeyAction(";", null, KeyType.CHARACTER),
                        KeyAction("!", "॥", KeyType.CHARACTER, extraVariants = listOf("॥", "।")),
                        KeyAction("?", null, KeyType.CHARACTER)
                    ))
                } else {
                    addAll(listOf("*", "\"", "'", ":", ";", "!", "?").map { KeyAction(it, null, KeyType.CHARACTER) })
                }
                add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
            }
        )
    }

    val symbols2 = listOf(
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ").map { KeyAction(it, null, KeyType.CHARACTER) },
        listOf("£", "€", "¥", "৳", "₹", "¢", "^", "°", "=", "≠").map { KeyAction(it, null, KeyType.CHARACTER) },
        buildList {
            add(KeyAction("2/2", null, KeyType.MODE_SWITCH, weight = 1.3f))
            addAll(listOf("{", "}", "\\", "%", "©", "®", "™", "[", "]").map { KeyAction(it, null, KeyType.CHARACTER) })
            add(KeyAction("⌫", null, KeyType.BACKSPACE, weight = 1.3f))
        }
    )
}
