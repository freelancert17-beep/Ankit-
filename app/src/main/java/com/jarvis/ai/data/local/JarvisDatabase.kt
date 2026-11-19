package com.jarvis.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jarvis.ai.data.local.dao.ChatDao
import com.jarvis.ai.data.local.dao.MemoryDao
import com.jarvis.ai.data.local.entity.AgentTaskEntity
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.local.entity.MemoryEntity

@Database(
    entities = [ChatMessageEntity::class, MemoryEntity::class, AgentTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_master_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
