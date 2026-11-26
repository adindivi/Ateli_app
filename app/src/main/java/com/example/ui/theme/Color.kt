package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// ZIGZAG (ZDS - KakaoStyle) Official Design System Tokens
// =========================================================================

// --- Colors ---
val ZigzagStrong = Color(0xFF121212)           // Selected tab, heading emphasis, dark control fill
val ZigzagCanvas = Color(0xFFFFFFFF)           // Pure white page & card surface canvas
val ZigzagForeground = Color(0xFF292B2B)       // Repeated body, card & reading text
val ZigzagSecondary = Color(0xFF606567)        // Supporting detail text, text actions
val ZigzagMuted = Color(0xFF878F91)            // Inactive tabs, metadata, cool-gray captions
val ZigzagHairline = Color(0xFFECEDEE)         // Product thumbnail placeholder, 1px thin control borders
val ZigzagAppBanner = Color(0xFFF1ECEA)        // Soft warm gray-beige app banner & subtle surfaces
val ZigzagPillText = Color(0xFF242729)         // More button / pill text

// --- Actions & Buttons ---
val ZigzagCtaBlack = Color(0xFF121314)         // Fixed bottom purchase/main CTA button
val ZigzagCtaText = Color(0xFFFAFAFB)          // CTA text color on dark buttons
val ZigzagPressOverlay = Color(0x08000000)      // Subtle dark veil (rgba 0,0,0, 0.03~0.05) on pressed

// =========================================================================
// Compatibility aliases mapped to ZIGZAG tokens
// =========================================================================
val AstryxLightCanvasBg = ZigzagCanvas
val AstryxLightCardBg = ZigzagCanvas
val AstryxLightCardSubtle = ZigzagAppBanner
val AstryxLightBorder = ZigzagHairline
val AstryxLightBorderFocus = ZigzagStrong

val AstryxLightTextPrimary = ZigzagStrong
val AstryxLightTextSecondary = ZigzagSecondary
val AstryxLightTextMuted = ZigzagMuted

val AstryxLightAccent = ZigzagCtaBlack
val AstryxLightAccentHover = ZigzagStrong
val AstryxLightAccentTint = ZigzagAppBanner
val AstryxLightPillBg = ZigzagHairline

val AtelierMidnight = ZigzagStrong
val AtelierDeepPlum = ZigzagCtaBlack
val AtelierChampagne = ZigzagCtaText // Fixed: Light text on dark surfaces (resolves black-on-black text bug)
val AtelierChampagneLight = ZigzagHairline
val AtelierCoral = Color(0xFFD9483B)
val AtelierSage = Color(0xFF2E7D5B)
val AtelierIvory = ZigzagCanvas
val AtelierCreamSurface = ZigzagAppBanner
val AtelierCardLight = ZigzagCanvas
val AtelierSlateText = ZigzagSecondary

val DarkSurface = Color(0xFF121212)
val DarkCard = Color(0xFF1E1E1E)
val DarkCardElevated = Color(0xFF2A2A2A)
