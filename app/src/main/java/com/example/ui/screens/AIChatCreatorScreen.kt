package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AIService
import com.example.core.storage.AppDatabase
import com.example.core.storage.UserTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Data model for messages in the conversation
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isJson: Boolean = false,
    val parsedData: ParsedAgentConfig? = null
)

// Simplified representation of the AI-generated JSON definition
data class ParsedAgentConfig(
    val name: String,
    val triggerType: String,
    val triggerParams: Map<String, Any>,
    val actionType: String,
    val actionParams: Map<String, Any>,
    val wifiOnly: Boolean = false,
    val rawJson: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AIChatCreatorScreen(
    database: AppDatabase,
    onTaskCreated: () -> Unit,
    onOpenWizardWithConfig: (ParsedAgentConfig) -> Unit, // Seamless transition to visual editor
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiService = remember { AIService(context) }
    
    val listState = rememberLazyListState()
    var inputMessage by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    // System prompt configuration for the AI builder
    val systemPrompt = """
        You are Agentive's conversational AI Agent Builder. Your goal is to guide the user to design an automation agent.
        Greet the user friendly: "Hi! I'm your AI assistant. Tell me what kind of agent you want to create."
        Ask clarifying questions to gather any missing information, such as:
        1. What signal triggers this automation? (Available: SCHEDULE, MANUAL, NOTIFICATION_RECEIVED, CALL_STATE)
        2. Are there any conditions? (e.g. WiFi only)
        3. What actions should be taken when triggered? (Available actions: AI_GENERATE_AND_NOTIFY, SHOW_NOTIFICATION, WEBHOOK, SEND_EMAIL, OPEN_APP, SHOW_TOAST, COPY_CLIPBOARD, SHOW_OVERLAY)
        4. What parameters are needed? (e.g. prompt, email recipient, webhook URL, package name, text to show, scheduled time, trigger keyword)

        Make sure to ask questions one or two at a time so as not to overwhelm the user. Maintain the conversation state and remember prior choices.
        Once you have enough information to fully define the automation, output the complete agent JSON configuration inside markdown codeblocks like this:
        ```json
        {
          "name": "Evening Quote of the Day",
          "trigger": {
            "type": "SCHEDULE",
            "schedule": {
              "hour": 18,
              "minute": 30,
              "daysOfWeek": ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
            }
          },
          "condition": {
            "type": "NETWORK",
            "wifiOnly": false
          },
          "action": {
            "type": "AI_GENERATE_AND_NOTIFY",
            "prompt": "Give a short inspirational quote under 140 characters",
            "notificationChannel": "general"
          }
        }
        ```
        If the action is SHOW_NOTIFICATION, SHOW_TOAST, COPY_CLIPBOARD, or SHOW_OVERLAY, use the parameter "text" (e.g., "text": "Hello!").
        If the action is WEBHOOK, use the parameter "url".
        If the action is SEND_EMAIL, use the parameter "recipient".
        If the action is OPEN_APP, use the parameter "package".
        If the trigger is NOTIFICATION_RECEIVED, use the parameter "keyword" under trigger.

        Do not generate the final JSON code block until you have collected sufficient parameters or the user is ready.
    """.trimIndent()

    // Message lists
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hi! I'm your AI assistant. Let's design a custom automation agent together. Tell me, what task would you like to automate?",
                isUser = false
            )
        )
    }

    // Capture soft keyboard visibility to adjust scroll position automatically
    val isKeyboardVisible = WindowInsets.isImeVisible

    // Auto scroll to bottom when message arrives, when typing status changes, or when keyboard opens/closes
    LaunchedEffect(messages.size, isTyping, isKeyboardVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler(onBack = onCancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Agentive Chat Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Interactive Conversation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Explicitly zero insets so we handle nesting manually
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()) // Correct status bar adjustment
                .imePadding() // Pushes whole chat scene above soft keyboard flawlessly
                .navigationBarsPadding() // Space for system navigation bar when keyboard is hidden
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Conversational Scroll Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onSave = { config ->
                            scope.launch(Dispatchers.IO) {
                                saveParsedConfig(database, config) {
                                    scope.launch(Dispatchers.Main) {
                                        try {
                                            com.example.widget.updateWidget(context)
                                        } catch (e: Exception) {}
                                        onTaskCreated()
                                    }
                                }
                            }
                        },
                        onEdit = { config ->
                            onOpenWizardWithConfig(config)
                        }
                    )
                }

                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Suggestions & Input Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Contextual Suggestions Row (Only visible at start to help guide users)
                    if (messages.size == 1) {
                        ScrollableTabRow(
                            selectedTabIndex = 0,
                            divider = {},
                            indicator = {},
                            edgePadding = 12.dp,
                            containerColor = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            val suggestions = listOf(
                                "Create a daily quote notification ⏰" to "Create a scheduled task that runs every day at 18:30 to send a custom inspirational quote notification to my phone.",
                                "Notify me when WiFi connects 📶" to "Create an automation task that notifies me when I connect to WiFi with a custom welcome toast.",
                                "Send email when notified 📧" to "I want to build an agent that triggers when I receive a specific system notification to send a notification report email."
                            )
                            suggestions.forEach { (label, prompt) ->
                                AssistChip(
                                    onClick = {
                                        inputMessage = prompt
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Main TextInput row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = { Text("Describe trigger, conditions, or actions...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        FloatingActionButton(
                            onClick = {
                                val userText = inputMessage.trim()
                                if (userText.isNotEmpty()) {
                                    inputMessage = ""
                                    messages.add(ChatMessage(text = userText, isUser = true))
                                    isTyping = true
                                    
                                    // Call Gemini API with complete chat context to remember conversation
                                    scope.launch {
                                        val promptBuilder = StringBuilder()
                                        promptBuilder.append("System Instructions:\n$systemPrompt\n\n")
                                        promptBuilder.append("Here is the prior conversation history with the user:\n\n")
                                        messages.forEach {
                                            val role = if (it.isUser) "User" else "AI Assistant"
                                            promptBuilder.append("$role: ${it.text}\n")
                                        }
                                        promptBuilder.append("\nUser's latest message: $userText\n")
                                        promptBuilder.append("Please formulate your helpful response following the guidelines:")

                                        try {
                                            val responseText = withContext(Dispatchers.IO) {
                                                aiService.executeCustomPrompt(promptBuilder.toString())
                                            }
                                            
                                            // Detect if the response contains JSON
                                            val (cleanText, parsedConfig) = extractAndParseJson(responseText)
                                            
                                            isTyping = false
                                            messages.add(
                                                ChatMessage(
                                                    text = cleanText,
                                                    isUser = false,
                                                    isJson = parsedConfig != null,
                                                    parsedData = parsedConfig
                                                )
                                            )
                                        } catch (e: Exception) {
                                            isTyping = false
                                            messages.add(ChatMessage(text = "Sorry, I had an issue connecting. Let's try that again.", isUser = false))
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSave: (ParsedAgentConfig) -> Unit,
    onEdit: (ParsedAgentConfig) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (isUser) {
                // User Bubble with rich gradient background for high visual contrast
                Surface(
                    color = Color.Transparent,
                    shape = bubbleShape,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = bubbleShape
                        ),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            } else {
                // AI Bubble with neutral, highly readable material container color
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = bubbleShape,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 4.dp),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = message.text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Render Interactive Preview Card if JSON block was detected and parsed
        if (message.isJson && message.parsedData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AgentPreviewCard(
                config = message.parsedData,
                onSave = { onSave(message.parsedData) },
                onEdit = { onEdit(message.parsedData) }
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Gemini is defining your agent...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AgentPreviewCard(
    config: ParsedAgentConfig,
    onSave: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Success Verification Checkmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ready to Build!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = config.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("Trigger: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(config.triggerType, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("Action: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(config.actionType, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fine-tune", fontSize = 11.sp)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1.1f).padding(start = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & Run", fontSize = 11.sp)
                }
            }
        }
    }
}

// Extract JSON block from chat response and parse it
private fun extractAndParseJson(text: String): Pair<String, ParsedAgentConfig?> {
    try {
        val startTag = "```json"
        val endTag = "```"
        val startIndex = text.indexOf(startTag)
        if (startIndex != -1) {
            val jsonStart = startIndex + startTag.length
            val endIndex = text.indexOf(endTag, jsonStart)
            if (endIndex != -1) {
                val jsonString = text.substring(jsonStart, endIndex).trim()
                val root = JSONObject(jsonString)
                
                val name = root.optString("name", "Custom Conversational Agent")
                val triggerObj = root.optJSONObject("trigger") ?: JSONObject()
                val triggerType = triggerObj.optString("type", "SCHEDULE")
                
                val triggerParams = mutableMapOf<String, Any>()
                if (triggerType == "SCHEDULE") {
                    val sched = triggerObj.optJSONObject("schedule")
                    if (sched != null) {
                        triggerParams["hour"] = sched.optInt("hour", 12)
                        triggerParams["minute"] = sched.optInt("minute", 0)
                        val days = mutableListOf<String>()
                        val daysArr = sched.optJSONArray("daysOfWeek")
                        if (daysArr != null) {
                            for (i in 0 until daysArr.length()) {
                                days.add(daysArr.getString(i))
                            }
                        }
                        triggerParams["daysOfWeek"] = days
                    }
                } else if (triggerType == "NOTIFICATION_RECEIVED") {
                    triggerParams["keyword"] = triggerObj.optString("keyword", "")
                }

                val condObj = root.optJSONObject("condition")
                val wifiOnly = condObj?.optBoolean("wifiOnly", false) ?: false

                val actionObj = root.optJSONObject("action") ?: JSONObject()
                val actionType = actionObj.optString("type", "AI_GENERATE_AND_NOTIFY")
                val actionParams = mutableMapOf<String, Any>()
                when (actionType) {
                    "AI_GENERATE_AND_NOTIFY" -> {
                        actionParams["prompt"] = actionObj.optString("prompt", "Generate quote")
                    }
                    "SHOW_NOTIFICATION", "SHOW_TOAST", "COPY_CLIPBOARD", "SHOW_OVERLAY" -> {
                        actionParams["text"] = actionObj.optString("text", "Alert fired")
                    }
                    "WEBHOOK" -> {
                        actionParams["url"] = actionObj.optString("url", "https://hook.example.com")
                    }
                    "SEND_EMAIL" -> {
                        actionParams["recipient"] = actionObj.optString("recipient", "")
                    }
                    "OPEN_APP" -> {
                        actionParams["package"] = actionObj.optString("package", "")
                    }
                }

                // Prepare clean response text without the raw JSON block
                val cleanText = text.substring(0, startIndex).trim() + 
                    "\n\nI have successfully designed the agent parameters. See the preview below!"

                return Pair(
                    cleanText,
                    ParsedAgentConfig(
                        name = name,
                        triggerType = triggerType,
                        triggerParams = triggerParams,
                        actionType = actionType,
                        actionParams = actionParams,
                        wifiOnly = wifiOnly,
                        rawJson = jsonString
                    )
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AIChatCreatorScreen", "Error parsing agent JSON", e)
    }
    return Pair(text, null)
}

// Database persistent saver matching Section 10.2 schema payload exactly
private suspend fun saveParsedConfig(
    database: AppDatabase,
    config: ParsedAgentConfig,
    onComplete: () -> Unit
) {
    val rootObj = JSONObject()
    val id = "user_" + UUID.randomUUID().toString().take(6)
    
    rootObj.put("id", id)
    rootObj.put("name", config.name)

    val triggerObj = JSONObject()
    triggerObj.put("type", config.triggerType)
    if (config.triggerType == "SCHEDULE") {
        val scheduleObj = JSONObject()
        scheduleObj.put("hour", config.triggerParams["hour"] as? Int ?: 12)
        scheduleObj.put("minute", config.triggerParams["minute"] as? Int ?: 0)
        
        val daysArray = JSONArray()
        val days = config.triggerParams["daysOfWeek"] as? List<*>
        days?.forEach { daysArray.put(it) } ?: run {
            daysArray.put("MON").put("TUE").put("WED").put("THU").put("FRI")
        }
        scheduleObj.put("daysOfWeek", daysArray)
        
        triggerObj.put("schedule", scheduleObj)
    } else if (config.triggerType == "NOTIFICATION_RECEIVED") {
        triggerObj.put("keyword", config.triggerParams["keyword"] as? String ?: "")
    }
    rootObj.put("trigger", triggerObj)

    val condObj = JSONObject()
    condObj.put("type", "NETWORK")
    condObj.put("wifiOnly", config.wifiOnly)
    rootObj.put("condition", condObj)

    val actionObj = JSONObject()
    actionObj.put("type", config.actionType)
    when (config.actionType) {
        "AI_GENERATE_AND_NOTIFY" -> {
            actionObj.put("prompt", config.actionParams["prompt"] as? String ?: "Generate quote")
            actionObj.put("notificationChannel", "general")
        }
        "SHOW_NOTIFICATION", "SHOW_TOAST", "COPY_CLIPBOARD", "SHOW_OVERLAY" -> {
            actionObj.put("text", config.actionParams["text"] as? String ?: "Fired")
        }
        "WEBHOOK" -> {
            actionObj.put("url", config.actionParams["url"] as? String ?: "")
        }
        "SEND_EMAIL" -> {
            actionObj.put("recipient", config.actionParams["recipient"] as? String ?: "")
            actionObj.put("subject", "Automation Call triggered from TaskAI")
            actionObj.put("body", "Hello, Event inviter completed.")
        }
        "OPEN_APP" -> {
            actionObj.put("package", config.actionParams["package"] as? String ?: "")
        }
    }
    rootObj.put("action", actionObj)

    val entity = UserTaskEntity(
        id = id,
        name = config.name,
        jsonDefinition = rootObj.toString(),
        isEnabled = true
    )

    database.taskDao().insertUserTask(entity)
    onComplete()
}
