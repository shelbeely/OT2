package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun AppLockScreen(
    viewModel: TransitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_lock_screen")
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secured Vault",
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OpenTransition Vault Locked",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = if (isError) "Incorrect Passcode. Try Again." else "Enter your 4-digit client secure PIN to proceed",
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Indicators dots for entered PIN letters
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val entered = i < inputPin.length
                val dotColor = when {
                    isError -> MaterialTheme.colorScheme.error
                    entered -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Tactile Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "Back")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { key ->
                        if (key.isBlank()) {
                            Spacer(modifier = Modifier.size(70.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (key == "Clear" || key == "Back") Color.Transparent
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "Clear" -> {
                                                inputPin = ""
                                            }
                                            "Back" -> {
                                                if (inputPin.isNotEmpty()) {
                                                    inputPin = inputPin.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (inputPin.length < 4) {
                                                    inputPin += key
                                                    if (inputPin.length == 4) {
                                                        val unlocked = viewModel.unlockApp(inputPin)
                                                        if (!unlocked) {
                                                            isError = true
                                                            inputPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "Back") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = if (key == "Clear") 14.sp else 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
