package com.example.data.dictionary

enum class FancyFontStyle(
    val id: String,
    val displayName: String,
    val sample: String
) {
    NORMAL("normal", "Normal", "TypeGenius"),
    BOLD_SANS("bold_sans", "Bold Sans", "𝗧𝘆𝗽𝗲𝗚𝗲𝗻𝗶𝘂𝘀"),
    ITALIC_SANS("italic_sans", "Italic Sans", "𝘛𝘺𝘱𝘦𝘎𝘦𝘯𝘪𝘶𝘴"),
    BOLD_ITALIC("bold_italic", "Bold Italic", "𝙏𝙮𝙥𝙚𝙂𝙚𝙣𝙞𝙪𝙨"),
    SERIF_BOLD("serif_bold", "Serif Bold", "𝐓𝐲𝐩𝐞𝐆𝐞𝐧𝐢𝐮𝐬"),
    MONOSPACE("monospace", "Monospace", "𝚃𝚢𝚙𝚎𝙶𝚎𝚗𝚒𝚞𝚜"),
    DOUBLE_STRUCK("double_struck", "Double Struck", "𝕋𝕪𝕡𝕖𝔾𝕖𝕟𝕚𝕦𝕤"),
    CIRCLED("circled", "Bubbled", "ⓉⓨⓟⓔⒼⓔⓝⓘⓤⓢ"),
    SMALL_CAPS("small_caps", "Small Caps", "TʏᴘᴇGᴇɴɪᴜs"),
    FULLWIDTH("fullwidth", "Fullwidth", "ＴｙｐｅＧｅｎｉｕｓ"),
    DECORATED_STAR("decorated_star", "✦ Starred ✦", "✦ বাংলা / Text ✦"),
    DECORATED_BRACKET("decorated_bracket", "【 Framed 】", "【 বাংলা / Text 】");

    companion object {
        fun fromId(id: String): FancyFontStyle = entries.find { it.id == id } ?: NORMAL
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

    fun styleText(input: String, style: FancyFontStyle): String {
        if (style == FancyFontStyle.NORMAL || input.isEmpty()) return input
        if (style == FancyFontStyle.DECORATED_STAR) return "✦ $input ✦"
        if (style == FancyFontStyle.DECORATED_BRACKET) return "【 $input 】"

        val sb = StringBuilder(input.length * 2)
        for (c in input) {
            sb.append(mapChar(c, style))
        }
        return sb.toString()
    }

    fun styleSingleInput(token: String, style: FancyFontStyle): String {
        if (style == FancyFontStyle.NORMAL || token.isEmpty()) return token
        if (style == FancyFontStyle.DECORATED_STAR || style == FancyFontStyle.DECORATED_BRACKET) {
            return token
        }
        val sb = StringBuilder(token.length * 2)
        for (c in token) {
            sb.append(mapChar(c, style))
        }
        return sb.toString()
    }

    private fun codePointToString(cp: Int): String = String(Character.toChars(cp))

    private fun mapChar(c: Char, style: FancyFontStyle): String {
        return when (style) {
            FancyFontStyle.NORMAL,
            FancyFontStyle.DECORATED_STAR,
            FancyFontStyle.DECORATED_BRACKET -> c.toString()

            FancyFontStyle.BOLD_SANS -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D5D4 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D5EE + (c - 'a'))
                in '0'..'9' -> codePointToString(0x1D7EC + (c - '0'))
                else -> c.toString()
            }

            FancyFontStyle.ITALIC_SANS -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D608 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D622 + (c - 'a'))
                else -> c.toString()
            }

            FancyFontStyle.BOLD_ITALIC -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D63C + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D656 + (c - 'a'))
                else -> c.toString()
            }

            FancyFontStyle.SERIF_BOLD -> when (c) {
                in 'A'..'Z' -> codePointToString(0x1D400 + (c - 'A'))
                in 'a'..'z' -> codePointToString(0x1D41A + (c - 'a'))
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

            FancyFontStyle.SMALL_CAPS -> smallCapsMap[c]?.toString() ?: c.toString()

            FancyFontStyle.FULLWIDTH -> when {
                c == ' ' -> "\u3000"
                c in '!'..'~' -> (c.code + 0xFEE0).toChar().toString()
                else -> c.toString()
            }
        }
    }
}
