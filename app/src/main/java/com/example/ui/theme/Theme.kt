package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TGMagenta,
    onPrimary = Color.White,
    secondary = TGViolet,
    onSecondary = Color.White,
    tertiary = TGCyan,
    onTertiary = Color.Black,
    background = TGBgDark,
    onBackground = TGOnSurfaceDark,
    surface = TGSurfaceDark,
    onSurface = TGOnSurfaceDark,
    surfaceVariant = TGSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD4C8E8)
)

private val LightColorScheme = lightColorScheme(
    primary = TGMagenta,
    onPrimary = Color.White,
    secondary = TGViolet,
    onSecondary = Color.White,
    tertiary = TGCyan,
    onTertiary = Color.Black,
    background = TGBgLight,
    onBackground = TGOnSurfaceLight,
    surface = TGSurfaceLight,
    onSurface = TGOnSurfaceLight,
    surfaceVariant = TGSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4C3D66)
)

@Composable
fun TypeGeniusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
