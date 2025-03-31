package com.mitra.ai.xyz.data.repository

import com.mitra.ai.xyz.data.local.AppSettingsDao
import com.mitra.ai.xyz.data.local.entity.AppSettingsEntity
import com.mitra.ai.xyz.domain.model.AppSettings
import com.mitra.ai.xyz.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val settingsDao: AppSettingsDao
) : AppSettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomainModel() ?: AppSettings()
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertSettings(AppSettingsEntity.fromDomainModel(settings))
    }

    override suspend fun resetSettings() {
        settingsDao.clearSettings()
    }
} 