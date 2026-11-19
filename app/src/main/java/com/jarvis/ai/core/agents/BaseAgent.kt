package com.jarvis.ai.core.agents

interface BaseAgent {
    val agentType: AgentType
    fun canHandle(query: String): Boolean
    suspend fun execute(query: String, context: Map<String, String>): AgentExecutionResult
}
