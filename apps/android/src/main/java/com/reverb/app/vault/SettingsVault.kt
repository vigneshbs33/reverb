package com.reverb.app.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.reverb.core.domain.storage.SettingsStorage

/**
 * Securely stores user preferences for custom tone instructions across app restarts.
 */
class SettingsVault(context: Context) : SettingsStorage {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "reverb_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun saveCustomInstruction(toneName: String, instruction: String) {
        prefs.edit().putString("custom_instruction_$toneName", instruction).apply()
    }

    override fun getCustomInstruction(toneName: String): String {
        return prefs.getString("custom_instruction_$toneName", "") ?: ""
    }
}
