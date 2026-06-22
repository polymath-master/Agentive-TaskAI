package com.example.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.permissions.PermissionUtils
import com.example.core.permissions.SpecialPermission

@Composable
fun PermissionGate(
    permissions: List<SpecialPermission>,
    onDismiss: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    val ungranted = permissions.filter { !PermissionUtils.isSpecialPermissionGranted(context, it) }

    if (ungranted.isEmpty()) {
        onPermissionGranted()
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security permissions required",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Special Access Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This Assistant Agent requires special system authorization to function correctly. Your data is processed entirely on-device and is never shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            ungranted.forEach { permission ->
                PermissionExplanationCard(permission)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Skip for Now")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val p = ungranted.first()
                        when (p) {
                            SpecialPermission.NOTIFICATION_LISTENER -> {
                                PermissionUtils.openNotificationListenerSettings(context)
                            }
                            SpecialPermission.ACCESSIBILITY_SERVICE -> {
                                PermissionUtils.openAccessibilitySettings(context)
                            }
                            else -> {}
                        }
                    }
                ) {
                    Text("Grant Access")
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationCard(permission: SpecialPermission) {
    val (title, explanation) = when (permission) {
        SpecialPermission.NOTIFICATION_LISTENER -> {
            Pair(
                "Notification Listener Service",
                "Necessary to intercept incoming missed calls in real-time so we can trigger callback reminders."
            )
        }
        SpecialPermission.ACCESSIBILITY_SERVICE -> {
            Pair(
                "Accessibility Service",
                "Necessary to read active chat transcripts inside WhatsApp UI and draft appropriate, context-aware smart replies."
            )
        }
        SpecialPermission.GMAIL_OAUTH -> {
            Pair(
                "Workspace Gmail Access",
                "Requires secure GMail sending scope to dispatch invites automatically."
            )
        }
        SpecialPermission.GOOGLE_SHEETS_OAUTH -> {
            Pair(
                "Workspace Sheets Access",
                "Requires reading contacts arrays from secure Sheets spreadsheet."
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(text = explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
