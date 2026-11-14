package com.ayattla.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File

data class Surah(
    val id: Int,
    val nameAr: String,
    val nameEn: String?,
    val ayahCount: Int,
    val revelation: String?,
    val pageStart: Int?,
    val namePlain: String,
    val hasBasmala: Boolean,
) {
    /** Name without the leading "سورة " for lists and titles. */
    val shortName: String get() = namePlain.removePrefix("سورة ")
    val revelationAr: String get() = if (revelation?.lowercase()?.startsWith("med") == true) "مدنية" else "مكية"
}

data class Ayah(val surah: Int, val ayah: Int, val text: String, val page: Int?, val juz: Int?) {
    val key: String get() = "$surah:$ayah"
}

data class JuzStart(val juz: Int, val page: Int, val surah: Int, val ayah: Int)
data class Reciter(val id: String, val name: String, val folder: String, val highlight: Boolean)
data class ZikrCategory(val id: Int, val title: String)
data class Zikr(val id: Int, val category: Int, val text: String, val count: Int, val reference: String?, val note: String?)

/**
 * Read-only access to content-v1.sqlite, built in CI by tools/content/build_content_pack.py
 * (the same verified pack as the iOS app). Quran text is shown exactly as stored.
 */
class ContentStore private constructor(private val db: SQLiteDatabase) {

    val surahs: List<Surah> by lazy {
        db.rawQuery("SELECT id, name_ar, name_en, ayah_count, revelation, page_start, name_plain, basmala FROM surah ORDER BY id", null).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Surah(
                        c.getInt(0), c.getString(1), c.getStringOrNull(2), c.getInt(3), c.getStringOrNull(4),
                        if (c.isNull(5)) null else c.getInt(5), c.getString(6), c.getInt(7) == 1,
                    )
                )
            }
        }
    }

    fun surah(id: Int): Surah = surahs[id - 1]

    fun ayat(surah: Int, from: Int = 1, to: Int = 999): List<Ayah> =
        db.rawQuery(
            "SELECT surah, ayah, text_uthmani, page, juz FROM ayah WHERE surah = ? AND ayah BETWEEN ? AND ? ORDER BY ayah",
            arrayOf(surah.toString(), from.toString(), to.toString()),
        ).use { it.toAyat() }

    fun ayah(surah: Int, ayah: Int): Ayah? = ayat(surah, ayah, ayah).firstOrNull()

    val juzStarts: List<JuzStart> by lazy {
        db.rawQuery(
            """SELECT a.juz, a.page, a.surah, a.ayah FROM ayah a
               JOIN (SELECT juz, min(surah * 1000 + ayah) k FROM ayah WHERE juz IS NOT NULL GROUP BY juz) m
               ON a.juz = m.juz AND a.surah * 1000 + a.ayah = m.k ORDER BY a.juz""", null,
        ).use { c -> buildList { while (c.moveToNext()) add(JuzStart(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3))) } }
    }

    /** Search on the clean text (no tashkeel, unified alef), same normalization as the pack. */
    fun search(query: String, limit: Int = 200): List<Ayah> {
        val q = SearchText.clean(query)
        if (q.length < 2) return emptyList()
        return db.rawQuery(
            "SELECT surah, ayah, text_uthmani, page, juz FROM ayah WHERE text_clean LIKE ? ORDER BY surah, ayah LIMIT $limit",
            arrayOf("%$q%"),
        ).use { it.toAyat() }
    }

    val reciters: List<Reciter> by lazy {
        db.rawQuery("SELECT id, name, folder, highlight FROM reciter ORDER BY ord", null).use { c ->
            buildList { while (c.moveToNext()) add(Reciter(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1)) }
        }
    }

    val azkarCategories: List<ZikrCategory> by lazy {
        db.rawQuery("SELECT id, title FROM azkar_category ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(ZikrCategory(c.getInt(0), c.getString(1))) }
        }
    }

    fun azkar(category: Int): List<Zikr> =
        db.rawQuery(
            "SELECT id, category, text, count, reference, note FROM azkar WHERE category = ? ORDER BY id",
            arrayOf(category.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Zikr(c.getInt(0), c.getInt(1), c.getString(2), c.getInt(3).coerceAtLeast(1), c.getStringOrNull(4), c.getStringOrNull(5)))
            }
        }

    fun meta(key: String): String? =
        db.rawQuery("SELECT value FROM meta WHERE key = ?", arrayOf(key)).use { if (it.moveToFirst()) it.getString(0) else null }

    val tasbihPhrases: List<String> by lazy {
        val raw = meta("tasbih_phrases").orEmpty()
        runCatching { JSONArray(raw).let { a -> List(a.length()) { a.getString(it) } } }.getOrDefault(emptyList())
            .ifEmpty { listOf("سبحان الله", "الحمد لله", "لا إله إلا الله", "الله أكبر") }
    }

    private fun android.database.Cursor.toAyat(): List<Ayah> = buildList {
        while (moveToNext()) add(
            Ayah(getInt(0), getInt(1), getString(2), if (isNull(3)) null else getInt(3), if (isNull(4)) null else getInt(4))
        )
    }

    private fun android.database.Cursor.getStringOrNull(i: Int): String? = if (isNull(i)) null else getString(i)

    companion object {
        private const val ASSET = "content-v1.sqlite"
        const val EXPECTED_AYAT = 6236

        @Volatile private var instance: ContentStore? = null

        fun get(context: Context): ContentStore = instance ?: synchronized(this) {
            instance ?: open(context.applicationContext).also { instance = it }
        }

        /** Copies the pack out of the APK once per app version (SQLite needs a real file). */
        private fun open(context: Context): ContentStore {
            val dir = File(context.noBackupFilesDir, "content").apply { mkdirs() }
            val file = File(dir, ASSET)
            val version = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            val stamp = File(dir, "version")
            if (!file.exists() || stamp.takeIf { it.exists() }?.readText() != version.toString()) {
                val tmp = File(dir, "$ASSET.tmp")
                context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(file)
                stamp.writeText(version.toString())
            }
            val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
            return ContentStore(db)
        }
    }
}

object SearchText {
    private val tashkeel = Regex("[ؐ-ًؚ-ٰٟۖ-ۭـ]")
    private val alef = Regex("[آأإٱ]")
    private val spaces = Regex("\\s+")

    fun clean(text: String): String = text.replace(tashkeel, "").replace(alef, "ا").replace(spaces, " ").trim()
}

/** The ayah of the day: same list and rotation as the iOS app (AyatContent/DailyAyah). */
object DailyAyah {
    data class Pick(val surah: Int, val from: Int, val to: Int)

    val picks = listOf(
        Pick(1, 1, 7), Pick(2, 255, 255), Pick(2, 285, 286), Pick(94, 5, 6),
        Pick(112, 1, 4), Pick(67, 1, 1), Pick(13, 28, 28),
    )

    fun pick(date: java.time.LocalDate = java.time.LocalDate.now()): Pick {
        val day = date.toEpochDay() + 719163 // day number counted from 0001-01-01
        return picks[(day % picks.size).toInt()]
    }
}
