package com.jarvis.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.ui.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val activeAgent by viewModel.activeAgent.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val currentApiKey by viewModel.apiKey.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Permission launcher for Voice input (RECORD_AUDIO)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice Mic Animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color.Red else JarvisCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("JARVIS AI 5.0", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                            Text(
                                if (isListening) "🎤 Listening to Boss..." else "Active: ${activeAgent.displayName}",
                                fontSize = 11.sp,
                                color = if (isListening) Color.Red else JarvisCyan
                            )
                        }
                    }
                },
                actions = {
                    // API Key Settings Button
                    IconButton(onClick = {
                        tempApiKey = currentApiKey
                        showApiKeyDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "API Key", tint = JarvisCyan)
                    }
                    // Clear Chat Button
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = JarvisTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisSurface)
            )
        },
        containerColor = JarvisBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Quick Action Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf("Torch On", "Torch Off", "Battery", "GST 18% on 5000", "RSI Analysis", "Kaise ho Jarvis?", "Aaj ka weather kaisa hai?")
                items(chips) { chip ->
                    SuggestionChip(
                        onClick = { viewModel.sendCommand(chip) },
                        label = { Text(chip, color = JarvisCyan, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = JarvisSurface),
                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = JarvisCardBorder)
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubbleItem(msg)
                }
                if (isProcessing) {
                    item {
                        Text(
                            "⚡ JARVIS is processing your instruction...",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Input Bar with Mic and Send Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Mic Button
                IconButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.startVoiceInput()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .scale(if (isListening) pulseScale else 1.0f)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFFFF1744) else JarvisBlue)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Command",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask anything or speak…", color = JarvisTextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendCommand(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = JarvisBackground
                    )
                }
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key Setup", color = JarvisCyan, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Boss, live online AI answers ke liye apni Gemini API key yahan paste karein:",
                        fontSize = 13.sp,
                        color = JarvisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApiKey(tempApiKey)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                ) {
                    Text("Save Key", color = JarvisBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisSurface
        )
    }
}

@Composable
fun ChatBubbleItem(msg: ChatMessageEntity) {
    val isUser = msg.sender == "USER"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(if (isUser) JarvisBlue else JarvisSurface)
                .border(1.dp, if (isUser) JarvisCyan else JarvisCardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .widthIn(max = 300.dp)
        ) {
            Column {
                if (!isUser && msg.executionPlan != null) {
                    Text(
                        text = "PLAN: ${msg.executionPlan}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = msg.content,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
