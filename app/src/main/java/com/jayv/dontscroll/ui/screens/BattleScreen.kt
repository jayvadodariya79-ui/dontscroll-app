package com.jayv.dontscroll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.BattleEngine
import com.jayv.dontscroll.data.BattleOverview
import com.jayv.dontscroll.data.BattleResult
import com.jayv.dontscroll.data.Challenge
import com.jayv.dontscroll.data.Outcome
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.ui.components.BrainArt
import com.jayv.dontscroll.ui.components.HpBar
import com.jayv.dontscroll.ui.components.MonsterArt
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.ProgressLine
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BattleScreen() {
    val key = rememberRefreshKey()
    val ov by produceState<BattleOverview?>(null, key) {
        value = withContext(Dispatchers.IO) { BattleEngine.overview() }
    }
    var picking by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text("Battle", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
        val o = ov
        if (o == null) {
            Text("Loading…", color = C.TextLo)
            return@Column
        }

        // Level
        Panel(padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(C.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${o.level.number}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = C.OnAccent)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.level.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.Text)
                    val next = o.level.nextXp
                    Text(
                        if (next != null) "${o.xp} XP · ${next - o.xp} XP to next level" else "${o.xp} XP · top level",
                        fontSize = 13.sp, color = C.TextLo,
                    )
                }
            }
            val next = o.level.nextXp
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                ProgressLine((o.xp - o.level.minXp).toFloat() / (next - o.level.minXp), C.Accent)
            }
        }

        Spacer(Modifier.height(14.dp))
        // You vs the Scroll Monster
        Panel(padding = 18.dp) {
            Text("Today's battle", fontSize = 14.sp, color = C.TextLo)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Fighter("You", o.youHp, C.Good, Modifier.weight(1f)) { BrainArt(o.youHp, Modifier.size(110.dp)) }
                Text("VS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = C.Accent, modifier = Modifier.padding(horizontal = 8.dp))
                Fighter("Scroll Monster", o.monsterHp, C.Monster, Modifier.weight(1f)) { MonsterArt(o.monsterHp, Modifier.size(110.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            val budget = Prefs.reelLimit
            val msg = when (o.today.result) {
                BattleResult.LOST -> "The monster won today: ${o.today.reels} reels is over your budget of $budget. A new battle starts at midnight."
                else -> {
                    val left = (budget - o.today.reels).coerceAtLeast(0)
                    "Stay under $budget reels until midnight to win. $left reels of health left. The monster gets weaker every hour you hold on."
                }
            }
            Text(msg, fontSize = 14.sp, color = C.Text)
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniCard("🔥 ${o.streak}", "day streak", Modifier.weight(1f))
            MiniCard("${o.bestStreak}", "best streak", Modifier.weight(1f))
            MiniCard("${o.wins}–${o.losses}", "won–lost", Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle("Last 7 days")
        Panel(padding = 16.dp) {
            Row(Modifier.fillMaxWidth()) {
                o.last7.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val (bg, label) = when (d.result) {
                            BattleResult.WON -> C.Good to "W"
                            BattleResult.LOST -> C.Bad to "L"
                            BattleResult.FIGHTING -> C.Accent to "•"
                            BattleResult.NOT_TRACKED -> C.Card2 to "–"
                        }
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(bg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, color = if (d.result == BattleResult.NOT_TRACKED) C.TextDim else C.OnAccent)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(d.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 12.sp, color = C.TextLo)
                        Text("${d.reels}", fontSize = 12.sp, color = C.TextDim)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle("Today's challenge")
        val ch = o.today.challenge
        Panel(padding = 18.dp) {
            if (ch == null) {
                Text("Pick a challenge for extra XP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Text)
                Spacer(Modifier.height(4.dp))
                Text("Beat the monster for ${BattleEngine.WIN_XP} XP. A challenge adds more.", fontSize = 13.sp, color = C.TextLo)
                Spacer(Modifier.height(12.dp))
                ChooseButton("Choose a challenge") { picking = true }
            } else {
                val outcome = o.today.challengeOutcome ?: Outcome.IN_PROGRESS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ch.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.Text)
                        Text(ch.desc, fontSize = 13.sp, color = C.TextLo)
                    }
                    Text("+${ch.xp} XP", fontWeight = FontWeight.Bold, color = C.Accent)
                }
                Spacer(Modifier.height(12.dp))
                val (txt, col) = when (outcome) {
                    Outcome.IN_PROGRESS -> "In progress" to C.Accent
                    Outcome.DONE -> "Completed" to C.Good
                    Outcome.FAILED -> "Failed today" to C.Bad
                }
                Text(txt, fontWeight = FontWeight.SemiBold, color = col)
                if (outcome == Outcome.IN_PROGRESS) {
                    Spacer(Modifier.height(10.dp))
                    ChooseButton("Change challenge") { picking = true }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle("How it works")
        Panel(padding = 18.dp) {
            Text(
                "Every day you fight the Scroll Monster. Your reel budget is your health. " +
                    "Stay under it until midnight and you win ${BattleEngine.WIN_XP} XP. " +
                    "Wins in a row build your streak. Change your budget in Limits.",
                fontSize = 14.sp, color = C.TextLo,
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = C.Card,
            title = { Text("Choose today's challenge", color = C.Text) },
            text = {
                Column {
                    Challenge.entries.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    Repo.setChallenge(LocalDate.now(), c.id)
                                    picking = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.title, color = C.Text, fontWeight = FontWeight.SemiBold)
                                Text(c.desc, color = C.TextLo, fontSize = 13.sp)
                            }
                            Text("+${c.xp}", color = C.Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text("Close", color = C.Accent) }
            },
        )
    }
}

@Composable
private fun Fighter(name: String, hp: Int, color: Color, modifier: Modifier, art: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        art()
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        HpBar(hp, color)
        Text("$hp HP", fontSize = 12.sp, color = C.TextLo)
    }
}

@Composable
private fun MiniCard(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(C.Card)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
        Text(label, fontSize = 12.sp, color = C.TextLo)
    }
}

@Composable
private fun ChooseButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, C.Accent, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, color = C.Accent, fontWeight = FontWeight.SemiBold)
    }
}
