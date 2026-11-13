package com.example.data.local

enum class SmartClipType(val badgeLabel: String) {
    OTP("OTP"),
    LINK("Link"),
    EMAIL("Email"),
    PHONE("Phone"),
    TEXT("Text")
}

data class SmartClipInfo(
    val type: SmartClipType,
    val rawText: String,
    val extractedValue: String,
    val quickPillLabel: String,
    val badgeLabel: String = type.badgeLabel
)

object SmartClipboardParser {

    private val urlRegex = Regex(
        """(https?://[^\s]+|www\.[^\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val emailRegex = Regex(
        """[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"""
    )
    private val phoneRegex = Regex(
        """(?:\+?\d{1,3}[\s\-]?)?(?:\(?\d{3,4}\)?[\s\-]?)?\d{3,4}[\s\-]?\d{4,6}"""
    )
    private val otpKeywordRegex = Regex(
        """\b(?:otp|code|pin|passcode|verification|verify|auth|কোড|ওটিপি|পিন)\b""",
        RegexOption.IGNORE_CASE
    )
    private val otpDigitsRegex = Regex("""\b(\d{4,8})\b""")

    fun classify(rawText: String): SmartClipInfo {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return SmartClipInfo(
                type = SmartClipType.TEXT,
                rawText = rawText,
                extractedValue = "",
                quickPillLabel = ""
            )
        }

        // 1. Standalone 4-8 digit OTP OR digits alongside OTP/Verification keyword
        val hasOtpKeyword = otpKeywordRegex.containsMatchIn(trimmed)
        val isPureOtp = trimmed.matches(Regex("""^\d{4,8}$"""))
        if (isPureOtp) {
            return SmartClipInfo(
                type = SmartClipType.OTP,
                rawText = trimmed,
                extractedValue = trimmed,
                quickPillLabel = "🔑 OTP: $trimmed"
            )
        }
        if (hasOtpKeyword) {
            val otpMatch = otpDigitsRegex.find(trimmed)?.groupValues?.getOrNull(1)
            if (!otpMatch.isNullOrBlank()) {
                return SmartClipInfo(
                    type = SmartClipType.OTP,
                    rawText = trimmed,
                    extractedValue = otpMatch,
                    quickPillLabel = "🔑 OTP: $otpMatch"
                )
            }
        }

        // 2. Web Links
        val urlMatch = urlRegex.find(trimmed)?.value
        if (!urlMatch.isNullOrBlank()) {
            val shortHost = urlMatch
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")
                .take(18)
            return SmartClipInfo(
                type = SmartClipType.LINK,
                rawText = trimmed,
                extractedValue = urlMatch,
                quickPillLabel = "🔗 $shortHost"
            )
        }

        // 3. Email Address
        val emailMatch = emailRegex.find(trimmed)?.value
        if (!emailMatch.isNullOrBlank()) {
            val shortEmail = if (emailMatch.length > 20) emailMatch.take(18) + "…" else emailMatch
            return SmartClipInfo(
                type = SmartClipType.EMAIL,
                rawText = trimmed,
                extractedValue = emailMatch,
                quickPillLabel = "✉️ $shortEmail"
            )
        }

        // 4. Phone Number (only when mostly numeric phone string)
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length in 7..15 && trimmed.length <= 22) {
            val phoneMatch = phoneRegex.find(trimmed)?.value?.trim()
            if (!phoneMatch.isNullOrBlank()) {
                return SmartClipInfo(
                    type = SmartClipType.PHONE,
                    rawText = trimmed,
                    extractedValue = phoneMatch,
                    quickPillLabel = "📞 $phoneMatch"
                )
            }
        }

        // 5. Generic Short Clip
        val preview = if (trimmed.length > 18) trimmed.take(16) + "…" else trimmed
        return SmartClipInfo(
            type = SmartClipType.TEXT,
            rawText = trimmed,
            extractedValue = trimmed,
            quickPillLabel = "📋 $preview"
        )
    }

    fun isHighPrioritySmartClip(info: SmartClipInfo): Boolean {
        return info.type != SmartClipType.TEXT && info.extractedValue.isNotBlank()
    }
}
