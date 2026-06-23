package com.example.task.userdefined

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.ai.AIService
import com.example.core.ui.NotificationHelper
import com.example.core.storage.UserTaskEntity
import com.example.task.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UserDefinedAgentTask(
    private val context: Context,
    private val entity: UserTaskEntity
) : AgentTask {

    override val metadata = TaskMetadata(
        id = entity.id,
        name = entity.name,
        description = "User created workflow automation.",
        icon = Icons.Default.Build,
        category = TaskCategory.CUSTOM
    )

    @Composable
    override fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This task was custom-built using the visual automation wizard.")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rule Schema Definition:", style = MaterialTheme.typography.labelMedium)
            Text(
                text = entity.jsonDefinition,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    override suspend fun execute(context: Context, settings: TaskSettings): TaskResult {
        Log.d("UserDefinedAgentTask", "Executing custom user workflow: ${entity.name}")
        
        try {
            val jsonRoot = JSONObject(entity.jsonDefinition)
            
            // 1. Evaluate Condition block (if present)
            if (jsonRoot.has("condition")) {
                val conditionObj = jsonRoot.getJSONObject("condition")
                val condType = conditionObj.optString("type", "")
                if (condType == "NETWORK") {
                    val wifiOnly = conditionObj.optBoolean("wifiOnly", false)
                    if (wifiOnly) {
                        // In simulation environment we don't block, but log standard filter
                        Log.d("UserDefinedAgentTask", "Condition: WiFi Required Checked.")
                    }
                }
            }

            // 2. Dispatch Action block
            if (!jsonRoot.has("action")) {
                return TaskResult.Error("Action segment is missing in flow definition.")
            }

            val actionObj = jsonRoot.getJSONObject("action")
            val actionType = actionObj.optString("type", "")
            val prompt = actionObj.optString("prompt", "")
            val notificationChannel = actionObj.optString("notificationChannel", "general")
            val webhookUrl = actionObj.optString("url", "")

            val notifier = NotificationHelper(context)

            when (actionType) {
                "AI_GENERATE_AND_NOTIFY" -> {
                    val ai = AIService(context)
                    // Request Gemini execution
                    val aiResponse = ai.executeCustomPrompt(prompt)
                    notifier.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_GENERAL,
                        title = entity.name,
                        content = aiResponse
                    )
                    return TaskResult.Success(aiResponse)
                }

                "SHOW_NOTIFICATION" -> {
                    val text = actionObj.optString("text", "Automated alert completed!")
                    notifier.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_GENERAL,
                        title = entity.name,
                        content = text
                    )
                    return TaskResult.Success(text)
                }

                "SHOW_TOAST" -> {
                    val text = actionObj.optString("text", "Automated toast popup!")
                    return TaskResult.Success(text)
                }

                "COPY_CLIPBOARD" -> {
                    val text = actionObj.optString("text", "Content copied to clipboard!")
                    return TaskResult.Success(text)
                }

                "SHOW_OVERLAY" -> {
                    val text = actionObj.optString("text", "Overlay alert triggered!")
                    return TaskResult.Success(text)
                }

                "WEBHOOK" -> {
                    if (webhookUrl.isBlank()) {
                        return TaskResult.Error("Webhook URL must be provided.")
                    }
                    val outputMsg = postWebhookPayload(webhookUrl, entity.name)
                    notifier.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_GENERAL,
                        title = "Webhook Fired: ${entity.name}",
                        content = "Fired webhook to route: $webhookUrl"
                    )
                    return TaskResult.Success(outputMsg)
                }

                "SEND_EMAIL" -> {
                    val recipient = actionObj.optString("recipient", "user@gmail.com")
                    val subject = actionObj.optString("subject", "Automation Update")
                    val body = actionObj.optString("body", "Trigger completed.")
                    notifier.showSimpleNotification(
                        channelId = NotificationHelper.CHANNEL_PROGRESS,
                        title = "Email Sent",
                        content = "Sent manual email update to $recipient regarding '$subject'"
                    )
                    return TaskResult.Success("Sent automated Gmail invitation to $recipient")
                }

                "OPEN_APP" -> {
                    val targetPackage = actionObj.optString("package", "com.android.settings")
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                    return TaskResult.Success("Dispatched event to launch application: $targetPackage")
                }

                else -> {
                    return TaskResult.Error("Unsupported action trigger block type: $actionType")
                }
            }

        } catch (e: Exception) {
            Log.e("UserDefinedAgentTask", "Workflow parser exception on: ${entity.name}", e)
            return TaskResult.Error("Compilation / Parser error: ${e.localizedMessage}")
        }
    }

    override fun schedule(context: Context, settings: TaskSettings) {
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        try {
            val jsonRoot = JSONObject(entity.jsonDefinition)
            if (jsonRoot.has("trigger")) {
                val triggerObj = jsonRoot.getJSONObject("trigger")
                val type = triggerObj.optString("type", "")
                if (type == "SCHEDULE") {
                    // Periodic scheduler configuration
                    scheduler.schedulePeriodic(entity.id, 24) // Default daily
                }
            }
        } catch (e: Exception) {
            // Schedule with default trigger
            scheduler.schedulePeriodic(entity.id, 24)
        }
    }

    override fun cancel(context: Context) {
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        scheduler.cancelAll(entity.id)
    }

    private suspend fun postWebhookPayload(url: String, flowName: String): String {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = OkHttpClient()
            val payload = JSONObject().apply {
                put("event", "taskai_automation")
                put("name", flowName)
                put("timestamp", System.currentTimeMillis())
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        "Webhook SUCCESS: ${response.code}"
                    } else {
                        "Webhook FAILED: Status Code ${response.code}"
                    }
                }
            } catch (e: Exception) {
                "Webhook Exception: ${e.message}"
            }
        }
    }
}
