package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClinicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var passcodeVisible by remember { mutableStateOf(false) }

    val errorState by viewModel.securityError.collectAsState()

    // Professional Polish Clean Med-Tech light colors
    val primaryBg = Color(0xFFF7F9FC)
    val cardBg = Color.White
    val accentBlue = Color(0xFF2563EB)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF64748B)
    val textMuted = Color(0xFF94A3B8)
    val borderLight = Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, primaryBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 480.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High level medical branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.5.dp, accentBlue, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "ClinOps Secure Symbol",
                    tint = accentBlue,
                    modifier = Modifier.size(42.dp)
                )
            }

            Text(
                text = "CLINOPS SECURE",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = textPrimary,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Clinical Access & EHR Portal",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Credential Card - White background, subtle shadow-like card feel representing clinical clean surfaces.
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, borderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sign In",
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Clinical Email Address", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedLabelColor = accentBlue,
                            unfocusedLabelColor = textSecondary,
                            focusedBorderColor = accentBlue,
                            unfocusedBorderColor = borderLight,
                            cursorColor = accentBlue
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    // Passcode Field
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("6-Digit Medical PIN", color = textSecondary) },
                        visualTransformation = if (passcodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            val image = if (passcodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passcodeVisible = !passcodeVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Passcode Visibility", tint = textSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedLabelColor = accentBlue,
                            unfocusedLabelColor = textSecondary,
                            focusedBorderColor = accentBlue,
                            unfocusedBorderColor = borderLight,
                            cursorColor = accentBlue
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passcode_input")
                    )

                    // Login feedback logic
                    AnimatedVisibility(visible = errorState != null) {
                        Text(
                            text = errorState ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Log in click action
                    Button(
                        onClick = { viewModel.login(email, passcode) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aegis Secure Login",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick Demo Credentials Selector
            Text(
                text = "Select Sample Secure Role for Quick Loading",
                fontSize = 12.sp,
                color = textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Patient profile quick filling
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable {
                            email = "tom@ahyx.org"
                            passcode = "123123"
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Patient Log", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("T. Miller", color = textSecondary, fontSize = 11.sp)
                        Text("(PIN: 123123)", color = textMuted, fontSize = 10.sp)
                    }
                }

                // Clinician Profile quick fill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16A34A).copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable {
                            email = "thorn@clinic.com"
                            passcode = "123456"
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Doctor Log", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Dr. Thorne", color = textSecondary, fontSize = 11.sp)
                        Text("(PIN: 123456)", color = textMuted, fontSize = 10.sp)
                    }
                }

                // Admin Profile quick fill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable {
                            email = "admin@clinic.com"
                            passcode = "123456"
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Admin Log", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("S. Jenkins", color = textSecondary, fontSize = 11.sp)
                        Text("(PIN: 123456)", color = textMuted, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legalese/Compliance badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(accentBlue)
                )
                Text(
                    text = "End-to-End Cryptography • GDPR Compliant EU 2016/679",
                    fontSize = 11.sp,
                    color = textSecondary,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
