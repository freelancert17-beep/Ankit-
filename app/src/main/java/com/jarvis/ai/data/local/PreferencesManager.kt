package com.jarvis.ai.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_VOICE_ENABLED = "voice_response_enabled"
        // User's active Gemini API Key
        private const val DEFAULT_API_KEY = "AQ.Ab8RN6LF11_e0qH4jWLOMeBm41IdYStRIva_pmBKxvWZ5-jGiQ"
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_GEMINI_API, value.trim()).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()
}
