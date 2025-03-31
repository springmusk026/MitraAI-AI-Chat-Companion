package com.mitra.ai.xyz.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mitra.ai.xyz.domain.model.AppSettings

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // We'll only ever have one row
    val themeMode: Int,
    val fontScale: Float
) {
    fun toDomainModel(): AppSettings {
        return AppSettings(
            themeMode = when (themeMode) {
                0 -> AppSettings.ThemeMode.LIGHT
                1 -> AppSettings.ThemeMode.DARK
                else -> AppSettings.ThemeMode.SYSTEM
            },
            fontScale = fontScale
        )
    }

    companion object {
        fun fromDomainModel(settings: AppSettings): AppSettingsEntity {
            return AppSettingsEntity(
                themeMode = when (settings.themeMode) {
                    AppSettings.ThemeMode.LIGHT -> 0
                    AppSettings.ThemeMode.DARK -> 1
                    AppSettings.ThemeMode.SYSTEM -> 2
                },
                fontScale = settings.fontScale
            )
        }
    }
} 