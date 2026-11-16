package com.ayattla.app.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.RemoteImages
import com.ayattla.app.data.Reciter
import com.ayattla.app.ui.theme.Num
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Makes an MP4 on the device from a [Project]: the background (video looping, photo, or colour)
 * under an animated text layer; each ayah stays for exactly its recitation (plus the pause the user
 * chose), and the recitation is the sound track. Recitations come from EveryAyah, unchanged.
 */
class ClipEngine(private val context: Context, private val content: ContentStore) {

    sealed interface Stage {
        data class Downloading(val done: Int, val total: Int) : Stage
        data object Background : Stage
        data object Drawing : Stage
        data class Encoding(val percent: Int) : Stage
    }

    private val root = File(context.cacheDir, "clips").apply { mkdirs() }

    /** Text parts of a project in order (the basmala before ayah 1 of a surah that has it). */
    fun parts(p: Project): List<Pair<Part, Pair<Int, Int>>> = buildList {
        p.ranges.forEach { r ->
            val s = content.surah(r.surah)
            if (r.from == 1 && s.hasBasmala) {
                val b = content.ayah(1, 1)!!
                add(Part(b.text, "سورة ${s.shortName}", listOf("${r.surah}:0")) to (1 to 1))
            }
            content.ayat(r.surah, r.from, r.to).forEach { a ->
                add(Part(a.text + " " + Num.ayahMark(a.ayah), "سورة ${s.shortName} · ${a.ayah}", listOf(a.key)) to (r.surah to a.ayah))
            }
        }
    }

    suspend fun make(p: Project, reciter: Reciter, onStage: (Stage) -> Unit): File {
        val parts = parts(p).take(MAX_AYAT + p.ranges.size)

        // 1. Recitation files (cached), and the pauses as silence.
        val audioDir = File(root, "audio").apply { mkdirs() }
        val files = parts.mapIndexed { i, (_, audio) ->
            onStage(Stage.Downloading(i, parts.size))
            com.ayattla.app.audio.Downloads.local(context, reciter.folder, audio.first, audio.second) ?: run {
                val f = File(audioDir, "${reciter.folder}_${audio.first}_${audio.second}.mp3")
                if (!f.exists() || f.length() < 1000) download(Recitation.url(reciter.folder, audio.first, audio.second), f)
                f
            }
        }
        onStage(Stage.Downloading(parts.size, parts.size))
        val pauses = parts.map { (part, _) -> (p.pauses[part.keys.first()] ?: 0).coerceIn(0, 5000).toLong() }
        val durations = files.mapIndexed { i, f -> durationMs(f) + pauses[i] }

        // 2. Background.
        val (w, h) = Composer.size(p.ratio, p.quality)
        val bg = p.background
        onStage(Stage.Background)
        val bgVideo: File? = when (bg) {
            is Background.Local -> if (bg.isVideo) File(bg.path).takeIf { it.exists() } else null
            is Background.Library -> if (bg.media.isVideo) bg.media.previewUrl?.let { url ->
                val f = File(File(root, "bg").apply { mkdirs() }, "v-" + url.hashCode().toUInt().toString(16) + ".mp4")
                runCatching { if (!f.exists() || f.length() < 10_000) download(url, f) }
                f.takeIf { it.exists() && it.length() >= 10_000 }
            } else null
            Background.Color -> null
        }
        val photo: Bitmap? = when (bg) {
            is Background.Local -> if (!bg.isVideo) android.graphics.BitmapFactory.decodeFile(bg.path) else null
            is Background.Library -> if (!bg.media.isVideo) bg.media.still?.let { RemoteImages.load(context, it, (w * 1.2f).toInt()) } else null
            Background.Color -> null
        }
        val onMedia = bgVideo != null || photo != null
        val composer = Composer(context, p.style, w, h, onMedia)

        // 3. Layers: one still background image (when there is no video), the base, one text layer per part.
        onStage(Stage.Drawing)
        val frameDir = File(root, "frames").apply { deleteRecursively(); mkdirs() }
        val stillBg = if (bgVideo == null) withContext(Dispatchers.Default) {
            File(frameDir, "bg.png").also { f -> composer.backgroundBitmap(photo).let { b -> f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }; b.recycle() } }
        } else null
        photo?.recycle()
        val overlay = AnimatedText(composer, parts.map { it.first }, durations)

        // 4. Compose and encode.
        val total = durations.sum()
        val bgItem = if (bgVideo != null) EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(bgVideo))).setRemoveAudio(true)
        else EditedMediaItem.Builder(MediaItem.Builder().setUri(Uri.fromFile(stillBg)).setImageDurationMs(total).build()).setFrameRate(30)
        bgItem.setEffects(Effects(listOf(), listOf(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))))
        val audio = mutableListOf<EditedMediaItem>()
        files.forEachIndexed { i, f ->
            audio += EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(f))).build()
            if (pauses[i] > 0) audio += EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(silence(pauses[i], f)))).build()
        }
        val videoSeq = EditedMediaItemSequence.Builder(listOf(bgItem.build())).apply { if (bgVideo != null) setIsLooping(true) }.build()
        val composition = Composition.Builder(videoSeq, EditedMediaItemSequence.Builder(audio).build())
            .setEffects(Effects(listOf(), listOf(OverlayEffect(ImmutableList.of<TextureOverlay>(overlay))))).build()
        val out = File(ProjectStore.exportsDir(context), "ayattla-${p.ranges.first().surah}-${System.currentTimeMillis()}.mp4")
        try {
            encode(composition, out, p.quality.bitrate, onStage)
        } catch (e: ExportException) {
            // 4K is not supported by every encoder: fall back to 1080p once.
            if (p.quality != Quality.Q2160) throw e
            out.delete()
            return make(p.copy(quality = Quality.Q1080), reciter, onStage)
        }
        // Keep only the last few exports in the app (the user's copies are in the gallery).
        ProjectStore.exportsDir(context).listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(6).forEach { it.delete() }
        return out
    }

    /**
     * The text layer over time: per frame, which part is on screen and how far into it; only redrawn
     * while something moves (two buffers in turn, so the overlay sees a new bitmap when it changes).
     */
    private class AnimatedText(private val composer: Composer, private val parts: List<Part>, durations: List<Long>) : BitmapOverlay() {
        private val starts = durations.runningFold(0L) { a, b -> a + b }
        private val durs = durations
        private val base = composer.baseLayer()
        private val buffers = arrayOf(newBuffer(), newBuffer())
        private var which = 0
        private var lastKey = ""
        private var layerKey = ""
        private var layer: Bitmap? = null

        private fun newBuffer() = Bitmap.createBitmap(composer.w, composer.h, Bitmap.Config.ARGB_8888)

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val ms = presentationTimeUs / 1000
            var i = starts.indexOfLast { it <= ms }.coerceIn(0, parts.lastIndex)
            if (i > parts.lastIndex) i = parts.lastIndex
            val t = (ms - starts[i]).coerceAtLeast(0)
            val d = durs[i]
            val style = composer.style
            val words = if (style.anim == Anim.Words) {
                val n = composer.words(parts[i])
                val span = (d * style.wordPace).coerceAtLeast(1f)
                ((t / span) * n).toInt().plus(1).coerceIn(1, n)
            } else Int.MAX_VALUE
            val lk = "$i/$words"
            if (lk != layerKey) { layer?.recycle(); layer = composer.textLayer(parts[i], words); layerKey = lk }
            val anim = style.anim
            val moving = anim != Anim.None && anim != Anim.Words &&
                (t < style.animMs || (i < parts.lastIndex && d - t < style.animMs * 0.6f))
            val key = if (moving) "$lk@${t / 33}" else "$lk@still"
            if (key == lastKey) return buffers[which]
            lastKey = key
            which = 1 - which
            val out = buffers[which]
            val c = Canvas(out)
            Composer.clear(c)
            c.drawBitmap(base, 0f, 0f, null)
            composer.drawAnimated(c, layer!!, t, d, i == parts.lastIndex)
            return out
        }
    }

    private suspend fun encode(composition: Composition, out: File, bitrate: Int, onStage: (Stage) -> Unit) = withContext(Dispatchers.Main) {
        coroutineScope {
            lateinit var transformer: Transformer
            val poll = launch {
                val holder = ProgressHolder()
                while (true) {
                    delay(300)
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onStage(Stage.Encoding(holder.progress))
                }
            }
            try {
                suspendCancellableCoroutine { cont ->
                    transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(
                            DefaultEncoderFactory.Builder(context)
                                .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                                .build(),
                        )
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) { if (cont.isActive) cont.resume(Unit) }
                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()
                    cont.invokeOnCancellation { transformer.cancel() }
                    transformer.start(composition, out.path)
                }
            } finally { poll.cancel() }
        }
    }

    /** A silent WAV with the same sample rate and channels as [like], [ms] long. */
    private fun silence(ms: Long, like: File): File {
        val r = MediaMetadataRetriever()
        val (rate, ch) = try {
            r.setDataSource(like.path)
            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100) to 2
        } catch (_: Exception) { 44100 to 2 } finally { r.release() }
        val f = File(File(root, "silence").apply { mkdirs() }, "s-$ms-$rate.wav")
        if (f.exists()) return f
        val frames = (rate * ms / 1000).toInt()
        val data = frames * ch * 2
        RandomAccessFile(f, "rw").use { o ->
            fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
            fun le16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
            o.write("RIFF".toByteArray()); o.write(le32(36 + data)); o.write("WAVEfmt ".toByteArray())
            o.write(le32(16)); o.write(le16(1)); o.write(le16(ch)); o.write(le32(rate)); o.write(le32(rate * ch * 2)); o.write(le16(ch * 2)); o.write(le16(16))
            o.write("data".toByteArray()); o.write(le32(data)); o.write(ByteArray(data))
        }
        return f
    }

    private suspend fun download(url: String, to: File) = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            val tmp = File(to.path + ".tmp")
            c.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            tmp.renameTo(to)
        } finally { c.disconnect() }
    }

    private fun durationMs(f: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 4000L
        } finally { r.release() }
    }

    companion object {
        const val MAX_AYAT = 30

        fun saveToGallery(context: Context, file: File): Uri? {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AyatTutla")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val r = context.contentResolver
            val uri = r.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            r.openOutputStream(uri)?.use { o -> file.inputStream().use { it.copyTo(o) } }
            if (Build.VERSION.SDK_INT >= 29) { values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0); r.update(uri, values, null, null) }
            return uri
        }

        fun shareUri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "com.ayattla.app.fileprovider", file)
    }
}
