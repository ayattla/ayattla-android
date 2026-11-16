package com.ayattla.app.notify

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.ayattla.app.LocalPrefs
import com.ayattla.app.calc.Prayer
import com.ayattla.app.calc.PrayerTimes
import com.ayattla.app.screens.ToolPage
import com.ayattla.app.screens.prayerLocation
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.theme.LocalBrand
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "h:mm ص/م" for a minute of the day. */
fun clock(minute: Int): String {
    val m = ((minute % 1440) + 1440) % 1440
    val h = m / 60
    return String.format(Locale.US, "%d:%02d %s", if (h % 12 == 0) 12 else h % 12, m % 60, if (h < 12) "ص" else "م")
}

private val weekDays = listOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة")

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink)
    AppCard(content = content)
}

@Composable
private fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = b.ink)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = b.muted) }
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = b.green))
    }
}

@Composable
private fun <T> Choices(label: String, items: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    val b = LocalBrand.current
    Text(label, style = MaterialTheme.typography.labelMedium, color = b.muted, modifier = Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (v, t) -> Chip(t, v == selected, { onPick(v) }) }
    }
}

@Composable
private fun TimeButton(label: String, minute: Int, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = b.muted, modifier = Modifier.weight(1f))
        TextButton(onClick = { TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minute / 60, minute % 60, false).show() }) { Text(clock(minute)) }
    }
}

@Composable
fun NotificationsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val cfg = NotifyStore.config
    var refresh by remember { mutableIntStateOf(0) }
    // Permissions are read again whenever the screen comes back (after the user visits the settings).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.currentStateFlow.collect { if (it == androidx.lifecycle.Lifecycle.State.RESUMED) refresh++ } }
    val canNotify = remember(refresh) { Scheduler.canNotify(ctx) }
    val exact = remember(refresh) { Scheduler.exactAllowed(ctx) }
    val upcoming = remember(refresh, cfg) { runCatching { Scheduler.upcoming(ctx) }.getOrDefault(emptyList()) }
    var pending by remember { mutableStateOf<((NotifyConfig) -> NotifyConfig)?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pending?.let { f -> if (ok) NotifyStore.update(ctx, f) }
        pending = null; refresh++
    }
    /** Turning something on asks for the notification permission first (Android 13+). */
    fun set(f: (NotifyConfig) -> NotifyConfig) {
        if (f(cfg).anyOn && !Scheduler.canNotify(ctx) && Build.VERSION.SDK_INT >= 33) { pending = f; askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        else { NotifyStore.update(ctx, f); refresh++ }
    }
    val loc = remember(cfg) { prayerLocation(com.ayattla.app.data.Prefs(ctx), cfg) }
    val today = remember(loc) { loc.times(LocalDate.now()) }

    ToolPage(nav, "مركز التنبيهات") {
        // ── Status and permissions
        AppCard {
            Text(if (upcoming.isEmpty()) "لا توجد تنبيهات مجدولة" else "التالي: ${upcoming.first().title} · ${upcoming.first().at.format(DateTimeFormatter.ofPattern("EEEE h:mm a", Locale("ar")))}",
                style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
            Text("${upcoming.size} تنبيها خلال اليومين القادمين · ${loc.name}", style = MaterialTheme.typography.bodySmall, color = b.muted)
            Text(if (canNotify) "✓ إذن الإشعارات ممنوح" else "✗ إذن الإشعارات غير ممنوح", style = MaterialTheme.typography.bodyMedium,
                color = if (canNotify) b.primaryInk else b.goldInk, modifier = Modifier.padding(top = 8.dp))
            if (!canNotify) BrandButton("اسمح بالإشعارات", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) {
                if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
            Text(if (exact) "✓ التنبيه في الوقت بالضبط مسموح" else "✗ التنبيه في الوقت بالضبط غير مسموح: قد يتأخر التنبيه بضع دقائق",
                style = MaterialTheme.typography.bodyMedium, color = if (exact) b.primaryInk else b.goldInk)
            if (!exact && Build.VERSION.SDK_INT >= 31) BrandButton("اسمح بالتنبيه في الوقت بالضبط", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandButton("تنبيه تجريبي", Modifier.weight(1f), kind = BtnKind.Ghost) { if (canNotify) Scheduler.test(ctx) else if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS); refresh++ }
                BrandButton("أصوات القنوات", Modifier.weight(1f), kind = BtnKind.Ghost) {
                    Scheduler.channels(ctx)
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
            }
        }

        // ── Prayers
        Section("الصلوات") {
            val allOn = NotifyConfig.PRAYERS.all { cfg.prayers[it]?.on == true }
            SwitchRow("كل الصلوات الخمس", "الشروق ليس صلاة فلا يُرسل له تنبيه", allOn) { v ->
                set { c -> c.copy(prayers = c.prayers.mapValues { it.value.copy(on = v) }) }
            }
            var open by remember { mutableStateOf<Prayer?>(null) }
            NotifyConfig.PRAYERS.forEach { p ->
                val a = cfg.prayers[p] ?: PrayerAlert()
                val sub = buildString {
                    append(PrayerTimes.hm12(today.hours(p))); append(" · "); append(a.sound.title)
                    if (a.before > 0) append(" · قبلها ${a.before} د")
                    if (a.iqama > 0) append(" · الإقامة ${a.iqama} د")
                }
                SwitchRow("صلاة ${p.title}", sub, a.on) { v -> set { c -> c.copy(prayers = c.prayers + (p to a.copy(on = v))) } }
                TextButton(onClick = { open = if (open == p) null else p }) { Text(if (open == p) "إخفاء الإعدادات" else "إعدادات ${p.title}") }
                if (open == p) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(b.tint).padding(10.dp)) {
                    Choices("طريقة التنبيه", Sound.entries.map { it to it.title }, a.sound) { v -> set { c -> c.copy(prayers = c.prayers + (p to a.copy(sound = v))) } }
                    Choices("تذكير قبل الصلاة", listOf(0 to "بدون", 5 to "5 د", 10 to "10 د", 15 to "15 د", 20 to "20 د", 30 to "30 د"), a.before) { v -> set { c -> c.copy(prayers = c.prayers + (p to a.copy(before = v))) } }
                    Choices("تذكير الإقامة بعد الأذان", listOf(0 to "بدون", 10 to "10 د", 15 to "15 د", 20 to "20 د", 25 to "25 د", 30 to "30 د"), a.iqama) { v -> set { c -> c.copy(prayers = c.prayers + (p to a.copy(iqama = v))) } }
                }
            }
            Text("يستخدم التنبيه صوت الإشعار في جهازك. لا يشغّل التطبيق أذانا كاملا لأن أندرويد لا يضمن تشغيل صوت طويل من الإشعار في كل الأجهزة.",
                style = MaterialTheme.typography.bodySmall, color = b.muted)
            TextButton(onClick = { nav.navigate("prayer-settings") }) { Text("طريقة حساب المواقيت والتعديل اليدوي") }
        }

        // ── Azkar
        Section("الأذكار") {
            SwitchRow("أذكار الصباح", "بعد الفجر بـ ${cfg.morningOffset} دقيقة", cfg.morning) { v -> set { it.copy(morning = v) } }
            if (cfg.morning) Choices("بعد الفجر بـ", listOf(15 to "15 د", 30 to "30 د", 45 to "45 د", 60 to "ساعة"), cfg.morningOffset) { v -> set { it.copy(morningOffset = v) } }
            SwitchRow("أذكار المساء", "بعد العصر بـ ${cfg.eveningOffset} دقيقة", cfg.evening) { v -> set { it.copy(evening = v) } }
            if (cfg.evening) Choices("بعد العصر بـ", listOf(15 to "15 د", 30 to "30 د", 45 to "45 د", 60 to "ساعة"), cfg.eveningOffset) { v -> set { it.copy(eveningOffset = v) } }
            SwitchRow("الأذكار بعد الصلاة", "بعد كل صلاة بـ ${cfg.afterPrayerDelay} دقيقة (وبعد الإقامة إن حددتها)", cfg.afterPrayer) { v -> set { it.copy(afterPrayer = v) } }
            if (cfg.afterPrayer) Choices("بعد الصلاة بـ", listOf(5 to "5 د", 10 to "10 د", 15 to "15 د", 20 to "20 د"), cfg.afterPrayerDelay) { v -> set { it.copy(afterPrayerDelay = v) } }
            SwitchRow("أذكار النوم", clock(cfg.sleepAt), cfg.sleep) { v -> set { it.copy(sleep = v) } }
            if (cfg.sleep) TimeButton("الوقت", cfg.sleepAt) { v -> set { it.copy(sleepAt = v) } }
            SwitchRow("أذكار الاستيقاظ", "قبل الفجر بعشرين دقيقة", cfg.wake) { v -> set { it.copy(wake = v) } }
            SwitchRow("الاستغفار والتسبيح", clock(cfg.istighfarAt), cfg.istighfar) { v -> set { it.copy(istighfar = v) } }
            if (cfg.istighfar) TimeButton("الوقت", cfg.istighfarAt) { v -> set { it.copy(istighfarAt = v) } }
        }

        // ── Quran
        Section("القرآن") {
            SwitchRow("آية اليوم", clock(cfg.ayahAt), cfg.ayah) { v -> set { it.copy(ayah = v) } }
            if (cfg.ayah) TimeButton("الوقت", cfg.ayahAt) { v -> set { it.copy(ayahAt = v) } }
            SwitchRow("ورد الختمة", "${clock(cfg.khatmaAt)} · عند وجود خطة ختمة ولم تُتم ورد اليوم", cfg.khatma) { v -> set { it.copy(khatma = v) } }
            if (cfg.khatma) TimeButton("الوقت", cfg.khatmaAt) { v -> set { it.copy(khatmaAt = v) } }
            SwitchRow("الحفظ والمراجعة", "${clock(cfg.hifzAt)} · عند وجود خطة حفظ", cfg.hifz) { v -> set { it.copy(hifz = v) } }
            if (cfg.hifz) TimeButton("الوقت", cfg.hifzAt) { v -> set { it.copy(hifzAt = v) } }
        }

        // ── Friday and fasting
        Section("الجمعة والصيام") {
            SwitchRow("سورة الكهف يوم الجمعة", clock(cfg.fridayAt), cfg.friday) { v -> set { it.copy(friday = v) } }
            if (cfg.friday) TimeButton("الوقت", cfg.fridayAt) { v -> set { it.copy(fridayAt = v) } }
            SwitchRow("صيام الاثنين والخميس", "ليلة الاثنين وليلة الخميس ${clock(cfg.fastingAt)}", cfg.fasting) { v -> set { it.copy(fasting = v) } }
            SwitchRow("الأيام البيض", "ليلة 13 و14 و15 من الشهر الهجري", cfg.whiteDays) { v -> set { it.copy(whiteDays = v) } }
            if (cfg.fasting || cfg.whiteDays) TimeButton("وقت التذكير", cfg.fastingAt) { v -> set { it.copy(fastingAt = v) } }
        }

        // ── Personal reminders
        Section("تذكيراتي") {
            cfg.personal.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.bodyLarge, color = b.ink)
                        Text("${clock(r.minute)} · " + if (r.days.size == 7) "كل يوم" else weekDays.filter { it.first in r.days }.joinToString("، ") { it.second },
                            style = MaterialTheme.typography.bodySmall, color = b.muted)
                    }
                    TextButton(onClick = { set { c -> c.copy(personal = c.personal.filter { it.id != r.id }) } }) { Text("حذف") }
                }
            }
            var title by remember { mutableStateOf("") }
            var minute by remember { mutableIntStateOf(21 * 60) }
            var days by remember { mutableStateOf((1..7).toSet()) }
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (title.isEmpty()) Text("عنوان التذكير، مثل: قيام الليل", color = b.muted)
                BasicTextField(title, { title = it.take(60) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink),
                    cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
            }
            TimeButton("الوقت", minute) { minute = it }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekDays.forEach { (d, n) -> Chip(n, d in days, { days = if (d in days) days - d else days + d }) }
            }
            BrandButton("أضف التذكير", Modifier.fillMaxWidth().padding(top = 6.dp), kind = BtnKind.Secondary, enabled = title.isNotBlank() && days.isNotEmpty()) {
                val t = title.trim(); val m = minute; val ds = days
                set { c -> c.copy(personal = c.personal + Personal(System.currentTimeMillis().toString(36), t, m, ds)) }
                title = ""
            }
        }

        // ── Quiet hours and snooze
        Section("الهدوء الليلي والتأجيل") {
            SwitchRow("الهدوء الليلي", "${clock(cfg.quietFrom)} – ${clock(cfg.quietTo)} · لا يمنع تنبيهات الصلاة", cfg.quiet) { v -> set { it.copy(quiet = v) } }
            if (cfg.quiet) {
                TimeButton("من", cfg.quietFrom) { v -> set { it.copy(quietFrom = v) } }
                TimeButton("إلى", cfg.quietTo) { v -> set { it.copy(quietTo = v) } }
            }
            Choices("زر «أجّل» في الإشعار", listOf(5 to "5 د", 10 to "10 د", 15 to "15 د", 30 to "30 د"), cfg.snooze) { v -> set { it.copy(snooze = v) } }
        }

        // ── Scheduled and history
        Section("المجدولة القادمة") {
            if (upcoming.isEmpty()) Text("فعّل تنبيها لتظهر مواعيده هنا.", color = b.muted)
            upcoming.take(15).forEach { a ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(a.title, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.weight(1f))
                    Text(a.at.format(DateTimeFormatter.ofPattern("EEE h:mm a", Locale("ar"))), style = MaterialTheme.typography.bodySmall, color = b.muted)
                }
            }
        }
        val history = remember(refresh) { NotifyStore.history(ctx) }
        Section("سجل التنبيهات") {
            if (history.isEmpty()) Text("لم يصل أي تنبيه بعد.", color = b.muted)
            history.take(20).forEach { h ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(h.title, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.weight(1f))
                    Text(java.time.Instant.ofEpochMilli(h.at).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d/M h:mm a", Locale("ar"))),
                        style = MaterialTheme.typography.bodySmall, color = b.muted)
                }
            }
        }
        Text("تُجدول التنبيهات على جهازك ولا تحتاج بقاء التطبيق مفتوحا، وتُعاد جدولتها بعد إعادة التشغيل وتغيير الوقت أو المنطقة الزمنية.",
            style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}

@Composable
fun PrayerSettingsScreen(nav: NavHostController) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    val cfg = NotifyStore.config
    val loc = remember(cfg, prefs.cityName, prefs.myLat) { prayerLocation(prefs, cfg) }
    val today = remember(loc) { loc.times(LocalDate.now()) }
    fun set(f: (NotifyConfig) -> NotifyConfig) = NotifyStore.update(ctx, f)

    ToolPage(nav, "حساب مواقيت الصلاة") {
        AppCard {
            Text("${loc.name} · اليوم", style = MaterialTheme.typography.titleMedium, color = b.primaryInk)
            Prayer.entries.forEach { p ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(p.title, color = if (p.isPrayer) b.ink else b.muted, modifier = Modifier.weight(1f))
                    Text(PrayerTimes.hm12(today.hours(p)), color = b.ink)
                }
            }
        }
        Section("طريقة الحساب") {
            Choices("الطريقة", listOf("auto" to "تلقائي (مثل الموقع)") + PrayerTimes.Method.choices.map { it.first to it.second }, cfg.method) { v -> set { it.copy(method = v) } }
            Text("«تلقائي»: أم القرى في مدن السعودية (العشاء بعد المغرب بـ 90 دقيقة، و120 في رمضان)، وطريقة كل بلد كما في موقع آيات تتلى.",
                style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
        Section("صلاة العصر") {
            Choices("المذهب", listOf(false to "الجمهور (ظل المثل)", true to "الحنفي (ظل المثلين)"), cfg.asrHanafi) { v -> set { it.copy(asrHanafi = v) } }
        }
        Section("التعديل اليدوي بالدقائق") {
            Prayer.entries.forEach { p ->
                val m = cfg.adjust[p] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.title, color = b.ink, modifier = Modifier.weight(1f))
                    TextButton(onClick = { set { c -> c.copy(adjust = (c.adjust + (p to (m - 1).coerceAtLeast(-30))).filterValues { it != 0 }) } }) { Text("−") }
                    Text(if (m > 0) "+$m" else "$m", color = b.primaryInk)
                    TextButton(onClick = { set { c -> c.copy(adjust = (c.adjust + (p to (m + 1).coerceAtMost(30))).filterValues { it != 0 }) } }) { Text("+") }
                }
            }
            BrandButton("إعادة الضبط الافتراضي", Modifier.fillMaxWidth(), kind = BtnKind.Ghost) { set { it.copy(method = "auto", asrHanafi = false, adjust = emptyMap()) } }
        }
        Text("الحساب فلكي على جهازك (معادلات موقع آيات تتلى نفسها). تتأثر المواقيت والتنبيهات والصفحة الرئيسية بهذه الإعدادات معا.",
            style = MaterialTheme.typography.bodySmall, color = b.muted)
    }
}
