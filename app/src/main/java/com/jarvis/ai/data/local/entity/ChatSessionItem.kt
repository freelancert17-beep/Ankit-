package com.jarvis.ai.data.local.entity

data class ChatSessionItem(
    val sessionId: String,
    val title: String,
    val lastTimestamp: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)
