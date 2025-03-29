package com.mitra.ai.xyz.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Simulate initialization delay
            // In a real app, you would:
            // - Load user preferences
            // - Initialize databases
            // - Check authentication status
            // - Preload necessary data
            // - etc.
            delay(1500) // Minimum delay to show splash screen
            _isReady.value = true
        }
    }
} 