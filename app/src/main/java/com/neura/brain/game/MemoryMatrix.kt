package com.neura.brain.game

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.GameScaffold
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private enum class MxPhase { SHOW, INPUT, WRONG_REVEAL, CLEARED, DEAD }

private data class MxState(
    val level: Int = 1,
    val attempt: Int = 0,
    val lives: Int = 3,
    val score: Int = 0,
    val size: Int = 3,
    val pattern: Set<Int> = emptySet(),
    val patternList: List<Int> = emptyList(),
    val revealed: Int = 0,
    val picked: Set<Int> = emptySet(),
    val wrong: Set<Int> = emptySet(),
    val okCount: Int = 0,
    val taps: Int = 0,
    val shakeKey: Int = 0,
    val phase: MxPhase = MxPhase.SHOW
)

private fun gridSizeFor(level: Int) = when { level < 3 -> 3; level < 5 -> 4; level < 7 -> 5; else -> 6 }
private fun kFor(level: Int, size: Int) = min(level + 2, (size * size * 0.55).roundToInt())

@Composable
fun MemoryMatrixGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    var st by remember { mutableStateOf(MxState(level = (level + 1).coerceIn(1, 8))) }
    val k = kFor(st.level, st.size)

    val hud = GameHud(
        score = st.score,
        lives = st.lives,
        maxLives = 3,
        status = "LVL ${st.level}",
        level = level,
        subtitle = when (st.phase) {
            MxPhase.SHOW -> "MEMORIZE THE PATTERN"
            MxPhase.INPUT -> "REBUILD IT — $k TILE${if (k != 1) "S" else ""}"
            MxPhase.WRONG_REVEAL -> "PATTERN BROKEN — AGAIN"
            MxPhase.CLEARED -> "CLEARED"
            MxPhase.DEAD -> "OUT OF LIVES"
        },
        tone = when (st.phase) {
            MxPhase.CLEARED -> SubTone.GOOD
            MxPhase.WRONG_REVEAL, MxPhase.DEAD -> SubTone.ALERT
            else -> SubTone.NORMAL
        }
    )

    LaunchedEffect(st.level, st.attempt) {
        val size = gridSizeFor(st.level)
        val kk = kFor(st.level, size)
        val list = List(size * size) { it }.shuffled().take(kk)
        st = MxState(
            level = st.level, attempt = st.attempt, lives = st.lives, score = st.score,
            okCount = st.okCount, taps = st.taps,
            size = size, pattern = list.toSet(), patternList = list, phase = MxPhase.SHOW
        )
        list.forEachIndexed { i, _ ->
            delay(if (i == 0) 200L else 95L)
            st = st.copy(revealed = i + 1)
            feedback.tick()
        }
        delay(700L)
        st = st.copy(phase = MxPhase.INPUT)
    }

    LaunchedEffect(st.phase) {
        when (st.phase) {
            MxPhase.WRONG_REVEAL -> {
                delay(350L)
                st = st.copy(revealed = st.patternList.size)
                delay(1200L)
                st = st.copy(attempt = st.attempt + 1)
            }
            MxPhase.CLEARED -> {
                delay(700L)
                st = st.copy(level = st.level + 1, attempt = 0)
            }
            MxPhase.DEAD -> {
                delay(500L)
                st = st.copy(revealed = st.patternList.size)
                delay(1100L)
                val acc = if (st.taps > 0) st.okCount * 100 / st.taps else 0
                onFinish(
                    GameOutcome(
                        GameId.MATRIX, st.score,
                        listOf("LEVEL ${st.level}", "ACC $acc%", "${st.okCount} TILES")
                    )
                )
            }
            else -> {}
        }
    }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(st.shakeKey) {
        if (st.shakeKey > 0) {
            repeat(2) { shake.snapTo(-5f); delay(45); shake.snapTo(5f); delay(45) }
            shake.snapTo(0f)
        }
    }

    GameScaffold(hud, onQuit) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(st.size) { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(st.size) { c ->
                            val i = r * st.size + c
                            val ordinal = st.patternList.indexOf(i)
                            val isLit = ordinal in 0 until st.revealed
                            val isPicked = i in st.picked
                            val isWrong = i in st.wrong
                            val fill = when {
                                isWrong -> Bad
                                isPicked -> Accent
                                isLit -> Ink
                                else -> Panel
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(fill)
                                    .border(1.5.dp, fill, RoundedCornerShape(10.dp))
                                    .clickable(
                                        enabled = st.phase == MxPhase.INPUT &&
                                            !isPicked && !isWrong && st.picked.size < k
                                    ) {
                                        if (st.phase != MxPhase.INPUT) return@clickable
                                        if (i in st.pattern) {
                                            feedback.good(); feedback.buzz(8)
                                            val picked = st.picked + i
                                            var s = st.copy(
                                                picked = picked,
                                                okCount = st.okCount + 1,
                                                taps = st.taps + 1,
                                                score = st.score + 10
                                            )
                                            if (picked.size == k) {
                                                if (s.wrong.isEmpty()) {
                                                    s = s.copy(score = s.score + 50, phase = MxPhase.CLEARED)
                                                    feedback.done(); feedback.buzz(20)
                                                } else {
                                                    s = s.copy(phase = MxPhase.WRONG_REVEAL)
                                                }
                                            }
                                            st = s
                                        } else {
                                            feedback.bad(); feedback.buzz(70)
                                            val lives = st.lives - 1
                                            st = st.copy(
                                                wrong = st.wrong + i,
                                                lives = lives,
                                                taps = st.taps + 1,
                                                shakeKey = st.shakeKey + 1,
                                                phase = if (lives <= 0) MxPhase.DEAD else st.phase
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
