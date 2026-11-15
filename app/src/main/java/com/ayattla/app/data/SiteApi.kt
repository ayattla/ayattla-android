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
}
