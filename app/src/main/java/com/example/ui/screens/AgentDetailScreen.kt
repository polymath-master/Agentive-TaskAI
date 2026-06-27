package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.permissions.PermissionUtils
import com.example.core.scheduler.AgentRunner
import com.example.core.storage.AppDatabase
import com.example.core.storage.PreferencesManager
import com.example.core.storage.TaskHistory
import com.example.task.AgentTask
import com.example.task.TaskRegistry
import com.example.task.TaskSettings
import com.example.task.userdefined.UserDefinedAgentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(
    taskId: String,
    database: AppDatabase,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAIChatReconfigure: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val registry = remember { TaskRegistry(context) }
    
    // Resolve task definition
    var agentTask by remember { mutableStateOf<AgentTask?>(null) }
    var taskName by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskIcon by remember { mutableStateOf(Icons.Default.Android) }
    var isUserDefined by remember { mutableStateOf(false) }
    
    LaunchedEffect(taskId) {
        isUserDefined = taskId.startsWith("user_")
        if (isUserDefined) {
            val entity = database.taskDao().getUserTaskById(taskId)
            if (entity != null) {
                taskName = entity.name
                taskDesc = "User-created automated workflow."
                taskIcon = Icons.Default.Build
                agentTask = UserDefinedAgentTask(context, entity)
            }
        } else {
            val builtIn = registry.getTaskById(taskId)
            if (builtIn != null) {
                taskName = builtIn.metadata.name
                taskDesc = builtIn.metadata.description
                taskIcon = builtIn.metadata.icon
                agentTask = builtIn
            }
        }
    }

    // Load active logs
    val historyLogs by database.taskDao().getHistoryByTaskIdFlow(taskId).collectAsState(initial = emptyList())
    
    // Load enabled status
    var isEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(taskId) {
        preferencesManager.isTaskEnabledFlow(taskId).collect {
            isEnabled = it
        }
    }

    var isRunning by remember { mutableStateOf(false) }
    var showLogsOnly by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (taskName.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // 1. Agent Title Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = taskIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = taskName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = taskDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Status & Control Panel Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Agent Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEnabled) "✅ Active Scheduled" else "❌ Halted / Paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    isEnabled = checked
                                    coroutineScope.launch {
                                        preferencesManager.setTaskEnabled(taskId, checked)
                                        agentTask?.let { task ->
                                            if (checked) {
                                                task.schedule(context, TaskSettings())
                                                Toast.makeText(context, "Agent schedules activated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                task.cancel(context)
                                                Toast.makeText(context, "Agent schedules disabled.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Execution Type",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isUserDefined) "Custom Flowchart Chain" else "Built-In Assistant System",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Button(
                                onClick = {
                                    isRunning = true
                                    coroutineScope.launch {
                                        val runner = AgentRunner(context)
                                        runner.runAgent(taskId)
                                        isRunning = false
                                    }
                                },
                                enabled = !isRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Executing...")
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Now")
                                }
                            }
                        }
                    }
                }

                if (isUserDefined) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "AI Conversation Companion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Chat with the agent to edit triggers, modify conditions, add custom actions, or rollback to a previous version.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            FilledTonalButton(
                                onClick = { onNavigateToAIChatReconfigure(taskId) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat Reconfig")
                            }
                        }
                    }
                }

                // 3. Telemetry Logs Header and Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telemetry Execution Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showLogsOnly = !showLogsOnly }) {
                        Text(if (showLogsOnly) "Collapse Logs" else "Show Logs")
                    }
                }

                // 4. Scrollable Logs List
                AnimatedVisibility(
                    visible = showLogsOnly,
                    modifier = Modifier.weight(1f)
                ) {
                    if (historyLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No telemetry logs recorded for this agent yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            items(historyLogs) { log ->
                                LogEntryRow(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryRow(log: TaskHistory) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedTime = dateFormat.format(Date(log.timestamp))

    val badgeColor = when (log.status) {
        "SUCCESS" -> Color(0xFF2E7D32)     // Forest Green
        "ERROR" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline  // Cancelled or Idle
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
