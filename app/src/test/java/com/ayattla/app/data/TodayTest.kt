package com.ayattla.app.data

import com.ayattla.app.calc.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TodayTest {
    // Fajr 4:50, sunrise 6:10, Dhuhr 11:40, Asr 14:50, Maghrib 17:10, Isha 18:40.
    private val t = PrayerTimes(4 + 50 / 60.0, 6 + 10 / 60.0, 11 + 40 / 60.0, 14 + 50 / 60.0, 17 + 10 / 60.0, 18 + 40 / 60.0)
    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 11, 23, h, m)

    @Test fun slotFollowsThePrayers() {
        assertEquals(TodaySlot.Night, Today.slotAt(at(2, 0), t))      // after midnight, before Fajr
        assertEquals(TodaySlot.Morning, Today.slotAt(at(4, 52), t))
        assertEquals(TodaySlot.Morning, Today.slotAt(at(11, 30), t))
        assertEquals(TodaySlot.Wird, Today.slotAt(at(11, 45), t))
        assertEquals(TodaySlot.Evening, Today.slotAt(at(15, 0), t))
        assertEquals(TodaySlot.Hadith, Today.slotAt(at(17, 30), t))
        assertEquals(TodaySlot.Night, Today.slotAt(at(21, 0), t))
    }

    @Test fun nightBeforeFajrBelongsToYesterday() {
        assertEquals(LocalDate.of(2026, 11, 22), Today.dayOf(at(2, 0), t))
        assertEquals(LocalDate.of(2026, 11, 23), Today.dayOf(at(5, 0), t))
    }

    @Test fun nextWrapsAroundTheDay() {
        assertEquals(TodaySlot.Wird, Today.next(TodaySlot.Morning))
        assertEquals(TodaySlot.Morning, Today.next(TodaySlot.Night))
    }

    @Test fun samePickAllDayNewPickNextDay() {
        val xs = listOf("a", "b", "c")
        val d = LocalDate.of(2026, 11, 23)
        assertEquals(Today.pickForDay(xs, d), Today.pickForDay(xs, d))
        assert(Today.pickForDay(xs, d) != Today.pickForDay(xs, d.plusDays(1)))
        assertEquals(null, Today.pickForDay(emptyList<String>(), d))
    }
}
