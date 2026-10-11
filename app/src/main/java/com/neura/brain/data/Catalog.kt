package com.neura.brain.data

import androidx.annotation.DrawableRes
import com.neura.brain.R
import java.time.LocalDate
import java.util.Locale

enum class SubTone { NORMAL, ALERT, GOOD }

data class GameHud(
    val score: Int = 0,
    val lives: Int? = null,
    val maxLives: Int = 3,
    val status: String? = null,
    val subtitle: String = "",
    val tone: SubTone = SubTone.NORMAL,
    val timerFraction: Float? = null,
    val timerSeconds: Float? = null,
    val timerPenaltyKey: Int = 0,
    val level: Int? = null
)

data class GameOutcome(val gameId: GameId, val score: Int, val chips: List<String>)

enum class GameId(
    val num: String,
    val title: String,
    val domain: String,
    @DrawableRes val glyph: Int,
    val tag: String,
    val maxScore: Int,
    val implemented: Boolean,
    val how: String,
    val insights: List<String>
) {
    MATRIX(
        "01", "Memory Matrix", "Working memory", R.drawable.ic_matrix, "PATTERN RECALL", 800, true,
        "A pattern of tiles flashes on the grid. Hold it in your mind, then rebuild it from memory. Three lives — every wrong tile costs one.",
        listOf(
            "The pattern fades fast. Don't count tiles — see them as one shape and rebuild it.",
            "Solid recall. Chunk tiles into lines, corners and blocks, and the next level opens up.",
            "Your spatial sketchpad is holding whole patterns at once — keep widening the grid."
        )
    ),
    REACTION(
        "02", "Reaction", "Reflex speed", R.drawable.ic_bolt, "MILLISECONDS", 900, true,
        "Five rounds. The pad turns green after a random delay — tap the instant it does. Tap early and the round resets. Amber decoys may flash first: hold.",
        listOf(
            "Blink delays add up. Settle on the dot, breathe out, and move on the color change.",
            "A steady hand. Expect the green, but don't jump it — anticipation costs rounds.",
            "Under a 300ms average — that's the reflex arc barely bothering to check in."
        )
    ),
    STROOP(
        "03", "Stroop Rush", "Cognitive control", R.drawable.ic_venn, "INK OVER WORD", 600, true,
        "A color word appears, printed in a different ink. Tap the swatch that matches the INK — never the word. Misses burn 2 seconds.",
        listOf(
            "The word keeps winning. One trick: soften your focus until only the color remains.",
            "You're catching the interference. Slow the eye, not the hand — ink first, word never.",
            "The reading reflex never stood a chance — you're overriding it effortlessly."
        )
    ),
    MATH(
        "04", "Math Sprint", "Numerical speed", R.drawable.ic_calc, "COMBO MULTIPLIER", 500, true,
        "Forty seconds of arithmetic against the clock. Correct answers build a combo multiplier — misses burn 2 seconds.",
        listOf(
            "Accuracy beats speed here. A calm 80% out-scores a frantic streak of misses.",
            "Clean and quick. Skip counting the small ones — pattern recognition is faster.",
            "Running arithmetic like a co-processor. The combo multiplier loves you."
        )
    ),
    ECHO(
        "05", "Sequence Echo", "Sequencing", R.drawable.ic_wave, "ECHO IT BACK", 950, true,
        "The pads play a sequence — watch and listen, then echo it back. It grows by one step every round. Two misses end the run.",
        listOf(
            "Long chains collapse fast. Hum the tones in your head as they play — sound sticks.",
            "The sequence holds, then slips. Group the pads by melody, not by position.",
            "Echoing eight-plus steps back — your audio-spatial loop is in great shape."
        )
    ),
    ODD(
        "06", "Odd One Out", "Perception", R.drawable.ic_target, "SPOT THE DIFFERENCE", 550, true,
        "A field of near-identical dots hides one impostor. Tap it before the clock drains — each miss burns 3 seconds.",
        listOf(
            "The field blurs together. Soften your gaze — the odd dot pops in peripheral view.",
            "Good eye. Scan in a fixed sweep; random searching wastes the clock.",
            "Near-perceptual discrimination — the impostor barely stood a chance."
        )
    ),
    IMPULSE(
        "07", "Impulse", "Impulse control", R.drawable.ic_pulse, "TAP / HOLD", 500, true,
        "A stream of signals flashes by. Tap the moment you see green — but hold absolutely still on red. False alarms burn 2 seconds.",
        listOf(
            "The foot is on the gas before the eyes check the sign. Wait half a beat longer on every signal.",
            "Solid braking. You're separating the urge to move from the move itself.",
            "Near-perfect inhibition — the red light barely registered as a temptation."
        )
    ),
    NBACK(
        "08", "N-Back", "Working memory update", R.drawable.ic_loop, "2-BACK STREAM", 250, true,
        "Letters stream one at a time. Hit MATCH only when the current letter is the same as two steps back. Keep the stream live in your head.",
        listOf(
            "Two-back is genuinely hard — hold the last two letters like cards in a hand and refresh them under your breath.",
            "The mental queue is holding. You're updating, not just remembering — that's the hard part.",
            "Running a live 2-back buffer effortlessly — n-back regulars would call this fluent."
        )
    ),
    CHAIN(
        "09", "Number Chain", "Visual search", R.drawable.ic_route, "ORDER UNDER SPEED", 750, true,
        "Numbers are scattered across the field. Tap them in ascending order as fast as you can. Rounds grow — errors add hidden time.",
        listOf(
            "Eyes were hunting, not leading. Fixate the center and let peripheral vision find the next number.",
            "Clean sweeps. Your visual search is pairing with number order instead of hunting blind.",
            "Trail speed like this is what the classic attention test calls 'well within norm' — flattering you, really."
        )
    ),
    SHIFT(
        "10", "Rule Shift", "Task switching", R.drawable.ic_swap, "COLOR ↔ SHAPE", 600, true,
        "Two cards, one rule: match by COLOR or by SHAPE. The rule flips without warning — notice fast, answer faster. Misses burn 2 seconds.",
        listOf(
            "The old rule kept grabbing the answer. Pause a beat after every flip and re-read the badge.",
            "You're catching the shifts mid-flight. Flexibility is compounding with speed now.",
            "Rule-switching this fast is executive control showing off — keep the streaks honest."
        )
    ),
    WORD(
        "11", "Word Rush", "Verbal fluency", R.drawable.ic_text, "UNSCRAMBLE", 500, true,
        "A jumbled word sits in pieces. Tap the letters in order to rebuild it. Hints cost 2 seconds, skips cost 3. Forty-five seconds, as many words as you can.",
        listOf(
            "Letters weren't landing. Hunt common endings first — -ING, -ED, -ER — and build backwards from them.",
            "You're hearing the word inside the scramble — that's lexical retrieval warming up.",
            "Straight decoding — your dictionary hands you the word before the letters finish landing."
        )
    ),
    ROTATE(
        "12", "Mind's Eye", "Spatial rotation", R.drawable.ic_eye, "SAME OR MIRRORED?", 500, true,
        "Two shapes appear. The second is the first one turned — or flipped. Rotated copies count as SAME; reflections are MIRRORED. Wrong calls burn 2 seconds.",
        listOf(
            "Flips kept slipping through. Spin the shape in your head before judging — rotation is cheap, mirroring is not.",
            "Solid mental rotation. You're turning objects without moving your eyes — that's the parietal loop at work.",
            "Rotation this quick is chess-player territory — you're transforming, not comparing."
        )
    ),
    FLASH(
        "13", "Count Flash", "Numerosity", R.drawable.ic_dots, "DOTS IN A FLASH", 400, true,
        "Dots flash for a split second, then vanish. Call the count from four options. Eight rounds — and the flash gets shorter each time.",
        listOf(
            "Beyond four, everyone's estimating. Cluster the flash into groups of two-and-three and add the clusters.",
            "Subitizing is stretching — you're counting without counting, which is exactly the skill.",
            "You're reading quantities like words — approximate number system firing on all cylinders."
        )
    ),
    FLANK(
        "14", "Flanker", "Selective attention", R.drawable.ic_flank, "CENTER ONLY", 450, true,
        "Five arrows, one job: answer the CENTER arrow's direction. The outer arrows are noise — sometimes lying. Wrong direction burns 2 seconds.",
        listOf(
            "The flankers kept dragging the answer sideways. Narrow the eyes to a pinhole over the middle.",
            "Clean filtering. The noise registered but never reached the hand — that's real selective control.",
            "Near-total suppression — the flankers might as well not have existed."
        )
    ),
    REVERSE(
        "15", "Reverse Echo", "Backward span", R.drawable.ic_rewind, "PLAY IT BACKWARDS", 700, true,
        "Pads flash a sequence. Then play it back in REVERSE — last flash first. It grows every round; two misses end the run. Six pads, six tones.",
        listOf(
            "Backwards recall is a different muscle — hold the sequence, then walk it from the far end.",
            "You're not just storing, you're reversing — that's manipulation, the deep end of span.",
            "A reversed span this long would turn heads in a memory lab — no exaggeration."
        )
    ),
    DNB(
        "16", "Dual N-Back", "Dual-task memory", R.drawable.ic_dnb, "LETTER + POSITION", 320, true,
        "A grid position and a letter fire together, every beat. Tap LETTER when the letter repeats two steps back, POSITION when the cell does — either channel or both. The famous one.",
        listOf(
            "Two streams at once is the deep end. Park the letter in your voice and the cell in your eye.",
            "You're updating two mental slots per step — holding both channels live is elite territory.",
            "Running a live dual 2-back — this is the task studies tie to fluid intelligence gains."
        )
    ),
    EST(
        "17", "Estimate", "Number sense", R.drawable.ic_scale, "WHICH IS BIGGER?", 460, true,
        "Two values, one question: which is bigger? They're too close to compute in time — round hard and feel the magnitude. Wrong calls burn 2 seconds.",
        listOf(
            "Exact math is too slow here. 13 × 9 is 'about 120' — commit and move.",
            "Your number line is calibrating. Estimation is beating calculation — that's the goal.",
            "Approximate number system in top gear — you compare magnitudes like lengths."
        )
    ),
    VIGIL(
        "18", "Vigilance", "Sustained attention", R.drawable.ic_radar, "TAP ALL BUT 3", 250, true,
        "Numbers stream past, one per beat. Tap every single one — except 3. The trap repeats. Lapses burn a second; tapping the 3 burns three.",
        listOf(
            "Vigilance sags in waves — that's normal. Reset your focus on every new digit, never coast.",
            "Steady monitoring. The 'no 3' rule stays loaded without decaying — that's the skill.",
            "Rare-signal discipline like this is what air-traffic screening is built to test."
        )
    ),
    SEQ(
        "19", "Next Number", "Logical reasoning", R.drawable.ic_qmark, "CRACK THE RULE", 450, true,
        "A number series hides a rule — step, factor, alternation, growing gaps or accumulation. Find it, then pick what comes next. Misses burn 3 seconds. Eight series to crack, and leftover time pays a bonus.",
        listOf(
            "Test one hypothesis at a time: gaps first, then ratios, then 'does the gap itself grow?'.",
            "You're hypothesis-testing fluently — that's induction, the core of fluid reasoning.",
            "Rule extraction this fast is what the IQ batteries call matrix-level performance."
        )
    ),
    CODE(
        "20", "Symbol Code", "Associative speed", R.drawable.ic_key, "DECODE FROM KEY", 460, true,
        "Symbols map to digits — the key sits up top. Decode each probe by tapping its digit. The buttons reshuffle every tap, so the key is the only anchor. Higher levels add symbols.",
        listOf(
            "Coding is lookup speed. Trace with your eyes — symbol, key, digit — and trust the loop.",
            "The mapping is automating; you're starting to skip the key for some symbols.",
            "Near-instant decoding — digit-symbol speed like this is a classic g-loaded score."
        )
    );
}

fun insightFor(id: GameId, score: Int): String {
    val r = score.toDouble() / id.maxScore
    return id.insights[when { r >= 0.7 -> 2; r >= 0.35 -> 1; else -> 0 }]
}

fun circuitForToday(): List<GameId> {
    val pool = GameId.entries.filter { it.implemented }
    val day = LocalDate.now().toEpochDay()
    return List(minOf(3, pool.size)) { pool[((day + it * 2) % pool.size).toInt()] }
}

fun computeStreak(lastDate: String, count: Int, today: LocalDate): Int = when (lastDate) {
    today.toString() -> count
    today.minusDays(1).toString() -> count + 1
    else -> 1
}

fun fmt(n: Int): String = "%,d".format(Locale.US, n)
