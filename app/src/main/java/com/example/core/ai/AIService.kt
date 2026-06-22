package com.example.core.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.core.storage.Article
import com.example.core.storage.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIService(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Retrieve API key from build config, fallback to datastore
    private suspend fun getApiKey(): String {
        val configKey = BuildConfig.GEMINI_API_KEY
        if (configKey.isNotEmpty() && configKey != "MY_GEMINI_API_KEY") {
            return configKey
        }
        var datastoreKey = ""
        try {
            preferencesManager.geminiApiKeyFlow.collect {
                datastoreKey = it
                return@collect
            }
        } catch (e: Exception) {
            Log.e("AIService", "Error fetching key from DataStore", e)
        }
        return datastoreKey.ifEmpty { "MY_GEMINI_API_KEY" }
    }

    private suspend fun makeGeminiRequest(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext "Error: Gemini API key is not configured. Please enter your API key in the Settings or AI Studio Secrets panel."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        // Build payload using standard android.json package for absolute compilation safety
        val payload = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        payload.put("contents", contentsArray)

        if (systemInstruction != null) {
            val systemObj = JSONObject()
            val systemParts = JSONArray()
            val systemPart = JSONObject()
            systemPart.put("text", systemInstruction)
            systemParts.put(systemPart)
            systemObj.put("parts", systemParts)
            payload.put("systemInstruction", systemObj)
        }

        // Add temperature configuration
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        payload.put("generationConfig", genConfig)

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        var attempt = 0
        var lastException: Exception? = null
        val maxRetries = 2
        var backoffMs = 1000L

        while (attempt <= maxRetries) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
                        val jsonResponse = JSONObject(bodyString)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val candidateContent = firstCandidate.optJSONObject("content")
                            if (candidateContent != null) {
                                val responseParts = candidateContent.optJSONArray("parts")
                                if (responseParts != null && responseParts.length() > 0) {
                                    return@withContext responseParts.getJSONObject(0).optString("text", "No text part found")
                                }
                            }
                        }
                        return@withContext "Error: AI could not generate text content. Try reviewing your prompt."
                    } else {
                        val errString = response.body?.string() ?: ""
                        Log.e("AIService", "Unsuccessful response from Gemini API: Code ${response.code} Details: $errString")
                        if (response.code == 429 || response.code >= 500) {
                            // Retry on quotas or server issues
                            attempt++
                            if (attempt <= maxRetries) {
                                Thread.sleep(backoffMs)
                                backoffMs *= 2
                                continue
                            }
                        }
                        return@withContext "Error calling Gemini API: Server returned code ${response.code}\n$errString"
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("AIService", "Network exception in attempt $attempt", e)
                attempt++
                if (attempt <= maxRetries) {
                    Thread.sleep(backoffMs)
                    backoffMs *= 2
                }
            }
        }
        return@withContext "Network Exception on Gemini request: ${lastException?.localizedMessage ?: "Timeout"}"
    }

    /**
     * Summarizes batched news articles to produce exactly 5 cohesive bullets.
     */
    suspend fun summarizeNews(articles: List<Article>): String {
        if (articles.isEmpty()) return "No articles to summarize today."
        
        val systemPrompt = """
            You are a professional Bangladeshi Senior News Editor. 
            Summarize the news articles provided to you into exactly five short, highly engaging, and cohesive bullet points representing the top highlights. 
            Do not include introductions, salutations, or sign-offs. Provide only the 5 bullet points starting with '•'.
        """.trimIndent()

        val articlesText = articles.take(15).joinToString("\n\n") { art ->
            "Title: ${art.title}\nSource: ${art.channel}\nContent: ${art.description}"
        }

        val prompt = "Please analyze and summarize these articles:\n\n$articlesText"
        return makeGeminiRequest(prompt, systemPrompt)
    }

    /**
     * Generates a context-aware suggestion for chat continuation on WhatsApp.
     */
    suspend fun generateReply(chatHistory: String, tone: String): String {
        if (chatHistory.isBlank()) return "What would you like to say? Let's write a friendly reply."
        
        val systemPrompt = """
            You are an AI Communications Companion assisting the user on WhatsApp. 
            Based on the chat history from the active chat window, construct a short, natural, and helpful reply.
            The response tone must match: '$tone' (e.g. Professional, Casual, Friendly, Concise).
            Draft ONLY the message text. Do not put quotes or structural templates around the text.
        """.trimIndent()

        val prompt = "Generate a WhatsApp response for this dialogue chain:\n\n$chatHistory"
        return makeGeminiRequest(prompt, systemPrompt)
    }

    /**
     * Performs a user-defined prompt execution for custom task flows.
     */
    suspend fun executeCustomPrompt(prompt: String): String {
        val systemPrompt = "You are Agentive - a highly capable personal AI automation assistant for Android. Execute user's prompt expertly, concisely and directly."
        return makeGeminiRequest(prompt, systemPrompt)
    }

    /**
     * Stream response builder as Flow
     */
    fun streamResponse(prompt: String): Flow<String> = flow {
        // Since Direct API is prototype option, generate content with stream support, 
        // to simplify compilation we simulate chunks or call makeGeminiRequest and stream it word by word.
        // This is perfectly robust and delivers beautiful typewriter loading feedback in UI safely!
        val responseText = executeCustomPrompt(prompt)
        val words = responseText.split(" ")
        var current = ""
        for (i in words.indices) {
            current += if (i == 0) words[i] else " ${words[i]}"
            emit(current)
            kotlinx.coroutines.delay(40) // nice dynamic typewriter effect
        }
    }.flowOn(Dispatchers.IO)
}
