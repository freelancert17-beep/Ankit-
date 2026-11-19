package com.jarvis.ai.data.repository

import com.jarvis.ai.data.local.dao.ChatDao
import com.jarvis.ai.data.local.dao.MemoryDao
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val chatDao: ChatDao,
    private val memoryDao: MemoryDao
) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun saveMessage(message: ChatMessageEntity): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "GENERAL") {
        memoryDao.saveMemory(MemoryEntity(key = key, value = value, category = category))
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }
}
