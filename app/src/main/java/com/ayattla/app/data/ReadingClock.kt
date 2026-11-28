package com.ayattla.app.data

import android.content.Context
import java.time.LocalTime

/**
 * When the person usually reads (on the device only): the minute of day of recent reading sessions.
 * The wird reminders can then come a little before that time instead of a fixed clock.
 */
object ReadingClock {
    private const val MAX = 14
    private const val GAP_MS = 45 * 60 * 1000L
    private fun sp(c: Context) = c.getSharedPreferences("reading.clock", Context.MODE_PRIVATE)

    /** One sample per sitting: opening several pages in a row counts once. */
    fun record(c: Context, nowMs: Long = System.currentTimeMillis(), minute: Int = LocalTime.now().let { it.hour * 60 + it.minute }) {
        val p = sp(c)
        if (nowMs - p.getLong("last", 0L) < GAP_MS) return
        val list = samples(c).takeLast(MAX - 1) + minute
        p.edit().putLong("last", nowMs).putString("m", list.joinToString(",")).apply()
    }

    fun samples(c: Context): List<Int> = sp(c).getString("m", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }

    /** 15 minutes before the usual reading time, once there are at least 3 sittings. */
    fun reminderMinute(c: Context): Int? = usual(samples(c))?.let { (it - 15 + 1440) % 1440 }

    fun usual(s: List<Int>): Int? = if (s.size < 3) null else s.sorted()[s.size / 2]
}
