package com.jayv.dontscroll.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Apps
import com.jayv.dontscroll.data.Fmt
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Schedule
import com.jayv.dontscroll.ui.components.BackHeader
import com.jayv.dontscroll.ui.components.Divider
import com.jayv.dontscroll.ui.components.NavRow
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.PrimaryButton
import com.jayv.dontscroll.ui.components.SecondaryButton
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.SwitchRow
import com.jayv.dontscroll.ui.theme.C

@Composable
fun ScheduleScreen(id: Long?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val existing = remember(id) { id?.let { i -> Prefs.schedules.firstOrNull { it.id == i } } }
    var name by remember { mutableStateOf(existing?.name ?: "Study time") }
    var start by remember { mutableIntStateOf(existing?.startMin ?: (21 * 60)) }
    var end by remember { mutableIntStateOf(existing?.endMin ?: (23 * 60)) }
    var days by remember { mutableIntStateOf(existing?.days ?: Schedule.ALL_DAYS) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var apps by remember { mutableStateOf(existing?.apps ?: emptySet()) }

    fun pick(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, false).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        BackHeader(if (existing == null) "New schedule" else "Edit schedule", onBack)

        Panel(padding = 16.dp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Accent,
                    unfocusedBorderColor = C.Line,
                    focusedLabelColor = C.Accent,
                    cursorColor = C.Accent,
                    focusedTextColor = C.Text,
                    unfocusedTextColor = C.Text,
                ),
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow("Schedule on", null, enabled) { enabled = it }
            Divider()
            NavRow("Starts", value = Fmt.clock(start)) { pick(start) { start = it } }
            Divider()
            NavRow("Ends", subtitle = if (end <= start) "Ends the next day" else null, value = Fmt.clock(end)) { pick(end) { end = it } }
        }

        SectionTitle("Days")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, l ->
                val on = days and (1 shl i) != 0
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (on) C.Accent else C.Card2)
                        .clickable { days = days xor (1 shl i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(l, fontWeight = FontWeight.Bold, color = if (on) C.OnAccent else C.TextLo)
                }
            }
        }

        SectionTitle("Apps")
        Panel(padding = 16.dp) {
            SwitchRow("All apps", null, apps.isEmpty()) { all -> apps = if (all) emptySet() else Apps.all.map { it.key }.toSet() }
            Apps.all.forEach { app ->
                Divider()
                SwitchRow("${app.label} ${app.feature}", null, apps.isEmpty() || app.key in apps) { on ->
                    val current = if (apps.isEmpty()) Apps.all.map { it.key }.toSet() else apps
                    val next = if (on) current + app.key else current - app.key
                    apps = if (next.size == Apps.all.size) emptySet() else next
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Save", enabled = name.isNotBlank() && days != 0 && start != end) {
            Prefs.upsertSchedule(
                Schedule(
                    id = existing?.id ?: Prefs.nextScheduleId(),
                    name = name.trim(),
                    startMin = start,
                    endMin = end,
                    days = days,
                    enabled = enabled,
                    apps = apps,
                )
            )
            onBack()
        }
        if (existing != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Delete schedule") {
                Prefs.deleteSchedule(existing.id)
                onBack()
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "While a schedule is on, Reels and Shorts show the stop screen. Snoozes still work.",
            fontSize = 13.sp, color = C.TextLo,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(4.dp),
        )
        Spacer(Modifier.height(28.dp))
    }
}
