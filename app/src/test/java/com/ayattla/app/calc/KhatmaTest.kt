package com.ayattla.app.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class KhatmaTest {
    @Test fun thirtyDaysCoverEveryPageOnce() {
        val p = KhatmaPlan.create(30, 1, LocalDate.of(2026, 11, 1))
        val pages = (1..30).flatMap { p.range(it)!!.toList() }
        assertEquals((1..604).toList(), pages)
        assertEquals(1..20, p.range(1))
        assertNull(p.range(31))
    }

    @Test fun roundTrip() {
        val p = KhatmaPlan.create(7, 100, LocalDate.of(2026, 11, 15)).copy(completedDays = setOf(1, 3))
        assertEquals(p, KhatmaPlan.decode(p.encode()))
    }

    @Test fun splitOverFivePrayers() {
        val parts = KhatmaPlan.splitOverPrayers(1..20)
        assertEquals(listOf(1..4, 5..8, 9..12, 13..16, 17..20), parts)
    }
}
