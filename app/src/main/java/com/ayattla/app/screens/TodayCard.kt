package com.ayattla.app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.data.Hadith
import com.ayattla.app.data.Journey
import com.ayattla.app.data.SiteApi
import com.ayattla.app.data.Stats
import com.ayattla.app.data.Today
import com.ayattla.app.data.TodaySlot
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import java.time.LocalDateTime
import kotlinx.coroutines.delay

private fun TodaySlot.icon(): ImageVector = when (this) {
    TodaySlot.Morning -> Icons.Outlined.WbSunny
    TodaySlot.Wird -> Icons.Outlined.MenuBook
    TodaySlot.Evening -> Icons.Outlined.NightsStay
    TodaySlot.Hadith -> Icons.Outlined.AutoStories
    TodaySlot.Night -> Icons.Outlined.Bedtime
}

/**
 * «يومي»: one clear step for this part of the day, chosen by the real prayer times. A strip of five
 * marks shows the day at a glance; a finished step says so and names what comes next.
 */
@Composable
fun TodayCard(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val loc = rememberPrayerLocation()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { Journey.load(ctx); while (true) { delay(30_000); now = LocalDateTime.now() } }
    val times = remember(loc, now.toLocalDate()) { loc.times(now.toLocalDate()) }
    val slot = Today.slotAt(now, times)
    val day = Today.dayOf(now, times)
    val seasons = remember(day, slot) {
        val h = runCatching { com.ayattla.app.calc.Hijri.fromGregorian(day) }.getOrNull()
        if (h == null) emptyList() else com.ayattla.app.data.Seasons.notes(
            day, h.month, h.day, slot,
            com.ayattla.app.calc.PrayerTimes.hm12(times.maghrib), com.ayattla.app.calc.PrayerTimes.hm12(loc.times(day.plusDays(1)).fajr),
        )
    }
    // Re-read the counters whenever the card comes back into view (after the user returns from a step).
    var tick by remember { mutableStateOf(0) }
    val entry by nav.currentBackStackEntryAsState()
    LaunchedEffect(entry) { tick++ }

    fun azkarCat(word: String) = content.azkarCategories.firstOrNull { "أذكار $word" == it.title || word in it.title }
    fun progress(s: TodaySlot): Float = todayProgress(ctx, content, prefs, day, s)
    val all = remember(day, slot, tick) { TodaySlot.entries.associateWith { progress(it) } }
    val frac = all[slot] ?: 0f
    val done = frac >= 1f

    // The hadith of the day: from the saved hadith list first, so it shows offline too.
    var hadith by remember { mutableStateOf<Hadith?>(null) }
    LaunchedEffect(slot, day) {
        if (slot == TodaySlot.Hadith && hadith == null) runCatching {
            Today.pickForDay(SiteApi.hadith(ctx, "", 1, "", preferNetwork = false).items, day)
        }.getOrNull()?.let { hadith = it }
    }

    fun open(s: TodaySlot) {
        Stats.event(ctx, "today_opened", s.key)
        when (s) {
            TodaySlot.Morning -> nav.navigate(azkarCat("الصباح")?.let { Routes.azkar(it.id) } ?: "azkar")
            TodaySlot.Evening -> nav.navigate(azkarCat("المساء")?.let { Routes.azkar(it.id) } ?: "azkar")
            TodaySlot.Wird -> nav.navigate("journey")
            TodaySlot.Hadith -> { Today.markOpened(ctx, day, s); nav.navigate("hadith") }
            TodaySlot.Night -> { Today.markOpened(ctx, day, s); nav.navigate(Routes.reader(67)) }
        }
    }

    Column(
        Modifier.fillMaxWidth().surface(b).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Where we are in the day, and the five steps at a glance.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "يومي · بعد ${slot.after.title}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = b.primaryInk, modifier = Modifier.clip(CircleShape).background(b.tint).padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clip(CircleShape).clickable { nav.navigate("week") }.padding(6.dp).semantics {
                contentDescription = "أتممت ${all.values.count { it >= 1f }} من خمس خطوات اليوم، اضغط لملخص الأسبوع"
            }) {
                TodaySlot.entries.forEach { s ->
                    val full = (all[s] ?: 0f) >= 1f
                    Box(
                        Modifier.size(if (s == slot) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (full) b.green else if (s == slot) b.surface else b.surface2)
                            .then(if (s == slot && !full) Modifier.border(2.dp, b.green, CircleShape) else Modifier),
                    )
                }
            }
        }

        AnimatedContent(slot to done, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "today") { (s, isDone) ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(s.icon(), style = TileStyle.Gradient(s.ordinal), size = 46.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(s.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = b.ink)
                        Text(if (isDone) "أحسنت، أتممتها اليوم" else s.line, style = MaterialTheme.typography.bodyMedium, color = b.muted)
                    }
                    if (isDone) Box(
                        Modifier.size(30.dp).clip(CircleShape).background(b.green),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, "تمت", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp)) }
                }

                if (s == TodaySlot.Hadith) hadith?.let { h ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(b.surface2).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(h.text, fontFamily = Fonts.naskh, fontSize = 17.sp, lineHeight = 30.sp, color = b.ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        // «موثق»: the source and grade, and a tap shows where every text comes from.
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.navigate("sources") },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.Verified, "موثق", tint = b.green, modifier = Modifier.size(16.dp))
                            Text(listOf(h.source, h.grade).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium, color = b.goldInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (s != TodaySlot.Hadith && !isDone && frac > 0f) LinearProgressIndicator(
                    progress = { frac }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = b.green, trackColor = b.surface2, drawStopIndicator = {}, gapSize = 0.dp,
                )

                if (seasons.isNotEmpty()) Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    seasons.forEach { n ->
                        Column(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(b.goldSoft)
                                .clickable { Stats.event(ctx, "season_opened", n.key); nav.navigate(n.route) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(n.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink, maxLines = 1)
                            Text(n.line, style = MaterialTheme.typography.labelSmall, color = b.goldInk, maxLines = 1)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isDone) {
                        val n = Today.next(s)
                        Text("التالي بعد ${n.after.title}: ${n.title}", style = MaterialTheme.typography.bodyMedium, color = b.muted, modifier = Modifier.weight(1f))
                        BrandButton("افتح", kind = BtnKind.Secondary) { open(s) }
                    } else {
                        BrandButton(
                            when (s) { TodaySlot.Night -> "سورة الملك"; TodaySlot.Hadith -> "اقرأ المزيد"; else -> if (frac > 0f) "تابع" else "ابدأ" },
                            Modifier.weight(1f),
                        ) { open(s) }
                        when (s) {
                            TodaySlot.Night -> BrandButton("أذكار النوم", Modifier.weight(1f), kind = BtnKind.Secondary) {
                                Stats.event(ctx, "today_opened", "sleep")
                                nav.navigate(azkarCat("النوم")?.let { Routes.azkar(it.id) } ?: "azkar")
                            }
                            TodaySlot.Hadith -> hadith?.let { h ->
                                BrandButton("شارك", kind = BtnKind.Secondary, icon = Icons.Filled.Share) {
                                    Stats.event(ctx, "today_shared", s.key)
                                    com.ayattla.app.studio.PendingText.value = com.ayattla.app.studio.CardText(
                                        "بطاقة حديث", h.text, listOf(h.source, h.grade).filter { it.isNotBlank() }.joinToString(" · "))
                                    com.ayattla.app.studio.PendingText.source = "today"
                                    nav.navigate("text-card")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

/** How far a step of «يومي» is done on a day (0..1), from the counters the app already keeps. */
fun todayProgress(ctx: android.content.Context, content: com.ayattla.app.data.ContentStore, prefs: com.ayattla.app.data.Prefs, day: java.time.LocalDate, s: TodaySlot): Float {
    fun azkarFrac(word: String): Float {
        val c = content.azkarCategories.firstOrNull { "أذكار $word" == it.title || word in it.title } ?: return 0f
        val items = content.azkar(c.id)
        val total = items.sumOf { it.count }.coerceAtLeast(1)
        val done = items.sumOf { it.count - prefs.zikrRemaining(day.toString(), it.id, it.count) }
        return (done.toFloat() / total).coerceIn(0f, 1f)
    }
    return when (s) {
        TodaySlot.Morning -> azkarFrac("الصباح")
        TodaySlot.Evening -> azkarFrac("المساء")
        TodaySlot.Wird -> (Journey.day(day.toEpochDay()).pages.size.toFloat() / Journey.goal.coerceAtLeast(1)).coerceIn(0f, 1f)
        TodaySlot.Hadith -> if (Today.opened(ctx, day, s)) 1f else 0f
        TodaySlot.Night -> maxOf(azkarFrac("النوم"), if (Today.opened(ctx, day, s)) 1f else 0f)
    }
}
