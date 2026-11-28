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
import com.ayattla.app.studio.CardRatio
import com.ayattla.app.studio.CardRenderer
import com.ayattla.app.studio.CardStyle
import java.time.LocalDate

/**
 * Home-screen widget «دعاء من القرآن»: the same Quran dua as the home card today, drawn in the
 * mushaf font on the ivory card with its surah and ayah.
 */
class DuaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = ContentStore.get(context)
        val refs = content.library.duas?.quran.orEmpty()
        val bmp: Bitmap? = if (refs.isEmpty()) null else {
            val r = refs[(LocalDate.now().toEpochDay() % refs.size).toInt()]
            val ayat = content.ayat(r.s, r.a, r.b)
            val ref = "دعاء من القرآن · سورة ${content.surah(r.s).shortName} " + if (r.a == r.b) "${r.a}" else "${r.a}-${r.b}"
            val big = CardRenderer.render(context, ayat, ref, CardRatio.Wide, CardStyle.Ivory)
            Bitmap.createScaledBitmap(big, 720, 400, true).also { if (it !== big) big.recycle() }
        }
        provideContent {
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())) {
                if (bmp != null) Image(ImageProvider(bmp), "دعاء من القرآن", GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

class DuaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DuaWidget()
}
