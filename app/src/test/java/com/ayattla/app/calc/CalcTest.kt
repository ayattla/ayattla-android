package com.ayattla.app.calc

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Golden values captured from the live site on 2026-11-02 (same files as the iOS app). */
class CalcTest {
    private fun res(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    @Test fun prayerTimesMatchTheSiteForEveryCity() {
        val date = LocalDate.of(2026, 11, 2)
        var checked = 0
        res("cities.txt").lines().filter { it.isNotBlank() }.forEach { line ->
            val (name, cfg, times) = line.split("|")
            val p = cfg.split(",")
            val t = PrayerTimes.forDate(
                date, p[0].toDouble(), p[1].toDouble(), ZoneId.of(p[2]),
                PrayerTimes.Method(p[3].toDouble(), p[4].toDouble()),
            )
            val got = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha).map(PrayerTimes::hhmm)
            assertEquals(name, times.split(","), got)
            checked++
        }
        assertTrue(checked >= 30)
    }

    @Test fun qiblaMatchesTheSite() {
        val cases = JSONObject(res("qibla.json")).getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            assertEquals(c.getString("city"), c.getDouble("bearing"), Qibla.bearing(c.getDouble("lat"), c.getDouble("lng")), 0.001)
            assertEquals(c.getString("city"), c.getDouble("km"), Qibla.distanceKm(c.getDouble("lat"), c.getDouble("lng")), 0.01)
        }
    }

    @Test fun hijriMatchesUmmAlQuraOnTheSite() {
        val cases = JSONObject(res("hijri.json")).getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val h = c.getJSONObject("hijri")
            val g = LocalDate.parse(c.getString("gregorian"))
            assertEquals(g.toString(), Hijri.Date(h.getInt("y"), h.getInt("m"), h.getInt("d")), Hijri.fromGregorian(g))
        }
    }

    @Test fun zakatMatchesTheSite() {
        val cases = JSONObject(res("zakat.json")).getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val v = c.getJSONObject("input")
            val e = c.getJSONObject("expected")
            val r = Zakat.calculate(
                Zakat.Input(
                    v.getDouble("gp"), v.getDouble("spr"), v.getDouble("cash"), v.getDouble("trade"),
                    v.getDouble("g24"), v.getDouble("g21"), v.getDouble("g18"), v.getDouble("sv"),
                    v.getDouble("rec"), v.getDouble("debt"),
                    if (v.getString("basis") == "silver") Zakat.Basis.Silver else Zakat.Basis.Gold,
                )
            )
            assertEquals(e.getDouble("total"), r.total, 0.01)
            assertEquals(e.getDouble("nisab"), r.nisab, 0.01)
            assertEquals(e.getDouble("due"), r.due, 0.01)
        }
    }

    @Test fun searchNormalizationStripsTashkeelAndAlef() {
        assertEquals("الحمد لله", com.ayattla.app.data.SearchText.clean("ٱلْحَمْدُ لِلَّهِ"))
    }
}
