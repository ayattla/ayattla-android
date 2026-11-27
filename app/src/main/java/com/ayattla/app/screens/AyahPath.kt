package com.ayattla.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.Net
import com.ayattla.app.data.SiteApi
import com.ayattla.app.data.Stats
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand

/** «مسار الآية»: one ayah, five steps in order — listen, understand, memorise, recite, share. */
enum class PathStep(val key: String, val title: String, val line: String) {
    Listen("listen", "استمع", "استمع للآية بصوت قارئك"),
    Meaning("meaning", "افهم المعنى", "التفسير الميسر لهذه الآية"),
    Memorize("memorize", "احفظ", "كرر الآية خمس مرات حتى تثبت"),
    Recite("recite", "التسميع", "اقرأها من حفظك ويصحح لك التطبيق"),
    Share("share", "شارك", "اصنع بطاقة بالآية ومصدرها"),
}

object AyahPath {
    private fun sp(c: Context) = c.getSharedPreferences("path", Context.MODE_PRIVATE)
    fun done(c: Context, s: Int, a: Int, step: PathStep) = sp(c).getBoolean("$s:$a.${step.key}", false)
    fun mark(c: Context, s: Int, a: Int, step: PathStep) {
        sp(c).edit().putBoolean("$s:$a.${step.key}", true).apply()
        Stats.event(c, "path_step", step.key)
    }
}

@Composable
fun AyahPathScreen(nav: NavHostController, surah: Int, ayah: Int) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val reciter = rememberReciter()
    val s = content.surah(surah)
    val text = remember(surah, ayah) { runCatching { content.ayat(surah, ayah, ayah).firstOrNull()?.text }.getOrNull() ?: "" }
    var tick by remember { mutableIntStateOf(0) }
    var tafsir by remember { mutableStateOf<String?>(null) }
    var showMeaning by remember { mutableStateOf(false) }
    var meaningFailed by remember { mutableStateOf(false) }
    LaunchedEffect(showMeaning) {
        if (!showMeaning || tafsir != null) return@LaunchedEffect
        meaningFailed = false
        runCatching { SiteApi.words(ctx, surah, "en").firstOrNull { it.n == ayah || it.key == "$surah:$ayah" }?.tafsir }
            .onSuccess { tafsir = it; if (it == null) meaningFailed = true }
            .onFailure { meaningFailed = true }
    }
    fun mark(step: PathStep) { AyahPath.mark(ctx, surah, ayah, step); tick++ }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("مسار الآية", onBack = { nav.popBackStack() }, subtitle = "سورة ${s.shortName} · الآية $ayah")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text, fontFamily = Fonts.quran, fontSize = 22.sp, lineHeight = 44.sp, color = b.ink,
                modifier = Modifier.fillMaxWidth().surface(b).padding(18.dp),
            )
            val doneCount = remember(tick) { PathStep.entries.count { AyahPath.done(ctx, surah, ayah, it) } }
            Text("أتممت $doneCount من ${PathStep.entries.size} خطوات", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)

            PathStep.entries.forEachIndexed { i, step ->
                val done = remember(tick) { AyahPath.done(ctx, surah, ayah, step) }
                Column(Modifier.fillMaxWidth().surface(b).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(if (done) b.green else b.tint),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Icon(Icons.Filled.Check, "تمت", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                            else Text("${i + 1}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = b.primaryInk)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(step.title, style = MaterialTheme.typography.titleMedium, color = b.ink)
                            Text(step.line, style = MaterialTheme.typography.bodyMedium, color = b.muted)
                        }
                    }
                    if (step == PathStep.Meaning && showMeaning) {
                        when {
                            tafsir != null -> Column(Modifier.fillMaxWidth().background(b.surface2, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(12.dp)) {
                                Text(tafsir!!, style = MaterialTheme.typography.bodyLarge, color = b.ink)
                                Text("التفسير الميسر · مجمع الملك فهد", style = MaterialTheme.typography.labelMedium, color = b.goldInk, modifier = Modifier.padding(top = 6.dp))
                            }
                            meaningFailed -> Text(if (Net.isOnline) "تعذر جلب التفسير، حاول لاحقا" else "التفسير يحتاج اتصالا بالإنترنت أول مرة", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                            else -> Text("جار جلب التفسير…", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                        }
                    }
                    val label = when (step) {
                        PathStep.Listen -> "استمع"
                        PathStep.Meaning -> if (showMeaning) "فهمت" else "اعرض المعنى"
                        PathStep.Memorize -> "كرر خمس مرات"
                        PathStep.Recite -> "ابدأ التسميع"
                        PathStep.Share -> "اصنع بطاقة"
                    }
                    BrandButton(label, Modifier.fillMaxWidth(), kind = if (done) BtnKind.Secondary else BtnKind.Primary) {
                        when (step) {
                            PathStep.Listen -> { Recitation.play(ctx, content, reciter, surah, ayah, ayah); mark(step) }
                            PathStep.Meaning -> if (!showMeaning) showMeaning = true else mark(step)
                            PathStep.Memorize -> { Recitation.play(ctx, content, reciter, surah, ayah, ayah, repeatEach = 5); mark(step) }
                            PathStep.Recite -> { mark(step); nav.navigate(Routes.tasmee(surah, ayah, ayah)) }
                            PathStep.Share -> { mark(step); nav.navigate(Routes.studio(surah, ayah, ayah)) }
                        }
                    }
                }
            }
            if (ayah < s.ayahCount) {
                BrandButton("الآية التالية", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) {
                    nav.navigate(Routes.path(surah, ayah + 1)) { popUpTo("path/{s}/{a}") { inclusive = true } }
                }
            }
        }
    }
}
