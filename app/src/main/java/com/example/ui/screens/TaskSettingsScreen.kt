package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.permissions.PermissionUtils
import com.example.core.storage.PreferencesManager
import com.example.core.ui.PermissionGate
import com.example.task.AgentTask
import com.example.task.TaskSettings
import kotlinx.coroutines.launch

@Composable
fun TaskSettingsScreen(
    task: AgentTask,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Query active settings
    var currentSettings by remember { mutableStateOf(TaskSettings()) }
    var isEnabled by remember { mutableStateOf(true) }
    var showPermissionGate by remember { mutableStateOf(false) }

    // Read stored preferences at screen startup
    LaunchedEffect(task.metadata.id) {
        preferencesManager.isTaskEnabledFlow(task.metadata.id).collect {
            isEnabled = it
        }
    }

    LaunchedEffect(task.metadata.id) {
        // Hydrate settings keys from local DataStore caches
        val hValues = mutableMapOf<String, String>()
        preferencesManager.newsScheduleTimeFlow.collect { hValues["schedule_time"] = it }
        preferencesManager.reminderDelayMinutesFlow.collect { hValues["reminder_delay_minutes"] = it.toString() }
        preferencesManager.whatsappResponseToneFlow.collect { hValues["whatsapp_response_tone"] = it }
        preferencesManager.gmailUserEmailFlow.collect { hValues["gmail_user_email"] = it }
        currentSettings = TaskSettings(hValues)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("${task.metadata.name} Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header block
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = task.metadata.icon, contentDescription = null, sizeModifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = task.metadata.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = task.metadata.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Global Enabled Switch Row
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Service State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Toggle background scheduling of this helper agent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                coroutineScope.launch {
                                    preferencesManager.setTaskEnabled(task.metadata.id, it)
                                }
                            }
                        )
                    }
                }

                // Call task-plugin layout context to draw settings input fields
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Task Configurations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        task.ConfigurationScreen(
                            settings = currentSettings,
                            onSettingsChanged = { updated -> currentSettings = updated }
                        )
                    }
                }

                // Security permissions state indicator card
                val missingPerms = task.requiredSpecialPermissions().filter { !PermissionUtils.isSpecialPermissionGranted(context, it) }
                if (missingPerms.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showPermissionGate = true }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚠️ Missing ${missingPerms.size} required system permissions. Tap to fix.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Save Actions block
            Button(
                onClick = {
                    coroutineScope.launch {
                        // Persist config inputs to preferences
                        currentSettings.values.forEach { (key, value) ->
                            when (key) {
                                "schedule_time" -> preferencesManager.saveNewsScheduleTime(value)
                                "reminder_delay_minutes" -> value.toIntOrNull()?.let { preferencesManager.saveReminderDelayMinutes(it) }
                                "whatsapp_response_tone" -> preferencesManager.saveWhatsappResponseTone(value)
                                "gmail_user_email" -> preferencesManager.saveGmailUserEmail(value)
                            }
                        }

                        // Schedule or Cancel the WorkManager operations based on active flag
                        if (isEnabled) {
                            // Check if permission constraints are missing
                            val ungranted = task.requiredSpecialPermissions().filter { !PermissionUtils.isSpecialPermissionGranted(context, it) }
                            if (ungranted.isNotEmpty()) {
                                showPermissionGate = true
                            } else {
                                task.schedule(context, currentSettings)
                                Toast.makeText(context, "${task.metadata.name} configured & scheduled!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        } else {
                            task.cancel(context)
                            Toast.makeText(context, "Schedules halted.", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Apply Schedules", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPermissionGate) {
        AlertDialog(
            onDismissRequest = { showPermissionGate = false },
            confirmButton = {},
            text = {
                PermissionGate(
                    permissions = task.requiredSpecialPermissions(),
                    onDismiss = { showPermissionGate = false },
                    onPermissionGranted = {
                        showPermissionGate = false
                        task.schedule(context, currentSettings)
                    }
                )
            }
        )
    }
}

// Inline helper for cleanly sized icons
@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, sizeModifier: Modifier) {
    androidx.compose.material3.Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = sizeModifier)
}
