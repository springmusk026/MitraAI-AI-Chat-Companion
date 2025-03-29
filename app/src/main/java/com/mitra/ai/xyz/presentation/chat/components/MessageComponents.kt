package com.mitra.ai.xyz.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mitra.ai.xyz.domain.model.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: Message,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    var isMessageFocused by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    val maxCollapsedLines = 4
    val shouldCollapse = remember(message.content) {
        message.content.count { it == '\n' } > maxCollapsedLines - 1 ||
        message.content.length > 200
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        if (!message.isUser && !message.isStreaming) {
                            isMessageFocused = !isMessageFocused
                        }
                    }
                )
            },
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = when {
                message.isError -> MaterialTheme.colorScheme.errorContainer
                message.isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            tonalElevation = if (message.isUser) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        message.isError -> MaterialTheme.colorScheme.onErrorContainer
                        message.isUser -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = if (!isExpanded && shouldCollapse) maxCollapsedLines else Int.MAX_VALUE,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (shouldCollapse) 4.dp else 8.dp
                    )
                )

                if (shouldCollapse) {
                    ExpandCollapseButton(
                        isExpanded = isExpanded,
                        onToggle = { isExpanded = !isExpanded },
                        isUserMessage = message.isUser
                    )
                }
            }
        }

        MessageActions(
            message = message,
            isMessageFocused = isMessageFocused,
            onRetry = onRetry,
            onCopy = onCopy,
            onShare = onShare,
            onActionPerformed = { isMessageFocused = false }
        )
    }
}

@Composable
private fun ExpandCollapseButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isUserMessage: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isUserMessage) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isExpanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = if (isExpanded) 
                    Icons.Filled.KeyboardArrowUp 
                else 
                    Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun MessageActions(
    message: Message,
    isMessageFocused: Boolean,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onActionPerformed: () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnimatedVisibility(
            visible = !message.isUser && isMessageFocused && !message.isStreaming,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                MessageActionButton(
                    icon = Icons.Outlined.ContentCopy,
                    description = "Copy message",
                    onClick = { 
                        onCopy(message.content)
                        onActionPerformed()
                    }
                )

                MessageActionButton(
                    icon = Icons.Outlined.Share,
                    description = "Share message",
                    onClick = { 
                        onShare(message.content)
                        onActionPerformed()
                    }
                )

                if (message.isError) {
                    MessageActionButton(
                        icon = Icons.Outlined.Refresh,
                        description = "Retry message",
                        onClick = { 
                            onRetry()
                            onActionPerformed()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    LoadingDot(delay = it * 200L)
                }
            }
        }
    }
}

@Composable
private fun LoadingDot(delay: Long) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay)
        while (true) {
            visible = !visible
            kotlinx.coroutines.delay(600)
        }
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (visible) 0.8f else 0.2f
                )
            )
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 