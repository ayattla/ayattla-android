package com.ayattla.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ayattla.app.MainActivity
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.data.Prefs
import com.ayattla.app.data.Seasons
import com.ayattla.app.data.Today
import com.ayattla.app.data.TodaySlot
import com.ayattla.app.notify.NotifyStore
import com.ayattla.app.screens.prayerLocation
import java.time.LocalDateTime

/**
 * «الخطوة التالية»: the step of «يومي» for this part of the day, the next prayer, and a seasonal
 * note when there is one. A tap opens the app on the home screen, where the step starts.
 */
class NextStepWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loc = prayerLocation(Prefs(context), NotifyStore.load(context))
        val now = LocalDateTime.now()
        val t = loc.times(now.toLocalDate())
        val slot = Today.slotAt(now, t)
        val day = Today.dayOf(now, t)
        val next = slot.next()
        val nextAt = if (next == TodaySlot.Morning) loc.times(now.toLocalDate().plusDays(if (now.hour >= 12) 1 else 0)).fajr else t.hours(next.after)
        val season = runCatching {
            val h = Hijri.fromGregorian(day)
            Seasons.notes(day, h.month, h.day, slot, PrayerTimes.hm12(t.maghrib), PrayerTimes.hm12(loc.times(day.plusDays(1)).fajr)).firstOrNull()
        }.getOrNull()
        val open = Intent(context, MainActivity::class.java).putExtra("open", "home")

        val surface = ColorProvider(Color.White, Color(0xFF121D16))
        val ink = ColorProvider(Color(0xFF0F1F17), Color(0xFFEAF2EC))
        val muted = ColorProvider(Color(0xFF5A6B61), Color(0xFFA3B5A9))
        val green = ColorProvider(Color(0xFF23753A), Color(0xFF6BD07F))
        val tint = ColorProvider(Color(0xFFE6F2E9), Color(0xFF16271C))
        val gold = ColorProvider(Color(0xFF7A5F28), Color(0xFFD9C28E))
        val goldSoft = ColorProvider(Color(0xFFF1E8D3), Color(0xFF2A2A1C))

        provideContent {
            Column(
                GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(surface).padding(14.dp).clickable(actionStartActivity(open)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.cornerRadius(10.dp).background(tint).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("يومي · بعد ${slot.after.title}", style = TextStyle(color = green, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    TodaySlot.entries.forEach { s ->
                        Box(GlanceModifier.size(7.dp).cornerRadius(4.dp).background(if (s == slot) green else tint)) {}
                        Spacer(GlanceModifier.width(4.dp))
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                Text(slot.title, style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(slot.line, style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
                Spacer(GlanceModifier.height(8.dp))
                if (season != null) {
                    Box(GlanceModifier.cornerRadius(10.dp).background(goldSoft).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${season.title} · ${season.line}", style = TextStyle(color = gold, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    }
                } else {
                    Text("التالي: ${next.title} · ${PrayerTimes.hm12(nextAt)}", style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1)
                }
            }
        }
    }
}

class NextStepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextStepWidget()
}
