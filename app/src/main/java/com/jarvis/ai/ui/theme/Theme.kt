package com.jarvis.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    secondary = JarvisBlue,
    background = JarvisBackground,
    surface = JarvisSurface,
    onPrimary = JarvisBackground,
    onBackground = JarvisTextPrimary,
    onSurface = JarvisTextPrimary
)

@Composable
fun JarvisAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        content = content
    )
}
