package com.ayattla.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.audio.AudioCache
import com.ayattla.app.audio.Downloads
import com.ayattla.app.data.Net
import com.ayattla.app.data.OfflineLevel
import com.ayattla.app.data.OfflinePrep
import com.ayattla.app.data.OfflineTools
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.LauncherIcon
import com.ayattla.app.ui.components.OfflineChip
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.offlineIcon
import com.ayattla.app.ui.components.offlineTint
import com.ayattla.app.ui.theme.LocalBrand
import java.util.Locale

/**
 * «الاستخدام بدون إنترنت»: what works now, getting the app ready (hadith, tajweed and meanings,
 * backgrounds, recitations), the space used, and the related settings.
 */
@Composable
fun OfflineScreen(nav: NavHostController, from: String? = null) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val online = Net.isOnline
    val v = OfflinePrep.version + Downloads.version
    val status = remember(v) { OfflinePrep.status(ctx) }
    val reciter = rememberReciter()
    val fromTool = from?.let { id -> SiteTool.all.firstOrNull { it.id == id } }
    val sp = remember { ctx.getSharedPreferences("ayat", android.content.Context.MODE_PRIVATE) }

    ToolPage(nav, "الاستخدام بدون إنترنت", "offline") {
        // 1. Connection state.
        EmeraldPanel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(if (online) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null, tint = Color(0xFFE9C46A), modifier = Modifier.size(30.dp))
                }
                Text(if (online) "أنت متصل بالإنترنت" else "أنت بدون إنترنت الآن", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center)
                Text(
                    if (online) "جهّز التطبيق الآن ليعمل كاملا عند انقطاع الاتصال."
                    else "القرآن والأذكار والمواقيت والقبلة والأدوات اليومية تعمل كما هي.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                )
            }
        }

        if (fromTool != null && OfflineTools.level(fromTool.id) == OfflineLevel.Online && !online) AppCard {
            Text("«${fromTool.title}» تحتاج اتصالا بالإنترنت", style = MaterialTheme.typography.titleMedium, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("ستعمل تلقائيا عند عودة الاتصال. هذه أدوات تعمل الآن بدلا منها:", style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // 2. Preparation.
        SectionHeader("جهّز التطبيق", subtitle = "يحفظ على جهازك ما تحتاجه الأدوات")
        AppCard {
            PrepRow(Icons.Outlined.Download, "الأحاديث الصحيحة والحسنة", if (status.hadithCount > 0) "${Num(status.hadithCount)} حديثا محفوظا للتصفح والبحث والتحقق" else "لم تحفظ بعد")
            PrepRow(Icons.Outlined.Download, "التجويد ومعاني الكلمات", if (status.tajweedSurahs > 0) "${Num(status.tajweedSurahs)} سورة بالتجويد و${Num(status.wordSurahs)} بمعاني الكلمات" else "جزء عم والسور المشهورة")
            PrepRow(Icons.Outlined.Download, "خلفيات البطاقات والحالات", if (status.images > 0) "${Num(status.images)} صورة محفوظة" else "مختارات من المساجد والطبيعة والسماء")
            if (OfflinePrep.running) {
                Text(OfflinePrep.stage, style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
                LinearProgressIndicator(progress = { OfflinePrep.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = b.green, trackColor = b.line)
                BrandButton("إيقاف", Modifier.fillMaxWidth(), kind = BtnKind.Ghost) { OfflinePrep.cancel() }
            } else {
                BrandButton(
                    if (status.hadithCount > 0) "تحديث الملفات المحفوظة" else "جهّز التطبيق للعمل بدون إنترنت",
                    Modifier.fillMaxWidth().padding(top = 8.dp), icon = Icons.Outlined.Download, enabled = online,
                ) { OfflinePrep.start(ctx) }
                if (!online) Text("يتطلب التجهيز اتصالا بالإنترنت.", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            OfflinePrep.lastResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = b.primaryInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
        }

        // 3. Recitations.
        SectionHeader("التلاوات بدون إنترنت", subtitle = "ما تستمع إليه يحفظ تلقائيا")
        AppCard {
            PrepRow(Icons.Outlined.Headphones, "السور المحمّلة", "${mb(status.recitationBytes)} على جهازك")
            PrepRow(Icons.Outlined.Headphones, "ما استمعت إليه", "${mb(AudioCache.bytes())} يعاد تشغيله بدون إنترنت")
            val juz = (78..114).toList()
            BrandButton(
                "حمّل جزء عم بصوت ${reciter.name} (نحو ${Num(OfflinePrep.estimateMb(content, juz))} م.ب)",
                Modifier.fillMaxWidth().padding(top = 8.dp), kind = BtnKind.Gold, enabled = online && !OfflinePrep.running,
            ) { OfflinePrep.downloadRecitations(ctx, content, reciter) }
            BrandButton("اختر السور والقارئ", Modifier.fillMaxWidth().padding(top = 8.dp), kind = BtnKind.Secondary) { nav.navigate("downloads") }
        }

        // 4. What works, by level.
        listOf(OfflineLevel.Full, OfflineLevel.Saved, OfflineLevel.Online).forEach { level ->
            val tools = SiteTool.all.filter { OfflineTools.level(it.id) == level && it.id != "offline" }
            if (tools.isEmpty()) return@forEach
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { OfflineChip(level) }
            Text(level.hint, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            AppCard {
                tools.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { t ->
                            LauncherIcon(
                                t.short, t.icon, TileStyle.Accent(ToolAccent.color(t.id, b.dark)), Modifier.weight(1f), size = 48.dp,
                                dimmed = level == OfflineLevel.Online && !online,
                            ) { openTool(nav, ctx, t) }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // 5. Settings and space.
        SectionHeader("الإعدادات والمساحة")
        AppCard {
            var keep by remember { mutableStateOf(AudioCache.enabled(ctx)) }
            var auto by remember { mutableStateOf(sp.getBoolean("offline.auto", true)) }
            SwitchLine("حفظ التلاوات التي أستمع إليها", "تعاد بدون إنترنت، حتى 600 م.ب", keep) { keep = it; sp.edit().putBoolean("audio.keepHeard", it).apply() }
            SwitchLine("التحديث التلقائي على واي فاي", "مرة كل أسبوع، بدون التلاوات", auto) { auto = it; sp.edit().putBoolean("offline.auto", it).apply() }
            Text("المساحة المستعملة للملفات المحفوظة: ${mb(status.cacheBytes)}", style = MaterialTheme.typography.bodyMedium, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            var confirm by remember { mutableStateOf(false) }
            BrandButton(if (confirm) "اضغط مرة أخرى للتأكيد" else "حذف الملفات المحفوظة", Modifier.fillMaxWidth().padding(top = 8.dp), icon = Icons.Outlined.DeleteSweep, kind = BtnKind.Ghost, enabled = !OfflinePrep.running) {
                if (confirm) { OfflinePrep.clearSaved(ctx); confirm = false } else confirm = true
            }
            Text("لا يحذف السور المحمّلة ولا محفوظاتك وإعداداتك.", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PrepRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(b.green.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = b.green, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            Text(value, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SwitchLine(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = b.ink, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            Text(sub, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
        Switch(on, onChange)
    }
}

private fun Num(n: Int) = n.toString()
private fun mb(bytes: Long) = String.format(Locale.US, "%.1f م.ب", bytes / 1_048_576.0)
