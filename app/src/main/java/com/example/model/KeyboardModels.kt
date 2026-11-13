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
    BANGLA_PROBHAT("bn_probhat", "Bengali (Probhat)", "বাংলা • প্রভাত", "প্রভাত", false, "South Asia"),
    BANGLA_PHONETIC("bn_phonetic", "Bengali (Avro Phonetic)", "বাংলা • অভ্র ফোনেটিক", "বাংলা", false, "South Asia"),
    BANGLA_NATIONAL("bn_national", "Bengali (National / Jatiya)", "বাংলা • জাতীয়", "জাতীয়", false, "South Asia"),
    BANGLA_UNIBIJOY("bn_unibijoy", "Bengali (UniBijoy)", "বাংলা • ইউনিবিজয়", "ইউনিবিজয়", false, "South Asia"),
    SYLHETI("syl", "Sylheti (Syloti Nagri & Bangla)", "সিলেটি • ꠍꠤꠟꠐꠤ ও বাংলা", "সিলেটি", false, "South Asia"),
    ENGLISH("en", "English (US)", "English (US)", "EN", false, "Global"),
    ENGLISH_UK("en_gb", "English (UK)", "English (UK)", "EN-UK", false, "Global"),
    HINDI("hi", "Hindi", "हिन्दी (Hindi)", "HI", false, "South Asia"),
    ARABIC("ar", "Arabic", "العربية (Arabic)", "AR", true, "Middle East"),
    URDU("ur", "Urdu", "اردو (Urdu)", "UR", true, "South Asia"),
    ASSAMESE("as", "Assamese", "অসমীয়া (Assamese)", "অসমীয়া", false, "South Asia"),
    NEPALI("ne", "Nepali", "नेपाली (Nepali)", "NE", false, "South Asia"),
    SANSKRIT("sa", "Sanskrit", "संस्कृतम् (Sanskrit)", "SA", false, "South Asia"),
    MARATHI("mr", "Marathi", "मराठी (Marathi)", "MR", false, "South Asia"),
    TAMIL("ta", "Tamil", "தமிழ் (Tamil)", "TA", false, "South Asia"),
    TELUGU("te", "Telugu", "తెలుగు (Telugu)", "TE", false, "South Asia"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી (Gujarati)", "GU", false, "South Asia"),
    PUNJABI("pa", "Punjabi (Gurmukhi)", "ਪੰਜਾਬੀ (Punjabi)", "PA", false, "South Asia"),
    MALAYALAM("ml", "Malayalam", "മലയാളം (Malayalam)", "ML", false, "South Asia"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ (Kannada)", "KN", false, "South Asia"),
    // Middle East & RTL
    PERSIAN("fa", "Persian (Farsi)", "فارسی (Persian)", "FA", true, "Middle East"),
    HEBREW("he", "Hebrew", "עברית (Hebrew)", "HE", true, "Middle East"),
    // European & Global
    SPANISH("es", "Spanish", "Español (Spanish)", "ES", false, "Europe & Americas"),
    FRENCH("fr", "French", "Français (French)", "FR", false, "Europe & Americas"),
    GERMAN("de", "German", "Deutsch (German)", "DE", false, "Europe & Americas"),
    PORTUGUESE("pt", "Portuguese", "Português (Portuguese)", "PT", false, "Europe & Americas"),
    ITALIAN("it", "Italian", "Italiano (Italian)", "IT", false, "Europe & Americas"),
    TURKISH("tr", "Turkish", "Türkçe (Turkish)", "TR", false, "Europe & Asia"),
    RUSSIAN("ru", "Russian", "Русский (Russian)", "RU", false, "Europe & Asia"),
    // Southeast & East Asia
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "ID", false, "Southeast Asia"),
    MALAY("ms", "Malay", "Bahasa Melayu", "MS", false, "Southeast Asia"),
    KOREAN("ko", "Korean", "한국어 (Korean)", "KO", false, "East Asia"),
    JAPANESE("ja", "Japanese (Kana)", "日本語 (Japanese)", "JP", false, "East Asia");

    val scriptLabel: String
        get() = when (this) {
            BANGLA_PROBHAT -> "বাংলা লে-আউট • প্রভাত"
            BANGLA_PHONETIC -> "অভ্র ফোনেটিক • বাংলা বানান"
            BANGLA_NATIONAL -> "বাংলা জাতীয় লে-আউট"
            BANGLA_UNIBIJOY -> "ইউনিবিজয় ক্লাসিক লে-আউট"
            SYLHETI -> "সিলেটি নাগরী ও বাংলা লিপি"
            ENGLISH, ENGLISH_UK -> "Latin Script • QWERTY"
            ARABIC, URDU, PERSIAN -> "RTL • Arabic Script"
            HEBREW -> "RTL • Hebrew Script"
            HINDI, NEPALI, MARATHI, SANSKRIT -> "Devanagari Script"
            ASSAMESE -> "অসমীয়া লিপি"
            else -> region
        }

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
    FULL("full", "Full"),
    LEFT("left", "Left"),
    RIGHT("right", "Right"),
    SPLIT("split", "Split");

    companion object {
        fun fromId(id: String): OneHandedMode = entries.find { it.id == id } ?: FULL
    }
}

enum class KeyboardHeightPreset(val id: String, val rowHeightDp: Int, val label: String) {
    COMPACT("compact", 56, "Compact"),
    NORMAL("normal", 66, "Normal"),
    TALL("tall", 76, "Tall");

    companion object {
        fun fromId(id: String): KeyboardHeightPreset = entries.find { it.id == id } ?: NORMAL
    }
}

enum class VibrationStrength(val id: String, val label: String, val durationMs: Long, val amplitude: Int) {
    OFF("off", "Off", 0L, 0),
    LIGHT("light", "Light", 10L, 60),
    MEDIUM("medium", "Medium", 18L, 145),
    STRONG("strong", "Strong", 28L, 235);

    companion object {
        fun fromId(id: String): VibrationStrength = entries.find { it.id == id } ?: MEDIUM
    }
}

enum class SoundProfile(val id: String, val label: String, val volume: Float) {
    SYSTEM("system", "Default Android Click", 0.50f),
    MECHANICAL("mechanical", "Mechanical Clicky (Blue Switch)", 0.95f),
    TYPEWRITER("typewriter", "Vintage Typewriter", 0.85f),
    THOCK("thock", "Creamy Thock", 0.70f),
    POPPY("poppy", "Bubble Pop / iOS Crisp", 0.60f);

    companion object {
        fun fromId(id: String): SoundProfile = entries.find { it.id == id } ?: SYSTEM
    }
}

enum class KeyboardViewMode {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2,
    NUMPAD,
    CALCULATOR,
    UTILITIES,
    EMOJI,
    STICKERS_KAOMOJI,
    CLIPBOARD,
    TEXT_EDITOR,
    TRANSLATOR,
    GEMINI_AI,
    THEME_PICKER,
    FANCY_FONTS,
    MORE_TOOLS
}

enum class KeyboardToolId(
    val id: String,
    val label: String,
    val testTag: String
) {
    TEXT_EDITING("text_editing", "Text edit", "toolbox_text_editing"),
    GEMINI_AI("gemini_ai", "TypeGenius AI", "toolbox_gemini"),
    EMOJI("emoji", "Emoji", "toolbox_emoji"),
    STICKERS("stickers", "Stickers", "toolbox_stickers"),
    CLIPBOARD("clipboard", "Clipboard", "toolbox_clipboard"),
    TRANSLATOR("translator", "Translate", "toolbox_translator"),
    CALCULATOR("calculator", "Calculator", "toolbox_calculator"),
    UTILITIES("utilities", "Notes & Date", "toolbox_utilities"),
    FANCY_FONTS("fancy_fonts", "Fonts", "toolbox_fonts"),
    THEMES("themes", "Themes", "toolbox_themes"),
    INCOGNITO("incognito", "Incognito", "toolbox_incognito"),
    SETTINGS("settings", "Settings", "toolbox_settings"),
    GIF("gif", "GIF", "toolbox_gif"),
    ONE_HANDED("one_handed", "One-hand", "toolbox_one_handed"),
    FLOATING("floating", "Floating", "toolbox_floating"),
    RESIZE("resize", "Resize", "toolbox_resize"),
    NUMPAD("numpad", "Numpad", "toolbox_numpad"),
    SHARE("share", "Share", "toolbox_share");

    companion object {
        val DEFAULT_TOOLBAR: List<KeyboardToolId> = listOf(
            TEXT_EDITING,
            GEMINI_AI,
            EMOJI,
            STICKERS,
            CLIPBOARD,
            TRANSLATOR,
            CALCULATOR,
            UTILITIES,
            FANCY_FONTS,
            THEMES,
            SETTINGS
        )

        fun fromId(id: String): KeyboardToolId? = entries.find { it.id == id }
    }
}

data class KeyAction(
    val label: String,
    val secondaryLabel: String? = null,
    val type: KeyType,
    val weight: Float = 1.0f,
    val extraVariants: List<String> = emptyList()
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
