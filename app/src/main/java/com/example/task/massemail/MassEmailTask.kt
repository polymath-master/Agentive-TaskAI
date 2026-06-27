package com.example.task.massemail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.permissions.SpecialPermission
import com.example.core.google.GoogleApiHelper
import com.example.ui.screens.GoogleAuthConsentDialog
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
        val currentContext = LocalContext.current
        val googleApiHelper = remember { GoogleApiHelper.getInstance(currentContext) }
        var isGoogleConnected by remember { mutableStateOf(googleApiHelper.isUserAuthenticated(currentContext)) }
        var connectedEmail by remember { mutableStateOf(googleApiHelper.getConnectedEmail(currentContext)) }
        var showConsentDialog by remember { mutableStateOf(false) }

        val sheetUrl = settings.values["sheet_url"] ?: "https://docs.google.com/spreadsheets/d/example_headcount"
        val templateDocUrl = settings.values["template_doc_url"] ?: "https://docs.google.com/document/d/example_template"

        var sheetInput by remember(sheetUrl) { mutableStateOf(sheetUrl) }
        var docInput by remember(templateDocUrl) { mutableStateOf(templateDocUrl) }

        if (showConsentDialog) {
            GoogleAuthConsentDialog(
                onDismiss = { showConsentDialog = false },
                onAuthorize = { email, accessToken, refreshToken ->
                    googleApiHelper.connectAccount(
                        context = currentContext,
                        email = email,
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                    isGoogleConnected = true
                    connectedEmail = email
                    showConsentDialog = false
                    Toast.makeText(currentContext, "Google Account Authorized successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            // OAuth Authorization Alert Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoogleConnected) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                    else 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isGoogleConnected) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isGoogleConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGoogleConnected) "Google Account Connected" else "Google Account Disconnected",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isGoogleConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Text(
                        text = if (isGoogleConnected) 
                            "Authorized to execute automation using $connectedEmail" 
                        else 
                            "To build personalized invitations and send emails on background schedules, you must connect your Google Account.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = {
                            if (isGoogleConnected) {
                                googleApiHelper.disconnectAccount(currentContext)
                                isGoogleConnected = false
                                connectedEmail = ""
                            } else {
                                showConsentDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGoogleConnected) 
                                MaterialTheme.colorScheme.error 
                              else 
                                MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isGoogleConnected) "Disconnect" else "Connect Account")
                    }
                }
            }

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

        // Verify Google Workspace Authorization before launching Foreground Worker
        val googleApiHelper = GoogleApiHelper.getInstance(context)
        if (!googleApiHelper.isUserAuthenticated(context)) {
            return TaskResult.Error("OAuth authorization is required. Please authorize your Google Workspace account in Configuration or settings before running invitations.")
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
        val scheduler = com.example.core.scheduler.TaskScheduler(context)
        scheduler.cancelAll("massemail")
    }
}
