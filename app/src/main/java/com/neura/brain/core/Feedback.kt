package com.neura.brain.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sin

enum class Wave { SINE, SQUARE, SAW }

class Feedback(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } catch (_: Exception) { null }

    var muted: () -> Boolean = { false }

    fun buzz(ms: Long) {
        try {
            val v = vibrator ?: return
            if (Build.VERSION.SDK_INT >= 31 && ms <= 40)
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            else
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    fun tone(freq: Double, durMs: Long, wave: Wave = Wave.SINE, vol: Double = 0.14, delayMs: Long = 0, endFreq: Double = 0.0) {
        if (muted()) return
        try {
            val rate = 44100
            val n = (rate * durMs / 1000.0).toInt().coerceIn(64, rate * 3)
            val pcm = ShortArray(n)
            val durS = n.toDouble() / rate
            var ph = 0.0
            for (i in 0 until n) {
                val p = i.toDouble() / n
                val f = if (endFreq > 0.0) freq * Math.pow(endFreq / freq, p) else freq
                ph += 2.0 * Math.PI * f / rate
                val t = p * durS
                val env = if (t < 0.004) t / 0.004 else exp(-3.5 * p)
                val raw = sin(ph)
                val s = when (wave) {
                    Wave.SINE -> raw
                    Wave.SQUARE -> if (raw >= 0) 1.0 else -1.0
                    Wave.SAW -> 2.0 * ((ph / (2.0 * Math.PI)) % 1.0) - 1.0
                }
                pcm[i] = (s * env * vol * Short.MAX_VALUE).toInt().toShort()
            }
            val buf = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
            buf.asShortBuffer().put(pcm)
            val bytes = buf.array()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bytes.size)
                .build()
            track.write(bytes, 0, bytes.size)
            handler.postDelayed({ try { track.play() } catch (_: Exception) {} }, delayMs)
            handler.postDelayed({ try { track.release() } catch (_: Exception) {} }, delayMs + durMs + 300)
        } catch (_: Exception) {}
    }

    fun tap() = tone(880.0, 45, Wave.SQUARE, 0.05)
    fun tick() = tone(1150.0, 30, Wave.SQUARE, 0.05)
    fun good() { tone(660.0, 70, vol = 0.12); tone(990.0, 90, vol = 0.10, delayMs = 60) }
    fun bad() = tone(150.0, 180, Wave.SAW, 0.13, endFreq = 90.0)
    fun go() = tone(880.0, 120, vol = 0.14, endFreq = 1320.0)
    fun done() = listOf(523.25, 659.25, 783.99, 1046.50).forEachIndexed { i, f -> tone(f, 130, vol = 0.10, delayMs = i * 90L) }
    fun fanfare() = listOf(659.25, 830.61, 987.77, 1318.51)
        .forEachIndexed { i, f -> tone(f, 160, vol = 0.11, delayMs = i * 95L) }
    fun pad(i: Int) = tone(listOf(392.0, 493.88, 587.33, 739.99).getOrElse(i) { 440.0 }, 260, vol = 0.15)
}
