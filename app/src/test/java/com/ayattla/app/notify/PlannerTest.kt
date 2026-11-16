package com.ayattla.app.notify

import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PlannerTest {
    // Fajr 4:50, sunrise 6:10, Dhuhr 11:40, Asr 14:50, Maghrib 17:10, Isha 18:40.
    private val t = PrayerTimes(4 + 50 / 60.0, 6 + 10 / 60.0, 11 + 40 / 60.0, 14 + 50 / 60.0, 17 + 10 / 60.0, 18 + 40 / 60.0)
    private val inputs = PlanInputs("الرياض", { t }, hijriDay = { d -> if (d == LocalDate.of(2026, 11, 18)) 13 else 1 })
    private val monday = LocalDateTime.of(2026, 11, 16, 0, 1) // Monday

    @Test fun nothingWhenAllOff() = assertTrue(Planner.plan(NotifyConfig(), monday, 2, inputs).isEmpty())

    @Test fun fivePrayersNeverSunrise() {
        val c = NotifyConfig(prayers = NotifyConfig.PRAYERS.associateWith { PrayerAlert(on = true) })
        val day = Planner.plan(c, monday, 1, inputs)
        assertEquals(5, day.size)
        assertFalse(day.any { "الشروق" in it.title })
        assertEquals(LocalDateTime.of(2026, 11, 16, 4, 50), day.first().at)
        assertEquals("حان وقت صلاة الفجر", day.first().title)
    }

    @Test fun beforeAndIqama() {
        val c = NotifyConfig(prayers = mapOf(Prayer.Dhuhr to PrayerAlert(on = true, before = 10, iqama = 20)))
        val day = Planner.plan(c, monday, 1, inputs).map { it.at.toLocalTime().toString() }
        assertEquals(listOf("11:30", "11:40", "12:00"), day)
    }

    @Test fun soundChoosesTheChannel() {
        val c = NotifyConfig(prayers = mapOf(Prayer.Asr to PrayerAlert(on = true, sound = Sound.Silent)))
        assertEquals(Channels.PRAYER_SILENT, Planner.plan(c, monday, 1, inputs).single().channel)
    }

    @Test fun azkarFollowFajrAndAsr() {
        val c = NotifyConfig(morning = true, evening = true, morningOffset = 30, eveningOffset = 15)
        val day = Planner.plan(c, monday, 1, inputs).associate { it.title to it.at.toLocalTime().toString() }
        assertEquals("05:20", day["أذكار الصباح"])
        assertEquals("15:05", day["أذكار المساء"])
    }

    @Test fun quietHoursHoldAzkarButNeverPrayers() {
        val c = NotifyConfig(prayers = mapOf(Prayer.Fajr to PrayerAlert(on = true)), wake = true, quiet = true, quietFrom = 23 * 60, quietTo = 5 * 60)
        val day = Planner.plan(c, monday, 1, inputs).map { it.title }
        assertEquals(listOf("حان وقت صلاة الفجر"), day) // waking azkar at 4:30 falls in quiet hours
    }

    @Test fun quietAcrossMidnight() {
        assertTrue(Planner.inQuiet(LocalDateTime.of(2026, 1, 1, 23, 30), 23 * 60, 5 * 60))
        assertTrue(Planner.inQuiet(LocalDateTime.of(2026, 1, 1, 4, 59), 23 * 60, 5 * 60))
        assertFalse(Planner.inQuiet(LocalDateTime.of(2026, 1, 1, 5, 0), 23 * 60, 5 * 60))
    }

    @Test fun fridayFastingAndWhiteDays() {
        val c = NotifyConfig(friday = true, fasting = true, whiteDays = true)
        val week = Planner.plan(c, monday, 7, inputs)
        assertEquals(1, week.count { it.title == "يوم الجمعة" && it.at.dayOfWeek == java.time.DayOfWeek.FRIDAY })
        // Eve of Thursday (Wednesday) and eve of Monday (Sunday).
        assertEquals(listOf(java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.SUNDAY), week.filter { it.title == "تذكير بالصيام" }.map { it.at.dayOfWeek })
        // 2026-11-18 is the 13th: reminded the evening before.
        assertEquals(LocalDate.of(2026, 11, 17), week.single { it.title == "الأيام البيض" }.at.toLocalDate())
    }

    @Test fun personalOnChosenDays() {
        val c = NotifyConfig(personal = listOf(Personal("a", "قيام الليل", 3 * 60, setOf(1, 4))))
        val week = Planner.plan(c, monday, 7, inputs)
        assertEquals(listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.THURSDAY), week.map { it.at.dayOfWeek })
    }

    @Test fun pastAlertsAreDropped() {
        val c = NotifyConfig(prayers = NotifyConfig.PRAYERS.associateWith { PrayerAlert(on = true) })
        val afternoon = LocalDateTime.of(2026, 11, 16, 15, 0)
        assertEquals(listOf("المغرب", "العشاء"), Planner.plan(c, afternoon, 1, inputs).map { it.title.removePrefix("حان وقت صلاة ") })
    }

    @Test fun hanafiAsrIsLater() {
        val m = PrayerTimes.Method.UmmAlQura
        val shafii = PrayerTimes.compute(2026, 11, 16, 24.7136, 46.6753, 3.0, m)
        val hanafi = PrayerTimes.compute(2026, 11, 16, 24.7136, 46.6753, 3.0, m, asrFactor = 2)
        assertTrue(hanafi.asr - shafii.asr > 0.5)   // about 50 minutes later in Riyadh in November
        assertEquals(shafii.dhuhr, hanafi.dhuhr, 1e-9)
    }

    @Test fun manualAdjustment() {
        val a = t.adjusted(mapOf(Prayer.Isha to 5, Prayer.Fajr to -2))
        assertEquals(PrayerTimes.displayMinute(t.isha) + 5, PrayerTimes.displayMinute(a.isha))
        assertEquals(PrayerTimes.displayMinute(t.fajr) - 2, PrayerTimes.displayMinute(a.fajr))
    }
}
