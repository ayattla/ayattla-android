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
data class CardText(
    val title: String, val text: String, val reference: String, val naskh: Boolean = true, val ratio: CardRatio = CardRatio.Square,
    /** Occasion name drawn at the top («يوم الجمعة»); the user can switch it off. */
    val header: String? = null,
    val style: CardStyle = CardStyle.Emerald,
    val ornament: Ornament = Ornament.Classic,
    /** Start on the colour design rather than a photo (statuses). */
    val colourFirst: Boolean = false,
)

object PendingText { var value: CardText? = null }

/** Card maker for a hadith, a zikr or a name: the text as published, a real photo or a colour, four sizes. */
@Composable
fun TextCardScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val card = remember { PendingText.value } ?: run { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    var ratio by remember { mutableStateOf(card.ratio) }
    var style by remember { mutableStateOf(card.style) }
    var ornament by remember { mutableStateOf(card.ornament) }
    var showHeader by remember { mutableStateOf(card.header != null) }
    var bg by remember { mutableStateOf(PendingMedia.take()) }
    var touched by remember { mutableStateOf(bg != null || card.colourFirst) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) { if (!touched) bg = defaultMedia(ctx, video = false) }
    LaunchedEffect(ratio, style, bg, ornament, showHeader) {
        bitmap = null
        val photo = bg?.still?.let { RemoteImages.load(ctx, it, 1400) }
        bitmap = withContext(Dispatchers.Default) {
            runCatching {
                CardRenderer.renderText(ctx, card.text, card.reference, ratio, style, photo = photo, naskh = card.naskh,
                    ornament = ornament, header = if (showHeader) card.header else null)
            }.getOrNull()
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
                Text("التصميم", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Spacer(Modifier.height(8.dp))
                if (bg == null) {
                    Text("الألوان", style = MaterialTheme.typography.labelLarge, color = b.muted)
                    PalettePicker(style) { style = it }
                    Spacer(Modifier.height(10.dp))
                }
                Text("الزخرفة", style = MaterialTheme.typography.labelLarge, color = b.muted)
                OrnamentPicker(ornament, if (bg == null) style else CardStyle.Night) { ornament = it }
                if (card.header != null) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("اسم المناسبة أعلى البطاقة", showHeader, { showHeader = !showHeader })
                    }
                }
            }
            AppCard {
                Text("الخلفية", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Chip("لون وزخرفة", bg == null, { bg = null; touched = true })
                }
                BackgroundPicker(bg, allowVideo = false) { bg = it; touched = true }
            }
            AppCard {
                Text("المقاس", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    CardRatio.entries.filter { it != CardRatio.Wide }.forEach { r -> Chip(r.label, r == ratio, { ratio = r }) }
                }
            }
            Text("النص كما في مصدره دون أي تعديل.", style = MaterialTheme.typography.bodySmall, color = b.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

