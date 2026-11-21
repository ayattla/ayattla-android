package com.ayattla.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ayattla.app.MainActivity
import com.ayattla.app.R
import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.Prefs
import com.ayattla.app.screens.prayerLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * «الصلاة القادمة على شاشة القفل»: a quiet, ongoing notification with a live countdown to the next
 * prayer (shown on the lock screen and in the status bar). It renews itself at every prayer time.
 */
object PrayerOngoing {
    private const val CHANNEL = "prayer_now"
    private const val ID = 77_001
    private const val ALARM = 990_002
    const val KEY = "prayer.ongoing"

    fun enabled(c: Context) = c.getSharedPreferences("ayat", Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(c: Context, on: Boolean) {
        c.getSharedPreferences("ayat", Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        if (on) refresh(c) else cancel(c)
    }

    /** The next prayer from now: its name, time and the city. */
    data class Next(val prayer: Prayer, val at: LocalDateTime, val label: String, val place: String)

    fun next(c: Context, now: LocalDateTime = LocalDateTime.now()): Next {
        val loc = prayerLocation(Prefs(c), NotifyStore.load(c))
        val five = listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
        for (d in 0..1) {
            val date: LocalDate = now.toLocalDate().plusDays(d.toLong())
            val t = loc.times(date)
            five.forEach { p ->
                val at = date.atStartOfDay().plusMinutes(PrayerTimes.displayMinute(t.hours(p)).toLong())
                if (at.isAfter(now)) return Next(p, at, PrayerTimes.hm12(t.hours(p)), loc.name)
            }
        }
        val t = loc.times(now.toLocalDate().plusDays(1))
        return Next(Prayer.Fajr, now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(PrayerTimes.displayMinute(t.fajr).toLong()), PrayerTimes.hm12(t.fajr), loc.name)
    }

    fun refresh(c: Context) {
        if (!enabled(c) || !Scheduler.canNotify(c)) return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "الصلاة القادمة (دائم)", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
        val n = runCatching { next(c) }.getOrNull() ?: return
        val millis = n.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tap = PendingIntent.getActivity(
            c, ID, Intent(c, MainActivity::class.java).putExtra("open", "route:prayer").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("صلاة ${n.prayer.title} ${n.label}")
            .setContentText("${n.place} · الوقت المتبقي")
            .setWhen(millis).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(tap)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(c).notify(ID, notif)
        // Renew one minute after the prayer comes in, so the next one shows.
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(c, ALARM, Intent(c, AlertReceiver::class.java).setAction("com.ayattla.app.ONGOING").putExtra("id", "ongoing"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching {
            if (Scheduler.exactAllowed(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis + 60_000, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis + 60_000, pi)
        }
    }

    fun cancel(c: Context) {
        NotificationManagerCompat.from(c).cancel(ID)
        val pi = PendingIntent.getBroadcast(c, ALARM, Intent(c, AlertReceiver::class.java).setAction("com.ayattla.app.ONGOING").putExtra("id", "ongoing"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (pi != null) c.getSystemService(AlarmManager::class.java).cancel(pi)
    }
}
