package com.ayattla.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
                    r.release()
                    android.util.Log.i("AyatSelfTest", "CLIP OK bytes=${f.length()} durationMs=$ms size=${w}x$h audio=$audio took=${System.currentTimeMillis() - t0}ms")
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
    Tab("home", "الرئيسية", Icons.Filled.Home),
    Tab("quran", "القرآن", Icons.AutoMirrored.Filled.MenuBook),
    Tab("studio", "اصنع", Icons.Filled.AutoAwesome),
    Tab("azkar", "الأذكار", Icons.Filled.WbTwilight),
    Tab("more", "المزيد", Icons.Filled.GridView),
)

object Routes {
    fun reader(surah: Int, ayah: Int = 1) = "reader/$surah/$ayah"
    fun azkar(category: Int) = "azkar/$category"
    fun studio(surah: Int, from: Int, to: Int) = "studio?s=$surah&a=$from&b=$to"
}

@Composable
fun AppRoot(startScreen: String? = null) {
    val nav = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(startScreen) {
        if (startScreen != null && startScreen != "home") runCatching { nav.navigate(startScreen) }
    }
    val b = LocalBrand.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = tabs.any { route == it.route || (it.route == "studio" && route?.startsWith("studio") == true) }
    Scaffold(
        containerColor = b.background,
        bottomBar = {
            Column {
                if (Recitation.active) PlayerBar()
                if (onTab) NavigationBar(containerColor = b.surface, tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        val selected = route == t.route || (t.route == "studio" && route?.startsWith("studio") == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = b.onPrimary, indicatorColor = b.primary,
                                selectedTextColor = b.primaryInk, unselectedIconColor = b.muted, unselectedTextColor = b.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize().padding(pad)) {
            composable("home") { HomeScreen(nav) }
            composable("quran") { QuranIndexScreen(nav) }
            composable(
                "studio?s={s}&a={a}&b={b}",
                arguments = listOf(
                    navArgument("s") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("a") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("b") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { e ->
                val a = e.arguments!!
                StudioScreen(nav, a.getInt("s"), a.getInt("a"), a.getInt("b"))
            }
            composable("azkar") { AzkarListScreen(nav) }
            composable("more") { MoreScreen(nav) }
            composable(
                "reader/{surah}/{ayah}",
                arguments = listOf(navArgument("surah") { type = NavType.IntType }, navArgument("ayah") { type = NavType.IntType }),
            ) { e -> ReaderScreen(nav, e.arguments!!.getInt("surah"), e.arguments!!.getInt("ayah")) }
            composable("azkar/{cat}", arguments = listOf(navArgument("cat") { type = NavType.IntType })) { e ->
                AzkarScreen(nav, e.arguments!!.getInt("cat"))
            }
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
}

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

