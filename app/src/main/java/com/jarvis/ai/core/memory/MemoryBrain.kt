package com.jarvis.ai.core.memory

import com.jarvis.ai.data.repository.JarvisRepository

class MemoryBrain(private val repository: JarvisRepository) {

    /**
     * Inspects input text for personal facts, preferences, goals, and projects.
     * Returns true if a memory was detected and saved.
     */
    suspend fun extractAndSaveMemory(query: String): Boolean {
        val q = query.trim()
        val lower = q.lowercase()

        // 1. User Name detection ("mera naam ... hai" / "my name is ...")
        val nameRegexHindi = Regex("""mera naam\s+([A-Za-z\u0900-\u097F]+)\s*(?:hai)?""", RegexOption.IGNORE_CASE)
        val nameRegexEng = Regex("""my name is\s+([A-Za-z]+)""", RegexOption.IGNORE_CASE)

        val nameMatch = nameRegexHindi.find(lower) ?: nameRegexEng.find(lower)
        if (nameMatch != null) {
            val name = nameMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            repository.saveMemory("user_name", name, "IDENTITY")
            return true
        }

        // 2. Favorite item ("meri favorite [car/food/bike] [name] hai" / "my favorite [X] is [Y]")
        val favRegex = Regex("""(?:meri|mera|my)\s+favorite\s+([a-zA-Z]+)\s+([A-Za-z0-9\s]+?)(?:\s+hai|\s+is)?$""", RegexOption.IGNORE_CASE)
        val favMatch = favRegex.find(lower)
        if (favMatch != null) {
            val itemType = favMatch.groupValues[1].trim()
            val itemValue = favMatch.groupValues[2].trim()
            repository.saveMemory("favorite_$itemType", itemValue, "PREFERENCE")
            return true
        }

        // 3. Likes ("mujhe [X] pasand hai" / "i like [X]")
        val likesRegex = Regex("""mujhe\s+([A-Za-z0-9\s]+?)\s+pasand\s+hai""", RegexOption.IGNORE_CASE)
        val likesMatch = likesRegex.find(lower)
        if (likesMatch != null) {
            val likeItem = likesMatch.groupValues[1].trim()
            repository.saveMemory("likes_${likeItem.take(15)}", likeItem, "PREFERENCE")
            return true
        }

        // 4. Goal & Project Tracking ("hamara goal [X] hai", "project [X] par kaam kar rahe hain")
        if (lower.contains("goal hai") || lower.contains("target hai") || lower.contains("my goal is")) {
            val goalText = q.substringAfter("goal hai").substringAfter("target hai").substringAfter("is").trim()
            if (goalText.isNotBlank()) {
                repository.saveMemory("active_goal", goalText, "GOAL")
                return true
            }
        }

        // 5. Explicit Memory command ("yaad rakhna ki [X]" / "remember that [X]")
        if (lower.startsWith("yaad rakhna ki ") || lower.startsWith("yaad rakhna ") || lower.startsWith("remember that ")) {
            val note = q.substringAfter("yaad rakhna ki ")
                .substringAfter("yaad rakhna ")
                .substringAfter("remember that ")
                .trim()
            if (note.isNotBlank()) {
                repository.saveMemory("note_${System.currentTimeMillis() % 100000}", note, "EPISODIC")
                return true
            }
        }

        return false
    }

    /**
     * Formats all saved memories into a clean prompt context block for LLMs.
     */
    suspend fun getFormattedMemoryContext(): String {
        val memories = repository.getAllMemoriesList()
        if (memories.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("\n=== USER PERSONAL BRAIN & LONG-TERM MEMORIES ===\n")
        for (m in memories) {
            val readableKey = m.key.replace("_", " ").replaceFirstChar { it.uppercase() }
            sb.append("• [$readableKey] (${m.category}): ${m.value}\n")
        }
        sb.append("Always honor and remember these facts when interacting with Boss.\n===============================================\n")
        return sb.toString()
    }

    /**
     * Context Compression: Condenses older conversation turns to prevent context window overflow
     * while retaining critical context and recent turns.
     */
    fun compressChatHistory(history: List<Pair<String, String>>, maxRecent: Int = 6): List<Pair<String, String>> {
        if (history.size <= maxRecent) return history

        val older = history.dropLast(maxRecent)
        val recent = history.takeLast(maxRecent)

        // Compress older messages into a single summary entry
        val summaryBullets = older.take(4).joinToString("; ") { (sender, text) ->
            "$sender asked: ${text.take(40)}"
        }

        val compressedEntry = Pair("SYSTEM", "[Previous conversation context summary: $summaryBullets]")
        return listOf(compressedEntry) + recent
    }
}
