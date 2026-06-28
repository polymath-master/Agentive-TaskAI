package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.AIService
import com.example.core.storage.AppDatabase
import com.example.core.storage.PromptTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val promptList by db.taskDao().getAllPromptsFlow().collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<PromptTemplate?>(null) }

    var inputName by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var inputLabels by remember { mutableStateOf("") }

    var isImprovingPrompt by remember { mutableStateOf(false) }

    // Prepopulate some template prompts if empty
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existing = db.taskDao().getAllPromptsFlow()
            // We insert some default ones if there's nothing
            if (promptList.isEmpty()) {
                db.taskDao().insertPrompt(
                    PromptTemplate(
                        name = "Article Summarizer Brief",
                        text = "Summarize the following tech article in exactly 3 short bullet points, focusing on the core innovation and market impact.",
                        labels = "Summarization, News"
                    )
                )
                db.taskDao().insertPrompt(
                    PromptTemplate(
                        name = "Professional Email Reply",
                        text = "Draft a professional and polite reply to the sender, acknowledging receipt of their proposal and scheduling a call next Tuesday.",
                        labels = "Email, Professional"
                    )
                )
                db.taskDao().insertPrompt(
                    PromptTemplate(
                        name = "Technical Code Simplifier",
                        text = "Analyze this block of code and simplify it. Use clear, modern patterns and document complexity changes.",
                        labels = "Coding, Productivity"
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Prompt Manager 💡", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPrompt = null
                    inputName = ""
                    inputText = ""
                    inputLabels = ""
                    showEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Prompt")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Manage your AI Instructions. Tap any prompt to copy, or edit to improve with Gemini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (promptList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(promptList) { prompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Prompt Template", prompt.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Prompt text copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prompt.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                // Duplicate prompt
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        db.taskDao().insertPrompt(
                                                            PromptTemplate(
                                                                name = "${prompt.name} (Copy)",
                                                                text = prompt.text,
                                                                labels = prompt.labels
                                                            )
                                                        )
                                                    }
                                                    Toast.makeText(context, "Prompt duplicated!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(20.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                editingPrompt = prompt
                                                inputName = prompt.name
                                                inputText = prompt.text
                                                inputLabels = prompt.labels
                                                showEditDialog = true
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        db.taskDao().deletePromptById(prompt.id)
                                                    }
                                                    Toast.makeText(context, "Prompt deleted!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = prompt.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Draw chips for tags
                                if (prompt.labels.isNotBlank()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        prompt.labels.split(",").forEach { label ->
                                            if (label.trim().isNotBlank()) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(label.trim(), style = MaterialTheme.typography.labelSmall) },
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingPrompt == null) "New Prompt Template" else "Edit Prompt Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Template Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputLabels,
                        onValueChange = { inputLabels = it },
                        label = { Text("Labels (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("AI Instruction Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    // Improve button
                    Button(
                        onClick = {
                            if (inputText.isBlank()) {
                                Toast.makeText(context, "Enter some text to improve", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                isImprovingPrompt = true
                                val ai = AIService(context)
                                val prompt = """
                                    Improve this prompt, make it more precise, concise, and professional while retaining original intent. 
                                    Keep instructions clear. Respond ONLY with the improved prompt text, no explanation or conversational text or markdown code blocks:
                                    
                                    $inputText
                                """.trimIndent()
                                try {
                                    val result = ai.executeCustomPrompt(prompt)
                                    if (result.isNotBlank() && !result.startsWith("Error")) {
                                        inputText = result
                                        Toast.makeText(context, "Prompt improved with Gemini!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Gemini Improvement Failed: $result", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isImprovingPrompt = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isImprovingPrompt
                    ) {
                        if (isImprovingPrompt) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Improving...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Improve with AI")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isBlank() || inputText.isBlank()) {
                            Toast.makeText(context, "Name and Text are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                if (editingPrompt == null) {
                                    db.taskDao().insertPrompt(
                                        PromptTemplate(
                                            name = inputName,
                                            text = inputText,
                                            labels = inputLabels
                                        )
                                    )
                                } else {
                                    db.taskDao().insertPrompt(
                                        editingPrompt!!.copy(
                                            name = inputName,
                                            text = inputText,
                                            labels = inputLabels,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            Toast.makeText(context, "Prompt Template saved!", Toast.LENGTH_SHORT).show()
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
