package com.ayattla.app.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.calc.Hijri
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.StatusOccasion
import com.ayattla.app.screens.copyText
import com.ayattla.app.screens.shareText
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import java.time.DayOfWeek
import java.time.LocalDate

/** How each occasion looks: its group, icon and the card design it starts with. */
private data class Look(val group: String, val icon: ImageVector, val palette: CardStyle, val ornament: Ornament)

private val looks = mapOf(
    "jumuah" to Look("المناسبات", Icons.Outlined.Mosque, CardStyle.Emerald, Ornament.Arch),
    "ramadan" to Look("المناسبات", Icons.Outlined.NightsStay, CardStyle.Violet, Ornament.Lantern),
    "laylat-al-qadr" to Look("المناسبات", Icons.Outlined.Star, CardStyle.Royal, Ornament.Lantern),
    "eid-al-fitr" to Look("المناسبات", Icons.Outlined.Celebration, CardStyle.Teal, Ornament.Bands),
    "eid-al-adha" to Look("المناسبات", Icons.Outlined.Celebration, CardStyle.Burgundy, Ornament.Bands),
    "arafah" to Look("المناسبات", Icons.Outlined.Landscape, CardStyle.Sand, Ornament.Arch),
    "dhul-hijjah" to Look("المناسبات", Icons.Outlined.Landscape, CardStyle.Sand, Ornament.Medallion),
    "hajj" to Look("المناسبات", Icons.Outlined.Mosque, CardStyle.Ivory, Ornament.Arch),
    "ashura" to Look("المناسبات", Icons.Outlined.Spa, CardStyle.Night, Ornament.Classic),
    "marriage" to Look("التهنئة والدعاء", Icons.Outlined.Favorite, CardStyle.Blush, Ornament.Corners),
    "newborn" to Look("التهنئة والدعاء", Icons.Outlined.ChildFriendly, CardStyle.Sky, Ornament.Corners),
    "success" to Look("التهنئة والدعاء", Icons.Outlined.EmojiEvents, CardStyle.Royal, Ornament.Medallion),
    "healing" to Look("التهنئة والدعاء", Icons.Outlined.Healing, CardStyle.Teal, Ornament.Classic),
    "condolence" to Look("التهنئة والدعاء", Icons.Outlined.VolunteerActivism, CardStyle.Charcoal, Ornament.Classic),
    "sadaqah-jariyah" to Look("التهنئة والدعاء", Icons.Outlined.VolunteerActivism, CardStyle.Olive, Ornament.Medallion),
    "morning" to Look("اليومية", Icons.Outlined.WbSunny, CardStyle.Sand, Ornament.Classic),
    "evening" to Look("اليومية", Icons.Outlined.WbTwilight, CardStyle.Night, Ornament.Medallion),
    "sleep" to Look("اليومية", Icons.Outlined.Bedtime, CardStyle.Royal, Ornament.Classic),
    "after-prayer" to Look("اليومية", Icons.Outlined.SelfImprovement, CardStyle.Emerald, Ornament.Classic),
    "istighfar" to Look("اليومية", Icons.Outlined.Spa, CardStyle.Olive, Ornament.Corners),
    "relief" to Look("اليومية", Icons.Outlined.ThumbUp, CardStyle.Teal, Ornament.Medallion),
    "travel" to Look("اليومية", Icons.Outlined.FlightTakeoff, CardStyle.Sky, Ornament.Classic),
    "rain" to Look("اليومية", Icons.Outlined.Umbrella, CardStyle.Sky, Ornament.Medallion),
    "hadith" to Look("محتوى جاهز", Icons.Outlined.Book, CardStyle.Emerald, Ornament.Classic),
)
private val defaultLook = Look("المزيد", Icons.Outlined.Cloud, CardStyle.Emerald, Ornament.Classic)
private fun look(slug: String) = looks[slug] ?: defaultLook
private val groupOrder = listOf("المناسبات", "التهنئة والدعاء", "اليومية", "محتوى جاهز", "المزيد")

/** The occasion that fits today (Friday, Ramadan, the ten days, Arafah, the Eids, Ashura). */
fun todaysOccasion(today: LocalDate = LocalDate.now()): String? {
    val h = runCatching { Hijri.fromGregorian(today) }.getOrNull()
    if (h != null) {
        when {
            h.month == 10 && h.day <= 3 -> return "eid-al-fitr"
            h.month == 12 && h.day == 9 -> return "arafah"
            h.month == 12 && h.day in 10..13 -> return "eid-al-adha"
            h.month == 12 && h.day <= 8 -> return "dhul-hijjah"
            h.month == 9 && h.day >= 21 -> return "laylat-al-qadr"
            h.month == 9 -> return "ramadan"
            h.month == 1 && h.day in 9..10 -> return "ashura"
        }
    }
    if (today.dayOfWeek == DayOfWeek.FRIDAY || today.dayOfWeek == DayOfWeek.THURSDAY) return "jumuah"
    return null
}

/** Opens the card maker on a status text, sized for a status, with the occasion's design. */
fun openStatusCard(nav: NavHostController, content: ContentStore, occ: StatusOccasion, item: StatusOccasion.Item) {
    val l = look(occ.slug)
    val ref = item.ayah
    val card = if (ref != null) {
        val ayat = runCatching { content.ayat(ref.s).filter { it.ayah in ref.a..ref.b } }.getOrDefault(emptyList())
        CardText(
            occ.title, ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }.ifBlank { item.text },
            item.ref, naskh = false, ratio = CardRatio.Story, header = occ.title, style = l.palette, ornament = l.ornament, colourFirst = true,
        )
    } else {
        CardText(occ.title, item.text, item.ref.ifBlank { item.src }, naskh = true, ratio = CardRatio.Story,
            header = occ.title, style = l.palette, ornament = l.ornament, colourFirst = true)
    }
    PendingText.value = card
    nav.navigate("text-card")
}

/** «حالات واتساب»: today's status, then the occasions by group (as ayattla.com/status/). */
@Composable
fun StatusScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val all = remember { content.library.status }
    val todaySlug = remember { todaysOccasion() }
    val today = all.firstOrNull { it.slug == todaySlug }
    val groups = remember(all) { all.groupBy { look(it.slug).group }.toList().sortedBy { groupOrder.indexOf(it.first).let { i -> if (i < 0) 99 else i } } }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("حالات واتساب", onBack = { nav.popBackStack() })
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = LocalBottomInset.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("صمم حالتك بنص موثق", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = b.ink, textAlign = TextAlign.Center)
                    Text("آيات وأحاديث صحيحة وأذكار لكل مناسبة، بمصادرها، بمقاس الحالة 9:16", style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
                }
            }
            if (today != null && today.items.isNotEmpty()) item { TodayStatus(nav, content, today) }
            if (all.isEmpty()) item { EmptyState("المحتوى غير متاح", "حدّث التطبيق لتصلك حالات المناسبات", icon = Icons.Outlined.AutoStories) }
            groups.forEach { (g, list) ->
                item(key = "g-$g") { GroupTitle(g) }
                list.chunked(2).forEach { row ->
                    item(key = "r-" + row.joinToString { it.slug }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { o -> OccasionTile(o, Modifier.weight(1f)) { nav.navigate("status/${o.slug}") } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item { GroupTitle("صمم من البداية") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StartTile("آية", Icons.AutoMirrored.Outlined.MenuBook, 0, Modifier.weight(1f)) { nav.navigate(Routes.studioMode(video = false)) }
                    StartTile("مقطع متحرك", Icons.Outlined.AutoStories, 1, Modifier.weight(1f)) { nav.navigate(Routes.studioMode(video = true)) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StartTile("ذكر", Icons.Outlined.WbSunny, 2, Modifier.weight(1f)) { nav.navigate("azkar-all") }
                    StartTile("حديث", Icons.Outlined.Book, 3, Modifier.weight(1f)) { nav.navigate("hadith") }
                }
            }
            item {
                Text("لا ننشر دعاء أو عبارة دينية بلا مصدر. النصوص كما في الموقع.", style = MaterialTheme.typography.bodySmall, color = b.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GroupTitle(t: String) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(1.dp)).background(b.accent))
        Text(t, style = MaterialTheme.typography.titleLarge, color = b.ink)
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(1.dp)).background(b.accent))
    }
}

@Composable
private fun TodayStatus(nav: NavHostController, content: ContentStore, occ: StatusOccasion) {
    val l = look(occ.slug)
    val item = occ.items.first()
    val text = item.ayah?.let { r -> runCatching { content.ayat(r.s).filter { it.ayah in r.a..r.b }.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) } }.getOrNull() } ?: item.text
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(l.palette.top), Color(l.palette.bottom))))
            .border(1.dp, Color(l.palette.accent).copy(alpha = 0.5f), RoundedCornerShape(26.dp))
            .clickable { nav.navigate("status/${occ.slug}") }.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("حالة اليوم · ${occ.title}", style = MaterialTheme.typography.labelLarge, color = Color(l.palette.accent))
        Text(text, fontFamily = if (item.ayah != null) Fonts.quran else Fonts.naskh, fontSize = 20.sp, lineHeight = 38.sp,
            color = Color(l.palette.text), textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis)
        Text(item.ref, style = MaterialTheme.typography.labelMedium, color = Color(l.palette.accent), textAlign = TextAlign.Center, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("صممها حالة", Icons.Outlined.Palette, Color(l.palette.accent), Color(l.palette.top)) { openStatusCard(nav, content, occ, item) }
            Pill("كل نصوص ${occ.title}", Icons.Outlined.AutoStories, Color(l.palette.text).copy(alpha = 0.16f), Color(l.palette.text)) { nav.navigate("status/${occ.slug}") }
        }
    }
}

@Composable
private fun Pill(text: String, icon: ImageVector, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

@Composable
private fun OccasionTile(o: StatusOccasion, modifier: Modifier, onClick: () -> Unit) {
    val b = LocalBrand.current
    val l = look(o.slug)
    val tint = o.color?.let { Color(it) } ?: b.green
    Column(
        modifier.heightIn(min = 120.dp).pressable(onClick).surface(b).border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp).semantics { contentDescription = "حالات ${o.title}، ${o.items.size} نصا" },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(l.palette.top), Color(l.palette.bottom)))), contentAlignment = Alignment.Center) {
            Icon(l.icon, null, tint = Color(l.palette.accent), modifier = Modifier.size(24.dp))
        }
        Text(o.title, style = MaterialTheme.typography.titleSmall, color = b.ink, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${o.items.size} نصا جاهزا", style = MaterialTheme.typography.labelSmall, color = tint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StartTile(title: String, icon: ImageVector, tile: Int, modifier: Modifier, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(modifier.pressable(onClick).surface(b).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconTile(icon, style = TileStyle.Gradient(tile), size = 42.dp)
        Text(title, style = MaterialTheme.typography.titleSmall, color = b.ink, textAlign = TextAlign.Center)
    }
}

/** One occasion: every text with its type, reference and source, and «صممها حالة» under each. */
@Composable
fun StatusOccasionScreen(nav: NavHostController, slug: String) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val occ = remember(slug) { content.library.status.firstOrNull { it.slug == slug } }
    var filter by rememberSaveable { mutableStateOf("الكل") }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(occ?.title?.let { "حالات $it" } ?: "حالات واتساب", onBack = { nav.popBackStack() })
        if (occ == null) { EmptyState("غير موجود", "ارجع واختر مناسبة أخرى", icon = Icons.Outlined.AutoStories); return }
        val l = look(occ.slug)
        val kinds = listOf("الكل") + occ.items.map { it.tag }.distinct()
        val shown = occ.items.filter { filter == "الكل" || it.tag == filter }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = LocalBottomInset.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (occ.intro.isNotBlank()) item {
                Text(occ.intro, style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            if (kinds.size > 2) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    kinds.forEach { k -> Chip(k, k == filter, { filter = k }) }
                }
            }
            items(shown, key = { it.text.hashCode().toString() + it.ref }) { item ->
                val text = item.ayah?.let { r -> remember(r) { runCatching { content.ayat(r.s).filter { it.ayah in r.a..r.b }.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) } }.getOrNull() } } ?: item.text
                var open by remember { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxWidth().surface(b)
                        .border(width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(l.palette.accent).copy(alpha = 0.7f)))
                    Text(item.tag, style = MaterialTheme.typography.labelMedium, color = b.primaryInk,
                        modifier = Modifier.clip(CircleShape).background(b.tint).padding(horizontal = 12.dp, vertical = 4.dp))
                    Text(text, fontFamily = if (item.ayah != null) Fonts.quran else Fonts.naskh, fontSize = 20.sp, lineHeight = 38.sp, color = b.ink, textAlign = TextAlign.Center)
                    if (item.ref.isNotBlank()) Text(item.ref, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted, textAlign = TextAlign.Center)
                    if (item.src.isNotBlank()) Text(item.src, style = MaterialTheme.typography.labelSmall, color = b.muted, textAlign = TextAlign.Center)
                    if (item.sharh.isNotBlank()) {
                        Text(if (open) "إخفاء الشرح" else "شرح مختصر", style = MaterialTheme.typography.labelLarge, color = b.primaryInk,
                            modifier = Modifier.clip(CircleShape).clickable { open = !open }.padding(horizontal = 10.dp, vertical = 4.dp))
                        if (open) Text(item.sharh, style = MaterialTheme.typography.bodyMedium, color = b.ink, textAlign = TextAlign.Center)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("صممها حالة", Icons.Outlined.Palette, b.green, Color.White) { openStatusCard(nav, content, occ, item) }
                        Pill("نسخ", Icons.Outlined.ContentCopy, b.surface2, b.ink) { copyText(ctx, listOf(text, item.ref).filter { it.isNotBlank() }.joinToString("\n")) }
                        Pill("مشاركة", Icons.Outlined.Share, b.surface2, b.ink) { shareText(ctx, listOf(text, item.ref).filter { it.isNotBlank() }.joinToString("\n")) }
                    }
                }
            }
        }
    }
}
