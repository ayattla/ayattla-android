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
fun StudioScreen(nav: NavHostController, s0: Int, a0: Int, b0: Int, v0: Int = -1) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val daily = remember { DailyAyah.pick() }
    val valid = s0 in 1..114 && a0 >= 1 && a0 <= content.surah(s0).ayahCount
    var surah by rememberSaveable { mutableIntStateOf(if (valid) s0 else daily.surah) }
    var from by rememberSaveable { mutableIntStateOf(if (valid) a0 else daily.from) }
    var to by rememberSaveable {
        mutableIntStateOf(if (valid) b0.coerceIn(a0, minOf(content.surah(s0).ayahCount, a0 + MAX_AYAT - 1)) else daily.to)
    }
    var ratio by rememberSaveable { mutableStateOf(CardRatio.Square) }
    var style by rememberSaveable { mutableStateOf(CardStyle.Emerald) }
    var pickSurah by remember { mutableStateOf(false) }
    var video by rememberSaveable(v0) { mutableStateOf(v0 == 1) }
    val maxAyat = if (video) ClipEngine.MAX_AYAT else MAX_AYAT
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val s = content.surah(surah)

    LaunchedEffect(surah, from, to, ratio, style, video) {
        bitmap = null
        val ayat = content.ayat(surah, from, if (video) from else to)
        val ref = "سورة ${s.shortName} · " + if (from == to || video) "$from" else "$from-$to"
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = com.ayattla.app.LocalBottomInset.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        com.ayattla.app.ui.components.LargeTitle("صناعة المحتوى")
        com.ayattla.app.ui.components.HeroHeader(
            if (video) "مقطع قرآني" else "بطاقة آية", eyebrow = "الاستوديو",
            subtitle = if (video) "آيات بصوت القارئ وتظليل الكلمات، تصنع على جهازك بلا علامة مائية" else "صورة بالمقاس الذي تريد، جاهزة للحالة والمنشور",
        ) {
            com.ayattla.app.ui.components.SiteSegmented(
                listOf(false to "بطاقة صورة", true to "مقطع فيديو"), video,
                { v -> video = v; if (v && ratio == CardRatio.Portrait) ratio = CardRatio.Story },
            )
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(ratio.w.toFloat() / ratio.h).clip(CardShape).background(b.surface2),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp == null) CircularProgressIndicator(color = b.accent)
            else Image(bmp.asImageBitmap(), "معاينة البطاقة", Modifier.fillMaxSize())
        }
        if (video) ClipPanel(surah, from, to.coerceAtMost(from + maxAyat - 1), ratio, style)
        else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.ayattla.app.ui.components.BrandButton("مشاركة", Modifier.weight(1f), icon = Icons.Filled.Share, enabled = bitmap != null) {
                val bmp = bitmap ?: return@BrandButton
                val uri = CardRenderer.shareUri(ctx, bmp)
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(send, "مشاركة البطاقة"))
            }
            com.ayattla.app.ui.components.BrandButton("حفظ في الصور", Modifier.weight(1f), icon = Icons.Filled.Download, kind = com.ayattla.app.ui.components.BtnKind.Secondary, enabled = bitmap != null) {
                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else save()
            }
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
            Stepper("من آية", from, 1, s.ayahCount) { from = it; if (to < it) to = it; if (to - it >= maxAyat) to = it + maxAyat - 1 }
            Stepper("إلى آية", to, from, minOf(s.ayahCount, from + maxAyat - 1)) { to = it }
        }
        AppCard {
            Text("المقاس", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CardRatio.entries.filter { it != CardRatio.Wide && (!video || it != CardRatio.Portrait) }.forEach { r -> Chip(r.label, r == ratio, { ratio = r }) }
            }
            Spacer(Modifier.height(12.dp))
            Text("الخلفية", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CardStyle.entries.forEach { st -> Chip(st.label, st == style, { style = st }) }
            }
        }
        if (!video) Text("نص الآيات من المصحف كما هو دون أي تعديل. حد البطاقة $MAX_AYAT آيات.", style = MaterialTheme.typography.bodySmall, color = b.muted)
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
