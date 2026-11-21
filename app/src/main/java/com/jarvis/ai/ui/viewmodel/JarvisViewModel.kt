package com.jarvis.ai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.core.agents.AgentOrchestrator
import com.jarvis.ai.core.agents.AgentType
import com.jarvis.ai.core.sound.JarvisSoundEffects
import com.jarvis.ai.core.voice.VoiceManager
import com.jarvis.ai.data.local.AiProvider
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.local.PreferencesManager
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.local.entity.ChatSessionItem
import com.jarvis.ai.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(db.chatDao(), db.memoryDao())
    private val orchestrator = AgentOrchestrator(application, repository)
    private val prefs = PreferencesManager(application)

    // Current Session ID (Fresh session generated on every new chat / app start)
    private val _currentSessionId = MutableStateFlow("session_${System.currentTimeMillis()}")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // Active messages in the current session
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            repository.getMessagesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All chat sessions list for the side drawer
    private val _sessions = MutableStateFlow<List<ChatSessionItem>>(emptyList())
    val sessions: StateFlow<List<ChatSessionItem>> = _sessions.asStateFlow()

    // Attached Image URI (Multimodal Vision)
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val voiceManager = VoiceManager(
        context = application,
        onSpeechRecognized = { recognizedText ->
            sendCommand(query = recognizedText, isVoiceQuery = true)
        },
        onListeningState = { listening ->
            _isListening.value = listening
        }
    )

    private val _activeAgent = MutableStateFlow(AgentType.MAIN_JARVIS)
    val activeAgent: StateFlow<AgentType> = _activeAgent.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Multi-Provider state
    private val _selectedProvider = MutableStateFlow(prefs.selectedProvider)
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.getActiveApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        refreshSessionsList()
    }

    fun refreshSessionsList() {
        viewModelScope.launch {
            repository.allSessionIds.collectLatest { sessionIds ->
                val list = mutableListOf<ChatSessionItem>()
                for (id in sessionIds) {
                    val details = repository.getSessionDetails(id)
                    list.add(details)
                }
                _sessions.value = list
            }
        }
    }

    fun startNewChat() {
        _currentSessionId.value = "session_${System.currentTimeMillis()}"
        _selectedImageUri.value = null
        voiceManager.stopSpeaking()
        JarvisSoundEffects.playBootChime()
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _selectedImageUri.value = null
        voiceManager.stopSpeaking()
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewChat()
            }
            refreshSessionsList()
        }
    }

    fun selectImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun selectProvider(provider: AiProvider) {
        prefs.selectedProvider = provider
        _selectedProvider.value = provider
        _apiKey.value = prefs.getActiveApiKey()
    }

    fun updateKeyForProvider(provider: AiProvider, newKey: String) {
        prefs.setKeyForProvider(provider, newKey)
        if (provider == prefs.selectedProvider) {
            _apiKey.value = newKey
        }
    }

    fun getKeyForProvider(provider: AiProvider): String {
        return prefs.getKeyForProvider(provider)
    }

    fun updateApiKey(newKey: String) {
        updateKeyForProvider(_selectedProvider.value, newKey)
    }

    fun startVoiceInput() {
        if (_isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun sendCommand(query: String, isVoiceQuery: Boolean = false) {
        if (query.isBlank() && _selectedImageUri.value == null) return

        val activeSession = _currentSessionId.value
        val attachedImageUri = _selectedImageUri.value
        _selectedImageUri.value = null // Reset after sending

        viewModelScope.launch {
            // Save User message
            repository.saveMessage(
                ChatMessageEntity(
                    sessionId = activeSession,
                    sender = "USER",
                    content = query,
                    imageUri = attachedImageUri?.toString()
                )
            )

            _isProcessing.value = true

            // Gather recent history for conversation context
            val history = messages.value.takeLast(6).map { it.sender to it.content }

            // Extract Base64 and MIME type from Image if attached
            var imageBase64: String? = null
            var imageMimeType: String? = null
            if (attachedImageUri != null) {
                val imageBundle = processImageUri(attachedImageUri)
                imageBase64 = imageBundle?.first
                imageMimeType = imageBundle?.second
            }

            // Execute via Multi-Agent & Multi-Model Orchestrator
            val result = orchestrator.routeAndExecute(
                query = query,
                history = history,
                imageBase64 = imageBase64,
                imageMimeType = imageMimeType
            )
            _activeAgent.value = result.agentType

            // Save Jarvis response
            repository.saveMessage(
                ChatMessageEntity(
                    sessionId = activeSession,
                    sender = "JARVIS",
                    content = result.outputText,
                    agentType = result.agentType.name,
                    executionPlan = result.planSummary,
                    filePath = result.generatedFilePath,
                    fileName = result.generatedFileName,
                    fileType = result.generatedFileType
                )
            )

            JarvisSoundEffects.playTaskComplete()

            // Voice response:
            // Only speak aloud if triggered by Voice Assistant
            if (isVoiceQuery) {
                val textToSpeak = result.spokenText ?: result.outputText
                voiceManager.speak(textToSpeak)
            }

            _isProcessing.value = false
            refreshSessionsList()
        }
    }

    private suspend fun processImageUri(uri: Uri): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val cr = getApplication<Application>().contentResolver
                val mimeType = cr.getType(uri) ?: "image/jpeg"

                cr.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null

                    // Scale down for optimal network transfer
                    val maxDim = 1024
                    val scale = minOf(1.0f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
                    val scaledBitmap = if (scale < 1.0f) {
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else {
                        bitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    Pair(b64, "image/jpeg")
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun openFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val extension = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
            startNewChat()
            refreshSessionsList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
