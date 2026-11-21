package com.ayattla.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Word timings for highlighting, from Quran.com (Quran Foundation) chapter recitations, the same
 * source and rules as the iOS app and the site: fetched when needed, kept on the device for at most
 * 6 days as Quran Foundation's terms require (docs/CONTENT_AND_LICENSES.md), then fetched again.
 */
object QuranCom {
    const val MAX_AGE_MS = 6L * 86_400_000

    /** One ayah in the chapter audio: its span and each word's start (word numbers count from 1). */
    data class Stamp(val key: String, val from: Long, val to: Long, val words: List<Pair<Int, Long>>)
    data class Recitation(val audioUrl: String, val stamps: Map<String, Stamp>)

    private fun dir(c: Context) = File(c.filesDir, "qf").apply { mkdirs() }

    /** Removes anything older than the allowed 6 days (timings and chapter audio). */
    fun prune(c: Context) {
        val old = System.currentTimeMillis() - MAX_AGE_MS
        dir(c).listFiles().orEmpty().filter { it.lastModified() < old }.forEach { it.delete() }
    }

    fun parse(json: String): Recitation {
        val af = JSONObject(json).getJSONObject("audio_file")
        val ts = af.getJSONArray("timestamps")
        val stamps = HashMap<String, Stamp>()
        for (i in 0 until ts.length()) {
            val t = ts.getJSONObject(i)
            val segs = t.optJSONArray("segments")
            val words = buildList {
                if (segs != null) for (j in 0 until segs.length()) {
                    val s = segs.optJSONArray(j) ?: continue
                    if (s.length() >= 3) add(s.getDouble(0).toInt() to s.getDouble(1).toLong())
                }
            }
            val key = t.getString("verse_key")
            stamps[key] = Stamp(key, t.getLong("timestamp_from"), t.getLong("timestamp_to"), words)
        }
        return Recitation(af.getString("audio_url"), stamps)
    }

    /** Timings for [qfId] (a Quran.com chapter-recitation id) and [chapter]; cached copy offline. */
    suspend fun recitation(c: Context, qfId: Int, chapter: Int): Recitation = withContext(Dispatchers.IO) {
        prune(c)
        val f = File(dir(c), "rec-$qfId-$chapter.json")
        val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS
        if (!fresh && Net.isOnline) {
            runCatching { get("https://api.quran.com/api/v4/chapter_recitations/$qfId/$chapter?segments=true", f) }
        }
        if (!f.exists()) error("timings unavailable")
        parse(f.readText())
    }

    /** The chapter's audio file, kept with the timings (same 6-day limit). */
    suspend fun audio(c: Context, qfId: Int, chapter: Int, url: String): File = withContext(Dispatchers.IO) {
        val f = File(dir(c), "audio-$qfId-$chapter.mp3")
        if (!f.exists() || f.length() < 10_000) get(url, f)
        f
    }

    private fun get(url: String, to: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 60_000
        conn.setRequestProperty("Accept", "application/json, audio/mpeg, */*")
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val tmp = File(to.path + ".tmp")
            conn.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            tmp.renameTo(to)
        } finally { conn.disconnect() }
    }

    /**
     * Word starts for one ayah, mapped onto the ayah's own tokens (stop marks and the ayah number are
     * tokens without letters, so they are skipped). Null when Quran.com's word count does not fit the
     * text: that ayah is then shown without highlighting, never with a wrong word lit.
     */
    fun tokenStarts(text: String, stamp: Stamp): List<Pair<Long, Int>>? {
        val tokens = Recite.tokens(text)
        val words = Recite.wordIndexes(tokens)
        if (stamp.words.isEmpty() || stamp.words.any { it.first !in 1..words.size }) return null
        return stamp.words.map { (w, start) -> (start - stamp.from).coerceAtLeast(0) to words[w - 1] }
    }
}
