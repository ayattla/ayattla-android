package com.ayattla.app.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.audio.Listener
import com.ayattla.app.data.Journey
import com.ayattla.app.data.Recite
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num

/** Short surahs offered first (the trial started with Juz Amma). */
private val quick = listOf(1, 112, 113, 114, 108, 103, 97, 93)
private const val MAX_AYAT = 30

/**
 * «التسميع الذكي»: the ayat are hidden and appear word by word as the user recites them from memory.
 * The device's own speech recognition listens; words skipped or heard differently are marked
 * «راجع هذه الكلمة» (never «خطأ»), because recognition is approximate. The text shown is always the
 * mushaf's own text from the pack.
 */
@Composable
fun TasmeeScreen(nav: NavHostController, startSurah: Int?, startFrom: Int?, startTo: Int?) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    var surah by remember { mutableIntStateOf(startSurah?.coerceIn(1, 114) ?: 112) }
    val count = content.surah(surah).ayahCount
    var from by remember { mutableIntStateOf(startFrom ?: 1) }
    var to by remember { mutableIntStateOf(startTo ?: minOf(count, 10)) }
    from = from.coerceIn(1, count); to = to.coerceIn(from, minOf(count, from + MAX_AYAT - 1))
    var hidden by remember { mutableStateOf(true) }

    val listener = remember { Listener(ctx) }
    DisposableEffect(Unit) { onDispose { listener.release() } }
    val ayat = remember(surah, from, to) { content.ayat(surah, from, to) }
    // Recited words of the passage: (ayah index, token index) and the comparison key.
    val words = remember(ayat) {
        ayat.flatMapIndexed { ai, a -> Recite.tokens(a.text).let { t -> Recite.wordIndexes(t).map { ti -> Triple(ai, ti, Recite.key(t[ti])) } } }
    }
    val stream = remember(ayat) { mutableStateListOf<String>() }
    var consumed by remember(ayat) { mutableIntStateOf(0) }
    LaunchedEffect(ayat) { listener.stop(); listener.clear(); consumed = 0 }
    LaunchedEffect(listener.finals.size) {
        while (consumed < listener.finals.size) { stream += Recite.heardKeys(listener.finals[consumed]); consumed++ }
    }
    val heard = stream + Recite.heardKeys(listener.partial)
    val progress = remember(words, heard) { Recite.align(words.map { it.third }, heard) }
    var counted by remember(ayat) { mutableStateOf(false) }
    LaunchedEffect(progress.done) {
        if (progress.done && words.isNotEmpty()) {
            listener.stop()
            if (!counted) { counted = true; Journey.load(ctx); Journey.hifzDone() }
        }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) listener.start() }
    fun toggle() {
        if (listener.listening) { listener.stop(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listener.start()
        else mic.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun restart() { listener.stop(); listener.clear(); stream.clear(); consumed = 0; counted = false }

    ToolPage(nav, "التسميع الذكي", "tasmee") {
        AppCard {
            SurahPicker(surah) { surah = it; from = 1; to = minOf(content.surah(it).ayahCount, 10) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { s -> Chip(content.surah(s).shortName, s == surah, { surah = s; from = 1; to = minOf(content.surah(s).ayahCount, 10) }) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper("من آية", from, 1, count, Modifier.weight(1f)) { from = it; if (to < it) to = it }
                Stepper("إلى آية", to, from, minOf(count, from + MAX_AYAT - 1), Modifier.weight(1f)) { to = it }
            }
        }

        // The passage: hidden words keep their place as light bars until recited.
        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("سورة ${content.surah(surah).shortName} · ${(from)}–${(to)}", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
                Spacer(Modifier.weight(1f))
                Chip(if (hidden) "اعرض الآيات" else "أخفِ الآيات", false, { hidden = !hidden })
            }
            val text = buildAnnotatedString {
                var word = -1
                ayat.forEachIndexed { ai, a ->
                    Recite.tokens(a.text).forEach { tok ->
                        // A stop mark follows the word before it.
                        if (Recite.key(tok).isNotEmpty()) word++
                        val mark = progress.marks.getOrNull(word)
                        val shown = !hidden || word < progress.revealed
                        val style = when {
                            !shown -> SpanStyle(color = Color.Transparent, background = b.tint)
                            mark == Recite.Mark.Review -> SpanStyle(color = Color(0xFF9A5B00), background = Color(0x33E9A23B))
                            word < progress.revealed -> SpanStyle(color = b.ink)
                            else -> SpanStyle(color = b.muted)
                        }
                        withStyle(style) { append(tok) }
                        append(" ")
                    }
                    val end = words.indexOfLast { it.first == ai }
                    withStyle(if (!hidden || end < progress.revealed) SpanStyle(color = b.goldInk) else SpanStyle(color = Color.Transparent)) {
                        append(Num.ayahMark(a.ayah))
                    }
                    append(" ")
                }
            }
            Text(text, fontFamily = Fonts.quran, fontSize = 24.sp, lineHeight = 48.sp, textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "نص التسميع" })
            Text("${(progress.revealed)} من ${(words.size)} كلمة", style = MaterialTheme.typography.bodySmall, color = b.muted,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
        }

        if (progress.done && words.isNotEmpty()) {
            AppCard {
                Text(if (progress.review.isEmpty()) "أتممت التسميع، بارك الله فيك" else "أتممت التسميع",
                    style = MaterialTheme.typography.titleMedium, color = b.primaryInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (progress.review.isNotEmpty()) {
                    Text("راجع هذه الكلمات في المصحف:", style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.padding(top = 8.dp))
                    progress.review.forEach { i ->
                        val (ai, ti) = words[i].first to words[i].second
                        val a = ayat[ai]
                        Text("${Recite.tokens(a.text)[ti]}  ·  آية ${(a.ayah)}", fontFamily = Fonts.quran, fontSize = 20.sp, color = b.ink,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("سمّع مرة أخرى", Modifier.weight(1f)) { restart() }
                    BrandButton("افتح في المصحف", Modifier.weight(1f), kind = BtnKind.Secondary) { nav.navigate(Routes.reader(surah, from)) }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(if (listener.listening) Color(0xFFB4432F) else b.green)
                        .clickable { toggle() }.semantics { contentDescription = if (listener.listening) "إيقاف الاستماع" else "ابدأ التسميع" },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (listener.listening) Icons.Filled.Stop else Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                Text(
                    when {
                        listener.problem != null -> listener.problem!!
                        listener.listening -> "أستمع إليك… اقرأ من حفظك"
                        progress.revealed > 0 -> "متوقف، اضغط للمتابعة"
                        else -> "اضغط وابدأ القراءة من أول المقطع"
                    },
                    style = MaterialTheme.typography.bodyLarge, color = if (listener.problem != null) Color(0xFFB4432F) else b.ink, textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("أظهر الكلمة التالية", kind = BtnKind.Secondary, enabled = progress.revealed < words.size) {
                        stream += Recite.hint(progress.revealed)
                    }
                    BrandButton("من البداية", kind = BtnKind.Ghost) { restart() }
                }
            }
        }

        SectionHeader("قبل أن تبدأ")
        AppCard {
            Text(
                "يستمع التطبيق بخدمة التعرف على الكلام في جهازك، ولا يسجل صوتك ولا يحفظه. التعرف تقريبي، " +
                    "فالكلمة المظللة تعني «راجعها في المصحف» وليست حكما بالخطأ.",
                style = MaterialTheme.typography.bodyMedium, color = b.ink,
            )
            if (listener.listening || listener.finals.isNotEmpty()) Text(
                if (listener.onDevice) "يعمل الآن على جهازك دون إنترنت" else "يستخدم خدمة التعرف على الكلام في جهازك",
                style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val b = LocalBrand.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = b.muted)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, b.line, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepKey("+", value < max) { onChange(value + 1) }
            Text((value), style = MaterialTheme.typography.titleMedium, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            StepKey("−", value > min) { onChange(value - 1) }
        }
    }
}

@Composable
private fun StepKey(sign: String, enabled: Boolean, onClick: () -> Unit) {
    val b = LocalBrand.current
    Box(Modifier.size(44.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(sign, style = MaterialTheme.typography.titleLarge, color = if (enabled) b.primaryInk else b.line)
    }
    Spacer(Modifier.width(0.dp))
}
