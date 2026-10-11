package com.neura.brain.game

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.core.Wave
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private enum class ImpPhase { BLANK, GO, NOGO, HIT, FALSE }

@Composable
fun ImpulseGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    var phase by remember { mutableStateOf(ImpPhase.BLANK) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var falseAlarms by remember { mutableIntStateOf(0) }
    var rejects by remember { mutableIntStateOf(0) }
    var event by remember { mutableStateOf("") }
    var over by remember { mutableStateOf(false) }

    fun finish() {
        if (over) return; over = true
        val denom = hits + rejects + misses + falseAlarms
        val acc = if (denom > 0) (hits + rejects) * 100 / denom else 0
        onFinish(GameOutcome(GameId.IMPULSE, score, listOf("HITS $hits", "FALSE $falseAlarms", "ACC $acc%")))
    }
    val clock = rememberGameClock(30f, onEnd = { finish() })

    // Signal driver: blank gap -> green or red -> window -> judge.
    LaunchedEffect(Unit) {
        delay(400)
        while (!over) {
            phase = ImpPhase.BLANK; event = ""
            delay(Random.nextLong(500, 1100))
            if (over) break
            phase = if (Random.nextFloat() < 0.72f - (level - 1) * 0.03f) ImpPhase.GO else ImpPhase.NOGO
            delay(900L - (level - 1) * 40L)
            if (over) break
            when (phase) {
                ImpPhase.GO -> {
                    misses++; streak = 0
                    clock.penalize(1f); event = "MISSED −1S"
                    feedback.tone(220.0, 120, Wave.SAW, 0.07)
                }
                ImpPhase.NOGO -> {
                    rejects++; score += 12
                    feedback.tick(); feedback.buzz(6); event = "HELD +12"
                }
                else -> {}
            }
            delay(250)
        }
    }

    fun tap() {
        if (over) return
        when (phase) {
            ImpPhase.GO -> {
                hits++; streak++
                val gained = 30 + min(streak - 1, 5) * 3
                score += gained
                phase = ImpPhase.HIT; event = "HIT +$gained"
                feedback.good(); feedback.buzz(10)
            }
            ImpPhase.NOGO -> {
                falseAlarms++; streak = 0
                clock.penalize(2f)
                phase = ImpPhase.FALSE; event = "FALSE ALARM −2S"
                feedback.bad(); feedback.buzz(80)
            }
            else -> {}
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = "TAP GREEN — HOLD RED",
            tone = if (phase == ImpPhase.FALSE) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 30f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, Ink, RoundedCornerShape(20.dp))
                    .clickable { tap() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val stim = when (phase) {
                        ImpPhase.GO, ImpPhase.HIT -> Good
                        ImpPhase.NOGO, ImpPhase.FALSE -> Bad
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier.size(96.dp).clip(CircleShape)
                            .then(
                                if (stim == Color.Transparent) Modifier.border(2.dp, LineStrong, CircleShape)
                                else Modifier.background(stim)
                            )
                    )
                    Text(
                        when (phase) {
                            ImpPhase.GO -> "TAP!"
                            ImpPhase.HIT -> "HIT"
                            ImpPhase.NOGO -> "HOLD"
                            ImpPhase.FALSE -> "FALSE ALARM"
                            else -> "WATCH"
                        },
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = when (phase) {
                            ImpPhase.GO, ImpPhase.HIT -> Good
                            ImpPhase.NOGO, ImpPhase.FALSE -> Bad
                            else -> Ink
                        }
                    )
                    if (phase == ImpPhase.BLANK && event.isNotEmpty())
                        Text(event, style = monoStyle(9.5.sp, color = Muted, ls = 1.4.sp))
                }
            }
        }
    }
}
