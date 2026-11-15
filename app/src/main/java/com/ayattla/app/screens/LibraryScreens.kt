package com.ayattla.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.calc.KhatmaPlan
import com.ayattla.app.data.Hadith
import com.ayattla.app.data.SearchText
import com.ayattla.app.data.SiteApi
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.LoadingState
import com.ayattla.app.ui.components.Pill
import com.ayattla.app.ui.components.SmallShape
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------- Hadith (sahih and hasan only, from the site's verified collection) ----------

@Composable
fun HadithScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var cat by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    val items = remember { mutableStateListOf<Hadith>() }
    var cats by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(cat, submitted, page) {
        loading = true; failed = false
        runCatching { SiteApi.hadith(ctx, cat, page, submitted) }
            .onSuccess { r -> if (page == 1) items.clear(); items.addAll(r.items); cats = r.cats; total = r.total }
            .onFailure { failed = true }
        loading = false
    }
    fun reset() { page = 1; items.clear() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("الأحاديث", onBack = { nav.popBackStack() }) {
            TextButton(onClick = { nav.navigate("hadith-check") }) { Text("تحقق من حديث") }
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true, shape = SmallShape,
            placeholder = { Text("ابحث في الأحاديث") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { TextButton(onClick = { submitted = query.trim(); reset() }) { Text("بحث") } },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        )
        if (cats.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("الكل", cat.isEmpty(), { cat = ""; reset() })
            cats.forEach { (n, c) -> Chip("$n ($c)", cat == n, { cat = n; reset() }) }
        }
        when {
            failed && items.isEmpty() -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState("تعذر تحميل الأحاديث", "تحقق من اتصال الإنترنت ثم أعد المحاولة.")
                Button(onClick = { reset(); page = 1; submitted = submitted + "" }) { Text("إعادة المحاولة") }
            }
            loading && items.isEmpty() -> LoadingState()
            items.isEmpty() -> EmptyState("لا نتائج", "جرّب كلمة أخرى.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("$total حديثًا · صحيح وحسن فقط", style = MaterialTheme.typography.labelMedium, color = b.muted) }
                items(items, key = { it.id + it.url }) { h -> HadithCard(h, nav) }
                if (items.size < total) item {
                    OutlinedButton(onClick = { page += 1 }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp)) else Text("المزيد")
                    }
                }
            }
        }
    }
}

@Composable
fun HadithCard(h: Hadith, nav: NavHostController? = null) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    AppCard {
        h.title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = b.primaryInk) }
        Text(h.text, fontFamily = Fonts.naskh, fontSize = 19.sp, lineHeight = 34.sp, color = b.ink, modifier = Modifier.padding(vertical = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (h.source.isNotBlank()) Text(h.source, style = MaterialTheme.typography.labelMedium, color = b.goldInk)
                if (h.grade.isNotBlank()) Pill(h.grade, Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = { shareText(ctx, h.text + "\n\n" + listOf(h.source, h.grade).filter { it.isNotBlank() }.joinToString(" · ") + "\nayattla.com") }) {
                Icon(Icons.Filled.Share, "مشاركة", tint = b.muted)
            }
            if (h.url.startsWith("http")) TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(h.url))) }) { Text("المصدر") }
        }
        if (nav != null) com.ayattla.app.ui.components.BrandButton(
            "اصنع بطاقة حديث", Modifier.fillMaxWidth().padding(top = 6.dp), icon = Icons.Filled.Share, kind = com.ayattla.app.ui.components.BtnKind.Secondary,
        ) {
            com.ayattla.app.studio.PendingText.value = com.ayattla.app.studio.CardText("بطاقة حديث", h.text, listOf(h.source, h.grade).filter { it.isNotBlank() }.joinToString(" · "))
            nav.navigate("text-card")
        }
    }
}

@Composable
fun HadithCheckScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<List<Hadith>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("التحقق من حديث", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("الصق نص الحديث، ونبحث عنه في مصادره الموثقة مع درجته.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
            OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth(), placeholder = { Text("نص الحديث") })
            Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { SiteApi.checkHadith(text.trim()) }.onSuccess { result = it }.onFailure { error = "تعذر الاتصال بالموقع. أعد المحاولة." }
                        busy = false
                    }
                },
                enabled = text.trim().length >= 10 && !busy, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = b.primary),
            ) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = b.onPrimary) else Text("تحقق") }
            error?.let { Text(it, color = b.goldInk) }
            result?.let { r ->
                if (r.isEmpty()) EmptyState("لم نجد هذا النص", "لم نعثر عليه في المصادر المتاحة. لا يعني ذلك حكمًا عليه؛ راجع أهل العلم.")
                else r.forEach { HadithCard(it, nav) }
            }
        }
    }
}

// ---------- Ayat by topic ----------

@Composable
fun TopicsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val topics = LocalContent.current.library.topics
    var query by rememberSaveable { mutableStateOf("") }
    val q = SearchText.clean(query)
    val list = topics.filter { q.isEmpty() || q in SearchText.clean(it.title + " " + it.desc) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("آيات بالموضوع", onBack = { nav.popBackStack() })
        if (topics.isEmpty()) { EmptyState("غير متاحة في هذه النسخة", "لم تجهز حزمة المحتوى بالمواضيع."); return@Column }
        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, shape = SmallShape, placeholder = { Text("ابحث عن موضوع") },
            leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.slug }) { t ->
                AppCard(onClick = { nav.navigate("topic/${t.slug}") }) {
                    Text(t.title, style = MaterialTheme.typography.titleMedium, color = b.ink)
                    if (t.desc.isNotBlank()) Text(t.desc, style = MaterialTheme.typography.bodySmall, color = b.muted, maxLines = 2)
                    Text("${t.items.size} مواضع", style = MaterialTheme.typography.labelMedium, color = b.goldInk, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun TopicScreen(nav: NavHostController, slug: String) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ctx = LocalContext.current
    val reciter = rememberReciter()
    val t = content.library.topics.firstOrNull { it.slug == slug }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(t?.title ?: "موضوع", onBack = { nav.popBackStack() })
        if (t == null) return@Column
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (t.desc.isNotBlank()) item { Text(t.desc, style = MaterialTheme.typography.bodyMedium, color = b.muted) }
            items(t.items, key = { "${it.s}:${it.a}-${it.b}" }) { r ->
                val ayat = remember(r) { content.ayat(r.s, r.a, r.b) }
                AppCard {
                    Text(ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }, fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 40.sp, color = b.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("سورة ${content.surah(r.s).shortName} · " + if (r.a == r.b) "${r.a}" else "${r.a}-${r.b}",
                            style = MaterialTheme.typography.labelMedium, color = b.goldInk, modifier = Modifier.weight(1f))
                        IconButton(onClick = { Recitation.play(ctx, content, reciter, r.s, r.a, r.b) }) { Icon(Icons.Filled.PlayArrow, "استمع", tint = b.goldInk) }
                        IconButton(onClick = { nav.navigate(Routes.studio(r.s, r.a, minOf(r.b, r.a + 9))) }) { Icon(Icons.Filled.AutoAwesome, "بطاقة", tint = b.goldInk) }
                        TextButton(onClick = { nav.navigate(Routes.reader(r.s, r.a)) }) { Text("في المصحف") }
                    }
                }
            }
        }
    }
}

// ---------- Duas ----------

@Composable
fun DuasScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ctx = LocalContext.current
    val duas = content.library.duas
    var need by rememberSaveable { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("الأدعية", onBack = { if (need >= 0) need = -1 else nav.popBackStack() })
        if (duas == null) { EmptyState("غير متاحة في هذه النسخة", "لم تجهز حزمة المحتوى بالأدعية."); return@Column }
        if (need < 0) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (duas.quran.isNotEmpty()) item {
                    AppCard(onClick = { need = Int.MAX_VALUE }) {
                        Text("أدعية من القرآن", style = MaterialTheme.typography.titleMedium, color = b.ink)
                        Text("${duas.quran.size} دعاء بالرسم العثماني", style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                }
                items(duas.needs.size) { i ->
                    val n = duas.needs[i]
                    AppCard(onClick = { need = i }) {
                        Text(n.need, style = MaterialTheme.typography.titleMedium, color = b.ink)
                        Text("${n.chapters.sumOf { it.items.size }} دعاء", style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                }
            }
        } else if (need == Int.MAX_VALUE) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(duas.quran, key = { "${it.s}:${it.a}" }) { r ->
                    val ayat = remember(r) { content.ayat(r.s, r.a, r.b) }
                    AppCard {
                        Text(ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }, fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 40.sp, color = b.ink)
                        Text("سورة ${content.surah(r.s).shortName} · ${r.a}", style = MaterialTheme.typography.labelMedium, color = b.goldInk)
                    }
                }
            }
        } else {
            val n = duas.needs[need]
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(n.need, style = MaterialTheme.typography.titleLarge, color = b.ink) }
                n.chapters.forEach { ch ->
                    item { Text(ch.name, style = MaterialTheme.typography.titleSmall, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp)) }
                    items(ch.items.size) { i ->
                        val text = ch.items[i]
                        AppCard {
                            Text(text, fontFamily = Fonts.naskh, fontSize = 19.sp, lineHeight = 34.sp, color = b.ink)
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = { shareText(ctx, "$text\n\nayattla.com") }) { Icon(Icons.Filled.Share, "مشاركة", tint = b.muted) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Prayer guide ----------

@Composable
fun GuideScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val g = LocalContent.current.library.guide
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("صفة الصلاة", onBack = { nav.popBackStack() })
        if (g == null) { EmptyState("غير متاحة في هذه النسخة", "لم تجهز حزمة المحتوى بدليل الصلاة."); return@Column }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (g.lead.isNotBlank()) item { Text(g.lead, style = MaterialTheme.typography.bodyMedium, color = b.muted) }
            g.sections.forEach { s ->
                item { Text(s.title, style = MaterialTheme.typography.titleMedium, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp)) }
                items(s.items.size) { i ->
                    val it = s.items[i]
                    AppCard {
                        Text(it.text, fontFamily = Fonts.naskh, fontSize = 18.sp, lineHeight = 32.sp, color = b.ink)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(listOf(it.source, it.grade).filter { x -> x.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = b.goldInk, modifier = Modifier.weight(1f))
                            if (it.url.startsWith("http")) TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url))) }) { Text("المصدر") }
                        }
                    }
                }
            }
            if (g.source.isNotBlank()) item { Text("المصدر: ${g.source}", style = MaterialTheme.typography.bodySmall, color = b.muted) }
        }
    }
}

// ---------- Khatma ----------

@Composable
fun KhatmaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    val content = LocalContent.current
    val plan = KhatmaPlan.decode(prefs.khatma)
    var days by rememberSaveable { mutableIntStateOf(30) }
    var startPage by rememberSaveable { mutableIntStateOf(1) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("خطة الختمة", onBack = { nav.popBackStack() }) {
            if (plan != null) TextButton(onClick = { prefs.saveKhatma(null) }) { Text("حذف الخطة") }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (plan == null) {
                Text("مدة الختمة", style = MaterialTheme.typography.titleMedium, color = b.ink)
                FlowChips(KhatmaPlan.presets.map { "$it يومًا" }, "$days يومًا") { days = it.substringBefore(" ").toInt() }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أبدأ من صفحة $startPage", style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                        TextButton(onClick = { if (startPage > 1) startPage -= 1 }) { Text("−") }
                        TextButton(onClick = { if (startPage < KhatmaPlan.PAGES) startPage += 1 }) { Text("+") }
                    }
                    if (startPage > 1) Text("سورة ${content.surah(content.page(startPage).first().surah).shortName}", style = MaterialTheme.typography.bodySmall, color = b.muted)
                    val p = KhatmaPlan.create(days, startPage, LocalDate.now())
                    Text("نحو ${String.format(java.util.Locale.US, "%.1f", p.pagesPerDay)} صفحة يوميًا، من صفحة $startPage إلى 604.", style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.padding(top = 6.dp))
                }
                Button(onClick = { prefs.saveKhatma(KhatmaPlan.create(days, startPage, LocalDate.now()).encode()) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = b.primary)) {
                    Text("ابدأ الخطة")
                }
            } else {
                val today = plan.dayOn(LocalDate.now()).coerceIn(1, plan.days)
                EmeraldPanel {
                    Text(if (plan.isFinished) "أتممت الختمة، تقبل الله منك" else "اليوم $today من ${plan.days}", style = MaterialTheme.typography.titleLarge, color = b.onPrimary)
                    plan.range(today)?.let { r -> Text("صفحات ${r.first} إلى ${r.last}", style = MaterialTheme.typography.titleMedium, color = b.accent) }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { plan.completedDays.size.toFloat() / plan.days }, color = b.accent, trackColor = b.onPrimary.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth())
                    Text("${plan.completedDays.size} من ${plan.days} يومًا · الختام المتوقع ${plan.expectedEnd}", style = MaterialTheme.typography.bodySmall, color = b.onPrimary.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp))
                }
                plan.range(today)?.let { r ->
                    Button(onClick = { nav.navigate("page/${r.first}") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = b.primary)) { Text("اقرأ ورد اليوم") }
                    AppCard {
                        Text("مقسّمًا على الصلوات", style = MaterialTheme.typography.titleSmall, color = b.ink)
                        val names = listOf("بعد الفجر", "بعد الظهر", "بعد العصر", "بعد المغرب", "بعد العشاء")
                        KhatmaPlan.splitOverPrayers(r).forEachIndexed { i, part ->
                            if (part != null) Text("${names[i]}: صفحات ${part.first} - ${part.last}", style = MaterialTheme.typography.bodyMedium, color = b.muted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text("الأيام", style = MaterialTheme.typography.titleMedium, color = b.ink)
                AppCard(padding = 4.dp) {
                    (1..plan.days).forEach { d ->
                        val done = d in plan.completedDays
                        val r = plan.range(d)
                        Row(
                            Modifier.fillMaxWidth().background(if (d == today) b.tint else b.surface).clickable {
                                val set = if (done) plan.completedDays - d else plan.completedDays + d
                                prefs.saveKhatma(plan.copy(completedDays = set).encode())
                            }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (done) b.accent else b.lineStrong)
                            Text("اليوم $d", style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                            if (r != null) Text("${r.first}-${r.last}", style = MaterialTheme.typography.bodySmall, color = b.muted)
                        }
                        HorizontalDivider(color = b.line)
                    }
                }
            }
            Text("الحساب على مصحف المدينة 604 صفحات، مطابق لأداة الختمة في الموقع.", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center)
        }
    }
}
