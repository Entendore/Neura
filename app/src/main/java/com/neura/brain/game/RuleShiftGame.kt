package com.neura.brain.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

enum class RShape { CIRCLE, SQUARE, TRIANGLE, DIAMOND }
private val SHIFT_COLORS = listOf(Color(0xFFC93A22), Color(0xFF2E7D4E), Color(0xFF2E63B8), Color(0xFFC98A12))

internal fun shiftFlipBase(level: Int): Int = (4 - (level - 1)).coerceAtLeast(2)

private data class ShiftTrial(val ref: Pair<RShape, Int>, val choice: Pair<RShape, Int>, val matches: Boolean)

private fun genShiftTrial(ruleColor: Boolean): ShiftTrial {
    val matches = Random.nextBoolean()
    val ref = RShape.entries.random() to Random.nextInt(4)
    val cShape: RShape
    val cColor: Int
    if (ruleColor) {
        cShape = RShape.entries.random()
        cColor = if (matches) ref.second
        else { var c = Random.nextInt(4); while (c == ref.second) c = Random.nextInt(4); c }
    } else {
        cShape = if (matches) ref.first
        else { var s = RShape.entries.random(); while (s == ref.first) s = RShape.entries.random(); s }
        cColor = Random.nextInt(4)
    }
    return ShiftTrial(ref, cShape to cColor, matches)
}

@Composable
private fun ShapeGlyph(shape: RShape, color: Color, side: Dp) {
    when (shape) {
        RShape.CIRCLE -> Box(Modifier.size(side).clip(CircleShape).background(color))
        RShape.SQUARE -> Box(Modifier.size(side).clip(RoundedCornerShape(6.dp)).background(color))
        RShape.DIAMOND -> Box(
            Modifier.size(side * 0.72f).graphicsLayer {
                rotationZ = 45f; shape = RoundedCornerShape(5.dp); clip = true
            }.background(color)
        )
        RShape.TRIANGLE -> Canvas(Modifier.size(side)) {
            val p = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height * 0.92f)
                lineTo(0f, size.height * 0.92f)
                close()
            }
            drawPath(p, color)
        }
    }
}

@Composable
fun RuleShiftGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var ruleColor by remember { mutableStateOf(true) }
    var trial by remember { mutableStateOf(genShiftTrial(true)) }
    var locked by remember { mutableStateOf(false) }
    var pickedMatch by remember { mutableStateOf<Boolean?>(null) }
    var shiftFlash by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var shifts by remember { mutableIntStateOf(0) }
    var sinceFlip by remember { mutableIntStateOf(0) }
    val flipAt = remember { mutableIntStateOf(shiftFlipBase(level) + Random.nextInt(4)) }

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.SHIFT, score, listOf("ANSWERED $answered", "ACC $acc%", "SHIFTS $shifts", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(45f, onEnd = { finish() })

    fun submit(saysMatch: Boolean) {
        if (over || locked) return
        locked = true; pickedMatch = saysMatch; answered++
        val right = saysMatch == trial.matches
        if (right) {
            correct++; streak++; best = maxOf(best, streak)
            score += 20 + min(streak - 1, 6) * 3
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0
            clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        scope.launch {
            delay(if (right) 140 else 380)
            if (over) return@launch
            sinceFlip++
            if (sinceFlip >= flipAt.intValue) {
                sinceFlip = 0
                flipAt.intValue = shiftFlipBase(level) + Random.nextInt(4)
                ruleColor = !ruleColor
                shifts++; shiftFlash = true
                feedback.buzz(40)
                scope.launch { delay((700 - (level - 1) * 120).coerceAtLeast(300)); shiftFlash = false }
            }
            trial = genShiftTrial(ruleColor)
            pickedMatch = null; locked = false
        }
    }

    @Composable
    fun Card(t: Pair<RShape, Int>) {
        Box(
            Modifier.size(width = 170.dp, height = 120.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Ink, RoundedCornerShape(16.dp))
                .background(Panel),
            contentAlignment = Alignment.Center
        ) { ShapeGlyph(t.first, SHIFT_COLORS[t.second], 54.dp) }
    }

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = if (shiftFlash) "RULE SHIFT!" else if (ruleColor) "MATCH BY COLOR" else "MATCH BY SHAPE",
            tone = if (shiftFlash) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 45f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(trial.ref)
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (shiftFlash) Accent else Ink)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (ruleColor) "MATCH BY COLOR" else "MATCH BY SHAPE",
                            style = monoStyle(9.sp, color = Color.White, ls = 1.4.sp)
                        )
                    }
                    Card(trial.choice)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fun answerBtn(text: String, bg: Color, active: Boolean, action: () -> Unit) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .then(if (active) Modifier.border(3.dp, Ink, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { action() },
                        contentAlignment = Alignment.Center
                    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
                answerBtn("MATCH", Good, pickedMatch == true) { submit(true) }
                answerBtn("NO MATCH", Bad, pickedMatch == false) { submit(false) }
            }
        }
    }
}
