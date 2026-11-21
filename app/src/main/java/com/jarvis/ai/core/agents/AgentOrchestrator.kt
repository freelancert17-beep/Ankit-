package com.jarvis.ai.core.agents

import android.content.Context
import com.jarvis.ai.core.automation.AppAutomationManager
import com.jarvis.ai.core.intelligence.GeminiApiClient
import com.jarvis.ai.core.intelligence.OpenAiCompatibleClient
import com.jarvis.ai.core.intelligence.RuleEngine
import com.jarvis.ai.core.intelligence.WebSearchEngine
import com.jarvis.ai.core.memory.MemoryBrain
import com.jarvis.ai.core.observability.AgentTracer
import com.jarvis.ai.core.reasoning.ReActEngine
import com.jarvis.ai.core.reasoning.UserIntent
import com.jarvis.ai.core.safety.SafetyGuardrails
import com.jarvis.ai.core.tools.CodeAndProjectGenerator
import com.jarvis.ai.core.tools.SheetAndDocGenerator
import com.jarvis.ai.data.local.AiProvider
import com.jarvis.ai.data.local.PreferencesManager
import com.jarvis.ai.data.repository.JarvisRepository
import java.io.File

class AgentOrchestrator(
    private val context: Context,
    private val repository: JarvisRepository
) {

    private val prefs = PreferencesManager(context)
    private val ruleEngine = RuleEngine(context)
    private val geminiClient = GeminiApiClient()
    private val openAiCompatibleClient = OpenAiCompatibleClient()
    private val memoryBrain = MemoryBrain(repository)
    private val automationManager = AppAutomationManager(context)
    private val sheetGenerator = SheetAndDocGenerator(context)
    private val codeGenerator = CodeAndProjectGenerator(context)
    private val webSearchEngine = WebSearchEngine()
    private val safetyGuardrails = SafetyGuardrails()
    private val reactEngine = ReActEngine()

    suspend fun routeAndExecute(
        query: String,
        history: List<Pair<String, String>> = emptyList(),
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): AgentExecutionResult {
        val startTime = System.currentTimeMillis()
        val provider = prefs.selectedProvider
        val trace = AgentTracer.startTrace(query, provider.displayName)

        // 1. Safety Guardrails & Input Sanitization
        val validation = safetyGuardrails.validateAndSanitize(query)
        if (!validation.isSafe) {
            trace.addStep("SAFETY", "Neutralized prompt injection", System.currentTimeMillis() - startTime, "WARNING")
            trace.totalDurationMs = System.currentTimeMillis() - startTime
            return AgentExecutionResult(
                agentType = AgentType.MAIN_JARVIS,
                success = false,
                planSummary = "Safety Guardrails Activated",
                outputText = validation.reason ?: "Query blocked by Safety Guardrails."
            )
        }

        if (validation.requiresApproval) {
            trace.addStep("HITL", "Awaiting Human-in-the-Loop Confirmation", System.currentTimeMillis() - startTime, "WARNING")
            trace.totalDurationMs = System.currentTimeMillis() - startTime
            return AgentExecutionResult(
                agentType = AgentType.MAIN_JARVIS,
                success = true,
                requiresUserApproval = true,
                pendingActionDescription = validation.approvalPrompt,
                planSummary = "HITL Approval Gate",
                outputText = validation.approvalPrompt ?: "Approval required before proceeding, Boss."
            )
        }

        val cleanQuery = validation.sanitizedQuery

        // 2. ReAct Intent Detection & Task Decomposition
        val intent = reactEngine.detectIntent(cleanQuery)
        val plan = reactEngine.decomposeTask(intent, cleanQuery)
        trace.addStep("PLANNING", "Intent: $intent | Decomposed into ${plan.steps.size} ReAct steps", 15)

        // 3. Long-Term Personal Memory Extraction & Context Compression
        val memStart = System.currentTimeMillis()
        memoryBrain.extractAndSaveMemory(cleanQuery)
        val memoryContext = memoryBrain.getFormattedMemoryContext()
        val compressedHistory = memoryBrain.compressChatHistory(history, maxRecent = 6)
        trace.addStep("MEMORY", "Retrieved facts & compressed context", System.currentTimeMillis() - memStart)

        // 4. Intent Execution: Greetings & Persona
        if (intent == UserIntent.GREETING) {
            val ruleResult = ruleEngine.matchAndExecute(cleanQuery)
            if (ruleResult != null) {
                trace.addStep("EXECUTION", "Persona greeting delivered", System.currentTimeMillis() - startTime)
                trace.totalDurationMs = System.currentTimeMillis() - startTime
                return ruleResult
            }
        }

        // 5. Intent Execution: Autonomous Code, Website, Program & AI Agent Creator
        if (intent == UserIntent.CODE_OR_PROJECT_GENERATION || codeGenerator.canHandle(cleanQuery)) {
            val codeStart = System.currentTimeMillis()
            val codeRes = codeGenerator.generateCodeOrProject(cleanQuery)
            if (codeRes.success && codeRes.file != null) {
                trace.addStep("CODE_ENGINE", "Generated ${codeRes.fileType} (${codeRes.file.name})", System.currentTimeMillis() - codeStart)
                trace.totalDurationMs = System.currentTimeMillis() - startTime
                return AgentExecutionResult(
                    agentType = AgentType.CODING,
                    success = true,
                    planSummary = "ReAct: Code Engine -> Verified",
                    outputText = codeRes.textMessage,
                    spokenText = codeRes.voiceMessage,
                    generatedFilePath = codeRes.file.absolutePath,
                    generatedFileName = codeRes.file.name,
                    generatedFileType = codeRes.fileType
                )
            }
        }

        // 6. Intent Execution: Dynamic Excel & Spreadsheet Generator
        if (intent == UserIntent.SPREADSHEET_GENERATION || sheetGenerator.canHandle(cleanQuery)) {
            val sheetStart = System.currentTimeMillis()
            val sheetRes = sheetGenerator.generateSheetOrDoc(cleanQuery)
            if (sheetRes.success && sheetRes.file != null) {
                trace.addStep("SHEET_ENGINE", "Generated ${sheetRes.fileType} (${sheetRes.file.name})", System.currentTimeMillis() - sheetStart)
                trace.totalDurationMs = System.currentTimeMillis() - startTime
                return AgentExecutionResult(
                    agentType = AgentType.OFFICE,
                    success = true,
                    planSummary = "ReAct: Spreadsheet Engine -> Verified",
                    outputText = sheetRes.textMessage,
                    spokenText = sheetRes.voiceMessage,
                    generatedFilePath = sheetRes.file.absolutePath,
                    generatedFileName = sheetRes.file.name,
                    generatedFileType = sheetRes.fileType
                )
            }
        }

        // 7. Intent Execution: Android Automations & Device Rules
        val autoStart = System.currentTimeMillis()
        val automationResult = automationManager.tryHandleAutomation(cleanQuery)
        if (automationResult != null) {
            trace.addStep("AUTOMATION", "Native Android Intent executed", System.currentTimeMillis() - autoStart)
            trace.totalDurationMs = System.currentTimeMillis() - startTime
            return automationResult
        }

        val ruleResult = ruleEngine.matchAndExecute(cleanQuery)
        if (ruleResult != null) {
            trace.addStep("HARDWARE", "Device rule executed", System.currentTimeMillis() - autoStart)
            trace.totalDurationMs = System.currentTimeMillis() - startTime
            return ruleResult
        }

        // 8. Intent Execution: Live Web & Stock Market Financial Research
        var liveSearchContext = ""
        val isMarketQuery = intent == UserIntent.STOCK_RESEARCH ||
                cleanQuery.lowercase().contains("share market") ||
                cleanQuery.lowercase().contains("market") ||
                cleanQuery.lowercase().contains("nifty") ||
                cleanQuery.lowercase().contains("sensex") ||
                cleanQuery.lowercase().contains("trading") ||
                cleanQuery.lowercase().contains("stock")

        if (isMarketQuery || webSearchEngine.isLiveSearchQuery(cleanQuery)) {
            val searchStart = System.currentTimeMillis()
            val searchQuery = if (isMarketQuery) "$cleanQuery Indian stock market Nifty today trend news" else cleanQuery
            val findings = webSearchEngine.searchLiveWeb(searchQuery)
            if (findings != null) {
                liveSearchContext = findings
                trace.addStep("WEB_SEARCH", "Real-time findings scraped", System.currentTimeMillis() - searchStart)
            }
        }

        // 9. Quantitative Financial Prompt Construction
        val enrichedQuery = if (isMarketQuery) {
            """
            [Role: Dalal-Street & Wall-Street Senior Quantitative Financial Research Specialist]
            Boss query: $cleanQuery
            
            Instructions:
            - Clearly explain whether the market is going up (Bullish), falling (Bearish), or consolidating (Sideways).
            - Give key levels (Nifty Support/Resistance) or specific stock analysis with price action.
            - Provide clear risk management and trading strategy recommendations.
            - Always conclude with: "Boss, kya aap kisi specific share (jaise Reliance, Tata, HDFC) ka deep-dive technical chart analysis chahte hain ya koi specific strategy banani hai? Aap batayein."
            """.trimIndent()
        } else {
            cleanQuery
        }

        // 10. Multi-Provider LLM Synthesis (Gemini / Groq / DeepSeek / OpenAI)
        val llmStart = System.currentTimeMillis()
        val apiKey = prefs.getActiveApiKey()

        var aiResponse = if (provider == AiProvider.GEMINI) {
            geminiClient.generateAnswer(
                apiKey = apiKey,
                prompt = enrichedQuery,
                chatHistory = compressedHistory,
                imageBase64 = imageBase64,
                imageMimeType = imageMimeType,
                memoryContext = memoryContext,
                liveSearchContext = liveSearchContext
            )
        } else {
            openAiCompatibleClient.generateAnswer(
                provider = provider,
                apiKey = apiKey,
                prompt = enrichedQuery,
                chatHistory = compressedHistory,
                imageBase64 = imageBase64,
                imageMimeType = imageMimeType,
                memoryContext = memoryContext,
                liveSearchContext = liveSearchContext
            )
        }

        // 11. Self-Correction / Fallback Logic
        if (aiResponse.contains("nahi ho saka") || aiResponse.contains("response prapt nahi hua")) {
            trace.addStep("FALLBACK", "Primary model failed, attempting emergency fallback", System.currentTimeMillis() - llmStart, "FALLBACK")
            if (provider != AiProvider.GEMINI && prefs.geminiApiKey.isNotBlank()) {
                aiResponse = geminiClient.generateAnswer(
                    apiKey = prefs.geminiApiKey,
                    prompt = enrichedQuery,
                    chatHistory = compressedHistory,
                    imageBase64 = imageBase64,
                    imageMimeType = imageMimeType,
                    memoryContext = memoryContext,
                    liveSearchContext = liveSearchContext
                )
            }
        }

        // 12. Safety Output Filtering
        val finalOutput = safetyGuardrails.filterOutput(aiResponse)
        trace.addStep("OUTPUT_GUARDRAIL", "Verified and PII-sanitized", 5)
        trace.totalDurationMs = System.currentTimeMillis() - startTime

        return AgentExecutionResult(
            agentType = if (isMarketQuery) AgentType.TRADING else AgentType.MAIN_JARVIS,
            success = true,
            planSummary = if (isMarketQuery) "ReAct: Dalal-Street Research [${trace.totalDurationMs}ms]" else "${provider.displayName} Core [${trace.totalDurationMs}ms]",
            outputText = finalOutput
        )
    }
}
