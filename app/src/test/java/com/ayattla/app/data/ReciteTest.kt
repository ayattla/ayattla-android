package com.ayattla.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures are Al-Fatiha in the pack's Uthmani spelling; heard text is plain spelling, as recognizers write it. */
class ReciteTest {
    private val fatiha = listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
        "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "مَٰلِكِ يَوْمِ ٱلدِّينِ",
        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ",
    )
    private val expected = fatiha.flatMap { Recite.tokens(it) }.map { Recite.key(it) }

    private fun run(heard: String) = Recite.align(expected, Recite.heardKeys(heard))

    @Test fun uthmaniAndPlainSpellingMatch() {
        listOf("ٱلصَّلَوٰةَ" to "الصلاة", "إِبْرَٰهِۦمَ" to "إبراهيم", "ٱلْكِتَٰبُ" to "الكتاب", "شَىْءٍ" to "شيء", "مَٰلِكِ" to "مالك")
            .forEach { (u, p) -> assertTrue("$u ~ $p", Recite.similar(Recite.key(u), Recite.key(p))) }
        assertFalse(Recite.similar(Recite.key("قَالَ"), Recite.key("كان")))
        assertFalse(Recite.similar(Recite.key("رَبِّ"), Recite.key("من")))
    }

    @Test fun fullRecitationWithIstiadhaIsAllCorrect() {
        val p = run("أعوذ بالله من الشيطان الرجيم بسم الله الرحمن الرحيم الحمد لله رب العالمين الرحمن الرحيم مالك يوم الدين إياك نعبد وإياك نستعين اهدنا الصراط المستقيم")
        assertTrue(p.done)
        assertTrue(p.review.isEmpty())
    }

    @Test fun skippedWordsAreMarkedForReview() {
        val p = run("بسم الله الرحمن الرحيم الحمد لله رب العالمين مالك يوم الدين")
        assertEquals(13, p.revealed)
        assertEquals(listOf(8, 9), p.review)
    }

    @Test fun aWordHeardDifferentlyIsRevealedForReview() {
        val p = run("بسم الله الرحمن الرحيم الحمد لله رب الناس الناس الناس الرحمن")
        assertEquals(listOf(7), p.review)
        assertEquals(9, p.revealed)
    }

    @Test fun partialRecitationStopsWhereTheReciterIs() {
        val p = run("بسم الله الرحمن")
        assertEquals(3, p.revealed)
        assertFalse(p.done)
    }

    @Test fun hintRevealsTheNextWordForReview() {
        val p = Recite.align(expected, Recite.heardKeys("بسم الله") + Recite.hint(2) + Recite.heardKeys("الرحيم"))
        assertEquals(4, p.revealed)
        assertEquals(listOf(2), p.review)
    }

    @Test fun stopMarksAreNotWords() {
        val t = Recite.tokens("لَا رَيْبَ ۛ فِيهِ ۛ هُدًى")
        assertEquals(6, t.size)
        assertEquals(listOf(0, 1, 3, 5), Recite.wordIndexes(t))
    }

    @Test fun voiceSearchFindsTheAyah() {
        val ayat = fatiha.mapIndexed { i, t -> Ayah(1, i + 1, t, 1, 1) } +
            Ayah(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", 604, 30)
        val hits = VoiceSearch(ayat).find("إياك نعبد وإياك نستعين")
        assertEquals(5, hits.first().ayah.ayah)
        assertEquals(112, VoiceSearch(ayat).find("قل هو الله أحد").first().ayah.surah)
        assertTrue(VoiceSearch(ayat).find("كتب عليكم الصيام").isEmpty())
    }
}
