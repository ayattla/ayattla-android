package com.ayattla.app

import android.app.Application
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Prefs

class AyatApp : Application() {
    lateinit var prefs: Prefs
        private set
    val content: ContentStore by lazy { ContentStore.get(this) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }
}
