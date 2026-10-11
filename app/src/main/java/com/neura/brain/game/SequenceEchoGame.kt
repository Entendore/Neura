package com.neura.brain.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val ECHO_COLORS = listOf(
    Color(0xFFD9532B), Color(0xFFCE931C), Color(0xFF3E8E5A), Color(0xFF3F5F8C)
)

@Composable
fun SequenceEchoGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var seq by remember { mutableStateOf(List(maxOf(1, (level + 1) / 2)) { Random.nextInt(4) }) }
    var lit by remember { mutableIntStateOf(-1) }
    var inputOn by remember { mutableStateOf(false) }
    var pos by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(2) }
    var score by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf("WATCH THE SEQUENCE") }
    var subTone by remember { mutableStateOf(SubTone.NORMAL) }

    fun play() {
        inputOn = false
        sub = "ROUND ${seq.size} — WATCH"; subTone = SubTone.NORMAL
        scope.launch {
            delay(450)
            val step = (440 - seq.size * 18).coerceAtLeast(200)
            val dur = (step * 7 / 10).coerceAtLeast(150)
            seq.forEach { s ->
                lit = s; feedback.pad(s)
                delay(dur.toLong()); lit = -1
                delay((step - dur).toLong().coerceAtLeast(50))
            }
            delay(160)
            pos = 0; inputOn = true
            sub = "REPEAT — ${seq.size} STEP${if (seq.size != 1) "S" else ""}"
            subTone = SubTone.GOOD
        }
    }

    fun tap(i: Int) {
        if (over || !inputOn) return
        lit = i; feedback.pad(i)
        scope.launch { delay(160); if (lit == i) lit = -1 }
        taps++
        if (i == seq[pos]) {
            pos++
            if (pos == seq.size) {
                inputOn = false
                score += seq.size * 25
                sub = "ECHOED"; subTone = SubTone.GOOD
                feedback.good(); feedback.buzz(14)
                seq = seq + Random.nextInt(4)
                scope.launch { delay(850); if (!over) play() }
            }
        } else {
            inputOn = false; lives--
            feedback.bad(); feedback.buzz(80)
            if (lives > 0) {
                sub = "MISSED — WATCH AGAIN"; subTone = SubTone.ALERT
                scope.launch { delay(950); if (!over) play() }
            } else {
                sub = "OUT OF ECHOES"; subTone = SubTone.ALERT
                over = true
                val done = seq.size - 1
                scope.launch {
                    delay(950)
                    onFinish(GameOutcome(GameId.ECHO, score, listOf("SEQUENCE $done", "PAD TAPS $taps")))
                }
            }
        }
    }

    LaunchedEffect(Unit) { delay(700); play() }

    GameScaffold(
        GameHud(
            score = score, lives = lives, maxLives = 2,
            subtitle = sub, tone = subTone,
            level = level
        ), onQuit
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.82f).widthIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(rowStart * 2, rowStart * 2 + 1).forEach { i ->
                            val isLit = lit == i
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        val s = if (isLit) 1.03f else 1f
                                        scaleX = s; scaleY = s
                                    }
                                    .alpha(if (isLit) 1f else 0.8f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ECHO_COLORS[i])
                                    .border(1.5.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .clickable { tap(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}
