package com.neura.brain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neura.brain.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent { NeuraTheme { Root() } }
    }
}

@Composable
fun Root() {
    val vm: AppViewModel = viewModel()
    val nav = rememberNavController()
    Box(
        Modifier.fillMaxSize().background(Paper)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        NavHost(nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm,
                    onStartGame = { vm.startGame(it); nav.navigateSingle("game") },
                    onStartCircuit = { vm.startCircuit(); nav.navigateSingle("game") },
                    onResumeCircuit = {
                        if (!vm.resumeCircuit()) vm.startCircuit()   // stale snapshot → fresh start
                        nav.navigateSingle("game")
                    },
                    onOpenStats = { nav.navigateSingle("stats") }
                )
            }
            composable("stats") { StatsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("game") {
                GameScreen(
                    vm,
                    onFinish = { nav.navigateSingle("result") },
                    onQuit = { nav.popBackStack("home", inclusive = false) }
                )
            }
            composable("result") {
                ResultScreen(
                    vm,
                    onHome = { nav.popBackStack("home", inclusive = false) },
                    onNext = {
                        if (vm.circuitAdvance() == null) nav.navigateSingle("report")
                        else nav.navigateSingle("game")
                    },
                    onRetry = {
                        vm.lastResult?.let { r -> vm.startGame(r.gameId); nav.navigateSingle("game") }
                    }
                )
            }
            composable("report") {
                CircuitReportScreen(
                    vm,
                    onDone = { vm.clearCircuit(); nav.popBackStack("home", inclusive = false) }
                )
            }
        }
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo("home") { inclusive = false }
    }
}
