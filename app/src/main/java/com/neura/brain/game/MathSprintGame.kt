package com.neura.brain.game

import android.os.SystemClock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private data class MQuestion(val text: String, val answer: Int, val options: List<Int>)

private fun mk(txt: String, ans: Int): MQuestion {
    val set = linkedSetOf(ans)
    while (set.size < 4) {
        var d = ans + if (Random.nextBoolean())
            listOf(-1, -2, 1, 2, -10, 10)[Random.nextInt(6)]
        else Random.nextInt(-9, 10)
        if (d < 0) d = -d + 4
        set.add(d)
    }
    return MQuestion(txt, ans, set.shuffled())
}

private fun genQuestion(levelBoost: Int): MQuestion {
    val combo = levelBoost
    val level = when { combo < 3 -> 0; combo < 6 -> 1; else -> 2 }
    val r = Random.nextFloat()
    return when (level) {
        0 -> if (Random.nextBoolean()) {
            val a = Random.nextInt(3, 13); val b = Random.nextInt(3, 13)
            mk("$a + $b", a + b)
        } else {
            val a = Random.nextInt(5, 20); val b = Random.nextInt(2, a)
            mk("$a − $b", a - b)
        }
        1 -> when {
            r < 0.4f -> { val a = Random.nextInt(12, 59); val b = Random.nextInt(11, 40); mk("$a + $b", a + b) }
            r < 0.75f -> { val a = Random.nextInt(25, 90); val b = Random.nextInt(6, a - 3); mk("$a − $b", a - b) }
            else -> { val a = Random.nextInt(3, 10); val b = Random.nextInt(3, 10); mk("$a × $b", a * b) }
        }
        else -> when {
            r < 0.45f -> { val a = Random.nextInt(6, 13); val b = Random.nextInt(6, 13); mk("$a × $b", a * b) }
            r < 0.75f -> {
                val a = Random.nextInt(3, 10); val b = Random.nextInt(3, 10); val c = Random.nextInt(5, 21)
                mk("$a × $b + $c", a * b + c)
            }
            else -> { val b = Random.nextInt(3, 10); val q = Random.nextInt(4, 13); mk("${b * q} ÷ $b", q) }
        }
    }
}

@Composable
fun MathSprintGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf(genQuestion((level - 1) * 2)) }
    var answeredThis by remember { mutableStateOf(false) }
    var picked by remember { mutableIntStateOf(-1) }
    var revealAnswer by remember { mutableStateOf<Int?>(null) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var solved by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    val times = remember { mutableListOf<Double>() }
    var qStart by remember { mutableLongStateOf(0L) }

    fun nextQuestion() {
        q = genQuestion(combo + (level - 1) * 2); answeredThis = false; picked = -1; revealAnswer = null
        qStart = SystemClock.elapsedRealtime()
    }

    fun finish() {
        if (over) return; over = true
        val acc = if (tries > 0) solved * 100 / tries else 0
        val avg = if (times.isNotEmpty()) times.average() else 0.0
        onFinish(GameOutcome(GameId.MATH, score, listOf(
            "SOLVED $solved", "ACC $acc%", "AVG ${"%.1f".format(avg)}S", "TOP COMBO $bestCombo")))
    }
    val clock = rememberGameClock(40f, onEnd = { finish() })
    LaunchedEffect(Unit) { qStart = SystemClock.elapsedRealtime() }

    fun submit(idx: Int, value: Int) {
        if (over || answeredThis) return
        answeredThis = true; picked = idx; tries++
        times += (SystemClock.elapsedRealtime() - qStart) / 1000.0
        val right = value == q.answer
        if (right) {
            solved++; combo++; bestCombo = maxOf(bestCombo, combo)
            score += 10 + min(combo - 1, 6) * 2
            feedback.good(); feedback.buzz(8)
            scope.launch { delay(150); if (!over) nextQuestion() }
        } else {
            combo = 0; revealAnswer = q.answer
            clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
            scope.launch { delay(520); if (!over) nextQuestion() }
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "COMBO ×$combo", subtitle = "BUILD THE COMBO",
            timerFraction = clock.remaining / 40f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(q.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(rowStart * 2, rowStart * 2 + 1).forEach { i ->
                            val v = q.options[i]
                            val bg = when {
                                picked == i && v == q.answer -> Good
                                picked == i -> Bad
                                revealAnswer == v -> Good
                                else -> Panel
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.5.dp, if (bg == Panel) Ink else bg, RoundedCornerShape(12.dp))
                                    .clickable { submit(i, v) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$v", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = if (bg == Panel) Ink else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
