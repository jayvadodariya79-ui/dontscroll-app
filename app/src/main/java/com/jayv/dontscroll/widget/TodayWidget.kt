package com.jayv.dontscroll.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jayv.dontscroll.MainActivity
import com.jayv.dontscroll.R
import com.jayv.dontscroll.data.Meter
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo

/** Home-screen widget: reels and time today, plus budget left. */
class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repo.init(context)
        render(context, manager, ids)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
            if (ids.isEmpty()) return
            render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val t = Repo.todayTotals()
            val left = Meter.left(t.reels, t.reelMs, Prefs.reelLimit, Prefs.timeLimitMin)
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            for (id in ids) {
                val v = RemoteViews(context.packageName, R.layout.widget_today)
                v.setTextViewText(R.id.w_reels, t.reels.toString())
                v.setTextViewText(R.id.w_time, Fmt.duration(t.reelMs))
                v.setTextViewText(R.id.w_status, "$left% left · ${Meter.label(left)}")
                v.setOnClickPendingIntent(R.id.w_root, open)
                manager.updateAppWidget(id, v)
            }
        }
    }
}
