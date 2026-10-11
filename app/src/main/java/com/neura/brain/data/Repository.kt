package com.neura.brain.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HistoryEntry(val s: Int, val d: Long)

@Serializable
data class Progress(
    val bests: Map<String, Int> = emptyMap(),
    val history: Map<String, List<HistoryEntry>> = emptyMap(),
    val sessions: Int = 0,
    val points: Int = 0,
    val streakCount: Int = 0,
    val streakLast: String = "",
    val muted: Boolean = false,
    val circuitDate: String = "",
    val circuitTotal: Int = 0,
    val difficulty: Map<String, Int> = emptyMap(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val circuitLive: Boolean = false,
    val circuitDay: String = "",
    val circuitIds: List<String> = emptyList(),
    val circuitIndex: Int = 0,
    val circuitScores: List<Int> = emptyList(),
    val circuitBests: List<Boolean> = emptyList(),
    val milestones: List<String> = emptyList()
)

internal val Context.neuraDataStore by preferencesDataStore(name = "neura")

class ProgressRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    internal companion object { internal val KEY = stringPreferencesKey("progress") }

    init {
        scope.launch {
            val raw = appContext.neuraDataStore.data.first()[KEY]
            if (raw != null) {
                runCatching { json.decodeFromString<Progress>(raw) }
                    .onSuccess { _progress.value = it }
            }
        }
    }

    fun update(transform: (Progress) -> Progress) {
        val next = transform(_progress.value)
        _progress.value = next
        scope.launch {
            runCatching { appContext.neuraDataStore.edit { it[KEY] = json.encodeToString(next) } }
        }
    }

    fun reset() = update { Progress() }
}
