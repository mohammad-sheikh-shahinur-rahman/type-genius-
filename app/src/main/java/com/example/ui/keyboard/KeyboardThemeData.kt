package com.example.ui.keyboard

import androidx.compose.ui.graphics.Color
import com.example.model.KeyboardTheme

data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val keySpecialBackground: Color,
    val keyPressedBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val suggestionBackground: Color,
    val enterKeyBackground: Color,
    val enterKeyText: Color
)

object KeyboardThemeData {
    fun getColors(theme: KeyboardTheme): KeyboardColors {
        return when (theme) {
            KeyboardTheme.TYPEGENIUS_PURPLE -> KeyboardColors(
                background = Color(0xFF140924),
                keyBackground = Color(0xFF26143F),
                keySpecialBackground = Color(0xFF3B1D5F),
                keyPressedBackground = Color(0xFF562888),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFC7B1E6),
                accent = Color(0xFFFF3377),
                suggestionBackground = Color(0xFF1B0E2E),
                enterKeyBackground = Color(0xFFFF3377),
                enterKeyText = Color(0xFFFFFFFF)
            )
            KeyboardTheme.DARK_AMOLED -> KeyboardColors(
                background = Color(0xFF000000),
                keyBackground = Color(0xFF1A1A1A),
                keySpecialBackground = Color(0xFF2C2C2C),
                keyPressedBackground = Color(0xFF3D3D3D),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFF9E9E9E),
                accent = Color(0xFF00E5FF),
                suggestionBackground = Color(0xFF0D0D0D),
                enterKeyBackground = Color(0xFF00E5FF),
                enterKeyText = Color(0xFF000000)
            )
            KeyboardTheme.MIDNIGHT_BLUE -> KeyboardColors(
                background = Color(0xFF0A1428),
                keyBackground = Color(0xFF132240),
                keySpecialBackground = Color(0xFF1D335E),
                keyPressedBackground = Color(0xFF2A4880),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFF8BA2CE),
                accent = Color(0xFF00B0FF),
                suggestionBackground = Color(0xFF0D1B36),
                enterKeyBackground = Color(0xFF00B0FF),
                enterKeyText = Color(0xFFFFFFFF)
            )
            KeyboardTheme.LIGHT_MINIMAL -> KeyboardColors(
                background = Color(0xFFF2F4F7),
                keyBackground = Color(0xFFFFFFFF),
                keySpecialBackground = Color(0xFFE4E7EC),
                keyPressedBackground = Color(0xFFD0D5DD),
                textPrimary = Color(0xFF101828),
                textSecondary = Color(0xFF667085),
                accent = Color(0xFF7F56D9),
                suggestionBackground = Color(0xFFEAECF0),
                enterKeyBackground = Color(0xFF7F56D9),
                enterKeyText = Color(0xFFFFFFFF)
            )
        }
    }
}
