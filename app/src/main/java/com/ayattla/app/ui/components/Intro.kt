package com.ayattla.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayattla.app.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** «الافتتاحية»: on/off in Settings; shown once per launch. */
object IntroPrefs {
    private fun sp(c: Context) = c.getSharedPreferences("intro", Context.MODE_PRIVATE)
    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()
    /** Already shown in this process (rotation or returning to the app does not replay it). */
    var shownThisLaunch = false
}

private val Edge = Color(0xFF0A2E22)
private val Mid = Color(0xFF17623F)
private val Centre = Color(0xFF2A8A57)
private val Gold = Color(0xFFE8C878)
private val GoldLight = Color(0xFFFFF1C2)
private val GoldDeep = Color(0xFFB8893F)
private val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/** An 8-point star (two squares) as a closed path. */
private fun starPath(c: Offset, outer: Float): Path {
    val inner = outer * (cos(PI / 4) / cos(PI / 8)).toFloat()
    return Path().apply {
        for (i in 0 until 16) {
            val a = (PI / 8 * i - PI / 2).toFloat()
            val r = if (i % 2 == 0) outer else inner
            val x = c.x + cos(a) * r; val y = c.y + sin(a) * r
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** Draws [path] from 0 to [t] of its length. */
private fun DrawScope.trace(path: Path, t: Float, brush: Brush, width: Float, alpha: Float = 1f) {
    if (t <= 0f) return
    val pm = PathMeasure().apply { setPath(path, false) }
    val part = Path()
    pm.getSegment(0f, pm.length * t.coerceIn(0f, 1f), part, true)
    drawPath(part, brush, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * The opening (about 2.6 s, once per launch):
 * the system splash hands over on the same deep green; a gold geometric rosette is traced line by line
 * (outer ring, the eight-point star, the inner star, sixteen rays) while a warm light blooms at its centre;
 * the gold book rises into that light and a sheen sweeps across it; the calligraphic name and the tagline
 * follow; then the whole scene eases forward and dissolves into the app.
 * A tap skips it; with system animations off, or turned off in Settings, it does not play.
 */
@Composable
fun IntroOverlay(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val ring = remember { Animatable(0f) }
    val star = remember { Animatable(0f) }
    val rays = remember { Animatable(0f) }
    val bloom = remember { Animatable(0f) }
    val book = remember { Animatable(0f) }
    val sheen = remember { Animatable(-0.4f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val drift = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val scale = runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)
        if (scale == 0f) { onDone(); return@LaunchedEffect }
        coroutineScope {
            val d = async { drift.animateTo(1f, tween(3200, easing = LinearEasing)) }
            val steps = listOf(
                async { ring.animateTo(1f, tween(700, easing = EaseInOut)) },
                async { delay(120); star.animateTo(1f, tween(900, easing = EaseInOut)) },
                async { delay(420); rays.animateTo(1f, tween(700, easing = EaseOut)) },
                async { delay(300); bloom.animateTo(1f, tween(1100, easing = EaseOut)) },
                async { delay(640); book.animateTo(1f, tween(800, easing = EaseOut)) },
                async { delay(1150); sheen.animateTo(1.4f, tween(750, easing = EaseInOut)) },
                async { delay(1050); word.animateTo(1f, tween(650, easing = EaseOut)) },
                async { delay(1450); tag.animateTo(1f, tween(500, easing = EaseOut)) },
            )
            steps.forEach { it.await() }
            delay(420)
            exit.animateTo(1f, tween(420, easing = EaseInOut))
            d.cancel()
        }
        onDone()
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                val s = 1f + 0.06f * exit.value
                scaleX = s; scaleY = s
            }
            .background(Brush.radialGradient(listOf(Centre, Mid, Edge), radius = 1700f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }
            .semantics { contentDescription = "آيات تتلى" },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val c = Offset(w / 2f, h * 0.42f)
            val r = min(w, h) * 0.40f
            // Geometry, light and dust.
            Canvas(Modifier.fillMaxSize()) {
                // Light bloom behind the book.
                val b = bloom.value
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFFFFE7A6).copy(alpha = 0.50f * b), Color(0xFFF2C96B).copy(alpha = 0.14f * b), Color.Transparent), c, r * 1.25f),
                    r * 1.25f, c,
                )
                val gold = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep, Gold), c - Offset(r, r), c + Offset(r, r))
                rotate(drift.value * 10f, c) {
                    // Outer double ring.
                    val ringPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r * 1.02f)) }
                    trace(ringPath, ring.value, gold, 2.dp.toPx(), 0.55f)
                    val ring2 = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r * 1.10f)) }
                    trace(ring2, ring.value, gold, 1.dp.toPx(), 0.28f)
                    // Sixteen beads on the ring.
                    for (i in 0 until 16) {
                        val a = (PI / 8 * i).toFloat()
                        val p = Offset(c.x + cos(a) * r * 1.02f, c.y + sin(a) * r * 1.02f)
                        drawCircle(Gold.copy(alpha = 0.8f * ring.value), if (i % 2 == 0) 3.dp.toPx() else 1.8.dp.toPx(), p)
                    }
                    // Rays.
                    for (i in 0 until 16) {
                        val a = (PI / 8 * i).toFloat()
                        val from = Offset(c.x + cos(a) * r * 0.30f, c.y + sin(a) * r * 0.30f)
                        val to = Offset(c.x + cos(a) * r * (0.30f + 0.66f * rays.value), c.y + sin(a) * r * (0.30f + 0.66f * rays.value))
                        drawLine(Gold.copy(alpha = 0.14f * rays.value), from, to, 1.dp.toPx())
                    }
                    // The eight-point star: soft fill, gold outline, inner star.
                    val outer = starPath(c, r * 0.98f)
                    drawPath(outer, Brush.verticalGradient(listOf(Color(0xFF2C9862).copy(alpha = 0.45f * star.value), Color(0xFF0E4A33).copy(alpha = 0.45f * star.value)), c.y - r, c.y + r))
                    trace(outer, star.value, gold, 3.dp.toPx())
                    trace(starPath(c, r * 0.80f), (star.value * 1.25f - 0.25f), gold, 1.4.dp.toPx(), 0.5f)
                }
                // Gold dust rising slowly.
                for (i in 0 until 22) {
                    val seed = (i * 73 % 100) / 100f
                    val x = w * ((i * 37 % 100) / 100f)
                    val y = h * (1.05f - ((seed + drift.value * (0.25f + seed * 0.3f)) % 1.1f))
                    val a = (0.15f + 0.45f * ((i * 17 % 10) / 10f)) * bloom.value
                    drawCircle(GoldLight.copy(alpha = a), (1.2f + (i % 3)).dp.toPx(), Offset(x, y))
                }
            }
            // The gold book, rising into the light, with a sheen passing over it.
            val bookW = with(androidx.compose.ui.platform.LocalDensity.current) { (r * 1.42f).toDp() }
            val cy = with(androidx.compose.ui.platform.LocalDensity.current) { c.y.toDp() }
            Image(
                painterResource(R.drawable.intro_book), null,
                Modifier.width(bookW).align(Alignment.TopCenter)
                    .offset(y = cy - bookW * (503f / 1852f) / 2f + 10.dp)
                    .graphicsLayer {
                        alpha = book.value
                        translationY = (1f - book.value) * 26.dp.toPx()
                        val s = 0.92f + 0.08f * book.value
                        scaleX = s; scaleY = s
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val x = size.width * sheen.value
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                                start = Offset(x - size.width * 0.18f, 0f), end = Offset(x + size.width * 0.18f, size.height),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    },
            )
            // Name and tagline.
            Column(
                Modifier.align(Alignment.TopCenter).offset(y = cy + with(androidx.compose.ui.platform.LocalDensity.current) { (r * 1.18f).toDp() }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painterResource(R.drawable.intro_word), "آيات تتلى",
                    Modifier.width(bookW * 0.82f).graphicsLayer {
                        alpha = word.value
                        translationY = (1f - word.value) * 14.dp.toPx()
                    },
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)
                    .graphicsLayer { alpha = tag.value; translationY = (1f - tag.value) * 10.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width((72 * tag.value).dp).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))))
                Spacer(Modifier.height(12.dp))
                Text("القرآن والأذكار وأدوات المسلم اليومية", color = Color.White.copy(alpha = 0.94f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("مجاني · بلا حساب · يعمل دون إنترنت", color = Gold.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
