package com.ayattla.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ayattla.app.MainActivity
import com.ayattla.app.R
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.KhatmaPlan
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.data.Hifz
import com.ayattla.app.data.Prefs
import com.ayattla.app.screens.prayerLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules the planned alerts with AlarmManager: exact when the user allowed exact alarms,
 * otherwise "while idle" (Android may then deliver a few minutes late). Only the next two days are
 * set; every delivered alert, a daily refresh, a reboot and a time or time-zone change plan again,
 * so nothing relies on the app staying open.
 */
object Scheduler {
    private const val MAX = 120
    private const val REFRESH_CODE = 990_001

    fun inputs(c: Context): PlanInputs {
        val prefs = Prefs(c)
        val cfg = NotifyStore.load(c)
        val loc = prayerLocation(prefs, cfg)
        val content = runCatching { ContentStore.get(c) }.getOrNull()
        Hifz.load(c)
        return PlanInputs(
            place = loc.name,
            times = { d -> loc.times(d) },
            ayahText = { d ->
                content?.let { ct -> val p = DailyAyah.pick(d); ct.ayat(p.surah, p.from, p.to).joinToString(" ") { it.text }.take(160) + " ﴿${ct.surah(p.surah).shortName}﴾" }
            },
            khatmaWird = { d ->
                KhatmaPlan.decode(prefs.khatma)?.let { k ->
                    val day = k.dayOn(d)
                    if (day in 1..k.days && day !in k.completedDays) k.range(day)?.let { "ورد اليوم: من صفحة ${it.first} إلى ${it.last}" } else null
                }
            },
            hifzToday = { d ->
                if (Hifz.plan == null) null
                else if (d == LocalDate.now() && Hifz.learnedToday() && Hifz.due().isEmpty()) null
                else "مقطع اليوم ومراجعاتك في خطة الحفظ"
            },
            hijriDay = { d -> runCatching { Hijri.fromGregorian(d).day }.getOrNull() },
        )
    }

    fun upcoming(c: Context, days: Int = 2): List<Alert> =
        Planner.plan(NotifyStore.load(c), LocalDateTime.now(), days, inputs(c)).take(MAX)

    fun exactAllowed(c: Context) = Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun canNotify(c: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pending(c: Context, a: Alert?, code: Int, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? {
        val i = Intent(c, AlertReceiver::class.java).setAction("com.ayattla.app.ALERT.$code")
        if (a != null) i.putExtra("id", a.id).putExtra("title", a.title).putExtra("body", a.body).putExtra("channel", a.channel).putExtra("open", a.open)
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_IMMUTABLE or flags)
    }

    @Synchronized
    fun reschedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val sp = c.getSharedPreferences("notify", Context.MODE_PRIVATE)
        // Cancel what was set last time (also the old reminders of version 2.0).
        val old = sp.getStringSet("codes", emptySet())!!.mapNotNull { it.toIntOrNull() }
        old.forEach { code -> pending(c, null, code, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() } }
        legacyCancel(c, am)
        val alerts = runCatching { upcoming(c) }.getOrDefault(emptyList())
        val exact = exactAllowed(c)
        val codes = HashSet<String>()
        alerts.forEach { a ->
            val code = a.id.hashCode() and 0x7fffffff
            val pi = pending(c, a, code) ?: return@forEach
            val ms = a.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            runCatching {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
            codes += code.toString()
        }
        // Plan again shortly after midnight even if nothing fires today.
        if (NotifyStore.config.anyOn) {
            val ms = LocalDate.now().plusDays(1).atTime(0, 5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            pending(c, Alert("refresh", LocalDateTime.now(), "", "", "", ""), REFRESH_CODE)?.let { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, it) }
        }
        sp.edit().putStringSet("codes", codes).putLong("at", System.currentTimeMillis()).apply()
    }

    /** Version 2.0 set three repeating reminders on request codes 1–3: replaced by the planner. */
    private fun legacyCancel(c: Context, am: AlarmManager) {
        for (code in 1..3) {
            val i = Intent(c, com.ayattla.app.widget.ReminderReceiver::class.java)
            PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
        }
    }

    fun channels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        fun ch(id: String, name: String, imp: Int, withSound: Boolean, vibrate: Boolean) = NotificationChannel(id, name, imp).apply {
            if (withSound) setSound(sound, attrs) else setSound(null, null)
            enableVibration(vibrate)
        }
        nm.createNotificationChannels(listOf(
            ch(Channels.PRAYER, "الصلوات: صوت", NotificationManager.IMPORTANCE_HIGH, true, true),
            ch(Channels.PRAYER_VIBRATE, "الصلوات: اهتزاز", NotificationManager.IMPORTANCE_HIGH, false, true),
            ch(Channels.PRAYER_SILENT, "الصلوات: صامت", NotificationManager.IMPORTANCE_DEFAULT, false, false),
            ch(Channels.PRAYER_PRE, "قبل الصلاة والإقامة", NotificationManager.IMPORTANCE_HIGH, true, true),
            ch(Channels.AZKAR, "الأذكار", NotificationManager.IMPORTANCE_DEFAULT, true, false),
            ch(Channels.QURAN, "القرآن والختمة والحفظ", NotificationManager.IMPORTANCE_DEFAULT, true, false),
            ch(Channels.OCCASIONS, "الجمعة والصيام", NotificationManager.IMPORTANCE_DEFAULT, true, false),
            ch(Channels.PERSONAL, "تذكيراتي", NotificationManager.IMPORTANCE_DEFAULT, true, false),
        ))
        nm.deleteNotificationChannel("reminders")
    }

    fun show(c: Context, id: String, title: String, body: String, channel: String, open: String) {
        if (!canNotify(c)) return
        channels(c)
        val code = id.hashCode() and 0x7fffffff
        val tap = PendingIntent.getActivity(
            c, code, Intent(c, MainActivity::class.java).putExtra("open", open).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val snooze = PendingIntent.getBroadcast(
            c, code xor 0x5a5a, Intent(c, AlertReceiver::class.java).setAction(AlertReceiver.SNOOZE + code)
                .putExtra("id", id).putExtra("title", title).putExtra("body", body).putExtra("channel", channel).putExtra("open", open),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(if (channel in Channels.prayerChannels) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(tap).setAutoCancel(true)
            .addAction(0, "أجّل ${NotifyStore.load(c).snooze} دقائق", snooze)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(c).notify(code, n)
        NotifyStore.log(c, title, body)
    }

    fun snooze(c: Context, i: Intent) {
        val mins = NotifyStore.load(c).snooze
        val id = (i.getStringExtra("id") ?: return) + ".snooze"
        val a = Alert(id, LocalDateTime.now().plusMinutes(mins.toLong()), i.getStringExtra("title") ?: "", i.getStringExtra("body") ?: "",
            i.getStringExtra("channel") ?: Channels.PERSONAL, i.getStringExtra("open") ?: "route:home")
        val code = id.hashCode() and 0x7fffffff
        NotificationManagerCompat.from(c).cancel(i.getStringExtra("id")!!.hashCode() and 0x7fffffff)
        val ms = a.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        pending(c, a, code)?.let { c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, it) }
    }

    /** "Test notification" in the notification center. */
    fun test(c: Context) = show(c, "test.${System.currentTimeMillis()}", "تنبيه تجريبي", "هكذا تصلك تنبيهات آيات تتلى.", Channels.PRAYER, "route:notifications")
}

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val action = i.action.orEmpty()
        when {
            action.startsWith(SNOOZE) -> Scheduler.snooze(c, i)
            i.getStringExtra("id") == "refresh" -> Scheduler.reschedule(c)
            else -> {
                val pr = goAsync()
                Thread {
                    try {
                        Scheduler.show(c, i.getStringExtra("id") ?: "x", i.getStringExtra("title") ?: "", i.getStringExtra("body") ?: "",
                            i.getStringExtra("channel") ?: Channels.PERSONAL, i.getStringExtra("open") ?: "route:home")
                        if (i.getStringExtra("id")?.endsWith(".snooze") != true) Scheduler.reschedule(c)
                    } finally { pr.finish() }
                }.start()
            }
        }
    }
    companion object { const val SNOOZE = "com.ayattla.app.SNOOZE." }
}

/** Plans again after a reboot, an app update, a clock or time-zone change, or exact alarms being allowed. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pr = goAsync()
        Thread { try { Scheduler.reschedule(c) } finally { pr.finish() } }.start()
    }
}
