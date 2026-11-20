package com.ayattla.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayattla.app.R
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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

private val Deep = Color(0xFF102D27)
private val Mid = Color(0xFF1F5A3C)
private val Green = Color(0xFF2E9145)
private val Gold = Color(0xFFE3CB8E)
private val GoldDeep = Color(0xFFC3A66B)

/**
 * The opening: picks up exactly where the system splash ends (same deep green), draws the gold
 * eight-point star stroke by stroke, rings out, then the full logo rises in with the tagline and the
 * whole thing fades into the app. About 2.2 s; a tap skips; with animations off it is skipped.
 */
@Composable
fun IntroOverlay(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val draw = remember { Animatable(0f) }      // star outline 0→1
    val ring = remember { Animatable(0f) }      // outer rings
    val logo = remember { Animatable(0f) }      // logo rise
    val tag = remember { Animatable(0f) }       // tagline
    val out = remember { Animatable(1f) }       // whole overlay alpha
    val spin = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val scale = runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)
        if (scale == 0f) { onDone(); return@LaunchedEffect }
        kotlinx.coroutines.coroutineScope {
            val s = async { spin.animateTo(1f, tween(2600, easing = LinearEasing)) }
            listOf(
                async { draw.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) },
                async { delay(250); ring.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) },
                async { delay(520); logo.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) },
                async { delay(1000); tag.animateTo(1f, tween(500)) },
            ).awaitAll()
            delay(650)
            out.animateTo(0f, tween(380))
            s.cancel()
        }
        onDone()
    }
    Box(
        Modifier.fillMaxSize().alpha(out.value)
            .background(Brush.radialGradient(listOf(Green, Mid, Deep), radius = 1500f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }
            .semantics { contentDescription = "آيات تتلى" },
        contentAlignment = Alignment.Center,
    ) {
        // Geometric field: the star, two rings with small diamonds, rotating very slowly.
        Canvas(Modifier.size(340.dp)) {
            val c = center
            val r = min(size.width, size.height) / 2f
            rotate(spin.value * 22f, c) {
                // Rings
                val rr = ring.value
                drawCircle(Gold.copy(alpha = 0.22f * rr), radius = r * (0.70f + 0.25f * rr), center = c, style = Stroke(1.2.dp.toPx()))
                drawCircle(Gold.copy(alpha = 0.12f * rr), radius = r * (0.78f + 0.20f * rr), center = c, style = Stroke(0.8.dp.toPx()))
                for (i in 0 until 16) {
                    val a = Math.toRadians(i * 22.5).toFloat()
                    val p = Offset(c.x + cos(a) * r * (0.70f + 0.25f * rr), c.y + sin(a) * r * (0.70f + 0.25f * rr))
                    drawCircle(Gold.copy(alpha = 0.55f * rr), radius = if (i % 2 == 0) 2.6.dp.toPx() else 1.5.dp.toPx(), center = p)
                }
                // Eight-point star, drawn progressively.
                val star = Path()
                val outer = r * 0.62f
                val inner = outer * (cos(Math.PI / 4) / cos(Math.PI / 8)).toFloat()
                for (i in 0 until 16) {
                    val a = (Math.PI / 8 * i - Math.PI / 2).toFloat()
                    val rad = if (i % 2 == 0) outer else inner
                    val x = c.x + cos(a) * rad; val y = c.y + sin(a) * rad
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                val pm = PathMeasure().apply { setPath(star, false) }
                val part = Path()
                pm.getSegment(0f, pm.length * draw.value, part, true)
                drawPath(star, Deep.copy(alpha = 0.45f * draw.value))
                drawPath(part, Brush.linearGradient(listOf(Gold, GoldDeep)), style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painterResource(R.drawable.logo_full_light), null,
                Modifier.size(170.dp).graphicsLayer {
                    alpha = logo.value
                    val s = 0.86f + 0.14f * logo.value
                    scaleX = s; scaleY = s
                    translationY = (1f - logo.value) * 24.dp.toPx()
                },
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 56.dp).alpha(tag.value)
                .graphicsLayer { translationY = (1f - tag.value) * 12.dp.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width((60 * tag.value).dp).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))))
            Spacer(Modifier.height(12.dp))
            Text("القرآن والأذكار وأدوات المسلم اليومية", color = Color.White.copy(alpha = 0.92f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("مجاني · بلا حساب · يعمل دون إنترنت", color = Gold.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}
