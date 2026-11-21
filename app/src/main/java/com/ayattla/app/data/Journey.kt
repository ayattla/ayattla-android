package com.ayattla.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * «رحلتي مع القرآن»: what was done each day — mushaf pages read, ayat heard, hifz portions — the
 * daily goal in pages, and the run of days in a row with time for the Quran. Kept on the phone only.
 */
object Journey {
    /** One day: the pages opened (each page counts once a day), ayat heard, hifz portions learnt or reviewed. */
    data class Day(val pages: Set<Int> = emptySet(), val ayat: Int = 0, val hifz: Int = 0) {
        /** A day counts in the run with at least one page, five ayat heard, or one hifz portion. */
        val active: Boolean get() = pages.isNotEmpty() || ayat >= 5 || hifz > 0
    }

    val goals = listOf(1, 2, 4, 10, 20)

    var days by mutableStateOf<Map<Long, Day>>(emptyMap())
        private set
    var goal by mutableIntStateOf(2)
        private set
    private var file: File? = null

    fun today() = LocalDate.now().toEpochDay()

    fun load(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, "journey.json"); file = f
        runCatching {
            val o = JSONObject(f.readText())
            goal = o.optInt("goal", 2)
            val d = o.optJSONObject("days") ?: JSONObject()
            days = d.keys().asSequence().associate { k ->
                val x = d.getJSONObject(k)
                val p = x.optJSONArray("p") ?: JSONArray()
                k.toLong() to Day((0 until p.length()).map { p.getInt(it) }.toSet(), x.optInt("a"), x.optInt("h"))
            }
        }
    }

    private fun update(day: Long = today(), f: (Day) -> Day) {
        val old = days[day] ?: Day()
        val new = f(old)
        if (new == old) return
        days = days + (day to new)
        save()
    }

    /** A mushaf page was on screen. */
    fun readPage(page: Int) { if (page in 1..604) update { it.copy(pages = it.pages + page) } }
    /** An ayah of a recitation finished (radio does not count). */
    fun listened(n: Int = 1) = update { it.copy(ayat = it.ayat + n) }
    /** A hifz portion learnt or a review rated. */
    fun hifzDone() = update { it.copy(hifz = it.hifz + 1) }

    fun setGoal(n: Int) { goal = n.coerceIn(1, 40); save() }

    fun day(d: Long = today()) = days[d] ?: Day()
    fun pagesToday() = day().pages.size
    fun goalMet(d: Long = today()) = day(d).pages.size >= goal

    /** Days in a row, ending today (or yesterday while today is still open). */
    fun streak(now: Long = today()): Int {
        var d = if (day(now).active) now else now - 1
        var n = 0
        while (day(d).active) { n++; d-- }
        return n
    }

    /** Longest run ever kept. */
    fun best(): Int {
        var best = 0; var run = 0; var prev = Long.MIN_VALUE
        days.filterValues { it.active }.keys.sorted().forEach { d -> run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
        return best
    }

    /** The last seven days, oldest first: date and pages read. */
    fun week(now: Long = today()): List<Pair<LocalDate, Day>> = (6 downTo 0).map { LocalDate.ofEpochDay(now - it) to day(now - it) }

    fun monthPages(now: LocalDate = LocalDate.now()): Int =
        days.filterKeys { LocalDate.ofEpochDay(it).let { d -> d.year == now.year && d.month == now.month } }.values.sumOf { it.pages.size }

    fun totalAyat(): Int = days.values.sumOf { it.ayat }

    /** Keeps the last 400 days. */
    private fun save() {
        val f = file ?: return
        val keep = days.filterKeys { it > today() - 400 }
        val d = JSONObject()
        keep.forEach { (k, v) -> d.put(k.toString(), JSONObject().put("p", JSONArray(v.pages.sorted())).put("a", v.ayat).put("h", v.hifz)) }
        runCatching { f.writeAtomic(JSONObject().put("goal", goal).put("days", d).toString()) }
    }

    /** For tests: start from given days. */
    internal fun setForTest(map: Map<Long, Day>, g: Int = 2) { days = map; goal = g }
}
