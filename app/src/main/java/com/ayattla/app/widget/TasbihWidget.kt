package com.ayattla.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

/**
 * Home-screen widget «عداد الأذكار»: tap to count without opening the app. It follows the tasbih
 * after prayer — 33 of each phrase, then the count starts again. Kept on the device only.
 */
object TasbihCount {
    val phrases = listOf("سبحان الله", "الحمد لله", "الله أكبر")
    private fun sp(c: Context) = c.getSharedPreferences("tasbih.widget", Context.MODE_PRIVATE)
    fun get(c: Context) = sp(c).getInt("n", 0)
    fun set(c: Context, n: Int) = sp(c).edit().putInt("n", n).apply()
}

class TasbihWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val n = TasbihCount.get(context)
            val phrase = TasbihCount.phrases[(n / 33) % 3]
            val inRound = n % 33
            Column(
                GlanceModifier.fillMaxSize().background(Color(0xFFFFFFFF)).cornerRadius(24.dp).padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("عداد الأذكار", style = TextStyle(color = ColorProvider(Color(0xFF2E6B33)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(6.dp))
                Box(
                    GlanceModifier.fillMaxWidth().defaultWeight().background(Color(0xFFE8F1EA)).cornerRadius(20.dp)
                        .clickable(actionRunCallback<TasbihTap>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(phrase, style = TextStyle(color = ColorProvider(Color(0xFF102D27)), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                        Text("$inRound / 33", style = TextStyle(color = ColorProvider(Color(0xFF427E45)), fontSize = 26.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المجموع $n", style = TextStyle(color = ColorProvider(Color(0xFF5B6B62)), fontSize = 12.sp))
                    Spacer(GlanceModifier.defaultWeight())
                    Text("تصفير", style = TextStyle(color = ColorProvider(Color(0xFF7A5F28)), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionRunCallback<TasbihReset>()).padding(6.dp))
                }
            }
        }
    }
}

class TasbihTap : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TasbihCount.set(context, TasbihCount.get(context) + 1)
        TasbihWidget().updateAll(context)
    }
}

class TasbihReset : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TasbihCount.set(context, 0)
        TasbihWidget().updateAll(context)
    }
}

class TasbihWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasbihWidget()
}
