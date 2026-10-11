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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.core.Feedback
import com.neura.brain.data.*
import com.neura.brain.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val SEQ_ROUNDS = 8

private data class SeqTrial(val terms: List<Int>, val answer: Int, val options: List<Int>)

private fun genSeq(level: Int): SeqTrial {
    val kinds = if (level <= 2) 3 else 5
    val terms: MutableList<Int>
    val answer: Int
    when (Random.nextInt(kinds)) {
        0 -> {                       // constant step
            var t = Random.nextInt(2, 16); val d = Random.nextInt(2, 11)
            terms = mutableListOf(t)
            repeat(4) { t += d; terms += t }
            answer = t + d
        }
        1 -> {                       // constant ratio
            var t = Random.nextInt(1, 4); val r = if (Random.nextBoolean()) 2 else 3
            terms = mutableListOf(t)
            repeat(4) { t *= r; terms += t }
            answer = t * r
        }
        2 -> {                       // growing gaps (quadratic)
            var t = Random.nextInt(1, 10); var d = Random.nextInt(2, 5)
            val acc = Random.nextInt(0, 2)
            terms = mutableListOf(t)
            repeat(4) { t += d; terms += t; d += acc }
            answer = t + d
        }
        3 -> {                       // alternating steps
            var t = Random.nextInt(2, 20)
            val d1 = Random.nextInt(2, 10); val d2 = Random.nextInt(2, 10)
            terms = mutableListOf(t)
            repeat(4) { k -> t += if (k % 2 == 0) d1 else d2; terms += t }
            answer = t + d1
        }
        else -> {                    // accumulation (fib-style)
            terms = mutableListOf(Random.nextInt(1, 7), Random.nextInt(2, 9))
            repeat(3) { terms += terms[terms.size - 1] + terms[terms.size - 2] }
            answer = terms[terms.size - 1] + terms[terms.size - 2]
        }
    }
    val opts = linkedSetOf(answer)
    var guard = 0
    while (opts.size < 4 && guard++ < 60) {
        val last = terms.last(); val prev = terms[terms.size - 2]
        val cand = when (Random.nextInt(4)) {
            0 -> answer + (last - prev)
            1 -> answer + Random.nextInt(-4, 5).let { if (it == 0) 3 else it }
            2 -> last + (last - prev) + Random.nextInt(-3, 4)
            else -> answer * 2 + Random.nextInt(-6, 7)
        }
        if (cand > 0) opts.add(cand)
    }
    return SeqTrial(terms, answer, opts.shuffled())
}

@Composable
fun NextNumberGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    var trial by remember { mutableStateOf(genSeq(level)) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var cracked by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }

    fun finish(timeBonus: Int) {
        if (over) return; over = true
        onFinish(GameOutcome(GameId.SEQ, score + timeBonus,
            listOf("SERIES $SEQ_ROUNDS/$SEQ_ROUNDS", "CRACKED $cracked", "MISSED $misses")))
    }
    val clock = rememberGameClock(60f, onEnd = { finish(0) })

    fun advance(afterMs: Long) {
        scope.launch {
            delay(afterMs)
            if (over) return@launch
            val next = round + 1
            if (next >= SEQ_ROUNDS) finish((clock.remaining * 2).toInt())
            else { round = next; trial = genSeq(level); picked = -1 }
        }
    }

    fun pick(i: Int, v: Int) {
        if (over || picked != -1) return
        picked = i
        if (v == trial.answer) {
            cracked++; score += 40
            feedback.good(); feedback.buzz(10)
            advance(300)
        } else {
            misses++; clock.penalize(3f)
            feedback.bad(); feedback.buzz(70)
            advance(650)
        }
    }

    val answerIdx = trial.options.indexOf(trial.answer)

    GameScaffold(
        GameHud(
            score = score, status = "SERIES ${round + 1} / $SEQ_ROUNDS",
            subtitle = "WHAT COMES NEXT?",
            tone = if (picked != -1 && picked != answerIdx) SubTone.ALERT else SubTone.NORMAL,
            timerFraction = clock.remaining / 60f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, LineStrong, RoundedCornerShape(16.dp))
                        .padding(vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        trial.terms.joinToString(",  "),
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("?", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Accent)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0, 1).forEach { rowStart ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(rowStart * 2, rowStart * 2 + 1).forEach { i ->
                            val v = trial.options.getOrElse(i) { -1 }
                            val bg = when {
                                picked != -1 && i == answerIdx -> Good
                                picked == i -> Bad
                                else -> Panel
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.5.dp, if (bg == Panel) Ink else bg, RoundedCornerShape(12.dp))
                                    .clickable { pick(i, v) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$v", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = if (bg == Panel) Ink else androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
