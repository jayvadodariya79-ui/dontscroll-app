package com.jayv.dontscroll.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jayv.dontscroll.MainActivity
import com.jayv.dontscroll.R
import com.jayv.dontscroll.data.Brain
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import java.time.LocalDateTime
import java.time.ZoneId

object Notifier {
    const val CH_SUMMARY = "summary"
    const val CH_ALERTS = "alerts"
    private const val ID_SUMMARY = 1001
    private const val ID_LIMIT = 1002

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_SUMMARY, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Your reels and brain score each evening"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, "Limit alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When you reach your daily limit"
            }
        )
    }

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @android.annotation.SuppressLint("MissingPermission")
    fun showSummary(ctx: Context) {
        if (!canNotify(ctx)) return
        val t = Repo.todayTotals()
        val health = Brain.health(t.reels, t.reelMs, Prefs.reelLimit, Prefs.timeLimitMin)
        val title = if (t.reels <= Prefs.reelLimit) "You beat the Scroll Monster today 💪" else "The Scroll Monster won today"
        val body = "${t.reels} reels · ${Fmt.duration(t.reelMs)} in reels · Brain ${health}% (${Brain.label(health)})"
        val n = NotificationCompat.Builder(ctx, CH_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_SUMMARY, n)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun showLimitReached(ctx: Context, text: String) {
        if (!canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily limit reached")
            .setContentText(text)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_LIMIT, n)
    }

    /** Schedules the next nightly summary (inexact, battery friendly). */
    fun scheduleSummary(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 2001,
            Intent(ctx, SummaryReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        if (!Prefs.summaryEnabled) return
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atStartOfDay().plusMinutes(Prefs.summaryAt.toLong())
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}

class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        Repo.flush()
        if (Prefs.summaryEnabled) Notifier.showSummary(context)
        Notifier.scheduleSummary(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        Notifier.scheduleSummary(context)
    }
}
