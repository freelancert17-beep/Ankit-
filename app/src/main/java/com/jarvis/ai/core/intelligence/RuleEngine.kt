package com.jarvis.ai.core.intelligence

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import com.jarvis.ai.core.agents.AgentExecutionResult
import com.jarvis.ai.core.agents.AgentType

class RuleEngine(private val context: Context) {

    private var isTorchOn = false

    fun matchAndExecute(rawQuery: String): AgentExecutionResult? {
        val q = rawQuery.trim().lowercase()

        // 1. Flashlight / Torch
        if (q.contains("torch on") || q.contains("flashlight on") || q.contains("light on") || q.contains("टॉर्च चालू")) {
            val res = toggleTorch(true)
            return AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = res,
                planSummary = "Turn on device camera flashlight",
                outputText = if (res) "Sir, flashlight has been turned ON." else "Unable to activate flashlight, Sir."
            )
        }
        if (q.contains("torch off") || q.contains("flashlight off") || q.contains("light off") || q.contains("टॉर्च बंद")) {
            val res = toggleTorch(false)
            return AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = res,
                planSummary = "Turn off device camera flashlight",
                outputText = if (res) "Sir, flashlight has been turned OFF." else "Unable to turn off flashlight, Sir."
            )
        }

        // 2. Battery Status
        if (q.contains("battery") || q.contains("चार्ज") || q.contains("बैटरी")) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return AgentExecutionResult(
                agentType = AgentType.AUTOMATION,
                success = true,
                planSummary = "Query hardware BatteryManager",
                outputText = "Sir, your phone's battery level is currently at $level%."
            )
        }

        // 3. Quick GST Calculator (e.g. "gst 18% on 5000" or "5000 par 18% gst")
        val gstRegex = Regex("""(?:gst\s*)?(\d+(?:\.\d+)?)\s*(?:par|on|pe)?\s*(\d+(?:\.\d+)?)\s*%?""")
        val match = gstRegex.find(q)
        if (q.contains("gst") && match != null) {
            val val1 = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val val2 = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val (amount, rate) = if (val1 > val2) Pair(val1, val2) else Pair(val2, val1)
            val gstAmount = (amount * rate) / 100.0
            val cgst = gstAmount / 2.0
            val sgst = gstAmount / 2.0
            val total = amount + gstAmount
            return AgentExecutionResult(
                agentType = AgentType.OFFICE,
                success = true,
                planSummary = "Compute GST breakdown (CGST + SGST)",
                outputText = "Sir, GST Breakdown for ₹$amount @ $rate%:\n• CGST (Half): ₹$cgst\n• SGST (Half): ₹$sgst\n• Total GST: ₹$gstAmount\n• Grand Total: ₹$total"
            )
        }

        return null
    }

    private fun toggleTorch(enable: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            true
        } catch (e: Exception) {
            false
        }
    }
}
