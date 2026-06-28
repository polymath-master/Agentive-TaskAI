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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.storage.AppDatabase
import com.example.core.storage.PreferencesManager
import com.example.core.storage.ResultStateHolder
import com.example.core.storage.TaskHistory
import com.example.task.*
import com.example.task.userdefined.UserDefinedAgentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    database: AppDatabase,
    preferencesManager: PreferencesManager,
    onNavigateToSettings: (String) -> Unit, // passes taskId
    onNavigateToCreator: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToGlobalSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Tab state: 0 = Results, 1 = Agents
    var selectedTab by remember { mutableIntStateOf(0) }

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

    // Read reactive latest results state
    val latestAgentId by ResultStateHolder.getLastAgentIdFlow(context).collectAsState(initial = null)
    val latestOutputData by ResultStateHolder.getLastOutputDataFlow(context).collectAsState(initial = null)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Agentive TaskAI 🔮", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (selectedTab == 0) "Latest Execution Results" else "My Automated AI Agents",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGlobalSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Global App Settings")
                    }
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = "Results") },
                    label = { Text("Results", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = "Agents") },
                    label = { Text("Agents", fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // --- RESULTS TAB ---
                    if (latestAgentId != null && latestOutputData != null) {
                        val activeTask = remember(latestAgentId, compiledTasksList) {
                            compiledTasksList.find { it.metadata.id == latestAgentId }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header showing which agent produced this result
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = activeTask?.metadata?.icon ?: Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Output: ${activeTask?.metadata?.name ?: "Custom Agent"}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    
                                    Text(
                                        text = "Interactive",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // Render the active task's custom result view!
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (activeTask != null) {
                                    activeTask.ResultView(
                                        outputData = latestOutputData!!,
                                        onAction = { action ->
                                            when (action) {
                                                is AgentAction.OpenUrl -> {
                                                    try {
                                                        val intent = android.content.Intent(
                                                            android.content.Intent.ACTION_VIEW,
                                                            android.net.Uri.parse(action.url)
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                is AgentAction.SaveNote -> {
                                                    Toast.makeText(context, "Note Saved: ${action.title}", Toast.LENGTH_SHORT).show()
                                                }
                                                is AgentAction.PageChanged -> {
                                                    // Handle ebook page offsets if applicable
                                                }
                                                else -> {
                                                    Toast.makeText(context, "Action: $action", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Fallback to default result view
                                    DefaultResultView(
                                        outputData = latestOutputData!!,
                                        onAction = {}
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty State Results
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Agent Executions Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Run any built-in or custom agent from the 'Agents' tab, and their rich outputs will display here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { selectedTab = 1 }) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Active Agents")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // --- AGENTS TAB ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            groupedCategories.forEach { category ->
                                val filtered = compiledTasksList.filter { it.metadata.category == category }
                                if (filtered.isNotEmpty()) {
                                    item {
                                        CategoryDivider(category)
                                    }
                                    items(filtered) { agent ->
                                        val isUserDefined = agent.metadata.id.startsWith("user_")
                                        AgentInteractionCard(
                                            agent = agent,
                                            preferencesManager = preferencesManager,
                                            isRunning = runningTaskId == agent.metadata.id,
                                            onRunNow = {
                                                runningTaskId = agent.metadata.id
                                                coroutineScope.launch {
                                                    val runResult = withContext(Dispatchers.IO) {
                                                        try {
                                                            val pm = preferencesManager
                                                            val vals = mapOf(
                                                                "gmail_user_email" to pm.gmailUserEmailFlow.first(),
                                                                "sheet_url" to pm.sheetUrlFlow.first(),
                                                                "template_doc_url" to pm.templateDocUrlFlow.first()
                                                            )
                                                            agent.execute(context, TaskSettings(vals))
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
                                                            Toast.makeText(context, "Agent completed successfully!", Toast.LENGTH_SHORT).show()
                                                            // Auto switch to Results tab to view output!
                                                            selectedTab = 0
                                                        }
                                                        is TaskResult.Error -> {
                                                            Toast.makeText(context, "Error: ${runResult.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            },
                                            onConfigure = { onNavigateToSettings(agent.metadata.id) },
                                            onDelete = if (isUserDefined) {
                                                {
                                                    coroutineScope.launch {
                                                        agent.cancel(context)
                                                        withContext(Dispatchers.IO) {
                                                            database.taskDao().deleteUserTaskById(agent.metadata.id)
                                                        }
                                                        Toast.makeText(context, "Agent deleted successfully.", Toast.LENGTH_SHORT).show()
                                                        try {
                                                            com.example.widget.updateWidget(context)
                                                        } catch (e: Exception) {}
                                                    }
                                                }
                                            } else null
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

val groupedCategories = listOf(
    TaskCategory.NEWS,
    TaskCategory.COMMUNICATION,
    TaskCategory.PRODUCTIVITY,
    TaskCategory.CUSTOM
)

@Composable
fun CategoryDivider(category: TaskCategory) {
    val name = when (category) {
        TaskCategory.NEWS -> "Trending News & Summarizer Agents"
        TaskCategory.COMMUNICATION -> "Dialer & Messaging Correspondence"
        TaskCategory.PRODUCTIVITY -> "Personal Office Productivity"
        TaskCategory.CUSTOM -> "My Custom Automated Workflows"
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
    onConfigure: () -> Unit,
    onDelete: (() -> Unit)? = null
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
                    Icon(
                        imageVector = agent.metadata.icon,
                        contentDescription = null,
                        sizeModifier = Modifier.size(28.dp),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Agent",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                                try {
                                    com.example.widget.updateWidget(context)
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
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
