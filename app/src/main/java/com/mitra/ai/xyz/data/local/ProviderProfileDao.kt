package com.mitra.ai.xyz.data.local

import androidx.room.*
import com.mitra.ai.xyz.domain.model.AiProviderProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderProfileDao {
    @Query("SELECT * FROM provider_profiles ORDER BY `order` ASC")
    fun getProviderProfiles(): Flow<List<AiProviderProfile>>

    @Query("SELECT * FROM provider_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): AiProviderProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AiProviderProfile)

    @Update
    suspend fun updateProfile(profile: AiProviderProfile)

    @Delete
    suspend fun deleteProfile(profile: AiProviderProfile)

    @Query("UPDATE provider_profiles SET isActive = CASE WHEN id = :profileId THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(profileId: String)

    @Transaction
    suspend fun updateProfiles(profiles: List<AiProviderProfile>) {
        // Delete all existing profiles
        profiles.forEach { deleteProfile(it) }
        // Insert all profiles with new order
        profiles.forEach { insertProfile(it) }
    }

    @Query("DELETE FROM provider_profiles")
    suspend fun deleteAll()
} 