package com.neura.brain.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

private const val FLASH_ROUNDS = 8

private fun placeDots(n: Int): List<Pair<Float, Float>> {
    var minDist = 0.135f
    while (minDist > 0.05f) {
        val pts = mutableListOf<Pair<Float, Float>>()
        var guard = 0
        while (pts.size < n && guard < 5000) {
            guard++
            val p = (0.12f + Random.nextFloat() * 0.76f) to (0.12f + Random.nextFloat() * 0.76f)
            if (pts.all { q -> hypot(p.first - q.first, p.second - q.second) > minDist }) pts += p
        }
        if (pts.size == n) return pts
        minDist -= 0.02f
    }
    return List(n) { 0.5f to 0.5f }
}

/** Always returns 4 distinct options near the target (fixed: no infinite loop at high counts). */
private fun genOptions(target: Int): List<Int> {
    val opts = linkedSetOf(target)
    for (d in listOf(-2, -1, 1, 2).shuffled()) {
        val v = target + d
        if (v in 2..20) opts.add(v)
        if (opts.size == 4) break
    }
    var extra = 1
    while (opts.size < 4) {
        opts.add(target + extra)
        if (opts.size < 4) opts.add((target - extra).coerceAtLeast(1))
        extra++
    }
    return opts.shuffled()
}

private enum class CFPhase { READY, FLASH, ANSWER, REVEAL }

@Composable
fun CountFlashGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(CFPhase.READY) }
    var count by remember { mutableIntStateOf(4) }
    var dots by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var options by remember { mutableStateOf(listOf<Int>()) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }

    fun finish() {
        if (over) return; over = true
        val acc = hits * 100 / FLASH_ROUNDS
        onFinish(GameOutcome(GameId.FLASH, score, listOf("HITS $hits/$FLASH_ROUNDS", "ACC $acc%", "TOP STREAK $best")))
    }

    LaunchedEffect(round) {
        if (round >= FLASH_ROUNDS) { finish(); return@LaunchedEffect }
        count = 3 + level + round + Random.nextInt(0, 2)          // 4..16 by LV5
        dots = placeDots(count)
        picked = -1; options = emptyList()
        phase = CFPhase.READY
        delay(750)
        if (over) return@LaunchedEffect
        phase = CFPhase.FLASH
        feedback.go()
        delay((650 - round * 25 - (level - 1) * 45).coerceAtLeast(330).toLong())
        if (over) return@LaunchedEffect
        options = genOptions(count)
        phase = CFPhase.ANSWER
    }

    fun answer(v: Int) {
        if (over || phase != CFPhase.ANSWER) return
        picked = v
        if (v == count) {
            hits++; streak++; best = maxOf(best, streak)
            score += 30 + min(streak - 1, 4) * 5
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0
            feedback.bad(); feedback.buzz(60)
        }
        phase = CFPhase.REVEAL
        scope.launch { delay(900); if (!over) round++ }
    }

    val sub = when (phase) {
        CFPhase.READY -> "GET READY…"
        CFPhase.FLASH -> ""
        CFPhase.ANSWER -> "HOW MANY DOTS?"
        CFPhase.REVEAL -> "IT WAS $count"
    }

    GameScaffold(
        GameHud(
            score = score, status = "ROUND ${min(round + 1, FLASH_ROUNDS)} / $FLASH_ROUNDS",
            subtitle = sub,
            tone = when {
                phase == CFPhase.REVEAL && picked == count -> SubTone.GOOD
                phase == CFPhase.REVEAL -> SubTone.ALERT
                else -> SubTone.NORMAL
            },
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, LineStrong, RoundedCornerShape(20.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * 0.034f
                    val alpha = if (phase == CFPhase.REVEAL) 0.30f else 1f
                    dots.forEach { (fx, fy) ->
                        drawCircle(color = Ink.copy(alpha = alpha), radius = r,
                            center = Offset(fx * size.width, fy * size.height))
                    }
                }
                if (phase == CFPhase.READY) {
                    Text("+", fontSize = 40.sp, fontWeight = FontWeight.Light, color = Muted,
                        modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(rowStart * 2, rowStart * 2 + 1).forEach { i ->
                            val v = options.getOrElse(i) { -1 }
                            val bg = when {
                                phase == CFPhase.REVEAL && v == count -> Good
                                phase == CFPhase.REVEAL && picked == v -> Bad
                                else -> Panel
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.5.dp, if (bg == Panel) Ink else bg, RoundedCornerShape(12.dp))
                                    .clickable(enabled = phase == CFPhase.ANSWER) { if (v > 0) answer(v) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (v > 0) "$v" else "·",
                                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = if (bg == Panel) Ink else androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
