package com.ayattla.app.studio

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Decorations for cards, statuses and clips. Pure geometry (no text), drawn in the style's accent colour
 * so every palette gets a matching look; [k] is the scale from the 1080-wide design.
 */
enum class Ornament(val label: String) {
    Classic("إطار ذهبي"),
    Arch("محراب"),
    Corners("زوايا مزخرفة"),
    Medallion("نجمة خلفية"),
    Bands("شريط زخرفي"),
    Lantern("قناديل"),
    None("بدون"),
}

object Ornaments {
    private fun a(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    fun star(cx: Float, cy: Float, outer: Float, rot: Double = -PI / 2): Path {
        val inner = outer * (cos(PI / 4) / cos(PI / 8)).toFloat()
        return Path().apply {
            for (i in 0 until 16) {
                val ang = PI / 8 * i + rot
                val r = if (i % 2 == 0) outer else inner
                val x = cx + cos(ang).toFloat() * r; val y = cy + sin(ang).toFloat() * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    fun draw(c: Canvas, w: Float, h: Float, k: Float, accent: Int, kind: Ornament) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        when (kind) {
            Ornament.None -> Unit
            Ornament.Classic -> {
                stroke.strokeWidth = 3f * k; stroke.color = a(accent, 150)
                c.drawRoundRect(RectF(36f * k, 36f * k, w - 36f * k, h - 36f * k), 40f * k, 40f * k, stroke)
                stroke.strokeWidth = 1.5f * k; stroke.color = a(accent, 80)
                c.drawRoundRect(RectF(52f * k, 52f * k, w - 52f * k, h - 52f * k), 30f * k, 30f * k, stroke)
            }
            Ornament.Arch -> {
                // A mihrab arch: straight sides, pointed top, with an inner echo and a small star at the apex.
                fun arch(inset: Float): Path {
                    val l = inset; val r = w - inset; val b = h - inset
                    val spring = h * 0.30f
                    val top = inset + 10f * k
                    return Path().apply {
                        moveTo(l, b); lineTo(l, spring)
                        cubicTo(l, spring - (spring - top) * 0.62f, w / 2 - (w / 2 - l) * 0.28f, top + (spring - top) * 0.18f, w / 2, top)
                        cubicTo(w / 2 + (r - w / 2) * 0.28f, top + (spring - top) * 0.18f, r, spring - (spring - top) * 0.62f, r, spring)
                        lineTo(r, b); close()
                    }
                }
                stroke.strokeWidth = 3.5f * k; stroke.color = a(accent, 170); c.drawPath(arch(44f * k), stroke)
                stroke.strokeWidth = 1.5f * k; stroke.color = a(accent, 90); c.drawPath(arch(64f * k), stroke)
                fill.color = a(accent, 200); c.drawPath(star(w / 2, 54f * k + 26f * k, 16f * k), fill)
            }
            Ornament.Corners -> {
                stroke.strokeWidth = 2f * k; stroke.color = a(accent, 110)
                c.drawRoundRect(RectF(48f * k, 48f * k, w - 48f * k, h - 48f * k), 24f * k, 24f * k, stroke)
                val r = 46f * k
                listOf(48f * k to 48f * k, w - 48f * k to 48f * k, 48f * k to h - 48f * k, w - 48f * k to h - 48f * k).forEach { (x, y) ->
                    fill.color = a(accent, 230); c.drawPath(star(x, y, r * 0.62f), fill)
                    stroke.color = a(accent, 160); stroke.strokeWidth = 2f * k; c.drawPath(star(x, y, r), stroke)
                    stroke.color = a(accent, 110)
                }
                // A small knot in the middle of each side.
                listOf(w / 2 to 48f * k, w / 2 to h - 48f * k).forEach { (x, y) -> fill.color = a(accent, 200); c.drawPath(star(x, y, 12f * k), fill) }
            }
            Ornament.Medallion -> {
                val r = min(w, h) * 0.46f
                stroke.strokeWidth = 2.5f * k; stroke.color = a(accent, 60)
                c.drawPath(star(w / 2, h / 2, r), stroke)
                c.drawPath(star(w / 2, h / 2, r * 0.78f), stroke.apply { color = a(accent, 40) })
                c.drawCircle(w / 2, h / 2, r * 1.05f, stroke.apply { color = a(accent, 45); strokeWidth = 1.5f * k })
                for (i in 0 until 16) {
                    val ang = PI / 8 * i
                    c.drawLine(w / 2 + cos(ang).toFloat() * r * 0.35f, h / 2 + sin(ang).toFloat() * r * 0.35f,
                        w / 2 + cos(ang).toFloat() * r * 0.98f, h / 2 + sin(ang).toFloat() * r * 0.98f, stroke.apply { color = a(accent, 22); strokeWidth = 1.2f * k })
                }
                stroke.strokeWidth = 2f * k; stroke.color = a(accent, 110)
                c.drawRoundRect(RectF(40f * k, 40f * k, w - 40f * k, h - 40f * k), 34f * k, 34f * k, stroke)
            }
            Ornament.Bands -> {
                // Geometric bands at the top and bottom: a row of small eight-point stars between two rules.
                fun band(y: Float) {
                    stroke.strokeWidth = 2f * k; stroke.color = a(accent, 150)
                    c.drawLine(60f * k, y - 26f * k, w - 60f * k, y - 26f * k, stroke)
                    c.drawLine(60f * k, y + 26f * k, w - 60f * k, y + 26f * k, stroke)
                    val n = 11; val step = (w - 160f * k) / (n - 1)
                    for (i in 0 until n) {
                        val x = 80f * k + i * step
                        if (i % 2 == 0) { fill.color = a(accent, 210); c.drawPath(star(x, y, 13f * k), fill) }
                        else { stroke.color = a(accent, 140); c.drawPath(star(x, y, 11f * k, 0.0), stroke) }
                    }
                }
                band(96f * k); band(h - 96f * k)
            }
            Ornament.Lantern -> {
                // Hanging lanterns (Ramadan and Eid): three cords from the top edge, each with a small lantern.
                stroke.strokeWidth = 2f * k
                listOf(0.22f to 0.085f, 0.5f to 0.12f, 0.78f to 0.07f).forEach { (fx, fy) ->
                    val x = w * fx; val y = h * fy
                    stroke.color = a(accent, 150); c.drawLine(x, 0f, x, y - 34f * k, stroke)
                    val body = Path().apply {
                        moveTo(x, y - 34f * k)
                        lineTo(x + 20f * k, y - 14f * k); lineTo(x + 20f * k, y + 22f * k)
                        lineTo(x, y + 40f * k); lineTo(x - 20f * k, y + 22f * k); lineTo(x - 20f * k, y - 14f * k); close()
                    }
                    fill.color = a(accent, 70); c.drawPath(body, fill)
                    stroke.color = a(accent, 210); c.drawPath(body, stroke)
                    fill.color = a(Color.rgb(255, 236, 170), 200); c.drawCircle(x, y + 4f * k, 7f * k, fill)
                    fill.color = a(accent, 220); c.drawPath(star(x, y + 52f * k, 6f * k), fill)
                }
                stroke.strokeWidth = 1.5f * k; stroke.color = a(accent, 90)
                c.drawRoundRect(RectF(40f * k, 40f * k, w - 40f * k, h - 40f * k), 30f * k, 30f * k, stroke)
            }
        }
    }
}
