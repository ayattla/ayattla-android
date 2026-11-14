package com.ayattla.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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

@Composable
fun AzkarListScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val cats = content.azkarCategories
    // Morning and evening first, as on the site.
    val featured = cats.filter { "الصباح" in it.title || "المساء" in it.title || "النوم" in it.title }
    val rest = cats - featured.toSet()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("الأذكار", style = MaterialTheme.typography.headlineSmall, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("من حصن المسلم", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        items(featured, key = { "f${it.id}" }) { c ->
            val icon = when {
                "الصباح" in c.title -> Icons.Filled.WbSunny
                "المساء" in c.title -> Icons.Filled.WbTwilight
                else -> Icons.Filled.NightsStay
            }
            AppCard(onClick = { nav.navigate(Routes.azkar(c.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(SmallShape).background(b.goldSoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = b.goldInk) }
                    Text(c.title, style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
        item { Text("كل الأبواب", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.padding(top = 8.dp)) }
        items(rest, key = { it.id }) { c ->
            Column {
                Text(
                    c.title, style = MaterialTheme.typography.bodyLarge, color = b.ink,
                    modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.azkar(c.id)) }.padding(vertical = 12.dp),
                )
                HorizontalDivider(color = b.line)
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
                        Text(z.reference.orEmpty(), style = MaterialTheme.typography.labelSmall, color = b.goldInk, modifier = Modifier.weight(1f))
                        IconButton(onClick = { shareText(ctx, z.text + (z.reference?.let { "\n\n$it" } ?: "") + "\nayattla.com") }) {
                            Icon(Icons.Filled.Share, "مشاركة", tint = b.muted)
                        }
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
