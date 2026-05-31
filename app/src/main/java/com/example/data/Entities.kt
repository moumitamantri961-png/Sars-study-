package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val profilePhotoUrl: String,
    val role: String, // "TEACHER" or "STUDENT"
    val isPrimaryAdmin: Boolean = false,
    val isLoggedIn: Boolean = false,
    val dateJoined: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val creatorEmail: String,
    val creatorName: String,
    val memberCount: Int = 1,
    val mutedStudentEmails: String = "", // Comma-separated list of emails that are currently muted
    val assistantTeacherEmails: String = "", // Comma-separated list of promoted assistant teachers
    val pinnedAnnouncementId: Int? = null, // ID of the currently pinned message/announcement
    val lastMessageTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: String,
    val senderEmail: String,
    val senderName: String,
    val senderRole: String, // "TEACHER" or "STUDENT"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentPath: String? = null, // Local simulated path or Coil drawable resource
    val attachmentName: String? = null, // E.g., "grammar_guide.pdf"
    val attachmentType: String? = null, // "IMAGE", "PDF", "AUDIO", "DOC"
    val isPriority: Boolean = false, // Teacher priority message flag
    val isAnnouncement: Boolean = false, // Show in pinned announcements
    val isRead: Boolean = false, // Simulated read receipts
    val reactions: String = "", // E.g., "👍:mou@mail.com,❤️:stu@mail.com"
    val replyToId: Int? = null, // Reply to message ID
    val replyToText: String? = null // Quick reply fallback text
)

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: String,
    val title: String,
    val fileType: String, // "PDF", "DOCX", "WORKsheet", "AUDIO"
    val fileSize: String, // E.g., "1.4 MB"
    val uploaderName: String,
    val uploaderEmail: String,
    val uploadTime: Long = System.currentTimeMillis(),
    val localUri: String = "",
    val isPinned: Boolean = false
)

@Entity(tableName = "ai_messages")
data class AIMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "push_notifications")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val text: String,
    val category: String, // "CHAT", "ANNOUNCEMENT", "FILE", "AI_REMINDER"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
