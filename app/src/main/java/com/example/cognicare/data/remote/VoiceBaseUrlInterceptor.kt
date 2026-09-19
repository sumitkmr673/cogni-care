package com.example.cognicare.data.remote

import com.example.cognicare.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** The voice service's default port (voice_service/.env.example, VOICE_PORT). */
const val DEFAULT_VOICE_PORT = 8100

/**
 * Where the voice service lives. It runs on the same GPU machine as the local backend, so by
 * default it follows [ApiConfig]'s host (including a host changed at runtime in Settings) on
 * [voicePort]. [override] (COGNICARE_VOICE_BASE_URL in local.properties) pins it elsewhere.
 */
fun voiceOrigin(apiBase: HttpUrl, override: HttpUrl?, voicePort: Int = DEFAULT_VOICE_PORT): HttpUrl =
    override ?: apiBase.newBuilder().port(voicePort).build()

/** Like [BaseUrlInterceptor], but pointing requests at the voice service. */
class VoiceBaseUrlInterceptor @Inject constructor(
    private val apiConfig: ApiConfig
) : Interceptor {

    private val override: HttpUrl? = BuildConfig.VOICE_BASE_URL.takeIf { it.isNotBlank() }?.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = voiceOrigin(apiConfig.baseUrl.value, override)
        val redirectedUrl = chain.request().url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(redirectedUrl).build())
    }
}
