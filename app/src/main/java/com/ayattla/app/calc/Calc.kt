package com.ayattla.app.calc

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Qibla bearing and distance: same formulas and Kaaba point as ayattla.com/tools/qibla. */
object Qibla {
    const val KAABA_LAT = 21.4225
    const val KAABA_LNG = 39.8262

    /** Degrees clockwise from true north, 0 until 360. */
    fun bearing(lat: Double, lng: Double): Double {
        val r = Math.PI / 180
        val p1 = lat * r
        val p2 = KAABA_LAT * r
        val dl = (KAABA_LNG - lng) * r
        val b = atan2(sin(dl), cos(p1) * tan(p2) - sin(p1) * cos(dl)) / r
        return (b + 360) % 360
    }

    fun distanceKm(lat: Double, lng: Double): Double {
        val r = Math.PI / 180
        val p1 = lat * r
        val p2 = KAABA_LAT * r
        val dp = p2 - p1
        val dl = (KAABA_LNG - lng) * r
        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

/** Hijri dates on the Umm al-Qura calendar (java.time's Hijrah chronology is Umm al-Qura). */
object Hijri {
    data class Date(val year: Int, val month: Int, val day: Int)

    val months = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    fun fromGregorian(date: LocalDate): Date {
        val h = HijrahDate.from(date)
        return Date(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
    }

    fun toGregorian(h: Date): LocalDate? = runCatching { LocalDate.from(HijrahChronology.INSTANCE.date(h.year, h.month, h.day)) }.getOrNull()

    fun isRamadan(date: LocalDate) = runCatching { fromGregorian(date).month == 9 }.getOrDefault(false)

    fun format(date: LocalDate): String = runCatching {
        val h = fromGregorian(date)
        "${h.day} ${months[h.month - 1]} ${h.year} هـ"
    }.getOrDefault("")

    fun monthLength(year: Int, month: Int): Int = runCatching {
        HijrahChronology.INSTANCE.date(year, month, 1).lengthOfMonth()
    }.getOrDefault(30)
}

/** Zakat: port of ayattla.com/tools/zakat calc(): nisab 85 g gold or 595 g silver, 2.5 %. */
object Zakat {
    enum class Basis { Gold, Silver }

    data class Input(
        val goldPricePerGram24: Double = 0.0, val silverPricePerGram: Double = 0.0,
        val cash: Double = 0.0, val trade: Double = 0.0,
        val gold24: Double = 0.0, val gold21: Double = 0.0, val gold18: Double = 0.0, val silverGrams: Double = 0.0,
        val receivables: Double = 0.0, val debts: Double = 0.0, val basis: Basis = Basis.Gold,
    )

    data class Result(val goldValue: Double, val silverValue: Double, val total: Double, val nisab: Double, val due: Double) {
        val reachedNisab: Boolean get() = nisab > 0 && total >= nisab
    }

    fun calculate(v: Input): Result {
        val gold = (v.gold24 + v.gold21 * 21 / 24 + v.gold18 * 18 / 24) * v.goldPricePerGram24
        val silver = v.silverGrams * v.silverPricePerGram
        val total = v.cash + v.trade + gold + silver + v.receivables - v.debts
        val nisab = if (v.basis == Basis.Silver) 595 * v.silverPricePerGram else 85 * v.goldPricePerGram24
        val due = if (nisab > 0 && total >= nisab) total * 0.025 else 0.0
        return Result(gold, silver, total, nisab, due)
    }
}
