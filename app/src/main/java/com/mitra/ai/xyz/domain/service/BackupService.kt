package com.mitra.ai.xyz.domain.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.mitra.ai.xyz.domain.model.*
import com.mitra.ai.xyz.domain.repository.ChatRepository
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("HardwareIds")
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val json: Json
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createBackup(
        components: Set<BackupComponent>,
        outputUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Collect data based on selected components
            val content = BackupContent(
                settings = if (components.contains(BackupComponent.SETTINGS)) {
                    settingsRepository.getAppSettings().first()
                } else null,
                providers = if (components.contains(BackupComponent.PROVIDERS)) {
                    settingsRepository.getProviderProfiles().first()
                } else null,
                chats = if (components.contains(BackupComponent.CHATS)) {
                    val chats = chatRepository.getAllChats().first()
                    chats.map { chat ->
                        val messages = chatRepository.getMessages(chat.id, Int.MAX_VALUE, 0)
                        chat.toBackupChat(messages)
                    }
                } else null
            )

            val metadata = context.packageManager.getPackageInfo(context.packageName, 0).versionName?.let {
                BackupMetadata(
                    appVersion = it,
                    deviceId = deviceId
                )
            }

            // Serialize data without checksum first
            val tempBackup = metadata?.let { BackupData(it, content, "") }
            val serialized = json.encodeToString(tempBackup)
            
            // Calculate checksum
            val checksum = calculateChecksum(serialized)
            
            // Create final backup with checksum
            val backup = tempBackup?.copy(checksum = checksum)
            val finalSerialized = json.encodeToString(backup)

            // Write to file
            context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                stream.write(finalSerialized.toByteArray())
            } ?: throw IllegalStateException("Could not open output stream")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        inputUri: Uri,
        conflictResolution: ConflictResolution
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read backup file
            val backupStr = context.contentResolver.openInputStream(inputUri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IllegalStateException("Could not open input stream")

            // Parse backup
            val backup = json.decodeFromString<BackupData>(backupStr)

            // Verify checksum
            val tempBackup = backup.copy(checksum = "")
            val calculatedChecksum = calculateChecksum(json.encodeToString(tempBackup))
            if (calculatedChecksum != backup.checksum) {
                return@withContext Result.failure(IllegalStateException("Invalid backup file: checksum mismatch"))
            }

            // Verify backup version
            if (backup.metadata.backupVersion > BackupMetadata.BACKUP_VERSION) {
                return@withContext Result.failure(IllegalStateException("Unsupported backup version"))
            }

            // Restore data based on conflict resolution
            backup.data.settings?.let { settings ->
                when (conflictResolution) {
                    ConflictResolution.REPLACE_WITH_BACKUP -> settingsRepository.updateAppSettings(settings)
                    ConflictResolution.MERGE -> {
                        val existing = settingsRepository.getAppSettings().first()
                        settingsRepository.updateAppSettings(mergeSettings(existing, settings))
                    }
                    ConflictResolution.KEEP_EXISTING -> { /* Do nothing */ }
                }
            }

            backup.data.providers?.let { providers ->
                when (conflictResolution) {
                    ConflictResolution.REPLACE_WITH_BACKUP -> {
                        settingsRepository.clearProviderProfiles()
                        providers.forEach { settingsRepository.addProviderProfile(it) }
                    }
                    ConflictResolution.MERGE -> {
                        val existing = settingsRepository.getProviderProfiles().first()
                        val merged = mergeProviders(existing, providers)
                        settingsRepository.clearProviderProfiles()
                        merged.forEach { settingsRepository.addProviderProfile(it) }
                    }
                    ConflictResolution.KEEP_EXISTING -> { /* Do nothing */ }
                }
            }

            backup.data.chats?.let { backupChats ->
                when (conflictResolution) {
                    ConflictResolution.REPLACE_WITH_BACKUP -> {
                        chatRepository.clearChats()
                        backupChats.forEach { backupChat ->
                            val chat = backupChat.toChat()
                            chatRepository.saveChat(chat, backupChat.messages.map { it.toMessage() })
                        }
                    }
                    ConflictResolution.MERGE -> {
                        val existingChats = chatRepository.getAllChats().first()
                        val existingMessages = existingChats.associateWith { chat ->
                            chatRepository.getMessages(chat.id, Int.MAX_VALUE, 0)
                        }
                        
                        val mergedChats = mergeChats(
                            existing = existingChats.map { it.toBackupChat(existingMessages[it] ?: emptyList()) },
                            backup = backupChats
                        )

                        mergedChats.forEach { backupChat ->
                            val chat = backupChat.toChat()
                            chatRepository.saveChat(chat, backupChat.messages.map { it.toMessage() })
                        }
                    }
                    ConflictResolution.KEEP_EXISTING -> { /* Do nothing */ }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateChecksum(data: String): String {
        val bytes = data.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun mergeSettings(existing: AppSettings, backup: AppSettings): AppSettings {
        // Prefer existing settings but take backup values if they exist and existing are default
        return existing.copy(
            themeMode = if (existing.themeMode == AppSettings.ThemeMode.SYSTEM) backup.themeMode else existing.themeMode,
            fontScale = if (existing.fontScale == 1.0f) backup.fontScale else existing.fontScale
        )
    }

    private fun mergeProviders(existing: List<AiProviderProfile>, backup: List<AiProviderProfile>): List<AiProviderProfile> {
        // Merge by name, prefer existing but add new ones from backup
        val merged = existing.toMutableList()
        backup.forEach { backupProfile ->
            if (!merged.any { it.name == backupProfile.name }) {
                merged.add(backupProfile)
            }
        }
        return merged
    }

    private fun mergeChats(existing: List<BackupChat>, backup: List<BackupChat>): List<BackupChat> {
        // Merge by ID, keeping newer versions of chats and their messages
        val mergedMap = (existing + backup)
            .groupBy { it.id }
            .mapValues { (_, chats) -> 
                // Take the chat with the latest update time
                val newestChat = chats.maxByOrNull { it.updatedAt }!!
                
                // Merge messages from all versions of this chat
                val allMessages = chats.flatMap { it.messages }
                    .groupBy { it.id }
                    .mapValues { (_, messages) -> messages.maxByOrNull { it.timestamp }!! }
                    .values
                    .toList()
                    .sortedBy { it.timestamp }

                newestChat.copy(messages = allMessages)
            }
        return mergedMap.values.toList()
    }
} 