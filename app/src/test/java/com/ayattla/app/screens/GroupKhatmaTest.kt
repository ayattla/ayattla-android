package com.ayattla.app.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupKhatmaTest {
    @Test fun splitsAllThirtyInOrder() {
        val s = GroupKhatma("ختمة", listOf("أ", "ب", "ج", "د"), emptySet()).split()
        assertEquals(listOf(1..8, 9..16, 17..23, 24..30), s.map { it.second })
        assertEquals(30, s.sumOf { it.second.count() })
    }

    @Test fun onePersonReadsEverything() =
        assertEquals(1..30, GroupKhatma("ختمة", listOf("أ"), emptySet()).split().single().second)
}
