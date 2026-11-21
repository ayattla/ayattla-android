package com.ayattla.app.audio

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Recitations kept on the device for listening without internet (files/recitations/<reciter>/),
 * the same EveryAyah files, unchanged. Playback and clips use a downloaded file when there is one.
 */
object Downloads {
    /** Surah → progress 0..1 while downloading (key "folder/surah"). */
    val progress = mutableStateMapOf<String, Float>()
    var version by mutableStateOf(0)
        private set

    fun dir(c: Context, folder: String) = File(File(c.filesDir, "recitations"), folder)
    fun file(c: Context, folder: String, surah: Int, ayah: Int) =
        File(dir(c, folder), String.format(Locale.US, "%03d%03d.mp3", surah, ayah))
    fun local(c: Context, folder: String, surah: Int, ayah: Int): File? = file(c, folder, surah, ayah).takeIf { it.length() > 1000 }

    fun isDownloaded(c: Context, folder: String, surah: Int, ayahCount: Int) =
        (1..ayahCount).all { local(c, folder, surah, it) != null } && (surah == 1 || surah == 9 || local(c, folder, 1, 1) != null)

    fun sizeBytes(c: Context, folder: String? = null): Long {
        val root = if (folder != null) dir(c, folder) else File(c.filesDir, "recitations")
        return root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun folders(c: Context): List<String> = File(c.filesDir, "recitations").listFiles().orEmpty().filter { it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }.map { it.name }

    /** Downloads every ayah of [surah] (and the basmala, 1:1, which is played before it). */
    suspend fun download(c: Context, folder: String, surah: Int, ayahCount: Int): Boolean = withContext(Dispatchers.IO) {
        val key = "$folder/$surah"
        val list = buildList { if (surah != 1 && surah != 9) add(1 to 1); for (a in 1..ayahCount) add(surah to a) }
        var ok = true
        list.forEachIndexed { i, (s, a) ->
            progress[key] = i / list.size.toFloat()
            val f = file(c, folder, s, a)
            if (f.length() > 1000) return@forEachIndexed
            val done = runCatching {
                f.parentFile?.mkdirs()
                // Already heard: copied from the kept file, no download needed.
                val url = Recitation.url(folder, s, a)
                if (AudioCache.has(c, url) && AudioCache.copyTo(c, url, f)) return@runCatching
                if (!com.ayattla.app.data.Net.isOnline) error("offline")
                val conn = URL(Recitation.url(folder, s, a)).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 30_000
                try {
                    if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
                    val tmp = File(f.path + ".part")
                    conn.inputStream.use { inp -> tmp.outputStream().use { inp.copyTo(it) } }
                    tmp.renameTo(f)
                } finally { conn.disconnect() }
            }.isSuccess
            if (!done) ok = false
        }
        progress.remove(key)
        version++
        ok
    }

    fun delete(c: Context, folder: String, surah: Int, ayahCount: Int) {
        for (a in 1..ayahCount) file(c, folder, surah, a).delete()
        version++
    }

    fun deleteAll(c: Context, folder: String) { dir(c, folder).deleteRecursively(); version++ }
}
