package com.mitra.ai.xyz.domain.repository

import com.mitra.ai.xyz.domain.model.AiProviderConfig
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import kotlinx.coroutines.flow.Flow

data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = ""
)

interface SettingsRepository {
    suspend fun getApiKey(): String
    suspend fun getActiveProviderOnce(): String
    suspend fun setActiveProvider(provider: String)
    fun getActiveProviderFlow(): Flow<String>
    suspend fun updateProviderConfig(
        apiKey: String,
        baseUrl: String = "",
        model: String = ""
    )
    suspend fun getProviderConfig(): AiProviderConfig
    suspend fun fetchAvailableModels(apiKey: String, baseUrl: String? = null): List<String>
    fun getProviderProfiles(): Flow<List<AiProviderProfile>>
    suspend fun addProviderProfile(profile: AiProviderProfile)
    suspend fun updateProviderProfile(profile: AiProviderProfile)
    suspend fun deleteProviderProfile(profile: AiProviderProfile)
    suspend fun getActiveProfile(): AiProviderProfile?
}