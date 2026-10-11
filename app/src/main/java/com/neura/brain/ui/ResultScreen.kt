package com.neura.brain.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.R
import com.neura.brain.data.fmt
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ResultScreen(vm: AppViewModel, onHome: () -> Unit, onNext: () -> Unit, onRetry: () -> Unit) {
    val r = vm.lastResult
    if (r == null) { LaunchedEffect(Unit) { onHome() }; return }

    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(80); target = r.score }
    val shown by animateIntAsState(target, tween(900, easing = FastOutSlowInEasing), label = "score")
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(500); pop.animateTo(1f, spring(dampingRatio = 0.55f)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.width(34.dp).height(3.dp).background(Ink))
        Spacer(Modifier.height(20.dp))
        Text(
            "DRILL ${r.gameId.num} COMPLETE — ${r.gameId.title.uppercase(Locale.US)}",
            style = monoStyle(9.5.sp, color = Muted, ls = 1.5.sp), textAlign = TextAlign.Center
        )
        Text("$shown", fontSize = 62.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .then(
                    if (r.isBest) Modifier.background(Accent)
                    else Modifier.border(1.5.dp, LineStrong, RoundedCornerShape(20.dp))
                )
                .graphicsLayer {
                    val s = if (r.isBest) pop.value.coerceAtLeast(0.01f) else 1f
                    scaleX = s; scaleY = s; alpha = s
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                if (r.isBest) "NEW PERSONAL BEST" else "BEST ${fmt(r.prevBest)}",
                style = monoStyle(9.5.sp, color = if (r.isBest) Color.White else InkSoft, ls = 1.3.sp)
            )
        }
        r.vsAvg?.takeIf { it != 0 }?.let { d ->
            Spacer(Modifier.height(10.dp))
            Text(
                (if (d > 0) "▲ +" else "▼ −") + abs(d) + " VS YOUR LAST 5",
                style = monoStyle(9.sp, color = if (d > 0) Good else Muted, ls = 1.3.sp)
            )
        }
        ChipsRow(r.chips, Modifier.padding(top = 20.dp))
        if (r.levelAfter != r.levelBefore) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (r.levelAfter > r.levelBefore) "▲ ADAPTIVE LEVEL UP — LV ${r.levelAfter}"
                else "▼ ADAPTIVE — LV ${r.levelAfter}",
                style = monoStyle(9.sp, color = if (r.levelAfter > r.levelBefore) Good else Muted, ls = 1.3.sp)
            )
        }
        r.newMilestones.forEach { m ->
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Accent)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(painterResource(m.icon), null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(
                    "MILESTONE — ${m.title.uppercase(Locale.US)}",
                    style = monoStyle(9.sp, color = Color.White, ls = 1.2.sp)
                )
            }
        }
        Text(
            r.insight,
            fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, bottom = 26.dp).widthIn(max = 300.dp)
        )
        if (r.circuit) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                repeat(r.circuitSize) { i ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (i <= r.circuitIndex) Accent else LineStrong))
                }
            }
        }
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!r.circuit) GhostButton("RETRY DRILL", onClick = onRetry)
            PrimaryButton(
                if (r.circuit) (if (r.circuitIndex < r.circuitSize - 1) "NEXT DRILL" else "VIEW REPORT") else "CONTINUE",
                accent = r.circuit, trailingIcon = R.drawable.ic_arrow,
                onClick = { if (r.circuit) onNext() else onHome() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChipsRow(chips: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { c ->
            Box(
                Modifier.border(1.5.dp, LineStrong, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) { Text(c, style = monoStyle(10.sp, color = InkSoft, ls = 0.8.sp)) }
        }
    }
}

@Composable
fun CircuitReportScreen(vm: AppViewModel, onDone: () -> Unit) {
    val circuit = vm.circuit
    if (circuit == null) { LaunchedEffect(Unit) { onDone() }; return }
    val p by vm.progress.collectAsState()
    val ctx = LocalContext.current
    val shareText = buildString {
        append("NEURA daily circuit — ${fmt(vm.reportTotal)} pts, ${circuit.results.size} drills")
        circuit.results.forEach { append("\n· ${it.gameId.title}: ${fmt(it.score)}") }
        if (p.streakCount > 1) append("\n${p.streakCount}-day streak")
    }

    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(80); target = vm.reportTotal }
    val shown by animateIntAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "total")
    val date = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)
        .format(LocalDate.now()).uppercase(Locale.US)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.width(34.dp).height(3.dp).background(Ink))
        Spacer(Modifier.height(20.dp))
        Text("DAILY CIRCUIT — $date", style = monoStyle(9.5.sp, color = Muted, ls = 1.5.sp), textAlign = TextAlign.Center)
        Text("$shown", fontSize = 62.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Accent)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) { Text("CIRCUIT COMPLETE", style = monoStyle(9.5.sp, color = Color.White, ls = 1.3.sp)) }
        if (vm.pendingMilestones.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                vm.pendingMilestones.forEach { m ->
                    Text(
                        "★ MILESTONE — ${m.title.uppercase(Locale.US)}",
                        style = monoStyle(9.sp, color = Accent, ls = 1.3.sp)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth()) {
            HLine(Line)
            circuit.results.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.gameId.num, style = monoStyle(10.5.sp, color = Muted), modifier = Modifier.width(24.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.gameId.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(s.gameId.domain.uppercase(Locale.US), style = monoStyle(9.sp, color = Muted, ls = 1.2.sp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (s.isBest) "NEW BEST" else "SCORE", style = monoStyle(8.sp, color = Muted, ls = 1.4.sp))
                        Text(fmt(s.score), style = monoStyle(11.5.sp, color = Ink, weight = FontWeight.SemiBold))
                    }
                }
                HLine(Line)
            }
        }
        Text(
            if (p.streakCount > 1)
                "Streak extended — ${p.streakCount} days. Come back tomorrow to keep the chain alive."
            else
                "Streak started — day one. The chain begins today.",
            fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, bottom = 26.dp).widthIn(max = 300.dp)
        )
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("SHARE RESULT", onClick = {
                ctx.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        },
                        "Share circuit"
                    )
                )
            })
            PrimaryButton("DONE", onClick = onDone)
        }
    }
}
