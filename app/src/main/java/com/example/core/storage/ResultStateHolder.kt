package com.example.core.storage

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import com.example.task.AgentOutputData
import com.example.task.ArticleOutput

object ResultStateHolder {
    private val LAST_AGENT_ID_KEY = stringPreferencesKey("last_agent_id")
    private val LAST_OUTPUT_DATA_JSON_KEY = stringPreferencesKey("last_output_data_json")

    fun getLastAgentIdFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[LAST_AGENT_ID_KEY]
        }
    }

    fun getLastOutputDataFlow(context: Context): Flow<AgentOutputData?> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[LAST_OUTPUT_DATA_JSON_KEY] ?: return@map null
            deserializeOutputData(json)
        }
    }

    suspend fun saveResult(context: Context, agentId: String, outputData: AgentOutputData) {
        val json = serializeOutputData(outputData)
        context.dataStore.edit { prefs ->
            prefs[LAST_AGENT_ID_KEY] = agentId
            prefs[LAST_OUTPUT_DATA_JSON_KEY] = json
        }
    }

    fun serializeOutputData(data: AgentOutputData): String {
        val obj = JSONObject()
        when (data) {
            is AgentOutputData.GenericText -> {
                obj.put("type", "GENERIC")
                obj.put("text", data.text)
            }
            is AgentOutputData.TextNote -> {
                obj.put("type", "NOTE")
                obj.put("title", data.title)
                obj.put("content", data.content)
            }
            is AgentOutputData.Media -> {
                obj.put("type", "MEDIA")
                obj.put("title", data.title)
                obj.put("mediaUrl", data.mediaUrl)
                obj.put("isVideo", data.isVideo)
            }
            is AgentOutputData.BookContent -> {
                obj.put("type", "BOOK")
                obj.put("title", data.title)
                obj.put("author", data.author)
                obj.put("content", data.content)
                obj.put("currentPage", data.currentPage)
            }
            is AgentOutputData.ArticleList -> {
                obj.put("type", "ARTICLES")
                val arr = org.json.JSONArray()
                data.articles.forEach { art ->
                    val artObj = JSONObject().apply {
                        put("title", art.title)
                        put("summary", art.summary)
                        put("innovation", art.innovation)
                        put("link", art.link)
                    }
                    arr.put(artObj)
                }
                obj.put("articles", arr)
            }
        }
        return obj.toString()
    }

    fun deserializeOutputData(jsonStr: String): AgentOutputData? {
        return try {
            val obj = JSONObject(jsonStr)
            when (obj.optString("type")) {
                "GENERIC" -> AgentOutputData.GenericText(obj.optString("text"))
                "NOTE" -> AgentOutputData.TextNote(obj.optString("title"), obj.optString("content"))
                "MEDIA" -> AgentOutputData.Media(obj.optString("title"), obj.optString("mediaUrl"), obj.optBoolean("isVideo"))
                "BOOK" -> AgentOutputData.BookContent(obj.optString("title"), obj.optString("author"), obj.optString("content"), obj.optInt("currentPage"))
                "ARTICLES" -> {
                    val arr = obj.optJSONArray("articles") ?: return null
                    val list = mutableListOf<ArticleOutput>()
                    for (i in 0 until arr.length()) {
                        val artObj = arr.getJSONObject(i)
                        list.add(
                            ArticleOutput(
                                title = artObj.optString("title"),
                                summary = artObj.optString("summary"),
                                innovation = artObj.optString("innovation"),
                                link = artObj.optString("link")
                            )
                        )
                    }
                    AgentOutputData.ArticleList(list)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
