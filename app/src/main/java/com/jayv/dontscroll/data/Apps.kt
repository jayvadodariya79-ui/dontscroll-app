package com.jayv.dontscroll.data

/**
 * One app DontScroll watches, and how to recognise its short-video player.
 * View IDs are the part after ":id/". Apps rename them in updates, so old and new
 * names sit side by side here; this file is the only place to update them.
 */
data class WatchedApp(
    val key: String,
    val label: String,
    /** What the app calls its short videos: Reels, Shorts, Spotlight. */
    val feature: String,
    val packages: Set<String>,
    /** Any of these on screen, roughly full screen, means the player is open. */
    val surfaceIds: List<String>,
    /** Vertical pagers whose scroll events mean "next reel". */
    val pagerIds: List<String>,
    /** Fallback: a scrolling view whose ID contains one of these is the player. */
    val idKeywords: List<String>,
    /** No stable IDs: also recognise the player by its layout (action rail on the right). */
    val layoutDetect: Boolean = false,
    val color: Long,
    /** Detection is still being tuned for this app. */
    val beta: Boolean = false,
)

object Apps {
    val INSTAGRAM = WatchedApp(
        key = "instagram",
        label = "Instagram",
        feature = "Reels",
        packages = setOf("com.instagram.android"),
        surfaceIds = listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container"),
        pagerIds = listOf("clips_viewer_view_pager"),
        idKeywords = listOf("clips_viewer"),
        color = 0xFFE1306C,
    )

    val YOUTUBE = WatchedApp(
        key = "youtube",
        label = "YouTube",
        feature = "Shorts",
        packages = setOf("com.google.android.youtube", "app.revanced.android.youtube"),
        surfaceIds = listOf("reel_recycler", "reel_player_page_container", "reel_watch_fragment_root", "reel_watch_player"),
        pagerIds = listOf("reel_recycler"),
        idKeywords = listOf("reel_recycler", "reel_watch"),
        color = 0xFFFF0033,
    )

    val SNAPCHAT = WatchedApp(
        key = "snapchat",
        label = "Snapchat",
        feature = "Spotlight",
        packages = setOf("com.snapchat.android"),
        surfaceIds = listOf("spotlight_container", "spotlight_view_pager", "spotlight_feed"),
        pagerIds = listOf("spotlight_container", "spotlight_view_pager", "spotlight_feed"),
        idKeywords = listOf("spotlight"),
        color = 0xFFFFFC00,
        beta = true,
    )

    val FACEBOOK = WatchedApp(
        key = "facebook",
        label = "Facebook",
        feature = "Reels",
        packages = setOf("com.facebook.katana"),
        surfaceIds = emptyList(),
        pagerIds = emptyList(),
        idKeywords = listOf("reel"),
        layoutDetect = true,
        color = 0xFF1877F2,
        beta = true,
    )

    val all: List<WatchedApp> = listOf(INSTAGRAM, YOUTUBE, SNAPCHAT, FACEBOOK)

    private val byPkg: Map<String, WatchedApp> = all.flatMap { app -> app.packages.map { it to app } }.toMap()

    fun byPackage(pkg: String?): WatchedApp? = if (pkg == null) null else byPkg[pkg]

    fun byKey(key: String?): WatchedApp? = all.firstOrNull { it.key == key }

    /** "com.instagram.android:id/clips_viewer_view_pager" -> "clips_viewer_view_pager". */
    fun idName(viewId: String?): String? {
        if (viewId.isNullOrEmpty()) return null
        val i = viewId.indexOf(":id/")
        return if (i >= 0) viewId.substring(i + 4) else viewId
    }
}
