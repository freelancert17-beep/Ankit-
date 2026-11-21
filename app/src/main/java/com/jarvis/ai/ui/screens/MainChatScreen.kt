package com.jarvis.ai.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.ai.R
import com.jarvis.ai.data.local.entity.ChatMessageEntity
import com.jarvis.ai.data.local.entity.ChatSessionItem
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.ui.viewmodel.JarvisViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val messages by viewModel.messages.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.currentSessionId.collectAsState()
    val activeAgent by viewModel.activeAgent.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val currentApiKey by viewModel.apiKey.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(currentApiKey.isBlank()) }
    var tempApiKey by remember { mutableStateOf(currentApiKey) }

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Speech Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    // Photo/Document Picker launcher for Gemini Vision AI
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.selectImage(uri)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Arc Reactor high-tech rotation & pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_effects")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 450 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // DeepSeek / ChatGPT Style Navigation Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = JarvisSurface,
                drawerContentColor = JarvisTextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(JarvisBlue)
                            .border(1.5.dp, JarvisCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_jarvis_logo),
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "JARVIS ARCHIVES",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = JarvisTextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Active Sessions: ${sessions.size}",
                            fontSize = 11.sp,
                            color = JarvisCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "+ New Chat" Button
                Button(
                    onClick = {
                        viewModel.startNewChat()
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, JarvisCyan)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = JarvisCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ New Chat",
                        color = JarvisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                    color = JarvisCardBorder
                )

                Text(
                    text = "PREVIOUS CONVERSATIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    letterSpacing = 1.sp
                )

                // Sessions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    if (sessions.isEmpty()) {
                        item {
                            Text(
                                text = "No previous chats yet.\nStart asking JARVIS anything!",
                                color = JarvisTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    } else {
                        items(sessions) { session ->
                            val isSelected = session.sessionId == activeSessionId
                            SessionDrawerItem(
                                session = session,
                                isSelected = isSelected,
                                onSelect = {
                                    viewModel.selectSession(session.sessionId)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onDelete = {
                                    viewModel.deleteSession(session.sessionId)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Drawer",
                                tint = JarvisCyan
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(JarvisSurface)
                                    .border(1.5.dp, if (isListening) Color(0xFFFF1744) else JarvisCyan, CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_jarvis_logo),
                                    contentDescription = "JARVIS Core",
                                    tint = if (isListening) Color(0xFFFF1744) else JarvisCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "JARVIS AI 5.0",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = JarvisTextPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isListening) Color(0xFFFF1744) else JarvisAccentGreen)
                                    )
                                }
                                Text(
                                    text = if (isListening) "🎤 LISTENING..." else "ONLINE // ${activeAgent.displayName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isListening) Color(0xFFFF5252) else JarvisCyan
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick New Chat Action
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = JarvisCyan
                            )
                        }

                        // API Key Configuration Button
                        IconButton(
                            onClick = {
                                tempApiKey = currentApiKey
                                showApiKeyDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = JarvisCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = JarvisSurface
                    )
                )
            },
            containerColor = JarvisBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Quick Command Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chips = listOf(
                        "📄 Student Marksheet banao",
                        "💰 Expense Budget Sheet banao",
                        "💬 WhatsApp message bhejo",
                        "🎵 YouTube par gaana play karo",
                        "📞 Phone Dialer kholo",
                        "🔦 Torch On",
                        "🔋 Battery Status",
                        "🌐 Aaj ka mausam kaisa hai?"
                    )
                    items(chips) { chip ->
                        SuggestionChip(
                            onClick = {
                                val clean = chip.substringAfter(" ")
                                viewModel.sendCommand(clean, isVoiceQuery = false)
                            },
                            label = { Text(chip, color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = JarvisSurface),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = JarvisCardBorder)
                        )
                    }
                }

                // Message History List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubbleItem(
                            msg = msg,
                            context = context,
                            onOpenFile = { path -> viewModel.openFile(context, path) },
                            onSpeak = { text -> viewModel.speakMessage(text) }
                        )
                    }
                    if (isProcessing) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = JarvisCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "⚡ JARVIS Neural Engine is executing...",
                                    color = JarvisCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Attached Image Thumbnail Preview (Gemini Vision AI)
                if (selectedImageUri != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = JarvisSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, JarvisCyan)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val thumbnailBitmap = remember(selectedImageUri) {
                                try {
                                    context.contentResolver.openInputStream(selectedImageUri!!)?.use {
                                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                                    }
                                } catch (_: Exception) { null }
                            }

                            if (thumbnailBitmap != null) {
                                Image(
                                    bitmap = thumbnailBitmap,
                                    contentDescription = "Selected image preview",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📷 Image Attached for Vision AI",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyan
                                )
                                Text(
                                    text = "Jarvis will analyze this photo",
                                    fontSize = 11.sp,
                                    color = JarvisTextSecondary
                                )
                            }

                            IconButton(onClick = { viewModel.selectImage(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attached photo",
                                    tint = JarvisTextSecondary
                                )
                            }
                        }
                    }
                }

                // Bottom Input Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = JarvisSurface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, JarvisCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic / Voice Command Button (Iron Man arc reactor sound)
                        IconButton(
                            onClick = {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    viewModel.startVoiceInput()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .scale(if (isListening) pulseScale else 1.0f)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color(0xFFFF1744) else JarvisBlue)
                                .border(1.5.dp, if (isListening) Color.White else JarvisCyan, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Photo / Document Picker for Gemini Vision
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(JarvisBackground)
                                .border(1.dp, JarvisCardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach Photo",
                                tint = if (selectedImageUri != null) JarvisCyan else JarvisTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Text Input Field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask anything or type command…",
                                    color = JarvisTextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() || selectedImageUri != null) {
                                        viewModel.sendCommand(inputText, isVoiceQuery = false)
                                        inputText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            trailingIcon = {
                                if (inputText.isNotBlank()) {
                                    IconButton(onClick = { inputText = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = JarvisTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary,
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCardBorder,
                                focusedContainerColor = JarvisBackground,
                                unfocusedContainerColor = JarvisBackground
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || selectedImageUri != null) {
                                    viewModel.sendCommand(inputText, isVoiceQuery = false)
                                    inputText = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank() || selectedImageUri != null) JarvisCyan else JarvisCardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Command",
                                tint = if (inputText.isNotBlank() || selectedImageUri != null) JarvisBackground else JarvisTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Gemini API Key Setup Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = JarvisCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Gemini API Key Setup",
                        color = JarvisCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Boss, live unlimited Gemini AI response ke liye apni API key yahan set karein:",
                        fontSize = 13.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("AQ... ya AIzaSy...", color = JarvisTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedContainerColor = JarvisBackground,
                            unfocusedContainerColor = JarvisBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApiKey(tempApiKey)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Key", color = JarvisBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun SessionDrawerItem(
    session: ChatSessionItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(session.lastTimestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(session.lastTimestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onSelect() },
        color = if (isSelected) JarvisBlue.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) BorderStroke(1.dp, JarvisCyan) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = if (isSelected) JarvisCyan else JarvisTextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    color = JarvisTextSecondary,
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Session",
                    tint = JarvisTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    msg: ChatMessageEntity,
    context: Context = LocalContext.current,
    onOpenFile: (String) -> Unit = {},
    onSpeak: (String) -> Unit = {}
) {
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
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) JarvisBlue else JarvisSurface)
                .border(
                    1.dp,
                    if (isUser) JarvisCyan else JarvisCardBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .widthIn(max = 320.dp)
        ) {
            Column {
                // Attached Photo Preview for User
                if (isUser && !msg.imageUri.isNullOrBlank()) {
                    val userImageBitmap = remember(msg.imageUri) {
                        try {
                            val uri = Uri.parse(msg.imageUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                            }
                        } catch (_: Exception) { null }
                    }

                    if (userImageBitmap != null) {
                        Image(
                            bitmap = userImageBitmap,
                            contentDescription = "User attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Agent Header / Execution Plan for Jarvis
                if (!isUser && !msg.executionPlan.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.executionPlan.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisCyan,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Tap-to-speak button
                        IconButton(
                            onClick = { onSpeak(msg.content) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen to answer",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Message Text
                if (msg.content.isNotBlank()) {
                    Text(
                        text = msg.content,
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // Generated Document / Sheet Card (Download / Open)
                if (!msg.filePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFile(msg.filePath) },
                        color = JarvisBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, JarvisCyan)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg.fileName ?: "Spreadsheet / Document",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to open or share",
                                    fontSize = 10.sp,
                                    color = JarvisCyan
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Document",
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
