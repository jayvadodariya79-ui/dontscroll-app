package com.jayv.dontscroll.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.DayStats
import com.jayv.dontscroll.data.DaySummary
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.ui.components.AppIcon
import com.jayv.dontscroll.ui.components.Divider
import com.jayv.dontscroll.ui.components.HourlyChart
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.WeekChart
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen(onOpen: (Route) -> Unit) {
    val key = rememberRefreshKey()
    var day by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    if (day.isAfter(today)) day = today

    val stats by produceState<DayStats?>(null, key, day) {
        value = withContext(Dispatchers.IO) { Repo.dayStats(day) }
    }
    val week by produceState<List<DaySummary>>(emptyList(), key, day) {
        value = withContext(Dispatchers.IO) { Repo.range(day, 7) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text("Progress", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

        // Day switcher
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { day = day.minusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day", tint = C.Text)
            }
            Text(dayLabel(day, today), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
            IconButton(onClick = { if (day.isBefore(today)) day = day.plusDays(1) }, enabled = day.isBefore(today)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = if (day.isBefore(today)) C.Text else C.TextDim,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val s = stats
        Panel(padding = 18.dp) {
            HourlyChart(s?.hourly ?: IntArray(24))
        }

        Spacer(Modifier.height(16.dp))
        Panel(padding = 26.dp) {
            Row(Modifier.fillMaxWidth()) {
                BigStat("${s?.reels ?: 0}", "Total Reels", Modifier.weight(1f))
                BigStat(Fmt.duration(s?.reelMs ?: 0L), "Time spent", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))
        // Banner like the design, but blocking is free here.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(C.Banner)
                .clickable { onOpen(Route.Blocking) }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(22.dp)).background(C.AccentLight).padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text("FREE", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = C.OnAccent)
            }
            Spacer(Modifier.width(14.dp))
            Text("Block Shorts & Reels", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = C.Text)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = C.Text)
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle("Top apps")
        Panel(padding = 16.dp) {
            val apps = (s?.apps ?: Apps.all.map { com.jayv.dontscroll.data.AppStat(it, 0, 0, 0, 0) })
                .sortedWith(compareByDescending<com.jayv.dontscroll.data.AppStat> { it.reels }.thenByDescending { it.reelMs })
            apps.forEachIndexed { i, a ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a.app, 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.app.label, fontSize = 17.sp, color = C.Text)
                        val sub = buildString {
                            append(a.app.feature)
                            if (a.appMs > 0) append(" · app ${Fmt.duration(a.appMs)}")
                            if (a.sessions > 0) append(" · ${a.sessions} sessions")
                            if (a.app.beta) append(" · beta")
                        }
                        Text(sub, fontSize = 12.sp, color = C.TextLo)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${a.reels}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.Text)
                        if (a.reelMs > 0) Text(Fmt.duration(a.reelMs), fontSize = 13.sp, color = C.TextLo)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle(if (day == today) "Last 7 days" else "7 days to ${day.format(DateTimeFormatter.ofPattern("d MMM"))}")
        Panel(padding = 18.dp) {
            val limit = Prefs.reelLimit
            WeekChart(
                values = week.map { it.reels },
                labels = week.map { it.day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                limit = limit,
                highlightLast = true,
            )
            Spacer(Modifier.height(12.dp))
            val total = week.sumOf { it.reels }
            val avg = if (week.isNotEmpty()) total / week.size else 0
            val under = week.count { it.reels in 1..limit }
            Row(Modifier.fillMaxWidth()) {
                SmallStat("$avg", "avg reels/day", Modifier.weight(1f))
                SmallStat(Fmt.duration(week.sumOf { it.reelMs }), "in reels", Modifier.weight(1f))
                SmallStat("$under", "days under $limit", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(C.Bad))
                Text("  over limit    ", fontSize = 12.sp, color = C.TextLo)
                Text("- - -", fontSize = 12.sp, color = C.TextDim)
                Text("  daily limit", fontSize = 12.sp, color = C.TextLo)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 16.sp, color = C.TextLo)
    }
}

@Composable
private fun SmallStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.Text)
        Text(label, fontSize = 12.sp, color = C.TextLo, textAlign = TextAlign.Center)
    }
}
