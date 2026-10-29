package com.jayv.dontscroll.data

import java.time.LocalDate

data class AppStat(
    val app: WatchedApp,
    val reels: Int,
    val reelMs: Long,
    val appMs: Long,
    val sessions: Int,
)

class DayStats(
    val day: LocalDate,
    /** Reels counted in each hour, 0..23. */
    val hourly: IntArray,
    val apps: List<AppStat>,
) {
    val reels: Int get() = apps.sumOf { it.reels }
    val reelMs: Long get() = apps.sumOf { it.reelMs }
    val appMs: Long get() = apps.sumOf { it.appMs }
    fun reelsBetween(fromHour: Int, toHourExclusive: Int): Int {
        var n = 0
        for (h in fromHour until toHourExclusive.coerceAtMost(24)) n += hourly[h]
        return n
    }
}

data class DaySummary(val day: LocalDate, val reels: Int, val reelMs: Long)

/** One row of the days table: the budget that applied that day and the chosen challenge. */
data class DayMeta(
    val day: LocalDate,
    val budget: Int,
    val timeLimitMin: Int,
    val challenge: String?,
    val tracked: Boolean,
)

data class TodayTotals(
    val reels: Int,
    val reelMs: Long,
    val reelsByApp: Map<String, Int>,
    val reelMsByApp: Map<String, Long>,
)

object Brain {
    /** 100 = sharp, 0 = brain rot. Uses the stricter of the reel and time limits. */
    fun health(reels: Int, reelMs: Long, reelLimit: Int, timeLimitMin: Int): Int {
        val byReels = if (reelLimit > 0) 100 - reels * 100 / reelLimit else 100
        val byTime = if (timeLimitMin > 0) (100 - reelMs * 100 / (timeLimitMin * 60_000L)).toInt() else 100
        return minOf(byReels, byTime).coerceIn(0, 100)
    }

    fun label(health: Int): String = when {
        health >= 80 -> "Sharp"
        health >= 50 -> "Okay"
        health >= 20 -> "Foggy"
        else -> "Brain rot"
    }

    fun line(health: Int): String = when {
        health >= 80 -> "Your brain feels great. Keep it that way."
        health >= 50 -> "Doing okay. A few more reels and it gets foggy."
        health >= 20 -> "Your brain is getting foggy. Time for a break."
        else -> "Brain rot reached. Put the phone down for a bit."
    }
}

object Fmt {
    fun duration(ms: Long): String {
        val totalSec = ms / 1000
        if (totalSec < 60) return if (totalSec <= 0) "0m" else "${totalSec}s"
        val totalMin = totalSec / 60
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) (if (m > 0) "${h}h ${m}m" else "${h}h") else "${m}m"
    }

    fun clock(minutesOfDay: Int): String {
        val h = (minutesOfDay / 60) % 24
        val m = minutesOfDay % 60
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, ampm)
    }
}
