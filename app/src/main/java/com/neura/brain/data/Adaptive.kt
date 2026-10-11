package com.neura.brain.data

object Adaptive {
    const val MIN = 1
    const val MAX = 5
    fun clamp(v: Int) = v.coerceIn(MIN, MAX)

    /** Promote at >=70% of a drill's max score, demote below 35%, hold between. */
    fun adjust(current: Int, scoreRatio: Double): Int = when {
        scoreRatio >= 0.70 -> clamp(current + 1)
        scoreRatio < 0.35 -> clamp(current - 1)
        else -> clamp(current)
    }
}
