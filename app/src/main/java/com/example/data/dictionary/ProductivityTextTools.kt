package com.example.data.dictionary

import com.example.data.BengaliCalendarUtils
import com.example.data.IslamicToolkit
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class TextTransformMode(val label: String, val tagSuffix: String) {
    UPPER("UPPER", "upper"),
    LOWER("lower", "lower"),
    TITLE("Title", "title"),
    SNAKE("snake_case", "snake"),
    CAMEL("camelCase", "camel"),
    TO_BENGALI_DIGITS("১২৩", "to_bn"),
    TO_ENGLISH_DIGITS("123", "to_en"),
    SOUTH_ASIAN_COMMA("৳ কমা", "bdt_comma")
}

data class InlineConversionResult(
    val resultString: String,
    val detailLabel: String
)

data class AutofillFieldSchema(
    val key: String,
    val label: String,
    val placeholder: String
)

data class QuickTodoItem(
    val id: String = UUID.randomUUID().toString().take(8),
    val text: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class QuickBlockPreset(
    val id: String,
    val label: String,
    val content: String
)

object ProductivityTextTools {

    val autofillSchema: List<AutofillFieldSchema> = listOf(
        AutofillFieldSchema("name", "নাম (Name)", "আপনার পূর্ণ নাম"),
        AutofillFieldSchema("phone", "মোবাইল (Phone)", "+8801XXXXXXXXX"),
        AutofillFieldSchema("email", "ইমেইল (Email)", "you@example.com"),
        AutofillFieldSchema("address", "ঠিকানা (Address)", "বাসা/রোড, এলাকা, শহর"),
        AutofillFieldSchema("bkash", "বিকাশ / পেমেন্ট", "bKash Personal: 017..."),
        AutofillFieldSchema("link", "লিংক / পোর্টফোলিও", "https://...")
    )

    val listAndEmailPresets: List<QuickBlockPreset> = listOf(
        QuickBlockPreset("bullet_dot", "• বুলেট", "• "),
        QuickBlockPreset("bullet_check", "✅ টিক", "✅ "),
        QuickBlockPreset("bullet_arrow", "➤ পয়েন্ট", "➤ "),
        QuickBlockPreset("email_bn_open", "📧 বাংলা শুরু", "আসসালামু আলাইকুম,\nআশা করি ভালো আছেন।\n"),
        QuickBlockPreset("email_bn_formal", "📄 অফিসিয়াল", "সম্মানিত মহোদয়,\nবিষয়: \n\nবিনীত নিবেদক,\n"),
        QuickBlockPreset("email_en_open", "✉️ Eng Email", "Dear Sir/Madam,\nHope you are doing well.\n"),
        QuickBlockPreset("email_en_close", "🙏 Regards", "\nBest regards,\n"),
        QuickBlockPreset("meeting_dynamic", "📅 মিটিং", "আগামী {বার} ({তারিখ}) {সময়}-এ কথা হবে।")
    )

    private val bnWeekdays = arrayOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    private val bnDigitToAscii = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val asciiToBnDigit = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    fun toBengaliDigits(input: String): String =
        input.map { asciiToBnDigit[it] ?: it }.joinToString("")

    fun toEnglishDigits(input: String): String =
        input.map { bnDigitToAscii[it] ?: it }.joinToString("")

    fun transformText(input: String, mode: TextTransformMode): String {
        if (input.isEmpty()) return input
        return when (mode) {
            TextTransformMode.UPPER -> input.uppercase(Locale.getDefault())
            TextTransformMode.LOWER -> input.lowercase(Locale.getDefault())
            TextTransformMode.TITLE -> transformTitleCase(input)
            TextTransformMode.SNAKE -> splitIdentifiers(input).joinToString("_") { it.lowercase(Locale.US) }.ifEmpty { input }
            TextTransformMode.CAMEL -> {
                val parts = splitIdentifiers(input)
                if (parts.isEmpty()) input
                else buildString {
                    parts.forEachIndexed { idx, part ->
                        val lower = part.lowercase(Locale.US)
                        if (idx == 0) append(lower)
                        else append(lower.replaceFirstChar { it.uppercaseChar() })
                    }
                }
            }
            TextTransformMode.TO_BENGALI_DIGITS -> toBengaliDigits(input)
            TextTransformMode.TO_ENGLISH_DIGITS -> toEnglishDigits(input)
            TextTransformMode.SOUTH_ASIAN_COMMA -> formatSouthAsianComma(input)
        }
    }

    private fun transformTitleCase(input: String): String {
        val sb = StringBuilder(input.length)
        var capitalizeNext = true
        for (ch in input) {
            if (ch.isWhitespace() || ch == '-' || ch == '_' || ch == '/') {
                sb.append(ch)
                capitalizeNext = true
            } else if (capitalizeNext) {
                sb.append(ch.titlecaseChar())
                capitalizeNext = false
            } else {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    private fun splitIdentifiers(input: String): List<String> {
        val normalized = input
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('.', ' ')
        return normalized.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    fun formatSouthAsianComma(input: String): String {
        val hasBnDigits = input.any { it in '০'..'৯' }
        val normalized = toEnglishDigits(input.replace(",", ""))
        val match = Regex("""^([^\d-]*)(-?\d+)(\.\d+)?(.*)$""").find(normalized.trim()) ?: return input
        val prefix = match.groupValues[1]
        val intPartSigned = match.groupValues[2]
        val decPart = match.groupValues[3]
        val suffix = match.groupValues[4]

        val isNeg = intPartSigned.startsWith("-")
        val digits = if (isNeg) intPartSigned.drop(1) else intPartSigned
        val grouped = groupIndianSubcontinentDigits(digits)
        val combined = buildString {
            append(prefix)
            if (isNeg) append('-')
            append(grouped)
            append(decPart)
            append(suffix)
        }
        return if (hasBnDigits) toBengaliDigits(combined) else combined
    }

    private fun groupIndianSubcontinentDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val chunks = mutableListOf<String>()
        var idx = rest.length
        while (idx > 0) {
            val start = (idx - 2).coerceAtLeast(0)
            chunks.add(rest.substring(start, idx))
            idx = start
        }
        return chunks.reversed().joinToString(",") + "," + last3
    }

    fun nextNumberedLinePrefix(textBeforeCursor: String, bengaliDigits: Boolean): String {
        val currentLine = textBeforeCursor.substringAfterLast('\n')
        val trimmedLine = currentLine.trimStart()
        val match = Regex("""^([0-9০-৯]+)[.)]\s*.*$""").find(trimmedLine)
        val nextNum = if (match != null) {
            val numAscii = toEnglishDigits(match.groupValues[1]).toIntOrNull() ?: 0
            (numAscii + 1).coerceAtLeast(1)
        } else {
            1
        }
        val numStr = if (bengaliDigits) toBengaliDigits(nextNum.toString()) else nextNum.toString()
        val needsNewline = currentLine.isNotEmpty()
        return if (needsNewline) "\n$numStr. " else "$numStr. "
    }

    fun resolveTemplateVariables(
        template: String,
        clipboardLatest: String = "",
        now: Date = Date()
    ): String {
        if (!template.contains('{')) return template
        val bnCal = BengaliCalendarUtils.toBanglaDate(now)
        val cal = Calendar.getInstance().apply { time = now }
        val bnWeekday = bnWeekdays[cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]
        val enDate = BengaliCalendarUtils.formatEnglishDate(now)
        val bnGregDate = run {
            val bnMonths = arrayOf(
                "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
            )
            val d = toBengaliDigits(cal.get(Calendar.DAY_OF_MONTH).toString())
            val m = bnMonths[cal.get(Calendar.MONTH)]
            val y = toBengaliDigits(cal.get(Calendar.YEAR).toString())
            "$d $m $y"
        }
        val bnTime = BengaliCalendarUtils.formatBanglaTime(now)
        val enTime = BengaliCalendarUtils.formatEnglishTime(now)
        val enDay = BengaliCalendarUtils.formatEnglishWeekday(now)
        val hijriBn = IslamicToolkit.toHijriDate(now).formattedBn

        return template
            .replace("{তারিখ}", bnGregDate)
            .replace("{বাংলা_তারিখ}", bnCal.formattedFull)
            .replace("{হিজরি}", hijriBn)
            .replace("{hijri}", hijriBn)
            .replace("{সময়}", bnTime)
            .replace("{সময়}", bnTime)
            .replace("{বার}", bnWeekday)
            .replace("{ক্লিপ}", clipboardLatest)
            .replace("{date}", enDate)
            .replace("{time}", enTime)
            .replace("{day}", enDay)
            .replace("{clip}", clipboardLatest)
    }

    /**
     * Evaluates currency or unit expressions typed inline (e.g., `$50`, `10km`, `75kg`, `38°C`, `500৳`)
     * so converted results appear automatically on the suggestion bar without opening Calculator.
     */
    fun evaluateInlineUnitOrCurrency(rawToken: String): InlineConversionResult? {
        val token = rawToken.trim()
        if (token.length < 2 || token.length > 24) return null

        val asciiToken = toEnglishDigits(token).replace(",", "")
        val hasBengaliDigits = token.any { it in '০'..'৯' }

        // 1. Prefix currency: $50, €20, £10, ৳500, ₹100, USD50, EUR20, GBP10, SAR100, INR500
        val prefixMatch = Regex("""^(USD|EUR|GBP|SAR|INR|BDT|\$|€|£|৳|₹)\s*(-?\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE)
            .find(asciiToken)
        if (prefixMatch != null) {
            val sym = prefixMatch.groupValues[1].uppercase(Locale.US)
            val num = prefixMatch.groupValues[2].toDoubleOrNull() ?: return null
            return convertCurrencyToken(token, sym, num, hasBengaliDigits)
        }

        // 2. Suffix unit or currency: 50usd, 500৳, 10km, 5mi, 75kg, 150lb, 38c, 38°c, 100f, 100°f, 6ft, 10m, 100cm, 12in
        val suffixMatch = Regex("""^(-?\d+(?:\.\d+)?)\s*(USD|EUR|GBP|SAR|INR|BDT|\$|€|£|৳|₹|km|mi|kg|g|lb|lbs|°c|°f|c|f|ft|m|cm|in)$""", RegexOption.IGNORE_CASE)
            .find(asciiToken)
        if (suffixMatch != null) {
            val num = suffixMatch.groupValues[1].toDoubleOrNull() ?: return null
            val unit = suffixMatch.groupValues[2].lowercase(Locale.US)
            return when (unit) {
                "usd", "$", "eur", "€", "gbp", "£", "sar", "inr", "₹", "bdt", "৳" ->
                    convertCurrencyToken(token, unit.uppercase(Locale.US), num, hasBengaliDigits)
                "km" -> {
                    val out = formatDecimal(num * 0.621371)
                    buildUnitResult(token, out, "mi", hasBengaliDigits)
                }
                "mi" -> {
                    val out = formatDecimal(num / 0.621371)
                    buildUnitResult(token, out, "km", hasBengaliDigits)
                }
                "kg" -> {
                    val out = formatDecimal(num * 2.20462)
                    buildUnitResult(token, out, "lb", hasBengaliDigits)
                }
                "lb", "lbs" -> {
                    val out = formatDecimal(num / 2.20462)
                    buildUnitResult(token, out, "kg", hasBengaliDigits)
                }
                "g" -> {
                    val out = formatDecimal(num / 1000.0)
                    buildUnitResult(token, out, "kg", hasBengaliDigits)
                }
                "c", "°c" -> {
                    val out = formatDecimal((num * 9.0 / 5.0) + 32.0)
                    buildUnitResult(token, out, "°F", hasBengaliDigits)
                }
                "f", "°f" -> {
                    val out = formatDecimal((num - 32.0) * 5.0 / 9.0)
                    buildUnitResult(token, out, "°C", hasBengaliDigits)
                }
                "ft" -> {
                    val out = formatDecimal(num * 0.3048)
                    buildUnitResult(token, out, "m", hasBengaliDigits)
                }
                "m" -> {
                    val out = formatDecimal(num / 0.3048)
                    buildUnitResult(token, out, "ft", hasBengaliDigits)
                }
                "cm" -> {
                    val out = formatDecimal(num / 2.54)
                    buildUnitResult(token, out, "in", hasBengaliDigits)
                }
                "in" -> {
                    val out = formatDecimal(num * 2.54)
                    buildUnitResult(token, out, "cm", hasBengaliDigits)
                }
                else -> null
            }
        }
        return null
    }

    private fun convertCurrencyToken(
        originalToken: String,
        symbolUpper: String,
        amount: Double,
        preferBnDigits: Boolean
    ): InlineConversionResult {
        val resultText = when (symbolUpper) {
            "$", "USD" -> "${formatSouthAsianComma(formatDecimal(amount * 121.0, decimals = 2))}৳"
            "€", "EUR" -> "${formatSouthAsianComma(formatDecimal(amount * 131.0, decimals = 2))}৳"
            "£", "GBP" -> "${formatSouthAsianComma(formatDecimal(amount * 154.0, decimals = 2))}৳"
            "SAR" -> "${formatSouthAsianComma(formatDecimal(amount * 32.25, decimals = 2))}৳"
            "₹", "INR" -> "${formatSouthAsianComma(formatDecimal(amount * 1.42, decimals = 2))}৳"
            "৳", "BDT" -> "$${formatDecimal(amount / 121.0, decimals = 2)}"
            else -> "${formatSouthAsianComma(formatDecimal(amount * 121.0, decimals = 2))}৳"
        }
        val localized = if (preferBnDigits) toBengaliDigits(resultText) else resultText
        return InlineConversionResult(
            resultString = localized,
            detailLabel = "$originalToken = $localized"
        )
    }

    private fun buildUnitResult(
        originalToken: String,
        valueAscii: String,
        targetUnit: String,
        preferBnDigits: Boolean
    ): InlineConversionResult {
        val numText = if (preferBnDigits) toBengaliDigits(valueAscii) else valueAscii
        val result = "$numText $targetUnit"
        return InlineConversionResult(
            resultString = result,
            detailLabel = "$originalToken = $result"
        )
    }

    private fun formatDecimal(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (abs(rounded.toDouble()) < 1e-9) "0" else rounded.toPlainString()
    }

    fun parseAutofillProfile(rawJson: String): Map<String, String> {
        if (rawJson.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(rawJson)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k, "").trim()
                    if (k.isNotBlank() && v.isNotBlank()) put(k, v)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun serializeAutofillProfile(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            val clean = v.trim()
            if (k.isNotBlank() && clean.isNotEmpty()) {
                obj.put(k, clean)
            }
        }
        return obj.toString()
    }

    fun parseTodoItems(rawJson: String): List<QuickTodoItem> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val text = obj.optString("text", "").trim()
                    if (text.isNotEmpty()) {
                        add(
                            QuickTodoItem(
                                id = obj.optString("id", UUID.randomUUID().toString().take(8)),
                                text = text,
                                done = obj.optBoolean("done", false),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serializeTodoItems(items: List<QuickTodoItem>): String {
        val arr = JSONArray()
        items.take(40).forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text.trim())
                    put("done", item.done)
                    put("createdAt", item.createdAt)
                }
            )
        }
        return arr.toString()
    }
}
