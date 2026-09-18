package com.example.cognicare.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Rewrites every request's scheme/host/port to [ApiConfig]'s current value, keeping Retrofit's
 * declared path and query untouched. Retrofit still needs a syntactically valid base URL at
 * build time (BuildConfig.API_BASE_URL); this is what makes it actually live-configurable.
 */
class BaseUrlInterceptor @Inject constructor(
    private val apiConfig: ApiConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = apiConfig.baseUrl.value
        val originalUrl = chain.request().url
        val redirectedUrl = originalUrl.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(redirectedUrl).build())
    }
}
