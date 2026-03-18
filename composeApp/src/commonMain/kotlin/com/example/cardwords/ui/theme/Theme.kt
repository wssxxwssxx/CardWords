package com.example.cardwords.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
// Midnight Blue palette — cool, deep, premium
// ═══════════════════════════════════════════════════════════════

// Primary — sky blue
val SkyBlue = Color(0xFF38BDF8)
val SkyBlueDark = Color(0xFF1E3A5F)
val SkyBlueMuted = Color(0xFF0EA5E9)

// Keep Amber for streak/learning accents
val Amber = Color(0xFFF59E0B)
val AmberDark = Color(0xFF2D2418)
val AmberMuted = Color(0xFFB89445)
val AmberLight = Color(0xFFE8C878)

// Secondary accent — soft gold (for stats)
val SoftGold = Color(0xFFF59E0B)

// Purple accent (dungeon, today marker)
val DungeonPurple = Color(0xFF7C3AED)
val DungeonPurpleLight = Color(0xFFA78BFA)

// Game accent — blue
val GameBlue = Color(0xFF38BDF8)
val GameBlueMuted = Color(0xFF818CF8)

// Secondary — muted indigo
val Indigo = Color(0xFF7B8CDE)
val IndigoDark = Color(0xFF1C1F35)
val IndigoMuted = Color(0xFF5A6ABE)

// Success
val Green40 = Color(0xFF16A34A)
val Green60 = Color(0xFF4ADE80)
val GreenDark = Color(0xFF0D2818)

// Error
val Red40 = Color(0xFFDC2626)
val Red60 = Color(0xFFFB7185)

// Amber for "learning" state
val Amber40 = Color(0xFFF59E0B)
val Amber60 = Color(0xFFFBBF24)

// Orange for streaks
val Orange40 = Color(0xFFF97316)
val Orange60 = Color(0xFFFB923C)

// ═══════════════════════════════════════════════════════════════
// Surface hierarchy — deep slate tones
// ═══════════════════════════════════════════════════════════════
val Surface0 = Color(0xFF0F172A)         // base background (deep navy)
val Surface1 = Color(0xFF1E293B)         // card level 1
val Surface2 = Color(0xFF334155)         // card level 2 / borders
val Surface3 = Color(0xFF475569)         // dialogs, modals

// ═══════════════════════════════════════════════════════════════
// Text hierarchy — cool slate tones
// ═══════════════════════════════════════════════════════════════
val TextPrimary = Color(0xFFF1F5F9)      // near white
val TextHeading = Color(0xFFF1F5F9)      // headings
val TextSecondary = Color(0xFF94A3B8)    // cool grey
val TextMuted = Color(0xFF64748B)        // muted slate
val TextDim = Color(0xFF475569)          // very dim

// ═══════════════════════════════════════════════════════════════
// Dark-only Color Scheme — Midnight Blue
// ═══════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Surface0,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SkyBlue,
    secondary = DungeonPurple,
    onSecondary = Surface0,
    secondaryContainer = Surface1,
    onSecondaryContainer = DungeonPurpleLight,
    tertiary = Amber,
    onTertiary = Surface0,
    tertiaryContainer = AmberDark,
    onTertiaryContainer = AmberLight,
    error = Red60,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Surface0,
    onBackground = TextPrimary,
    surface = Surface0,
    onSurface = TextPrimary,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    outlineVariant = Surface2,
)

// ═══════════════════════════════════════════════════════════════
// Typography — tight letter-spacing, premium feel
// ═══════════════════════════════════════════════════════════════
private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        color = TextHeading,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp,
        color = TextHeading,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
        color = TextHeading,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = (-0.1).sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.1).sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun CardWordsTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
