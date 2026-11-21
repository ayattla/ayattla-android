package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.Routes
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.SearchText
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.components.pressable
import com.ayattla.app.ui.components.surface
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.ToolAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * «البحث الشامل»: one field over the tools, surahs, topics, azkar, reciters and the Quran text.
 * Everything is on the device; results open the matching page.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun UnifiedSearchScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    var q by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("الكل") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val clean = SearchText.clean(q.trim())
    fun hit(s: String) = clean.length >= 2 && SearchText.clean(s).contains(clean)
    val tools = remember(clean) { if (clean.length < 2) emptyList() else SiteTool.all.filter { hit(it.title) || hit(it.subtitle) || hit(it.short) } }
    val surahs = remember(clean) {
        if (clean.isEmpty()) emptyList() else content.surahs.filter { hit(it.namePlain) || q.trim() == it.id.toString() }
    }
    val topics = remember(clean) { if (clean.length < 2) emptyList() else content.library.topics.filter { hit(it.title) } }
    val azkarCats = remember(clean) { if (clean.length < 2) emptyList() else content.azkarCategories.filter { hit(it.title) } }
    val reciters = remember(clean) { if (clean.length < 2) emptyList() else content.reciters.filter { hit(it.name) } }
    var ayat by remember { mutableStateOf<List<com.ayattla.app.data.Ayah>>(emptyList()) }
    LaunchedEffect(clean) {
        ayat = emptyList()
        if (clean.length < 2) return@LaunchedEffect
        delay(250)
        ayat = withContext(Dispatchers.IO) { runCatching { content.search(q, 60) }.getOrDefault(emptyList()) }
    }
    val groups = listOf("الكل", "الأدوات", "السور", "الآيات", "المواضيع", "الأذكار", "القراء")
    fun show(g: String) = filter == "الكل" || filter == g

    Column(Modifier.fillMaxSize()) {
        AppTopBar("البحث الشامل", onBack = { nav.popBackStack() })
        OutlinedTextField(
            q, { q = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
            placeholder = { Text("ابحث عن أداة أو سورة أو آية أو ذكر…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = b.muted) },
            trailingIcon = {
                if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Outlined.Close, "مسح", tint = b.muted) }
                else IconButton(onClick = { nav.navigate("voice-search") }) { Icon(Icons.Outlined.Mic, "البحث بالصوت", tint = b.green) }
            },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = {}),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = b.green, unfocusedBorderColor = b.line, focusedContainerColor = b.surface, unfocusedContainerColor = b.surface),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { g -> Chip(g, filter == g, { filter = g }) }
        }
        val empty = tools.isEmpty() && surahs.isEmpty() && topics.isEmpty() && azkarCats.isEmpty() && reciters.isEmpty() && ayat.isEmpty()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (clean.isEmpty()) {
                item {
                    Text("اقتراحات", style = MaterialTheme.typography.labelLarge, color = b.muted, modifier = Modifier.padding(vertical = 6.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("الكهف", "أذكار الصباح", "القبلة", "الصبر", "الرحمة", "تسبيح", "الملك").forEach { s -> Chip(s, false, { q = s }) }
                    }
                }
            } else if (empty && clean.length >= 2) {
                item { EmptyState("لا نتائج", "جرب كلمة أخرى أو جزءا من الكلمة", icon = Icons.Outlined.Search) }
            }
            if (show("الأدوات") && tools.isNotEmpty()) {
                item { GroupTitle("الأدوات", tools.size) }
                items(tools, key = { "t" + it.id }) { t ->
                    ResultRow(t.title, t.subtitle, t.icon, TileStyle.Accent(ToolAccent.color(t.id, b.dark))) { openTool(nav, ctx, t) }
                }
            }
            if (show("السور") && surahs.isNotEmpty()) {
                item { GroupTitle("السور", surahs.size) }
                items(surahs.take(20), key = { "s" + it.id }) { s ->
                    ResultRow(s.namePlain, "السورة ${s.id} · ${s.ayahCount} آية", Icons.AutoMirrored.Outlined.MenuBook, TileStyle.Gradient(0)) { nav.navigate(Routes.reader(s.id, 1)) }
                }
            }
            if (show("المواضيع") && topics.isNotEmpty()) {
                item { GroupTitle("المواضيع", topics.size) }
                items(topics, key = { "p" + it.slug }) { t ->
                    ResultRow(t.title, t.desc, Icons.Outlined.Lightbulb, TileStyle.Gradient(3)) { nav.navigate("topic/${t.slug}") }
                }
            }
            if (show("الأذكار") && azkarCats.isNotEmpty()) {
                item { GroupTitle("الأذكار", azkarCats.size) }
                items(azkarCats, key = { "z" + it.id }) { c ->
                    ResultRow(c.title, null, Icons.Outlined.WbTwilight, TileStyle.Gradient(2)) { nav.navigate(Routes.azkar(c.id)) }
                }
            }
            if (show("القراء") && reciters.isNotEmpty()) {
                item { GroupTitle("القراء", reciters.size) }
                items(reciters.take(20), key = { "r" + it.id }) { r ->
                    ResultRow(r.name, "استمع لسورة الفاتحة", Icons.Outlined.Headphones, TileStyle.Gradient(1)) {
                        Recitation.play(ctx, content, r, 1, 1, continuous = true)
                    }
                }
            }
            if (show("الآيات") && ayat.isNotEmpty()) {
                item { GroupTitle("الآيات", ayat.size) }
                items(ayat, key = { "a${it.surah}:${it.ayah}" }) { a ->
                    Column(
                        Modifier.fillMaxWidth().pressable { nav.navigate(Routes.reader(a.surah, a.ayah)) }.surface(b).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(a.text, fontFamily = Fonts.quran, fontSize = 19.sp, lineHeight = 36.sp, color = b.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("${content.surah(a.surah).namePlain} · الآية ${a.ayah}", style = MaterialTheme.typography.labelMedium, color = b.primaryInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(t: String, n: Int) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, style = MaterialTheme.typography.titleSmall, color = b.ink, modifier = Modifier.weight(1f))
        Text("$n", style = MaterialTheme.typography.labelMedium, color = b.muted,
            modifier = Modifier.background(b.surface2, RoundedCornerShape(10.dp)).border(1.dp, b.line, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun ResultRow(title: String, sub: String?, icon: ImageVector, style: TileStyle, onClick: () -> Unit) {
    val b = LocalBrand.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).pressable(onClick).surface(b).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, style = style, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = b.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
