package com.mitra.ai.xyz.data.repository

import com.google.gson.Gson
import com.mitra.ai.xyz.data.local.ChatDao
import com.mitra.ai.xyz.data.local.MessageDao
import com.mitra.ai.xyz.data.remote.OpenAIService
import com.mitra.ai.xyz.data.remote.OpenAIServiceFactory
import com.mitra.ai.xyz.data.remote.EventSource
import com.mitra.ai.xyz.data.remote.EventSourceListener
import com.mitra.ai.xyz.data.remote.SSEInterceptor
import com.mitra.ai.xyz.data.remote.SSEListener
import com.mitra.ai.xyz.domain.model.Chat
import com.mitra.ai.xyz.domain.model.Message
import com.mitra.ai.xyz.domain.repository.ChatRepository
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
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

        val service = openAIServiceFactory.createService(config.baseUrl.takeIf { it.isNotBlank() })
        val response = service.createStreamingChatCompletion(apiKey, request)
        val responseBody = response.body()
        
        if (!response.isSuccessful) {
            val errorMessage = when (response.code()) {
                401 -> "Invalid API key. Please check your API key in settings."
                429 -> "Rate limit exceeded. Please try again later."
                500 -> "OpenAI server error. Please try again later."
                else -> "API request failed: ${response.code()} ${response.message()}"
            }
            throw Exception(errorMessage)
        }
        
        if (responseBody == null) {
            throw Exception("No response from server")
        }

        val reader = responseBody.charStream().buffered()
        var line: String?
        
        while (reader.readLine().also { line = it } != null) {
            if (line!!.startsWith("data: ")) {
                val data = line!!.substring(6)
                if (data == "[DONE]") {
                    val finalMessage = aiMessage.copy(
                        content = contentBuilder.toString(),
                        isStreaming = false,
                        isComplete = true
                    )
                    messageDao.insertMessage(finalMessage)
                    emit(finalMessage)
                    break
                }
                
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
                            if (isFirstChunk) {
                                isFirstChunk = false
                            }

                            emit(updatedMessage)
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed JSON
                }
            }
        }
    }
    .flowOn(Dispatchers.IO)
    .catch { e ->
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