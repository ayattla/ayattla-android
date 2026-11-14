package com.ayattla.app.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.calc.PrayerCity
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.Prefs
import com.ayattla.app.data.Reciter
import java.time.ZoneId

data class PrayerLocation(val name: String, val lat: Double, val lng: Double, val zone: ZoneId, val method: PrayerTimes.Method)

@Composable
fun rememberPrayerLocation(): PrayerLocation {
    val prefs = LocalPrefs.current
    return remember(prefs.cityName, prefs.myLat, prefs.myLng) { prayerLocation(prefs) }
}

fun prayerLocation(prefs: Prefs): PrayerLocation {
    if (prefs.cityName == Prefs.MY_LOCATION && !prefs.myLat.isNaN()) {
        return PrayerLocation(
            "موقعي", prefs.myLat, prefs.myLng, ZoneId.systemDefault(),
            PrayerTimes.Method.forLocation(prefs.myLat, prefs.myLng),
        )
    }
    val c = PrayerCity.named(prefs.cityName) ?: PrayerCity.all.first()
    return PrayerLocation(c.name, c.lat, c.lng, ZoneId.of(c.zone), c.method)
}

/** The chosen reciter, or the first one in the site's order. */
@Composable
fun rememberReciter(): Reciter {
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    return remember(prefs.reciterId) {
        content.reciters.firstOrNull { it.id == prefs.reciterId } ?: content.reciters.first()
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "مشاركة"))
}

fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("آية", text))
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
}

fun vibrate(context: Context, strong: Boolean = false) {
    val v: Vibrator = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    if (!v.hasVibrator()) return
    v.vibrate(VibrationEffect.createOneShot(if (strong) 60 else 18, if (strong) 255 else 120))
}
