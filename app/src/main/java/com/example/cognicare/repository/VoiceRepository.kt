package com.example.cognicare.repository

import com.example.cognicare.data.remote.VoiceApi
import com.example.cognicare.data.remote.dto.InterpretTextRequestDto
import com.example.cognicare.data.remote.dto.VoiceContextDto
import com.example.cognicare.data.remote.dto.VoiceOptionDto
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** What the voice service made of an answer. */
sealed interface VoiceChoice {
    /** One of the offered option ids. */
    data class Chosen(val id: String) : VoiceChoice

    /** The model heard it but it was unclear or contradicted itself: ask again, never guess. */
    data object NotUnderstood : VoiceChoice

    /** No answer from the service (PC off, other Wi-Fi, 503, not signed in): match on the device. */
    data object Unavailable : VoiceChoice
}

data class VoiceOptionRequest(val id: String, val label: String)

interface VoiceRepository {
    suspend fun interpret(
        transcript: String,
        question: String?,
        options: List<VoiceOptionRequest>,
        languageTag: String
    ): VoiceChoice

    /**
     * Whisper's transcript of a recorded answer. [hint] should list every answer on screen (never
     * only the right one), so Whisper is nudged toward all of them equally.
     */
    suspend fun transcribe(audioFile: File, languageTag: String, hint: String?): Transcript

    /** False while the service is cooling off after a failure: record with Android's recogniser instead. */
    fun shouldTryService(): Boolean
}

sealed interface Transcript {
    data class Text(val text: String) : Transcript

    /** Whisper heard nothing usable: ask again, as for any unclear answer. */
    data object Empty : Transcript

    /** Service unreachable or refused: the phone's own recogniser takes over. */
    data object Unavailable : Transcript
}

/**
 * After a failure, skip the service for [cooldownMs] so each spoken answer doesn't wait out a
 * connect timeout while the GPU machine is off. One try is let through when the cooldown ends.
 */
class VoiceServiceBreaker(
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    @Volatile
    private var retryAt = 0L

    fun shouldTry(): Boolean = clock() >= retryAt

    fun recordFailure() {
        retryAt = clock() + cooldownMs
    }

    fun recordSuccess() {
        retryAt = 0L
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 60_000L
    }
}

@Singleton
class RemoteVoiceRepository @Inject constructor(
    private val api: VoiceApi
) : VoiceRepository {

    private val breaker = VoiceServiceBreaker()

    override suspend fun interpret(
        transcript: String,
        question: String?,
        options: List<VoiceOptionRequest>,
        languageTag: String
    ): VoiceChoice {
        if (transcript.isBlank()) return VoiceChoice.NotUnderstood
        if (!breaker.shouldTry()) return VoiceChoice.Unavailable
        val request = InterpretTextRequestDto(
            // The service caps both; trimming here keeps a long ramble from turning into a 422.
            transcript = transcript.trim().take(MAX_TRANSCRIPT),
            context = VoiceContextDto(
                language = languageTag,
                question = question,
                options = options.map { VoiceOptionDto(it.id, it.label.take(MAX_LABEL)) }
            )
        )
        return try {
            val response = api.interpretText(request)
            breaker.recordSuccess()
            val choice = response.choice
            if (choice != null && options.any { it.id == choice }) VoiceChoice.Chosen(choice) else VoiceChoice.NotUnderstood
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Network errors, HTTP 4xx/5xx and malformed replies alike: the device takes over.
            breaker.recordFailure()
            VoiceChoice.Unavailable
        }
    }

    override fun shouldTryService(): Boolean = breaker.shouldTry()

    override suspend fun transcribe(audioFile: File, languageTag: String, hint: String?): Transcript {
        if (!breaker.shouldTry()) return Transcript.Unavailable
        val audio = MultipartBody.Part.createFormData(
            "audio", audioFile.name, audioFile.asRequestBody("audio/mp4".toMediaType())
        )
        val plain = "text/plain".toMediaType()
        return try {
            val response = api.transcribeAudio(
                audio = audio,
                language = languageTag.toRequestBody(plain),
                hint = hint?.takeIf { it.isNotBlank() }?.take(MAX_HINT)?.toRequestBody(plain)
            )
            breaker.recordSuccess()
            response.transcript.trim().takeIf { it.isNotEmpty() }?.let(Transcript::Text) ?: Transcript.Empty
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            breaker.recordFailure()
            Transcript.Unavailable
        }
    }

    private companion object {
        const val MAX_TRANSCRIPT = 500
        const val MAX_LABEL = 120
        const val MAX_HINT = 300
    }
}

/** For the speak button, a composable outside any ViewModel, to reach the singleton repository. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface VoiceRepositoryEntryPoint {
    fun voiceRepository(): VoiceRepository
}
