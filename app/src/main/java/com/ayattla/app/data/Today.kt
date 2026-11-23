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

/** A short seasonal note shown with «يومي» (Friday, Ramadan, the ten days of Dhul-Hijjah, voluntary fasts). */
data class SeasonNote(val key: String, val title: String, val line: String, val route: String)

/**
 * The seasons the day belongs to, read from the Hijri date and the weekday. Only well-known
 * practices with a sound basis; wording without tashkeel like the rest of the app.
 */
object Seasons {
    fun notes(
        date: LocalDate, hijriMonth: Int, hijriDay: Int, slot: TodaySlot,
        maghrib: String, fajrTomorrow: String,
    ): List<SeasonNote> = buildList {
        val beforeMaghrib = slot == TodaySlot.Morning || slot == TodaySlot.Wird || slot == TodaySlot.Evening
        val dow = date.dayOfWeek
        // Friday: Surat al-Kahf and sending blessings on the Prophet, during the day.
        if (dow == java.time.DayOfWeek.FRIDAY && beforeMaghrib) {
            add(SeasonNote("kahf", "سورة الكهف", "من سنن يوم الجمعة", "reader/18/1"))
            add(SeasonNote("salawat", "الصلاة على النبي", "أكثروا منها يوم الجمعة", "tasbih"))
        }
        // Ramadan: the iftar time by day, tarawih after Maghrib, suhoor at night, and the month's khatma.
        if (hijriMonth == 9) {
            if (beforeMaghrib) add(SeasonNote("iftar", "الإفطار $maghrib", "موعد أذان المغرب اليوم", "prayer"))
            if (slot == TodaySlot.Hadith) add(SeasonNote("tarawih", "صلاة التراويح", "بعد صلاة العشاء", "prayer"))
            if (slot == TodaySlot.Night) add(SeasonNote("suhoor", "السحور قبل $fajrTomorrow", "موعد أذان الفجر", "prayer"))
            add(SeasonNote("khatma", "ختمة رمضان", "وزع المصحف على أيام الشهر", "khatma"))
        }
        // The first ten days of Dhul-Hijjah: takbir; fasting the day of Arafah; the day of Eid.
        if (hijriMonth == 12 && hijriDay in 1..9) add(SeasonNote("takbir", "التكبير في العشر", "الله أكبر ولله الحمد", "tasbih"))
        if (hijriMonth == 12 && hijriDay == 8 && !beforeMaghrib) add(SeasonNote("arafah-eve", "غدا يوم عرفة", "صيامه يكفر سنتين", "sawm"))
        if (hijriMonth == 12 && hijriDay == 9) add(SeasonNote("arafah", "يوم عرفة", "صيامه يكفر سنتين، وأكثر من الدعاء", "duas"))
        if (hijriMonth == 12 && hijriDay == 10) add(SeasonNote("eid", "عيد الأضحى", "تقبل الله منا ومنكم", "occasions"))
        // Voluntary fasts: Monday and Thursday, and the white days (13th–15th), named the night before.
        if (slot == TodaySlot.Night && hijriMonth != 9) {
            val tomorrow = date.plusDays(1).dayOfWeek
            if (tomorrow == java.time.DayOfWeek.MONDAY) add(SeasonNote("fast-mon", "صيام الاثنين غدا", "من صيام التطوع", "sawm"))
            if (tomorrow == java.time.DayOfWeek.THURSDAY) add(SeasonNote("fast-thu", "صيام الخميس غدا", "من صيام التطوع", "sawm"))
            if (hijriDay in 12..14) add(SeasonNote("white", "الأيام البيض", "غدا اليوم ${hijriDay + 1} من الشهر", "sawm"))
        }
    }
}
