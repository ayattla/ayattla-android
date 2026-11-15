package com.ayattla.app.widget

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ayattla.app.MainActivity
import com.ayattla.app.R
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.data.Prefs
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Daily reminders (inexact, so no special alarm permission): morning and evening azkar, and the ayah of the day. */
object Reminders {
    data class Kind(val key: String, val id: Int, val time: LocalTime, val title: String, val open: String)

    val kinds = listOf(
        Kind("remindMorning", 1, LocalTime.of(6, 30), "أذكار الصباح", "azkar:morning"),
        Kind("remindAyah", 2, LocalTime.of(9, 0), "آية اليوم", "home"),
        Kind("remindEvening", 3, LocalTime.of(17, 0), "أذكار المساء", "azkar:evening"),
    )

    private const val CHANNEL = "reminders"

    fun reschedule(context: Context, prefs: Prefs) {
        val am = context.getSystemService(AlarmManager::class.java)
        kinds.forEach { k ->
            val on = when (k.key) { "remindMorning" -> prefs.remindMorning; "remindEvening" -> prefs.remindEvening; else -> prefs.remindAyah }
            val pi = PendingIntent.getBroadcast(
                context, k.id, Intent(context, ReminderReceiver::class.java).putExtra("kind", k.key),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.cancel(pi)
            if (on) {
                var at = LocalDateTime.now().with(k.time)
                if (at.isBefore(LocalDateTime.now())) at = at.plusDays(1)
                val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, ms, AlarmManager.INTERVAL_DAY, pi)
            }
        }
    }

    fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, key: String) {
        val k = kinds.firstOrNull { it.key == key } ?: return
        if (!canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "التذكيرات", NotificationManager.IMPORTANCE_DEFAULT))
        val body = if (key == "remindAyah") {
            val p = DailyAyah.pick()
            val c = ContentStore.get(context)
            c.ayat(p.surah, p.from, p.to).joinToString(" ") { it.text }.take(160) + " ﴿${c.surah(p.surah).shortName}﴾"
        } else "حان وقت ${k.title}. اضغط لفتحها."
        val open = PendingIntent.getActivity(
            context, k.id, Intent(context, MainActivity::class.java).putExtra("open", k.open).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(k.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(k.id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(context, intent.getStringExtra("kind") ?: return)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
            Reminders.reschedule(context, Prefs(context))
    }
}
