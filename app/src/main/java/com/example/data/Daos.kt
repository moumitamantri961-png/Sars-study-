package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("SELECT * FROM users")
    fun getAllRegisteredUsers(): Flow<List<UserEntity>>
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM study_groups ORDER BY lastMessageTime DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM study_groups WHERE id = :id LIMIT 1")
    fun getGroupById(id: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroup(groupId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Int): MessageEntity?

    @Query("SELECT * FROM messages WHERE groupId = :groupId AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(groupId: String, query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Update
    suspend fun updateMessage(message: MessageEntity)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources WHERE groupId = :groupId ORDER BY uploadTime DESC")
    fun getResourcesForGroup(groupId: String): Flow<List<ResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)
}

@Dao
interface AIMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllAIMessages(): Flow<List<AIMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAIMessage(message: AIMessageEntity)

    @Query("DELETE FROM ai_messages")
    suspend fun clearChat()
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM push_notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationLogEntity)

    @Query("SELECT COUNT(*) FROM push_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE push_notifications SET isRead = 1")
    suspend fun markAllRead()
}
