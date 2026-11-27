package com.ayattla.app

import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import com.ayattla.app.ui.components.softShadow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
    private fun selfTestClip(video: Boolean = false) {
        val app = application as AyatApp
        (this as androidx.lifecycle.LifecycleOwner).let { owner ->
            owner.lifecycleScope.launch {
                val t0 = System.currentTimeMillis()
                runCatching {
                    val bg = if (video) com.ayattla.app.studio.defaultMedia(this@MainActivity, true) ?: error("no background video") else null
                    android.util.Log.i("AyatSelfTest", "background=${bg?.previewUrl}")
                    val p = com.ayattla.app.studio.Project(
                        ranges = listOf(com.ayattla.app.studio.AyahRange(108, 1, 3)), ratio = com.ayattla.app.studio.CardRatio.Story,
                        background = bg?.let { com.ayattla.app.studio.Background.Library(it) } ?: com.ayattla.app.studio.Background.Color,
                        style = com.ayattla.app.studio.Style(anim = if (video) com.ayattla.app.studio.Anim.Words else com.ayattla.app.studio.Anim.Slide),
                        pauses = if (video) mapOf("108:1" to 500) else emptyMap(),
                    )
                    com.ayattla.app.studio.ClipEngine(this@MainActivity, app.content).make(p, app.content.reciters.first()) {}
                }.onSuccess { f ->
                    val r = android.media.MediaMetadataRetriever().apply { setDataSource(f.path) }
                    val ms = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val audio = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    val rot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    r.release()
                    android.util.Log.i("AyatSelfTest", "CLIP OK video=$video bytes=${f.length()} durationMs=$ms size=${w}x$h rotation=$rot audio=$audio took=${System.currentTimeMillis() - t0}ms")
                }.onFailure { android.util.Log.e("AyatSelfTest", "CLIP FAILED", it) }
            }
        }
    }

    /** Notification taps: "route:<screen>", "azkar:<word in the category title>" (and 2.0's "azkar:morning"/"azkar:evening"/"home"). */
    private fun openTarget(i: android.content.Intent?): String? {
        val o = i?.getStringExtra("open") ?: return null
        val content = (application as AyatApp).content
        fun cat(word: String) = content.azkarCategories.firstOrNull { word in it.title }?.let { Routes.azkar(it.id) } ?: "azkar"
        return when {
            o == "azkar:morning" -> cat("الصباح")
            o == "azkar:evening" -> cat("المساء")
            o.startsWith("azkar:") -> cat(o.removePrefix("azkar:"))
            o.startsWith("route:") -> o.removePrefix("route:")
            o == "home" -> "home"
            else -> null
        }
    }

    /** A notification tapped while the app is open opens its screen too. */
    private val pendingOpen = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        openTarget(intent)?.let { pendingOpen.value = it }
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
        if (debuggable && intent.getStringExtra("selftest") == "clipvideo") selfTestClip(video = true)
        val startScreen = openTarget(intent) ?: if (debuggable) intent.getStringExtra("screen") else null
        // Debug builds: «-e offline 1» shows the app as it is without internet (CI screenshots and checks).
        if (debuggable && intent.getStringExtra("offline") == "1") com.ayattla.app.data.Net.simulateOffline = true
        setContent {
            AyatTheme(app.prefs.themeMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalContent provides app.content,
                    LocalPrefs provides app.prefs,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AppRoot(startScreen, pendingOpen)
                        // «تجربة البداية»: first run only (never after an update, never twice), on a normal launch.
                        var welcome by remember {
                            mutableStateOf(startScreen == "welcome" || startScreen == null && com.ayattla.app.data.Welcome.shouldShow(this@MainActivity))
                        }
                        // «الافتتاحية»: only on a normal launch (not a deep link, notification or CI screen), not with the welcome.
                        var intro by remember { mutableStateOf(!welcome && (startScreen == "intro" || startScreen == null && !com.ayattla.app.ui.components.IntroPrefs.shownThisLaunch && com.ayattla.app.ui.components.IntroPrefs.enabled(this@MainActivity))) }
                        if (intro) com.ayattla.app.ui.components.IntroOverlay(force = startScreen == "intro") { com.ayattla.app.ui.components.IntroPrefs.shownThisLaunch = true; intro = false }
                        if (welcome) com.ayattla.app.screens.WelcomeFlow { com.ayattla.app.ui.components.IntroPrefs.shownThisLaunch = true; welcome = false }
                    }
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("home", "الرئيسية", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("quran", "القرآن", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Tab("studio", "اصنع", Icons.Outlined.SmartDisplay, Icons.Filled.SmartDisplay),
    Tab("azkar", "الأذكار", Icons.Outlined.WbSunny, Icons.Filled.WbSunny),
    Tab("more", "المزيد", Icons.Outlined.GridView, Icons.Filled.GridView),
)

/** Space a tab screen leaves at its bottom for the floating tab bar (and the player above it). */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

object Routes {
    fun reader(surah: Int, ayah: Int = 1) = "reader/$surah/$ayah"
    fun azkar(category: Int) = "azkar/$category"
    fun studio(surah: Int, from: Int, to: Int, video: Boolean = false) = "studio?s=$surah&a=$from&b=$to&v=${if (video) 1 else 0}"
    fun studioMode(video: Boolean) = "studio?v=${if (video) 1 else 0}"
    fun tasmee(surah: Int, from: Int, to: Int) = "tasmee?s=$surah&a=$from&b=$to"
    fun path(surah: Int, ayah: Int) = "path/$surah/$ayah"
}

@Composable
fun AppRoot(startScreen: String? = null, openRequest: androidx.compose.runtime.MutableState<String?>? = null) {
    val nav = rememberNavController()
    val req = openRequest?.value
    androidx.compose.runtime.LaunchedEffect(req) {
        if (req != null) { if (req != "home") runCatching { nav.navigate(req) }; openRequest.value = null }
    }
    androidx.compose.runtime.LaunchedEffect(startScreen) {
        if (startScreen != null && startScreen != "home") runCatching { nav.navigate(startScreen) }
    }
    val b = LocalBrand.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore("?")
    // Anonymous stats: which screens are opened (the route pattern only, never its arguments).
    val statsCtx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(route) { route?.let { com.ayattla.app.data.Stats.event(statsCtx, "screen_viewed", it.substringBefore("/")) } }
    val onTab = tabs.any { route == it.route }
    val stripShown = !com.ayattla.app.data.Net.isOnline && !offlineStripClosed.value
    val inset = if (onTab) (if (Recitation.active) 190.dp else 120.dp) + (if (stripShown) 64.dp else 0.dp) else 0.dp
    Box(Modifier.fillMaxSize().patternedScreen(b)) {
        Column(Modifier.fillMaxSize()) {
          CompositionLocalProvider(LocalBottomInset provides inset) {
            NavHost(
                nav, startDestination = "home", modifier = Modifier.weight(1f).fillMaxWidth(),
                // Tabs cross-fade; a tool slides in from the reading side (RTL) and back out.
                enterTransition = {
                    if (isTabSwitch()) fadeIn(tween(180))
                    else fadeIn(tween(220)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 10 }
                },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = {
                    if (isTabSwitch()) fadeOut(tween(160))
                    else fadeOut(tween(200)) + slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 10 }
                },
            ) {
            composable("home") { com.ayattla.app.screens.HomeScreenV5(nav) }
            composable("search") { com.ayattla.app.screens.UnifiedSearchScreen(nav) }
            composable("path/{s}/{a}", arguments = listOf(navArgument("s") { type = NavType.IntType }, navArgument("a") { type = NavType.IntType })) { e ->
                com.ayattla.app.screens.AyahPathScreen(nav, e.arguments!!.getInt("s"), e.arguments!!.getInt("a"))
            }
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
            composable("tajweed") { com.ayattla.app.screens.TajweedScreen(nav) }
            composable("word-by-word") { com.ayattla.app.screens.WordByWordScreen(nav) }
            composable("hifz-test") { com.ayattla.app.screens.HifzTestScreen(nav) }
            composable("ruqya") { com.ayattla.app.screens.RuqyaScreen(nav) }
            composable("asma") { com.ayattla.app.screens.AsmaScreen(nav) }
            composable("occasions") { com.ayattla.app.screens.OccasionsScreen(nav) }
            composable("qada") { com.ayattla.app.screens.QadaScreen(nav) }
            composable("bio") { com.ayattla.app.screens.BioScreen(nav) }
            composable("sawm") { com.ayattla.app.screens.SawmScreen(nav) }
            composable("mosques") { com.ayattla.app.screens.MosquesScreen(nav) }
            composable("kids") { com.ayattla.app.screens.KidsScreen(nav) }
            composable("kids/{n}", arguments = listOf(navArgument("n") { type = NavType.IntType })) { e -> com.ayattla.app.screens.KidsSurahScreen(nav, e.arguments!!.getInt("n")) }
            composable("notifications") { com.ayattla.app.notify.NotificationsScreen(nav) }
            composable("prayer-settings") { com.ayattla.app.notify.PrayerSettingsScreen(nav) }
            composable("hifz-plan") { com.ayattla.app.screens.HifzPlanScreen(nav) }
            composable("downloads") { com.ayattla.app.screens.DownloadsScreen(nav) }
            composable("kids-quiz") { com.ayattla.app.screens.KidsQuizScreen(nav) }
            composable("ayah-today") { com.ayattla.app.screens.AyahTodayScreen(nav) }
            composable("text-card") { com.ayattla.app.studio.TextCardScreen(nav) }
            composable("status") { com.ayattla.app.studio.StatusScreen(nav) }
            composable("status/{slug}") { e -> com.ayattla.app.studio.StatusOccasionScreen(nav, e.arguments?.getString("slug").orEmpty()) }
            composable("prayer") { PrayerTimesScreen(nav) }
            composable("qibla") { QiblaScreen(nav) }
            composable("hijri") { HijriScreen(nav) }
            composable("zakat") { ZakatScreen(nav) }
            composable("saved") { SavedScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
            composable("about") { AboutScreen(nav) }
            composable("sources") { com.ayattla.app.screens.SourcesScreen(nav) }
            composable("week") { com.ayattla.app.screens.WeekScreen(nav) }
            composable("radios") { RadiosScreen(nav) }
            composable("hadith") { com.ayattla.app.screens.HadithScreen(nav) }
            composable("hadith-check") { com.ayattla.app.screens.HadithCheckScreen(nav) }
            composable("topics") { com.ayattla.app.screens.TopicsScreen(nav) }
            composable("topic/{slug}") { e -> com.ayattla.app.screens.TopicScreen(nav, e.arguments?.getString("slug").orEmpty()) }
            composable("duas") { com.ayattla.app.screens.DuasScreen(nav) }
            composable("guide") { com.ayattla.app.screens.GuideScreen(nav) }
            composable("khatma") { com.ayattla.app.screens.KhatmaScreen(nav) }
            composable("offline?from={from}", arguments = listOf(navArgument("from") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                com.ayattla.app.screens.OfflineScreen(nav, e.arguments?.getString("from"))
            }
            composable("journey") { com.ayattla.app.screens.JourneyScreen(nav) }
            composable(
                "tasmee?s={s}&a={a}&b={b}",
                arguments = listOf(
                    navArgument("s") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("a") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("b") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { e ->
                val a = e.arguments
                fun arg(k: String) = a?.getInt(k)?.takeIf { it > 0 }
                com.ayattla.app.screens.TasmeeScreen(nav, arg("s"), arg("a"), arg("b"))
            }
            composable("voice-search") { com.ayattla.app.screens.VoiceSearchScreen(nav) }
            composable("welcome-edit") { com.ayattla.app.screens.WelcomeFlow(edit = true) { nav.popBackStack() } }
            composable("kaffarah") { com.ayattla.app.screens.KaffarahScreen(nav) }
            composable("crypto-zakat") { com.ayattla.app.screens.CryptoZakatScreen(nav) }
            composable("page/{page}", arguments = listOf(navArgument("page") { type = NavType.IntType })) { e ->
                com.ayattla.app.screens.MushafScreen(nav, e.arguments!!.getInt("page"))
            }
            }
          }
            if (!onTab && Recitation.active) PlayerBar()
        }
        if (onTab) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()) {
            OfflineStrip(route, nav)
            if (Recitation.active) Box(Modifier.padding(horizontal = 12.dp)) { PlayerBar() }
            FloatingTabBar(route) { nav.goTab(it) }
        } else Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = if (Recitation.active) 84.dp else 12.dp)) {
            OfflineStrip(route, nav)
        }
    }
}

/** Session flag: the offline strip was closed (it shows again the next time the connection drops). */
private val offlineStripClosed = androidx.compose.runtime.mutableStateOf(false)

/** «أنت بدون إنترنت» above the tab bar while there is no connection, except in the offline centre itself. */
@Composable
private fun OfflineStrip(route: String?, nav: NavHostController) {
    val offline = !com.ayattla.app.data.Net.isOnline
    androidx.compose.runtime.LaunchedEffect(offline) { if (!offline) offlineStripClosed.value = false }
    androidx.compose.animation.AnimatedVisibility(
        visible = offline && !offlineStripClosed.value && route != "offline",
        enter = fadeIn(tween(220)) + androidx.compose.animation.expandVertically(),
        exit = fadeOut(tween(160)) + androidx.compose.animation.shrinkVertically(),
    ) {
        com.ayattla.app.ui.components.OfflinePill(Modifier.padding(bottom = 6.dp), onOpen = { nav.navigate("offline") }) { offlineStripClosed.value = true }
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.isTabSwitch(): Boolean {
    val from = initialState.destination.route?.substringBefore("?")
    val to = targetState.destination.route?.substringBefore("?")
    return tabs.any { it.route == from } && tabs.any { it.route == to }
}

/** iOS-style floating tab bar (the previous design): white rounded bar, the chosen tab filled with the site gradient. */
@Composable
private fun FloatingTabBar(route: String?, onSelect: (String) -> Unit) {
    val b = LocalBrand.current
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .softShadow(shape, 24.dp, Color(0xFF0F1F17).copy(alpha = if (b.dark) 0.5f else 0.16f), DpOffset(0.dp, 10.dp))
            .clip(shape).background(b.surface.copy(alpha = 0.97f)).border(1.dp, b.line, shape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { t ->
            val on = route == t.route
            val fade by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, androidx.compose.animation.core.tween(220), label = "tab")
            val scale by androidx.compose.animation.core.animateFloatAsState(if (on) 1.08f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f), label = "tabIcon")
            Column(
                Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(b.siteGradient), alpha = fade)
                    .clickable { onSelect(t.route) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (on) t.selectedIcon else t.icon, t.label, tint = if (on) Color.White else b.muted,
                    modifier = Modifier.size(25.dp).graphicsLayer { scaleX = scale; scaleY = scale })
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

