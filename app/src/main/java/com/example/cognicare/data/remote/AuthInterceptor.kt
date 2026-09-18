package com.example.cognicare.data.remote

import com.example.cognicare.data.local.AppPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches the cached access token to every request except login itself. Retrofit's suspend
 * functions already run off the main thread, so the [runBlocking] read here never blocks the UI.
 */
class AuthInterceptor @Inject constructor(
    private val preferences: AppPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.endsWith("/auth/login")) return chain.proceed(request)
        // A caller that set its own token (confirming a brand-new login) keeps it.
        if (request.header("Authorization") != null) return chain.proceed(request)

        val token = runBlocking { preferences.currentAccessToken() }
        val authorized = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(authorized)
    }
}
