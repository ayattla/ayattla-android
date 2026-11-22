package com.ayattla.app.data

import com.ayattla.app.screens.SiteTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeTest {
    /** Every answer points only to tools that really exist in the app (no promise of missing services). */
    @Test fun answersPointToRealTools() {
        val ids = SiteTool.all.map { it.id }.toSet()
        (Welcome.uses + Welcome.likes).flatMap { it.tools }.forEach { assertTrue("missing tool $it", it in ids) }
    }

    @Test fun choiceIdsAreUniqueAndStable() {
        listOf(Welcome.uses, Welcome.likes, Welcome.reminders).forEach { l -> assertEquals(l.size, l.map { it.id }.toSet().size) }
        assertEquals(listOf("hello", "uses", "likes", "listen", "reminders", "theme", "done"), Welcome.Step.entries.map { it.key })
    }

    /** Stats keys are reduced to plain ids: no spaces, Arabic text or other free text gets through. */
    @Test fun statsKeysAreCleaned() {
        assertEquals("tool_opened", Stats.clean("tool_opened"))
        assertEquals("prayer-times", Stats.clean("prayer-times"))
        assertEquals("", Stats.clean("نص حر"))
        assertEquals("ab", Stats.clean("a b"))
        assertTrue(Stats.clean("x".repeat(100)).length <= 48)
    }
}
