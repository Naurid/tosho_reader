package com.naurid.tosho.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================================================
// SENIOR DESIGNER PALETTE: Japanese Editorial Matte & Frosted Glass
// Designed for extended reading comfort: avoids eye-burning #FFFFFF white
// and harsh OLED #000000 halation.
// ========================================================================

// LIGHT THEME: Warm archival canvas & Frosted Acrylic
val LightBackground = Color(0xFFF6F5F2)          // Soft warm paper, non-glare
val LightSurface = Color(0xFFFAF9F6)             // Clean editorial off-white
val LightGlassBackground = Color(0xFFFFFFFF).copy(alpha = 0.95f) // Translucent frosted glass
val LightGlassBorder = Color(0xFF18181B).copy(alpha = 0.08f)     // Specular glass edge
val LightTextPrimary = Color(0xFF18181B)         // Deep graphite ink
val LightTextSecondary = Color(0xFF71717A)       // Muted zinc
val LightDivider = Color(0xFF18181B).copy(alpha = 0.06f)

// DARK THEME: Deep Obsidian Charcoal & Smoke Glass
val DarkBackground = Color(0xFF121316)           // Muted deep charcoal, eliminates halation
val DarkSurface = Color(0xFF18191E)              // Velvet container
val DarkGlassBackground = Color(0xFF18191E).copy(alpha = 0.95f)  // Translucent smoke glass
val DarkGlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)      // Crisp glass light-catch edge
val DarkTextPrimary = Color(0xFFF1F1F3)          // Soft ivory white
val DarkTextSecondary = Color(0xFFA1A1AA)        // Refined mid-tone zinc
val DarkDivider = Color(0xFFFFFFFF).copy(alpha = 0.08f)

// M3 Expressive Accents (Harmonious & Earthy)
val ExpressivePrimary = Color(0xFF688D74)        // Sage pine
val ExpressiveSecondary = Color(0xFF5DC080)      // Jade emerald
val ExpressiveTertiary = Color(0xFFD39C9D)       // Muted coral rose
