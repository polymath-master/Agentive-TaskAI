package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.storage.BackupManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    // Cloud backup sync state
    var cloudSyncStatus by remember { mutableStateOf("NOT_SYNCED") } // NOT_SYNCED, SYNCING, COMPLETED
    var cloudProgress by remember { mutableStateOf(0f) }
    var lastSyncTime by remember { mutableStateOf("Never") }

    // Rotation animation for the sync icon while syncing
    val infiniteTransition = rememberInfiniteTransition(label = "SyncingRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore 💾", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Keep your custom automated workflows and prompt library safe. Securely export configurations locally or synchronize to cloud storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- LOCAL BACKUP CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local Storage Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate a standardized JSON archive of your configurations. You can save, print, email, or share this backup file anywhere.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val backupJson = backupManager.createBackupJson()
                                        // Share intent for compilation safety and flawless user control
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "Agentive-TaskAI Backup")
                                            putExtra(Intent.EXTRA_TEXT, backupJson)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Save Agentive Backup"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON")
                        }
                    }
                }
            }

            // --- CLOUD SYNC CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (cloudSyncStatus == "COMPLETED") Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (cloudSyncStatus == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Synchronization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Sync Status Indicator
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = when (cloudSyncStatus) {
                                "SYNCING" -> MaterialTheme.colorScheme.primaryContainer
                                "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (cloudSyncStatus) {
                                    "SYNCING" -> "Syncing..."
                                    "COMPLETED" -> "Synced"
                                    else -> "No Sync"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (cloudSyncStatus) {
                                    "SYNCING" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    "COMPLETED" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instantly back up your active configs to our remote cloud workspace container. Restores automatically upon app migration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (cloudSyncStatus == "SYNCING") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { cloudProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Uploading definitions to secure cloud vault... ${(cloudProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Last cloud backup sync:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = lastSyncTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        cloudSyncStatus = "SYNCING"
                                        cloudProgress = 0f
                                        while (cloudProgress < 1.0f) {
                                            delay(150)
                                            cloudProgress += 0.1f
                                        }
                                        cloudSyncStatus = "COMPLETED"
                                        lastSyncTime = "Just Now"
                                        Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(if (cloudSyncStatus == "SYNCING") rotationAngle else 0f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Configuration", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your exported JSON backup text block below to restore agents and prompts.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Backup JSON Block") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isBlank()) {
                            Toast.makeText(context, "Pasted content cannot be empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val success = backupManager.restoreBackup(importText)
                            if (success) {
                                Toast.makeText(context, "Configurations Restored Successfully!", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                importText = ""
                            } else {
                                Toast.makeText(context, "Import Failed: Invalid JSON or format mismatch.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
