package com.example.core.scheduler

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.core.storage.AppDatabase
import com.example.core.storage.TaskHistory
import com.example.core.ui.NotificationHelper
import com.example.task.AgentTask
import com.example.task.TaskRegistry
import com.example.task.TaskResult
import com.example.task.TaskSettings
import com.example.task.userdefined.UserDefinedAgentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AgentRunner(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val taskDao = database.taskDao()
    private val registry = TaskRegistry(context)
    private val notificationHelper = NotificationHelper(context)

    /**
     * Executes any agent by its ID (either built-in or user-defined).
     * This is the universal pipeline for both manual triggers and scheduled WorkManager tasks.
     * It handles:
     * 1. Evaluating execution conditions (like Wi-Fi constraints).
     * 2. Running the task implementation.
     * 3. Logging results into Room (TaskHistory).
     * 4. Dispatching action outputs (notifications, toasts, webhook logs, copy to clipboard, or overlays).
     */
    suspend fun runAgent(taskId: String): TaskResult = withContext(Dispatchers.IO) {
        Log.d("AgentRunner", "Initializing universal execution pipeline for task: $taskId")

        val agentTask: AgentTask?
        val isUserDefined = taskId.startsWith("user_")
        var taskName = ""

        if (isUserDefined) {
            val entity = taskDao.getUserTaskById(taskId)
            if (entity == null) {
                val errorMsg = "User-defined task with ID $taskId was not found in persistence."
                Log.e("AgentRunner", errorMsg)
                return@withContext TaskResult.Error(errorMsg)
            }
            if (!entity.isEnabled) {
                val skipMsg = "Task $taskId is disabled. Skipping execution."
                Log.d("AgentRunner", skipMsg)
                return@withContext TaskResult.Cancelled
            }
            taskName = entity.name
            agentTask = UserDefinedAgentTask(context, entity)
        } else {
            val builtIn = registry.getTaskById(taskId)
            if (builtIn == null) {
                val errorMsg = "Built-in task with ID $taskId was not found in registry."
                Log.e("AgentRunner", errorMsg)
                return@withContext TaskResult.Error(errorMsg)
            }
            taskName = builtIn.metadata.name
            agentTask = builtIn
        }

        // 1. Evaluate Conditions (Nano Flowchart logic)
        if (isUserDefined) {
            val entity = taskDao.getUserTaskById(taskId)
            if (entity != null) {
                try {
                    val jsonRoot = JSONObject(entity.jsonDefinition)
                    if (jsonRoot.has("condition")) {
                        val conditionObj = jsonRoot.getJSONObject("condition")
                        val condType = conditionObj.optString("type", "")
                        if (condType == "NETWORK") {
                            val wifiOnly = conditionObj.optBoolean("wifiOnly", false)
                            if (wifiOnly && !isWifiConnected(context)) {
                                val message = "Halted: Wi-Fi connection required but device is on mobile or offline."
                                Log.d("AgentRunner", message)
                                insertHistory(taskId, taskName, "CANCELLED", message)
                                dispatchOutput(taskName, "Halted: Wi-Fi connection required.", "SHOW_TOAST")
                                return@withContext TaskResult.Cancelled
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AgentRunner", "Error parsing conditions", e)
                }
            }
        }

        // 2. Execute Task
        val settings = TaskSettings() // Settings are collected from DB inside agents if needed
        val result = try {
            agentTask.execute(context, settings)
        } catch (e: Exception) {
            TaskResult.Error("Runtime crash: ${e.message}")
        }

        // 3. Log into Database (TaskHistory)
        val status = when (result) {
            is TaskResult.Success -> "SUCCESS"
            is TaskResult.Error -> "ERROR"
            is TaskResult.Cancelled -> "CANCELLED"
        }
        val message = when (result) {
            is TaskResult.Success -> result.output ?: "Execution ran successfully."
            is TaskResult.Error -> result.message
            is TaskResult.Cancelled -> "Halted or Cancelled."
        }
        insertHistory(taskId, taskName, status, message)

        // 4. Dispatch Action Output
        if (result is TaskResult.Success) {
            val outputText = result.output ?: "Action completed successfully."
            if (isUserDefined) {
                val entity = taskDao.getUserTaskById(taskId)
                if (entity != null) {
                    try {
                        val jsonRoot = JSONObject(entity.jsonDefinition)
                        val actionObj = jsonRoot.optJSONObject("action")
                        val actionType = actionObj?.optString("type", "SHOW_NOTIFICATION") ?: "SHOW_NOTIFICATION"
                        
                        // Output dispatcher mapping
                        dispatchOutput(taskName, outputText, actionType)
                    } catch (e: Exception) {
                        dispatchOutput(taskName, outputText, "SHOW_NOTIFICATION")
                    }
                }
            } else {
                // For built-in tasks, we can show a brief Toast or confirmation to satisfy universal visibility.
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Agent '$taskName' completed successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (result is TaskResult.Error) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Agent '$taskName' failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }

        return@withContext result
    }

    private suspend fun insertHistory(taskId: String, taskName: String, status: String, message: String) {
        try {
            taskDao.insertHistory(
                TaskHistory(
                    taskId = taskId,
                    taskName = taskName,
                    status = status,
                    message = message
                )
            )
        } catch (e: Exception) {
            Log.e("AgentRunner", "Failed to insert history log", e)
        }
    }

    private suspend fun dispatchOutput(agentName: String, outputText: String, actionType: String) {
        withContext(Dispatchers.Main) {
            when (actionType) {
                "AI_GENERATE_AND_NOTIFY", "SHOW_NOTIFICATION" -> {
                    notificationHelper.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_GENERAL,
                        title = agentName,
                        content = outputText
                    )
                }
                "SHOW_TOAST" -> {
                    Toast.makeText(context, "$agentName: $outputText", Toast.LENGTH_LONG).show()
                }
                "COPY_CLIPBOARD", "CLIPBOARD" -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Agentive Output", outputText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Result copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
                "SHOW_OVERLAY" -> {
                    // Check if permission is granted first
                    if (com.example.core.permissions.PermissionUtils.isOverlayPermissionGranted(context)) {
                        Toast.makeText(context, "🔮 Overlay Output [$agentName]:\n$outputText", Toast.LENGTH_LONG).show()
                    } else {
                        // Fallback to standard notification
                        notificationHelper.showSimpleNotification(
                            channelId = NotificationHelper.CHANNEL_GENERAL,
                            title = "$agentName (Overlay Fallback)",
                            content = outputText
                        )
                    }
                }
                else -> {
                    // Default fallback is notification
                    notificationHelper.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_GENERAL,
                        title = agentName,
                        content = outputText
                    )
                }
            }
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }
}
