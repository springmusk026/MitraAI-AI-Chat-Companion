package com.mitra.ai.xyz.domain.repository

import com.mitra.ai.xyz.domain.model.Chat
import com.mitra.ai.xyz.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllChats(): Flow<List<Chat>>
    
    suspend fun getChatById(chatId: String): Chat?
    
    suspend fun insertChat(chat: Chat)
    
    suspend fun deleteChat(chat: Chat)
    
    suspend fun updateChatTitle(chatId: String, title: String)
    
    suspend fun updateChatPin(chatId: String, isPinned: Boolean)
    
    suspend fun updateChatTimestamp(chatId: String, timestamp: Long = System.currentTimeMillis())
    
    suspend fun getMessages(chatId: String, limit: Int, offset: Int): List<Message>
    
    suspend fun getMessageCount(chatId: String): Int
    
    suspend fun insertMessage(message: Message)
    
    suspend fun deleteMessage(message: Message)
    
    fun sendMessage(content: String, chatId: String): Flow<Message>
} 