package com.example.ui.keyboard

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
    val enterKeyText: Color,
    val pillKeyBackground: Color = keySpecialBackground
)

object KeyboardThemeData {

    fun getColors(
        theme: KeyboardTheme,
        context: Context? = null,
        isSystemDark: Boolean = true
    ): KeyboardColors {
        if (theme == KeyboardTheme.MATERIAL_YOU) {
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val m3 = if (isSystemDark) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                    return KeyboardColors(
                        background = m3.surfaceContainerLowest,
                        keyBackground = m3.surfaceContainerHigh,
                        keySpecialBackground = m3.surfaceContainerHighest,
                        keyPressedBackground = m3.primaryContainer,
                        textPrimary = m3.onSurface,
                        textSecondary = m3.onSurfaceVariant,
                        accent = m3.primary,
                        suggestionBackground = m3.surfaceContainerLow,
                        enterKeyBackground = m3.primary,
                        enterKeyText = m3.onPrimary,
                        pillKeyBackground = m3.secondaryContainer
                    )
                } catch (_: Throwable) {
                    // Fallback gracefully if dynamic color extraction is unavailable
                }
            }
            return if (isSystemDark) {
                KeyboardColors(
                    background = Color(0xFF121316),
                    keyBackground = Color(0xFF282A2F),
                    keySpecialBackground = Color(0xFF34373E),
                    keyPressedBackground = Color(0xFF474C57),
                    textPrimary = Color(0xFFE3E5EA),
                    textSecondary = Color(0xFFA8ADB8),
                    accent = Color(0xFFD2E3FC),
                    suggestionBackground = Color(0xFF1A1B1F),
                    enterKeyBackground = Color(0xFFE8EEF8),
                    enterKeyText = Color(0xFF181C24),
                    pillKeyBackground = Color(0xFF333842)
                )
            } else {
                KeyboardColors(
                    background = Color(0xFFEFF1F5),
                    keyBackground = Color(0xFFFFFFFF),
                    keySpecialBackground = Color(0xFFDDE2EC),
                    keyPressedBackground = Color(0xFFC7D0E0),
                    textPrimary = Color(0xFF1A1C1E),
                    textSecondary = Color(0xFF5E636B),
                    accent = Color(0xFF1B6EF3),
                    suggestionBackground = Color(0xFFE4E8F0),
                    enterKeyBackground = Color(0xFF1B6EF3),
                    enterKeyText = Color(0xFFFFFFFF),
                    pillKeyBackground = Color(0xFFD3E3FD)
                )
            }
        }

        return when (theme) {
            KeyboardTheme.MATERIAL_YOU -> error("Handled above")
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
            KeyboardTheme.RIDMIK_CLASSIC_DARK -> KeyboardColors(
                background = Color(0xFF1B1C1E),
                keyBackground = Color(0xFF2D2F33),
                keySpecialBackground = Color(0xFF232528),
                keyPressedBackground = Color(0xFF42454B),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFAEB4BE),
                accent = Color(0xFF00C853),
                suggestionBackground = Color(0xFF141517),
                enterKeyBackground = Color(0xFF26A69A),
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
            KeyboardTheme.EMERALD_FOREST -> KeyboardColors(
                background = Color(0xFF071A15),
                keyBackground = Color(0xFF0F2E26),
                keySpecialBackground = Color(0xFF174539),
                keyPressedBackground = Color(0xFF1F5E4E),
                textPrimary = Color(0xFFECFFF9),
                textSecondary = Color(0xFF8AD4BF),
                accent = Color(0xFF00E676),
                suggestionBackground = Color(0xFF0A221C),
                enterKeyBackground = Color(0xFF00E676),
                enterKeyText = Color(0xFF042214)
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
