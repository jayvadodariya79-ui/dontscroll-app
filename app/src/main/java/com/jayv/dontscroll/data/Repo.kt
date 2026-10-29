package com.jayv.dontscroll.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import com.jayv.dontscroll.widget.TodayWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The single place that reads and writes tracking data.
 * Reels are written to the database immediately; time is kept in memory and
 * written every few seconds by [flush], so the database is never hit every tick.
 */
object Repo {
    private lateinit var appContext: Context
    private lateinit var db: Db

    private val _version = MutableStateFlow(0L)
    /** Changes whenever data changes; screens reload when it does. */
    val version: StateFlow<Long> get() = _version

    private var today: LocalDate = LocalDate.now()

    // Totals for today: what is in the database plus what has not been flushed yet.
    private val todayReels = HashMap<String, Int>()
    private val todayReelMs = HashMap<String, Long>()
    private val todayAppMs = HashMap<String, Long>()

    // Not yet written to the database.
    private val pendingReelMs = HashMap<String, Long>()
    private val pendingAppMs = HashMap<String, Long>()
    private val pendingSessions = HashMap<String, Int>()

    private var lastWidgetUpdate = 0L

    @Synchronized
    fun init(ctx: Context) {
        if (::db.isInitialized) return
        appContext = ctx.applicationContext
        Prefs.init(appContext)
        db = Db(appContext)
        today = LocalDate.now()
        loadToday()
    }

    private fun bump() { _version.value = _version.value + 1 }

    private fun loadToday() {
        todayReels.clear(); todayReelMs.clear(); todayAppMs.clear()
        val d = today.toString()
        val r = db.readableDatabase
        r.rawQuery("SELECT app, COUNT(*) FROM reels WHERE day = ? GROUP BY app", arrayOf(d)).use { c ->
            while (c.moveToNext()) todayReels[c.getString(0)] = c.getInt(1)
        }
        r.rawQuery("SELECT app, reel_ms, app_ms FROM usage WHERE day = ?", arrayOf(d)).use { c ->
            while (c.moveToNext()) {
                todayReelMs[c.getString(0)] = c.getLong(1)
                todayAppMs[c.getString(0)] = c.getLong(2)
            }
        }
    }

    /** Call often; handles midnight. Returns true when the day changed. */
    @Synchronized
    fun ensureToday(): Boolean {
        val now = LocalDate.now()
        if (now == today) return false
        flushLocked()
        today = now
        loadToday()
        bump()
        return true
    }

    /** Marks today as a tracked day and stores the budget that applies to it. */
    @Synchronized
    fun markTracked() {
        val d = today.toString()
        val w = db.writableDatabase
        val cv = ContentValues().apply {
            put("day", d)
            put("budget", Prefs.reelLimit)
            put("time_limit", Prefs.timeLimitMin)
            put("tracked", 1)
        }
        val inserted = w.insertWithOnConflict("days", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        if (inserted == -1L) {
            w.execSQL("UPDATE days SET tracked = 1 WHERE day = ?", arrayOf(d))
        }
    }

    /** Keeps today's stored budget in step when the user changes limits. */
    @Synchronized
    fun syncTodayBudget() {
        val w = db.writableDatabase
        w.execSQL(
            "UPDATE days SET budget = ?, time_limit = ? WHERE day = ?",
            arrayOf<Any>(Prefs.reelLimit, Prefs.timeLimitMin, today.toString())
        )
        bump()
    }

    @Synchronized
    fun recordReel(appKey: String, ts: Long = System.currentTimeMillis()) {
        ensureToday()
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
        val cv = ContentValues().apply {
            put("ts", ts)
            put("day", today.toString())
            put("hour", t.hour)
            put("app", appKey)
        }
        db.writableDatabase.insert("reels", null, cv)
        todayReels[appKey] = (todayReels[appKey] ?: 0) + 1
        bump()
        maybeUpdateWidget()
    }

    @Synchronized
    fun recordTime(appKey: String, reelMs: Long, appMs: Long) {
        if (reelMs <= 0 && appMs <= 0) return
        if (reelMs > 0) {
            pendingReelMs[appKey] = (pendingReelMs[appKey] ?: 0L) + reelMs
            todayReelMs[appKey] = (todayReelMs[appKey] ?: 0L) + reelMs
        }
        if (appMs > 0) {
            pendingAppMs[appKey] = (pendingAppMs[appKey] ?: 0L) + appMs
            todayAppMs[appKey] = (todayAppMs[appKey] ?: 0L) + appMs
        }
    }

    @Synchronized
    fun recordSession(appKey: String) {
        pendingSessions[appKey] = (pendingSessions[appKey] ?: 0) + 1
    }

    @Synchronized
    fun flush() {
        if (flushLocked()) {
            bump()
            maybeUpdateWidget()
        }
    }

    private fun flushLocked(): Boolean {
        val keys = (pendingReelMs.keys + pendingAppMs.keys + pendingSessions.keys).toSet()
        if (keys.isEmpty()) return false
        val w = db.writableDatabase
        val d = today.toString()
        w.beginTransaction()
        try {
            for (k in keys) {
                w.execSQL("INSERT OR IGNORE INTO usage (day, app) VALUES (?, ?)", arrayOf(d, k))
                w.execSQL(
                    "UPDATE usage SET reel_ms = reel_ms + ?, app_ms = app_ms + ?, sessions = sessions + ? WHERE day = ? AND app = ?",
                    arrayOf<Any>(pendingReelMs[k] ?: 0L, pendingAppMs[k] ?: 0L, pendingSessions[k] ?: 0, d, k)
                )
            }
            w.setTransactionSuccessful()
        } finally {
            w.endTransaction()
        }
        pendingReelMs.clear(); pendingAppMs.clear(); pendingSessions.clear()
        return true
    }

    private fun maybeUpdateWidget() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWidgetUpdate < 15_000) return
        lastWidgetUpdate = now
        try {
            TodayWidget.updateAll(appContext)
        } catch (e: Exception) {
            // The widget is optional; never let it break tracking.
        }
    }

    @Synchronized
    fun todayTotals(): TodayTotals {
        ensureToday()
        return TodayTotals(
            reels = todayReels.values.sum(),
            reelMs = todayReelMs.values.sum(),
            reelsByApp = HashMap(todayReels),
            reelMsByApp = HashMap(todayReelMs),
        )
    }

    /** Everything for one day: per-app numbers and reels per hour. */
    @Synchronized
    fun dayStats(day: LocalDate): DayStats {
        ensureToday()
        val d = day.toString()
        val r = db.readableDatabase
        val hourly = IntArray(24)
        r.rawQuery("SELECT hour, COUNT(*) FROM reels WHERE day = ? GROUP BY hour", arrayOf(d)).use { c ->
            while (c.moveToNext()) {
                val h = c.getInt(0)
                if (h in 0..23) hourly[h] = c.getInt(1)
            }
        }
        val reels = HashMap<String, Int>()
        val reelMs = HashMap<String, Long>()
        val appMs = HashMap<String, Long>()
        val sessions = HashMap<String, Int>()
        if (day == today) {
            reels.putAll(todayReels); reelMs.putAll(todayReelMs); appMs.putAll(todayAppMs)
            r.rawQuery("SELECT app, sessions FROM usage WHERE day = ?", arrayOf(d)).use { c ->
                while (c.moveToNext()) sessions[c.getString(0)] = c.getInt(1)
            }
            pendingSessions.forEach { (k, v) -> sessions[k] = (sessions[k] ?: 0) + v }
        } else {
            r.rawQuery("SELECT app, COUNT(*) FROM reels WHERE day = ? GROUP BY app", arrayOf(d)).use { c ->
                while (c.moveToNext()) reels[c.getString(0)] = c.getInt(1)
            }
            r.rawQuery("SELECT app, reel_ms, app_ms, sessions FROM usage WHERE day = ?", arrayOf(d)).use { c ->
                while (c.moveToNext()) {
                    val k = c.getString(0)
                    reelMs[k] = c.getLong(1); appMs[k] = c.getLong(2); sessions[k] = c.getInt(3)
                }
            }
        }
        val apps = Apps.all.map { a ->
            AppStat(a, reels[a.key] ?: 0, reelMs[a.key] ?: 0L, appMs[a.key] ?: 0L, sessions[a.key] ?: 0)
        }
        return DayStats(day, hourly, apps)
    }

    /** Reels and time for [count] days ending at [endDay], oldest first. */
    @Synchronized
    fun range(endDay: LocalDate, count: Int): List<DaySummary> {
        ensureToday()
        val start = endDay.minusDays((count - 1).toLong())
        val r = db.readableDatabase
        val reels = HashMap<String, Int>()
        val ms = HashMap<String, Long>()
        r.rawQuery(
            "SELECT day, COUNT(*) FROM reels WHERE day >= ? AND day <= ? GROUP BY day",
            arrayOf(start.toString(), endDay.toString())
        ).use { c -> while (c.moveToNext()) reels[c.getString(0)] = c.getInt(1) }
        r.rawQuery(
            "SELECT day, SUM(reel_ms) FROM usage WHERE day >= ? AND day <= ? GROUP BY day",
            arrayOf(start.toString(), endDay.toString())
        ).use { c -> while (c.moveToNext()) ms[c.getString(0)] = c.getLong(1) }
        val todayKey = today.toString()
        val pendingToday = pendingReelMs.values.sum()
        return (0 until count).map { i ->
            val d = start.plusDays(i.toLong())
            val k = d.toString()
            val extra = if (k == todayKey) pendingToday else 0L
            DaySummary(d, reels[k] ?: 0, (ms[k] ?: 0L) + extra)
        }
    }

    /** Stored meta for days in a range (only days that have a row). */
    @Synchronized
    fun dayMetas(from: LocalDate, to: LocalDate): Map<LocalDate, DayMeta> {
        val out = HashMap<LocalDate, DayMeta>()
        db.readableDatabase.rawQuery(
            "SELECT day, budget, time_limit, challenge, tracked FROM days WHERE day >= ? AND day <= ?",
            arrayOf(from.toString(), to.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val d = LocalDate.parse(c.getString(0))
                out[d] = DayMeta(
                    day = d,
                    budget = c.getInt(1),
                    timeLimitMin = c.getInt(2),
                    challenge = if (c.isNull(3)) null else c.getString(3),
                    tracked = c.getInt(4) == 1,
                )
            }
        }
        return out
    }

    /** First day DontScroll was tracking, or null. */
    @Synchronized
    fun firstTrackedDay(): LocalDate? {
        val first: String? = db.readableDatabase.rawQuery("SELECT MIN(day) FROM days WHERE tracked = 1", null).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
        return first?.let { LocalDate.parse(it) }
    }

    @Synchronized
    fun setChallenge(day: LocalDate, challengeId: String?) {
        val w = db.writableDatabase
        val cv = ContentValues().apply {
            put("day", day.toString())
            put("budget", Prefs.reelLimit)
            put("time_limit", Prefs.timeLimitMin)
            put("tracked", 0)
        }
        w.insertWithOnConflict("days", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        w.execSQL("UPDATE days SET challenge = ? WHERE day = ?", arrayOf(challengeId, day.toString()))
        bump()
    }

    /** Writes every reel and daily total to a CSV file and returns it. */
    @Synchronized
    fun exportCsv(): File {
        flushLocked()
        val dir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "dontscroll-${LocalDate.now()}.csv")
        val r = db.readableDatabase
        file.bufferedWriter().use { out ->
            out.write("section,day,app,hour,timestamp,reels,reel_minutes,app_minutes,sessions\n")
            r.rawQuery("SELECT day, app, reel_ms, app_ms, sessions FROM usage ORDER BY day, app", null).use { c ->
                while (c.moveToNext()) {
                    val day = c.getString(0)
                    val app = c.getString(1)
                    val reels = r.rawQuery(
                        "SELECT COUNT(*) FROM reels WHERE day = ? AND app = ?", arrayOf(day, app)
                    ).use { q -> if (q.moveToFirst()) q.getInt(0) else 0 }
                    out.write("daily,$day,$app,,,$reels,${c.getLong(2) / 60000},${c.getLong(3) / 60000},${c.getInt(4)}\n")
                }
            }
            r.rawQuery("SELECT day, app, hour, ts FROM reels ORDER BY ts", null).use { c ->
                while (c.moveToNext()) {
                    out.write("reel,${c.getString(0)},${c.getString(1)},${c.getInt(2)},${c.getLong(3)},1,,,\n")
                }
            }
        }
        return file
    }

    @Synchronized
    fun resetAll() {
        val w = db.writableDatabase
        w.execSQL("DELETE FROM reels")
        w.execSQL("DELETE FROM usage")
        w.execSQL("DELETE FROM days")
        pendingReelMs.clear(); pendingAppMs.clear(); pendingSessions.clear()
        today = LocalDate.now()
        loadToday()
        bump()
        try { TodayWidget.updateAll(appContext) } catch (e: Exception) { }
    }
}
