package com.ayattla.app.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Reciter
import java.util.Locale

/**
 * Plays ayat one by one from EveryAyah (as the site does), with the basmala before ayah 1
 * of every surah except Al-Fatiha and At-Tawbah. State is exposed for Compose.
 */
object Recitation {
    private var controller: MediaController? = null
    private var pending: (() -> Unit)? = null

    /** "surah:ayah" of the ayah being recited, "surah:0" during the basmala. */
    var current by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var repeatOne by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val active: Boolean get() = current != null

    fun connect(ctx: Context) {
        if (controller != null) return
        // Bound to the application so the controller survives activities being recreated.
        val context = ctx.applicationContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = sync(player)
                override fun onPlayerError(e: PlaybackException) {
                    error = if (!com.ayattla.app.data.Net.isOnline)
                        "هذه الآيات غير محفوظة على جهازك. حمّلها من «التلاوات المحمّلة» لتسمعها بدون إنترنت."
                    else "تعذر تشغيل التلاوة. تحقق من اتصال الإنترنت."
                }
            })
            sync(c)
            pending?.invoke()
            pending = null
        }, ContextCompat.getMainExecutor(context))
    }

    // ── Sleep timer, continuing to the next surah, and the last listening position.
    /** When the sleep timer stops playback (epoch ms), or null. */
    var sleepAt by mutableStateOf<Long?>(null)
        private set
    /** Stop at the end of the current surah instead of a set time. */
    var sleepAtSurahEnd by mutableStateOf(false)
        private set
    /** After the last ayah of a whole surah, the next surah follows (saved setting). */
    var autoNext by mutableStateOf(true)
        private set
    private val timer = android.os.Handler(android.os.Looper.getMainLooper())
    private val sleepRun = Runnable { controller?.pause(); sleepAt = null }
    private var session by mutableStateOf<Session?>(null)
    /** Whether «السورة التالية» applies to what is playing (mushaf listening). */
    val continuous: Boolean get() = session?.continuous == true

    /** What is playing as a whole surah or range, to continue with the next surah and to resume later. */
    private data class Session(val reciter: Reciter, val surah: Int, val to: Int, val repeatEach: Int, val continuous: Boolean)

    /** Off → 15 → 30 → 60 minutes → end of surah → off. */
    fun cycleSleep() {
        timer.removeCallbacks(sleepRun)
        val steps = listOf(15, 30, 60)
        when {
            sleepAtSurahEnd -> { sleepAtSurahEnd = false; sleepAt = null }
            sleepAt == null -> setSleep(steps[0])
            else -> {
                val left = ((sleepAt!! - System.currentTimeMillis()) / 60_000.0).let { kotlin.math.ceil(it).toInt() }
                val next = steps.firstOrNull { it > left }
                if (next != null) setSleep(next) else { sleepAt = null; sleepAtSurahEnd = true }
            }
        }
    }

    private fun setSleep(minutes: Int) {
        sleepAtSurahEnd = false
        sleepAt = System.currentTimeMillis() + minutes * 60_000L
        timer.postDelayed(sleepRun, minutes * 60_000L)
    }

    /** Minutes left on the sleep timer (for the player). */
    fun sleepLabel(): String = when {
        sleepAtSurahEnd -> "نهاية السورة"
        sleepAt != null -> "${kotlin.math.ceil((sleepAt!! - System.currentTimeMillis()) / 60_000.0).toInt().coerceAtLeast(1)} د"
        else -> "مؤقت"
    }

    fun toggleAutoNext(ctx: Context) {
        autoNext = !autoNext
        ctx.getSharedPreferences("ayat", Context.MODE_PRIVATE).edit().putBoolean("listen.autoNext", autoNext).apply()
    }

    /** The last place listened to: (reciter folder, surah, ayah), kept across launches. */
    fun lastListened(ctx: Context): Triple<String, Int, Int>? =
        ctx.getSharedPreferences("ayat", Context.MODE_PRIVATE).getString("listen.last", null)?.split("|")
            ?.takeIf { it.size == 3 }?.let { (f, s, a) -> s.toIntOrNull()?.let { si -> a.toIntOrNull()?.let { ai -> Triple(f, si, ai) } } }

    private fun saveLast(surah: Int, ayah: Int) {
        val c = appContext ?: return
        val r = session?.reciter ?: return
        c.getSharedPreferences("ayat", Context.MODE_PRIVATE).edit().putString("listen.last", "${r.folder}|$surah|${ayah.coerceAtLeast(1)}").apply()
    }

    private var lastContent: ContentStore? = null

    /** Reciter (or station) name of the current item. */
    var artist by mutableStateOf("")
        private set
    var index by mutableStateOf(0)
        private set
    var count by mutableStateOf(0)
        private set
    val positionMs: Long get() = controller?.currentPosition ?: 0L
    val durationMs: Long get() = controller?.duration?.takeIf { it > 0 } ?: 0L
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    private fun sync(p: Player) {
        artist = p.mediaMetadata.artist?.toString() ?: ""
        index = p.currentMediaItemIndex
        count = p.mediaItemCount
        isPlaying = p.isPlaying
        isBuffering = p.playbackState == Player.STATE_BUFFERING
        current = p.currentMediaItem?.mediaId
        title = p.mediaMetadata.title?.toString() ?: ""
        current?.split(":")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { (s, a) -> saveLast(s, a) }
        if (p.playbackState == Player.STATE_ENDED || p.mediaItemCount == 0) {
            if (p.playbackState == Player.STATE_ENDED) {
                current = null; isPlaying = false
                onSurahEnded()
            }
        }
    }

    /** End of the list: the sleep timer «نهاية السورة» stops here; otherwise the next surah follows. */
    private fun onSurahEnded() {
        val se = session ?: return
        if (sleepAtSurahEnd) { sleepAtSurahEnd = false; session = null; return }
        val ctx = appContext ?: return
        val content = lastContent ?: return
        val whole = se.to >= content.surah(se.surah).ayahCount
        if (se.continuous && autoNext && whole && se.surah < 114) play(ctx, content, se.reciter, se.surah + 1, 1, Int.MAX_VALUE, se.repeatEach, continuous = true)
        else session = null
    }

    fun url(folder: String, surah: Int, ayah: Int): String =
        String.format(Locale.US, "https://everyayah.com/data/%s/%03d%03d.mp3", folder, surah, ayah)

    /** Recite [surah] from [from] to [to] (inclusive). */
    /** [repeatEach]: each ayah is heard that many times in a row (memorisation, kids' Juz Amma). */
    /** [continuous]: the mushaf and the listen buttons; only then does the next surah follow at the end. */
    fun play(context: Context, content: ContentStore, reciter: Reciter, surah: Int, from: Int, to: Int = Int.MAX_VALUE, repeatEach: Int = 1, continuous: Boolean = false) {
        error = null
        appContext = context.applicationContext
        lastContent = content
        autoNext = context.getSharedPreferences("ayat", Context.MODE_PRIVATE).getBoolean("listen.autoNext", true)
        session = Session(reciter, surah, to, repeatEach, continuous)
        val s = content.surah(surah)
        val last = minOf(to, s.ayahCount)
        val items = buildList {
            if (from == 1 && s.hasBasmala) add(item(reciter, surah, 0, "بسم الله الرحمن الرحيم", "1:1", s.shortName))
            for (a in from..last) repeat(repeatEach.coerceIn(1, 10)) { add(item(reciter, surah, a, "سورة ${s.shortName} - آية $a", "$surah:$a", s.shortName)) }
        }
        val run = {
            controller?.run {
                setMediaItems(items)
                setPlaybackSpeed(speed)
                repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                prepare()
                play()
            }
        }
        if (controller == null) { pending = { run() }; connect(context) } else run()
    }

    /** A list of passages, one after another (the full ruqya); [times] repeats the whole list. */
    fun playPassages(context: Context, content: ContentStore, reciter: Reciter, passages: List<Triple<Int, Int, Int>>, times: Int = 1) {
        if (passages.isEmpty()) return
        error = null
        appContext = context.applicationContext
        lastContent = content
        val (fs, fa, fb) = passages.first()
        session = Session(reciter, fs, fb, 1, false)
        val items = buildList {
            repeat(times.coerceIn(1, 7)) {
                passages.forEach { (sId, a, b) ->
                    val s = content.surah(sId)
                    if (a == 1 && s.hasBasmala && sId != 1) add(item(reciter, sId, 0, "بسم الله الرحمن الرحيم", "1:1", s.shortName))
                    for (x in a..minOf(b, s.ayahCount)) add(item(reciter, sId, x, "سورة ${s.shortName} - آية $x", "$sId:$x", s.shortName))
                }
            }
        }
        val run = {
            controller?.run {
                setMediaItems(items); setPlaybackSpeed(speed); repeatMode = Player.REPEAT_MODE_OFF; prepare(); play()
            }
        }
        if (controller == null) { pending = { run() }; connect(context) } else run()
    }

    private var appContext: Context? = null

    private fun item(r: Reciter, surah: Int, ayah: Int, title: String, fileKey: String, album: String): MediaItem {
        val (fs, fa) = fileKey.split(":").map(String::toInt)
        // A downloaded copy plays without internet.
        val local = appContext?.let { Downloads.local(it, r.folder, fs, fa) }
        return MediaItem.Builder()
            .setMediaId("$surah:$ayah")
            .setUri(if (local != null) Uri.fromFile(local) else Uri.parse(url(r.folder, fs, fa)))
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(r.name).setAlbumTitle("سورة $album")
                    .setIsPlayable(true).build()
            )
            .build()
    }

    fun toggle() { controller?.run { if (isPlaying) pause() else play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun stop() {
        controller?.run { stop(); clearMediaItems() }; current = null; isPlaying = false
        session = null; timer.removeCallbacks(sleepRun); sleepAt = null; sleepAtSurahEnd = false
    }

    fun cycleSpeed() {
        val steps = listOf(1f, 1.25f, 1.5f, 0.75f)
        speed = steps[(steps.indexOf(speed).coerceAtLeast(0) + 1) % steps.size]
        controller?.setPlaybackSpeed(speed)
    }

    fun toggleRepeat() {
        repeatOne = !repeatOne
        controller?.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    /** Plays one stream (a radio or a single file) through the same session. */
    fun playStream(context: Context, url: String, name: String) {
        error = null
        session = null
        val it = MediaItem.Builder().setMediaId("stream").setUri(Uri.parse(url))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(name).setArtist("آيات تتلى").build()).build()
        val run = { controller?.run { setMediaItem(it); prepare(); play() } }
        if (controller == null) { pending = { run() }; connect(context) } else run()
    }
}
