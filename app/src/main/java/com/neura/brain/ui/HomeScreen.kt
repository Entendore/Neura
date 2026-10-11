package com.neura.brain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.brain.R
import com.neura.brain.data.GameId
import com.neura.brain.data.Progress
import com.neura.brain.data.circuitForToday
import com.neura.brain.data.fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.minOf
import kotlin.math.roundToInt

private fun brainIndex(p: Progress): Pair<Int, Int> {
    var sum = 0.0; var n = 0
    GameId.entries.forEach { g ->
        val b = p.bests[g.name] ?: 0
        if (b > 0) { sum += minOf(100.0, b * 100.0 / g.maxScore); n++ }
    }
    return if (n > 0) (sum / n).roundToInt() to n else 0 to 0
}

private fun agoLabel(ts: Long, today: LocalDate): String {
    if (ts == 0L) return "NEVER"
    val d = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(d, today)
    return when {
        days <= 0 -> "TODAY"
        days == 1L -> "YESTERDAY"
        else -> "${days}D AGO"
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartGame: (GameId) -> Unit,
    onStartCircuit: () -> Unit,
    onResumeCircuit: () -> Unit,
    onOpenStats: () -> Unit
) {
    val p by vm.progress.collectAsState()
    val (index, calibrated) = brainIndex(p)
    val picks = circuitForToday()
    val today = LocalDate.now()
    val doneToday = p.circuitDate == today.toString()
    val resumable = p.circuitLive && p.circuitDay == today.toString() &&
        p.circuitIndex < p.circuitIds.size && !doneToday
    val dateLabel = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US).format(today).uppercase(Locale.US)
    val topDrill = GameId.entries
        .map { it to (p.bests[it.name] ?: 0) / it.maxScore.toFloat() }
        .filter { it.second > 0 }
        .maxByOrNull { it.second }?.first
    val week = (6 downTo 0).map { off ->
        val d = today.minusDays(off.toLong())
        d to p.history.values.sumOf { list ->
            list.count { Instant.ofEpochMilli(it.d).atZone(ZoneId.systemDefault()).toLocalDate() == d }
        }
    }
    val due = GameId.entries.filter { it !in picks }
        .minByOrNull { g -> p.history[g.name]?.lastOrNull()?.d ?: 0L }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(painterResource(R.drawable.ic_spark), null, tint = Accent, modifier = Modifier.size(18.dp))
                Text("NEURA", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.4.sp)
            }
            Spacer(Modifier.weight(1f))
            IconHitBox(onClick = { vm.toggleMute() }) {
                Icon(
                    painterResource(if (p.muted) R.drawable.ic_mute else R.drawable.ic_sound),
                    if (p.muted) "Unmute" else "Mute", tint = Ink, modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrainGauge(index, calibrated)
                Spacer(Modifier.width(18.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .drawBehind {
                            drawLine(Line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.5.dp.toPx())
                        }
                        .padding(start = 16.dp)
                ) {
                    HeroStat(R.drawable.ic_flame, "${p.streakCount}", "DAY STREAK")
                    HLine()
                    HeroStat(null, "${p.sessions}", "DRILLS DONE")
                    HLine()
                    HeroStat(null, topDrill?.title?.uppercase(Locale.US) ?: "—", "TOP DRILL", small = true)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MOMENTUM — LAST 7 DAYS", style = monoStyle(8.5.sp, color = Muted, ls = 1.3.sp))
                Spacer(Modifier.weight(1f))
                Text(
                    "${p.streakCount}-DAY STREAK",
                    style = monoStyle(8.5.sp, color = if (p.streakCount > 0) Accent else Muted, ls = 1.3.sp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { (d, n) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(12.dp).clip(CircleShape).background(
                                when {
                                    n > 0 && d == today -> Accent
                                    n == 0 -> Line
                                    n == 1 -> Ink.copy(alpha = 0.45f)
                                    else -> Ink
                                }
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            d.dayOfWeek.name.first().toString(),
                            style = monoStyle(
                                7.5.sp,
                                color = if (d == today) Ink else Muted,
                                weight = if (d == today) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Small drills,", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp, letterSpacing = (-0.4).sp)
            Text(
                buildAnnotatedString {
                    append("measurable")
                    withStyle(SpanStyle(color = Accent)) { append(" gains.") }
                },
                fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp, letterSpacing = (-0.4).sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Twenty exercises across memory, language, space, numbers and control. Five minutes a day — that's the whole protocol.",
                fontSize = 13.5.sp, lineHeight = 21.sp, color = InkSoft
            )

            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ink).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY'S CIRCUIT — $dateLabel", style = monoStyle(9.sp, color = AccentSoft, ls = 1.3.sp), modifier = Modifier.weight(1f))
                    Text(
                        when {
                            resumable -> "RESUME · ${p.circuitIndex}/${p.circuitIds.size}"
                            doneToday -> "DONE · ${fmt(p.circuitTotal)} PTS"
                            else -> "${picks.size} DRILLS · ~${picks.size + 1} MIN"
                        },
                        style = monoStyle(9.sp, color = Color(0x8CF2EDE3), ls = 1.3.sp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    picks.forEachIndexed { i, gid ->
                        if (i > 0) Icon(painterResource(R.drawable.ic_chev), null, tint = Color(0x66F2EDE3), modifier = Modifier.size(11.dp))
                        val doneChip = resumable && i < p.circuitIndex
                        Column(
                            Modifier
                                .weight(1f)
                                .border(1.dp, if (doneChip) AccentSoft else Color(0x38F2EDE3), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 8.dp)
                        ) {
                            Text(gid.num, style = monoStyle(9.5.sp, color = AccentSoft))
                            Spacer(Modifier.height(3.dp))
                            Text(
                                gid.title.uppercase(Locale.US),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp, color = Paper, maxLines = 2
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                val (btnLabel, btnAction) = when {
                    resumable -> "RESUME CIRCUIT · ${p.circuitIndex + 1} OF ${p.circuitIds.size}" to onResumeCircuit
                    doneToday -> "REPLAY CIRCUIT" to onStartCircuit
                    else -> "START CIRCUIT" to onStartCircuit
                }
                PrimaryButton(btnLabel, accent = true, trailingIcon = R.drawable.ic_arrow, onClick = btnAction)
            }

            due?.let { g ->
                val last = p.history[g.name]?.lastOrNull()?.d ?: 0L
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, LineStrong, RoundedCornerShape(12.dp))
                        .clickable { onStartGame(g) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Accent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(g.glyph), null, tint = Accent, modifier = Modifier.size(17.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DUE FOR A RUN", style = monoStyle(8.sp, color = Accent, ls = 1.3.sp))
                        Spacer(Modifier.height(2.dp))
                        Text(g.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(agoLabel(last, today), style = monoStyle(8.5.sp, color = Muted, ls = 1.1.sp))
                    Spacer(Modifier.width(10.dp))
                    Icon(painterResource(R.drawable.ic_chev), null, tint = Muted, modifier = Modifier.size(15.dp))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ALL DRILLS", style = monoStyle(9.5.sp, color = Muted, ls = 1.5.sp))
                Text(
                    "$calibrated/${GameId.entries.size} CALIBRATED",
                    style = monoStyle(9.5.sp, color = Muted, ls = 1.5.sp)
                )
            }
            GameId.entries.forEachIndexed { i, g ->
                val best = p.bests[g.name] ?: 0
                val lvl = p.difficulty[g.name] ?: 1
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onStartGame(g) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(g.num, style = monoStyle(10.5.sp, color = Muted), modifier = Modifier.width(24.dp))
                    Spacer(Modifier.width(9.dp))
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).border(1.5.dp, Ink, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(g.glyph), null, tint = Ink, modifier = Modifier.size(19.dp)) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            buildAnnotatedString {
                                append(g.domain.uppercase(Locale.US))
                                if (lvl > 1) {
                                    withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) { append("  ·  LV$lvl") }
                                }
                                if (g in picks) {
                                    withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                                        append("  ·  IN TODAY'S CIRCUIT")
                                    }
                                }
                            },
                            style = monoStyle(9.sp, color = Muted, ls = 1.2.sp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BEST", style = monoStyle(8.sp, color = Muted, ls = 1.4.sp))
                        Text(if (best > 0) fmt(best) else "—", style = monoStyle(11.5.sp, color = Ink, weight = FontWeight.SemiBold))
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(painterResource(R.drawable.ic_chev), null, tint = Muted, modifier = Modifier.size(15.dp))
                }
                if (i < GameId.entries.lastIndex) HLine()
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.clickable(onClick = onOpenStats).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("STATS & HISTORY", style = monoStyle(10.5.sp, color = Ink, weight = FontWeight.SemiBold, ls = 1.4.sp))
                Icon(painterResource(R.drawable.ic_arrow), null, tint = Ink, modifier = Modifier.size(14.dp))
            }
            Text("DATA NEVER LEAVES THIS DEVICE", style = monoStyle(8.sp, color = Muted, ls = 1.5.sp))
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HeroStat(iconRes: Int?, value: String, label: String, small: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        iconRes?.let {
            Icon(painterResource(it), null, tint = Accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            value,
            fontSize = if (small) 12.sp else 20.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 130.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = monoStyle(8.5.sp, color = Muted, ls = 1.3.sp))
    }
}
