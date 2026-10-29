package com.jayv.dontscroll

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.ui.components.NavGlyph
import com.jayv.dontscroll.ui.screens.AboutScreen
import com.jayv.dontscroll.ui.screens.BattleScreen
import com.jayv.dontscroll.ui.screens.BlockingScreen
import com.jayv.dontscroll.ui.screens.DebugScreen
import com.jayv.dontscroll.ui.screens.OnboardingScreen
import com.jayv.dontscroll.ui.screens.ProgressScreen
import com.jayv.dontscroll.ui.screens.ReliabilityScreen
import com.jayv.dontscroll.ui.screens.Route
import com.jayv.dontscroll.ui.screens.ScheduleScreen
import com.jayv.dontscroll.ui.screens.SettingsScreen
import com.jayv.dontscroll.ui.screens.YouScreen
import com.jayv.dontscroll.ui.theme.C
import com.jayv.dontscroll.ui.theme.DontScrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            DontScrollTheme {
                Box(Modifier.fillMaxSize().background(C.Bg)) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var onboarded by remember { mutableStateOf(Prefs.onboarded) }
    if (!onboarded) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            OnboardingScreen(onDone = {
                Prefs.onboarded = true
                onboarded = true
            })
        }
    } else {
        MainShell()
    }
}

@Composable
private fun MainShell() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Route>() }
    val open: (Route) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.isNotEmpty()) { back() }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }

    val top = stack.lastOrNull()
    if (top != null) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (top) {
                Route.Settings -> SettingsScreen(onBack = back, onOpen = open)
                Route.Blocking -> BlockingScreen(onBack = back, onOpen = open)
                is Route.EditSchedule -> ScheduleScreen(top.id, onBack = back)
                Route.Reliability -> ReliabilityScreen(onBack = back)
                Route.Debug -> DebugScreen(onBack = back)
                Route.About -> AboutScreen(onBack = back)
            }
        }
        return
    }

    Scaffold(
        containerColor = C.Bg,
        bottomBar = {
            NavigationBar(containerColor = C.Bg, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
                val items = listOf("You", "Battle", "Progress")
                items.forEachIndexed { i, label ->
                    val selected = tab == i
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = i },
                        icon = { NavGlyph(i, if (selected) C.AccentLight else C.TextLo) },
                        label = {
                            Text(
                                label,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = C.AccentLight,
                            unselectedTextColor = C.TextLo,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> YouScreen(onOpen = open)
                1 -> BattleScreen()
                else -> ProgressScreen(onOpen = open)
            }
        }
    }
}
