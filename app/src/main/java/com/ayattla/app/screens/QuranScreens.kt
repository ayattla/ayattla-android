package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.ui.components.Badge
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chevron
import com.ayattla.app.ui.components.HeroHeader
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.LargeTitle
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.Ayah
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.SmallShape
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num

/** «القرآن» tab, as the iOS QuranView: large title, search, hero with continue reading, Quran tools, every surah. */
@Composable
fun QuranIndexScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var showJuz by rememberSaveable { mutableStateOf(false) }
    val q = query.trim()
    val surahHits = remember(q) { if (q.isEmpty()) content.surahs else content.surahs.filter { q in it.shortName || "${it.id}" == q } }
    val ayahHits = remember(q) { if (q.length < 2) emptyList() else content.search(q) }
    val topicHits = remember(q) { if (q.length < 2) emptyList() else content.library.topics.filter { q in it.title || q in it.desc } }
    val last = prefs.lastRead?.split(":")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
    val lastPage = last?.let { content.pageOf(it[0], it[1]) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, LocalBottomInset.current), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                LargeTitle(if (showJuz) "الأجزاء" else "القرآن")
                // iOS search field: grey rounded box.
                Row(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(b.ink.copy(alpha = 0.07f)).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Search, null, tint = b.muted, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("ابحث في السور والآيات", style = MaterialTheme.typography.bodyLarge, color = b.muted)
                        BasicTextField(query, { query = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
                    }
                    if (query.isNotEmpty()) Icon(Icons.Filled.Close, "مسح", tint = b.muted, modifier = Modifier.size(18.dp).clickable { query = "" })
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        if (q.isNotEmpty()) {
            if (surahHits.isNotEmpty()) {
                item { SectionHeader("السور"); Spacer(Modifier.height(8.dp)) }
                item { SurahBlock(surahHits, nav) }
            }
            if (topicHits.isNotEmpty()) {
                item { Spacer(Modifier.height(20.dp)); SectionHeader("مواضيع", subtitle = "آيات مختارة عن الموضوع مع التفسير الميسر"); Spacer(Modifier.height(8.dp)) }
                items(topicHits, key = { "t-" + it.slug }) { t ->
                    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().pressable { nav.navigate("topic/${t.slug}") }.surface(b).padding(14.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium, color = b.ink)
                        Text("${t.items.size} مواضع", style = MaterialTheme.typography.labelMedium, color = b.primaryInk)
                    }
                }
            }
            if (ayahHits.isNotEmpty()) {
                item { Spacer(Modifier.height(20.dp)); SectionHeader("الآيات", subtitle = "${ayahHits.size} نتيجة · كلمات متعددة تجد الآيات التي فيها كلها، والعبارة بين \"\" تطابق كما هي"); Spacer(Modifier.height(8.dp)) }
                items(ayahHits, key = { it.key }) { a ->
                    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().pressable { nav.navigate(Routes.reader(a.surah, a.ayah)) }.surface(b).padding(14.dp)) {
                        Text(a.text, fontFamily = Fonts.quran, fontSize = 20.sp, lineHeight = 38.sp, color = b.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("سورة ${content.surah(a.surah).shortName} · ${a.ayah}", style = MaterialTheme.typography.labelMedium, color = b.primaryInk)
                    }
                }
            }
            if (surahHits.isEmpty() && ayahHits.isEmpty() && topicHits.isEmpty()) item { EmptyState("لا نتائج", "جرّب كلمة أخرى من الآية، دون تشكيل.") }
            return@LazyColumn
        }
        if (showJuz) {
            item {
                Column(Modifier.fillMaxWidth().surface(b)) {
                    content.juzStarts.forEachIndexed { i, j ->
                        Row(
                            Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reader(j.surah, j.ayah)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NumberBadge(j.juz)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("الجزء ${j.juz}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                                Text("يبدأ من سورة ${content.surah(j.surah).shortName} آية ${j.ayah} · صفحة ${j.page}", style = MaterialTheme.typography.bodySmall, color = b.muted)
                            }
                            Chevron()
                        }
                        if (i < content.juzStarts.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(0.5.dp).background(b.line))
                    }
                }
                Spacer(Modifier.height(12.dp))
                BrandButton("عودة إلى السور", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) { showJuz = false }
            }
            return@LazyColumn
        }
        item {
            HeroHeader("القرآن الكريم", eyebrow = "المصحف الشريف", subtitle = "٦٠٤ صفحة بترقيم مصحف المدينة والرسم العثماني، مع التلاوة والتفسير") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    BrandButton(
                        if (last != null) "تابع: سورة ${content.surah(last[0]).shortName}" else "ابدأ القراءة",
                        icon = Icons.AutoMirrored.Outlined.MenuBook, kind = BtnKind.Gold,
                    ) { if (last != null) nav.navigate(Routes.reader(last[0], last[1])) else nav.navigate(Routes.reader(1)) }
                    BrandButton("الأجزاء", kind = BtnKind.Ghost) { showJuz = true }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        item {
            SectionHeader("أدوات القرآن", subtitle = "اقرأ واستمع واحفظ وتدبر")
            Spacer(Modifier.height(12.dp))
            val tools = listOf(
                Triple("الأجزاء", Icons.Outlined.FormatListNumbered) { showJuz = true },
                Triple("آيات بالموضوع", Icons.Outlined.Eco) { nav.navigate("topics") },
                Triple("التجويد الملون", Icons.Outlined.Palette) { nav.navigate("tajweed") },
                Triple("معاني الكلمات", Icons.Outlined.Translate) { nav.navigate("word-by-word") },
                Triple("اختبار الحفظ", Icons.Outlined.VisibilityOff) { nav.navigate("hifz-test") },
                Triple("خطة الختمة", Icons.Outlined.CalendarMonth) { nav.navigate("khatma") },
                Triple("الإذاعات", Icons.Outlined.Radio) { nav.navigate("radios") },
                Triple("الرقية الشرعية", Icons.Outlined.Shield) { nav.navigate("ruqya") },
                Triple("الأسماء الحسنى", Icons.Outlined.AutoAwesome) { nav.navigate("asma") },
                Triple("المصحف بالصفحات", Icons.Outlined.AutoStories) { nav.navigate("page/" + (lastPage ?: 1)) },
                Triple("آياتي المحفوظة", Icons.Outlined.StarOutline) { nav.navigate("saved") },
                Triple("آية اليوم", Icons.Outlined.WbSunny) { nav.navigate("ayah-today") },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tools.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (t, icon, go) ->
                            Column(
                                Modifier.weight(1f).heightIn(min = 112.dp).pressable(go).surface(b).padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                            ) {
                                IconTile(icon, size = 46.dp)
                                Text(t, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        item {
            SectionHeader("السور", subtitle = "${content.surahs.size} سورة · ابحث بالاسم أو بكلمة من الآية")
            Spacer(Modifier.height(12.dp))
            SurahBlock(content.surahs, nav)
        }
    }
    @Suppress("UNUSED_EXPRESSION") ctx
}

@Composable
private fun SurahBlock(list: List<com.ayattla.app.data.Surah>, nav: NavHostController) {
    val b = LocalBrand.current
    Column(Modifier.fillMaxWidth().surface(b)) {
        list.forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reader(s.id)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberBadge(s.id)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("سورة ${s.shortName}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                    Text("${s.revelationAr} · ${s.ayahCount} آية", style = MaterialTheme.typography.bodySmall, color = b.muted)
                }
                Text(s.nameAr, fontFamily = Fonts.quran, fontSize = 20.sp, color = b.primaryInk)
            }
            if (i < list.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(0.5.dp).background(b.line))
        }
    }
}

@Composable
fun NumberBadge(n: Int) {
    val b = LocalBrand.current
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(b.tint), contentAlignment = Alignment.Center) {
        Text("$n", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(nav: NavHostController, surahId: Int, startAyah: Int) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val surah = content.surah(surahId)
    val ayat = remember(surahId) { content.ayat(surahId) }
    val reciter = rememberReciter()
    val list = rememberLazyListState()
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var showFont by remember { mutableStateOf(false) }
    val pages = remember(surahId) { ayat.groupBy { it.page ?: 0 }.values.toList() }
    val header = 1
    fun pageIndexOf(ayah: Int) = pages.indexOfFirst { p -> p.any { it.ayah == ayah } }.coerceAtLeast(0)

    LaunchedEffect(surahId, startAyah) {
        if (startAyah > 1) list.scrollToItem(pageIndexOf(startAyah) + header)
    }
    // Follow the recitation.
    LaunchedEffect(Recitation.current) {
        val cur = Recitation.current?.split(":")?.mapNotNull { it.toIntOrNull() } ?: return@LaunchedEffect
        if (cur.size == 2 && cur[0] == surahId && cur[1] > 0) list.animateScrollToItem(pageIndexOf(cur[1]) + header)
    }
    // Remember the reading position.
    val visiblePage = pages.getOrNull((list.firstVisibleItemIndex - header).coerceIn(0, pages.size - 1)) ?: ayat
    LaunchedEffect(list.firstVisibleItemIndex) { prefs.setLastRead(surahId, visiblePage.first().ayah) }
    val paper = if (b.dark) b.background else Color(0xFFFCF9F1)
    val highlight = if (b.dark) Color(0xFF3A3317) else Color(0xFFF5E6B0)

    Column(Modifier.fillMaxSize().background(paper).statusBarsPadding()) {
        AppTopBar(
            "سورة ${surah.shortName}", onBack = { nav.popBackStack() },
            subtitle = "الجزء ${visiblePage.first().juz ?: 1} · صفحة ${visiblePage.first().page ?: 1}",
        ) {
            IconButton(onClick = { showFont = true }) { Icon(Icons.Filled.FormatSize, "حجم الخط", tint = b.primaryInk) }
            IconButton(onClick = { Recitation.play(ctx, content, reciter, surahId, 1) }) { Icon(Icons.Filled.PlayArrow, "استمع للسورة", tint = b.primaryInk) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("المصحف", true, {})
            Chip("عرض الصفحات", false, { nav.navigate("page/" + (visiblePage.first().page ?: 1)) })
            Chip("استمع", false, { Recitation.play(ctx, content, reciter, surahId, visiblePage.first().ayah) })
        }
        LazyColumn(state = list, contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, if (Recitation.active) 230.dp else 32.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(1.dp).background(b.green.copy(alpha = 0.25f)))
                        Text("سورة ${surah.shortName}", fontFamily = Fonts.naskh, fontSize = 24.sp, color = b.primaryInk, modifier = Modifier.padding(horizontal = 14.dp))
                        Box(Modifier.weight(1f).height(1.dp).background(b.green.copy(alpha = 0.25f)))
                    }
                    if (surah.hasBasmala) {
                        // The basmala text as printed in Al-Fatiha 1:1 of the pack.
                        val basmala = remember { content.ayah(1, 1)?.text.orEmpty() }
                        Text(
                            basmala, fontFamily = Fonts.quran, fontSize = (prefs.quranFontSize * 0.9f).sp, color = b.ink,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        )
                    }
                }
            }
            items(pages, key = { it.first().key }) { pageAyat ->
                val playingKey = Recitation.current
                val text = buildAnnotatedString {
                    pageAyat.forEach { a ->
                        val fav = a.key in prefs.favorites
                        withLink(LinkAnnotation.Clickable("ayah-${a.key}", TextLinkStyles(SpanStyle(textDecoration = TextDecoration.None))) { selected = a }) {
                            withStyle(SpanStyle(background = if (a.key == playingKey) highlight else Color.Transparent, color = b.ink)) {
                                append(a.text)
                                append(" ")
                                withStyle(SpanStyle(color = if (fav) b.goldInk else b.ink)) { append(Num.ayahMark(a.ayah)) }
                            }
                        }
                        append(" ")
                    }
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text, fontFamily = Fonts.quran, fontSize = prefs.quranFontSize.sp, lineHeight = (prefs.quranFontSize * 2.1f).sp,
                        textAlign = TextAlign.Justify, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(0.5.dp).background(b.line))
                        Text("${pageAyat.first().page ?: ""}", style = MaterialTheme.typography.labelMedium, color = b.muted, modifier = Modifier.padding(horizontal = 10.dp))
                        Box(Modifier.weight(1f).height(0.5.dp).background(b.line))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (surahId > 1) BrandButton("السورة السابقة", kind = BtnKind.Secondary) { nav.navigate(Routes.reader(surahId - 1)) { popUpTo("reader/{surah}/{ayah}") { inclusive = true } } }
                    else Spacer(Modifier.width(1.dp))
                    if (surahId < 114) BrandButton("السورة التالية", kind = BtnKind.Secondary) { nav.navigate(Routes.reader(surahId + 1)) { popUpTo("reader/{surah}/{ayah}") { inclusive = true } } }
                }
            }
        }
    }

    selected?.let { a ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = b.surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("سورة ${surah.shortName} · آية ${a.ayah}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Text(a.text, fontFamily = Fonts.quran, fontSize = 20.sp, lineHeight = 38.sp, color = b.ink, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
                val share = a.text + "\n\n[سورة ${surah.shortName} ${a.ayah}]\nayattla.com"
                SheetAction(Icons.Filled.PlayArrow, "استمع من هنا") { Recitation.play(ctx, content, reciter, surahId, a.ayah); selected = null }
                SheetAction(Icons.Filled.Repeat, "استمع لهذه الآية فقط") { Recitation.play(ctx, content, reciter, surahId, a.ayah, a.ayah); selected = null }
                SheetAction(if (a.key in prefs.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (a.key in prefs.favorites) "إزالة من المحفوظات" else "حفظ الآية") {
                    prefs.toggleFavorite(a.key); selected = null
                }
                SheetAction(Icons.Filled.AutoAwesome, "اصنع بطاقة") { selected = null; nav.navigate(Routes.studio(surahId, a.ayah, a.ayah)) }
                SheetAction(Icons.Filled.ContentCopy, "نسخ") { copyText(ctx, share); selected = null }
                SheetAction(Icons.Filled.Share, "مشاركة") { shareText(ctx, share); selected = null }
            }
        }
    }

    if (showFont) {
        ModalBottomSheet(onDismissRequest = { showFont = false }, containerColor = b.surface) {
            Column(Modifier.padding(20.dp)) {
                Text("حجم خط المصحف: ${prefs.quranFontSize.toInt()}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Slider(value = prefs.quranFontSize, onValueChange = { prefs.setQuranFont(it) }, valueRange = 18f..44f, steps = 12)
                Text(ayat.first().text, fontFamily = Fonts.quran, fontSize = prefs.quranFontSize.sp, lineHeight = (prefs.quranFontSize * 1.9f).sp, color = b.ink, maxLines = 2)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().clip(SmallShape).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = b.goldInk)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = b.ink)
    }
}

/** Player card (iOS PlayerBar): reciter tile, ayah and reciter, position, scrubber, speed, previous, play, next, repeat. */
@Composable
fun PlayerBar() {
    val b = LocalBrand.current
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    LaunchedEffect(Recitation.current, Recitation.isPlaying) {
        while (true) { pos = Recitation.positionMs; dur = Recitation.durationMs; kotlinx.coroutines.delay(400) }
    }
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
            .softShadow(shape, 24.dp, Color(0xFF0F1F17).copy(alpha = 0.16f), DpOffset(0.dp, 10.dp))
            .clip(shape).background(b.surface).border(1.dp, b.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.GraphicEq, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(Recitation.title.ifEmpty { "جاري التحميل" }, style = MaterialTheme.typography.titleMedium, color = b.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = Recitation.error ?: Recitation.artist
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = if (Recitation.error != null) b.newInk else b.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (Recitation.count > 1) Badge("${Recitation.index + 1} من ${Recitation.count}")
            Icon(Icons.Filled.Close, "إيقاف", tint = b.muted, modifier = Modifier.size(22.dp).clickable { Recitation.stop() })
        }
        if (dur > 0) {
            Slider(
                value = (pos.toFloat() / dur).coerceIn(0f, 1f), onValueChange = { Recitation.seekTo((it * dur).toLong()); pos = (it * dur).toLong() },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = b.green, inactiveTrackColor = b.line),
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(mmss(pos), style = MaterialTheme.typography.labelSmall, color = b.muted)
                Spacer(Modifier.weight(1f))
                Text("-" + mmss((dur - pos).coerceAtLeast(0)), style = MaterialTheme.typography.labelSmall, color = b.muted)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PlayerPill("${Recitation.speed}x".replace(".0x", "x"), Icons.Filled.Speed) { Recitation.cycleSpeed() }
            Spacer(Modifier.weight(1f))
            // RTL: «previous» sits on the right.
            IconButton(onClick = { Recitation.previous() }) { Icon(Icons.Filled.SkipNext, "السابقة", tint = b.green, modifier = Modifier.size(30.dp)) }
            Box(
                Modifier.size(62.dp).softShadow(CircleShape, 12.dp, Color(0xFF1F8A62).copy(alpha = 0.35f), DpOffset(0.dp, 6.dp))
                    .clip(CircleShape).background(Brush.linearGradient(b.siteGradient)).clickable { Recitation.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (Recitation.isBuffering) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(if (Recitation.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { Recitation.next() }) { Icon(Icons.Filled.SkipPrevious, "التالية", tint = b.green, modifier = Modifier.size(30.dp)) }
            Spacer(Modifier.weight(1f))
            PlayerPill(if (Recitation.repeatOne) "تكرار" else "مرة", if (Recitation.repeatOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat, on = Recitation.repeatOne) { Recitation.toggleRepeat() }
        }
    }
}

@Composable
private fun PlayerPill(text: String, icon: ImageVector, on: Boolean = true, onClick: () -> Unit) {
    val b = LocalBrand.current
    Row(
        Modifier.height(44.dp).clip(CircleShape).then(if (on) Modifier.background(Brush.linearGradient(b.siteGradient)) else Modifier.background(b.tint))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = if (on) Color.White else b.primaryInk, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else b.primaryInk)
    }
}

private fun mmss(ms: Long): String { val t = ms / 1000; return "%d:%02d".format(t / 60, t % 60) }
