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
    val Bg = Color(0xFF130E22)
    val Card = Color(0xFF1E1733)
    val Card2 = Color(0xFF2B2342)
    val Line = Color(0xFF352B52)
    val Text = Color(0xFFF3F0FF)
    val TextLo = Color(0xFFA9A1C4)
    val TextDim = Color(0xFF6F6890)
    val Accent = Color(0xFFA78BFA)
    val AccentLight = Color(0xFFC4B5FD)
    val AccentDeep = Color(0xFF7C5CF0)
    val OnAccent = Color(0xFF1A1030)
    val Mint = Color(0xFF5EEAD4)
    val Good = Color(0xFF5EEAD4)
    val Warn = Color(0xFFFACC15)
    val Bad = Color(0xFFFB7185)
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
