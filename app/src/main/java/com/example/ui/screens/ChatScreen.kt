package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeGroup by chatViewModel.activeGroup.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val resources by chatViewModel.resources.collectAsState()
    val typingUsersMap by chatViewModel.typingUsers.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var activeSubView by remember { mutableStateOf<String?>("CHAT") } // "CHAT", "RESOURCES", "SETTINGS"

    // Thread Reply logic
    var replyToMessage by remember { mutableStateOf<MessageEntity?>(null) }

    // Simulated Voice Recorder state
    var isRecordingSimulated by remember { mutableStateOf(false) }
    var voiceRecordDuration by remember { mutableStateOf(0) }
    var voicePlaybackState by remember { mutableStateOf<Int?>(null) } // Target message ID currently playing

    // Automated scroll-to-bottom
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recorder timer counter
    LaunchedEffect(isRecordingSimulated) {
        if (isRecordingSimulated) {
            voiceRecordDuration = 0
            while (isRecordingSimulated) {
                delay(1000)
                voiceRecordDuration += 1
            }
        }
    }

    val group = activeGroup ?: return

    val typingList = typingUsersMap[group.id] ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                // Main Header
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EnglishOnBackground)
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group.memberCount} members online",
                                style = MaterialTheme.typography.bodySmall.copy(color = EnglishSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    actions = {
                        // Resource Library Toggle
                        IconButton(
                            onClick = { activeSubView = if (activeSubView == "RESOURCES") "CHAT" else "RESOURCES" },
                            modifier = Modifier.testTag("toggle_resources_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderShared,
                                contentDescription = "Resources",
                                tint = if (activeSubView == "RESOURCES") EnglishPrimary else EnglishMuted
                            )
                        }

                        // Group Settings / Classroom Panel
                        IconButton(
                            onClick = { activeSubView = if (activeSubView == "SETTINGS") "CHAT" else "SETTINGS" },
                            modifier = Modifier.testTag("toggle_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (activeSubView == "SETTINGS") EnglishTeacherBadge else EnglishMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Pinned Announcement Banner (if any is active on group)
                val pinnedId = group.pinnedAnnouncementId
                val pinnedMessage = messages.find { it.id == pinnedId }
                if (pinnedMessage != null) {
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = EnglishPriorityBg,
                            border = null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "Announcement",
                                        tint = EnglishPriorityBorder,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Pinned Announcement • ${pinnedMessage.senderName}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EnglishPriorityBorder
                                            )
                                        )
                                        Text(
                                            text = pinnedMessage.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = EnglishOnSurface
                                        )
                                    }
                                }

                                Row {
                                    // Scroll to message
                                    IconButton(
                                        onClick = {
                                            val index = messages.indexOfFirst { it.id == pinnedMessage.id }
                                            if (index >= 0) {
                                                scope.launch { listState.scrollToItem(index) }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveRedEye, contentDescription = "View", tint = EnglishSecondary, modifier = Modifier.size(16.dp))
                                    }

                                    // Dismiss/Unpin if teacher
                                    if (currentUser.role == "TEACHER") {
                                        IconButton(
                                            onClick = { chatViewModel.unpinAnnouncement() },
                                            modifier = Modifier.size(32.dp).testTag("unpin_announcement_button")
                                        ) {
                                            Icon(Icons.Default.PinDrop, contentDescription = "Unpin", tint = EnglishMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(EnglishBackground)
        ) {
            when (activeSubView) {
                "RESOURCES" -> ResourceLibraryView(
                    resources = resources,
                    onUploadClicked = { title, ext, size ->
                        chatViewModel.uploadSimulatedResource(title, ext, size, currentUser)
                        Toast.makeText(context, "$title ($ext) Shared successfully!", Toast.LENGTH_SHORT).show()
                    },
                    currentUser = currentUser
                )

                "SETTINGS" -> ClassSettingsView(
                    group = group,
                    chatViewModel = chatViewModel,
                    currentUser = currentUser,
                    onBackToChat = { activeSubView = "CHAT" }
                )

                else -> {
                    // MAIN CHAT SCREEN
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Message list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            items(messages) { msg ->
                                MessageRowItem(
                                    msg = msg,
                                    currentUser = currentUser,
                                    onReactionSelected = { emoji ->
                                        chatViewModel.toggleReaction(msg.id, emoji, currentUser.email)
                                    },
                                    onDeleteMessage = {
                                        chatViewModel.deleteMessage(msg.id)
                                    },
                                    onReplyToMessage = {
                                        replyToMessage = msg
                                    },
                                    onPinAsAnnouncement = {
                                        chatViewModel.pinAnnouncement(msg.id)
                                    },
                                    voicePlaybackState = voicePlaybackState,
                                    onToggleVoicePlay = { id ->
                                        voicePlaybackState = if (voicePlaybackState == id) null else id
                                    }
                                )
                            }

                            // Peer typing feedback
                            if (typingList.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = EnglishSurface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    color = EnglishPrimary,
                                                    strokeWidth = 1.6.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "${typingList.joinToString(", ")} is typing...",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                    color = EnglishPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reply quoting board overlay
                        if (replyToMessage != null) {
                            val reply = replyToMessage!!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EnglishBubbleMe)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = EnglishPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Replying to @${reply.senderName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EnglishPrimary)
                                        )
                                        Text(
                                            text = reply.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { replyToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Simulated voice note recording state UI bar
                        if (isRecordingSimulated) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EnglishPriorityBg)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = EnglishPriorityBorder)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recording Simulated Audio: ${voiceRecordDuration}s",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EnglishPriorityBorder
                                        )
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { isRecordingSimulated = false }) {
                                        Text("Discard", color = EnglishMuted)
                                    }
                                    Button(
                                        onClick = {
                                            chatViewModel.sendVoiceNote(voiceRecordDuration, currentUser)
                                            isRecordingSimulated = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EnglishPriorityBorder)
                                    ) {
                                        Text("Stop & Send Voice")
                                    }
                                }
                            }
                        }

                        // Chat Sender Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(12.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // File attachment simulation key sheet
                            var showAttachmentOptions by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showAttachmentOptions = !showAttachmentOptions },
                                    modifier = Modifier.testTag("attachment_menu_button")
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = EnglishSecondary)
                                }

                                DropdownMenu(
                                    expanded = showAttachmentOptions,
                                    onDismissRequest = { showAttachmentOptions = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = EnglishPriorityBorder) },
                                        text = { Text("Attach Assignment PDF") },
                                        onClick = {
                                            chatViewModel.uploadSimulatedResource("IELTS Essay Task #2 Format Checklist", "PDF", "1.4 MB", currentUser)
                                            showAttachmentOptions = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = EnglishPrimary) },
                                        text = { Text("Attach Grammar Photo Example") },
                                        onClick = {
                                            chatViewModel.sendPhoto("tense_formulas.jpg", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&q=80&w=400", currentUser)
                                            showAttachmentOptions = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null, tint = EnglishTeacherBadge) },
                                        text = { Text("Attach Audio Spoken Lesson") },
                                        onClick = {
                                            chatViewModel.uploadSimulatedResource("British Accent Pronunciation Practice Round", "AUDIO", "6.2 MB", currentUser)
                                            showAttachmentOptions = false
                                        }
                                    )
                                }
                            }

                            // Dynamic inputs (checks if Priority announcement option selected)
                            var isPriorityToggle by remember { mutableStateOf(false) }
                            var isAnnouncementToggle by remember { mutableStateOf(false) }

                            // Teachers show Priority controllers
                            if (currentUser.role == "TEACHER") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            isPriorityToggle = !isPriorityToggle
                                            if (isPriorityToggle) isAnnouncementToggle = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PriorityHigh,
                                            contentDescription = "Priority",
                                            tint = if (isPriorityToggle) EnglishPriorityBorder else EnglishMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            isAnnouncementToggle = !isAnnouncementToggle
                                            if (isAnnouncementToggle) isPriorityToggle = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = "Announcement",
                                            tint = if (isAnnouncementToggle) EnglishSuccess else EnglishMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = {
                                    Text(
                                        text = when {
                                            isAnnouncementToggle -> "Assemble announcement..."
                                            isPriorityToggle -> "Compose priority note..."
                                            else -> "Type classroom message..."
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                singleLine = false,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (textInput.isNotEmpty()) {
                                        chatViewModel.sendMessage(
                                            content = textInput,
                                            sender = currentUser,
                                            isPriority = isPriorityToggle,
                                            isAnnouncement = isAnnouncementToggle,
                                            replyToId = replyToMessage?.id,
                                            replyToText = replyToMessage?.content
                                        )
                                        textInput = ""
                                        replyToMessage = null
                                        isPriorityToggle = false
                                        isAnnouncementToggle = false
                                    }
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_text_field")
                            )

                            // Send Voice note if field empty, else standard Text Send
                            if (textInput.trim().isEmpty() && !isRecordingSimulated) {
                                FloatingActionButton(
                                    onClick = { isRecordingSimulated = true },
                                    containerColor = EnglishPriorityBorder,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("record_voice_fab")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice note", modifier = Modifier.size(20.dp))
                                }
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        chatViewModel.sendMessage(
                                            content = textInput,
                                            sender = currentUser,
                                            isPriority = isPriorityToggle,
                                            isAnnouncement = isAnnouncementToggle,
                                            replyToId = replyToMessage?.id,
                                            replyToText = replyToMessage?.content
                                        )
                                        textInput = ""
                                        replyToMessage = null
                                        isPriorityToggle = false
                                        isAnnouncementToggle = false
                                    },
                                    containerColor = EnglishPrimary,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// INDIVIDUAL MESSAGE BUBBLE ROW (With Reactions)
// ==========================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessageRowItem(
    msg: MessageEntity,
    currentUser: UserEntity,
    onReactionSelected: (String) -> Unit,
    onDeleteMessage: () -> Unit,
    onReplyToMessage: () -> Unit,
    onPinAsAnnouncement: () -> Unit,
    voicePlaybackState: Int?,
    onToggleVoicePlay: (Int) -> Unit
) {
    val isMe = msg.senderEmail == currentUser.email
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(msg.timestamp) { dateFormat.format(Date(msg.timestamp)) }

    // Dropdown for message long press choices
    var showPressMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Sender name banner (if not me)
            if (!isMe) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground, fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Teacher role badge
                    if (msg.senderRole == "TEACHER") {
                        Text(
                            text = "TEACHER",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EnglishTeacherBadge,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EnglishTeacherBadge.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Main Message bubble Box
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .combinedClickable(
                        onClick = {
                            if (msg.attachmentType == "AUDIO") {
                                onToggleVoicePlay(msg.id)
                            }
                        },
                        onLongClick = { showPressMenu = true }
                    )
                    .testTag("message_bubble_${msg.id}"),
                shape = RoundedCornerShape(
                    topStart = if (!isMe) 2.dp else 16.dp,
                    topEnd = if (isMe) 2.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        msg.isPriority -> EnglishBubblePriority
                        isMe -> EnglishBubbleMe
                        else -> EnglishSurface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.6.dp),
                border = if (msg.isPriority) CardDefaults.outlinedCardBorder(true).copy(
                    brush = Brush.linearGradient(listOf(EnglishPriorityBorder, EnglishPriorityBorder))
                ) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Quoted Threaded Reply Banner If Any
                    if (msg.replyToId != null && msg.replyToText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .padding(6.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(3.dp)
                                        .background(EnglishSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        "Quote",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EnglishSecondary, fontSize = 9.sp)
                                    )
                                    Text(
                                        text = msg.replyToText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Priority Marker label inside bubble
                    if (msg.isPriority) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EnglishPriorityBorder, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PRIORITY ANNOUNCEMENT",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EnglishPriorityBorder, fontSize = 8.sp)
                            )
                        }
                    }

                    // Display Image if photo attachment
                    if (msg.attachmentType == "IMAGE" && msg.attachmentPath != null) {
                        AsyncImage(
                            model = msg.attachmentPath,
                            contentDescription = "Photo Attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Display Voice notes simulation
                    if (msg.attachmentType == "AUDIO") {
                        val isPlaying = voicePlaybackState == msg.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EnglishPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { onToggleVoicePlay(msg.id) }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice",
                                        tint = EnglishPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPlaying) "Playing audio voice..." else "Click to Play Voice Note",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = if (isPlaying) 0.6f else 0.0f,
                                    color = EnglishPrimary,
                                    trackColor = EnglishBackground,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                )
                            }
                        }
                    }

                    // Content text
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium.copy(color = EnglishOnBackground)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Small timestamp and read tick indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = EnglishMuted)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = if (msg.isRead) EnglishPrimary else EnglishMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Display Message Reactions (👍, ❤️ etc) under bubble if present
            if (msg.reactions.isNotEmpty()) {
                val reactionsParsed = msg.reactions.split(",").map { it.split(":") }.filter { it.size == 2 }
                val reactionCounts = reactionsParsed.groupBy { it[0] }.mapValues { it.value.size }

                Row(
                    modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reactionCounts.forEach { (emoji, count) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(0.6.dp, EnglishPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(count.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EnglishPrimary)
                        }
                    }
                }
            }
        }

        // Dropdown Menu Choice Panel on Long Press
        DropdownMenu(
            expanded = showPressMenu,
            onDismissRequest = { showPressMenu = false }
        ) {
            // Emojis reaction quick selector
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        listOf("👍", "❤️", "👏", "🔥", "😮").forEach { emoji ->
                            Text(
                                emoji,
                                modifier = Modifier
                                    .clickable {
                                        onReactionSelected(emoji)
                                        showPressMenu = false
                                    }
                                    .padding(4.dp),
                                fontSize = 20.sp
                            )
                        }
                    }
                },
                onClick = {}
            )

            HorizontalDivider()

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                text = { Text("Quoted Reply") },
                onClick = {
                    onReplyToMessage()
                    showPressMenu = false
                }
            )

            // Pin to group announcements if teacher
            if (currentUser.role == "TEACHER") {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = EnglishSecondary) },
                    text = { Text("Pin Class Announcement") },
                    onClick = {
                        onPinAsAnnouncement()
                        showPressMenu = false
                    }
                )
            }

            // Delete option: Teacher can delete any, student can delete native
            if (currentUser.role == "TEACHER" || isMe) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = EnglishPriorityBorder) },
                    text = { Text("Delete Message") },
                    onClick = {
                        onDeleteMessage()
                        showPressMenu = false
                    }
                )
            }
        }
    }
}

// ==========================================
// RESOURCE LIBRARY TAB VIEW
// ==========================================
@Composable
fun ResourceLibraryView(
    resources: List<ResourceEntity>,
    onUploadClicked: (String, String, String) -> Unit,
    currentUser: UserEntity
) {
    var resourceTitleInput by remember { mutableStateOf("") }
    var resourceTypeInput by remember { mutableStateOf("PDF") } // "PDF", "DOCX", "AUDIO", "WORKSHEET"
    var showUploadSheet by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = EnglishPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resource Library & Worksheets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground)
                )
            }

            // Teachers upload resources
            if (currentUser.role == "TEACHER") {
                Button(
                    onClick = { showUploadSheet = !showUploadSheet },
                    colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add resource", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Form to upload new worksheet lessons if toggled
        if (showUploadSheet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EnglishPriorityBg),
                border = CardDefaults.outlinedCardBorder(true).copy(
                    brush = Brush.linearGradient(listOf(EnglishTeacherBadge, EnglishTeacherBadge))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Upload study file lessons",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EnglishTeacherBadge)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resourceTitleInput,
                        onValueChange = { resourceTitleInput = it },
                        label = { Text("File Title (e.g., Prefixes Worksheet)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("resource_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PDF", "DOCX", "AUDIO", "WORKSHEET").forEach { ext ->
                            val isSelected = resourceTypeInput == ext
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) EnglishTeacherBadge else Color.White)
                                    .clickable { resourceTypeInput = ext }
                                    .border(0.6.dp, EnglishTeacherBadge, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    ext,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else EnglishTeacherBadge,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showUploadSheet = false }) {
                            Text("Cancel", color = EnglishMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (resourceTitleInput.isNotEmpty()) {
                                    val sizeRandom = "${(2..9).random()}.${(0..9).random()} MB"
                                    onUploadClicked(resourceTitleInput, resourceTypeInput, sizeRandom)
                                    resourceTitleInput = ""
                                    showUploadSheet = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge),
                            modifier = Modifier.testTag("resource_submit")
                        ) {
                            Text("Publish Resource")
                        }
                    }
                }
            }
        }

        // Resources list
        if (resources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(48.dp), tint = EnglishMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Class file registry is empty. Add homework sheets!", color = EnglishMuted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(resources) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val logoColor = when (item.fileType) {
                                "PDF" -> EnglishPriorityBorder
                                "DOCX" -> EnglishPrimary
                                "AUDIO" -> EnglishAIBadge
                                else -> EnglishTeacherBadge
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(logoColor.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.fileType) {
                                        "PDF" -> Icons.Default.PictureAsPdf
                                        "AUDIO" -> Icons.Default.Audiotrack
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = logoColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.fileType} • ${item.fileSize} • Uploaded by ${item.uploaderName}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EnglishMuted, fontSize = 10.sp)
                                )
                            }

                            // Download icon simulation key
                            var isSimDownloaded by remember { mutableStateOf(false) }
                            IconButton(onClick = { isSimDownloaded = true }) {
                                Icon(
                                    imageVector = if (isSimDownloaded) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                                    contentDescription = "Download",
                                    tint = if (isSimDownloaded) EnglishSuccess else EnglishPrimary
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
// CLASS / GROUP CONTROL AND MANAGEMENT PANEL
// ==========================================
@Composable
fun ClassSettingsView(
    group: GroupEntity,
    chatViewModel: ChatViewModel,
    currentUser: UserEntity,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    var addMemberInput by remember { mutableStateOf("") }

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
                Icon(Icons.Default.SettingsApplications, contentDescription = null, tint = EnglishTeacherBadge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Classroom Circle Admin",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            IconButton(onClick = onBackToChat) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card displaying current circle description info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EnglishBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnglishOnBackground)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = EnglishMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin features: Add custom simulated target students
        if (currentUser.role == "TEACHER") {
            Text(
                "Add Class Member",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = addMemberInput,
                    onValueChange = { addMemberInput = it },
                    placeholder = { Text("Email (e.g., student@school.edu)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("add_member_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (addMemberInput.isNotEmpty()) {
                            Toast.makeText(context, "$addMemberInput enrolled successfully!", Toast.LENGTH_SHORT).show()
                            addMemberInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnglishTeacherBadge)
                ) {
                    Text("Enroll")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Manage existing student actions (Muting students and promoting assistant teachers)
            Text(
                "Manage Connected Students",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val studentListMock = listOf(
                "jane.doe@student.com" to "Jane Doe (IELTS Aspirant)",
                "alex.smith@student.com" to "Alex Smith (Advanced Composition)",
                "maria.gonzalez@student.com" to "Maria Gonzalez (Spoken Accent Unit)"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.5.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    studentListMock.forEach { (email, name) ->
                        val isMuted = group.mutedStudentEmails.split(",").contains(email)
                        val isAssistant = group.assistantTeacherEmails.split(",").contains(email)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(email, style = MaterialTheme.typography.bodySmall, color = EnglishMuted)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Assistant promote trigger
                                AssistChip(
                                    onClick = { chatViewModel.togglePromoteAssistantTeacher(email) },
                                    label = {
                                        Text(
                                            if (isAssistant) "Assistant Coach" else "Promote Coach",
                                            fontSize = 9.sp
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = if (isAssistant) EnglishSecondary else EnglishMuted,
                                        containerColor = if (isAssistant) EnglishSecondary.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                )

                                // Mute Student toggle trigger
                                AssistChip(
                                    onClick = { chatViewModel.toggleMuteStudent(email) },
                                    label = {
                                        Text(
                                            if (isMuted) "Muted" else "Mute Stud",
                                            fontSize = 9.sp
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = if (isMuted) EnglishPriorityBorder else EnglishSuccess,
                                        containerColor = if (isMuted) EnglishPriorityBg else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Class circle destruction
            Button(
                onClick = {
                    chatViewModel.deleteActiveGroup()
                    Toast.makeText(context, "Class Circle Disbanded", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("delete_classroom_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EnglishPriorityBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disband Study Group Circle")
            }
        } else {
            // Student Settings View (no direct admin keys, shows class circle directory stats)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EnglishBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EnglishPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Student Classroom Guide",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EnglishPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are enrolled as a student. You have full access to study resources, real-time discussions, priorities, and reactions. Feel safe to ask the English AI companion for study helper lessons!",
                        style = MaterialTheme.typography.bodySmall,
                        color = EnglishOnSurface
                    )
                }
            }
        }
    }
}
