package com.ayattla.app.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.CardShape
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.theme.LocalBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_AYAT = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(nav: NavHostController, s0: Int, a0: Int, b0: Int) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val daily = remember { DailyAyah.pick() }
    var surah by rememberSaveable { mutableIntStateOf(if (s0 > 0) s0 else daily.surah) }
    var from by rememberSaveable { mutableIntStateOf(if (s0 > 0) a0 else daily.from) }
    var to by rememberSaveable { mutableIntStateOf(if (s0 > 0) b0.coerceAtLeast(a0) else daily.to) }
    var ratio by rememberSaveable { mutableStateOf(CardRatio.Square) }
    var style by rememberSaveable { mutableStateOf(CardStyle.Emerald) }
    var pickSurah by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val s = content.surah(surah)

    LaunchedEffect(surah, from, to, ratio, style) {
        bitmap = null
        val ayat = content.ayat(surah, from, to)
        val ref = "سورة ${s.shortName} · " + if (from == to) "$from" else "$from-$to"
        bitmap = withContext(Dispatchers.Default) { CardRenderer.render(ctx, ayat, ref, ratio, style) }
    }

    val save: () -> Unit = {
        bitmap?.let { bmp ->
            scope.launch {
                val uri = withContext(Dispatchers.IO) { CardRenderer.saveToGallery(ctx, bmp, "ayattla-$surah-$from-$to-${System.currentTimeMillis()}") }
                Toast.makeText(ctx, if (uri != null) "حُفظت البطاقة في الصور" else "تعذر الحفظ", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) save() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("اصنع بطاقة آية", style = MaterialTheme.typography.headlineSmall, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Box(
            Modifier.fillMaxWidth().aspectRatio(ratio.w.toFloat() / ratio.h).clip(CardShape).background(b.surface2),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp == null) CircularProgressIndicator(color = b.accent)
            else Image(bmp.asImageBitmap(), "معاينة البطاقة", Modifier.fillMaxSize())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val bmp = bitmap ?: return@Button
                    val uri = CardRenderer.shareUri(ctx, bmp)
                    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(send, "مشاركة البطاقة"))
                },
                enabled = bitmap != null, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = b.primary),
            ) { Icon(Icons.Filled.Share, null); Spacer(Modifier.size(6.dp)); Text("مشاركة") }
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    else save()
                },
                enabled = bitmap != null, modifier = Modifier.weight(1f),
            ) { Icon(Icons.Filled.Download, null); Spacer(Modifier.size(6.dp)); Text("حفظ في الصور") }
        }

        AppCard {
            Text("الآيات", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(
                Modifier.fillMaxWidth().clip(CardShape).clickable { pickSurah = true }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("سورة ${s.shortName}", style = MaterialTheme.typography.bodyLarge, color = b.primaryInk, modifier = Modifier.weight(1f))
                Text("تغيير", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
            }
            Stepper("من آية", from, 1, s.ayahCount) { from = it; if (to < it) to = it; if (to - it >= MAX_AYAT) to = it + MAX_AYAT - 1 }
            Stepper("إلى آية", to, from, minOf(s.ayahCount, from + MAX_AYAT - 1)) { to = it }
        }
        AppCard {
            Text("المقاس", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CardRatio.entries.forEach { r -> Chip(r.label, r == ratio, { ratio = r }) }
            }
            Spacer(Modifier.height(12.dp))
            Text("الخلفية", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CardStyle.entries.forEach { st -> Chip(st.label, st == style, { style = st }) }
            }
        }
        Text("نص الآيات من المصحف كما هو دون أي تعديل. حد البطاقة $MAX_AYAT آيات.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }

    if (pickSurah) {
        ModalBottomSheet(onDismissRequest = { pickSurah = false }, containerColor = b.surface) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(content.surahs, key = { it.id }) { x ->
                    Text(
                        "${x.id}. سورة ${x.shortName}", style = MaterialTheme.typography.bodyLarge, color = b.ink,
                        modifier = Modifier.fillMaxWidth().clickable {
                            surah = x.id; from = 1; to = minOf(x.ayahCount, 1); pickSurah = false
                        }.padding(vertical = 12.dp),
                    )
                    HorizontalDivider(color = b.line)
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Icon(Icons.Filled.Remove, "أقل") }
        Text("$value", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
        IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Icon(Icons.Filled.Add, "أكثر") }
    }
}
