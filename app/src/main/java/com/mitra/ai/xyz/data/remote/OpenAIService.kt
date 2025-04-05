package com.mitra.ai.xyz.data.remote

import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response

interface OpenAIService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse

    @Streaming
    @POST("chat/completions")
    suspend fun createStreamingChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ResponseBody>

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): ModelsResponse

    data class ChatRequest(
        val model: String = "gpt-3.5-turbo",
        val messages: List<ChatMessage>,
        val temperature: Double = 0.7,
        val max_tokens: Int = 1000,
        val stream: Boolean = false
    )

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatResponse(
        val id: String,
        val choices: List<Choice>,
        val created: Long,
        val model: String,
        val usage: Usage
    )

    data class Choice(
        val index: Int,
        val message: ChatMessage,
        val finish_reason: String
    )

    data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )

    data class ModelsResponse(
        val data: List<Model>,
        @SerializedName("object") val objectType: String
    )

    data class Model(
        val id: String,
        @SerializedName("created")
        val createdAt: String,
        val owned_by: String,
        @SerializedName("object") val objectType: String,
        val base_model: String? = null,
        val organization: String? = null,
        val task: String? = null,
        val context_length: Long? = null,
        val languages: List<String>? = null,
        val parameters: Long? = null,
        val parameters_str: String? = null,
        val tier: String? = null,
        val license: String? = null,
        val is_fine_tunable: Boolean? = null
    )

    // Streaming response data classes
    data class StreamResponse(
        val id: String,
        val choices: List<StreamChoice>,
        val created: Long,
        @SerializedName("object") val objectType: String,
        val model: String
    )

    data class StreamChoice(
        val index: Int,
        val delta: DeltaMessage,
        val finish_reason: String?
    )

    data class DeltaMessage(
        val role: String? = null,
        val content: String? = null
    )
} 