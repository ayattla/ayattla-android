package com.ayattla.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Hadith(
    val id: String, val title: String?, val text: String, val source: String,
    val grade: String, val cat: String?, val url: String, val match: Int?,
)

data class HadithPage(val cats: List<Pair<String, Int>>, val total: Int, val page: Int, val items: List<Hadith>)

/**
 * Read-only client for ayattla.com's public JSON endpoints (the same ones the site's pages use).
 * Each GET is also kept on disk so the last result shows when the device is offline.
 */
object SiteApi {
    private const val BASE = "https://ayattla.com"

    private fun open(url: String, method: String = "GET"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "AyatTutla-Android")
            setRequestProperty("Accept", "application/json")
        }

    private suspend fun getCached(context: Context, url: String, key: String): String = withContext(Dispatchers.IO) {
        val f = File(File(context.cacheDir, "api").apply { mkdirs() }, key.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120))
        try {
            val c = open(url)
            try {
                if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().readText().also { f.writeText(it) }
            } finally { c.disconnect() }
        } catch (e: Exception) {
            if (f.exists()) f.readText() else throw e
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** GET /api/hadith/browse?cat=&page=&q= (12 per page, as on /tools/hadith). */
    suspend fun hadith(context: Context, cat: String, page: Int, query: String): HadithPage {
        val raw = getCached(context, "$BASE/api/hadith/browse?cat=${enc(cat)}&page=$page&q=${enc(query)}", "hadith-$cat-$page-${query.hashCode()}")
        val o = JSONObject(raw)
        val cats = o.optJSONArray("cats") ?: JSONArray()
        return HadithPage(
            List(cats.length()) { cats.getJSONObject(it).let { c -> c.getString("name") to c.optInt("count") } },
            o.optInt("total"), o.optInt("page", page), items(o.getJSONArray("items")),
        )
    }

    /** POST /api/hadith/check {"text": ...} (as on /tools/hadith-checker). */
    suspend fun checkHadith(text: String): List<Hadith> = withContext(Dispatchers.IO) {
        val c = open("$BASE/api/hadith/check", "POST")
        try {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(JSONObject().put("text", text).toString().toByteArray()) }
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            items(JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("items"))
        } finally { c.disconnect() }
    }

    private fun items(a: JSONArray) = List(a.length()) { i ->
        val h = a.getJSONObject(i)
        Hadith(
            h.opt("id")?.toString() ?: "$i", h.optString("title").ifBlank { null }, h.getString("text"),
            h.optString("source"), h.optString("grade"), h.optString("cat").ifBlank { null }, h.optString("url"),
            if (h.has("match") && !h.isNull("match")) h.optDouble("match").toInt() else null,
        )
    }

    // ---- Quran.com (Quran Foundation) through the site's endpoints, as the iOS app (cached; refreshed after 6 days).

    data class TajweedVerse(val key: String, val html: String)
    data class Word(val t: String, val m: String)
    data class WordsVerse(val key: String, val n: Int, val words: List<Word>, val tafsir: String?)

    private suspend fun getQf(context: Context, url: String, key: String): String = withContext(Dispatchers.IO) {
        val f = File(File(context.cacheDir, "qf").apply { mkdirs() }, key)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < 6L * 86_400_000) return@withContext f.readText()
        try {
            val c = open(url)
            try {
                if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().readText().also { f.writeText(it) }
            } finally { c.disconnect() }
        } catch (e: Exception) { if (f.exists()) f.readText() else throw e }
    }

    /** GET /api/qf/tajweed/{chapter}: verses with the site's tajweed HTML. */
    suspend fun tajweed(context: Context, chapter: Int): List<TajweedVerse> {
        val a = JSONObject(getQf(context, "$BASE/api/qf/tajweed/$chapter", "tj-$chapter")).getJSONArray("verses")
        return List(a.length()) { a.getJSONObject(it).let { v -> TajweedVerse(v.getString("key"), v.getString("html")) } }
    }

    /** GET /api/qf/words/{chapter}?lang=&page= (all pages): word meanings and the tafsir line. */
    suspend fun words(context: Context, chapter: Int, lang: String, onPage: (List<WordsVerse>) -> Unit = {}): List<WordsVerse> {
        val out = ArrayList<WordsVerse>()
        var page: Int? = 1
        var guard = 0
        while (page != null && out.size <= 300 && guard++ < 60) {
            val before = out.size
            val o = JSONObject(getQf(context, "$BASE/api/qf/words/$chapter?lang=${enc(lang)}&page=$page", "wbw-$chapter-$lang-$page"))
            val vs = o.getJSONArray("verses")
            for (i in 0 until vs.length()) {
                val v = vs.getJSONObject(i)
                val ws = v.getJSONArray("words")
                out += WordsVerse(
                    v.getString("key"), v.optInt("n"),
                    List(ws.length()) { ws.getJSONObject(it).let { w -> Word(w.optString("t"), w.optString("m")) } },
                    v.optString("tafsir").ifBlank { null },
                )
            }
            onPage(out.subList(before, out.size).toList())
            page = o.optJSONObject("pagination")?.let { p -> if (p.isNull("next_page")) null else p.optInt("next_page").takeIf { it > 0 } }
        }
        return out
    }

    data class Mosque(val name: String, val lat: Double, val lng: Double, val distanceM: Double)

    /** Mosques around a point from OpenStreetMap (Overpass API); the search sends only the coordinates. */
    /** Public Overpass servers: the main one is often at capacity (HTTP 429/502/504), so the others are tried in turn. */
    private val overpass = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
    )

    suspend fun mosques(lat: Double, lng: Double, radiusM: Int): List<Mosque> = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:25];(node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radiusM,$lat,$lng);" +
            "way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radiusM,$lat,$lng););out center 80;"
        var last: Exception? = null
        for (server in overpass) {
            try { return@withContext queryMosques(server, q, lat, lng) } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("no server")
    }

    private fun queryMosques(server: String, q: String, lat: Double, lng: Double): List<Mosque> {
        val c = (URL(server).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "AyatTutla-Android (ayattla.com)"); setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            c.outputStream.use { it.write(("data=" + enc(q)).toByteArray()) }
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            val els = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("elements")
            return List(els.length()) { els.getJSONObject(it) }.mapNotNull { e ->
                val la = if (e.has("lat")) e.getDouble("lat") else e.optJSONObject("center")?.optDouble("lat") ?: return@mapNotNull null
                val lo = if (e.has("lon")) e.getDouble("lon") else e.optJSONObject("center")?.optDouble("lon") ?: return@mapNotNull null
                val tags = e.optJSONObject("tags")
                val name = tags?.optString("name:ar")?.ifBlank { null } ?: tags?.optString("name")?.ifBlank { null } ?: "مسجد"
                Mosque(name, la, lo, distance(lat, lng, la, lo))
            }.sortedBy { it.distanceM }
        } finally { c.disconnect() }
    }

    private fun distance(a1: Double, o1: Double, a2: Double, o2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(a2 - a1); val dLon = Math.toRadians(o2 - o1)
        val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(a1)) * Math.cos(Math.toRadians(a2)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(h))
    }
}
