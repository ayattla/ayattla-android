package com.ayattla.app.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.calc.Hijri
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.data.SiteApi
import com.ayattla.app.studio.CardRatio
import com.ayattla.app.studio.CardText
import com.ayattla.app.studio.PendingText
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Badge
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chevron
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.GroupedRow
import com.ayattla.app.ui.components.GroupedSection
import com.ayattla.app.ui.components.HeroHeader
import com.ayattla.app.ui.components.LoadingState
import com.ayattla.app.ui.components.SiteSegmented
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ───────────── Shared pieces ─────────────

/** A tool page: inline bar, then a scrolling column with the tool's head band. */
@Composable
fun ToolPage(nav: NavHostController, title: String, id: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val tool = id?.let { runCatching { SiteTool.byId(it) }.getOrNull() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(title, onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (tool != null) HeroHeader(tool.title, subtitle = tool.subtitle)
            content()
        }
    }
}

/** Surah chooser row (opens a sheet with all 114). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahPicker(surah: Int, label: String = "السورة", onPick: (Int) -> Unit) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(b.surface)
            .border(1.dp, b.line, RoundedCornerShape(14.dp)).clickable { open = true }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = b.muted)
        Spacer(Modifier.weight(1f))
        Text("$surah. سورة ${content.surah(surah).shortName}", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
        Icon(Icons.Filled.ExpandMore, null, tint = b.muted)
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }, containerColor = b.surface) {
        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
            items(content.surahs, key = { it.id }) { s ->
                Text(
                    "${s.id}. سورة ${s.shortName}", style = MaterialTheme.typography.bodyLarge,
                    color = if (s.id == surah) b.primaryInk else b.ink,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(s.id); open = false }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun NumberField(value: String, placeholder: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val b = LocalBrand.current
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).border(1.dp, b.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = b.muted)
        BasicTextField(
            value, { v -> onChange(v.filter { it.isDigit() || it in '٠'..'٩' }.map { if (it in '٠'..'٩') '0' + (it - '٠') else it }.joinToString("").take(5)) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun sp(c: Context) = c.getSharedPreferences("tools", Context.MODE_PRIVATE)

// ───────────── Tajweed (/tools/tajweed) ─────────────

object Tajweed {
    val rules = listOf(
        "ham_wasl" to "همزة الوصل", "laam_shamsiyah" to "اللام الشمسية", "slnt" to "حرف لا ينطق",
        "madda_normal" to "مد طبيعي (حركتان)", "madda_permissible" to "مد جائز (٢ أو ٤ أو ٦)",
        "madda_obligatory" to "مد واجب (٤ أو ٥)", "madda_necessary" to "مد لازم (٦ حركات)", "qalaqah" to "قلقلة",
        "ghunnah" to "غنة", "ikhafa" to "إخفاء", "ikhafa_shafawi" to "إخفاء شفوي", "iqlab" to "إقلاب",
        "idgham_ghunnah" to "إدغام بغنة", "idgham_wo_ghunnah" to "إدغام بلا غنة", "idgham_shafawi" to "إدغام شفوي",
        "idgham_mutajanisayn" to "إدغام متجانسين", "idgham_mutaqaribayn" to "إدغام متقاربين",
    )
    private val palette = mapOf(
        "ham_wasl" to (0x9A9A9A to 0x8E948F), "laam_shamsiyah" to (0x9A9A9A to 0x8E948F), "slnt" to (0x9A9A9A to 0x8E948F),
        "madda_normal" to (0x3B6FE0 to 0x7EA2FF), "madda_permissible" to (0x2F45D8 to 0x8C97FF),
        "madda_obligatory" to (0x1F3AAE to 0xA6B4FF), "madda_necessary" to (0x0A1E8C to 0xC2C9FF),
        "qalaqah" to (0xC8102E to 0xFF6B7A), "ghunnah" to (0xE8701A to 0xFFA35C), "ikhafa" to (0x8A1FA0 to 0xD58BE6),
        "ikhafa_shafawi" to (0xC21AA8 to 0xF07BDC), "iqlab" to (0x1E9FD6 to 0x6FD0FF),
        "idgham_ghunnah" to (0x137A60 to 0x5CD4B2), "idgham_wo_ghunnah" to (0x1C8A1C to 0x7BDB7B),
        "idgham_shafawi" to (0x137A60 to 0x5CD4B2), "idgham_mutajanisayn" to (0x4E9F00 to 0xA6E05C),
        "idgham_mutaqaribayn" to (0x8C8C8C to 0xB4B4B4),
    )
    fun color(rule: String, dark: Boolean): Color? = palette[rule]?.let { Color(0xFF000000 or (if (dark) it.second else it.first).toLong()) }

    private val tag = Regex("<span class=\"tj-([a-z_]+)\">(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
    private fun unescape(s: String) = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")

    /** The site's tajweed HTML as coloured text; the letters inside the tags are kept exactly. */
    fun annotate(html: String, dark: Boolean): AnnotatedString = buildAnnotatedString {
        var pos = 0
        for (m in tag.findAll(html)) {
            if (m.range.first > pos) append(unescape(html.substring(pos, m.range.first)))
            val rule = m.groupValues[1]
            val inner = unescape(m.groupValues[2])
            if (rule == "end") append("۝" + inner)
            else {
                val c = color(rule, dark)
                if (c != null) withStyle(SpanStyle(color = c)) { append(inner) } else append(inner)
            }
            pos = m.range.last + 1
        }
        if (pos < html.length) append(unescape(html.substring(pos)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TajweedLegend() {
    val b = LocalBrand.current
    var open by remember { mutableStateOf(false) }
    AppCard(onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("مفتاح الألوان", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, null, tint = b.muted)
        }
        if (open) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tajweed.rules.forEach { (r, name) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ب", fontFamily = Fonts.quran, fontSize = 22.sp, color = Tajweed.color(r, b.dark) ?: b.ink)
                    Text(name, style = MaterialTheme.typography.labelMedium, color = b.ink)
                }
            }
        }
    }
}

@Composable
fun TajweedScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var surah by rememberSaveable { mutableIntStateOf(1) }
    var size by rememberSaveable { mutableStateOf(26) }
    var verses by remember { mutableStateOf<List<SiteApi.TajweedVerse>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(surah) {
        verses = emptyList(); failed = false
        runCatching { SiteApi.tajweed(ctx, surah) }.onSuccess { verses = it }.onFailure { failed = true }
    }
    ToolPage(nav, "المصحف الملون بالتجويد", "tajweed") {
        SurahPicker(surah) { surah = it }
        SiteSegmented(listOf(22 to "صغير", 26 to "متوسط", 32 to "كبير"), size, { size = it })
        TajweedLegend()
        AppCard {
            when {
                failed -> Text("تعذر تحميل السورة من Quran.com، حاول لاحقا.", color = b.muted)
                verses.isEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = b.green) }
                else -> {
                    val all = buildAnnotatedString { verses.forEach { append(Tajweed.annotate(it.html, b.dark)); append(" ") } }
                    Text(all, fontFamily = Fonts.quran, fontSize = size.sp, lineHeight = (size * 1.9f).sp, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Text("نص ملون بأحكام التجويد من Quran.com (Quran Foundation).", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Word by word (/tools/word-by-word) ─────────────

private val WordLanguages = listOf("en" to "English", "ur" to "اردو", "id" to "Indonesia", "tr" to "Türkçe", "bn" to "বাংলা", "fr" to "Français")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var surah by rememberSaveable { mutableIntStateOf(1) }
    var lang by rememberSaveable { mutableStateOf(sp(ctx).getString("wbw.lang", "en") ?: "en") }
    var verses by remember { mutableStateOf<List<SiteApi.WordsVerse>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(surah, lang) {
        verses = emptyList(); failed = false
        sp(ctx).edit().putString("wbw.lang", lang).apply()
        runCatching { SiteApi.words(ctx, surah, lang) }.onSuccess { verses = it }.onFailure { failed = true }
    }
    ToolPage(nav, "معاني الكلمات", "word-by-word") {
        SurahPicker(surah) { surah = it }
        Text("لغة المعنى", style = MaterialTheme.typography.titleMedium, color = b.ink)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WordLanguages.forEach { (id, t) -> Chip(t, id == lang, { lang = id }) }
        }
        when {
            failed -> Text("تعذر التحميل من Quran.com الآن.", color = b.muted)
            verses.isEmpty() -> LoadingState(Modifier.height(160.dp))
        }
        verses.forEach { v ->
            AppCard {
                Text("الآية ${v.n}", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    v.words.forEach { w ->
                        Column(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(b.tint).padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(w.t, fontFamily = Fonts.quran, fontSize = 22.sp, color = b.ink)
                            Text(w.m, style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
                        }
                    }
                }
                v.tafsir?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        Text("معاني الكلمات من Quran.com (Quran Foundation)، والتفسير الميسر من مجمع الملك فهد.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}


// ───────────── Hifz test (/tools/hifz-test) ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HifzTestScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var surah by rememberSaveable { mutableIntStateOf(1) }
    var from by rememberSaveable { mutableIntStateOf(1) }
    var to by rememberSaveable { mutableIntStateOf(7) }
    var level by rememberSaveable { mutableStateOf(0.5) }
    var started by remember { mutableStateOf(false) }
    val hidden = remember { mutableStateListOf<String>() }
    val count = content.surah(surah).ayahCount
    val ayat = remember(surah, from, to) { content.ayat(surah, from, to) }
    fun start() {
        val cands = ayat.flatMap { a -> a.text.split(" ").mapIndexedNotNull { i, w -> if (w.codePointCount(0, w.length) > 1) "${a.key}#$i" else null } }
        val n = if (level >= 1) cands.size else Math.round(cands.size * level).toInt()
        hidden.clear(); hidden.addAll(cands.shuffled().take(n)); started = true
    }
    ToolPage(nav, "اختبار الحفظ", "hifz-test") {
        SurahPicker(surah) { surah = it; from = 1; to = minOf(content.surah(it).ayahCount, 7); started = false }
        AppCard {
            StepRow("من آية", from, 1, count) { from = it; if (to < it) to = it; started = false }
            StepRow("إلى آية", to, from, count) { to = it; started = false }
            Text("نسبة الإخفاء", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            SiteSegmented(listOf(0.25 to "25٪", 0.5 to "50٪", 1.0 to "الكل"), level, { level = it })
            Spacer(Modifier.height(12.dp))
            BrandButton(if (started) "اختبار جديد" else "ابدأ الاختبار", Modifier.fillMaxWidth()) { start() }
        }
        if (started) {
            AppCard {
                ayat.forEach { a ->
                    FlowRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        a.text.split(" ").forEachIndexed { i, w ->
                            val k = "${a.key}#$i"
                            if (k in hidden) Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(b.tint).border(1.dp, b.line, RoundedCornerShape(6.dp)).clickable { hidden.remove(k) }.padding(horizontal = 4.dp),
                            ) { Text(w, fontFamily = Fonts.quran, fontSize = 22.sp, color = Color.Transparent) }
                            else Text(w, fontFamily = Fonts.quran, fontSize = 22.sp, color = b.ink)
                        }
                        Text(Num.ayahMark(a.ayah), fontFamily = Fonts.quran, fontSize = 22.sp, color = b.ink)
                    }
                }
                BrandButton("اكشف الكل", Modifier.fillMaxWidth().padding(top = 8.dp), kind = BtnKind.Secondary) { hidden.clear() }
            }
            Text("اضغط على الكلمة المخفية لكشفها. المخفي الآن: ${hidden.size}", style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}

@Composable
private fun StepRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
        StepBtn("−", value > min) { onChange(value - 1) }
        Text("$value", style = MaterialTheme.typography.titleMedium, color = b.primaryInk, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        StepBtn("+", value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepBtn(t: String, enabled: Boolean, onClick: () -> Unit) {
    val b = LocalBrand.current
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(b.tint).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(t, style = MaterialTheme.typography.titleLarge, color = if (enabled) b.primaryInk else b.lineStrong) }
}

// ───────────── Ruqya (/tools/ruqya) ─────────────

@Composable
fun RuqyaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ctx = LocalContext.current
    val reciter = rememberReciter()
    val data = content.library.ruqya
    ToolPage(nav, "الرقية الشرعية", "ruqya") {
        if (data == null) { EmptyState("الرقية غير متاحة", "لم تجهز حزمة المحتوى في هذه النسخة"); return@ToolPage }
        if (data.first.isNotBlank()) Text(data.first, style = MaterialTheme.typography.titleMedium, color = b.ink)
        BrandButton("استمع للرقية كاملة", Modifier.fillMaxWidth(), icon = Icons.Filled.PlayArrow) {
            data.second.firstOrNull()?.let { Recitation.play(ctx, content, reciter, it.s, it.a, it.b) }
        }
        data.second.forEach { q ->
            val ayat = remember(q) { content.ayat(q.s, q.a, q.b) }
            val name = content.surah(q.s).shortName
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (q.a == q.b) "$name · ${q.a}" else "$name · ${q.a}–${q.b}", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.weight(1f))
                    Box(Modifier.size(40.dp).clip(CircleShape).background(b.tint).clickable { Recitation.play(ctx, content, reciter, q.s, q.a, q.b) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, "استمع", tint = b.primaryInk)
                    }
                }
                Text(
                    ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) },
                    fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 40.sp, color = b.ink, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Text("النص من المصحف الموثق، والتلاوة من EveryAyah.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Asma ul-Husna (/tools/asma-ul-husna) ─────────────

@Composable
fun AsmaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val names = LocalContent.current.library.asma
    var q by rememberSaveable { mutableStateOf("") }
    val shown = names.filter { q.isBlank() || q.trim() in it.second }
    ToolPage(nav, "الأسماء الحسنى", "asma-ul-husna") {
        if (names.isEmpty()) { EmptyState("غير متاح", "لم تجهز حزمة المحتوى في هذه النسخة"); return@ToolPage }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(b.surface).border(1.dp, b.line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (q.isEmpty()) Text("ابحث في الأسماء", color = b.muted, style = MaterialTheme.typography.bodyMedium)
            BasicTextField(q, { q = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
        }
        Text("اضغط أي اسم لتصنع منه بطاقة", style = MaterialTheme.typography.bodySmall, color = b.muted)
        AppCard(level = com.ayattla.app.ui.components.Surface.Gold) {
            Text("عن هذه القائمة", style = MaterialTheme.typography.titleMedium, color = b.goldInk)
            Text(
                "الثابت في الصحيحين قوله ﷺ: «إن لله تسعة وتسعين اسما، مائة إلا واحدا، من أحصاها دخل الجنة» (البخاري 2736 و7392، ومسلم 2677). " +
                    "أما سرد الأسماء بهذا العدد والترتيب فورد في رواية الترمذي (3507)، وقد ذكر كثير من المحققين أن تعيينها فيها من إدراج بعض الرواة وليس من لفظ النبي ﷺ؛ " +
                    "ولذلك تختلف قوائم العلماء في بعض الأسماء. القائمة هنا كما هي في موقع آيات تتلى.",
                style = MaterialTheme.typography.bodySmall, color = b.ink, modifier = Modifier.padding(top = 4.dp),
            )
        }
        shown.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (n, name) ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 86.dp).pressable {
                            PendingText.value = CardText("بطاقة اسم", name, "من أسماء الله الحسنى")
                            nav.navigate("text-card")
                        }.surface(b).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("$n", style = MaterialTheme.typography.labelSmall, color = b.muted)
                        Text(name, fontFamily = Fonts.naskh, fontSize = 21.sp, color = b.primaryInk, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ───────────── Occasions (/tools/occasions) ─────────────

@Composable
fun OccasionsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val today = LocalDate.now()
    val list = remember(today) {
        listOf(Triple("رمضان", 9, 1), Triple("عيد الفطر", 10, 1), Triple("يوم عرفة", 12, 9), Triple("عيد الأضحى", 12, 10)).mapNotNull { (name, m, d) ->
            val hy = Hijri.fromGregorian(today).year
            val g = listOf(hy, hy + 1).firstNotNullOfOrNull { y -> Hijri.toGregorian(Hijri.Date(y, m, d))?.takeIf { !it.isBefore(today) }?.let { it to y } }
            g?.let { (date, y) -> Triple(name, "$d ${Hijri.months[m - 1]} $y هـ · ${date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ar")))} م", ChronoUnit.DAYS.between(today, date)) }
        }.sortedBy { it.third }
    }
    ToolPage(nav, "المناسبات", "occasions") {
        GroupedSection {
            list.forEachIndexed { i, (name, line, days) ->
                GroupedRow(name, subtitle = line, value = if (days == 0L) "اليوم" else "بعد $days يوما", divider = i < list.lastIndex)
            }
        }
        Text("حسب تقويم أم القرى، وقد يختلف يوما عن رؤية الهلال.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Qada of prayers (/tools/qada-salah) ─────────────

private val PrayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

@Composable
fun QadaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    fun load(): Triple<List<Int>, Int, Int> = runCatching {
        val o = JSONObject(sp(ctx).getString("qada", null) ?: "{}")
        val l = o.optJSONArray("left") ?: JSONArray("[0,0,0,0,0]")
        Triple(List(5) { l.optInt(it) }, o.optInt("done"), o.optInt("per", 1))
    }.getOrDefault(Triple(listOf(0, 0, 0, 0, 0), 0, 1))
    var state by remember { mutableStateOf(load()) }
    fun save(s: Triple<List<Int>, Int, Int>) {
        state = s
        sp(ctx).edit().putString("qada", JSONObject().put("left", JSONArray(s.first)).put("done", s.second).put("per", s.third).toString()).apply()
    }
    var days by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val (left, done, per) = state
    val worst = left.max()
    val daysLeft = if (worst == 0) 0 else (worst + per - 1) / per
    ToolPage(nav, "قضاء الصلوات", "qada-salah") {
        AppCard {
            Text("عدد الأيام الفائتة تقريبا", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(days, "مثال: 30", Modifier.weight(1f)) { days = it }
                BrandButton("أضف للعداد", enabled = (days.toIntOrNull() ?: 0) > 0) {
                    val n = days.toIntOrNull() ?: 0
                    save(Triple(left.map { it + n }, done, per)); days = ""
                }
            }
            Text("يضاف العدد لكل صلاة من الصلوات الخمس، ويمكنك تعديل كل صلاة بالأزرار.", style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.padding(top = 6.dp))
        }
        Text("المتبقي: ${left.sum()} صلاة — تم قضاء: $done", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
        GroupedSection {
            PrayerNames.forEachIndexed { i, name ->
                GroupedRow(name, divider = i < 4) {
                    Text("${left[i]}", style = MaterialTheme.typography.titleLarge, color = b.primaryInk, modifier = Modifier.padding(horizontal = 10.dp))
                    BrandButton("قضيت واحدة", enabled = left[i] > 0) {
                        save(Triple(left.mapIndexed { j, v -> if (j == i) v - 1 else v }, done + 1, per))
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(40.dp).clip(CircleShape).background(b.tint).clickable { save(Triple(left.mapIndexed { j, v -> if (j == i) v + 1 else v }, done, per)) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, "زيادة", tint = b.primaryInk)
                    }
                }
            }
        }
        Text("كم صلاة تقضي يوميا مع كل فرض؟", style = MaterialTheme.typography.titleMedium, color = b.ink)
        SiteSegmented(listOf(1 to "واحدة", 2 to "اثنتان", 3 to "ثلاث", 5 to "خمس"), per, { save(Triple(left, done, it)) })
        if (daysLeft > 0) Text("بقضاء $per مع كل فرض تنتهي خلال نحو $daysLeft يوما بإذن الله", style = MaterialTheme.typography.bodyLarge, color = b.primaryInk)
        BrandButton(if (confirm) "اضغط مرة أخرى للتصفير" else "تصفير العداد", Modifier.fillMaxWidth(), kind = BtnKind.Secondary, enabled = left.sum() > 0 || done > 0) {
            if (confirm) { save(Triple(listOf(0, 0, 0, 0, 0), 0, per)); confirm = false } else confirm = true
        }
        Text("هذه الأداة تنظم العدد فقط. اسأل أهل العلم عن أحكام القضاء وترتيبه. يحفظ على جهازك فقط.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Fasting tracker (/tools/sawm-tracker) ─────────────

@Composable
fun SawmScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val marks = remember {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            runCatching { JSONObject(sp(ctx).getString("sawm.marks", "{}")!!).let { o -> o.keys().forEach { k -> put(k, o.getString(k)) } } }
        }
    }
    var required by remember { mutableIntStateOf(sp(ctx).getInt("sawm.required", 0)) }
    var reqText by remember { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("qada") }
    var month by remember { mutableStateOf(YearMonth.now()) }
    fun persist() { sp(ctx).edit().putString("sawm.marks", JSONObject(marks.toMap()).toString()).putInt("sawm.required", required).apply() }
    val qadaDone = marks.values.count { it == "qada" }
    val year = LocalDate.now().year
    val tat = marks.count { it.value == "tatawwu" && it.key.startsWith("$year-") }
    ToolPage(nav, "متتبع الصيام", "sawm-tracker") {
        Text("القضاء: $qadaDone من $required — التطوع هذا العام: $tat يوما", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
        AppCard {
            Text("عدد أيام القضاء المطلوبة", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(reqText, "$required", Modifier.weight(1f)) { reqText = it }
                BrandButton("حفظ", kind = BtnKind.Secondary, enabled = reqText.isNotEmpty()) { required = reqText.toIntOrNull() ?: 0; reqText = ""; persist() }
            }
        }
        SiteSegmented(listOf("qada" to "تسجيل قضاء", "tatawwu" to "تسجيل تطوع"), mode, { mode = it })
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { month = month.minusMonths(1) }, contentAlignment = Alignment.Center) { Text("›", style = MaterialTheme.typography.headlineSmall, color = b.primaryInk) }
                val first = month.atDay(1)
                val h = Hijri.fromGregorian(first)
                Text(
                    first.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("ar"))) + " — ${Hijri.months[h.month - 1]} ${h.year} هـ",
                    style = MaterialTheme.typography.titleMedium, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { month = month.plusMonths(1) }, contentAlignment = Alignment.Center) { Text("‹", style = MaterialTheme.typography.headlineSmall, color = b.primaryInk) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val lead = month.atDay(1).dayOfWeek.value % 7   // Sunday first
            val cells = List(lead) { null } + (1..month.lengthOfMonth()).toList()
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        if (d == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else {
                            val key = "${month.year}-${month.monthValue}-$d"
                            val m = marks[key]
                            val fill = when (m) { "qada" -> b.green; "tatawwu" -> b.accent; else -> Color.Transparent }
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).padding(3.dp).clip(CircleShape).background(fill)
                                    .clickable { if (marks[key] == mode) marks.remove(key) else marks[key] = mode; persist() },
                                contentAlignment = Alignment.Center,
                            ) { Text("$d", style = MaterialTheme.typography.bodyMedium, color = if (m != null) Color.White else b.ink) }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("اختر قضاء أو تطوع ثم اضغط اليوم في التقويم. الضغط مرة ثانية يلغي العلامة. يحفظ على جهازك فقط.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Nearby mosques (/tools/nearby-mosques) ─────────────

@Composable
fun MosquesScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var radius by rememberSaveable { mutableIntStateOf(3) }
    var phase by remember { mutableStateOf("idle") }
    var list by remember { mutableStateOf<List<SiteApi.Mosque>>(emptyList()) }
    var at by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    fun search() {
        val p = at ?: return
        phase = "searching"
        scope.launch {
            runCatching { SiteApi.mosques(p.first, p.second, radius * 1000) }
                .onSuccess { list = it; phase = "done" }.onFailure { phase = "failed" }
        }
    }
    val locate = rememberLocationRequest { loc ->
        if (loc == null) phase = "denied" else { at = loc.latitude to loc.longitude; search() }
    }
    ToolPage(nav, "المساجد القريبة", "nearby-mosques") {
        SiteSegmented(listOf(1 to "1 كم", 3 to "3 كم", 5 to "5 كم"), radius, { radius = it; if (at != null) search() })
        BrandButton("حدد موقعي وابحث", Modifier.fillMaxWidth(), icon = Icons.Outlined.MyLocation, kind = BtnKind.Dark, enabled = phase != "searching") {
            phase = "searching"; locate()
        }
        when (phase) {
            "idle" -> Text("اختر نصف القطر ثم اضغط «حدد موقعي وابحث». البحث في خرائط OpenStreetMap ولا يحفظ موقعك.", style = MaterialTheme.typography.bodySmall, color = b.muted)
            "searching" -> LoadingState(Modifier.height(140.dp))
            "denied" -> EmptyState("الموقع غير متاح", "اسمح للتطبيق بالوصول إلى موقعك وشغل خدمة الموقع، ثم حاول مرة أخرى.")
            "failed" -> EmptyState("تعذر البحث", "تحقق من الاتصال بالإنترنت ثم حاول مرة أخرى.")
            else -> if (list.isEmpty()) EmptyState("لا توجد مساجد ضمن $radius كم", if (radius < 5) "جرب نصف قطر أكبر" else "لم تعثر الخرائط على مساجد قريبة")
            else {
                Text("${list.size} مسجدا ضمن $radius كم", style = MaterialTheme.typography.titleMedium, color = b.ink)
                GroupedSection {
                    list.take(60).forEachIndexed { i, m ->
                        GroupedRow(
                            m.name, icon = Icons.Outlined.Mosque, tile = TileStyle.Soft,
                            value = if (m.distanceM < 1000) "${m.distanceM.toInt()} م" else String.format(Locale.US, "%.1f كم", m.distanceM / 1000),
                            divider = i < minOf(list.size, 60) - 1,
                            onClick = {
                                val uri = Uri.parse("geo:${m.lat},${m.lng}?q=${m.lat},${m.lng}(${Uri.encode(m.name)})")
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            },
                        )
                    }
                }
                Text("بيانات الخرائط من OpenStreetMap. اضغط مسجدا لفتح الاتجاهات.", style = MaterialTheme.typography.bodySmall, color = b.muted)
            }
        }
    }
}

// ───────────── Kids' Juz Amma (/tools/kids-juz-amma) ─────────────

object Kids {
    val surahs = (78..114).reversed().toList()
    val badges = listOf(1 to "البداية", 5 to "المثابر", 10 to "النجم", 20 to "البطل", 37 to "حافظ جزء عم")
    fun name(c: Context) = sp(c).getString("kids.name", "") ?: ""
    fun done(c: Context): Set<Int> = sp(c).getString("kids.done", "")!!.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    fun saveDone(c: Context, s: Set<Int>) = sp(c).edit().putString("kids.done", s.joinToString(",")).apply()
}

@Composable
fun KidsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    var name by remember { mutableStateOf(Kids.name(ctx)) }
    val done = remember { Kids.done(ctx) }
    ToolPage(nav, "تحفيظ الأطفال جزء عم", "kids-juz-amma") {
        AppCard {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (name.isEmpty()) Text("اسم الطفل", color = b.muted)
                BasicTextField(name, { name = it.take(30); sp(ctx).edit().putString("kids.name", name).apply() }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
            }
            Text("حفظ: ${done.size} من ${Kids.surahs.size} سورة", style = MaterialTheme.typography.titleMedium, color = b.primaryInk, modifier = Modifier.padding(top = 10.dp))
            LinearProgressIndicator(progress = { done.size / Kids.surahs.size.toFloat() }, color = b.accent, trackColor = b.tint, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kids.badges.forEach { (n, t) ->
                    val earned = done.size >= n
                    Row(
                        Modifier.clip(CircleShape).background(if (earned) b.tint else Color.Transparent).border(1.dp, b.line, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.WorkspacePremium, null, tint = if (earned) b.primaryInk else b.muted, modifier = Modifier.size(16.dp))
                        Text("وسام $t", style = MaterialTheme.typography.labelMedium, color = if (earned) b.primaryInk else b.muted)
                    }
                }
            }
        }
        Text("اختر سورة لتبدأ الاستماع والتكرار.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
        Kids.surahs.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { n ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 92.dp).pressable { nav.navigate("kids/$n") }.surface(b).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text(content.surah(n).shortName, style = MaterialTheme.typography.titleMedium, color = b.ink, maxLines = 1)
                        Text("$n", style = MaterialTheme.typography.labelMedium, color = b.muted)
                        Icon(if (n in done) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = if (n in done) Color(0xFFC9A227) else b.muted)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        BrandButton("شهادة تقدير", Modifier.fillMaxWidth(), icon = Icons.Outlined.WorkspacePremium, kind = BtnKind.Gold, enabled = done.isNotEmpty()) {
            val who = name.ifBlank { "بطلنا" }
            PendingText.value = CardText(
                "شهادة تقدير", "شهادة تقدير\n$who\nحفظ ${done.size} سورة من جزء عم\n" + "★".repeat(minOf(5, maxOf(1, (done.size + 7) / 8))),
                "تحفيظ الأطفال جزء عم", naskh = true, ratio = CardRatio.Portrait,
            )
            nav.navigate("text-card")
        }
        Text("النص: مصحف التنزيل. الصوت: EveryAyah.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

@Composable
fun KidsSurahScreen(nav: NavHostController, surah: Int) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val reciter = rememberReciter()
    var repeatEach by rememberSaveable { mutableIntStateOf(sp(ctx).getInt("kids.repeat", 3)) }
    var done by remember { mutableStateOf(surah in Kids.done(ctx)) }
    val ayat = remember(surah) { content.ayat(surah) }
    val s = content.surah(surah)
    val playing = Recitation.active && Recitation.current?.startsWith("$surah:") == true
    ToolPage(nav, "سورة ${s.shortName}") {
        Text("تكرار كل آية", style = MaterialTheme.typography.titleMedium, color = b.ink)
        SiteSegmented(listOf(1 to "مرة", 3 to "3 مرات", 5 to "5 مرات"), repeatEach, { repeatEach = it; sp(ctx).edit().putInt("kids.repeat", it).apply() })
        BrandButton(if (playing) "إيقاف" else "استمع وكرر", Modifier.fillMaxWidth(), icon = if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, kind = BtnKind.Dark) {
            if (playing) Recitation.stop() else Recitation.play(ctx, content, reciter, surah, 1, repeatEach = repeatEach)
        }
        AppCard {
            if (s.hasBasmala) Text(content.ayah(1, 1)?.text.orEmpty(), fontFamily = Fonts.quran, fontSize = 24.sp, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            ayat.forEach { a ->
                val current = Recitation.current == a.key
                Text(
                    a.text + " " + Num.ayahMark(a.ayah), fontFamily = Fonts.quran, fontSize = 26.sp, lineHeight = 50.sp, color = b.ink,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (current) b.tint else Color.Transparent)
                        .clickable { Recitation.play(ctx, content, reciter, surah, a.ayah, repeatEach = repeatEach) }.padding(6.dp),
                )
            }
        }
        BrandButton(if (done) "إلغاء الحفظ" else "حفظتها ★", Modifier.fillMaxWidth(), kind = if (done) BtnKind.Secondary else BtnKind.Gold) {
            val set = Kids.done(ctx).toMutableSet()
            if (done) set.remove(surah) else set.add(surah)
            Kids.saveDone(ctx, set); done = !done
        }
    }
}

// ───────────── Ayah of the day (/tools/ayah-of-the-day) ─────────────

@Composable
fun AyahTodayScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val reciter = rememberReciter()
    val pick = remember { DailyAyah.pick(LocalDate.now()) }
    val ayat = remember(pick) { content.ayat(pick.surah, pick.from, pick.to) }
    val ref = "سورة ${content.surah(pick.surah).shortName} · " + if (pick.from == pick.to) "${pick.from}" else "${pick.from}–${pick.to}"
    var tafsir by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(pick) {
        runCatching { SiteApi.words(ctx, pick.surah, "en") }.onSuccess { vs ->
            tafsir = vs.filter { it.n in pick.from..pick.to }.mapNotNull { it.tafsir }
        }
    }
    ToolPage(nav, "آية اليوم", "ayah-of-the-day") {
        EmeraldPanel {
            Text(ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }, fontFamily = Fonts.quran, fontSize = 24.sp, lineHeight = 46.sp, color = Color(0xFFF6F2E9), textAlign = TextAlign.Center)
            Text(ref, style = MaterialTheme.typography.labelLarge, color = b.onDeepAccent, modifier = Modifier.padding(top = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandButton("استمع", Modifier.weight(1f), icon = Icons.Filled.PlayArrow) { Recitation.play(ctx, content, reciter, pick.surah, pick.from, pick.to) }
            BrandButton("اصنع بطاقة", Modifier.weight(1f), icon = Icons.Outlined.PhotoLibrary, kind = BtnKind.Gold) { nav.navigate(Routes.studio(pick.surah, pick.from, pick.to)) }
        }
        BrandButton(if ("${pick.surah}:${pick.from}" in prefs.favorites) "محفوظة" else "احفظ الآية", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) {
            prefs.toggleFavorite("${pick.surah}:${pick.from}")
        }
        if (tafsir.isNotEmpty()) AppCard {
            Text("التفسير الميسر", style = MaterialTheme.typography.titleMedium, color = b.ink)
            tafsir.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

