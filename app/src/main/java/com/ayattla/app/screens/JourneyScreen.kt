package com.ayattla.app.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.data.Journey
import com.ayattla.app.notify.NotifyStore
import com.ayattla.app.notify.Scheduler
import com.ayattla.app.studio.todaysOccasion
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.theme.LocalBrand
import java.time.DayOfWeek
import java.time.LocalDate

/** Names for a pages goal: «صفحة»، «صفحتان»، «4 صفحات»، «جزء». */
fun goalLabel(n: Int) = when (n) { 1 -> "صفحة"; 2 -> "صفحتان"; 20 -> "جزء"; in 3..10 -> "$n صفحات"; else -> "$n صفحة" }

/** «يوم واحد»، «يومان»، «5 أيام متتالية»، «12 يوما متتاليا». */
fun streakLabel(n: Int) = when (n) { 1 -> "يوم واحد"; 2 -> "يومان متتاليان"; in 3..10 -> "$n أيام متتالية"; else -> "$n يوما متتاليا" }

private fun dayShort(d: DayOfWeek) = when (d) {
    DayOfWeek.SATURDAY -> "سبت"; DayOfWeek.SUNDAY -> "أحد"; DayOfWeek.MONDAY -> "اثنين"; DayOfWeek.TUESDAY -> "ثلاثاء"
    DayOfWeek.WEDNESDAY -> "أربعاء"; DayOfWeek.THURSDAY -> "خميس"; DayOfWeek.FRIDAY -> "جمعة"
}

/** Where to continue reading: the last place read, else Al-Fatiha. */
fun continueRoute(lastRead: String?): String = lastRead?.split(":")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
    ?.let { "reader/${it[0]}/${it[1]}" } ?: "reader/1/1"

/**
 * «رحلتي مع القرآن»: today's goal in pages, the run of days, this week and this month, and the
 * shortcuts to continue (reading, hifz review, today's status) — what brings the user back each day.
 */
@Composable
fun JourneyScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    LaunchedEffect(Unit) { Journey.load(ctx) }
    val days = Journey.days
    val goal = Journey.goal
    val today = Journey.day()
    val streak = remember(days) { Journey.streak() }
    val best = remember(days) { Journey.best() }

    ToolPage(nav, "رحلتي مع القرآن", "journey") {
        EmeraldPanel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalRing(today.pages.size, goal, 132, Color.White, Color.White.copy(alpha = 0.18f), Color(0xFFE9C46A))
                Text(
                    if (Journey.goalMet()) "أتممت هدفك اليوم، بارك الله فيك" else "بقي ${goalLabel((goal - today.pages.size).coerceAtLeast(1))} من هدفك اليوم",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = Color(0xFFE9C46A), modifier = Modifier.size(20.dp))
                    Text(if (streak > 0) "سلسلتك: ${streakLabel(streak)}" else "ابدأ سلسلتك اليوم",
                        style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
                if (best > streak) Text("أطول سلسلة: ${streakLabel(best)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                BrandButton("تابع القراءة", Modifier.fillMaxWidth().padding(top = 4.dp), kind = BtnKind.Gold) { nav.navigate(continueRoute(prefs.lastRead)) }
            }
        }

        SectionHeader("هدفي اليومي", subtitle = "يحتسب كل صفحة تبقى أمامك ثانيتين في المصحف أو القراءة")
        AppCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Journey.goals.forEach { g ->
                    val on = g == goal
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) b.green else b.tint).clickable { Journey.chooseGoal(g) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(goalLabel(g), style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else b.ink, textAlign = TextAlign.Center) }
                }
            }
            Text("بهذا الهدف تختم القرآن في نحو ${(604 + goal - 1) / goal} يوما.", style = MaterialTheme.typography.bodySmall, color = b.muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        SectionHeader("هذا الأسبوع", subtitle = "الصفحات المقروءة كل يوم")
        AppCard { WeekBars(goal) }

        SectionHeader("هذا الشهر")
        AppCard {
            MonthDots()
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("${Journey.monthPages()}", "صفحة هذا الشهر")
                Stat("${Journey.totalAyat()}", "آية استمعت إليها")
                Stat("${days.values.sumOf { it.hifz }}", "مقطع حفظ ومراجعة")
            }
        }

        SectionHeader("أكمل يومك")
        AppCard {
            BrandButton("مراجعة الحفظ", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) { nav.navigate("hifz-plan") }
            BrandButton("حالة اليوم للواتساب", Modifier.fillMaxWidth().padding(top = 8.dp), kind = BtnKind.Secondary) {
                nav.navigate(todaysOccasion()?.let { "status/$it" } ?: "status")
            }
            BrandButton("استمع لسورة", Modifier.fillMaxWidth().padding(top = 8.dp), kind = BtnKind.Ghost) { nav.navigate("listen") }
        }

        SectionHeader("التذكير")
        AppCard {
            var on by remember { mutableStateOf(NotifyStore.load(ctx).journey) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ذكّرني إن لم أكمل هدفي", style = MaterialTheme.typography.bodyLarge, color = b.ink, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    Text("الساعة 9 مساء، ولا يصلك إن أكملت هدفك", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
                Switch(on, { v ->
                    if (v && !Scheduler.canNotify(ctx)) { nav.navigate("notifications"); return@Switch }
                    on = v; NotifyStore.update(ctx) { it.copy(journey = v) }; Scheduler.reschedule(ctx)
                })
            }
        }
    }
}

@Composable
fun GoalRing(done: Int, goal: Int, sizeDp: Int, ink: Color, track: Color, fill: Color) {
    val frac by animateFloatAsState((done.toFloat() / goal).coerceIn(0f, 1f), tween(700), label = "goal")
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = track, strokeWidth = (sizeDp / 12).dp, gapSize = 0.dp)
        CircularProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxSize(), color = fill, strokeWidth = (sizeDp / 12).dp,
            trackColor = Color.Transparent, gapSize = 0.dp, strokeCap = StrokeCap.Round)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done", style = (if (sizeDp > 100) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.Bold), color = ink)
            Text("من ${goalLabel(goal)}", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun WeekBars(goal: Int) {
    val b = LocalBrand.current
    val week = remember(Journey.days) { Journey.week() }
    val max = maxOf(goal, week.maxOf { it.second.pages.size }, 1)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / 7f
            val bw = slot * 0.5f
            val goalY = size.height * (1 - goal.toFloat() / max)
            week.forEachIndexed { i, (_, d) ->
                // Same order as the labels under it (laid out right to left): today at the right edge.
                val x = slot * i + (slot - bw) / 2
                val h = size.height * d.pages.size / max
                drawRoundRect(b.tint, Offset(x, 0f), Size(bw, size.height), CornerRadius(8f, 8f))
                if (h > 0) drawRoundRect(if (d.pages.size >= goal) b.green else b.green.copy(alpha = 0.55f), Offset(x, size.height - h), Size(bw, h), CornerRadius(8f, 8f))
            }
            drawLine(b.muted, Offset(0f, goalY), Offset(size.width, goalY), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            week.reversed().forEach { (date, d) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${d.pages.size}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink)
                    Text(if (date == LocalDate.now()) "اليوم" else dayShort(date.dayOfWeek), style = MaterialTheme.typography.labelSmall, color = b.muted)
                }
            }
        }
        Text("الخط المتقطع هدفك اليومي", style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

/** This month as a grid of days: green when the goal was met, light when there was some reading or listening. */
@Composable
private fun MonthDots() {
    val b = LocalBrand.current
    val now = LocalDate.now()
    val first = now.withDayOfMonth(1)
    val lead = (first.dayOfWeek.value % 7 + 1) % 7 // Saturday first
    val cells = List(lead) { null } + (1..now.lengthOfMonth()).map { first.withDayOfMonth(it) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة").forEach {
                Text(it.take(2), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { d ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val day = Journey.day(d.toEpochDay())
                            val met = day.pages.size >= Journey.goal
                            val bg = when { met -> b.green; day.active -> b.green.copy(alpha = 0.3f); else -> b.tint }
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                                Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (d == now) FontWeight.Bold else FontWeight.Normal),
                                    color = if (met) Color.White else b.ink)
                            }
                        }
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val b = LocalBrand.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = b.primaryInk)
        Text(label, style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
    }
}
