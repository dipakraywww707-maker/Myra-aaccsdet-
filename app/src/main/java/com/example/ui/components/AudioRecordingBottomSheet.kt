package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.RecorderState
import com.example.data.audio.VoiceRecorderManager
import com.example.ui.theme.MyraBgDark
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraEmerald
import com.example.ui.theme.MyraRose
import com.example.ui.theme.MyraSurfaceDark
import com.example.ui.theme.MyraSurfaceElevated
import com.example.ui.theme.MyraTextMuted
import com.example.ui.theme.MyraTextSecondary
import com.example.ui.theme.MyraViolet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecordingBottomSheet(
    sheetState: SheetState,
    recorderState: RecorderState,
    durationSeconds: Int,
    currentAmplitude: Float,
    waveformSamples: List<Float>,
    transcribedText: String,
    statusMessage: String,
    audioFile: File?,
    playbackProgress: Float,
    onDismiss: () -> Unit,
    onToggleRecord: () -> Unit,
    onPlayAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onCancelRecording: () -> Unit,
    onUpdateTranscribedText: (String) -> Unit,
    onSubmitCommand: (String) -> Unit
) {
    var commandInput by remember(transcribedText) { mutableStateOf(transcribedText) }

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

    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (recorderState == RecorderState.RECORDING) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MyraBgDark,
        dragHandle = null,
        modifier = Modifier.testTag("audio_recording_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (recorderState) {
                                    RecorderState.RECORDING -> MyraRose.copy(alpha = 0.2f)
                                    RecorderState.PLAYING -> MyraEmerald.copy(alpha = 0.2f)
                                    else -> MyraCyan.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (recorderState) {
                                RecorderState.RECORDING -> Icons.Default.GraphicEq
                                RecorderState.PLAYING -> Icons.Default.PlayArrow
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "Microphone Status",
                            tint = when (recorderState) {
                                RecorderState.RECORDING -> MyraRose
                                RecorderState.PLAYING -> MyraEmerald
                                else -> MyraCyan
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VOICE AUDIO CAPTURE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MyraCyan,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = MyraTextSecondary)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_recording_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MyraTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Waveform & Recording Pulse Visualizer Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraSurfaceDark),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recording Timer & Live Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (recorderState == RecorderState.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MyraRose)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = VoiceRecorderManager.formatDuration(durationSeconds),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (recorderState == RecorderState.RECORDING) MyraRose else Color.White
                            ),
                            modifier = Modifier.testTag("recording_timer_display")
                        )
                        if (recorderState == RecorderState.RECORDING) {
                            Text(
                                text = " / 00:30",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MyraTextMuted)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Waveform Equalizer Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveformSamples.forEachIndexed { index, sample ->
                            val dynamicHeight = (sample * 46f).coerceIn(4f, 48f)
                            val barColor = when {
                                recorderState == RecorderState.RECORDING -> {
                                    if (index % 2 == 0) MyraCyan else MyraViolet
                                }
                                recorderState == RecorderState.PLAYING -> MyraEmerald
                                else -> MyraTextMuted.copy(alpha = 0.5f)
                            }
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(dynamicHeight.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    // Audio File Playback Preview if recorded
                    if (audioFile != null && audioFile.exists() && recorderState != RecorderState.RECORDING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MyraSurfaceElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (recorderState == RecorderState.PLAYING) onPauseAudio()
                                                else onPlayAudio()
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MyraEmerald.copy(alpha = 0.2f))
                                                .testTag("play_recorded_audio_button")
                                        ) {
                                            Icon(
                                                imageVector = if (recorderState == RecorderState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Playback Audio",
                                                tint = MyraEmerald,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (recorderState == RecorderState.PLAYING) "Playing Audio Clip..." else "Recorded Audio Clip",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "${audioFile.name.take(18)}... (${audioFile.length() / 1024} KB)",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MyraTextMuted
                                                )
                                            )
                                        }
                                    }
                                }

                                if (recorderState == RecorderState.PLAYING) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { playbackProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MyraEmerald,
                                        trackColor = MyraSurfaceDark,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Command Text Field (Transcription / Editable)
            OutlinedTextField(
                value = commandInput,
                onValueChange = {
                    commandInput = it
                    onUpdateTranscribedText(it)
                },
                placeholder = {
                    Text(
                        if (recorderState == RecorderState.RECORDING) "Listening to your voice..."
                        else "Voice command will appear here...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MyraTextMuted)
                    )
                },
                label = {
                    Text(
                        "Captured Voice Command",
                        style = MaterialTheme.typography.labelSmall.copy(color = MyraCyan)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_command_text_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MyraSurfaceElevated,
                    unfocusedContainerColor = MyraSurfaceElevated,
                    focusedBorderColor = MyraCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Voice Command Suggestion Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MyraSurfaceElevated,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                commandInput = cmd
                                onUpdateTranscribedText(cmd)
                            }
                            .testTag("rec_quick_cmd_${cmd.take(8)}")
                    ) {
                        Text(
                            text = cmd,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MyraCyan,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Controls Row (Cancel / Record Toggle / Submit Command)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel / Discard Button
                IconButton(
                    onClick = onCancelRecording,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MyraSurfaceElevated)
                        .testTag("cancel_recording_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Discard Recording",
                        tint = MyraRose
                    )
                }

                // Center Big Microphone Record / Stop Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (recorderState == RecorderState.RECORDING)
                                    listOf(MyraRose, Color(0xFFFF5252))
                                else
                                    listOf(MyraCyan, MyraViolet)
                            )
                        )
                        .clickable(onClick = onToggleRecord)
                        .testTag("audio_record_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (recorderState == RecorderState.RECORDING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (recorderState == RecorderState.RECORDING) "Stop Recording" else "Start Recording",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Send / Execute Voice Command Button
                Button(
                    onClick = {
                        val textToSubmit = commandInput.trim().ifBlank {
                            "Voice command audio captured"
                        }
                        onSubmitCommand(textToSubmit)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (commandInput.isNotBlank()) MyraCyan else MyraSurfaceElevated,
                        contentColor = if (commandInput.isNotBlank()) Color.Black else MyraTextMuted
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("submit_voice_command_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Execute",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
