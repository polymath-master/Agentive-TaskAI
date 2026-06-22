package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.ai.AIService
import com.example.core.storage.AppDatabase
import com.example.core.storage.PreferencesManager
import com.example.core.storage.TaskHistory
import com.example.task.*
import com.example.task.userdefined.UserDefinedAgentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    database: AppDatabase,
    preferencesManager: PreferencesManager,
    onNavigateToSettings: (String) -> Unit, // passes taskId
    onNavigateToCreator: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Load active API Key from DataStore
    val localApiKey by preferencesManager.geminiApiKeyFlow.collectAsState(initial = "")
    var apiKeyInput by remember { mutableStateOf("") }
    LaunchedEffect(localApiKey) {
        apiKeyInput = localApiKey
    }

    // Load theme configuration
    val isDarkTheme by preferencesManager.isDarkThemeFlow.collectAsState(initial = true)

    // Load list of user-created custom tasks from Room
    val userTasksDb by database.taskDao().getAllUserTasksFlow().collectAsState(initial = emptyList())

    // Combine built-in and user-defined tasks
    val registry = remember { TaskRegistry(context) }
    val compiledTasksList = remember(userTasksDb) {
        val list = mutableListOf<AgentTask>()
        list.addAll(registry.builtInTasks)
        userTasksDb.forEach { entity ->
            list.add(UserDefinedAgentTask(context, entity))
        }
        list
    }

    // Active instant execution status holder
    var runningTaskId by remember { mutableStateOf<String?>(null) }
    var aiOutputDisplay by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Agentive TaskAI", fontWeight = FontWeight.ExtraBold)
                        Text("Modular Personal Automation Assistant", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "History Audit logs")
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch { preferencesManager.saveDarkTheme(!isDarkTheme) }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreator,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Agent", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            // Secure API Key Configuration Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Settings Connection Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Securely associate your AI Studio Gemini API credentials below:", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("Paste Gemini API Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    preferencesManager.saveGeminiApiKey(apiKeyInput)
                                    Toast.makeText(context, "API Key updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Done, contentDescription = "Save Key")
                        }
                    }
                }
            }

            // AI Instant Execution Output Shimmer/Overlay panel
            AnimatedVisibility(
                visible = aiOutputDisplay != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Result", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Assistant Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { aiOutputDisplay = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close panel")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiOutputDisplay ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Modular Task list header
            Text(
                text = "Automated AI Agents",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Categorized Lists scroll context
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                groupedCategories.forEach { category ->
                    val filtered = compiledTasksList.filter { it.metadata.category == category }
                    if (filtered.isNotEmpty()) {
                        item {
                            CategoryDivider(category)
                        }
                        items(filtered) { agent ->
                            AgentInteractionCard(
                                agent = agent,
                                preferencesManager = preferencesManager,
                                isRunning = runningTaskId == agent.metadata.id,
                                onRunNow = {
                                    runningTaskId = agent.metadata.id
                                    coroutineScope.launch {
                                        // Dynamic variable loading
                                        val runResult = withContext(Dispatchers.IO) {
                                            try {
                                                agent.execute(context, TaskSettings())
                                            } catch (e: Exception) {
                                                TaskResult.Error(e.localizedMessage ?: "execution error")
                                            }
                                        }

                                        // Store run history
                                        withContext(Dispatchers.IO) {
                                            database.taskDao().insertHistory(
                                                TaskHistory(
                                                    taskId = agent.metadata.id,
                                                    taskName = agent.metadata.name,
                                                    status = if (runResult is TaskResult.Success) "SUCCESS" else "ERROR",
                                                    message = when (runResult) {
                                                        is TaskResult.Success -> runResult.output ?: "Manual trigger executed."
                                                        is TaskResult.Error -> runResult.message
                                                        is TaskResult.Cancelled -> "Cancelled."
                                                    }
                                                )
                                            )
                                        }

                                        runningTaskId = null
                                        when (runResult) {
                                            is TaskResult.Success -> {
                                                aiOutputDisplay = runResult.output ?: "Execution finished successfully!"
                                            }
                                            is TaskResult.Error -> {
                                                Toast.makeText(context, "Error: ${runResult.message}", Toast.LENGTH_LONG).show()
                                            }
                                            else -> {}
                                        }
                                    }
                                },
                                onConfigure = { onNavigateToSettings(agent.metadata.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

val groupedCategories = listOf(
    TaskCategory.NEWS,
    TaskCategory.COMMUNICATION,
    TaskCategory.PRODUCTIVITY,
    TaskCategory.CUSTOM
)

@Composable
fun CategoryDivider(category: TaskCategory) {
    val name = when (category) {
        TaskCategory.NEWS -> "News & Editorial Brifs"
        TaskCategory.COMMUNICATION -> "Dialer & Correspondence Assistants"
        TaskCategory.PRODUCTIVITY -> "Workspace Automation"
        TaskCategory.CUSTOM -> "My Customized Automated Schemes"
    }

    Text(
        text = name.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun AgentInteractionCard(
    agent: AgentTask,
    preferencesManager: PreferencesManager,
    isRunning: Boolean,
    onRunNow: () -> Unit,
    onConfigure: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }

    var isEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(agent.metadata.id) {
        preferencesManager.isTaskEnabledFlow(agent.metadata.id).collect {
            isEnabled = it
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Icon(imageVector = agent.metadata.icon, contentDescription = null, sizeModifier = Modifier.size(28.dp), tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = agent.metadata.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isEnabled) "Active schedule: Daily" else "Currently Halted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        coroutineScope.launch {
                            preferencesManager.setTaskEnabled(agent.metadata.id, it)
                            if (it) {
                                agent.schedule(context, TaskSettings())
                            } else {
                                agent.cancel(context)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = agent.metadata.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expanded Settings & Immediate run block
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onConfigure) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRunNow,
                        enabled = !isRunning
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running...")
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, sizeModifier: Modifier, tint: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = sizeModifier, tint = tint)
}
