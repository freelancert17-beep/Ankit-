package com.jarvis.ai.core.intelligence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WebSearchEngine {

    /**
     * Determines if the user's query asks for live real-time web info.
     */
    fun isLiveSearchQuery(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("aaj ") || q.contains("today") ||
                q.contains("cricket") || q.contains("score") ||
                q.contains("match") || q.contains("khabar") ||
                q.contains("news") || q.contains("mausam") ||
                q.contains("weather") || q.contains("taza") ||
                q.contains("kya chal raha hai") || q.contains("latest") ||
                q.contains("rate") || q.contains("price")
    }

    /**
     * Executes a live web search via DuckDuckGo and returns concise real-time context.
     */
    suspend fun searchLiveWeb(query: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                // DuckDuckGo Instant Answer API
                val urlString = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; JARVIS-AI/5.0)")
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)

                    val abstractText = json.optString("AbstractText", "")
                    val answer = json.optString("Answer", "")
                    val relatedTopics = json.optJSONArray("RelatedTopics")

                    val sb = StringBuilder()
                    if (answer.isNotBlank()) {
                        sb.append("Direct Answer: $answer\n")
                    }
                    if (abstractText.isNotBlank()) {
                        sb.append("Abstract: $abstractText\n")
                    }

                    if (relatedTopics != null && relatedTopics.length() > 0) {
                        for (i in 0 until minOf(relatedTopics.length(), 4)) {
                            val topicObj = relatedTopics.optJSONObject(i)
                            val text = topicObj?.optString("Text", "")
                            if (!text.isNullOrBlank()) {
                                sb.append("• $text\n")
                            }
                        }
                    }

                    val result = sb.toString().trim()
                    if (result.isNotBlank()) {
                        return@withContext "=== LIVE INTERNET SEARCH FINDINGS ===\n$result\n======================================"
                    }
                }
            } catch (_: Exception) {}
            null
        }
    }
}
