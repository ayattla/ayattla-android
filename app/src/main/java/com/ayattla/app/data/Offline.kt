package com.ayattla.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Whether the device can reach the internet right now (Compose state).
 * Debug builds can start in a simulated offline state («-e offline 1») for the CI checks.
 */
object Net {
    var online by mutableStateOf(true)
        private set
    /** The connection is Wi-Fi or another network that is not charged by size. */
    var unmetered by mutableStateOf(false)
        private set
    /** Simulated offline (debug only), so screens can be checked without cutting the emulator's network. */
    var simulateOffline by mutableStateOf(false)

    val isOnline: Boolean get() = online && !simulateOffline

    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        fun refresh() {
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            unmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        }
        refresh()
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh()
                override fun onLost(network: Network) = refresh()
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refresh()
            })
        }
    }
}

/** How much of a tool works without internet. */
enum class OfflineLevel(val label: String, val hint: String) {
    Full("يعمل بدون إنترنت", "كل ما في هذه الأداة محفوظ في التطبيق"),
    Saved("يعمل بما حفظته", "يعمل بدون إنترنت بما حمّلته أو فتحته من قبل"),
    Online("يحتاج إنترنت", "هذه الأداة تحتاج اتصالا بالإنترنت"),
}

object OfflineTools {
    /** Tools that need a live connection (live streams, a search on a server). */
    private val online = setOf("radio", "nearby-mosques")
    /** Tools that work with what was downloaded or opened before (recitations, hadith pages, backgrounds). */
    private val saved = setOf(
        "clip", "listen", "ruqya", "kids-juz-amma", "hadith", "hadith-card", "hadith-checker", "tajweed", "word-by-word",
        "asma-ul-husna", "crypto-zakat",
    )

    fun level(id: String): OfflineLevel = when (id) {
        in online -> OfflineLevel.Online
        in saved -> OfflineLevel.Saved
        else -> OfflineLevel.Full
    }
}

/**
 * «جهّز التطبيق للعمل بدون إنترنت»: keeps on the device what the offline tools need — the hadith
 * collection, the tajweed text and word meanings of chosen surahs, and the curated backgrounds.
 * Recitations are downloaded separately (they are large) through [com.ayattla.app.audio.Downloads].
 * Everything is the same data the tools use online, stored as it is.
 */
object OfflinePrep {
    var running by mutableStateOf(false)
        private set
    var stage by mutableStateOf("")
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var lastResult by mutableStateOf<String?>(null)
        private set
    /** Bumped when something was saved, so status counts are read again. */
    var version by mutableStateOf(0)
        private set

    /** Juz Amma and the surahs most read on their own (Al-Fatiha, Al-Kahf, Ya-Sin, Al-Mulk, Al-Waqi'a, Ar-Rahman). */
    val essentialSurahs: List<Int> = listOf(1, 18, 36, 55, 56, 67) + (78..114)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    data class Status(
        val hadithPages: Int, val hadithCount: Int, val tajweedSurahs: Int, val wordSurahs: Int,
        val images: Int, val recitationBytes: Long, val cacheBytes: Long,
    )

    fun status(c: Context): Status {
        val api = File(c.filesDir, "offline/api")
        val qf = File(c.filesDir, "offline/qf")
        val hadithFiles = api.listFiles { f -> f.name.startsWith("hadith-") }.orEmpty()
        val ids = HashSet<String>()
        hadithFiles.forEach { f ->
            runCatching {
                val a = org.json.JSONObject(f.readText()).optJSONArray("items") ?: return@runCatching
                for (i in 0 until a.length()) ids += a.getJSONObject(i).opt("id")?.toString() ?: continue
            }
        }
        val qfFiles = qf.listFiles().orEmpty()
        val tj = qfFiles.count { it.name.startsWith("tj-") }
        val wbw = qfFiles.filter { it.name.startsWith("wbw-") }.map { it.name.split("-").getOrNull(1) }.toSet().size
        val img = File(c.cacheDir, "img").listFiles { f -> f.name.endsWith(".bin") }.orEmpty().size
        val cache = (c.cacheDir.walkTopDown() + File(c.filesDir, "offline").walkTopDown()).filter { it.isFile }.sumOf { it.length() } +
            com.ayattla.app.audio.AudioCache.bytes()
        return Status(hadithFiles.size, ids.size, tj, wbw, img, com.ayattla.app.audio.Downloads.sizeBytes(c), cache)
    }

    fun start(c: Context, surahs: List<Int> = essentialSurahs, hadith: Boolean = true, quranText: Boolean = true, backgrounds: Boolean = true) {
        if (running) return
        val app = c.applicationContext
        running = true; lastResult = null; progress = 0f
        job = scope.launch {
            var failures = 0
            val steps = (if (hadith) 1 else 0) + (if (quranText) 1 else 0) + (if (backgrounds) 1 else 0)
            var done = 0
            fun part(p: Float) { progress = ((done + p.coerceIn(0f, 1f)) / steps.coerceAtLeast(1)) }
            try {
                if (hadith) {
                    stage = "الأحاديث الصحيحة والحسنة"
                    failures += prepHadith(app) { part(it) }
                    done++
                }
                if (quranText) {
                    stage = "التجويد ومعاني الكلمات"
                    surahs.forEachIndexed { i, s ->
                        part(i / surahs.size.toFloat())
                        if (runCatching { SiteApi.tajweed(app, s) }.isFailure) failures++
                        if (runCatching { SiteApi.words(app, s, "en") }.isFailure) failures++
                        version++
                    }
                    done++
                }
                if (backgrounds) {
                    stage = "الخلفيات المختارة"
                    val cats = listOf("mosques", "nature", "night", "sky", "sea")
                    cats.forEachIndexed { i, cat ->
                        part(i / cats.size.toFloat())
                        val items = MediaLibrary.curated(MediaLibrary.search(app, "image", cat))
                        items.forEach { m -> m.still?.let { if (RemoteImages.load(app, it, 1400) == null) failures++ } }
                        version++
                    }
                    done++
                }
                progress = 1f
                lastResult = if (failures == 0) "اكتمل التجهيز. الأدوات تعمل الآن بدون إنترنت."
                else "اكتمل التجهيز إلا بعض الملفات. أعد المحاولة عند اتصال أفضل لإكمالها."
                c.getSharedPreferences("ayat", Context.MODE_PRIVATE).edit().putLong("offline.prepAt", System.currentTimeMillis()).apply()
            } catch (e: kotlinx.coroutines.CancellationException) {
                lastResult = "أوقفت التجهيز. ما حُفظ يبقى على جهازك."
                throw e
            } finally {
                running = false; stage = ""; version++
            }
        }
    }

    fun cancel() { job?.cancel() }

    /** Downloads the recitation of [surahs] with [reciter] (Juz Amma by default) for listening offline. */
    fun downloadRecitations(c: Context, content: ContentStore, reciter: Reciter, surahs: List<Int> = (78..114).toList(), label: String = "جزء عم") {
        if (running) return
        val app = c.applicationContext
        running = true; lastResult = null; progress = 0f
        job = scope.launch {
            var failures = 0
            try {
                surahs.forEachIndexed { i, id ->
                    val s = content.surah(id)
                    stage = "تلاوة $label · سورة ${s.shortName}"
                    progress = i / surahs.size.toFloat()
                    if (!com.ayattla.app.audio.Downloads.download(app, reciter.folder, id, s.ayahCount)) failures++
                }
                progress = 1f
                lastResult = if (failures == 0) "حُمّلت تلاوة $label بصوت ${reciter.name}. تسمعها الآن بدون إنترنت."
                else "حُمّلت أغلب السور. أعد المحاولة عند اتصال أفضل لإكمال $failures سورة."
            } catch (e: kotlinx.coroutines.CancellationException) {
                lastResult = "أوقفت التحميل. ما حُمّل يبقى على جهازك."
                throw e
            } finally { running = false; stage = ""; version++ }
        }
    }

    /** Approximate size of a surah list's recitation (EveryAyah files average about 60 KB an ayah at 64 kbps). */
    fun estimateMb(content: ContentStore, surahs: List<Int>): Int = (surahs.sumOf { content.surah(it).ayahCount } * 60 / 1024).coerceAtLeast(1)

    /** Every page of every hadith category (12 a page, as the site), so browsing and search work offline. */
    private suspend fun prepHadith(c: Context, onProgress: (Float) -> Unit): Int {
        var failures = 0
        val first = runCatching { SiteApi.hadith(c, "", 1, "") }.getOrElse { return 1 }
        val pages = buildList {
            first.cats.forEach { (cat, count) -> for (p in 1..((count + 11) / 12).coerceAtLeast(1)) add(cat to p) }
        }
        pages.forEachIndexed { i, (cat, p) ->
            onProgress(i / pages.size.toFloat())
            if (runCatching { SiteApi.hadith(c, cat, p, "", preferNetwork = false) }.isFailure) failures++
            if (i % 10 == 0) version++
        }
        return failures
    }

    /** Last automatic preparation, so it runs at most once a week, on Wi-Fi, in release builds. */
    fun maybeAuto(c: Context) {
        val debuggable = (c.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable || !Net.isOnline || !Net.unmetered) return
        val sp = c.getSharedPreferences("ayat", Context.MODE_PRIVATE)
        if (!sp.getBoolean("offline.auto", true)) return
        if (System.currentTimeMillis() - sp.getLong("offline.prepAt", 0) < 7L * 86_400_000) return
        start(c)
    }

    /** Removes the saved hadith, tajweed, meanings, backgrounds and listened recitations (not the downloaded surahs). */
    fun clearSaved(c: Context) {
        File(c.filesDir, "offline").deleteRecursively()
        listOf("api", "qf", "img", "media").forEach { File(c.cacheDir, it).deleteRecursively() }
        com.ayattla.app.audio.AudioCache.clear()
        c.getSharedPreferences("ayat", Context.MODE_PRIVATE).edit().remove("offline.prepAt").apply()
        version++
    }
}
