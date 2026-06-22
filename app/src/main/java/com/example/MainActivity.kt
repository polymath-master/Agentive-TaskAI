package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.core.storage.AppDatabase
import com.example.core.storage.PreferencesManager
import com.example.task.TaskRegistry
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryLogsScreen
import com.example.ui.screens.TaskCreatorScreen
import com.example.ui.screens.TaskSettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    DASHBOARD,
    SETTINGS,
    CREATOR,
    HISTORY
}

class MainActivity : ComponentActivity() {
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

                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                database = database,
                                preferencesManager = preferencesManager,
                                onNavigateToSettings = { taskId ->
                                    selectedTaskId = taskId
                                    currentScreen = Screen.SETTINGS
                                },
                                onNavigateToCreator = {
                                    currentScreen = Screen.CREATOR
                                },
                                onNavigateToHistory = {
                                    currentScreen = Screen.HISTORY
                                }
                            )
                        }

                        Screen.SETTINGS -> {
                            val activeTask = selectedTaskId?.let { registry.getTaskById(it) }
                            if (activeTask != null) {
                                TaskSettingsScreen(
                                    task = activeTask,
                                    preferencesManager = preferencesManager,
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            } else {
                                currentScreen = Screen.DASHBOARD
                            }
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
                    }
                }
            }
        }
    }
}
