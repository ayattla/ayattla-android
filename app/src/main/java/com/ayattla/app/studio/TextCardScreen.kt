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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.Routes
import com.ayattla.app.data.RemoteImages
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.CardShape
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.HeroHeader
import com.ayattla.app.ui.components.ServiceRow
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.theme.LocalBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Text for a card made on another screen (hadith, a name of Allah, a zikr). */
data class CardText(val title: String, val text: String, val reference: String, val naskh: Boolean = true, val ratio: CardRatio = CardRatio.Square)

object PendingText { var value: CardText? = null }

/** Card maker for a hadith, a zikr or a name: the text as published, a real photo or a colour, four sizes. */
@Composable
fun TextCardScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val card = remember { PendingText.value } ?: run { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    var ratio by remember { mutableStateOf(card.ratio) }
    var style by remember { mutableStateOf(CardStyle.Emerald) }
    var bg by remember { mutableStateOf(PendingMedia.take()) }
    var touched by remember { mutableStateOf(bg != null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) { if (!touched) bg = defaultMedia(ctx, video = false) }
    LaunchedEffect(ratio, style, bg) {
        bitmap = null
        val photo = bg?.still?.let { RemoteImages.load(ctx, it, 1400) }
        bitmap = withContext(Dispatchers.Default) {
            CardRenderer.renderText(ctx, card.text, card.reference, ratio, style, photo = photo, naskh = card.naskh)
        }
    }
    val save: () -> Unit = {
        bitmap?.let { bmp ->
            scope.launch {
                val uri = withContext(Dispatchers.IO) { CardRenderer.saveToGallery(ctx, bmp, "ayattla-card-${System.currentTimeMillis()}") }
                Toast.makeText(ctx, if (uri != null) "حُفظت البطاقة في الصور" else "تعذر الحفظ", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) save() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(card.title, onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(ratio.w.toFloat() / ratio.h).clip(CardShape).background(b.surface2),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = bitmap
                if (bmp == null) CircularProgressIndicator(color = b.green) else Image(bmp.asImageBitmap(), "معاينة البطاقة", Modifier.fillMaxSize())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandButton("مشاركة", Modifier.weight(1f), icon = Icons.Filled.Share, enabled = bitmap != null) {
                    val bmp = bitmap ?: return@BrandButton
                    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, CardRenderer.shareUri(ctx, bmp))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(send, "مشاركة البطاقة"))
                }
                BrandButton("حفظ في الصور", Modifier.weight(1f), icon = Icons.Filled.Download, kind = BtnKind.Secondary, enabled = bitmap != null) {
                    if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    else save()
                }
            }
            AppCard {
                Text("الخلفية", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Spacer(Modifier.height(8.dp))
                BackgroundPicker(bg, allowVideo = false) { bg = it; touched = true }
                if (bg == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    CardStyle.entries.forEach { st -> Chip(st.label, st == style, { style = st }) }
                }
            }
            AppCard {
                Text("المقاس", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    CardRatio.entries.filter { it != CardRatio.Wide }.forEach { r -> Chip(r.label, r == ratio, { ratio = r }) }
                }
            }
            Text("النص كما في مصدره دون أي تعديل.", style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}

/** «حالات واتساب»: where the status starts (as the iOS StatusSourceView). */
@Composable
fun StatusScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("حالة", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = LocalBottomInset.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeroHeader("صمم حالتك", eyebrow = "حالات واتساب", subtitle = "صورة بمقاس الحالة 9:16 بخلفية حقيقية، أو مقطع متحرك بالتلاوة")
            Text("من أين تبدأ؟", style = MaterialTheme.typography.titleLarge, color = LocalBrand.current.ink, modifier = Modifier.padding(top = 8.dp))
            ServiceRow("آية", "بالرسم العثماني من المصحف الموثق", Icons.Outlined.AutoStories, style = TileStyle.Emerald) {
                nav.navigate(Routes.studioMode(video = false))
            }
            ServiceRow("ذكر", "من حصن المسلم، ثم «اصنع حالة» تحت الذكر", Icons.Outlined.WbSunny, style = TileStyle.Gradient(2)) { nav.navigate("azkar-all") }
            ServiceRow("حديث صحيح أو حسن", "بالراوي والمصدر والحكم من الدرر السنية", Icons.Outlined.Book, style = TileStyle.Gradient(1)) { nav.navigate("hadith") }
            ServiceRow("مقطع متحرك", "آيات بصوت القارئ فوق فيديو حقيقي", Icons.Outlined.AutoStories, style = TileStyle.Gradient(0)) { nav.navigate(Routes.studioMode(video = true)) }
        }
    }
}
