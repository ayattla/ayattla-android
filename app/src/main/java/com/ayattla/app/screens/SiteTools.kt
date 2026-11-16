package com.ayattla.app.screens

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.ayattla.app.Routes
import com.ayattla.app.goTab

/** The site's tool groups, as the iOS SiteTool.Group. */
enum class ToolGroup(val title: String) { Content("صناعة المحتوى"), Quran("القرآن"), Daily("أدوات يومية"), Hadith("الحديث والتعلم") }

/**
 * Every tool the Android app has, with the iOS app's titles, short names, lines, «جديد» badges,
 * groups and order (iOS SiteCatalog.swift / HomeLauncher.swift). `route` opens it here.
 */
data class SiteTool(
    val id: String, val title: String, val short: String, val subtitle: String,
    val icon: ImageVector, val group: ToolGroup, val isNew: Boolean, val route: String,
) {
    companion object {
        val all: List<SiteTool> = listOf(
            SiteTool("clip", "صانع المقاطع القرآنية", "صانع المقاطع", "فيديو بتظليل الكلمات وأكثر من ٣٠ قارئا", Icons.Outlined.SmartDisplay, ToolGroup.Content, false, Routes.studioMode(video = true)),
            SiteTool("ayah-card", "صانع بطاقات الآيات", "بطاقة آية", "صورة جاهزة للحالة والمنشور بأربعة مقاسات", Icons.Outlined.PhotoLibrary, ToolGroup.Content, true, Routes.studioMode(video = false)),
            SiteTool("status", "حالات واتساب", "حالات واتساب", "صورة أو فيديو أو نص متحرك", Icons.Outlined.Forum, ToolGroup.Content, false, "status"),
            SiteTool("listen", "المصحف المسموع والتحفيظ", "المصحف المسموع", "استمع للسورة آية آية مع التكرار للحفظ", Icons.Outlined.Headphones, ToolGroup.Quran, true, "quran"),
            SiteTool("search", "البحث في القرآن", "البحث", "ابحث بكلمة واصنع من النتيجة فيديو أو بطاقة", Icons.Outlined.Search, ToolGroup.Quran, true, "quran"),
            SiteTool("topics", "آيات بالموضوع", "بالموضوع", "الصبر والرزق والتوبة والرحمة مع التفسير الميسر", Icons.Outlined.Eco, ToolGroup.Quran, true, "topics"),
            SiteTool("favorites", "آياتي المحفوظة", "المحفوظة", "الآيات التي حفظتها لتعود إليها وتصنع منها", Icons.Outlined.StarOutline, ToolGroup.Content, true, "saved"),
            SiteTool("ayah-of-the-day", "آية اليوم", "آية اليوم", "آية جديدة كل يوم للتدبر والمشاركة", Icons.Outlined.WbSunny, ToolGroup.Quran, false, "ayah-today"),
            SiteTool("azkar", "الأذكار", "الأذكار", "أذكار الصباح والمساء والنوم مع العداد", Icons.Outlined.WbTwilight, ToolGroup.Daily, true, "azkar"),
            SiteTool("hisn-almuslim", "حصن المسلم", "حصن المسلم", "أبواب الأذكار والأدعية كاملة مع العداد", Icons.Outlined.Shield, ToolGroup.Daily, true, "azkar-all"),
            SiteTool("radio", "إذاعات القرآن", "الإذاعات", "بث مباشر لإذاعات التلاوة", Icons.Outlined.Radio, ToolGroup.Quran, true, "radios"),
            SiteTool("prayer-times", "مواقيت الصلاة", "المواقيت", "حسب مدينتك بطريقة الحساب المعتمدة", Icons.Outlined.Schedule, ToolGroup.Daily, false, "prayer"),
            SiteTool("notifications", "مركز التنبيهات", "التنبيهات", "الصلوات والأذكار والورد والصيام وتذكيراتك", Icons.Outlined.NotificationsActive, ToolGroup.Daily, true, "notifications"),
            SiteTool("qibla", "اتجاه القبلة", "القبلة", "زاوية القبلة من موقعك مع البوصلة", Icons.Outlined.Explore, ToolGroup.Daily, false, "qibla"),
            SiteTool("khatma", "خطة ختم القرآن", "خطة الختم", "وزع الختمة على الأيام والصلوات", Icons.AutoMirrored.Outlined.MenuBook, ToolGroup.Quran, true, "khatma"),
            SiteTool("hijri", "التاريخ الهجري", "الهجري", "تحويل بين الهجري والميلادي", Icons.Outlined.CalendarMonth, ToolGroup.Daily, false, "hijri"),
            SiteTool("occasions", "المناسبات الإسلامية", "المناسبات", "كم بقي على رمضان والعيدين وعرفة", Icons.Outlined.NightsStay, ToolGroup.Daily, true, "occasions"),
            SiteTool("zakat", "حاسبة الزكاة", "الزكاة", "زكاة المال والذهب والفضة وعروض التجارة", Icons.Outlined.Savings, ToolGroup.Daily, true, "zakat"),
            SiteTool("tasbih", "المسبحة الإلكترونية", "المسبحة", "عداد تسبيح يحفظ العدد على جهازك", Icons.Outlined.TouchApp, ToolGroup.Daily, false, "tasbih"),
            SiteTool("hadith-card", "بطاقات الأحاديث الصحيحة", "بطاقة حديث", "حديث صحيح أو حسن مع الراوي والمصدر والحكم", Icons.Outlined.Book, ToolGroup.Content, true, "hadith"),
            SiteTool("tajweed", "المصحف الملون بالتجويد", "التجويد", "أحكام التجويد بالألوان مع المفتاح", Icons.Outlined.Palette, ToolGroup.Quran, true, "tajweed"),
            SiteTool("word-by-word", "معاني الكلمات", "معاني الكلمات", "كلمة بكلمة مع التفسير الميسر", Icons.Outlined.Translate, ToolGroup.Quran, true, "word-by-word"),
            SiteTool("hifz-test", "اختبار الحفظ", "اختبار الحفظ", "أخف الكلمات واختبر حفظك", Icons.Outlined.Autorenew, ToolGroup.Quran, true, "hifz-test"),
            SiteTool("hifz-plan", "خطة الحفظ والمراجعة", "خطة الحفظ", "مقطع جديد كل يوم ومراجعة متباعدة لما حفظت", Icons.Outlined.EventRepeat, ToolGroup.Quran, true, "hifz-plan"),
            SiteTool("downloads", "التلاوات المحمّلة", "التحميلات", "حمّل السور واستمع بلا إنترنت", Icons.Outlined.DownloadForOffline, ToolGroup.Quran, true, "downloads"),
            SiteTool("duas", "أدعية من القرآن والسنة", "الأدعية", "مصنفة حسب الحاجة مع المصدر", Icons.Outlined.VolunteerActivism, ToolGroup.Daily, true, "duas"),
            SiteTool("mushaf", "المصحف بالصفحات", "المصحف", "٦٠٤ صفحات بترقيم مصحف المدينة", Icons.Outlined.AutoStories, ToolGroup.Quran, true, "page/1"),
            SiteTool("ruqya", "الرقية الشرعية", "الرقية", "آيات الرقية من القرآن تتلى متتابعة", Icons.Outlined.PlayCircle, ToolGroup.Quran, true, "ruqya"),
            SiteTool("asma-ul-husna", "أسماء الله الحسنى", "الأسماء الحسنى", "بطاقة لكل اسم من الأسماء الحسنى بخلفية حقيقية", Icons.Outlined.Stars, ToolGroup.Quran, true, "asma"),
            SiteTool("hadith", "الأحاديث النبوية حسب الموضوع", "الأحاديث", "أحاديث صحيحة وحسنة بالتخريج والحكم والشرح", Icons.AutoMirrored.Outlined.LibraryBooks, ToolGroup.Hadith, true, "hadith"),
            SiteTool("hadith-checker", "التحقق من حديث", "تحقق من حديث", "الصق حديثا وصلك لتعرف تخريجه وحكمه", Icons.Outlined.Verified, ToolGroup.Hadith, true, "hadith-check"),
            SiteTool("prayer-guide", "صفة الوضوء والصلاة", "الوضوء والصلاة", "من الأحاديث الصحيحة مع الشرح", Icons.Outlined.WaterDrop, ToolGroup.Hadith, true, "guide"),
            SiteTool("kids-juz-amma", "تحفيظ الأطفال جزء عم", "تحفيظ الأطفال", "استماع وتكرار ونجوم وشهادة باسم الطفل", Icons.Outlined.ChildCare, ToolGroup.Hadith, true, "kids"),
            SiteTool("kids-quiz", "مسابقة جزء عم للأطفال", "مسابقة الأطفال", "أكمل الآية ومن أي سورة، من نص المصحف", Icons.Outlined.Quiz, ToolGroup.Hadith, true, "kids-quiz"),
            SiteTool("group-khatma", "الختمة الجماعية", "الختمة الجماعية", "وزع الأجزاء على المجموعة برابط واحد", Icons.Outlined.Groups, ToolGroup.Quran, true, "khatma"),
            SiteTool("qada-salah", "متتبع قضاء الصلوات", "قضاء الصلوات", "عداد لكل صلاة وخطة يومية", Icons.Outlined.Checklist, ToolGroup.Daily, true, "qada"),
            SiteTool("sawm-tracker", "متتبع الصيام", "متتبع الصيام", "قضاء رمضان والتطوع على تقويم هجري وميلادي", Icons.Outlined.DarkMode, ToolGroup.Daily, true, "sawm"),
            SiteTool("nearby-mosques", "المساجد القريبة", "المساجد القريبة", "أقرب المساجد مع المسافة والاتجاهات", Icons.Outlined.Place, ToolGroup.Daily, true, "mosques"),
        )

        val featured = setOf("clip", "ayah-card", "status")

        fun byId(id: String) = all.first { it.id == id }
    }
}

/** How often each tool is opened, on this device only («الأكثر استخداما»). */
object ToolUsage {
    private fun sp(c: Context) = c.getSharedPreferences("tools.usage", Context.MODE_PRIVATE)
    fun record(c: Context, id: String) { val p = sp(c); p.edit().putInt(id, p.getInt(id, 0) + 1).apply() }
    fun hasHistory(c: Context) = sp(c).all.isNotEmpty()
    fun top(c: Context, n: Int): List<SiteTool> {
        val counts = sp(c).all.mapValues { (it.value as? Int) ?: 0 }
        val used = SiteTool.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
        val defaults = listOf("listen", "prayer-times", "azkar", "qibla", "tasbih", "search", "hadith-card", "khatma").map { SiteTool.byId(it) }
        return (used + defaults).distinctBy { it.id }.take(n)
    }
}

/** Opens a tool: tab roots switch tabs, the rest push a screen. Counts the use. */
fun openTool(nav: androidx.navigation.NavHostController, c: Context, t: SiteTool) {
    ToolUsage.record(c, t.id)
    val r = t.route
    if (r == "home" || r == "quran" || r == "azkar" || r == "more" || r.startsWith("studio")) {
        if (r.startsWith("studio")) nav.navigate(r) { launchSingleTop = true } else nav.goTab(r)
    } else nav.navigate(r)
}
