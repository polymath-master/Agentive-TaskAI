package com.example.core.scheduler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.storage.AppDatabase
import com.example.core.storage.TaskHistory
import com.example.task.TaskRegistry
import com.example.task.TaskResult
import com.example.task.TaskSettings
import com.example.task.userdefined.UserDefinedAgentTask
import java.util.UUID

class AgentTaskWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("TASK_ID") ?: return Result.failure()
        Log.d("AgentTaskWorker", "AgentTaskWorker started executing work for task ID: $taskId")

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.taskDao()

        // Distinguish between Built-in tasks and User-Defined Tasks
        if (taskId.startsWith("user_")) {
            // User defined
            val userTaskEntity = dao.getUserTaskById(taskId)
            if (userTaskEntity == null) {
                Log.e("AgentTaskWorker", "User task entity $taskId not found in persistence.")
                return Result.failure()
            }
            if (!userTaskEntity.isEnabled) {
                Log.d("AgentTaskWorker", "User task $taskId is disabled. Skipping worker run.")
                return Result.success()
            }

            val userTask = UserDefinedAgentTask(applicationContext, userTaskEntity)
            val settings = TaskSettings() // Settings are stored raw within the JSON schema itself
            
            val runResult = try {
                userTask.execute(applicationContext, settings)
            } catch (e: Exception) {
                TaskResult.Error("Execution failed: ${e.message}")
            }

            // Write telemetry log
            val history = TaskHistory(
                taskId = taskId,
                taskName = userTaskEntity.name,
                status = when (runResult) {
                    is TaskResult.Success -> "SUCCESS"
                    is TaskResult.Error -> "ERROR"
                    is TaskResult.Cancelled -> "CANCELLED"
                },
                message = when (runResult) {
                    is TaskResult.Success -> runResult.output ?: "Trigger successfully executed."
                    is TaskResult.Error -> runResult.message
                    is TaskResult.Cancelled -> "Task execution halted by schedule filter."
                }
            )
            dao.insertHistory(history)
            
            return when (runResult) {
                is TaskResult.Success -> Result.success()
                else -> Result.retry()
            }
        } else {
            // Built-in task
            val registry = TaskRegistry(applicationContext)
            val task = registry.getTaskById(taskId)
            if (task == null) {
                Log.e("AgentTaskWorker", "Task ID $taskId contains no built-in definitions.")
                return Result.failure()
            }

            // Load settings from db or fallback to empty
            // For simplicity, we fetch settings from PreferencesManager or build standard configs
            val settings = TaskSettings()

            val runResult = try {
                task.execute(applicationContext, settings)
            } catch (e: Exception) {
                TaskResult.Error("Worker crash: ${e.message}")
            }

            // Write telemetry log
            val history = TaskHistory(
                taskId = taskId,
                taskName = task.metadata.name,
                status = when (runResult) {
                    is TaskResult.Success -> "SUCCESS"
                    is TaskResult.Error -> "ERROR"
                    is TaskResult.Cancelled -> "CANCELLED"
                },
                message = when (runResult) {
                    is TaskResult.Success -> runResult.output ?: "Execution ran successfully in background."
                    is TaskResult.Error -> runResult.message
                    is TaskResult.Cancelled -> "Task execution cancelled."
                }
            )
            dao.insertHistory(history)

            return when (runResult) {
                is TaskResult.Success -> Result.success()
                else -> Result.retry()
            }
        }
    }
}
