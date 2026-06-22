package com.example.task

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PhoneCallback
import com.example.task.callreminder.CallReminderTask
import com.example.task.massemail.MassEmailTask
import com.example.task.news.NewsTask
import com.example.task.whatsapp.WhatsAppContinuationTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TaskRegistry(private val context: Context) {

    private val newsTask = NewsTask(context)
    private val callReminderTask = CallReminderTask(context)
    private val whatsAppContinuationTask = WhatsAppContinuationTask(context)
    private val massEmailTask = MassEmailTask(context)

    // Set of built-in tasks
    val builtInTasks = listOf(
        newsTask,
        callReminderTask,
        whatsAppContinuationTask,
        massEmailTask
    )

    fun getTaskById(taskId: String): AgentTask? {
        return builtInTasks.find { it.metadata.id == taskId }
    }
}
