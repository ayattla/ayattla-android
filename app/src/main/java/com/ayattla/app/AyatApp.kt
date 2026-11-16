package com.ayattla.app

import android.app.Application
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Prefs

class AyatApp : Application() {
    lateinit var prefs: Prefs
        private set
    val content: ContentStore by lazy { ContentStore.get(this) }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        com.ayattla.app.crash.CrashReporter.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        com.ayattla.app.data.Notes.load(this)
        com.ayattla.app.notify.NotifyStore.load(this)
        // Every start plans the notifications again (cheap): times follow the city, settings and day.
        Thread { runCatching { com.ayattla.app.notify.Scheduler.reschedule(this) } }.start()
        com.ayattla.app.data.Hifz.load(this)
    }
}
