package com.mitra.ai.xyz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import com.mitra.ai.xyz.domain.model.Chat
import com.mitra.ai.xyz.domain.model.Message

@Database(
    entities = [
        Message::class,
        Chat::class,
        AiProviderProfile::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun providerProfileDao(): ProviderProfileDao

    companion object {
        const val DATABASE_NAME = "mitra_ai_db"
    }
}