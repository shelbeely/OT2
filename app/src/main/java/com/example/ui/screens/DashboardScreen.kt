package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MilestoneEntry
import com.example.data.model.PhotoEntry
import com.example.data.model.VoiceEntry
import com.example.ui.viewmodel.TransitionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransitionViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.photos.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val voiceEntries by viewModel.voiceEntries.collectAsState()
    val daysCount = viewModel.getTransitionDaysCount()

    // Retrieve absolute latest individual entry overall
    val latestPhoto = photos.maxByOrNull { it.timestamp }
    val latestMilestone = milestones.maxByOrNull { it.timestamp }
    val latestVoice = voiceEntries.maxByOrNull { it.timestamp }

    val latestEntry: Any? = listOfNotNull(latestPhoto, latestMilestone, latestVoice)
        .maxByOrNull { entry ->
            when (entry) {
                is PhotoEntry -> entry.timestamp
                is MilestoneEntry -> entry.timestamp
                is VoiceEntry -> entry.timestamp
                else -> 0L
            }
        }

    var showBottomSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. THE MOMENTUM CARD (Top portion)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "JOURNEY TIMELINE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (daysCount == 0L) {
                        // Low pressure welcoming aesthetic statement for Day 0
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Your Private Transition Sandbox",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Welcome. There is no rush, and absolutely zero external pressure. Log daily progress photos, timeline events, or pitches on your own terms. Everything is securely sandbox-stored locally.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        // Day 1+ momentum circle progress tracker
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DAY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "$daysCount",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Every day is another progressive step on your path.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "🔒 Sandbox Mode: 100% Secure & On-Device Storage Only",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // 2. THE "ONE BIG BUTTON" RULE (Centered prominent action trigger)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier
                        .height(58.dp)
                        .widthIn(min = 220.dp)
                        .testTag("one_big_action_button"),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log Today",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // 3. CLEAN VISUAL HIERARCHY & COMPACT PROGRESS (Only shows absolute single latest entry to avoid text anxiety)
        if (latestEntry != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LATEST ENTRY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        when (latestEntry) {
                            is PhotoEntry -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToCamera() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = File(latestEntry.filePath),
                                            contentDescription = "Latest Transition Capture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Progress Snapshot",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (latestEntry.notes.isNotBlank()) {
                                            Text(
                                                text = latestEntry.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF55CDFC).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = latestEntry.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF55CDFC),
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(latestEntry.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Navigate to Gallery",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is MilestoneEntry -> {
                                val categoryColor = when (latestEntry.category) {
                                    "Medical" -> Color(0xFF55CDFC)
                                    "Social" -> Color(0xFFF7A8B8)
                                    "Legal" -> Color(0xFF6C63FF)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToMilestones() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(categoryColor.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = categoryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = latestEntry.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (latestEntry.description.isNotBlank()) {
                                            Text(
                                                text = latestEntry.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = categoryColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = latestEntry.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = categoryColor,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(latestEntry.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Navigate to Milestones",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is VoiceEntry -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToVoice() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFF7A8B8).copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color(0xFFF7A8B8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Vocal Pitch Recording",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        val durationSec = (latestEntry.durationMs / 1000).coerceAtLeast(1)
                                        val pitchText = if (latestEntry.estimatedPitchHz > 0) "${latestEntry.estimatedPitchHz.toInt()} Hz" else "Recorded"
                                        Text(
                                            text = "Duration: ${durationSec}s | Log Pitch Check: $pitchText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF7A8B8).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "Voice Entry",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFF7A8B8),
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(latestEntry.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Navigate to Voice Recorder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Choice Trigger
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.35f)
        ) {
            BottomSheetContent(
                onNavigateToCamera = {
                    showBottomSheet = false
                    onNavigateToCamera()
                },
                onNavigateToVoice = {
                    showBottomSheet = false
                    onNavigateToVoice()
                },
                onNavigateToMilestones = {
                    showBottomSheet = false
                    onNavigateToMilestones()
                },
                onNavigateToHealthConnect = {
                    showBottomSheet = false
                    onNavigateToHealthConnect()
                },
                onDismiss = {
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
fun BottomSheetContent(
    onNavigateToCamera: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToHealthConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Record Today's Progress",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose an entry type below. Your choices are safely kept private.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Option 1: Photo Entry (Sky Blue theme)
        BottomSheetOptionRow(
            title = "Capture Daily Photo",
            description = "High quality alignment overlays & guide support to capture progressive changes.",
            icon = Icons.Default.CameraAlt,
            containerColor = Color(0xFF55CDFC).copy(alpha = 0.12f),
            iconColor = Color(0xFF55CDFC),
            onClick = onNavigateToCamera,
            testTag = "log_option_photo"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Voice Diary (Classic Pink theme)
        BottomSheetOptionRow(
            title = "Voice Pitch Record",
            description = "Compare frequency analysis and record tracking vocal progress.",
            icon = Icons.Default.Mic,
            containerColor = Color(0xFFF7A8B8).copy(alpha = 0.12f),
            iconColor = Color(0xFFF7A8B8),
            onClick = onNavigateToVoice,
            testTag = "log_option_voice"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 3: Milestone Addition (Primary App theme)
        BottomSheetOptionRow(
            title = "Add Milestone Step",
            description = "Mark hormone steps, sharing logs, document updates, or personal events.",
            icon = Icons.Default.Flag,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            iconColor = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToMilestones,
            testTag = "log_option_milestone"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 4: Health Connect Sync Hub (Modern Unified Purple)
        BottomSheetOptionRow(
            title = "Health Connect & Labs",
            description = "Sync, view and manual log on-device clinical FHIR blood records & targets.",
            icon = Icons.Default.CloudSync,
            containerColor = Color(0xFF6C63FF).copy(alpha = 0.12f),
            iconColor = Color(0xFF6C63FF),
            onClick = onNavigateToHealthConnect,
            testTag = "log_option_health_connect"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Clean text escape option
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                "Close Menu",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun BottomSheetOptionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Keeping MilestoneRowItem at the bottom to remain fully compatible with package usages in MilestonesScreen.kt
@Composable
fun MilestoneRowItem(
    milestone: MilestoneEntry,
    onDelete: () -> Unit,
    showDeleteButton: Boolean = false
) {
    val categoryColor = when (milestone.category) {
        "Medical" -> Color(0xFF55CDFC)
        "Social" -> Color(0xFFF7A8B8)
        "Legal" -> Color(0xFF6C63FF)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(categoryColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(milestone.category, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(milestone.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            if (showDeleteButton) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Milestone",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
