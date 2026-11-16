package com.ayattla.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** Writes a file atomically (a crash mid-write never leaves half a file). */
internal fun File.writeAtomic(text: String) {
    parentFile?.mkdirs()
    val tmp = File(path + ".tmp"); tmp.writeText(text); tmp.renameTo(this)
}

/**
 * Reflection notes and categories on ayat («ملاحظات التدبر»), as the iOS app's FavoriteNotes:
 * one entry per ayah key ("s:a"). Kept in the app's files (backed up with the user's data).
 */
object Notes {
    data class Entry(val note: String = "", val tag: String = "", val updated: Long = System.currentTimeMillis())

    var all by mutableStateOf<Map<String, Entry>>(emptyMap())
        private set
    private var file: File? = null

    fun load(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, "notes.json"); file = f
        all = runCatching {
            val o = JSONObject(f.readText())
            o.keys().asSequence().associateWith { k -> o.getJSONObject(k).let { Entry(it.optString("note"), it.optString("tag"), it.optLong("updated")) } }
        }.getOrDefault(emptyMap())
    }

    fun set(key: String, e: Entry?) {
        val clean = e?.copy(note = e.note.trim(), tag = e.tag.trim(), updated = System.currentTimeMillis())
        all = if (clean == null || (clean.note.isEmpty() && clean.tag.isEmpty())) all - key else all + (key to clean)
        val o = JSONObject()
        all.forEach { (k, v) -> o.put(k, JSONObject().put("note", v.note).put("tag", v.tag).put("updated", v.updated)) }
        file?.writeAtomic(o.toString())
    }

    val tags get() = all.values.map { it.tag }.filter { it.isNotBlank() }.distinct().sorted()
    val defaultTags = listOf("للحفظ", "للتدبر", "دعاء", "بشارة", "موعظة")
}

/**
 * Memorisation plan with spaced review («خطة الحفظ والمراجعة»): a new portion each day in the
 * chosen order, then each memorised portion comes back for review after 1 day, then longer gaps
 * as it is rated well (and back to 1 day when it is hard). Kept on the device.
 */
object Hifz {
    data class Unit(val surah: Int, val from: Int, val to: Int) { val key get() = "$surah:$from-$to"; val count get() = to - from + 1 }
    data class Review(val unit: Unit, val interval: Int, val due: Long, val reps: Int)
    data class Plan(
        val forward: Boolean,       // from Al-Fatiha onwards, or from An-Nas back
        val daily: Int,             // ayat per day
        val startSurah: Int,
        val nextSurah: Int,
        val nextAyah: Int,
        val lastNewDay: Long,       // epoch day the last new portion was learnt
        val reviews: List<Review>,
        val started: Long,
    )

    var plan by mutableStateOf<Plan?>(null)
        private set
    private var file: File? = null

    fun today() = LocalDate.now().toEpochDay()

    fun load(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, "hifz.json"); file = f
        plan = runCatching { parse(JSONObject(f.readText())) }.getOrNull()
    }

    fun start(forward: Boolean, daily: Int, startSurah: Int) =
        save(Plan(forward, daily, startSurah, startSurah, 1, -1, emptyList(), today()))

    fun reset() { plan = null; file?.delete() }

    fun setDaily(n: Int) = plan?.let { save(it.copy(daily = n)) }

    /** Today's new portion (null when the whole Quran is done in this order). */
    fun nextUnit(content: ContentStore): Unit? {
        val p = plan ?: return null
        if (p.nextSurah !in 1..114) return null
        val count = content.surah(p.nextSurah).ayahCount
        return Unit(p.nextSurah, p.nextAyah, minOf(count, p.nextAyah + p.daily - 1))
    }

    fun learnedToday() = plan?.lastNewDay == today()

    /** The new portion is memorised: it joins the reviews (due tomorrow) and the pointer moves on. */
    fun learned(content: ContentStore) {
        val p = plan ?: return
        val u = nextUnit(content) ?: return
        val count = content.surah(u.surah).ayahCount
        val (ns, na) = if (u.to < count) u.surah to u.to + 1 else (if (p.forward) u.surah + 1 else u.surah - 1) to 1
        save(p.copy(nextSurah = ns, nextAyah = na, lastNewDay = today(), reviews = p.reviews + Review(u, 1, today() + 1, 0)))
    }

    fun due(): List<Review> = plan?.reviews?.filter { it.due <= today() }?.sortedBy { it.due }.orEmpty()

    /** 0 hard → tomorrow; 1 fair → 1.5× the gap; 2 strong → 2.5× the gap (at most 60 days). */
    fun rate(r: Review, grade: Int) {
        val p = plan ?: return
        val next = when (grade) { 0 -> 1; 1 -> maxOf(2, (r.interval * 1.5).toInt()); else -> maxOf(3, (r.interval * 2.5).toInt()) }.coerceAtMost(60)
        save(p.copy(reviews = p.reviews.map { if (it.unit == r.unit) Review(it.unit, next, today() + next, it.reps + 1) else it }))
    }

    fun memorisedAyat() = plan?.reviews?.sumOf { it.unit.count } ?: 0

    private fun save(p: Plan) {
        plan = p
        val o = JSONObject().put("forward", p.forward).put("daily", p.daily).put("startSurah", p.startSurah).put("nextSurah", p.nextSurah)
            .put("nextAyah", p.nextAyah).put("lastNewDay", p.lastNewDay).put("started", p.started)
            .put("reviews", JSONArray(p.reviews.map {
                JSONObject().put("s", it.unit.surah).put("a", it.unit.from).put("b", it.unit.to).put("interval", it.interval).put("due", it.due).put("reps", it.reps)
            }))
        file?.writeAtomic(o.toString())
    }

    private fun parse(o: JSONObject): Plan {
        val rs = o.optJSONArray("reviews") ?: JSONArray()
        return Plan(
            o.getBoolean("forward"), o.getInt("daily"), o.getInt("startSurah"), o.getInt("nextSurah"), o.getInt("nextAyah"),
            o.optLong("lastNewDay", -1), List(rs.length()) { i ->
                rs.getJSONObject(i).let { Review(Unit(it.getInt("s"), it.getInt("a"), it.getInt("b")), it.getInt("interval"), it.getLong("due"), it.optInt("reps")) }
            }, o.optLong("started"),
        )
    }
}
