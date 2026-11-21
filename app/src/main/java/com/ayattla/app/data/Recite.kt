package com.ayattla.app.data

import kotlin.math.max
import kotlin.math.min

/**
 * Matching what the device heard (plain Arabic from speech recognition) against the mushaf text,
 * for التسميع الذكي and البحث بالصوت. Nothing here changes or generates Quran text: the words shown
 * are always the pack's own words; this only decides which of them were recited.
 *
 * Same rules as the iOS app (AyatTutla/ReciteMatch.swift); keep the two in step.
 */
object Recite {
    private val marks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u08D3-\\u08FF\\u0640]")
    private val notLetters = Regex("[^\\u0621-\\u064A]")

    /** A word reduced for comparison: no marks, one alef, ى→ي, ة→ه, ؤ→و, ئ→ي, letters only. */
    fun key(word: String): String = word.replace(marks, "")
        .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
        .replace(notLetters, "")

    /** The consonant outline (no long vowels or hamza), which survives Uthmani vs plain spelling. */
    fun skeleton(key: String): String = key.filterNot { it == 'ا' || it == 'و' || it == 'ي' || it == 'ء' }

    /** Words of an ayah as shown (stop marks and the ayah number stay with the word before them). */
    fun tokens(text: String): List<String> = text.split(Regex("\\s+")).filter { it.isNotBlank() }

    /** Indexes into [tokens] of the words that are recited (tokens with letters). */
    fun wordIndexes(tokens: List<String>): List<Int> = tokens.indices.filter { key(tokens[it]).isNotEmpty() }

    fun similar(expected: String, heard: String): Boolean {
        if (expected.isEmpty() || heard.isEmpty()) return false
        if (expected == heard) return true
        val ratio = ratio(expected, heard)
        if (min(expected.length, heard.length) <= 2) return ratio >= 0.99
        if (ratio >= 0.72) return true
        val a = skeleton(expected)
        val b = skeleton(heard)
        return a.length >= 2 && (a == b || (a.length >= 4 && ratio(a, b) >= 0.8))
    }

    fun ratio(a: String, b: String): Double {
        val d = levenshtein(a, b)
        return 1.0 - d.toDouble() / max(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Heard text split into comparison keys. */
    fun heardKeys(text: String): List<String> = text.split(Regex("\\s+")).map { key(it) }.filter { it.isNotEmpty() }

    enum class Mark { Hidden, Ok, Review }

    /** Where the recitation stands: how many words are revealed, and the mark of each word. */
    data class Progress(val revealed: Int, val marks: List<Mark>) {
        val done: Boolean get() = revealed >= marks.size
        val review: List<Int> get() = marks.indices.filter { marks[it] == Mark.Review }
    }

    /** A hint the user asked for: reveals expected word [index] (counted as «راجع هذه الكلمة»). */
    fun hint(index: Int) = "#hint:$index"

    /**
     * Follows the heard words through the expected ones, in order. A skipped word (the reciter went on
     * past it) and a word heard differently three times in a row are both marked for review, never as
     * «خطأ». Before the first matched word anything heard (isti'adha, basmala, noise) is ignored, and a
     * word repeated after it was matched is not counted against the reciter.
     */
    fun align(expected: List<String>, heard: List<String>, lookahead: Int = 3): Progress {
        val marks = MutableList(expected.size) { Mark.Hidden }
        var p = 0
        var misses = 0
        var i = 0
        while (i < heard.size && p < expected.size) {
            val h = heard[i]
            if (h.startsWith("#hint:")) {
                if (h.removePrefix("#hint:").toIntOrNull() == p) { marks[p] = Mark.Review; p++; misses = 0 }
                i++; continue
            }
            val next = heard.getOrNull(i + 1)?.takeUnless { it.startsWith("#") }
            var matched = false
            // The recitation starts at the first word: before it is matched, no word is skipped.
            for (j in 0..(if (p > 0) lookahead else 0)) {
                val e = p + j
                if (e >= expected.size) break
                val single = similar(expected[e], h)
                val joined = !single && next != null && similar(expected[e], h + next)
                if (single || joined) {
                    for (s in p until e) marks[s] = Mark.Review
                    marks[e] = if (marks[e] == Mark.Review) Mark.Review else Mark.Ok
                    p = e + 1
                    misses = 0
                    if (joined) i++
                    matched = true
                    break
                }
            }
            if (!matched) {
                val repeat = (1..2).any { b -> p - b >= 0 && similar(expected[p - b], h) }
                if (!repeat && p > 0) {
                    misses++
                    if (misses >= 3) { marks[p] = Mark.Review; p++; misses = 0 }
                }
            }
            i++
        }
        return Progress(p, marks)
    }
}

/**
 * Finds ayat from a few recited words, offline, over the whole mushaf. Scores each ayah by how many
 * of the heard words appear in it in order (allowing Uthmani vs plain spelling).
 */
class VoiceSearch(ayat: List<Ayah>) {
    private data class Entry(val ayah: Ayah, val keys: List<String>)
    private val entries = ayat.map { a -> Entry(a, Recite.tokens(a.text).map { Recite.key(it) }.filter { it.isNotEmpty() }) }

    data class Hit(val ayah: Ayah, val score: Double)

    fun find(heardText: String, limit: Int = 10): List<Hit> {
        val heard = Recite.heardKeys(heardText).let { h ->
            // A leading isti'adha or basmala is not what the user is looking for.
            val drop = listOf("اعوذ", "بالله", "من", "الشيطان", "الرجيم")
            var k = 0
            while (k < h.size && k < drop.size && h[k] == drop[k]) k++
            if (k >= 4) h.drop(k) else h
        }.take(12)
        if (heard.isEmpty()) return emptyList()
        val out = ArrayList<Hit>()
        for (e in entries) {
            val s = score(e.keys, heard)
            if (s >= 0.6) out += Hit(e.ayah, s)
        }
        return out.sortedWith(compareByDescending<Hit> { it.score }.thenBy { it.ayah.surah }.thenBy { it.ayah.ayah }).take(limit)
    }

    /** Best in-order match of [heard] inside [words], from any start, as a share of the heard words. */
    private fun score(words: List<String>, heard: List<String>): Double {
        if (words.isEmpty()) return 0.0
        var best = 0
        var bestSpan = Int.MAX_VALUE
        for (start in words.indices) {
            if (!Recite.similar(words[start], heard[0]) && (heard.size < 2 || !Recite.similar(words[start], heard[1]))) continue
            var w = start
            var n = 0
            var last = start
            for (h in heard) {
                var k = w
                val stop = min(words.size, w + 3)
                while (k < stop && !Recite.similar(words[k], h)) k++
                if (k < stop) { n++; last = k; w = k + 1 }
                if (w >= words.size) break
            }
            val span = last - start
            if (n > best || (n == best && span < bestSpan)) { best = n; bestSpan = span }
        }
        return best.toDouble() / heard.size
    }
}
