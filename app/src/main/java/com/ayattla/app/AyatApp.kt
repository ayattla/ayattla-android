package com.ayattla.app

import kotlinx.coroutines.launch

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
        com.ayattla.app.data.Journey.load(this)
        runCatching { com.ayattla.app.notify.PrayerOngoing.refresh(this) }
        com.ayattla.app.data.Net.start(this)
        // Once a week on Wi-Fi (release builds), the offline files are refreshed quietly.
        android.os.Handler(mainLooper).postDelayed({ runCatching { com.ayattla.app.data.OfflinePrep.maybeAuto(this) } }, 8_000)
        // Anonymous usage counts (Settings → «الإحصاءات المجهولة»): sent quietly a little after start.
        android.os.Handler(mainLooper).postDelayed({
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { runCatching { com.ayattla.app.data.Stats.flush(this@AyatApp) } }
        }, 12_000)
    }
}
