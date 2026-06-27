package com.example.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val summary: String,
    val pubDate: Long,
    val channel: String
)

@Entity(tableName = "user_tasks")
data class UserTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jsonDefinition: String,
    val isEnabled: Boolean = true,
    val chatHistoryJson: String = "[]",
    val versionsJson: String = "[]"
)

@Entity(tableName = "task_history")
data class TaskHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val taskName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "ERROR", "CANCELLED"
    val message: String
)

@Entity(tableName = "email_recipients")
data class EmailRecipient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val name: String,
    val status: String = "PENDING", // "PENDING", "SENT", "FAILED"
    val errorMsg: String? = null
)
