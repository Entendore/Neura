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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private const val VIGIL_TRAP = 3

private fun genVigil(count: Int): List<Int> {
    val traps = (count * 0.12).roundToInt()
    val trapAt = (0 until count).shuffled().take(traps).toSet()
    val go = (1..9).filter { it != VIGIL_TRAP }
    return List(count) { if (it in trapAt) VIGIL_TRAP else go.random() }
}

@Composable
fun VigilGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val pace = 1150L - (level - 1) * 80L
    val seq = remember { genVigil(36) }
    var idx by remember { mutableIntStateOf(-1) }
    var tapped by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var goes by remember { mutableIntStateOf(0) }
    var rejects by remember { mutableIntStateOf(0) }
    var omissions by remember { mutableIntStateOf(0) }
    var commissions by remember { mutableIntStateOf(0) }
    var stamp by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }

    fun finish() {
        if (over) return; over = true
        val acc = (goes + rejects) * 100 / seq.size
        onFinish(GameOutcome(GameId.VIGIL, score,
            listOf("TAPPED $goes", "TRAP $commissions", "MISSED $omissions", "ACC $acc%")))
    }
    // Fixed: 45s (not 40s) — 36 digits at LV1 pace needs ~42s.
    val clock = rememberGameClock(45f, onEnd = { finish() })

    LaunchedEffect(Unit) {
        delay(800)
        for (i in seq.indices) {
            if (over) return@LaunchedEffect
            idx = i; tapped = false; stamp = false
            delay(pace)
            if (over) return@LaunchedEffect
            val d = seq[i]
            if (d == VIGIL_TRAP) {
                if (!tapped) { rejects++; score += 6 }   // correct hold
            } else if (!tapped) {
                omissions++; streak = 0; clock.penalize(1f)
            }
        }
        finish()
    }

    fun tap() {
        if (over || tapped || idx < 0) return
        tapped = true
        if (seq[idx] == VIGIL_TRAP) {
            commissions++; streak = 0; stamp = true
            score = (score - 10).coerceAtLeast(0)
            clock.penalize(3f)
            feedback.bad(); feedback.buzz(90)
        } else {
            goes++; streak++
            score += 6 + min(streak / 10, 3)
            feedback.tick(); feedback.buzz(6)
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "DIGIT ${maxOf(1, idx + 1)} / ${seq.size}",
            subtitle = "TAP EVERY DIGIT — NOT 3",
            tone = if (stamp) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 45f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, LineStrong, RoundedCornerShape(20.dp))
                .clickable { tap() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                seq.getOrElse(idx) { "" },
                fontSize = 110.sp, fontWeight = FontWeight.Bold,
                color = if (stamp) Bad else Ink
            )
        }
    }
}
