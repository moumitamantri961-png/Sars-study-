package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get custom Application singleton references
        val app = application as EnglishConnectApp
        val repository = app.repository

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Create view models using our custom multi-repository factory
                val factory = AppViewModelFactory(repository)
                val authViewModel: AuthViewModel = viewModel(factory = factory)
                val chatViewModel: ChatViewModel = viewModel(factory = factory)
                val aiViewModel: AIViewModel = viewModel(factory = factory)
                val notificationViewModel: NotificationViewModel = viewModel(factory = factory)

                val currentUser by authViewModel.currentUser.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Google Account Login & Role Selection Dialog
                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Main Tabbed Dashboard (Groups list, AI Bot, Alerts logs, Profile)
                        composable("dashboard") {
                            DashboardScreen(
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                aiViewModel = aiViewModel,
                                notificationViewModel = notificationViewModel,
                                onNavigateToChat = { groupId ->
                                    navController.navigate("chat")
                                },
                                onSignOut = {
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. Immersive Classroom Chat window (WhatsApp design, pins, settings, library)
                        composable("chat") {
                            val user = currentUser
                            if (user != null) {
                                ChatScreen(
                                    chatViewModel = chatViewModel,
                                    currentUser = user,
                                    onBack = {
                                        chatViewModel.selectGroup(null)
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
