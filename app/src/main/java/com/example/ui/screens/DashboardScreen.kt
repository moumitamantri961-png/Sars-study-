package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    aiViewModel: AIViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToChat: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val unreadNotifications by notificationViewModel.notificationCount.collectAsState()

    // Redirect to login if user logs out
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onSignOut()
        }
    }

    val user = currentUser ?: return

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Tab 0: Home / Groups
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Groups") },
                    label = { Text("Groups") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EnglishPrimary,
                        selectedTextColor = EnglishPrimary,
                        unselectedIconColor = EnglishMuted,
                        unselectedTextColor = EnglishMuted
                    )
                )

                // Tab 1: AI English Assistant
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EnglishAIBadge,
                        selectedTextColor = EnglishAIBadge,
                        unselectedIconColor = EnglishMuted,
                        unselectedTextColor = EnglishMuted
                    )
                )

                // Tab 2: Push Notification logs
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(
                                        containerColor = EnglishPriorityBorder
                                    ) {
                                        Text(unreadNotifications.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    },
                    label = { Text("Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EnglishSecondary,
                        selectedTextColor = EnglishSecondary,
                        unselectedIconColor = EnglishMuted,
                        unselectedTextColor = EnglishMuted
                    )
                )

                // Tab 3: Student / Teacher Profile
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EnglishTeacherBadge,
                        selectedTextColor = EnglishTeacherBadge,
                        unselectedIconColor = EnglishMuted,
                        unselectedTextColor = EnglishMuted
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (selectedTab) {
                0 -> GroupsTab(chatViewModel, user, onNavigateToChat)
                1 -> AIAssistantTab(aiViewModel)
                2 -> NotificationsTab(notificationViewModel)
                3 -> ProfileTab(authViewModel, chatViewModel, user)
            }
        }
    }
}

// ==========================================
// GROUPS TAB
// ==========================================
@Composable
fun GroupsTab(
    chatViewModel: ChatViewModel,
    currentUser: UserEntity,
    onNavigateToChat: (String) -> Unit
) {
    val groupList by chatViewModel.groups.collectAsState()
    var isCreatingGroup by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    var groupDescInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper dynamic dashboard header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EnglishPrimary),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EnglishPrimary, EnglishSecondary)
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "English Connect Classroom",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser.role == "TEACHER") {
                                "Hello Teacher ${currentUser.name}! Setup lessons and pin announcements below."
                            } else {
                                "Welcome student ${currentUser.name}! Tap your designated group to learn."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentUser.role == "TEACHER") Icons.Default.CastForEducation else Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = EnglishPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Study Groups",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EnglishOnBackground
                    )
                )
            }

            // Teachers can click to create groups
            if (currentUser.role == "TEACHER") {
                Button(
                    onClick = { isCreatingGroup = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_group_trigger")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Class", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // Group creation Dialog/Form
        if (isCreatingGroup) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EnglishPriorityBg),
                border = CardDefaults.outlinedCardBorder(true).copy(
                    brush = Brush.linearGradient(listOf(EnglishTeacherBadge, EnglishTeacherBadge))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create Learning Circle",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnglishTeacherBadge
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Circle Topic (e.g., IELTS Practice)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_group_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupDescInput,
                        onValueChange = { groupDescInput = it },
                        label = { Text("Topic Syllabus/Description") },
                        modifier = Modifier.fillMaxWidth().testTag("new_group_desc_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCreatingGroup = false }) {
                            Text("Cancel", color = EnglishMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (groupNameInput.isNotEmpty()) {
                                    chatViewModel.createGroup(groupNameInput, groupDescInput, currentUser)
                                    groupNameInput = ""
                                    groupDescInput = ""
                                    isCreatingGroup = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge),
                            modifier = Modifier.testTag("new_group_submit")
                        ) {
                            Text("Deploy Circle")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Group list view
        if (groupList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = EnglishMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No study groups yet. Create one above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EnglishMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(groupList) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chatViewModel.selectGroup(group.id)
                                onNavigateToChat(group.id)
                            }
                            .testTag("group_card_${group.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = CardDefaults.outlinedCardBorder(true).copy(
                            brush = Brush.linearGradient(listOf(EnglishPrimary, Color.Transparent))
                        )
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
                                    .background(EnglishBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImportContacts,
                                    contentDescription = null,
                                    tint = EnglishSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EnglishOnBackground
                                        )
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EnglishBackground)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PeopleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = EnglishPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${group.memberCount} members",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = EnglishPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = EnglishMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// AI ENGLISH ASSISTANT TAB
// ==========================================
@Composable
fun AIAssistantTab(aiViewModel: AIViewModel) {
    val aiHistory by aiViewModel.aiHistory.collectAsState()
    val isGenerating by aiViewModel.isGenerating.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll chat to end when new message gets logged
    LaunchedEffect(aiHistory.size, isGenerating) {
        if (aiHistory.isNotEmpty()) {
            listState.animateScrollToItem(aiHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EnglishSuccess.copy(alpha = 0.08f)),
            border = CardDefaults.outlinedCardBorder(true).copy(
                brush = Brush.linearGradient(listOf(EnglishAIBadge, EnglishAIBadge))
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EnglishAIBadge,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "AI English Grammar & Essay Helper",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground)
                    )
                    Text(
                        "Ask questions in English. Get teacher explanations & spelling fixes.",
                        style = MaterialTheme.typography.bodySmall.copy(color = EnglishMuted)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Prompt Chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "Fix my Grammar" to "Please correct the grammar mistakes in this paragraph: 'She do not goes to school yesterday because she were sick.'",
                "Expand IELTS Vocab" to "Give me 5 advanced IELTS alternatives for basic words like 'very good', 'bad', and 'happy'.",
                "Explain Past Tense" to "Explain the difference in usage between Present Perfect and Simple Past with quick examples."
            )

            suggestions.forEach { (label, prompt) ->
                AssistChip(
                    onClick = {
                        inputQuery = prompt
                    },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = EnglishPrimary,
                        leadingIconContentColor = EnglishPrimary
                    )
                )
            }
        }

        // Conversation list
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            items(aiHistory) { msg ->
                val isMe = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag(if (isMe) "ai_msg_user" else "ai_msg_bot"),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) EnglishBubbleMe else EnglishSurface
                        ),
                        elevation = CardDefaults.cardElevation(1.dp),
                        border = if (!isMe) CardDefaults.outlinedCardBorder(true).copy(
                            brush = Brush.linearGradient(listOf(EnglishAIBadge.copy(alpha = 0.5f), Color.Transparent))
                        ) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isMe) "My Query" else "English Connect Assistant (Tutor)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) EnglishSecondary else EnglishAIBadge
                                    )
                                )
                                Icon(
                                    imageVector = if (isMe) Icons.Default.Face else Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = if (isMe) EnglishSecondary else EnglishAIBadge,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = EnglishOnSurface,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EnglishBackground),
                            modifier = Modifier.testTag("ai_generating_indicator")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EnglishAIBadge,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Tutor is formulating explanations...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EnglishMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom text sender box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { aiViewModel.clearHistory() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EnglishPriorityBg)
                    .testTag("clear_ai_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = EnglishPriorityBorder
                )
            }

            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask English questions...") },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputQuery.isNotEmpty() && !isGenerating) {
                        aiViewModel.askTutor(inputQuery)
                        inputQuery = ""
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input_field")
            )

            FloatingActionButton(
                onClick = {
                    if (inputQuery.isNotEmpty() && !isGenerating) {
                        aiViewModel.askTutor(inputQuery)
                        inputQuery = ""
                    }
                },
                containerColor = EnglishAIBadge,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_ai_query_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==========================================
// NOTIFICATIONS / FCM ALERTS TAB
// ==========================================
@Composable
fun NotificationsTab(notificationViewModel: NotificationViewModel) {
    val logs by notificationViewModel.notificationLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EnglishSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FCM Push Logger Logs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            TextButton(
                onClick = { notificationViewModel.clearAndMarkAllAsRead() },
                colors = ButtonColors(
                    containerColor = EnglishPriorityBg,
                    contentColor = EnglishPriorityBorder,
                    disabledContentColor = Color.Gray,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("clear_alerts_button")
            ) {
                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dismiss All", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(64.dp), tint = EnglishMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No push notification logs captured yet.", color = EnglishMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Alerts arrive when tasks or files get posted.", style = MaterialTheme.typography.bodySmall, color = EnglishMuted)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(logs) { log ->
                    val colorAndIcon = when (log.category) {
                        "ANNOUNCEMENT" -> EnglishPriorityBorder to Icons.Default.Campaign
                        "FILE" -> EnglishTeacherBadge to Icons.Default.FolderZip
                        "CHAT" -> EnglishPrimary to Icons.Default.Forum
                        else -> EnglishSuccess to Icons.Default.BookmarkAdded
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        border = CardDefaults.outlinedCardBorder(true).copy(
                            brush = Brush.linearGradient(listOf(colorAndIcon.first.copy(alpha = 0.4f), Color.Transparent))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(colorAndIcon.first.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = colorAndIcon.second,
                                    contentDescription = null,
                                    tint = colorAndIcon.first,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground)
                                    )
                                    Text(
                                        text = log.category,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorAndIcon.first
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = log.text, style = MaterialTheme.typography.bodySmall, color = EnglishOnSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFILE TAB
// ==========================================
@Composable
fun ProfileTab(authViewModel: AuthViewModel, chatViewModel: ChatViewModel, currentUser: UserEntity) {
    val groups by chatViewModel.groups.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper background header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = currentUser.profilePhotoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(EnglishBackground),
                            contentScale = ContentScale.Crop,
                            error = remember { null } // fallbacks automatically
                        )

                        // If error, draw custom letter circle
                        if (currentUser.profilePhotoUrl.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(EnglishPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.name.take(1).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Role small indicator icon
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (currentUser.role == "TEACHER") EnglishTeacherBadge else EnglishPrimary,
                                    CircleShape
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser.role == "TEACHER") Icons.Default.CastForEducation else Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentUser.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnglishOnBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentUser.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EnglishMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentUser.role == "TEACHER") EnglishTeacherBadge.copy(alpha = 0.12f) else EnglishPrimary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentUser.role == "TEACHER") "TEACHER & ADMIN" else "STUDENT ACADEMIC",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser.role == "TEACHER") EnglishTeacherBadge else EnglishPrimary
                            )
                        )
                    }
                }
            }
        }

        // Stats card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EnglishBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = groups.size.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = EnglishPrimary)
                        )
                        Text(text = "Class Circles", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentUser.role == "TEACHER") "2" else "1",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = EnglishTeacherBadge)
                        )
                        Text(text = "Files Shared", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "A+",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = EnglishSuccess)
                        )
                        Text(text = "AI Engagement", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Group joined details list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enrolled Study Circles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    groups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = EnglishSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = { authViewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EnglishPriorityBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out Google Account")
            }
        }
    }
}
