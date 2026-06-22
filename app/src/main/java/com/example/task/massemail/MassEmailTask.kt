package com.example.task.massemail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.permissions.SpecialPermission
import com.example.task.*

class MassEmailTask(private val context: Context) : AgentTask {

    override val metadata = TaskMetadata(
        id = "massemail",
        name = "Mass Email Invitation",
        description = "Parses mailing lists from Google Sheets, maps values to Google Docs templates, and sends personalized Gmail invitations.",
        icon = Icons.Default.Email,
        category = TaskCategory.PRODUCTIVITY
    )

    override fun requiredSpecialPermissions(): List<SpecialPermission> {
        return listOf(
            SpecialPermission.GMAIL_OAUTH,
            SpecialPermission.GOOGLE_SHEETS_OAUTH
        )
    }

    @Composable
    override fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    ) {
        val sheetUrl = settings.values["sheet_url"] ?: "https://docs.google.com/spreadsheets/d/example_headcount"
        val templateDocUrl = settings.values["template_doc_url"] ?: "https://docs.google.com/document/d/example_template"

        var sheetInput by remember { mutableStateOf(sheetUrl) }
        var docInput by remember { mutableStateOf(templateDocUrl) }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            OutlinedTextField(
                value = sheetInput,
                onValueChange = {
                    sheetInput = it
                    onSettingsChanged(TaskSettings(mapOf("sheet_url" to it, "template_doc_url" to docInput)))
                },
                label = { Text("Google Sheets URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = docInput,
                onValueChange = {
                    docInput = it
                    onSettingsChanged(TaskSettings(mapOf("sheet_url" to sheetInput, "template_doc_url" to it)))
                },
                label = { Text("Google Docs Template URL") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    override suspend fun execute(context: Context, settings: TaskSettings): TaskResult {
        val sheetUrl = settings.values["sheet_url"] ?: ""
        val templateDocUrl = settings.values["template_doc_url"] ?: ""

        if (sheetUrl.isBlank() || templateDocUrl.isBlank()) {
            return TaskResult.Error("Configuration invalid: Sheet URL and Template Doc URL must be specified.")
        }

        // Mass Email execution is handled under EmailForegroundService to keep it persistent in background 
        // with standard ongoing notifications, avoiding death when user navigates away or clears backstack.
        try {
            val serviceIntent = Intent(context, EmailForegroundService::class.java).apply {
                putExtra("SHEET_URL", sheetUrl)
                putExtra("TEMPLATE_URL", templateDocUrl)
            }
            context.startService(serviceIntent)
            return TaskResult.Success("Mass email foreground worker successfully started.")
        } catch (e: Exception) {
            return TaskResult.Error("Failed to initiate mass mailing: ${e.message}")
        }
    }

    override fun schedule(context: Context, settings: TaskSettings) {
        // Enqueue on WorkManager scheduler
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        scheduler.scheduleOneShot("massemail", 0L) // Execute immediately
    }

    override fun cancel(context: Context) {
        val serviceIntent = Intent(context, EmailForegroundService::class.java)
        context.stopService(serviceIntent)
    }
}
