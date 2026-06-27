package com.example.core.scheduler

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.core.storage.PreferencesManager
import java.util.concurrent.TimeUnit

class TaskScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val preferencesManager = PreferencesManager(context)

    fun schedulePeriodic(taskId: String, intervalHours: Int, flexMinutes: Int = 15) {
        val data = Data.Builder()
            .putString("TASK_ID", taskId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create periodic request
        val periodicRequest = PeriodicWorkRequestBuilder<AgentTaskWorker>(
            intervalHours.toLong(), TimeUnit.HOURS,
            flexMinutes.toLong(), TimeUnit.MINUTES
        )
        .setInputData(data)
        .setConstraints(constraints)
        .addTag(taskId)
        .addTag("AGENT_TASK")
        .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_$taskId",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
        Log.d("TaskScheduler", "Scheduled periodic task: $taskId every $intervalHours hours")
    }

    /**
     * Schedules a daily periodic task to execute exactly at a specific target time (e.g. "11:00").
     * Calculates the initial delay in minutes between current system time and the target time
     * to avoid random immediate background runs and conserve battery.
     */
    fun schedulePeriodicAtSpecificTime(taskId: String, timeString: String) {
        val delayMinutes = calculateInitialDelayMinutes(timeString)
        val data = Data.Builder()
            .putString("TASK_ID", taskId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a 24-hour periodic request with precise initial delay
        val periodicRequest = PeriodicWorkRequestBuilder<AgentTaskWorker>(
            24L, TimeUnit.HOURS,
            15L, TimeUnit.MINUTES
        )
        .setInputData(data)
        .setConstraints(constraints)
        .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        .addTag(taskId)
        .addTag("AGENT_TASK")
        .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_$taskId",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
        Log.d("TaskScheduler", "Scheduled daily task '$taskId' at $timeString. Initial delay is $delayMinutes mins.")
    }

    private fun calculateInitialDelayMinutes(targetTime: String): Long {
        try {
            val parts = targetTime.trim().split(":")
            if (parts.size != 2) return 0L
            val hour = parts[0].toIntOrNull() ?: return 0L
            val minute = parts[1].toIntOrNull() ?: return 0L

            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val diffMs = target.timeInMillis - now.timeInMillis
            return diffMs / (60 * 1000)
        } catch (e: Exception) {
            Log.e("TaskScheduler", "Error calculating initial delay for time: $targetTime", e)
            return 0L
        }
    }

    fun scheduleOneShot(taskId: String, delayMinutes: Long) {
        val data = Data.Builder()
            .putString("TASK_ID", taskId)
            .build()

        val oneShotRequest = OneTimeWorkRequestBuilder<AgentTaskWorker>()
            .setInputData(data)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(taskId)
            .addTag("AGENT_TASK")
            .build()

        workManager.enqueueUniqueWork(
            "oneshot_$taskId",
            ExistingWorkPolicy.REPLACE,
            oneShotRequest
        )
        Log.d("TaskScheduler", "Scheduled one-shot task: $taskId with action delay: $delayMinutes minutes")
    }

    fun executeExpeditedNow(taskId: String) {
        val data = Data.Builder()
            .putString("TASK_ID", taskId)
            .build()

        val expediteRequest = OneTimeWorkRequestBuilder<AgentTaskWorker>()
            .setInputData(data)
            .addTag(taskId)
            .addTag("AGENT_TASK")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueue(expediteRequest)
        Log.d("TaskScheduler", "Enqueued expedited direct execution for task: $taskId")
    }

    fun cancelAll(taskId: String) {
        workManager.cancelAllWorkByTag(taskId)
        workManager.cancelUniqueWork("periodic_$taskId")
        workManager.cancelUniqueWork("oneshot_$taskId")
        Log.d("TaskScheduler", "Cancelled all background executions of task: $taskId")
    }
}
