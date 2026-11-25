package com.ayattla.app.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.Pill
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import org.json.JSONObject

// ───────────── Short religious bio phrases (/tools/bio) ─────────────
// Data: assets/bio_data.json, same file as the site. Ayat are the Uthmani text matched word for word;
// adhkar and duas carry their hadith source.

private data class BioItem(val t: String, val ayah: Boolean, val c: String, val src: String)

private val BioDecors = listOf("" to "بدون زخرفة", "🤍|🤍" to "🤍", "✨|✨" to "✨", "『|』" to "『 』", "۞|۞" to "۞", "🌿|🌿" to "🌿")

@Composable
fun BioScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val items = remember {
        runCatching {
            val arr = JSONObject(ctx.assets.open("bio_data.json").bufferedReader().use { it.readText() }).getJSONArray("items")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                BioItem(o.getString("t"), o.getString("k") == "ayah", o.getString("c"), o.getString("src"))
            }
        }.getOrDefault(emptyList())
    }
    val cats = remember(items) { listOf("الكل", "من القرآن", "أذكار وأدعية") + items.map { it.c }.distinct() }
    var filter by remember { mutableStateOf("الكل") }
    var decor by remember { mutableStateOf("") }
    var withSrc by remember { mutableStateOf(false) }
    fun out(x: BioItem): String {
        var t = if (x.ayah) "﴿${x.t}﴾" else x.t
        if (decor.isNotEmpty() && !x.ayah) decor.split("|").let { t = it[0] + " " + t + " " + it[1] }
        if (withSrc) t += " (${x.src})"
        return t
    }
    ToolPage(nav, "عبارات للبايو", "bio") {
        Text(
            "اختر عبارة واضغط نسخ وألصقها في بايو انستقرام أو تيك توك أو حالة واتساب. كل آية بنصها ومكانها في المصحف، وكل ذكر مع رقم الحديث.",
            style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c -> Chip(c, filter == c, { filter = c }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioDecors.forEach { (v, label) -> Chip(label, decor == v, { decor = v }) }
            Chip("انسخ المصدر معها", withSrc, { withSrc = !withSrc })
        }
        items.filter {
            filter == "الكل" || (filter == "من القرآن" && it.ayah) || (filter == "أذكار وأدعية" && !it.ayah) || filter == it.c
        }.forEach { x ->
            val text = out(x)
            val n = text.codePointCount(0, text.length)
            AppCard {
                Text(
                    if (x.ayah) "﴿${x.t}﴾" else x.t,
                    style = if (x.ayah) TextStyle(fontFamily = Fonts.quran, fontSize = 24.sp, lineHeight = 46.sp)
                    else TextStyle(fontFamily = Fonts.naskh, fontSize = 20.sp, lineHeight = 36.sp),
                    color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Pill(x.src)
                    Text(
                        "$n حرف" + when { n <= 80 -> " · تيك توك وانستقرام"; n <= 150 -> " · انستقرام"; else -> " · طويل للبايو" },
                        style = MaterialTheme.typography.labelMedium, color = if (n > 150) Color(0xFFB3261E) else b.muted,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("نسخ", Modifier.weight(1f)) { copyText(ctx, text) }
                    BrandButton("مشاركة", Modifier.weight(1f), kind = BtnKind.Secondary) { shareText(ctx, text) }
                }
            }
        }
        Text(
            "الزخرفة تضاف للأذكار فقط، وتبقى الآيات بين القوسين ﴿ ﴾ كما هي احتراما لنص القرآن.",
            style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}
