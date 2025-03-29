package com.mitra.ai.xyz.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val defaultBaseUrl = "https://api.openai.com/"
    private var currentBaseUrl: String = defaultBaseUrl
    private var currentService: OpenAIService? = null

    fun getOkHttpClient(): OkHttpClient = okHttpClient

    fun createService(baseUrl: String? = null): OpenAIService {
        val effectiveBaseUrl = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { if (it.endsWith("/")) it else "$it/" }
            ?: defaultBaseUrl

        if (currentService == null || effectiveBaseUrl != currentBaseUrl) {
            currentBaseUrl = effectiveBaseUrl
            currentService = Retrofit.Builder()
                .baseUrl(effectiveBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenAIService::class.java)
        }

        return currentService!!
    }
}