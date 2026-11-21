package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.audio.Downloads
import com.ayattla.app.data.Ayah
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Hifz
import com.ayattla.app.data.Notes
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import kotlinx.coroutines.launch
import java.util.Locale

/* ───────────── ملاحظات التدبر على الآيات المحفوظة ───────────── */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteSheet(key: String, title: String, onDismiss: () -> Unit) {
    val b = LocalBrand.current
    val old = Notes.all[key]
    var note by remember { mutableStateOf(old?.note.orEmpty()) }
    var tag by remember { mutableStateOf(old?.tag.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = b.background) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ملاحظة تدبر · $title", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).padding(12.dp)) {
                if (note.isEmpty()) Text("اكتب ما تدبرته في هذه الآية…", color = b.muted)
                BasicTextField(note, { note = it.take(2000) }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink),
                    cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
            }
            Text("التصنيف", style = MaterialTheme.typography.labelLarge, color = b.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (Notes.defaultTags + Notes.tags).distinct().forEach { t -> Chip(t, tag == t, { tag = if (tag == t) "" else t }) }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (tag.isEmpty()) Text("أو اكتب تصنيفا جديدا", color = b.muted)
                BasicTextField(tag, { tag = it.take(24) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink),
                    cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandButton("حفظ", Modifier.weight(1f)) { Notes.set(key, Notes.Entry(note, tag)); onDismiss() }
                if (old != null) BrandButton("حذف الملاحظة", Modifier.weight(1f), kind = BtnKind.Secondary) { Notes.set(key, null); onDismiss() }
            }
        }
    }
}

/* ───────────── خطة الحفظ والمراجعة المتباعدة ───────────── */

@Composable
fun HifzPlanScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    LaunchedEffect(Unit) { Hifz.load(ctx) }
    val plan = Hifz.plan
    ToolPage(nav, "خطة الحفظ والمراجعة", "hifz-plan") {
        if (plan == null) {
            var forward by remember { mutableStateOf(false) }
            var daily by remember { mutableIntStateOf(3) }
            AppCard {
                Text("ابدأ خطة حفظ", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
                Text("الترتيب", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("من سورة الناس صعودا", !forward, { forward = false })
                    Chip("من الفاتحة", forward, { forward = true })
                }
                Text("مقدار الحفظ اليومي", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 10).forEach { n -> Chip("$n آيات", daily == n, { daily = n }) }
                }
            }
            BrandButton("ابدأ الخطة", Modifier.fillMaxWidth()) { Hifz.start(forward, daily, if (forward) 1 else 114) }
            return@ToolPage
        }
        val unit = Hifz.nextUnit(content)
        val due = Hifz.due()
        AppCard {
            Text("حفظت ${Hifz.memorisedAyat()} آية", style = MaterialTheme.typography.titleLarge, color = b.primaryInk)
            LinearProgressIndicator(progress = { Hifz.memorisedAyat() / 6236f }, color = b.accent, trackColor = b.tint, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text("مراجعات اليوم: ${due.size} · المقدار اليومي: ${plan.daily} آيات", style = MaterialTheme.typography.bodyMedium, color = b.muted)
        }
        Text("حفظ اليوم", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
        when {
            unit == null -> AppCard { Text("أتممت القرآن كاملا في هذه الخطة. بارك الله فيك.", color = b.ink) }
            Hifz.learnedToday() -> AppCard { Text("أنجزت حفظ اليوم ✓ — المقطع التالي غدا: سورة ${content.surah(unit.surah).shortName} ${unit.from}–${unit.to}", color = b.ink) }
            else -> UnitCard(content, unit, nav) {
                BrandButton("حفظته", Modifier.fillMaxWidth(), kind = BtnKind.Gold) { Hifz.learned(content) }
            }
        }
        Text("المراجعة", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
        if (due.isEmpty()) Text("لا مراجعة مستحقة اليوم.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
        due.forEach { r ->
            UnitCard(content, r.unit, nav) {
                Text("قيّم مراجعتك:", style = MaterialTheme.typography.labelMedium, color = b.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("صعب", Modifier.weight(1f), kind = BtnKind.Secondary) { Hifz.rate(r, 0) }
                    BrandButton("متوسط", Modifier.weight(1f), kind = BtnKind.Secondary) { Hifz.rate(r, 1) }
                    BrandButton("أتقنته", Modifier.weight(1f)) { Hifz.rate(r, 2) }
                }
            }
        }
        val upcoming = plan.reviews.filter { it.due > Hifz.today() }.sortedBy { it.due }.take(5)
        if (upcoming.isNotEmpty()) {
            Text("القادم", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
            AppCard {
                upcoming.forEach { r ->
                    val days = r.due - Hifz.today()
                    Text("سورة ${content.surah(r.unit.surah).shortName} ${r.unit.from}–${r.unit.to} · بعد ${if (days == 1L) "يوم" else "$days أيام"}",
                        style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
        Text("المقدار اليومي", style = MaterialTheme.typography.labelLarge, color = b.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1, 3, 5, 10).forEach { n -> Chip("$n", plan.daily == n, { Hifz.setDaily(n) }) } }
        var confirm by remember { mutableStateOf(false) }
        BrandButton(if (confirm) "اضغط مرة أخرى لحذف الخطة" else "إعادة بدء الخطة", Modifier.fillMaxWidth(), kind = BtnKind.Ghost) {
            if (confirm) Hifz.reset() else confirm = true
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitCard(content: ContentStore, u: Hifz.Unit, nav: NavHostController, actions: @Composable () -> Unit) {
    val b = LocalBrand.current
    val ayat = remember(u) { content.ayat(u.surah, u.from, u.to) }
    AppCard {
        Text("سورة ${content.surah(u.surah).shortName} · ${u.from}${if (u.to > u.from) "–${u.to}" else ""}",
            style = MaterialTheme.typography.labelLarge, color = b.goldInk)
        Text(ayat.joinToString(" ") { it.text + " ﴿${it.ayah}﴾" }, fontFamily = Fonts.quran, fontSize = 22.sp, lineHeight = 42.sp, color = b.ink,
            modifier = Modifier.padding(vertical = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("افتح في المصحف", false, { nav.navigate(Routes.reader(u.surah, u.from)) })
            Chip("سمّع بصوتك", false, { nav.navigate(Routes.tasmee(u.surah, u.from, minOf(u.to, u.from + 29))) })
            Chip("اختبر حفظك", false, { nav.navigate("hifz-test") })
        }
        actions()
    }
}

/* ───────────── التلاوات المحمّلة ───────────── */

@Composable
fun DownloadsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val scope = rememberCoroutineScope()
    var reciter by remember { mutableStateOf(rememberReciterStatic(content, ctx)) }
    val v = Downloads.version
    val size = remember(v, reciter, com.ayattla.app.data.OfflinePrep.version) { Downloads.sizeBytes(ctx) }
    var failed by remember { mutableStateOf<String?>(null) }
    ToolPage(nav, "التلاوات المحمّلة", "downloads") {
        AppCard {
            Text("المساحة المستعملة: ${mb(size)}", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
            Text("السور المحمّلة تعمل بدون إنترنت في الاستماع والرقية وتحفيظ الأطفال وصانع المقاطع. وما تستمع إليه يحفظ تلقائيا أيضا.", style = MaterialTheme.typography.bodySmall, color = b.muted)
            if (com.ayattla.app.data.OfflinePrep.running) {
                Text(com.ayattla.app.data.OfflinePrep.stage, style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp))
                androidx.compose.material3.LinearProgressIndicator(progress = { com.ayattla.app.data.OfflinePrep.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = b.green, trackColor = b.line)
            } else com.ayattla.app.ui.components.BrandButton(
                "حمّل جزء عم كاملا بصوت ${reciter.name}", Modifier.fillMaxWidth().padding(top = 8.dp),
                kind = com.ayattla.app.ui.components.BtnKind.Gold, enabled = com.ayattla.app.data.Net.isOnline,
            ) { com.ayattla.app.data.OfflinePrep.downloadRecitations(ctx, content, reciter) }
            com.ayattla.app.data.OfflinePrep.lastResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = b.primaryInk, modifier = Modifier.padding(top = 6.dp)) }
        }
        Text("القارئ", style = MaterialTheme.typography.labelLarge, color = b.muted)
        // One scrolling row: the surahs stay on the first screen.
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content.reciters.forEach { r -> Chip(r.name, r.id == reciter.id, { reciter = r }) }
        }
        failed?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFB3261E), style = MaterialTheme.typography.bodyMedium) }
        AppCard(padding = 4.dp) {
            content.surahs.forEach { s ->
                val key = "${reciter.folder}/${s.id}"
                val p = Downloads.progress[key]
                val have = remember(v, reciter, s.id) { Downloads.isDownloaded(ctx, reciter.folder, s.id, s.ayahCount) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.id}. ${s.shortName}", style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                    Text("${s.ayahCount} آية", style = MaterialTheme.typography.labelMedium, color = b.muted)
                    when {
                        p != null -> Box(Modifier.padding(12.dp)) { CircularProgressIndicator(progress = { p }, color = b.green, modifier = Modifier.padding(2.dp)) }
                        have -> IconButton({ Downloads.delete(ctx, reciter.folder, s.id, s.ayahCount) }) { Icon(Icons.Outlined.Delete, "حذف", tint = b.muted) }
                        else -> IconButton({
                            val r = reciter
                            scope.launch {
                                failed = null
                                if (!Downloads.download(ctx, r.folder, s.id, s.ayahCount)) failed = "تعذر تحميل بعض آيات سورة ${s.shortName}. تحقق من الاتصال وأعد المحاولة."
                            }
                        }) { Icon(Icons.Outlined.Download, "تحميل", tint = b.green) }
                    }
                }
            }
        }
        BrandButton("حذف كل تلاوات ${reciter.name}", Modifier.fillMaxWidth(), kind = BtnKind.Ghost) { Downloads.deleteAll(ctx, reciter.folder) }
    }
}

private fun rememberReciterStatic(content: ContentStore, ctx: android.content.Context) =
    (ctx.applicationContext as com.ayattla.app.AyatApp).prefs.reciterId.let { id -> content.reciters.firstOrNull { it.id == id } ?: content.reciters.first() }

private fun mb(bytes: Long) = String.format(Locale.US, "%.1f م.ب", bytes / 1_048_576.0)

/* ───────────── مسابقة الأطفال (جزء عم) ───────────── */

/** Questions built only from the mushaf text itself: no wording is generated. */
object KidsQuiz {
    data class Q(val prompt: String, val ayah: Ayah, val options: List<String>, val answer: Int, val kind: Int)

    private fun words(a: Ayah) = a.text.split(" ").filter { it.isNotBlank() }

    fun make(content: ContentStore, pool: List<Int>, rnd: java.util.Random): Q {
        val surahs = pool.ifEmpty { Kids.surahs }
        val s = surahs[rnd.nextInt(surahs.size)]
        val ayat = content.ayat(s).filter { words(it).size >= 4 }.ifEmpty { content.ayat(s) }
        val a = ayat[rnd.nextInt(ayat.size)]
        return if (rnd.nextBoolean()) {
            // «أكمل الآية»: the first half shown, the real second half among others from Juz Amma.
            val w = words(a)
            // Cut between words, never just before a pause mark, so both halves stay whole words.
            var cut = w.size / 2
            while (cut < w.size - 1 && w[cut].none { it in '\u0621'..'\u064A' }) cut++
            val right = w.drop(cut).joinToString(" ")
            val wrong = generateSequence { val o = content.ayat(Kids.surahs[rnd.nextInt(Kids.surahs.size)]).filter { words(it).size >= 4 }.randomOrNull() ?: a; words(o).let { it.drop(it.size / 2).joinToString(" ") } }
                .filter { it != right }.distinct().take(2).toList()
            val opts = (wrong + right).shuffled(kotlin.random.Random(rnd.nextLong()))
            Q(w.take(cut).joinToString(" ") + " …", a, opts, opts.indexOf(right), 0)
        } else {
            // «من أي سورة؟»
            val right = content.surah(s).shortName
            val wrong = Kids.surahs.filter { it != s }.shuffled(kotlin.random.Random(rnd.nextLong())).take(2).map { content.surah(it).shortName }
            val opts = (wrong + right).shuffled(kotlin.random.Random(rnd.nextLong()))
            Q(a.text, a, opts, opts.indexOf(right), 1)
        }
    }
}

@Composable
fun KidsQuizScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val rnd = remember { java.util.Random() }
    val pool = remember { Kids.done(ctx).toList() }
    var q by remember { mutableStateOf(KidsQuiz.make(content, pool, rnd)) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    ToolPage(nav, "مسابقة جزء عم") {
        Text("النتيجة: $score من $total", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
        AppCard {
            Text(if (q.kind == 0) "أكمل الآية" else "من أي سورة هذه الآية؟", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
            Text(q.prompt, fontFamily = Fonts.quran, fontSize = 24.sp, lineHeight = 46.sp, color = b.ink, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        q.options.forEachIndexed { i, o ->
            val kind = when {
                picked == null -> BtnKind.Secondary
                i == q.answer -> BtnKind.Primary
                i == picked -> BtnKind.Ghost
                else -> BtnKind.Secondary
            }
            BrandButton(o, Modifier.fillMaxWidth(), kind = kind, enabled = picked == null || i == q.answer) {
                if (picked == null) { picked = i; total++; if (i == q.answer) score++ }
            }
        }
        picked?.let { p ->
            Text(if (p == q.answer) "أحسنت! ★" else "الجواب الصحيح مظلل. سورة ${content.surah(q.ayah.surah).shortName} الآية ${q.ayah.ayah}.",
                style = MaterialTheme.typography.titleMedium, color = if (p == q.answer) b.primaryInk else b.ink)
            BrandButton("السؤال التالي", Modifier.fillMaxWidth(), kind = BtnKind.Gold) { q = KidsQuiz.make(content, pool, rnd); picked = null }
        }
    }
}
