package com.neura.brain.game

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.graphicsLayer
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

private val WORDS = listOf(
    "WOLF", "STAR", "MIND", "TREE", "CODE", "GAME", "HAND", "ROAD", "WAVE", "FIRE",
    "SONG", "KING", "BOOK", "DOOR", "RAIN", "SNOW", "FISH", "BIRD", "MILK", "TIME",
    "GOLD", "SHIP", "MOON", "WIND", "SOUP", "DESK", "LAMP", "RING", "FROG", "LEAF",
    "MASK", "NOSE", "PALM", "ROPE", "SEED", "TENT", "VASE", "WOOL", "YARN", "ZERO",
    "APPLE", "BREAD", "CHAIR", "DANCE", "EARTH", "FLAME", "GRAPE", "HOUSE", "JUICE", "LEMON",
    "MUSIC", "NIGHT", "OCEAN", "PLANT", "RIVER", "SMILE", "TIGER", "WATER", "CLOUD", "BRAIN",
    "CANDY", "DOZEN", "EAGLE", "FROST", "GHOST", "HONEY", "IVORY", "KNIFE", "LIGHT", "MEDAL",
    "NURSE", "OLIVE", "PEARL", "QUEEN", "ROBOT", "STORM", "TRAIN", "VOICE", "WHEEL", "ZEBRA",
    "BRIDGE", "CANDLE", "CASTLE", "DRAGON", "ENGINE", "FLOWER", "GARDEN", "HAMMER", "ISLAND", "JUNGLE",
    "KETTLE", "LADDER", "MARKET", "NEEDLE", "ORANGE", "PENCIL", "PLANET", "PUZZLE", "RIBBON", "SILVER",
    "SPIDER", "SPRING", "STRING", "TICKET", "VELVET", "WINDOW", "WINTER", "MARBLE", "MIRROR", "PEBBLE"
)

private data class WRTile(val id: Int, val ch: Char)

private fun poolFor(level: Int) = when {
    level <= 2 -> WORDS.filter { it.length == 4 }
    level == 3 -> WORDS.filter { it.length <= 5 }
    else -> WORDS.filter { it.length >= 5 }
}

private fun scramble(word: String): List<WRTile> {
    var chars = word.toCharArray().toMutableList()
    var guard = 0
    while (String(chars.toCharArray()) == word && guard++ < 20) chars.shuffle()
    return chars.mapIndexed { i, c -> WRTile(i, c) }
}

@Composable
fun WordRushGame(feedback: Feedback, level: Int = 1, onQuit: () -> Unit, onFinish: (GameOutcome) -> Unit) {
    val scope = rememberCoroutineScope()
    var over by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var solved by remember { mutableIntStateOf(0) }
    var skips by remember { mutableIntStateOf(0) }
    var word by remember { mutableStateOf(poolFor(level).random()) }
    var tiles by remember { mutableStateOf(scramble(word)) }
    var placed by remember { mutableStateOf(listOf<Int>()) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var lastWrong by remember { mutableStateOf(false) }

    fun newWord() {
        val w = poolFor(level).random()
        word = w; tiles = scramble(w); placed = emptyList(); locked = false; lastWrong = false
    }
    fun finish() {
        if (over) return; over = true
        val attempts = solved + skips
        val acc = if (attempts > 0) solved * 100 / attempts else 0
        onFinish(GameOutcome(GameId.WORD, score, listOf("WORDS $solved", "SKIPS $skips", "TOP STREAK $best")))
    }
    val clock = rememberGameClock(45f, onEnd = { finish() })

    fun place(t: WRTile) {
        if (over || locked || t.id in placed) return
        placed = placed + t.id
        feedback.tick(); feedback.buzz(6)
        if (placed.size == word.length) {
            locked = true
            val guess = placed.map { id -> tiles.first { it.id == id }.ch }.joinToString("")
            if (guess == word) {
                solved++; streak++; best = maxOf(best, streak)
                score += 40 + min(streak - 1, 5) * 4
                lastWrong = false
                feedback.good(); feedback.buzz(8)
                scope.launch { delay(280); if (!over) newWord() }
            } else {
                lastWrong = true; shakeKey++
                feedback.bad(); feedback.buzz(60)
                scope.launch { delay(480); if (!over) { placed = emptyList(); locked = false } }
            }
        }
    }
    fun unplace(id: Int) { if (!locked && !over) placed = placed - id }
    fun hint() {
        if (over || locked) return
        val need = word[placed.size]
        tiles.firstOrNull { it.ch == need && it.id !in placed }?.let {
            clock.penalize(2f); feedback.buzz(30); place(it)
        }
    }
    fun skip() {
        if (over || locked) return
        skips++; streak = 0; lastWrong = false
        clock.penalize(3f)
        feedback.tone(200.0, 120, Wave.SAW, 0.08)
        newWord()
    }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            repeat(2) { shake.snapTo(-5f); delay(45); shake.snapTo(5f); delay(45) }
            shake.snapTo(0f)
        }
    }

    GameScaffold(
        GameHud(
            score = score, status = "SOLVED $solved",
            subtitle = when {
                lastWrong -> "NOPE — REBUILD IT"
                locked -> "CORRECT"
                else -> "REBUILD THE WORD"
            },
            tone = when { lastWrong -> SubTone.ALERT; locked -> SubTone.GOOD; else -> SubTone.NORMAL },
            timerFraction = clock.remaining / 45f,
            timerSeconds = clock.remaining,
            timerPenaltyKey = clock.penaltyKey,
            level = level
        ), onQuit
    ) {
        Column(
            Modifier.fillMaxSize().graphicsLayer { translationX = shake.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                word.forEachIndexed { i, _ ->
                    val id = placed.getOrNull(i)
                    val ch = id?.let { t -> tiles.firstOrNull { it.id == t }?.ch }
                    Box(
                        Modifier.size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ch != null) Ink else Panel)
                            .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                            .clickable(enabled = ch != null && !locked) { id?.let(::unplace) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ch?.toString() ?: "",
                            color = Paper, fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t ->
                            val used = t.id in placed
                            Box(
                                Modifier.size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (used) Line else Panel)
                                    .border(1.5.dp, if (used) Line else Ink, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !used) { place(t) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${t.ch}", fontWeight = FontWeight.Bold, fontSize = 24.sp,
                                    color = if (used) Muted else Ink
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.width(280.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, LineStrong, RoundedCornerShape(10.dp))
                        .clickable { hint() },
                    contentAlignment = Alignment.Center
                ) { Text("HINT −2S", style = monoStyle(9.5.sp, color = InkSoft, ls = 1.2.sp)) }
                Box(
                    Modifier.weight(1f).height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, LineStrong, RoundedCornerShape(10.dp))
                        .clickable { skip() },
                    contentAlignment = Alignment.Center
                ) { Text("SKIP −3S", style = monoStyle(9.5.sp, color = InkSoft, ls = 1.2.sp)) }
            }
        }
    }
}
