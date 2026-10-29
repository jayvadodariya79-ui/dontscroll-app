package com.jayv.dontscroll.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.notify.Notifier
import com.jayv.dontscroll.ui.Perms
import com.jayv.dontscroll.ui.components.BackHeader
import com.jayv.dontscroll.ui.components.Divider
import com.jayv.dontscroll.ui.components.NavRow
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.StepperRow
import com.jayv.dontscroll.ui.components.SwitchRow
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val refresh = rememberRefreshKey()
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        BackHeader("Settings", onBack)

        SectionTitle("Limits")
        Panel(padding = 16.dp) {
            NavRow(
                "Limits, blocking & schedules",
                "${Prefs.reelLimit} reels" + if (Prefs.timeLimitMin > 0) " · ${Fmt.duration(Prefs.timeLimitMin * 60_000L)} a day" else " a day",
            ) { onOpen(Route.Blocking) }
        }

        SectionTitle("While you scroll")
        Panel(padding = 16.dp) {
            SwitchRow("Live counter", "Shows reels and time at the top of Reels/Shorts", Prefs.liveCounter) { Prefs.liveCounter = it }
            Divider()
            SwitchRow("Pause before reels", "A short breath each time you open Reels/Shorts", Prefs.pauseBeforeReels) { Prefs.pauseBeforeReels = it }
            if (Prefs.pauseBeforeReels) {
                StepperRow(
                    "Pause length", null, "${Prefs.pauseSeconds} s",
                    onMinus = { Prefs.pauseSeconds = Prefs.pauseSeconds - 1 },
                    onPlus = { Prefs.pauseSeconds = Prefs.pauseSeconds + 1 },
                )
            }
            Divider()
            SwitchRow("Warn me at 80%", null, Prefs.warnings) { Prefs.warnings = it }
        }

        SectionTitle("Notifications")
        Panel(padding = 16.dp) {
            SwitchRow("Nightly summary", "Your reels and brain score each evening", Prefs.summaryEnabled) {
                Prefs.summaryEnabled = it
                Notifier.scheduleSummary(ctx)
            }
            if (Prefs.summaryEnabled) {
                Divider()
                NavRow("Summary time", value = Fmt.clock(Prefs.summaryAt)) {
                    val at = Prefs.summaryAt
                    TimePickerDialog(ctx, { _, h, m ->
                        Prefs.summaryAt = h * 60 + m
                        Notifier.scheduleSummary(ctx)
                    }, at / 60, at % 60, false).show()
                }
            }
            Divider()
            NavRow("Notification permission", if (Notifier.canNotify(ctx)) "Allowed" else "Not allowed") {
                Perms.openNotificationSettings(ctx)
            }
        }

        SectionTitle("Keep tracking reliable")
        Panel(padding = 16.dp) {
            NavRow(
                "Accessibility",
                when {
                    Perms.trackerRunning() -> "On and counting"
                    Perms.accessibilityEnabled(ctx) -> "On, but not running"
                    else -> "Off"
                },
            ) { Perms.openAccessibility(ctx) }
            Divider()
            NavRow("Battery & Samsung tips", if (Perms.ignoringBatteryOptimizations(ctx)) "Battery unrestricted" else "Battery is optimised") {
                onOpen(Route.Reliability)
            }
            Divider()
            NavRow("Detection check", "See what the tracker sees, live") { onOpen(Route.Debug) }
        }

        SectionTitle("Your data")
        Panel(padding = 16.dp) {
            NavRow("Export as CSV", "Every reel and daily total") {
                try {
                    val file = Repo.exportCsv()
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(send, "Export DontScroll data"))
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            Divider()
            NavRow("Reset all data", "Deletes history, streaks and settings") { confirmReset = true }
        }

        SectionTitle("About")
        Panel(padding = 16.dp) {
            NavRow("Privacy & how it works") { onOpen(Route.About) }
        }
        Spacer(Modifier.height(12.dp))
        Text("DontScroll 0.1.0 · free · no account · no internet", fontSize = 12.sp, color = C.TextDim, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(28.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = C.Card,
            title = { Text("Reset everything?", color = C.Text) },
            text = { Text("This deletes all reels, history, streaks, XP and your settings on this phone. It cannot be undone.", color = C.TextLo) },
            confirmButton = {
                TextButton(onClick = {
                    Repo.resetAll()
                    Prefs.resetAll()
                    Notifier.scheduleSummary(ctx)
                    confirmReset = false
                }) { Text("Reset", color = C.Bad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = C.Accent) }
            },
        )
    }
}
