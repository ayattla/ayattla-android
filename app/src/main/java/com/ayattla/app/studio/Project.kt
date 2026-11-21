package com.ayattla.app.studio

import android.content.Context
import com.ayattla.app.data.SiteMedia
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One run of ayat inside a project (a project can hold several, from different surahs). */
data class AyahRange(val surah: Int, val from: Int, val to: Int)

enum class Anim(val label: String) { None("بدون"), Fade("تلاشي"), Slide("انزلاق"), Zoom("تكبير"), Words("كلمة كلمة") }

/** Export size: the long side scales from the 1080-wide design. */
enum class Quality(val label: String, val scale: Float, val bitrate: Int) {
    Q720("720p", 720f / 1080f, 3_000_000),
    Q1080("1080p", 1f, 6_000_000),
    Q2160("4K", 2f, 20_000_000),
}

enum class Align(val label: String) { Center("وسط"), Start("يمين"), End("يسار") }

/** Text layer: everything about how the Quran text is set (the letters themselves never change). */
data class TextLayer(
    val size: Float = 1f,          // scale of the auto-fitted size
    val lineSpacing: Float = 0.55f, // extra space between lines, in em
    val wordSpacing: Float = 0f,    // extra space between words, in em (Android 10+)
    val color: Long? = null,        // null: the style's own colour
    val shadow: Boolean = true,
    val posY: Float = 0f,           // -1 top … 1 bottom
    val align: Align = Align.Center,
)

/** Reference layer («سورة … · آية»). */
data class RefLayer(val show: Boolean = true, val size: Float = 1f, val color: Long? = null, val posY: Float = 0f)

data class Style(
    val colors: CardStyle = CardStyle.Emerald,
    val text: TextLayer = TextLayer(),
    val ref: RefLayer = RefLayer(),
    val watermark: Boolean = true,
    val frame: Boolean = true,
    val veil: Float = 0.45f,        // darkness over photos and videos
    val anim: Anim = Anim.Fade,
    val animMs: Int = 600,
    val wordPace: Float = 0.8f,     // share of each ayah over which its words appear (Words)
    val ornament: Ornament = Ornament.Classic, // drawn only when [frame] is on
)

sealed interface Background {
    data object Color : Background
    data class Library(val media: SiteMedia) : Background
    /** A photo or video imported from the device, copied into the app's own storage. */
    data class Local(val path: String, val isVideo: Boolean) : Background
}

val Background.isVideo get() = when (this) { is Background.Library -> media.isVideo; is Background.Local -> isVideo; Background.Color -> false }

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "مشروع جديد",
    val video: Boolean = true,
    val ratio: CardRatio = CardRatio.Story,
    val quality: Quality = Quality.Q1080,
    val ranges: List<AyahRange> = listOf(AyahRange(1, 1, 7)),
    val style: Style = Style(),
    val background: Background = Background.Color,
    val reciterId: String? = null,
    /** Extra pause after an ayah ("s:a" → ms), on top of its recitation. */
    val pauses: Map<String, Int> = emptyMap(),
    /** Cards: how many ayat on one card when making a set. */
    val perCard: Int = 1,
    val updated: Long = System.currentTimeMillis(),
    /** Light each word as it is recited (reciters with Quran.com timings, one surah per clip). */
    val highlight: Boolean = true,
    /** Colour of the recited word (ARGB); null: the brand gold. */
    val highlightColor: Long? = null,
) {
    val ayahCount get() = ranges.sumOf { it.to - it.from + 1 }
}

// ───────────── JSON ─────────────

private fun JSONObject.optLongOrNull(k: String) = if (has(k) && !isNull(k)) getLong(k) else null

fun TextLayer.json(): JSONObject = JSONObject().put("size", size.toDouble()).put("line", lineSpacing.toDouble()).put("word", wordSpacing.toDouble())
    .put("color", color ?: JSONObject.NULL).put("shadow", shadow).put("y", posY.toDouble()).put("align", align.name)
fun parseText(o: JSONObject?) = o?.let {
    TextLayer(it.optDouble("size", 1.0).toFloat(), it.optDouble("line", 0.55).toFloat(), it.optDouble("word", 0.0).toFloat(), it.optLongOrNull("color"),
        it.optBoolean("shadow", true), it.optDouble("y", 0.0).toFloat(), runCatching { Align.valueOf(it.optString("align")) }.getOrDefault(Align.Center))
} ?: TextLayer()

fun Style.json(): JSONObject = JSONObject().put("colors", colors.name).put("text", text.json())
    .put("ref", JSONObject().put("show", ref.show).put("size", ref.size.toDouble()).put("color", ref.color ?: JSONObject.NULL).put("y", ref.posY.toDouble()))
    .put("watermark", watermark).put("frame", frame).put("veil", veil.toDouble()).put("anim", anim.name).put("animMs", animMs).put("wordPace", wordPace.toDouble()).put("ornament", ornament.name)

fun parseStyle(o: JSONObject?): Style = o?.let {
    val r = it.optJSONObject("ref")
    Style(
        runCatching { CardStyle.valueOf(it.optString("colors")) }.getOrDefault(CardStyle.Emerald), parseText(it.optJSONObject("text")),
        if (r == null) RefLayer() else RefLayer(r.optBoolean("show", true), r.optDouble("size", 1.0).toFloat(), r.optLongOrNull("color"), r.optDouble("y", 0.0).toFloat()),
        it.optBoolean("watermark", true), it.optBoolean("frame", true), it.optDouble("veil", 0.45).toFloat(),
        runCatching { Anim.valueOf(it.optString("anim")) }.getOrDefault(Anim.Fade), it.optInt("animMs", 600), it.optDouble("wordPace", 0.8).toFloat(),
        runCatching { Ornament.valueOf(it.optString("ornament")) }.getOrDefault(Ornament.Classic),
    )
} ?: Style()

private fun SiteMedia.json() = JSONObject().put("provider", provider).put("mediaId", mediaId).put("type", type).put("thumbUrl", thumbUrl ?: "")
    .put("previewUrl", previewUrl ?: "").put("width", width ?: 0).put("height", height ?: 0).put("curated", curated)
private fun siteMedia(o: JSONObject) = SiteMedia(o.optString("provider"), o.optString("mediaId"), o.optString("type"), o.optString("thumbUrl").ifBlank { null },
    o.optString("previewUrl").ifBlank { null }, o.optInt("width").takeIf { it > 0 }, o.optInt("height").takeIf { it > 0 }, o.optBoolean("curated"))

fun Background.json(): JSONObject = when (this) {
    Background.Color -> JSONObject().put("kind", "color")
    is Background.Library -> JSONObject().put("kind", "library").put("media", media.json())
    is Background.Local -> JSONObject().put("kind", "local").put("path", path).put("video", isVideo)
}
fun parseBackground(o: JSONObject?): Background = when (o?.optString("kind")) {
    "library" -> Background.Library(siteMedia(o.getJSONObject("media")))
    "local" -> Background.Local(o.getString("path"), o.optBoolean("video")).takeIf { File(it.path).exists() } ?: Background.Color
    else -> Background.Color
}

fun Project.json(): JSONObject = JSONObject().put("v", 1).put("id", id).put("name", name).put("video", video).put("ratio", ratio.name).put("quality", quality.name)
    .put("ranges", JSONArray(ranges.map { JSONObject().put("s", it.surah).put("a", it.from).put("b", it.to) }))
    .put("style", style.json()).put("bg", background.json()).put("reciter", reciterId ?: JSONObject.NULL)
    .put("pauses", JSONObject(pauses as Map<*, *>)).put("perCard", perCard).put("updated", updated)
    .put("hl", highlight).put("hlColor", highlightColor ?: JSONObject.NULL)

fun parseProject(o: JSONObject): Project {
    val rs = o.optJSONArray("ranges") ?: JSONArray()
    val p = o.optJSONObject("pauses")
    return Project(
        o.getString("id"), o.optString("name", "مشروع"), o.optBoolean("video", true),
        runCatching { CardRatio.valueOf(o.optString("ratio")) }.getOrDefault(CardRatio.Story),
        runCatching { Quality.valueOf(o.optString("quality")) }.getOrDefault(Quality.Q1080),
        List(rs.length()) { rs.getJSONObject(it).let { r -> AyahRange(r.getInt("s"), r.getInt("a"), r.getInt("b")) } }.ifEmpty { listOf(AyahRange(1, 1, 7)) },
        parseStyle(o.optJSONObject("style")), parseBackground(o.optJSONObject("bg")),
        if (o.isNull("reciter")) null else o.optString("reciter"),
        p?.keys()?.asSequence()?.associateWith { p.getInt(it) } ?: emptyMap(),
        o.optInt("perCard", 1), o.optLong("updated"),
        o.optBoolean("hl", true), o.optLongOrNull("hlColor"),
    )
}

// ───────────── Storage (the app's files, not the cache: kept across closing and updating) ─────────────

object ProjectStore {
    private fun dir(c: Context) = File(c.filesDir, "projects").apply { mkdirs() }
    fun all(c: Context): List<Project> = dir(c).listFiles { f -> f.extension == "json" }.orEmpty()
        .mapNotNull { runCatching { parseProject(JSONObject(it.readText())) }.getOrNull() }.sortedByDescending { it.updated }
    fun save(c: Context, p: Project) {
        val f = File(dir(c), "${p.id}.json")
        val tmp = File(f.path + ".tmp"); tmp.writeText(p.json().toString()); tmp.renameTo(f)
        c.getSharedPreferences("studio", Context.MODE_PRIVATE).edit().putString("current", p.id).apply()
    }
    fun load(c: Context, id: String): Project? = runCatching { parseProject(JSONObject(File(dir(c), "$id.json").readText())) }.getOrNull()
    fun current(c: Context): Project? = c.getSharedPreferences("studio", Context.MODE_PRIVATE).getString("current", null)?.let { load(c, it) }
    fun delete(c: Context, id: String) { File(dir(c), "$id.json").delete() }

    /** Imported photos and videos live here (files/media), so a project keeps its background. */
    fun mediaDir(c: Context) = File(c.filesDir, "media").apply { mkdirs() }
    fun exportsDir(c: Context) = File(c.filesDir, "exports").apply { mkdirs() }
}

/** Saved looks: the built-in ones and the user's own (files/templates.json). */
data class Template(val name: String, val style: Style, val builtIn: Boolean = false)

object TemplateStore {
    val builtIn = listOf(
        Template("زمردي كلاسيكي", Style(), true),
        Template("ليلي ذهبي", Style(colors = CardStyle.Night, ref = RefLayer(color = 0xFFD9C28E), anim = Anim.Fade), true),
        Template("عاجي هادئ", Style(colors = CardStyle.Ivory, frame = true, anim = Anim.Slide), true),
        Template("سينمائي", Style(text = TextLayer(size = 1.1f, posY = 0.15f), frame = false, veil = 0.55f, anim = Anim.Zoom, animMs = 900), true),
        Template("كلمة كلمة", Style(anim = Anim.Words, wordPace = 0.75f, frame = false), true),
        Template("محراب زمردي", Style(ornament = Ornament.Arch, anim = Anim.Fade, animMs = 800), true),
        Template("ملكي مزخرف", Style(colors = CardStyle.Royal, ornament = Ornament.Corners, anim = Anim.Slide), true),
        Template("رمضاني بالقناديل", Style(colors = CardStyle.Violet, ornament = Ornament.Lantern, text = TextLayer(posY = 0.08f), anim = Anim.Fade, animMs = 900), true),
        Template("فيروزي للعيد", Style(colors = CardStyle.Teal, ornament = Ornament.Bands, anim = Anim.Zoom), true),
        Template("عنابي فاخر", Style(colors = CardStyle.Burgundy, ornament = Ornament.Medallion, anim = Anim.Fade), true),
        Template("رملي هادئ", Style(colors = CardStyle.Sand, ornament = Ornament.Arch, anim = Anim.Slide), true),
        Template("وردي ناعم", Style(colors = CardStyle.Blush, ornament = Ornament.Corners, anim = Anim.Fade), true),
        Template("سماوي صاف", Style(colors = CardStyle.Sky, ornament = Ornament.Medallion, anim = Anim.Words, wordPace = 0.7f), true),
        Template("زيتوني", Style(colors = CardStyle.Olive, ornament = Ornament.Bands, anim = Anim.Slide), true),
        Template("فحمي ذهبي", Style(colors = CardStyle.Charcoal, ornament = Ornament.Classic, ref = RefLayer(color = 0xFFCBAE6E), anim = Anim.Zoom, animMs = 800), true),
    )
    private fun file(c: Context) = File(c.filesDir, "templates.json")
    fun mine(c: Context): List<Template> = runCatching {
        val a = JSONArray(file(c).readText())
        List(a.length()) { a.getJSONObject(it).let { o -> Template(o.getString("name"), parseStyle(o.getJSONObject("style"))) } }
    }.getOrDefault(emptyList())
    private fun write(c: Context, list: List<Template>) =
        file(c).writeText(JSONArray(list.map { JSONObject().put("name", it.name).put("style", it.style.json()) }).toString())
    fun add(c: Context, t: Template) = write(c, mine(c).filter { it.name != t.name } + t)
    fun remove(c: Context, name: String) = write(c, mine(c).filter { it.name != name })
}
