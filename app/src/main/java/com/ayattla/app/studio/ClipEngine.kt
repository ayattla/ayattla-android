package com.ayattla.app.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Effects
import com.ayattla.app.data.SiteMedia
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.Ayah
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Reciter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Makes an MP4 on the device: one frame per ayah (Quran text exactly as in the pack, Amiri Quran)
 * shown for exactly as long as that ayah's recitation, with the recitation as the sound track.
 * Recitation files come from EveryAyah, the same source the site uses; nothing is changed in them.
 */
class ClipEngine(private val context: Context, private val content: ContentStore) {

    sealed interface Stage {
        data class Downloading(val done: Int, val total: Int) : Stage
        data object Drawing : Stage
        data object Background : Stage
        data class Encoding(val percent: Int) : Stage
    }

    private val root = File(context.cacheDir, "clips").apply { mkdirs() }

    suspend fun make(
        reciter: Reciter, surah: Int, from: Int, to: Int,
        ratio: CardRatio, style: CardStyle, background: SiteMedia? = null, onStage: (Stage) -> Unit,
    ): File {
        val s = content.surah(surah)
        // Parts in order: the basmala first when the clip starts at ayah 1 (not Al-Fatiha or At-Tawbah).
        data class Part(val text: List<Ayah>, val audio: Pair<Int, Int>, val label: String)
        val ayat = content.ayat(surah, from, to)
        val parts = buildList {
            if (from == 1 && s.hasBasmala) {
                val basmala = content.ayah(1, 1)!!
                add(Part(listOf(basmala.copy(ayah = 0)), 1 to 1, "بسم الله الرحمن الرحيم"))
            }
            ayat.forEach { add(Part(listOf(it), surah to it.ayah, "سورة ${s.shortName} · ${it.ayah}")) }
        }

        // 1. Recitation files (cached).
        val audioDir = File(root, "audio").apply { mkdirs() }
        val files = parts.mapIndexed { i, p ->
            onStage(Stage.Downloading(i, parts.size))
            val f = File(audioDir, "${reciter.folder}_${p.audio.first}_${p.audio.second}.mp3")
            if (!f.exists() || f.length() < 1000) download(Recitation.url(reciter.folder, p.audio.first, p.audio.second), f)
            f
        }
        onStage(Stage.Downloading(parts.size, parts.size))
        val durations = files.map { durationMs(it) }

        // 1b. The real background from the site's library: a looping video or a photo.
        val bgVideo: File? = if (background?.isVideo == true) background.previewUrl?.let { url ->
            onStage(Stage.Background)
            val f = File(File(root, "bg").apply { mkdirs() }, "v-" + url.hashCode().toUInt().toString(16) + ".mp4")
            runCatching { if (!f.exists() || f.length() < 10_000) download(url, f) }.getOrNull()
            f.takeIf { it.exists() && it.length() >= 10_000 }
        } else null
        val photo: Bitmap? = if (background != null && !background.isVideo) background.still?.let { url ->
            onStage(Stage.Background)
            com.ayattla.app.data.RemoteImages.load(context, url, 1600)
        } else null

        // 2. One frame per part (over a video: text and veil only, on a transparent frame).
        onStage(Stage.Drawing)
        val frameDir = File(root, "frames").apply { deleteRecursively(); mkdirs() }
        val overlay = bgVideo != null
        val frames = withContext(Dispatchers.Default) {
            parts.mapIndexed { i, p ->
                val bmp = if (p.text.first().ayah == 0) CardRenderer.renderText(context, p.text.first().text, "سورة ${s.shortName}", ratio, style, photo = photo, overlay = overlay)
                else CardRenderer.render(context, p.text, p.label, ratio, style, photo = photo, overlay = overlay)
                File(frameDir, "f$i.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; bmp.recycle() }
            }
        }
        if (bgVideo != null) {
            val out = File(File(root, "out").apply { mkdirs() }, "ayattla-$surah-$from-$to.mp4").apply { delete() }
            encode(videoComposition(bgVideo, frames, durations, files, ratio), out, onStage)
            return out
        }

        // 3. Encode.
        val out = File(File(root, "out").apply { mkdirs() }, "ayattla-$surah-$from-$to.mp4").apply { delete() }
        val video = frames.mapIndexed { i, f ->
            EditedMediaItem.Builder(MediaItem.Builder().setUri(Uri.fromFile(f)).setImageDurationMs(durations[i]).build())
                .setFrameRate(15).build()
        }
        val audio = files.map { EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(it))).build() }
        val composition = Composition.Builder(
            EditedMediaItemSequence.Builder(video).build(),
            EditedMediaItemSequence.Builder(audio).build(),
        ).build()
        encode(composition, out, onStage)
        return out
    }

    private suspend fun encode(composition: Composition, out: File, onStage: (Stage) -> Unit) = withContext(Dispatchers.Main) {
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

    /**
     * The background video loops under the recitation; each ayah's frame is laid over it for exactly
     * that ayah's recitation (a timed bitmap overlay), and the video's own sound is removed.
     */
    private fun videoComposition(bg: File, frames: List<File>, durations: List<Long>, audioFiles: List<File>, ratio: CardRatio): Composition {
        val ends = durations.runningReduce { a, b -> a + b }.map { it * 1000 }
        val overlay = TimedFrames(frames, ends)
        val bgItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(bg)))
            .setRemoveAudio(true)
            .setEffects(Effects(listOf(), listOf(Presentation.createForWidthAndHeight(ratio.w, ratio.h, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))))
            .build()
        val audio = audioFiles.map { EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(it))).build() }
        return Composition.Builder(
            EditedMediaItemSequence.Builder(listOf(bgItem)).setIsLooping(true).build(),
            EditedMediaItemSequence.Builder(audio).build(),
        ).setEffects(Effects(listOf(), listOf(OverlayEffect(com.google.common.collect.ImmutableList.of<TextureOverlay>(overlay))))).build()
    }

    /** Shows frame i until ends[i] (microseconds from the start of the clip). */
    private class TimedFrames(private val frames: List<File>, private val ends: List<Long>) : BitmapOverlay() {
        private var index = -1
        private var bitmap: Bitmap? = null
        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            var i = ends.indexOfFirst { presentationTimeUs < it }
            if (i < 0) i = frames.lastIndex
            if (i != index || bitmap == null) {
                bitmap?.recycle()
                bitmap = android.graphics.BitmapFactory.decodeFile(frames[i].path)
                index = i
            }
            return bitmap!!
        }
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
        const val MAX_AYAT = 20

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
