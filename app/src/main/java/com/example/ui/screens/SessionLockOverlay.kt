package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClinicViewModel

@Composable
fun SessionLockOverlay(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var pinEntered by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }

    val accentBlue = Color(0xFF2563EB)
    val darkSlateBg = Color(0xFF0F172A)
    val buttonBg = Color(0xFF1E293B)
    val buttonBorder = Color(0xFF334155)
    val textSecondary = Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkSlateBg.copy(alpha = 0.98f))
            .clickable(enabled = false) {}, // Intercepts clicks fully to seal background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Session Locked Shield",
                tint = accentBlue,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = "SESSION LOCKED",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )

            Text(
                text = "To resume the clinical session for ${currentUser?.name ?: "ThomasMiller"}, please enter your 6-digit secure medical PIN.",
                color = textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            // Entered PIN Dot indicators (6 dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 1..6) {
                    val active = pinEntered.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (active) accentBlue else buttonBg)
                            .border(1.dp, if (active) accentBlue else buttonBorder, CircleShape)
                    )
                }
            }

            AnimatedVisibility(visible = pinErrorText != null) {
                Text(
                    text = pinErrorText ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secure Virtual Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                val padKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "Unlock")
                )

                padKeys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (key == "Unlock") accentBlue else buttonBg)
                                    .border(1.dp, buttonBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        pinErrorText = null
                                        when (key) {
                                            "C" -> {
                                                if (pinEntered.isNotEmpty()) {
                                                    pinEntered = pinEntered.dropLast(1)
                                                }
                                            }
                                            "Unlock" -> {
                                                if (pinEntered.length == 6) {
                                                    val success = viewModel.unlockSession(pinEntered)
                                                    if (!success) {
                                                        pinErrorText = "PIN is incorrect"
                                                        pinEntered = ""
                                                    }
                                                } else {
                                                    pinErrorText = "Enter full 6 digits"
                                                }
                                            }
                                            else -> {
                                                if (pinEntered.length < 6) {
                                                    pinEntered += key
                                                    // Auto trigger when 6 digits are entered
                                                    if (pinEntered.length == 6) {
                                                        // Optional instant validation on 6th digit press
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "C") {
                                    Icon(imageVector = Icons.Default.Backspace, contentDescription = "backspace", tint = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (key == "Unlock") 13.sp else 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
            ) {
                Text("Log Out Session", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
