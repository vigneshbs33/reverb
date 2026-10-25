package com.reverb.core.domain.storage

/** Stores the API key for the default engine. Implemented with encrypted storage on Android. */
interface ApiKeyStorage {
    fun save(key: String)
    fun get(): String?
    fun clear()
}

/**
 * Stores the user's writing samples, one per tone name plus [StyleSamples.REPLY_KEY]
 * for Smart Reply.
 */
interface SettingsStorage {
    fun saveCustomInstruction(toneName: String, instruction: String)
    fun getCustomInstruction(toneName: String): String
}

/** A non persistent [SettingsStorage], handy for tests and for hosts that keep samples elsewhere. */
class InMemorySettingsStorage(initial: Map<String, String> = emptyMap()) : SettingsStorage {
    private val samples = initial.toMutableMap()

    override fun saveCustomInstruction(toneName: String, instruction: String) {
        samples[toneName] = instruction
    }

    override fun getCustomInstruction(toneName: String): String = samples[toneName].orEmpty()
}
