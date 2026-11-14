package com.example.data.dictionary

enum class FontCategory(val label: String) {
    ALL("সব"),
    HANDWRITING("✍️ হাতের লেখা"),
    STYLISH("স্টাইলিশ"),
    BOXED("বক্স ও বাবল"),
    BANGLA_DECOR("বাংলা ডেকোর")
}

enum class FancyFontStyle(
    val id: String,
    val displayName: String,
    val badgeLabel: String,
    val sample: String,
    val category: FontCategory,
    val isDecoratorFrame: Boolean = false,
    val supportsAllScripts: Boolean = false,
    val isHandwritingFont: Boolean = false
) {
    NORMAL("normal", "সাধারণ (Normal)", "Aa", "TypeGenius", FontCategory.ALL),
    HANDWRITING_CUSTOM(
        id = "handwriting_custom",
        displayName = "আমার হাতের লেখা ফন্ট (Custom)",
        badgeLabel = "✍️ নিজস্ব",
        sample = "আমার হাতের লেখা",
        category = FontCategory.HANDWRITING,
        supportsAllScripts = true,
        isHandwritingFont = true
    ),
    HANDWRITING_ATMA(
        id = "handwriting_atma",
        displayName = "বাংলা হাতের লেখা (Atma)",
        badgeLabel = "✍️ আত্মা",
        sample = "হাতের লেখা বাংলা",
        category = FontCategory.HANDWRITING,
        supportsAllScripts = true,
        isHandwritingFont = true
    ),
    HANDWRITING_GALADA(
        id = "handwriting_galada",
        displayName = "বাংলা ক্যালিগ্রাফি (Galada)",
        badgeLabel = "🖋️ গলাদা",
        sample = "শৈল্পিক বাংলা লেখা",
        category = FontCategory.HANDWRITING,
        supportsAllScripts = true,
        isHandwritingFont = true
    ),
    BOLD_SANS("bold_sans", "Bold Sans", "𝗔𝗮", "𝗧𝘆𝗽𝗲𝗚𝗲𝗻𝗶𝘂𝘀", FontCategory.STYLISH),
    ITALIC_SANS("italic_sans", "Italic Sans", "𝘈𝘢", "𝘛𝘺𝘱𝘦𝘎𝘦𝘯𝘪𝘶𝘴", FontCategory.STYLISH),
    BOLD_ITALIC("bold_italic", "Bold Italic", "𝘼𝙖", "𝙏𝙮𝙥𝙚𝙂𝙚𝙣𝙞𝙪𝙨", FontCategory.STYLISH),
    SCRIPT_BOLD("script_bold", "Bold Cursive", "𝓐𝓪", "𝓣𝔂𝓹𝓮𝓖𝓮𝓷𝓲𝓾𝓼", FontCategory.STYLISH),
    SCRIPT_REGULAR("script_regular", "Calligraphy", "𝒜𝒶", "𝒯𝓎𝓅ℯ𝒢ℯ𝓃𝒾𝓊𝓈", FontCategory.STYLISH),
    FRAKTUR_BOLD("fraktur_bold", "Gothic Bold", "𝕬𝖆", "𝕿𝖞𝖕𝖊𝕲𝖊𝖓𝖎𝖚𝖘", FontCategory.STYLISH),
    FRAKTUR_REGULAR("fraktur_regular", "Classic Gothic", "𝔄𝔞", "𝔗𝔶𝔭𝔢𝔊𝔢𝔫𝔦𝔲𝔰", FontCategory.STYLISH),
    SERIF_BOLD("serif_bold", "Serif Bold", "𝐀𝐚", "𝐓𝐲𝐩𝐞𝐆𝐞𝐧𝐢𝐮𝐬", FontCategory.STYLISH),
    SERIF_ITALIC("serif_italic", "Serif Italic", "𝐴𝑎", "𝑇𝑦𝑝𝑒𝐺𝑒𝑛𝑖𝑢𝑠", FontCategory.STYLISH),
    MONOSPACE("monospace", "Typewriter", "𝙰𝚊", "𝚃𝚢𝚙𝚎𝙶𝚎𝚗𝚒𝚞𝚜", FontCategory.STYLISH),
    DOUBLE_STRUCK("double_struck", "Double Struck", "𝔸𝕒", "𝕋𝕪𝕡𝕖𝔾𝕖𝕟𝕚𝕦𝕤", FontCategory.STYLISH),
    SMALL_CAPS("small_caps", "Small Caps", "Aᴀ", "TʏᴘᴇGᴇɴɪᴜs", FontCategory.STYLISH),
    FULLWIDTH("fullwidth", "Wide Aesthetic", "Ａａ", "ＴｙｐｅＧｅｎｉｕｓ", FontCategory.BOXED),
    CIRCLED("circled", "Bubbled", "Ⓐⓐ", "ⓉⓨⓟⓔⒼⓔⓝⓘⓤⓢ", FontCategory.BOXED),
    NEGATIVE_CIRCLED("negative_circled", "Dark Bubble", "🅐", "🅣🅨🅟🅔", FontCategory.BOXED),
    SQUARED("squared", "Squared", "🄰", "🅃🅈🄿🄴", FontCategory.BOXED),
    NEGATIVE_SQUARED("negative_squared", "Dark Box", "🅰", "🆃🆈🅿🅴", FontCategory.BOXED),
    UNDERLINE("underline", "Underlined (All Scripts)", "A͟ব͟", "বাংলা / Text", FontCategory.BANGLA_DECOR, supportsAllScripts = true),
    DOUBLE_UNDERLINE("double_underline", "Double Line (All Scripts)", "A̳ব̳", "বাংলা / Text", FontCategory.BANGLA_DECOR, supportsAllScripts = true),
    STRIKETHROUGH("strikethrough", "Strikethrough (All Scripts)", "A̶ব̶", "বাংলা / Text", FontCategory.BANGLA_DECOR, supportsAllScripts = true),
    DECORATED_STAR("decorated_star", "✦ Starred Frame ✦", "✦", "✦ বাংলা / Text ✦", FontCategory.BANGLA_DECOR, isDecoratorFrame = true, supportsAllScripts = true),
    DECORATED_BRACKET("decorated_bracket", "【 Bold Bracket 】", "【】", "【 বাংলা / Text 】", FontCategory.BANGLA_DECOR, isDecoratorFrame = true, supportsAllScripts = true),
    DECORATED_ROYAL("decorated_royal", "꧁ Royal Wing ꧂", "꧁꧂", "꧁ বাংলা / Text ꧂", FontCategory.BANGLA_DECOR, isDecoratorFrame = true, supportsAllScripts = true),
    DECORATED_WING("decorated_wing", "★彡 Star Wing 彡★", "★彡", "★彡 বাংলা / Text 彡★", FontCategory.BANGLA_DECOR, isDecoratorFrame = true, supportsAllScripts = true);

    fun handwritingPresetId(): String? = when (this) {
        HANDWRITING_CUSTOM -> "custom"
        HANDWRITING_ATMA -> "atma"
        HANDWRITING_GALADA -> "galada"
        else -> null
    }

    companion object {
        fun fromId(id: String): FancyFontStyle = entries.find { it.id == id } ?: NORMAL

        fun fromHandwritingPresetId(presetId: String): FancyFontStyle? = when (presetId) {
            "custom" -> HANDWRITING_CUSTOM
            "atma" -> HANDWRITING_ATMA
            "galada" -> HANDWRITING_GALADA
            else -> null
        }
    }
}

object UnicodeTextStyler {

    private val smallCapsMap = mapOf(
        'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ', 'f' to 'ꜰ', 'g' to 'ɢ',
        'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ', 'k' to 'ᴋ', 'l' to 'ʟ', 'm' to 'ᴍ', 'n' to 'ɴ',
        'o' to 'ᴏ', 'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ', 's' to 'ꜱ', 't' to 'ᴛ', 'u' to 'ᴜ',
        'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x', 'y' to 'ʏ', 'z' to 'ᴢ'
    )

    private val doubleStruckExceptions = mapOf(
        'C' to "ℂ", 'H' to "ℍ", 'N' to "ℕ", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'Z' to "ℤ"
    )

    private val regularScriptExceptions = mapOf(
        'B' to "ℬ", 'E' to "ℰ", 'F' to "ℱ", 'H' to "ℋ", 'I' to "ℐ", 'L' to "ℒ", 'M' to "ℳ", 'R' to "ℛ",
        'e' to "ℯ", 'g' to "ℊ", 'o' to "ℴ"
    )

    private val regularFrakturExceptions = mapOf(
        'C' to "ℭ", 'H' to "ℌ", 'I' to "ℑ", 'R' to "ℜ", 'Z' to "ℨ"
    )

    private val combiningMarks = setOf('\u0332', '\u0333', '\u0336', '\u0338')

    // Reverse lookup map so already-styled text can be cleanly converted back to plain text before restyling
    private val reverseStyledMap: Map<String, Char> by lazy {
        val map = HashMap<String, Char>(1024)
        val stylesToIndex = FancyFontStyle.entries.filter {
            it != FancyFontStyle.NORMAL &&
                !it.isHandwritingFont &&
                !it.isDecoratorFrame &&
                !it.supportsAllScripts &&
                it != FancyFontStyle.NEGATIVE_CIRCLED &&
                it != FancyFontStyle.SQUARED &&
                it != FancyFontStyle.NEGATIVE_SQUARED
        }
        for (style in stylesToIndex) {
            for (c in 'A'..'Z') {
                val mapped = mapChar(c, style)
                if (mapped != c.toString()) map.putIfAbsent(mapped, c)
            }
            for (c in 'a'..'z') {
                val mapped = mapChar(c, style)
                if (mapped != c.toString()) map.putIfAbsent(mapped, c)
            }
            for (c in '0'..'9') {
                val mapped = mapChar(c, style)
                if (mapped != c.toString()) map.putIfAbsent(mapped, c)
            }
        }
        // Fullwidth punctuation & space
        map["\u3000"] = ' '
        for (c in '!'..'~') {
            val fw = (c.code + 0xFEE0).toChar().toString()
            map.putIfAbsent(fw, c)
        }
        // Negative circled / squared uppercase fallback
        for (style in listOf(FancyFontStyle.NEGATIVE_CIRCLED, FancyFontStyle.SQUARED, FancyFontStyle.NEGATIVE_SQUARED)) {
            for (c in 'A'..'Z') {
                val mapped = mapChar(c, style)
                if (mapped != c.toString()) map.putIfAbsent(mapped, c)
            }
            for (c in '0'..'9') {
                val mapped = mapChar(c, style)
                if (mapped != c.toString()) map.putIfAbsent(mapped, c)
            }
        }
        map
    }

    fun unstyleToPlain(input: String): String {
        if (input.isEmpty()) return input
        var clean = input
            .removePrefix("✦ ").removeSuffix(" ✦")
            .removePrefix("【 ").removeSuffix(" 】")
            .removePrefix("꧁ ").removeSuffix(" ꧂")
            .removePrefix("★彡 ").removeSuffix(" 彡★")
            .trim()
        if (clean.isEmpty()) clean = input

        val sb = StringBuilder(clean.length)
        var i = 0
        while (i < clean.length) {
            val cp = clean.codePointAt(i)
            val charCount = Character.charCount(cp)
            val token = clean.substring(i, i + charCount)
            if (charCount == 1 && token[0] in combiningMarks) {
                i += charCount
                continue
            }
            val plain = reverseStyledMap[token]
            if (plain != null) {
                sb.append(plain)
            } else {
                sb.append(token)
            }
            i += charCount
        }
        return sb.toString()
    }

    fun styleText(input: String, style: FancyFontStyle): String {
        if (input.isEmpty()) return input
        val normalized = unstyleToPlain(input)
        if (style == FancyFontStyle.NORMAL || style.isHandwritingFont) return normalized
        return when (style) {
            FancyFontStyle.NORMAL,
            FancyFontStyle.HANDWRITING_CUSTOM,
            FancyFontStyle.HANDWRITING_ATMA,
            FancyFontStyle.HANDWRITING_GALADA -> normalized
            FancyFontStyle.DECORATED_STAR -> "✦ $normalized ✦"
            FancyFontStyle.DECORATED_BRACKET -> "【 $normalized 】"
            FancyFontStyle.DECORATED_ROYAL -> "꧁ $normalized ꧂"
            FancyFontStyle.DECORATED_WING -> "★彡 $normalized 彡★"
            FancyFontStyle.UNDERLINE -> applyCombiningMarkSafe(normalized, '\u0332')
            FancyFontStyle.DOUBLE_UNDERLINE -> applyCombiningMarkSafe(normalized, '\u0333')
            FancyFontStyle.STRIKETHROUGH -> applyCombiningMarkSafe(normalized, '\u0336')
            else -> {
                val sb = StringBuilder(normalized.length * 2)
                for (c in normalized) {
                    sb.append(mapChar(c, style))
                }
                sb.toString()
            }
        }
    }

    /**
     * Used for live per-character typing.
     * Note: Decorator frames do not wrap individual characters; they wrap completed words.
     * Handwriting fonts output clean plain characters & render in the live handwriting preview/card.
     */
    fun styleSingleInput(token: String, style: FancyFontStyle): String {
        if (style == FancyFontStyle.NORMAL || style.isHandwritingFont || token.isEmpty()) return token
        if (style.isDecoratorFrame) return token
        return when (style) {
            FancyFontStyle.UNDERLINE -> applyCombiningMarkSafe(token, '\u0332')
            FancyFontStyle.DOUBLE_UNDERLINE -> applyCombiningMarkSafe(token, '\u0333')
            FancyFontStyle.STRIKETHROUGH -> applyCombiningMarkSafe(token, '\u0336')
            else -> {
                val sb = StringBuilder(token.length * 2)
                for (c in token) {
                    sb.append(mapChar(c, style))
                }
                sb.toString()
            }
        }
    }

    /**
     * Appends combining marks safely at syllable/cluster boundaries so Indic/Bengali
     * conjuncts (virama ্) and dependent vowel signs (া, ি, ী, etc.) remain unbroken.
     */
    private fun applyCombiningMarkSafe(input: String, mark: Char): String {
        val sb = StringBuilder(input.length * 2)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            sb.append(c)
            if (!c.isWhitespace()) {
                val next = input.getOrNull(i + 1)
                val isCurrentVirama = c == '\u09CD' || c == '\u094D'
                val isNextDependentIndic = next != null && isIndicCombiningOrVirama(next)
                if (!isCurrentVirama && !isNextDependentIndic) {
                    sb.append(mark)
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun isIndicCombiningOrVirama(c: Char): Boolean {
        val code = c.code
        return (code in 0x0981..0x0983) || // Bengali Chandrabindu, Anusvara, Visarga
            code == 0x09BC || // Bengali Nukta
            (code in 0x09BE..0x09CC) || // Bengali Vowel signs
            code == 0x09CD || // Bengali Virama (Hasanta)
            code == 0x09D7 || // Bengali Au length mark
            (code in 0x09E2..0x09E3) || // Bengali Vocalic vowel signs
            (code in 0x0900..0x0903) || // Devanagari signs
            (code in 0x093A..0x094F) || // Devanagari vowel signs & virama
            (code in 0x064B..0x065F) // Arabic harakat
    }

    private fun codePointToString(cp: Int): String = String(Character.toChars(cp))

    private fun mapChar(c: Char, style: FancyFontStyle): String {
        return when (style) {
            FancyFontStyle.NORMAL,
            FancyFontStyle.HANDWRITING_CUSTOM,
            FancyFontStyle.HANDWRITING_ATMA,
            FancyFontStyle.HANDWRITING_GALADA,
            FancyFontStyle.UNDERLINE,
            FancyFontStyle.DOUBLE_UNDERLINE,
            FancyFontStyle.STRIKETHROUGH,
            FancyFontStyle.DECORATED_STAR,
            FancyFontStyle.DECORATED_BRACKET,
            FancyFontStyle.DECORATED_ROYAL,
            FancyFontStyle.DECORATED_WING -> c.toString()

            FancyFontStyle.BOLD_SANS -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D5D4 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D5EE + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7EC + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.ITALIC_SANS -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D608 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D622 + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7E2 + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.BOLD_ITALIC -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D63C + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D656 + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7EC + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.SCRIPT_BOLD -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D4D0 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D4EA + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.SCRIPT_REGULAR -> when (c) {
                in 'A'..'Z' -> regularScriptExceptions[c] ?: codePointToString(0x1D49C + (c - 'A'))
                in 'a'..'z' -> regularScriptExceptions[c] ?: codePointToString(0x1D4B6 + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.FRAKTUR_BOLD -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D56C + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D586 + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.FRAKTUR_REGULAR -> when (c) {
                in 'A'..'Z' -> regularFrakturExceptions[c] ?: codePointToString(0x1D504 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D51E + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.SERIF_BOLD -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D400 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D41A + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.SERIF_ITALIC -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D434 + (c - 'A'))
                'h' -> "\u210E"
                in 'a'..'z' -> codePointToString(0x1D44E + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7CE + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.MONOSPACE -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D670 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D68A + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7F6 + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.DOUBLE_STRUCK -> when (c) {
                in 'A'..'Z' -> doubleStruckExceptions[c] ?: codePointToString(0x1D538 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D552 + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7D8 + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.CIRCLED -> when (c) {
                in 'A'..'Z' -> (0x24B6 + (c - 'A')).toChar().toString()
                in 'a'..'z' -> (0x24D0 + (c - 'a')).toChar().toString()
                in '1'..'9' -> (0x2460 + (c - '1')).toChar().toString()
                '0' -> "\u24EA"
                else -> c.toString()
            }

            FancyFontStyle.NEGATIVE_CIRCLED -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1F150 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1F150 + (c.uppercaseChar() - 'A'))
                in '1'..'9' -> (0x2776 + (c - '1')).toChar().toString()
                '0' -> "\u24FF"
                else -> c.toString()
            }

            FancyFontStyle.SQUARED -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1F130 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1F130 + (c.uppercaseChar() - 'A'))
                else -> c.toString()
            }

            FancyFontStyle.NEGATIVE_SQUARED -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1F170 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1F170 + (c.uppercaseChar() - 'A'))
                else -> c.toString()
            }

            FancyFontStyle.SMALL_CAPS -> smallCapsMap[c]?.toString() ?: c.toString()

            FancyFontStyle.FULLWIDTH -> when {
                c == ' ' -> "\u3000"
                c in '!'..'~' -> (c.code + 0xFEE0).toChar().toString()
                else -> c.toString()
            }
        }
    }
}
