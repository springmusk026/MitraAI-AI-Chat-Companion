package com.mitra.ai.xyz.data.remote.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ModelResponse(
    val id: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("created")
    val createdAt: Long,
    val base_model: String?,
    val owned_by: String,
    val organization: String?,
    val task: String?,
    val context_length: Long?,
    val languages: List<String>?,
    val parameters: Long?,
    val parameters_str: String?,
    val tier: String?,
    val license: String?,
    val is_fine_tunable: Boolean?
)

class DateTypeAdapter : JsonSerializer<Long>, JsonDeserializer<Long> {
    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun serialize(src: Long?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Long {
        return try {
            // Try parsing as ISO date string first
            val dateStr = json?.asString
            if (dateStr != null) {
                try {
                    val instant = Instant.from(formatter.parse(dateStr))
                    instant.epochSecond
                } catch (e: DateTimeParseException) {
                    // If date parsing fails, try parsing as number
                    json.asLong
                }
            } else {
                // If not a string, try parsing as number
                json?.asLong ?: 0L
            }
        } catch (e: Exception) {
            0L // Return 0 for any parsing errors
        }
    }
} 