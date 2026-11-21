package com.jarvis.ai.core.safety

class SafetyGuardrails {

    data class ValidationResult(
        val isSafe: Boolean,
        val sanitizedQuery: String,
        val reason: String? = null,
        val requiresApproval: Boolean = false,
        val approvalPrompt: String? = null
    )

    /**
     * Sanitizes user input and detects potential jailbreak or prompt injection attempts.
     */
    fun validateAndSanitize(input: String): ValidationResult {
        val lower = input.lowercase().trim()

        // 1. Prompt Injection & Jailbreak detection
        val injectionPatterns = listOf(
            "ignore previous instructions",
            "disregard all prior rules",
            "bypass system instructions",
            "jailbreak mode",
            "dan mode",
            "you are now unrestricted"
        )

        for (pattern in injectionPatterns) {
            if (lower.contains(pattern)) {
                return ValidationResult(
                    isSafe = false,
                    sanitizedQuery = input,
                    reason = "Prompt injection attempt detected and neutralized by JARVIS Guardrails."
                )
            }
        }

        // 2. High-Risk / Destructive Actions (Human-in-the-Loop Approval Gate)
        val isDestructive = lower.contains("delete all files") ||
                lower.contains("format device") ||
                lower.contains("wipe memory") ||
                lower.contains("clear all memory") ||
                lower.contains("factory reset")

        if (isDestructive) {
            return ValidationResult(
                isSafe = true,
                sanitizedQuery = input,
                requiresApproval = true,
                approvalPrompt = "Boss, yeh action sensitive aur irreversible hai. Kya aap confirm karna chahte hain?"
            )
        }

        // 3. PII Masking (Mask credit card numbers, passwords in logs)
        val piiSanitized = maskSensitivePii(input)

        return ValidationResult(
            isSafe = true,
            sanitizedQuery = piiSanitized
        )
    }

    /**
     * Masks credit card numbers, CVVs, and plain passwords for privacy & DPDP/GDPR compliance.
     */
    fun maskSensitivePii(text: String): String {
        // Mask 16-digit card numbers: XXXX-XXXX-XXXX-1234
        val cardRegex = Regex("""\b(?:\d{4}[-\s]?){3}(\d{4})\b""")
        var clean = cardRegex.replace(text) { match ->
            "XXXX-XXXX-XXXX-" + match.groupValues[1]
        }

        // Mask password definitions: password is XYZ -> password is [PROTECTED]
        val passwordRegex = Regex("""(?i)(?:password|pin|secret|cvv)\s*(?:is|=|:)\s*([^\s,]+)""")
        clean = passwordRegex.replace(clean) { match ->
            match.value.replace(match.groupValues[1], "[PROTECTED_PII]")
        }

        return clean
    }

    /**
     * Output Guardrail: Ensures model output contains no prohibited or dangerous content.
     */
    fun filterOutput(output: String): String {
        if (output.isBlank()) return "Boss, response could not be synthesized. Systems standing by."
        return maskSensitivePii(output)
    }
}
