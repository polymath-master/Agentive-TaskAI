package com.example.core.scheduler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.task.TaskResult

class AgentTaskWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("TASK_ID") ?: return Result.failure()
        Log.d("AgentTaskWorker", "AgentTaskWorker delegating task ID to AgentRunner: $taskId")

        val runner = AgentRunner(applicationContext)
        val runResult = try {
            runner.runAgent(taskId)
        } catch (e: Exception) {
            TaskResult.Error("Worker crash: ${e.message}")
        }

        return when (runResult) {
            is TaskResult.Success -> Result.success()
            is TaskResult.Cancelled -> Result.success() // cancel is considered finished
            else -> {
                // Return failure rather than retry to avoid continuous loop and prevent battery draining.
                Log.w("AgentTaskWorker", "Task $taskId failed. Returning failure to prevent battery-draining retry loops.")
                Result.failure()
            }
        }
    }
}
