package com.ayattla.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.data.Journey
import com.ayattla.app.data.TodaySlot
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.LocalBrand
import java.time.DayOfWeek
import java.time.LocalDate

private val SHORT = mapOf(
    TodaySlot.Morning to "الصباح", TodaySlot.Wird to "الورد", TodaySlot.Evening to "المساء",
    TodaySlot.Hadith to "الحديث", TodaySlot.Night to "الليل",
)

private fun dayName(d: LocalDate) = when (d.dayOfWeek) {
    DayOfWeek.SATURDAY -> "السبت"; DayOfWeek.SUNDAY -> "الأحد"; DayOfWeek.MONDAY -> "الاثنين"
    DayOfWeek.TUESDAY -> "الثلاثاء"; DayOfWeek.WEDNESDAY -> "الأربعاء"; DayOfWeek.THURSDAY -> "الخميس"
    else -> "الجمعة"
}

/** The five prayers recorded in «صلواتي اليوم» on a day (0..5). */
private fun prayersLogged(c: Context, d: LocalDate) =
    c.getSharedPreferences("prayer.tracker", Context.MODE_PRIVATE).getStringSet("d.$d", emptySet()).orEmpty().size

/**
 * «أسبوعي»: the last seven days, one row each, the five steps of «يومي» and the prayers recorded.
 * Read from what the app already keeps on the device; nothing is compared with anyone.
 */
@Composable
fun WeekScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    LaunchedEffect(Unit) { Journey.load(ctx) }
    val today = LocalDate.now()
    val days = remember(today) { (0 until 7).map { today.minusDays(it.toLong()) } }
    val grid = remember(today) { days.associateWith { d -> TodaySlot.entries.associateWith { todayProgress(ctx, content, prefs, d, it) } } }
    val stepsDone = grid.values.sumOf { m -> m.values.count { it >= 1f } }
    val prayers = days.sumOf { prayersLogged(ctx, it) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("أسبوعي", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Two numbers that sum up the week, then the days.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("$stepsDone من 35" to "خطوة من «يومي»", "$prayers من 35" to "صلاة سجلتها").forEach { (n, l) ->
                    Column(Modifier.weight(1f).surface(b).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(n, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = b.ink)
                        Text(l, style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().surface(b).padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("", Modifier.width(72.dp))
                    TodaySlot.entries.forEach { s ->
                        Text(SHORT[s]!!, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
                    }
                    Text("الصلوات", Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
                }
                days.forEach { d ->
                    val row = grid[d].orEmpty()
                    val pr = prayersLogged(ctx, d)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).semantics {
                            contentDescription = "${dayName(d)}: ${row.values.count { it >= 1f }} من خمس خطوات، و$pr صلوات"
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (d == today) "اليوم" else dayName(d), Modifier.width(72.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (d == today) FontWeight.Bold else FontWeight.Medium), color = b.ink)
                        TodaySlot.entries.forEach { s ->
                            val f = row[s] ?: 0f
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape)
                                        .background(if (f >= 1f) b.green else if (f > 0f) b.tint else b.surface2)
                                        .then(if (f > 0f && f < 1f) Modifier.border(1.5.dp, b.green, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { if (f >= 1f) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                            }
                        }
                        Text("$pr/5", Modifier.width(52.dp), style = MaterialTheme.typography.labelLarge, color = if (pr == 5) b.primaryInk else b.muted, textAlign = TextAlign.Center)
                    }
                }
            }
            Text(
                "السجل محفوظ على جهازك فقط، ولا يقارن بأحد. الدائرة الممتلئة خطوة أتممتها، والمحددة خطوة بدأتها.",
                style = MaterialTheme.typography.bodySmall, color = b.muted,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(b.surface2).padding(12.dp),
            )
        }
    }
}
