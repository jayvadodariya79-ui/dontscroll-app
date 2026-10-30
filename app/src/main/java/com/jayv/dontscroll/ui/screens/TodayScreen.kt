package com.jayv.dontscroll.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.Meter
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.data.TodayTotals
import com.jayv.dontscroll.ui.Perms
import com.jayv.dontscroll.ui.components.AppIcon
import com.jayv.dontscroll.ui.components.MeterArt
import com.jayv.dontscroll.ui.components.Divider
import com.jayv.dontscroll.ui.components.NavRow
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.PrimaryButton
import com.jayv.dontscroll.ui.components.ProgressLine
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.SwitchRow
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun TodayScreen(onOpen: (Route) -> Unit) {
    val ctx = LocalContext.current
    val key = rememberRefreshKey()
    val totals by produceState(TodayTotals(0, 0, emptyMap(), emptyMap()), key) {
        value = withContext(Dispatchers.IO) { Repo.todayTotals() }
    }
    val enabled = Perms.accessibilityEnabled(ctx)
    val running = Perms.trackerRunning()
    val limit = Prefs.reelLimit
    val timeLimit = Prefs.timeLimitMin
    val left = Meter.left(totals.reels, totals.reelMs, limit, timeLimit)
    val activeSchedule = Apps.all.firstNotNullOfOrNull { Prefs.activeSchedule(it.key, LocalDateTime.now()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Today", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.weight(1f))
            IconButton(onClick = { onOpen(Route.Settings) }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = C.Text)
            }
        }

        if (!enabled || !running) {
            Spacer(Modifier.height(12.dp))
            Panel(color = WarnPanel) {
                Text(
                    if (!enabled) "Tracking is off" else "Tracking is paused by the phone",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.Text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (!enabled) "Turn on SwipeStop in Accessibility so it can count your reels."
                    else "Android stopped the tracker. Turn SwipeStop off and on again in Accessibility, and set battery to Unrestricted.",
                    fontSize = 14.sp, color = C.TextLo,
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Turn on tracking") { Perms.openAccessibility(ctx) }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Help: \"Restricted setting\" or it keeps stopping",
                    fontSize = 13.sp, color = C.Accent,
                    modifier = Modifier.clickable { onOpen(Route.Reliability) }.padding(vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MeterArt(left, Modifier.size(210.dp))
        }
        Text(
            "$left% left",
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            color = leftColor(left),
        )
        Text(
            Meter.label(left),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = C.Text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            Meter.line(left),
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = C.TextLo,
        )

        Spacer(Modifier.height(20.dp))
        Panel {
            Row(Modifier.fillMaxWidth()) {
                Stat("${totals.reels}", "of $limit reels", Modifier.weight(1f))
                Stat(Fmt.duration(totals.reelMs), if (timeLimit > 0) "of ${Fmt.duration(timeLimit * 60_000L)}" else "time spent", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            ProgressLine(totals.reels.toFloat() / limit.coerceAtLeast(1), if (totals.reels >= limit) C.Bad else C.Accent)
            if (timeLimit > 0) {
                Spacer(Modifier.height(10.dp))
                ProgressLine(totals.reelMs.toFloat() / (timeLimit * 60_000f), if (totals.reelMs >= timeLimit * 60_000L) C.Bad else C.Mint)
            }
            Spacer(Modifier.height(10.dp))
            val swipesLeft = (limit - totals.reels).coerceAtLeast(0)
            Text(
                if (swipesLeft > 0) "$swipesLeft swipes left before the stop screen" else "Limit reached for today",
                fontSize = 13.sp, color = C.TextLo,
            )
        }

        if (activeSchedule != null) {
            Spacer(Modifier.height(12.dp))
            Panel(color = C.Card2) {
                Text("${activeSchedule.name} is on", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
                Text("Reels and Shorts are blocked until ${Fmt.clock(activeSchedule.endMin)}.", fontSize = 14.sp, color = C.AccentLight)
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle("Today by app")
        Panel(padding = 16.dp) {
            Apps.all.forEachIndexed { i, app ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontSize = 15.sp, color = C.Text)
                        Text(app.feature + if (Prefs.blockAlways(app.key)) " · blocked" else "", fontSize = 12.sp, color = C.TextLo)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${totals.reelsByApp[app.key] ?: 0}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
                        Text(Fmt.duration(totals.reelMsByApp[app.key] ?: 0L), fontSize = 12.sp, color = C.TextLo)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle("Quick controls")
        Panel(padding = 16.dp) {
            SwitchRow("Limit screen", "Stop Reels and Shorts when you hit your limit", Prefs.blockOnLimit) { Prefs.blockOnLimit = it }
            Divider()
            SwitchRow("Live counter", "Small counter on top while you scroll", Prefs.liveCounter) { Prefs.liveCounter = it }
            Divider()
            SwitchRow("Pause before reels", "A ${Prefs.pauseSeconds}-second breath before Reels opens", Prefs.pauseBeforeReels) { Prefs.pauseBeforeReels = it }
            Divider()
            NavRow("Limits, blocking & schedules", value = "$limit/day") { onOpen(Route.Blocking) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private val WarnPanel = Color(0xFF3A1730)

fun leftColor(left: Int) = when {
    left >= 50 -> C.Mint
    left >= 20 -> C.Warn
    else -> C.Bad
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
        Text(label, fontSize = 14.sp, color = C.TextLo)
    }
}
