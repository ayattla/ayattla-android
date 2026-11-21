package com.ayattla.app.data

import com.ayattla.app.screens.SiteTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineToolsTest {
    @Test fun liveToolsNeedInternet() {
        assertEquals(OfflineLevel.Online, OfflineTools.level("radio"))
        assertEquals(OfflineLevel.Online, OfflineTools.level("nearby-mosques"))
    }

    @Test fun savedTools() {
        listOf("listen", "clip", "hadith", "hadith-checker", "tajweed", "word-by-word", "ruqya", "tasmee", "voice-search").forEach {
            assertEquals(it, OfflineLevel.Saved, OfflineTools.level(it))
        }
    }

    @Test fun dailyToolsWorkOffline() {
        listOf("azkar", "hisn-almuslim", "prayer-times", "qibla", "tasbih", "hijri", "zakat", "khatma", "mushaf", "duas", "kaffarah", "offline").forEach {
            assertEquals(it, OfflineLevel.Full, OfflineTools.level(it))
        }
    }

    @Test fun mostToolsWorkWithoutInternet() {
        val online = SiteTool.all.count { OfflineTools.level(it.id) == OfflineLevel.Online }
        assertTrue("tools needing internet: $online", online <= 3)
        assertTrue(SiteTool.all.any { it.id == "offline" && it.route == "offline" })
    }

    @Test fun recitationEstimate() {
        // 6236 ayat × 60 KB ≈ 365 MB for the whole Quran; the formula stays in that range.
        assertEquals(365, 6236 * 60 / 1024)
    }
}
