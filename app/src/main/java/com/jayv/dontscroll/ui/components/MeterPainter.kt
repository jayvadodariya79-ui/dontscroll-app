package com.jayv.dontscroll.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Draws SwipeStop's meter with plain Android graphics, so the same drawing is used in
 * the app (through Compose's native canvas) and in the overlays.
 *
 * A phone whose screen fills up with violet as swipes are used. [left] is 0..100
 * (percent of today's budget still left). At 0 the stop bar crosses the phone.
 */
object MeterPainter {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val r = RectF()

    private const val BODY = 0xFF2B2342.toInt()
    private const val EDGE = 0xFF4A3D70.toInt()
    private const val SCREEN = 0xFF130E22.toInt()

    /** Colour for how much is left: mint when plenty, yellow when low, rose when out. */
    fun levelColor(left: Int): Int = when {
        left >= 50 -> 0xFF5EEAD4.toInt()
        left >= 20 -> 0xFFFACC15.toInt()
        else -> 0xFFFB7185.toInt()
    }

    fun draw(c: Canvas, w: Float, h: Float, left: Int) {
        val pct = left.coerceIn(0, 100)
        // Phone body
        val ph = h * 0.86f
        val pw = minOf(w * 0.56f, ph * 0.55f)
        val cx = w / 2f
        val top = (h - ph) / 2f
        val radius = pw * 0.16f
        fill.color = BODY
        r.set(cx - pw / 2, top, cx + pw / 2, top + ph)
        c.drawRoundRect(r, radius, radius, fill)
        stroke.color = EDGE
        stroke.strokeWidth = pw * 0.035f
        c.drawRoundRect(r, radius, radius, stroke)

        // Screen
        val inset = pw * 0.09f
        val sl = cx - pw / 2 + inset
        val sr = cx + pw / 2 - inset
        val st = top + inset * 1.5f
        val sb = top + ph - inset * 1.5f
        fill.color = SCREEN
        r.set(sl, st, sr, sb)
        c.drawRoundRect(r, radius * 0.5f, radius * 0.5f, fill)

        // Level: swipes left, filled from the bottom
        val levelTop = sb - (sb - st) * pct / 100f
        if (pct > 0) {
            fill.color = levelColor(pct)
            fill.alpha = 70
            r.set(sl, levelTop, sr, sb)
            c.drawRoundRect(r, radius * 0.5f, radius * 0.5f, fill)
            fill.alpha = 255
            stroke.color = levelColor(pct)
            stroke.strokeWidth = pw * 0.035f
            c.drawLine(sl + inset * 0.3f, levelTop, sr - inset * 0.3f, levelTop, stroke)
        }

        // Up chevron (the swipe)
        val chW = (sr - sl) * 0.32f
        val chY = st + (sb - st) * 0.3f
        stroke.color = Color.WHITE
        stroke.strokeWidth = pw * 0.07f
        c.drawLine(cx - chW, chY + chW * 0.8f, cx, chY, stroke)
        c.drawLine(cx, chY, cx + chW, chY + chW * 0.8f, stroke)

        // Stop bar: violet while there are swipes left, rose when out
        val barH = pw * 0.15f
        val barY = st + (sb - st) * 0.52f
        fill.color = if (pct == 0) 0xFFFB7185.toInt() else 0xFFA78BFA.toInt()
        r.set(cx - pw * 0.78f, barY, cx + pw * 0.78f, barY + barH)
        c.drawRoundRect(r, barH / 2, barH / 2, fill)
    }
}
