package com.jarvis.ai.data.repository

import com.jarvis.ai.data.local.dao.ChatDao
import com.jarvis.ai.data.local.dao.MemoryDao
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.local.entity.ChatSessionItem
import com.jarvis.ai.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val chatDao: ChatDao,
    private val memoryDao: MemoryDao
) {
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesBySession(sessionId)
    }

    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val allSessionIds: Flow<List<String>> = chatDao.getAllSessionIds()

    suspend fun getSessionDetails(sessionId: String): ChatSessionItem {
        val firstMsg = chatDao.getFirstUserMessage(sessionId)
        val lastMsg = chatDao.getLastMessage(sessionId)
        val count = chatDao.getMessageCount(sessionId)

        val rawTitle = firstMsg?.content ?: "Conversation"
        val title = if (rawTitle.length > 32) rawTitle.take(30) + "…" else rawTitle
        val timestamp = lastMsg?.timestamp ?: System.currentTimeMillis()

        return ChatSessionItem(
            sessionId = sessionId,
            title = title,
            lastTimestamp = timestamp,
            messageCount = count
        )
    }

    suspend fun saveMessage(message: ChatMessageEntity): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "GENERAL") {
        memoryDao.saveMemory(MemoryEntity(key = key, value = value, category = category))
    }

    suspend fun getAllMemoriesList(): List<MemoryEntity> {
        return memoryDao.getMemoriesList()
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }
}
