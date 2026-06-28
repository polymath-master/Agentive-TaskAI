package com.example.task

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    @Composable
    fun ResultView(outputData: AgentOutputData, onAction: (AgentAction) -> Unit) {
        DefaultResultView(outputData, onAction)
    }
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

// --- Agentive-TaskAI v4.0 Schema & Actions ---

sealed class AgentOutputData {
    data class ArticleList(val articles: List<ArticleOutput>) : AgentOutputData()
    data class Media(val title: String, val mediaUrl: String, val isVideo: Boolean = false) : AgentOutputData()
    data class TextNote(val title: String, val content: String) : AgentOutputData()
    data class BookContent(val title: String, val author: String, val content: String, val currentPage: Int = 0) : AgentOutputData()
    data class GenericText(val text: String) : AgentOutputData()
}

data class ArticleOutput(
    val title: String,
    val summary: String,
    val innovation: String,
    val link: String
)

sealed class AgentAction {
    data class OpenUrl(val url: String) : AgentAction()
    data class PlayMedia(val url: String) : AgentAction()
    data class SaveNote(val title: String, val content: String) : AgentAction()
    data class SummarizeText(val selectedText: String) : AgentAction()
    data class PageChanged(val pageIndex: Int) : AgentAction()
}

@Composable
fun DefaultResultView(outputData: AgentOutputData, onAction: (AgentAction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (outputData) {
                    is AgentOutputData.GenericText -> {
                        Text(
                            text = "Agent Output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = outputData.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is AgentOutputData.TextNote -> {
                        Text(
                            text = outputData.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = outputData.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        // Fallback generic info
                        Text(
                            text = "Result Content",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = outputData.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

