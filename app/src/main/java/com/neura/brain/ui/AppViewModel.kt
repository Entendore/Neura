package com.neura.brain.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.neura.brain.core.Feedback
import com.neura.brain.core.ReminderScheduler
import com.neura.brain.data.*
import com.neura.brain.neura
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

data class ResultData(
    val gameId: GameId,
    val score: Int,
    val prevBest: Int,
    val isBest: Boolean,
    val chips: List<String>,
    val insight: String,
    val circuit: Boolean,
    val circuitIndex: Int,
    val circuitSize: Int,
    val levelBefore: Int = 0,
    val levelAfter: Int = 0,
    val vsAvg: Int? = null,
    val newMilestones: List<Milestone> = emptyList()
)

data class CircuitStep(val gameId: GameId, val score: Int, val isBest: Boolean)

class Circuit(val ids: List<GameId>, var index: Int, val results: MutableList<CircuitStep>)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.neura.repo
    private val feedback = app.neura.feedback
    private val json = Json { ignoreUnknownKeys = true }

    val progress: StateFlow<Progress> = repo.progress

    var currentGameId by mutableStateOf<GameId?>(null); private set
    var invocation by mutableIntStateOf(0); private set
    var inCircuit by mutableStateOf(false); private set
    var circuit by mutableStateOf<Circuit?>(null); private set
    var lastResult by mutableStateOf<ResultData?>(null); private set
    var reportTotal by mutableIntStateOf(0); private set
    var pendingMilestones by mutableStateOf(listOf<Milestone>()); private set

    init { feedback.muted = { repo.progress.value.muted } }

    fun levelFor(id: GameId): Int = Adaptive.clamp(progress.value.difficulty[id.name] ?: 1)

    fun startGame(id: GameId, circuitMode: Boolean = false) {
        currentGameId = id
        inCircuit = circuitMode
        lastResult = null
        invocation++
    }

    fun startCircuit() {
        val ids = circuitForToday()
        circuit = Circuit(ids, 0, mutableListOf())
        inCircuit = true
        persistCircuitSnapshot()
        startGame(ids.first(), circuitMode = true)
    }

    /** Rebuild an in-flight circuit from storage. False if none is live today. */
    fun resumeCircuit(): Boolean {
        val p = progress.value
        if (!p.circuitLive || p.circuitDay != LocalDate.now().toString()) return false
        val ids = p.circuitIds.mapNotNull { runCatching { GameId.valueOf(it) }.getOrNull() }
        if (ids.size != 3 || p.circuitIndex !in ids.indices) return false
        val c = Circuit(ids, p.circuitIndex, mutableListOf())
        p.circuitScores.forEachIndexed { i, s ->
            if (i < p.circuitIndex)
                c.results += CircuitStep(ids.getOrElse(i) { ids[0] }, s, p.circuitBests.getOrElse(i) { false })
        }
        circuit = c
        inCircuit = true
        startGame(ids[p.circuitIndex], circuitMode = true)
        return true
    }

    private fun persistCircuitSnapshot() {
        val c = circuit ?: return
        repo.update {
            it.copy(
                circuitLive = true,
                circuitDay = LocalDate.now().toString(),
                circuitIds = c.ids.map { g -> g.name },
                circuitIndex = c.index,
                circuitScores = c.results.map { r -> r.score },
                circuitBests = c.results.map { r -> r.isBest }
            )
        }
    }

    fun toggleMute() {
        repo.update { it.copy(muted = !it.muted) }
        if (!repo.progress.value.muted) feedback.tap()
    }

    fun resetAll() {
        repo.reset()
        circuit = null; lastResult = null; inCircuit = false
        pendingMilestones = emptyList()
        ReminderScheduler.cancel(getApplication())
    }

    fun setReminderEnabled(enabled: Boolean) {
        repo.update { it.copy(reminderEnabled = enabled) }
        val ctx = getApplication<Application>()
        if (enabled) ReminderScheduler.schedule(ctx, progress.value.reminderHour)
        else ReminderScheduler.cancel(ctx)
    }

    fun setReminderHour(hour: Int) {
        val h = ((hour % 24) + 24) % 24
        repo.update { it.copy(reminderHour = h) }
        if (progress.value.reminderEnabled)
            ReminderScheduler.schedule(getApplication(), h)
    }

    private fun grantMilestones(): List<Milestone> {
        val p = repo.progress.value
        val fresh = evaluateMilestones(p) - p.milestones.toSet()
        if (fresh.isEmpty()) return emptyList()
        repo.update { it.copy(milestones = it.milestones + fresh) }
        return MILESTONES.filter { it.id in fresh }
    }

    fun finishGame(outcome: GameOutcome): ResultData {
        val id = outcome.gameId
        val p = repo.progress.value
        val prev = p.bests[id.name] ?: 0
        val isBest = outcome.score > prev
        val today = LocalDate.now().toString()
        val streak = computeStreak(p.streakLast, p.streakCount, LocalDate.now())
        val lvlBefore = Adaptive.clamp(p.difficulty[id.name] ?: 1)
        val lvlAfter = Adaptive.adjust(lvlBefore, outcome.score.toDouble() / id.maxScore)
        val oldHist = p.history[id.name].orEmpty()
        val prev5 = oldHist.takeLast(5)
        val vsAvg = if (prev5.isNotEmpty())
            (outcome.score - prev5.map { it.s }.average()).roundToInt()
        else null
        val hist = (oldHist + HistoryEntry(outcome.score, System.currentTimeMillis())).takeLast(40)
        repo.update {
            it.copy(
                bests = it.bests + (id.name to max(prev, outcome.score)),
                history = it.history + (id.name to hist),
                sessions = it.sessions + 1,
                points = it.points + outcome.score,
                streakCount = streak,
                streakLast = today,
                difficulty = it.difficulty + (id.name to lvlAfter)
            )
        }
        if (inCircuit) {
            circuit?.results?.add(CircuitStep(id, outcome.score, isBest))
            persistCircuitSnapshot()
        }
        val fresh = grantMilestones()
        pendingMilestones = fresh                 // always overwritten → no stale badges
        if (fresh.isNotEmpty()) feedback.fanfare()
        val res = ResultData(
            gameId = id, score = outcome.score, prevBest = prev, isBest = isBest,
            chips = outcome.chips, insight = insightFor(id, outcome.score),
            circuit = inCircuit,
            circuitIndex = circuit?.index ?: 0,
            circuitSize = circuit?.ids?.size ?: 0,
            levelBefore = lvlBefore, levelAfter = lvlAfter,
            vsAvg = vsAvg, newMilestones = fresh
        )
        lastResult = res
        return res
    }

    /** Advance the circuit; returns the next GameId, or null when finished. */
    fun circuitAdvance(): GameId? {
        val c = circuit ?: return null
        return if (c.index + 1 < c.ids.size) {
            c.index++
            persistCircuitSnapshot()
            startGame(c.ids[c.index], circuitMode = true)
            c.ids[c.index]
        } else {
            val total = c.results.sumOf { it.score }
            val today = LocalDate.now().toString()
            repo.update {
                it.copy(
                    circuitLive = false,
                    circuitIds = emptyList(), circuitIndex = 0,
                    circuitScores = emptyList(), circuitBests = emptyList(),
                    circuitDate = today,
                    circuitTotal = if (it.circuitDate == today) max(it.circuitTotal, total) else total
                )
            }
            reportTotal = total
            pendingMilestones = grantMilestones()
            if (pendingMilestones.isNotEmpty()) feedback.fanfare()
            null
        }
    }

    fun clearCircuit() {
        circuit = null; lastResult = null
        pendingMilestones = emptyList()
    }

    fun exportData(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.encodeToString(repo.progress.value).toByteArray())
                }
            }
        }
    }

    fun importData(uri: Uri, onDone: (Boolean) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("empty")
                repo.update { json.decodeFromString<Progress>(text) }
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (ok) {
                    circuit = null; lastResult = null; inCircuit = false
                    val p = repo.progress.value
                    if (p.reminderEnabled) ReminderScheduler.schedule(app, p.reminderHour)
                    else ReminderScheduler.cancel(app)
                }
                onDone(ok)
            }
        }
    }
}
