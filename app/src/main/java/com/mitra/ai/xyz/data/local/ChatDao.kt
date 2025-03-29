package com.mitra.ai.xyz.data.local

import androidx.room.*
import com.mitra.ai.xyz.domain.model.Chat
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Delete
    suspend fun deleteChat(chat: Chat)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: String, title: String)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updateChatPin(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatTimestamp(chatId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
} 