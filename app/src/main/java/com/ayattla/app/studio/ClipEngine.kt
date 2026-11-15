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
import androidx.media3.transformer.Composition
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
        data class Encoding(val percent: Int) : Stage
    }

    private val root = File(context.cacheDir, "clips").apply { mkdirs() }

    suspend fun make(
        reciter: Reciter, surah: Int, from: Int, to: Int,
        ratio: CardRatio, style: CardStyle, onStage: (Stage) -> Unit,
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

        // 2. One frame per part.
        onStage(Stage.Drawing)
        val frameDir = File(root, "frames").apply { deleteRecursively(); mkdirs() }
        val frames = withContext(Dispatchers.Default) {
            parts.mapIndexed { i, p ->
                val bmp = if (p.text.first().ayah == 0) CardRenderer.renderText(context, p.text.first().text, "سورة ${s.shortName}", ratio, style)
                else CardRenderer.render(context, p.text, p.label, ratio, style)
                File(frameDir, "f$i.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; bmp.recycle() }
            }
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
