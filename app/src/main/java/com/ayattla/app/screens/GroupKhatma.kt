package com.ayattla.app.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ayattla.app.ui.components.*
import com.ayattla.app.ui.theme.LocalBrand
import org.json.JSONArray
import org.json.JSONObject

/**
 * «الختمة الجماعية»: the organizer adds names, the 30 juz are split between them in order, and one
 * message shares who reads what. Each juz is ticked when its reader says it's done. All on the device.
 */
data class GroupKhatma(val title: String, val names: List<String>, val done: Set<Int>) {
    /** Juz numbers for each person, split as evenly as possible in reading order. */
    fun split(): List<Pair<String, IntRange>> {
        if (names.isEmpty()) return emptyList()
        val n = names.size.coerceAtMost(30)
        val base = 30 / n; val extra = 30 % n
        var start = 1
        return names.take(n).mapIndexed { i, name ->
            val count = base + if (i < extra) 1 else 0
            val r = start until start + count; start += count
            name to r
        }
    }

    fun message(): String = buildString {
        append("ختمة جماعية: ").append(title).append('\n')
        this@GroupKhatma.split().forEach { (name, r) -> append(name).append(": ").append(if (r.first == r.last) "الجزء ${r.first}" else "الأجزاء ${r.first} إلى ${r.last}").append('\n') }
        append("\nاقرأ وردك من تطبيق آيات تتلى: https://play.google.com/store/apps/details?id=com.ayattla.app")
    }

    companion object {
        private fun sp(c: Context) = c.getSharedPreferences("group.khatma", Context.MODE_PRIVATE)
        fun load(c: Context): GroupKhatma? = runCatching {
            val o = JSONObject(sp(c).getString("g", null) ?: return null)
            val n = o.getJSONArray("names"); val d = o.getJSONArray("done")
            GroupKhatma(o.getString("title"), List(n.length()) { n.getString(it) }, List(d.length()) { d.getInt(it) }.toSet())
        }.getOrNull()
        fun save(c: Context, g: GroupKhatma?) {
            if (g == null) { sp(c).edit().remove("g").apply(); return }
            val o = JSONObject().put("title", g.title).put("names", JSONArray(g.names)).put("done", JSONArray(g.done.sorted()))
            sp(c).edit().putString("g", o.toString()).apply()
        }
    }
}

@Composable
fun GroupKhatmaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var g by remember { mutableStateOf(GroupKhatma.load(ctx)) }
    fun update(n: GroupKhatma?) { g = n; GroupKhatma.save(ctx, n) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("الختمة الجماعية", onBack = { nav.popBackStack() }) {
            if (g != null) TextButton(onClick = { update(null) }) { Text("ختمة جديدة") }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val cur = g
            if (cur == null) {
                var title by remember { mutableStateOf("ختمة العائلة") }
                var name by remember { mutableStateOf("") }
                val names = remember { mutableStateListOf<String>() }
                Text("وزع أجزاء القرآن الثلاثين على أهلك أو أصحابك، وأرسل لهم رسالة فيها نصيب كل واحد.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("اسم الختمة") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("اسم المشارك") }, singleLine = true)
                    BrandButton("أضف", enabled = name.isNotBlank() && names.size < 30) { names += name.trim(); name = "" }
                }
                names.forEachIndexed { i, n ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. $n", style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                        TextButton(onClick = { names.removeAt(i) }) { Text("حذف") }
                    }
                }
                BrandButton("وزع الأجزاء", Modifier.fillMaxWidth(), enabled = names.isNotEmpty() && title.isNotBlank()) {
                    update(GroupKhatma(title.trim(), names.toList(), emptySet()))
                }
            } else {
                Text(cur.title, style = MaterialTheme.typography.titleLarge, color = b.ink)
                Text("تم ${cur.done.size} من 30 جزء", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                LinearProgressIndicator(progress = { cur.done.size / 30f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                if (cur.done.size == 30) Text("تمت الختمة، تقبل الله منكم", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
                BrandButton("أرسل التوزيع للمجموعة", Modifier.fillMaxWidth(), icon = Icons.Outlined.Share) { shareText(ctx, cur.message()) }
                cur.split().forEach { (name, r) ->
                    AppCard {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = b.ink)
                        r.forEach { j ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(j in cur.done, { on -> update(cur.copy(done = if (on) cur.done + j else cur.done - j)) })
                                Text("الجزء $j", style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                                TextButton(onClick = { nav.navigate("page/${MushafMarks.juzPages(j).first}") }) { Text("اقرأ") }
                            }
                        }
                    }
                }
            }
        }
    }
}
