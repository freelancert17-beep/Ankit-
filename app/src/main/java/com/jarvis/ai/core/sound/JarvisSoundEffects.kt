package com.jarvis.ai.core.sound

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object JarvisSoundEffects {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (_: Exception) {}
    }

    /**
     * Futuristic Iron Man Arc-Reactor boot chime
     */
    fun playBootChime() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                delay(100)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (_: Exception) {}
        }
    }

    /**
     * Arc-Reactor active / listening start chirp
     */
    fun playListeningStart() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            } catch (_: Exception) {}
        }
    }

    /**
     * Task completion / answer ready sci-fi confirmation
     */
    fun playTaskComplete() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            } catch (_: Exception) {}
        }
    }

    /**
     * Action alert
     */
    fun playAlert() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (_: Exception) {}
        }
    }
}
