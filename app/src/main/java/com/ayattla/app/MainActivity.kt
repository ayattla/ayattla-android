package com.ayattla.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import com.ayattla.app.ui.components.patternedScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayattla.app.audio.Recitation
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.Prefs
import com.ayattla.app.screens.AboutScreen
import com.ayattla.app.screens.AzkarListScreen
import com.ayattla.app.screens.AzkarScreen
import com.ayattla.app.screens.HijriScreen
import com.ayattla.app.screens.HomeScreen
import com.ayattla.app.screens.MoreScreen
import com.ayattla.app.screens.PlayerBar
import com.ayattla.app.screens.PrayerTimesScreen
import com.ayattla.app.screens.QiblaScreen
import com.ayattla.app.screens.QuranIndexScreen
import com.ayattla.app.screens.RadiosScreen
import com.ayattla.app.screens.ReaderScreen
import com.ayattla.app.screens.SavedScreen
import com.ayattla.app.screens.SettingsScreen
import com.ayattla.app.screens.TasbihScreen
import com.ayattla.app.screens.ZakatScreen
import com.ayattla.app.studio.StudioScreen
import com.ayattla.app.ui.theme.AyatTheme
import com.ayattla.app.ui.theme.LocalBrand

val LocalContent = staticCompositionLocalOf<ContentStore> { error("content") }
val LocalPrefs = staticCompositionLocalOf<Prefs> { error("prefs") }

class MainActivity : ComponentActivity() {
    /** Debug only: makes a real 3-ayah clip (Al-Kawthar) and logs the result, for the CI emulator run. */
    private fun selfTestClip() {
        val app = application as AyatApp
        (this as androidx.lifecycle.LifecycleOwner).let { owner ->
            owner.lifecycleScope.launch {
                val t0 = System.currentTimeMillis()
                runCatching {
                    com.ayattla.app.studio.ClipEngine(this@MainActivity, app.content).make(
                        app.content.reciters.first(), 108, 1, 3,
                        com.ayattla.app.studio.CardRatio.Story, com.ayattla.app.studio.CardStyle.Emerald,
                    ) {}
                }.onSuccess { f ->
                    val r = android.media.MediaMetadataRetriever().apply { setDataSource(f.path) }
                    val ms = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val audio = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    val rot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    r.release()
                    android.util.Log.i("AyatSelfTest", "CLIP OK bytes=${f.length()} durationMs=$ms size=${w}x$h rotation=$rot audio=$audio took=${System.currentTimeMillis() - t0}ms")
                }.onFailure { android.util.Log.e("AyatSelfTest", "CLIP FAILED", it) }
            }
        }
    }

    /** Notification taps: "home", "azkar:morning" or "azkar:evening". */
    private fun openTarget(i: android.content.Intent?): String? {
        val o = i?.getStringExtra("open") ?: return null
        val content = (application as AyatApp).content
        fun cat(word: String) = content.azkarCategories.firstOrNull { word in it.title }?.let { Routes.azkar(it.id) } ?: "azkar"
        return when (o) {
            "azkar:morning" -> cat("الصباح")
            "azkar:evening" -> cat("المساء")
            "home" -> "home"
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as AyatApp
        Recitation.connect(this)
        // Debug builds only: open a given screen directly (used by the CI screenshots).
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent.getStringExtra("selftest") == "clip") selfTestClip()
        val startScreen = openTarget(intent) ?: if (debuggable) intent.getStringExtra("screen") else null
        setContent {
            AyatTheme(app.prefs.themeMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalContent provides app.content,
                    LocalPrefs provides app.prefs,
                ) { AppRoot(startScreen) }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "الرئيسية", Icons.Outlined.Home),
    Tab("quran", "القرآن", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("studio", "اصنع", Icons.Outlined.SmartDisplay),
    Tab("azkar", "الأذكار", Icons.Outlined.WbSunny),
    Tab("more", "المزيد", Icons.Outlined.GridView),
)

/** Space a tab screen leaves at its bottom for the floating tab bar (and the player above it). */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

object Routes {
    fun reader(surah: Int, ayah: Int = 1) = "reader/$surah/$ayah"
    fun azkar(category: Int) = "azkar/$category"
    fun studio(surah: Int, from: Int, to: Int, video: Boolean = false) = "studio?s=$surah&a=$from&b=$to&v=${if (video) 1 else 0}"
    fun studioMode(video: Boolean) = "studio?v=${if (video) 1 else 0}"
}

@Composable
fun AppRoot(startScreen: String? = null) {
    val nav = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(startScreen) {
        if (startScreen != null && startScreen != "home") runCatching { nav.navigate(startScreen) }
    }
    val b = LocalBrand.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore("?")
    val onTab = tabs.any { route == it.route }
    val inset = if (onTab) (if (Recitation.active) 190.dp else 120.dp) else 0.dp
    Box(Modifier.fillMaxSize().patternedScreen(b)) {
        Column(Modifier.fillMaxSize()) {
          CompositionLocalProvider(LocalBottomInset provides inset) {
            NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f).fillMaxWidth()) {
            composable("home") { HomeScreen(nav) }
            composable("quran") { QuranIndexScreen(nav) }
            composable(
                "studio?s={s}&a={a}&b={b}&v={v}",
                arguments = listOf(
                    navArgument("s") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("a") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("b") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("v") { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { e ->
                val a = e.arguments!!
                StudioScreen(nav, a.getInt("s"), a.getInt("a"), a.getInt("b"), a.getInt("v"))
            }
            composable("azkar") { AzkarListScreen(nav) }
            composable(
                "more?group={group}&focus={focus}",
                arguments = listOf(
                    navArgument("group") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("focus") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { e -> MoreScreen(nav, e.arguments?.getString("group"), e.arguments?.getInt("focus") == 1) }
            composable(
                "reader/{surah}/{ayah}",
                arguments = listOf(navArgument("surah") { type = NavType.IntType }, navArgument("ayah") { type = NavType.IntType }),
            ) { e -> ReaderScreen(nav, e.arguments!!.getInt("surah"), e.arguments!!.getInt("ayah")) }
            composable("azkar/{cat}", arguments = listOf(navArgument("cat") { type = NavType.IntType })) { e ->
                AzkarScreen(nav, e.arguments!!.getInt("cat"))
            }
            composable("azkar-all") { com.ayattla.app.screens.AzkarAllScreen(nav) }
            composable("tasbih") { TasbihScreen(nav) }
            composable("prayer") { PrayerTimesScreen(nav) }
            composable("qibla") { QiblaScreen(nav) }
            composable("hijri") { HijriScreen(nav) }
            composable("zakat") { ZakatScreen(nav) }
            composable("saved") { SavedScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
            composable("about") { AboutScreen(nav) }
            composable("radios") { RadiosScreen(nav) }
            composable("hadith") { com.ayattla.app.screens.HadithScreen(nav) }
            composable("hadith-check") { com.ayattla.app.screens.HadithCheckScreen(nav) }
            composable("topics") { com.ayattla.app.screens.TopicsScreen(nav) }
            composable("topic/{slug}") { e -> com.ayattla.app.screens.TopicScreen(nav, e.arguments?.getString("slug").orEmpty()) }
            composable("duas") { com.ayattla.app.screens.DuasScreen(nav) }
            composable("guide") { com.ayattla.app.screens.GuideScreen(nav) }
            composable("khatma") { com.ayattla.app.screens.KhatmaScreen(nav) }
            composable("page/{page}", arguments = listOf(navArgument("page") { type = NavType.IntType })) { e ->
                com.ayattla.app.screens.MushafScreen(nav, e.arguments!!.getInt("page"))
            }
            }
          }
            if (!onTab && Recitation.active) PlayerBar()
        }
        if (onTab) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()) {
            if (Recitation.active) Box(Modifier.padding(horizontal = 12.dp)) { PlayerBar() }
            FloatingTabBar(route) { nav.goTab(it) }
        }
    }
}

/** iOS-style floating tab bar: white rounded bar, the chosen tab filled with the site gradient. */
@Composable
private fun FloatingTabBar(route: String?, onSelect: (String) -> Unit) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .dropShadow(shape, Shadow(radius = 24.dp, color = Color(0xFF0F1F17).copy(alpha = if (b.dark) 0.5f else 0.16f), offset = DpOffset(0.dp, 10.dp)))
            .clip(shape).background(b.surface.copy(alpha = 0.97f)).border(1.dp, b.line, shape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { t ->
            val on = route == t.route
            Column(
                Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(24.dp))
                    .then(if (on) Modifier.background(Brush.linearGradient(b.siteGradient)) else Modifier)
                    .clickable { onSelect(t.route) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(t.icon, t.label, tint = if (on) Color.White else b.muted, modifier = Modifier.size(25.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = if (on) Color.White else b.muted, maxLines = 1)
            }
        }
    }
}

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

