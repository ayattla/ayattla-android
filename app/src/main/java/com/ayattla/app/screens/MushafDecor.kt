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
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.graphics.compositeOver

/**
 * The reading backgrounds of the Quran pages. Calm, flat tones only (no patterns), like the reading
 * themes of the leading mushaf apps; night follows the app's dark mode.
 */
enum class MushafTheme(val key: String, val label: String, val page: Color, val backdrop: Color, val gold: Color, val ink: Color) {
    Paper("paper", "ورقي", Color(0xFFFCF8EE), Color(0xFFF4EFE2), Color(0xFFB08A3E), Color(0xFF1F2620)),
    White("white", "أبيض", Color(0xFFFFFFFF), Color(0xFFF1F3F2), Color(0xFF9C8350), Color(0xFF1A1F1C)),
    Green("green", "أخضر هادئ", Color(0xFFF4F8F2), Color(0xFFE5EEE3), Color(0xFF427E45), Color(0xFF102D27)),
    Night("night", "ليلي", Color(0xFF14211C), Color(0xFF0D1713), Color(0xFFC9A75F), Color(0xFFEDE6D3));
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } ?: Paper }
}

object MushafColors {
    @Composable fun theme(): MushafTheme = if (LocalBrand.current.dark) MushafTheme.Night else MushafTheme.of(com.ayattla.app.LocalPrefs.current.mushafTheme)
    @Composable fun paper() = theme().page
    @Composable fun backdrop() = theme().backdrop
    @Composable fun gold() = theme().gold
    @Composable fun ink() = theme().ink
}

/** Picks the page background; shown in the reader's top bar. */
@Composable
fun MushafThemeButton() {
    val prefs = com.ayattla.app.LocalPrefs.current
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        androidx.compose.material3.IconButton(onClick = { open = true }) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Palette, "خلفية المصحف")
        }
        androidx.compose.material3.DropdownMenu(open, onDismissRequest = { open = false }) {
            MushafTheme.entries.forEach { t ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(t.label) },
                    leadingIcon = {
                        Box(Modifier.size(22.dp).background(t.page, androidx.compose.foundation.shape.CircleShape)
                            .border(if (prefs.mushafTheme == t.key) 2.dp else 1.dp, t.gold, androidx.compose.foundation.shape.CircleShape))
                    },
                    onClick = { prefs.setMushafTheme(t.key); open = false },
                )
            }
        }
    }
}

/** Soft gold corner brackets: the modern replacement for ornamental corner stars. */
private fun DrawScope.corner(x: Float, y: Float, dx: Float, dy: Float, len: Float, color: Color) {
    val p = Path().apply { moveTo(x, y + dy * len); lineTo(x, y + dy * len * 0.25f); quadraticTo(x, y, x + dx * len * 0.25f, y); lineTo(x + dx * len, y) }
    drawPath(p, color, style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

/**
 * A mushaf page: paper inside a double gold rule with soft corner brackets, a running head (surah, juz) and the
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
            val c = 2.dp.toPx(); val len = 22.dp.toPx(); val w = size.width; val h = size.height
            corner(c, c, 1f, 1f, len, gold); corner(w - c, c, -1f, 1f, len, gold)
            corner(c, h - c, 1f, -1f, len, gold); corner(w - c, h - c, -1f, -1f, len, gold)
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
                Box(
                    Modifier.heightIn(min = 26.dp).background(paper, androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .border(1.dp, gold, androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$page", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (b.dark) gold else Color(0xFF6B5320))
                }
            }
        }
    }
}

/** The surah title in a soft framed capsule, its place of revelation and ayah count. */
@Composable
fun SurahBanner(surah: Surah, modifier: Modifier = Modifier) {
    val gold = MushafColors.gold()
    val paper = MushafColors.paper()
    val green = if (MushafColors.theme() == MushafTheme.Night) Color(0xFF1C3A2E) else MushafColors.gold().copy(alpha = 0.10f).compositeOver(MushafColors.paper())
    Column(modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                // A soft capsule with a fine double gold rule and a dot at each end.
                val h = size.height; val w = size.width; val r = h / 2
                drawRoundRect(green, cornerRadius = CornerRadius(r))
                drawRoundRect(gold, cornerRadius = CornerRadius(r), style = Stroke(1.4.dp.toPx()))
                val i = 5.dp.toPx()
                drawRoundRect(gold.copy(alpha = 0.45f), Offset(i, i), Size(w - 2 * i, h - 2 * i), CornerRadius(r - i), style = Stroke(0.8.dp.toPx()))
                drawCircle(gold, 3.dp.toPx(), Offset(r * 0.9f, h / 2)); drawCircle(gold, 3.dp.toPx(), Offset(w - r * 0.9f, h / 2))
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
