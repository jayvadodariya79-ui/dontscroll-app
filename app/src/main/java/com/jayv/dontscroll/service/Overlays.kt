package com.jayv.dontscroll.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.jayv.dontscroll.ui.components.BrainPainter

/**
 * Windows DontScroll draws over Instagram/YouTube/Snapchat/Facebook, using the
 * accessibility overlay window type (no "display over other apps" permission needed).
 * Built with plain Views because they live in the service, not in an activity.
 */
class Overlays(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val density = ctx.resources.displayMetrics.density

    private var bubble: TextView? = null
    private var banner: TextView? = null
    private var block: View? = null
    private var blockKey: String? = null
    private var pause: View? = null
    private var pauseApp: String? = null

    private val hideBanner = Runnable { removeBanner() }

    private fun dp(v: Float) = (v * density).toInt()

    private fun baseParams(touchable: Boolean, full: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val size = if (full) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        return WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        )
    }

    private fun pill(bg: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        setColor(bg); cornerRadius = radiusDp * density
    }

    private fun label(size: Float, color: Int, bold: Boolean = false): TextView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private fun safeAdd(v: View, p: WindowManager.LayoutParams): Boolean = try {
        wm.addView(v, p); true
    } catch (e: Exception) {
        DebugLog.add("overlay", "could not show: ${e.javaClass.simpleName}")
        false
    }

    private fun safeRemove(v: View?) {
        if (v == null) return
        try { wm.removeViewImmediate(v) } catch (e: Exception) { }
    }

    // ---------------- Live counter bubble ----------------

    fun showBubble(textValue: String) {
        val b = bubble
        if (b != null) {
            if (b.text != textValue) b.text = textValue
            return
        }
        val t = label(13f, Color.WHITE, bold = true).apply {
            text = textValue
            background = pill(Color.argb(215, 22, 21, 19), 18f)
            setPadding(dp(14f), dp(7f), dp(14f), dp(7f))
        }
        val p = baseParams(touchable = false, full = false).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(56f)
        }
        if (safeAdd(t, p)) bubble = t
    }

    fun hideBubble() { safeRemove(bubble); bubble = null }

    // ---------------- Banner (warnings) ----------------

    fun showBanner(message: String, millis: Long = 4500) {
        removeBanner()
        val t = label(14f, Color.rgb(42, 26, 20), bold = true).apply {
            text = message
            background = pill(Color.rgb(255, 176, 103), 16f)
            setPadding(dp(18f), dp(12f), dp(18f), dp(12f))
            maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.9f).toInt()
        }
        val p = baseParams(touchable = false, full = false).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(100f)
        }
        if (safeAdd(t, p)) {
            banner = t
            handler.removeCallbacks(hideBanner)
            handler.postDelayed(hideBanner, millis)
        }
    }

    private fun removeBanner() {
        handler.removeCallbacks(hideBanner)
        safeRemove(banner); banner = null
    }

    // ---------------- Brain view for overlays ----------------

    private class BrainView(ctx: Context, private val health: Int) : View(ctx) {
        private val start = System.currentTimeMillis()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val phase = ((System.currentTimeMillis() - start) % 2400L) / 2400f
            BrainPainter.draw(canvas, width.toFloat(), height.toFloat(), health, phase)
            postInvalidateDelayed(40)
        }
    }

    private fun button(caption: String, primary: Boolean): TextView = label(16f, if (primary) Color.rgb(42, 26, 20) else Color.WHITE, bold = true).apply {
        text = caption
        background = if (primary) pill(Color.rgb(255, 176, 103), 28f) else pill(Color.argb(40, 255, 255, 255), 28f)
        setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
        isClickable = true
        isFocusable = false
    }

    private fun column(): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(28f), dp(24f), dp(28f), dp(24f))
    }

    private fun full(content: View): FrameLayout = FrameLayout(ctx).apply {
        setBackgroundColor(Color.argb(245, 10, 10, 9))
        isClickable = true // swallow touches so the feed behind cannot be used
        addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        )
    }

    private fun spacer(h: Float) = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun fullWidth(v: View) = v.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    // ---------------- Block screen ----------------

    val isBlockShowing: Boolean get() = block != null

    /**
     * Full-screen limit screen. [key] identifies the reason so the same screen is not rebuilt
     * every second. [snoozeLabel] null = no snooze offered.
     */
    @SuppressLint("SetTextI18n")
    fun showBlock(
        key: String,
        title: String,
        message: String,
        stats: String,
        health: Int,
        snoozeLabel: String?,
        onLeave: () -> Unit,
        onSnooze: () -> Unit,
    ) {
        if (block != null && blockKey == key) return
        hideBlock()
        hidePause()
        hideBubble()
        val col = column()
        col.addView(BrainView(ctx, health), LinearLayout.LayoutParams(dp(170f), dp(170f)))
        col.addView(spacer(8f))
        col.addView(fullWidth(label(26f, Color.WHITE, bold = true).apply { text = title }))
        col.addView(spacer(10f))
        col.addView(fullWidth(label(16f, Color.rgb(200, 195, 186)).apply { text = message }))
        col.addView(spacer(8f))
        col.addView(fullWidth(label(14f, Color.rgb(255, 176, 103), bold = true).apply { text = stats }))
        col.addView(spacer(32f))
        val leave = button("Leave now", primary = true)
        leave.setOnClickListener { onLeave() }
        col.addView(fullWidth(leave))
        if (snoozeLabel != null) {
            col.addView(spacer(12f))
            val snooze = button("Wait 10s…", primary = false)
            snooze.alpha = 0.5f
            snooze.isEnabled = false
            col.addView(fullWidth(snooze))
            var left = 10
            val tick = object : Runnable {
                override fun run() {
                    if (block == null) return
                    left--
                    if (left <= 0) {
                        snooze.text = snoozeLabel
                        snooze.alpha = 1f
                        snooze.isEnabled = true
                        snooze.setOnClickListener { onSnooze() }
                    } else {
                        snooze.text = "Wait ${left}s…"
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.postDelayed(tick, 1000)
        }
        val root = full(col)
        val p = baseParams(touchable = true, full = true)
        if (safeAdd(root, p)) {
            block = root
            blockKey = key
        }
    }

    fun hideBlock() {
        safeRemove(block); block = null; blockKey = null
    }

    // ---------------- Pause before reels ----------------

    val pauseShowingFor: String? get() = if (pause != null) pauseApp else null

    @SuppressLint("SetTextI18n")
    fun showPause(appKey: String, appFeature: String, seconds: Int, onContinue: () -> Unit, onLeave: () -> Unit) {
        if (pause != null && pauseApp == appKey) return
        hidePause()
        hideBubble()
        val col = column()
        col.addView(BrainView(ctx, 100), LinearLayout.LayoutParams(dp(150f), dp(150f)))
        col.addView(spacer(8f))
        col.addView(fullWidth(label(26f, Color.WHITE, bold = true).apply { text = "Take a breath" }))
        col.addView(spacer(10f))
        col.addView(fullWidth(label(16f, Color.rgb(200, 195, 186)).apply {
            text = "You're opening $appFeature. Do you really want to scroll right now?"
        }))
        col.addView(spacer(28f))
        val counter = label(44f, Color.rgb(255, 176, 103), bold = true).apply { text = seconds.toString() }
        col.addView(fullWidth(counter))
        col.addView(spacer(24f))
        val leave = button("I'll skip it", primary = true)
        leave.setOnClickListener { onLeave() }
        col.addView(fullWidth(leave))
        col.addView(spacer(12f))
        val cont = button("Continue to $appFeature", primary = false).apply { alpha = 0.4f; isEnabled = false }
        col.addView(fullWidth(cont))
        var left = seconds
        val tick = object : Runnable {
            override fun run() {
                if (pause == null) return
                left--
                if (left <= 0) {
                    counter.text = "✓"
                    cont.alpha = 1f
                    cont.isEnabled = true
                    cont.setOnClickListener { onContinue() }
                } else {
                    counter.text = left.toString()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(tick, 1000)
        val root = full(col)
        if (safeAdd(root, baseParams(touchable = true, full = true))) {
            pause = root
            pauseApp = appKey
        }
    }

    fun hidePause() {
        safeRemove(pause); pause = null; pauseApp = null
    }

    fun hideAll() {
        hideBubble(); removeBanner(); hideBlock(); hidePause()
    }
}
