package com.ayattla.app.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.ui.components.Badge
import com.ayattla.app.ui.components.CategorySelector
import com.ayattla.app.ui.components.Chevron
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.SectionHeader
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.pageHeadSurface
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.SmallShape
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import java.time.LocalDate

/** «الأذكار والحديث» tab, as the iOS AzkarHadithView: head band, today's four chapters, grouped rows. */
@Composable
fun AzkarListScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val cats = content.azkarCategories
    val pinned = listOfNotNull(
        cats.firstOrNull { "الصباح" in it.title },
        cats.firstOrNull { "المساء" in it.title },
        cats.firstOrNull { "النوم" in it.title && "الاستيقاظ" !in it.title },
        cats.firstOrNull { "الاستيقاظ" in it.title },
    ).distinctBy { it.id }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("الأذكار والحديث", onBack = if (nav.previousBackStackEntry?.destination?.route.let { it != null && it != "home" }) ({ nav.popBackStack() }) else null)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = LocalBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.fillMaxWidth().pageHeadSurface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge("حصن المسلم والسنة")
                Text("الأذكار والحديث", style = MaterialTheme.typography.headlineSmall, color = b.ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("أذكار اليوم")
                pinned.chunked(2).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { i, c ->
                            Row(
                                Modifier.weight(1f).heightIn(min = 66.dp).pressable { nav.navigate(Routes.azkar(c.id)) }.surface(b).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IconTile(azkarIcon(c.title), style = TileStyle.Gradient(r * 2 + i + 1), size = 42.dp)
                                Text(c.title, style = MaterialTheme.typography.labelLarge, color = b.ink)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            AzkarWeekLog(pinned.take(2))
            HubGroup("الأذكار والأدعية", "من حصن المسلم ومن القرآن والسنة، مع المصدر") {
                HubRow("حصن المسلم", "أبواب الأذكار والأدعية كاملة مع العداد", Icons.Outlined.Shield, 0) { nav.navigate("azkar-all") }
                HubRow("الأذكار", "أذكار الصباح والمساء والنوم مع العداد", Icons.Outlined.WbSunny, 1) { pinned.firstOrNull()?.let { nav.navigate(Routes.azkar(it.id)) } }
                HubRow("أدعية من القرآن والسنة", "مصنفة حسب الحاجة مع المصدر", Icons.Outlined.VolunteerActivism, 2) { nav.navigate("duas") }
                HubRow("الرقية الشرعية", "آيات الرقية من القرآن تتلى متتابعة", Icons.Outlined.PlayCircle, 3) { nav.navigate("ruqya") }
                HubRow("المسبحة الإلكترونية", "عداد تسبيح يحفظ العدد على جهازك", Icons.Outlined.TouchApp, 4) { nav.navigate("tasbih") }
                HubRow("أسماء الله الحسنى", "بطاقة لكل اسم من الأسماء الحسنى", Icons.Outlined.AutoAwesome, 0, divider = false) { nav.navigate("asma") }
            }
            HubGroup("الحديث والتعلم", "أحاديث صحيحة وحسنة فقط، بالراوي والمصدر والحكم من الدرر السنية") {
                HubRow("الأحاديث النبوية حسب الموضوع", "أحاديث صحيحة وحسنة بالتخريج والحكم والشرح", Icons.Outlined.Book, 1) { nav.navigate("hadith") }
                HubRow("التحقق من حديث", "الصق حديثا وصلك لتعرف تخريجه وحكمه", Icons.Outlined.Verified, 2) { nav.navigate("hadith-check") }
                HubRow("بطاقات الأحاديث الصحيحة", "حديث صحيح أو حسن مع الراوي والمصدر والحكم", Icons.Outlined.CropPortrait, 3) { nav.navigate("hadith") }
                HubRow("صفة الوضوء والصلاة", "من الأحاديث الصحيحة مع الشرح", Icons.Outlined.WaterDrop, 4) { nav.navigate("guide") }
                HubRow("تحفيظ الأطفال جزء عم", "استماع وتكرار ونجوم وشهادة باسم الطفل", Icons.Outlined.ChildCare, 0, divider = false) { nav.navigate("kids") }
            }
        }
    }
}

fun azkarIcon(title: String): ImageVector = when {
    "الصباح" in title -> Icons.Outlined.WbTwilight
    "المساء" in title -> Icons.Outlined.WbTwilight
    "النوم" in title -> Icons.Outlined.Bedtime
    "الاستيقاظ" in title -> Icons.Outlined.WbSunny
    else -> Icons.Outlined.WbSunny
}

@Composable
private fun HubGroup(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, subtitle = subtitle)
        Column(Modifier.fillMaxWidth().surface(b), content = content)
    }
}

@Composable
private fun HubRow(title: String, subtitle: String, icon: ImageVector, tile: Int, divider: Boolean = true, onClick: () -> Unit) {
    val b = LocalBrand.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, style = TileStyle.Gradient(tile), size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted)
            }
            Chevron()
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(0.5.dp).background(b.line))
    }
}

/** Every chapter of Hisn al-Muslim. */
@Composable
fun AzkarAllScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var q by rememberSaveable { mutableStateOf("") }
    val cats = content.azkarCategories.filter { q.isBlank() || q.trim() in it.title }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("حصن المسلم", onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                CategorySelector(listOf("all" to "${content.azkarCategories.size} بابا"), "all", {}, contentPadding = 0.dp)
                Spacer(Modifier.height(12.dp))
            }
            itemsIndexed(cats, key = { _, c -> c.id }) { i, c ->
                val shape = when {
                    cats.size == 1 -> RoundedCornerShape(18.dp)
                    i == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    i == cats.lastIndex -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Column(Modifier.fillMaxWidth().clip(shape).background(b.surface).clickable { nav.navigate(Routes.azkar(c.id)) }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.title, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                        Chevron()
                    }
                    if (i != cats.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(b.line))
                }
            }
        }
    }
}

@Composable
fun AzkarScreen(nav: NavHostController, categoryId: Int) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val cat = content.azkarCategories.firstOrNull { it.id == categoryId }
    val items = remember(categoryId) { content.azkar(categoryId) }
    val day = LocalDate.now().toString()
    val remaining = remember(categoryId, day) {
        mutableStateMapOf<Int, Int>().apply { items.forEach { put(it.id, prefs.zikrRemaining(day, it.id, it.count)) } }
    }
    val total = items.sumOf { it.count }
    val done = items.sumOf { it.count - (remaining[it.id] ?: it.count) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar(cat?.title ?: "الأذكار", onBack = { nav.popBackStack() }) {
            IconButton(onClick = { items.forEach { remaining[it.id] = it.count; prefs.setZikrRemaining(day, it.id, it.count) } }) {
                Icon(Icons.Filled.Refresh, "إعادة")
            }
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = b.accent, trackColor = b.line,
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { z ->
                val left = remaining[z.id] ?: z.count
                val finished = left == 0
                val bg by animateColorAsState(if (finished) b.tint else b.surface, label = "zikr")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCard).background(bg)
                        .clickable(enabled = !finished) {
                            val n = left - 1
                            remaining[z.id] = n
                            prefs.setZikrRemaining(day, z.id, n)
                            vibrate(ctx, strong = n == 0)
                        }
                        .padding(16.dp),
                ) {
                    Text(z.text, fontFamily = Fonts.naskh, fontSize = 20.sp, lineHeight = 36.sp, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    z.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The data's own reference; where it has none we say so, and never write one ourselves.
                        val ref = z.reference?.takeIf { it.isNotBlank() }
                        Text(
                            ref ?: "من حصن المسلم · المرجع التفصيلي غير مذكور في المصدر",
                            style = MaterialTheme.typography.labelSmall, color = if (ref != null) b.goldInk else b.muted, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { shareText(ctx, z.text + (z.reference?.let { "\n\n$it" } ?: "") + "\nayattla.com") }) {
                            Icon(Icons.Filled.Share, "مشاركة", tint = b.muted)
                        }
                        IconButton(onClick = {
                            com.ayattla.app.studio.PendingText.value = com.ayattla.app.studio.CardText(
                                "اصنع حالة", z.text, z.reference ?: (cat?.title ?: ""), ratio = com.ayattla.app.studio.CardRatio.Story,
                            )
                            nav.navigate("text-card")
                        }) { Icon(Icons.Outlined.PhotoLibrary, "اصنع حالة", tint = b.muted) }
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(if (finished) b.accent else b.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (finished) Icon(Icons.Filled.Check, "تم", tint = b.deep)
                            else Text("$left", style = MaterialTheme.typography.titleMedium, color = b.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

private val RoundedCard = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)


/**
 * «سجل الإنجاز»: the last seven days of the morning and evening azkar, from the counters saved per
 * day (complete when every zikr of the category was counted to zero that day). Kept on the device.
 */
@Composable
private fun AzkarWeekLog(cats: List<com.ayattla.app.data.ZikrCategory>) {
    if (cats.isEmpty()) return
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val items = remember(cats) { cats.associate { c -> c.id to content.azkar(c.id) } }
    fun state(day: LocalDate, catId: Int): Float {
        val list = items[catId].orEmpty()
        val total = list.sumOf { it.count }.coerceAtLeast(1)
        val done = list.sumOf { it.count - prefs.zikrRemaining(day.toString(), it.id, it.count) }
        return done.toFloat() / total
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("سجل الإنجاز")
        Column(Modifier.fillMaxWidth().surface(b).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(64.dp))
                days.forEach { d ->
                    Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale("ar")).take(3),
                        style = MaterialTheme.typography.labelSmall, color = if (d == today) b.primaryInk else b.muted,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            cats.forEach { c ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title.removePrefix("أذكار "), style = MaterialTheme.typography.labelLarge, color = b.ink, modifier = Modifier.width(64.dp))
                    days.forEach { d ->
                        val f = state(d, c.id)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(when { f >= 1f -> b.green; f > 0f -> b.green.copy(alpha = 0.35f); else -> b.tint }),
                                contentAlignment = Alignment.Center,
                            ) { if (f >= 1f) Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            val streak = generateSequence(today) { it.minusDays(1) }.take(60)
                .takeWhile { d -> cats.all { state(d, it.id) >= 1f } }.count()
            Text(if (streak > 0) "أتممت أذكار الصباح والمساء $streak ${if (streak == 1) "يوما" else "أيام"} متتالية" else "أتمم أذكار اليوم لتبدأ سلسلة الإنجاز",
                style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}
