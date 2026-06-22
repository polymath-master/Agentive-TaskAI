package com.example.core.permissions

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.example.task.callreminder.CallReminderTask

class AgentNotificationListenerService : NotificationListenerService() {

    companion object {
        private val handledNotificationKeys = java.util.concurrent.ConcurrentLinkedQueue<String>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val category = notification.category
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Check if it's a missed call
        val isMissedCallCategory = category == Notification.CATEGORY_MISSED_CALL
        val isMissedCallText = title.contains("missed", ignoreCase = true) || 
                              title.contains("call back", ignoreCase = true) ||
                              text.contains("missed call", ignoreCase = true)

        val isDialerApp = packageName.contains("phone", ignoreCase = true) || 
                          packageName.contains("dialer", ignoreCase = true) || 
                          packageName.contains("telephony", ignoreCase = true)

        val meetsMissedCallCriteria = isMissedCallCategory || (isDialerApp && isMissedCallText)

        if (!meetsMissedCallCriteria) return

        // Deduplicate using notification unique key
        val key = sbn.key ?: "${packageName}_${notification.`when`}"
        if (handledNotificationKeys.contains(key)) {
            Log.d("NotificationListener", "Skipping duplicated missed call notification: $key")
            return
        }

        handledNotificationKeys.add(key)
        if (handledNotificationKeys.size > 20) {
            handledNotificationKeys.poll()
        }

        val contactName = if (title.isNotBlank()) title else if (text.isNotBlank()) text else "Unknown Caller"

        Log.d("NotificationListener", "Validated Missed Call from contact: $contactName (Package: $packageName, Key: $key)")

        try {
            val callReminderTask = CallReminderTask(applicationContext)
            callReminderTask.onMissedCallReceived(contactName)
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error updating reminder prompt", e)
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification?) {
        super.onNotificationRemoved(statusBarNotification)
    }
}
