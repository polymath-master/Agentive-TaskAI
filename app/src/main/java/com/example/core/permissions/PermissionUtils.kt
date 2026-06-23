package com.example.core.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object PermissionUtils {

    /**
     * Checks if the NotificationListenerService is authorized for our application package.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = context.packageName + "/" + AgentNotificationListenerService::class.java.name
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                if (cn.equals(name, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if the active AccessibilityService is enabled.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = context.packageName + "/" + AgentAccessibilityService::class.java.name
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(service, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Launches the system settings panel to grant Notification Listener permission.
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Launches the system settings panel to grant Accessibility service permission.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if the app can draw custom overlays on other applications.
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Launches the system settings panel for Draw Over Other Apps permission.
     */
    fun openOverlaySettings(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Checks all required special permissions for a task block.
     */
    fun isSpecialPermissionGranted(context: Context, permission: SpecialPermission): Boolean {
        return when (permission) {
            SpecialPermission.NOTIFICATION_LISTENER -> isNotificationListenerEnabled(context)
            SpecialPermission.ACCESSIBILITY_SERVICE -> isAccessibilityServiceEnabled(context)
            // Simulations are initialized/granted on true
            SpecialPermission.GMAIL_OAUTH -> true
            SpecialPermission.GOOGLE_SHEETS_OAUTH -> true
        }
    }
}
