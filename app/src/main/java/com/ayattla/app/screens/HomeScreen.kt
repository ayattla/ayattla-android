package com.ayattla.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.goTab
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.Pill
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.components.ToolTile
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val pick = remember(today) { DailyAyah.pick(today) }
    val ayat = remember(pick) { content.ayat(pick.surah, pick.from, pick.to) }
    val surah = content.surah(pick.surah)
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }
    val loc = rememberPrayerLocation()
    val next = remember(loc, now.hour, now.minute) {
        val t = PrayerTimes.forDate(today, loc.lat, loc.lng, loc.zone, loc.method)
        val t2 = PrayerTimes.forDate(today.plusDays(1), loc.lat, loc.lng, loc.zone, loc.method)
        PrayerTimes.next(now.hour * 60 + now.minute, t, t2) to t
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("آيات تتلى", style = MaterialTheme.typography.headlineMedium, color = b.primaryInk)
            Text(Hijri.format(today), style = MaterialTheme.typography.bodyMedium, color = b.muted)
        }

        // Next prayer
        AppCard(onClick = { nav.navigate("prayer") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, null, tint = b.goldInk)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("الصلاة القادمة: ${next.first.prayer.title}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                    Text(
                        "بعد ${Num.duration(next.first.minutesUntil)} · ${loc.name}",
                        style = MaterialTheme.typography.bodySmall, color = b.muted,
                    )
                }
                Text(
                    PrayerTimes.hm12(if (next.first.isTomorrow) next.second.fajr else next.second.hours(next.first.prayer)),
                    style = MaterialTheme.typography.titleLarge, color = b.primaryInk,
                )
            }
        }

        // Ayah of the day
        EmeraldPanel {
            Pill("آية اليوم")
            Spacer(Modifier.height(12.dp))
            Text(
                ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) },
                fontFamily = Fonts.quran, fontSize = 22.sp, lineHeight = 44.sp, color = b.onPrimary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            val ref = if (pick.from == pick.to) "${pick.from}" else "${pick.from}-${pick.to}"
            Text("سورة ${surah.shortName} · $ref", style = MaterialTheme.typography.labelLarge, color = b.accent)
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                val reciter = rememberReciter()
                IconButton(onClick = { Recitation.play(ctx, content, reciter, pick.surah, pick.from, pick.to) }) {
                    Icon(Icons.Filled.PlayArrow, "استمع", tint = b.accent)
                }
                val key = "${pick.surah}:${pick.from}"
                IconButton(onClick = { prefs.toggleFavorite(key) }) {
                    Icon(if (key in prefs.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "حفظ", tint = b.accent)
                }
                IconButton(onClick = { nav.navigate(Routes.studio(pick.surah, pick.from, pick.to)) }) {
                    Icon(Icons.Filled.AutoAwesome, "اصنع بطاقة", tint = b.accent)
                }
                IconButton(onClick = { shareText(ctx, ayat.joinToString(" ") { it.text } + "\n\nسورة ${surah.shortName} $ref\nayattla.com") }) {
                    Icon(Icons.Filled.Share, "مشاركة", tint = b.accent)
                }
            }
        }

        // Continue reading
        prefs.lastRead?.let { lr ->
            val (s, a) = lr.split(":").map(String::toInt)
            AppCard(onClick = { nav.navigate(Routes.reader(s, a)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = b.goldInk)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("متابعة القراءة", style = MaterialTheme.typography.titleMedium, color = b.ink)
                        Text("سورة ${content.surah(s).shortName} · آية $a", style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                }
            }
        }

        SectionHeader("أدوات يومية")
        ToolGrid(
            listOf(
                ToolItem("المصحف", "اقرأ واستمع", Icons.AutoMirrored.Filled.MenuBook) { nav.goTab("quran") },
                ToolItem("الأذكار", "الصباح والمساء", Icons.Filled.WbTwilight) { nav.goTab("azkar") },
                ToolItem("المسبحة", "عداد التسبيح", Icons.Filled.TouchApp) { nav.navigate("tasbih") },
                ToolItem("مواقيت الصلاة", "حسب مدينتك", Icons.Filled.AccessTime) { nav.navigate("prayer") },
                ToolItem("القبلة", "اتجاه الكعبة", Icons.Filled.Explore) { nav.navigate("qibla") },
                ToolItem("التاريخ الهجري", "أم القرى", Icons.Filled.CalendarMonth) { nav.navigate("hijri") },
                ToolItem("الأحاديث", "صحيح وحسن", Icons.Filled.LibraryBooks) { nav.navigate("hadith") },
                ToolItem("خطة الختمة", "وردك اليومي", Icons.Filled.EventNote) { nav.navigate("khatma") },
            )
        )
        Spacer(Modifier.height(12.dp))
    }
}

data class ToolItem(val title: String, val subtitle: String?, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

@Composable
fun ToolGrid(items: List<ToolItem>, columns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t -> ToolTile(t.title, t.subtitle, t.icon, Modifier.weight(1f), onClick = t.onClick) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
