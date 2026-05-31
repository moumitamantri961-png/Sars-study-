package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Auth ViewModel ---
class AuthViewModel(private val repository: AppRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRegisteredUsers: StateFlow<List<UserEntity>> = repository.allRegisteredUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep track of whether the user is in the process of selection
    private val _showRoleSelection = MutableStateFlow(false)
    val showRoleSelection: StateFlow<Boolean> = _showRoleSelection.asStateFlow()

    fun signInWithGoogle(email: String, name: String, photoUrl: String, onboardingNeeded: Boolean) {
        viewModelScope.launch {
            // Default first login role setup
            val defaultRole = "STUDENT" // Overwritten during role selection
            repository.loginAs(email, name, photoUrl, defaultRole)
            if (onboardingNeeded) {
                _showRoleSelection.value = true
            }
        }
    }

    fun selectRole(role: String) {
        viewModelScope.launch {
            repository.updateCurrentUserRole(role)
            _showRoleSelection.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.logout()
            _showRoleSelection.value = false
        }
    }
}

// --- Chat ViewModel ---
class ChatViewModel(private val repository: AppRepository) : ViewModel() {

    // List of groups
    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Group ID selection
    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    // Active Group Details
    val activeGroup: StateFlow<GroupEntity?> = _activeGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Group Messages
    val messages: StateFlow<List<MessageEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Group Resources
    val resources: StateFlow<List<ResourceEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getResourcesForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message search query and results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<MessageEntity>> = combine(_activeGroupId, _searchQuery) { id, query ->
        id to query
    }.flatMapLatest { (id, query) ->
        if (id != null && query.isNotEmpty()) {
            repository.searchMessages(id, query)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typing Indicators (Map of GroupId to list of users currently typing)
    private val _typingUsers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<String, List<String>>> = _typingUsers.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGroup(groupId: String?) {
        _activeGroupId.value = groupId
        _searchQuery.value = ""
    }

    // --- Message Interactions ---
    fun sendMessage(
        content: String,
        sender: UserEntity,
        isPriority: Boolean = false,
        isAnnouncement: Boolean = false,
        replyToId: Int? = null,
        replyToText: String? = null
    ) {
        val groupId = _activeGroupId.value ?: return
        if (content.trim().isEmpty()) return

        viewModelScope.launch {
            // Check if student is muted inside this group
            val group = repository.getGroupFlow(groupId).firstOrNull()
            val isMuted = group?.mutedStudentEmails?.split(",")?.contains(sender.email) == true
            if (isMuted && sender.role == "STUDENT") {
                // Ignore send or add warning
                repository.addLocalNotification(
                    title = "Failed to Send Message",
                    text = "You have been muted by the teacher in ${group.name}.",
                    category = "CHAT"
                )
                return@launch
            }

            repository.sendMessage(
                groupId = groupId,
                senderEmail = sender.email,
                senderName = sender.name,
                senderRole = sender.role,
                content = content,
                isPriority = isPriority,
                isAnnouncement = isAnnouncement,
                replyToId = replyToId,
                replyToText = replyToText
            )

            // Auto-simulation of other study members for lifelike interactions:
            simulatePeerResponses(content, sender, group)
        }
    }

    private suspend fun simulatePeerResponses(content: String, sender: UserEntity, group: GroupEntity?) {
        if (group == null) return
        val lower = content.lowercase()

        // Students respond to announcements or questions
        if (sender.role == "TEACHER" && (lower.contains("homework") || lower.contains("assignment") || lower.contains("welcome") || lower.contains("test"))) {
            delay(2000)
            // Show typing indicator
            val typingKey = group.id
            _typingUsers.value = _typingUsers.value + (typingKey to listOf("Jane Doe"))
            delay(1500)
            _typingUsers.value = _typingUsers.value - typingKey

            repository.sendMessage(
                groupId = group.id,
                senderEmail = "jane.doe@student.com",
                senderName = "Jane Doe (Student)",
                senderRole = "STUDENT",
                content = "Got it, Mr. Harris! I will check the Resource Library right away and submit by tomorrow evening.",
                replyToId = null
            )
        }
    }

    fun uploadSimulatedResource(title: String, fileType: String, size: String, uploader: UserEntity) {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.uploadResource(
                groupId = groupId,
                title = title,
                fileType = fileType,
                fileSize = size,
                uploaderName = uploader.name,
                uploaderEmail = uploader.email
            )
        }
    }

    fun sendVoiceNote(durationSeconds: Int, sender: UserEntity) {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                groupId = groupId,
                senderEmail = sender.email,
                senderName = sender.name,
                senderRole = sender.role,
                content = "🎤 Voice Note ($durationSeconds seconds)",
                attachmentPath = "simulated_audio_note.aac",
                attachmentName = "VoiceNote_${System.currentTimeMillis()}.aac",
                attachmentType = "AUDIO"
            )
        }
    }

    fun sendPhoto(photoName: String, photoUri: String, sender: UserEntity) {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                groupId = groupId,
                senderEmail = sender.email,
                senderName = sender.name,
                senderRole = sender.role,
                content = "📷 Photo Shared",
                attachmentPath = photoUri,
                attachmentName = photoName,
                attachmentType = "IMAGE"
            )
        }
    }

    fun toggleReaction(messageId: Int, emoji: String, userEmail: String) {
        viewModelScope.launch {
            repository.toggleReaction(messageId, emoji, userEmail)
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun pinAnnouncement(messageId: Int) {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.pinAnnouncementAtTop(groupId, messageId)
        }
    }

    fun unpinAnnouncement() {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.pinAnnouncementAtTop(groupId, null)
        }
    }

    // --- Teacher Actions ---
    fun createGroup(name: String, desc: String, teacher: UserEntity) {
        viewModelScope.launch {
            repository.createGroup(name, desc, teacher.email, teacher.name)
        }
    }

    fun deleteActiveGroup() {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteGroup(groupId)
            _activeGroupId.value = null
        }
    }

    fun toggleMuteStudent(studentEmail: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            val list = group.mutedStudentEmails.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (list.contains(studentEmail)) {
                list.remove(studentEmail)
            } else {
                list.add(studentEmail)
            }
            repository.updateGroupMutesAndAssistants(
                groupId = group.id,
                mutedEmails = list.joinToString(","),
                assistants = group.assistantTeacherEmails
            )
        }
    }

    fun togglePromoteAssistantTeacher(studentEmail: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            val list = group.assistantTeacherEmails.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (list.contains(studentEmail)) {
                list.remove(studentEmail)
            } else {
                list.add(studentEmail)
            }
            repository.updateGroupMutesAndAssistants(
                groupId = group.id,
                mutedEmails = group.mutedStudentEmails,
                assistants = list.joinToString(",")
            )
        }
    }
}

// --- AI Assistant ViewModel ---
class AIViewModel(private val repository: AppRepository) : ViewModel() {

    val aiHistory: StateFlow<List<AIMessageEntity>> = repository.aiMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        // Pre-populate with welcoming AI helper intro if chat has no entries
        viewModelScope.launch {
            val history = repository.aiMessages.firstOrNull() ?: emptyList()
            if (history.isEmpty()) {
                repository.sendAIMessageAssistant(
                    "Hello! I am your AI English Study Companion. 🎓\n\n" +
                    "I am here to guide you in your English learning journey. You can ask me to:\n" +
                    "- **Refine & correct** your paragraphs/essays.\n" +
                    "- Explain tricky **grammatical tenses**.\n" +
                    "- Suggest dynamic **TOEFL/IELTS vocabulary**.\n" +
                    "- **Translate** your sentence or native thoughts into professional English.\n\n" +
                    "What would you like to review or compose today?"
                )
            }
        }
    }

    fun askTutor(query: String) {
        if (query.trim().isEmpty() || _isGenerating.value) return
        viewModelScope.launch {
            _isGenerating.value = true
            // Save user prompt in localized db
            repository.sendAIMessageUser(query)

            // Invoke direct Gemini model corrected response
            val reply = repository.requestAIEnglishCorrection(query)

            // Save AI reply in localized db
            repository.sendAIMessageAssistant(reply)
            _isGenerating.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAIChat()
            repository.sendAIMessageAssistant("Chat history erased. What English concepts shall we study next?")
        }
    }
}

// --- Push Notification ViewModel ---
class NotificationViewModel(private val repository: AppRepository) : ViewModel() {

    val notificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationLogs: StateFlow<List<NotificationLogEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAndMarkAllAsRead() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun triggerMockNotification(title: String, text: String, category: String) {
        viewModelScope.launch {
            repository.addLocalNotification(title, text, category)
        }
    }
}

// --- Universal ViewModel Factory ---
class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(repository) as T
            modelClass.isAssignableFrom(AIViewModel::class.java) -> AIViewModel(repository) as T
            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> NotificationViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
