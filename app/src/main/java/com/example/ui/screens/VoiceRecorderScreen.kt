package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceEntry
import com.example.ui.viewmodel.TransitionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoiceRecorderScreen(
    viewModel: TransitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceEntries by viewModel.voiceEntries.collectAsState()

    // Recorder parameters
    val isRecording by viewModel.isRecording.collectAsState()
    val recordDurationSeconds by viewModel.recordDurationSeconds.collectAsState()
    val amplitudes by viewModel.audioVisualizerAmplitudes.collectAsState()

    // Player parameters
    val playingFileId by viewModel.playingFileId.collectAsState()
    val playPositionMs by viewModel.playPositionMs.collectAsState()
    val currentPlayerDurationMs by viewModel.currentPlayerDurationMs.collectAsState()

    // Note adding parameter
    var recordingNotesInput by remember { mutableStateOf("") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_recorder_screen")
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Vocal Diary Header
        Text(
            text = "Vocal Pitch & Resonance Logs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Document vocal tone, timber, and pitch changes privately over time.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive recording console Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "Recording Active Private Voice Entry" else "Voice Resonance Capture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Waveform visualizer container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        // Dynamic wave drawn reactively
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            amplitudes.forEach { amp ->
                                val barHeight = (amp * 60f).coerceIn(4f, 60f).dp
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(barHeight)
                                        .padding(horizontal = 1.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Waveform visualizer starts when you speak",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Micro timer tick display
                Text(
                    text = formatDuration(recordDurationSeconds * 1000),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes input field to attach context to recording
                if (!isRecording) {
                    OutlinedTextField(
                        value = recordingNotesInput,
                        onValueChange = { recordingNotesInput = it },
                        placeholder = { Text("Log notes (e.g. Hormone Week 12, morning voice)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Record Button
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAudioRecording(recordingNotesInput)
                                recordingNotesInput = ""
                            } else {
                                if (hasMicPermission) {
                                    viewModel.startAudioRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.testTag("record_audio_fab"),
                        shape = CircleShape,
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Record Voice Log"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saved vocal recordings section listing
        Text(
            text = "Your Recorded Logs (${voiceEntries.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (hasMicPermission && voiceEntries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No voice diaries saved yet",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Begin vocal recording to secure voice logs offline.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else if (!hasMicPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Microphone access is disabled",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Mic Permission")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(voiceEntries) { entry ->
                    val isPlayingThis = playingFileId == entry.id
                    val displayPitch = entry.estimatedPitchHz

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini play trigger
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .background(
                                            if (isPlayingThis) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                        .clickable { viewModel.playVoiceEntry(entry) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isPlayingThis) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Duration: ${formatDuration(entry.durationMs)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                // Interactive simulated pitch rating container
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = getPitchColor(displayPitch).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${displayPitch.toInt()} Hz",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = getPitchColor(displayPitch)
                                        )
                                        Text(
                                            text = getPitchLabel(displayPitch),
                                            fontSize = 9.sp,
                                            color = getPitchColor(displayPitch)
                                        )
                                    }
                                }
                            }

                            if (entry.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Notes: ${entry.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            // Dynamic interactive audio playback bar overlay
                            if (isPlayingThis) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDuration(playPositionMs.toLong()),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Slider(
                                        value = playPositionMs.toFloat(),
                                        onValueChange = {},
                                        valueRange = 0f..currentPlayerDurationMs.toFloat(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )

                                    Text(
                                        text = formatDuration(currentPlayerDurationMs.toLong()),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Row of interactive actions (Delete button is aligned to end)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.deleteVoiceEntry(entry) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove file", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Format duration from millis to M:SS
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

// Helpers to color estimated pitch tags
private fun getPitchColor(hz: Float): Color {
    return when {
         hz < 130 -> Color(0xFF55CDFC) // Under 130: typical masculine tone index colors
         hz in 130f..180f -> Color(0xFF904252) // 130-180: neutral therapeutic core ranges
         else -> Color(0xFFF7A8B8) // Over 180: typical feminine tones
    }
}

private fun getPitchLabel(hz: Float): String {
    return when {
        hz < 130 -> "Deep/Low"
        hz in 130f..180f -> "Androgynous"
        else -> "High/Bright"
    }
}
