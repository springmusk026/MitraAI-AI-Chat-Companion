package com.mitra.ai.xyz.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class SettingsState(
    val profiles: List<AiProviderProfile> = emptyList(),
    val isAddingProfile: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                settingsRepository.getProviderProfiles()
                    .collect { profiles ->
                        _settingsState.update { it.copy(
                            profiles = profiles.sortedBy { it.order },
                            isLoading = false,
                            error = null
                        )}
                    }
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to load profiles: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    suspend fun fetchAvailableModels(apiKey: String, baseUrl: String?): List<String> {
        return try {
            settingsRepository.fetchAvailableModels(apiKey, baseUrl)
        } catch (e: Exception) {
            throw e
        }
    }

    fun addProfile(
        name: String,
        baseUrl: String = "https://api.openai.com/v1",
        apiKey: String,
        model: String = "gpt-3.5-turbo"
    ) {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                val newProfile = AiProviderProfile(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    order = _settingsState.value.profiles.size
                )
                settingsRepository.addProviderProfile(newProfile)
                _settingsState.update { it.copy(
                    isAddingProfile = false,
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to add profile: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun updateProfile(profile: AiProviderProfile) {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                settingsRepository.updateProviderProfile(profile)
                _settingsState.update { it.copy(
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to update profile: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun deleteProfile(profile: AiProviderProfile) {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                settingsRepository.deleteProviderProfile(profile)
                _settingsState.update { it.copy(
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to delete profile: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun setActiveProfile(profile: AiProviderProfile) {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                val updatedProfiles = _settingsState.value.profiles.map {
                    it.copy(isActive = it.id == profile.id)
                }
                updatedProfiles.forEach { settingsRepository.updateProviderProfile(it) }
                _settingsState.update { it.copy(
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to set active profile: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun reorderProfiles(profiles: List<AiProviderProfile>) {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }
                profiles.forEachIndexed { index, profile ->
                    settingsRepository.updateProviderProfile(profile.copy(order = index))
                }
                _settingsState.update { it.copy(
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _settingsState.update { it.copy(
                    error = "Failed to reorder profiles: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun setAddingProfile(isAdding: Boolean) {
        _settingsState.update { it.copy(isAddingProfile = isAdding) }
    }

    fun clearError() {
        _settingsState.update { it.copy(error = null) }
    }
} 