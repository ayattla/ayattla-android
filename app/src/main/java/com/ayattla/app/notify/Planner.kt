package com.ayattla.app.notify

import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The notification center's rules, shared in meaning with the iPhone app. Pure: it only turns the
 * settings and the day's prayer times into a list of dated alerts, so it is unit tested; each
 * platform schedules the list with its own system (AlarmManager here, UNUserNotificationCenter there).
 */
enum class Sound(val title: String) { Default("صوت التنبيه"), Vibrate("اهتزاز فقط"), Silent("صامت") }

data class PrayerAlert(
    val on: Boolean = false,
    val sound: Sound = Sound.Default,
    /** Reminder this many minutes before the time (0 = none). */
    val before: Int = 0,
    /** Reminder of the iqama this many minutes after the time (0 = none). */
    val iqama: Int = 0,
)

data class Personal(val id: String, val title: String, val minute: Int, val days: Set<Int>) // days: 1 = Monday … 7 = Sunday

data class NotifyConfig(
    val prayers: Map<Prayer, PrayerAlert> = PRAYERS.associateWith { PrayerAlert() },
    val afterPrayer: Boolean = false, val afterPrayerDelay: Int = 10,
    val morning: Boolean = false, val morningOffset: Int = 30,   // minutes after Fajr
    val evening: Boolean = false, val eveningOffset: Int = 30,   // minutes after Asr
    val sleep: Boolean = false, val sleepAt: Int = 22 * 60 + 30,
    val wake: Boolean = false,                                   // at Fajr minus 20 min
    val istighfar: Boolean = false, val istighfarAt: Int = 16 * 60 + 30,
    val ayah: Boolean = false, val ayahAt: Int = 9 * 60,
    val khatma: Boolean = false, val khatmaAt: Int = 20 * 60,
    val hifz: Boolean = false, val hifzAt: Int = 19 * 60,
    val friday: Boolean = false, val fridayAt: Int = 10 * 60,
    val fasting: Boolean = false, val fastingAt: Int = 21 * 60,  // eve of Monday/Thursday
    val whiteDays: Boolean = false,                               // eve of the 13th, 14th, 15th
    val personal: List<Personal> = emptyList(),
    val quiet: Boolean = false, val quietFrom: Int = 23 * 60, val quietTo: Int = 5 * 60,
    val snooze: Int = 10,
    // Prayer-time calculation (used everywhere times are shown).
    val method: String = "auto",
    val asrHanafi: Boolean = false,
    val adjust: Map<Prayer, Int> = emptyMap(),
) {
    val anyOn: Boolean get() = prayers.values.any { it.on } || afterPrayer || morning || evening || sleep || wake || istighfar ||
        ayah || khatma || hifz || friday || fasting || whiteDays || personal.isNotEmpty()

    companion object { val PRAYERS = Prayer.entries.filter { it.isPrayer } }
}

/** One notification at a time. [open] is what a tap opens ("route:…" or "azkar:<word in the category>"). */
data class Alert(val id: String, val at: LocalDateTime, val title: String, val body: String, val channel: String, val open: String)

object Channels {
    const val PRAYER = "prayer"; const val PRAYER_VIBRATE = "prayer_vibrate"; const val PRAYER_SILENT = "prayer_silent"
    const val PRAYER_PRE = "prayer_pre"; const val AZKAR = "azkar"; const val QURAN = "quran"
    const val OCCASIONS = "occasions"; const val PERSONAL = "personal"
    /** Channels that quiet hours never hold back. */
    val prayerChannels = setOf(PRAYER, PRAYER_VIBRATE, PRAYER_SILENT, PRAYER_PRE)
}

/** What the planner needs from the rest of the app (content, plans, calendar). */
class PlanInputs(
    val place: String,
    val times: (LocalDate) -> PrayerTimes,
    val ayahText: (LocalDate) -> String? = { null },
    val khatmaWird: (LocalDate) -> String? = { null },
    val hifzToday: (LocalDate) -> String? = { null },
    val hijriDay: (LocalDate) -> Int? = { null },
)

object Planner {
    fun minute(hours: Double) = PrayerTimes.displayMinute(hours)

    fun plan(c: NotifyConfig, now: LocalDateTime, days: Int, inp: PlanInputs): List<Alert> {
        val out = ArrayList<Alert>()
        for (d in 0 until days) {
            val date = now.toLocalDate().plusDays(d.toLong())
            val t = inp.times(date)
            fun at(m: Int): LocalDateTime = date.atStartOfDay().plusMinutes(m.toLong())
            val key = date.toString()

            // The five prayers (sunrise is never a prayer and gets no alert).
            for (p in NotifyConfig.PRAYERS) {
                val a = c.prayers[p] ?: continue
                val m = minute(t.hours(p))
                if (a.on) {
                    val ch = when (a.sound) { Sound.Default -> Channels.PRAYER; Sound.Vibrate -> Channels.PRAYER_VIBRATE; Sound.Silent -> Channels.PRAYER_SILENT }
                    out += Alert("prayer.${p.name}.$key", at(m), "حان وقت صلاة ${p.title}", "${inp.place} · ${PrayerTimes.hm12(t.hours(p))}", ch, "route:prayer")
                    if (a.before > 0) out += Alert("pre.${p.name}.$key", at(m - a.before), "صلاة ${p.title} بعد ${a.before} دقيقة",
                        "${inp.place} · ${PrayerTimes.hm12(t.hours(p))}", Channels.PRAYER_PRE, "route:prayer")
                    if (a.iqama > 0) out += Alert("iqama.${p.name}.$key", at(m + a.iqama), "إقامة صلاة ${p.title}",
                        "حان موعد الإقامة الذي حددته (${a.iqama} دقيقة بعد الأذان)", Channels.PRAYER_PRE, "route:prayer")
                }
                if (c.afterPrayer) out += Alert("after.${p.name}.$key", at(m + c.afterPrayerDelay + a.iqama.coerceAtLeast(0)),
                    "الأذكار بعد الصلاة", "أذكار ما بعد السلام من صلاة ${p.title}", Channels.AZKAR, "azkar:بعد السلام")
            }
            if (c.wake) out += Alert("wake.$key", at(minute(t.fajr) - 20), "أذكار الاستيقاظ", "اقترب الفجر: أذكار الاستيقاظ من النوم", Channels.AZKAR, "azkar:الاستيقاظ")
            if (c.morning) out += Alert("morning.$key", at(minute(t.fajr) + c.morningOffset), "أذكار الصباح", "حان وقت أذكار الصباح", Channels.AZKAR, "azkar:الصباح")
            if (c.evening) out += Alert("evening.$key", at(minute(t.asr) + c.eveningOffset), "أذكار المساء", "حان وقت أذكار المساء", Channels.AZKAR, "azkar:المساء")
            if (c.sleep) out += Alert("sleep.$key", at(c.sleepAt), "أذكار النوم", "قبل أن تنام: أذكار النوم", Channels.AZKAR, "azkar:أذكار النوم")
            if (c.istighfar) out += Alert("istighfar.$key", at(c.istighfarAt), "الاستغفار والتسبيح", "دقيقة للاستغفار والتسبيح", Channels.AZKAR, "route:tasbih")
            if (c.ayah) out += Alert("ayah.$key", at(c.ayahAt), "آية اليوم", inp.ayahText(date) ?: "آية اليوم مع تفسيرها", Channels.QURAN, "route:ayah-today")
            if (c.khatma) inp.khatmaWird(date)?.let { out += Alert("khatma.$key", at(c.khatmaAt), "ورد الختمة", it, Channels.QURAN, "route:khatma") }
            if (c.hifz) inp.hifzToday(date)?.let { out += Alert("hifz.$key", at(c.hifzAt), "الحفظ والمراجعة", it, Channels.QURAN, "route:hifz-plan") }
            if (c.friday && date.dayOfWeek == DayOfWeek.FRIDAY)
                out += Alert("friday.$key", at(c.fridayAt), "يوم الجمعة", "قراءة سورة الكهف", Channels.OCCASIONS, "route:reader/18/1")
            if (c.fasting) {
                val tomorrow = date.plusDays(1).dayOfWeek
                if (tomorrow == DayOfWeek.MONDAY || tomorrow == DayOfWeek.THURSDAY)
                    out += Alert("fast.$key", at(c.fastingAt), "تذكير بالصيام", "غدا ${if (tomorrow == DayOfWeek.MONDAY) "الاثنين" else "الخميس"}", Channels.OCCASIONS, "route:sawm")
            }
            if (c.whiteDays) inp.hijriDay(date.plusDays(1))?.let { h ->
                if (h in 13..15) out += Alert("white.$key", at(c.fastingAt), "الأيام البيض", "غدا اليوم $h من الشهر الهجري", Channels.OCCASIONS, "route:sawm")
            }
            c.personal.forEach { r ->
                if (date.dayOfWeek.value in r.days) out += Alert("personal.${r.id}.$key", at(r.minute), r.title, "تذكيرك الشخصي", Channels.PERSONAL, "route:home")
            }
        }
        return out
            .filter { it.at.isAfter(now) }
            .filter { !c.quiet || it.channel in Channels.prayerChannels || !inQuiet(it.at, c.quietFrom, c.quietTo) }
            .distinctBy { it.id }
            .sortedBy { it.at }
    }

    /** Whether a time falls in the quiet hours (which may cross midnight). */
    fun inQuiet(at: LocalDateTime, from: Int, to: Int): Boolean {
        val m = at.hour * 60 + at.minute
        return if (from <= to) m in from until to else m >= from || m < to
    }
}
