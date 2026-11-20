package com.jarvis.ai.core.intelligence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiClient {

    // Supported models in fallback order
    private val models = listOf("gemini-1.5-flash", "gemini-2.5-flash", )

    suspend fun generateAnswer(apiKey: String, prompt: String, chatHistory: List<Pair<String, String>> = emptyList()): String {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext "Boss, Gemini API key configure nahi hai. Kripya Settings icon par click karke apni API key enter karein."
            }

            var lastError = "Unable to reach Gemini servers."

            for (model in models) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                        doInput = true
                        connectTimeout = 15000
                        readTimeout = 20000
                    }

                    // Build JSON payload
                    val payload = JSONObject()
                    val contentsArray = JSONArray()

                    // System Instruction to enforce "Boss" persona & natural voice output
                    val systemInstruction = JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are JARVIS, an advanced autonomous personal AI assistant. Always address the user as 'Boss'. Respond in natural, polite Hindi, English, or Hinglish. Keep answers concise, intelligent, and conversational so they sound natural when spoken aloud.")
                            })
                        })
                    }
                    payload.put("system_instruction", systemInstruction)

                    // Recent history context
                    for ((role, text) in chatHistory.takeLast(6)) {
                        val historyItem = JSONObject().apply {
                            put("role", if (role == "USER") "user" else "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", text))
                            })
                        }
                        contentsArray.put(historyItem)
                    }

                    // Current user prompt
                    val userItem = JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    }
                    contentsArray.put(userItem)

                    payload.put("contents", contentsArray)

                    // Send request
                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                        val responseJson = JSONObject(responseText)
                        val candidates = responseJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val reply = parts.getJSONObject(0).optString("text", "").trim()
                                if (reply.isNotBlank()) {
                                    return@withContext reply
                                }
                            }
                        }
                    } else {
                        val errorStream = conn.errorStream
                        val errText = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                        lastError = "API Error ($responseCode): $errText"
                    }
                } catch (e: Exception) {
                    lastError = "Connection Error: ${e.localizedMessage ?: e.message}"
                }
            }

            return@withContext "Yes Boss, AI server se connect hone mein samasya aayi: $lastError"
        }
    }
}
