package com.mitra.ai.xyz.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.google.gson.Gson
import com.mitra.ai.xyz.data.local.AppDatabase
import com.mitra.ai.xyz.data.local.ChatDao
import com.mitra.ai.xyz.data.local.MessageDao
import com.mitra.ai.xyz.data.local.ProviderProfileDao
import com.mitra.ai.xyz.data.remote.OpenAIService
import com.mitra.ai.xyz.data.remote.OpenAIServiceFactory
import com.mitra.ai.xyz.data.remote.SSEInterceptor
import com.mitra.ai.xyz.data.remote.SSEListener
import com.mitra.ai.xyz.data.repository.ChatRepositoryImpl
import com.mitra.ai.xyz.data.repository.SettingsRepositoryImpl
import com.mitra.ai.xyz.domain.repository.ChatRepository
import com.mitra.ai.xyz.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // This will recreate tables if schema changed
        .build()
    }

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideProviderProfileDao(db: AppDatabase): ProviderProfileDao = db.providerProfileDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val request = chain.request()
                val authHeader = request.header("Authorization")
                
                // If Authorization header exists but doesn't start with "Bearer ", prepend it
                val newRequest = if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $authHeader")
                        .build()
                } else {
                    request
                }

                chain.proceed(newRequest)
            }
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("sseClient")
    fun provideSSEClient(okHttpClient: OkHttpClient): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(SSEInterceptor(object : SSEListener {
                override fun onEvent(data: String) {
                    // This will be overridden by the repository
                }
                override fun onFailure(t: Throwable) {
                    // This will be overridden by the repository
                }
                override fun onComplete() {
                    // This will be overridden by the repository
                }
            }))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIServiceFactory(okHttpClient: OkHttpClient): OpenAIServiceFactory {
        return OpenAIServiceFactory(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideOpenAIService(openAIServiceFactory: OpenAIServiceFactory): OpenAIService {
        // We'll create the service with default URL, but the factory will handle custom URLs when needed
        return openAIServiceFactory.createService()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
        openAIServiceFactory: OpenAIServiceFactory,
        providerProfileDao: ProviderProfileDao
    ): SettingsRepository {
        return SettingsRepositoryImpl(context, openAIServiceFactory, providerProfileDao)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        openAIService: OpenAIService,
        settingsRepository: SettingsRepository,
        chatDao: ChatDao,
        messageDao: MessageDao,
        gson: Gson,
        @Named("sseClient") sseClient: OkHttpClient,
        openAIServiceFactory: OpenAIServiceFactory
    ): ChatRepository {
        return ChatRepositoryImpl(
            openAIService,
            settingsRepository,
            chatDao,
            messageDao,
            gson,
            sseClient,
            openAIServiceFactory
        )
    }
} 