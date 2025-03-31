package com.mitra.ai.xyz.domain.repository

import com.mitra.ai.xyz.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun resetSettings()
} 