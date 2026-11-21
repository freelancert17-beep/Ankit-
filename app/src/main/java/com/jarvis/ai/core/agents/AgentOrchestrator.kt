package com.jarvis.ai.core.agents

import android.content.Context
import com.jarvis.ai.core.automation.AppAutomationManager
import com.jarvis.ai.core.intelligence.GeminiApiClient
import com.jarvis.ai.core.intelligence.RuleEngine
import com.jarvis.ai.core.intelligence.WebSearchEngine
import com.jarvis.ai.core.memory.MemoryBrain
import com.jarvis.ai.core.tools.SheetAndDocGenerator
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
    private val memoryBrain = MemoryBrain(repository)
    private val automationManager = AppAutomationManager(context)
    private val sheetGenerator = SheetAndDocGenerator(context)
    private val webSearchEngine = WebSearchEngine()

    private val agents: List<BaseAgent> = listOf(
        CodingAgent(File(context.filesDir, "workspace")),
        OfficeAgent(),
        TradingAgent()
    )

    suspend fun routeAndExecute(
        query: String,
        history: List<Pair<String, String>> = emptyList(),
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): AgentExecutionResult {

        // 1. Long-Term Memory Extraction (Learn personal facts about Boss in background)
        memoryBrain.extractAndSaveMemory(query)

        // 2. Document & Spreadsheet Generator (Sheet, CSV, Marks, Budget, Expense)
        if (sheetGenerator.canHandle(query)) {
            val sheetRes = sheetGenerator.generateSheetOrDoc(query)
            if (sheetRes.success && sheetRes.file != null) {
                return AgentExecutionResult(
                    agentType = AgentType.OFFICE,
                    success = true,
                    planSummary = "Generate ${sheetRes.fileType} Document",
                    outputText = sheetRes.textMessage,
                    spokenText = sheetRes.voiceMessage,
                    generatedFilePath = sheetRes.file.absolutePath,
                    generatedFileName = sheetRes.file.name,
                    generatedFileType = sheetRes.fileType
                )
            }
        }

        // 3. Fast Hardware / Math Rule Engine (Torch, Battery, GST)
        val ruleResult = ruleEngine.matchAndExecute(query)
        if (ruleResult != null) {
            return ruleResult
        }

        // 4. Android Automations (WhatsApp, YouTube, Phone Calls, App Launch, Alarms)
        val automationResult = automationManager.tryHandleAutomation(query)
        if (automationResult != null) {
            return automationResult
        }

        // 5. Specialized Agents (Local Coding, Office formulas, Trading signals)
        for (agent in agents) {
            if (agent.canHandle(query) && imageBase64 == null) {
                val agentRes = agent.execute(query, emptyMap())
                if (!query.contains("?") && !query.lowercase().startsWith("kya") && !query.lowercase().startsWith("how")) {
                    return agentRes
                }
            }
        }

        // 6. Live Web Search (if query asks for current match, scores, news, weather)
        var liveSearchContext = ""
        if (webSearchEngine.isLiveSearchQuery(query)) {
            val findings = webSearchEngine.searchLiveWeb(query)
            if (findings != null) {
                liveSearchContext = findings
            }
        }

        // 7. Retrieve Long-Term Memory context to feed into Gemini
        val memoryContext = memoryBrain.getFormattedMemoryContext()

        // 8. Live Gemini Multimodal AI Engine
        val apiKey = prefs.geminiApiKey
        val aiResponse = geminiClient.generateAnswer(
            apiKey = apiKey,
            prompt = query,
            chatHistory = history,
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType,
            memoryContext = memoryContext,
            liveSearchContext = liveSearchContext
        )

        return AgentExecutionResult(
            agentType = AgentType.MAIN_JARVIS,
            success = true,
            planSummary = if (liveSearchContext.isNotBlank()) "JARVIS Neural Engine + Live Web" else "JARVIS Neural Engine",
            outputText = aiResponse
        )
    }
}
