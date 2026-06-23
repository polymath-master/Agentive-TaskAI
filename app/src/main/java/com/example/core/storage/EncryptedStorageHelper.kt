package com.example.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Enterprise-grade cryptographic helper utilizing Android Keystore system.
 * Generates and manages dynamic hardware-backed AES keys to securely encrypt
 * sensitive user OAuth refresh and access tokens before persistence.
 */
object EncryptedStorageHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "AgentiveSecretKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "agentive_secure_prefs"

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(4 + iv.size + encryptedBytes.size)
            combined[0] = (iv.size shr 24).toByte()
            combined[1] = (iv.size shr 16).toByte()
            combined[2] = (iv.size shr 8).toByte()
            combined[3] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 4 + iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size < 4) return ""
            val ivSize = ((combined[0].toInt() and 0xFF) shl 24) or
                         ((combined[1].toInt() and 0xFF) shl 16) or
                         ((combined[2].toInt() and 0xFF) shl 8) or
                         (combined[3].toInt() and 0xFF)
            if (combined.size < 4 + ivSize) return ""
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 4, iv, 0, ivSize)
            
            val encryptedBytesSize = combined.size - 4 - ivSize
            val encryptedBytes = ByteArray(encryptedBytesSize)
            System.arraycopy(combined, 4 + ivSize, encryptedBytes, 0, encryptedBytesSize)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveToken(context: Context, key: String, token: String) {
        val encrypted = encrypt(token)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, encrypted)
            .apply()
    }

    fun getToken(context: Context, key: String): String {
        val encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null) ?: return ""
        return decrypt(encrypted)
    }

    fun clearToken(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
}
