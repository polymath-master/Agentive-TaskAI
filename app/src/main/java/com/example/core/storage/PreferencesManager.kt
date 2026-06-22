package com.example.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agentive_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val GEMINI_API_KEY_KEY = stringPreferencesKey("gemini_api_key")
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        val NEWS_SCHEDULE_TIME_KEY = stringPreferencesKey("news_schedule_time")
        val REMINDER_DELAY_MINUTES_KEY = intPreferencesKey("reminder_delay_minutes")
        val WHATSAPP_RESPONSE_TONE_KEY = stringPreferencesKey("whatsapp_response_tone")
        val GMAIL_USER_EMAIL_KEY = stringPreferencesKey("gmail_user_email")
        
        // Fast key-value for enabling / disabling tasks
        fun taskEnabledKey(taskId: String) = booleanPreferencesKey("task_enabled_$taskId")
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_API_KEY_KEY] ?: ""
    }

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_THEME_KEY] ?: true // default to dark theme for beautiful contrast
    }

    val newsScheduleTimeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[NEWS_SCHEDULE_TIME_KEY] ?: "11:00"
    }

    val reminderDelayMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[REMINDER_DELAY_MINUTES_KEY] ?: 30
    }

    val whatsappResponseToneFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WHATSAPP_RESPONSE_TONE_KEY] ?: "Professional"
    }

    val gmailUserEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GMAIL_USER_EMAIL_KEY] ?: "user@gmail.com"
    }

    fun isTaskEnabledFlow(taskId: String): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[taskEnabledKey(taskId)] ?: true // default enabled
        }
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[GEMINI_API_KEY_KEY] = apiKey
        }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = isDark
        }
    }

    suspend fun saveNewsScheduleTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[NEWS_SCHEDULE_TIME_KEY] = time
        }
    }

    suspend fun saveReminderDelayMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_DELAY_MINUTES_KEY] = minutes
        }
    }

    suspend fun saveWhatsappResponseTone(tone: String) {
        context.dataStore.edit { prefs ->
            prefs[WHATSAPP_RESPONSE_TONE_KEY] = tone
        }
    }

    suspend fun saveGmailUserEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[GMAIL_USER_EMAIL_KEY] = email
        }
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[taskEnabledKey(taskId)] = enabled
        }
    }
}
