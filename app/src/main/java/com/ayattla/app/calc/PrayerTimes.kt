package com.ayattla.app.calc

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Prayer times: line-by-line port of ayattla.com/tools/prayer-times (same as the iOS app).
 * Asr uses the majority (Shafii) shadow factor, as on the site.
 */
data class PrayerTimes(
    val fajr: Double, val sunrise: Double, val dhuhr: Double,
    val asr: Double, val maghrib: Double, val isha: Double,
) {
    /** Fajr angle, and Isha angle or (if > 30) minutes after Maghrib. 90 becomes 120 in Ramadan. */
    data class Method(val fajrAngle: Double, val isha: Double) {
        companion object {
            /** Methods the user can choose (id, name, method). "auto" keeps the site's choice per city. */
            val choices = listOf(
                Triple("umm-al-qura", "أم القرى (مكة المكرمة)", Method(18.5, 90.0)),
                Triple("mwl", "رابطة العالم الإسلامي", Method(18.0, 17.0)),
                Triple("egypt", "الهيئة المصرية العامة للمساحة", Method(19.5, 17.5)),
                Triple("karachi", "جامعة العلوم الإسلامية بكراتشي", Method(18.0, 18.0)),
                Triple("isna", "أمريكا الشمالية (ISNA)", Method(15.0, 15.0)),
                Triple("dubai", "دبي", Method(18.2, 18.2)),
                Triple("kuwait", "الكويت", Method(18.0, 17.5)),
                Triple("qatar", "قطر", Method(18.0, 90.0)),
            )
            fun byId(id: String) = choices.firstOrNull { it.first == id }?.third
            val UmmAlQura = Method(18.5, 90.0)
            val DefaultGeo = Method(18.0, 17.0)
            /** Same rule as the site's "موقعي" button. */
            fun forLocation(lat: Double, lng: Double): Method {
                val ksa = lat > 16 && lat < 32.5 && lng > 34.5 && lng < 55.7
                return if (ksa) UmmAlQura else DefaultGeo
            }
        }
    }

    /** Each prayer moved by the user's minutes (the "manual adjustment" setting). */
    fun adjusted(minutes: Map<Prayer, Int>): PrayerTimes = if (minutes.isEmpty()) this else PrayerTimes(
        fajr + (minutes[Prayer.Fajr] ?: 0) / 60.0, sunrise + (minutes[Prayer.Sunrise] ?: 0) / 60.0,
        dhuhr + (minutes[Prayer.Dhuhr] ?: 0) / 60.0, asr + (minutes[Prayer.Asr] ?: 0) / 60.0,
        maghrib + (minutes[Prayer.Maghrib] ?: 0) / 60.0, isha + (minutes[Prayer.Isha] ?: 0) / 60.0,
    )

    fun hours(p: Prayer): Double = when (p) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
    }

    companion object {
        fun compute(
            year: Int, month: Int, day: Int, lat: Double, lng: Double,
            tzHours: Double, method: Method, ramadan: Boolean = false, asrFactor: Int = 1,
        ): PrayerTimes {
            val jd = Astro.julian(year, month, day) - lng / (15 * 24)
            fun mid(t: Double) = Astro.fix(12 - Astro.sunPos(jd + t).second, 24.0)
            fun ang(a: Double, t: Double, ccw: Boolean = false): Double {
                val d = Astro.sunPos(jd + t).first
                val tt = Astro.darccos((-Astro.dsin(a) - Astro.dsin(d) * Astro.dsin(lat)) / (Astro.dcos(d) * Astro.dcos(lat))) / 15
                return mid(t) + if (ccw) -tt else tt
            }
            fun asr(t: Double): Double {
                val d = Astro.sunPos(jd + t).first
                return ang(-Astro.darccot(asrFactor + Astro.dtan(abs(lat - d))), t)
            }
            fun adj(t: Double) = t + tzHours - lng / 15
            val fajr = adj(ang(method.fajrAngle, 5.0 / 24, ccw = true))
            val sunrise = adj(ang(0.833, 6.0 / 24, ccw = true))
            val dhuhr = adj(mid(0.5))
            val asrT = adj(asr(13.0 / 24))
            val maghrib = adj(ang(0.833, 18.0 / 24))
            val isha = if (method.isha > 30) {
                val minutes = if (method.isha == 90.0 && ramadan) 120.0 else method.isha
                maghrib + minutes / 60
            } else adj(ang(method.isha, 18.0 / 24))
            return PrayerTimes(fajr, sunrise, dhuhr, asrT, maghrib, isha)
        }

        /** Times for a civil date in a zone, with Ramadan from the Umm al-Qura calendar. */
        fun forDate(date: LocalDate, lat: Double, lng: Double, zone: ZoneId, method: Method, asrFactor: Int = 1): PrayerTimes {
            val tz = ZonedDateTime.of(date.atTime(12, 0), zone).offset.totalSeconds / 3600.0
            return compute(date.year, date.monthValue, date.dayOfMonth, lat, lng, tz, method, Hijri.isRamadan(date), asrFactor)
        }

        /** "HH:mm" (24h), rounded like the site's fmt (half a minute, then floor). */
        fun hhmm(hours: Double): String {
            val m = displayMinute(hours)
            return String.format(java.util.Locale.US, "%02d:%02d", m / 60, m % 60)
        }

        /** "h:mm ص/م" for the interface. */
        fun hm12(hours: Double): String {
            val m = displayMinute(hours)
            val h = m / 60
            val suffix = if (h < 12) "ص" else "م"
            val h12 = if (h % 12 == 0) 12 else h % 12
            return String.format(java.util.Locale.US, "%d:%02d %s", h12, m % 60, suffix)
        }

        fun displayMinute(hours: Double): Int {
            val h = Astro.fix(hours + 0.5 / 60, 24.0)
            val hh = floor(h).toInt()
            return hh * 60 + floor((h - hh) * 60).toInt()
        }

        data class Next(val prayer: Prayer, val minutesUntil: Int, val isTomorrow: Boolean)

        /** The next of the five prayers strictly after minuteOfDay; after Isha it is tomorrow's Fajr. */
        fun next(minuteOfDay: Int, today: PrayerTimes, tomorrow: PrayerTimes): Next {
            for (p in Prayer.entries) if (p.isPrayer) {
                val m = displayMinute(today.hours(p))
                if (m > minuteOfDay) return Next(p, m - minuteOfDay, false)
            }
            return Next(Prayer.Fajr, 1440 - minuteOfDay + displayMinute(tomorrow.fajr), true)
        }
    }
}

enum class Prayer(val title: String) {
    Fajr("الفجر"), Sunrise("الشروق"), Dhuhr("الظهر"), Asr("العصر"), Maghrib("المغرب"), Isha("العشاء");
    val isPrayer: Boolean get() = this != Sunrise
}

internal object Astro {
    private const val RAD = Math.PI / 180
    fun dsin(d: Double) = sin(d * RAD)
    fun dcos(d: Double) = cos(d * RAD)
    fun dtan(d: Double) = tan(d * RAD)
    fun darcsin(x: Double) = asin(x) / RAD
    fun darccos(x: Double) = acos(x.coerceIn(-1.0, 1.0)) / RAD
    fun darctan2(y: Double, x: Double) = atan2(y, x) / RAD
    fun darccot(x: Double) = atan(1 / x) / RAD
    fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }

    /** (declination, equation of time). */
    fun sunPos(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
        return darcsin(dsin(e) * dsin(l)) to (q / 15 - fix(ra, 24.0))
    }

    fun julian(year: Int, month: Int, day: Int): Double {
        var y = year.toDouble()
        var m = month.toDouble()
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }
}

/** A city from the site's prayer-times menu with the settings the site uses for it. */
data class PrayerCity(val name: String, val lat: Double, val lng: Double, val zone: String, val method: PrayerTimes.Method) {
    companion object {
        private fun c(n: String, lat: Double, lng: Double, z: String, f: Double, i: Double) = PrayerCity(n, lat, lng, z, PrayerTimes.Method(f, i))
        val all = listOf(
            c("الرياض", 24.7136, 46.6753, "Asia/Riyadh", 18.5, 90.0),
            c("مكة المكرمة", 21.4225, 39.8262, "Asia/Riyadh", 18.5, 90.0),
            c("المدينة المنورة", 24.4672, 39.6111, "Asia/Riyadh", 18.5, 90.0),
            c("جدة", 21.5433, 39.1728, "Asia/Riyadh", 18.5, 90.0),
            c("الدمام", 26.4207, 50.0888, "Asia/Riyadh", 18.5, 90.0),
            c("الخبر", 26.2794, 50.2083, "Asia/Riyadh", 18.5, 90.0),
            c("الطائف", 21.2703, 40.4158, "Asia/Riyadh", 18.5, 90.0),
            c("تبوك", 28.3838, 36.555, "Asia/Riyadh", 18.5, 90.0),
            c("بريدة", 26.3592, 43.9818, "Asia/Riyadh", 18.5, 90.0),
            c("أبها", 18.2164, 42.5053, "Asia/Riyadh", 18.5, 90.0),
            c("حائل", 27.5114, 41.7208, "Asia/Riyadh", 18.5, 90.0),
            c("جازان", 16.8892, 42.5511, "Asia/Riyadh", 18.5, 90.0),
            c("نجران", 17.4924, 44.1277, "Asia/Riyadh", 18.5, 90.0),
            c("الأحساء", 25.3833, 49.5833, "Asia/Riyadh", 18.5, 90.0),
            c("ينبع", 24.089, 38.0618, "Asia/Riyadh", 18.5, 90.0),
            c("دبي", 25.2048, 55.2708, "Asia/Dubai", 18.2, 18.2),
            c("أبوظبي", 24.4539, 54.3773, "Asia/Dubai", 18.2, 18.2),
            c("الكويت", 29.3759, 47.9774, "Asia/Kuwait", 18.0, 17.5),
            c("الدوحة", 25.2854, 51.531, "Asia/Qatar", 18.0, 90.0),
            c("المنامة", 26.2285, 50.586, "Asia/Bahrain", 18.5, 90.0),
            c("مسقط", 23.588, 58.3829, "Asia/Muscat", 18.0, 18.0),
            c("القاهرة", 30.0444, 31.2357, "Africa/Cairo", 19.5, 17.5),
            c("الإسكندرية", 31.2001, 29.9187, "Africa/Cairo", 19.5, 17.5),
            c("عمان", 31.9539, 35.9106, "Asia/Amman", 18.0, 18.0),
            c("بغداد", 33.3152, 44.3661, "Asia/Baghdad", 18.0, 17.0),
            c("بيروت", 33.8938, 35.5018, "Asia/Beirut", 18.0, 17.0),
            c("دمشق", 33.5138, 36.2765, "Asia/Damascus", 18.0, 17.0),
            c("القدس", 31.7683, 35.2137, "Asia/Jerusalem", 18.0, 17.0),
            c("صنعاء", 15.3694, 44.191, "Asia/Aden", 18.5, 90.0),
            c("الخرطوم", 15.5007, 32.5599, "Africa/Khartoum", 19.5, 17.5),
            c("الدار البيضاء", 33.5731, -7.5898, "Africa/Casablanca", 19.0, 17.0),
            c("الرباط", 34.0209, -6.8416, "Africa/Casablanca", 19.0, 17.0),
            c("الجزائر", 36.7538, 3.0588, "Africa/Algiers", 18.0, 17.0),
            c("تونس", 36.8065, 10.1815, "Africa/Tunis", 18.0, 18.0),
            c("طرابلس", 32.8872, 13.1913, "Africa/Tripoli", 18.0, 17.0),
            c("نواكشوط", 18.0735, -15.9582, "Africa/Nouakchott", 18.0, 17.0),
        )
        fun named(name: String) = all.firstOrNull { it.name == name }
    }
}
