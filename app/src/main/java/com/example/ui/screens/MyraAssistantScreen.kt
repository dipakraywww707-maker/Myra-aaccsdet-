package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.ui.MyraViewModel
import com.example.ui.components.MyraCoreVisualizer
import com.example.ui.components.MyraState
import com.example.ui.theme.MyraBgDark
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraEmerald
import com.example.ui.theme.MyraRose
import com.example.ui.theme.MyraSurfaceDark
import com.example.ui.theme.MyraSurfaceElevated
import com.example.ui.theme.MyraTextMuted
import com.example.ui.theme.MyraTextSecondary
import com.example.ui.theme.MyraViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyraAssistantScreen(
    viewModel: MyraViewModel,
    onStartVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val myraState by viewModel.myraState.collectAsState()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsState()
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Torch chalu karo 🔦",
        "Battery kitna hai? 🔋",
        "Volume 50% karo 🔉",
        "YouTube open karo ▶️",
        "WhatsApp kholo 💬",
        "Camera chalu karo 📸",
        "Phone specs batao 📱",
        "Wi-Fi settings kholo 📶"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MyraBgDark)
    ) {
        // Top Myra Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (myraState) {
                                    MyraState.IDLE -> MyraCyan
                                    MyraState.LISTENING -> MyraEmerald
                                    MyraState.THINKING -> MyraViolet
                                    MyraState.SPEAKING -> MyraRose
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MYRA AI COMPANION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MyraCyan
                        )
                    )
                }
                Text(
                    text = when (myraState) {
                        MyraState.IDLE -> "System Online & Listening"
                        MyraState.LISTENING -> "Listening to your voice..."
                        MyraState.THINKING -> "Processing AI intelligence..."
                        MyraState.SPEAKING -> "Speaking aloud..."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
                )
            }

            Row {
                IconButton(
                    onClick = { viewModel.toggleMuteVoice() },
                    modifier = Modifier.testTag("mute_voice_toggle")
                ) {
                    Icon(
                        imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute Voice",
                        tint = if (isVoiceMuted) MyraRose else MyraCyan
                    )
                }
                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = MyraTextMuted
                    )
                }
            }
        }

        // Center Animated Myra Hologram Core
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            MyraCoreVisualizer(
                state = myraState,
                size = 150.dp,
                onClick = onStartVoiceInput
            )
        }

        // Quick Command Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MyraSurfaceElevated,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.handleUserPrompt(cmd) }
                        .testTag("quick_cmd_${cmd.take(8)}")
                ) {
                    Text(
                        text = cmd,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MyraCyan,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Chat Message Log
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
        ) {
            items(messages) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeakAgain = { viewModel.voiceManager.speak(msg.text) }
                )
            }

            if (myraState == MyraState.THINKING) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MyraViolet,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Myra soch rahi hai...",
                            style = MaterialTheme.typography.bodySmall.copy(color = MyraViolet)
                        )
                    }
                }
            }
        }

        // Bottom Input Field with Mic & Send buttons
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MyraSurfaceDark),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (myraState == MyraState.LISTENING) MyraEmerald
                            else MyraCyan.copy(alpha = 0.15f)
                        )
                        .clickable(onClick = onStartVoiceInput)
                        .testTag("voice_input_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (myraState == MyraState.LISTENING) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (myraState == MyraState.LISTENING) Color.Black else MyraCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Myra se baat karein ya command dein...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MyraTextMuted)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_textfield"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MyraSurfaceElevated,
                        unfocusedContainerColor = MyraSurfaceElevated,
                        focusedBorderColor = MyraCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val msg = textInput
                            textInput = ""
                            viewModel.handleUserPrompt(msg)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (textInput.isNotBlank()) MyraCyan else Color(0xFF1E283E))
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) Color.Black else MyraTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessageEntity,
    onSpeakAgain: () -> Unit
) {
    val isUser = message.isFromUser
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) MyraViolet.copy(alpha = 0.25f) else MyraSurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isUser) MyraViolet else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.testTag(if (isUser) "user_chat_bubble" else "myra_chat_bubble")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "MYRA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MyraCyan,
                                    letterSpacing = 1.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play voice again",
                                tint = MyraCyan.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onSpeakAgain)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    )

                    // Action tag badge if local hardware command was executed
                    if (!message.actionTag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MyraCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⚡ System Action: ${message.actionTag}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MyraCyan
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MyraTextMuted
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
