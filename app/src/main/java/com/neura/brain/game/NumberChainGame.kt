package com.neura.brain.game

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

private fun chainRounds(level: Int) = if (level >= 3) listOf(8, 10, 12, 14) else listOf(8, 10, 12)

@Composable
fun NumberChainGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    val rounds = remember { chainRounds(level) }
    var round by remember { mutableIntStateOf(0) }
    var n by remember { mutableIntStateOf(rounds[0]) }
    var cells by remember { mutableStateOf(listOf<Int>()) }
    var next by remember { mutableIntStateOf(1) }
    var errors by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var flashIdx by remember { mutableIntStateOf(-1) }
    var penaltyMs by remember { mutableIntStateOf(0) }
    var roundStart by remember { mutableLongStateOf(0L) }
    var totalMs by remember { mutableLongStateOf(0L) }
    var interRound by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }

    fun buildRound() {
        n = rounds[round]
        cells = (1..n).shuffled()
        next = 1; penaltyMs = 0; interRound = false
        roundStart = SystemClock.elapsedRealtime()
    }

    fun finishRound() {
        val elapsed = SystemClock.elapsedRealtime() - roundStart + penaltyMs
        totalMs += elapsed
        score += (250 - elapsed / 1000f * 12f).roundToInt().coerceIn(40, 250)
        feedback.good(); feedback.buzz(15)
        if (round + 1 < rounds.size) {
            interRound = true
            scope.launch { delay(700); if (!over) { round++; buildRound() } }
        } else {
            over = true
            val totalS = "%.1f".format(totalMs / 1000f)
            scope.launch {
                delay(500)
                onFinish(GameOutcome(GameId.CHAIN, score, listOf("ROUNDS ${rounds.size}", "ERRORS $errors", "TOTAL ${totalS}S")))
            }
        }
    }

    fun tap(number: Int, cellIndex: Int) {
        if (over || interRound) return
        if (number == next) {
            feedback.tick(); feedback.buzz(6)
            if (number == n) finishRound() else next++
        } else {
            errors++; penaltyMs += 400 + (level - 1) * 100
            flashIdx = cellIndex
            feedback.bad(); feedback.buzz(60)
            scope.launch { delay(320); flashIdx = -1 }
        }
    }

    LaunchedEffect(Unit) { buildRound() }

    val rows = (n + 3) / 4
    GameScaffold(
        GameHud(
            score = score,
            status = "ROUND ${round + 1} · NEXT $next",
            subtitle = if (interRound) "ROUND CLEAR" else "TAP 1 → $n IN ORDER",
            tone = if (interRound) SubTone.GOOD else SubTone.NORMAL,
            level = level
        ), onQuit
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(rows) { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { c ->
                            val idx = r * 4 + c
                            if (idx < cells.size) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(if (flashIdx == idx) Bad else Ink)
                                        .then(
                                            if (flashIdx == idx) Modifier.border(2.dp, Bad, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { tap(cells[idx], idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${cells[idx]}",
                                        color = Paper, fontWeight = FontWeight.Bold,
                                        fontSize = if (n >= 12) 15.sp else 17.sp
                                    )
                                }
                            } else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
