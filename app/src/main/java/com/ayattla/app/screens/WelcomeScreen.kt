package com.ayattla.app.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.R
import com.ayattla.app.data.Stats
import com.ayattla.app.data.Welcome
import com.ayattla.app.data.Welcome.Step
import com.ayattla.app.notify.NotifyStore
import com.ayattla.app.notify.Scheduler
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.Radius
import com.ayattla.app.ui.components.drawIslamicPattern
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.ThemeMode

private val icons: Map<String, ImageVector> = mapOf(
    "u:quran" to Icons.AutoMirrored.Outlined.MenuBook, "u:listen" to Icons.Outlined.Headphones, "u:status" to Icons.Outlined.AutoAwesome,
    "u:clips" to Icons.Outlined.SmartDisplay, "u:azkar" to Icons.Outlined.Spa, "u:hadith" to Icons.Outlined.FormatQuote,
    "u:cards" to Icons.Outlined.Wallpaper, "u:gifts" to Icons.Outlined.CardGiftcard, "u:tafsir" to Icons.Outlined.Lightbulb,
    "u:hifz" to Icons.Outlined.RecordVoiceOver, "u:all" to Icons.Outlined.Apps,
    "l:short" to Icons.Outlined.FormatQuote, "l:tafsir" to Icons.AutoMirrored.Outlined.MenuBook, "l:audio" to Icons.Outlined.GraphicEq,
    "l:designs" to Icons.Outlined.Palette, "l:azkar" to Icons.Outlined.WbTwilight, "l:hadith" to Icons.Outlined.VolunteerActivism,
    "l:varied" to Icons.Outlined.Shuffle,
    "r:morning" to Icons.Outlined.WbSunny, "r:evening" to Icons.Outlined.NightsStay, "r:journey" to Icons.Outlined.Timeline,
    "r:khatma" to Icons.Outlined.Bookmark, "r:ayah" to Icons.Outlined.Today,
)

/** The questions, in order (the welcome and done screens are around them). */
private val questions = listOf(Step.Uses, Step.Likes, Step.Listen, Step.Remind, Step.Look)

/**
 * «تجربة البداية»: a short welcome, five optional questions and a done screen. Every question can be
 * skipped, «ادخل مباشرة» leaves at once, answers are saved as they are given (closing the app resumes
 * at the same step) and nothing is asked again after it ends. No permission is requested unless the
 * user turns on a reminder, and then only notifications. [edit]: opened from Settings to change answers.
 */
@Composable
fun WelcomeFlow(edit: Boolean = false, onFinish: () -> Unit) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val prefs = LocalPrefs.current
    var step by remember { mutableStateOf(if (edit) Step.Uses else Welcome.step(ctx)) }
    var forward by remember { mutableStateOf(true) }
    var uses by remember { mutableStateOf(Welcome.usesOf(ctx)) }
    var likes by remember { mutableStateOf(Welcome.likesOf(ctx)) }
    var reminders by remember { mutableStateOf(Welcome.remindersOf(ctx)) }
    var listen by remember { mutableStateOf(Welcome.listenOf(ctx)) }
    var notifyDenied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (!edit && step == Step.Hello) Stats.event(ctx, "onboarding_started") }

    fun go(to: Step, fwd: Boolean = true) {
        forward = fwd
        step = to
        if (!edit) Welcome.saveStep(ctx, to)
    }
    fun next(skipped: Boolean = false) {
        Stats.event(ctx, if (skipped) "onboarding_skipped" else "onboarding_step_completed", step.key)
        go(Step.entries[step.ordinal + 1])
    }
    fun back() { if (step.ordinal > (if (edit) Step.Uses.ordinal else 0)) go(Step.entries[step.ordinal - 1], fwd = false) }
    fun finish(leftEarly: Boolean) {
        Welcome.finish(ctx)
        Stats.event(ctx, if (leftEarly) "onboarding_skipped" else "onboarding_completed", if (leftEarly) step.key else "")
        onFinish()
    }

    /** Turns on the chosen reminders in the alerts centre (the same settings as «مركز التنبيهات»). */
    fun applyReminders(on: Boolean) {
        Welcome.saveReminders(ctx, reminders)
        reminders.forEach { Stats.event(ctx, "notification_preferences_saved", it) }
        if (!on || reminders.isEmpty()) return
        NotifyStore.update(ctx) { c ->
            c.copy(
                morning = c.morning || "morning" in reminders, evening = c.evening || "evening" in reminders,
                journey = c.journey || "journey" in reminders, khatma = c.khatma || "khatma" in reminders,
                ayah = c.ayah || "ayah" in reminders,
            )
        }
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifyDenied = !ok
        applyReminders(ok)
        next()
    }

    BackHandler(enabled = step != Step.Hello && step != Step.Done) { back() }

    Box(Modifier.fillMaxSize().background(b.background)) {
        // Calm geometric pattern, fading out towards the content.
        Canvas(Modifier.fillMaxSize().alpha(if (b.dark) 0.35f else 0.55f)) { drawIslamicPattern(b.primaryInk.copy(alpha = 0.10f)) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, b.background.copy(alpha = 0.92f), b.background), startY = 0f)))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                // RTL: going forward enters from the left, going back from the right.
                val dir = if (forward) -1 else 1
                (slideInHorizontally(tween(260)) { dir * it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { -dir * it / 5 } + fadeOut(tween(160)))
            },
            label = "welcome",
        ) { s ->
            when (s) {
                Step.Hello -> Hello(onStart = { next() }, onEnter = { finish(leftEarly = true) })
                Step.Done -> Done(uses, notifyDenied, onStart = { finish(leftEarly = false) })
                else -> Question(
                    s, edit,
                    onBack = { back() },
                    onSkip = { if (s == Step.Remind) { reminders = emptySet(); Welcome.saveReminders(ctx, reminders) }; next(skipped = true) },
                    onNext = {
                        when (s) {
                            Step.Uses -> { Welcome.saveUses(ctx, uses); uses.forEach { Stats.event(ctx, "interests_selected", it) }; next() }
                            Step.Likes -> { Welcome.saveLikes(ctx, likes); likes.forEach { Stats.event(ctx, "content_selected", it) }; next() }
                            Step.Listen -> { Welcome.saveListen(ctx, listen); Stats.event(ctx, "listen_choice", listen.substringBefore(':').ifBlank { "none" }); next() }
                            Step.Remind -> {
                                if (reminders.isNotEmpty() && Build.VERSION.SDK_INT >= 33 && !Scheduler.canNotify(ctx)) {
                                    askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else { applyReminders(reminders.isNotEmpty()); next() }
                            }
                            Step.Look -> { Stats.event(ctx, "theme_selected", prefs.themeMode.name); next() }
                            else -> next()
                        }
                    },
                ) {
                    when (s) {
                        Step.Uses -> ChoiceGrid(Welcome.uses, "u", uses) { uses = it }
                        Step.Likes -> ChoiceGrid(Welcome.likes, "l", likes) { likes = it }
                        Step.Listen -> ListenStep(listen) { listen = it }
                        Step.Remind -> RemindStep(reminders) { reminders = it }
                        Step.Look -> LookStep()
                        else -> {}
                    }
                }
            }
        }
    }
}

// ───────────── Welcome ─────────────

@Composable
private fun Hello(onStart: () -> Unit, onEnter: () -> Unit) {
    val b = LocalBrand.current
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(520)) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(if (b.dark) R.drawable.logo_full_light else R.drawable.logo_full), "آيات تتلى",
            Modifier.widthIn(max = 220.dp).fillMaxWidth(0.55f).scale(0.94f + 0.06f * rise.value).alpha(rise.value),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "رفيقك للقرآن والذكر وصناعة الخير", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.alpha(rise.value),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "مصحف وتلاوات وأذكار ومواقيت، وصانع حالات ومقاطع وبطاقات إسلامية، في تطبيق واحد يعمل حتى دون إنترنت.",
            style = MaterialTheme.typography.bodyLarge, color = b.muted, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp).alpha(rise.value),
        )
        Spacer(Modifier.weight(1.2f))
        Column(Modifier.widthIn(max = 460.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandButton("ابدأ رحلتك", Modifier.fillMaxWidth().heightIn(min = 54.dp)) { onStart() }
            Text(
                "ادخل مباشرة", style = MaterialTheme.typography.titleSmall, color = b.primaryInk, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onEnter).padding(vertical = 14.dp),
            )
            Text(
                "مجاني بالكامل · بلا إعلانات · بلا تسجيل", style = MaterialTheme.typography.labelMedium, color = b.muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
    }
}

// ───────────── A question ─────────────

private val titles = mapOf(
    Step.Uses to ("ما الذي تريد استخدام آيات تتلى من أجله؟" to "اختر ما يهمك، وسنقدمه لك في الرئيسية"),
    Step.Likes to ("ما نوع المحتوى الذي تفضله؟" to "يساعدنا في اقتراح ما يناسبك"),
    Step.Listen to ("كيف تفضل الاستماع إلى القرآن؟" to "يستخدم في المصحف المسموع وصانع المقاطع"),
    Step.Remind to ("ما التذكيرات التي ترغب فيها؟" to "تذكيرات لطيفة، وتستطيع تغييرها في أي وقت"),
    Step.Look to ("كيف تفضل عرض التطبيق؟" to "يتغير المظهر فورا لترى الفرق"),
)

@Composable
private fun Question(
    step: Step, edit: Boolean, onBack: () -> Unit, onSkip: () -> Unit, onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val b = LocalBrand.current
    val index = questions.indexOf(step)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Top bar: back, progress, skip.
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(enabled = index > 0 || !edit, onClick = onBack)
                    .semantics { contentDescription = "رجوع" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = if (index > 0 || !edit) b.ink else Color.Transparent) }
            Row(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                questions.forEachIndexed { i, _ ->
                    val c by animateColorAsState(if (i <= index) b.green else b.line, tween(260), label = "p$i")
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c))
                }
            }
            Text(
                "تخطي", style = MaterialTheme.typography.titleSmall, color = b.primaryInk,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onSkip).padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                val (t, sub) = titles.getValue(step)
                Text("${index + 1} من ${questions.size}", style = MaterialTheme.typography.labelLarge, color = b.goldInk, modifier = Modifier.padding(top = 8.dp))
                Text(t, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = b.ink, modifier = Modifier.padding(top = 6.dp))
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = b.muted, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                content()
                Spacer(Modifier.height(16.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            BrandButton(if (step == Step.Look) "إنهاء" else "التالي", Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(min = 54.dp)) { onNext() }
        }
    }
}

/** Multi-select cards: two columns on phones, three on tablets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGrid(choices: List<Welcome.Choice>, prefix: String, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = if (maxWidth >= 560.dp) 3 else 2
        val gap = 10.dp
        // Fixed rows with equal weights: rounding can never push a card onto its own row.
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            choices.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { c ->
                        val on = c.id in selected
                        OptionCard(c.label, icons["$prefix:${c.id}"], on, Modifier.weight(1f).fillMaxHeight().heightIn(min = 104.dp)) {
                            onChange(if (on) selected - c.id else selected + c.id)
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(label: String, icon: ImageVector?, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val b = LocalBrand.current
    val border by animateColorAsState(if (on) b.green else b.line, tween(180), label = "bd")
    val fill by animateColorAsState(if (on) b.tint else b.surface, tween(180), label = "fl")
    Box(
        modifier.clip(RoundedCornerShape(Radius.card)).background(fill).border(if (on) 2.dp else 1.dp, border, RoundedCornerShape(Radius.card))
            .clickable(role = Role.Checkbox, onClick = onClick).semantics { selected = on }.padding(14.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            if (icon != null) Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(if (on) b.green else b.tint), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (on) Color.White else b.primaryInk, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = b.ink, maxLines = 2)
        }
        if (on) Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(b.green), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** One wide card with a radio mark (single choice). */
@Composable
private fun RowOption(title: String, subtitle: String?, icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    val b = LocalBrand.current
    val border by animateColorAsState(if (on) b.green else b.line, tween(180), label = "rb")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(Radius.card)).background(if (on) b.tint else b.surface)
            .border(if (on) 2.dp else 1.dp, border, RoundedCornerShape(Radius.card)).clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = on }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(if (on) b.green else b.tint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (on) Color.White else b.primaryInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = b.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) b.green else b.lineStrong, CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(12.dp).clip(CircleShape).background(b.green))
        }
    }
}

// ───────────── Listening ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListenStep(value: String, onChange: (String) -> Unit) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    val reciters = LocalContent.current.reciters
    val picked = value.startsWith("reciter")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RowOption("قارئ مفضل", "اختر من القراء المتاحين في التطبيق", Icons.Outlined.Headphones, picked) {
            onChange("reciter:" + (prefs.reciterId.ifBlank { reciters.firstOrNull()?.id.orEmpty() }))
        }
        if (picked) {
            FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reciters.forEach { r ->
                    val on = value == "reciter:${r.id}"
                    Text(
                        r.name, style = MaterialTheme.typography.bodyMedium, color = if (on) Color.White else b.ink,
                        modifier = Modifier.clip(CircleShape).background(if (on) b.green else b.surface).border(1.dp, if (on) b.green else b.line, CircleShape)
                            .clickable { onChange("reciter:${r.id}"); prefs.setReciter(r.id) }.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
        RowOption("قراء متنوعون", "أتنقل بين القراء", Icons.Outlined.Shuffle, value == "various") { onChange("various") }
        RowOption("لا أفضل خيارا الآن", null, Icons.Outlined.Schedule, value == "later") { onChange("later") }
    }
}

// ───────────── Reminders ─────────────

@Composable
private fun RemindStep(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val b = LocalBrand.current
    val prefs = LocalPrefs.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Welcome.reminders.forEach { r ->
            val on = r.id in selected
            val sub = when (r.id) {
                "morning" -> "بعد الفجر بنصف ساعة"
                "evening" -> "بعد العصر بنصف ساعة"
                "journey" -> "التاسعة مساء، ولا يصلك إن أكملت هدفك"
                "khatma" -> "حين تنشئ خطة ختمتك"
                else -> "التاسعة صباحا"
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(Radius.card)).background(if (on) b.tint else b.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) b.green else b.line, RoundedCornerShape(Radius.card))
                    .clickable(role = Role.Checkbox) { onChange(if (on) selected - r.id else selected + r.id) }
                    .semantics { this.selected = on }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(if (on) b.green else b.tint), contentAlignment = Alignment.Center) {
                    Icon(icons["r:${r.id}"] ?: Icons.Outlined.Today, null, tint = if (on) Color.White else b.primaryInk, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(r.label, style = MaterialTheme.typography.titleSmall, color = b.ink)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = b.muted)
                }
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (on) b.green else Color.Transparent).border(2.dp, if (on) b.green else b.lineStrong, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            "أوقات الأذكار حسب مدينة ${prefs.cityName}، ولا نحتاج موقعك. إن اخترت تذكيرا نطلب إذن الإشعارات حين تتابع.",
            style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ───────────── Look ─────────────

@Composable
private fun LookStep() {
    val prefs = LocalPrefs.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RowOption("الوضع الفاتح", "الأخضر والأبيض، المظهر الأصلي", Icons.Outlined.LightMode, prefs.themeMode == ThemeMode.Light) { prefs.setTheme(ThemeMode.Light) }
        RowOption("الوضع الداكن", "مريح للقراءة ليلا", Icons.Outlined.DarkMode, prefs.themeMode == ThemeMode.Dark) { prefs.setTheme(ThemeMode.Dark) }
        RowOption("حسب إعدادات الجهاز", "يتبدل تلقائيا مع جوالك", Icons.Outlined.PhoneAndroid, prefs.themeMode == ThemeMode.System) { prefs.setTheme(ThemeMode.System) }
    }
}

// ───────────── Done ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Done(uses: Set<String>, notifyDenied: Boolean, onStart: () -> Unit) {
    val b = LocalBrand.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(380)) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        Box(Modifier.size(88.dp).scale(pop.value).clip(CircleShape).background(b.green), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("أهلا بك في آيات تتلى", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = b.ink, textAlign = TextAlign.Center)
        Text("جهزنا لك تجربة تناسب اهتماماتك", style = MaterialTheme.typography.bodyLarge, color = b.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        val picked = Welcome.uses.filter { it.id in uses }
        if (picked.isNotEmpty()) FlowRow(
            Modifier.widthIn(max = 520.dp).padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            picked.forEach { u ->
                Text(u.label, style = MaterialTheme.typography.labelLarge, color = b.primaryInk,
                    modifier = Modifier.clip(CircleShape).background(b.tint).padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
        if (notifyDenied) Text(
            "لم تفعل التذكيرات لأن إذن الإشعارات لم يمنح، ويمكنك تفعيلها لاحقا من مركز التنبيهات.",
            style = MaterialTheme.typography.bodySmall, color = b.goldInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 460.dp).padding(top = 16.dp),
        )
        Spacer(Modifier.height(40.dp))
        BrandButton("ابدأ الآن", Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = 54.dp)) { onStart() }
        Text(
            "نرسل إحصاءات استخدام مجهولة بلا أي بيانات شخصية لنحسن التطبيق، ويمكنك إيقافها من الإعدادات.",
            style = MaterialTheme.typography.labelMedium, color = b.muted, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 460.dp).padding(top = 12.dp, bottom = 16.dp),
        )
    }
}
