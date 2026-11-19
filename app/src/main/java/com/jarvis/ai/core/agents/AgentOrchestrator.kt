package com.jarvis.ai.core.agents

import android.content.Context
import com.jarvis.ai.core.intelligence.RuleEngine
import java.io.File

class AgentOrchestrator(context: Context) {

    private val ruleEngine = RuleEngine(context)
    private val agents: List<BaseAgent> = listOf(
        CodingAgent(File(context.filesDir, "workspace")),
        OfficeAgent(),
        TradingAgent()
    )

    suspend fun routeAndExecute(query: String): AgentExecutionResult {
        // Tier 1: Fast Rule Engine Check
        val ruleResult = ruleEngine.matchAndExecute(query)
        if (ruleResult != null) {
            return ruleResult
        }

        // Tier 2: Specialized Multi-Agent Match
        for (agent in agents) {
            if (agent.canHandle(query)) {
                return agent.execute(query, emptyMap())
            }
        }

        // Tier 3: Core JARVIS General Intelligence
        return AgentExecutionResult(
            agentType = AgentType.MAIN_JARVIS,
            success = true,
            planSummary = "Core General Reasoning",
            outputText = "Sir, I am processing your instruction across active systems. How may I further assist your workflow?"
        )
    }
}
