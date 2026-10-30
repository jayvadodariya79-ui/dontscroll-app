package com.jayv.dontscroll

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import com.jayv.dontscroll.data.Db
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Random

/**
 * Debug builds only. Replaces all data with four weeks of sample usage so store
 * screenshots show a lived-in app:
 *   adb shell am broadcast -a com.jayv.swipestop.DEMO_SEED -p com.jayv.swipestop
 */
class DemoSeedReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        Repo.init(ctx)
        Prefs.onboarded = true
        Prefs.reelLimit = 60
        Prefs.timeLimitMin = 45

        val rnd = Random(7)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val nowHour = LocalTime.now().hour

        // When people scroll: a little in the morning, lunch, and a late-night peak.
        val hourWeight = intArrayOf(2, 1, 0, 0, 0, 0, 0, 2, 5, 4, 2, 2, 4, 7, 5, 3, 3, 4, 5, 6, 8, 10, 12, 7)
        val apps = listOf("instagram" to 52, "youtube" to 31, "snapchat" to 11, "facebook" to 6)

        val helper = Db(ctx)
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM reels")
            db.execSQL("DELETE FROM usage")
            db.execSQL("DELETE FROM days")
            for (back in 27 downTo 0) {
                val day = today.minusDays(back.toLong())
                val reels = when {
                    back == 0 -> 38
                    back <= 9 -> 31 + rnd.nextInt(26)              // nine days under 60: a streak
                    else -> 70 + rnd.nextInt(60) + (back - 10) * 6 // before: well over budget
                }
                db.insert("days", null, ContentValues().apply {
                    put("day", day.toString())
                    put("budget", 60)
                    put("time_limit", 45)
                    put("tracked", 1)
                })

                val lastHour = if (back == 0) nowHour else 23
                val weights = hourWeight.mapIndexed { h, w -> if (h <= lastHour) w else 0 }
                val total = weights.sum().coerceAtLeast(1)
                val perApp = HashMap<String, Int>()
                repeat(reels) {
                    var pick = rnd.nextInt(total)
                    var hour = 0
                    while (pick >= weights[hour]) { pick -= weights[hour]; hour++ }
                    var a = rnd.nextInt(100)
                    var app = apps.last().first
                    for ((k, w) in apps) { if (a < w) { app = k; break }; a -= w }
                    val minute = if (back == 0 && hour == nowHour) rnd.nextInt(LocalTime.now().minute + 1) else rnd.nextInt(60)
                    val ts = day.atTime(hour, minute, rnd.nextInt(60)).atZone(zone).toInstant().toEpochMilli()
                    db.insert("reels", null, ContentValues().apply {
                        put("ts", ts); put("day", day.toString()); put("hour", hour); put("app", app)
                    })
                    perApp[app] = (perApp[app] ?: 0) + 1
                }
                for ((app, n) in perApp) {
                    val reelMs = n * (11_000L + rnd.nextInt(7_000))
                    db.insert("usage", null, ContentValues().apply {
                        put("day", day.toString()); put("app", app)
                        put("reel_ms", reelMs)
                        put("app_ms", reelMs * 17 / 10)
                        put("sessions", (n / 9).coerceAtLeast(1))
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            helper.close()
        }
        Repo.reload()
    }
}
