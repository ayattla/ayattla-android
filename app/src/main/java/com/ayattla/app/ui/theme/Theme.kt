package com.ayattla.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayattla.app.R

/** Brand tokens, same values as the site's tokens.css and the iOS app (docs/DESIGN_SYSTEM.md). */
@Immutable
data class Brand(
    val primary: Color,
    val primaryInk: Color,
    val g0: Color,
    val deep: Color,
    val accent: Color,
    val goldInk: Color,
    val goldSoft: Color,
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val tint: Color,
    val line: Color,
    val lineStrong: Color,
    val onPrimary: Color,
    val dark: Boolean,
)

val LightBrand = Brand(
    primary = Color(0xFF102D27), primaryInk = Color(0xFF14463A),
    g0 = Color(0xFF17392F), deep = Color(0xFF0B211C),
    accent = Color(0xFFC3A66B), goldInk = Color(0xFF7A5F28), goldSoft = Color(0xFFF1E8D3),
    background = Color(0xFFF6F2E9), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEFE9DC),
    ink = Color(0xFF202823), muted = Color(0xFF5C625D), tint = Color(0xFFE4ECE6),
    line = Color(0xFFE3EBE5), lineStrong = Color(0xFFCFDCD3), onPrimary = Color(0xFFF6F2E9),
    dark = false,
)

val DarkBrand = Brand(
    primary = Color(0xFF1B4238), primaryInk = Color(0xFFD9C28E),
    g0 = Color(0xFF143128), deep = Color(0xFF0A1814),
    accent = Color(0xFFD9C28E), goldInk = Color(0xFFD9C28E), goldSoft = Color(0xFF2A3A2C),
    background = Color(0xFF0C1C18), surface = Color(0xFF11261F), surface2 = Color(0xFF173129),
    ink = Color(0xFFF1ECE1), muted = Color(0xFFB9B3A5), tint = Color(0xFF1E3A30),
    line = Color(0xFF22392F), lineStrong = Color(0xFF2E4A3E), onPrimary = Color(0xFFF6F2E9),
    dark = true,
)

val LocalBrand = staticCompositionLocalOf { LightBrand }

object Fonts {
    val ui = FontFamily(
        Font(R.font.plex_regular, FontWeight.Normal),
        Font(R.font.plex_medium, FontWeight.Medium),
        Font(R.font.plex_semibold, FontWeight.SemiBold),
        Font(R.font.plex_bold, FontWeight.Bold),
    )
    /** Mushaf text only. */
    val quran = FontFamily(Font(R.font.amiri_quran))
    /** Azkar and hadith text. */
    val naskh = FontFamily(Font(R.font.amiri))
}

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.ui() = copy(fontFamily = Fonts.ui)
    return Typography(
        displayLarge = base.displayLarge.ui(), displayMedium = base.displayMedium.ui(), displaySmall = base.displaySmall.ui(),
        headlineLarge = base.headlineLarge.ui().copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.ui().copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.ui().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.ui().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.ui().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.ui().copy(fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.ui().copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.ui().copy(lineHeight = 22.sp),
        bodySmall = base.bodySmall.ui(),
        labelLarge = base.labelLarge.ui().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.ui(), labelSmall = base.labelSmall.ui(),
    )
}

enum class ThemeMode { System, Light, Dark }

@Composable
fun AyatTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val b = if (dark) DarkBrand else LightBrand
    val scheme = if (dark) darkColorScheme(
        primary = b.accent, onPrimary = b.deep, secondary = b.accent, background = b.background,
        surface = b.surface, surfaceVariant = b.surface2, onBackground = b.ink, onSurface = b.ink,
        onSurfaceVariant = b.muted, outline = b.lineStrong, outlineVariant = b.line,
        primaryContainer = b.primary, onPrimaryContainer = b.onPrimary, secondaryContainer = b.tint,
        onSecondaryContainer = b.ink, surfaceContainer = b.surface, surfaceContainerHigh = b.surface2,
        surfaceContainerLow = b.surface, surfaceContainerHighest = b.surface2, surfaceContainerLowest = b.background,
    ) else lightColorScheme(
        primary = b.primary, onPrimary = b.onPrimary, secondary = b.goldInk, background = b.background,
        surface = b.surface, surfaceVariant = b.surface2, onBackground = b.ink, onSurface = b.ink,
        onSurfaceVariant = b.muted, outline = b.lineStrong, outlineVariant = b.line,
        primaryContainer = b.primary, onPrimaryContainer = b.onPrimary, secondaryContainer = b.tint,
        onSecondaryContainer = b.ink, surfaceContainer = b.surface, surfaceContainerHigh = b.surface2,
        surfaceContainerLow = b.surface, surfaceContainerHighest = b.surface2, surfaceContainerLowest = b.background,
    )
    CompositionLocalProvider(LocalBrand provides b) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}
