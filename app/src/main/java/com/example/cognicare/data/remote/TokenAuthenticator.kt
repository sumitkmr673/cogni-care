package com.example.cognicare.data.remote

import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.local.SecureCredentialStore
import com.example.cognicare.data.remote.dto.LoginRequestDto
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * The backend's access token lasts 30 minutes with no refresh endpoint (see
 * cogni-care/backend/app/security.py). On a 401, this silently repeats the original email/password
 * login using [SecureCredentialStore] and retries the request once — otherwise every patient or
 * caregiver session would need re-typing a password every half hour.
 *
 * [api] is a [Provider] to break the dependency cycle: the [okhttp3.OkHttpClient] this
 * authenticator attaches to is itself a dependency of the Retrofit instance that builds [api].
 */
class TokenAuthenticator @Inject constructor(
    private val credentialStore: SecureCredentialStore,
    private val preferences: AppPreferences,
    private val api: Provider<CogniCareApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRIES) return null
        val credentials = credentialStore.read() ?: return null

        val newToken = runBlocking {
            runCatching { api.get().login(LoginRequestDto(credentials.email, credentials.password)).access_token }
                .onSuccess { token ->
                    val current = preferences.session.firstOrNull()
                    if (current != null) preferences.saveSession(current.copy(accessToken = token))
                }
                .getOrNull()
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}
