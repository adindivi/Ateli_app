package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =========================================================================
// ZIGZAG (ZDS) Color Schemes
// =========================================================================
private val DarkColorScheme = darkColorScheme(
    primary = ZigzagCanvas,
    onPrimary = ZigzagStrong,
    primaryContainer = Color(0xFF242729),
    onPrimaryContainer = ZigzagCanvas,
    secondary = ZigzagHairline,
    onSecondary = ZigzagStrong,
    background = DarkSurface,
    onBackground = ZigzagCanvas,
    surface = DarkCard,
    onSurface = ZigzagCanvas,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = ZigzagMuted,
    outline = Color(0xFF333738)
)

private val LightColorScheme = lightColorScheme(
    primary = ZigzagCtaBlack,            // #121314 (메인 구매/액션 CTA)
    onPrimary = ZigzagCtaText,           // #FAFAFB
    primaryContainer = ZigzagAppBanner,  // #F1ECEA
    onPrimaryContainer = ZigzagStrong,   // #121212
    secondary = ZigzagSecondary,         // #606567
    onSecondary = ZigzagCanvas,
    secondaryContainer = ZigzagHairline, // #ECEDEE
    onSecondaryContainer = ZigzagForeground, // #292B2B
    tertiary = ZigzagPillText,           // #242729
    onTertiary = ZigzagCanvas,
    background = ZigzagCanvas,           // #FFFFFF (순백색 캔버스)
    onBackground = ZigzagForeground,     // #292B2B
    surface = ZigzagCanvas,              // #FFFFFF
    onSurface = ZigzagForeground,        // #292B2B
    surfaceVariant = ZigzagAppBanner,    // #F1ECEA
    onSurfaceVariant = ZigzagSecondary,  // #606567
    outline = ZigzagHairline,            // #ECEDEE (1px hairline)
    outlineVariant = ZigzagHairline
)

@Composable
fun AteliTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
