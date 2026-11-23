package com.ayattla.app.data

import android.content.Context
import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * «يومي»: the day split by the five prayers, one short step after each. The step on show follows
 * the real prayer times of the user's city, so it moves with the seasons and never needs setting.
 * The steps reuse tools the app already has; nothing here is new content.
 */
enum class TodaySlot(val key: String, val after: Prayer, val title: String, val line: String) {
    Morning("morning", Prayer.Fajr, "أذكار الصباح", "من الفجر حتى الشروق"),
    Wird("wird", Prayer.Dhuhr, "وردك من القرآن", "صفحات اليوم من هدفك"),
    Evening("evening", Prayer.Asr, "أذكار المساء", "من العصر حتى المغرب"),
    Hadith("hadith", Prayer.Maghrib, "حديث اليوم", "حديث صحيح أو حسن مع مصدره وحكمه"),
    Night("night", Prayer.Isha, "سورة الملك وأذكار النوم", "قبل أن تنام"),
}

object Today {
    private fun minute(h: Double) = PrayerTimes.displayMinute(h)

    /** The step for this moment: the slot of the last prayer whose time has come (before Fajr: last night's). */
    fun slotAt(now: LocalDateTime, t: PrayerTimes): TodaySlot {
        val m = now.hour * 60 + now.minute
        return TodaySlot.entries.lastOrNull { m >= minute(t.hours(it.after)) } ?: TodaySlot.Night
    }

    /** The day a slot belongs to: after midnight and before Fajr, the night still belongs to yesterday. */
    fun dayOf(now: LocalDateTime, t: PrayerTimes): LocalDate {
        val m = now.hour * 60 + now.minute
        return if (m < minute(t.fajr)) now.toLocalDate().minusDays(1) else now.toLocalDate()
    }

    /** The slot after this one, for «التالي». */
    fun next(s: TodaySlot): TodaySlot = TodaySlot.entries[(s.ordinal + 1) % TodaySlot.entries.size]

    // Steps that have no counter of their own (the hadith, Surat al-Mulk) are marked when opened.
    private fun sp(c: Context) = c.getSharedPreferences("today", Context.MODE_PRIVATE)
    fun markOpened(c: Context, day: LocalDate, s: TodaySlot) = sp(c).edit().putBoolean("$day.${s.key}", true).apply()
    fun opened(c: Context, day: LocalDate, s: TodaySlot) = sp(c).getBoolean("$day.${s.key}", false)

    /** Same hadith all day, a new one each day, from whatever list is at hand. */
    fun <T> pickForDay(items: List<T>, day: LocalDate): T? = if (items.isEmpty()) null else items[Math.floorMod(day.toEpochDay(), items.size.toLong()).toInt()]
}
