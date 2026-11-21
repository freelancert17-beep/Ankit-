package com.jarvis.ai.core.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.jarvis.ai.core.agents.AgentExecutionResult
import com.jarvis.ai.core.agents.AgentType

class AppAutomationManager(private val context: Context) {

    fun tryHandleAutomation(rawQuery: String): AgentExecutionResult? {
        val q = rawQuery.trim().lowercase()

        // 1. WhatsApp Automation ("WhatsApp par [message] bhejo", "send whatsapp message to [number] [msg]")
        if (q.contains("whatsapp")) {
            return handleWhatsApp(rawQuery)
        }

        // 2. YouTube Automation ("YouTube par [song/video] chalao / play karo")
        if (q.contains("youtube") || (q.contains("play") && (q.contains("song") || q.contains("gaana")))) {
            return handleYouTube(rawQuery)
        }

        // 3. Call & Phone Dialer ("call lagao [number]", "phone karo [number]")
        if (q.startsWith("call ") || q.contains("ko call") || q.contains("phone lagao") || q.contains("call lagao")) {
            return handleCall(rawQuery)
        }

        // 4. Alarm / Reminder ("alarm lagao 7 baje", "set alarm for 6 am")
        if (q.contains("alarm") || q.contains("reminder")) {
            return handleAlarm(rawQuery)
        }

        // 5. Open Apps ("open instagram", "open facebook", "open camera", "open settings", etc.)
        if (q.startsWith("open ") || q.startsWith("kholo ") || q.endsWith(" open karo") || q.endsWith(" kholo")) {
            val appResult = handleAppLaunch(rawQuery)
            if (appResult != null) return appResult
        }

        return null
    }

    private fun handleWhatsApp(query: String): AgentExecutionResult {
        // Extract message content
        var message = query
            .replace(Regex("""(?i)whatsapp\s*(?:par|pe|me|pr)?"""), "")
            .replace(Regex("""(?i)(?:message\s*(?:bhejo|send\s*karo|karo)|send)"""), "")
            .trim()

        if (message.isBlank()) {
            message = "Hello! Sent via JARVIS AI"
        }

        // Check if a 10-digit phone number is mentioned
        val phoneRegex = Regex("""\b(\d{10})\b""")
        val phoneMatch = phoneRegex.find(query)
        val phoneNumber = phoneMatch?.groupValues?.get(1)

        val intent = if (!phoneNumber.isNullOrBlank()) {
            val formatted = if (phoneNumber.startsWith("91")) phoneNumber else "91$phoneNumber"
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // General WhatsApp Share Intent
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return try {
            context.startActivity(intent)
            AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = true,
                planSummary = "Launch WhatsApp Intent",
                outputText = "Boss, WhatsApp message open kar diya hai: \"$message\""
            )
        } catch (e: Exception) {
            // Fallback: open WhatsApp in browser/store
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Launch WhatsApp Web Intent",
                    outputText = "Boss, WhatsApp web link open kar diya hai."
                )
            } catch (_: Exception) {
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = false,
                    planSummary = "WhatsApp Intent Failed",
                    outputText = "Sir, WhatsApp app device par install nahi mila."
                )
            }
        }
    }

    private fun handleYouTube(query: String): AgentExecutionResult {
        val cleanQuery = query
            .replace(Regex("""(?i)youtube\s*(?:par|pe|me|pr)?"""), "")
            .replace(Regex("""(?i)(?:play\s*karo|chalao|search\s*karo|play)"""), "")
            .replace(Regex("""(?i)(?:gaana|song|video)"""), "")
            .trim()

        val searchQuery = if (cleanQuery.isBlank()) "Top trending music" else cleanQuery

        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", searchQuery)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = true,
                planSummary = "Launch YouTube App Search",
                outputText = "Boss, YouTube par \"$searchQuery\" play kar raha hoon."
            )
        } catch (_: Exception) {
            // Fallback to browser YouTube URL
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(browserIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Launch YouTube Browser Search",
                    outputText = "Boss, browser me YouTube par \"$searchQuery\" open kar diya hai."
                )
            } catch (e: Exception) {
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = false,
                    planSummary = "YouTube Intent Failed",
                    outputText = "YouTube open karne mein samasya aayi Boss: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleCall(query: String): AgentExecutionResult {
        // Extract phone number if present
        val digitRegex = Regex("""(\d{10,12})""")
        val match = digitRegex.find(query)
        val number = match?.groupValues?.get(1)

        val intent = if (!number.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // Open dialer directly
            Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return try {
            context.startActivity(intent)
            AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = true,
                planSummary = "Launch Phone Dialer",
                outputText = if (!number.isNullOrBlank()) "Boss, $number par call karne ke liye Phone Dialer open kar diya hai." else "Boss, Phone Dialer open kar diya hai."
            )
        } catch (e: Exception) {
            AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = false,
                planSummary = "Phone Dialer Failed",
                outputText = "Call intent launch nahi ho saka Boss: ${e.localizedMessage}"
            )
        }
    }

    private fun handleAlarm(query: String): AgentExecutionResult {
        // Look for hour digits
        val hourRegex = Regex("""(\d{1,2})(?:\s*baje|\s*am|\s*pm|\s*hours)?""", RegexOption.IGNORE_CASE)
        val match = hourRegex.find(query)
        val hour = match?.groupValues?.get(1)?.toIntOrNull() ?: 7

        val isPm = query.lowercase().contains("pm") || query.lowercase().contains("shaam") || query.lowercase().contains("raat")
        val effectiveHour = if (isPm && hour < 12) hour + 12 else hour

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, effectiveHour)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alert")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = true,
                planSummary = "Configure Hardware Clock Alarm",
                outputText = "Boss, subah/shaam $hour baje ka alarm set kar diya hai."
            )
        } catch (_: Exception) {
            val showAlarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(showAlarmIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Open Clock Alarms Screen",
                    outputText = "Boss, Clock / Alarms app open kar diya hai."
                )
            } catch (e: Exception) {
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = false,
                    planSummary = "Alarm Action Failed",
                    outputText = "Alarm set nahi ho saka Boss: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleAppLaunch(query: String): AgentExecutionResult? {
        val q = query.lowercase()

        // Common apps mapping
        val appMap = mapOf(
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "fb" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "telegram" to "org.telegram.messenger",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "play store" to "com.android.vending",
            "calculator" to "com.google.android.calculator",
            "photos" to "com.google.android.apps.photos"
        )

        // Check explicit system targets first
        if (q.contains("camera")) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(cameraIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Launch Camera",
                    outputText = "Boss, Camera open kar diya hai."
                )
            } catch (_: Exception) { null }
        }

        if (q.contains("gallery") || q.contains("photos")) {
            val galleryIntent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(galleryIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Launch Photos/Gallery",
                    outputText = "Boss, Gallery open kar di hai."
                )
            } catch (_: Exception) { null }
        }

        if (q.contains("settings")) {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(settingsIntent)
                AgentExecutionResult(
                    agentType = AgentType.AUTOMATION,
                    success = true,
                    planSummary = "Launch System Settings",
                    outputText = "Boss, Phone Settings open kar di hain."
                )
            } catch (_: Exception) { null }
        }

        // Check mapped packages
        for ((appName, pkg) in appMap) {
            if (q.contains(appName)) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val capName = appName.replaceFirstChar { it.uppercase() }
                    return AgentExecutionResult(
                        agentType = AgentType.AUTOMATION,
                        success = true,
                        planSummary = "Launch $capName App",
                        outputText = "Boss, $capName app open kar diya hai."
                    )
                }
            }
        }

        return null
    }
}
