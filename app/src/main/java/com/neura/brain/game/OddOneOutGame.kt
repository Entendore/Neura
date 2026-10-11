package com.neura.brain.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun OddOneOutGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var level2 by remember { mutableIntStateOf(1) }
    var found by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var n by remember { mutableIntStateOf(3) }
    var colors by remember { mutableStateOf(List(9) { Color.Gray }) }
    var oddIndex by remember { mutableIntStateOf(0) }
    var wrongIdx by remember { mutableIntStateOf(-1) }

    fun build(lv: Int) {
        val size = (3 + (lv - 1) / 2).coerceAtMost(6)
        val hue = Random.nextFloat() * 360f
        val sat = 0.50f + Random.nextFloat() * 0.14f
        val light = 0.50f + Random.nextFloat() * 0.10f
        val delta = (22f - lv * (1.6f + level * 0.18f)).coerceAtLeast(2.2f)
        val dir = if (Random.nextBoolean()) 1f else -1f
        val odd = Random.nextInt(size * size)
        n = size
        oddIndex = odd
        colors = List(size * size) { i ->
            if (i == odd) Color.hsl((hue + dir * delta + 360f) % 360f, sat, light)
            else Color.hsl(hue, sat, (light + (Random.nextFloat() * 4f - 2f) / 100f).coerceIn(0f, 1f))
        }
        busy = false
    }

    fun finish() {
        if (over) return; over = true
        val acc = if (found + misses > 0) found * 100 / (found + misses) else 0
        onFinish(GameOutcome(GameId.ODD, score, listOf("LEVEL $level2", "FOUND $found", "ACC $acc%")))
    }
    val clock = rememberGameClock(45f, onEnd = { finish() })

    fun tap(i: Int) {
        if (over || busy) return
        if (i == oddIndex) {
            busy = true; found++; score += 20
            feedback.good(); feedback.buzz(8)
            val lv = level2 + 1; level2 = lv
            scope.launch { delay(170); if (!over) build(lv) }
        } else {
            misses++; wrongIdx = i
            clock.penalize(3f)
            feedback.bad(); feedback.buzz(50)
            scope.launch { delay(350); wrongIdx = -1 }
        }
    }

    LaunchedEffect(Unit) { build(1) }

    GameScaffold(
        GameHud(
            score = score, status = "FOUND $found",
            subtitle = "LEVEL $level2 — TAP THE DIFFERENT DOT",
            timerFraction = clock.remaining / 45f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                repeat(n) { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        repeat(n) { c ->
                            val i = r * n + c
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(colors[i])
                                    .then(
                                        if (wrongIdx == i) Modifier.border(3.dp, Bad, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { tap(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}
