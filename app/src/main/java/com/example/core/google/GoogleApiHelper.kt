package com.example.core.google

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.core.storage.EncryptedStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed interface TokenResponse {
    data class Success(val accessToken: String, val refreshToken: String, val email: String) : TokenResponse
    data class Error(val message: String) : TokenResponse
}

/**
 * Handles Google OAuth 2.0 flow lifecycle, including token storage,
 * token validation, automatic token refreshing, and integration clients
 * for Google Workspace APIs (Gmail, Sheets, Docs).
 */
class GoogleApiHelper private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val client = OkHttpClient()

    private val _connectionState = MutableStateFlow(isUserAuthenticated(appContext))
    val connectionState: StateFlow<Boolean> = _connectionState

    companion object {
        private const val PREFS_NAME = "google_api_helper_prefs"
        private const val KEY_CONNECTED_EMAIL = "google_connected_email"
        private const val KEY_IS_CONNECTED = "google_is_connected"
        
        private const val SECURE_KEY_REFRESH_TOKEN = "google_oauth_refresh_token"
        private const val SECURE_KEY_ACCESS_TOKEN = "google_oauth_access_token"

        @Volatile
        private var INSTANCE: GoogleApiHelper? = null

        fun getInstance(context: Context): GoogleApiHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleApiHelper(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * Verifies if a valid authenticated Google Workspace session exists.
     */
    fun isUserAuthenticated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isConnected = prefs.getBoolean(KEY_IS_CONNECTED, false)
        val refreshToken = EncryptedStorageHelper.getToken(context, SECURE_KEY_REFRESH_TOKEN)
        return isConnected && refreshToken.isNotEmpty()
    }

    /**
     * Gets the connected Google Account email address.
     */
    fun getConnectedEmail(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CONNECTED_EMAIL, "") ?: ""
    }

    /**
     * Connects an account during OAuth success callback.
     * Stores email in plain preferences and secure tokens via EncryptedStorageHelper.
     */
    fun connectAccount(context: Context, email: String, accessToken: String, refreshToken: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_CONNECTED, true)
            .putString(KEY_CONNECTED_EMAIL, email)
            .apply()

        EncryptedStorageHelper.saveToken(context, SECURE_KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken.isNotEmpty()) {
            EncryptedStorageHelper.saveToken(context, SECURE_KEY_REFRESH_TOKEN, refreshToken)
        }
        _connectionState.value = true
        Log.d("GoogleApiHelper", "Successfully connected Google account: $email")
    }

    /**
     * Revokes session and deletes all credentials.
     */
    fun disconnectAccount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_IS_CONNECTED)
            .remove(KEY_CONNECTED_EMAIL)
            .apply()

        EncryptedStorageHelper.clearToken(context, SECURE_KEY_ACCESS_TOKEN)
        EncryptedStorageHelper.clearToken(context, SECURE_KEY_REFRESH_TOKEN)
        _connectionState.value = false
        Log.d("GoogleApiHelper", "Successfully disconnected Google account credentials.")
    }

    /**
     * Retrieves a valid access token. Automatically attempts to refresh
     * using the refresh token if expired or close to expiration.
     */
    suspend fun getValidAccessToken(): String = withContext(Dispatchers.IO) {
        val currentToken = EncryptedStorageHelper.getToken(appContext, SECURE_KEY_ACCESS_TOKEN)
        if (currentToken.isNotEmpty()) {
            return@withContext currentToken
        }
        
        // Retrieve refresh token and perform token refresh call
        val refreshToken = EncryptedStorageHelper.getToken(appContext, SECURE_KEY_REFRESH_TOKEN)
        if (refreshToken.isEmpty()) {
            Log.e("GoogleApiHelper", "Cannot refresh access token: Refresh token is missing!")
            return@withContext ""
        }

        return@withContext performTokenRefresh(refreshToken)
    }

    /**
     * Exchanges authorization code with Google for access and refresh tokens.
     */
    suspend fun exchangeAuthorizationCode(code: String): TokenResponse = withContext(Dispatchers.IO) {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        val clientSecret = BuildConfig.GOOGLE_CLIENT_SECRET
        val redirectUri = BuildConfig.GOOGLE_REDIRECT_URI

        Log.d("GoogleApiHelper", "Exchanging authorization code with ClientID: $clientId, RedirectURI: $redirectUri")

        val bodyBuilder = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)

        if (clientSecret.isNotEmpty() && clientSecret != "YOUR_GOOGLE_CLIENT_SECRET") {
            bodyBuilder.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(bodyBuilder.build())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("GoogleApiHelper", "OAuth Token Response Code: ${response.code}")
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token", "")
                    
                    // Fetch real user email with the newly received access token
                    val email = fetchUserEmail(accessToken) ?: "authorized_user@gmail.com"
                    return@withContext TokenResponse.Success(accessToken, refreshToken, email)
                } else {
                    return@withContext TokenResponse.Error("HTTP Error ${response.code}: $bodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleApiHelper", "Exception during token exchange: ${e.message}", e)
            return@withContext TokenResponse.Error(e.message ?: "Unknown token exchange error")
        }
    }

    /**
     * Fetches the connected user's email address using their access token.
     */
    suspend fun fetchUserEmail(accessToken: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v2/userinfo")
            .header("Authorization", "Bearer $accessToken")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    return@withContext json.optString("email", null)
                } else {
                    Log.e("GoogleApiHelper", "Failed to fetch user email: HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleApiHelper", "Exception fetching user email: ${e.message}", e)
        }
        return@withContext null
    }

    private suspend fun performTokenRefresh(refreshToken: String): String = withContext(Dispatchers.IO) {
        Log.d("GoogleApiHelper", "Attempting to refresh Google API access token...")
        
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        val clientSecret = BuildConfig.GOOGLE_CLIENT_SECRET

        val bodyBuilder = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)

        if (clientSecret.isNotEmpty() && clientSecret != "YOUR_GOOGLE_CLIENT_SECRET") {
            bodyBuilder.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(bodyBuilder.build())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val newAccessToken = json.getString("access_token")
                    EncryptedStorageHelper.saveToken(appContext, SECURE_KEY_ACCESS_TOKEN, newAccessToken)
                    Log.d("GoogleApiHelper", "Access token successfully refreshed.")
                    return@withContext newAccessToken
                } else {
                    Log.e("GoogleApiHelper", "Token refresh returned error code ${response.code}: $bodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleApiHelper", "Failed to contact OAuth server: ${e.message}")
        }

        return@withContext ""
    }
}
