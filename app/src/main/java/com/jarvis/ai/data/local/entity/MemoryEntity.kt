package com.jarvis.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "GENERAL", // PREFERENCE, PROJECT, IDENTITY, SYSTEM
    val updatedAt: Long = System.currentTimeMillis(),
    val isSensitive: Boolean = false
)
