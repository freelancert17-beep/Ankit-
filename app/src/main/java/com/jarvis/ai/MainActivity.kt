package com.jarvis.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jarvis.ai.ui.screens.MainChatScreen
import com.jarvis.ai.ui.theme.JarvisAITheme
import com.jarvis.ai.ui.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisAITheme {
                MainChatScreen(viewModel = viewModel)
            }
        }
    }
}
