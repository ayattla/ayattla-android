package com.ayattla.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayattla.app.data.Surah
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Mushaf paper and gold for light and dark reading. */
object MushafColors {
    @Composable fun paper() = if (LocalBrand.current.dark) Color(0xFF14211C) else Color(0xFFFCF8EE)
    @Composable fun gold() = if (LocalBrand.current.dark) Color(0xFFC9A75F) else Color(0xFFB08A3E)
    @Composable fun ink() = if (LocalBrand.current.dark) Color(0xFFEDE6D3) else Color(0xFF1F2620)
}

private fun DrawScope.star8(c: Offset, r: Float, color: Color, fill: Boolean) {
    val inner = r * (cos(PI / 4) / cos(PI / 8)).toFloat()
    val p = Path()
    for (i in 0 until 16) {
        val a = (PI / 8 * i - PI / 2).toFloat()
        val rr = if (i % 2 == 0) r else inner
        val x = c.x + cos(a) * rr; val y = c.y + sin(a) * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    if (fill) drawPath(p, color) else drawPath(p, color, style = Stroke(1.2.dp.toPx()))
}

/**
 * A mushaf page: paper inside a double gold rule with star corners, a running head (surah, juz) and the
 * page number in a small medallion at the foot. The ayat go in [content].
 */
@Composable
fun MushafFrame(surahName: String, juz: Int?, page: Int?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val gold = MushafColors.gold()
    val paper = MushafColors.paper()
    val b = LocalBrand.current
    Box(modifier.fillMaxWidth().background(paper, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val o = 6.dp.toPx(); val i = 11.dp.toPx()
            drawRoundRect(gold, Offset(o, o), Size(size.width - 2 * o, size.height - 2 * o), CornerRadius(12.dp.toPx()), style = Stroke(1.6.dp.toPx()))
            drawRoundRect(gold.copy(alpha = 0.55f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius(8.dp.toPx()), style = Stroke(0.8.dp.toPx()))
            val r = 7.dp.toPx()
            listOf(Offset(o + 2, o + 2), Offset(size.width - o - 2, o + 2), Offset(o + 2, size.height - o - 2), Offset(size.width - o - 2, size.height - o - 2)).forEach {
                star8(it, r, paper, true); star8(it, r, gold, false); star8(it, r * 0.45f, gold, true)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)) {
            // Running head.
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("سورة $surahName", style = MaterialTheme.typography.labelMedium, color = gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                if (juz != null) Text("الجزء $juz", style = MaterialTheme.typography.labelMedium, color = gold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Box(Modifier.fillMaxWidth().height(0.8.dp).background(gold.copy(alpha = 0.45f)))
            content()
            // Page number medallion.
            if (page != null) Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) {
                        val c = center; val rr = size.minDimension / 2 - 1.dp.toPx()
                        star8(c, rr, paper, true); star8(c, rr, gold, false)
                    }
                    Text("$page", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (b.dark) gold else Color(0xFF6B5320))
                }
            }
        }
    }
}

/** The surah title as a framed cartouche with star ends, its place of revelation and ayah count. */
@Composable
fun SurahBanner(surah: Surah, modifier: Modifier = Modifier) {
    val gold = MushafColors.gold()
    val paper = MushafColors.paper()
    val green = if (LocalBrand.current.dark) Color(0xFF1C3A2E) else Color(0xFFE8F1EA)
    Column(modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val h = size.height; val w = size.width; val cut = h * 0.5f
                // Cartouche: a band with pointed ends.
                val band = Path().apply {
                    moveTo(cut, 0f); lineTo(w - cut, 0f); lineTo(w, h / 2); lineTo(w - cut, h); lineTo(cut, h); lineTo(0f, h / 2); close()
                }
                drawPath(band, green)
                drawPath(band, gold, style = Stroke(1.6.dp.toPx()))
                val inset = 5.dp.toPx()
                val inner = Path().apply {
                    moveTo(cut + inset, inset); lineTo(w - cut - inset, inset); lineTo(w - inset * 1.6f, h / 2); lineTo(w - cut - inset, h - inset)
                    lineTo(cut + inset, h - inset); lineTo(inset * 1.6f, h / 2); close()
                }
                drawPath(inner, gold.copy(alpha = 0.5f), style = Stroke(0.8.dp.toPx()))
                star8(Offset(cut * 0.95f, h / 2), h * 0.17f, gold, true)
                star8(Offset(w - cut * 0.95f, h / 2), h * 0.17f, gold, true)
                star8(Offset(cut * 0.95f, h / 2), h * 0.27f, gold, false)
                star8(Offset(w - cut * 0.95f, h / 2), h * 0.27f, gold, false)
            }
            Text(
                "سورة ${surah.shortName}", fontFamily = Fonts.naskh, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MushafColors.ink(),
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }.padding(horizontal = 70.dp),
            )
        }
        val place = when (surah.revelation?.lowercase()) { "meccan", "makkah", "mecca", "مكية" -> "مكية"; "medinan", "madinah", "medina", "مدنية" -> "مدنية"; else -> null }
        Text(listOfNotNull(place, "آياتها ${surah.ayahCount}").joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = gold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}
