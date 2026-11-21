package com.ayattla.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.SearchText
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.LoadingState
import com.ayattla.app.ui.components.SmallShape
import com.ayattla.app.ui.theme.LocalBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RadioStation(val name: String, val url: String)

/** The site's radio list (mp3quran streams), cached at the edge by Cloudflare. */
private suspend fun loadRadios(): List<RadioStation> = withContext(Dispatchers.IO) {
    val c = URL("https://ayattla.com/api/radios").openConnection() as HttpURLConnection
    c.connectTimeout = 10_000; c.readTimeout = 15_000
    c.setRequestProperty("User-Agent", "AyatTutla-Android")
    try {
        val a = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("radios")
        List(a.length()) { val o = a.getJSONObject(it); RadioStation(o.getString("name"), o.getString("url")) }
            .filter { it.url.startsWith("https://") }
    } finally { c.disconnect() }
}

@Composable
fun RadiosScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var radios by remember { mutableStateOf<List<RadioStation>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(attempt, com.ayattla.app.data.Net.isOnline) {
        failed = false
        runCatching { loadRadios() }.onSuccess { radios = it }.onFailure { failed = true }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("الإذاعات القرآنية", onBack = { nav.popBackStack() })
        when {
            failed && !com.ayattla.app.data.Net.isOnline -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                com.ayattla.app.ui.components.OfflineNotice(
                    "الإذاعات بث مباشر يحتاج إنترنت",
                    "تعمل تلقائيا عند عودة الاتصال. تقدر الآن تستمع للسور المحمّلة وما استمعت إليه من قبل.",
                    action = "التلاوات المحفوظة",
                ) { nav.navigate("downloads") }
            }
            failed -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState("تعذر تحميل الإذاعات", "تحقق من اتصال الإنترنت ثم أعد المحاولة.")
                Button(onClick = { attempt++ }) { Text("إعادة المحاولة") }
            }
            radios == null -> LoadingState()
            else -> {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true, shape = SmallShape,
                    placeholder = { Text("ابحث باسم القارئ") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                )
                val q = SearchText.clean(query)
                val list = radios!!.filter { q.isEmpty() || q in SearchText.clean(it.name) }
                LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp)) {
                    items(list, key = { it.url }) { r ->
                        val playing = Recitation.current == "stream" && Recitation.title == r.name
                        Row(
                            Modifier.fillMaxWidth().background(if (playing) b.tint else b.background)
                                .clickable { Recitation.playStream(ctx, r.url, r.name) }.padding(vertical = 14.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (playing) Icons.Filled.Equalizer else Icons.Filled.Radio, null, tint = if (playing) b.accent else b.goldInk)
                            Text(r.name, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.padding(horizontal = 12.dp))
                        }
                        HorizontalDivider(color = b.line)
                    }
                }
            }
        }
    }
}
