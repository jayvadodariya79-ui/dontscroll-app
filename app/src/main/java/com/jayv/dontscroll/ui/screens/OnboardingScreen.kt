package com.jayv.dontscroll.ui.screens

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.notify.Notifier
import com.jayv.dontscroll.ui.Perms
import com.jayv.dontscroll.ui.components.MeterArt
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.PrimaryButton
import com.jayv.dontscroll.ui.components.SecondaryButton
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C

private const val STEPS = 6

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var agreed by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val refresh = rememberRefreshKey() // re-reads permission state on resume

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        step++
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Progress dots
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == step) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i <= step) C.Accent else C.Line)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (step) {
            0 -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MeterArt(100, Modifier.size(220.dp)) }
                Title("Every swipe counted.\nStop when you choose.")
                Body("SwipeStop counts the Reels and Shorts you swipe on Instagram, YouTube, Snapchat and Facebook, shows the time they take, and stops the feed at the limit you set.")
                Spacer(Modifier.height(10.dp))
                Body("Free. No account. Nothing leaves your phone.")
                Spacer(Modifier.height(28.dp))
                PrimaryButton("Get started") { step = 1 }
            }
            1 -> {
                var v by remember { mutableFloatStateOf(Prefs.reelLimit.toFloat()) }
                val limit = ((v / 5).toInt() * 5).coerceIn(5, 300)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MeterArt(if (limit <= 30) 100 else if (limit <= 80) 60 else 30, Modifier.size(160.dp))
                }
                Title("Your daily reel budget")
                Body("How many reels a day is okay for you? You can change this any time.")
                Spacer(Modifier.height(24.dp))
                Text("$limit reels", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = C.Accent)
                Slider(
                    value = v,
                    onValueChange = { v = it },
                    valueRange = 5f..300f,
                    colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.Card2),
                )
                Text("Most people scroll 100–300 reels a day without noticing.", fontSize = 13.sp, color = C.TextLo)
                Spacer(Modifier.height(28.dp))
                PrimaryButton("Set $limit reels a day") {
                    Prefs.reelLimit = limit
                    Repo.syncTodayBudget()
                    step = 2
                }
            }
            2 -> {
                val enabled = Perms.accessibilityEnabled(ctx)
                Title("Allow SwipeStop to see Reels")
                Panel(padding = 18.dp) {
                    Text("Why SwipeStop needs Accessibility", fontWeight = FontWeight.Bold, color = C.Text, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Bullet("It notices when Instagram Reels, YouTube Shorts, Snapchat Spotlight or Facebook Reels are on your screen.")
                    Bullet("It counts each reel you swipe and the time you spend there.")
                    Bullet("It shows your counter and the stop screen when you reach a limit you set.")
                    Spacer(Modifier.height(8.dp))
                    Text("What it never does", fontWeight = FontWeight.Bold, color = C.Text, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Bullet("It does not read your messages or what you type.")
                    Bullet("It does not collect or share personal data. The app has no internet access.")
                    Bullet("It only looks at those four apps.")
                }
                Spacer(Modifier.height(16.dp))
                if (enabled) {
                    Text("✓ Accessibility is on", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.Good)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Continue") { step = 3 }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = agreed,
                            onCheckedChange = null,
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = C.Accent, checkmarkColor = C.OnAccent),
                        )
                        Text("I understand and agree", color = C.Text, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Agree and open settings", enabled = agreed) { Perms.openAccessibility(ctx) }
                    Spacer(Modifier.height(10.dp))
                    Body("In Settings: Installed apps → SwipeStop → switch on. If it says \"Restricted setting\", open App info → ⋮ → Allow restricted settings, then try again.")
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Open App info") { Perms.openAppInfo(ctx) }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Skip for now") { step = 3 }
                }
            }
            3 -> {
                Title("Nightly summary")
                Body("Get one notification each evening with your reels and time, and a heads-up when you hit your limit.")
                Spacer(Modifier.height(28.dp))
                if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(ctx)) {
                    PrimaryButton("Allow notifications") { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Not now") { step = 4 }
                } else {
                    PrimaryButton("Continue") { step = 4 }
                }
            }
            4 -> {
                Title(if (Perms.isSamsung) "Keep it running on Samsung" else "Keep it running")
                Body("Phones sometimes stop background apps to save battery. That would stop the counting.")
                Spacer(Modifier.height(16.dp))
                Panel(padding = 18.dp) {
                    Bullet("App info → Battery → Unrestricted.")
                    if (Perms.isSamsung) {
                        Bullet("Settings → Battery → Background usage limits → Never auto sleeping apps → add SwipeStop.")
                    } else {
                        Bullet("On Xiaomi, Vivo, Oppo or Realme also turn on Autostart.")
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Open App info") { Perms.openAppInfo(ctx) }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(if (Perms.ignoringBatteryOptimizations(ctx)) "Done ✓ Continue" else "Continue") { step = 5 }
            }
            else -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MeterArt(100, Modifier.size(180.dp)) }
                Title("You're ready")
                Body("Open Instagram Reels or YouTube Shorts and swipe. The counter at the top shows it's working.")
                Spacer(Modifier.height(10.dp))
                Body("Keep your streak going in Goals and see your patterns in Insights.")
                Spacer(Modifier.height(28.dp))
                PrimaryButton("Start") {
                    Notifier.scheduleSummary(ctx)
                    onDone()
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Title(t: String) {
    Text(t, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, lineHeight = 34.sp, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun Body(t: String) {
    Text(t, fontSize = 16.sp, color = C.TextLo, lineHeight = 22.sp)
}

@Composable
private fun Bullet(t: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = C.Accent, fontSize = 15.sp, modifier = Modifier.width(16.dp))
        Text(t, color = C.Text, fontSize = 14.sp)
    }
}
