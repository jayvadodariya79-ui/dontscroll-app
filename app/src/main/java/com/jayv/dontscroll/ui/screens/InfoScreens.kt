package com.jayv.dontscroll.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.service.DebugLog
import com.jayv.dontscroll.ui.Perms
import com.jayv.dontscroll.ui.components.BackHeader
import com.jayv.dontscroll.ui.components.Dot
import com.jayv.dontscroll.ui.components.Panel
import com.jayv.dontscroll.ui.components.PrimaryButton
import com.jayv.dontscroll.ui.components.SecondaryButton
import com.jayv.dontscroll.ui.components.SectionTitle
import com.jayv.dontscroll.ui.components.Ticker
import com.jayv.dontscroll.ui.components.rememberRefreshKey
import com.jayv.dontscroll.ui.theme.C
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReliabilityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val refresh = rememberRefreshKey()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        BackHeader("Keep tracking reliable", onBack)

        StatusLine("Accessibility switched on", Perms.accessibilityEnabled(ctx))
        StatusLine("Tracker running", Perms.trackerRunning())
        StatusLine("Battery unrestricted", Perms.ignoringBatteryOptimizations(ctx))
        Spacer(Modifier.height(12.dp))

        SectionTitle("1. Turn on Accessibility")
        Panel(padding = 18.dp) {
            Steps(
                "Tap the button below.",
                "Open Installed apps (Samsung) or Downloaded apps, then SwipeStop.",
                "Switch it on and tap Allow.",
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Open Accessibility") { Perms.openAccessibility(ctx) }
        }

        SectionTitle("2. If Android says \"Restricted setting\"")
        Panel(padding = 18.dp) {
            Text(
                "Android 13+ blocks Accessibility for apps installed from a file. You allow it once:",
                fontSize = 14.sp, color = C.TextLo,
            )
            Spacer(Modifier.height(8.dp))
            Steps(
                "Tap the button below to open App info.",
                "Tap ⋮ (top right) and choose \"Allow restricted settings\". Confirm with your PIN or fingerprint.",
                "Go back and switch SwipeStop on in Accessibility.",
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Open App info") { Perms.openAppInfo(ctx) }
        }

        SectionTitle("3. Stop the phone from killing it")
        Panel(padding = 18.dp) {
            if (Perms.isSamsung) {
                Text("Samsung", fontWeight = FontWeight.Bold, color = C.Text)
                Spacer(Modifier.height(6.dp))
                Steps(
                    "App info → Battery → choose Unrestricted.",
                    "Settings → Battery → Background usage limits → Never auto sleeping apps → add SwipeStop.",
                    "Do not swipe SwipeStop away with \"Close all\" in recent apps; if you do, it may need switching on again.",
                )
            } else {
                Steps(
                    "App info → Battery → choose Unrestricted (or \"Don't optimise\").",
                    "On Xiaomi, Vivo, Oppo and Realme also turn on Autostart for SwipeStop.",
                )
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Open App info") { Perms.openAppInfo(ctx) }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Open battery settings") { Perms.openBatterySettings(ctx) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "If counting stops after an update: Accessibility → SwipeStop → switch off and on again.",
            fontSize = 13.sp, color = C.TextLo,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Dot(if (ok) C.Good else C.Bad)
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), color = C.Text, fontSize = 15.sp)
        Text(if (ok) "Yes" else "No", color = if (ok) C.Good else C.Bad, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Steps(vararg steps: String) {
    steps.forEachIndexed { i, s ->
        Row(Modifier.padding(vertical = 4.dp)) {
            Text("${i + 1}.", Modifier.width(22.dp), color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(s, color = C.Text, fontSize = 14.sp)
        }
    }
}

@Composable
fun DebugScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version by DebugLog.version.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    Ticker(1000) { tick++ }
    val entries = remember(version, tick) { DebugLog.snapshot() }
    val f = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BackHeader("Detection check", onBack)
        Panel(padding = 16.dp) {
            Text("Open Instagram Reels or YouTube Shorts, swipe a few times, then come back here.", fontSize = 13.sp, color = C.TextLo)
            Spacer(Modifier.height(10.dp))
            KV("Tracker running", if (DebugLog.serviceRunning) "yes" else "no")
            KV("App in front", DebugLog.foregroundApp ?: "-")
            KV("Player open", DebugLog.surfaceApp ?: "-")
            KV("Found by", DebugLog.lastHow)
            KV("Last scroll view", DebugLog.lastScrollId)
            Spacer(Modifier.height(12.dp))
            Row {
                SecondaryButton("Copy log", Modifier.weight(1f)) { copy(ctx, DebugLog.asText()) }
                Spacer(Modifier.width(10.dp))
                SecondaryButton("Clear", Modifier.weight(1f)) { DebugLog.clear() }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(entries) { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(f.format(Date(e.ts)), fontSize = 12.sp, color = C.TextDim, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(8.dp))
                    Text(e.app, Modifier.width(72.dp), fontSize = 12.sp, color = C.Accent, fontFamily = FontFamily.Monospace)
                    Text(e.msg, fontSize = 12.sp, color = C.Text, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, Modifier.weight(1f), fontSize = 14.sp, color = C.TextLo)
        Text(v, fontSize = 14.sp, color = C.Text, fontFamily = FontFamily.Monospace)
    }
}

private fun copy(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SwipeStop log", text))
    Toast.makeText(ctx, "Log copied", Toast.LENGTH_SHORT).show()
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        BackHeader("Privacy & how it works", onBack)
        Panel(padding = 18.dp) {
            Para("What SwipeStop does", "It counts the reels and shorts you watch on Instagram, YouTube, Snapchat and Facebook, measures the time, and stops you when you reach the limits you set.")
            Para("How it sees reels", "Through Android's Accessibility service. It looks at the layout of those four apps (view IDs, sizes, and for Facebook the labels of the like/comment/share buttons) to know when a Reels or Shorts player is open, and counts each swipe.")
            Para("What it never does", "It does not read your messages, does not record your screen, has no account and no internet permission. Nothing leaves your phone.")
            Para("Where your data lives", "In a small database on this phone. Export it as CSV or delete it any time in Settings. Uninstalling removes it.")
            Para("Free", "Every feature is free, including blocking.")
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Para(title: String, body: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
    Spacer(Modifier.height(4.dp))
    Text(body, fontSize = 14.sp, color = C.TextLo)
    Spacer(Modifier.height(14.dp))
}
