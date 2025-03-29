package com.mitra.ai.xyz.domain.model

import androidx.room.*
import java.util.*

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("chatId")
    ]
)
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false,
    val isComplete: Boolean = true
) 