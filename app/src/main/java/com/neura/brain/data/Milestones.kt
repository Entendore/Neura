package com.neura.brain.data

import androidx.annotation.DrawableRes
import com.neura.brain.R

data class Milestone(
    val id: String,
    val title: String,
    val desc: String,
    @DrawableRes val icon: Int
)

val MILESTONES: List<Milestone> = listOf(
    Milestone("first_run", "First Spark", "Complete your first drill", R.drawable.ic_spark),
    Milestone("runs_10", "Warmed Up", "Complete 10 drills", R.drawable.ic_bolt),
    Milestone("runs_50", "Half Century", "Complete 50 drills", R.drawable.ic_matrix),
    Milestone("runs_100", "Centurion", "Complete 100 drills", R.drawable.ic_dnb),
    Milestone("streak_3", "Chain III", "Reach a 3-day streak", R.drawable.ic_flame),
    Milestone("streak_7", "Weeklong", "Reach a 7-day streak", R.drawable.ic_flame),
    Milestone("streak_30", "Monthlong", "Reach a 30-day streak", R.drawable.ic_flame),
    Milestone("circuit_done", "Circuit Closed", "Finish a daily circuit", R.drawable.ic_venn),
    Milestone("calibrated_all", "Full Deck", "Play every drill at least once", R.drawable.ic_loop),
    Milestone("lv5_any", "Peak LV5", "Reach LV5 on any drill", R.drawable.ic_pulse),
    Milestone("lv5_three", "Triple Peak", "Reach LV5 on three drills", R.drawable.ic_swap),
    Milestone("mastered", "Mastered", "Score 90%+ of a drill's max", R.drawable.ic_target),
    Milestone("points_5k", "5K Club", "Bank 5,000 lifetime points", R.drawable.ic_calc),
    Milestone("points_25k", "25K Club", "Bank 25,000 lifetime points", R.drawable.ic_key)
)

/** Pure unlock check. Unlocked ids are unioned into storage, so badges are permanent. */
fun evaluateMilestones(p: Progress): Set<String> {
    val s = HashSet<String>()
    if (p.sessions >= 1) s += "first_run"
    if (p.sessions >= 10) s += "runs_10"
    if (p.sessions >= 50) s += "runs_50"
    if (p.sessions >= 100) s += "runs_100"
    if (p.streakCount >= 3) s += "streak_3"
    if (p.streakCount >= 7) s += "streak_7"
    if (p.streakCount >= 30) s += "streak_30"
    if (p.circuitTotal > 0) s += "circuit_done"
    if (p.bests.size >= GameId.entries.size) s += "calibrated_all"
    val peaks = p.difficulty.count { it.value >= Adaptive.MAX }
    if (peaks >= 1) s += "lv5_any"
    if (peaks >= 3) s += "lv5_three"
    if (GameId.entries.any { (p.bests[it.name] ?: 0) >= it.maxScore * 9 / 10 }) s += "mastered"
    if (p.points >= 5_000) s += "points_5k"
    if (p.points >= 25_000) s += "points_25k"
    return s
}
