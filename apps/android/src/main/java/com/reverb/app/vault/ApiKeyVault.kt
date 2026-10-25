package com.reverb.app.vault

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.reverb.core.domain.storage.ApiKeyStorage

/**
 * Encrypted storage for the user's OpenAI API key using Android Keystore + AES-256.
 * The key never leaves the device.
 */
class ApiKeyVault(context: Context) : ApiKeyStorage {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e("ApiKeyVault", "Keystore/Prefs corrupted, recreating...", e)
            context.deleteSharedPreferences(PREFS_FILE)
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (k: Exception) {
                Log.e("ApiKeyVault", "Failed to purge hardware keystore alias", k)
            }
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    override fun save(key: String) {
        try {
            prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        } catch (e: Exception) {
            Log.e("ApiKeyVault", "Failed to save key", e)
        }
    }

    override fun get(): String? {
        return try {
            prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("ApiKeyVault", "Failed to read key, keystore might be invalid", e)
            null
        }
    }

    override fun clear() {
        try {
            prefs.edit().remove(KEY_API_KEY).apply()
        } catch (e: Exception) {
            Log.e("ApiKeyVault", "Failed to clear key", e)
        }
    }

    companion object {
        private const val PREFS_FILE = "reverb_vault"
        private const val KEY_API_KEY = "openai_api_key"
    }
}
