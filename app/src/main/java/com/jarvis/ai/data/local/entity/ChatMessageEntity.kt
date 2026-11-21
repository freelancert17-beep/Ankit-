package com.jarvis.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val sender: String, // "USER" or "JARVIS"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentType: String = "MAIN_JARVIS",
    val isVerified: Boolean = true,
    val executionPlan: String? = null,
    val imageUri: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val fileType: String? = null // "SHEET", "DOC", "CODE", etc.
)
