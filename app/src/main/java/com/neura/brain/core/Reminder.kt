package com.neura.brain.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neura.brain.MainActivity
import com.neura.brain.R
import com.neura.brain.data.Progress
import com.neura.brain.data.ProgressRepository
import com.neura.brain.data.neuraDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "neura.daily.reminder"

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannel(
            "neura.reminders", "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Nudges you to protect your NEURA streak" }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun schedule(ctx: Context, hour: Int) {
        ensureChannel(ctx)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour.coerceIn(0, 23), 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        return try {
            val raw = ctx.neuraDataStore.data.first()[ProgressRepository.KEY] ?: return Result.success()
            val p = runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString<Progress>(raw)
            }.getOrNull() ?: return Result.success()

            // Only remind if the user asked for it AND today's streak isn't banked yet.
            if (!p.reminderEnabled || p.streakLast == LocalDate.now().toString())
                return Result.success()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
            ) return Result.success()

            ReminderScheduler.ensureChannel(ctx)
            val intent = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val text = if (p.streakCount > 0)
                "Your ${p.streakCount}-day streak is waiting — about four minutes keeps the chain alive."
            else
                "Five minutes a day. Start today's circuit and calibrate your baseline."

            NotificationManagerCompat.from(ctx).notify(
                1001,
                NotificationCompat.Builder(ctx, "neura.reminders")
                    .setSmallIcon(R.drawable.ic_spark)
                    .setContentTitle("NEURA")
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(intent)
                    .build()
            )
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
