package com.neura.brain

import android.app.Application
import android.content.Context
import com.neura.brain.core.Feedback
import com.neura.brain.data.ProgressRepository

class NeuraApp : Application() {
    val repo by lazy { ProgressRepository(this) }
    val feedback by lazy { Feedback(this) }
}

val Context.neura: NeuraApp get() = applicationContext as NeuraApp
