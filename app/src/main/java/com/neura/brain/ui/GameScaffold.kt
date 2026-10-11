package com.neura.brain.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.R
import com.neura.brain.data.GameHud
import com.neura.brain.data.GameId
import com.neura.brain.data.SubTone
import com.neura.brain.data.fmt
import kotlinx.coroutines.delay

@Composable
fun GameScaffold(hud: GameHud, onQuit: () -> Unit, content: @Composable () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(2200); armed = false } }
    val requestQuit = { if (!armed) armed = true else onQuit() }
    BackHandler { requestQuit() }

    val scorePop = remember { Animatable(1f) }
    LaunchedEffect(hud.score) {
        if (hud.score > 0) { scorePop.jumpTo(1.3f); scorePop.animateTo(1f, tween(160)) }
    }
    val penaltyFlash = remember { Animatable(0f) }
    LaunchedEffect(hud.timerPenaltyKey) {
        if (hud.timerPenaltyKey > 0) { penaltyFlash.snapTo(1f); penaltyFlash.animateTo(0f, tween(400)) }
    }
    val subColor = when (hud.tone) {
        SubTone.NORMAL -> Muted; SubTone.ALERT -> Bad; SubTone.GOOD -> Good
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp)
                    .then(if (armed) Modifier.border(1.5.dp, Bad, RoundedCornerShape(8.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { requestQuit() },
                contentAlignment = Alignment.Center
            ) {
                if (armed) Text("QUIT?", style = monoStyle(9.sp, color = Bad, weight = FontWeight.SemiBold))
                else Icon(painterResource(R.drawable.ic_x), null, tint = Ink, modifier = Modifier.size(20.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    hud.lives?.let { lives ->
                        repeat(hud.maxLives) { i ->
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(if (i < lives) Ink else Color.Transparent)
                                    .then(if (i < lives) Modifier else Modifier.border(1.5.dp, LineStrong, CircleShape))
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    hud.level?.let { lv ->
                        Text("LV$lv", style = monoStyle(8.5.sp, color = Accent, weight = FontWeight.SemiBold))
                        Spacer(Modifier.width(7.dp))
                    }
                    hud.status?.let { Text(it, style = monoStyle(10.sp, color = Ink)) }
                }
            }
            Text(
                fmt(hud.score),
                style = monoStyle(15.sp, color = Ink, weight = FontWeight.SemiBold),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .graphicsLayer { scaleX = scorePop.value; scaleY = scorePop.value }
            )
        }
        Text(
            hud.subtitle,
            style = monoStyle(10.sp, color = subColor, ls = 1.7.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
        )
        hud.timerFraction?.let { frac ->
            val low = (hud.timerSeconds ?: 99f) < 5f
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Line)) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth(frac.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (low) Bad else Accent)
                    )
                }
                val timeColor = if (low) Bad else lerp(Ink, Bad, penaltyFlash.value)
                Text(
                    hud.timerSeconds?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "",
                    style = monoStyle(11.sp, color = timeColor, weight = FontWeight.SemiBold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(46.dp)
                )
            }
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 16.dp)) { content() }
    }
}

@Composable
fun IntroPane(
    id: GameId,
    isCircuit: Boolean,
    circuitIndex: Int,
    circuitSize: Int,
    best: Int,
    onBegin: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).border(2.dp, Ink, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(id.glyph), null, tint = Ink, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("DRILL ${id.num} · ${id.domain.uppercase()}", style = monoStyle(9.5.sp, color = Accent, ls = 1.6.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(id.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        Spacer(Modifier.height(10.dp))
        Text(id.how, fontSize = 13.5.sp, lineHeight = 20.sp, color = InkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            if (best > 0) "PERSONAL BEST — ${fmt(best)}" else "NO RECORD YET",
            style = monoStyle(9.5.sp, color = Muted, ls = 1.4.sp)
        )
        Spacer(Modifier.height(24.dp))
        val label = when {
            !id.implemented -> "COMING NEXT UPDATE"
            isCircuit -> "DRILL ${circuitIndex + 1} OF $circuitSize — BEGIN"
            else -> "BEGIN"
        }
        PrimaryButton(label, Modifier.width(280.dp), accent = id.implemented, enabled = id.implemented, onClick = onBegin)
    }
}
