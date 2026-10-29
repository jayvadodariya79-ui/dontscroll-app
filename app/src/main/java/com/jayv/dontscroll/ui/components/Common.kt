package com.jayv.dontscroll.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.data.WatchedApp
import com.jayv.dontscroll.ui.theme.C

/** A number that changes whenever data or settings change, or the app comes back to the front. */
@Composable
fun rememberRefreshKey(): Long {
    val data by Repo.version.collectAsState()
    val prefs by Prefs.version.collectAsState()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    return data * 31 + prefs * 7 + resumes
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = C.Card,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = C.Text,
    )
}

@Composable
private fun rememberPhase(durationMs: Int = 2400): Float {
    val t = rememberInfiniteTransition(label = "phase")
    val v by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    return v
}

@Composable
fun BrainArt(health: Int, modifier: Modifier = Modifier, animate: Boolean = true) {
    val phase = if (animate) rememberPhase() else 0f
    Canvas(modifier) {
        drawIntoCanvas { BrainPainter.draw(it.nativeCanvas, size.width, size.height, health, phase) }
    }
}

@Composable
fun MonsterArt(hp: Int, modifier: Modifier = Modifier) {
    val phase = rememberPhase(2000)
    Canvas(modifier) {
        drawIntoCanvas { BrainPainter.drawMonster(it.nativeCanvas, size.width, size.height, hp, phase) }
    }
}

@Composable
fun HpBar(value: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(C.Line)
    ) {
        Box(
            Modifier
                .fillMaxWidth((value.coerceIn(0, 100)) / 100f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
    }
}

@Composable
fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(C.Line)
    ) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

/** The real app icon when installed, otherwise a coloured letter. */
@Composable
fun AppIcon(app: WatchedApp, size: Dp = 44.dp) {
    val ctx = LocalContext.current
    val bitmap = remember(app.key) {
        var drawable: Drawable? = null
        for (p in app.packages) {
            drawable = try { ctx.packageManager.getApplicationIcon(p) } catch (e: Exception) { null }
            if (drawable != null) break
        }
        try { drawable?.toBitmap(144, 144)?.asImageBitmap() } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(Color(app.color)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                app.label.take(1),
                color = if (app.key == "snapchat") Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ charts

/** Reels per hour, 24 bars, like the Progress screen in the design. */
@Composable
fun HourlyChart(hourly: IntArray, modifier: Modifier = Modifier) {
    val max = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    val top = niceTop(max)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(190.dp)) {
            Column(
                Modifier.width(30.dp).height(190.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(top.toString())
                AxisLabel((top / 2).toString())
                AxisLabel("0")
            }
            Canvas(Modifier.weight(1f).height(190.dp).padding(top = 8.dp, bottom = 8.dp)) {
                val slot = size.width / 24f
                val barW = (slot * 0.55f).coerceAtLeast(3f)
                val grid = C.Line
                for (i in 0..2) {
                    val y = size.height * i / 2f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                }
                val brush = Brush.verticalGradient(listOf(C.AccentLight, C.Accent, C.AccentDeep))
                for (h in 0 until 24) {
                    val v = hourly[h]
                    val x = slot * h + (slot - barW) / 2f
                    val bh = if (v == 0) 4f else (size.height * v / top).coerceAtLeast(6f)
                    val color = if (v == 0) C.Line else null
                    if (color != null) {
                        drawRoundRect(color, Offset(x, size.height - bh), Size(barW, bh), CornerRadius(barW / 2, barW / 2))
                    } else {
                        drawRoundRect(brush, Offset(x, size.height - bh), Size(barW, bh), CornerRadius(barW / 2, barW / 2))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 30.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            AxisLabel("12am"); AxisLabel("6am"); AxisLabel("12pm"); AxisLabel("6pm"); AxisLabel("12am")
        }
    }
}

/** Reels per day with a dashed line at the daily limit. */
@Composable
fun WeekChart(values: List<Int>, labels: List<String>, limit: Int, highlightLast: Boolean, modifier: Modifier = Modifier) {
    val max = maxOf(values.maxOrNull() ?: 0, limit).coerceAtLeast(1)
    val top = niceTop(max)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 6.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.5f
            val ly = size.height - size.height * limit / top
            drawLine(C.TextDim, Offset(0f, ly), Offset(size.width, ly), strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)), cap = StrokeCap.Round)
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - barW) / 2f
                val bh = if (v == 0) 4f else (size.height * v / top).coerceAtLeast(6f)
                val over = v > limit
                val color = when {
                    v == 0 -> C.Line
                    over -> C.Bad
                    highlightLast && i == values.lastIndex -> C.Accent
                    else -> C.AccentDeep.copy(alpha = 0.75f)
                }
                drawRoundRect(color, Offset(x, size.height - bh), Size(barW, bh), CornerRadius(10f, 10f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { l ->
                Text(l, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = C.TextLo)
            }
        }
    }
}

@Composable
private fun AxisLabel(t: String) {
    Text(t, fontSize = 12.sp, color = C.TextLo)
}

private fun niceTop(max: Int): Int = when {
    max <= 4 -> 4
    max <= 8 -> 8
    max <= 16 -> 16
    max <= 20 -> 20
    max <= 40 -> 40
    max <= 50 -> 50
    max <= 100 -> 100
    else -> ((max + 99) / 100) * 100
}

// ------------------------------------------------------------------ rows

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = C.Text)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = C.TextLo)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = C.OnAccent,
                checkedTrackColor = C.Accent,
                uncheckedThumbColor = C.TextLo,
                uncheckedTrackColor = C.Card2,
                uncheckedBorderColor = C.Line,
            ),
        )
    }
}

@Composable
fun NavRow(title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = C.Text)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = C.TextLo)
        }
        if (value != null) Text(value, fontSize = 15.sp, color = C.Accent, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = C.TextLo)
    }
}

@Composable
fun StepperRow(title: String, subtitle: String? = null, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = C.Text)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = C.TextLo)
        }
        RoundButton("–", onMinus)
        Text(value, Modifier.width(72.dp), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
        RoundButton("+", onPlus)
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(C.Card2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 20.sp, color = C.Text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = C.Text,
            )
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) C.Accent else C.Card2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) C.OnAccent else C.TextDim, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(C.Card2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Small round status dot. */
@Composable
fun Dot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** Bottom-bar icons drawn by hand: brain (You), crossed swords (Battle), bars (Progress). */
@Composable
fun NavGlyph(kind: Int, tint: Color, modifier: Modifier = Modifier.size(28.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (kind) {
            0 -> {
                val u = w / 2f * 0.85f
                val cx = w / 2f
                val cy = h / 2f
                val lobes = listOf(
                    Triple(-0.42f, -0.05f, 0.38f), Triple(-0.15f, -0.35f, 0.36f), Triple(0.18f, -0.36f, 0.36f),
                    Triple(0.45f, -0.05f, 0.36f), Triple(0.25f, 0.22f, 0.36f), Triple(-0.22f, 0.22f, 0.36f),
                    Triple(0f, 0f, 0.45f),
                )
                lobes.forEach { (x, y, r) -> drawCircle(tint, r * u, Offset(cx + x * u, cy + y * u)) }
                drawCircle(C.Bg, 0.1f * u, Offset(cx - 0.2f * u, cy + 0.02f * u))
                drawCircle(C.Bg, 0.1f * u, Offset(cx + 0.2f * u, cy + 0.02f * u))
            }
            1 -> {
                val sw = w * 0.1f
                drawLine(tint, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.78f, h * 0.78f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.82f, h * 0.18f), Offset(w * 0.22f, h * 0.78f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.58f, h * 0.82f), Offset(w * 0.82f, h * 0.58f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.82f), Offset(w * 0.18f, h * 0.58f), sw, StrokeCap.Round)
            }
            else -> {
                val bw = w * 0.2f
                val base = h * 0.88f
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.5f), Size(bw, base - h * 0.5f), CornerRadius(3f, 3f))
                drawRoundRect(tint, Offset(w * 0.4f, h * 0.15f), Size(bw, base - h * 0.15f), CornerRadius(3f, 3f))
                drawRoundRect(tint, Offset(w * 0.68f, h * 0.38f), Size(bw, base - h * 0.38f), CornerRadius(3f, 3f))
                drawLine(tint, Offset(w * 0.06f, base), Offset(w * 0.94f, base), w * 0.07f, StrokeCap.Round)
            }
        }
    }
}

/** Runs [block] every [periodMs] while on screen; used for live screens like Detection check. */
@Composable
fun Ticker(periodMs: Long, block: () -> Unit) {
    LaunchedEffect(periodMs) {
        while (true) {
            block()
            kotlinx.coroutines.delay(periodMs)
        }
    }
}
