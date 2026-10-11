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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.neura.brain.core.Feedback
import com.neura.brain.core.Wave
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private data class FlankTrial(val centerRight: Boolean, val congruent: Boolean) {
    val flankRight: Boolean get() = if (congruent) centerRight else !centerRight
}

internal fun flankerCongruentProb(level: Int): Float =
    (0.65f - (level - 1) * 0.05f).coerceAtLeast(0.40f)

private fun genFlank(congruentP: Float) =
    FlankTrial(Random.nextBoolean(), Random.nextFloat() < congruentP)

@Composable
private fun FlankArrow(right: Boolean, side: Dp, color: Color) {
    Canvas(Modifier.size(side, side * 0.95f)) {
        val d = if (right) 1f else -1f
        val cx = size.width / 2f; val cy = size.height / 2f
        drawLine(
            color = color,
            start = Offset(cx - d * size.width * 0.26f, cy),
            end = Offset(cx + d * size.width * 0.20f, cy),
            strokeWidth = size.width * 0.15f,
            cap = StrokeCap.Round
        )
        val head = Path().apply {
            moveTo(cx + d * size.width * 0.46f, cy)
            lineTo(cx + d * size.width * 0.10f, cy - size.height * 0.28f)
            lineTo(cx + d * size.width * 0.10f, cy + size.height * 0.28f)
            close()
        }
        drawPath(head, color)
    }
}

@Composable
fun FlankerGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    val congruentP = remember { flankerCongruentProb(level) }
    val deadline = remember { (2200L - (level - 1) * 200L).coerceAtLeast(1400L) }
    var over by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var trial by remember { mutableStateOf(genFlank(congruentP)) }
    var pickedRight by remember { mutableStateOf<Boolean?>(null) }
    var lastWrong by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var omissions by remember { mutableIntStateOf(0) }

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.FLANK, score,
            listOf("TRIALS $answered", "ACC $acc%", "MISSED $omissions", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(35f, onEnd = { finish() })

    fun advanceTrial(afterMs: Long) {
        scope.launch {
            delay(afterMs)
            if (over) return@launch
            trial = genFlank(congruentP)
            pickedRight = null; locked = false; lastWrong = false
        }
    }

    fun submit(right: Boolean) {
        if (over || locked) return
        locked = true; pickedRight = right; answered++
        val hit = right == trial.centerRight
        lastWrong = !hit
        if (hit) {
            correct++; streak++; best = maxOf(best, streak)
            score += 15 + min(streak - 1, 5) * 3
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0
            clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        advanceTrial(if (hit) 130 else 380)
    }

    // Response deadline: an unanswered trial is a scored lapse.
    LaunchedEffect(trial) {
        delay(deadline)
        if (!locked && !over) {
            locked = true; answered++; omissions++; streak = 0; lastWrong = true
            clock.penalize(1f)
            feedback.tone(220.0, 120, Wave.SAW, 0.07)
            advanceTrial(420)
        }
    }

    val centerColor = if (lastWrong) Bad else Ink

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = "ANSWER THE CENTER ARROW ONLY",
            tone = if (lastWrong) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 35f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val flankR = trial.flankRight
                    FlankArrow(flankR, 30.dp, Muted)
                    FlankArrow(flankR, 30.dp, Muted)
                    FlankArrow(trial.centerRight, 44.dp, centerColor)
                    FlankArrow(flankR, 30.dp, Muted)
                    FlankArrow(flankR, 30.dp, Muted)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fun dirBtn(text: String, isRight: Boolean) {
                    val active = pickedRight == isRight
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) (if (lastWrong) Bad else Good) else Ink)
                            .then(if (active) Modifier.border(3.dp, Ink, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { submit(isRight) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text, color = Paper, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                dirBtn("◀ LEFT", false)
                dirBtn("RIGHT ▶", true)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
