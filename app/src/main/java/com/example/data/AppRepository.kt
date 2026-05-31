package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AppRepository(private val db: AppDatabase) {

    // --- Flows ---
    val currentUser: Flow<UserEntity?> = db.userDao().getCurrentUser()
    val allRegisteredUsers: Flow<List<UserEntity>> = db.userDao().getAllRegisteredUsers()
    val allGroups: Flow<List<GroupEntity>> = db.groupDao().getAllGroups()
    val aiMessages: Flow<List<AIMessageEntity>> = db.aiMessageDao().getAllAIMessages()
    val unreadNotificationCount: Flow<Int> = db.notificationLogDao().getUnreadCount()
    val allNotifications: Flow<List<NotificationLogEntity>> = db.notificationLogDao().getNotifications()

    // --- Setup/Seed Sample Data if empty ---
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Core initial simulated users:
        val existingUsers = db.userDao().getAllRegisteredUsers().firstOrNull() ?: emptyList()
        if (existingUsers.isEmpty()) {
            val teacherUser = UserEntity(
                email = "mr.harris@englishconnect.edu",
                name = "Mr. Harris (Lead Instructor)",
                profilePhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=120",
                role = "TEACHER",
                isPrimaryAdmin = true
            )
            val studentUser = UserEntity(
                email = "jane.doe@student.com",
                name = "Jane Doe (IELTS Aspirant)",
                profilePhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=120",
                role = "STUDENT"
            )
            db.userDao().insertUser(teacherUser)
            db.userDao().insertUser(studentUser)
        }

        // Core study groups:
        val existingGroups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
        if (existingGroups.isEmpty()) {
            val group1 = GroupEntity(
                id = "grammar_mastery",
                name = "Grammar & Composition Elite",
                description = "Master English syntax, advanced tenses, punctuation, and style guide drills.",
                creatorEmail = "mr.harris@englishconnect.edu",
                creatorName = "Mr. Harris",
                memberCount = 14
            )
            val group2 = GroupEntity(
                id = "ielts_prep_2026",
                name = "IELTS & TOEFL Prep Club",
                description = "Target band 8.0+ of academic readings, intensive essay feedback, and spoken card topics.",
                creatorEmail = "mr.harris@englishconnect.edu",
                creatorName = "Mr. Harris",
                memberCount = 9
            )
            db.groupDao().insertGroup(group1)
            db.groupDao().insertGroup(group2)

            // Seed initial welcome messages
            db.messageDao().insertMessage(
                MessageEntity(
                    groupId = "grammar_mastery",
                    senderEmail = "mr.harris@englishconnect.edu",
                    senderName = "Mr. Harris (Lead)",
                    senderRole = "TEACHER",
                    content = "Welcome everyone to our English Elite community! Here we will refine our grammar weekly. Make sure to download the Grammar Guide PDF from our resource library.",
                    isPriority = true,
                    isAnnouncement = true
                )
            )

            db.messageDao().insertMessage(
                MessageEntity(
                    groupId = "grammar_mastery",
                    senderEmail = "jane.doe@student.com",
                    senderName = "Jane Doe",
                    senderRole = "STUDENT",
                    content = "Thank you Mr. Harris! Really excited to improve my phrasing and prepositions.",
                    isRead = true
                )
            )

            // Seed resources
            db.resourceDao().insertResource(
                ResourceEntity(
                    groupId = "grammar_mastery",
                    title = "Advanced prepositions & active voice formulas",
                    fileType = "PDF",
                    fileSize = "2.4 MB",
                    uploaderName = "Mr. Harris",
                    uploaderEmail = "mr.harris@englishconnect.edu"
                )
            )
            db.resourceDao().insertResource(
                ResourceEntity(
                    groupId = "grammar_mastery",
                    title = "Weekly descriptive homework task #1",
                    fileType = "DOCX",
                    fileSize = "342 KB",
                    uploaderName = "Mr. Harris",
                    uploaderEmail = "mr.harris@englishconnect.edu"
                )
            )
        }
    }

    // --- Authentication ---
    suspend fun loginAs(email: String, name: String, photoUrl: String, role: String) = withContext(Dispatchers.IO) {
        db.userDao().logoutAll()
        val user = UserEntity(
            email = email,
            name = name,
            profilePhotoUrl = photoUrl,
            role = role,
            isPrimaryAdmin = (role == "TEACHER"),
            isLoggedIn = true
        )
        db.userDao().insertUser(user)
        // Add welcome notification
        addLocalNotification(
            title = "Signed In Successfully",
            text = "Welcome back, $name! You are logged in as a $role.",
            category = "AI_REMINDER"
        )
    }

    suspend fun updateCurrentUserRole(role: String) = withContext(Dispatchers.IO) {
        val current = db.userDao().getCurrentUser().firstOrNull() ?: return@withContext
        val updated = current.copy(role = role, isPrimaryAdmin = (role == "TEACHER"))
        db.userDao().insertUser(updated)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        db.userDao().logoutAll()
    }

    // --- Groups Management ---
    fun getGroupFlow(groupId: String): Flow<GroupEntity?> {
        return db.groupDao().getGroupById(groupId)
    }

    suspend fun createGroup(name: String, description: String, creatorEmail: String, creatorName: String) = withContext(Dispatchers.IO) {
        val id = "group_" + System.currentTimeMillis()
        val group = GroupEntity(
            id = id,
            name = name,
            description = description,
            creatorEmail = creatorEmail,
            creatorName = creatorName,
            memberCount = 1
        )
        db.groupDao().insertGroup(group)

        // Add automated welcome announcement
        db.messageDao().insertMessage(
            MessageEntity(
                groupId = id,
                senderEmail = creatorEmail,
                senderName = creatorName,
                senderRole = "TEACHER",
                content = "Welcome to our newly formed learning study circle: $name. Let's practice English!",
                isPriority = true,
                isAnnouncement = true
            )
        )

        addLocalNotification(
            title = "New Group Created",
            text = "Group \"$name\" has been established by $creatorName.",
            category = "ANNOUNCEMENT"
        )
    }

    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        db.groupDao().deleteGroupById(groupId)
    }

    suspend fun updateGroupMutesAndAssistants(groupId: String, mutedEmails: String, assistants: String) = withContext(Dispatchers.IO) {
        val current = db.groupDao().getGroupById(groupId).firstOrNull() ?: return@withContext
        val updated = current.copy(
            mutedStudentEmails = mutedEmails,
            assistantTeacherEmails = assistants
        )
        db.groupDao().updateGroup(updated)
    }

    suspend fun deleteMessage(messageId: Int) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessageById(messageId)
    }

    // --- Chat messages ---
    fun getMessagesForGroup(groupId: String): Flow<List<MessageEntity>> {
        return db.messageDao().getMessagesForGroup(groupId)
    }

    suspend fun sendMessage(
        groupId: String,
        senderEmail: String,
        senderName: String,
        senderRole: String,
        content: String,
        attachmentPath: String? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        isPriority: Boolean = false,
        isAnnouncement: Boolean = false,
        replyToId: Int? = null,
        replyToText: String? = null
    ) = withContext(Dispatchers.IO) {
        val msg = MessageEntity(
            groupId = groupId,
            senderEmail = senderEmail,
            senderName = senderName,
            senderRole = senderRole,
            content = content,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            isPriority = isPriority,
            isAnnouncement = isAnnouncement,
            replyToId = replyToId,
            replyToText = replyToText,
            timestamp = System.currentTimeMillis()
        )
        val id = db.messageDao().insertMessage(msg)

        // If it's an announcement, pin it to the group!
        if (isAnnouncement) {
            val group = db.groupDao().getGroupById(groupId).firstOrNull()
            if (group != null) {
                db.groupDao().updateGroup(group.copy(pinnedAnnouncementId = id.toInt()))
            }
            // Trigger announcement notification
            addLocalNotification(
                title = "New Class Announcement!",
                text = "$senderName posted: $content",
                category = "ANNOUNCEMENT"
            )
        } else if (isPriority) {
            addLocalNotification(
                title = "Urgent: Priority Message",
                text = "Teacher $senderName: $content",
                category = "CHAT"
            )
        } else {
            // Normal message notifications trigger occasionally or simulated for testing
            if (senderEmail != "jane.doe@student.com" && senderEmail != "mr.harris@englishconnect.edu") {
                addLocalNotification(
                    title = "New group message",
                    text = "$senderName: $content",
                    category = "CHAT"
                )
            }
        }

        // Set group last message timestamp
        val group = db.groupDao().getGroupById(groupId).firstOrNull()
        if (group != null) {
            db.groupDao().updateGroup(group.copy(lastMessageTime = System.currentTimeMillis()))
        }
    }

    suspend fun pinAnnouncementAtTop(groupId: String, messageId: Int?) = withContext(Dispatchers.IO) {
        val group = db.groupDao().getGroupById(groupId).firstOrNull() ?: return@withContext
        db.groupDao().updateGroup(group.copy(pinnedAnnouncementId = messageId))
    }

    suspend fun toggleReaction(messageId: Int, emoji: String, userEmail: String) = withContext(Dispatchers.IO) {
        val msg = db.messageDao().getMessageById(messageId) ?: return@withContext
        val reactionString = msg.reactions // Format "👍:mou@mail.com,❤️:stu@mail.com"
        val reactionPairs = if (reactionString.isEmpty()) {
            mutableListOf()
        } else {
            reactionString.split(",").map { it.split(":") }.filter { it.size == 2 }.map { it[0] to it[1] }.toMutableList()
        }

        val existingIndex = reactionPairs.indexOfFirst { it.first == emoji && it.second == userEmail }
        if (existingIndex >= 0) {
            // Remove reaction
            reactionPairs.removeAt(existingIndex)
        } else {
            // Add reaction
            reactionPairs.add(emoji to userEmail)
        }

        val updatedReactions = reactionPairs.joinToString(",") { "${it.first}:${it.second}" }
        db.messageDao().updateMessage(msg.copy(reactions = updatedReactions))
    }

    fun searchMessages(groupId: String, query: String): Flow<List<MessageEntity>> {
        return db.messageDao().searchMessages(groupId, query)
    }

    // --- Resource Library ---
    fun getResourcesForGroup(groupId: String): Flow<List<ResourceEntity>> {
        return db.resourceDao().getResourcesForGroup(groupId)
    }

    suspend fun uploadResource(
        groupId: String,
        title: String,
        fileType: String,
        fileSize: String,
        uploaderName: String,
        uploaderEmail: String,
        localUri: String = ""
    ) = withContext(Dispatchers.IO) {
        val res = ResourceEntity(
            groupId = groupId,
            title = title,
            fileType = fileType,
            fileSize = fileSize,
            uploaderName = uploaderName,
            uploaderEmail = uploaderEmail,
            localUri = localUri
        )
        db.resourceDao().insertResource(res)

        // Automatically share file notification
        addLocalNotification(
            title = "Weekly Study Resource Shared",
            text = "$uploaderName uploaded \"$title ($fileType)\" in class library.",
            category = "FILE"
        )

        // Automatically post a notification message into group chat!
        sendMessage(
            groupId = groupId,
            senderEmail = uploaderEmail,
            senderName = uploaderName,
            senderRole = if (uploaderEmail.contains("harris")) "TEACHER" else "STUDENT",
            content = "I shared a study file: \"$title ($fileType, $fileSize)\". Click the resources library tab at the top-right to view and download it!",
            attachmentName = title,
            attachmentType = fileType,
            attachmentPath = localUri
        )
    }

    suspend fun deleteResource(resource: ResourceEntity) = withContext(Dispatchers.IO) {
        db.resourceDao().deleteResource(resource)
    }

    // --- AI Chatbot Assistant ---
    suspend fun sendAIMessageUser(content: String) = withContext(Dispatchers.IO) {
        val userMsg = AIMessageEntity(sender = "USER", content = content)
        db.aiMessageDao().insertAIMessage(userMsg)
    }

    suspend fun sendAIMessageAssistant(content: String) = withContext(Dispatchers.IO) {
        val aiMsg = AIMessageEntity(sender = "AI", content = content)
        db.aiMessageDao().insertAIMessage(aiMsg)
    }

    suspend fun clearAIChat() = withContext(Dispatchers.IO) {
        db.aiMessageDao().clearChat()
    }

    /**
     * Calls Gemini API to get friendly English corrections and educational suggestions.
     * Keeps instructions tight to English teaching only.
     */
    suspend fun requestAIEnglishCorrection(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = RetrofitClient.getApiKeySafe()

        // Strict system guidance for the English tutor model
        val systemInstructionText = """
            You are a kind, professional, friendly and encouraging English Teacher.
            Your role is to assist the student with: English grammar correction, IELTS/TOEFL vocabulary boost, explanation of rules, homework suggestions, active vs passive voice structures, essay refinement, or translation to English.
            RULES:
            1. You MUST ALWAYS communicate in English.
            2. Maintain a supportive, reassuring, teacher-like tone.
            3. Highlight the corrections clearly (using bullet points or bold text) so they are easy to scan.
            4. Make responses concise and beautiful. Keep explanations clean and professional.
        """.trimIndent()

        if (apiKey.isEmpty()) {
            // Simulates high-quality, friendly AI responses offline
            Log.d("AppRepository", "No API key found. Simulating offline English correction.")
            return@withContext getMockAICorrection(prompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize. I couldn't formulate a lesson plan for this query. Could you rephrase your question?"
        } catch (e: Exception) {
            Log.e("AppRepository", "Gemini API error: ${e.message}", e)
            val fallback = getMockAICorrection(prompt)
            "$fallback\n\n*(Note: Powered by offline assistant support. Provide actual API key for full generative power)*"
        }
    }

    private fun getMockAICorrection(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") -> {
                "Hello there! I'm your English Teach AI Companion. How can I help you refine your grammar, expand your vocabulary, or practice sentences today?"
            }
            lower.contains("grammar") || lower.contains("correct") || lower.contains("incorrect") -> {
                "**Teacher's Grammar Corner:**\n\n- **In your sentence:** \"She don't like English.\"\n- **Correction:** \"She **doesn't** like English.\"\n- **Why?** \"She\" is a third-person singular pronoun. We must use \"does not\" (doesn't) instead of \"do not\" (don't).\n\nKeep up the great effort! Ask me for more drills if you wish!"
            }
            lower.contains("translate") || lower.contains("how to say") -> {
                "**Teacher's Translation Corner:**\n\nTo translate feelings of gratitude into formal academic English, try using:\n- *\"I am deeply appreciative of your mentorship.\"*\n- *\"I highly values your precious guidance.\"*\n\nWould you like me to translate more colloquial phrases into English?"
            }
            lower.contains("essay") || lower.contains("writing") || lower.contains("ielts") -> {
                "**Teacher's IELTS Writing Tips:**\n\nTo improve your task achievement score, structure your argumentative essays with:\n1. **Introduction:** Restate the prompt and clearly present your stance.\n2. **Body Paragraph 1 & 2:** Use cohesive devices (e.g., *Furthermore, Conversely, In addition*).\n3. **Conclusion:** Summarize key arguments without introducing new ideas.\n\nType in your practice essay paragraph below, and I will correct it sentence by sentence!"
            }
            else -> {
                "That is a great English inquiry! Let's examine it:\n\n1. Ensure your grammatical subject agrees with the verb in number.\n2. Use descriptive adjectives to elevate your vocabulary (e.g., instead of \"very good\", try \"splendid\" or \"exemplary\").\n\nWould you like me to draft a spelling exercise or write examples for you?"
            }
        }
    }

    // --- Local push notification simulator ---
    suspend fun addLocalNotification(title: String, text: String, category: String) = withContext(Dispatchers.IO) {
        val log = NotificationLogEntity(
            title = title,
            text = text,
            category = category,
            timestamp = System.currentTimeMillis()
        )
        db.notificationLogDao().insertNotification(log)
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        db.notificationLogDao().markAllRead()
    }
}
