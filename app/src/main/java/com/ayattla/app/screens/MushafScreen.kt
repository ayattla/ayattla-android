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
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("صفحة $current · الجزء ${first?.juz ?: ""}", style = MaterialTheme.typography.labelLarge, color = b.muted)
        }
    }
}
