package com.example.model

enum class LanguageId(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val spacebarLabel: String,
    val isRtl: Boolean
) {
    BANGLA_PROBHAT("bn_probhat", "Bengali Probhat", "প্রভাত", "প্রভাত", false),
    BANGLA_PHONETIC("bn_phonetic", "Bengali Phonetic", "বাংলা (Avro)", "বাংলা", false),
    BANGLA_NATIONAL("bn_national", "Bengali National", "জাতীয়", "জাতীয়", false),
    ENGLISH("en", "English", "English", "EN", false),
    HINDI("hi", "Hindi", "हिन्दी", "HI", false),
    ARABIC("ar", "Arabic", "العربية", "AR", true);

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
    TYPEGENIUS_PURPLE("tg_purple", "TypeGenius Vibrant (Magenta/Purple)"),
    DARK_AMOLED("dark_amoled", "Deep AMOLED Black"),
    MIDNIGHT_BLUE("midnight_blue", "Midnight Ocean Blue"),
    LIGHT_MINIMAL("light_minimal", "Clean Minimalist Light");

    companion object {
        fun fromId(id: String): KeyboardTheme {
            return entries.find { it.id == id } ?: TYPEGENIUS_PURPLE
        }
    }
}

enum class KeyboardViewMode {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD,
    AI_TOOLS
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
