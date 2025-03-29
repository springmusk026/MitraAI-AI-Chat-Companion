package com.mitra.ai.xyz.presentation.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitra.ai.xyz.domain.model.Message
import com.mitra.ai.xyz.presentation.chat.components.ChatInput
import com.mitra.ai.xyz.presentation.chat.components.MessageBubble
import com.mitra.ai.xyz.presentation.chat.components.TypingIndicator
import com.mitra.ai.xyz.presentation.chat.components.ChatDrawer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var messageText by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current

    // Auto-scroll to bottom when new message is added
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty() && !chatState.isLoadingMore) {
            listState.scrollToItem(0)
        }
    }

    // Hide keyboard when streaming starts
    LaunchedEffect(chatState.isStreaming) {
        if (chatState.isStreaming) {
            keyboardController?.hide()
        }
    }

    // Check if we need to load more messages
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex > chatState.messages.size - 5 && 
            !chatState.isLoadingMore && 
            chatState.canLoadMore) {
            viewModel.loadMoreMessages()
        }
    }

    // Handle message actions
    val handleCopy: (String) -> Unit = { content ->
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", content)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val handleShare: (String) -> Unit = { content ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "Share message"))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !chatState.isStreaming && !chatState.isLoading,
        drawerContent = {
            ModalDrawerSheet {
                ChatDrawer(
                    chats = chatState.chats,
                    selectedChatId = chatState.selectedChatId,
                    onChatSelected = { chat ->
                        viewModel.selectChat(chat)
                        scope.launch { drawerState.close() }
                    },
                    onCreateNewChat = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteChat = { chat ->
                        viewModel.deleteChat(chat)
                    },
                    onPinChat = { chat, isPinned ->
                        viewModel.pinChat(chat, isPinned)
                    },
                    onRenameChat = { chat, newTitle ->
                        viewModel.renameChat(chat, newTitle)
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            enabled = !chatState.isStreaming && !chatState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Open drawer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (!drawerState.isOpen) {
                                Icon(
                                    imageVector = Icons.Outlined.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = chatState.chats.find { it.id == chatState.selectedChatId }?.title 
                                    ?: "AI Assistant",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    actions = {
                        if (!drawerState.isOpen) {
                            IconButton(
                                onClick = {
                                    viewModel.createNewChat()
                                    scope.launch { drawerState.close() }
                                },
                                enabled = !chatState.isStreaming && !chatState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    ChatInput(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendMessage = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                                keyboardController?.hide()
                            }
                        },
                        isStreaming = chatState.isStreaming,
                        isLoading = chatState.isLoading,
                        error = chatState.error,
                        focusRequester = focusRequester
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                reverseLayout = true // This ensures newest messages are at the bottom
            ) {
                if (chatState.isLoading || chatState.isStreaming) {
                    item(key = "typing_indicator") {
                        TypingIndicator()
                    }
                }

                if (chatState.messages.isEmpty() && !chatState.isLoading && !chatState.isStreaming) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Welcome to MitraAI",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your AI assistant is ready to help",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Try asking:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                listOf(
                                    "Explain quantum computing in simple terms",
                                    "Write a creative story about a time traveler",
                                    "Help me debug a coding problem",
                                    "Suggest healthy dinner recipes"
                                ).forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = { 
                                            messageText = suggestion
                                            focusRequester.requestFocus()
                                        },
                                        label = { Text(suggestion) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = chatState.messages,
                    key = { message -> "${message.id}_${message.timestamp}" }
                ) { message ->
                    MessageBubble(
                        message = message,
                        onRetry = { 
                            if (message.isError) {
                                viewModel.sendMessage(message.content)
                            }
                        },
                        onCopy = handleCopy,
                        onShare = handleShare
                    )
                }

                if (chatState.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

