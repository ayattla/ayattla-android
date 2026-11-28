package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.audio.Recitation
import com.ayattla.app.calc.KhatmaPlan
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num

/** The Madani mushaf page by page (604 pages); swipe like a printed mushaf. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MushafScreen(nav: NavHostController, startPage: Int) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val reciter = rememberReciter()
    val pager = rememberPagerState(initialPage = (startPage - 1).coerceIn(0, KhatmaPlan.PAGES - 1)) { KhatmaPlan.PAGES }
    val current = pager.currentPage + 1
    val first = remember(current) { content.page(current).firstOrNull() }

    LaunchedEffect(current) {
        first?.let { prefs.setLastRead(it.surah, it.ayah) }
        // A page that stays on screen two seconds counts as read for «رحلتي مع القرآن».
        kotlinx.coroutines.delay(2000); com.ayattla.app.data.Journey.load(ctx); com.ayattla.app.data.Journey.readPage(current)
    }

    // Reading the mushaf keeps the screen awake, like the leading mushaf apps.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    Column(Modifier.fillMaxSize().background(MushafColors.backdrop()).statusBarsPadding()) {
        AppTopBar(first?.let { "سورة ${content.surah(it.surah).shortName}" } ?: "المصحف", onBack = { nav.popBackStack() }) {
            MushafThemeButton()
            IconButton(onClick = { first?.let { Recitation.play(ctx, content, reciter, it.surah, it.ayah, continuous = true) } }) { Icon(Icons.Filled.PlayArrow, "استمع من أول الصفحة") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1, key = { it }) { index ->
            val page = index + 1
            val ayat = remember(page) { content.page(page) }
            val head = ayat.firstOrNull()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                MushafFrame(head?.let { content.surah(it.surah).shortName } ?: "", head?.juz, page) {
                    // Surahs on the same page are separated by their own titles.
                    ayat.groupBy { it.surah }.forEach { (s, list) ->
                        val surah = content.surah(s)
                        if (list.first().ayah == 1) {
                            SurahBanner(surah)
                            if (surah.hasBasmala) Text(
                                content.ayah(1, 1)?.text.orEmpty(), fontFamily = Fonts.quran, fontSize = 22.sp, color = MushafColors.ink(),
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            )
                        }
                        val text = buildAnnotatedString {
                            list.forEachIndexed { i, a ->
                                val playing = Recitation.current == a.key
                                withStyle(SpanStyle(background = if (playing) b.tint else androidx.compose.ui.graphics.Color.Transparent)) { append(a.text) }
                                withStyle(SpanStyle(color = b.goldInk)) { append(" " + Num.ayahMark(a.ayah)) }
                                if (i < list.size - 1) append(" ")
                            }
                        }
                        Text(
                            text, fontFamily = Fonts.quran, fontSize = (prefs.quranFontSize - 2).sp, lineHeight = ((prefs.quranFontSize - 2) * 1.95f).sp,
                            color = MushafColors.ink(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        // Where you are in the juz, and a bookmark for this page — like the leading mushaf apps.
        val juz = first?.juz ?: MushafMarks.juzOf(current)
        val (from, to) = MushafMarks.juzPages(juz)
        var marks by remember { mutableStateOf(MushafMarks.load(ctx)) }
        var showMarks by remember { mutableStateOf(false) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { ((current - from + 1).toFloat() / (to - from + 1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = MushafColors.gold(), trackColor = MushafColors.gold().copy(alpha = 0.18f),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الجزء $juz · صفحة ${current - from + 1} من ${to - from + 1}", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.weight(1f))
                IconButton(onClick = { marks = MushafMarks.toggle(ctx, current) }) {
                    Icon(if (current in marks) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (current in marks) "إزالة علامة الصفحة" else "ضع علامة على الصفحة", tint = MushafColors.gold())
                }
                IconButton(onClick = { showMarks = true }) { Icon(Icons.AutoMirrored.Outlined.List, "علاماتي") }
            }
        }
        if (showMarks) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showMarks = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text("علاماتي", style = MaterialTheme.typography.titleLarge, color = b.ink)
                if (marks.isEmpty()) Text("ضع علامة على أي صفحة من زر العلامة أسفل المصحف، وترجع لها من هنا", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                marks.sorted().forEach { p ->
                    val a = remember(p) { content.page(p).firstOrNull() }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { showMarks = false; scope.launch { pager.animateScrollToPage(p - 1) } }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Bookmark, null, tint = MushafColors.gold())
                        Text("صفحة $p" + (a?.let { " · سورة ${content.surah(it.surah).shortName}" } ?: ""), style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                        Text("الجزء ${a?.juz ?: MushafMarks.juzOf(p)}", style = MaterialTheme.typography.labelMedium, color = b.muted)
                    }
                }
            }
        }
    }
}

/** Page bookmarks (kept on the device) and the juz boundaries of the Madinah mushaf (604 pages). */
object MushafMarks {
    private val starts = intArrayOf(1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282, 302, 322, 342, 362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582)
    fun juzOf(page: Int) = starts.indexOfLast { it <= page }.coerceAtLeast(0) + 1
    fun juzPages(juz: Int): Pair<Int, Int> { val j = juz.coerceIn(1, 30); return starts[j - 1] to (if (j == 30) 604 else starts[j] - 1) }
    private fun sp(c: android.content.Context) = c.getSharedPreferences("mushaf", android.content.Context.MODE_PRIVATE)
    fun load(c: android.content.Context): Set<Int> = sp(c).getString("bookmarks", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet()
    fun toggle(c: android.content.Context, page: Int): Set<Int> {
        val now = load(c).let { if (page in it) it - page else it + page }
        sp(c).edit().putString("bookmarks", now.joinToString(",")).apply()
        return now
    }
}
