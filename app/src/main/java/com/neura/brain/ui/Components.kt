package com.neura.brain.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.R
import com.neura.brain.data.GameId
import kotlin.math.roundToInt

@Composable
fun IconHitBox(size: Dp = 38.dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun HLine(color: Color = Line) {
    Box(Modifier.fillMaxWidth().height(1.5.dp).background(color))
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: Int? = null,
    onClick: () -> Unit
) {
    val bg = if (accent) Accent else Ink
    val fg = if (accent) Color(0xFFFFF6ED) else Paper
    Row(
        modifier = modifier
            .fillMaxWidth().height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        trailingIcon?.let {
            Spacer(Modifier.width(9.dp))
            Icon(painterResource(it), null, tint = fg, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth().height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, LineStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun BrainGauge(index: Int, gamesPlayed: Int, modifier: Modifier = Modifier) {
    val target = if (gamesPlayed > 0) index / 100f else 0f
    val anim by animateFloatAsState(
        target, tween(900, easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)), label = "gauge"
    )
    Box(modifier.size(118.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2 + 1
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(Line, -90f, 360f, false, topLeft = tl, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (anim > 0.001f)
                drawArc(Accent, -90f, 360f * anim, false, topLeft = tl, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (gamesPlayed > 0) "${(anim * 100).roundToInt()}" else "–",
                fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (gamesPlayed > 0) "INDEX · $gamesPlayed/${GameId.entries.size}" else "UNCALIBRATED",
                style = monoStyle(7.5.sp, color = Muted, ls = 1.2.sp)
            )
        }
    }
}

@Composable
fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val maxV = maxOf(1, values.max())
    Canvas(modifier) {
        val n = values.size
        val gap = 6.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val h = (v.toFloat() / maxV * size.height).coerceAtLeast(4.dp.toPx())
            drawRoundRect(
                color = if (i == n - 1) Accent else LineStrong,
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}
