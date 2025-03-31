package com.mitra.ai.xyz.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1.0f
) {
    @Serializable
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }
} 