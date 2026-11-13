package com.example.model

enum class LanguageId(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val spacebarLabel: String,
    val isRtl: Boolean,
    val region: String
) {
    // South Asian, Bengali & Sylheti Packages
    BANGLA_PROBHAT("bn_probhat", "Bengali (Probhat)", "বাংলা (প্রভাত)", "প্রভাত", false, "South Asia"),
    BANGLA_PHONETIC("bn_phonetic", "Bengali (Avro Phonetic)", "বাংলা (ফোনেটিক)", "বাংলা", false, "South Asia"),
    BANGLA_NATIONAL("bn_national", "Bengali (National)", "বাংলা (জাতীয়)", "জাতীয়", false, "South Asia"),
    SYLHETI("syl", "Sylheti (Syloti Nagri & Bangla)", "সিলেটি (ꠍꠤꠟꠐꠤ)", "সিলেটি", false, "South Asia"),
    ENGLISH("en", "English (US)", "English (US)", "EN", false, "Global"),
    ENGLISH_UK("en_gb", "English (UK)", "English (UK)", "EN-UK", false, "Global"),
    HINDI("hi", "Hindi", "हिन्दी", "HI", false, "South Asia"),
    ARABIC("ar", "Arabic", "العربية", "AR", true, "Middle East"),
    URDU("ur", "Urdu", "اردو", "UR", true, "South Asia"),
    ASSAMESE("as", "Assamese", "অসমীয়া", "অসমীয়া", false, "South Asia"),
    NEPALI("ne", "Nepali", "नेपाली", "NE", false, "South Asia"),
    SANSKRIT("sa", "Sanskrit", "संस्कृतम्", "SA", false, "South Asia"),
    MARATHI("mr", "Marathi", "मराठी", "MR", false, "South Asia"),
    TAMIL("ta", "Tamil", "தமிழ்", "TA", false, "South Asia"),
    TELUGU("te", "Telugu", "తెలుగు", "TE", false, "South Asia"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી", "GU", false, "South Asia"),
    PUNJABI("pa", "Punjabi (Gurmukhi)", "ਪੰਜਾਬੀ", "PA", false, "South Asia"),
    MALAYALAM("ml", "Malayalam", "മലയാളം", "ML", false, "South Asia"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", "KN", false, "South Asia"),
    // Middle East & RTL
    PERSIAN("fa", "Persian (Farsi)", "فارسی", "FA", true, "Middle East"),
    HEBREW("he", "Hebrew", "עברית", "HE", true, "Middle East"),
    // European & Global
    SPANISH("es", "Spanish", "Español", "ES", false, "Europe & Americas"),
    FRENCH("fr", "French", "Français", "FR", false, "Europe & Americas"),
    GERMAN("de", "German", "Deutsch", "DE", false, "Europe & Americas"),
    PORTUGUESE("pt", "Portuguese", "Português", "PT", false, "Europe & Americas"),
    ITALIAN("it", "Italian", "Italiano", "IT", false, "Europe & Americas"),
    TURKISH("tr", "Turkish", "Türkçe", "TR", false, "Europe & Asia"),
    RUSSIAN("ru", "Russian", "Русский", "RU", false, "Europe & Asia"),
    // Southeast & East Asia
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "ID", false, "Southeast Asia"),
    MALAY("ms", "Malay", "Bahasa Melayu", "MS", false, "Southeast Asia"),
    KOREAN("ko", "Korean", "한국어", "KO", false, "East Asia"),
    JAPANESE("ja", "Japanese (Kana)", "日本語", "JP", false, "East Asia");

    companion object {
        fun fromCode(code: String): LanguageId {
            return entries.find { it.code == code } ?: BANGLA_PROBHAT
        }
    }
}

enum class SwipeSensitivity(val thresholdDp: Float, val label: String) {
    LOW(80f, "Low (Requires longer swipe)"),
    MEDIUM(50f, "Medium (Balanced)"),
    HIGH(30f, "High (Quick responsive swipe)");

    companion object {
        fun fromName(name: String): SwipeSensitivity {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class KeyboardTheme(val id: String, val displayName: String) {
    MATERIAL_YOU("material_you", "Material You (Dynamic Wallpaper)"),
    TYPEGENIUS_PURPLE("tg_purple", "TypeGenius Vibrant (Magenta/Purple)"),
    RIDMIK_CLASSIC_DARK("classic_dark", "Classic Charcoal Dark"),
    DARK_AMOLED("dark_amoled", "Deep AMOLED Black"),
    MIDNIGHT_BLUE("midnight_blue", "Midnight Ocean Blue"),
    EMERALD_FOREST("emerald_forest", "Emerald Cyber Dark"),
    LIGHT_MINIMAL("light_minimal", "Clean Minimalist Light");

    companion object {
        fun fromId(id: String): KeyboardTheme {
            return entries.find { it.id == id } ?: MATERIAL_YOU
        }
    }
}

enum class OneHandedMode(val id: String, val label: String) {
    FULL("full", "Full Width"),
    LEFT("left", "Left-Handed"),
    RIGHT("right", "Right-Handed");

    companion object {
        fun fromId(id: String): OneHandedMode = entries.find { it.id == id } ?: FULL
    }
}

enum class KeyboardHeightPreset(val id: String, val rowHeightDp: Int, val label: String) {
    COMPACT("compact", 39, "Compact"),
    NORMAL("normal", 45, "Normal"),
    TALL("tall", 51, "Tall");

    companion object {
        fun fromId(id: String): KeyboardHeightPreset = entries.find { it.id == id } ?: NORMAL
    }
}

enum class KeyboardViewMode {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2,
    NUMPAD,
    EMOJI,
    CLIPBOARD,
    TEXT_EDITOR,
    TRANSLATOR,
    THEME_PICKER,
    MORE_TOOLS
}

data class KeyAction(
    val label: String,
    val secondaryLabel: String? = null,
    val type: KeyType,
    val weight: Float = 1.0f
)

enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    SPACEBAR,
    ENTER,
    MODE_SWITCH,
    LANGUAGE_SWITCH,
    EMOJI_PICKER,
    CLIPBOARD,
    AI_TOOLS,
    VOICE,
    SETTINGS
}
