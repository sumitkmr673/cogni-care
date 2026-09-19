package com.example.cognicare.core.di

import com.example.cognicare.BuildConfig
import com.example.cognicare.data.remote.AuthInterceptor
import com.example.cognicare.data.remote.BaseUrlInterceptor
import com.example.cognicare.data.remote.CogniCareApi
import com.example.cognicare.data.remote.TokenAuthenticator
import com.example.cognicare.data.remote.VoiceApi
import com.example.cognicare.data.remote.VoiceBaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** The app-only voice service's client, kept apart from the shared backend's. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VoiceService

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Full request/response logging is a debug-only convenience: it would otherwise put
            // patient data and bearer tokens in release logcat.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // BaseUrlInterceptor rewrites the scheme/host/port of every request at runtime;
            // this only needs to be a syntactically valid URL for Retrofit to resolve paths against.
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideCogniCareApi(retrofit: Retrofit): CogniCareApi = retrofit.create(CogniCareApi::class.java)

    /**
     * Same token and silent re-login as the main client, but short timeouts: when the GPU machine
     * is off, the patient should hear back from on-device matching within a few seconds rather
     * than wait out the main client's 20 s.
     */
    @Provides
    @Singleton
    @VoiceService
    fun provideVoiceOkHttpClient(
        authInterceptor: AuthInterceptor,
        voiceBaseUrlInterceptor: VoiceBaseUrlInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(voiceBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(3, TimeUnit.SECONDS)
            // Whisper on a long Daily Recall answer takes several seconds.
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideVoiceApi(@VoiceService okHttpClient: OkHttpClient, json: Json): VoiceApi =
        Retrofit.Builder()
            // VoiceBaseUrlInterceptor sets the real origin per request, as for the main API.
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(VoiceApi::class.java)
}
