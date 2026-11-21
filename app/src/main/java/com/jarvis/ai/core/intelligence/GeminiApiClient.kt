package com.jarvis.ai.core.intelligence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiClient {

    companion object {
        @Volatile
        private var cachedWorkingModel: String? = null
        @Volatile
        private var cachedApiVersion: String = "v1beta"
    }

    private val staticCandidateModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash-latest",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-pro"
    )

    private val apiVersions = listOf("v1beta", "v1")

    suspend fun generateAnswer(
        apiKey: String,
        prompt: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        imageBase64: String? = null,
        imageMimeType: String? = null,
        memoryContext: String = "",
        liveSearchContext: String = ""
    ): String {
        return withContext(Dispatchers.IO) {
            val key = apiKey.trim()
            if (key.isBlank()) {
                return@withContext "Boss, Gemini API key configure nahi hai. Kripya Settings icon (🔑) par click karke apni API key enter karein."
            }

            // Build enriched context
            val enrichedPrompt = buildString {
                if (liveSearchContext.isNotBlank()) {
                    append(liveSearchContext)
                    append("\n\n")
                }
                append(prompt)
            }

            // 1. Try cached working model if already known
            val currentCachedModel = cachedWorkingModel
            if (!currentCachedModel.isNullOrBlank()) {
                val cachedResult = executeRequest(key, cachedApiVersion, currentCachedModel, enrichedPrompt, chatHistory, imageBase64, imageMimeType, memoryContext)
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }

            // 2. Discover available models dynamically via Google ModelService.ListModels
            val discoveredList = queryAvailableModels(key)
            for ((version, modelName) in discoveredList) {
                val result = executeRequest(key, version, modelName, enrichedPrompt, chatHistory, imageBase64, imageMimeType, memoryContext)
                if (result != null) {
                    cachedWorkingModel = modelName
                    cachedApiVersion = version
                    return@withContext result
                }
            }

            // 3. Fallback: try static matrix across versions and model identifiers
            for (version in apiVersions) {
                for (model in staticCandidateModels) {
                    val result = executeRequest(key, version, model, enrichedPrompt, chatHistory, imageBase64, imageMimeType, memoryContext)
                    if (result != null) {
                        cachedWorkingModel = model
                        cachedApiVersion = version
                        return@withContext result
                    }
                }
            }

            return@withContext "Yes Boss, AI server se response prapt nahi hua. Kripya internet connection ya Top bar (🔑) mein Gemini API key check karein."
        }
    }

    private fun queryAvailableModels(apiKey: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        for (version in apiVersions) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/$version/models?key=$apiKey"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey)
                    connectTimeout = 7000
                    readTimeout = 7000
                }

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val modelsArr = json.optJSONArray("models")
                    if (modelsArr != null) {
                        val names = mutableListOf<String>()
                        for (i in 0 until modelsArr.length()) {
                            val obj = modelsArr.getJSONObject(i)
                            val rawName = obj.optString("name", "")
                            val methods = obj.optJSONArray("supportedGenerationMethods")
                            var supportsGenerate = false
                            if (methods != null) {
                                for (j in 0 until methods.length()) {
                                    if (methods.getString(j) == "generateContent") {
                                        supportsGenerate = true
                                        break
                                    }
                                }
                            }
                            if (supportsGenerate && rawName.isNotBlank()) {
                                val clean = if (rawName.startsWith("models/")) rawName.removePrefix("models/") else rawName
                                names.add(clean)
                            }
                        }

                        // Prefer flash models for lower latency
                        val sorted = names.sortedWith(compareByDescending<String> {
                            when {
                                it.contains("2.5-flash") -> 100
                                it.contains("2.0-flash") -> 90
                                it.contains("1.5-flash") -> 80
                                it.contains("flash") -> 70
                                it.contains("pro") -> 60
                                else -> 10
                            }
                        })

                        for (name in sorted) {
                            list.add(version to name)
                        }

                        if (list.isNotEmpty()) {
                            return list
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return list
    }

    private fun executeRequest(
        apiKey: String,
        version: String,
        modelName: String,
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        imageBase64: String?,
        imageMimeType: String?,
        memoryContext: String
    ): String? {
        val cleanModel = if (modelName.startsWith("models/")) modelName.removePrefix("models/") else modelName
        val endpoint = "https://generativelanguage.googleapis.com/$version/models/$cleanModel:generateContent?key=$apiKey"

        // First attempt with systemInstruction
        var result = sendHttpPost(endpoint, apiKey, prompt, chatHistory, includeSystemInstruction = true, imageBase64, imageMimeType, memoryContext)
        if (result == null) {
            // Second attempt: embed instruction in user prompt
            result = sendHttpPost(endpoint, apiKey, prompt, chatHistory, includeSystemInstruction = false, imageBase64, imageMimeType, memoryContext)
        }
        return result
    }

    private fun sendHttpPost(
        endpoint: String,
        apiKey: String,
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        includeSystemInstruction: Boolean,
        imageBase64: String?,
        imageMimeType: String?,
        memoryContext: String
    ): String? {
        try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("x-goog-api-key", apiKey)
                doOutput = true
                doInput = true
                connectTimeout = 12000
                readTimeout = 18000
            }

            val payload = JSONObject()

            val baseInstruction = "You are JARVIS, Tony Stark's ultra-advanced autonomous personal AI assistant. Always address the user as 'Boss'. Respond in polite, intelligent, and natural Hindi, English, or Hinglish. Keep answers concise, highly intelligent, and direct."
            val bossSystemText = if (memoryContext.isNotBlank()) {
                "$baseInstruction\n$memoryContext"
            } else {
                baseInstruction
            }

            if (includeSystemInstruction) {
                val sys = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", bossSystemText)
                        })
                    })
                }
                payload.put("systemInstruction", sys)
            }

            val contentsArray = JSONArray()

            val effectivePrompt = if (!includeSystemInstruction) {
                "[System: $bossSystemText]\n\nBoss query: $prompt"
            } else {
                prompt
            }

            // History
            for ((role, text) in chatHistory.takeLast(4)) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (role == "USER") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", text))
                    })
                })
            }

            // User parts (Supports Multimodal Vision Images & Documents)
            val userParts = JSONArray()
            if (!imageBase64.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", imageMimeType ?: "image/jpeg")
                    put("data", imageBase64)
                }
                userParts.put(JSONObject().apply {
                    put("inlineData", inlineData)
                })
            }
            userParts.put(JSONObject().apply {
                put("text", effectivePrompt)
            })

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            })

            payload.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1000)
            }
            payload.put("generationConfig", genConfig)

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val reply = parts.getJSONObject(0).optString("text", "").trim()
                        if (reply.isNotBlank()) {
                            return reply
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
