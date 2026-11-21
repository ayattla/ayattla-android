package com.ayattla.app.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Every EveryAyah file that is heard is kept on the device (up to 600 MB, the least recently heard
 * leave first), so a surah listened to once plays again without internet. Live radio streams are
 * never kept. The files are stored as downloaded, unchanged.
 */
@OptIn(UnstableApi::class)
object AudioCache {
    private const val MAX_BYTES = 600L * 1024 * 1024
    private var cache: SimpleCache? = null

    fun enabled(c: Context) = c.getSharedPreferences("ayat", Context.MODE_PRIVATE).getBoolean("audio.keepHeard", true)

    @Synchronized
    fun get(c: Context): SimpleCache {
        cache?.let { return it }
        val app = c.applicationContext
        return SimpleCache(File(app.filesDir, "audio-cache"), LeastRecentlyUsedCacheEvictor(MAX_BYTES), StandaloneDatabaseProvider(app))
            .also { cache = it }
    }

    fun bytes(): Long = runCatching { cache?.cacheSpace ?: 0L }.getOrDefault(0L)

    fun clear() {
        val c = cache ?: return
        runCatching { c.keys.toList().forEach { c.removeResource(it) } }
    }

    private fun http() = DefaultHttpDataSource.Factory().setUserAgent("AyatTutla-Android").setConnectTimeoutMs(15_000).setReadTimeoutMs(30_000)

    /** Data source for the player: recitation files through the cache, everything else directly. */
    fun playerFactory(c: Context): DataSource.Factory {
        val app = c.applicationContext
        val plain = DefaultDataSource.Factory(app, http())
        val cached = CacheDataSource.Factory().setCache(get(app)).setUpstreamDataSourceFactory(plain)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DataSource.Factory { Routing(plain.createDataSource(), cached.createDataSource()) { enabled(app) } }
    }

    private fun isRecitation(uri: Uri) = uri.host?.endsWith("everyayah.com") == true

    /** Whether the whole file of [url] is kept. */
    fun has(c: Context, url: String): Boolean = runCatching {
        val sc = get(c)
        val len = ContentMetadata.getContentLength(sc.getContentMetadata(url))
        len > 0 && sc.isCached(url, 0, len)
    }.getOrDefault(false)

    /** Copies [url] into [dest] through the cache (from the device when kept, else downloaded and kept). */
    fun copyTo(c: Context, url: String, dest: File): Boolean = runCatching {
        val src = CacheDataSource.Factory().setCache(get(c)).setUpstreamDataSourceFactory(DefaultDataSource.Factory(c.applicationContext, http())).createDataSource()
        val tmp = File(dest.path + ".part")
        try {
            src.open(DataSpec(Uri.parse(url)))
            tmp.outputStream().use { out ->
                val buf = ByteArray(32 * 1024)
                while (true) {
                    val n = src.read(buf, 0, buf.size)
                    if (n == C.RESULT_END_OF_INPUT) break
                    out.write(buf, 0, n)
                }
            }
        } finally { src.close() }
        tmp.length() > 1000 && tmp.renameTo(dest)
    }.getOrDefault(false)

    /** Opens recitation URLs through the cache and anything else (radio) directly. */
    private class Routing(private val plain: DataSource, private val cached: DataSource, private val keep: () -> Boolean) : DataSource {
        private var current: DataSource? = null
        override fun addTransferListener(transferListener: TransferListener) {
            plain.addTransferListener(transferListener); cached.addTransferListener(transferListener)
        }
        override fun open(dataSpec: DataSpec): Long {
            val src = if (isRecitation(dataSpec.uri) && keep()) cached else plain
            current = src
            return src.open(dataSpec)
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = current!!.read(buffer, offset, length)
        override fun getUri(): Uri? = current?.uri
        override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()
        override fun close() { current?.close(); current = null }
    }
}
