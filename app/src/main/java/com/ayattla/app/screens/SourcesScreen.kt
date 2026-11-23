package com.ayattla.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.LocalBrand

private data class Source(val icon: ImageVector, val what: String, val from: String, val url: String?)

private val SOURCES = listOf(
    Source(Icons.Outlined.MenuBook, "النص القرآني", "مشروع Tanzil برواية حفص بالرسم العثماني، منقول حرفيا ويطابق آليا مع المصدر في كل إصدار", "https://tanzil.net"),
    Source(Icons.Outlined.AutoStories, "التفسير الميسر", "مجمع الملك فهد لطباعة المصحف الشريف", "https://qurancomplex.gov.sa"),
    Source(Icons.Outlined.Translate, "معاني الكلمات وتوقيتاتها", "Quran.com من Quran Foundation", "https://quran.com"),
    Source(Icons.Outlined.Headphones, "التلاوات", "EveryAyah، تشغل من مصدرها دون تعديل", "https://everyayah.com"),
    Source(Icons.Outlined.Spa, "الأذكار", "أبواب حصن المسلم مع مصدر كل ذكر", null),
    Source(Icons.Outlined.Verified, "الأحاديث وأحكامها", "موسوعة الأحاديث النبوية (HadeethEnc) مع الراوي والتخريج والحكم", "https://hadeethenc.com"),
    Source(Icons.Outlined.Calculate, "المواقيت والقبلة والتاريخ الهجري والزكاة", "تحسب على جهازك، والتاريخ بتقويم أم القرى", null),
    Source(Icons.Outlined.FontDownload, "الخطوط", "Amiri Quran وAmiri وIBM Plex Sans Arabic برخصة OFL", null),
)

/** «مصادرنا»: where every religious text comes from, one card each, with a link to the source. */
@Composable
fun SourcesScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("مصادرنا", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(b.tint).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Verified, null, tint = b.green, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("كل نص ديني له مصدر مسمى", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = b.ink)
                    Text("لا نكتب نصا دينيا بالذكاء الاصطناعي، ويعرض كل حديث بمصدره وحكمه.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                }
            }
            SOURCES.forEach { s ->
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (s.url != null) Modifier.pressable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url))) } else Modifier)
                        .surface(b).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(s.icon, style = TileStyle.Accent(b.green), size = 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(s.what, style = MaterialTheme.typography.titleSmall, color = b.ink)
                        Text(s.from, style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                    if (s.url != null) com.ayattla.app.ui.components.Chevron()
                }
            }
        }
    }
}
