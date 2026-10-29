package com.jayv.dontscroll.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.sin

/**
 * Draws the brain mascot with plain Android graphics, so the same drawing is used
 * in the app (through Compose's native canvas) and in the block screen overlays.
 * [health] 0..100 changes colour, eyes, mouth and adds drips at low health.
 * [phase] 0..1 drives a gentle bob.
 */
object BrainPainter {
    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42, 26, 20) }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.rgb(42, 26, 20)
    }
    private val blush = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val oval = RectF()

    private val lobes = arrayOf(
        floatArrayOf(-0.42f, -0.05f, 0.38f),
        floatArrayOf(-0.15f, -0.35f, 0.36f),
        floatArrayOf(0.18f, -0.36f, 0.36f),
        floatArrayOf(0.45f, -0.05f, 0.36f),
        floatArrayOf(0.25f, 0.22f, 0.36f),
        floatArrayOf(-0.22f, 0.22f, 0.36f),
        floatArrayOf(0f, 0f, 0.45f),
    )

    private fun lerp(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * tt).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * tt).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * tt).toInt(),
        )
    }

    fun bodyColor(health: Int): Int {
        val healthy = Color.rgb(255, 158, 184)
        val tired = Color.rgb(214, 160, 150)
        val rotten = Color.rgb(141, 160, 90)
        return if (health >= 50) lerp(tired, healthy, (health - 50) / 50f)
        else lerp(rotten, tired, health / 50f)
    }

    fun draw(c: Canvas, w: Float, h: Float, health: Int, phase: Float = 0f) {
        val size = minOf(w, h)
        val u = size / 2f * 0.78f
        val bob = sin(phase * 2 * PI).toFloat() * u * 0.03f
        val cx = w / 2f
        val cy = h / 2f - u * 0.05f + bob

        // Shadow
        shadow.color = Color.argb(70, 0, 0, 0)
        oval.set(cx - u * 0.75f, h / 2f + u * 0.78f, cx + u * 0.75f, h / 2f + u * 0.92f)
        c.drawOval(oval, shadow)

        val bc = bodyColor(health)
        body.color = bc
        for (l in lobes) c.drawCircle(cx + l[0] * u, cy + l[1] * u, l[2] * u, body)

        // Drips at low health
        if (health < 35) {
            val n = if (health < 15) 3 else 2
            val xs = floatArrayOf(-0.35f, 0.3f, -0.02f)
            for (i in 0 until n) {
                val x = cx + xs[i] * u
                val top = cy + 0.48f * u
                val len = (0.18f + 0.08f * i) * u * (1f + 0.15f * sin((phase + i * 0.3f) * 2 * PI).toFloat())
                path.reset()
                path.moveTo(x - 0.06f * u, top)
                path.quadTo(x - 0.07f * u, top + len * 0.7f, x, top + len)
                path.quadTo(x + 0.07f * u, top + len * 0.7f, x + 0.06f * u, top)
                path.close()
                c.drawPath(path, body)
            }
        }

        // Folds
        fold.color = lerp(bc, Color.BLACK, 0.18f)
        fold.strokeWidth = u * 0.06f
        path.reset()
        path.moveTo(cx, cy - 0.62f * u)
        path.quadTo(cx - 0.12f * u, cy - 0.45f * u, cx, cy - 0.3f * u)
        path.quadTo(cx + 0.12f * u, cy - 0.15f * u, cx, cy - 0.05f * u)
        c.drawPath(path, fold)
        path.reset()
        path.moveTo(cx - 0.62f * u, cy - 0.25f * u)
        path.quadTo(cx - 0.45f * u, cy - 0.2f * u, cx - 0.48f * u, cy + 0.02f * u)
        c.drawPath(path, fold)
        path.reset()
        path.moveTo(cx + 0.62f * u, cy - 0.25f * u)
        path.quadTo(cx + 0.45f * u, cy - 0.2f * u, cx + 0.48f * u, cy + 0.02f * u)
        c.drawPath(path, fold)

        // Blush when healthy
        if (health >= 70) {
            blush.color = Color.argb(110, 255, 110, 140)
            c.drawCircle(cx - 0.38f * u, cy + 0.22f * u, 0.08f * u, blush)
            c.drawCircle(cx + 0.38f * u, cy + 0.22f * u, 0.08f * u, blush)
        }

        // Eyes
        val ex = 0.2f * u
        val ey = cy + 0.05f * u
        val er = 0.11f * u
        stroke.strokeWidth = u * 0.055f
        if (health < 20) {
            // X eyes
            for (sx in floatArrayOf(-1f, 1f)) {
                val x = cx + sx * ex
                val d = er * 0.75f
                c.drawLine(x - d, ey - d, x + d, ey + d, stroke)
                c.drawLine(x - d, ey + d, x + d, ey - d, stroke)
            }
        } else {
            for (sx in floatArrayOf(-1f, 1f)) {
                val x = cx + sx * ex
                c.drawCircle(x, ey, er, white)
                c.drawCircle(x + er * 0.15f, ey + er * 0.15f, er * 0.5f, dark)
                c.drawCircle(x - er * 0.15f, ey - er * 0.25f, er * 0.18f, white)
                if (health < 50) {
                    // Heavy eyelids
                    oval.set(x - er * 1.15f, ey - er * 1.25f, x + er * 1.15f, ey + er * 0.05f)
                    c.drawArc(oval, 180f, 180f, true, body)
                    c.drawLine(x - er, ey - er * 0.1f, x + er, ey - er * 0.1f, stroke)
                }
            }
        }

        // Mouth
        val my = cy + 0.28f * u
        val mw = 0.16f * u
        path.reset()
        when {
            health >= 80 -> {
                path.moveTo(cx - mw, my)
                path.quadTo(cx, my + 0.16f * u, cx + mw, my)
            }
            health >= 50 -> {
                path.moveTo(cx - mw * 0.8f, my + 0.04f * u)
                path.lineTo(cx + mw * 0.8f, my + 0.04f * u)
            }
            health >= 20 -> {
                path.moveTo(cx - mw * 0.8f, my + 0.08f * u)
                path.quadTo(cx, my - 0.03f * u, cx + mw * 0.8f, my + 0.08f * u)
            }
            else -> {
                path.moveTo(cx - mw, my + 0.04f * u)
                path.quadTo(cx - mw * 0.5f, my - 0.04f * u, cx, my + 0.04f * u)
                path.quadTo(cx + mw * 0.5f, my + 0.12f * u, cx + mw, my + 0.04f * u)
            }
        }
        c.drawPath(path, stroke)
    }

    /** The Scroll Monster for the Battle tab: a purple ghost holding a phone. */
    fun drawMonster(c: Canvas, w: Float, h: Float, hp: Int, phase: Float = 0f) {
        val size = minOf(w, h)
        val u = size / 2f * 0.8f
        val bob = sin((phase + 0.5f) * 2 * PI).toFloat() * u * 0.04f
        val cx = w / 2f
        val cy = h / 2f + bob
        val weak = hp < 30
        body.color = if (weak) Color.rgb(110, 98, 150) else Color.rgb(139, 108, 255)

        shadow.color = Color.argb(70, 0, 0, 0)
        oval.set(cx - u * 0.6f, h / 2f + u * 0.82f, cx + u * 0.6f, h / 2f + u * 0.94f)
        c.drawOval(oval, shadow)

        path.reset()
        val left = cx - 0.62f * u
        val right = cx + 0.62f * u
        val top = cy - 0.75f * u
        val bottom = cy + 0.62f * u
        oval.set(left, top, right, top + 1.24f * u)
        path.arcTo(oval, 180f, 180f, false)
        path.lineTo(right, bottom)
        val waves = 4
        val ww = (right - left) / waves
        for (i in 0 until waves) {
            val x0 = right - i * ww
            val wiggle = sin((phase + i * 0.25f) * 2 * PI).toFloat() * 0.04f * u
            path.quadTo(x0 - ww / 2, bottom - 0.16f * u + wiggle, x0 - ww, bottom)
        }
        path.close()
        c.drawPath(path, body)

        // Eyes
        val ey = cy - 0.2f * u
        for (sx in floatArrayOf(-1f, 1f)) {
            val x = cx + sx * 0.22f * u
            c.drawCircle(x, ey, 0.13f * u, white)
            if (weak) {
                stroke.strokeWidth = u * 0.05f
                val d = 0.08f * u
                c.drawLine(x - d, ey - d, x + d, ey + d, stroke)
                c.drawLine(x - d, ey + d, x + d, ey - d, stroke)
            } else {
                dark.color = Color.rgb(200, 30, 60)
                c.drawCircle(x, ey + 0.02f * u, 0.06f * u, dark)
                dark.color = Color.rgb(42, 26, 20)
                // Angry brows
                stroke.strokeWidth = u * 0.06f
                c.drawLine(x - sx * 0.14f * u, ey - 0.22f * u, x + sx * 0.1f * u, ey - 0.13f * u, stroke)
            }
        }
        // Grin
        stroke.strokeWidth = u * 0.05f
        path.reset()
        if (weak) {
            path.moveTo(cx - 0.14f * u, cy + 0.1f * u)
            path.quadTo(cx, cy + 0.02f * u, cx + 0.14f * u, cy + 0.1f * u)
        } else {
            path.moveTo(cx - 0.2f * u, cy + 0.02f * u)
            path.quadTo(cx, cy + 0.2f * u, cx + 0.2f * u, cy + 0.02f * u)
        }
        c.drawPath(path, stroke)

        // Phone in hand
        val px = cx + 0.5f * u
        val py = cy + 0.18f * u
        dark.color = Color.rgb(30, 28, 34)
        oval.set(px - 0.15f * u, py - 0.26f * u, px + 0.15f * u, py + 0.26f * u)
        c.drawRoundRect(oval, 0.05f * u, 0.05f * u, dark)
        white.color = Color.rgb(255, 176, 103)
        oval.set(px - 0.11f * u, py - 0.2f * u, px + 0.11f * u, py + 0.18f * u)
        c.drawRoundRect(oval, 0.03f * u, 0.03f * u, white)
        white.color = Color.WHITE
        dark.color = Color.rgb(42, 26, 20)
    }
}
