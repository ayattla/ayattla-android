package com.ayattla.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One photo or video from the site's media library (GET /api/media/search). */
data class SiteMedia(
    val provider: String, val mediaId: String, val type: String,
    val thumbUrl: String?, val previewUrl: String?,
    val width: Int?, val height: Int?, val curated: Boolean,
) {
    val isVideo get() = type == "video"
    /** Best still for a tile: a photo's preview, a video's thumbnail. */
    val still get() = if (isVideo) thumbUrl else previewUrl ?: thumbUrl
}

/** The site's media library, as the iOS MediaLibrary / FeaturedMedia (cached one hour on disk). */
object MediaLibrary {
    private const val BASE = "https://ayattla.com/api/media/search"
    private val mem = HashMap<String, List<SiteMedia>>()
    private val lock = Mutex()

    suspend fun search(context: Context, type: String, category: String, orientation: String = "portrait"): List<SiteMedia> = lock.withLock {
        val key = "$type-$category-$orientation"
        mem[key]?.let { return it }
        val url = "$BASE?type=$type&q=&category=${URLEncoder.encode(category, "UTF-8")}&page=1&orientation=$orientation"
        val raw = withContext(Dispatchers.IO) {
            val f = File(File(context.cacheDir, "media").apply { mkdirs() }, "search-$key.json")
            val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < 3_600_000
            if (fresh) f.readText() else runCatching {
                val c = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000; readTimeout = 20_000
                    setRequestProperty("User-Agent", "AyatTutla-Android"); setRequestProperty("Accept", "application/json")
                }
                try { if (c.responseCode != 200) error("HTTP ${c.responseCode}"); c.inputStream.bufferedReader().readText().also { f.writeText(it) } }
                finally { c.disconnect() }
            }.getOrElse { if (f.exists()) f.readText() else return@withContext null }
        } ?: return emptyList()
        val items = runCatching {
            val arr = JSONObject(raw).getJSONArray("items")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SiteMedia(
                    o.optString("provider"), o.opt("mediaId")?.toString().orEmpty(), o.optString("type", type),
                    o.optString("thumbUrl").ifBlank { null }, o.optString("previewUrl").ifBlank { null },
                    o.optInt("width").takeIf { it > 0 }, o.optInt("height").takeIf { it > 0 }, o.optBoolean("curated"),
                )
            }
        }.getOrDefault(emptyList())
        if (items.isNotEmpty()) mem[key] = items
        items
    }

    /** Curated first (up to ten), as FeaturedMedia.curated. */
    fun curated(items: List<SiteMedia>): List<SiteMedia> = items.filter { it.curated }.ifEmpty { items }.take(10)

    /** Video, photo, photo, video… for the gallery rail. */
    fun mix(videos: List<SiteMedia>, images: List<SiteMedia>): List<SiteMedia> {
        val out = ArrayList<SiteMedia>()
        val vi = videos.iterator(); val ii = images.iterator()
        while (out.size < 14) {
            var added = false
            if (vi.hasNext()) { out += vi.next(); added = true }
            repeat(2) { if (ii.hasNext()) { out += ii.next(); added = true } }
            if (!added) break
        }
        return out
    }
}

/** Small image loader: memory LRU + disk cache, downsampled to the requested width. */
object RemoteImages {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun cached(url: String): Bitmap? = cache.get(url)

    suspend fun load(context: Context, url: String, maxWidth: Int = 900): Bitmap? {
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "img").apply { mkdirs() }
                val f = File(dir, url.hashCode().toUInt().toString(16) + ".bin")
                if (!f.exists() || f.length() == 0L) {
                    val c = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000; readTimeout = 30_000
                        setRequestProperty("User-Agent", "AyatTutla-Android")
                    }
                    try {
                        if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                        val tmp = File(dir, f.name + ".part")
                        c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                        tmp.renameTo(f)
                    } finally { c.disconnect() }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()?.also { cache.put(url, it) }
        }
    }
}
