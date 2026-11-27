package com.ayattla.app.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.layout.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ayattla.app.R
import com.ayattla.app.ui.theme.Brand
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.tileGradient
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Shapes and spacing: Brand.Radius / Brand.Space of the iOS app.
object Radius { val small = 12.dp; val card = 18.dp; val tile = 16.dp; val hero = 24.dp; val feature = 28.dp }
val CardShape = RoundedCornerShape(Radius.card)
val SmallShape = RoundedCornerShape(Radius.small)
val ButtonShape = RoundedCornerShape(14.dp)

private val Brand.siteBrush get() = Brush.linearGradient(siteGradient)
private fun hBrush(c: List<Color>) = Brush.horizontalGradient(c)

// ───────────────────────── Surfaces (AyatDesign/Surfaces.swift) ─────────────────────────

enum class Surface { Raised, Flat, Tinted, Gold }

/** `.surface(level)`: the site's card. White, hairline border and the two soft shadows. */
/**
 * Lets a sideways row run under the screen's side padding, edge to edge, so cards scroll to the
 * edge and their shadows are not cut by the parent's padding.
 */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { m, c ->
    val p = horizontal.roundToPx()
    val pl = m.measure(c.copy(minWidth = c.minWidth + 2 * p, maxWidth = c.maxWidth + 2 * p))
    layout(pl.width - 2 * p, pl.height) { pl.place(-p, 0) }
}

fun Modifier.surface(b: Brand, level: Surface = Surface.Raised, radius: Dp = Radius.card): Modifier {
    val shape = RoundedCornerShape(radius)
    val fill = when (level) { Surface.Raised, Surface.Flat -> b.surface; Surface.Tinted -> b.surface2; Surface.Gold -> b.goldSoft }
    val border = if (level == Surface.Gold) b.accent.copy(alpha = 0.45f) else b.line
    val shadowed = if (level == Surface.Raised && !b.dark) this
        .softShadow(shape, 14.dp, b.shadow.copy(alpha = 0.08f), DpOffset(0.dp, 6.dp))
        .softShadow(shape, 1.dp, b.shadow.copy(alpha = 0.05f), DpOffset(0.dp, 1.dp))
    else this
    return shadowed.clip(shape).background(fill).border(1.dp, border, shape)
}

/** Deep emerald panel with the Islamic pattern and the inset gold frame (Quran content). */
fun Modifier.heroSurface(b: Brand, radius: Dp = Radius.hero): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .softShadow(shape, 20.dp, Color(0xFF0B211C).copy(alpha = 0.25f), DpOffset(0.dp, 12.dp))
        .clip(shape)
        .background(Brush.linearGradient(b.gradient))
        .drawBehind {
            drawIslamicPattern(b.accent.copy(alpha = 0.10f))
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent), center = Offset(0f, 0f), radius = 320.dp.toPx()))
        }
        .padding(7.dp)
        .border(1.dp, b.accent.copy(alpha = 0.40f), RoundedCornerShape(max(radius.value - 7, 4f).dp))
}

/** Green to teal feature panel with the white circle top-right (white text). */
fun Modifier.featureSurface(b: Brand, radius: Dp = Radius.feature): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .softShadow(shape, 24.dp, Color(0xFF1F8A62).copy(alpha = 0.45f), DpOffset(0.dp, 16.dp))
        .clip(shape)
        .background(b.siteBrush)
        .drawBehind {
            // RTL: the iOS top-trailing corner is the top-left.
            drawCircle(Color.White.copy(alpha = 0.10f), radius = 90.dp.toPx(), center = Offset(30.dp.toPx(), 20.dp.toPx()))
        }
}

/** The site's .page-head: white card with a green glow and a teal glow. */
fun Modifier.pageHeadSurface(b: Brand, radius: Dp = Radius.hero): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .then(if (b.dark) Modifier else Modifier.softShadow(shape, 18.dp, b.shadow.copy(alpha = 0.10f), DpOffset(0.dp, 12.dp)))
        .clip(shape)
        .background(b.surface)
        .drawBehind {
            // RTL mirror of UnitPoint(0.15, 0) and (0.9, 1).
            drawRect(Brush.radialGradient(listOf(b.green.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.85f, 0f), 260.dp.toPx()))
            drawRect(Brush.radialGradient(listOf(b.teal.copy(alpha = 0.13f), Color.Transparent), Offset(size.width * 0.1f, size.height), 240.dp.toPx()))
        }
        .border(1.dp, b.line, shape)
}

/** Screen backdrop: mint page with the faint glow under the bar (`.patternedScreen()`). */
fun Modifier.patternedScreen(b: Brand, strong: Boolean = false): Modifier = this
    .background(b.background)
    .drawBehind {
        val h = (if (strong) 620.dp else 360.dp).toPx()
        drawRect(
            Brush.radialGradient(listOf(b.green.copy(alpha = if (strong) 0.18f else 0.10f), Color.Transparent), Offset(size.width * 0.9f, 0f), (if (strong) 380.dp else 320.dp).toPx()),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
        drawRect(
            Brush.radialGradient(listOf(b.teal.copy(alpha = if (strong) 0.10f else 0.08f), Color.Transparent), Offset(size.width * 0.05f, h * if (strong) 0.6f else 0.2f), (if (strong) 320.dp else 280.dp).toPx()),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
    }

/** The site's star lattice: two rotated squares per cell, hairline gold. */
fun DrawScope.drawIslamicPattern(color: Color, cellDp: Dp = 46.dp, lineWidth: Dp = 0.8.dp) {
    val cell = cellDp.toPx()
    val r = cell * 0.34f
    val path = Path()
    var y = 0f
    var row = 0
    while (y <= size.height + cell) {
        var x = if (row % 2 == 0) 0f else cell / 2
        while (x <= size.width + cell) {
            for (k in 0..1) {
                val a0 = k * Math.PI / 4
                for (i in 0..3) {
                    val a = a0 + i * Math.PI / 2 + Math.PI / 4
                    val px = x + r * cos(a).toFloat()
                    val py = y + r * sin(a).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            x += cell
        }
        y += cell * 0.5f
        row++
    }
    drawPath(path, color, style = Stroke(lineWidth.toPx()))
}

/** A full screen with the patterned backdrop. */
@Composable
fun BrandScreen(modifier: Modifier = Modifier, strong: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().patternedScreen(LocalBrand.current, strong), content = content)
}

/** Press feedback as iOS `.pressable`: scale 0.975. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return this.scale(if (pressed) 0.975f else 1f).clickable(source, indication = null, onClick = onClick)
}

// ───────────────────────── Cards and panels ─────────────────────────

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, level: Surface = Surface.Raised, content: @Composable ColumnScope.() -> Unit) {
    // Contents centred by default; full-width rows are unaffected.
    val b = LocalBrand.current
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .surface(b, level)
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Deep emerald hero (Quran content), centered. */
@Composable
fun EmeraldPanel(modifier: Modifier = Modifier, radius: Dp = Radius.hero, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Column(
        modifier.fillMaxWidth().heroSurface(b, radius).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
fun FeaturePanel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Column(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.pressable(onClick) else Modifier).featureSurface(b).padding(20.dp),
        content = content,
    )
}

// ───────────────────────── Text components ─────────────────────────

/** The site's .eyebrow: soft gradient capsule, gradient dot, bold green-deep text. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val b = LocalBrand.current
    Row(
        modifier
            .clip(CircleShape)
            .background(if (onDark) hBrush(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.06f))) else hBrush(listOf(b.green.copy(alpha = 0.10f), b.teal.copy(alpha = 0.10f))))
            .border(1.dp, if (onDark) b.onDeepAccent.copy(alpha = 0.35f) else b.green.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(b.siteBrush))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (onDark) b.onDeepAccent else b.greenDeep)
    }
}

/** Section heading: gradient rule, eyebrow, title with the green highlighter, subtitle. */
@Composable
fun SiteSectionHead(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, subtitle: String? = null) {
    val b = LocalBrand.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(44.dp).height(4.dp).clip(CircleShape).background(b.siteBrush))
        if (eyebrow != null) Eyebrow(eyebrow)
        Text(
            title, style = MaterialTheme.typography.headlineSmall, color = b.ink, textAlign = TextAlign.Center,
            modifier = Modifier.drawBehind {
                val h = 10.dp.toPx()
                drawRoundRect(b.green.copy(alpha = 0.14f), topLeft = Offset(0f, size.height - h - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            }.padding(horizontal = 6.dp),
        )
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
    }
}

/**
 * The home's section header (Canva/SaaS style): a soft icon tile, a bold title with a short line
 * under it, and an optional «الكل» pill at the end. One look for every home section.
 */
@Composable
fun HomeHead(title: String, subtitle: String? = null, icon: ImageVector? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(b.tint),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = b.primaryInk, modifier = Modifier.size(21.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = b.ink, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (action != null && onAction != null) Text(
            action, style = MaterialTheme.typography.labelLarge, color = b.primaryInk,
            modifier = Modifier.heightIn(min = 36.dp).clip(CircleShape).background(b.surface).border(1.dp, b.line, CircleShape)
                .clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Section root header (.page-head): eyebrow, bold title, lead, then actions. */
@Composable
fun HeroHeader(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit = {}) {
    val b = LocalBrand.current
    Column(
        modifier.fillMaxWidth().pageHeadSurface(b).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (eyebrow != null) Eyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = b.ink, textAlign = TextAlign.Center)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
        content()
    }
}

/** Small rounded label; gold = the site's «جديد». */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, gold: Boolean = false) {
    val b = LocalBrand.current
    Text(
        text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (gold) b.newInk else b.primaryInk,
        modifier = modifier.clip(CircleShape).background(if (gold) b.newFill else b.tint).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier) = Badge(text, modifier, gold = true)

/** Large title of a tab root (iOS .large title). */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = LocalBrand.current.ink,
        textAlign = TextAlign.Center, modifier = modifier.padding(top = 6.dp, bottom = 4.dp))
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val b = LocalBrand.current
    // Centred title; an action, if any, keeps its place at the end.
    Box(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = if (trailing != null) 56.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center)
        }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

// ───────────────────────── Icon tiles and tool cards ─────────────────────────

sealed interface TileStyle {
    data class Gradient(val index: Int) : TileStyle
    data object Gold : TileStyle
    data object Emerald : TileStyle
    data object Soft : TileStyle
    data class Accent(val color: Color) : TileStyle
    data class Solid(val color: Color) : TileStyle
}

/** Rounded icon tile (radius = size × 0.31) with the coloured shadow. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, style: TileStyle = TileStyle.Gradient(0), size: Dp = 52.dp) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(size * 0.31f)
    val shadow = when (style) {
        is TileStyle.Gradient -> tileGradient(style.index)[0].copy(alpha = 0.35f)
        is TileStyle.Solid -> style.color.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val fg = when (style) {
        is TileStyle.Gradient, is TileStyle.Solid -> Color.White
        TileStyle.Emerald -> b.onDeepAccent
        TileStyle.Gold -> b.goldInk
        TileStyle.Soft -> b.primaryInk
        is TileStyle.Accent -> style.color
    }
    Box(
        modifier
            .size(size)
            .then(if (shadow != Color.Transparent) Modifier.softShadow(shape, 8.dp, shadow, DpOffset(0.dp, 6.dp)) else Modifier)
            .clip(shape)
            .then(
                when (style) {
                    is TileStyle.Gradient -> Modifier.background(Brush.linearGradient(tileGradient(style.index)))
                    TileStyle.Emerald -> Modifier.background(Brush.linearGradient(b.gradient))
                    TileStyle.Gold -> Modifier.background(b.goldSoft)
                    TileStyle.Soft -> Modifier.background(b.tint)
                    is TileStyle.Accent -> Modifier.background(style.color.copy(alpha = 0.11f)).border(1.dp, style.color.copy(alpha = 0.2f), shape)
                    is TileStyle.Solid -> Modifier.background(style.color)
                },
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.46f)) }
}

/** The site's tool card: centred tile, title (+ «جديد»), one line. */
@Composable
fun ToolTile(title: String, subtitle: String?, icon: ImageVector, modifier: Modifier = Modifier, accent: Color? = null, index: Int = 0, badge: String? = null, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(
        modifier.pressable(onClick).surface(b).padding(horizontal = 14.dp, vertical = 18.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
    ) {
        IconTile(icon, style = if (accent != null) TileStyle.Accent(accent) else TileStyle.Gradient(index))
        Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (badge != null) Badge(badge, gold = true)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** Row variant of the service card: tile, title and line, chevron. */
@Composable
fun ServiceRow(title: String, subtitle: String?, icon: ImageVector, modifier: Modifier = Modifier, style: TileStyle = TileStyle.Gradient(0), badge: String? = null, onClick: () -> Unit) {
    val b = LocalBrand.current
    Row(
        modifier.fillMaxWidth().pressable(onClick).surface(b).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, style = style, size = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = b.ink)
                if (badge != null) Badge(badge, gold = true)
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted)
        }
        Chevron()
    }
}

/** Launcher tile: icon over a short title (app-style grids, no card). */
@Composable
fun LauncherIcon(title: String, icon: ImageVector, style: TileStyle, modifier: Modifier = Modifier, isNew: Boolean = false, size: Dp = 54.dp, dimmed: Boolean = false, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(modifier.pressable(onClick).alpha(if (dimmed) 0.45f else 1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box {
            IconTile(icon, style = style, size = size)
            // Offline, a tool that needs internet carries a small «cloud off» mark.
            if (dimmed) Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(20.dp).clip(CircleShape).background(b.surface).padding(2.dp),
                contentAlignment = Alignment.Center,
            ) { Icon(androidx.compose.material.icons.Icons.Outlined.CloudOff, "يحتاج إنترنت", tint = b.muted, modifier = Modifier.size(14.dp)) }
            else if (isNew) Box(
                Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(11.dp).clip(CircleShape)
                    .background(b.surface).padding(2.dp).clip(CircleShape).background(b.accent),
            )
        }
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun Chevron(modifier: Modifier = Modifier, color: Color = LocalBrand.current.lineStrong) {
    // RTL: auto-mirrored arrow points left, as iOS «chevron.forward» does in Arabic.
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = color, modifier = modifier.size(20.dp))
}

// ───────────────────────── Buttons ─────────────────────────

enum class BtnKind { Primary, Secondary, Gold, Dark, Ghost }

@Composable
fun BrandButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, kind: BtnKind = BtnKind.Primary, enabled: Boolean = true, onClick: () -> Unit) {
    val b = LocalBrand.current
    val shadow = when (kind) {
        BtnKind.Primary -> Color(0xFF1F8A62).copy(alpha = 0.35f)
        BtnKind.Gold -> Color(0xFFB7791F).copy(alpha = 0.35f)
        BtnKind.Secondary, BtnKind.Ghost -> b.shadow.copy(alpha = 0.07f)
        BtnKind.Dark -> Color.Transparent
    }
    val fg = if (kind == BtnKind.Secondary || kind == BtnKind.Ghost) b.ink else Color.White
    Row(
        modifier
            .heightIn(min = 50.dp)
            .then(if (enabled) Modifier.pressable(onClick) else Modifier)
            .then(if (shadow != Color.Transparent && !b.dark) Modifier.softShadow(ButtonShape, 12.dp, shadow, DpOffset(0.dp, 8.dp)) else Modifier)
            .clip(ButtonShape)
            .then(
                when (kind) {
                    BtnKind.Primary -> Modifier.background(b.siteBrush)
                    BtnKind.Gold -> Modifier.background(Brush.linearGradient(b.goldGradient))
                    BtnKind.Dark -> Modifier.background(b.primary)
                    BtnKind.Secondary, BtnKind.Ghost -> Modifier.background(b.surface).border(1.dp, b.line, ButtonShape)
                },
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) fg else fg.copy(alpha = 0.5f), maxLines = 1)
    }
}

// ───────────────────────── Chips, segmented, grouped lists ─────────────────────────

/** Category chip (.chip / .chip.on): gradient when on. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(b.siteBrush) else Modifier.background(b.surface).border(1.dp, b.lineStrong, CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = if (selected) Color.White else b.ink, maxLines = 1)
    }
}

@Composable
fun <T> CategorySelector(items: List<Pair<T, String>>, selection: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, contentPadding: Dp = 16.dp) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { items.forEach { (id, title) -> Chip(title, id == selection, { onSelect(id) }) } }
}

/** The site's segmented choice: mint track, chosen option in the gradient. */
@Composable
fun <T> SiteSegmented(items: List<Pair<T, String>>, selection: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    val track = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().clip(track).background(b.surface2).border(1.dp, b.line, track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (id, title) ->
            val on = id == selection
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(11.dp))
                    .then(if (on) Modifier.background(b.siteBrush) else Modifier)
                    .clickable { onSelect(id) },
                contentAlignment = Alignment.Center,
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = if (on) Color.White else b.muted, maxLines = 1)
            }
        }
    }
}

/** iOS inset-grouped list section: optional header/footer, rounded white block, hairline dividers. */
@Composable
fun GroupedSection(modifier: Modifier = Modifier, header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Column(modifier.fillMaxWidth()) {
        if (header != null) Text(header, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(b.surface).border(1.dp, b.line, RoundedCornerShape(Radius.card)), content = content)
        if (footer != null) Text(footer, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp))
    }
}

@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tile: TileStyle = TileStyle.Gradient(0),
    value: String? = null,
    divider: Boolean = true,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val b = LocalBrand.current
    Column(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { IconTile(icon, style = tile, size = 32.dp); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = b.ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted)
            }
            if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = b.muted)
            trailing?.invoke(this)
            if (chevron) { Spacer(Modifier.width(4.dp)); Chevron() }
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = if (icon != null) 60.dp else 16.dp).height(0.5.dp).background(b.line))
    }
}

// ───────────────────────── Bars ─────────────────────────

/** iOS inline navigation bar: back chevron, centred title, actions; transparent over the backdrop. */
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    val b = LocalBrand.current
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 4.dp)) {
        if (onBack != null) Box(
            Modifier.align(Alignment.CenterStart).size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "رجوع", tint = b.primaryInk, modifier = Modifier.size(20.dp).offset(x = 3.dp)) }
        Column(Modifier.align(Alignment.Center).widthIn(max = 220.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = b.muted, maxLines = 1)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

/** Horizontal logo for the home bar (light/dark artwork from the iOS asset catalog). */
@Composable
fun LogoHorizontal(modifier: Modifier = Modifier) {
    val b = LocalBrand.current
    Image(painterResource(if (b.dark) R.drawable.logo_horizontal_light else R.drawable.logo_horizontal), "آيات تتلى", modifier)
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val b = LocalBrand.current
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = b.muted, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = b.ink, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalBrand.current.green) }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

