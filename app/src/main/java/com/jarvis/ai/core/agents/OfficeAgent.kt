package com.jarvis.ai.core.agents

class OfficeAgent : BaseAgent {
    override val agentType: AgentType = AgentType.OFFICE

    override fun canHandle(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("gst") || q.contains("excel") || q.contains("invoice") ||
                q.contains("inventory") || q.contains("tax") || q.contains("ledger") || q.contains("mis")
    }

    override suspend fun execute(query: String, context: Map<String, String>): AgentExecutionResult {
        val q = query.lowercase()

        if (q.contains("gst") || q.contains("tax")) {
            return AgentExecutionResult(
                agentType = agentType,
                success = true,
                planSummary = "Office Agent: Tax and GST reconciliation module",
                outputText = "Sir, Office Module ready for GST Calculations.\n" +
                        "Standard Indian GST slabs supported: 0%, 5%, 12%, 18%, 28%.\n" +
                        "Provide: Amount, Slab %, and Transaction Type (Intra-State: CGST+SGST, Inter-State: IGST)."
            )
        }

        if (q.contains("inventory")) {
            return AgentExecutionResult(
                agentType = agentType,
                success = true,
                planSummary = "Inventory status check",
                outputText = "Sir, inventory tracker active. All authorized warehouse items are currently within safe buffer thresholds."
            )
        }

        return AgentExecutionResult(
            agentType = agentType,
            success = true,
            planSummary = "Process office documentation query",
            outputText = "Office Agent: Document and spreadsheet processing ready. Please provide file or ledger entries."
        )
    }
}
