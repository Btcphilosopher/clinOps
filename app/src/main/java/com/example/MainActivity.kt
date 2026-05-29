package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClinicViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: ClinicViewModel = viewModel(
          factory = ClinicViewModel.Factory(application)
      )

      MyApplicationTheme {
        val currentScreen by viewModel.currentScreen.collectAsState()
        val currentUser by viewModel.currentUser.collectAsState()
        val isLocked by viewModel.isSessionLocked.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
        ) {
          // Dynamic router mapping with smooth animations
          AnimatedContent(
              targetState = currentScreen,
              transitionSpec = {
                fadeIn() togetherWith fadeOut()
              },
              label = "cli_screens_router"
          ) { screen ->
            when (screen) {
              "LOGIN" -> LoginScreen(viewModel = viewModel)
              "PORTAL" -> {
                when (currentUser?.role) {
                  "PATIENT" -> PatientDashboardScreen(viewModel = viewModel)
                  "CLINICIAN" -> ClinicianDashboardScreen(viewModel = viewModel)
                  "ADMIN" -> AdminDashboardScreen(viewModel = viewModel)
                  else -> LoginScreen(viewModel = viewModel)
                }
              }
              else -> LoginScreen(viewModel = viewModel)
            }
          }

          // Zero-Trust Session Lock Screen Overlay
          AnimatedVisibility(
              visible = isLocked,
              enter = fadeIn() + slideInVertically { it },
              exit = fadeOut() + slideOutVertically { it }
          ) {
            SessionLockOverlay(viewModel = viewModel)
          }
        }
      }
    }
  }
}
