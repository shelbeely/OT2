package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MilestoneEntry
import com.example.ui.viewmodel.TransitionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MilestonesScreen(
    viewModel: TransitionViewModel,
    modifier: Modifier = Modifier
) {
    val milestones by viewModel.milestones.collectAsState()
    val context = LocalContext.current

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog fields
    var milestoneTitle by remember { mutableStateOf("") }
    var milestoneDescription by remember { mutableStateOf("") }
    var milestoneCategory by remember { mutableStateOf("Medical") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val categories = listOf("Medical", "Social", "Legal", "Personal")

    Scaffold(
        modifier = modifier.testTag("milestones_container_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_milestone_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Milestone Entry")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transition Milestones Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "A safe private space to log legal, medical, social, or personal steps of your journey.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (milestones.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No milestones documented yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Click the '+' button below to mark your first milestone event (e.g. Decided on transition, Started HRT, Shared with parents, Passport update).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    // Vertical Timeline Layout with connect guides
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(milestones) { index, item ->
                            val isLast = index == milestones.lastIndex

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Draw Vertical Connect Dots & connector lines
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(32.dp)
                                ) {
                                    val categoryColor = getCategoryColor(item.category)

                                    // Bubble top
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor)
                                    )

                                    // Line connecting to next
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(120.dp) // responsive connector gap height
                                                .background(categoryColor.copy(alpha = 0.35f))
                                        )
                                    }
                                }

                                // Timeline content card description
                                Column(modifier = Modifier.weight(1f)) {
                                    MilestoneRowItem(
                                        milestone = item,
                                        onDelete = { viewModel.deleteMilestone(item) },
                                        showDeleteButton = true
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Dialog Pop-up
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = {
                        Text(
                            text = "New Milestone Record",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = milestoneTitle,
                                onValueChange = { milestoneTitle = it },
                                label = { Text("Event Title") },
                                placeholder = { Text("e.g. Hormones Decided, Came out to friends") },
                                modifier = Modifier.fillMaxWidth().testTag("milestone_title_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = milestoneDescription,
                                onValueChange = { milestoneDescription = it },
                                label = { Text("Description") },
                                placeholder = { Text("Add beautiful memory context here...") },
                                modifier = Modifier.fillMaxWidth().testTag("milestone_desc_input"),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Category Selector
                            Text(
                                text = "Category Variant:",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = milestoneCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) getCategoryColor(cat)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { milestoneCategory = cat }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Interactive Date Picker Trigger
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = selectedDateMillis
                            val df = SimpleDateFormat("MMM yyyy, dd", Locale.US)

                            OutlinedButton(
                                onClick = {
                                    val datePickerDialog = DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val outCal = Calendar.getInstance()
                                            outCal.set(year, month, dayOfMonth)
                                            selectedDateMillis = outCal.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    datePickerDialog.show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Date Picker: " + df.format(calendar.time))
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (milestoneTitle.isNotBlank()) {
                                    viewModel.addMilestone(
                                        title = milestoneTitle,
                                        description = milestoneDescription,
                                        category = milestoneCategory,
                                        timestamp = selectedDateMillis
                                    )
                                    // Reset Fields
                                    milestoneTitle = ""
                                    milestoneDescription = ""
                                    milestoneCategory = "Medical"
                                    selectedDateMillis = System.currentTimeMillis()
                                    showAddDialog = false
                                }
                            }
                        ) {
                            Text("Save Target")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Medical" -> Color(0xFF55CDFC) // Soft pride blue
        "Social" -> Color(0xFFF7A8B8) // Soft pride pink
        "Legal" -> Color(0xFF6C63FF) // Legal slate blue
        else -> Color(0xFF006689) // Personal deep
    }
}
