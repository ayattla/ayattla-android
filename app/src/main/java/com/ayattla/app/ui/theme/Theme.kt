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

/** Brand tokens: the iOS app's AyatDesign/Brand.swift values (light / dark), one for one. */
@Immutable
data class Brand(
    val green: Color,
    val greenDeep: Color,
    val teal: Color,
    val primary: Color,
    val primaryInk: Color,
    val g0: Color,
    val deep: Color,
    val accent: Color,
    val goldInk: Color,
    val goldSoft: Color,
    val newInk: Color,
    val newFill: Color,
    val onDeepAccent: Color,
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
) {
    /** --gradient-brand (deep emerald). */
    val gradient get() = listOf(g0, primary, deep)
    /** --s-grad: the site's green to teal. */
    val siteGradient get() = listOf(Color(0xFF2E9145), Color(0xFF1F8A62), Color(0xFF0F7E86))
    val goldGradient get() = listOf(Color(0xFFB7791F), Color(0xFFE2B04A))
    val shadow get() = Color(0xFF0F1F17)
}

/** The site's tile colours, cycled by position (Brand.tileGradients). */
val TileGradients = listOf(
    listOf(Color(0xFF2E9145), Color(0xFF0F7E86)),
    listOf(Color(0xFF0F7E86), Color(0xFF3BB3B0)),
    listOf(Color(0xFFB7791F), Color(0xFFE2B04A)),
    listOf(Color(0xFF1D5D8C), Color(0xFF4F9BD1)),
    listOf(Color(0xFF1FA855), Color(0xFF8AD16A)),
)
fun tileGradient(i: Int) = TileGradients[((i % 5) + 5) % 5]

val LightBrand = Brand(
    green = Color(0xFF2E9145), greenDeep = Color(0xFF23753A), teal = Color(0xFF0F7E86),
    primary = Color(0xFF102D27), primaryInk = Color(0xFF23753A),
    g0 = Color(0xFF17392F), deep = Color(0xFF0B211C),
    accent = Color(0xFFC3A66B), goldInk = Color(0xFF7A5F28), goldSoft = Color(0xFFF1E8D3),
    newInk = Color(0xFF8A5A0F), newFill = Color(0xFFF9EED8), onDeepAccent = Color(0xFFD9C28E),
    background = Color(0xFFF4F8F5), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF4F8F5),
    ink = Color(0xFF0F1F17), muted = Color(0xFF5A6B61), tint = Color(0xFFE6F2E9),
    line = Color(0xFFE3EBE5), lineStrong = Color(0xFFCFDCD3), onPrimary = Color(0xFFFFFFFF),
    dark = false,
)

val DarkBrand = Brand(
    green = Color(0xFF4CBF63), greenDeep = Color(0xFF6BD07F), teal = Color(0xFF0F7E86),
    primary = Color(0xFF1B4238), primaryInk = Color(0xFF6BD07F),
    g0 = Color(0xFF143128), deep = Color(0xFF0A1814),
    accent = Color(0xFFD9C28E), goldInk = Color(0xFFD9C28E), goldSoft = Color(0xFF2A2A1C),
    newInk = Color(0xFFE2B04A), newFill = Color(0xFF2B2414), onDeepAccent = Color(0xFFD9C28E),
    background = Color(0xFF0B120E), surface = Color(0xFF121D16), surface2 = Color(0xFF111B15),
    ink = Color(0xFFEAF2EC), muted = Color(0xFFA3B5A9), tint = Color(0xFF16271C),
    line = Color(0xFF223429), lineStrong = Color(0xFF2E4536), onPrimary = Color(0xFFFFFFFF),
    dark = true,
)

/** Tool colours by kind (the iOS ToolAccent), light / dark. */
object ToolAccent {
    private val worship = Color(0xFF9A6516) to Color(0xFFD9A441)
    private val prayer = Color(0xFF1D6670) to Color(0xFF5FB3BC)
    private val calendar = Color(0xFF2E4380) to Color(0xFF93A6E0)
    private val money = Color(0xFF7A5F28) to Color(0xFFCBB06E)
    private val hadith = Color(0xFF7A4E2D) to Color(0xFFC9935E)
    private val quran = Color(0xFF2E9145) to Color(0xFF4FBF6A)
    fun color(id: String, dark: Boolean): Color {
        val p = when (id) {
            "azkar", "duas", "tasbih", "hisn-almuslim", "ruqya" -> worship
            "prayer-times", "qibla", "nearby-mosques", "prayer-guide" -> prayer
            "hijri", "occasions", "sawm-tracker" -> calendar
            "zakat", "qada-salah" -> money
            "hadith", "hadith-checker", "hadith-card" -> hadith
            else -> quran
        }
        return if (dark) p.second else p.first
    }
}

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

/** iOS text styles in IBM Plex Sans Arabic: largeTitle 34, title 28, title2 22, title3 20, headline 17,
 *  body 17, callout 16, subheadline 15, footnote 13, caption 12, caption2 11. */
private fun typography(): Typography {
    // Centred by default (the owner's rule: text and icons sit in the middle, not at the right edge).
    // A text that must start at the edge (a list row's title, a field) sets textAlign itself.
    fun t(size: Int, w: FontWeight = FontWeight.Normal, lh: Float = 1.35f) =
        TextStyle(fontFamily = Fonts.ui, fontSize = size.sp, fontWeight = w, lineHeight = (size * lh).sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    return Typography(
        displayLarge = t(40, FontWeight.Bold), displayMedium = t(36, FontWeight.Bold), displaySmall = t(34, FontWeight.Bold),
        headlineLarge = t(34, FontWeight.SemiBold), headlineMedium = t(28, FontWeight.Bold), headlineSmall = t(22, FontWeight.Bold),
        titleLarge = t(20, FontWeight.Medium), titleMedium = t(17, FontWeight.SemiBold), titleSmall = t(16, FontWeight.Medium),
        bodyLarge = t(17, lh = 1.5f), bodyMedium = t(15, lh = 1.5f), bodySmall = t(13, lh = 1.45f),
        labelLarge = t(15, FontWeight.SemiBold), labelMedium = t(12, FontWeight.Medium), labelSmall = t(11, FontWeight.Medium),
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
        primary = b.green, onPrimary = b.onPrimary, secondary = b.accent, background = b.background,
        surface = b.surface, surfaceVariant = b.surface2, onBackground = b.ink, onSurface = b.ink,
        onSurfaceVariant = b.muted, outline = b.lineStrong, outlineVariant = b.line,
        primaryContainer = b.primary, onPrimaryContainer = b.onPrimary, secondaryContainer = b.tint,
        onSecondaryContainer = b.ink, surfaceContainer = b.surface, surfaceContainerHigh = b.surface2,
        surfaceContainerLow = b.surface, surfaceContainerHighest = b.surface2, surfaceContainerLowest = b.background,
    ) else lightColorScheme(
        primary = b.green, onPrimary = b.onPrimary, secondary = b.goldInk, background = b.background,
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
