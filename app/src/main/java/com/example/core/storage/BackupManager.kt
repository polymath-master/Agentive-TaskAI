package com.example.core.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Export User-Defined Agents
        val userTasks = db.taskDao().getAllUserTasks()
        val tasksArr = JSONArray()
        userTasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("name", task.name)
                put("jsonDefinition", task.jsonDefinition)
                put("isEnabled", task.isEnabled)
                put("chatHistoryJson", task.chatHistoryJson)
                put("versionsJson", task.versionsJson)
            }
            tasksArr.put(obj)
        }
        root.put("user_tasks", tasksArr)

        // 2. Export Prompts
        val prompts = try {
            db.taskDao().getAllPromptsFlow().first()
        } catch (e: Exception) {
            emptyList()
        }
        val promptsArr = JSONArray()
        prompts.forEach { prompt ->
            val obj = JSONObject().apply {
                put("id", prompt.id)
                put("name", prompt.name)
                put("text", prompt.text)
                put("labels", prompt.labels)
                put("createdAt", prompt.createdAt)
                put("updatedAt", prompt.updatedAt)
            }
            promptsArr.put(obj)
        }
        root.put("prompt_templates", promptsArr)

        return@withContext root.toString(4)
    }

    suspend fun restoreBackup(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)

            // 1. Restore Prompts
            if (root.has("prompt_templates")) {
                val promptsArr = root.getJSONArray("prompt_templates")
                for (i in 0 until promptsArr.length()) {
                    val obj = promptsArr.getJSONObject(i)
                    db.taskDao().insertPrompt(
                        PromptTemplate(
                            name = obj.optString("name", "Restored Prompt"),
                            text = obj.optString("text", ""),
                            labels = obj.optString("labels", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 2. Restore User-Defined Tasks
            if (root.has("user_tasks")) {
                val tasksArr = root.getJSONArray("user_tasks")
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.getJSONObject(i)
                    db.taskDao().insertUserTask(
                        UserTaskEntity(
                            id = obj.optString("id", "user_${System.currentTimeMillis()}"),
                            name = obj.optString("name", "Restored Agent"),
                            jsonDefinition = obj.optString("jsonDefinition", "{}"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            chatHistoryJson = obj.optString("chatHistoryJson", "[]"),
                            versionsJson = obj.optString("versionsJson", "[]")
                        )
                    )
                }
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
