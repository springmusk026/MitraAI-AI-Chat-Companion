package com.mitra.ai.xyz.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "provider_profiles")
@Serializable
data class AiProviderProfile(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val isActive: Boolean = false,
    val order: Int = 0
)

data class AiProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
) 