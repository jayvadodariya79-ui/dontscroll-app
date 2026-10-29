package com.jayv.dontscroll.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.WatchedApp

/** Result of one look at the screen. */
data class Look(
    /** true = player on screen, false = not, null = could not tell. */
    val visible: Boolean?,
    /** Changes with every reel; null when unknown. */
    val itemKey: String?,
    val how: String,
)

/**
 * Finds the Reels/Shorts/Spotlight player in the current window.
 * Reads view IDs, sizes and positions; for Facebook also the labels of the
 * like/comment/share buttons, which is how its player is recognised.
 */
class Detector(private val screenW: () -> Int, private val screenH: () -> Int) {

    fun look(app: WatchedApp, root: AccessibilityNodeInfo, pkg: String): Look {
        // 1) Known view IDs.
        for (id in app.surfaceIds) {
            val nodes = try {
                root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
            } catch (e: Exception) {
                emptyList<AccessibilityNodeInfo>()
            }
            val node = nodes.firstOrNull { it.isVisibleToUser && isBig(it) }
            if (node != null) {
                val pager = if (id in app.pagerIds) node else findPager(app, root, pkg)
                return Look(true, pager?.let { itemKey(it) }, "id:$id")
            }
        }
        // 2) Facebook (and Snapchat as a fallback): the layout of a reel player.
        if (app.layoutDetect || app.beta) {
            val rail = railLook(root)
            if (rail != null) return rail
        }
        // 3) Nothing matched.
        return Look(if (app.surfaceIds.isEmpty() && !app.layoutDetect) null else false, null, "none")
    }

    private fun findPager(app: WatchedApp, root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        for (id in app.pagerIds) {
            val n = try {
                root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
            } catch (e: Exception) {
                emptyList<AccessibilityNodeInfo>()
            }.firstOrNull { it.isVisibleToUser }
            if (n != null) return n
        }
        return null
    }

    private fun isBig(n: AccessibilityNodeInfo): Boolean {
        val r = Rect()
        n.getBoundsInScreen(r)
        val w = screenW().coerceAtLeast(1)
        val h = screenH().coerceAtLeast(1)
        return r.width() >= w * 0.7f && r.height() >= h * 0.45f
    }

    /** Key of the item that fills most of the pager: its list position if known, else its description. */
    private fun itemKey(pager: AccessibilityNodeInfo): String? {
        val pb = Rect()
        pager.getBoundsInScreen(pb)
        var best: AccessibilityNodeInfo? = null
        var bestArea = 0
        val count = pager.childCount
        for (i in 0 until count) {
            val c = pager.getChild(i) ?: continue
            if (!c.isVisibleToUser) continue
            val r = Rect()
            c.getBoundsInScreen(r)
            if (!r.intersect(pb)) continue
            val area = r.width() * r.height()
            if (area > bestArea) { best = c; bestArea = area }
        }
        val item = best ?: return null
        val row = item.collectionItemInfo?.rowIndex ?: -1
        if (row >= 0) return "row:$row"
        val desc = firstDescription(item, 0)
        if (desc != null) return "desc:${desc.hashCode()}"
        return null
    }

    private fun firstDescription(n: AccessibilityNodeInfo, depth: Int): String? {
        val d = n.contentDescription?.toString()
        if (!d.isNullOrBlank() && d.length > 8) return d
        if (depth >= 3) return null
        val count = n.childCount.coerceAtMost(6)
        for (i in 0 until count) {
            val c = n.getChild(i) ?: continue
            val found = firstDescription(c, depth + 1)
            if (found != null) return found
        }
        return null
    }

    /**
     * A full-screen video with a column of action buttons (like, comment, share)
     * on the right edge. Used where the app has no stable view IDs.
     */
    private fun railLook(root: AccessibilityNodeInfo): Look? {
        val w = screenW().coerceAtLeast(1)
        val h = screenH().coerceAtLeast(1)
        val railMinX = w * 3 / 4
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0
        val actions = HashSet<String>()
        var bigSurface = false
        var fingerprint: String? = null
        val r = Rect()
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val n = queue.removeAt(0)
            visited++
            n.getBoundsInScreen(r)
            val label = (n.contentDescription?.toString() ?: n.text?.toString())?.lowercase()
            if (label != null && r.centerX() >= railMinX && r.width() < w / 4) {
                val word = ACTION_WORDS.firstOrNull { label.contains(it) }
                if (word != null) actions.add(word)
            }
            if (r.width() >= w * 0.95f && r.height() >= h * 0.6f && n.isVisibleToUser) {
                bigSurface = true
                val d = n.contentDescription?.toString()
                if (!d.isNullOrBlank() && d.length > 15 && fingerprint == null) fingerprint = d
            }
            val count = n.childCount
            for (i in 0 until count) {
                val c = n.getChild(i) ?: continue
                queue.addLast(c)
            }
        }
        if (actions.size >= 2 && bigSurface) {
            return Look(true, fingerprint?.let { "rail:${it.hashCode()}" }, "layout(${actions.joinToString("+")})")
        }
        return null
    }

    /** Is this scroll event from the player? */
    fun isPlayerScroll(app: WatchedApp, viewId: String?): Boolean {
        val name = Apps.idName(viewId) ?: return false
        if (name in app.pagerIds || name in app.surfaceIds) return true
        return app.idKeywords.any { name.contains(it) }
    }

    companion object {
        private const val MAX_NODES = 350
        private val ACTION_WORDS = listOf(
            "like", "comment", "share", "send", "remix", "save", "react",
            "पसंद", "टिप्पणी", "कमेंट", "शेयर",
        )
    }
}
