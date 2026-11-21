package com.ayattla.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyTest {
    private val t = Journey.today()
    private fun pages(n: Int) = Journey.Day(pages = (1..n).toSet())

    @Test fun streakCountsDaysInARow() {
        Journey.setForTest(mapOf(t to pages(1), t - 1 to pages(3), t - 2 to Journey.Day(ayat = 7), t - 4 to pages(2)))
        assertEquals(3, Journey.streak())
        assertEquals(3, Journey.best())
    }

    @Test fun todayStillOpenKeepsYesterdaysRun() {
        Journey.setForTest(mapOf(t - 1 to pages(2), t - 2 to pages(2)))
        assertEquals(2, Journey.streak())
    }

    @Test fun fewAyatOrNothingDoNotCount() {
        Journey.setForTest(mapOf(t to Journey.Day(ayat = 3), t - 1 to pages(1)))
        assertEquals(1, Journey.streak())
        assertFalse(Journey.Day(ayat = 4).active)
        assertTrue(Journey.Day(hifz = 1).active)
    }

    @Test fun goalAndWeek() {
        Journey.setForTest(mapOf(t to pages(4)), g = 4)
        assertTrue(Journey.goalMet())
        Journey.setForTest(mapOf(t to pages(3)), g = 4)
        assertFalse(Journey.goalMet())
        val w = Journey.week()
        assertEquals(7, w.size)
        assertEquals(3, w.last().second.pages.size)
    }

    @Test fun bestRunAcrossGaps() {
        Journey.setForTest(mapOf(t - 10 to pages(1), t - 9 to pages(1), t - 8 to pages(1), t - 7 to pages(1), t to pages(1)))
        assertEquals(4, Journey.best())
        assertEquals(1, Journey.streak())
    }
}
