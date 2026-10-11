package com.neura.brain.game

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private typealias Cells = List<Pair<Int, Int>>

private val BASE_SHAPES: List<Cells> = listOf(
    listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2),   // V
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 3),   // long L
    listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1, 0 to 2),   // P
    listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),           // S
    listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),           // N-ish
    listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1, 1 to 2)    // T-ish
)

private fun norm(c: Cells): Cells {
    val mx = c.minOf { it.first }; val my = c.minOf { it.second }
    return c.map { (x, y) -> x - mx to y - my }.sortedBy { it.first * 10 + it.second }
}

private fun rot90(c: Cells) = norm(c.map { (x, y) -> y to -x })
private fun mirror(c: Cells) = norm(c.map { (x, y) -> -x to y })

private fun rotations(c: Cells): List<Cells> {
    val out = ArrayList<Cells>(4)
    var cur = norm(c)
    repeat(4) { out += cur; cur = rot90(cur) }
    return out
}

/** Only chiral shapes make "mirrored" meaningfully different. */
private val SHAPES: List<Cells> = BASE_SHAPES.map(::norm).filter { mirror(it) !in rotations(it) }

private data class METrial(val target: Cells, val cand: Cells, val same: Boolean)

private fun genMETrial(): METrial {
    val shape = SHAPES.random()
    val same = Random.nextBoolean()
    val r = if (same) Random.nextInt(1, 4) else Random.nextInt(0, 4)
    val cand = if (same) rotations(shape)[r] else rotations(mirror(shape))[r]
    return METrial(norm(shape), cand, same)
}

@Composable
private fun ShapeView(cells: Cells, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cell = size.minDimension / 4f
        val w = cells.maxOf { it.first } + 1
        val h = cells.maxOf { it.second } + 1
        val ox = (size.width - w * cell) / 2f
        val oy = (size.height - h * cell) / 2f
        cells.forEach { (x, y) ->
            drawRoundRect(
                color = Ink,
                topLeft = Offset(ox + x * cell + 1.5f, oy + y * cell + 1.5f),
                size = Size(cell - 3f, cell - 3f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
fun MindEyeGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var trial by remember { mutableStateOf(genMETrial()) }
    var picked by remember { mutableStateOf<Boolean?>(null) }
    var hideA by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }

    fun finish() {
        if (over) return; over = true
        val acc = if (answered > 0) correct * 100 / answered else 0
        onFinish(GameOutcome(GameId.ROTATE, score, listOf("TRIALS $answered", "ACC $acc%", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(40f, onEnd = { finish() })

    // LV4+: A blinks out after 1.3s — judge B from a mental image.
    LaunchedEffect(trial) {
        if (level >= 4) {
            hideA = false
            delay(1300)
            if (!locked && !over) hideA = true
        }
    }

    fun submit(saysSame: Boolean) {
        if (over || locked) return
        locked = true; picked = saysSame; answered++
        if (level >= 4) hideA = false
        val right = saysSame == trial.same
        if (right) {
            correct++; streak++; best = maxOf(best, streak)
            score += 25 + minOf(streak - 1, 5) * 4
            feedback.good(); feedback.buzz(8)
        } else {
            streak = 0
            clock.penalize(2f)
            feedback.bad(); feedback.buzz(70)
        }
        scope.launch {
            delay(if (right) 150 else 400)
            if (over) return@launch
            trial = genMETrial(); picked = null; locked = false
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "STREAK $streak",
            subtitle = if (level >= 4 && hideA) "B FROM MEMORY — RECALL A" else "IS B THE SAME SHAPE AS A — JUST TURNED?",
            timerFraction = clock.remaining / 40f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hideA) {
                            Box(
                                Modifier.fillMaxWidth(0.62f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("?", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Muted) }
                        } else {
                            ShapeView(trial.target, Modifier.fillMaxWidth(0.62f).aspectRatio(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("A", style = monoStyle(11.sp, color = Muted, weight = FontWeight.SemiBold))
                    }
                    Box(Modifier.width(1.5.dp).fillMaxHeight(0.55f).background(Line))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        ShapeView(trial.cand, Modifier.fillMaxWidth(0.62f).aspectRatio(1f))
                        Spacer(Modifier.height(10.dp))
                        Text("B", style = monoStyle(11.sp, color = Accent, weight = FontWeight.SemiBold))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fun answerBtn(text: String, bg: androidx.compose.ui.graphics.Color, active: Boolean, action: () -> Unit) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .then(if (active) Modifier.border(3.dp, Ink, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { action() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text, textAlign = TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                }
                answerBtn("SAME\n(ROTATED)", Good, picked == true) { submit(true) }
                answerBtn("MIRRORED\n(FLIPPED)", Bad, picked == false) { submit(false) }
            }
        }
    }
}
