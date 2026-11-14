package com.ayattla.app.screens

import androidx.compose.foundation.background
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

@Composable
fun QuranIndexScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { if (query.isBlank()) emptyList() else content.search(query) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "القرآن الكريم", style = MaterialTheme.typography.headlineSmall, color = b.ink,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp),
            placeholder = { Text("ابحث في الآيات") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "مسح") } },
            singleLine = true, shape = SmallShape,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = b.surface, unfocusedContainerColor = b.surface, focusedBorderColor = b.primaryInk, unfocusedBorderColor = b.line),
        )
        if (query.isNotBlank()) {
            if (results.isEmpty()) EmptyState("لا نتائج", "جرّب كلمة أخرى من الآية، دون تشكيل.")
            else {
                Text("${results.size} نتيجة", style = MaterialTheme.typography.labelMedium, color = b.muted, modifier = Modifier.padding(horizontal = 16.dp))
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results, key = { it.key }) { a ->
                        Column(
                            Modifier.fillMaxWidth().clip(SmallShape).background(b.surface).clickable { nav.navigate(Routes.reader(a.surah, a.ayah)) }.padding(14.dp)
                        ) {
                            Text(a.text, fontFamily = Fonts.quran, fontSize = 20.sp, lineHeight = 38.sp, color = b.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text("سورة ${content.surah(a.surah).shortName} · ${a.ayah}", style = MaterialTheme.typography.labelMedium, color = b.goldInk)
                        }
                    }
                }
            }
            return@Column
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("السور", tab == 0, { tab = 0 })
            Chip("الأجزاء", tab == 1, { tab = 1 })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp)) {
                items(content.surahs, key = { it.id }) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reader(s.id)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NumberBadge(s.id)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("سورة ${s.shortName}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                            Text("${s.revelationAr} · ${s.ayahCount} آية", style = MaterialTheme.typography.bodySmall, color = b.muted)
                        }
                        Text(s.nameAr, fontFamily = Fonts.quran, fontSize = 20.sp, color = b.primaryInk)
                    }
                    HorizontalDivider(color = b.line)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp)) {
                items(content.juzStarts, key = { it.juz }) { j ->
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reader(j.surah, j.ayah)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NumberBadge(j.juz)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("الجزء ${j.juz}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                            Text("يبدأ من سورة ${content.surah(j.surah).shortName} آية ${j.ayah} · صفحة ${j.page}", style = MaterialTheme.typography.bodySmall, color = b.muted)
                        }
                    }
                    HorizontalDivider(color = b.line)
                }
            }
        }
    }
}

@Composable
fun NumberBadge(n: Int) {
    val b = LocalBrand.current
    Box(Modifier.size(38.dp).clip(CircleShape).background(b.goldSoft), contentAlignment = Alignment.Center) {
        Text("$n", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
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
    val header = if (surah.hasBasmala) 2 else 1

    LaunchedEffect(surahId, startAyah) {
        if (startAyah > 1) list.scrollToItem((startAyah - 1 + header).coerceAtMost(ayat.size + header - 1))
    }
    // Follow the recitation.
    LaunchedEffect(Recitation.current) {
        val cur = Recitation.current?.split(":")?.map(String::toInt) ?: return@LaunchedEffect
        if (cur[0] == surahId && cur[1] > 0) list.animateScrollToItem(cur[1] - 1 + header)
    }
    // Remember the reading position.
    LaunchedEffect(list.firstVisibleItemIndex) {
        val i = (list.firstVisibleItemIndex - header).coerceIn(0, ayat.size - 1)
        prefs.setLastRead(surahId, ayat[i].ayah)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("سورة ${surah.shortName}", onBack = { nav.popBackStack() }) {
            IconButton(onClick = { showFont = true }) { Icon(Icons.Filled.FormatSize, "حجم الخط") }
            IconButton(onClick = { Recitation.play(ctx, content, reciter, surahId, 1) }) { Icon(Icons.Filled.PlayArrow, "استمع للسورة") }
        }
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(surah.nameAr, fontFamily = Fonts.quran, fontSize = 30.sp, color = b.primaryInk)
                    Text("${surah.revelationAr} · ${surah.ayahCount} آية", style = MaterialTheme.typography.bodySmall, color = b.muted)
                }
            }
            if (surah.hasBasmala) item {
                // The basmala text as printed in Al-Fatiha 1:1 of the pack.
                val basmala = remember { content.ayah(1, 1)?.text.orEmpty() }
                Text(
                    basmala, fontFamily = Fonts.quran, fontSize = (prefs.quranFontSize * 0.9f).sp, color = b.ink,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }
            items(ayat, key = { it.key }) { a ->
                val playing = Recitation.current == a.key
                val fav = a.key in prefs.favorites
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(SmallShape)
                        .background(if (playing) b.tint else b.background)
                        .clickable { selected = a }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text(
                        a.text + " " + Num.ayahMark(a.ayah),
                        fontFamily = Fonts.quran, fontSize = prefs.quranFontSize.sp, lineHeight = (prefs.quranFontSize * 1.9f).sp,
                        color = b.ink, textAlign = TextAlign.Justify, modifier = Modifier.fillMaxWidth(),
                    )
                    if (fav) Icon(Icons.Filled.Favorite, "محفوظة", tint = b.accent, modifier = Modifier.size(14.dp))
                }
                HorizontalDivider(color = b.line.copy(alpha = 0.6f))
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (surahId > 1) TextButton(onClick = { nav.navigate(Routes.reader(surahId - 1)) { popUpTo("reader/{surah}/{ayah}") { inclusive = true } } }) {
                        Text("السورة السابقة")
                    } else Spacer(Modifier.width(1.dp))
                    if (surahId < 114) TextButton(onClick = { nav.navigate(Routes.reader(surahId + 1)) { popUpTo("reader/{surah}/{ayah}") { inclusive = true } } }) {
                        Text("السورة التالية")
                    }
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

/** Mini player shown above the tab bar while a recitation or radio is active. */
@Composable
fun PlayerBar() {
    val b = LocalBrand.current
    Column(Modifier.fillMaxWidth().background(b.primary)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(Recitation.title.ifEmpty { "جاري التحميل" }, style = MaterialTheme.typography.titleSmall, color = b.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Recitation.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = b.accent, maxLines = 1) }
            }
            TextButton(onClick = { Recitation.cycleSpeed() }) {
                Text("${Recitation.speed}x".replace(".0x", "x"), color = b.accent)
            }
            IconButton(onClick = { Recitation.toggleRepeat() }) {
                Icon(if (Recitation.repeatOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "تكرار", tint = if (Recitation.repeatOne) b.accent else b.onPrimary.copy(alpha = 0.7f))
            }
            IconButton(onClick = { Recitation.previous() }) { Icon(Icons.Filled.SkipNext, "السابقة", tint = b.onPrimary) }
            Box(Modifier.size(44.dp).clip(CircleShape).background(b.accent).clickable { Recitation.toggle() }, contentAlignment = Alignment.Center) {
                if (Recitation.isBuffering) CircularProgressIndicator(Modifier.size(20.dp), color = b.deep, strokeWidth = 2.dp)
                else Icon(if (Recitation.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل", tint = b.deep)
            }
            IconButton(onClick = { Recitation.next() }) { Icon(Icons.Filled.SkipPrevious, "التالية", tint = b.onPrimary) }
            IconButton(onClick = { Recitation.stop() }) { Icon(Icons.Filled.Close, "إيقاف", tint = b.onPrimary.copy(alpha = 0.7f)) }
        }
    }
}
