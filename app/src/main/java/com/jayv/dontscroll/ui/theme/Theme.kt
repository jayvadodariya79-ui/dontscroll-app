package com.jayv.dontscroll.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF0A0A09)
    val Card = Color(0xFF161513)
    val Card2 = Color(0xFF1F1D1A)
    val Line = Color(0xFF2A2724)
    val Text = Color(0xFFF5F1EA)
    val TextLo = Color(0xFFA39E95)
    val TextDim = Color(0xFF6E6A63)
    val Accent = Color(0xFFFFB067)
    val AccentLight = Color(0xFFFFE2BF)
    val AccentDeep = Color(0xFFF08A3C)
    val OnAccent = Color(0xFF2A1A14)
    val Banner = Color(0xFF6E3B05)
    val Good = Color(0xFF6FD08C)
    val Warn = Color(0xFFFFCF5C)
    val Bad = Color(0xFFFF6B6B)
    val Monster = Color(0xFF8B6CFF)
}

private val scheme = darkColorScheme(
    primary = C.Accent,
    onPrimary = C.OnAccent,
    secondary = C.AccentLight,
    onSecondary = C.OnAccent,
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Card,
    onSurface = C.Text,
    surfaceVariant = C.Card2,
    onSurfaceVariant = C.TextLo,
    surfaceContainer = C.Card,
    surfaceContainerHigh = C.Card2,
    surfaceContainerHighest = C.Card2,
    outline = C.Line,
    outlineVariant = C.Line,
    error = C.Bad,
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun DontScrollTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
