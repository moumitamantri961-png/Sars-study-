package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val showRoleSelection by authViewModel.showRoleSelection.collectAsState()

    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(currentUser, showRoleSelection) {
        if (currentUser != null && !showRoleSelection) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Upper background ambient gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EnglishPrimary, Color.White)
                    )
                )
        )

        if (showRoleSelection) {
            RoleSelectionContent(
                onSelectRole = { role ->
                    authViewModel.selectRole(role)
                    onLoginSuccess()
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                            .padding(8.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "App logo",
                            tint = EnglishPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "English Connect",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnglishOnBackground,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Connecting Teachers, Students & AI",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EnglishMuted,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Middle Login Form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Google Sign-In Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EnglishOnBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Choose a default educational profile or enter custom details to test.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EnglishMuted,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Default Google Buttons
                        Button(
                            onClick = {
                                authViewModel.signInWithGoogle(
                                    email = "mr.harris@englishconnect.edu",
                                    name = "Mr. Harris (Lead Instructor)",
                                    photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=120",
                                    onboardingNeeded = false // Teacher role direct
                                )
                                authViewModel.selectRole("TEACHER")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("teacher_signin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisedUserCircle,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In as Mr. Harris (Teacher)")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                authViewModel.signInWithGoogle(
                                    email = "jane.doe@student.com",
                                    name = "Jane Doe (IELTS Aspirant)",
                                    photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=120",
                                    onboardingNeeded = false // Student role direct
                                )
                                authViewModel.selectRole("STUDENT")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("student_signin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EnglishPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In as Jane Doe (Student)")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                " OR Custom Login ",
                                style = MaterialTheme.typography.bodySmall,
                                color = EnglishMuted
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        // Custom dynamic onboarding simulator
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Your Name") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("custom_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Google Account Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("custom_email_input")
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (customName.trim().isEmpty() || customEmail.trim().isEmpty()) {
                                    errorMessage = "Please enter both Email and Name."
                                } else {
                                    errorMessage = ""
                                    // Simulated login which triggers first login screen
                                    authViewModel.signInWithGoogle(
                                        email = customEmail.trim().lowercase(),
                                        name = customName.trim(),
                                        photoUrl = "",
                                        onboardingNeeded = true
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_login_next_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EnglishSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Sign-In")
                        }
                    }
                }

                // Footer
                Text(
                    text = "Professional Education Gateway • Secure OAuth 2.0",
                    style = MaterialTheme.typography.bodySmall.copy(color = EnglishMuted)
                )
            }
        }
    }
}

@Composable
fun RoleSelectionContent(
    onSelectRole: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to English Connect!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = EnglishOnBackground
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To customize your classroom tools, please select your primary educational role:",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = EnglishMuted,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role Option 1: Teacher Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectRole("TEACHER") }
                .testTag("select_teacher_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = EnglishTeacherBadge.copy(alpha = 0.08f)
            ),
            border = CardDefaults.outlinedCardBorder(true).copy(
                brush = Brush.linearGradient(listOf(EnglishTeacherBadge, EnglishTeacherBadge))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EnglishTeacherBadge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Class,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "I am a Teacher",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnglishTeacherBadge
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage groups, pin announcements, mute students, upload materials, and grade assignments.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EnglishOnSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Role Option 2: Student Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectRole("STUDENT") }
                .testTag("select_student_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = EnglishPrimary.copy(alpha = 0.08f)
            ),
            border = CardDefaults.outlinedCardBorder(true).copy(
                brush = Brush.linearGradient(listOf(EnglishPrimary, EnglishPrimary))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EnglishPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "I am a Student",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnglishPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Join class groups, submit homework, shared materials, receive notes, and speak with our AI tutor.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EnglishOnSurface
                        )
                    )
                }
            }
        }
    }
}
