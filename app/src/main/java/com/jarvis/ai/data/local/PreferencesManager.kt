package com.jarvis.ai.data.local

import android.content.Context
import android.content.SharedPreferences

enum class AiProvider(val id: String, val displayName: String, val defaultModel: String, val helpUrl: String) {
    GEMINI("GEMINI", "Google Gemini", "gemini-2.5-flash", "aistudio.google.com"),
    GROQ("GROQ", "Groq (Ultra-Fast Free)", "llama-3.3-70b-versatile", "console.groq.com/keys"),
    DEEPSEEK("DEEPSEEK", "DeepSeek Official", "deepseek-chat", "platform.deepseek.com"),
    OPENAI("OPENAI", "OpenAI / ChatGPT", "gpt-4o-mini", "platform.openai.com/api-keys");

    companion object {
        fun fromId(id: String): AiProvider {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GEMINI
        }
    }
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROVIDER = "selected_ai_provider"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_GROQ_API = "groq_api_key"
        private const val KEY_DEEPSEEK_API = "deepseek_api_key"
        private const val KEY_OPENAI_API = "openai_api_key"
        private const val KEY_VOICE_ENABLED = "voice_response_enabled"
        private const val DEFAULT_VALUE = ""
    }

    var selectedProvider: AiProvider
        get() = AiProvider.fromId(prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.id) ?: AiProvider.GEMINI.id)
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.id).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) = prefs.edit().putString(KEY_GEMINI_API, value.trim()).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) = prefs.edit().putString(KEY_GROQ_API, value.trim()).apply()

    var deepSeekApiKey: String
        get() = prefs.getString(KEY_DEEPSEEK_API, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_API, value.trim()).apply()

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI_API, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) = prefs.edit().putString(KEY_OPENAI_API, value.trim()).apply()

    fun getActiveApiKey(): String {
        return when (selectedProvider) {
            AiProvider.GEMINI -> geminiApiKey
            AiProvider.GROQ -> groqApiKey
            AiProvider.DEEPSEEK -> deepSeekApiKey
            AiProvider.OPENAI -> openAiApiKey
        }
    }

    fun setKeyForProvider(provider: AiProvider, key: String) {
        when (provider) {
            AiProvider.GEMINI -> geminiApiKey = key
            AiProvider.GROQ -> groqApiKey = key
            AiProvider.DEEPSEEK -> deepSeekApiKey = key
            AiProvider.OPENAI -> openAiApiKey = key
        }
    }

    fun getKeyForProvider(provider: AiProvider): String {
        return when (provider) {
            AiProvider.GEMINI -> geminiApiKey
            AiProvider.GROQ -> groqApiKey
            AiProvider.DEEPSEEK -> deepSeekApiKey
            AiProvider.OPENAI -> openAiApiKey
        }
    }

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()
}
