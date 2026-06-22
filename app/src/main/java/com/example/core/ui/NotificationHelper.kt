package com.example.core.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_NEWS = "news_summary"
        const val CHANNEL_REMINDER = "call_reminder"
        const val CHANNEL_PROGRESS = "task_progress"
        const val CHANNEL_GENERAL = "general"

        const val NOTIFICATION_ID_NEWS = 1001
        const val NOTIFICATION_ID_REMINDER = 1002
        const val NOTIFICATION_ID_PROGRESS = 1003
        const val NOTIFICATION_ID_GENERAL = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newsChannel = NotificationChannel(
                CHANNEL_NEWS,
                "News Summaries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily AI-generated news briefs and summaries."
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Call Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Missed call alerts and task reminders."
                enableVibration(true)
            }

            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Task Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status feeds for active background agents."
                enableVibration(false)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "System notifications and customizable triggers."
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(newsChannel)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }

    fun showNewsNotification(title: String, summaryText: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "NEWS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEWS)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText("Bangladesh Daily News is ready.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_NEWS, notification)
        } catch (e: SecurityException) {
            // Log missing runtime notification permissions
        }
    }

    fun showMissedCallReminderPrompt(contactName: String) {
        val remindIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("ACTION_TRIGGER", "REMIND_30_MIN")
            putExtra("PARAM_NAME", contactName)
        }
        val remindPendingIntent = PendingIntent.getActivity(
            context, 1, remindIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val customIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("ACTION_TRIGGER", "REMIND_CUSTOM")
            putExtra("PARAM_NAME", contactName)
        }
        val customPendingIntent = PendingIntent.getActivity(
            context, 2, customIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle("Missed Call from $contactName")
            .setContentText("Would you like to schedule an automated callback reminder?")
            .addAction(android.R.drawable.ic_menu_agenda, "Remind in 30 mins", remindPendingIntent)
            .addAction(android.R.drawable.ic_menu_set_as, "Custom delay...", customPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_REMINDER, notification)
        } catch (e: SecurityException) {
            // Log
        }
    }

    fun showSimpleNotification(channelId: String, title: String, content: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_GENERAL, notification)
        } catch (e: SecurityException) {
            // Log
        }
    }

    fun getOngoingProgressNotificationBuilder(title: String, initialProgress: Int, maxProgress: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(title)
            .setContentText("Sending email $initialProgress of $maxProgress...")
            .setProgress(maxProgress, initialProgress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    fun updateProgressNotification(id: Int, builder: NotificationCompat.Builder, progress: Int, max: Int) {
        builder.setProgress(max, progress, false)
            .setContentText("Sending email $progress of $max...")
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Log
        }
    }

    fun cancelNotification(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
