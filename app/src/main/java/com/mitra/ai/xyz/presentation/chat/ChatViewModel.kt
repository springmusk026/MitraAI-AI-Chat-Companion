package com.mitra.ai.xyz.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.ai.xyz.domain.model.Chat
import com.mitra.ai.xyz.domain.model.Message
import com.mitra.ai.xyz.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

data class ChatState(
    val chats: List<Chat> = emptyList(),
    val selectedChatId: String? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isStreaming: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllChats().collect { chats ->
                _chatState.update { state ->
                    state.copy(
                        chats = chats,
                        selectedChatId = if (state.selectedChatId == null && chats.isNotEmpty()) {
                            loadMessagesForChat(chats.first().id)
                            chats.first().id
                        } else state.selectedChatId
                    )
                }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newChat = Chat(
                title = "New Chat ${_chatState.value.chats.size + 1}"
            )
            repository.insertChat(newChat)
            selectChat(newChat)
        }
    }

    fun selectChat(chat: Chat) {
        viewModelScope.launch {
            _chatState.update { it.copy(selectedChatId = chat.id, messages = emptyList()) }
            loadMessagesForChat(chat.id)
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            repository.deleteChat(chat)
            if (chat.id == _chatState.value.selectedChatId) {
                _chatState.update { it.copy(selectedChatId = null, messages = emptyList()) }
            }
        }
    }

    fun pinChat(chat: Chat, isPinned: Boolean) {
        viewModelScope.launch {
            repository.updateChatPin(chat.id, isPinned)
        }
    }

    fun renameChat(chat: Chat, newTitle: String) {
        viewModelScope.launch {
            repository.updateChatTitle(chat.id, newTitle)
        }
    }

    private suspend fun loadMessagesForChat(chatId: String) {
        _chatState.update { it.copy(isLoading = true, error = null) }
        try {
            val messages = repository.getMessages(chatId, MESSAGES_PER_PAGE, 0)
            val messageCount = repository.getMessageCount(chatId)
            _chatState.update { state ->
                state.copy(
                    messages = messages,
                    isLoading = false,
                    canLoadMore = messages.size < messageCount
                )
            }
        } catch (e: Exception) {
            _chatState.update { it.copy(
                isLoading = false,
                error = "Failed to load messages: ${e.message}"
            )}
        }
    }

    fun loadMoreMessages() {
        val currentState = _chatState.value
        if (currentState.isLoadingMore || !currentState.canLoadMore || currentState.selectedChatId == null) return

        viewModelScope.launch {
            _chatState.update { it.copy(isLoadingMore = true) }
            try {
                val moreMessages = repository.getMessages(
                    currentState.selectedChatId,
                    MESSAGES_PER_PAGE,
                    currentState.messages.size
                )
                val totalCount = repository.getMessageCount(currentState.selectedChatId)
                
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages + moreMessages,
                        isLoadingMore = false,
                        canLoadMore = state.messages.size + moreMessages.size < totalCount
                    )
                }
            } catch (e: Exception) {
                _chatState.update { it.copy(
                    isLoadingMore = false,
                    error = "Failed to load more messages: ${e.message}"
                )}
            }
        }
    }

    fun sendMessage(content: String) {
        val currentState = _chatState.value
        if (content.isBlank() || currentState.isLoading || currentState.isStreaming) return

        val selectedChatId = currentState.selectedChatId ?: run {
            viewModelScope.launch {
                val newChat = Chat(title = "New Chat")
                repository.insertChat(newChat)
                _chatState.update { it.copy(selectedChatId = newChat.id) }
                sendMessage(content)
            }
            return
        }

        viewModelScope.launch {
            val userMessage = Message(
                chatId = selectedChatId,
                content = content,
                isUser = true,
                isComplete = true
            )
            
            repository.insertMessage(userMessage)
            repository.updateChatTimestamp(selectedChatId)

            // Add user message to state immediately
            _chatState.update { state ->
                state.copy(
                    messages = listOf(userMessage) + state.messages
                )
            }

            // Update chat title if this is the first message
            if (repository.getMessageCount(selectedChatId) <= 1) {
                val title = if (content.length > 40) {
                    content.take(40).trim() + "..."
                } else {
                    content.trim()
                }
                repository.updateChatTitle(selectedChatId, title)
            }

            // Start streaming AI response
            _chatState.update { it.copy(isStreaming = true, error = null) }
            
            try {
                repository.sendMessage(content, selectedChatId)
                    .collect { message ->
                        repository.insertMessage(message)
                        repository.updateChatTimestamp(selectedChatId)
                        _chatState.update { state ->
                            state.copy(
                                messages = listOf(message) + state.messages.filter { it.id != message.id },
                                isStreaming = !message.isComplete,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                val errorMessage = Message(
                    chatId = selectedChatId,
                    content = content,
                    isUser = true,
                    isError = true,
                    isComplete = true
                )
                repository.insertMessage(errorMessage)
                _chatState.update { it.copy(
                    messages = listOf(errorMessage) + it.messages.filter { msg -> msg.id != errorMessage.id },
                    isStreaming = false,
                    error = e.message
                )}
            }
        }
    }

    companion object {
        private const val MESSAGES_PER_PAGE = 20
    }
} 