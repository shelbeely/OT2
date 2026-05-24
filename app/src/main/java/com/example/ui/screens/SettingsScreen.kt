package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TransitionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: TransitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val startDate by viewModel.startDate.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val savedPin by viewModel.savedPin.collectAsState()

    // Dialog confirmations
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var pinInputChange by remember { mutableStateOf(savedPin) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonString by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Settings & Local Privacy",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Fine-tune timeline settings, lock accounts locally, and back up history safely.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        // Privacy First Secure Sandbox Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EnhancedEncryption,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Privacy-First Offline Vault",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "All of your transition milestones, guided profile photos, and voice recording tones bypass external server backups. Everything resides locally inside secure sandboxed storage on this phone.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            }
        }

        // Timeline start date configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Timeline Start Date Setup",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Select when you initiated your gender transition milestones. Your dashboard days counter will tick forward dynamically based on this date.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Date label & trigger
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = startDate
                val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Day-Zero Star Date:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = sdf.format(calendar.time),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val picker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val finalCal = Calendar.getInstance()
                                    finalCal.set(year, month, dayOfMonth)
                                    viewModel.updateStartDate(finalCal.timeInMillis)
                                    Toast.makeText(context, "Transition Timeline Reset!", Toast.LENGTH_SHORT).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            picker.show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose Date", fontSize = 12.sp)
                    }
                }
            }
        }

        // App Lock PIN card section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PIN Lock Protection",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gate application boot behind a 4-digit numeric passcode to retain privacy.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { active ->
                            viewModel.toggleAppLock(active)
                            if (active) {
                                showPinChangeDialog = true
                                pinInputChange = savedPin
                            }
                        },
                        modifier = Modifier.testTag("app_lock_switch")
                    )
                }

                if (isAppLockEnabled) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Local PIN code:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "PIN: **** ($savedPin)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pinInputChange = savedPin
                                showPinChangeDialog = true
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change PIN", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Backups export / Sharing and purges Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "System Backup & Clear tools",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Export Button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportedJsonString = viewModel.exportBackupJson()
                            showExportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("export_backup_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup JSON", fontSize = 12.sp)
                    }
                }

                // Delete Clear all records row
                Button(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("clear_all_data_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Transition Data", fontSize = 12.sp)
                }
            }
        }

        // Dialogs overlays
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Purge all transition data?") },
                text = {
                    Text("This is an IRREVERSIBLE offline data deletion. It will permanently shred database logs, customized milestone timelines, recorded voice tone diaries, and guides photo assets inside your private sandbox.")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.clearAllData()
                            showClearConfirmDialog = false
                            Toast.makeText(context, "All Private Transitions Cleared!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPinChangeDialog) {
            AlertDialog(
                onDismissRequest = { showPinChangeDialog = false },
                title = { Text("Configure PIN Passcode") },
                text = {
                    Column {
                        Text("Enter 4-digit code to local privacy lock protection:")
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pinInputChange,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    pinInputChange = input
                                }
                            },
                            singleLine = true,
                            label = { Text("Numeric Guard PIN") },
                            placeholder = { Text("1234") },
                            modifier = Modifier.fillMaxWidth().testTag("pin_code_change_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = pinInputChange.length == 4,
                        onClick = {
                            viewModel.updatePin(pinInputChange)
                            showPinChangeDialog = false
                            Toast.makeText(context, "PIN code configuration locked!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Set Passcode")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinChangeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Export/Share Backup dialog overlay view
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Local Database Backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Copy or share the securely exported database parameters below:")
                        OutlinedTextField(
                            value = exportedJsonString,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("backup_json_text_viewer"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportedJsonString)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export OpenTransition backup JSON")
                            context.startActivity(shareIntent)
                            showExportDialog = false
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Text")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
