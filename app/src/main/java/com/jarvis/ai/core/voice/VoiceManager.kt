package com.jarvis.ai.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Configure Indian English / Hindi locale preference
            val localeIn = Locale("en", "IN")
            val result = tts?.setLanguage(localeIn)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            tts?.setPitch(1.05f) // Natural friendly female tone modulation
            tts?.setSpeechRate(1.0f)
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            val clean = text.replace("*", "").replace("#", "").replace("`", "")
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_TTS")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
