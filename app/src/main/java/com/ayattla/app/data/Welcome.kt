package com.ayattla.app.data

import android.content.Context
import com.ayattla.app.screens.ToolUsage

/**
 * First-run answers («تجربة البداية»): what the user wants from the app, the content they like, how they
 * listen and which reminders they chose. Kept only on the device; used to order the home screen and
 * suggest tools — never to hide or lock any tool. Same ids as the iOS app (Welcome.swift).
 */
object Welcome {
    /** One answer: its id (stable, also the stats key), label and the real tools it points to. */
    data class Choice(val id: String, val label: String, val tools: List<String> = emptyList())

    val uses = listOf(
        Choice("quran", "قراءة القرآن الكريم", listOf("mushaf", "khatma", "journey", "search", "tajweed")),
        Choice("listen", "الاستماع إلى القرآن والقراء", listOf("listen", "radio", "downloads", "ruqya")),
        Choice("status", "تصميم حالات واتساب", listOf("status", "ayah-card", "hadith-card", "occasions")),
        Choice("clips", "إنشاء مقاطع قرآنية ومشاركتها", listOf("clip", "ayah-card", "status")),
        Choice("azkar", "الأذكار والأدعية", listOf("azkar", "hisn-almuslim", "duas", "tasbih")),
        Choice("hadith", "الأحاديث النبوية", listOf("hadith", "hadith-card", "hadith-checker")),
        Choice("cards", "البطاقات والخلفيات الإسلامية", listOf("ayah-card", "hadith-card", "status")),
        Choice("gifts", "الإهداءات والتهاني بالمناسبات", listOf("status", "occasions", "ayah-card")),
        Choice("tafsir", "التفسير والتدبر", listOf("topics", "word-by-word", "ayah-of-the-day", "tajweed")),
        Choice("hifz", "الحفظ والتسميع", listOf("hifz-plan", "tasmee", "hifz-test", "listen")),
        Choice("all", "استكشاف جميع الأدوات"),
    )

    val likes = listOf(
        Choice("short", "آيات قصيرة ومؤثرة", listOf("ayah-of-the-day", "ayah-card")),
        Choice("tafsir", "آيات مع التفسير", listOf("topics", "word-by-word")),
        Choice("audio", "مقاطع قرآنية صوتية", listOf("clip", "listen")),
        Choice("designs", "تصاميم إسلامية جاهزة", listOf("status", "ayah-card")),
        Choice("azkar", "أذكار الصباح والمساء", listOf("azkar")),
        Choice("hadith", "أحاديث وأدعية", listOf("hadith", "duas")),
        Choice("varied", "محتوى متنوع"),
    )

    /** Reminders offered at first run: each switches on an existing reminder of the alerts centre. */
    val reminders = listOf(
        Choice("morning", "أذكار الصباح"),
        Choice("evening", "أذكار المساء"),
        Choice("journey", "تذكير يومي بقراءة القرآن"),
        Choice("khatma", "ورد الختمة اليومي"),
        Choice("ayah", "آية اليوم كل صباح"),
    )

    enum class Step(val key: String) { Hello("hello"), Uses("uses"), Likes("likes"), Listen("listen"), Remind("reminders"), Look("theme"), Done("done") }

    private fun sp(c: Context) = c.getSharedPreferences("welcome", Context.MODE_PRIVATE)

    /**
     * Whether the first-run questions should open. Never for people updating from an older version
     * (they set the app up already) and never again once finished or skipped.
     */
    fun shouldShow(c: Context): Boolean {
        val p = sp(c)
        if (p.contains("done")) return !p.getBoolean("done", true)
        val updated = runCatching {
            val info = c.packageManager.getPackageInfo(c.packageName, 0)
            info.lastUpdateTime - info.firstInstallTime > 60_000
        }.getOrDefault(false) || ToolUsage.hasHistory(c)
        if (updated) { p.edit().putBoolean("done", true).putBoolean("upgraded", true).apply(); return false }
        return true
    }

    fun finish(c: Context) = sp(c).edit().putBoolean("done", true).remove("step").apply()
    fun upgraded(c: Context) = sp(c).getBoolean("upgraded", false)

    /** People who updated never saw the first-run note about anonymous stats: they get it once on the home screen. */
    fun statsNoticeDue(c: Context) = upgraded(c) && !sp(c).getBoolean("statsNotice", false)
    fun statsNoticeSeen(c: Context) = sp(c).edit().putBoolean("statsNotice", true).apply()

    /** Where the user stopped, so closing the app mid-way resumes there. */
    fun step(c: Context): Step = Step.entries.firstOrNull { it.key == sp(c).getString("step", null) } ?: Step.Hello
    fun saveStep(c: Context, s: Step) = sp(c).edit().putString("step", s.key).apply()

    fun usesOf(c: Context): Set<String> = sp(c).getStringSet("uses", emptySet())!!.toSet()
    fun likesOf(c: Context): Set<String> = sp(c).getStringSet("likes", emptySet())!!.toSet()
    fun remindersOf(c: Context): Set<String> = sp(c).getStringSet("reminders", emptySet())!!.toSet()
    fun listenOf(c: Context): String = sp(c).getString("listen", "") ?: ""
    fun saveUses(c: Context, v: Set<String>) = sp(c).edit().putStringSet("uses", v).apply()
    fun saveLikes(c: Context, v: Set<String>) = sp(c).edit().putStringSet("likes", v).apply()
    fun saveReminders(c: Context, v: Set<String>) = sp(c).edit().putStringSet("reminders", v).apply()
    fun saveListen(c: Context, v: String) = sp(c).edit().putString("listen", v).apply()

    /** Tools suggested by the answers, in the order chosen (uses first, then content). */
    fun suggested(c: Context): List<String> {
        val u = usesOf(c)
        val l = likesOf(c)
        return (uses.filter { it.id in u }.flatMap { it.tools } + likes.filter { it.id in l }.flatMap { it.tools }).distinct()
    }

    /** Uses the user picked, in the list's order (for the home «مختارات لك» rows). */
    fun pickedUses(c: Context): List<Choice> = usesOf(c).let { u -> uses.filter { it.id in u && it.tools.isNotEmpty() } }
}
