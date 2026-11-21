package com.jarvis.ai.core.observability

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TraceStep(
    val phase: String, // INTENT, PLANNING, TOOL_EXECUTION, LLM_CALL, VERIFICATION
    val description: String,
    val durationMs: Long = 0,
    val status: String = "SUCCESS" // SUCCESS, WARNING, ERROR, FALLBACK
)

data class ExecutionTrace(
    val traceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val steps: MutableList<TraceStep> = mutableListOf(),
    var totalDurationMs: Long = 0,
    var modelUsed: String = "JARVIS Core",
    var status: String = "COMPLETED"
) {
    fun addStep(phase: String, description: String, durationMs: Long = 0, status: String = "SUCCESS") {
        steps.add(TraceStep(phase, description, durationMs, status))
    }

    fun toFormattedSummary(): String {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val sb = StringBuilder()
        sb.append("⚡ TRACE [$timeStr | ${totalDurationMs}ms | $modelUsed]\n")
        for (step in steps) {
            val statusIcon = if (step.status == "SUCCESS") "✓" else if (step.status == "FALLBACK") "↩" else "⚠"
            sb.append(" $statusIcon [${step.phase}] ${step.description} (${step.durationMs}ms)\n")
        }
        return sb.toString().trim()
    }
}

object AgentTracer {

    private val traces = mutableListOf<ExecutionTrace>()
    private const val MAX_TRACE_HISTORY = 50

    fun startTrace(query: String, model: String): ExecutionTrace {
        val trace = ExecutionTrace(
            traceId = "tr_${System.currentTimeMillis() % 100000}",
            query = query,
            modelUsed = model
        )
        synchronized(traces) {
            if (traces.size >= MAX_TRACE_HISTORY) {
                traces.removeAt(0)
            }
            traces.add(trace)
        }
        return trace
    }

    fun getRecentTraces(): List<ExecutionTrace> {
        return synchronized(traces) { traces.toList() }
    }
}
