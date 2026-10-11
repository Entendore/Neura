package com.neura.brain.game

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private val STROOP_COLORS = listOf(
    "RED" to Color(0xFFC93A22),
    "GREEN" to Color(0xFF2E7D4E),
    "BLUE" to Color(0xFF2E63B8),
    "AMBER" to Color(0xFFC98A12)
)

private fun genTrial(): Pair<Int, Int> {
    val w = Random.nextInt(4)
    var i = Random.nextInt(4)
    if (Random.nextFloat() < 0.78f && i == w) i = (i + 1 + Random.nextInt(3)) % 4
    return w to i
}

@Composable
fun StroopGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var picked by remember { mutableIntStateOf(-1) }
    var lastWrong by remember { mutableStateOf(false) }
    var trial by remember { mutableStateOf(genTrial()) }
    val word = trial.first
    val ink = trial.second

    fun nextTrial() { trial = genTrial() }

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.STROOP, score, listOf("ANSWERED $answered", "ACC $acc%", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(30f, onEnd = { finish() })

    fun submit(i: Int) {
        if (over || locked) return
        locked = true; answered++; picked = i
        val right = i == ink
        lastWrong = !right
        if (right) {
            correct++; streak++; best = maxOf(best, streak)
            score += 10 + min(streak - 1, 5) * 2
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0
            clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        scope.launch {
            delay(if (right) maxOf(60L, (150 - level * 15).toLong()) else 320)
            if (!over) { picked = -1; nextTrial(); locked = false }
        }
    }

    GameScaffold(
        GameHud(
            score = score,
            status = "STREAK $streak",
            subtitle = if (picked >= 0 && lastWrong) "MISS — STREAK RESET" else "TAP THE INK — NOT THE WORD",
            tone = if (picked >= 0 && lastWrong) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 30f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    STROOP_COLORS[word].first,
                    color = STROOP_COLORS[ink].second,
                    fontSize = 46.sp, fontWeight = FontWeight.Bold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(rowStart * 2, rowStart * 2 + 1).forEach { i ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(2.1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(STROOP_COLORS[i].second)
                                    .then(
                                        if (picked == i) Modifier.border(3.dp, Ink, RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .clickable { submit(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}
