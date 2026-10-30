package com.jayv.dontscroll.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.data.Schedule
import com.jayv.dontscroll.ui.components.AppIcon
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
fun BlockingScreen(onBack: () -> Unit, onOpen: (Route) -> Unit) {
    // Read so the screen redraws when settings change.
    @Suppress("UNUSED_VARIABLE") val refresh = rememberRefreshKey()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        BackHeader("Limits & blocking", onBack)

        SectionTitle("Daily limits")
        Panel(padding = 18.dp) {
            LimitSlider(
                title = "Reels per day",
                value = Prefs.reelLimit,
                range = 5f..300f,
                step = 5,
                label = { "$it reels" },
                onDone = { Prefs.reelLimit = it; Repo.syncTodayBudget() },
            )
            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            LimitSlider(
                title = "Time in Reels per day",
                value = Prefs.timeLimitMin,
                range = 0f..180f,
                step = 5,
                label = { if (it == 0) "No time limit" else Fmt.duration(it * 60_000L) },
                onDone = { Prefs.timeLimitMin = it; Repo.syncTodayBudget() },
            )
            Spacer(Modifier.height(6.dp))
            Text("Both limits count all four apps together. Your reel limit is also your daily goal.", fontSize = 13.sp, color = C.TextLo)
        }

        SectionTitle("When I hit a limit")
        Panel(padding = 16.dp) {
            SwitchRow("Show the limit screen", "Covers Reels/Shorts with a stop screen", Prefs.blockOnLimit) { Prefs.blockOnLimit = it }
            Divider()
            StepperRow(
                "Snoozes per day",
                "\"${Prefs.snoozeMinutes} more minutes\" button, after a 10 s wait",
                if (Prefs.snoozesPerDay == 0) "None" else "${Prefs.snoozesPerDay}",
                onMinus = { Prefs.snoozesPerDay = Prefs.snoozesPerDay - 1 },
                onPlus = { Prefs.snoozesPerDay = Prefs.snoozesPerDay + 1 },
            )
            Divider()
            StepperRow(
                "Snooze length",
                null,
                "${Prefs.snoozeMinutes} min",
                onMinus = { Prefs.snoozeMinutes = Prefs.snoozeMinutes - 1 },
                onPlus = { Prefs.snoozeMinutes = Prefs.snoozeMinutes + 1 },
            )
            Divider()
            SwitchRow("Warn me at 80%", "A short banner when you are close", Prefs.warnings) { Prefs.warnings = it }
        }

        SectionTitle("Apps")
        Apps.all.forEach { app ->
            Panel(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
                        Text(app.feature + if (app.beta) " · detection in beta" else "", fontSize = 13.sp, color = C.TextLo)
                    }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow("Track ${app.feature}", null, Prefs.tracked(app.key)) { Prefs.setTracked(app.key, it) }
                Divider()
                SwitchRow("Limit screen for ${app.feature}", "When daily limits are reached", Prefs.blockOnLimitFor(app.key)) { Prefs.setBlockOnLimitFor(app.key, it) }
                Divider()
                SwitchRow("Block ${app.feature} completely", "The rest of ${app.label} keeps working", Prefs.blockAlways(app.key)) { Prefs.setBlockAlways(app.key, it) }
            }
            Spacer(Modifier.height(12.dp))
        }

        SectionTitle("Schedules")
        Panel(padding = 16.dp) {
            val list = Prefs.schedules
            if (list.isEmpty()) Text("No schedules yet.", color = C.TextLo, fontSize = 14.sp)
            list.forEachIndexed { i, s ->
                if (i > 0) Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        NavRow(
                            s.name,
                            subtitle = "${Fmt.clock(s.startMin)} – ${Fmt.clock(s.endMin)} · ${daysLabel(s.days)}",
                            value = if (s.enabled) "On" else "Off",
                        ) { onOpen(Route.EditSchedule(s.id)) }
                    }
                }
            }
            Divider()
            NavRow("Add a schedule", "Block Reels/Shorts at set times") { onOpen(Route.EditSchedule(null)) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LimitSlider(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    step: Int,
    label: (Int) -> String,
    onDone: (Int) -> Unit,
) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val snapped = ((v / step).toInt() * step).coerceIn(range.start.toInt(), range.endInclusive.toInt())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 16.sp, color = C.Text)
        Text(label(snapped), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Accent)
    }
    Slider(
        value = v,
        onValueChange = { v = it },
        // Read v here (not the composed `snapped`), so the last drag delta is not lost.
        onValueChangeFinished = { onDone(((v / step).toInt() * step).coerceIn(range.start.toInt(), range.endInclusive.toInt())) },
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = C.Accent,
            activeTrackColor = C.Accent,
            inactiveTrackColor = C.Card2,
        ),
    )
}

fun daysLabel(days: Int): String {
    if (days == Schedule.ALL_DAYS) return "Every day"
    if (days == 0b0011111) return "Weekdays"
    if (days == 0b1100000) return "Weekends"
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return names.filterIndexed { i, _ -> days and (1 shl i) != 0 }.joinToString(" ").ifEmpty { "No days" }
}
