package com.example.core.permissions

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import com.example.task.callreminder.CallReminderTask

class AgentNotificationListenerService : NotificationListenerService() {

    companion object {
        private val handledNotificationKeys = java.util.concurrent.ConcurrentLinkedQueue<String>()
        private var lastTriggerTime: Long = 0
        private var lastContactName: String = ""
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

        // Filter missed call notifications strictly
        val isMissedCallCategory = category == Notification.CATEGORY_MISSED_CALL
        
        val containsMissedCallPhrases = title.contains("missed call", ignoreCase = true) || 
                                       title.contains("missed video call", ignoreCase = true) ||
                                       title.contains("missed voice call", ignoreCase = true) ||
                                       text.contains("missed call", ignoreCase = true) ||
                                       text.contains("missed video call", ignoreCase = true) ||
                                       text.contains("missed voice call", ignoreCase = true)

        val isDialerApp = packageName.contains("phone", ignoreCase = true) || 
                          packageName.contains("dialer", ignoreCase = true) || 
                          packageName.contains("telephony", ignoreCase = true) ||
                          packageName.contains("contacts", ignoreCase = true) ||
                          packageName.contains("android.server.telecom", ignoreCase = true)

        // Only process dialer system apps OR explicit missed call category with text phrases
        val meetsMissedCallCriteria = isMissedCallCategory || (isDialerApp && containsMissedCallPhrases) || containsMissedCallPhrases

        if (!meetsMissedCallCriteria) return

        // Extract contact name securely
        val contactName = if (title.isNotBlank() && !title.contains("missed", ignoreCase = true)) {
            title
        } else if (text.isNotBlank() && !text.contains("missed", ignoreCase = true)) {
            text
        } else {
            "Unknown Caller"
        }

        // Time-based deduplication to prevent floods (e.g. multiple callbacks in 1500 milliseconds)
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 1500L && contactName == lastContactName) {
            Log.d("NotificationListener", "Prevented flooding: duplicate missed call callback from $contactName within 1500ms")
            return
        }

        // Deduplicate using notification key
        val key = sbn.key ?: "${packageName}_${notification.`when`}"
        if (handledNotificationKeys.contains(key)) {
            Log.d("NotificationListener", "Skipping duplicated notification key: $key")
            return
        }

        handledNotificationKeys.add(key)
        if (handledNotificationKeys.size > 20) {
            handledNotificationKeys.poll()
        }

        lastTriggerTime = now
        lastContactName = contactName

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
