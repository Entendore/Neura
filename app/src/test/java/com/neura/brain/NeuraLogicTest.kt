package com.neura.brain

import com.neura.brain.data.Adaptive
import com.neura.brain.data.GameId
import com.neura.brain.data.MILESTONES
import com.neura.brain.data.Progress
import com.neura.brain.data.circuitForToday
import com.neura.brain.data.computeStreak
import com.neura.brain.data.evaluateMilestones
import com.neura.brain.data.insightFor
import com.neura.brain.game.flankerCongruentProb
import com.neura.brain.game.shiftFlipBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NeuraLogicTest {

    @Test fun `insight tiers switch at 35 and 70 percent`() {
        val g = GameId.MATRIX                                  // max 800
        assertEquals(g.insights[0], insightFor(g, 100))        // 12.5%
        assertEquals(g.insights[1], insightFor(g, 280))        // exactly 35%
        assertEquals(g.insights[1], insightFor(g, 500))        // 62.5%
        assertEquals(g.insights[2], insightFor(g, 560))        // exactly 70%
    }

    @Test fun `adaptive promotes holds and demotes with clamps`() {
        assertEquals(2, Adaptive.adjust(1, 0.85))
        assertEquals(5, Adaptive.adjust(5, 0.99))              // capped at MAX
        assertEquals(3, Adaptive.adjust(3, 0.50))              // dead zone holds
        assertEquals(2, Adaptive.adjust(3, 0.20))
        assertEquals(1, Adaptive.adjust(1, 0.10))              // floored at MIN
    }

    @Test fun `streak same day keeps next day grows gap resets`() {
        val today = LocalDate.of(2025, 1, 10)
        assertEquals(4, computeStreak("2025-01-10", 4, today))
        assertEquals(5, computeStreak("2025-01-09", 4, today))
        assertEquals(1, computeStreak("2025-01-05", 4, today))
        assertEquals(1, computeStreak("", 0, today))
    }

    @Test fun `daily circuit is deterministic distinct and fully implemented`() {
        val a = circuitForToday()
        assertEquals(a, circuitForToday())
        assertEquals(3, a.size)
        assertEquals(3, a.toSet().size)
        assertTrue(a.all { it.implemented })
    }

    @Test fun `flanker and shift tuning helpers clamp at sensible floors`() {
        assertEquals(0.65f, flankerCongruentProb(1), 1e-4f)
        assertEquals(0.45f, flankerCongruentProb(5), 1e-4f)
        assertEquals(0.40f, flankerCongruentProb(9), 1e-4f)   // never below 40% congruent
        assertEquals(4, shiftFlipBase(1))
        assertEquals(3, shiftFlipBase(2))
        assertEquals(2, shiftFlipBase(5))
        assertEquals(2, shiftFlipBase(99))                     // flips never faster than every 2 trials
    }

    @Test fun `milestones unlock in tiers and cover the full list at maxed progress`() {
        assertTrue(evaluateMilestones(Progress()).isEmpty())
        assertEquals(setOf("first_run"), evaluateMilestones(Progress(sessions = 1)))
        val mid = evaluateMilestones(Progress(sessions = 12, streakCount = 7, points = 6_000))
        assertTrue(mid.containsAll(listOf("first_run", "runs_10", "streak_3", "streak_7", "points_5k")))
        assertTrue("streak_30" !in mid)
        val maxed = Progress(
            sessions = 100, points = 30_000, streakCount = 30, circuitTotal = 900,
            bests = GameId.entries.associate { it.name to (it.maxScore * 9 / 10) },
            difficulty = GameId.entries.associate { it.name to 5 }
        )
        assertEquals(MILESTONES.map { it.id }.toSet(), evaluateMilestones(maxed))
    }
}
