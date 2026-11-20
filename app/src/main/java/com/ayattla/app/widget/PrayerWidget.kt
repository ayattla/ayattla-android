package com.ayattla.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ayattla.app.MainActivity
import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.Prefs
import com.ayattla.app.notify.NotifyStore
import com.ayattla.app.screens.prayerLocation
import java.time.LocalDate
import java.time.LocalDateTime

/** Home-screen widget: the next prayer and its time, the city, and today's five times. */
class PrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loc = prayerLocation(Prefs(context), NotifyStore.load(context))
        val today = LocalDate.now()
        val t = loc.times(today)
        val now = LocalDateTime.now()
        val five = listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
        fun at(h: Double) = today.atStartOfDay().plusMinutes(PrayerTimes.displayMinute(h).toLong())
        val next = five.firstOrNull { at(t.hours(it)).isAfter(now) }
        val nextTime = if (next != null) PrayerTimes.hm12(t.hours(next)) else PrayerTimes.hm12(loc.times(today.plusDays(1)).fajr)
        val white = ColorProvider(Color.White, Color.White)
        val gold = ColorProvider(Color(0xFFE3CB8E), Color(0xFFE3CB8E))
        val soft = ColorProvider(Color(0xCCFFFFFF), Color(0xCCFFFFFF))
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(ColorProvider(Color(0xFF17623F), Color(0xFF0E3B2C)))
                    .padding(14.dp).clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("الصلاة القادمة · ${loc.name}", style = TextStyle(color = gold, fontSize = 12.sp, textAlign = TextAlign.Center))
                Text("${(next ?: Prayer.Fajr).title} $nextTime", style = TextStyle(color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    five.forEach { p ->
                        Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.title, style = TextStyle(color = if (p == next) gold else soft, fontSize = 11.sp, textAlign = TextAlign.Center))
                            Text(PrayerTimes.hm12(t.hours(p)).substringBefore(" "), style = TextStyle(color = if (p == next) gold else white, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
                        }
                    }
                }
            }
        }
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
