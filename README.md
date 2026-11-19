# JARVIS AI 5.0 — Autonomous Android Assistant

Offline-first, privacy-focused, multi-agent Android AI application engineered with Kotlin, Jetpack Compose, Room Database, MediaPipe GenAI / LiteRT, and Jetpack Glance.

---

## 🚀 Key Features

* **Three-Tier Intelligence Model:**
  * **Tier 1:** Instant Rule Engine (Torch, Battery, Volume, Fast GST calculations)
  * **Tier 2:** Multi-Agent Routing (Coding, Office, Trading, Automation)
  * **Tier 3:** On-device Gemma 2B (MediaPipe GenAI) & Cloud Fallback
* **Multi-Agent Architecture:**
  * `CodingAgent`: Sandboxed code generation & linting.
  * `OfficeAgent`: GST breakdowns (CGST + SGST), Excel/CSV workflows, and inventory tracking.
  * `TradingAgent`: RSI (14), MACD, Moving Averages with mandatory risk disclaimers.
  * `AutomationAgent`: Hardware toggles and Android integrations.
* **Offline-First Persistence:**
  * Room SQLite Database for chat messages, local memory, and task execution logs.
* **Jetpack Glance Widget:**
  * Interactive 1-tap launcher widget for Android home screen.
* **Natural Voice Engine:**
  * Indian English / Hindi locale speech synthesis with TextToSpeech.

---

## 🛠️ Project Structure

```
JarvisAI/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jarvis/ai/
│       │   ├── JarvisApp.kt
│       │   ├── MainActivity.kt
│       │   ├── core/
│       │   │   ├── agents/ (AgentOrchestrator, CodingAgent, OfficeAgent, TradingAgent)
│       │   │   ├── intelligence/ (RuleEngine)
│       │   │   └── voice/ (VoiceManager)
│       │   ├── data/
│       │   │   ├── local/ (JarvisDatabase, ChatDao, MemoryDao, Entities)
│       │   │   └── repository/ (JarvisRepository)
│       │   ├── ui/
│       │   │   ├── screens/ (MainChatScreen)
│       │   │   ├── theme/ (Color, Theme)
│       │   │   └── viewmodel/ (JarvisViewModel)
│       │   └── widget/ (JarvisVoiceWidget, JarvisVoiceWidgetReceiver)
│       └── res/
└── gradle/libs.versions.toml
```

---

## 📦 How to Build & Run

1. Open Android Studio (Ladybug / Koala or newer).
2. Click **Open Project** and select `JarvisAI`.
3. Allow Gradle to sync.
4. Run on a physical Android device (API 26+) or emulator.
