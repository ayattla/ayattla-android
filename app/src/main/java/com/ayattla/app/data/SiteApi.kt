package com.ayattla.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

data class HadithPage(val cats: List<Pair<String, Int>>, val total: Int, val page: Int, val items: List<Hadith>, val offline: Boolean = false)

/** No connection and nothing saved for this request. */
class OfflineException : Exception("offline")

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

    /** Saved answers live with the app's files (not the cache the system may empty), so tools keep working offline. */
    private fun store(context: Context, sub: String) = File(File(context.filesDir, "offline"), sub).apply { mkdirs() }

    /**
     * [preferNetwork] false: a saved copy is used without asking the server (the offline preparation).
     * Offline, a saved copy is returned at once instead of waiting for a timeout.
     */
    private suspend fun getCached(context: Context, url: String, key: String, preferNetwork: Boolean = true): String = withContext(Dispatchers.IO) {
        val f = File(store(context, "api"), key.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120))
        if (f.exists() && (!preferNetwork || !Net.isOnline)) return@withContext f.readText()
        if (!Net.isOnline) throw OfflineException()
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

    /** GET /api/hadith/browse?cat=&page=&q= (12 per page, as on /tools/hadith). Offline: the saved collection. */
    suspend fun hadith(context: Context, cat: String, page: Int, query: String, preferNetwork: Boolean = true): HadithPage {
        val raw = try {
            getCached(context, "$BASE/api/hadith/browse?cat=${enc(cat)}&page=$page&q=${enc(query)}", "hadith-$cat-$page-${query.hashCode()}", preferNetwork)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            return savedHadith(context, cat, page, query) ?: throw e
        }
        val o = JSONObject(raw)
        val cats = o.optJSONArray("cats") ?: JSONArray()
        return HadithPage(
            List(cats.length()) { cats.getJSONObject(it).let { c -> c.getString("name") to c.optInt("count") } },
            o.optInt("total"), o.optInt("page", page), items(o.getJSONArray("items")),
        )
    }

    /** Every hadith saved on the device (from pages opened or prepared), without repeats. */
    suspend fun savedCollection(context: Context): Pair<List<Pair<String, Int>>, List<Hadith>> = withContext(Dispatchers.IO) {
        val files = store(context, "api").listFiles { f -> f.name.startsWith("hadith-") }.orEmpty()
        val seen = LinkedHashMap<String, Hadith>()
        var cats: List<Pair<String, Int>> = emptyList()
        files.sortedBy { it.name }.forEach { f ->
            runCatching {
                val o = JSONObject(f.readText())
                val c = o.optJSONArray("cats")
                if (c != null && c.length() > cats.size) cats = List(c.length()) { c.getJSONObject(it).let { x -> x.getString("name") to x.optInt("count") } }
                items(o.getJSONArray("items")).forEach { h -> seen.putIfAbsent(h.id + h.url, h) }
            }
        }
        cats to seen.values.toList()
    }

    /** A page of the saved collection, filtered as the site does (category, words of the search). */
    private suspend fun savedHadith(context: Context, cat: String, page: Int, query: String): HadithPage? {
        val (cats, all) = savedCollection(context)
        if (all.isEmpty()) return null
        val words = SearchText.clean(query).split(' ').filter { it.length > 1 }
        val list = all.filter { h ->
            (cat.isEmpty() || h.cat == cat) && (words.isEmpty() || SearchText.clean(h.text + " " + (h.title ?: "")).let { t -> words.all { it in t } })
        }
        val savedCats = cats.map { (n, _) -> n to all.count { it.cat == n } }.filter { it.second > 0 }
        return HadithPage(savedCats, list.size, page, list.drop((page - 1) * 12).take(12), offline = true)
    }

    /** Offline check: the saved hadith whose words best match the pasted text (match = share of words found). */
    suspend fun checkHadithSaved(context: Context, text: String): List<Hadith> {
        val q = SearchText.clean(text).split(' ').filter { it.length > 2 }.toSet()
        if (q.isEmpty()) return emptyList()
        return savedCollection(context).second.map { h ->
            val t = SearchText.clean(h.text).split(' ').toSet()
            h to q.count { it in t } * 100 / q.size
        }.filter { it.second >= 50 }.sortedByDescending { it.second }.take(5).map { (h, m) -> h.copy(match = m) }
    }

    /** POST /api/hadith/check {"text": ...} (as on /tools/hadith-checker). */
    suspend fun checkHadith(text: String): List<Hadith> = withContext(Dispatchers.IO) {
        if (!Net.isOnline) throw OfflineException()
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
        val f = File(store(context, "qf"), key)
        if (f.exists() && (!Net.isOnline || System.currentTimeMillis() - f.lastModified() < 6L * 86_400_000)) return@withContext f.readText()
        if (!Net.isOnline) throw OfflineException()
        try {
            val c = open(url)
            try {
                if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().readText().also { f.writeText(it) }
            } finally { c.disconnect() }
        } catch (e: Exception) { if (f.exists()) f.readText() else throw e }
    }

    /** Whether a surah's tajweed text / word meanings are on the device. */
    fun hasTajweed(context: Context, chapter: Int) = File(store(context, "qf"), "tj-$chapter").exists()
    fun hasWords(context: Context, chapter: Int, lang: String) = File(store(context, "qf"), "wbw-$chapter-$lang-1").exists()

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
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
    )

    suspend fun mosques(lat: Double, lng: Double, radiusM: Int): List<Mosque> = withContext(Dispatchers.IO) {
        if (!Net.isOnline) throw OfflineException()
        val q = "[out:json][timeout:25];(node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radiusM,$lat,$lng);" +
            "way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radiusM,$lat,$lng););out center 80;"
        // All servers are asked at once and the first answer is used: each one is often busy or slow.
        kotlinx.coroutines.coroutineScope {
            val pending = overpass.map { server -> async { runCatching { queryMosques(server, q, lat, lng) } } }.toMutableList()
            var last: Throwable? = null
            while (pending.isNotEmpty()) {
                val (done, r) = kotlinx.coroutines.selects.select<Pair<kotlinx.coroutines.Deferred<Result<List<Mosque>>>, Result<List<Mosque>>>> {
                    pending.forEach { d -> d.onAwait { d to it } }
                }
                pending.remove(done)
                r.onSuccess { list -> pending.forEach { it.cancel() }; return@coroutineScope list }.onFailure { last = it }
            }
            throw last ?: IllegalStateException("no server")
        }
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
