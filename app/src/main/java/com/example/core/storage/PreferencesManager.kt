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
        val RSS_FEEDS_KEY = stringSetPreferencesKey("rss_feeds")
        val LAST_MISSED_CALL_CONTACT_KEY = stringPreferencesKey("last_missed_call_contact")
        val SHEET_URL_KEY = stringPreferencesKey("sheet_url")
        val TEMPLATE_DOC_URL_KEY = stringPreferencesKey("template_doc_url")
        val DEFAULT_FEEDS = setOf(
            "https://www.prothomalo.com/feed",
            "https://www.thedailystar.net/frontpage/rss.xml",
            "https://bdnews24.com/?widget=rssfeed"
        )
        
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

    val sheetUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SHEET_URL_KEY] ?: "https://docs.google.com/spreadsheets/d/example_headcount"
    }

    val templateDocUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEMPLATE_DOC_URL_KEY] ?: "https://docs.google.com/document/d/example_template"
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

    suspend fun saveSheetUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SHEET_URL_KEY] = url
        }
    }

    suspend fun saveTemplateDocUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[TEMPLATE_DOC_URL_KEY] = url
        }
    }

    val rssFeedsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[RSS_FEEDS_KEY] ?: DEFAULT_FEEDS
    }

    suspend fun saveRssFeeds(feeds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[RSS_FEEDS_KEY] = feeds
        }
    }

    val lastMissedCallContactFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_MISSED_CALL_CONTACT_KEY] ?: "Unsaved Contact"
    }

    suspend fun saveLastMissedCallContact(contactName: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_MISSED_CALL_CONTACT_KEY] = contactName
        }
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[taskEnabledKey(taskId)] = enabled
        }
    }
}
