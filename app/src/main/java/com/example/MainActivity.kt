package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.core.storage.AppDatabase
import com.example.core.storage.PreferencesManager
import com.example.task.TaskRegistry
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryLogsScreen
import com.example.ui.screens.TaskCreatorScreen
import com.example.ui.screens.TaskSettingsScreen
import com.example.ui.screens.GlobalSettingsScreen
import com.example.ui.screens.AgentDetailScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    DASHBOARD,
    SETTINGS,
    CREATOR,
    HISTORY,
    GLOBAL_SETTINGS,
    AGENT_DETAIL
}

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support is mandatory under Material 3 guidelines
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)
        val registry = TaskRegistry(applicationContext)

        setContent {
            val isDarkTheme by preferencesManager.isDarkThemeFlow.collectAsState(initial = true)

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                    var selectedTaskId by remember { mutableStateOf<String?>("news") }
                    var customReminderContact by remember { mutableStateOf<String?>(null) }

                    // Process incoming intent actions reactively
                    LaunchedEffect(intent) {
                        val trigger = intent?.getStringExtra("ACTION_TRIGGER")
                        val contactName = intent?.getStringExtra("PARAM_NAME") ?: "Caller"
                        if (trigger != null) {
                            if (trigger == "REMIND_30_MIN") {
                                preferencesManager.saveLastMissedCallContact(contactName)
                                val scheduler = com.example.core.scheduler.TaskScheduler(applicationContext)
                                scheduler.scheduleOneShot("callreminder", 30L)
                                android.widget.Toast.makeText(applicationContext, "Callback reminder scheduled for $contactName in 30 mins!", android.widget.Toast.LENGTH_LONG).show()
                            } else if (trigger == "REMIND_CUSTOM") {
                                preferencesManager.saveLastMissedCallContact(contactName)
                                customReminderContact = contactName
                            }
                            intent?.removeExtra("ACTION_TRIGGER")
                        }
                    }

                    if (customReminderContact != null) {
                        var minutesInput by remember { mutableStateOf("15") }
                        AlertDialog(
                            onDismissRequest = { customReminderContact = null },
                            title = { Text("Schedule Callback Reminder", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Enter delay in minutes to call back ${customReminderContact}:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = minutesInput,
                                        onValueChange = { minutesInput = it },
                                        label = { Text("Minutes Delay") },
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val minutes = minutesInput.toLongOrNull() ?: 15L
                                        val scheduler = com.example.core.scheduler.TaskScheduler(applicationContext)
                                        scheduler.scheduleOneShot("callreminder", minutes)
                                        android.widget.Toast.makeText(applicationContext, "Callback reminder scheduled for ${customReminderContact} in $minutes minutes!", android.widget.Toast.LENGTH_LONG).show()
                                        customReminderContact = null
                                    }
                                ) {
                                    Text("Schedule")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { customReminderContact = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                database = database,
                                preferencesManager = preferencesManager,
                                onNavigateToSettings = { taskId ->
                                    selectedTaskId = taskId
                                    currentScreen = Screen.AGENT_DETAIL
                                },
                                onNavigateToCreator = {
                                    currentScreen = Screen.CREATOR
                                },
                                onNavigateToHistory = {
                                    currentScreen = Screen.HISTORY
                                },
                                onNavigateToGlobalSettings = {
                                    currentScreen = Screen.GLOBAL_SETTINGS
                                }
                            )
                        }

                        Screen.SETTINGS -> {
                            val activeTask = selectedTaskId?.let { registry.getTaskById(it) }
                            if (activeTask != null) {
                                TaskSettingsScreen(
                                    task = activeTask,
                                    preferencesManager = preferencesManager,
                                    onBack = { currentScreen = Screen.AGENT_DETAIL },
                                    onNavigateToGlobalSettings = { currentScreen = Screen.GLOBAL_SETTINGS }
                                )
                            } else {
                                currentScreen = Screen.DASHBOARD
                            }
                        }

                        Screen.AGENT_DETAIL -> {
                            val taskId = selectedTaskId ?: "news"
                            AgentDetailScreen(
                                taskId = taskId,
                                database = database,
                                preferencesManager = preferencesManager,
                                onBack = { currentScreen = Screen.DASHBOARD },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }

                        Screen.CREATOR -> {
                            TaskCreatorScreen(
                                database = database,
                                onTaskCreated = { currentScreen = Screen.DASHBOARD },
                                onCancel = { currentScreen = Screen.DASHBOARD }
                            )
                        }

                        Screen.HISTORY -> {
                            HistoryLogsScreen(
                                database = database,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )
                        }

                        Screen.GLOBAL_SETTINGS -> {
                            GlobalSettingsScreen(
                                preferencesManager = preferencesManager,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }
}
