package com.jarvis.ai.core.agents

enum class AgentType(val displayName: String, val role: String) {
    MAIN_JARVIS("JARVIS Core", "Primary Orchestrator & Conversational AI"),
    CODING("Coding Agent", "Software Engineer & Code Debugger"),
    RESEARCH("Research Agent", "Information Verification & Synthesis"),
    OFFICE("Office Agent", "Excel, CSV, GST & MIS Manager"),
    TRADING("Trading Analyst", "Technical Indicators & Market Research"),
    AUTOMATION("Android Automation", "Hardware & App Controller")
}

data class AgentExecutionResult(
    val agentType: AgentType,
    val success: Boolean,
    val planSummary: String,
    val outputText: String,
    val spokenText: String? = null,
    val requiresUserApproval: Boolean = false,
    val pendingActionDescription: String? = null,
    val generatedFilePath: String? = null,
    val generatedFileName: String? = null,
    val generatedFileType: String? = null
)
