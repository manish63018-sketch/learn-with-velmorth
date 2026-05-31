package com.velmorth.app.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules and cancels the daily study reminder via WorkManager.
 */
object NotificationScheduler {

    private const val TAG         = "NotifScheduler"
    private const val WORK_NAME   = "velmorth_daily_reminder"

    /**
     * Schedules a PeriodicWorkRequest that fires every 24 hours,
     * with an initial delay calculated to hit [hour]:[minute] today (or tomorrow if already past).
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val initialDelay = calculateInitialDelayMs(hour, minute)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Reschedule if time changed
            request
        )
        Log.d(TAG, "Daily reminder scheduled at %02d:%02d (delay ${initialDelay / 60000}min)".format(hour, minute))
    }

    /**
     * Cancels the daily reminder — called when user toggles notifications off.
     */
    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Daily reminder cancelled")
    }

    /**
     * Computes how many milliseconds until the next occurrence of [hour]:[minute].
     * If that time has already passed today, returns the delay to the same time tomorrow.
     */
    private fun calculateInitialDelayMs(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1) // Push to tomorrow
        }
        return target.timeInMillis - now.timeInMillis
    }
}
