package com.neura.brain.game

import android.os.SystemClock
import androidx.compose.animation.core.*
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
import com.neura.brain.data.*
import com.neura.brain.ui.GameScaffold
import com.neura.brain.ui.monoStyle
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class RxPhase { IDLE, WAIT, DECOY, GO, EARLY, COOL }
private val RxAmber = Color(0xFFC98A12)

@Composable
fun ReactionGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    var phase by remember { mutableStateOf(RxPhase.IDLE) }
    var nonce by remember { mutableIntStateOf(0) }
    var goAt by remember { mutableLongStateOf(0L) }
    val times = remember { mutableStateListOf<Int>() }
    val decoyChance = if (level >= 3) (0.30f + (level - 3) * 0.05f) else 0f

    fun startRound() { phase = RxPhase.WAIT; nonce++ }
    fun finish() {
        val avg = times.average()
        val best = times.min()
        val score = (1200 - avg * 2).roundToInt().coerceAtLeast(0)
        onFinish(
            GameOutcome(
                GameId.REACTION, score,
                listOf("AVG ${avg.roundToInt()} MS", "FASTEST $best MS", "ROUNDS ${times.size}")
            )
        )
    }

    LaunchedEffect(phase, nonce) {
        when (phase) {
            RxPhase.WAIT -> {
                delay(Random.nextLong(700, 2200))
                if (phase != RxPhase.WAIT) return@LaunchedEffect
                if (Random.nextFloat() < decoyChance) {
                    phase = RxPhase.DECOY; feedback.tick()
                } else {
                    delay(Random.nextLong(500, 1600))
                    if (phase != RxPhase.WAIT) return@LaunchedEffect
                    goAt = SystemClock.elapsedRealtime()
                    phase = RxPhase.GO
                    feedback.go()
                }
            }
            RxPhase.DECOY -> {
                delay(Random.nextLong(350, 700))
                if (phase != RxPhase.DECOY) return@LaunchedEffect
                phase = RxPhase.WAIT; nonce++
            }
            RxPhase.EARLY -> { delay(850L); startRound() }
            RxPhase.COOL -> { delay(900L); if (times.size >= 5) finish() else startRound() }
            else -> {}
        }
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.82f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    val filled = phase == RxPhase.GO || phase == RxPhase.EARLY || phase == RxPhase.DECOY
    val zoneBg = when (phase) { RxPhase.GO -> Good; RxPhase.EARLY -> Bad; RxPhase.DECOY -> RxAmber; else -> Color.Transparent }
    val zoneBorder = when (phase) { RxPhase.GO -> Good; RxPhase.EARLY -> Bad; RxPhase.DECOY -> RxAmber; else -> Ink }
    val fg = if (filled) Paper else Ink
    val big = when (phase) {
        RxPhase.IDLE -> "TAP TO START"
        RxPhase.WAIT -> "WAIT FOR GREEN"
        RxPhase.DECOY -> "HOLD"
        RxPhase.GO -> "TAP!"
        RxPhase.EARLY -> "TOO SOON"
        RxPhase.COOL -> "${times.lastOrNull() ?: 0} MS"
    }
    val note = if (phase == RxPhase.IDLE) "ROUND 1 OF 5" else "ROUND ${min(times.size + 1, 5)} OF 5"

    GameScaffold(GameHud(subtitle = "MILLISECONDS", level = level), onQuit) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(zoneBg)
                    .border(2.dp, zoneBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        when (phase) {
                            RxPhase.IDLE -> startRound()
                            RxPhase.WAIT, RxPhase.DECOY -> { phase = RxPhase.EARLY; feedback.bad(); feedback.buzz(80) }
                            RxPhase.GO -> {
                                val dt = (SystemClock.elapsedRealtime() - goAt).toInt()
                                times.add(dt)
                                phase = RxPhase.COOL
                                feedback.good(); feedback.buzz(12)
                            }
                            else -> {}
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .size((26 * (if (phase == RxPhase.GO) 1f else pulse)).dp)
                            .clip(CircleShape)
                            .background(fg)
                    )
                    Text(big, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = fg)
                    Text(note, style = monoStyle(9.5.sp, color = fg.copy(alpha = 0.7f), ls = 1.4.sp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(46.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                times.forEachIndexed { i, t ->
                    Box(
                        Modifier
                            .width(26.dp)
                            .height((t / 14f).dp.coerceIn(6.dp, 44.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == times.lastIndex) Accent else LineStrong)
                    )
                }
            }
        }
    }
}
