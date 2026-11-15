package com.ayattla.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.ayattla.app.MainActivity
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.studio.CardRatio
import com.ayattla.app.studio.CardRenderer
import com.ayattla.app.studio.CardStyle

/** Home-screen widget: today's ayah drawn in the mushaf font (same pick as the app's home). */
class DailyAyahWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = ContentStore.get(context)
        val pick = DailyAyah.pick()
        val ayat = content.ayat(pick.surah, pick.from, pick.to)
        val s = content.surah(pick.surah)
        val ref = "آية اليوم · سورة ${s.shortName} " + if (pick.from == pick.to) "${pick.from}" else "${pick.from}-${pick.to}"
        val big = CardRenderer.render(context, ayat, ref, CardRatio.Wide, CardStyle.Emerald)
        val bmp = Bitmap.createScaledBitmap(big, 720, 400, true).also { if (it !== big) big.recycle() }
        provideContent {
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())) {
                Image(ImageProvider(bmp), "آية اليوم", GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

class DailyAyahWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyAyahWidget()
}
