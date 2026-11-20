package com.jarvis.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.core.agents.AgentOrchestrator
import com.jarvis.ai.core.agents.AgentType
import com.jarvis.ai.core.voice.VoiceManager
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.local.PreferencesManager
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.repository.JarvisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(db.chatDao(), db.memoryDao())
    private val orchestrator = AgentOrchestrator(application)
    private val prefs = PreferencesManager(application)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val voiceManager = VoiceManager(
        context = application,
        onSpeechRecognized = { recognizedText ->
            sendCommand(recognizedText)
        },
        onListeningState = { listening ->
            _isListening.value = listening
        }
    )

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeAgent = MutableStateFlow(AgentType.MAIN_JARVIS)
    val activeAgent: StateFlow<AgentType> = _activeAgent.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.geminiApiKey)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun updateApiKey(newKey: String) {
        prefs.geminiApiKey = newKey
        _apiKey.value = newKey
    }

    fun startVoiceInput() {
        if (_isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun sendCommand(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            // Save User message
            repository.saveMessage(
                ChatMessageEntity(
                    sender = "USER",
                    content = query
                )
            )

            _isProcessing.value = true

            // Gather recent history for conversation context
            val history = messages.value.takeLast(6).map { it.sender to it.content }

            // Execute via Multi-Agent & Gemini Orchestrator
            val result = orchestrator.routeAndExecute(query, history)
            _activeAgent.value = result.agentType

            // Save Jarvis response
            repository.saveMessage(
                ChatMessageEntity(
                    sender = "JARVIS",
                    content = result.outputText,
                    agentType = result.agentType.name,
                    executionPlan = result.planSummary
                )
            )

            // Voice response in natural Indian female tone
            if (prefs.isVoiceEnabled) {
                voiceManager.speak(result.outputText)
            }

            _isProcessing.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
