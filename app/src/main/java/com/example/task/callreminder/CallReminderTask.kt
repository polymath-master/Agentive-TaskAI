package com.example.task.callreminder

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.permissions.SpecialPermission
import com.example.core.ui.NotificationHelper
import com.example.task.*

class CallReminderTask(private val context: Context) : AgentTask {

    override val metadata = TaskMetadata(
        id = "callreminder",
        name = "Call Reminder Agent",
        description = "Detects missed calls via notification listener and schedules smart callback reminders on WorkManager.",
        icon = Icons.Default.PhoneCallback,
        category = TaskCategory.COMMUNICATION
    )

    override fun requiredSpecialPermissions(): List<SpecialPermission> {
        return listOf(SpecialPermission.NOTIFICATION_LISTENER)
    }

    @Composable
    override fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    ) {
        val defaultMinutes = settings.values["reminder_delay_minutes"] ?: "30"
        var delayInput by remember { mutableStateOf(defaultMinutes) }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Configure callback alert settings below. This delay is used for the direct action button triggers.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = delayInput,
                onValueChange = {
                    delayInput = it
                    onSettingsChanged(TaskSettings(mapOf("reminder_delay_minutes" to it)))
                },
                label = { Text("Default Reminder Delay (Minutes)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    override suspend fun execute(context: Context, settings: TaskSettings): TaskResult {
        // This is executed by the WorkManager scheduler when the delayed reminder fires
        val contactName = settings.values["contact_name"] ?: "Missed Number"
        val notificationHelper = NotificationHelper(context)
        
        notificationHelper.showSimpleNotification(
            channelId = NotificationHelper.CHANNEL_REMINDER,
            title = "Reminder: Call Back $contactName",
            content = "You scheduled an automated callback reminder for missed calls from $contactName."
        )

        return TaskResult.Success("Fired callback reminder alert for: $contactName")
    }

    override fun schedule(context: Context, settings: TaskSettings) {
        val delayMinutes = (settings.values["reminder_delay_minutes"] ?: "30").toLongOrNull() ?: 30L
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        scheduler.scheduleOneShot("callreminder", delayMinutes)
    }

    override fun cancel(context: Context) {
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        scheduler.cancelAll("callreminder")
    }

    /**
     * Called directly by NotificationListenerService when a missed call is detected
     */
    fun onMissedCallReceived(contactName: String) {
        val helper = NotificationHelper(context)
        helper.showMissedCallReminderPrompt(contactName)
    }
}
