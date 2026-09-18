package com.example.cognicare.data.remote

import com.example.cognicare.BuildConfig
import com.example.cognicare.data.local.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The backend origin (scheme/host/port) as a live value, so a caregiver can point the app at a
 * different deployment (a hosted URL instead of the compiled-in default) from Settings without
 * a rebuild. [BaseUrlInterceptor] applies it to every request.
 */
@Singleton
class ApiConfig @Inject constructor(preferences: AppPreferences) {

    private val defaultBaseUrl = BuildConfig.API_BASE_URL.toHttpUrl()

    private val _baseUrl = MutableStateFlow(defaultBaseUrl)
    val baseUrl: StateFlow<HttpUrl> = _baseUrl

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        preferences.apiBaseUrlOverride
            .onEach { override -> _baseUrl.value = override?.toHttpUrlOrNull() ?: defaultBaseUrl }
            .launchIn(scope)
    }

    private fun String.toHttpUrlOrNull(): HttpUrl? = runCatching {
        (if (endsWith("/")) this else "$this/").toHttpUrl()
    }.getOrNull()
}
