package com.jayv.dontscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.Brain
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.data.WatchedApp
import com.jayv.dontscroll.notify.Notifier

/**
 * The tracker. Watches Instagram, YouTube, Snapchat and Facebook through Android's
 * accessibility API, counts reels, measures time, and shows the live counter,
 * warnings, the pause screen and the limit screen.
 *
 * Everything runs on the main thread: accessibility callbacks plus a 1-second tick.
 */
class ReelService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val counter = ReelCounter()
    private lateinit var detector: Detector
    private lateinit var overlays: Overlays

    private var foreground: WatchedApp? = null
    private var lastTickAt = 0L
    private var lastFlushAt = 0L
    private val misses = HashMap<String, Int>()
    private val lastPlayerScrollAt = HashMap<String, Long>()
    private val idsSeen = HashSet<String>()
    private var lastLayoutCheckAt = 0L
    private var quickCheckPending = false
    private var leaving = false

    private val tick = object : Runnable {
        override fun run() {
            try {
                onTick(fromEvent = false)
            } catch (e: Exception) {
                DebugLog.add("service", "tick error: ${e.javaClass.simpleName}: ${e.message}")
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val quickCheck = Runnable {
        quickCheckPending = false
        try {
            onTick(fromEvent = true)
        } catch (e: Exception) {
            DebugLog.add("service", "check error: ${e.javaClass.simpleName}")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Repo.init(this)
        detector = Detector(
            screenW = { resources.displayMetrics.widthPixels },
            screenH = { resources.displayMetrics.heightPixels },
        )
        overlays = Overlays(this)
        running = true
        DebugLog.serviceRunning = true
        DebugLog.add("service", "connected")
        Repo.markTracked()
        lastTickAt = SystemClock.elapsedRealtime()
        lastFlushAt = lastTickAt
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (!running) return
        running = false
        DebugLog.serviceRunning = false
        DebugLog.add("service", "stopped")
        handler.removeCallbacks(tick)
        handler.removeCallbacks(quickCheck)
        if (::overlays.isInitialized) overlays.hideAll()
        leaveAll(System.currentTimeMillis())
        Repo.flush()
    }

    override fun onInterrupt() {}

    // ------------------------------------------------------------------ events

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !running) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val app = Apps.byPackage(pkg)
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (app != null && Prefs.tracked(app.key)) onScroll(app, event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> scheduleQuickCheck(150)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (app != null) scheduleQuickCheck(400)
        }
    }

    private fun scheduleQuickCheck(delay: Long) {
        if (quickCheckPending) return
        quickCheckPending = true
        handler.postDelayed(quickCheck, delay)
    }

    private fun onScroll(app: WatchedApp, e: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        val viewId = try { e.source?.viewIdResourceName } catch (ex: Exception) { null }
        val isPlayer = detector.isPlayerScroll(app, viewId) ||
            (app.layoutDetect && counter.isVisible(app.key))
        DebugLog.lastScrollId = "${app.key}:${Apps.idName(viewId) ?: "?"}"
        if (!isPlayer) return
        lastPlayerScrollAt[app.key] = now
        if (!counter.isVisible(app.key)) {
            setVisible(app, true, now, "scroll in ${Apps.idName(viewId)}")
        }
        val pos = if (e.itemCount > 0 && e.fromIndex >= 0) e.fromIndex else null
        if (counter.onScroll(app.key, pos, now)) {
            countReel(app, now, if (pos != null) "swipe to #$pos" else "swipe")
        }
    }

    // ------------------------------------------------------------------ tick

    private fun onTick(fromEvent: Boolean) {
        if (!running) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val now = System.currentTimeMillis()
        val delta = if (fromEvent) 0L else (nowElapsed - lastTickAt).coerceIn(0L, 2_500L)
        if (!fromEvent) lastTickAt = nowElapsed

        if (Repo.ensureToday()) {
            Repo.markTracked()
            DebugLog.add("service", "new day")
        }

        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) {
            if (foreground != null || counter.visibleApp() != null) {
                leaveAll(now)
                overlays.hideAll()
            }
            maybeFlush(nowElapsed)
            return
        }

        val root = try { rootInActiveWindow } catch (e: Exception) { null }
        val pkg = root?.packageName?.toString()
        if (root == null || pkg == null || pkg == packageName || pkg in TRANSIENT) {
            // Our own overlay, the notification shade or a keyboard: keep the current state.
            maybeFlush(nowElapsed)
            return
        }

        val app = Apps.byPackage(pkg)?.takeIf { Prefs.tracked(it.key) }
        if (app?.key != foreground?.key) onForegroundChanged(foreground, app, now)
        DebugLog.foregroundApp = app?.key

        if (app == null) {
            maybeFlush(nowElapsed)
            return
        }

        // Is the player on screen? Facebook's layout check is heavier, so at most every 1.5 s.
        val wantLayout = app.layoutDetect || app.beta
        val look = if (wantLayout && fromEvent && nowElapsed - lastLayoutCheckAt < 1_500) {
            null
        } else {
            if (wantLayout) lastLayoutCheckAt = nowElapsed
            detector.look(app, root, pkg)
        }
        if (look != null) {
            DebugLog.lastHow = look.how
            if (look.visible == true && look.how != "none") idsSeen.add(app.key)
            val recentScroll = now - (lastPlayerScrollAt[app.key] ?: 0L) < SCROLL_STICKY_MS
            val visible = when {
                look.visible == true -> true
                // IDs have worked for this app: trust them.
                app.key in idsSeen -> false
                // IDs never matched (app update renamed them): keep the player open while the user swipes.
                else -> recentScroll && counter.isVisible(app.key)
            }
            if (visible) {
                misses[app.key] = 0
                if (!counter.isVisible(app.key)) setVisible(app, true, now, look.how)
                if (counter.onItemKey(app.key, look.itemKey, now)) countReel(app, now, "new item (${look.how})")
            } else if (counter.isVisible(app.key) && !fromEvent) {
                // Only the 1-second tick counts misses, so fast content changes during a swipe cannot close the player.
                val m = (misses[app.key] ?: 0) + 1
                misses[app.key] = m
                if (m >= 2) setVisible(app, false, now, "player gone")
            }
        }

        val inPlayer = counter.isVisible(app.key)
        if (delta > 0) Repo.recordTime(app.key, if (inPlayer) delta else 0L, delta)
        enforce(app, inPlayer, now)
        maybeFlush(nowElapsed)
    }

    private fun maybeFlush(nowElapsed: Long) {
        if (nowElapsed - lastFlushAt >= FLUSH_MS) {
            lastFlushAt = nowElapsed
            Repo.flush()
        }
    }

    private fun onForegroundChanged(old: WatchedApp?, new: WatchedApp?, now: Long) {
        if (old != null) {
            if (counter.isVisible(old.key)) setVisible(old, false, now, "left app")
            DebugLog.add(old.key, "left app")
        }
        if (new != null) DebugLog.add(new.key, "opened app")
        foreground = new
        if (new == null) {
            overlays.hideBubble()
            overlays.hideBlock()
            overlays.hidePause()
        }
        Repo.flush()
    }

    private fun setVisible(app: WatchedApp, visible: Boolean, now: Long, why: String) {
        when (counter.setVisible(app.key, visible, now)) {
            ReelCounter.Change.NEW_SESSION -> {
                DebugLog.add(app.key, "${app.feature} opened ($why)")
                Repo.recordSession(app.key)
                val t = Repo.todayTotals()
                if (blockReason(app, t.reels, t.reelMs, now) == null) {
                    countReel(app, now, "first reel")
                } else {
                    DebugLog.add(app.key, "blocked on open, not counted")
                }
            }
            ReelCounter.Change.RETURNED -> DebugLog.add(app.key, "${app.feature} back ($why)")
            ReelCounter.Change.HIDDEN -> {
                DebugLog.add(app.key, "${app.feature} closed ($why)")
                misses[app.key] = 0
                overlays.hideBubble()
                overlays.hideBlock()
                overlays.hidePause()
            }
            ReelCounter.Change.NONE -> {}
        }
        DebugLog.surfaceApp = counter.visibleApp()
    }

    private fun leaveAll(now: Long) {
        val hidden = counter.hideAll(now)
        hidden.forEach { DebugLog.add(it, "player closed (screen off / left)") }
        foreground = null
        DebugLog.surfaceApp = null
        DebugLog.foregroundApp = null
    }

    private fun countReel(app: WatchedApp, now: Long, why: String) {
        Repo.recordReel(app.key, now)
        val t = Repo.todayTotals()
        DebugLog.add(app.key, "+1 reel ($why) → ${t.reelsByApp[app.key] ?: 0} today")
        maybeWarn(t.reels, t.reelMs, app)
    }

    // ------------------------------------------------------------------ limits

    private fun maybeWarn(reels: Int, reelMs: Long, app: WatchedApp) {
        if (!Prefs.warnings) return
        val limit = Prefs.reelLimit
        val timeLimitMs = Prefs.timeLimitMin * 60_000L
        val pct = maxOf(
            if (limit > 0) reels.toFloat() / limit else 0f,
            if (timeLimitMs > 0) reelMs.toFloat() / timeLimitMs else 0f,
        )
        if (pct >= 1f && !Prefs.warnedToday(2)) {
            Prefs.markWarned(2); Prefs.markWarned(1)
            val msg = "Daily limit reached: $reels reels · ${Fmt.duration(reelMs)}"
            overlays.showBanner(msg)
            Notifier.showLimitReached(this, msg)
        } else if (pct >= 0.8f && !Prefs.warnedToday(1)) {
            Prefs.markWarned(1)
            val left = (limit - reels).coerceAtLeast(0)
            overlays.showBanner("Heads up: $left reels left today (${app.feature})")
        }
    }

    /** Why the player should be blocked right now, or null. */
    private fun blockReason(app: WatchedApp, reels: Int, reelMs: Long, now: Long): Pair<String, String>? {
        if (now < Prefs.snoozeUntil) return null
        val schedule = Prefs.activeSchedule(app.key)
        if (schedule != null) {
            return "sched-${schedule.id}" to "${schedule.name} is on until ${Fmt.clock(schedule.endMin)}."
        }
        if (Prefs.blockAlways(app.key)) {
            return "always-${app.key}" to "You blocked ${app.label} ${app.feature}."
        }
        if (Prefs.blockOnLimit && Prefs.blockOnLimitFor(app.key)) {
            val limit = Prefs.reelLimit
            val timeLimit = Prefs.timeLimitMin
            if (limit > 0 && reels >= limit) {
                return "limit-reels" to "You reached your limit of $limit reels for today."
            }
            if (timeLimit > 0 && reelMs >= timeLimit * 60_000L) {
                return "limit-time" to "You reached your limit of ${Fmt.duration(timeLimit * 60_000L)} in reels today."
            }
        }
        return null
    }

    private fun enforce(app: WatchedApp, inPlayer: Boolean, now: Long) {
        if (!inPlayer) {
            overlays.hideBubble()
            overlays.hideBlock()
            overlays.hidePause()
            return
        }
        val t = Repo.todayTotals()
        val reason = blockReason(app, t.reels, t.reelMs, now)
        if (reason != null) {
            val health = Brain.health(t.reels, t.reelMs, Prefs.reelLimit, Prefs.timeLimitMin)
            val snoozes = Prefs.snoozesLeftToday
            val snoozeLabel = if (snoozes > 0) "${Prefs.snoozeMinutes} more minutes ($snoozes left today)" else null
            overlays.showBlock(
                key = reason.first,
                title = "Time to stop",
                message = reason.second,
                stats = "Today: ${t.reels} reels · ${Fmt.duration(t.reelMs)}",
                health = health,
                snoozeLabel = snoozeLabel,
                onLeave = { leavePlayer(app) },
                onSnooze = {
                    Prefs.useSnooze(System.currentTimeMillis())
                    DebugLog.add(app.key, "snoozed ${Prefs.snoozeMinutes} min")
                    overlays.hideBlock()
                },
            )
            return
        }
        overlays.hideBlock()

        if (Prefs.pauseBeforeReels && !counter.pauseCleared(app.key)) {
            overlays.showPause(
                appKey = app.key,
                appFeature = app.feature,
                seconds = Prefs.pauseSeconds,
                onContinue = {
                    counter.clearPause(app.key)
                    overlays.hidePause()
                },
                onLeave = {
                    counter.clearPause(app.key)
                    overlays.hidePause()
                    leavePlayer(app)
                },
            )
            return
        }

        if (Prefs.liveCounter && overlays.pauseShowingFor == null) {
            val mine = t.reelsByApp[app.key] ?: 0
            overlays.showBubble("🧠 ${t.reels}/${Prefs.reelLimit} reels · ${Fmt.duration(t.reelMs)}" +
                if (mine != t.reels) "  ($mine here)" else "")
        } else {
            overlays.hideBubble()
        }
    }

    /** Back out of the player; if that does not work, go to the home screen. */
    private fun leavePlayer(app: WatchedApp) {
        if (leaving) return
        leaving = true
        overlays.hideBlock()
        overlays.hidePause()
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            val root = try { rootInActiveWindow } catch (e: Exception) { null }
            val pkg = root?.packageName?.toString()
            val still = root != null && pkg != null && Apps.byPackage(pkg)?.key == app.key &&
                detector.look(app, root, pkg).visible == true
            if (still) {
                DebugLog.add(app.key, "back did not leave the player, going home")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            leaving = false
        }, 900)
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val FLUSH_MS = 10_000L
        /** After a swipe in the player, trust it is open for this long even if IDs are not found. */
        private const val SCROLL_STICKY_MS = 8_000L

        private val TRANSIENT = setOf(
            "com.android.systemui",
            "com.samsung.android.honeyboard",
            "com.samsung.android.app.cocktailbarservice",
            "com.google.android.inputmethod.latin",
            "com.sec.android.inputmethod",
            "com.touchtype.swiftkey",
        )

        /** True while the service is connected (set on the same process). */
        @Volatile var running: Boolean = false
            private set
    }
}
