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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

private data class EstTrial(val left: String, val right: String, val leftVal: Int, val rightVal: Int)

/** Gap between the two values shrinks with level: 20% at LV1 → 6% at LV5. */
private fun genEst(level: Int): EstTrial {
    val minGap = (0.20f - (level - 1) * 0.035f).coerceAtLeast(0.06f)
    val top = 9 + level * 2
    var guard = 0
    while (true) {
        guard++
        val text: String; val value: Int
        when (Random.nextInt(3)) {
            0 -> { val a = Random.nextInt(6, top + 1); val b = Random.nextInt(6, top + 1); text = "$a × $b"; value = a * b }
            1 -> { val a = Random.nextInt(15, top * 8 + 1); val b = Random.nextInt(15, top * 8 + 1); text = "$a + $b"; value = a + b }
            else -> { val a = Random.nextInt(6, top + 1); val b = Random.nextInt(6, top + 1); val c = Random.nextInt(20, 120); text = "$a × $b + $c"; value = a * b + c }
        }
        val plain = Random.nextInt(25, top * 15)
        if (guard < 400 && abs(value - plain) < maxOf(value, plain) * minGap) continue
        return if (Random.nextBoolean()) EstTrial(text, "$plain", value, plain)
        else EstTrial("$plain", text, plain, value)
    }
}

@Composable
private fun EstSide(
    text: String, state: Int, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val bg = when (state) { 1 -> Good; 2 -> Bad; else -> Panel }
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.5.dp, if (state == 0) Ink else bg, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = if (text.length > 7) 25.sp else 34.sp,
            fontWeight = FontWeight.Bold,
            color = if (state == 0) Ink else Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EstimateGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var trial by remember { mutableStateOf(genEst(level)) }
    var pickedLeft by remember { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.EST, score, listOf("ANSWERED $answered", "ACC $acc%", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(40f, onEnd = { finish() })

    fun pick(left: Boolean) {
        if (over || locked) return
        locked = true; pickedLeft = left; answered++
        val right = (left && trial.leftVal > trial.rightVal) || (!left && trial.rightVal > trial.leftVal)
        if (right) {
            correct++; streak++; best = maxOf(best, streak)
            score += 12 + min(streak - 1, 6) * 3
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0; clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        scope.launch {
            delay(if (right) 140 else 420)
            if (over) return@launch
            trial = genEst(level); pickedLeft = null; locked = false
        }
    }

    val leftWin = trial.leftVal > trial.rightVal
    fun stateOf(isLeft: Boolean): Int = when {
        pickedLeft == null -> 0
        pickedLeft == isLeft -> if (isLeft == leftWin) 1 else 2
        else -> if (isLeft == leftWin) 1 else 0
    }

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = "TAP THE BIGGER VALUE",
            tone = if (pickedLeft != null && stateOf(pickedLeft!!) == 2) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 40f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EstSide(trial.left, stateOf(true), !locked && !over, Modifier.weight(1f).fillMaxHeight()) { pick(true) }
            EstSide(trial.right, stateOf(false), !locked && !over, Modifier.weight(1f).fillMaxHeight()) { pick(false) }
        }
    }
}
