package com.jarvis.ai.core.reasoning

enum class UserIntent {
    GREETING,
    DEVICE_AUTOMATION,
    STOCK_RESEARCH,
    CODE_OR_PROJECT_GENERATION,
    SPREADSHEET_GENERATION,
    GENERAL_QUERY
}

data class PlanStep(
    val stepNumber: Int,
    val description: String,
    val targetAgent: String,
    var isCompleted: Boolean = false
)

data class ExecutionPlan(
    val intent: UserIntent,
    val goal: String,
    val steps: List<PlanStep>,
    val reflectionRequired: Boolean = false
)

class ReActEngine {

    /**
     * Intent Detection: Identifies user's true underlying goal.
     */
    fun detectIntent(query: String): UserIntent {
        val q = query.trim().lowercase()

        return when {
            q.contains("kaise ho") || q.contains("kya haal") || q.contains("how are you") ||
                    q == "hello jarvis" || q == "hi jarvis" || q == "hey jarvis" -> UserIntent.GREETING

            q.contains("sheet") || q.contains("excel") || q.contains("csv") ||
                    q.contains("marksheet") || q.contains("budget") || q.contains("expense") -> UserIntent.SPREADSHEET_GENERATION

            q.contains("website") || q.contains("program") || q.contains("python script") ||
                    q.contains("ai agent") || q.contains("automation script") ||
                    (q.contains("code") && (q.contains("banao") || q.contains("write") || q.contains("create"))) -> UserIntent.CODE_OR_PROJECT_GENERATION

            q.contains("share market") || q.contains("nifty") || q.contains("sensex") ||
                    q.contains("stock") || q.contains("trading") || q.contains("market gir raha") ||
                    q.contains("market upar") || q.contains("strategy") -> UserIntent.STOCK_RESEARCH

            q.contains("whatsapp") || q.contains("youtube") || q.contains("call") ||
                    q.contains("alarm") || q.contains("torch") || q.contains("battery") ||
                    q.startsWith("open ") || q.startsWith("kholo ") -> UserIntent.DEVICE_AUTOMATION

            else -> UserIntent.GENERAL_QUERY
        }
    }

    /**
     * Task Decomposition: Breaks down the goal into an actionable ReAct plan.
     */
    fun decomposeTask(intent: UserIntent, query: String): ExecutionPlan {
        val steps = when (intent) {
            UserIntent.GREETING -> listOf(
                PlanStep(1, "Acknowledge Boss with respectful loyal persona", "RuleEngine")
            )

            UserIntent.SPREADSHEET_GENERATION -> listOf(
                PlanStep(1, "Parse user domain and schema requirements", "SheetGenerator"),
                PlanStep(2, "Generate formatted CSV with realistic data rows", "SheetGenerator"),
                PlanStep(3, "Verify file integrity and produce download card", "CriticEngine")
            )

            UserIntent.CODE_OR_PROJECT_GENERATION -> listOf(
                PlanStep(1, "Architect project structure and framework", "CodingAgent"),
                PlanStep(2, "Write clean semantic code without placeholders", "CodingAgent"),
                PlanStep(3, "Save file to workspace and formulate proactive follow-up", "CriticEngine")
            )

            UserIntent.STOCK_RESEARCH -> listOf(
                PlanStep(1, "Scrape live market updates and sentiment via DuckDuckGo", "WebSearchEngine"),
                PlanStep(2, "Synthesize price action, RSI, support/resistance levels", "TradingAgent"),
                PlanStep(3, "Formulate actionable strategy and stock deep-dive offer", "TradingAgent")
            )

            UserIntent.DEVICE_AUTOMATION -> listOf(
                PlanStep(1, "Parse hardware or Android package targets", "AutomationManager"),
                PlanStep(2, "Dispatch native Android Intent safely", "AutomationManager")
            )

            UserIntent.GENERAL_QUERY -> listOf(
                PlanStep(1, "Retrieve personal long-term memories for Boss", "MemoryBrain"),
                PlanStep(2, "Consult LLM Neural Engine for intelligent synthesis", "AiClient"),
                PlanStep(3, "Format response with proactive follow-up", "CriticEngine")
            )
        }

        return ExecutionPlan(
            intent = intent,
            goal = query,
            steps = steps,
            reflectionRequired = intent == UserIntent.CODE_OR_PROJECT_GENERATION || intent == UserIntent.SPREADSHEET_GENERATION
        )
    }

    /**
     * Reflection & Self-Correction: Validates generated output before final dispatch.
     */
    fun verifyAndReflect(outputText: String, expectedFileType: String?): Boolean {
        if (outputText.isBlank()) return false
        if (expectedFileType != null && !outputText.contains(expectedFileType, ignoreCase = true) && !outputText.contains("File", ignoreCase = true)) {
            return false
        }
        return true
    }
}
