package com.mitra.ai.xyz.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mitra.ai.xyz.data.local.ProviderProfileDao
import com.mitra.ai.xyz.data.remote.OpenAIServiceFactory
import com.mitra.ai.xyz.domain.model.AiProviderConfig
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import com.mitra.ai.xyz.domain.model.AppSettings
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openAIServiceFactory: OpenAIServiceFactory,
    private val providerProfileDao: ProviderProfileDao
) : SettingsRepository {

    override suspend fun getApiKey(): String {
        return getActiveProfile()?.apiKey ?: ""
    }

    override suspend fun getActiveProviderOnce(): String {
        return getActiveProfile()?.name ?: "OPENAI"
    }

    override suspend fun setActiveProvider(provider: String) {
        withContext(Dispatchers.IO) {
            providerProfileDao.getProviderProfiles().first().find { it.name == provider }?.let { profile ->
                providerProfileDao.setActiveProfile(profile.id)
            }
        }
    }

    override fun getActiveProviderFlow(): Flow<String> {
        return providerProfileDao.getProviderProfiles().map { profiles ->
            profiles.find { it.isActive }?.name ?: "OPENAI"
        }
    }

    override suspend fun updateProviderConfig(
        apiKey: String,
        baseUrl: String,
        model: String
    ) {
        withContext(Dispatchers.IO) {
            val activeProfile = getActiveProfile()
            if (activeProfile != null) {
                val updatedProfile = activeProfile.copy(
                    apiKey = apiKey,
                    baseUrl = baseUrl.takeIf { it.isNotEmpty() } ?: activeProfile.baseUrl,
                    model = model.takeIf { it.isNotEmpty() } ?: activeProfile.model
                )
                providerProfileDao.updateProfile(updatedProfile)
            } else {
                // Create new profile if none exists
                val newProfile = AiProviderProfile(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "OPENAI",
                    apiKey = apiKey,
                    baseUrl = baseUrl.takeIf { it.isNotEmpty() } ?: "https://api.openai.com/v1",
                    model = model.takeIf { it.isNotEmpty() } ?: "gpt-3.5-turbo",
                    isActive = true,
                    order = 0
                )
                providerProfileDao.insertProfile(newProfile)
            }
        }
    }

    override suspend fun getProviderConfig(): AiProviderConfig = withContext(Dispatchers.IO) {
        val activeProfile = getActiveProfile()
        return@withContext if (activeProfile != null) {
            AiProviderConfig(
                baseUrl = activeProfile.baseUrl,
                apiKey = activeProfile.apiKey,
                model = activeProfile.model
            )
        } else {
            // Return default config if no active profile
            AiProviderConfig(
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                model = "gpt-3.5-turbo"
            )
        }
    }

    override suspend fun fetchAvailableModels(apiKey: String, baseUrl: String?): List<String> {
        return try {
            val service = openAIServiceFactory.createService(baseUrl)
            val response = service.listModels(
                authorization = "Bearer $apiKey"
            )
            response.data
                .map { it.id }
                .sorted()
        } catch (e: Exception) {
            // If API call fails, return default models
            listOf(
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k",
                "gpt-4",
                "gpt-4-turbo-preview",
                "gpt-4-32k"
            )
        }
    }

    override fun getProviderProfiles(): Flow<List<AiProviderProfile>> {
        return providerProfileDao.getProviderProfiles()
    }

    override suspend fun addProviderProfile(profile: AiProviderProfile) {
        withContext(Dispatchers.IO) {
            providerProfileDao.insertProfile(profile)
        }
    }

    override suspend fun updateProviderProfile(profile: AiProviderProfile) {
        withContext(Dispatchers.IO) {
            providerProfileDao.updateProfile(profile)
        }
    }

    override suspend fun deleteProviderProfile(profile: AiProviderProfile) {
        withContext(Dispatchers.IO) {
            providerProfileDao.deleteProfile(profile)
        }
    }

    override suspend fun getActiveProfile(): AiProviderProfile? {
        return withContext(Dispatchers.IO) {
            providerProfileDao.getActiveProfile()
        }
    }

    override suspend fun clearProviderProfiles() = withContext(Dispatchers.IO) {
        providerProfileDao.deleteAll()
    }

    override fun getAppSettings(): Flow<AppSettings> = flow {
        // For now, emit default settings
        // TODO: Implement actual settings storage
        emit(AppSettings())
    }

    override suspend fun updateAppSettings(settings: AppSettings) {
        // TODO: Implement actual settings storage
    }
} 