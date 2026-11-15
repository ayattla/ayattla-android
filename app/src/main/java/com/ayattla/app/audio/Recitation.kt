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
                    error = "تعذر تشغيل التلاوة. تحقق من اتصال الإنترنت."
                }
            })
            sync(c)
            pending?.invoke()
            pending = null
        }, ContextCompat.getMainExecutor(context))
    }

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
        if (p.playbackState == Player.STATE_ENDED || p.mediaItemCount == 0) {
            if (p.playbackState == Player.STATE_ENDED) { current = null; isPlaying = false }
        }
    }

    fun url(folder: String, surah: Int, ayah: Int): String =
        String.format(Locale.US, "https://everyayah.com/data/%s/%03d%03d.mp3", folder, surah, ayah)

    /** Recite [surah] from [from] to [to] (inclusive). */
    /** [repeatEach]: each ayah is heard that many times in a row (memorisation, kids' Juz Amma). */
    fun play(context: Context, content: ContentStore, reciter: Reciter, surah: Int, from: Int, to: Int = Int.MAX_VALUE, repeatEach: Int = 1) {
        error = null
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

    private fun item(r: Reciter, surah: Int, ayah: Int, title: String, fileKey: String, album: String): MediaItem {
        val (fs, fa) = fileKey.split(":").map(String::toInt)
        return MediaItem.Builder()
            .setMediaId("$surah:$ayah")
            .setUri(Uri.parse(url(r.folder, fs, fa)))
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(r.name).setAlbumTitle("سورة $album")
                    .setIsPlayable(true).build()
            )
            .build()
    }

    fun toggle() { controller?.run { if (isPlaying) pause() else play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun stop() { controller?.run { stop(); clearMediaItems() }; current = null; isPlaying = false }

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
        val it = MediaItem.Builder().setMediaId("stream").setUri(Uri.parse(url))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(name).setArtist("آيات تتلى").build()).build()
        val run = { controller?.run { setMediaItem(it); prepare(); play() } }
        if (controller == null) { pending = { run() }; connect(context) } else run()
    }
}
