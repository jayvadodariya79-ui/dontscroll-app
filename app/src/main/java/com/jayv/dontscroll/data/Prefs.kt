package com.jayv.dontscroll.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** A time window when Reels/Shorts are blocked, e.g. bedtime. Minutes are minutes after midnight. */
data class Schedule(
    val id: Long,
    val name: String,
    val startMin: Int,
    val endMin: Int,
    /** Bit 0 = Monday ... bit 6 = Sunday. */
    val days: Int,
    val enabled: Boolean,
    /** App keys this applies to; empty = all apps. */
    val apps: Set<String> = emptySet(),
) {
    fun appliesTo(appKey: String) = apps.isEmpty() || appKey in apps

    fun isActive(now: LocalDateTime): Boolean {
        if (!enabled || days == 0) return false
        val m = now.hour * 60 + now.minute
        val todayBit = 1 shl (now.dayOfWeek.value - 1)
        val yesterdayBit = 1 shl (now.minusDays(1).dayOfWeek.value - 1)
        return if (startMin == endMin) {
            (days and todayBit) != 0
        } else if (startMin < endMin) {
            ((days and todayBit) != 0) && m >= startMin && m < endMin
        } else {
            // Overnight, e.g. 23:00 -> 06:00
            (m >= startMin && (days and todayBit) != 0) || (m < endMin && (days and yesterdayBit) != 0)
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("start", startMin); put("end", endMin)
        put("days", days); put("enabled", enabled); put("apps", JSONArray(apps.toList()))
    }

    companion object {
        const val ALL_DAYS = 0b1111111

        fun fromJson(o: JSONObject): Schedule {
            val arr = o.optJSONArray("apps")
            val apps = mutableSetOf<String>()
            if (arr != null) for (i in 0 until arr.length()) apps.add(arr.getString(i))
            return Schedule(
                id = o.getLong("id"),
                name = o.optString("name", "Schedule"),
                startMin = o.optInt("start", 23 * 60),
                endMin = o.optInt("end", 6 * 60),
                days = o.optInt("days", ALL_DAYS),
                enabled = o.optBoolean("enabled", false),
                apps = apps,
            )
        }
    }
}

/** All user settings, stored in SharedPreferences. Nothing is synced anywhere. */
object Prefs {
    private lateinit var sp: SharedPreferences
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> get() = _version

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("dontscroll_settings", Context.MODE_PRIVATE)
    }

    private fun changed() { _version.value = _version.value + 1 }

    private fun int(key: String, def: Int) = sp.getInt(key, def)
    private fun bool(key: String, def: Boolean) = sp.getBoolean(key, def)
    private fun putInt(key: String, v: Int) { sp.edit().putInt(key, v).apply(); changed() }
    private fun putBool(key: String, v: Boolean) { sp.edit().putBoolean(key, v).apply(); changed() }

    var onboarded: Boolean
        get() = bool("onboarded", false)
        set(v) = putBool("onboarded", v)

    /** Daily reels budget across all apps. Also the daily goal on the Goals tab. */
    var reelLimit: Int
        get() = int("reel_limit", 50)
        set(v) = putInt("reel_limit", v.coerceIn(1, 1000))

    /** Daily minutes in Reels/Shorts across all apps. 0 = no time limit. */
    var timeLimitMin: Int
        get() = int("time_limit_min", 30)
        set(v) = putInt("time_limit_min", v.coerceIn(0, 600))

    /** Show the limit screen when a limit is reached. */
    var blockOnLimit: Boolean
        get() = bool("block_on_limit", true)
        set(v) = putBool("block_on_limit", v)

    fun tracked(appKey: String) = bool("tracked_$appKey", true)
    fun setTracked(appKey: String, v: Boolean) = putBool("tracked_$appKey", v)

    fun blockAlways(appKey: String) = bool("block_always_$appKey", false)
    fun setBlockAlways(appKey: String, v: Boolean) = putBool("block_always_$appKey", v)

    /** Apply limit blocking to this app (when [blockOnLimit] is on). */
    fun blockOnLimitFor(appKey: String) = bool("block_limit_$appKey", true)
    fun setBlockOnLimitFor(appKey: String, v: Boolean) = putBool("block_limit_$appKey", v)

    var snoozesPerDay: Int
        get() = int("snoozes_per_day", 2)
        set(v) = putInt("snoozes_per_day", v.coerceIn(0, 10))

    var snoozeMinutes: Int
        get() = int("snooze_minutes", 5)
        set(v) = putInt("snooze_minutes", v.coerceIn(1, 30))

    var liveCounter: Boolean
        get() = bool("live_counter", true)
        set(v) = putBool("live_counter", v)

    var pauseBeforeReels: Boolean
        get() = bool("pause_before", false)
        set(v) = putBool("pause_before", v)

    var pauseSeconds: Int
        get() = int("pause_seconds", 5)
        set(v) = putInt("pause_seconds", v.coerceIn(3, 30))

    var warnings: Boolean
        get() = bool("warnings", true)
        set(v) = putBool("warnings", v)

    var summaryEnabled: Boolean
        get() = bool("summary_enabled", true)
        set(v) = putBool("summary_enabled", v)

    /** Minutes after midnight for the nightly summary. */
    var summaryAt: Int
        get() = int("summary_at", 21 * 60 + 30)
        set(v) = putInt("summary_at", v.coerceIn(0, 24 * 60 - 1))

    // ---- Snooze state (resets every day) ----

    private fun todayKey() = LocalDate.now().toString()

    val snoozesUsedToday: Int
        get() = if (sp.getString("snooze_day", null) == todayKey()) int("snoozes_used", 0) else 0

    val snoozesLeftToday: Int get() = (snoozesPerDay - snoozesUsedToday).coerceAtLeast(0)

    val snoozeUntil: Long get() = sp.getLong("snooze_until", 0L)

    fun useSnooze(now: Long) {
        val used = snoozesUsedToday + 1
        sp.edit()
            .putString("snooze_day", todayKey())
            .putInt("snoozes_used", used)
            .putLong("snooze_until", now + snoozeMinutes * 60_000L)
            .apply()
        changed()
    }

    // ---- Warnings shown today (bit 0 = 80%, bit 1 = 100%) ----

    fun warnedToday(bit: Int): Boolean =
        sp.getString("warn_day", null) == todayKey() && (int("warn_bits", 0) and bit) != 0

    fun markWarned(bit: Int) {
        val bits = if (sp.getString("warn_day", null) == todayKey()) int("warn_bits", 0) else 0
        sp.edit().putString("warn_day", todayKey()).putInt("warn_bits", bits or bit).apply()
    }

    // ---- Schedules ----

    private var cachedSchedules: List<Schedule>? = null

    var schedules: List<Schedule>
        get() {
            cachedSchedules?.let { return it }
            val raw = sp.getString("schedules", null)
            val list = if (raw == null) {
                listOf(
                    Schedule(1, "Bedtime", 23 * 60, 6 * 60, Schedule.ALL_DAYS, enabled = false),
                    Schedule(2, "Focus hours", 10 * 60, 13 * 60, 0b0011111, enabled = false),
                )
            } else {
                try {
                    val arr = JSONArray(raw)
                    (0 until arr.length()).map { Schedule.fromJson(arr.getJSONObject(it)) }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            cachedSchedules = list
            return list
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(it.toJson()) }
            sp.edit().putString("schedules", arr.toString()).apply()
            cachedSchedules = v
            changed()
        }

    fun upsertSchedule(s: Schedule) {
        val list = schedules.toMutableList()
        val i = list.indexOfFirst { it.id == s.id }
        if (i >= 0) list[i] = s else list.add(s)
        schedules = list
    }

    fun deleteSchedule(id: Long) {
        schedules = schedules.filterNot { it.id == id }
    }

    fun nextScheduleId(): Long = (schedules.maxOfOrNull { it.id } ?: 0L) + 1

    fun activeSchedule(appKey: String, now: LocalDateTime = LocalDateTime.now()): Schedule? =
        schedules.firstOrNull { it.appliesTo(appKey) && it.isActive(now) }

    fun resetAll() {
        val keepOnboarded = onboarded
        sp.edit().clear().putBoolean("onboarded", keepOnboarded).apply()
        cachedSchedules = null
        changed()
    }
}
