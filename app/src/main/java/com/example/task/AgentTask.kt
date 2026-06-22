package com.example.task

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.permissions.SpecialPermission

interface AgentTask {
    val metadata: TaskMetadata

    @Composable
    fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    )

    suspend fun execute(context: Context, settings: TaskSettings): TaskResult

    fun schedule(context: Context, settings: TaskSettings)
    fun cancel(context: Context)

    fun requiredSpecialPermissions(): List<SpecialPermission> = emptyList()
}

data class TaskMetadata(
    val id: String,            // unique identifier
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: TaskCategory
)

enum class TaskCategory {
    NEWS,
    COMMUNICATION,
    PRODUCTIVITY,
    CUSTOM
}

data class TaskSettings(
    val values: Map<String, String> = emptyMap()
)

sealed class TaskResult {
    data class Success(val output: String? = null) : TaskResult()
    data class Error(val message: String) : TaskResult()
    object Cancelled : TaskResult()
}
