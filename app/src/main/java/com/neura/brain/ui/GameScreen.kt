package com.neura.brain.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.neura.brain.data.GameId
import com.neura.brain.game.*
import com.neura.brain.neura

@Composable
fun GameScreen(vm: AppViewModel, onFinish: () -> Unit, onQuit: () -> Unit) {
    val id = vm.currentGameId
    if (id == null) {
        LaunchedEffect(Unit) { onQuit() }
        return
    }
    val p by vm.progress.collectAsState()
    val feedback = LocalContext.current.neura.feedback
    val view = LocalView.current
    var started by remember(vm.invocation) { mutableStateOf(false) }
    val lvl = remember(vm.invocation) { vm.levelFor(id) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize()) {
        if (started) {
            key(vm.invocation) {
                when (id) {
                    GameId.MATRIX  -> MemoryMatrixGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.REACTION-> ReactionGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.STROOP  -> StroopGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.MATH    -> MathSprintGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.ECHO    -> SequenceEchoGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.ODD     -> OddOneOutGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.IMPULSE -> ImpulseGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.NBACK   -> NBackGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.CHAIN   -> NumberChainGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.SHIFT   -> RuleShiftGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.WORD    -> WordRushGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.ROTATE  -> MindEyeGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.FLASH   -> CountFlashGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.FLANK   -> FlankerGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.REVERSE -> ReverseEchoGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.DNB     -> DualNBackGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.EST     -> EstimateGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.VIGIL   -> VigilGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.SEQ     -> NextNumberGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                    GameId.CODE    -> SymbolCodeGame(feedback, level = lvl, onQuit = onQuit) { vm.finishGame(it); onFinish() }
                }
            }
        }
        if (!started) {
            IntroPane(
                id = id,
                isCircuit = vm.inCircuit,
                circuitIndex = vm.circuit?.index ?: 0,
                circuitSize = vm.circuit?.ids?.size ?: 1,
                best = p.bests[id.name] ?: 0
            ) { started = true }
        }
    }
}
