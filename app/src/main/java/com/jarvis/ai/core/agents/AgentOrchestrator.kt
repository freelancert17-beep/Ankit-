package com.jarvis.ai.core.agents

import android.content.Context
import com.jarvis.ai.core.intelligence.GeminiApiClient
import com.jarvis.ai.core.intelligence.RuleEngine
import com.jarvis.ai.data.local.PreferencesManager
import java.io.File

class AgentOrchestrator(context: Context) {

    private val prefs = PreferencesManager(context)
    private val ruleEngine = RuleEngine(context)
    private val geminiClient = GeminiApiClient()

    private val agents: List<BaseAgent> = listOf(
        CodingAgent(File(context.filesDir, "workspace")),
        OfficeAgent(),
        TradingAgent()
    )

    suspend fun routeAndExecute(query: String, history: List<Pair<String, String>> = emptyList()): AgentExecutionResult {
        // Tier 1: Fast Rule Engine (Torch, Battery, Local Calculations)
        val ruleResult = ruleEngine.matchAndExecute(query)
        if (ruleResult != null) {
            return ruleResult
        }

        // Tier 2: Specialized Local Agents (Code workspace check, specific GST slab info, etc.)
        for (agent in agents) {
            if (agent.canHandle(query)) {
                // If query is an open-ended question about coding/trading, let Gemini handle with agent plan
                val agentRes = agent.execute(query, emptyMap())
                if (!query.contains("?") && !query.lowercase().startsWith("kya") && !query.lowercase().startsWith("how")) {
                    return agentRes
                }
            }
        }

        // Tier 3: Live Gemini AI Engine (Answers ANY question in Hindi/English/Hinglish)
        val apiKey = prefs.geminiApiKey
        val aiResponse = geminiClient.generateAnswer(apiKey, query, history)

        return AgentExecutionResult(
            agentType = AgentType.MAIN_JARVIS,
            success = true,
            planSummary = "JARVIS Neural Engine",
            outputText = aiResponse
        )
    }
}
