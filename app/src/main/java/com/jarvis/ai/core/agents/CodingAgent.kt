package com.jarvis.ai.core.agents

import java.io.File

class CodingAgent(private val workspaceDir: File) : BaseAgent {
    override val agentType: AgentType = AgentType.CODING

    init {
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }
    }

    override fun canHandle(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("code") || q.contains("python") || q.contains("kotlin") ||
                q.contains("javascript") || q.contains("function") || q.contains("script") ||
                q.contains("debug") || q.contains("sql")
    }

    override suspend fun execute(query: String, context: Map<String, String>): AgentExecutionResult {
        return AgentExecutionResult(
            agentType = agentType,
            success = true,
            planSummary = "Generate & Validate code inside workspace: ${workspaceDir.name}",
            outputText = "Sir, Coding Agent initialized. Workspace directory secured at ${workspaceDir.absolutePath}.\n" +
                    "Ready to write, lint, and repair Python, Kotlin, JS, and SQL scripts."
        )
    }
}
