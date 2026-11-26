package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val JakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

// =========================================================================
// ZIGZAG (ZDS) Specific Typography Tokens
// =========================================================================
val ZigzagHeading = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 32.sp,
    lineHeight = 44.8.sp, // 1.4
    letterSpacing = 0.sp,
    color = ZigzagStrong,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagBody = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Normal, // 400
    fontSize = 16.sp,
    lineHeight = 22.sp,
    color = ZigzagForeground,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagMetadata = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Medium, // 500
    fontSize = 14.sp,
    lineHeight = 17.sp, // 1.21
    letterSpacing = 0.sp,
    color = ZigzagMuted,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagTabSelected = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 14.sp,
    lineHeight = 17.sp,
    color = ZigzagStrong,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagTabUnselected = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Medium, // 500
    fontSize = 14.sp,
    lineHeight = 17.sp,
    color = ZigzagMuted,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagTabBarLink = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Medium, // 500
    fontSize = 11.sp,
    lineHeight = 14.sp,
    color = ZigzagStrong,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagBuyCta = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Bold, // 700
    fontSize = 17.sp,
    lineHeight = 22.sp,
    color = ZigzagCtaText,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagMoreButton = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 15.sp,
    lineHeight = 20.sp,
    color = ZigzagPillText,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val ZigzagTextAction = TextStyle(
    fontFamily = JakartaFontFamily,
    fontWeight = FontWeight.Medium, // 500
    fontSize = 13.sp,
    lineHeight = 18.sp,
    color = ZigzagSecondary,
    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 44.8.sp,
        letterSpacing = 0.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    headlineLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    headlineMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    titleLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    titleMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    bodyLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = ZigzagForeground,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    bodyMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ZigzagForeground,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    bodySmall = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = ZigzagMuted,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    labelLarge = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    labelMedium = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    ),
    labelSmall = TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
    )
)
