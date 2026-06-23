package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.storage.AppDatabase
import com.example.core.storage.UserTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Composable
fun TaskCreatorScreen(
    database: AppDatabase,
    onTaskCreated: () -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    BackHandler {
        if (currentStep > 1) {
            currentStep--
        } else {
            onCancel()
        }
    }
    val totalSteps = 6

    // Form inputs state
    var selectedTrigger by remember { mutableStateOf("SCHEDULE") }
    // Trigger params
    var triggerHour by remember { mutableStateOf(12) }
    var triggerMinute by remember { mutableStateOf(0) }
    var selectedDays = remember { mutableStateListOf("MON", "TUE", "WED", "THU", "FRI") }
    var notificationKeyword by remember { mutableStateOf("") }

    // Condition state
    var conditionWifiOnly by remember { mutableStateOf(false) }

    // Action state
    var selectedAction by remember { mutableStateOf("AI_GENERATE_AND_NOTIFY") }
    // Action params
    var actionPrompt by remember { mutableStateOf("Generate an inspirational quote.") }
    var actionBodyText by remember { mutableStateOf("Alarm fired successfully!") }
    var actionRecipient by remember { mutableStateOf("mushfiq@aistudio.com") }
    var actionWebhookUrl by remember { mutableStateOf("https://hook.example.com/automation") }
    var actionTargetPackage by remember { mutableStateOf("com.android.settings") }

    // Metadata state
    var taskName by remember { mutableStateOf("Evening Quote of the Day") }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Agentive Task Builder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Stepper progress indicator
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Render dynamic content based on active wizard step
                when (currentStep) {
                    1 -> TriggerPickerSection(
                        selected = selectedTrigger,
                        onSelect = { selectedTrigger = it }
                    )
                    2 -> TriggerConfigSection(
                        triggerType = selectedTrigger,
                        hour = triggerHour,
                        minute = triggerMinute,
                        onTimeChanged = { h, m -> triggerHour = h; triggerMinute = m },
                        keyword = notificationKeyword,
                        onKeywordChange = { notificationKeyword = it }
                    )
                    3 -> ConditionSection(
                        wifiOnly = conditionWifiOnly,
                        onWifiToggle = { conditionWifiOnly = it }
                    )
                    4 -> ActionPickerSection(
                        selected = selectedAction,
                        onSelect = { selectedAction = it }
                    )
                    5 -> ActionConfigSection(
                        actionType = selectedAction,
                        prompt = actionPrompt,
                        onPromptChange = { actionPrompt = it },
                        body = actionBodyText,
                        onBodyChange = { actionBodyText = it },
                        recipient = actionRecipient,
                        onRecipientChange = { actionRecipient = it },
                        url = actionWebhookUrl,
                        onUrlChange = { actionWebhookUrl = it },
                        pkg = actionTargetPackage,
                        onPkgChange = { actionTargetPackage = it }
                    )
                    6 -> SaveAndCompleteSection(
                        name = taskName,
                        onNameChange = { taskName = it },
                        trigger = selectedTrigger,
                        action = selectedAction,
                        prompt = actionPrompt,
                        webhook = actionWebhookUrl
                    )
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    OutlinedButton(onClick = { currentStep-- }) {
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (currentStep < totalSteps) {
                    Button(onClick = { currentStep++ }) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                saveUserTask(
                                    database = database,
                                    name = taskName,
                                    trigger = selectedTrigger,
                                    hour = triggerHour,
                                    minute = triggerMinute,
                                    days = selectedDays,
                                    keyword = notificationKeyword,
                                    wifiOnly = conditionWifiOnly,
                                    action = selectedAction,
                                    prompt = actionPrompt,
                                    body = actionBodyText,
                                    recipient = actionRecipient,
                                    webhookUrl = actionWebhookUrl,
                                    targetPkg = actionTargetPackage,
                                    onComplete = {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            onTaskCreated()
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Save & Build")
                    }
                }
            }
        }
    }
}

@Composable
fun TriggerPickerSection(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("What triggers this workflow?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Choose the signal that launches your automation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        TriggerSelectorCard("Time Schedule", "Runs daily or weekly at a chosen hour.", "SCHEDULE", Icons.Default.Timeline, selected, onSelect)
        TriggerSelectorCard("Manual Button", "Runs only when you click 'Run now' or trigger the widget.", "MANUAL", Icons.Default.TouchApp, selected, onSelect)
        TriggerSelectorCard("Notification Intercept", "Triggers when a target app issues containing keywords notification.", "NOTIFICATION_RECEIVED", Icons.Default.Inbox, selected, onSelect)
        TriggerSelectorCard("Call State Changed", "Triggers immediately on missed/active phone call alerts.", "CALL_STATE", Icons.Default.Call, selected, onSelect)
    }
}

@Composable
fun TriggerSelectorCard(
    title: String,
    desc: String,
    type: String,
    icon: ImageVector,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == type
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onSelect(type) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TriggerConfigSection(
    triggerType: String,
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit,
    keyword: String,
    onKeywordChange: (String) -> Unit
) {
    Column {
        Text("Configure your trigger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        when (triggerType) {
            "SCHEDULE" -> {
                Text("Specify scheduled time below:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                // Clean time inputs
                Row {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { onTimeChanged(it.toIntOrNull() ?: hour, minute) },
                        label = { Text("Hour (24-HR)") },
                        modifier = Modifier.width(100.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = _formatTwoDigits(minute),
                        onValueChange = { onTimeChanged(hour, it.toIntOrNull() ?: minute) },
                        label = { Text("Minute") },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
            "NOTIFICATION_RECEIVED" -> {
                Text("Search keywords to intercept in system notifications:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    label = { Text("Trigger Keyword (e.g., 'Emergency')") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                Text("This trigger ('$triggerType') does not require additional manual settings. It runs automatically in real-time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun _formatTwoDigits(valDigit: Int): String {
    return if (valDigit < 10) "0$valDigit" else valDigit.toString()
}

@Composable
fun ConditionSection(wifiOnly: Boolean, onWifiToggle: (Boolean) -> Unit) {
    Column {
        Text("Workflow filters (Optional)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Set pre-conditions to restrict when this automation runs.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Restrict to WI-FI only", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Skip background run if connected to mobile data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = wifiOnly, onCheckedChange = onWifiToggle)
        }
    }
}

@Composable
fun ActionPickerSection(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("What action does the assistant perform?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        TriggerSelectorCard("AI Generation & Notification", "Gemini interprets draft variables and posts results.", "AI_GENERATE_AND_NOTIFY", Icons.Default.AutoAwesome, selected, onSelect)
        TriggerSelectorCard("Simple Notification Alert", "Displays static predefined alert notifications directly.", "SHOW_NOTIFICATION", Icons.Default.Notifications, selected, onSelect)
        TriggerSelectorCard("HTTP Webhook", "Pings target external REST endpoint URLs with POST/GET.", "WEBHOOK", Icons.Default.Link, selected, onSelect)
        TriggerSelectorCard("Workspace Gmail Dispatch", "Drafts and sends personalized invitation email template.", "SEND_EMAIL", Icons.Default.SendToMobile, selected, onSelect)
        TriggerSelectorCard("Launch Application", "Brings target application package directly to user foreground screen.", "OPEN_APP", Icons.Default.Launch, selected, onSelect)
        TriggerSelectorCard("Show Toast", "Displays a quick visual toast pop-up on the screen.", "SHOW_TOAST", Icons.Default.TextSnippet, selected, onSelect)
        TriggerSelectorCard("Copy to Clipboard", "Copies generated/custom text directly to device clipboard.", "COPY_CLIPBOARD", Icons.Default.ContentCopy, selected, onSelect)
        TriggerSelectorCard("Display Overlay Dialog", "Renders a floating system overlay dialog.", "SHOW_OVERLAY", Icons.Default.Layers, selected, onSelect)
    }
}

@Composable
fun ActionConfigSection(
    actionType: String,
    prompt: String,
    onPromptChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    recipient: String,
    onRecipientChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    pkg: String,
    onPkgChange: (String) -> Unit
) {
    Column {
        Text("Configure target actions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        when (actionType) {
            "AI_GENERATE_AND_NOTIFY" -> {
                Text("Enter the instructions/prompts for Gemini AI:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = { Text("Prompt instructions") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
            "SHOW_NOTIFICATION" -> {
                Text("Notification body text:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Notification Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "SHOW_TOAST" -> {
                Text("Enter the text to display in toast pop-up:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Toast Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "COPY_CLIPBOARD" -> {
                Text("Enter the text to copy to clipboard:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Clipboard Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "SHOW_OVERLAY" -> {
                Text("Enter the overlay alert text:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Overlay Dialog Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "WEBHOOK" -> {
                Text("Enter the remote target webhook REST URL:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("Target URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "SEND_EMAIL" -> {
                Text("Configure custom Gmail invitation recipient:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recipient,
                    onValueChange = onRecipientChange,
                    label = { Text("Recipient email") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "OPEN_APP" -> {
                Text("Specify the target application package name to open:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pkg,
                    onValueChange = onPkgChange,
                    label = { Text("Package (e.g. com.whatsapp)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SaveAndCompleteSection(
    name: String,
    onNameChange: (String) -> Unit,
    trigger: String,
    action: String,
    prompt: String,
    webhook: String
) {
    Column {
        Text("Label & Save Automation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Give your assistant workflow a unique title to complete.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Automation Title") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Assembled Automation Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Trigger Signal: $trigger", style = MaterialTheme.typography.bodyMedium)
                Text("Action Event: $action", style = MaterialTheme.typography.bodyMedium)
                if (action == "AI_GENERATE_AND_NOTIFY") {
                    Text("AI Prompt: '$prompt'", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (action == "WEBHOOK") {
                    Text("Route URL: $webhook", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private suspend fun saveUserTask(
    database: AppDatabase,
    name: String,
    trigger: String,
    hour: Int,
    minute: Int,
    days: List<String>,
    keyword: String,
    wifiOnly: Boolean,
    action: String,
    prompt: String,
    body: String,
    recipient: String,
    webhookUrl: String,
    targetPkg: String,
    onComplete: () -> Unit
) {
    // Generate Section 10.2 schema payload with absolute fidelity
    val rootObj = JSONObject()
    val id = "user_" + UUID.randomUUID().toString().take(6)
    
    rootObj.put("id", id)
    rootObj.put("name", name)

    val triggerObj = JSONObject()
    triggerObj.put("type", trigger)
    if (trigger == "SCHEDULE") {
        val scheduleObj = JSONObject()
        scheduleObj.put("hour", hour)
        scheduleObj.put("minute", minute)
        
        val daysArray = JSONArray()
        days.forEach { daysArray.put(it) }
        scheduleObj.put("daysOfWeek", daysArray)
        
        triggerObj.put("schedule", scheduleObj)
    } else if (trigger == "NOTIFICATION_RECEIVED") {
        triggerObj.put("keyword", keyword)
    }
    rootObj.put("trigger", triggerObj)

    val condObj = JSONObject()
    condObj.put("type", "NETWORK")
    condObj.put("wifiOnly", wifiOnly)
    rootObj.put("condition", condObj)

    val actionObj = JSONObject()
    actionObj.put("type", action)
    when (action) {
        "AI_GENERATE_AND_NOTIFY" -> {
            actionObj.put("prompt", prompt)
            actionObj.put("notificationChannel", "general")
        }
        "SHOW_NOTIFICATION" -> {
            actionObj.put("text", body)
        }
        "SHOW_TOAST" -> {
            actionObj.put("text", body)
        }
        "COPY_CLIPBOARD" -> {
            actionObj.put("text", body)
        }
        "SHOW_OVERLAY" -> {
            actionObj.put("text", body)
        }
        "WEBHOOK" -> {
            actionObj.put("url", webhookUrl)
        }
        "SEND_EMAIL" -> {
            actionObj.put("recipient", recipient)
            actionObj.put("subject", "Automation Call triggered from TaskAI")
            actionObj.put("body", "Hello, Event inviter completed.")
        }
        "OPEN_APP" -> {
            actionObj.put("package", targetPkg)
        }
    }
    rootObj.put("action", actionObj)

    val entity = UserTaskEntity(
        id = id,
        name = name,
        jsonDefinition = rootObj.toString(),
        isEnabled = true
    )

    database.taskDao().insertUserTask(entity)
    onComplete()
}
