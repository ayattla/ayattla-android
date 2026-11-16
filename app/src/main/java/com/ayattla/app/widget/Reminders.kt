package com.ayattla.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Version 2.0's reminders were replaced by the notification center (com.ayattla.app.notify). */
class ReminderReceiver : BroadcastReceiver() {
    // An alarm set by version 2.0 that is still pending only triggers a fresh plan (never a duplicate).
    override fun onReceive(context: Context, intent: Intent) = com.ayattla.app.notify.Scheduler.reschedule(context)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = com.ayattla.app.notify.Scheduler.reschedule(context)
}
