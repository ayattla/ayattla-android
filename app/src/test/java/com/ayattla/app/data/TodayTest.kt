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

class SeasonsTest {
    private val friday = LocalDate.of(2026, 11, 27)
    private fun keys(d: LocalDate, m: Int, day: Int, s: TodaySlot) = Seasons.notes(d, m, day, s, "5:04 م", "4:53 ص").map { it.key }

    @Test fun fridayDuringTheDayOnly() {
        assertEquals(listOf("kahf", "salawat"), keys(friday, 6, 17, TodaySlot.Wird))
        assertEquals(emptyList<String>(), keys(friday, 6, 17, TodaySlot.Hadith))
    }

    @Test fun ramadanFollowsTheDay() {
        val d = LocalDate.of(2027, 2, 15) // a Monday
        assertEquals(listOf("iftar", "khatma"), keys(d, 9, 8, TodaySlot.Morning))
        assertEquals(listOf("tarawih", "khatma"), keys(d, 9, 8, TodaySlot.Hadith))
        assertEquals(listOf("suhoor", "khatma"), keys(d, 9, 8, TodaySlot.Night))
    }

    @Test fun dhulHijjahAndVoluntaryFasts() {
        val d = LocalDate.of(2027, 5, 15)
        assert("takbir" in keys(d, 12, 3, TodaySlot.Morning))
        assert("arafah" in keys(d, 12, 9, TodaySlot.Morning))
        assert("arafah-eve" in keys(d, 12, 8, TodaySlot.Night))
        assertEquals(listOf("eid"), keys(d, 12, 10, TodaySlot.Morning))
        val sunday = LocalDate.of(2026, 11, 29)
        assertEquals(listOf("fast-mon"), keys(sunday, 6, 19, TodaySlot.Night))
        assertEquals(listOf("white"), keys(LocalDate.of(2026, 11, 24), 6, 13, TodaySlot.Night))
    }

    @Test fun noTashkeel() {
        val all = (1..12).flatMap { m -> TodaySlot.entries.flatMap { s -> Seasons.notes(friday, m, 9, s, "x", "y") } }
        all.forEach { n -> assert((n.title + n.line).none { it in 'ً'..'ْ' }) { n.title } }
    }
}
