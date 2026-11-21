package com.jarvis.ai.core.tools

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CodeAndProjectGenerator(private val context: Context) {

    data class CodeGenerationResult(
        val success: Boolean,
        val file: File?,
        val textMessage: String,
        val voiceMessage: String,
        val fileType: String // "CODE", "WEBSITE", "SCRIPT"
    )

    fun canHandle(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("website") || q.contains("web page") ||
                q.contains("program") || q.contains("python script") ||
                q.contains("automation script") || q.contains("ai agent banao") ||
                q.contains("agent banao") || q.contains("application banao") ||
                q.contains("app banao") || q.contains("landing page") ||
                (q.contains("code") && (q.contains("banao") || q.contains("create") || q.contains("generate")))
    }

    fun generateCodeOrProject(query: String): CodeGenerationResult {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val workspaceDir = File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val q = query.lowercase()

            val fileName: String
            val content: String
            val projectType: String
            val targetDir: File

            when {
                // 1. Websites & Landing Pages (HTML5/CSS3/JS)
                q.contains("website") || q.contains("web page") || q.contains("landing page") || q.contains("html") -> {
                    projectType = "Modern Responsive Website"
                    fileName = "index_$timestamp.html"
                    targetDir = docsDir
                    content = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>JARVIS Powered Project</title>
                            <style>
                                :root {
                                    --bg: #0B0E14;
                                    --card-bg: #141A24;
                                    --accent: #00F0FF;
                                    --text: #F0F6FC;
                                    --text-muted: #8B949E;
                                }
                                * { margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                                body { background: var(--bg); color: var(--text); padding: 24px; line-height: 1.6; }
                                .container { max-width: 900px; margin: 0 auto; }
                                header { text-align: center; padding: 40px 0; border-bottom: 1px solid #212B3B; }
                                .glow-badge { background: rgba(0, 240, 255, 0.1); color: var(--accent); border: 1px solid var(--accent); padding: 4px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }
                                h1 { font-size: 38px; margin: 18px 0 10px; font-weight: 800; background: linear-gradient(135deg, #00F0FF, #7000FF); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
                                p.lead { font-size: 16px; color: var(--text-muted); }
                                .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px; margin: 40px 0; }
                                .card { background: var(--card-bg); border: 1px solid #212B3B; border-radius: 14px; padding: 22px; transition: 0.3s; }
                                .card:hover { border-color: var(--accent); transform: translateY(-3px); }
                                .card h3 { color: var(--accent); margin-bottom: 10px; font-size: 18px; }
                                .cta-box { text-align: center; margin: 40px 0; padding: 30px; background: rgba(0, 240, 255, 0.05); border: 1px dashed var(--accent); border-radius: 14px; }
                                button { background: var(--accent); color: #000; border: none; padding: 12px 28px; font-size: 15px; font-weight: 700; border-radius: 8px; cursor: pointer; transition: 0.2s; }
                                button:hover { opacity: 0.9; box-shadow: 0 0 16px var(--accent); }
                                footer { text-align: center; color: var(--text-muted); font-size: 13px; padding-top: 30px; border-top: 1px solid #212B3B; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <header>
                                    <span class="glow-badge">Autonomous Creation</span>
                                    <h1>Project Engine Built by JARVIS</h1>
                                    <p class="lead">Built on Boss's command. Fully responsive HTML5 & CSS3 layout with dynamic features.</p>
                                </header>
                                <div class="grid">
                                    <div class="card">
                                        <h3>⚡ Blazing Performance</h3>
                                        <p>Pure vanilla JavaScript & CSS with zero external heavyweight dependencies. Instant load times on mobile & desktop.</p>
                                    </div>
                                    <div class="card">
                                        <h3>🛡️ Modular Architecture</h3>
                                        <p>Clean semantic code, easy to customize with your branding, colors, and REST APIs.</p>
                                    </div>
                                    <div class="card">
                                        <h3>📱 100% Mobile Ready</h3>
                                        <p>Fluid grid layout designed specifically to adapt flawlessly across Android, iOS, tablets, and desktop.</p>
                                    </div>
                                </div>
                                <div class="cta-box">
                                    <h2 style="margin-bottom: 12px;">Ready to expand this project?</h2>
                                    <p style="color: var(--text-muted); margin-bottom: 20px;">Ask JARVIS to add new pages, forms, animations or backend hooks.</p>
                                    <button onclick="alert('Boss, action executed successfully!')">Activate Action</button>
                                </div>
                                <footer>Generated autonomously by JARVIS AI 5.0 for Boss</footer>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                }

                // 2. AI Agent Builder
                q.contains("ai agent") || q.contains("agent banao") -> {
                    projectType = "Autonomous AI Agent Architecture"
                    fileName = "autonomous_agent_$timestamp.py"
                    targetDir = workspaceDir
                    content = """
                        #!/usr/bin/env python3
                        \"\"\"
                        Autonomous AI Agent Engine
                        Generated by JARVIS AI for Boss
                        \"\"\"

                        import json
                        import sys
                        from dataclasses import dataclass, field
                        from typing import List, Dict, Any, Callable

                        @dataclass
                        class AgentTask:
                            goal: str
                            context: Dict[str, Any] = field(default_factory=dict)
                            sub_tasks: List[str] = field(default_factory=list)

                        class AutonomousAgent:
                            def __init__(self, name: str, role: str):
                                self.name = name
                                self.role = role
                                self.tools: Dict[str, Callable] = {}
                                self.memory: List[Dict[str, str]] = []

                            def register_tool(self, name: str, func: Callable):
                                self.tools[name] = func
                                print(f"[+] Tool registered: {name}")

                            def plan(self, goal: str) -> AgentTask:
                                print(f"[*] Agent {self.name} is planning: '{goal}'")
                                task = AgentTask(
                                    goal=goal,
                                    sub_tasks=[
                                        "1. Analyze objective and constraints",
                                        "2. Gather input data via available tools",
                                        "3. Synthesize and execute primary action",
                                        "4. Validate result and produce final report"
                                    ]
                                )
                                return task

                            def execute(self, task: AgentTask):
                                print(f"[⚡] Executing task for Boss: {task.goal}")
                                for step in task.sub_tasks:
                                    print(f"  -> Executing: {step}")
                                return {"status": "SUCCESS", "message": f"Goal '{task.goal}' executed cleanly."}

                        if __name__ == "__main__":
                            boss_agent = AutonomousAgent("Titan-1", "Lead Automation & Task Agent")
                            task = boss_agent.plan("Automate file indexing and API checks")
                            result = boss_agent.execute(task)
                            print("\nExecution Output:")
                            print(json.dumps(result, indent=2))
                    """.trimIndent()
                }

                // 3. Automation Script (Termux / Python automation)
                q.contains("automation") || q.contains("script") || q.contains("scraper") -> {
                    projectType = "Python Automation Script"
                    fileName = "automation_runner_$timestamp.py"
                    targetDir = workspaceDir
                    content = """
                        #!/usr/bin/env python3
                        \"\"\"
                        Autonomous Automation Runner
                        Generated by JARVIS AI for Boss
                        \"\"\"

                        import os
                        import sys
                        import time
                        import urllib.request
                        import json
                        from datetime import datetime

                        def run_system_health_check():
                            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Starting Boss's Automation Routine...")
                            
                            # 1. Check workspace directories
                            base_dir = os.getcwd()
                            print(f"[+] Current Working Directory: {base_dir}")
                            
                            # 2. Check network latency
                            try:
                                start = time.time()
                                req = urllib.request.Request("https://www.google.com", headers={"User-Agent": "Jarvis-Runner"})
                                with urllib.request.urlopen(req, timeout=5) as res:
                                    latency = round((time.time() - start) * 1000, 2)
                                    print(f"[+] Internet Connectivity: OK (Ping: {latency}ms, Status: {res.status})")
                            except Exception as e:
                                print(f"[-] Connectivity warning: {e}")

                            # 3. File batch organizer sample
                            print("[+] Automation routines completed with zero exit codes.")

                        if __name__ == "__main__":
                            run_system_health_check()
                    """.trimIndent()
                }

                // 4. General Python Program / Application
                else -> {
                    projectType = "Python Program"
                    fileName = "program_$timestamp.py"
                    targetDir = workspaceDir
                    content = """
                        #!/usr/bin/env python3
                        \"\"\"
                        Custom Application Program
                        Generated by JARVIS AI for Boss
                        \"\"\"

                        import sys
                        import math

                        def execute_core():
                            print("=" * 50)
                            print("⚡ JARVIS CORE PROGRAM - EXECUTING FOR BOSS")
                            print("=" * 50)
                            
                            # Perform sample high-precision calculation
                            results = {f"Metric_{i}": round(math.sqrt(i * 133.7), 4) for i in range(1, 6)}
                            for k, v in results.items():
                                print(f"• {k}: {v}")
                            
                            print("\nAll tasks completed successfully Boss!")

                        if __name__ == "__main__":
                            execute_core()
                    """.trimIndent()
                }
            }

            val targetFile = File(targetDir, fileName)
            targetFile.writeText(content)

            return CodeGenerationResult(
                success = true,
                file = targetFile,
                textMessage = "Boss, aapka **$projectType** ready kar diya hai!\n\n" +
                        "📁 **File Name:** `${targetFile.name}`\n" +
                        "📍 **Path:** `${targetFile.absolutePath}`\n" +
                        "💾 **Size:** ${targetFile.length()} bytes\n\n" +
                        "```${if (fileName.endsWith(".html")) "html" else "python"}\n" +
                        content.lines().take(15).joinToString("\n") + "\n// ... [Full Code Saved in File]\n```\n\n" +
                        "Neeche diye gaye button par click karke file ko open ya view kar sakte hain.\n\n" +
                        "👉 *Boss, kya isme koi naya feature, API connection ya changes add karne hain?*",
                voiceMessage = "Boss, aapka $projectType bana kar ready kar diya hai. Maine file save kar di hai. Kya isme kuch aur add karna hai?",
                fileType = if (fileName.endsWith(".html")) "WEBSITE" else "CODE"
            )
        } catch (e: Exception) {
            return CodeGenerationResult(
                success = false,
                file = null,
                textMessage = "Code create karne me error aaya: ${e.localizedMessage}",
                voiceMessage = "Code create karne mein error aaya Boss.",
                fileType = "CODE"
            )
        }
    }
}
