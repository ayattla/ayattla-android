package com.ayattla.app.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat
import com.ayattla.app.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** What one screen of a clip (or one card) shows: Quran text exactly as in the pack, and its reference. */
data class Part(val text: String, val reference: String, val keys: List<String>)

/**
 * Draws a project in layers, at any size (everything scales from the 1080-wide design):
 *  background  — colour style, photo (centre-cropped) or the first frame of a video;
 *  base        — the veil over photos/videos and the gold frame (still for the whole clip);
 *  text        — Quran text and its reference (the layer that moves).
 * The text is set with Amiri Quran and never altered; hidden words (word-by-word) keep their place.
 */
class Composer(private val context: Context, val style: Style, val w: Int, val h: Int, private val onMedia: Boolean) {
    private val k = w / 1080f
    private val quran: Typeface = ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.SERIF
    private val ui: Typeface = ResourcesCompat.getFont(context, R.font.plex_semibold) ?: Typeface.DEFAULT_BOLD
    /** Over a photo or video the light palette is used (as the site does). */
    private val palette = if (onMedia) CardStyle.Night else style.colors
    private val textColor = style.text.color?.toInt() ?: palette.text
    private val refColor = style.ref.color?.toInt() ?: palette.accent

    fun backgroundBitmap(photo: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (photo != null) cover(c, photo)
        else c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, w * 0.3f, h.toFloat(), palette.top, palette.bottom, Shader.TileMode.CLAMP)
        })
        return bmp
    }

    fun baseLayer(): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (onMedia && style.veil > 0f) {
            val a = (style.veil * 255).roundToInt().coerceIn(0, 230)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.argb((a * 0.65f).toInt(), 0, 0, 0), Color.argb(a, 0, 0, 0), Shader.TileMode.CLAMP)
            })
        }
        if (style.frame) {
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 3f * k; color = alpha(palette.accent, 150) }
            c.drawRoundRect(RectF(36f * k, 36f * k, w - 36f * k, h - 36f * k), 40f * k, 40f * k, frame)
            c.drawRoundRect(RectF(52f * k, 52f * k, w - 52f * k, h - 52f * k), 30f * k, 30f * k, frame.apply { strokeWidth = 1.5f * k; color = alpha(palette.accent, 80) })
        }
        if (style.watermark) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ui; color = alpha(textColor, 150); textSize = 30f * k; textAlign = Paint.Align.CENTER }
            c.drawText("آيات تتلى · ayattla.com", w / 2f, h - 110f * k * (h / (w * 1f)).coerceAtMost(1.4f), p)
        }
        return bmp
    }

    /** Number of words in [part] (for word-by-word). */
    fun words(part: Part) = part.text.split(' ').count { it.isNotBlank() }

    /** Text and reference on a transparent frame. [visibleWords] < all: the rest is laid out but not drawn. */
    fun textLayer(part: Part, visibleWords: Int = Int.MAX_VALUE): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val padX = 120f * k
        val boxW = (w - padX * 2).toInt()
        val maxH = h - 2 * 230f * k
        val spannable = SpannableString(part.text)
        if (visibleWords < Int.MAX_VALUE) {
            var count = 0
            var i = 0
            while (i < part.text.length) {
                if (part.text[i] != ' ' && (i == 0 || part.text[i - 1] == ' ')) {
                    count++
                    if (count > visibleWords) { spannable.setSpan(ForegroundColorSpan(Color.TRANSPARENT), i, part.text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); break }
                }
                i++
            }
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = quran; color = textColor
            // Opaque shadow colour: its strength follows each letter's own alpha, so hidden words cast none.
            if (style.text.shadow) setShadowLayer(12f * k, 0f, 3f * k, Color.BLACK)
            if (Build.VERSION.SDK_INT >= 29) wordSpacing = 0f
        }
        val align = when (style.text.align) { Align.Center -> Layout.Alignment.ALIGN_CENTER; Align.Start -> Layout.Alignment.ALIGN_NORMAL; Align.End -> Layout.Alignment.ALIGN_OPPOSITE }
        var size = 72f * k
        var layout: StaticLayout
        while (true) {
            paint.textSize = size
            if (Build.VERSION.SDK_INT >= 29) paint.wordSpacing = style.text.wordSpacing * size
            layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, boxW)
                .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(size * style.text.lineSpacing, 1f).setIncludePad(true).build()
            if (layout.height <= maxH || size <= 24f * k) break
            size -= 2f * k
        }
        // The user's size, then fit again if it no longer fits.
        if (style.text.size != 1f) {
            var s2 = size * style.text.size
            while (true) {
                paint.textSize = s2
                if (Build.VERSION.SDK_INT >= 29) paint.wordSpacing = style.text.wordSpacing * s2
                layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, boxW)
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL)
                    .setLineSpacing(s2 * style.text.lineSpacing, 1f).setIncludePad(true).build()
                if (layout.height <= h - 2 * 140f * k || s2 <= 22f * k) break
                s2 -= 2f * k
            }
        }
        val free = (h - layout.height) / 2f
        val y = (free - 30f * k + style.text.posY * free * 0.85f).coerceIn(60f * k, max(60f * k, h - layout.height - 60f * k))
        c.save(); c.translate(padX, y); layout.draw(c); c.restore()

        if (style.ref.show && part.reference.isNotBlank()) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ui; color = refColor; textSize = 40f * k * style.ref.size; textAlign = Paint.Align.CENTER
                if (onMedia) setShadowLayer(8f * k, 0f, 2f * k, Color.argb(160, 0, 0, 0))
            }
            val below = y + layout.height + 70f * k
            val bottom = h - 170f * k
            val ry = (min(max(below, h * 0.55f), bottom) + style.ref.posY * 200f * k).coerceIn(90f * k, h - 60f * k)
            c.drawText(part.reference, w / 2f, ry, p)
        }
        return bmp
    }

    /**
     * Draws the moving text layer at [t] ms into a part of [duration] ms: in-animation at the start,
     * a fade out at the end (none for the last part, so the clip ends on the text).
     */
    fun drawAnimated(c: Canvas, layer: Bitmap, t: Long, duration: Long, last: Boolean) {
        val d = style.animMs.coerceAtLeast(1).toFloat()
        val inP = (t / d).coerceIn(0f, 1f)
        val outP = if (last || style.anim == Anim.None) 1f else ((duration - t) / (d * 0.6f)).coerceIn(0f, 1f)
        val ease = 1f - (1f - inP) * (1f - inP)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val m = Matrix()
        when (style.anim) {
            Anim.None, Anim.Words -> paint.alpha = (255 * outP).roundToInt()
            Anim.Fade -> paint.alpha = (255 * ease * outP).roundToInt()
            Anim.Slide -> { paint.alpha = (255 * ease * outP).roundToInt(); m.setTranslate(0f, (1f - ease) * 90f * k) }
            Anim.Zoom -> {
                paint.alpha = (255 * ease * outP).roundToInt()
                val s = 0.9f + 0.1f * ease
                m.setScale(s, s, w / 2f, h / 2f)
            }
        }
        c.drawBitmap(layer, m, paint)
    }

    /** One still image (a card, or the preview): background, base, text. */
    fun still(photo: Bitmap?, part: Part, background: Bitmap? = null): Bitmap {
        val out = (background ?: backgroundBitmap(photo)).copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        c.drawBitmap(baseLayer(), 0f, 0f, null)
        c.drawBitmap(textLayer(part), 0f, 0f, null)
        return out
    }

    private fun cover(c: Canvas, photo: Bitmap) {
        val scale = max(w.toFloat() / photo.width, h.toFloat() / photo.height)
        val dw = photo.width * scale; val dh = photo.height * scale
        c.drawBitmap(photo, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    companion object {
        fun alpha(color: Int, a: Int) = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        /** Output size for a ratio and quality (even numbers, as encoders need). */
        fun size(ratio: CardRatio, q: Quality): Pair<Int, Int> {
            fun even(x: Float) = (x.roundToInt() / 2) * 2
            return even(ratio.w * q.scale) to even(ratio.h * q.scale)
        }
        fun clear(c: Canvas) = c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }
}
