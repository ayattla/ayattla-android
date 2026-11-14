package com.ayattla.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ayattla.app.ui.theme.ThemeMode

/** User settings and personal data, stored only on the device. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("ayat", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(runCatching { ThemeMode.valueOf(sp.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System))
        private set
    var reciterId by mutableStateOf(sp.getString("reciter", "") ?: "")
        private set
    var quranFontSize by mutableStateOf(sp.getFloat("quranFont", 26f))
        private set
    var lastRead by mutableStateOf(sp.getString("lastRead", null))
        private set
    var favorites by mutableStateOf(sp.getStringSet("favorites", emptySet())!!.toSet())
        private set
    var cityName by mutableStateOf(sp.getString("city", "الرياض") ?: "الرياض")
        private set
    var myLat by mutableStateOf(sp.getFloat("lat", Float.NaN).toDouble())
        private set
    var myLng by mutableStateOf(sp.getFloat("lng", Float.NaN).toDouble())
        private set
    var onboarded by mutableStateOf(sp.getBoolean("onboarded", false))
        private set

    fun setTheme(m: ThemeMode) { themeMode = m; sp.edit().putString("theme", m.name).apply() }
    fun setReciter(id: String) { reciterId = id; sp.edit().putString("reciter", id).apply() }
    fun setQuranFont(size: Float) { quranFontSize = size; sp.edit().putFloat("quranFont", size).apply() }
    fun setLastRead(surah: Int, ayah: Int) { lastRead = "$surah:$ayah"; sp.edit().putString("lastRead", lastRead).apply() }
    fun setOnboarded() { onboarded = true; sp.edit().putBoolean("onboarded", true).apply() }

    fun toggleFavorite(key: String) {
        favorites = if (key in favorites) favorites - key else favorites + key
        sp.edit().putStringSet("favorites", favorites).apply()
    }

    /** City from the site's list, or "موقعي" with saved coordinates. */
    fun setCity(name: String) { cityName = name; sp.edit().putString("city", name).apply() }
    fun setMyLocation(lat: Double, lng: Double) {
        myLat = lat; myLng = lng; cityName = MY_LOCATION
        sp.edit().putFloat("lat", lat.toFloat()).putFloat("lng", lng.toFloat()).putString("city", MY_LOCATION).apply()
    }

    fun tasbihCount(phrase: String) = sp.getInt("tasbih:$phrase", 0)
    fun setTasbihCount(phrase: String, n: Int) = sp.edit().putInt("tasbih:$phrase", n).apply()

    /** Azkar progress resets each day. */
    fun zikrRemaining(day: String, id: Int, default: Int) = sp.getInt("zikr:$day:$id", default)
    fun setZikrRemaining(day: String, id: Int, n: Int) = sp.edit().putInt("zikr:$day:$id", n).apply()

    companion object { const val MY_LOCATION = "موقعي" }
}
