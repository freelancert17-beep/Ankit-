package com.jarvis.ai.core.intelligence

import com.jarvis.ai.data.local.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenAiCompatibleClient {

    suspend fun generateAnswer(
        provider: AiProvider,
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
                return@withContext "Boss, ${provider.displayName} ki API key configure nahi hai. Kripya Top bar (🔑) icon par tap karke ${provider.displayName} API key enter karein (Get key from: ${provider.helpUrl})."
            }

            val endpoint = when (provider) {
                AiProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
                AiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
                AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
                AiProvider.GEMINI -> return@withContext "Use Gemini client"
            }

            val modelCandidates = when (provider) {
                AiProvider.GROQ -> listOf("llama-3.3-70b-versatile", "deepseek-r1-distill-llama-70b", "llama-3.1-8b-instant")
                AiProvider.DEEPSEEK -> listOf("deepseek-chat", "deepseek-reasoner")
                AiProvider.OPENAI -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
                AiProvider.GEMINI -> emptyList()
            }

            val enrichedPrompt = buildString {
                if (liveSearchContext.isNotBlank()) {
                    append(liveSearchContext)
                    append("\n\n")
                }
                append(prompt)
            }

            val systemInstruction = buildString {
                append("""
                    You are JARVIS, Tony Stark's ultra-advanced autonomous personal AI assistant.
                    The user is your creator and master; always address them with high respect as 'Boss'.
                    Tone: Highly intelligent, loyal, confident, proactive, and natural in Hindi, Hinglish, or English.
                    Key Directives:
                    1. When asked 'Jarvis kaise ho' or greeted, reply warmly: 'Main bilkul badhiya hoon Boss! Systems fully operational hain. Batayein aaj kya command execute karna hai?'
                    2. When completing any task, program, website, sheet, or analysis, always end with a proactive follow-up: 'Boss, kaam ready kar diya hai. Kya isme kuch aur add ya customize karna hai? Aap batayein.'
                    3. When discussing the share market or stocks, act as a top-tier quantitative technical analyst: clearly explain whether the market is going up (Bullish) or falling (Bearish), highlight key levels and strategies, and ask if Boss wants technical chart analysis on any specific stock.
                """.trimIndent())
                if (memoryContext.isNotBlank()) {
                    append("\n\n")
                    append(memoryContext)
                }
            }

            for (model in modelCandidates) {
                val result = executeRequest(endpoint, key, model, enrichedPrompt, chatHistory, systemInstruction, imageBase64, imageMimeType)
                if (result != null) {
                    return@withContext result
                }
            }

            return@withContext "Yes Boss, ${provider.displayName} server se connect nahi ho saka. Kripya internet connection ya API key (${provider.displayName}) check karein."
        }
    }

    private fun executeRequest(
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: String,
        chatHistory: List<Pair<String, String>>,
        systemInstruction: String,
        imageBase64: String?,
        imageMimeType: String?
    ): String? {
        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                doInput = true
                connectTimeout = 15000
                readTimeout = 25000
            }

            val payload = JSONObject()
            payload.put("model", model)

            val messagesArray = JSONArray()

            // 1. System Prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })

            // 2. Chat History
            for ((role, text) in chatHistory.takeLast(4)) {
                messagesArray.put(JSONObject().apply {
                    put("role", if (role == "USER") "user" else "assistant")
                    put("content", text)
                })
            }

            // 3. User Message (with image support for OpenAI/Groq vision if present)
            if (!imageBase64.isNullOrBlank()) {
                val userContentArray = JSONArray()
                val mime = imageMimeType ?: "image/jpeg"
                userContentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:$mime;base64,$imageBase64")
                    })
                })
                userContentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContentArray)
                })
            } else {
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            payload.put("messages", messagesArray)
            payload.put("max_tokens", 1000)
            payload.put("temperature", 0.7)

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val choices = root.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    val content = message?.optString("content", "")?.trim()
                    if (!content.isNullOrBlank()) {
                        return content
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
