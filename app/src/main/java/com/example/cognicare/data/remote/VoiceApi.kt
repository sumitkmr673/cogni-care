package com.example.cognicare.data.remote

import com.example.cognicare.data.remote.dto.InterpretResponseDto
import com.example.cognicare.data.remote.dto.InterpretTextRequestDto
import com.example.cognicare.data.remote.dto.TranscribeResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * The app-only voice service (cogni-care voice_service: Whisper + Qwen on the GPU machine).
 * Separate from [CogniCareApi] on purpose — different host/port, and the shared backend used by
 * the web portal knows nothing about it. It accepts the same access token.
 */
interface VoiceApi {

    /** Android's recogniser already produced the transcript; Qwen picks which option it meant. */
    @POST("app/voice/interpret-text")
    suspend fun interpretText(@Body body: InterpretTextRequestDto): InterpretResponseDto

    /**
     * Whisper only, for the talking games, which check the words themselves. [language] and
     * [hint] are form fields beside the recording, as voice_service/app/main.py expects.
     */
    @Multipart
    @POST("app/voice/transcribe")
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody?,
        @Part("hint") hint: RequestBody?
    ): TranscribeResponseDto
}
