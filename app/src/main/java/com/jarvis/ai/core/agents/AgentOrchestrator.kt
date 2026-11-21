package com.jarvis.ai.core.agents

import android.content.Context
import com.jarvis.ai.core.automation.AppAutomationManager
import com.jarvis.ai.core.intelligence.GeminiApiClient
import com.jarvis.ai.core.intelligence.OpenAiCompatibleClient
import com.jarvis.ai.core.intelligence.RuleEngine
import com.jarvis.ai.core.intelligence.WebSearchEngine
import com.jarvis.ai.core.memory.MemoryBrain
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

    suspend fun routeAndExecute(
        query: String,
        history: List<Pair<String, String>> = emptyList(),
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): AgentExecutionResult {

        // 1. Long-Term Personal Memory Extraction (Learn personal facts about Boss)
        memoryBrain.extractAndSaveMemory(query)

        // 2. Persona Greetings & Hardware Rules ("Jarvis kaise ho", Torch, Battery, GST)
        val ruleResult = ruleEngine.matchAndExecute(query)
        if (ruleResult != null) {
            return ruleResult
        }

        // 3. Autonomous Code, Program, Website, Automation & AI Agent Creator
        if (codeGenerator.canHandle(query)) {
            val codeRes = codeGenerator.generateCodeOrProject(query)
            if (codeRes.success && codeRes.file != null) {
                return AgentExecutionResult(
                    agentType = AgentType.CODING,
                    success = true,
                    planSummary = "Construct ${codeRes.fileType} for Boss",
                    outputText = codeRes.textMessage,
                    spokenText = codeRes.voiceMessage,
                    generatedFilePath = codeRes.file.absolutePath,
                    generatedFileName = codeRes.file.name,
                    generatedFileType = codeRes.fileType
                )
            }
        }

        // 4. Dynamic Excel & Document Spreadsheet Generator
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

        // 5. Android Automations (WhatsApp, YouTube, Phone Calls, App Launch, Alarms)
        val automationResult = automationManager.tryHandleAutomation(query)
        if (automationResult != null) {
            return automationResult
        }

        // 6. Live Web & Stock Market Financial Research
        var liveSearchContext = ""
        val isMarketQuery = query.lowercase().contains("share market") ||
                query.lowercase().contains("market") ||
                query.lowercase().contains("nifty") ||
                query.lowercase().contains("sensex") ||
                query.lowercase().contains("trading") ||
                query.lowercase().contains("stock")

        if (isMarketQuery || webSearchEngine.isLiveSearchQuery(query)) {
            val searchQuery = if (isMarketQuery) "$query Indian stock market Nifty today trend news" else query
            val findings = webSearchEngine.searchLiveWeb(searchQuery)
            if (findings != null) {
                liveSearchContext = findings
            }
        }

        // 7. Retrieve Personal Memory facts
        val memoryContext = memoryBrain.getFormattedMemoryContext()

        // 8. Financial Research Context Prompt enrichment if query is about share market
        val enrichedQuery = if (isMarketQuery) {
            """
            [Role: Dalal-Street & Wall-Street Senior Quantitative Financial Research Specialist]
            Boss query: $query
            
            Instructions:
            - Clearly explain whether the market is going up (Bullish), falling (Bearish), or consolidating (Sideways).
            - Give key levels (Nifty Support/Resistance) or specific stock analysis with price action.
            - Provide clear risk management and trading strategy recommendations.
            - Always conclude with: "Boss, kya aap kisi specific share (jaise Reliance, Tata, HDFC) ka deep-dive technical chart analysis chahte hain ya koi specific strategy banani hai? Aap batayein."
            """.trimIndent()
        } else {
            query
        }

        // 9. Multi-Provider AI Engine (Gemini, Groq, DeepSeek, OpenAI)
        val provider = prefs.selectedProvider
        val apiKey = prefs.getActiveApiKey()

        val aiResponse = if (provider == AiProvider.GEMINI) {
            geminiClient.generateAnswer(
                apiKey = apiKey,
                prompt = enrichedQuery,
                chatHistory = history,
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
                chatHistory = history,
                imageBase64 = imageBase64,
                imageMimeType = imageMimeType,
                memoryContext = memoryContext,
                liveSearchContext = liveSearchContext
            )
        }

        return AgentExecutionResult(
            agentType = if (isMarketQuery) AgentType.TRADING else AgentType.MAIN_JARVIS,
            success = true,
            planSummary = if (isMarketQuery) "Financial Market Technical Research" else if (liveSearchContext.isNotBlank()) "${provider.displayName} + Live Research" else "${provider.displayName} Core",
            outputText = aiResponse
        )
    }
}
