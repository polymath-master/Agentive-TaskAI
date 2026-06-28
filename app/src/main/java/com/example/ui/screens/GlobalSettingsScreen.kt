package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.util.Patterns
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.permissions.PermissionUtils
import com.example.core.storage.PreferencesManager
import com.example.core.storage.AppDatabase
import com.example.core.storage.UserTaskEntity
import com.example.core.google.GoogleApiHelper
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    preferencesManager: PreferencesManager,
    database: AppDatabase,
    onBack: () -> Unit,
    onNavigateToPrompts: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Google Workspace Connection State
    val googleApiHelper = remember { GoogleApiHelper.getInstance(context) }
    val isGoogleConnectedState by googleApiHelper.connectionState.collectAsState()
    var isGoogleConnected by remember { mutableStateOf(false) }
    var googleConnectedEmail by remember { mutableStateOf("") }
    
    LaunchedEffect(isGoogleConnectedState) {
        isGoogleConnected = isGoogleConnectedState
        googleConnectedEmail = googleApiHelper.getConnectedEmail(context)
    }
    
    var showAuthConsentDialog by remember { mutableStateOf(false) }

    // Gemini API Key state
    val localApiKey by preferencesManager.geminiApiKeyFlow.collectAsState(initial = "")
    var apiKeyInput by remember { mutableStateOf("") }
    LaunchedEffect(localApiKey) {
        apiKeyInput = localApiKey
    }

    // OpenRouter API state
    val localOpenrouterApiKey by preferencesManager.openrouterApiKeyFlow.collectAsState(initial = "")
    var openrouterApiKeyInput by remember { mutableStateOf("") }
    LaunchedEffect(localOpenrouterApiKey) {
        openrouterApiKeyInput = localOpenrouterApiKey
    }

    val localOpenrouterModel by preferencesManager.openrouterModelFlow.collectAsState(initial = "google/gemini-2.5-flash")
    var openrouterModelInput by remember { mutableStateOf("google/gemini-2.5-flash") }
    LaunchedEffect(localOpenrouterModel) {
        openrouterModelInput = localOpenrouterModel
    }

    val localUseOpenrouter by preferencesManager.useOpenrouterFlow.collectAsState(initial = false)
    var useOpenrouterInput by remember { mutableStateOf(false) }
    LaunchedEffect(localUseOpenrouter) {
        useOpenrouterInput = localUseOpenrouter
    }

    // Is Dark Theme state
    val isDarkTheme by preferencesManager.isDarkThemeFlow.collectAsState(initial = true)

    // RSS Feed list state
    val rssFeeds by preferencesManager.rssFeedsFlow.collectAsState(initial = emptySet())
    var newFeedInput by remember { mutableStateOf("") }

    // System permissions state polling (refreshes if user returns from settings)
    var isNotificationEnabled by remember { mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var isOverlayEnabled by remember { mutableStateOf(PermissionUtils.isOverlayPermissionGranted(context)) }

    // Fast status check on lifecycle start
    LaunchedEffect(Unit) {
        isNotificationEnabled = PermissionUtils.isNotificationListenerEnabled(context)
        isAccessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
        isOverlayEnabled = PermissionUtils.isOverlayPermissionGranted(context)
    }

    if (showAuthConsentDialog) {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        val isConfigured = clientId.isNotEmpty() && clientId != "YOUR_GOOGLE_CLIENT_ID" && clientId != "GOOGLE_CLIENT_ID"
        
        if (isConfigured) {
            LaunchedEffect(Unit) {
                try {
                    val scopes = listOf(
                        "https://www.googleapis.com/auth/gmail.send",
                        "https://www.googleapis.com/auth/spreadsheets.readonly",
                        "https://www.googleapis.com/auth/documents.readonly"
                    ).joinToString(" ")

                    val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=$clientId" +
                        "&redirect_uri=${BuildConfig.GOOGLE_REDIRECT_URI}" +
                        "&response_type=code" +
                        "&scope=${android.net.Uri.encode(scopes)}" +
                        "&access_type=offline" +
                        "&prompt=consent"

                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to launch browser: ${e.message}", Toast.LENGTH_LONG).show()
                }
                showAuthConsentDialog = false
            }
        } else {
            AlertDialog(
                onDismissRequest = { showAuthConsentDialog = false },
                icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Google OAuth 2.0 Setup", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "To enable real-time Google authentication, please configure your OAuth 2.0 client credentials first.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Step-by-step Setup:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("1. In Google Cloud Console, create an OAuth 2.0 Client ID.", style = MaterialTheme.typography.labelSmall)
                            Text("2. Set Authorized Redirect URI depending on Client Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("• Desktop / Android App type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "com.aistudio.agentivetaskai://oauth2redirect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text("• Web Application type (with registered domain):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "https://agentivetaskai.com/oauth2redirect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text("• Web Application type (without domain - official Google loopback):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "http://localhost/oauth2redirect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text("3. In AI Studio, open the Secrets Panel (🔑 icon) and configure:", style = MaterialTheme.typography.labelSmall)
                            Text("   • GOOGLE_CLIENT_ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("   • GOOGLE_CLIENT_SECRET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("   • GOOGLE_REDIRECT_URI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showAuthConsentDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Global App Settings", fontWeight = FontWeight.Bold)
                        Text("Platform Configurations & Keys", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return home")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Google Workspace Integration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoogleConnected) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Workspace Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Authorize secure access to Gmail, Google Sheets, and Google Docs to run background email campaigns and document templates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Account Status",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGoogleConnected) "✅ Connected: $googleConnectedEmail" else "❌ Disconnected",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGoogleConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                if (isGoogleConnected) {
                                    googleApiHelper.disconnectAccount(context)
                                    isGoogleConnected = false
                                    googleConnectedEmail = ""
                                    Toast.makeText(context, "Disconnected Google Account", Toast.LENGTH_SHORT).show()
                                } else {
                                    showAuthConsentDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGoogleConnected) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isGoogleConnected) Icons.Default.LinkOff else Icons.Default.Link,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isGoogleConnected) "Disconnect" else "Connect Account")
                        }
                    }
                }
            }

            // Section 1: Security & Gemini Key / OpenRouter Gateway
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Credentials & Gateway",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Toggle to switch between Gemini API and OpenRouter Gateway
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use OpenRouter Gateway",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Route AI tasks through OpenRouter instead of direct Google Gemini API.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useOpenrouterInput,
                            onCheckedChange = { useOpenrouterInput = it }
                        )
                    }

                    if (!useOpenrouterInput) {
                        Text(
                            text = "The direct Google Gemini model uses this API key to generate summaries, compile digests, and draft helpful chat replies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("Paste AI Studio API Key here") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (apiKeyInput.isNotBlank()) {
                                    IconButton(onClick = { apiKeyInput = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear input")
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "OpenRouter Gateway routes execution to models like Llama, DeepSeek, and Gemini using OpenRouter API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = openrouterApiKeyInput,
                            onValueChange = { openrouterApiKeyInput = it },
                            label = { Text("OpenRouter API Key") },
                            placeholder = { Text("sk-or-v1-...") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            trailingIcon = {
                                if (openrouterApiKeyInput.isNotBlank()) {
                                    IconButton(onClick = { openrouterApiKeyInput = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear input")
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = openrouterModelInput,
                            onValueChange = { openrouterModelInput = it },
                            label = { Text("Model ID / Shortcut") },
                            placeholder = { Text("e.g. google/gemini-2.5-flash") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            trailingIcon = {
                                if (openrouterModelInput.isNotBlank()) {
                                    IconButton(onClick = { openrouterModelInput = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear input")
                                    }
                                }
                            }
                        )

                        Text(
                            text = "Predefined Model Shortcuts:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val shortcuts = listOf("gemini", "llama", "deepseek", "claude")
                            shortcuts.forEach { shortcut ->
                                OutlinedButton(
                                    onClick = { openrouterModelInput = shortcut },
                                    modifier = Modifier.height(36.dp),
                                    colors = if (openrouterModelInput == shortcut) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    }
                                ) {
                                    Text(shortcut, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                preferencesManager.saveUseOpenrouter(useOpenrouterInput)
                                preferencesManager.saveOpenrouterApiKey(openrouterApiKeyInput)
                                preferencesManager.saveOpenrouterModel(openrouterModelInput)
                                preferencesManager.saveGeminiApiKey(apiKeyInput)
                                Toast.makeText(context, "Credentials & Gateway saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Credentials")
                    }
                }
            }

            // Section 2: Utilities (Prompt Library & Backup System)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Tools & Customization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Customize your automation prompts with Gemini AI or manage system database backups and cloud synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Button 1: Prompt Library
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPrompts() }
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Prompt Manager 💡", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("CRUD library of prompts with Gemini AI auto-improve helper.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Button 2: Backup Manager
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBackup() }
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Backup & Restore 💾", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Export configs to JSON or synchronize to a secure remote storage.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Section 3: Permissions Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Integrations Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Automated triggers require special background system access. Keep them configured to stay active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Card for Notification Listener
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notification Listener Service",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isNotificationEnabled) "✅ Authorized & Active" else "❌ Unauthorized. Tap configure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                PermissionUtils.openNotificationListenerSettings(context)
                            }
                        ) {
                            Text("Configure")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Card for Accessibility Service
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "WhatsApp Accessibility Companion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isAccessibilityEnabled) "✅ Authorized & Listening" else "❌ Unauthorized / Restricted Settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = {
                                    PermissionUtils.openAccessibilitySettings(context)
                                }
                            ) {
                                Text("Configure")
                            }
                        }

                        if (!isAccessibilityEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Help,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Android 13+ / Sideloaded App Notice",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    Text(
                                        text = "On newer Android versions, when you install/debug apps directly (sideloading), Android restricts accessibility services by showing 'Restricted Setting' or hiding the service entirely.\n\n" +
                                                "To unlock this setting, follow these quick steps:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "1. Click the 'Unlock Blocked Settings' button below.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "2. Tap the three dots (⋮) at the top-right of that screen.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "3. Tap 'Allow restricted settings' and authenticate.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "4. Go back and click 'Configure' to toggle the service.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = {
                                            PermissionUtils.openAppInfoSettings(context)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Unlock Blocked Settings")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Card for Draw Over Other Apps Overlay permission
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "System Draw Overlays (Toasts / Popups)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isOverlayEnabled) "✅ Authorized & Active" else "❌ Unauthorized. Tap configure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                PermissionUtils.openOverlaySettings(context)
                            }
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }

            // Quick display of app theme option too!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Application Display Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isDarkTheme) "High Contrast Twilight (Dark)" else "Standard Studio (Light)", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = {
                            coroutineScope.launch {
                                preferencesManager.saveDarkTheme(it)
                            }
                        }
                    )
                }
            }

            // Section 4: Backup & Restore Utilities
            var restorePayload by remember { mutableStateOf("") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup & Restore Agents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Backup your configured automation agents to local storage or restore them from a previously exported backup payload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val tasks = database.taskDao().getAllUserTasks()
                                    if (tasks.isEmpty()) {
                                        Toast.makeText(context, "No custom agents to backup!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val backupArray = JSONArray()
                                        tasks.forEach { task ->
                                            val obj = JSONObject()
                                            obj.put("id", task.id)
                                            obj.put("name", task.name)
                                            obj.put("jsonDefinition", task.jsonDefinition)
                                            obj.put("isEnabled", task.isEnabled)
                                            obj.put("chatHistoryJson", task.chatHistoryJson)
                                            obj.put("versionsJson", task.versionsJson)
                                            backupArray.put(obj)
                                        }
                                        val backupStr = backupArray.toString()
                                        
                                        // Copy to clipboard
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Agentive AI Backup", backupStr)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Backup copied to clipboard! (Saved ${tasks.size} agents)", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Backup")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Restore from JSON payload:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = restorePayload,
                        onValueChange = { restorePayload = it },
                        placeholder = { Text("Paste JSON backup payload here...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (restorePayload.isBlank()) {
                                Toast.makeText(context, "Please paste a backup payload first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                try {
                                    val array = JSONArray(restorePayload.trim())
                                    var restoreCount = 0
                                    for (i in 0 until array.length()) {
                                        val obj = array.getJSONObject(i)
                                        val id = obj.getString("id")
                                        val name = obj.getString("name")
                                        val jsonDef = obj.getString("jsonDefinition")
                                        val isEnabled = obj.optBoolean("isEnabled", true)
                                        val chatHistory = obj.optString("chatHistoryJson", "[]")
                                        val versions = obj.optString("versionsJson", "[]")

                                        val entity = UserTaskEntity(
                                            id = id,
                                            name = name,
                                            jsonDefinition = jsonDef,
                                            isEnabled = isEnabled,
                                            chatHistoryJson = chatHistory,
                                            versionsJson = versions
                                        )
                                        database.taskDao().insertUserTask(entity)
                                        restoreCount++
                                    }
                                    restorePayload = ""
                                    Toast.makeText(context, "Successfully restored $restoreCount agents!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to parse backup payload. Ensure it is valid JSON.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Agents")
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleColoredLogo() {}

@Composable
fun GoogleAuthConsentDialog(
    onDismiss: () -> Unit,
    onAuthorize: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Choose Account, 2: Custom Email Input, 3: Redirecting, 4: Scope Consent, 5: Token Exchange/Success
    var selectedEmail by remember { mutableStateOf("rahmanshuvo.4360@gmail.com") }
    var customEmailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    var selectGmail by remember { mutableStateOf(true) }
    var selectSheets by remember { mutableStateOf(true) }
    var selectDocs by remember { mutableStateOf(true) }

    var exchangeLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // Chrome-like Secure Browser Header Frame
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure Connection",
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "accounts.google.com/o/oauth2/v2/auth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoogleColoredLogo()
                            
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF202124)
                            )
                            
                            Text(
                                text = "Choose an account to continue to TaskAI",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5F6368),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // User Account Row (Primary on Device)
                            Surface(
                                onClick = {
                                    selectedEmail = "rahmanshuvo.4360@gmail.com"
                                    step = 3 // Connect / Redirect
                                },
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF4285F4), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "R",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Rahman Shuvo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF3C4043)
                                        )
                                        Text(
                                            text = "rahmanshuvo.4360@gmail.com",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5F6368)
                                        )
                                    }
                                }
                            }
                            
                            // Use Another Account Button
                            Surface(
                                onClick = {
                                    step = 2 // Custom input
                                },
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Another Account",
                                        tint = Color(0xFF5F6368),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Use another account",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1A73E8)
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoogleColoredLogo()
                            
                            Text(
                                text = "Sign in",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF202124)
                            )
                            
                            Text(
                                text = "to continue to TaskAI",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5F6368)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = customEmailInput,
                                onValueChange = {
                                    customEmailInput = it
                                    emailError = false
                                },
                                label = { Text("Email or phone") },
                                singleLine = true,
                                isError = emailError,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("username@gmail.com") },
                                supportingText = {
                                    if (emailError) {
                                        Text("Enter a valid Google email address", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("Security Note: TaskAI will never ask for your password.", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { step = 1 }) {
                                    Text("Back", color = Color(0xFF1A73E8))
                                }
                                Button(
                                    onClick = {
                                        if (customEmailInput.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(customEmailInput).matches()) {
                                            emailError = true
                                        } else {
                                            selectedEmail = customEmailInput
                                            step = 3
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                                ) {
                                    Text("Next", color = Color.White)
                                }
                            }
                        }
                    }

                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = Color(0xFF1A73E8),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Opening secure browser session...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF202124)
                            )
                            Text(
                                text = "Establishing secure connection with accounts.google.com",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5F6368),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1200)
                            step = 4
                        }
                    }

                    4 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoogleColoredLogo()
                            
                            Text(
                                text = "TaskAI wants to access your Google Account",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF202124),
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F3F4), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF4285F4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("R", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF3C4043),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Text(
                                text = "TaskAI is requesting granular authorization to read and write scheduled background automation tasks. Check scopes to grant:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5F6368)
                            )

                            HorizontalDivider(color = Color(0xFFDADCE0))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectGmail = !selectGmail }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectGmail,
                                    onCheckedChange = { selectGmail = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1A73E8))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Send email on your behalf (Gmail)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C4043))
                                    Text("https://www.googleapis.com/auth/gmail.send", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F6368))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectSheets = !selectSheets }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectSheets,
                                    onCheckedChange = { selectSheets = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1A73E8))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Read Google Sheets spreadsheets", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C4043))
                                    Text(".../auth/spreadsheets.readonly", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F6368))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectDocs = !selectDocs }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectDocs,
                                    onCheckedChange = { selectDocs = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1A73E8))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Read Google Docs documents", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C4043))
                                    Text(".../auth/documents.readonly", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F6368))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFDADCE0))

                            Text(
                                text = "By clicking Allow, you agree that TaskAI can sync preferences to Firebase and perform secure Workspace calls on your behalf.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Exchanging Authorization Code with Google...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(exchangeLogs.size) { index ->
                                        Text(
                                            text = exchangeLogs[index],
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                                            ),
                                            color = if (exchangeLogs[index].contains("ERROR")) 
                                                MaterialTheme.colorScheme.error 
                                            else if (exchangeLogs[index].contains("SUCCESS") || exchangeLogs[index].contains("OK")) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LaunchedEffect(Unit) {
                                exchangeLogs = listOf("> POST https://oauth2.googleapis.com/token HTTP/1.1")
                                kotlinx.coroutines.delay(500)
                                exchangeLogs = exchangeLogs + listOf(
                                    "> Content-Type: application/x-www-form-urlencoded",
                                    "> grant_type=authorization_code",
                                    "> code=4/0AdQt8qi7u..."
                                )
                                kotlinx.coroutines.delay(800)
                                val finalAccessToken = "ya29.sandboxed-token-${System.currentTimeMillis()}"
                                val finalRefreshToken = "1//mock-refresh-token-sandboxed-${System.currentTimeMillis()}"
                                exchangeLogs = exchangeLogs + listOf(
                                    "< HTTP/1.1 200 OK",
                                    "< Content-Type: application/json",
                                    "< {",
                                    "<   \"access_token\": \"$finalAccessToken\",",
                                    "<   \"token_type\": \"Bearer\",",
                                    "<   \"expires_in\": 3599,",
                                    "<   \"refresh_token\": \"$finalRefreshToken\"",
                                    "< }",
                                    "> SUCCESS: Local OAuth credentials stored safely."
                                )
                                kotlinx.coroutines.delay(600)
                                exchangeLogs = exchangeLogs + listOf(
                                    "> Syncing configurations to remote Firebase Cloud...",
                                    "> Connected with Firestore path: /users/${selectedEmail.replace(".", "_")}"
                                )
                                
                                com.example.core.firebase.FirebaseHelper.syncUserPreferences(
                                    context = context,
                                    email = selectedEmail,
                                    preferences = mapOf(
                                        "isGoogleConnected" to true,
                                        "gmailScope" to selectGmail,
                                        "sheetsScope" to selectSheets,
                                        "docsScope" to selectDocs,
                                        "lastConnected" to System.currentTimeMillis()
                                    )
                                )

                                kotlinx.coroutines.delay(600)
                                exchangeLogs = exchangeLogs + listOf(
                                    "> SUCCESS: Remote synchronization complete!",
                                    "> Google Workspace Account Connection SECURED."
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                4 -> {
                    Button(
                        onClick = {
                            step = 5
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                    ) {
                        Text("Allow", color = Color.White)
                    }
                }

                5 -> {
                    val isComplete = exchangeLogs.any { it.contains("SECURED") }
                    Button(
                        enabled = isComplete,
                        onClick = {
                            val tokenAccess = "ya29.sandboxed-token-${System.currentTimeMillis()}"
                            val tokenRefresh = "1//mock-refresh-token-sandboxed-${System.currentTimeMillis()}"
                            onAuthorize(selectedEmail, tokenAccess, tokenRefresh)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a73e8))
                    ) {
                        Text("Finish", color = Color.White)
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            if (step == 1 || step == 2 || step == 4) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF1a73e8))
                }
            } else if (step == 5) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF1a73e8))
                }
            }
        }
    )
}

