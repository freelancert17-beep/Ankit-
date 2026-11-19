package com.jarvis.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskName: String,
    val agentType: String,
    val status: String, // PENDING, RUNNING, COMPLETED, FAILED, AWAITING_APPROVAL
    val planDetails: String,
    val result: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
