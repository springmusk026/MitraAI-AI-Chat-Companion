package com.mitra.ai.xyz.domain.model

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val data: BackupContent,
    val checksum: String // SHA-256 hash of the serialized data
)

@Serializable
data class BackupMetadata @RequiresApi(Build.VERSION_CODES.O) constructor(
    val appVersion: String,
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String,
    val backupVersion: Int = BACKUP_VERSION
) {
    companion object {
        const val BACKUP_VERSION = 1
    }
}

@Serializable
data class BackupContent(
    val settings: AppSettings? = null,
    val providers: List<AiProviderProfile>? = null,
    val chats: List<BackupChat>? = null
)

@Serializable
data class BackupChat(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val messages: List<BackupMessage>
)

@Serializable
data class BackupMessage(
    val id: String,
    val chatId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isError: Boolean,
    val isStreaming: Boolean,
    val isComplete: Boolean
) {
    fun toMessage() = Message(
        id = id,
        chatId = chatId,
        content = content,
        isUser = isUser,
        timestamp = timestamp,
        isError = isError,
        isStreaming = isStreaming,
        isComplete = isComplete
    )
}

@Serializable
enum class BackupComponent {
    SETTINGS,
    PROVIDERS,
    CHATS
}

@Serializable
enum class ConflictResolution {
    KEEP_EXISTING,
    REPLACE_WITH_BACKUP,
    MERGE
}

// Extension functions to convert between domain and backup models
fun Chat.toBackupChat(messages: List<Message>) = BackupChat(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    messages = messages.map { it.toBackupMessage() }
)

fun BackupChat.toChat() = Chat(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned
)

fun Message.toBackupMessage() = BackupMessage(
    id = id,
    chatId = chatId,
    content = content,
    isUser = isUser,
    timestamp = timestamp,
    isError = isError,
    isStreaming = isStreaming,
    isComplete = isComplete
) 