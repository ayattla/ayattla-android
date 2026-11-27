package com.ayattla.app.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.horizontalScroll
import com.ayattla.app.ui.components.ToolTile
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Surface
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.KhatmaPlan
import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Hifz
import com.ayattla.app.goTab
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.LauncherIcon
import com.ayattla.app.ui.components.LogoHorizontal
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.featureSurface
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.softShadow
import com.ayattla.app.ui.components.bleed
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.components.Radius
import com.ayattla.app.ui.theme.ToolAccent
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ───────────── Layout (order, visibility, favourite tools) ─────────────

/** The home sections the user can order and hide («تخصيص الرئيسية»). */
enum class HomeSection(val title: String) {
    Prayer("الصلاة القادمة"), Today("يومي"), ForYou("اختصاراتك"), Journey("رحلتي مع القرآن"), Create("صناعة المحتوى"), All("أقسام الأدوات"), Continue("تابع من حيث توقفت"),
    Wird("الورد والختمة"), Azkar("أذكار اليوم"), Tracker("صلواتي اليوم"), Dhikr("الذكر السريع"),
    Occasion("المناسبات"), Ayah("آية اليوم"), Quiz("مسابقة اليوم"), Tools("أدواتي"), Gallery("معرض الخلفيات"),
    Quick("الوصول السريع"), Name("اسم الله اليوم"), Dua("دعاء من القرآن"),
}

object HomeLayout {
    /**
     * Home 6: every section in a studied order — the day (prayer, «يومي», prayers logged), shortcuts,
     * the Quran (continue, journey, wird, ayah), remembrance (azkar, dhikr, a name of Allah), learning
     * and seasons (quiz, occasions), then making content and every tool.
     */
    val defaultOrder = listOf(
        HomeSection.Prayer, HomeSection.Today, HomeSection.Tracker, HomeSection.ForYou, HomeSection.Continue,
        HomeSection.Journey, HomeSection.Wird, HomeSection.Ayah, HomeSection.Azkar, HomeSection.Dhikr, HomeSection.Name, HomeSection.Dua,
        HomeSection.Quiz, HomeSection.Occasion, HomeSection.Create, HomeSection.Gallery, HomeSection.All,
    )
    /** Only true repeats start hidden: their tools are already in «اختصاراتك». */
    val hiddenByDefault = setOf(HomeSection.Quick, HomeSection.Tools)
    private const val KEY = "order.v4"
    private fun sp(c: Context) = c.getSharedPreferences("home.layout", Context.MODE_PRIVATE)

    /** Ordered sections with their visibility. New sections (from updates) join at the end, hidden. */
    fun load(c: Context): List<Pair<HomeSection, Boolean>> {
        if (!sp(c).contains(KEY)) return personalDefault(c).map { it to (it !in hiddenByDefault) }
        val saved = sp(c).getString(KEY, null)?.split(",")?.mapNotNull { e ->
            val (n, v) = e.split(":").let { it.getOrNull(0) to it.getOrNull(1) }
            HomeSection.entries.firstOrNull { it.name == n }?.let { it to (v != "0") }
        }.orEmpty()
        // «دعاء من القرآن» (2.9.1) joins right after «اسم الله اليوم» for those who saved a layout before it.
        val withDua = if (saved.none { it.first == HomeSection.Dua }) {
            val i = saved.indexOfFirst { it.first == HomeSection.Name }
            if (i >= 0) saved.toMutableList().apply { add(i + 1, HomeSection.Dua to true) } else saved
        } else saved
        return withDua + HomeSection.entries.filter { s -> withDua.none { it.first == s } }.map { it to false }
    }

    /** The default order, then the hidden sections (available in «تخصيص الرئيسية»). */
    fun personalDefault(@Suppress("UNUSED_PARAMETER") c: Context): List<HomeSection> =
        (defaultOrder + HomeSection.entries).distinct()

    fun save(c: Context, l: List<Pair<HomeSection, Boolean>>) =
        sp(c).edit().putString(KEY, l.joinToString(",") { "${it.first.name}:${if (it.second) 1 else 0}" }).apply()

    fun reset(c: Context) = sp(c).edit().remove(KEY).remove("order.v3").remove("order.v2").remove("pinned").apply()

    fun pinned(c: Context): List<String> = sp(c).getString("pinned", null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
    fun setPinned(c: Context, ids: List<String>) = sp(c).edit().putString("pinned", ids.joinToString(",")).apply()
}

// ───────────── Screen ─────────────

/** Home v5: brand header, the next prayer with a live countdown, and sections the user arranges. */
@Composable
fun HomeScreenV5(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var layout by remember { mutableStateOf(HomeLayout.load(ctx)) }
    var pinned by remember { mutableStateOf(HomeLayout.pinned(ctx)) }
    var customizing by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(nav, scroll)
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = LocalBottomInset.current + 16.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                var notice by remember { mutableStateOf(com.ayattla.app.data.Welcome.statsNoticeDue(ctx)) }
                if (notice) StatsNotice(
                    onOk = { com.ayattla.app.data.Welcome.statsNoticeSeen(ctx); notice = false },
                    onOff = { com.ayattla.app.data.Stats.setEnabled(ctx, false); com.ayattla.app.data.Welcome.statsNoticeSeen(ctx); notice = false },
                )
                layout.filter { it.second }.forEachIndexed { i, (s, _) ->
                    // A light entrance, once per launch: each section rises in turn.
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(40L * i.coerceAtMost(6)); shown = true }
                    AnimatedVisibility(shown, enter = fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 12 }) {
                        HomeSectionContent(s, nav, pinned)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).pressable { customizing = true }.clip(RoundedCornerShape(16.dp))
                        .border(1.dp, b.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Tune, null, tint = b.primaryInk, modifier = Modifier.size(20.dp))
                    Text("  تخصيص الرئيسية", style = MaterialTheme.typography.titleSmall, color = b.primaryInk)
                }
                HomeFooterPublic(nav)
            }
        }
    }
    if (customizing) HomeCustomizeSheet(layout, pinned, onChange = { l, p ->
        layout = l; pinned = p; HomeLayout.save(ctx, l); HomeLayout.setPinned(ctx, p)
    }, onReset = {
        HomeLayout.reset(ctx); layout = HomeLayout.load(ctx); pinned = emptyList()
    }) { customizing = false }
}

@Composable
private fun HomeSectionContent(s: HomeSection, nav: NavHostController, pinned: List<String>) {
    when (s) {
        HomeSection.Prayer -> PrayerHero(nav)
        HomeSection.Today -> TodayCard(nav)
        HomeSection.Journey -> JourneyHomeCard(nav)
        HomeSection.ForYou -> ForYou(nav)
        HomeSection.Quick -> MuslimServices(nav)
        HomeSection.Tracker -> PrayerTracker()
        HomeSection.Dhikr -> QuickDhikr(nav)
        HomeSection.Continue -> ContinueStrip(nav)
        HomeSection.Wird -> WirdCard(nav)
        HomeSection.Azkar -> AzkarStatus(nav)
        HomeSection.Quiz -> DailyQuiz()
        HomeSection.Tools -> MyTools(nav, pinned)
        HomeSection.Occasion -> NextOccasion(nav)
        HomeSection.Ayah -> HomeAyahPublic(nav)
        HomeSection.Create -> HomeCreatePublic(nav)
        HomeSection.Gallery -> HomeGalleryPublic(nav)
        HomeSection.All -> HomeAllToolsPublic(nav)
        HomeSection.Name -> NameOfTheDay(nav)
        HomeSection.Dua -> QuranDuaOfTheDay(nav)
    }
}

// ───────────── Header ─────────────

/** The brand header: the logo large at the reading start, search, alerts and settings; it gains a surface once the page scrolls. */
@Composable
private fun HomeHeader(nav: NavHostController, scroll: ScrollState) {
    val b = LocalBrand.current
    val lifted = scroll.value > 8
    val bg by animateColorAsState(if (lifted) b.surface.copy(alpha = 0.97f) else Color.Transparent, tween(200), label = "hdr")
    Box(Modifier.fillMaxWidth().background(bg).then(if (lifted) Modifier.softShadow(RoundedCornerShape(0.dp), 12.dp, b.shadow.copy(alpha = 0.08f), DpOffset(0.dp, 4.dp)) else Modifier)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoHorizontal(Modifier.height(38.dp))
            Spacer(Modifier.weight(1f))
            HeaderButton(Icons.Outlined.Search, "البحث الشامل") { nav.navigate("search") }
            HeaderButton(Icons.Outlined.NotificationsNone, "مركز التنبيهات") { nav.navigate("notifications") }
            HeaderButton(Icons.Outlined.Settings, "الإعدادات") { nav.navigate("settings") }
        }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val b = LocalBrand.current
    Box(
        Modifier.padding(start = 6.dp).size(42.dp).pressable(onClick).clip(CircleShape).background(b.surface)
            .border(1.dp, b.line, CircleShape).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = b.primaryInk, modifier = Modifier.size(21.dp)) }
}

/** A section title, centred, with a small gold mark on each side and an optional action. */
@Composable
fun CenteredTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) =
    com.ayattla.app.ui.components.HomeHead(title, subtitle = homeSubtitle(title), icon = homeIcon(title), action = action, onAction = onAction)

/** One icon per home section, so every header carries the same tile. */
private fun homeIcon(title: String): ImageVector = when {
    title.startsWith("تابع") -> Icons.Outlined.History
    title.startsWith("الورد") -> Icons.Outlined.MenuBook
    title.startsWith("رحلتي") -> Icons.Outlined.Route
    title.startsWith("أذكار") -> Icons.Outlined.WbTwilight
    title.startsWith("مسابقة") -> Icons.Outlined.Quiz
    title.startsWith("أدواتي") || title.startsWith("اختصاراتك") -> Icons.Outlined.GridView
    title.startsWith("المناسبات") -> Icons.Outlined.Event
    title.startsWith("صلواتي") -> Icons.Outlined.TaskAlt
    title.startsWith("الوصول") -> Icons.Outlined.Bolt
    title.startsWith("الذكر") -> Icons.Outlined.TouchApp
    title.startsWith("صناعة") -> Icons.Outlined.AutoAwesome
    title.startsWith("أقسام") -> Icons.Outlined.Apps
    title.startsWith("اسم الله") -> Icons.Outlined.AutoAwesome
    title.startsWith("دعاء") -> Icons.Outlined.MenuBook
    else -> Icons.Outlined.Star
}

private fun homeSubtitle(title: String): String? = when {
    title.startsWith("تابع") -> "ارجع لآخر ما قرأت وسمعت"
    title.startsWith("صلواتي") -> "سجل صلواتك الخمس بلمسة"
    title.startsWith("المناسبات") -> "المواسم والأيام الفاضلة القادمة"
    title.startsWith("اختصاراتك") -> "أدواتك المفضلة في مكان واحد"
    title.startsWith("أذكار") -> "تقدمك في أذكار اليوم"
    title.startsWith("دعاء") -> "دعاء جديد كل يوم بنص المصحف"
    else -> null
}

// ───────────── Prayer hero ─────────────

@Composable
private fun PrayerHero(nav: NavHostController) {
    val loc = rememberPrayerLocation()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = LocalDateTime.now() } }
    val today = now.toLocalDate()
    val t = remember(loc, today) { loc.times(today) }
    val tTomorrow = remember(loc, today) { loc.times(today.plusDays(1)) }
    val tYesterday = remember(loc, today) { loc.times(today.minusDays(1)) }
    val five = listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
    fun at(d: LocalDate, h: Double) = d.atStartOfDay().plusMinutes(PrayerTimes.displayMinute(h).toLong())
    val nextP = five.firstOrNull { at(today, t.hours(it)).isAfter(now) }
    val nextAt = if (nextP != null) at(today, t.hours(nextP)) else at(today.plusDays(1), tTomorrow.fajr)
    val nextName = (nextP ?: Prayer.Fajr).title
    val prevAt = five.map { at(today, t.hours(it)) }.lastOrNull { !it.isAfter(now) } ?: at(today.minusDays(1), tYesterday.isha)
    val left = Duration.between(now, nextAt).coerceAtLeast(Duration.ZERO)
    val span = Duration.between(prevAt, nextAt).seconds.coerceAtLeast(1)
    val progress = (1f - left.seconds.toFloat() / span).coerceIn(0f, 1f)
    val hour = now.hour
    val greeting = when (hour) { in 4..11 -> "صباح الخير"; in 12..16 -> "طاب يومك"; else -> "مساء الخير" }
    val animated by animateFloatAsState(progress, tween(600), label = "prayerProgress")

    Column(
        Modifier.fillMaxWidth().featureSurface(LocalBrand.current, Radius.hero).clickable { nav.navigate("prayer") }.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$greeting · ${Hijri.format(today)}", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        Text("صلاة $nextName", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(
            String.format(Locale.US, "%02d:%02d:%02d", left.seconds / 3600, (left.seconds % 3600) / 60, left.seconds % 60),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp), color = Color.White,
            modifier = Modifier.semantics { contentDescription = "بعد ${left.seconds / 3600} ساعة و${(left.seconds % 3600) / 60} دقيقة" },
        )
        Text("${loc.name} · ${PrayerTimes.hm12(if (nextP != null) t.hours(nextP) else tTomorrow.fajr)}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        LinearProgressIndicator(
            progress = { animated }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(CircleShape),
            color = Color(0xFFE3CB8E), trackColor = Color.White.copy(alpha = 0.18f), drawStopIndicator = {}, gapSize = 0.dp,
        )
        // The five times, the next one lit.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            five.forEach { p ->
                val on = p == (nextP ?: Prayer.Fajr)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (on) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                        .then(if (on) Modifier.border(1.dp, Color(0xFFE3CB8E).copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(p.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), color = Color.White.copy(alpha = if (on) 1f else 0.8f), maxLines = 1)
                    Text(PrayerTimes.hm12(t.hours(p)).substringBefore(" "), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            HeroPill(Icons.Outlined.Explore, "القبلة") { nav.navigate("qibla") }
            HeroPill(Icons.Outlined.Schedule, "المواقيت") { nav.navigate("prayer") }
            HeroPill(Icons.Outlined.NotificationsNone, "التنبيهات") { nav.navigate("notifications") }
        }
    }
}

@Composable
private fun HeroPill(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 38.dp).pressable(onClick).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

// ───────────── Quick actions ─────────────

// ───────────── Continue ─────────────

@Composable
private fun ContinueStrip(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val lr = prefs.lastRead?.split(":")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
    val heard = remember(Recitation.current) { Recitation.lastListened(ctx) }
    val lastTool = remember { ToolUsage.top(ctx, 1).firstOrNull() }
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("تابع من حيث توقفت")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ContinueCard(
                Icons.AutoMirrored.Outlined.MenuBook, 0, if (lr != null) "تابع القراءة" else "ابدأ القراءة",
                lr?.let { "${content.surah(it[0]).shortName} · صفحة ${content.pageOf(it[0], it[1])}" } ?: "سورة الفاتحة",
                progress = lr?.let { content.pageOf(it[0], it[1]) / 604f }, modifier = Modifier.weight(1f),
            ) { if (lr != null) nav.navigate(Routes.reader(lr[0], lr[1])) else nav.goTab("quran") }
            val reciter = heard?.let { h -> content.reciters.firstOrNull { it.folder == h.first } }
            ContinueCard(
                Icons.Outlined.Headphones, 1, if (heard != null && reciter != null) "تابع الاستماع" else "استمع للقرآن",
                if (heard != null && reciter != null && heard.second in 1..114) "${content.surah(heard.second).shortName} · آية ${heard.third}" else "اختر قارئك",
                modifier = Modifier.weight(1f),
            ) {
                if (heard != null && reciter != null && heard.second in 1..114) Recitation.play(ctx, content, reciter, heard.second, heard.third, continuous = true)
                else nav.goTab("quran")
            }
        }
        if (lastTool != null) Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp).pressable { openTool(nav, ctx, lastTool) }.surface(b, Surface.Tinted, 16.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            IconTile(lastTool.icon, style = TileStyle.Accent(ToolAccent.color(lastTool.id, b.dark)), size = 34.dp)
            Text("آخر أداة: ${lastTool.title}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("افتح", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
        }
    }
}

@Composable
private fun ContinueCard(icon: ImageVector, tile: Int, title: String, line: String, modifier: Modifier, progress: Float? = null, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(modifier.heightIn(min = 138.dp).pressable(onClick).surface(b, Surface.Tinted, 16.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconTile(icon, style = TileStyle.Gradient(tile), size = 40.dp)
        Text(title, style = MaterialTheme.typography.titleSmall, color = b.ink, textAlign = TextAlign.Center)
        Text(line, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (progress != null) LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = b.green, trackColor = b.tint, drawStopIndicator = {}, gapSize = 0.dp,
        )
    }
}

// ───────────── Wird and khatma ─────────────

@Composable
private fun WirdCard(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    val plan = remember(prefs.khatma) { KhatmaPlan.decode(prefs.khatma) }
    LaunchedEffect(Unit) { Hifz.load(ctx) }
    val hifz = Hifz.plan
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("الورد والختمة")
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val done = plan?.completedDays?.size ?: 0
            val total = plan?.days ?: 0
            val frac = if (total > 0) done.toFloat() / total else 0f
            ProgressRing(frac, if (plan != null) "${(frac * 100).toInt()}٪" else "—")
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (plan == null) {
                    Text("لا توجد خطة ختمة", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
                    Text("حدد مدة الختمة ويقسمها التطبيق وردا يوميا", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center)
                    Text("ابدأ خطة", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.padding(top = 4.dp).clickable { nav.navigate("khatma") })
                } else {
                    val day = plan.dayOn(LocalDate.now()).coerceIn(1, plan.days)
                    val r = plan.range(day)
                    val todayDone = day in plan.completedDays
                    Text(if (plan.isFinished) "أتممت الختمة" else "ورد اليوم ${if (todayDone) "✓" else ""}", style = MaterialTheme.typography.titleMedium, color = b.ink)
                    if (r != null && !plan.isFinished) Text("من صفحة ${r.first} إلى ${r.last} · اليوم $day من ${plan.days}", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (r != null && !plan.isFinished) Text("اقرأ الورد", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.clickable { nav.navigate("page/${r.first}") })
                        Text("الخطة", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.clickable { nav.navigate("khatma") })
                    }
                }
            }
        }
        if (hifz != null) Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp).pressable { nav.navigate("hifz-plan") }.surface(b, Surface.Tinted, 16.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            Text("الحفظ: ${Hifz.memorisedAyat()} آية · مراجعات اليوم ${Hifz.due().size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Center)
            Text(if (Hifz.learnedToday()) "✓" else "احفظ", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
        }
    }
}

@Composable
private fun ProgressRing(frac: Float, label: String) {
    val b = LocalBrand.current
    val a by animateFloatAsState(frac.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            progress = { 1f }, modifier = Modifier.fillMaxSize(), color = b.tint, strokeWidth = 7.dp, gapSize = 0.dp,
        )
        androidx.compose.material3.CircularProgressIndicator(
            progress = { a }, modifier = Modifier.fillMaxSize(), color = b.green, strokeWidth = 7.dp, trackColor = Color.Transparent, gapSize = 0.dp,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        Text(label, style = MaterialTheme.typography.titleSmall, color = b.primaryInk)
    }
}

// ───────────── «رحلتي مع القرآن» ─────────────

/** Today's goal ring, the run of days, and one tap to continue reading. */
@Composable
private fun JourneyHomeCard(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    LaunchedEffect(Unit) { com.ayattla.app.data.Journey.load(ctx) }
    val j = com.ayattla.app.data.Journey
    val days = j.days
    val streak = remember(days) { j.streak() }
    val today = j.day()
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("رحلتي مع القرآن", "التفاصيل") { nav.navigate("journey") }
        Row(
            Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).clickable { nav.navigate("journey") }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GoalRing(today.pages.size, j.goal, 78, b.primaryInk, b.tint, b.green)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (j.goalMet()) "أتممت هدفك اليوم" else "هدفك اليوم: ${goalLabel(j.goal)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = b.ink, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                Text(if (streak > 0) "سلسلتك: ${streakLabel(streak)}" else "اقرأ صفحة لتبدأ سلسلتك",
                    style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                Text("تابع القراءة", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable { nav.navigate(continueRoute(prefs.lastRead)) })
            }
        }
    }
}

// ───────────── Azkar status ─────────────

@Composable
private fun AzkarStatus(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val today = LocalDate.now().toString()
    val cats = remember { listOf("الصباح", "المساء").mapNotNull { w -> content.azkarCategories.firstOrNull { "أذكار $w" == it.title || w in it.title } } }
    val morning = LocalTime.now().hour < 14
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("أذكار اليوم", "الكل") { nav.goTab("azkar") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cats.forEachIndexed { i, c ->
                val items = remember(c.id) { content.azkar(c.id) }
                val total = items.sumOf { it.count }.coerceAtLeast(1)
                val done = items.sumOf { it.count - prefs.zikrRemaining(today, it.id, it.count) }
                val frac = done.toFloat() / total
                val now = (i == 0) == morning
                Column(
                    Modifier.weight(1f).pressable { nav.navigate(Routes.azkar(c.id)) }
                        .then(if (now) Modifier.border(1.5.dp, b.accent.copy(alpha = 0.7f), RoundedCornerShape(Radius.card)) else Modifier)
                        .surface(b, Surface.Tinted, 16.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconTile(if (i == 0) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay, style = TileStyle.Gradient(if (i == 0) 2 else 4), size = 40.dp)
                    Text(c.title, style = MaterialTheme.typography.titleSmall, color = b.ink, maxLines = 1, textAlign = TextAlign.Center)
                    LinearProgressIndicator(
                        progress = { frac.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = b.green, trackColor = b.tint, drawStopIndicator = {}, gapSize = 0.dp,
                    )
                    Text(if (frac >= 1f) "أتممتها اليوم ✓" else if (now) "حان وقتها" else "${(frac * 100).toInt()}٪", style = MaterialTheme.typography.labelMedium, color = if (frac >= 1f || now) b.primaryInk else b.muted)
                }
            }
        }
    }
}

// ───────────── Daily quiz ─────────────

/**
 * «مسابقة اليوم»: "which surah is this ayah from?", built only from the mushaf text (no generated
 * wording). The day's question is the same all day; «سؤال آخر» gives more.
 */
object DailyQuiz {
    data class Q(val ayah: com.ayattla.app.data.Ayah, val options: List<Int>, val answer: Int)

    fun make(content: ContentStore, seed: Long): Q {
        val rnd = java.util.Random(seed)
        var a: com.ayattla.app.data.Ayah
        do {
            val s = 1 + rnd.nextInt(114)
            val list = content.ayat(s)
            a = list[rnd.nextInt(list.size)]
        } while (a.text.split(" ").size < 6)
        val others = (1..114).filter { it != a.surah }.shuffled(rnd).take(3)
        val opts = (others + a.surah).shuffled(rnd)
        return Q(a, opts, opts.indexOf(a.surah))
    }
}

@Composable
private fun DailyQuiz() {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val sp = remember { ctx.getSharedPreferences("quiz.daily", Context.MODE_PRIVATE) }
    val day = LocalDate.now().toEpochDay()
    var extra by remember { mutableIntStateOf(0) }
    val q = remember(day, extra) { DailyQuiz.make(content, day * 7919 + extra) }
    var picked by remember(day, extra) { mutableStateOf(if (extra == 0) sp.getInt("pick.$day", -1).takeIf { it >= 0 } else null) }
    var score by remember { mutableIntStateOf(sp.getInt("score", 0)) }
    var total by remember { mutableIntStateOf(sp.getInt("total", 0)) }
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("مسابقة اليوم")
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Quiz, null, tint = b.goldInk, modifier = Modifier.size(18.dp))
                Text("من أي سورة هذه الآية؟", style = MaterialTheme.typography.titleSmall, color = b.goldInk)
            }
            Text(q.ayah.text, fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 40.sp, color = b.ink, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
            q.options.forEachIndexed { i, s ->
                val right = i == q.answer
                val bg = when {
                    picked == null -> b.surface2
                    right -> b.green.copy(alpha = 0.18f)
                    i == picked -> Color(0xFFB3261E).copy(alpha = 0.12f)
                    else -> b.surface2
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                        .then(if (picked != null && right) Modifier.border(1.dp, b.green, RoundedCornerShape(14.dp)) else Modifier)
                        .clickable(enabled = picked == null) {
                            picked = i; total++; if (right) score++
                            sp.edit().putInt("total", total).putInt("score", score).apply { if (extra == 0) putInt("pick.$day", i) }.apply()
                        }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Text("سورة ${content.surah(s).shortName}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink)
                    if (picked != null && right) Icon(Icons.Filled.Check, null, tint = b.green, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                }
            }
            if (picked != null) {
                Text(if (picked == q.answer) "أحسنت! ${content.surah(q.ayah.surah).shortName} · الآية ${q.ayah.ayah}" else "الجواب: ${content.surah(q.ayah.surah).shortName} · الآية ${q.ayah.ayah}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = b.primaryInk, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("نتيجتك: $score من $total", style = MaterialTheme.typography.labelMedium, color = b.muted)
                    Text("سؤال آخر", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.clip(CircleShape).clickable { extra++ }.padding(6.dp))
                }
            }
        }
    }
}

// ───────────── My tools ─────────────

@Composable
private fun MyTools(nav: NavHostController, pinned: List<String>) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val fav = pinned.mapNotNull { id -> SiteTool.all.firstOrNull { it.id == id } }
    // Pinned first, then real use; before there is any use, the tools matching the first-run interests.
    val top = remember {
        val suggested = if (ToolUsage.hasHistory(ctx)) emptyList() else com.ayattla.app.data.Welcome.suggested(ctx).mapNotNull { SiteTool.byIdOrNull(it) }
        (suggested + ToolUsage.top(ctx, 8)).distinctBy { it.id }
    }
    val list = (fav + top).distinctBy { it.id }.take(8)
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle(if (fav.isNotEmpty()) "أدواتي المفضلة" else "اختصاراتك", "كل الأدوات") { nav.goTab("more") }
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { t ->
                        val from = if (t.id in pinned) "favorite_tool_opened" else "home_shortcut_opened"
                        LauncherIcon(t.short, t.icon, TileStyle.Accent(ToolAccent.color(t.id, b.dark)), Modifier.weight(1f), isNew = t.isNew, dimmed = t.dimmedOffline()) { openTool(nav, ctx, t, from) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** One-time note for people who updated: what the anonymous stats are, and how to turn them off. */
@Composable
private fun StatsNotice(onOk: () -> Unit, onOff: () -> Unit) {
    val b = LocalBrand.current
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ساعدنا في تحسين آيات تتلى", style = MaterialTheme.typography.titleSmall, color = b.ink)
        Text(
            "نجمع أعدادا مجهولة لاستخدام الأدوات والخطوات، بلا اسم ولا رقم ولا موقع ولا أي معرف لجهازك. يمكنك إيقافها الآن أو من الإعدادات.",
            style = MaterialTheme.typography.bodySmall, color = b.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandButton("حسنا", Modifier.weight(1f)) { onOk() }
            BrandButton("إيقافها", Modifier.weight(1f), kind = BtnKind.Ghost) { onOff() }
        }
    }
}

// ───────────── For you ─────────────

/**
 * «مختارات لك»: a greeting by the time of day and, for each interest chosen at first run, a row of the
 * real tools it covers (each with its icon and line). With no interests, one card invites the user to
 * pick some. Every tool stays reachable from «كل الأدوات» whatever is chosen here.
 */
/**
 * «اختصاراتك»: one tidy grid of up to seven tools — pinned tools first, then the tools of the chosen
 * interests, then the most used — without repeats, and «الكل» last.
 */
@Composable
private fun ForYou(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val tools = remember {
        val ids = HomeLayout.pinned(ctx) + com.ayattla.app.data.Welcome.pickedUses(ctx).flatMap { it.tools } +
            listOf("mushaf", "azkar", "prayer-times", "listen", "qibla", "tasbih", "hadith", "khatma")
        ids.distinct().mapNotNull { SiteTool.byIdOrNull(it) }.take(7)
    }
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("اختصاراتك", "تعديل") { nav.navigate("welcome-edit") }
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(vertical = 14.dp, horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (tools.map { it as Any } + "all").chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        val t = item as? SiteTool
                        val accent = if (t != null) ToolAccent.color(t.id, b.dark) else b.primaryInk
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).pressable {
                                if (t != null) openTool(nav, ctx, t, "home_shortcut_opened") else nav.goTab("more")
                            }.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(t?.icon ?: Icons.Outlined.Apps, null, tint = accent, modifier = Modifier.size(26.dp))
                            }
                            Text(
                                t?.short ?: "كل الأدوات", style = MaterialTheme.typography.labelMedium, color = b.ink,
                                textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * «اسم الله اليوم»: one of the names listed in «الأسماء الحسنى», a new one each day, with a card to
 * share it. Only the name is shown (the list carries no explanations, and none is written here).
 */
@Composable
private fun NameOfTheDay(nav: NavHostController) {
    val b = LocalBrand.current
    val names = LocalContent.current.library.asma
    if (names.isEmpty()) return
    val (_, name) = remember { names[(LocalDate.now().toEpochDay() % names.size).toInt()] }
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("اسم الله اليوم", "كل الأسماء") { nav.navigate("asma") }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(b.goldSoft, b.surface2))).border(1.dp, b.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("من أسماء الله الحسنى", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
            Text(name, fontFamily = Fonts.naskh, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = b.ink, textAlign = TextAlign.Center)
            Row(
                Modifier.heightIn(min = 44.dp).clip(CircleShape).background(b.surface).border(1.dp, b.line, CircleShape)
                    .pressable { com.ayattla.app.studio.PendingText.value = com.ayattla.app.studio.CardText("بطاقة اسم", name, "من أسماء الله الحسنى"); nav.navigate("text-card") }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = b.primaryInk, modifier = Modifier.size(18.dp))
                Text("صمم بطاقة بالاسم", style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
            }
        }
    }
}

/**
 * «دعاء من القرآن»: one of the Quran duas listed in «الأدعية», a new one each day, shown with the
 * mushaf text and its surah and ayah, to listen to or turn into a card.
 */
@Composable
private fun QuranDuaOfTheDay(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val reciter = rememberReciter()
    val refs = content.library.duas?.quran.orEmpty()
    if (refs.isEmpty()) return
    val r = remember { refs[(LocalDate.now().toEpochDay() % refs.size).toInt()] }
    val text = remember(r) { runCatching { content.ayat(r.s, r.a, r.b).joinToString(" ") { it.text } }.getOrDefault("") }
    if (text.isEmpty()) return
    val where = "سورة ${content.surah(r.s).shortName} · " + if (r.a == r.b) "الآية ${r.a}" else "الآيات ${r.a}-${r.b}"
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("دعاء من القرآن", "كل الأدعية") { nav.navigate("duas") }
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, fontFamily = Fonts.quran, fontSize = 21.sp, lineHeight = 42.sp, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(where, style = MaterialTheme.typography.labelLarge, color = b.goldInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandButton("استمع", Modifier.weight(1f), kind = BtnKind.Secondary) { Recitation.play(ctx, content, reciter, r.s, r.a, r.b) }
                BrandButton("صمم بطاقة", Modifier.weight(1f)) { nav.navigate(Routes.studio(r.s, r.a, r.b)) }
            }
        }
    }
}

// ───────────── Next occasion ─────────────

@Composable
private fun NextOccasion(nav: NavHostController) {
    val b = LocalBrand.current
    val today = LocalDate.now()
    val next = remember(today) {
        val hy = Hijri.fromGregorian(today).year
        listOf(Triple("رمضان", 9, 1), Triple("عيد الفطر", 10, 1), Triple("يوم عرفة", 12, 9), Triple("عيد الأضحى", 12, 10), Triple("رأس السنة الهجرية", 1, 1), Triple("يوم عاشوراء", 1, 10))
            .mapNotNull { (n, m, d) ->
                listOf(hy, hy + 1).firstNotNullOfOrNull { y -> Hijri.toGregorian(Hijri.Date(y, m, d))?.takeIf { !it.isBefore(today) } }?.let { n to it }
            }.minByOrNull { it.second }
    }
    val friday = today.dayOfWeek == java.time.DayOfWeek.FRIDAY
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("المناسبات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (next != null) {
                val days = ChronoUnit.DAYS.between(today, next.second)
                Column(
                    Modifier.weight(1f).pressable { nav.navigate("occasions") }.clip(RoundedCornerShape(Radius.card))
                        .background(Brush.linearGradient(listOf(b.goldSoft, b.surface))).border(1.dp, b.accent.copy(alpha = 0.4f), RoundedCornerShape(Radius.card))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(next.first, style = MaterialTheme.typography.titleMedium, color = b.ink)
                    Text(if (days == 0L) "اليوم" else "بعد $days يوما", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = b.goldInk)
                    Text(next.second.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ar"))), style = MaterialTheme.typography.labelMedium, color = b.muted)
                }
            }
            Column(
                Modifier.weight(1f).pressable { nav.navigate(if (friday) Routes.reader(18, 1) else "sawm") }.surface(b, Surface.Tinted, 16.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (friday) "يوم الجمعة" else "صيام التطوع", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Text(if (friday) "سورة الكهف" else nextSunnahFast(today), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = b.primaryInk)
                Text(if (friday) "اقرأها الآن" else "متتبع الصيام", style = MaterialTheme.typography.labelMedium, color = b.muted)
            }
        }
    }
}

/** The next Monday/Thursday or white day (13–15 of the Hijri month). */
private fun nextSunnahFast(today: LocalDate): String {
    for (i in 1..20) {
        val d = today.plusDays(i.toLong())
        val h = runCatching { Hijri.fromGregorian(d).day }.getOrNull()
        if (h != null && h in 13..15) return if (i == 1) "غدا من الأيام البيض" else "الأيام البيض بعد $i أيام"
        if (d.dayOfWeek == java.time.DayOfWeek.MONDAY || d.dayOfWeek == java.time.DayOfWeek.THURSDAY)
            return if (i == 1) "غدا ${if (d.dayOfWeek == java.time.DayOfWeek.MONDAY) "الاثنين" else "الخميس"}" else "${if (d.dayOfWeek == java.time.DayOfWeek.MONDAY) "الاثنين" else "الخميس"} بعد $i أيام"
    }
    return "متتبع الصيام"
}

// ───────────── Customize sheet ─────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeCustomizeSheet(
    layout: List<Pair<HomeSection, Boolean>>, pinned: List<String>,
    onChange: (List<Pair<HomeSection, Boolean>>, List<String>) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit,
) {
    val b = LocalBrand.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = b.background, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Text("تخصيص الرئيسية", style = MaterialTheme.typography.titleLarge, color = b.ink, modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Outlined.Close, "إغلاق", tint = b.muted) }
            }
            Text("إعادة الترتيب الافتراضي", style = MaterialTheme.typography.labelLarge, color = b.goldInk,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape).clickable(onClick = onReset).padding(10.dp))
            Text("الأقسام", style = MaterialTheme.typography.labelLarge, color = b.muted)
            Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(vertical = 4.dp)) {
                layout.forEachIndexed { i, (s, on) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Switch(on, { v -> onChange(layout.toMutableList().also { it[i] = s to v }, pinned) }, colors = SwitchDefaults.colors(checkedTrackColor = b.green))
                        Text(s.title, style = MaterialTheme.typography.bodyLarge, color = if (on) b.ink else b.muted, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        IconButton(onClick = { if (i > 0) onChange(layout.toMutableList().also { val x = it[i]; it[i] = it[i - 1]; it[i - 1] = x }, pinned) }, enabled = i > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, "أعلى", tint = b.primaryInk)
                        }
                        IconButton(onClick = { if (i < layout.lastIndex) onChange(layout.toMutableList().also { val x = it[i]; it[i] = it[i + 1]; it[i + 1] = x }, pinned) }, enabled = i < layout.lastIndex) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "أسفل", tint = b.primaryInk)
                        }
                    }
                }
            }
            Text("أدواتي المفضلة (تظهر أولا في «أدواتي»)", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.padding(top = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SiteTool.all.forEach { t ->
                    val on = t.id in pinned
                    Chip(t.short, on, { onChange(layout, if (on) pinned - t.id else (pinned + t.id).take(8)) })
                }
            }
        }
    }
}

// ───────────── Today's prayers tracker ─────────────

/** «صلواتي اليوم»: tick each prayer once prayed; a 7-day dot strip shows the week. On the device only. */
@Composable
private fun PrayerTracker() {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("prayer.tracker", Context.MODE_PRIVATE) }
    val today = LocalDate.now()
    val five = listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
    fun key(d: LocalDate) = "d.$d"
    var done by remember { mutableStateOf(sp.getStringSet(key(today), emptySet()).orEmpty()) }
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("صلواتي اليوم")
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                five.forEach { p ->
                    val on = p.name in done
                    val bg by animateColorAsState(if (on) b.green else b.surface2, tween(220), label = "pt")
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg)
                            .border(1.dp, if (on) b.green else b.line, RoundedCornerShape(14.dp))
                            .clickable {
                                done = if (on) done - p.name else done + p.name
                                sp.edit().putStringSet(key(today), done).apply()
                                vibrate(ctx)
                            }
                            .padding(vertical = 10.dp)
                            .semantics { contentDescription = "${p.title} ${if (on) "صليت" else "لم تسجل"}" },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Check, null, tint = if (on) Color.White else b.line, modifier = Modifier.size(18.dp))
                        Text(p.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = if (on) Color.White else b.ink, maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("${done.size} من 5 اليوم", style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.padding(end = 10.dp))
                (6 downTo 0).forEach { i ->
                    val d = today.minusDays(i.toLong())
                    val n = if (i == 0) done.size else sp.getStringSet(key(d), emptySet()).orEmpty().size
                    Box(
                        Modifier.padding(start = 5.dp).size(12.dp).clip(CircleShape)
                            .background(if (n >= 5) b.green else if (n > 0) b.green.copy(alpha = 0.35f) else b.tint),
                    )
                }
            }
        }
    }
}

// ───────────── Muslim services ─────────────

@Composable
private fun MuslimServices(nav: NavHostController) {
    val b = LocalBrand.current
    data class S(val title: String, val icon: ImageVector, val tile: Int, val route: String)
    val items = listOf(
        S("المصحف", Icons.AutoMirrored.Outlined.MenuBook, 0, "tab:quran"),
        S("الاستماع", Icons.Outlined.Headphones, 1, "tab:quran"),
        S("المساجد", Icons.Outlined.Mosque, 2, "mosques"),
        S("الإذاعات", Icons.Outlined.Radio, 3, "radios"),
        S("الأدعية", Icons.Outlined.VolunteerActivism, 4, "duas"),
        S("الرقية", Icons.Outlined.Shield, 5, "ruqya"),
        S("الزكاة", Icons.Outlined.Calculate, 0, "zakat"),
        S("الأطفال", Icons.Outlined.ChildCare, 1, "kids"),
    )
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("الوصول السريع", "كل الأدوات") { nav.goTab("more") }
        Column(Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(vertical = 14.dp, horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { s ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { if (s.route.startsWith("tab:")) nav.goTab(s.route.removePrefix("tab:")) else nav.navigate(s.route) }.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IconTile(s.icon, style = TileStyle.Gradient(s.tile), size = 46.dp)
                            Text(s.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Quick dhikr ─────────────

/** A small tasbih right on the home page; shares its counts with «المسبحة». */
@Composable
private fun QuickDhikr(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val phrases = content.tasbihPhrases
    var phrase by remember { mutableStateOf(phrases.first()) }
    var count by remember(phrase) { mutableIntStateOf(prefs.tasbihCount(phrase)) }
    val pulse by animateFloatAsState(if (count % 2 == 0) 1f else 0.97f, tween(120), label = "dhikr")
    Column(Modifier.fillMaxWidth().surface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CenteredTitle("الذكر السريع", "المسبحة") { nav.navigate("tasbih") }
        Row(
            Modifier.fillMaxWidth().surface(b, Surface.Tinted, 16.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(96.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2E9145), Color(0xFF102D27))))
                    .clickable {
                        count += 1; prefs.setTasbihCount(phrase, count)
                        vibrate(ctx, strong = count % 33 == 0)
                    }
                    .semantics { contentDescription = "سبّح: $phrase، العدد $count" },
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phrase, fontFamily = Fonts.naskh, fontSize = 20.sp, color = b.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { (count % 33) / 33f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = b.accent, trackColor = b.tint, drawStopIndicator = {}, gapSize = 0.dp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    phrases.take(4).forEach { p -> Chip(p, p == phrase, { phrase = p }) }
                }
            }
        }
    }
}
