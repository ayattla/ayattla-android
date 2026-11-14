package com.ayattla.app.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.Routes
import com.ayattla.app.calc.Hijri
import com.ayattla.app.calc.PrayerCity
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.calc.Qibla
import com.ayattla.app.calc.Zakat
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.AppTopBar
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.EmeraldPanel
import com.ayattla.app.ui.components.EmptyState
import com.ayattla.app.ui.components.SmallShape
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import com.ayattla.app.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun MoreScreen(nav: NavHostController) {
    val b = LocalBrand.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("المزيد", style = MaterialTheme.typography.headlineSmall, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        ToolGrid(
            listOf(
                ToolItem("مواقيت الصلاة", "حسب مدينتك", Icons.Filled.AccessTime) { nav.navigate("prayer") },
                ToolItem("القبلة", "بوصلة الاتجاه", Icons.Filled.Explore) { nav.navigate("qibla") },
                ToolItem("المسبحة", "عداد التسبيح", Icons.Filled.TouchApp) { nav.navigate("tasbih") },
                ToolItem("التاريخ الهجري", "تحويل التاريخ", Icons.Filled.CalendarMonth) { nav.navigate("hijri") },
                ToolItem("الزكاة", "حاسبة النصاب", Icons.Filled.Calculate) { nav.navigate("zakat") },
                ToolItem("آياتي المحفوظة", "ما حفظته من آيات", Icons.Filled.Favorite) { nav.navigate("saved") },
                ToolItem("الإعدادات", "القارئ والمظهر", Icons.Filled.Settings) { nav.navigate("settings") },
                ToolItem("عن التطبيق", "المصادر والتراخيص", Icons.Filled.Info) { nav.navigate("about") },
            )
        )
    }
}

@Composable
fun TasbihScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val phrases = content.tasbihPhrases
    var phrase by rememberSaveable { mutableStateOf(phrases.first()) }
    var target by rememberSaveable { mutableIntStateOf(33) }
    var count by remember(phrase) { mutableIntStateOf(prefs.tasbihCount(phrase)) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("المسبحة", onBack = { nav.popBackStack() }) {
            IconButton(onClick = { count = 0; prefs.setTasbihCount(phrase, 0) }) { Icon(Icons.Filled.Refresh, "تصفير") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            phrases.forEach { p -> Chip(p, p == phrase, { phrase = p }) }
        }
        Row(Modifier.padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(33, 100, 1000, 0).forEach { t -> Chip(if (t == 0) "بلا هدف" else "$t", target == t, { target = t }) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(260.dp).clip(CircleShape).background(b.primary).clickable {
                    count += 1
                    prefs.setTasbihCount(phrase, count)
                    vibrate(ctx, strong = target > 0 && count % target == 0)
                },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(phrase, fontFamily = Fonts.naskh, fontSize = 22.sp, color = b.accent, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
                    Text("$count", style = MaterialTheme.typography.displayMedium, color = b.onPrimary)
                    if (target > 0) Text(
                        if (count >= target) "أتممت ${count / target} × $target" else "بقي ${target - count % target}",
                        style = MaterialTheme.typography.bodyMedium, color = b.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }
        Text("اضغط على الدائرة للتسبيح", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
    }
}

@Composable
fun PrayerTimesScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    val loc = rememberPrayerLocation()
    var day by remember { mutableStateOf(LocalDate.now()) }
    val times = remember(loc, day) { PrayerTimes.forDate(day, loc.lat, loc.lng, loc.zone, loc.method) }
    var message by remember { mutableStateOf<String?>(null) }
    val request = rememberLocationRequest { l ->
        if (l != null) { prefs.setMyLocation(l.latitude, l.longitude); message = null }
        else message = "تعذر تحديد موقعك. اختر مدينة من القائمة."
    }
    val now = LocalTime.now()
    val next = if (day == LocalDate.now()) PrayerTimes.next(now.hour * 60 + now.minute, times, PrayerTimes.forDate(day.plusDays(1), loc.lat, loc.lng, loc.zone, loc.method)) else null

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("مواقيت الصلاة", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmeraldPanel {
                Text(loc.name, style = MaterialTheme.typography.titleLarge, color = b.onPrimary)
                Text("${Hijri.format(day)} · $day", style = MaterialTheme.typography.bodySmall, color = b.accent)
                if (next != null) Text("${next.prayer.title} بعد ${Num.duration(next.minutesUntil)}", style = MaterialTheme.typography.titleMedium, color = b.onPrimary, modifier = Modifier.padding(top = 6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { day = day.minusDays(1) }, modifier = Modifier.weight(1f)) { Text("اليوم السابق") }
                OutlinedButton(onClick = { day = LocalDate.now() }, modifier = Modifier.weight(1f)) { Text("اليوم") }
                OutlinedButton(onClick = { day = day.plusDays(1) }, modifier = Modifier.weight(1f)) { Text("اليوم التالي") }
            }
            AppCard {
                com.ayattla.app.calc.Prayer.entries.forEach { p ->
                    val isNext = next != null && !next.isTomorrow && next.prayer == p
                    Row(
                        Modifier.fillMaxWidth().clip(SmallShape).background(if (isNext) b.tint else b.surface).padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, color = if (p.isPrayer) b.ink else b.muted, modifier = Modifier.weight(1f))
                        Text(PrayerTimes.hm12(times.hours(p)), style = MaterialTheme.typography.titleMedium, color = if (isNext) b.primaryInk else b.ink)
                    }
                }
            }
            Text("المدينة", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Button(onClick = request, colors = ButtonDefaults.buttonColors(containerColor = b.primary), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.size(8.dp)); Text("استخدم موقعي")
            }
            message?.let { Text(it, color = b.goldInk, style = MaterialTheme.typography.bodySmall) }
            FlowChips(PrayerCity.all.map { it.name }, prefs.cityName) { prefs.setCity(it) }
            Text(
                "طريقة الحساب مطابقة لموقع آيات تتلى: أم القرى للمدن السعودية، والعشاء بعد المغرب بـ 90 دقيقة (120 في رمضان).",
                style = MaterialTheme.typography.bodySmall, color = b.muted,
            )
        }
    }
}

@Composable
fun FlowChips(items: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { Chip(it, it == selected, { onPick(it) }) } }
        }
    }
}

@Composable
fun QiblaScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val loc = rememberPrayerLocation()
    val bearing = remember(loc) { Qibla.bearing(loc.lat, loc.lng) }
    val km = remember(loc) { Qibla.distanceKm(loc.lat, loc.lng) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    val request = rememberLocationRequest { l -> if (l != null) prefs.setMyLocation(l.latitude, l.longitude) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        hasSensor = sensor != null
        val rot = FloatArray(9)
        val ori = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val deg = ((Math.toDegrees(ori[0].toDouble()) + 360) % 360).toFloat()
                // Smooth the needle a little.
                var d = deg - azimuth
                if (d > 180) d -= 360
                if (d < -180) d += 360
                azimuth = (azimuth + d * 0.15f + 360) % 360
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("القبلة", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${loc.name} · ${bearing.roundToInt()}° من الشمال · ${km.roundToInt()} كم إلى الكعبة", style = MaterialTheme.typography.bodyMedium, color = b.muted, textAlign = TextAlign.Center)
            val diff = ((bearing - azimuth + 540) % 360) - 180
            val aligned = hasSensor && kotlin.math.abs(diff) < 4
            Canvas(Modifier.fillMaxWidth(0.85f).aspectRatio(1f)) {
                val r = size.minDimension / 2
                drawCircle(if (aligned) b.accent.copy(alpha = 0.25f) else b.surface2, r)
                drawCircle(b.lineStrong, r, style = Stroke(3f))
                rotate(-azimuth) {
                    // North tick
                    drawLine(b.muted, Offset(center.x, center.y - r), Offset(center.x, center.y - r + 28f), strokeWidth = 6f)
                    // Qibla needle
                    rotate(bearing.toFloat()) {
                        drawLine(b.primary, center, Offset(center.x, center.y - r * 0.82f), strokeWidth = 14f)
                        drawCircle(b.accent, 22f, Offset(center.x, center.y - r * 0.82f))
                    }
                }
                drawCircle(b.primary, 14f)
            }
            Text(
                when {
                    !hasSensor -> "لا توجد بوصلة في هذا الجهاز. اتجاه القبلة ${bearing.roundToInt()}° من الشمال."
                    aligned -> "أنت متجه إلى القبلة"
                    else -> "أدر الهاتف حتى تشير الإبرة إلى الأعلى"
                },
                style = MaterialTheme.typography.titleMedium, color = if (aligned) b.primaryInk else b.ink, textAlign = TextAlign.Center,
            )
            Button(onClick = request, colors = ButtonDefaults.buttonColors(containerColor = b.primary)) {
                Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.size(8.dp)); Text("حدّث موقعي")
            }
            Text("أبعد الهاتف عن المعادن والمغناطيس، وحرّكه على شكل 8 لمعايرة البوصلة.", style = MaterialTheme.typography.bodySmall, color = b.muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HijriScreen(nav: NavHostController) {
    val b = LocalBrand.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    val h = Hijri.fromGregorian(date)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("التاريخ الهجري", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmeraldPanel {
                Text("${h.day}", style = MaterialTheme.typography.displayMedium, color = b.onPrimary)
                Text("${Hijri.months[h.month - 1]} ${h.year} هـ", style = MaterialTheme.typography.titleLarge, color = b.accent)
                Text("$date م", style = MaterialTheme.typography.bodyMedium, color = b.onPrimary.copy(alpha = 0.8f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { date = date.minusDays(1) }, modifier = Modifier.weight(1f)) { Text("يوم قبل") }
                OutlinedButton(onClick = { date = LocalDate.now() }, modifier = Modifier.weight(1f)) { Text("اليوم") }
                OutlinedButton(onClick = { date = date.plusDays(1) }, modifier = Modifier.weight(1f)) { Text("يوم بعد") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { date = date.minusMonths(1) }, modifier = Modifier.weight(1f)) { Text("شهر قبل") }
                OutlinedButton(onClick = { date = date.plusMonths(1) }, modifier = Modifier.weight(1f)) { Text("شهر بعد") }
            }
            AppCard {
                Text("الشهر الهجري الحالي", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Text("${Hijri.months[h.month - 1]}: ${Hijri.monthLength(h.year, h.month)} يومًا", style = MaterialTheme.typography.bodyMedium, color = b.muted)
                val ramadan = Hijri.toGregorian(Hijri.Date(if (h.month <= 9) h.year else h.year + 1, 9, 1))
                ramadan?.let { Text("أول رمضان القادم: $it م", style = MaterialTheme.typography.bodyMedium, color = b.muted) }
            }
            Text("حسب تقويم أم القرى، وهو المعتمد في موقع آيات تتلى. قد يختلف يومًا عن رؤية الهلال.", style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}

@Composable
fun ZakatScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val fields = remember { mutableStateOf(mapOf<String, String>()) }
    var basis by remember { mutableStateOf(Zakat.Basis.Gold) }
    fun v(k: String) = fields.value[k]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    val r = Zakat.calculate(
        Zakat.Input(v("gp"), v("sp"), v("cash"), v("trade"), v("g24"), v("g21"), v("g18"), v("sv"), v("rec"), v("debt"), basis)
    )
    @Composable
    fun Field(key: String, label: String) {
        OutlinedTextField(
            value = fields.value[key] ?: "", onValueChange = { s -> fields.value = fields.value + (key to s.filter { it.isDigit() || it == '.' }) },
            label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("حاسبة الزكاة", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EmeraldPanel {
                Text(if (r.reachedNisab) "الزكاة الواجبة" else "لم يبلغ المال النصاب", style = MaterialTheme.typography.titleMedium, color = b.accent)
                Text(Num.money(r.due), style = MaterialTheme.typography.displaySmall, color = b.onPrimary)
                Text("صافي المال ${Num.money(r.total)} · النصاب ${Num.money(r.nisab)}", style = MaterialTheme.typography.bodySmall, color = b.onPrimary.copy(alpha = 0.8f))
            }
            Text("الأسعار", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Field("gp", "سعر جرام الذهب عيار 24")
            Field("sp", "سعر جرام الفضة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("النصاب بالذهب (85 جم)", basis == Zakat.Basis.Gold, { basis = Zakat.Basis.Gold })
                Chip("بالفضة (595 جم)", basis == Zakat.Basis.Silver, { basis = Zakat.Basis.Silver })
            }
            Text("الأموال", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Field("cash", "النقد والأرصدة")
            Field("trade", "عروض التجارة")
            Field("g24", "ذهب عيار 24 (جرام)")
            Field("g21", "ذهب عيار 21 (جرام)")
            Field("g18", "ذهب عيار 18 (جرام)")
            Field("sv", "فضة (جرام)")
            Field("rec", "ديون لك مرجوّة السداد")
            Field("debt", "ديون عليك حالّة")
            Text("الزكاة 2.5٪ من صافي المال إذا بلغ النصاب وحال عليه الحول. للحالات الخاصة راجع أهل العلم.", style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
    }
}

@Composable
fun SavedScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val saved = prefs.favorites.mapNotNull { k -> k.split(":").takeIf { it.size == 2 }?.let { it[0].toInt() to it[1].toInt() } }
        .sortedWith(compareBy({ it.first }, { it.second }))
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("آياتي المحفوظة", onBack = { nav.popBackStack() })
        if (saved.isEmpty()) EmptyState("لا آيات محفوظة بعد", "اضغط على أي آية في المصحف ثم اختر «حفظ الآية».")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            saved.forEach { (s, a) ->
                val ayah = content.ayah(s, a) ?: return@forEach
                AppCard(onClick = { nav.navigate(Routes.reader(s, a)) }) {
                    Text(ayah.text, fontFamily = Fonts.quran, fontSize = 20.sp, lineHeight = 38.sp, color = b.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("سورة ${content.surah(s).shortName} · $a", style = MaterialTheme.typography.labelMedium, color = b.goldInk, modifier = Modifier.weight(1f))
                        TextButton(onClick = { prefs.toggleFavorite("$s:$a") }) { Text("إزالة") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val current = rememberReciter()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("الإعدادات", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("المظهر", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("تلقائي", prefs.themeMode == ThemeMode.System, { prefs.setTheme(ThemeMode.System) })
                Chip("فاتح", prefs.themeMode == ThemeMode.Light, { prefs.setTheme(ThemeMode.Light) })
                Chip("داكن", prefs.themeMode == ThemeMode.Dark, { prefs.setTheme(ThemeMode.Dark) })
            }
            Text("القارئ", style = MaterialTheme.typography.titleMedium, color = b.ink)
            AppCard(padding = 4.dp) {
                content.reciters.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { prefs.setReciter(r.id) }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r.name, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
                        if (r.id == current.id) Box(Modifier.size(10.dp).clip(CircleShape).background(b.accent))
                    }
                    HorizontalDivider(color = b.line)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    val version = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppTopBar("عن التطبيق", onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmeraldPanel {
                Text("آيات تتلى", style = MaterialTheme.typography.headlineSmall, color = b.onPrimary)
                Text("الإصدار $version", style = MaterialTheme.typography.bodySmall, color = b.accent)
            }
            AppCard {
                Text("المصادر", style = MaterialTheme.typography.titleMedium, color = b.ink)
                listOf(
                    "نص القرآن: الرسم العثماني من مشروع Tanzil، منقول حرفيًا دون تعديل (${content.meta("ayah_count") ?: "6236"} آية).",
                    "الأذكار: أبواب حصن المسلم.",
                    "التلاوات: EveryAyah، تُشغَّل مباشرة من مصدرها.",
                    "المواقيت والقبلة والتاريخ الهجري: نفس معادلات موقع آيات تتلى، وتقويم أم القرى.",
                    "الخطوط: Amiri Quran وAmiri وIBM Plex Sans Arabic برخصة OFL.",
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = b.muted, modifier = Modifier.padding(top = 6.dp)) }
            }
            AppCard {
                Text("الخصوصية", style = MaterialTheme.typography.titleMedium, color = b.ink)
                Text("بلا إعلانات ولا حساب. كل بياناتك (المحفوظات، العدادات، الإعدادات) على جهازك فقط.", style = MaterialTheme.typography.bodyMedium, color = b.muted)
            }
            Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ayattla.com"))) }, colors = ButtonDefaults.buttonColors(containerColor = b.primary), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null); Spacer(Modifier.size(8.dp)); Text("زر موقع ayattla.com")
            }
            if (content.meta("built_at") != null) Text("حزمة المحتوى: ${content.meta("built_at")}", style = MaterialTheme.typography.labelSmall, color = b.muted)
        }
    }
}

