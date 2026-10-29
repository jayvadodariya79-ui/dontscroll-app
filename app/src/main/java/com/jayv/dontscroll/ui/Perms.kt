package com.jayv.dontscroll.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import com.jayv.dontscroll.service.ReelService

/** Permission checks and shortcuts into system settings. */
object Perms {
    fun component(ctx: Context) = ComponentName(ctx, ReelService::class.java)

    /** The user switched DontScroll on in Accessibility settings. */
    fun accessibilityEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = component(ctx)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** The service is actually connected and counting. */
    fun trackerRunning(): Boolean = ReelService.running

    fun openAccessibility(ctx: Context) {
        val flat = component(ctx).flattenToString()
        val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Samsung and Pixel highlight our entry with these.
            putExtra(":settings:fragment_args_key", flat)
            putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", flat) })
        }
        safeStart(ctx, i)
    }

    fun openAppInfo(ctx: Context) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(ctx, i)
    }

    fun ignoringBatteryOptimizations(ctx: Context): Boolean {
        val pm = ctx.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun openBatterySettings(ctx: Context) {
        val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!safeStart(ctx, i)) openAppInfo(ctx)
    }

    fun openNotificationSettings(ctx: Context) {
        val i = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
        }
        safeStart(ctx, i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    val isSamsung: Boolean get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    private fun safeStart(ctx: Context, i: Intent): Boolean = try {
        ctx.startActivity(i); true
    } catch (e: Exception) {
        false
    }
}
