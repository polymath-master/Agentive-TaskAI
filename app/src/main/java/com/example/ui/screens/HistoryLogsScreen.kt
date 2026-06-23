package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.storage.AppDatabase
import com.example.core.storage.TaskHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryLogsScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { database.taskDao() }
    val logsList by dao.getAllHistoryFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Agent Execution Audit", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    dao.clearHistory()
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { innerPadding ->
        if (logsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Telemetry Log is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Once your scheduled background agents execute, detailed audit trail summaries will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp)
            ) {
                items(logsList) { log ->
                    TaskHistoryCard(log)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TaskHistoryCard(item: TaskHistory) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val dateStr = remember(item.timestamp) { formatter.format(Date(item.timestamp)) }

    val isSuccess = item.status == "SUCCESS"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.AssignmentTurnedIn else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.taskName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
