package com.example.core.permissions

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.example.task.callreminder.CallReminderTask

class AgentNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Support standard CATEGORY_MISSED_CALL and popular telephone dialers
        val category = notification.category
        val isMissedCall = category == Notification.CATEGORY_MISSED_CALL || 
                           packageName.contains("phone") || 
                           packageName.contains("dialer") || 
                           extras.getString(Notification.EXTRA_TITLE)?.contains("missed", ignoreCase = true) == true

        if (isMissedCall) {
            val contactName = extras.getString(Notification.EXTRA_TITLE) ?: 
                              extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: 
                              "Unsaved Contact"
            
            Log.d("NotificationListener", "Intercepted Missed Call from contact: $contactName (Package: $packageName)")
            
            // Trigger the CallReminderTask callback sequence
            try {
                val callReminderTask = CallReminderTask(applicationContext)
                callReminderTask.onMissedCallReceived(contactName)
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error updating reminder prompt", e)
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification?) {
        super.onNotificationRemoved(statusBarNotification)
    }
}
