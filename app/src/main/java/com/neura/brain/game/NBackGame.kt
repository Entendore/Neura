package com.neura.brain.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.core.Wave
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlin.random.Random

private val NBACK_LETTERS = listOf("C", "H", "K", "L", "Q", "R", "S", "T")

/** n letters with ~30% guaranteed 2-back matches. */
private fun genNBackSequence(n: Int = 24): List<String> {
    val seq = ArrayList<String>(n)
    seq += NBACK_LETTERS.random()
    seq += NBACK_LETTERS.random()
    val matchAt = (2 until n).shuffled().take((n * 0.3f).toInt().coerceAtLeast(3)).toSet()
    for (i in 2 until n) {
        seq += if (i in matchAt) seq[i - 2]
        else {
            var c = NBACK_LETTERS.random()
            while (c == seq[i - 2]) c = NBACK_LETTERS.random()
            c
        }
    }
    return seq
}

@Composable
fun NBackGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val seq = remember { genNBackSequence(18 + level * 2) }
    var idx by remember { mutableIntStateOf(-1) }
    var responded by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var falseAlarms by remember { mutableIntStateOf(0) }
    var rejects by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    var over by remember { mutableStateOf(false) }

    fun finish() {
        if (over) return; over = true
        val denom = hits + rejects + misses + falseAlarms
        val acc = if (denom > 0) (hits + rejects) * 100 / denom else 0
        onFinish(GameOutcome(GameId.NBACK, score, listOf("HITS $hits", "FALSE $falseAlarms", "ACC $acc%")))
    }

    LaunchedEffect(Unit) {
        delay(600)
        for (i in seq.indices) {
            if (over) return@LaunchedEffect
            idx = i; responded = false; label = ""
            delay(2200L - (level - 1) * 120L)
            if (over) return@LaunchedEffect
            if (!responded) {
                if (i >= 2 && seq[i] == seq[i - 2]) {
                    misses++; label = "MISSED"
                    feedback.tone(220.0, 120, Wave.SAW, 0.07)
                } else {
                    rejects++; score += 5; label = "+5"
                    feedback.tick(); feedback.buzz(5)
                }
            }
            delay(120)
        }
        finish()
    }

    fun respond() {
        if (over || responded || idx < 2) return
        responded = true
        if (seq[idx] == seq[idx - 2]) {
            hits++; score += 25; label = "MATCH +25"
            feedback.good(); feedback.buzz(10)
        } else {
            falseAlarms++; score = (score - 15).coerceAtLeast(0); label = "FALSE −15"
            feedback.bad(); feedback.buzz(80)
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "LETTER ${maxOf(1, idx + 1)} / ${seq.size}",
            subtitle = "IS IT THE SAME AS 2 LETTERS BACK?",
            tone = when {
                label.startsWith("FALSE") || label == "MISSED" -> SubTone.ALERT
                label.isNotEmpty() -> SubTone.GOOD
                else -> SubTone.NORMAL
            },
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(seq.getOrElse(idx) { "" }, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (label.isNotEmpty())
                        Text(label, style = monoStyle(11.sp, color = if (label.contains("FALSE") || label == "MISSED") Bad else Good))
                }
            }
            PrimaryButton("MATCH", onClick = { respond() })
            Spacer(Modifier.height(8.dp))
            Text("ONLY IF IT MATCHES TWO BACK", style = monoStyle(8.5.sp, color = Muted, ls = 1.4.sp))
            Spacer(Modifier.height(14.dp))
        }
    }
}
