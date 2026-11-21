package com.ayattla.app.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayattla.app.data.MediaLibrary
import com.ayattla.app.data.SiteMedia
import com.ayattla.app.screens.RemoteImage
import com.ayattla.app.ui.components.CategorySelector
import com.ayattla.app.ui.components.SiteSegmented
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.theme.LocalBrand

/** A background chosen on another screen (home gallery, shortcuts), picked up by the editor. */
object PendingMedia {
    var media: SiteMedia? = null
    fun take(): SiteMedia? = media.also { media = null }
}

/** The site's collections (/api/media/collections, 4 Nov 2026), as the iOS fallback list. */
val MediaCollections = listOf(
    "mosques" to "مساجد", "sky" to "سماء", "night" to "ليل", "nature" to "طبيعة", "sea" to "بحر", "mountains" to "جبال",
    "rain" to "مطر", "desert" to "صحراء", "sunset" to "غروب", "light" to "ضوء", "water" to "ماء",
)

/**
 * Background picker: real photos and videos from the site's library (Pixabay and Pexels), by
 * collection, or a plain colour style. [allowVideo] offers videos (clip maker only).
 */
@Composable
fun BackgroundPicker(selected: SiteMedia?, allowVideo: Boolean, onSelect: (SiteMedia?) -> Unit) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(if (allowVideo && (selected == null || selected.isVideo)) "video" else "image") }
    var cat by rememberSaveable { mutableStateOf("mosques") }
    var items by remember { mutableStateOf<List<SiteMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val type = if (allowVideo) kind else "image"
    LaunchedEffect(type, cat) {
        loading = true
        items = MediaLibrary.curated(MediaLibrary.search(ctx, type, cat))
        loading = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allowVideo) SiteSegmented(listOf("video" to "فيديو متحرك", "image" to "صورة"), kind, { kind = it })
        CategorySelector(MediaCollections, cat, { cat = it }, contentPadding = 0.dp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Plain colour (the style chips below).
            Tile(selected == null, Modifier.pressable { onSelect(null) }) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(b.gradient)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.FormatColorFill, null, tint = b.onDeepAccent)
                        Text("لون", style = MaterialTheme.typography.labelMedium, color = b.onDeepAccent)
                    }
                }
            }
            if (items.isEmpty()) repeat(5) { Box(Modifier.width(78.dp).height(120.dp).clip(RoundedCornerShape(14.dp)).background(b.surface2)) }
            items.forEach { m ->
                Tile(selected?.mediaId == m.mediaId && selected.provider == m.provider, Modifier.pressable { onSelect(m) }) {
                    RemoteImage(m.thumbUrl ?: m.previewUrl, Modifier.fillMaxSize(), maxWidth = 300)
                    if (m.isVideo) Box(
                        Modifier.align(Alignment.BottomStart).padding(6.dp).size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                }
            }
        }
        if (!loading && items.isEmpty()) Text(if (!com.ayattla.app.data.Net.isOnline) "بدون إنترنت: اختر لونا أو صورة من جهازك، أو جهّز الخلفيات من «الاستخدام بدون إنترنت»." else "تعذر تحميل الخلفيات الآن. تحقق من الاتصال.", style = MaterialTheme.typography.bodySmall, color = b.muted)
        else Text("من مكتبة الموقع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted)
    }
}

@Composable
private fun Tile(on: Boolean, modifier: Modifier, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.width(78.dp).height(120.dp).clip(shape).background(b.surface2)
            .border(if (on) 3.dp else 1.dp, if (on) b.green else b.line, shape),
    ) {
        content()
        if (on) Icon(Icons.Filled.CheckCircle, null, tint = b.green, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(20.dp).clip(CircleShape).background(Color.White))
    }
}

/** First curated item of a kind, for a sensible default background. */
suspend fun defaultMedia(ctx: android.content.Context, video: Boolean): SiteMedia? =
    MediaLibrary.curated(MediaLibrary.search(ctx, if (video) "video" else "image", "mosques")).firstOrNull()
