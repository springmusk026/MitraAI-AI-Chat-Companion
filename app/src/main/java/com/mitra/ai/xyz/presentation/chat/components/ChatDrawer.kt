package com.mitra.ai.xyz.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mitra.ai.xyz.domain.model.Chat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    chats: List<Chat>,
    selectedChatId: String?,
    onChatSelected: (Chat) -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onPinChat: (Chat, Boolean) -> Unit,
    onRenameChat: (Chat, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chatToRename by remember { mutableStateOf<Chat?>(null) }
    var newTitleText by remember { mutableStateOf("") }

    val pinnedChats = remember(chats) { chats.filter { it.isPinned }.sortedByDescending { it.updatedAt } }
    val recentChats = remember(chats) { chats.filterNot { it.isPinned }.sortedByDescending { it.updatedAt } }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            TopAppBar(
                title = { 
                    Text(
                        text = "Your Chats",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = onCreateNewChat,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new chat",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (pinnedChats.isNotEmpty()) {
                    item(key = "pinned_header") {
                        ChatSectionHeader(
                            title = "Pinned",
                            icon = Icons.Filled.PushPin
                        )
                    }
                    items(
                        items = pinnedChats,
                        key = { "pinned_${it.id}" }
                    ) { chat ->
                        ChatItem(
                            chat = chat,
                            isSelected = chat.id == selectedChatId,
                            onClick = { onChatSelected(chat) },
                            onDelete = { onDeleteChat(chat) },
                            onPin = { isPinned -> onPinChat(chat, isPinned) },
                            onRename = { chatToRename = chat; newTitleText = chat.title }
                        )
                    }
                }

                if (recentChats.isNotEmpty()) {
                    item(key = "recent_header") {
                        ChatSectionHeader(
                            title = "Recent",
                            icon = Icons.Outlined.History,
                            modifier = Modifier.padding(top = if (pinnedChats.isNotEmpty()) 16.dp else 0.dp)
                        )
                    }
                    items(
                        items = recentChats,
                        key = { "recent_${it.id}" }
                    ) { chat ->
                        ChatItem(
                            chat = chat,
                            isSelected = chat.id == selectedChatId,
                            onClick = { onChatSelected(chat) },
                            onDelete = { onDeleteChat(chat) },
                            onPin = { isPinned -> onPinChat(chat, isPinned) },
                            onRename = { chatToRename = chat; newTitleText = chat.title }
                        )
                    }
                }

                if (chats.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyState()
                    }
                }
            }
        }

        // Remove the bottom New Chat button since we moved it to the top
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Rename Dialog
    if (chatToRename != null) {
        AlertDialog(
            onDismissRequest = { chatToRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Chat name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitleText.isNotBlank()) {
                            onRenameChat(chatToRename!!, newTitleText)
                        }
                        chatToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChatSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "Start a new chat to begin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatItem(
    chat: Chat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: (Boolean) -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = chat.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                )
            },
            supportingContent = {
                Text(
                    text = formatDate(chat.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = null,
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (chat.isPinned) 
                                        Icons.Filled.PushPin 
                                    else 
                                        Icons.Outlined.PushPin,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onPin(!chat.isPinned)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onRename()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago" // Add days
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
} 