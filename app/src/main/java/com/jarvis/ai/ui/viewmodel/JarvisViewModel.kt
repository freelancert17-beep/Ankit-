package com.jarvis.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.core.agents.AgentOrchestrator
import com.jarvis.ai.core.agents.AgentType
import com.jarvis.ai.core.voice.VoiceManager
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.repository.JarvisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(db.chatDao(), db.memoryDao())
    private val orchestrator = AgentOrchestrator(application)
    private val voiceManager = VoiceManager(application)

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeAgent = MutableStateFlow(AgentType.MAIN_JARVIS)
    val activeAgent: StateFlow<AgentType> = _activeAgent.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _pendingApproval = MutableStateFlow<String?>(null)
    val pendingApproval: StateFlow<String?> = _pendingApproval.asStateFlow()

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

            // Route via Multi-Agent Orchestrator
            val result = orchestrator.routeAndExecute(query)
            _activeAgent.value = result.agentType

            if (result.requiresUserApproval) {
                _pendingApproval.value = result.pendingActionDescription
            }

            // Save Jarvis response
            repository.saveMessage(
                ChatMessageEntity(
                    sender = "JARVIS",
                    content = result.outputText,
                    agentType = result.agentType.name,
                    executionPlan = result.planSummary
                )
            )

            // Voice response
            voiceManager.speak(result.outputText)

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
   fun askGemini(userPrompt: String, onResult: (String) -> Unit) {
    val apiKey = "" // <--
    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")

    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonInputString = """
                {
                  "contents": [{
                    "parts":[{"text": "You are JARVIS, a helpful AI assistant. Reply concisely. User: $userPrompt"}]
                  }]
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                val input: ByteArray = jsonInputString.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(response)
                val aiReply = jsonObject.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                
                withContext(Dispatchers.Main) {
                    onResult(aiReply)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult("Error: API call failed with code ${connection.responseCode}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Error: ${e.message}")
            }
        }
    }
} }
}
