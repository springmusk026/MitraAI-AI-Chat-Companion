package com.mitra.ai.xyz.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mitra.ai.xyz.data.local.ChatDao
import com.mitra.ai.xyz.data.local.MessageDao
import com.mitra.ai.xyz.data.remote.OpenAIService
import com.mitra.ai.xyz.data.remote.OpenAIServiceFactory
import com.mitra.ai.xyz.data.remote.EventSource
import com.mitra.ai.xyz.domain.model.Chat
import com.mitra.ai.xyz.domain.model.Message
import com.mitra.ai.xyz.domain.repository.ChatRepository
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val openAIService: OpenAIService,
    private val settingsRepository: SettingsRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val gson: Gson,
    @Named("sseClient") private val sseClient: OkHttpClient,
    private val openAIServiceFactory: OpenAIServiceFactory
) : ChatRepository {

    override fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()

    override suspend fun getChatById(chatId: String): Chat? = withContext(Dispatchers.IO) {
        chatDao.getChatById(chatId)
    }

    override suspend fun insertChat(chat: Chat) = withContext(Dispatchers.IO) {
        chatDao.insertChat(chat)
    }

    override suspend fun deleteChat(chat: Chat) = withContext(Dispatchers.IO) {
        chatDao.deleteChat(chat)
    }

    override suspend fun updateChatTitle(chatId: String, title: String) = withContext(Dispatchers.IO) {
        chatDao.updateChatTitle(chatId, title)
    }

    override suspend fun updateChatPin(chatId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        chatDao.updateChatPin(chatId, isPinned)
    }

    override suspend fun updateChatTimestamp(chatId: String, timestamp: Long) = withContext(Dispatchers.IO) {
        chatDao.updateChatTimestamp(chatId, timestamp)
    }

    override suspend fun getMessages(chatId: String, limit: Int, offset: Int): List<Message> = 
        withContext(Dispatchers.IO) {
            messageDao.getMessages(chatId, limit, offset)
        }

    override suspend fun getMessageCount(chatId: String): Int = withContext(Dispatchers.IO) {
        messageDao.getMessageCount(chatId)
    }

    override suspend fun insertMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    override suspend fun deleteMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(message)
    }

    override fun sendMessage(content: String, chatId: String): Flow<Message> = flow {
        val config = settingsRepository.getProviderConfig()
        val apiKey = settingsRepository.getApiKey()

        Log.d("ChatRepository", "API Key: ${apiKey.take(4)}...") // Only log first 4 chars for security

        // Create initial message
        val aiMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            content = "",
            isUser = false,
            isStreaming = true,
            isComplete = false
        )
        
        val contentBuilder = StringBuilder()
        var isFirstChunk = true

        try {
            if (apiKey.isBlank()) {
                throw Exception("API key is not set. Please configure your API key in settings.")
            }

            val request = OpenAIService.ChatRequest(
                messages = listOf(
                    OpenAIService.ChatMessage(
                        role = "user",
                        content = content
                    )
                ),
                model = config.model.takeIf { it.isNotBlank() } ?: "gpt-3.5-turbo",
                stream = true
            )

            val baseUrl = config.baseUrl.takeIf { it.isNotBlank() } ?: "https://api.openai.com/v1"
            val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(requestBody)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .build()

            EventSource.asFlow(sseClient, httpRequest)
                .collect { data ->
                    try {
                        val streamResponse = gson.fromJson(data, OpenAIService.StreamResponse::class.java)
                        if (streamResponse.choices.isNotEmpty()) {
                            val choice = streamResponse.choices[0]
                            val content = choice.delta.content ?: ""
                            
                            if (content.isNotBlank()) {
                                contentBuilder.append(content)
                                
                                val updatedMessage = aiMessage.copy(
                                    content = contentBuilder.toString(),
                                    isStreaming = true,
                                    isComplete = false
                                )

                                messageDao.insertMessage(updatedMessage)
                                emit(updatedMessage)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed JSON
                    }
                }

            // Emit final message
            val finalMessage = aiMessage.copy(
                content = contentBuilder.toString(),
                isStreaming = false,
                isComplete = true
            )
            messageDao.insertMessage(finalMessage)
            emit(finalMessage)
        } catch (e: Exception) {
            val errorMessage = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                content = "Error: ${e.message}",
                isUser = false,
                isError = true,
                isStreaming = false,
                isComplete = true
            )
            messageDao.insertMessage(errorMessage)
            emit(errorMessage)
            throw e
        }
    }
    .flowOn(Dispatchers.IO)

    override suspend fun clearChats() = withContext(Dispatchers.IO) {
        chatDao.deleteAllChats()
        messageDao.deleteAllMessages()
    }

    override suspend fun saveChat(chat: Chat, messages: List<Message>?): Unit = withContext(Dispatchers.IO) {
        chatDao.insertChat(chat)
        messages?.forEach { message ->
            messageDao.insertMessage(message)
        }
    }
} 