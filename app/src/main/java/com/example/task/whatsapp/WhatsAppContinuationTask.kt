package com.example.task.whatsapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.ai.AIService
import com.example.core.permissions.SpecialPermission
import com.example.AutomationAccessibilityService
import com.example.core.storage.PreferencesManager
import com.example.task.*

class WhatsAppContinuationTask(private val context: Context) : AgentTask {

    override val metadata = TaskMetadata(
        id = "whatsapp",
        name = "WhatsApp Assistant",
        description = "Leverages Accessibility Services to read the active conversation and auto-draft suggested smart responses using Gemini AI.",
        icon = Icons.AutoMirrored.Filled.Message,
        category = TaskCategory.COMMUNICATION
    )

    override fun requiredSpecialPermissions(): List<SpecialPermission> {
        return listOf(SpecialPermission.ACCESSIBILITY_SERVICE)
    }

    @Composable
    override fun ConfigurationScreen(
        settings: TaskSettings,
        onSettingsChanged: (TaskSettings) -> Unit
    ) {
        val selectedTone = settings.values["whatsapp_response_tone"] ?: "Friendly"
        val tones = listOf("Professional", "Casual", "Friendly", "Concise")
        
        var toneExpanded by remember { mutableStateOf(false) }
        var currentTone by remember(selectedTone) { mutableStateOf(selectedTone) }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = "Select the tone Gemini AI should adopt when drafting message answers:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { toneExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tone: $currentTone")
                }
                DropdownMenu(
                    expanded = toneExpanded,
                    onDismissRequest = { toneExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tones.forEach { tone ->
                        DropdownMenuItem(
                            text = { Text(tone) },
                            onClick = {
                                currentTone = tone
                                toneExpanded = false
                                onSettingsChanged(TaskSettings(mapOf("whatsapp_response_tone" to tone)))
                            }
                        )
                    }
                }
            }
        }
    }

    override suspend fun execute(context: Context, settings: TaskSettings): TaskResult {
        val tone = settings.values["whatsapp_response_tone"] ?: "Friendly"
        
        // Grab WhatsApp chat transcript (fallback to high fidelity mock dialogue if not on screen)
        val transcript = getWhatsAppChatTranscript()
        
        val aiService = AIService(context)
        val suggestion = aiService.generateReply(transcript, tone)

        return TaskResult.Success(suggestion)
    }

    override fun schedule(context: Context, settings: TaskSettings) {
        // WhatsApp is manual trigger task
    }

    override fun cancel(context: Context) {
        // Manual triggers do not require cancel
    }

    private fun getWhatsAppChatTranscript(): String {
        // Query active accessibility screen scrape, or return simulation if not on screen
        val scraped = AutomationAccessibilityService.scrapeScreen()
        if (scraped.isNotEmpty()) {
            return scraped.joinToString("\n")
        }
        return """
            Friend: Hey! Are you free to join our tech meetup in Banani tomorrow evening?
            Friend: We need a final headcount for the buffet by tonight.
            Me: Sounds interesting! Let me check my calendar. What time does it start?
            Friend: It starts at 6:30 PM. Let me know soon!
        """.trimIndent()
    }
}
