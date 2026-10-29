package com.jayv.dontscroll.service

/**
 * Turns signals from a short-video player into "one more reel".
 *
 * - Opening the player counts the reel on screen, unless the user was away for less
 *   than [reEntryGraceMs] (comments, share sheet, a quick look at another app).
 * - A scroll with a list position counts only when the position is higher than any
 *   seen this session, so scrolling back or a cancelled swipe never counts twice.
 * - A scroll without a position: one swipe sends a burst of events, so the first event
 *   after [burstGapMs] of quiet counts, at most once per [minIntervalMs].
 * - A new item on screen (checked every second) counts when no scroll counted it.
 */
class ReelCounter(
    private val reEntryGraceMs: Long = 30_000,
    private val burstGapMs: Long = 600,
    private val minIntervalMs: Long = 800,
    private val itemGraceMs: Long = 1_500,
) {
    private class Session {
        var visible = false
        var visibleSince = 0L
        var hiddenAt = Long.MIN_VALUE
        var sessionStart = 0L
        var maxPosition = -1
        var lastScrollAt = Long.MIN_VALUE
        var lastCountAt = Long.MIN_VALUE
        var lastItemKey: String? = null
        var pauseCleared = false
    }

    enum class Change { NONE, NEW_SESSION, RETURNED, HIDDEN }

    private val sessions = HashMap<String, Session>()

    private fun s(app: String) = sessions.getOrPut(app) { Session() }

    fun isVisible(app: String) = sessions[app]?.visible == true

    fun visibleApp(): String? = sessions.entries.firstOrNull { it.value.visible }?.key

    fun sessionStart(app: String) = sessions[app]?.sessionStart ?: 0L

    fun pauseCleared(app: String) = sessions[app]?.pauseCleared == true

    fun clearPause(app: String) { s(app).pauseCleared = true }

    /**
     * Records whether the player is on screen. Returns what changed;
     * [Change.NEW_SESSION] means the reel on screen should be counted.
     */
    fun setVisible(app: String, visible: Boolean, now: Long): Change {
        val s = s(app)
        if (visible == s.visible) return Change.NONE
        return if (visible) {
            s.visible = true
            s.visibleSince = now
            s.lastScrollAt = Long.MIN_VALUE
            s.lastItemKey = null
            val quickReturn = s.hiddenAt != Long.MIN_VALUE && now - s.hiddenAt in 0 until reEntryGraceMs
            if (quickReturn) {
                Change.RETURNED
            } else {
                s.maxPosition = -1
                s.sessionStart = now
                s.lastCountAt = now
                s.pauseCleared = false
                Change.NEW_SESSION
            }
        } else {
            s.visible = false
            s.hiddenAt = now
            Change.HIDDEN
        }
    }

    /** A scroll in the player. [position] is null when the app does not report one. */
    fun onScroll(app: String, position: Int?, now: Long): Boolean {
        val s = sessions[app] ?: return false
        if (!s.visible) return false
        val previous = s.lastScrollAt
        s.lastScrollAt = now
        if (position != null && position >= 0) {
            if (s.maxPosition < 0) {
                // The first position seen belongs to the reel counted when the player opened.
                s.maxPosition = position
                return false
            }
            if (position <= s.maxPosition) return false
            s.maxPosition = position
            s.lastCountAt = now
            return true
        }
        val newBurst = previous == Long.MIN_VALUE || now - previous >= burstGapMs
        val spaced = s.lastCountAt == Long.MIN_VALUE || now - s.lastCountAt >= minIntervalMs
        if (newBurst && spaced) {
            s.lastCountAt = now
            return true
        }
        return false
    }

    /** The item filling the player changed. Counts only if a scroll did not just count it. */
    fun onItemKey(app: String, key: String?, now: Long): Boolean {
        val s = sessions[app] ?: return false
        if (!s.visible || key == null) return false
        val previous = s.lastItemKey
        s.lastItemKey = key
        if (previous == null || previous == key) return false
        if (s.lastCountAt != Long.MIN_VALUE && now - s.lastCountAt < itemGraceMs) return false
        if (s.lastScrollAt != Long.MIN_VALUE && now - s.lastScrollAt < itemGraceMs && s.maxPosition >= 0) return false
        s.lastCountAt = now
        return true
    }

    fun hideAll(now: Long): List<String> {
        val hidden = sessions.filter { it.value.visible }.keys.toList()
        hidden.forEach { setVisible(it, false, now) }
        return hidden
    }
}
