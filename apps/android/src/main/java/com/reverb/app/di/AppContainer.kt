package com.reverb.app.di

import android.content.Context
import com.reverb.app.BuildConfig
import com.reverb.app.field.AccessibilityTextFieldGateway
import com.reverb.app.vault.ApiKeyVault
import com.reverb.app.vault.SettingsVault
import com.reverb.core.domain.ReverbEditor
import com.reverb.core.domain.engine.WritingEngine
import com.reverb.core.domain.field.TextFieldGateway
import com.reverb.core.domain.storage.ApiKeyStorage
import com.reverb.core.domain.storage.SettingsStorage
import com.reverb.core.network.OpenAiConfig
import com.reverb.core.network.OpenAiWritingEngine

/**
 * Builds and holds the app's single instances. This is the one place to swap an
 * implementation: point [writingEngine] at your own [WritingEngine], or [textField] at
 * your own [TextFieldGateway], and the rest of the app picks it up.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val apiKeyStorage: ApiKeyStorage by lazy { ApiKeyVault(appContext) }

    val settingsStorage: SettingsStorage by lazy { SettingsVault(appContext) }

    val writingEngine: WritingEngine by lazy {
        OpenAiWritingEngine(
            config = OpenAiConfig(
                baseUrl = BuildConfig.REVERB_LLM_BASE_URL,
                model = BuildConfig.REVERB_LLM_MODEL,
            ),
            apiKeyProvider = { apiKeyStorage.get() },
        )
    }

    val textField: TextFieldGateway = AccessibilityTextFieldGateway

    val editor: ReverbEditor by lazy { ReverbEditor(textField, writingEngine, settingsStorage) }
}
