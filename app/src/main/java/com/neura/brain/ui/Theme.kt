package com.neura.brain.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF2EDE3)
val Panel = Color(0xFFFAF7EF)
val Ink = Color(0xFF17140E)
val InkSoft = Color(0xFF4A443A)
val Muted = Color(0xFF7E7867)
val Line = Color(0xFFDCD5C6)
val LineStrong = Color(0xFFC6BEAC)
val Accent = Color(0xFFE04A22)
val AccentSoft = Color(0xFFFF7A4A)
val Good = Color(0xFF2E7D4E)
val Bad = Color(0xFFC23A2B)

val AppSans: FontFamily = FontFamily.SansSerif
val AppMono: FontFamily = FontFamily.Monospace

fun monoStyle(
    size: TextUnit,
    color: Color = Ink,
    weight: FontWeight = FontWeight.Medium,
    ls: TextUnit = 1.2.sp
) = TextStyle(fontFamily = AppMono, fontSize = size, color = color, fontWeight = weight, letterSpacing = ls)

@Composable
fun NeuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent, onPrimary = Color.White,
            background = Paper, onBackground = Ink,
            surface = Panel, onSurface = Ink
        ),
        content = content
    )
}
