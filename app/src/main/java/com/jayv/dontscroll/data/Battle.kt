package com.jayv.dontscroll.data

import java.time.LocalDate
import java.time.LocalDateTime

enum class Challenge(val id: String, val title: String, val desc: String, val xp: Int) {
    HALF_BUDGET("half", "Half Budget", "Stay under half of your reel budget today", 40),
    CLEAN_MORNING("morning", "Clean Morning", "No reels before 12 PM", 25),
    NIGHT_SHIELD("night", "Night Shield", "No reels after 10 PM", 25),
    TIME_CAP("time", "15-Minute Cap", "Less than 15 minutes in reels today", 30),
    ZERO_DAY("zero", "Zero Reels Day", "Watch no reels at all today", 60);

    companion object {
        fun byId(id: String?): Challenge? = entries.firstOrNull { it.id == id }
    }
}

enum class Outcome { IN_PROGRESS, DONE, FAILED }

enum class BattleResult { FIGHTING, WON, LOST, NOT_TRACKED }

data class BattleDay(
    val day: LocalDate,
    val reels: Int,
    val reelMs: Long,
    val budget: Int,
    val result: BattleResult,
    val challenge: Challenge?,
    val challengeOutcome: Outcome?,
)

data class Level(val number: Int, val name: String, val minXp: Int, val nextXp: Int?)

data class BattleOverview(
    val today: BattleDay,
    val todayStats: DayStats,
    val youHp: Int,
    val monsterHp: Int,
    val last7: List<BattleDay>,
    val streak: Int,
    val bestStreak: Int,
    val wins: Int,
    val losses: Int,
    val xp: Int,
    val level: Level,
)

object BattleEngine {
    const val WIN_XP = 25

    private val levels = listOf(
        0 to "Rookie",
        100 to "Focused",
        250 to "Disciplined",
        500 to "Monk Mode",
        900 to "Legend",
        1500 to "Brain Master",
    )

    fun level(xp: Int): Level {
        var idx = 0
        for (i in levels.indices) if (xp >= levels[i].first) idx = i
        val next = if (idx + 1 < levels.size) levels[idx + 1].first else null
        return Level(idx + 1, levels[idx].second, levels[idx].first, next)
    }

    fun challengeOutcome(ch: Challenge, stats: DayStats, budget: Int, isToday: Boolean, now: LocalDateTime): Outcome {
        val reels = stats.reels
        return when (ch) {
            Challenge.ZERO_DAY ->
                if (reels > 0) Outcome.FAILED else if (isToday) Outcome.IN_PROGRESS else Outcome.DONE
            Challenge.CLEAN_MORNING -> {
                val morning = stats.reelsBetween(0, 12)
                if (morning > 0) Outcome.FAILED
                else if (isToday && now.hour < 12) Outcome.IN_PROGRESS
                else Outcome.DONE
            }
            Challenge.NIGHT_SHIELD -> {
                val night = stats.reelsBetween(22, 24)
                if (night > 0) Outcome.FAILED else if (isToday) Outcome.IN_PROGRESS else Outcome.DONE
            }
            Challenge.HALF_BUDGET ->
                if (reels > budget / 2) Outcome.FAILED else if (isToday) Outcome.IN_PROGRESS else Outcome.DONE
            Challenge.TIME_CAP ->
                if (stats.reelMs >= 15 * 60_000L) Outcome.FAILED else if (isToday) Outcome.IN_PROGRESS else Outcome.DONE
        }
    }

    private fun result(reels: Int, budget: Int, tracked: Boolean, isToday: Boolean): BattleResult = when {
        reels > budget -> BattleResult.LOST
        isToday -> BattleResult.FIGHTING
        !tracked -> BattleResult.NOT_TRACKED
        else -> BattleResult.WON
    }

    /** Builds the whole Battle tab from local data. Looks back up to a year. */
    fun overview(now: LocalDateTime = LocalDateTime.now()): BattleOverview {
        val today = now.toLocalDate()
        val first = Repo.firstTrackedDay() ?: today
        val start = maxOf(first, today.minusDays(364))
        val count = (today.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)
        val metas = Repo.dayMetas(start, today)
        val summaries = Repo.range(today, count)

        val days = summaries.map { s ->
            val meta = metas[s.day]
            val isToday = s.day == today
            val budget = if (isToday) Prefs.reelLimit else meta?.budget ?: Prefs.reelLimit
            val tracked = isToday || meta?.tracked == true
            val ch = Challenge.byId(meta?.challenge)
            val outcome = if (ch != null && (tracked || isToday)) {
                challengeOutcome(ch, Repo.dayStats(s.day), budget, isToday, now)
            } else null
            BattleDay(s.day, s.reels, s.reelMs, budget, result(s.reels, budget, tracked, isToday), ch, outcome)
        }

        // Streak: wins in a row ending yesterday, plus today while it is not lost.
        var streak = 0
        for (i in days.indices.reversed()) {
            val d = days[i]
            if (d.day == today) {
                if (d.result == BattleResult.LOST) break else continue
            }
            if (d.result == BattleResult.WON) streak++ else break
        }
        var best = 0
        var run = 0
        for (d in days) {
            if (d.result == BattleResult.WON) { run++; best = maxOf(best, run) }
            else if (d.day != today) run = 0
        }
        best = maxOf(best, streak)

        val wins = days.count { it.result == BattleResult.WON }
        val losses = days.count { it.result == BattleResult.LOST }
        val xp = days.sumOf { d ->
            (if (d.result == BattleResult.WON) WIN_XP else 0) +
                (if (d.challengeOutcome == Outcome.DONE) d.challenge?.xp ?: 0 else 0)
        }

        val todayDay = days.last()
        val todayStats = Repo.dayStats(today)
        val budget = Prefs.reelLimit.coerceAtLeast(1)
        val youHp = if (todayDay.result == BattleResult.LOST) 0
        else (100 - todayDay.reels * 100 / budget).coerceIn(0, 100)
        val minutesPassed = now.hour * 60 + now.minute
        val monsterHp = if (todayDay.result == BattleResult.LOST) 100
        else (100 - minutesPassed * 100 / (24 * 60)).coerceIn(1, 100)

        val last7 = days.takeLast(7).let { list ->
            if (list.size >= 7) list
            else {
                val missing = 7 - list.size
                val pad = (missing downTo 1).map { i ->
                    val d = list.first().day.minusDays(i.toLong())
                    BattleDay(d, 0, 0, budget, BattleResult.NOT_TRACKED, null, null)
                }
                pad + list
            }
        }

        return BattleOverview(
            today = todayDay,
            todayStats = todayStats,
            youHp = youHp,
            monsterHp = monsterHp,
            last7 = last7,
            streak = streak,
            bestStreak = best,
            wins = wins,
            losses = losses,
            xp = xp,
            level = level(xp),
        )
    }
}
