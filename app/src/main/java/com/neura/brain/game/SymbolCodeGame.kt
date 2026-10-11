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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private enum class CodeSym { TRIANGLE, CIRCLE, SQUARE, DIAMOND, RING, CROSS }

@Composable
private fun CodeGlyph(sym: CodeSym, color: Color, side: Dp) {
    when (sym) {
        CodeSym.CIRCLE -> Box(Modifier.size(side).clip(CircleShape).background(color))
        CodeSym.SQUARE -> Box(Modifier.size(side).clip(RoundedCornerShape(4.dp)).background(color))
        CodeSym.RING -> Box(
            Modifier.size(side * 0.86f)
                .clip(CircleShape)
                .border(side * 0.15f, color, CircleShape)
        )
        CodeSym.TRIANGLE -> Canvas(Modifier.size(side)) {
            val p = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height * 0.92f)
                lineTo(0f, size.height * 0.92f)
                close()
            }
            drawPath(p, color)
        }
        CodeSym.DIAMOND -> Box(
            Modifier.size(side * 0.74f).graphicsLayer {
                rotationZ = 45f; shape = RoundedCornerShape(4.dp); clip = true
            }.background(color)
        )
        CodeSym.CROSS -> Canvas(Modifier.size(side)) {
            val w = size.width * 0.24f
            drawRoundRect(color, Offset(size.width / 2 - w / 2, 0f), Size(w, size.height), CornerRadius(w / 2))
            drawRoundRect(color, Offset(0f, size.height / 2 - w / 2), Size(size.width, w), CornerRadius(w / 2))
        }
    }
}

@Composable
fun SymbolCodeGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    val pool = remember { CodeSym.entries.shuffled() }
    val symCount = if (level >= 4) 6 else if (level >= 2) 5 else 4
    val mapping = remember { pool.take(symCount) }          // mapping[i] ↔ digit i+1

    var sym by remember { mutableStateOf(mapping.first()) }
    var order by remember { mutableStateOf((0 until symCount).shuffled()) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }

    fun answerDigit() = mapping.indexOf(sym) + 1

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.CODE, score, listOf("CODED $correct", "ACC $acc%", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(40f, onEnd = { finish() })

    fun tap(digit: Int) {
        if (over || locked) return
        locked = true; picked = digit; answered++
        val right = digit == answerDigit()
        if (right) {
            correct++; streak++; best = maxOf(best, streak)
            score += 8 + min(streak - 1, 6) * 2
            feedback.good(); feedback.buzz(7)
        } else {
            streak = 0; clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        scope.launch {
            delay(if (right) maxOf(60L, (160 - level * 15).toLong()) else 420)
            if (over) return@launch
            sym = mapping.random()
            order = (0 until symCount).shuffled()
            picked = -1; locked = false
        }
    }

    val ans = answerDigit()

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = "TAP THE DIGIT THAT MATCHES THE KEY",
            tone = if (picked != -1 && picked != ans) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 40f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                mapping.forEachIndexed { i, s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CodeGlyph(s, Ink, 20.dp)
                        Spacer(Modifier.height(5.dp))
                        Text("${i + 1}", style = monoStyle(11.sp, weight = FontWeight.SemiBold))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CodeGlyph(sym, Ink, 88.dp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                order.forEach { di ->
                    val digit = di + 1
                    val bg = when {
                        locked && digit == ans -> Good
                        locked && picked == digit -> Bad
                        else -> Panel
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.5.dp, if (bg == Panel) Ink else bg, RoundedCornerShape(12.dp))
                            .clickable { tap(digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$digit", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = if (bg == Panel) Ink else androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}
