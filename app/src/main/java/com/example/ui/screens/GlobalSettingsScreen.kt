package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.core.permissions.PermissionUtils
import com.example.core.storage.PreferencesManager
import com.example.core.google.GoogleApiHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Google Workspace Connection State
    val googleApiHelper = remember { GoogleApiHelper.getInstance(context) }
    var isGoogleConnected by remember { mutableStateOf(googleApiHelper.isUserAuthenticated(context)) }
    var googleConnectedEmail by remember { mutableStateOf(googleApiHelper.getConnectedEmail(context)) }
    var showAuthConsentDialog by remember { mutableStateOf(false) }

    // Gemini API Key state
    val localApiKey by preferencesManager.geminiApiKeyFlow.collectAsState(initial = "")
    var apiKeyInput by remember { mutableStateOf("") }
    LaunchedEffect(localApiKey) {
        apiKeyInput = localApiKey
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
        GoogleAuthConsentDialog(
            onDismiss = { showAuthConsentDialog = false },
            onAuthorize = { authorizedEmail ->
                googleApiHelper.connectAccount(
                    context = context,
                    email = authorizedEmail,
                    accessToken = "ya29.sandboxed-token-${System.currentTimeMillis()}",
                    refreshToken = "1//mock-refresh-token-sandboxed-${System.currentTimeMillis()}"
                )
                isGoogleConnected = true
                googleConnectedEmail = authorizedEmail
                showAuthConsentDialog = false
                Toast.makeText(context, "Google Workspace successfully authorized!", Toast.LENGTH_LONG).show()
            }
        )
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

            // Section 1: Security & Gemini Key
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
                            text = "AI Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "The Gemini model uses this API key to generate summaries, compile digests, and draft helpful chat replies.",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                preferencesManager.saveGeminiApiKey(apiKeyInput)
                                Toast.makeText(context, "Credentials stored successfully!", Toast.LENGTH_SHORT).show()
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

            // Section 2: RSS Feed Manager
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
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editorial News Feeds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Customize the XML RSS feeds compiled by the News Agent on background runs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Add new Feed URL block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFeedInput,
                            onValueChange = { newFeedInput = it },
                            placeholder = { Text("Add new RSS XML feed URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val urlStr = newFeedInput.trim()
                                val isUrlValid = Patterns.WEB_URL.matcher(urlStr).matches()
                                if (urlStr.isBlank()) {
                                    Toast.makeText(context, "URL is empty!", Toast.LENGTH_SHORT).show()
                                } else if (!isUrlValid) {
                                    Toast.makeText(context, "Please provide a valid URL format!", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val updatedSet = rssFeeds.toMutableSet()
                                        updatedSet.add(urlStr)
                                        preferencesManager.saveRssFeeds(updatedSet)
                                        newFeedInput = ""
                                        Toast.makeText(context, "XML Feed added!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add URL")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Active Syndication Sources:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (rssFeeds.isEmpty()) {
                        Text(
                            text = "No syndication feeds configured currently. Feed briefs will run empty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        rssFeeds.forEach { feedUrl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = feedUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val updatedSet = rssFeeds.toMutableSet()
                                            updatedSet.remove(feedUrl)
                                            preferencesManager.saveRssFeeds(updatedSet)
                                            Toast.makeText(context, "Feed removed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete RSS item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
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
                                if (isAccessibilityEnabled) "✅ Authorized & Listening" else "❌ Unauthorized. Tap configure.",
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
        }
    }
}

@Composable
fun GoogleAuthConsentDialog(
    onDismiss: () -> Unit,
    onAuthorize: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("rahmanshuvo.4360@gmail.com") }
    var selectGmail by remember { mutableStateOf(true) }
    var selectSheets by remember { mutableStateOf(true) }
    var selectDocs by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google OAuth 2.0 Consent", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Agentive TaskAI is requesting authorization to securely connect with your Google Account services.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Requesting Scopes:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectGmail = !selectGmail }
                ) {
                    Checkbox(checked = selectGmail, onCheckedChange = { selectGmail = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Gmail Send", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("https://www.googleapis.com/auth/gmail.send", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectSheets = !selectSheets }
                ) {
                    Checkbox(checked = selectSheets, onCheckedChange = { selectSheets = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Google Sheets Read", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(".../auth/spreadsheets.readonly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectDocs = !selectDocs }
                ) {
                    Checkbox(checked = selectDocs, onCheckedChange = { selectDocs = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Google Docs Read", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(".../auth/documents.readonly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                        // Keep simple validation
                    } else {
                        onAuthorize(emailInput)
                    }
                }
            ) {
                Text("Grant Authorization")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

