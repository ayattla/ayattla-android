package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Badge
import com.ayattla.app.ui.components.CategorySelector
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.FeaturePanel
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.ServiceRow
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.ToolTile
import com.ayattla.app.ui.components.pageHeadSurface
import com.ayattla.app.ui.components.patternedScreen
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.ToolAccent
import kotlinx.coroutines.delay

/** «المزيد»: the site's /tools/ page, as the iOS ToolsView. */
@Composable
fun MoreScreen(nav: NavHostController, initialGroup: String? = null, autoFocus: Boolean = false) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(initialGroup ?: "all") }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) { delay(400); runCatching { focus.requestFocus() } } }
    val browsing = query.isBlank() && filter == "all"
    val results = SiteTool.all.filter { t ->
        when (filter) { "all" -> true; "new" -> t.isNew; else -> t.group.name == filter }
    }.filter { query.isBlank() || query.trim() in it.title || query.trim() in it.subtitle }

    Column(Modifier.fillMaxSize().patternedScreen(b)) {
        AppTopBar("المزيد", onBack = if (nav.previousBackStackEntry != null && (initialGroup != null || autoFocus)) ({ nav.popBackStack() }) else null)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Head: title, count and the search box in one white band.
            Column(Modifier.fillMaxWidth().pageHeadSurface(b).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("أدوات تكمل يومك مع القرآن", style = MaterialTheme.typography.headlineSmall, color = b.ink)
                Text("${SiteTool.all.size} أداة مجانية تعمل على جهازك", style = MaterialTheme.typography.bodySmall, color = b.muted)
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(b.surface2)
                        .border(if (focused) 1.5.dp else 1.dp, if (focused) b.green else b.line, shape).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Search, null, tint = b.muted)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("ابحث في الأدوات: مثلا الأذكار أو القبلة", style = MaterialTheme.typography.bodyMedium, color = b.muted, maxLines = 1)
                        BasicTextField(
                            query, { query = it }, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = b.ink), cursorBrush = SolidColor(b.green),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
                        )
                    }
                    if (query.isNotEmpty()) Icon(Icons.Outlined.Cancel, "مسح البحث", tint = b.muted, modifier = Modifier.size(20.dp).pressable { query = "" })
                }
            }
            CategorySelector(
                listOf("all" to "الكل") + ToolGroup.entries.map { it.name to it.title } + listOf("new" to "جديد"),
                filter, { filter = it }, contentPadding = 0.dp,
            )
            if (browsing) {
                Frequent(nav)
                Featured(nav)
                NextPrayerCard(nav)
                ToolGroup.entries.forEach { g ->
                    val tools = SiteTool.all.filter { it.group == g && it.id !in SiteTool.featured }
                    if (tools.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GroupTitle(g.title, tools.size) { filter = g.name }
                        tools.forEach { t -> ToolRow(nav, t) }
                    }
                }
                QuickLinks(nav)
            } else if (results.isEmpty()) {
                EmptyState("لا توجد أداة بهذا الاسم", "جرب كلمة أخرى مثل الأذكار أو القبلة", icon = Icons.Outlined.Search)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (query.isNotBlank()) Text("${results.size} نتيجة", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = b.muted)
                    results.forEach { t -> ToolRow(nav, t) }
                }
            }
            // Privacy note (gold panel with the accent bar).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(b.goldSoft).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Lock, null, tint = b.goldInk, modifier = Modifier.size(18.dp))
                Text(
                    "كل الأدوات مجانية وبدون تسجيل، وما تحفظه (مثل عداد التسبيح وتقدم الختمة) يبقى على جهازك فقط.",
                    style = MaterialTheme.typography.bodySmall, color = b.goldInk, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ToolRow(nav: NavHostController, t: SiteTool) {
    val ctx = LocalContext.current
    val b = LocalBrand.current
    ServiceRow(t.title, t.subtitle, t.icon, style = TileStyle.Accent(ToolAccent.color(t.id, b.dark)), badge = if (t.isNew) "جديد" else null) { openTool(nav, ctx, t) }
}

@Composable
private fun Frequent(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (ToolUsage.hasHistory(ctx)) "الأكثر استخداما" else "وصول سريع", style = MaterialTheme.typography.titleMedium, color = b.ink)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolUsage.top(ctx, 8).forEach { t ->
                Column(Modifier.width(88.dp).pressable { openTool(nav, ctx, t) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconTile(t.icon, style = TileStyle.Solid(ToolAccent.color(t.id, b.dark)), size = 50.dp)
                    Text(t.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = b.ink, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun Featured(nav: NavHostController) {
    val ctx = LocalContext.current
    val b = LocalBrand.current
    val clip = SiteTool.byId("clip")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeaturePanel(onClick = { openTool(nav, ctx, clip) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(clip.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(clip.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(clip.icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("ayah-card", "status").map { SiteTool.byId(it) }.forEach { t ->
                ToolTile(
                    t.title, t.subtitle, t.icon, Modifier.weight(1f).fillMaxHeight(), accent = ToolAccent.color(t.id, b.dark),
                    badge = if (t.isNew) "جديد" else null,
                ) { openTool(nav, ctx, t) }
            }
        }
    }
}

@Composable
private fun QuickLinks(nav: NavHostController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ServiceRow("آياتي المحفوظة", "الآيات التي حفظتها لتعود إليها", Icons.Outlined.StarOutline, style = TileStyle.Emerald) { nav.navigate("saved") }
        ServiceRow("الاستخدام بدون إنترنت", "ما يعمل بلا اتصال وتجهيز التطبيق", Icons.Outlined.CloudDone, style = TileStyle.Emerald) { nav.navigate("offline") }
        ServiceRow("الإعدادات", "القارئ والخط والتنبيهات", Icons.Outlined.Settings, style = TileStyle.Soft) { nav.navigate("settings") }
        ServiceRow("عن آيات تتلى", "طريقة العمل والمزايا والمصادر", Icons.Outlined.Info, style = TileStyle.Soft) { nav.navigate("about") }
    }
}

