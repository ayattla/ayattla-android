package com.ayattla.app.screens

import com.ayattla.app.ui.components.softShadow
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Book
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.R
import com.ayattla.app.Routes
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.data.MediaLibrary
import com.ayattla.app.data.RemoteImages
import com.ayattla.app.data.SiteMedia
import com.ayattla.app.goTab
import com.ayattla.app.ui.components.Badge
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.CategorySelector
import com.ayattla.app.ui.components.Chevron
import com.ayattla.app.ui.components.Eyebrow
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.LauncherIcon
import com.ayattla.app.ui.components.LogoHorizontal
import com.ayattla.app.ui.components.Radius
import com.ayattla.app.ui.components.SiteSectionHead
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.featureSurface
import com.ayattla.app.ui.components.heroSurface
import com.ayattla.app.ui.components.pageHeadSurface
import com.ayattla.app.ui.components.patternedScreen
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import com.ayattla.app.ui.theme.ThemeMode
import com.ayattla.app.ui.theme.ToolAccent
import com.ayattla.app.ui.theme.tileGradient
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Home, as the iOS HomeView v4: head, content makers, today, every tool, ayah of the day, gallery, footer. */
@Composable
fun HomeScreen(nav: NavHostController) {
    val b = LocalBrand.current
    Box(Modifier.fillMaxSize().patternedScreen(b, strong = true)) {
        Column(Modifier.fillMaxSize()) {
            HomeBar(nav)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                HomeHead(nav)
                HomeCreateBento(nav)
                HomeToday(nav)
                HomeToolLauncher(nav)
                HomeAyahCard(nav)
                HomeMediaGallery(nav)
                HomeFooter(nav)
            }
        }
    }
}

@Composable
private fun HomeBar(nav: NavHostController) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 8.dp)) {
        BarIcon(Icons.Outlined.Settings, "الإعدادات", Modifier.align(Alignment.CenterEnd)) { nav.navigate("settings") }
        LogoHorizontal(Modifier.align(Alignment.Center).height(30.dp))
        BarIcon(if (b.dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, if (b.dark) "الوضع الفاتح" else "الوضع الداكن", Modifier.align(Alignment.CenterStart)) {
            prefs.setTheme(if (b.dark) ThemeMode.Light else ThemeMode.Dark)
        }
    }
}

@Composable
private fun BarIcon(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = LocalBrand.current.primaryInk, modifier = Modifier.size(24.dp))
    }
}

// ───────────── Head ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHead(nav: NavHostController) {
    val b = LocalBrand.current
    val today = LocalDate.now()
    val hour = LocalTime.now().hour
    val (greeting, icon) = when (hour) {
        in 4..11 -> "صباح الخير" to Icons.Outlined.WbTwilight
        in 12..16 -> "طاب يومك" to Icons.Outlined.WbSunny
        else -> "مساء الخير" to Icons.Outlined.NightsStay
    }
    Column(Modifier.fillMaxWidth().pageHeadSurface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(b.goldSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = b.goldInk, modifier = Modifier.size(17.dp))
            }
            Text(greeting, style = MaterialTheme.typography.labelLarge, color = b.primaryInk, modifier = Modifier.weight(1f))
            Text(
                "مجاني · بدون تسجيل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.primaryInk,
                modifier = Modifier.clip(CircleShape).background(b.tint).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Text("ماذا تريد اليوم؟", style = MaterialTheme.typography.headlineMedium, color = b.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DateLabel(Icons.Outlined.NightsStay, Hijri.format(today))
            DateLabel(Icons.Outlined.CalendarMonth, today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("ar"))))
        }
        // The tool search: opens «المزيد» with the keyboard up.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .pressable { nav.navigate("more?focus=1") }
                .softShadow(RoundedCornerShape(16.dp), 10.dp, b.shadow.copy(alpha = 0.06f), DpOffset(0.dp, 4.dp))
                .clip(RoundedCornerShape(16.dp)).background(b.surface).border(1.dp, b.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Search, null, tint = b.primaryInk)
            Text("ابحث عن أداة: الأذكار، القبلة، الزكاة…", style = MaterialTheme.typography.bodyMedium, color = b.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DateLabel(icon: ImageVector, text: String) {
    val b = LocalBrand.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = b.muted, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = b.muted)
    }
}

// ───────────── Content makers ─────────────

/** Curated photos and videos from the site's library, loaded once per launch (iOS FeaturedMedia). */
private object FeaturedMedia {
    var videos: List<SiteMedia> = emptyList()
    var images: List<SiteMedia> = emptyList()
    suspend fun load(c: android.content.Context) {
        if (videos.isNotEmpty() || images.isNotEmpty()) return
        videos = MediaLibrary.curated(MediaLibrary.search(c, "video", "mosques"))
        images = MediaLibrary.curated(MediaLibrary.search(c, "image", "mosques")) + MediaLibrary.curated(MediaLibrary.search(c, "image", "night"))
    }
}

@Composable
private fun rememberFeatured(): Pair<List<SiteMedia>, List<SiteMedia>> {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf(FeaturedMedia.videos to FeaturedMedia.images) }
    LaunchedEffect(Unit) { FeaturedMedia.load(ctx); state = FeaturedMedia.videos to FeaturedMedia.images }
    return state
}

@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, maxWidth: Int = 900) {
    val ctx = LocalContext.current
    var bmp by remember(url) { mutableStateOf<Bitmap?>(url?.let { RemoteImages.cached(it) }) }
    LaunchedEffect(url) { if (url != null && bmp == null) bmp = RemoteImages.load(ctx, url, maxWidth) }
    bmp?.let { Image(it.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop) }
}

/** A shortcut with a real photo behind it (or the tile gradient while it loads), a dark fade and an icon chip. */
@Composable
private fun MediaShortcut(
    title: String, subtitle: String, icon: ImageVector, media: SiteMedia?, gradient: Int, height: Dp,
    modifier: Modifier = Modifier, big: Boolean = false, badge: String? = null, onClick: () -> Unit,
) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(if (big) Radius.hero else Radius.card)
    Box(
        modifier.fillMaxWidth().height(height).pressable(onClick)
            .softShadow(shape, 14.dp, b.shadow.copy(alpha = 0.16f), DpOffset(0.dp, 8.dp))
            .clip(shape).background(Brush.linearGradient(tileGradient(gradient))),
    ) {
        RemoteImage(media?.still, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.62f)))))
        Column(Modifier.fillMaxSize().padding(if (big) 18.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (big) 8.dp else 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val s = if (big) 42.dp else 34.dp
                Box(
                    Modifier.size(s).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (big) 22.dp else 18.dp)) }
                Spacer(Modifier.weight(1f))
                if (badge != null) Badge(badge, gold = true)
            }
            Spacer(Modifier.weight(1f))
            Text(title, style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 2)
            Text(subtitle, style = if (big) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 2)
        }
    }
}

@Composable
private fun HomeCreateBento(nav: NavHostController) {
    val b = LocalBrand.current
    val (videos, images) = rememberFeatured()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SiteSectionHead("ابدأ صناعة المحتوى", eyebrow = "اصنع", subtitle = "مقاطع وبطاقات وحالات بخلفيات حقيقية، جاهزة للنشر")
        MediaShortcut(
            "مقطع تلاوة بالفيديو", "تظليل الكلمة مع التلاوة وأكثر من ٣٠ قارئا، بخلفية فيديو حقيقية",
            Icons.Filled.SmartDisplay, videos.firstOrNull(), 0, 210.dp, big = true, badge = "MP4",
        ) { com.ayattla.app.studio.PendingMedia.media = videos.firstOrNull(); nav.navigate(Routes.studioMode(video = true)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaShortcut("بطاقة آية", "صورة للمنشورات والحالة", Icons.Filled.ImageIcon, images.firstOrNull(), 1, 190.dp, Modifier.weight(1f)) {
                com.ayattla.app.studio.PendingMedia.media = images.firstOrNull(); nav.navigate(Routes.studioMode(video = false))
            }
            MediaShortcut("حالة واتساب", "صورة أو فيديو أو نص متحرك", Icons.Filled.Forum, images.drop(1).firstOrNull(), 2, 190.dp, Modifier.weight(1f)) {
                nav.navigate("status")
            }
        }
        Row(
            Modifier.fillMaxWidth().pressable { nav.navigate("hadith") }
                .clip(RoundedCornerShape(Radius.card)).background(b.goldSoft).border(1.dp, b.accent.copy(alpha = 0.35f), RoundedCornerShape(Radius.card))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(Icons.Outlined.Book, style = TileStyle.Gold, size = 50.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("بطاقة حديث", style = MaterialTheme.typography.titleMedium, color = b.ink)
                    Badge("جديد", gold = true)
                }
                Text("حديث صحيح أو حسن مع الراوي والمصدر والحكم", style = MaterialTheme.typography.bodySmall, color = b.muted)
            }
            Chevron()
        }
    }
}

// ───────────── Today ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeToday(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SiteSectionHead("الأدوات اليومية", eyebrow = "يومك", subtitle = "الصلاة والأذكار والورد في مكان واحد")
        NextPrayerCard(nav)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val lr = prefs.lastRead?.split(":")?.mapNotNull { it.toIntOrNull() }
            val page = lr?.takeIf { it.size == 2 }?.let { content.pageOf(it[0], it[1]) } ?: 1
            val name = lr?.takeIf { it.size == 2 }?.let { content.surah(it[0]).shortName } ?: content.surah(1).shortName
            TodayCard(
                Icons.Outlined.AutoStories, 0, if (lr != null) "تابع القراءة" else "ابدأ القراءة", "$name · صفحة $page",
                Modifier.weight(1f), progress = page / 604f,
            ) { if (lr != null && lr.size == 2) nav.navigate(Routes.reader(lr[0], lr[1])) else nav.goTab("quran") }
            TodayCard(Icons.Outlined.WbTwilight, 2, "أذكار اليوم", "الصباح والمساء والنوم مع العداد", Modifier.weight(1f)) { nav.goTab("azkar") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DailyChip("المسبحة", Icons.Outlined.TouchApp) { nav.navigate("tasbih") }
            DailyChip("القبلة", Icons.Outlined.Explore) { nav.navigate("qibla") }
            DailyChip("الأدعية", Icons.Outlined.VolunteerActivism) { nav.navigate("duas") }
            DailyChip("التقويم الهجري", Icons.Outlined.CalendarMonth) { nav.navigate("hijri") }
            DailyChip("الختمة", Icons.AutoMirrored.Outlined.MenuBook) { nav.navigate("khatma") }
            DailyChip("الإذاعات", Icons.Outlined.Radio) { nav.navigate("radios") }
        }
    }
}

@Composable
private fun TodayCard(icon: ImageVector, tile: Int, title: String, line: String, modifier: Modifier, progress: Float? = null, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(
        modifier.heightIn(min = 150.dp).pressable(onClick).surface(b).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon, style = TileStyle.Gradient(tile), size = 44.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink)
        Text(line, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted)
        if (progress != null) LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = b.green, trackColor = b.tint, drawStopIndicator = {}, gapSize = 0.dp,
        )
    }
}

@Composable
private fun DailyChip(title: String, icon: ImageVector, onClick: () -> Unit) {
    val b = LocalBrand.current
    Row(
        Modifier.heightIn(min = 44.dp).pressable(onClick).clip(CircleShape).background(b.surface).border(1.dp, b.line, CircleShape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = b.primaryInk, modifier = Modifier.size(17.dp))
        Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = b.ink)
    }
}

/** Next prayer: green to teal panel with the qibla button (iOS NextPrayerCard). */
@Composable
fun NextPrayerCard(nav: NavHostController) {
    val b = LocalBrand.current
    val today = LocalDate.now()
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }
    val loc = rememberPrayerLocation()
    val next = remember(loc, now.hour, now.minute) {
        val t = loc.times(today)
        val t2 = loc.times(today.plusDays(1))
        PrayerTimes.next(now.hour * 60 + now.minute, t, t2) to t
    }
    val time = PrayerTimes.hm12(if (next.first.isTomorrow) next.second.fajr else next.second.hours(next.first.prayer))
    Row(
        Modifier.fillMaxWidth().featureSurface(b, Radius.hero).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).clickable { nav.navigate("prayer") }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("الصلاة القادمة", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Color.White.copy(alpha = 0.85f))
            Text("${next.first.prayer.title} · $time", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("${loc.name} · بعد ${Num.duration(next.first.minutesUntil)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
        }
        Column(
            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)).clickable { nav.navigate("qibla") },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Explore, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text("القبلة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
        }
    }
}

// ───────────── Tool launcher ─────────────

/** Every tool, by section (as before), each section a framed panel with its own colour band. */
@Composable
private fun HomeToolLauncher(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    data class G(val icon: ImageVector, val top: Color, val bottom: Color)
    val look = mapOf(
        ToolGroup.Content to G(Icons.Filled.SmartDisplay, Color(0xFF1F7A4E), Color(0xFF0E3B2C)),
        ToolGroup.Quran to G(Icons.Outlined.Book, Color(0xFF2A5C8A), Color(0xFF13304D)),
        ToolGroup.Daily to G(Icons.Outlined.WbSunny, Color(0xFFB0832F), Color(0xFF6B4A12)),
        ToolGroup.Hadith to G(Icons.Outlined.School, Color(0xFF6B3F8E), Color(0xFF3A1F55)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SiteSectionHead("أقسام الأدوات", eyebrow = "الأدوات", subtitle = "${SiteTool.all.size} أداة مجانية في ${ToolGroup.entries.size} أقسام")
        ToolGroup.entries.forEach { g ->
            val tools = SiteTool.all.filter { it.group == g }
            if (tools.isEmpty()) return@forEach
            val l = look[g] ?: G(Icons.Outlined.Apps, b.green, b.primary)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).surface(b)) {
                // Colour band with the section's icon, name and count.
                Row(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(l.top, l.bottom))).clickable { nav.navigate("more?group=${g.name}") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFFE3CB8E).copy(alpha = 0.55f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                        Icon(l.icon, null, tint = Color(0xFFF3DFA2), modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Start)
                        Text("${tools.size} أداة", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF3DFA2), textAlign = TextAlign.Start)
                    }
                    Text("عرض الكل", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                    Chevron(Modifier.size(16.dp), Color.White.copy(alpha = 0.9f))
                }
                // Makers first, large, in the content section.
                if (g == ToolGroup.Content) {
                    val makers = tools.filter { it.id in SiteTool.featured }
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        makers.forEachIndexed { i, t ->
                            Column(
                                Modifier.weight(1f).pressable { openTool(nav, ctx, t) }.clip(RoundedCornerShape(18.dp))
                                    .background(Brush.verticalGradient(listOf(listOf(Color(0xFF1F7A4E), Color(0xFF2A5C8A), Color(0xFFB0832F))[i % 3], Color(0xFF0E2A20))))
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(t.icon, null, tint = Color(0xFFF3DFA2), modifier = Modifier.size(28.dp))
                                Text(t.short, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
                val rest = if (g == ToolGroup.Content) tools.filter { it.id !in SiteTool.featured } else tools
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rest.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { t ->
                                LauncherIcon(t.short, t.icon, TileStyle.Accent(ToolAccent.color(t.id, b.dark)), Modifier.weight(1f), isNew = t.isNew, dimmed = t.dimmedOffline()) { openTool(nav, ctx, t) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupTitle(title: String, count: Int, onAll: (() -> Unit)?) {
    val b = LocalBrand.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Brush.linearGradient(b.siteGradient)))
        Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink)
        Text(
            "$count", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.primaryInk,
            modifier = Modifier.clip(CircleShape).background(b.tint).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        if (onAll != null) Row(
            Modifier.heightIn(min = 44.dp).clickable(onClick = onAll), verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("عرض الكل", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = b.primaryInk)
            Chevron(Modifier.size(16.dp), b.primaryInk)
        }
    }
}

// ───────────── Ayah of the day ─────────────

@Composable
private fun HomeAyahCard(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val pick = remember(today) { DailyAyah.pick(today) }
    val ayat = remember(pick) { content.ayat(pick.surah, pick.from, pick.to) }
    val surah = content.surah(pick.surah)
    val ref = "${surah.shortName} · " + if (pick.from == pick.to) "${pick.from}" else "${pick.from}–${pick.to}"
    val text = ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }
    val key = "${pick.surah}:${pick.from}"
    Box(Modifier.padding(vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth().heroSurface(b, 30.dp).padding(15.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("آية اليوم", onDark = true)
                Spacer(Modifier.weight(1f))
                Text(ref, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = b.onDeepAccent)
                BarIconOn(Icons.Outlined.Share, "مشاركة") { shareText(ctx, "﴿${ayat.joinToString(" ") { it.text }}﴾ [$ref]\nayattla.com") }
                BarIconOn(if (key in prefs.favorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "المفضلة") { prefs.toggleFavorite(key) }
            }
            Text(
                text, fontFamily = Fonts.quran, fontSize = 24.sp, lineHeight = 46.sp, color = Color(0xFFF6F2E9),
                textAlign = TextAlign.Center, maxLines = 7, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(b.onDeepAccent)
                        .clickable { nav.navigate(Routes.reader(pick.surah, pick.from)) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = b.primary, modifier = Modifier.size(17.dp))
                    Text("اقرأ في المصحف", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF102D27))
                }
                Row(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, b.onDeepAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable { nav.navigate(Routes.studio(pick.surah, pick.from, pick.to, video = true)) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Outlined.SmartDisplay, null, tint = Color(0xFFF6F2E9), modifier = Modifier.size(17.dp))
                    Text("اصنع مقطعا", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFF6F2E9))
                }
            }
        }
        FloatChip("تظليل الكلمة مع التلاوة", Icons.Outlined.CheckCircle, Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = (-16).dp))
        FloatChip("+٣٠ قارئا", Icons.Outlined.GraphicEq, Modifier.align(Alignment.BottomStart).offset(x = 10.dp, y = 16.dp))
    }
}

@Composable
private fun BarIconOn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color(0xFFF6F2E9), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun FloatChip(text: String, icon: ImageVector, modifier: Modifier) {
    val b = LocalBrand.current
    Row(
        modifier.softShadow(CircleShape, 10.dp, b.shadow.copy(alpha = 0.18f), DpOffset(0.dp, 6.dp))
            .clip(CircleShape).background(b.surface).border(1.dp, b.line, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = b.green, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink)
    }
}

// ───────────── Gallery ─────────────

@Composable
private fun HomeMediaGallery(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val cats = listOf("mosques" to "مساجد", "sky" to "سماء", "night" to "ليل", "nature" to "طبيعة")
    var cat by remember { mutableStateOf("mosques") }
    var items by remember { mutableStateOf<List<SiteMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(cat) {
        loading = true
        val v = MediaLibrary.curated(MediaLibrary.search(ctx, "video", cat))
        val i = MediaLibrary.curated(MediaLibrary.search(ctx, "image", cat))
        items = MediaLibrary.mix(v, i)
        loading = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SiteSectionHead("خلفيات ومقاطع حقيقية", eyebrow = "المعرض", subtitle = "اضغط أي خلفية لتبدأ بها")
        CategorySelector(cats, cat, { cat = it }, Modifier.padding(horizontal = 0.dp), contentPadding = 0.dp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            if (items.isEmpty()) repeat(5) { i ->
                val tall = i % 3 == 0
                Box(Modifier.width(if (tall) 132.dp else 118.dp).height(if (tall) 236.dp else 158.dp).clip(RoundedCornerShape(18.dp)).background(b.surface2))
            }
            items.forEach { m ->
                val tall = m.isVideo
                Box(
                    Modifier.width(if (tall) 132.dp else 118.dp).height(if (tall) 236.dp else 158.dp)
                        .pressable { com.ayattla.app.studio.PendingMedia.media = m; nav.navigate(Routes.studioMode(video = m.isVideo)) }
                        .clip(RoundedCornerShape(18.dp)).background(b.surface2),
                ) {
                    RemoteImage(m.thumbUrl ?: m.previewUrl, Modifier.fillMaxSize(), maxWidth = 400)
                    if (m.isVideo) Box(
                        Modifier.align(Alignment.BottomStart).padding(8.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        if (!loading && items.isEmpty()) Text(if (!com.ayattla.app.data.Net.isOnline) "المعرض يحتاج إنترنت، ويعرض ما فتحته من قبل." else "تعذر تحميل المعرض الآن. تحقق من الاتصال", style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

// ───────────── Footer ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeFooter(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().surface(b)) {
            FooterRow("عن آيات تتلى", Icons.Outlined.Info, 0) { nav.navigate("about") }
            FooterRow("شارك التطبيق", Icons.Outlined.Share, 6, chevron = false, divider = false) {
                shareText(ctx, "آيات تتلى: مقاطع وبطاقات قرآنية وأدوات يومية، مجانا وبلا تسجيل\nhttps://ayattla.com")
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.hero)).background(b.surface2).padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painterResource(if (b.dark) R.drawable.logo_full_light else R.drawable.logo_full), null,
                Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)).background(b.surface).border(1.dp, b.line, RoundedCornerShape(20.dp)).padding(10.dp),
            )
            Text("صانع مقاطع وبطاقات قرآنية مجاني، صدقة جارية لكل من شارك", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = b.ink, textAlign = TextAlign.Center)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("دليل الاستخدام" to "/guide", "المصادر والتراخيص" to "/sources", "منهجية التوثيق" to "/methodology", "الخصوصية" to "/privacy", "الشروط" to "/terms", "تواصل معنا" to "/contact").forEach { (t, p) ->
                    Box(
                        Modifier.heightIn(min = 34.dp).clip(CircleShape).background(b.surface).border(1.dp, b.line, CircleShape)
                            .clickable { openUrl(ctx, "https://ayattla.com$p") }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(t, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink) }
                }
            }
            Text("© آيات تتلى · ayattla.com", style = MaterialTheme.typography.labelSmall, color = b.muted)
        }
    }
}

@Composable
private fun FooterRow(title: String, icon: ImageVector, tile: Int, chevron: Boolean = true, divider: Boolean = true, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, style = TileStyle.Gradient(tile), size = 36.dp)
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = b.ink, modifier = Modifier.weight(1f))
            if (chevron) Chevron()
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(0.5.dp).background(b.line))
    }
}

fun openUrl(c: android.content.Context, url: String) {
    runCatching { c.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// Public entry points for the v5 home's optional sections.
@Composable fun HomeAyahPublic(nav: NavHostController) = HomeAyahCard(nav)
@Composable fun HomeCreatePublic(nav: NavHostController) = HomeCreateBento(nav)
@Composable fun HomeGalleryPublic(nav: NavHostController) = HomeMediaGallery(nav)
@Composable fun HomeAllToolsPublic(nav: NavHostController) = HomeToolLauncher(nav)
@Composable fun HomeFooterPublic(nav: NavHostController) = HomeFooter(nav)
