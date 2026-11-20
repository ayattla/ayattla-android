package com.ayattla.app.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.ayattla.app.R
import com.ayattla.app.data.Ayah
import com.ayattla.app.ui.theme.Num
import java.io.File

enum class CardRatio(val label: String, val w: Int, val h: Int) {
    Square("مربع 1:1", 1080, 1080),
    Portrait("منشور 4:5", 1080, 1350),
    Story("حالة 9:16", 1080, 1920),
    /** Home-screen widget only (not offered in the studio). */
    Wide("عريض", 1080, 600),
}

enum class CardStyle(val label: String, val top: Int, val bottom: Int, val text: Int, val accent: Int) {
    Emerald("زمردي", 0xFF17392F.toInt(), 0xFF0B211C.toInt(), 0xFFF6F2E9.toInt(), 0xFFC3A66B.toInt()),
    Ivory("عاجي", 0xFFFBF8F1.toInt(), 0xFFEFE9DC.toInt(), 0xFF202823.toInt(), 0xFF7A5F28.toInt()),
    Night("ليلي", 0xFF14202B.toInt(), 0xFF070C12.toInt(), 0xFFEDE7DA.toInt(), 0xFFD9C28E.toInt()),
    Royal("ملكي", 0xFF1B2A5C.toInt(), 0xFF0B1230.toInt(), 0xFFF3EEDF.toInt(), 0xFFE0C27A.toInt()),
    Burgundy("عنابي", 0xFF5A1A2B.toInt(), 0xFF2A0A14.toInt(), 0xFFF7EDE4.toInt(), 0xFFE2C07C.toInt()),
    Violet("رمضاني", 0xFF3A2160.toInt(), 0xFF170B2E.toInt(), 0xFFF4EEFB.toInt(), 0xFFF0CF83.toInt()),
    Teal("فيروزي", 0xFF0F5A5E.toInt(), 0xFF06292C.toInt(), 0xFFEFF8F6.toInt(), 0xFFE6C77E.toInt()),
    Sand("رملي", 0xFFF2E6CF.toInt(), 0xFFDCC7A1.toInt(), 0xFF3B2A14.toInt(), 0xFF8A5E1E.toInt()),
    Blush("وردي", 0xFFFBEDEC.toInt(), 0xFFF1D3D1.toInt(), 0xFF4A2730.toInt(), 0xFFA8794A.toInt()),
    Sky("سماوي", 0xFFEAF4FA.toInt(), 0xFFCFE3F0.toInt(), 0xFF173248.toInt(), 0xFF8E6C2C.toInt()),
    Olive("زيتوني", 0xFF3E4A1F.toInt(), 0xFF1C230C.toInt(), 0xFFF4F1E2.toInt(), 0xFFD9BE74.toInt()),
    Charcoal("فحمي", 0xFF2A2A2E.toInt(), 0xFF0E0E10.toInt(), 0xFFF2EFE8.toInt(), 0xFFCBAE6E.toInt()),
}

/** Draws an ayah card: Quran text exactly as in the pack, Amiri Quran, centered, auto-fitted. */
object CardRenderer {
    /**
     * [photo]: a real photo from the site's library, drawn behind the text with a dark veil.
     * [overlay]: only the text and the veil on a transparent frame (laid over a background video).
     */
    fun render(context: Context, ayat: List<Ayah>, reference: String, ratio: CardRatio, style: CardStyle, photo: Bitmap? = null, overlay: Boolean = false): Bitmap =
        renderText(context, ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }, reference, ratio, style, photo = photo, overlay = overlay)

    /** Style used over a photo or video: light text, gold accent. */
    private val OnPhoto = CardStyle.Night

    fun renderText(
        context: Context, text: String, reference: String, ratio: CardRatio, style0: CardStyle,
        photo: Bitmap? = null, overlay: Boolean = false, naskh: Boolean = false, footer: String? = null,
        ornament: Ornament = Ornament.Classic, header: String? = null, keepStyleOnPhoto: Boolean = false,
    ): Bitmap {
        val onPhoto = photo != null || overlay
        val style = if (onPhoto && !keepStyleOnPhoto) OnPhoto else style0
        val bmp = Bitmap.createBitmap(ratio.w, ratio.h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = ratio.w.toFloat()
        val h = ratio.h.toFloat()
        if (photo != null) drawCover(c, photo, w, h)
        if (onPhoto) {
            // The veil: darker at the bottom where the reference sits, as on the site's cards.
            c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, Color.argb(95, 0, 0, 0), Color.argb(175, 0, 0, 0), Shader.TileMode.CLAMP) })
        } else {
            c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w * 0.3f, h, style.top, style.bottom, Shader.TileMode.CLAMP) })
        }

        // Decoration (the gold frame by default, like the site's emerald panel).
        Ornaments.draw(c, w, h, 1f, style.accent, ornament)

        val quran = ResourcesCompat.getFont(context, if (naskh) R.font.amiri else R.font.amiri_quran) ?: Typeface.SERIF
        val ui = ResourcesCompat.getFont(context, R.font.plex_semibold) ?: Typeface.DEFAULT_BOLD
        // Optional occasion name at the top (e.g. «يوم الجمعة»), never a phrase of worship.
        if (!header.isNullOrBlank() && ratio != CardRatio.Wide) {
            val hp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = style.accent; textSize = 44f; textAlign = Paint.Align.CENTER
                if (onPhoto) setShadowLayer(10f, 0f, 2f, Color.argb(140, 0, 0, 0)) }
            val y = if (ratio == CardRatio.Story) 250f else 170f
            c.drawText(header, w / 2, y, hp)
            val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(style.accent, 150); strokeWidth = 2f }
            val tw = hp.measureText(header) / 2 + 30f
            c.drawLine(w / 2 - tw - 90f, y - 14f, w / 2 - tw, y - 14f, lp); c.drawLine(w / 2 + tw, y - 14f, w / 2 + tw + 90f, y - 14f, lp)
        }

        val wide = ratio == CardRatio.Wide
        val padX = if (wide) 90f else 120f
        val boxW = (w - padX * 2).toInt()
        val maxH = if (wide) h - 230f else h - 2 * 220f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = quran; color = style.text
            if (onPhoto) setShadowLayer(14f, 0f, 3f, Color.argb(150, 0, 0, 0))
        }
        var size = 72f
        var layout: StaticLayout
        while (true) {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, boxW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(size * 0.55f, 1f)
                .setIncludePad(true)
                .build()
            if (layout.height <= maxH || size <= 26f) break
            size -= 2f
        }
        c.save()
        c.translate(padX, if (wide) (h - 130f - layout.height) / 2f + 20f else (h - layout.height) / 2f - 30f)
        layout.draw(c)
        c.restore()

        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = style.accent; textSize = 40f; textAlign = Paint.Align.CENTER }
        c.drawText(reference, w / 2, if (wide) h - 100f else h - 170f, refPaint)
        val mark = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = withAlpha(style.text, 150); textSize = 30f; textAlign = Paint.Align.CENTER }
        c.drawText(footer ?: "آيات تتلى · ayattla.com", w / 2, if (wide) h - 58f else h - 110f, mark)
        return bmp
    }

    /** Hadith card: text in Amiri as published, then the source and the grade. */
    fun renderHadith(context: Context, text: String, source: String, grade: String, ratio: CardRatio, style: CardStyle, photo: Bitmap? = null): Bitmap =
        renderText(context, text, listOf(source, grade).filter { it.isNotBlank() }.joinToString(" · "), ratio, style, photo = photo, naskh = true)

    /** Centre-crops [photo] to fill the frame. */
    private fun drawCover(c: Canvas, photo: Bitmap, w: Float, h: Float) {
        val scale = maxOf(w / photo.width, h / photo.height)
        val dw = photo.width * scale; val dh = photo.height * scale
        c.drawBitmap(photo, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun withAlpha(color: Int, a: Int) = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))

    /** Saves to Pictures/AyatTutla and returns the content Uri. */
    fun saveToGallery(context: Context, bmp: Bitmap, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AyatTutla")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (Build.VERSION.SDK_INT >= 29) {
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    fun shareUri(context: Context, bmp: Bitmap): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "ayah-card.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "com.ayattla.app.fileprovider", f)
    }
}
