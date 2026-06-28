package com.example.task

import android.content.Context
import com.example.task.trending.TrendingArticlesTask
import com.example.task.bookself.BookselfTask

class TaskRegistry(private val context: Context) {

    private val trendingArticlesTask = TrendingArticlesTask(context)
    private val bookselfTask = BookselfTask(context)

    // Set of built-in tasks for Agentive-TaskAI v4.0
    val builtInTasks = listOf(
        trendingArticlesTask,
        bookselfTask
    )

    fun getTaskById(taskId: String): AgentTask? {
        return builtInTasks.find { it.metadata.id == taskId }
    }
}
