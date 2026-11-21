package com.ayattla.app.notify

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ayattla.app.calc.Prayer
import com.ayattla.app.data.writeAtomic
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** The notification center's settings (files/notify.json), with the old three reminders carried over. */
object NotifyStore {
    var config by mutableStateOf(NotifyConfig())
        private set
    private var loaded = false

    fun load(c: Context): NotifyConfig {
        if (loaded) return config
        loaded = true
        val f = File(c.filesDir, "notify.json")
        config = if (f.exists()) runCatching { parse(JSONObject(f.readText())) }.getOrDefault(NotifyConfig())
        else {
            // First run of the notification center: keep what the old settings switches had on.
            val sp = c.getSharedPreferences("ayat", Context.MODE_PRIVATE)
            NotifyConfig(morning = sp.getBoolean("remindMorning", false), evening = sp.getBoolean("remindEvening", false), ayah = sp.getBoolean("remindAyah", false))
        }
        return config
    }

    fun save(c: Context, new: NotifyConfig) {
        config = new
        File(c.filesDir, "notify.json").writeAtomic(json(new).toString())
        Scheduler.reschedule(c)
    }

    fun update(c: Context, f: (NotifyConfig) -> NotifyConfig) = save(c, f(load(c)))

    private fun json(n: NotifyConfig) = JSONObject().apply {
        put("prayers", JSONObject().apply {
            n.prayers.forEach { (p, a) -> put(p.name, JSONObject().put("on", a.on).put("sound", a.sound.name).put("before", a.before).put("iqama", a.iqama)) }
        })
        put("afterPrayer", n.afterPrayer); put("afterPrayerDelay", n.afterPrayerDelay)
        put("morning", n.morning); put("morningOffset", n.morningOffset); put("evening", n.evening); put("eveningOffset", n.eveningOffset)
        put("sleep", n.sleep); put("sleepAt", n.sleepAt); put("wake", n.wake); put("istighfar", n.istighfar); put("istighfarAt", n.istighfarAt)
        put("ayah", n.ayah); put("ayahAt", n.ayahAt); put("khatma", n.khatma); put("khatmaAt", n.khatmaAt); put("hifz", n.hifz); put("hifzAt", n.hifzAt)
        put("journey", n.journey); put("journeyAt", n.journeyAt)
        put("friday", n.friday); put("fridayAt", n.fridayAt); put("fasting", n.fasting); put("fastingAt", n.fastingAt); put("whiteDays", n.whiteDays)
        put("personal", JSONArray(n.personal.map { JSONObject().put("id", it.id).put("title", it.title).put("minute", it.minute).put("days", JSONArray(it.days.sorted())) }))
        put("quiet", n.quiet); put("quietFrom", n.quietFrom); put("quietTo", n.quietTo); put("snooze", n.snooze)
        put("method", n.method); put("asrHanafi", n.asrHanafi)
        put("adjust", JSONObject().apply { n.adjust.forEach { (p, m) -> put(p.name, m) } })
    }

    private fun parse(o: JSONObject): NotifyConfig {
        val d = NotifyConfig()
        val ps = o.optJSONObject("prayers")
        val prayers = NotifyConfig.PRAYERS.associateWith { p ->
            ps?.optJSONObject(p.name)?.let {
                PrayerAlert(it.optBoolean("on"), runCatching { Sound.valueOf(it.optString("sound")) }.getOrDefault(Sound.Default), it.optInt("before"), it.optInt("iqama"))
            } ?: PrayerAlert()
        }
        val pa = o.optJSONArray("personal") ?: JSONArray()
        val adj = o.optJSONObject("adjust")
        return NotifyConfig(
            prayers = prayers,
            afterPrayer = o.optBoolean("afterPrayer"), afterPrayerDelay = o.optInt("afterPrayerDelay", d.afterPrayerDelay),
            morning = o.optBoolean("morning"), morningOffset = o.optInt("morningOffset", d.morningOffset),
            evening = o.optBoolean("evening"), eveningOffset = o.optInt("eveningOffset", d.eveningOffset),
            sleep = o.optBoolean("sleep"), sleepAt = o.optInt("sleepAt", d.sleepAt), wake = o.optBoolean("wake"),
            istighfar = o.optBoolean("istighfar"), istighfarAt = o.optInt("istighfarAt", d.istighfarAt),
            ayah = o.optBoolean("ayah"), ayahAt = o.optInt("ayahAt", d.ayahAt),
            khatma = o.optBoolean("khatma"), khatmaAt = o.optInt("khatmaAt", d.khatmaAt),
            hifz = o.optBoolean("hifz"), hifzAt = o.optInt("hifzAt", d.hifzAt),
            journey = o.optBoolean("journey"), journeyAt = o.optInt("journeyAt", d.journeyAt),
            friday = o.optBoolean("friday"), fridayAt = o.optInt("fridayAt", d.fridayAt),
            fasting = o.optBoolean("fasting"), fastingAt = o.optInt("fastingAt", d.fastingAt), whiteDays = o.optBoolean("whiteDays"),
            personal = List(pa.length()) { i ->
                pa.getJSONObject(i).let { r -> Personal(r.getString("id"), r.getString("title"), r.getInt("minute"),
                    r.getJSONArray("days").let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() }) }
            },
            quiet = o.optBoolean("quiet"), quietFrom = o.optInt("quietFrom", d.quietFrom), quietTo = o.optInt("quietTo", d.quietTo),
            snooze = o.optInt("snooze", d.snooze),
            method = o.optString("method", "auto"), asrHanafi = o.optBoolean("asrHanafi"),
            adjust = Prayer.entries.mapNotNull { p -> adj?.optInt(p.name, 0)?.takeIf { it != 0 }?.let { p to it } }.toMap(),
        )
    }

    /** The last 50 notifications shown (files/notify-log.json), newest first. */
    data class Logged(val at: Long, val title: String, val body: String)

    fun log(c: Context, title: String, body: String) {
        val f = File(c.filesDir, "notify-log.json")
        val a = runCatching { JSONArray(f.readText()) }.getOrDefault(JSONArray())
        val out = JSONArray().put(JSONObject().put("at", System.currentTimeMillis()).put("title", title).put("body", body.take(140)))
        for (i in 0 until minOf(a.length(), 49)) out.put(a.get(i))
        f.writeAtomic(out.toString())
    }

    fun history(c: Context): List<Logged> = runCatching {
        val a = JSONArray(File(c.filesDir, "notify-log.json").readText())
        List(a.length()) { a.getJSONObject(it).let { o -> Logged(o.getLong("at"), o.getString("title"), o.optString("body")) } }
    }.getOrDefault(emptyList())
}
