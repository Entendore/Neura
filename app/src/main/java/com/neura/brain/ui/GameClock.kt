package com.neura.brain.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

class GameClock internal constructor() {
    var remaining by mutableFloatStateOf(0f)
        internal set
    var penaltyKey by mutableIntStateOf(0)
        private set
    fun penalize(seconds: Float) {
        remaining = (remaining - seconds).coerceAtLeast(0f)
        penaltyKey++
    }
}

@Composable
fun rememberGameClock(totalSeconds: Float, running: Boolean = true, onEnd: () -> Unit): GameClock {
    val clock = remember { GameClock() }
    val end = rememberUpdatedState(onEnd)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        clock.remaining = totalSeconds
        var last = withFrameNanos { it }
        var acc = 0f
        while (clock.remaining > 0f) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000_000f
                last = now
                acc += dt
                if (acc >= 0.1f) {   // update HUD state at 10 Hz, not per frame
                    clock.remaining = (clock.remaining - acc).coerceAtLeast(0f)
                    acc = 0f
                }
            }
        }
        end.value()
    }
    return clock
}
