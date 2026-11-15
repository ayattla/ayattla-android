package com.ayattla.app.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Khatma plan over the 604 pages: port of AyatCalc/Khatma (same split as /tools/khatma). */
data class KhatmaPlan(val days: Int, val startPage: Int, val startDate: LocalDate, val completedDays: Set<Int> = emptySet()) {
    val totalPages: Int get() = PAGES - startPage + 1
    val pagesPerDay: Double get() = totalPages.toDouble() / days
    val isFinished: Boolean get() = completedDays.size >= days
    val expectedEnd: LocalDate get() = startDate.plusDays((days - 1).toLong())

    fun range(day: Int): IntRange? {
        if (day !in 1..days) return null
        val a = startPage + (day - 1) * totalPages / days
        val b = startPage + day * totalPages / days - 1
        return if (a <= b) a..b else null
    }

    fun dayOn(date: LocalDate): Int = ChronoUnit.DAYS.between(startDate, date).toInt() + 1

    fun encode(): String = "$days|$startPage|$startDate|" + completedDays.sorted().joinToString(",")

    companion object {
        const val PAGES = 604
        val presets = listOf(3, 7, 10, 15, 20, 30, 60)

        fun create(days: Int, startPage: Int, start: LocalDate) =
            KhatmaPlan(days.coerceAtLeast(1), startPage.coerceIn(1, PAGES), start)

        fun decode(raw: String?): KhatmaPlan? = runCatching {
            val p = raw!!.split("|")
            KhatmaPlan(p[0].toInt(), p[1].toInt(), LocalDate.parse(p[2]),
                p.getOrNull(3).orEmpty().split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet())
        }.getOrNull()

        /** Five parts, one after each prayer. */
        fun splitOverPrayers(r: IntRange): List<IntRange?> {
            val n = r.last - r.first + 1
            return (0 until 5).map { i ->
                val a = r.first + i * n / 5
                val b = r.first + (i + 1) * n / 5 - 1
                if (a <= b) a..b else null
            }
        }
    }
}
