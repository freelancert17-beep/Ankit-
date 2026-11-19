package com.jarvis.ai.core.agents

class TradingAgent : BaseAgent {
    override val agentType: AgentType = AgentType.TRADING

    override fun canHandle(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("trade") || q.contains("trading") || q.contains("rsi") ||
                q.contains("macd") || q.contains("nifty") || q.contains("crypto") ||
                q.contains("stock") || q.contains("share market")
    }

    override suspend fun execute(query: String, context: Map<String, String>): AgentExecutionResult {
        val q = query.lowercase()

        // Risk warning disclaimer mandate as per Blueprint section 2.6
        val disclaimer = "\n\n⚠️ Disclaimer: Financial market research only. Not financial advice. Live trading execution requires explicit human confirmation."

        if (q.contains("rsi")) {
            return AgentExecutionResult(
                agentType = agentType,
                success = true,
                planSummary = "Compute Relative Strength Index (RSI - 14 Period)",
                outputText = "Sir, RSI (14) indicator module active.\n" +
                        "• Above 70: Typically indicates Overbought conditions (Caution on longs).\n" +
                        "• Below 30: Typically indicates Oversold conditions (Watch for reversal bounce).\n" +
                        "• Centerline 50: Momentum pivot.$disclaimer"
            )
        }

        if (q.contains("macd")) {
            return AgentExecutionResult(
                agentType = agentType,
                success = true,
                planSummary = "Compute Moving Average Convergence Divergence (MACD 12, 26, 9)",
                outputText = "Sir, MACD Indicator analysis ready.\n" +
                        "• Bullish Crossover: MACD line crosses above Signal line.\n" +
                        "• Bearish Crossover: MACD line crosses below Signal line.$disclaimer"
            )
        }

        return AgentExecutionResult(
            agentType = agentType,
            success = true,
            planSummary = "Market Technical Overview",
            outputText = "Sir, Trading Research Agent initialized. Ready to compute moving averages (SMA 20/50/200), Bollinger Bands, and support/resistance zones.$disclaimer"
        )
    }
}
