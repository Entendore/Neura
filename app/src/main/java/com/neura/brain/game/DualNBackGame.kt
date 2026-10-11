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
import kotlin.random.Random

private const val DNB_N = 2
private val DNB_LETTERS = listOf("C", "H", "K", "L", "Q", "R", "S", "T")

private data class DualStream(val letters: List<String>, val positions: List<Int>)

/** Independent channels; non-match trials are guaranteed NOT to be 2-back matches. */
private fun genDual(trials: Int, matchRate: Float): DualStream {
    val letters = ArrayList<String>(trials)
    val positions = ArrayList<Int>(trials)
    repeat(DNB_N) { letters += DNB_LETTERS.random(); positions += Random.nextInt(9) }
    for (i in DNB_N until trials) {
        letters += if (Random.nextFloat() < matchRate) letters[i - DNB_N]
        else { var c = DNB_LETTERS.random(); while (c == letters[i - DNB_N]) c = DNB_LETTERS.random(); c }
        positions += if (Random.nextFloat() < matchRate) positions[i - DNB_N]
        else { var p = Random.nextInt(9); while (p == positions[i - DNB_N]) p = Random.nextInt(9); p }
    }
    return DualStream(letters, positions)
}

@Composable
fun DualNBackGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val trials = 16 + level * 2
    val interval = 2400L - (level - 1) * 150L
    val stream = remember { genDual(trials, 0.30f) }

    var idx by remember { mutableIntStateOf(-1) }
    var respL by remember { mutableStateOf(false) }
    var respP by remember { mutableStateOf(false) }
    var flashL by remember { mutableStateOf(false) }
    var flashP by remember { mutableStateOf(false) }
    var hitL by remember { mutableIntStateOf(0) }; var hitP by remember { mutableIntStateOf(0) }
    var falseL by remember { mutableIntStateOf(0) }; var falseP by remember { mutableIntStateOf(0) }
    var missL by remember { mutableIntStateOf(0) }; var missP by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var cue by remember { mutableStateOf("") }

    fun finish() {
        if (over) return; over = true
        val hits = hitL + hitP
        val denom = hits + falseL + falseP + missL + missP
        val acc = if (denom > 0) hits * 100 / denom else 0
        onFinish(GameOutcome(GameId.DNB, score, listOf("HITS $hits", "FALSE ${falseL + falseP}", "ACC $acc%")))
    }

    LaunchedEffect(Unit) {
        delay(900)
        for (i in stream.letters.indices) {
            if (over) return@LaunchedEffect
            idx = i; respL = false; respP = false; flashL = false; flashP = false; cue = ""
            feedback.tick()
            delay(interval - 500)
            if (over) return@LaunchedEffect
            val isLM = i >= DNB_N && stream.letters[i] == stream.letters[i - DNB_N]
            val isPM = i >= DNB_N && stream.positions[i] == stream.positions[i - DNB_N]
            if (isLM && respL) { hitL++; score += 25 } else if (isLM) missL++
            else if (respL) { falseL++; score = (score - 12).coerceAtLeast(0) }
            if (isPM && respP) { hitP++; score += 25 } else if (isPM) missP++
            else if (respP) { falseP++; score = (score - 12).coerceAtLeast(0) }
            // Fixed: a clean non-match beat stays silent instead of shouting FALSE ALARM.
            val falseHit = (!isLM && respL) || (!isPM && respP)
            cue = when {
                isLM && isPM && respL && respP -> "DOUBLE HIT +50"
                isLM && respL -> "LETTER HIT +25"
                isPM && respP -> "POSITION HIT +25"
                (isLM && !respL) || (isPM && !respP) -> "MISSED ONE"
                falseHit -> "FALSE ALARM −12"
                else -> ""
            }
            delay(500)
        }
        finish()
    }

    fun press(letter: Boolean) {
        if (over || idx < DNB_N) return
        if (letter && !respL) { respL = true; flashL = true; feedback.tap(); feedback.buzz(6) }
        if (!letter && !respP) { respP = true; flashP = true; feedback.tap(); feedback.buzz(6) }
    }

    GameScaffold(
        GameHud(
            score = score,
            status = "TRIAL ${maxOf(1, idx + 1)} / ${stream.letters.size}",
            subtitle = "LETTER 2 BACK? · POSITION 2 BACK?",
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stream.letters.getOrElse(idx) { "" },
                        fontSize = 56.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    if (cue.isNotEmpty())
                        Text(cue, style = monoStyle(9.5.sp, color = Muted, ls = 1.2.sp))
                }
            }
            Column(
                Modifier.fillMaxWidth(0.62f).widthIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(3) { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeat(3) { c ->
                            val on = idx >= 0 && stream.positions[idx] == r * 3 + c
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) Ink else Panel)
                                    .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fun dnbBtn(label: String, active: Boolean, action: () -> Unit) {
                    Box(
                        Modifier.weight(1f).height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Accent else Ink)
                            .clickable { action() },
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = Paper, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
                dnbBtn("LETTER", flashL) { press(true) }
                dnbBtn("POSITION", flashP) { press(false) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
