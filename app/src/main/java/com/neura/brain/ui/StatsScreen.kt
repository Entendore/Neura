package com.neura.brain.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neura.brain.R
import com.neura.brain.data.GameId
import com.neura.brain.data.HistoryEntry
import com.neura.brain.data.MILESTONES
import com.neura.brain.data.evaluateMilestones
import com.neura.brain.data.fmt
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val p by vm.progress.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconHitBox(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_chevl), null, tint = Ink, modifier = Modifier.size(20.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("STATS & HISTORY", style = monoStyle(10.sp, color = Ink, weight = FontWeight.SemiBold, ls = 1.8.sp))
            }
            Spacer(Modifier.width(38.dp))
        }

        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
            ) {
                SummaryCell(fmt(p.points), "LIFETIME PTS", Modifier.weight(1f))
                Box(Modifier.width(1.5.dp).fillMaxHeight().background(LineStrong))
                SummaryCell("${p.sessions}", "DRILLS", Modifier.weight(1f))
                Box(Modifier.width(1.5.dp).fillMaxHeight().background(LineStrong))
                SummaryCell("${p.streakCount}", "DAY STREAK", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            MilestonesCard(evaluateMilestones(p) + p.milestones.toSet())
            Spacer(Modifier.height(12.dp))
            GameId.entries.forEach { g ->
                StatBlock(g, p.history[g.name].orEmpty(), p.bests[g.name] ?: 0)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(0.dp))
            ReminderCard(vm)
            DataCard(vm)
            ResetButton(vm)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, style = monoStyle(8.sp, color = Muted, ls = 1.3.sp))
    }
}

@Composable
private fun StatBlock(g: GameId, hist: List<HistoryEntry>, best: Int) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).border(1.5.dp, Ink, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(painterResource(g.glyph), null, tint = Ink, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(g.domain.uppercase(Locale.US), style = monoStyle(9.sp, color = Muted, ls = 1.2.sp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BEST", style = monoStyle(8.sp, color = Muted, ls = 1.4.sp))
                Text(if (best > 0) fmt(best) else "—", style = monoStyle(11.5.sp, color = Ink, weight = FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.height(13.dp))
        if (hist.isEmpty()) {
            Text(
                "NO DATA — PLAY THIS DRILL TO CALIBRATE",
                style = monoStyle(9.sp, color = Muted, ls = 1.0.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            )
        } else {
            Sparkline(hist.map { it.s }.takeLast(12), Modifier.fillMaxWidth().height(44.dp))
            Spacer(Modifier.height(9.dp))
            val avg = hist.map { it.s }.average().roundToInt()
            val last = DateTimeFormatter.ofPattern("MMM d", Locale.US)
                .format(Instant.ofEpochMilli(hist.last().d).atZone(ZoneId.systemDefault()))
                .uppercase(Locale.US)
            Text("PLAYS ${hist.size} · AVG ${fmt(avg)} · LAST $last", style = monoStyle(8.5.sp, color = Muted, ls = 1.0.sp))
        }
    }
}

@Composable
private fun MilestonesCard(unlocked: Set<String>) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MILESTONES", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${unlocked.size} / ${MILESTONES.size}",
                style = monoStyle(10.sp, color = Accent, weight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.height(12.dp))
        MILESTONES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    val got = m.id in unlocked
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, if (got) Accent else Line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, if (got) Accent else Line, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(m.icon), null,
                                tint = if (got) Accent else LineStrong,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                m.title.uppercase(),
                                fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                                color = if (got) Ink else Muted, maxLines = 1
                            )
                            Text(
                                m.desc,
                                style = monoStyle(7.5.sp, color = Muted, ls = 0.9.sp),
                                lineHeight = 10.sp, maxLines = 2
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReminderCard(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminderEnabled(granted)
    }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY REMINDER", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("Nudges you once a day if the streak isn't safe yet",
                    style = monoStyle(8.5.sp, color = Muted, ls = 1.1.sp))
            }
            Switch(
                checked = p.reminderEnabled,
                onCheckedChange = { want ->
                    if (want && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                    ) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setReminderEnabled(want)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Paper, checkedTrackColor = Accent,
                    uncheckedThumbColor = Paper, uncheckedTrackColor = Line,
                    uncheckedBorderColor = LineStrong
                )
            )
        }
        if (p.reminderEnabled) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("REMIND AT", style = monoStyle(8.5.sp, color = Muted, ls = 1.2.sp))
                IconHitBox(size = 30.dp, onClick = { vm.setReminderHour(p.reminderHour - 1) }) {
                    Icon(painterResource(R.drawable.ic_chevl), null, Modifier.size(14.dp), tint = Ink)
                }
                Text("%02d:00".format(p.reminderHour), style = monoStyle(13.sp, weight = FontWeight.SemiBold))
                IconHitBox(size = 30.dp, onClick = { vm.setReminderHour(p.reminderHour + 1) }) {
                    Icon(painterResource(R.drawable.ic_chev), null, Modifier.size(14.dp), tint = Ink)
                }
            }
        }
    }
}

@Composable
private fun DataCard(vm: AppViewModel) {
    var importArmed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(importArmed) { if (importArmed) { delay(2600); importArmed = false } }
    LaunchedEffect(message) { if (message != null) { delay(5000); message = null } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) { vm.exportData(uri); message = "EXPORTED — CHECK YOUR FILES" } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importData(uri) { ok ->
        message = if (ok) "IMPORTED — PROGRESS REPLACED" else "IMPORT FAILED — FILE UNREADABLE"
    } }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, LineStrong, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("BACKUP & MIGRATE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(
            "Export saves everything — bests, streaks, levels — as a JSON file. Import replaces what's on this device.",
            style = monoStyle(8.5.sp, color = Muted, ls = 1.1.sp)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, LineStrong, RoundedCornerShape(10.dp))
                    .clickable { exportLauncher.launch("neura-backup-${LocalDate.now()}.json") },
                contentAlignment = Alignment.Center
            ) { Text("EXPORT", style = monoStyle(9.5.sp, color = InkSoft, ls = 1.2.sp)) }
            Box(
                Modifier.weight(1f).height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (importArmed) Bad else Color.Transparent)
                    .border(1.5.dp, if (importArmed) Bad else LineStrong, RoundedCornerShape(10.dp))
                    .clickable {
                        if (!importArmed) importArmed = true
                        else importLauncher.launch(arrayOf("application/json"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (importArmed) "TAP AGAIN — REPLACES DATA" else "IMPORT",
                    style = monoStyle(9.5.sp, color = if (importArmed) Color.White else InkSoft, ls = 1.2.sp)
                )
            }
        }
        message?.let {
            Spacer(Modifier.height(9.dp))
            Text(it, style = monoStyle(8.5.sp, color = Accent, ls = 1.1.sp))
        }
    }
}

@Composable
private fun ResetButton(vm: AppViewModel) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(2600); armed = false } }
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    Box(
        Modifier
            .fillMaxWidth().padding(top = 24.dp).height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (armed) Bad else Color.Transparent)
            .drawBehind {
                if (!armed) drawRoundRect(color = Bad, style = Stroke(1.5.dp.toPx(), pathEffect = dash))
            }
            .clickable { if (!armed) armed = true else vm.resetAll() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (armed) "TAP AGAIN TO CONFIRM RESET" else "RESET ALL DATA",
            style = monoStyle(9.5.sp, color = if (armed) Color.White else Bad, ls = 1.6.sp)
        )
    }
}
