package com.jarvis.ai.ui.screens

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.ui.viewmodel.JarvisViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: JarvisViewModel) {
    val messages by viewModel.messages.collectAsState()
    val activeAgent by viewModel.activeAgent.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("JARVIS 5.0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                            Text(
                                "Active: ${activeAgent.displayName}",
                                fontSize = 11.sp,
                                color = JarvisCyan
                            )
                        }
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
                val chips = listOf("Torch On", "Torch Off", "Battery", "GST 18% on 5000", "RSI Analysis", "Write Python Script")
                items(chips) { chip ->
                    SuggestionChip(
                        onClick = { viewModel.sendCommand(chip) },
                        label = { Text(chip, color = JarvisCyan, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = JarvisSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = JarvisCardBorder
                        )
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
                            "JARVIS is processing instruction...",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Command JARVIS…", color = JarvisTextSecondary) },
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
                        .size(48.dp)
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
