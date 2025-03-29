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

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
    }

    override suspend fun getApiKey(): String {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.API_KEY] ?: ""
        }.first()
    }

    override suspend fun getActiveProviderOnce(): String {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROVIDER] ?: "OPENAI"
        }.first()
    }

    override suspend fun setActiveProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROVIDER] = provider
        }
    }

    override fun getActiveProviderFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROVIDER] ?: "OPENAI"
        }
    }

    override suspend fun updateProviderConfig(
        apiKey: String,
        baseUrl: String,
        model: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
            if (baseUrl.isNotEmpty()) {
                preferences[PreferencesKeys.BASE_URL] = baseUrl
            }
            if (model.isNotEmpty()) {
                preferences[PreferencesKeys.MODEL] = model
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
            val preferences = context.dataStore.data.first()
            AiProviderConfig(
                baseUrl = preferences[PreferencesKeys.BASE_URL] ?: "",
                apiKey = preferences[PreferencesKeys.API_KEY] ?: "",
                model = preferences[PreferencesKeys.MODEL] ?: ""
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
} 