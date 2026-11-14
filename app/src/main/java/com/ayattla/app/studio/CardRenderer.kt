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
}

enum class CardStyle(val label: String, val top: Int, val bottom: Int, val text: Int, val accent: Int) {
    Emerald("زمردي", 0xFF17392F.toInt(), 0xFF0B211C.toInt(), 0xFFF6F2E9.toInt(), 0xFFC3A66B.toInt()),
    Ivory("عاجي", 0xFFFBF8F1.toInt(), 0xFFEFE9DC.toInt(), 0xFF202823.toInt(), 0xFF7A5F28.toInt()),
    Night("ليلي", 0xFF14202B.toInt(), 0xFF070C12.toInt(), 0xFFEDE7DA.toInt(), 0xFFD9C28E.toInt()),
}

/** Draws an ayah card: Quran text exactly as in the pack, Amiri Quran, centered, auto-fitted. */
object CardRenderer {
    fun render(context: Context, ayat: List<Ayah>, reference: String, ratio: CardRatio, style: CardStyle): Bitmap {
        val bmp = Bitmap.createBitmap(ratio.w, ratio.h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = ratio.w.toFloat()
        val h = ratio.h.toFloat()
        c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w * 0.3f, h, style.top, style.bottom, Shader.TileMode.CLAMP) })

        // Gold frame, like the site's emerald panel.
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 3f; color = withAlpha(style.accent, 150) }
        c.drawRoundRect(RectF(36f, 36f, w - 36f, h - 36f), 40f, 40f, frame)
        c.drawRoundRect(RectF(52f, 52f, w - 52f, h - 52f), 30f, 30f, frame.apply { strokeWidth = 1.5f; color = withAlpha(style.accent, 80) })

        val quran = ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.SERIF
        val ui = ResourcesCompat.getFont(context, R.font.plex_semibold) ?: Typeface.DEFAULT_BOLD
        val text = ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }

        val padX = 120f
        val boxW = (w - padX * 2).toInt()
        val maxH = h - 2 * 220f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = quran; color = style.text }
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
        c.translate(padX, (h - layout.height) / 2f - 30f)
        layout.draw(c)
        c.restore()

        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = style.accent; textSize = 40f; textAlign = Paint.Align.CENTER }
        c.drawText(reference, w / 2, h - 170f, refPaint)
        val mark = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = withAlpha(style.text, 150); textSize = 30f; textAlign = Paint.Align.CENTER }
        c.drawText("آيات تتلى · ayattla.com", w / 2, h - 110f, mark)
        return bmp
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
