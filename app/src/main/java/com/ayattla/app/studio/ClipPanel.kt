package com.ayattla.app.studio

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.theme.LocalBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Video clip controls: reciter, make, then share, save or play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPanel(surah: Int, from: Int, to: Int, ratio: CardRatio, style: CardStyle, background: com.ayattla.app.data.SiteMedia? = null) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val reciter = content.reciters.firstOrNull { it.id == prefs.reciterId } ?: content.reciters.first()
    var picking by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<ClipEngine.Stage?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var result by remember(surah, from, to, ratio, style, reciter.id, background) { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AppCard {
        Row(Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("القارئ", style = MaterialTheme.typography.labelMedium, color = b.muted)
                Text(reciter.name, style = MaterialTheme.typography.bodyLarge, color = b.primaryInk)
            }
            Text("تغيير", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
        }
        Spacer(Modifier.size(8.dp))
        val s = stage
        if (s != null) {
            val (label, p) = when (s) {
                is ClipEngine.Stage.Downloading -> "تنزيل التلاوة ${s.done} من ${s.total}" to (s.done.toFloat() / s.total.coerceAtLeast(1)) * 0.3f
                ClipEngine.Stage.Background -> "تنزيل الخلفية" to 0.3f
                ClipEngine.Stage.Drawing -> "رسم الآيات" to 0.35f
                is ClipEngine.Stage.Encoding -> "تجهيز المقطع ${s.percent}٪" to 0.35f + s.percent / 100f * 0.65f
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink)
            LinearProgressIndicator(progress = { p }, color = b.accent, trackColor = b.line, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            TextButton(onClick = { job?.cancel(); stage = null }) { Text("إلغاء") }
        } else {
            BrandButton("اصنع المقطع", Modifier.fillMaxWidth(), icon = Icons.Filled.Movie) {
                error = null; result = null
                job = scope.launch {
                    runCatching {
                        ClipEngine(ctx, content).make(reciter, surah, from, to, ratio, style, background) { stage = it }
                    }.onSuccess { result = it }.onFailure { e ->
                        if (e !is kotlinx.coroutines.CancellationException) error = "تعذر تجهيز المقطع. تحقق من الإنترنت ثم أعد المحاولة."
                    }
                    stage = null
                }
            }
        }
        error?.let { Text(it, color = b.goldInk, style = MaterialTheme.typography.bodySmall) }
        result?.let { f ->
            Text("المقطع جاهز", style = MaterialTheme.typography.titleSmall, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandButton("مشاركة المقطع", Modifier.fillMaxWidth(), icon = Icons.Filled.Share) {
                    val send = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, ClipEngine.shareUri(ctx, f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(send, "مشاركة المقطع"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("حفظ", Modifier.weight(1f), icon = Icons.Filled.Download, kind = BtnKind.Secondary) {
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) { ClipEngine.saveToGallery(ctx, f) }
                            Toast.makeText(ctx, if (uri != null) "حُفظ المقطع في الفيديوهات" else "تعذر الحفظ", Toast.LENGTH_SHORT).show()
                        }
                    }
                    BrandButton("تشغيل", Modifier.weight(1f), icon = Icons.Filled.PlayCircle, kind = BtnKind.Secondary) {
                        val view = Intent(Intent.ACTION_VIEW).setDataAndType(ClipEngine.shareUri(ctx, f), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { ctx.startActivity(view) }
                    }
                }
            }
        }
    }
    Text("يُصنع المقطع على جهازك: كل آية بخط المصحف تبقى طوال تلاوتها فوق الخلفية، والتلاوة من EveryAyah دون أي تعديل. حد المقطع ${ClipEngine.MAX_AYAT} آية.", style = MaterialTheme.typography.bodySmall, color = b.muted)

    if (picking) ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = b.surface) {
        Column(Modifier.padding(16.dp)) {
            content.reciters.forEach { r ->
                Text(r.name, style = MaterialTheme.typography.bodyLarge, color = if (r.id == reciter.id) b.primaryInk else b.ink,
                    modifier = Modifier.fillMaxWidth().clickable { prefs.setReciter(r.id); picking = false }.padding(vertical = 12.dp))
                HorizontalDivider(color = b.line)
            }
        }
    }
}
