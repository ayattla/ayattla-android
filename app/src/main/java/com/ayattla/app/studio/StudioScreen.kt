package com.ayattla.app.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.ayattla.app.LocalBottomInset
import com.ayattla.app.LocalContent
import com.ayattla.app.LocalPrefs
import com.ayattla.app.data.ContentStore
import com.ayattla.app.data.DailyAyah
import com.ayattla.app.data.RemoteImages
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.BrandButton
import com.ayattla.app.ui.components.BtnKind
import com.ayattla.app.ui.components.CardShape
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.components.HeroHeader
import com.ayattla.app.ui.components.IconTile
import com.ayattla.app.ui.components.LargeTitle
import com.ayattla.app.ui.components.SiteSegmented
import com.ayattla.app.ui.components.TileStyle
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

const val MAX_AYAT = 10

/** Cards of a project: [Project.perCard] ayat per card, within each range (no basmala on cards). */
fun cardParts(content: ContentStore, p: Project): List<Part> = p.ranges.flatMap { r ->
    val s = content.surah(r.surah)
    content.ayat(r.surah, r.from, r.to).chunked(p.perCard.coerceIn(1, MAX_AYAT)).map { g ->
        Part(
            g.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) },
            "سورة ${s.shortName} · " + if (g.size == 1) "${g.first().ayah}" else "${g.first().ayah}-${g.last().ayah}",
            g.map { it.key },
        )
    }
}

/**
 * The studio (v2): a project of one or more runs of ayat, made into a clip or a set of cards. Every
 * layer is set on its own (text, reference, watermark, frame, veil), the text can move, each ayah can
 * hold longer, looks can be saved as templates, backgrounds come from the site's library or the
 * device, and exports come in 720p, 1080p or 4K. Projects are kept in the app's files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(nav: NavHostController, s0: Int, a0: Int, b0: Int, v0: Int = -1) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var project by remember {
        val valid = s0 in 1..114 && a0 >= 1 && a0 <= content.surah(s0).ayahCount
        val pending = PendingMedia.take()?.let { Background.Library(it) }
        val start = when {
            valid -> Project(ranges = listOf(AyahRange(s0, a0, b0.coerceIn(a0, content.surah(s0).ayahCount))), video = v0 == 1)
            else -> ProjectStore.current(ctx) ?: DailyAyah.pick().let { d -> Project(ranges = listOf(AyahRange(d.surah, d.from, d.to))) }
        }
        mutableStateOf(
            start.let { if (v0 != -1) it.copy(video = v0 == 1) else it }
                .let { if (pending != null) it.copy(background = pending) else it }
                .let { if (valid || pending != null) it.copy(id = UUID.randomUUID().toString(), updated = System.currentTimeMillis()) else it },
        )
    }
    fun update(f: (Project) -> Project) { project = f(project).copy(updated = System.currentTimeMillis()) }
    // A real background by default (as on the site), until the user picks one.
    LaunchedEffect(project.id) {
        if (project.background == Background.Color && !prefs.studioColorChosen) defaultMedia(ctx, project.video)?.let { m -> update { it.copy(background = Background.Library(m)) } }
    }
    LaunchedEffect(project) { delay(400); withContext(Dispatchers.IO) { ProjectStore.save(ctx, project) } }

    val reciter = content.reciters.firstOrNull { it.id == (project.reciterId ?: prefs.reciterId) } ?: content.reciters.first()
    val engine = remember { ClipEngine(ctx, content) }
    val parts = remember(project.ranges, project.video, project.perCard) {
        if (project.video) engine.parts(project).map { it.first } else cardParts(content, project)
    }
    var index by remember { mutableIntStateOf(0) }
    if (index > parts.lastIndex) index = parts.lastIndex.coerceAtLeast(0)

    // ── Preview: background, base and text as separate layers (the text can play its animation).
    val pw = 540
    val ph = (pw * project.ratio.h / project.ratio.w.toFloat()).toInt()
    var bgBmp by remember { mutableStateOf<Bitmap?>(null) }
    var onMedia by remember { mutableStateOf(false) }
    LaunchedEffect(project.background, project.ratio, project.style.colors) {
        val photo = previewPhoto(ctx, project.background)
        onMedia = project.background != Background.Color && photo != null
        bgBmp = withContext(Dispatchers.Default) { Composer(ctx, project.style, pw, ph, onMedia).backgroundBitmap(photo) }
    }
    var baseBmp by remember { mutableStateOf<Bitmap?>(null) }
    var textBmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(project.style, project.ratio, onMedia, parts, index) {
        val part = parts.getOrNull(index) ?: return@LaunchedEffect
        val c = Composer(ctx, project.style, pw, ph, onMedia)
        baseBmp = withContext(Dispatchers.Default) { c.baseLayer() }
        textBmp = withContext(Dispatchers.Default) { c.textLayer(part) }
    }
    val anim = remember { Animatable(1f) }

    var open by rememberSaveable { mutableStateOf("ayat") }
    var showProjects by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = LocalBottomInset.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LargeTitle("صناعة المحتوى", Modifier.weight(1f))
            BrandButton("مشاريعي", icon = Icons.Outlined.FolderOpen, kind = BtnKind.Secondary) { showProjects = true }
        }
        HeroHeader(
            if (project.video) "مقطع قرآني" else "بطاقات آيات", eyebrow = "الاستوديو",
            subtitle = if (project.video) "طبقات مستقلة للنص والمرجع والخلفية، حركة وتوقيت لكل آية، وتصدير حتى 4K"
            else "بطاقة أو مجموعة بطاقات دفعة واحدة، بخلفية حقيقية أو من جهازك",
        ) {
            SiteSegmented(listOf(false to "بطاقات", true to "مقطع فيديو"), project.video, { v ->
                update { it.copy(video = v, ratio = if (v && it.ratio == CardRatio.Portrait) CardRatio.Story else it.ratio) }
            })
        }
        // Project name.
        NameField(project.name) { n -> update { it.copy(name = n) } }

        // Preview.
        Box(
            Modifier.fillMaxWidth().aspectRatio(project.ratio.w.toFloat() / project.ratio.h).clip(CardShape).background(b.surface2),
            contentAlignment = Alignment.Center,
        ) {
            val bgB = bgBmp; val baseB = baseBmp; val textB = textBmp
            if (bgB == null || baseB == null || textB == null) CircularProgressIndicator(color = b.green)
            else {
                Image(bgB.asImageBitmap(), null, Modifier.fillMaxSize())
                Image(baseB.asImageBitmap(), null, Modifier.fillMaxSize())
                val p = anim.value
                val e = 1f - (1f - p) * (1f - p)
                Image(
                    textB.asImageBitmap(), "معاينة", Modifier.fillMaxSize().graphicsLayer {
                        when (project.style.anim) {
                            Anim.None, Anim.Words -> alpha = if (project.style.anim == Anim.Words) p.coerceAtLeast(0.15f) else 1f
                            Anim.Fade -> alpha = e
                            Anim.Slide -> { alpha = e; translationY = (1f - e) * size.height * 0.05f }
                            Anim.Zoom -> { alpha = e; scaleX = 0.9f + 0.1f * e; scaleY = 0.9f + 0.1f * e }
                        }
                    },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "السابق", index > 0) { index-- }
            Text(
                if (project.video) "الشاشة ${index + 1} من ${parts.size}" else "البطاقة ${index + 1} من ${parts.size}",
                style = MaterialTheme.typography.labelLarge, color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "التالي", index < parts.lastIndex) { index++ }
            if (project.video) BrandButton("الحركة", icon = Icons.Filled.PlayArrow, kind = BtnKind.Secondary) {
                scope.launch { anim.snapTo(0f); anim.animateTo(1f, tween(project.style.animMs.coerceAtLeast(300) * if (project.style.anim == Anim.Words) 3 else 1)) }
            }
        }

        // ── Sections.
        Section("الآيات", "${project.ayahCount} آية في ${project.ranges.size} مقطع", Icons.Outlined.AutoStories, open == "ayat", { open = if (open == "ayat") "" else "ayat" }) {
            AyatSection(project) { f -> update(f) }
        }
        Section("الخلفية", backgroundLabel(project.background), Icons.Outlined.Image, open == "bg", { open = if (open == "bg") "" else "bg" }) {
            BackgroundSection(project, onPickColor = { prefs.markStudioColorChosen() }) { f -> update(f) }
        }
        Section("النص", "الحجم والتباعد واللون والموضع", Icons.Outlined.TextFields, open == "text", { open = if (open == "text") "" else "text" }) {
            TextSection(project.style.text) { t -> update { it.copy(style = it.style.copy(text = t)) } }
        }
        Section("المرجع والطبقات", "المرجع والعلامة والإطار والألوان", Icons.Outlined.Layers, open == "layers", { open = if (open == "layers") "" else "layers" }) {
            LayersSection(project, onMedia) { s -> update { it.copy(style = s) } }
        }
        if (project.video) {
            Section("الحركة", project.style.anim.label, Icons.Outlined.Animation, open == "anim", { open = if (open == "anim") "" else "anim" }) {
                AnimSection(project.style) { s -> update { it.copy(style = s) } }
            }
            Section("التوقيت والقارئ", reciter.name, Icons.Outlined.Timer, open == "timing", { open = if (open == "timing") "" else "timing" }) {
                TimingSection(project, engine, reciter.name) { f -> update(f) }
            }
        }
        Section("القوالب", "احفظ تصميمك واستخدمه لاحقا", Icons.Outlined.Palette, open == "tpl", { open = if (open == "tpl") "" else "tpl" }) {
            TemplatesSection(project.style) { s -> update { it.copy(style = s) } }
        }
        Section("المقاس والجودة", "${project.ratio.label} · ${project.quality.label}", Icons.Outlined.Tune, open == "size", { open = if (open == "size") "" else "size" }) {
            SizeSection(project) { f -> update(f) }
        }

        if (project.video) ClipExport(project, engine, reciter) else CardsExport(project, parts, onMedia)
        Text(
            "نص الآيات من المصحف كما هو دون أي تعديل، يرسم بخط المصحف بدقة الجودة المختارة. التلاوة من EveryAyah والخلفيات من مكتبة الموقع (Pixabay وPexels) أو من جهازك.",
            style = MaterialTheme.typography.bodySmall, color = b.muted,
        )
    }

    if (showProjects) ModalBottomSheet(onDismissRequest = { showProjects = false }, containerColor = b.surface) {
        ProjectsSheet(
            current = project.id,
            onOpen = { p -> project = p; index = 0; showProjects = false },
            onNew = { project = Project(video = project.video, style = project.style); index = 0; showProjects = false },
        )
    }
}

private suspend fun previewPhoto(ctx: android.content.Context, bg: Background): Bitmap? = when (bg) {
    Background.Color -> null
    is Background.Library -> (if (bg.media.isVideo) bg.media.thumbUrl else bg.media.still)?.let { RemoteImages.load(ctx, it, 700) }
    is Background.Local -> withContext(Dispatchers.IO) {
        if (bg.isVideo) com.ayattla.app.data.videoFrame(bg.path, 1280)
        else com.ayattla.app.data.decodeScaled(bg.path, 1280)
    }
}

private fun backgroundLabel(bg: Background) = when (bg) {
    Background.Color -> "لون"
    is Background.Library -> if (bg.media.isVideo) "فيديو من المكتبة" else "صورة من المكتبة"
    is Background.Local -> if (bg.isVideo) "فيديو من جهازك" else "صورة من جهازك"
}

// ───────────── Small pieces ─────────────

@Composable
private fun Section(title: String, summary: String, icon: ImageVector, expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val b = LocalBrand.current
    AppCard(padding = 0.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon, style = if (expanded) TileStyle.Gradient(0) else TileStyle.Soft, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = b.ink)
                Text(summary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted, maxLines = 1)
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = b.muted)
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val b = LocalBrand.current
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(b.tint).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (enabled) b.primaryInk else b.lineStrong) }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    val b = LocalBrand.current
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.labelLarge, color = b.primaryInk)
        }
        Slider(
            value, onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = b.green, activeTrackColor = b.green, inactiveTrackColor = b.line),
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = b.green, checkedThumbColor = Color.White))
    }
}

private val Swatches: List<Pair<Long?, String>> = listOf(
    null to "تلقائي", 0xFFFFFFFF to "أبيض", 0xFFF6F2E9 to "عاجي", 0xFFD9C28E to "ذهبي", 0xFFC3A66B to "ذهبي داكن",
    0xFF2E9145 to "أخضر", 0xFF102D27 to "زمردي", 0xFF0F1F17 to "داكن",
)

@Composable
private fun ColorRow(selected: Long?, onPick: (Long?) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Swatches.forEach { (c, name) ->
            val on = c == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPick(c) }) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(if (c == null) b.surface2 else Color(c))
                        .border(if (on) 3.dp else 1.dp, if (on) b.green else b.lineStrong, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (c == null) Text("A", style = MaterialTheme.typography.labelLarge, color = b.primaryInk) }
                Text(name, style = MaterialTheme.typography.labelSmall, color = b.muted)
            }
        }
    }
}

@Composable
private fun NameField(name: String, onChange: (String) -> Unit) {
    val b = LocalBrand.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp)).background(b.surface).border(1.dp, b.line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("اسم المشروع", style = MaterialTheme.typography.labelMedium, color = b.muted)
        Spacer(Modifier.width(10.dp))
        BasicTextField(name, { onChange(it.take(40)) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.weight(1f))
    }
}

// ───────────── Sections ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyatSection(p: Project, update: ((Project) -> Project) -> Unit) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var adding by remember { mutableStateOf(false) }
    p.ranges.forEachIndexed { i, r ->
        val s = content.surah(r.surah)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(b.surface2).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("سورة ${s.shortName}", style = MaterialTheme.typography.titleMedium, color = b.primaryInk, modifier = Modifier.weight(1f))
                if (p.ranges.size > 1) Icon(Icons.Filled.Close, "حذف المقطع", tint = b.muted, modifier = Modifier.size(22.dp).clickable { update { it.copy(ranges = it.ranges.filterIndexed { j, _ -> j != i }) } })
            }
            fun set(nr: AyahRange) = update { pr -> pr.copy(ranges = pr.ranges.mapIndexed { j, x -> if (j == i) nr else x }) }
            StepLine("من آية", r.from, 1, s.ayahCount) { v -> set(r.copy(from = v, to = maxOf(v, r.to).coerceAtMost(v + 39))) }
            StepLine("إلى آية", r.to, r.from, minOf(s.ayahCount, r.from + 39)) { v -> set(r.copy(to = v)) }
        }
    }
    BrandButton("أضف آيات من سورة أخرى", Modifier.fillMaxWidth(), icon = Icons.Filled.Add, kind = BtnKind.Secondary) { adding = true }
    if (p.video && p.ayahCount > ClipEngine.MAX_AYAT) Text("حد المقطع ${ClipEngine.MAX_AYAT} آية؛ تُصدَّر أول ${ClipEngine.MAX_AYAT}.", style = MaterialTheme.typography.bodySmall, color = b.newInk)
    if (adding) ModalBottomSheet(onDismissRequest = { adding = false }, containerColor = b.surface) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(content.surahs, key = { it.id }) { x ->
                Text(
                    "${x.id}. سورة ${x.shortName}", style = MaterialTheme.typography.bodyLarge, color = b.ink,
                    modifier = Modifier.fillMaxWidth().clickable {
                        update { it.copy(ranges = it.ranges + AyahRange(x.id, 1, minOf(x.ayahCount, 3))) }; adding = false
                    }.padding(vertical = 12.dp),
                )
                HorizontalDivider(color = b.line)
            }
        }
    }
}

@Composable
private fun StepLine(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink, modifier = Modifier.weight(1f))
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "أقل", value > min) { onChange(value - 1) }
        Text("$value", style = MaterialTheme.typography.titleMedium, color = b.primaryInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 44.dp))
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "أكثر", value < max) { onChange(value + 1) }
    }
}

@Composable
private fun BackgroundSection(p: Project, onPickColor: () -> Unit, update: ((Project) -> Project) -> Unit) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val local = withContext(Dispatchers.IO) { importMedia(ctx, uri) }
            importing = false
            if (local != null) update { it.copy(background = local) } else Toast.makeText(ctx, "تعذر استيراد الملف", Toast.LENGTH_SHORT).show()
        }
    }
    val selected = (p.background as? Background.Library)?.media
    BackgroundPicker(selected, allowVideo = p.video) { m ->
        if (m == null) onPickColor()
        update { it.copy(background = if (m == null) Background.Color else Background.Library(m)) }
    }
    BrandButton(
        if (importing) "جار الاستيراد…" else if (p.video) "استورد صورة أو فيديو من جهازك" else "استورد صورة من جهازك",
        Modifier.fillMaxWidth(), icon = Icons.Outlined.AddPhotoAlternate, kind = BtnKind.Secondary, enabled = !importing,
    ) {
        picker.launch(PickVisualMediaRequest(if (p.video) ActivityResultContracts.PickVisualMedia.ImageAndVideo else ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    if (p.background is Background.Local) Text("الخلفية من جهازك، محفوظة داخل التطبيق مع المشروع.", style = MaterialTheme.typography.bodySmall, color = b.primaryInk)
    if (p.background != Background.Color) LabeledSlider("تعتيم الخلفية", p.style.veil, 0f..0.85f, "${(p.style.veil * 100).toInt()}٪") { v ->
        update { it.copy(style = it.style.copy(veil = v)) }
    }
}

/** Copies a picked photo or video into the app's files (the gallery copy may go away). */
private fun importMedia(ctx: android.content.Context, uri: Uri): Background.Local? = runCatching {
    val type = ctx.contentResolver.getType(uri).orEmpty()
    val video = type.startsWith("video")
    val ext = if (video) "mp4" else if (type.contains("png")) "png" else "jpg"
    val f = File(ProjectStore.mediaDir(ctx), "${UUID.randomUUID()}.$ext")
    ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { i.copyTo(it) } }
    // Keep the last 20 imports.
    ProjectStore.mediaDir(ctx).listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(20).forEach { it.delete() }
    Background.Local(f.path, video)
}.getOrNull()

@Composable
private fun TextSection(t: TextLayer, set: (TextLayer) -> Unit) {
    val b = LocalBrand.current
    LabeledSlider("حجم الخط", t.size, 0.6f..1.6f, "${(t.size * 100).toInt()}٪") { set(t.copy(size = it)) }
    LabeledSlider("تباعد الأسطر", t.lineSpacing, 0.1f..1.2f, String.format(java.util.Locale.US, "%.2f", t.lineSpacing)) { set(t.copy(lineSpacing = it)) }
    if (Build.VERSION.SDK_INT >= 29) LabeledSlider("تباعد الكلمات", t.wordSpacing, 0f..0.6f, String.format(java.util.Locale.US, "%.2f", t.wordSpacing)) { set(t.copy(wordSpacing = it)) }
    LabeledSlider("الموضع (أعلى ← أسفل)", t.posY, -1f..1f, "${(t.posY * 100).toInt()}") { set(t.copy(posY = it)) }
    Text("المحاذاة", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    SiteSegmented(Align.entries.map { it to it.label }, t.align, { set(t.copy(align = it)) })
    Text("لون النص", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    ColorRow(t.color) { set(t.copy(color = it)) }
    SwitchRow("ظل خلف النص", t.shadow) { set(t.copy(shadow = it)) }
    Text("الخط: أميري قرآن (خط المصحف)، ولا يتغير شيء من حروف الآيات.", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted)
}

@Composable
private fun LayersSection(p: Project, onMedia: Boolean, set: (Style) -> Unit) {
    val b = LocalBrand.current
    val s = p.style
    SwitchRow("إظهار المرجع (السورة والآية)", s.ref.show) { set(s.copy(ref = s.ref.copy(show = it))) }
    if (s.ref.show) {
        LabeledSlider("حجم المرجع", s.ref.size, 0.6f..1.6f, "${(s.ref.size * 100).toInt()}٪") { set(s.copy(ref = s.ref.copy(size = it))) }
        LabeledSlider("موضع المرجع", s.ref.posY, -1f..1f, "${(s.ref.posY * 100).toInt()}") { set(s.copy(ref = s.ref.copy(posY = it))) }
        Text("لون المرجع", style = MaterialTheme.typography.bodyMedium, color = b.ink)
        ColorRow(s.ref.color) { set(s.copy(ref = s.ref.copy(color = it))) }
    }
    SwitchRow("العلامة المائية «آيات تتلى»", s.watermark) { set(s.copy(watermark = it)) }
    SwitchRow("الإطار الذهبي", s.frame) { set(s.copy(frame = it)) }
    if (!onMedia) {
        Text("ألوان الخلفية", style = MaterialTheme.typography.bodyMedium, color = b.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CardStyle.entries.forEach { st -> Chip(st.label, st == s.colors, { set(s.copy(colors = st)) }) } }
    }
}

@Composable
private fun AnimSection(s: Style, set: (Style) -> Unit) {
    val b = LocalBrand.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Anim.entries.forEach { a -> Chip(a.label, a == s.anim, { set(s.copy(anim = a)) }) }
    }
    if (s.anim != Anim.None && s.anim != Anim.Words) LabeledSlider("مدة الظهور والاختفاء", s.animMs.toFloat(), 200f..1500f, "${s.animMs} م.ث") { set(s.copy(animMs = it.toInt())) }
    if (s.anim == Anim.Words) LabeledSlider("سرعة ظهور الكلمات (نسبة من مدة الآية)", s.wordPace, 0.3f..1f, "${(s.wordPace * 100).toInt()}٪") { set(s.copy(wordPace = it)) }
    Text("تظهر كل آية مع بداية تلاوتها وتختفي قبل الآية التالية.", style = MaterialTheme.typography.bodySmall, color = b.muted)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimingSection(p: Project, engine: ClipEngine, reciterName: String, update: ((Project) -> Project) -> Unit) {
    val b = LocalBrand.current
    val content = LocalContent.current
    var picking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(b.surface2).clickable { picking = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("القارئ", style = MaterialTheme.typography.labelMedium, color = b.muted)
            Text(reciterName, style = MaterialTheme.typography.bodyLarge, color = b.primaryInk)
        }
        Text("تغيير", style = MaterialTheme.typography.labelLarge, color = b.goldInk)
    }
    Text("وقفة بعد كل آية (تُضاف بعد انتهاء التلاوة):", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    val parts = remember(p.ranges) { engine.parts(p).map { it.first } }
    parts.take(ClipEngine.MAX_AYAT + p.ranges.size).forEach { part ->
        val key = part.keys.first()
        val ms = p.pauses[key] ?: 0
        LabeledSlider(part.reference, ms.toFloat(), 0f..3000f, if (ms == 0) "بدون" else String.format(java.util.Locale.US, "%.1f ث", ms / 1000f)) { v ->
            val r = (v / 100).toInt() * 100
            update { it.copy(pauses = if (r == 0) it.pauses - key else it.pauses + (key to r)) }
        }
    }
    if (picking) ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = b.surface) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(content.reciters, key = { it.id }) { r ->
                Text(r.name, style = MaterialTheme.typography.bodyLarge, color = if (r.id == p.reciterId) b.primaryInk else b.ink,
                    modifier = Modifier.fillMaxWidth().clickable { update { it.copy(reciterId = r.id) }; picking = false }.padding(vertical = 12.dp))
                HorizontalDivider(color = b.line)
            }
        }
    }
}

@Composable
private fun TemplatesSection(current: Style, apply: (Style) -> Unit) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    var mine by remember { mutableStateOf(TemplateStore.mine(ctx)) }
    var name by remember { mutableStateOf("") }
    Text("قوالب جاهزة", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TemplateStore.builtIn.forEach { t -> Chip(t.name, t.style == current, { apply(t.style) }) }
    }
    Text("قوالبي", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    if (mine.isEmpty()) Text("لا قوالب بعد. صمم ثم احفظ التصميم باسم.", style = MaterialTheme.typography.bodySmall, color = b.muted)
    mine.forEach { t ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (t.style == current) b.tint else b.surface2).clickable { apply(t.style) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.name, style = MaterialTheme.typography.bodyLarge, color = b.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Close, "حذف القالب", tint = b.muted, modifier = Modifier.size(20.dp).clickable { TemplateStore.remove(ctx, t.name); mine = TemplateStore.mine(ctx) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(b.surface2).border(1.dp, b.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (name.isEmpty()) Text("اسم القالب", color = b.muted)
            BasicTextField(name, { name = it.take(30) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = b.ink), cursorBrush = SolidColor(b.green), modifier = Modifier.fillMaxWidth())
        }
        BrandButton("احفظ", icon = Icons.Outlined.BookmarkAdd, enabled = name.isNotBlank()) {
            TemplateStore.add(ctx, Template(name.trim(), current)); mine = TemplateStore.mine(ctx); name = ""
            Toast.makeText(ctx, "حُفظ القالب", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun SizeSection(p: Project, update: ((Project) -> Project) -> Unit) {
    val b = LocalBrand.current
    Text("المقاس", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CardRatio.entries.filter { it != CardRatio.Wide && (!p.video || it != CardRatio.Portrait) }.forEach { r -> Chip(r.label, r == p.ratio, { update { it.copy(ratio = r) } }) }
    }
    Text("الجودة", style = MaterialTheme.typography.bodyMedium, color = b.ink)
    SiteSegmented(Quality.entries.map { it to it.label }, p.quality, { q -> update { it.copy(quality = q) } })
    val (w, h) = Composer.size(p.ratio, p.quality)
    Text("$w×$h بكسل. ${if (p.quality == Quality.Q2160) "4K أبطأ في التجهيز وأكبر حجما، ويعود إلى 1080p إن لم يدعمه جهازك." else ""}", style = MaterialTheme.typography.bodySmall, color = b.muted)
    if (!p.video) {
        StepLine("آيات في كل بطاقة", p.perCard, 1, MAX_AYAT) { v -> update { it.copy(perCard = v) } }
    }
}

// ───────────── Export ─────────────

@Composable
private fun ClipExport(p: Project, engine: ClipEngine, reciter: com.ayattla.app.data.Reciter) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<ClipEngine.Stage?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var result by remember(p.copy(updated = 0, name = "")) { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AppCard {
        val s = stage
        if (s != null) {
            val (label, prog) = when (s) {
                is ClipEngine.Stage.Downloading -> "تنزيل التلاوة ${s.done} من ${s.total}" to (s.done.toFloat() / s.total.coerceAtLeast(1)) * 0.3f
                ClipEngine.Stage.Background -> "تجهيز الخلفية" to 0.3f
                ClipEngine.Stage.Drawing -> "رسم الطبقات" to 0.35f
                is ClipEngine.Stage.Encoding -> "تصدير المقطع ${s.percent}٪" to 0.35f + s.percent / 100f * 0.65f
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink)
            LinearProgressIndicator(progress = { prog }, color = b.green, trackColor = b.line, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            BrandButton("إلغاء", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) { job?.cancel(); stage = null }
        } else {
            BrandButton("اصنع المقطع (${p.quality.label})", Modifier.fillMaxWidth(), icon = Icons.Filled.Movie) {
                error = null; result = null
                job = scope.launch {
                    runCatching { engine.make(p, reciter) { stage = it } }
                        .onSuccess { result = it }
                        .onFailure { e -> if (e !is kotlinx.coroutines.CancellationException) error = "تعذر تجهيز المقطع. تحقق من الإنترنت ثم أعد المحاولة." }
                    stage = null
                }
            }
        }
        error?.let { Text(it, color = b.newInk, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        result?.let { f ->
            Text("المقطع جاهز (${f.length() / 1_000_000} ميجا)", style = MaterialTheme.typography.titleSmall, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandButton("مشاركة المقطع", Modifier.fillMaxWidth(), icon = Icons.Filled.Share) {
                    val send = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, ClipEngine.shareUri(ctx, f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(send, "مشاركة المقطع"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("حفظ", Modifier.weight(1f), icon = Icons.Filled.Download, kind = BtnKind.Secondary) {
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) { ClipEngine.saveToGallery(ctx, f) }
                            Toast.makeText(ctx, if (uri != null) "حُفظ المقطع في الفيديوهات" else "تعذر الحفظ", Toast.LENGTH_SHORT).show()
                        }
                    }
                    BrandButton("تشغيل", Modifier.weight(1f), icon = Icons.Filled.PlayCircle, kind = BtnKind.Secondary) {
                        val view = Intent(Intent.ACTION_VIEW).setDataAndType(ClipEngine.shareUri(ctx, f), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { ctx.startActivity(view) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsExport(p: Project, parts: List<Part>, onMedia: Boolean) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var made by remember(p.copy(updated = 0, name = "")) { mutableStateOf<List<File>>(emptyList()) }
    val q = if (p.quality == Quality.Q720) Quality.Q1080 else p.quality
    val save: () -> Unit = {
        scope.launch {
            var n = 0
            withContext(Dispatchers.IO) { made.forEach { f -> android.graphics.BitmapFactory.decodeFile(f.path)?.let { bmp -> if (CardRenderer.saveToGallery(ctx, bmp, f.nameWithoutExtension) != null) n++; bmp.recycle() } } }
            Toast.makeText(ctx, "حُفظت $n بطاقة في الصور", Toast.LENGTH_SHORT).show()
        }
    }
    val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) save() }
    AppCard {
        val label = busy
        if (label != null) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = b.ink)
            LinearProgressIndicator(color = b.green, trackColor = b.line, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        } else BrandButton(if (parts.size == 1) "اصنع البطاقة" else "اصنع ${parts.size} بطاقات دفعة واحدة", Modifier.fillMaxWidth(), icon = Icons.Outlined.Collections) {
            scope.launch {
                val (w, h) = Composer.size(p.ratio, q)
                val photo = previewPhoto(ctx, p.background)?.let { small ->
                    // Full-size photo for the cards themselves.
                    when (val bg = p.background) {
                        is Background.Library -> if (!bg.media.isVideo) bg.media.still?.let { RemoteImages.load(ctx, it, (w * 1.2f).toInt()) } ?: small else small
                        is Background.Local -> if (!bg.isVideo) com.ayattla.app.data.decodeScaled(bg.path, (maxOf(w, h) * 1.2f).toInt()) ?: small else small
                        Background.Color -> small
                    }
                }
                val media = photo != null && p.background != Background.Color
                val dir = File(ProjectStore.exportsDir(ctx), "cards-${p.id}").apply { deleteRecursively(); mkdirs() }
                val out = mutableListOf<File>()
                withContext(Dispatchers.Default) {
                    val c = Composer(ctx, p.style, w, h, media)
                    val bg = c.backgroundBitmap(photo)
                    parts.forEachIndexed { i, part ->
                        busy = "رسم البطاقة ${i + 1} من ${parts.size}"
                        val bmp = c.still(photo, part, bg)
                        val f = File(dir, "ayattla-card-${i + 1}.png")
                        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; bmp.recycle()
                        out += f
                    }
                }
                made = out; busy = null
            }
        }
        if (made.isNotEmpty()) {
            Text("جاهزة: ${made.size} بطاقة (${Composer.size(p.ratio, q).let { "${it.first}×${it.second}" }})", style = MaterialTheme.typography.titleSmall, color = b.primaryInk, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                made.take(12).forEach { f ->
                    val thumb = remember(f) { com.ayattla.app.data.decodeScaled(f.path, 360) }
                    thumb?.let { Image(it.asImageBitmap(), null, Modifier.height(120.dp).clip(RoundedCornerShape(10.dp))) }
                }
            }
            BrandButton(if (made.size == 1) "مشاركة" else "مشاركة الكل", Modifier.fillMaxWidth(), icon = Icons.Filled.Share) {
                val uris = ArrayList(made.map { ClipEngine.shareUri(ctx, it) })
                val send = if (uris.size == 1) Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uris[0])
                else Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/png").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                ctx.startActivity(Intent.createChooser(send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة البطاقات"))
            }
            Spacer(Modifier.height(8.dp))
            BrandButton(if (made.size == 1) "حفظ في الصور" else "حفظ الكل في الصور", Modifier.fillMaxWidth(), icon = Icons.Filled.Download, kind = BtnKind.Secondary) {
                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else save()
            }
        }
    }
}

@Composable
private fun ProjectsSheet(current: String, onOpen: (Project) -> Unit, onNew: () -> Unit) {
    val b = LocalBrand.current
    val ctx = LocalContext.current
    val content = LocalContent.current
    var list by remember { mutableStateOf(ProjectStore.all(ctx)) }
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("مشاريعي", style = MaterialTheme.typography.headlineSmall, color = b.ink)
        Text("تحفظ تلقائيا على جهازك، وتبقى بعد إغلاق التطبيق وتحديثه.", style = MaterialTheme.typography.bodySmall, color = b.muted)
        BrandButton("مشروع جديد", Modifier.fillMaxWidth(), icon = Icons.Filled.Add) { onNew() }
        list.take(30).forEach { p ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (p.id == current) b.tint else b.surface2).clickable { onOpen(p) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(if (p.video) Icons.Filled.Movie else Icons.Outlined.Collections, style = TileStyle.Gradient(if (p.video) 0 else 2), size = 38.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = b.ink, maxLines = 1)
                    Text(
                        p.ranges.joinToString("، ") { "${content.surah(it.surah).shortName} ${it.from}-${it.to}" } + " · " +
                            java.text.SimpleDateFormat("d MMM", java.util.Locale.forLanguageTag("ar")).format(java.util.Date(p.updated)),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = b.muted, maxLines = 1,
                    )
                }
                if (p.id != current) Icon(Icons.Filled.Close, "حذف المشروع", tint = b.muted, modifier = Modifier.size(22.dp).clickable { ProjectStore.delete(ctx, p.id); list = ProjectStore.all(ctx) })
            }
        }
    }
}
