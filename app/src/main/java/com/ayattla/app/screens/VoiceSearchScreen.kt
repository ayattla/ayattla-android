package com.ayattla.app.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.audio.Listener
import com.ayattla.app.data.VoiceSearch
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «البحث بالصوت»: the user recites a few words of an ayah and the app finds it in the mushaf kept on
 * the device (no server). Recognition is the device's own; the results are the pack's own ayat.
 */
@Composable
fun VoiceSearchScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val listener = remember { Listener(ctx) }
    DisposableEffect(Unit) { onDispose { listener.release() } }
    var heard by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<VoiceSearch.Hit>?>(null) }
    var busy by remember { mutableStateOf(false) }

    // One phrase at a time: the first complete result is searched and listening stops.
    LaunchedEffect(listener.finals.size) {
        val t = listener.finals.lastOrNull() ?: return@LaunchedEffect
        listener.stop()
        heard = t
        busy = true
        hits = withContext(Dispatchers.Default) { content.voiceSearch.find(t) }
        busy = false
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { listener.clear(); listener.start() } }
    fun toggle() {
        if (listener.listening) { listener.stop(); return }
        hits = null; heard = ""
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { listener.clear(); listener.start() }
        else mic.launch(Manifest.permission.RECORD_AUDIO)
    }

    ToolPage(nav, "البحث بالصوت", "voice-search") {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(if (listener.listening) Color(0xFFB4432F) else b.green)
                    .clickable { toggle() }.semantics { contentDescription = if (listener.listening) "إيقاف الاستماع" else "ابدأ البحث بالصوت" },
                contentAlignment = Alignment.Center,
            ) { Icon(if (listener.listening) Icons.Filled.Stop else Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(46.dp)) }
            Text(
                when {
                    listener.problem != null -> listener.problem!!
                    listener.listening -> listener.partial.ifBlank { "أستمع… اقرأ كلمات من الآية" }
                    heard.isNotBlank() -> "سمعت: $heard"
                    else -> "اضغط واقرأ بضع كلمات من الآية التي تبحث عنها"
                },
                style = MaterialTheme.typography.bodyLarge, color = if (listener.problem != null) Color(0xFFB4432F) else b.ink, textAlign = TextAlign.Center,
            )
            if (busy) CircularProgressIndicator(color = b.green)
        }

        val list = hits
        if (list != null) {
            if (list.isEmpty()) AppCard {
                Text("لم أجد آية بهذه الكلمات. جرّب أن تقرأ كلمات أكثر بوضوح، أو ابحث بالكتابة.", style = MaterialTheme.typography.bodyMedium, color = b.ink)
            } else {
                SectionHeader(if (list.size == 1) "وجدت آية واحدة" else "أقرب الآيات", subtitle = "اضغط على الآية لفتحها في المصحف")
                list.forEach { h ->
                    val a = h.ayah
                    AppCard(onClick = { nav.navigate(Routes.reader(a.surah, a.ayah)) }) {
                        Text("سورة ${content.surah(a.surah).shortName} · آية ${a.ayah}", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
                        Text(a.text + " " + Num.ayahMark(a.ayah), fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 40.sp, color = b.ink,
                            maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        AppCard {
            Text("يبحث في المصحف المحفوظ على جهازك، ويستمع بخدمة التعرف على الكلام في جهازك دون تسجيل صوتك.",
                style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}
