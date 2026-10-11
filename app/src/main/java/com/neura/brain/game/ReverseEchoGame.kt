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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

private const val REV_PADS = 6
private const val REV_START_LEN = 3

@Composable
fun ReverseEchoGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var seq by remember { mutableStateOf(List(REV_START_LEN + (level - 1) / 2) { Random.nextInt(REV_PADS) }) }
    var lit by remember { mutableIntStateOf(-1) }
    var inputOn by remember { mutableStateOf(false) }
    var pos by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(2) }
    var score by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf("WATCH THE SEQUENCE") }
    var subTone by remember { mutableStateOf(SubTone.NORMAL) }

    fun padTone(i: Int) = feedback.tone(294.0 * Math.pow(2.0, i / 6.0), 240, vol = 0.15)

    fun play() {
        inputOn = false; pos = 0
        sub = "ROUND ${seq.size} — WATCH"; subTone = SubTone.NORMAL
        scope.launch {
            delay(450)
            val step = max(230, 500 - seq.size * 15)
            seq.forEach { s ->
                lit = s; padTone(s)
                delay((step * 7L) / 10L); lit = -1
                delay(max(60L, step - (step * 7L) / 10L))
            }
            delay(160)
            inputOn = true
            sub = "REVERSE — ${seq.size} STEPS"; subTone = SubTone.GOOD
        }
    }

    fun finishRun() {
        over = true
        val span = seq.size
        scope.launch {
            delay(950)
            onFinish(GameOutcome(GameId.REVERSE, score, listOf("SPAN $span", "PAD TAPS $taps")))
        }
    }

    fun tap(i: Int) {
        if (over || !inputOn) return
        lit = i; padTone(i)
        scope.launch { delay(170); if (lit == i) lit = -1 }
        taps++
        val expected = seq[seq.size - 1 - pos]   // reversed walk
        if (i == expected) {
            pos++
            if (pos == seq.size) {
                inputOn = false
                score += seq.size * 25
                sub = "REVERSED"; subTone = SubTone.GOOD
                feedback.good(); feedback.buzz(14)
                seq = seq + Random.nextInt(REV_PADS)
                scope.launch { delay(900); if (!over) play() }
            }
        } else {
            inputOn = false; lives--
            feedback.bad(); feedback.buzz(80)
            if (lives > 0) {
                sub = "MISSED — WATCH AGAIN"; subTone = SubTone.ALERT
                scope.launch { delay(1000); if (!over) play() }
            } else {
                sub = "OUT OF ECHOES"; subTone = SubTone.ALERT
                finishRun()
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
                Modifier.fillMaxWidth(0.94f).widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(rowStart * 3, rowStart * 3 + 1, rowStart * 3 + 2).forEach { i ->
                            val isLit = lit == i
                            val enabled = inputOn && !isLit
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.92f)
                                    .graphicsLayer {
                                        val s = if (isLit) 1.04f else 1f
                                        scaleX = s; scaleY = s
                                    }
                                    .alpha(if (enabled) 1f else 0.75f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isLit) Ink else Panel)
                                    .border(1.5.dp, Ink, RoundedCornerShape(14.dp))
                                    .clickable { tap(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${i + 1}",
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (isLit) Paper else Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
