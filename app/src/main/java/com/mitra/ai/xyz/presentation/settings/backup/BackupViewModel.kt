package com.mitra.ai.xyz.presentation.settings.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.ai.xyz.domain.model.BackupComponent
import com.mitra.ai.xyz.domain.model.ConflictResolution
import com.mitra.ai.xyz.domain.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupService: BackupService
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun setSelectedComponents(components: Set<BackupComponent>) {
        _state.update { it.copy(selectedComponents = components) }
    }

    fun setConflictResolution(resolution: ConflictResolution) {
        _state.update { it.copy(conflictResolution = resolution) }
    }

    fun createBackup(outputUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            backupService.createBackup(state.value.selectedComponents, outputUri)
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        success = "Backup created successfully"
                    )}
                }
                .onFailure { error ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create backup"
                    )}
                }
        }
    }

    fun restoreBackup(inputUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            backupService.restoreBackup(inputUri, state.value.conflictResolution)
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        success = "Backup restored successfully"
                    )}
                }
                .onFailure { error ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to restore backup"
                    )}
                }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, success = null) }
    }
}

data class BackupState(
    val selectedComponents: Set<BackupComponent> = setOf(),
    val conflictResolution: ConflictResolution = ConflictResolution.MERGE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
) 