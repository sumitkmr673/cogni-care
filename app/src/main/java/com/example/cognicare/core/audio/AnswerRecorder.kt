package com.example.cognicare.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** A finished recording: [heardSpeech] false means only silence, so there is nothing to send. */
data class Recording(val file: File, val heardSpeech: Boolean)

/**
 * Records one spoken answer for Whisper as AAC in .m4a (16 kHz mono, ~32 kbps: about 4 KB a second,
 * well under the voice service's 5 MB limit even for a minute of talking), stopping by itself when
 * [SilenceEndpointer] decides the patient has finished, or when [stopRequested] turns true.
 */
class AnswerRecorder(private val context: Context) {

    /** Throws if the microphone can't be opened (e.g. another app holds it); callers fall back. */
    suspend fun record(config: EndpointConfig, stopRequested: () -> Boolean): Recording = withContext(Dispatchers.IO) {
        val file = File.createTempFile("answer", ".m4a", context.cacheDir)
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        val endpointer = SilenceEndpointer(config)
        var started = false
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(16_000)
            recorder.setAudioChannels(1)
            recorder.setAudioEncodingBitRate(32_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            started = true

            val startedAt = System.currentTimeMillis()
            recorder.maxAmplitude // the first reading is always 0; discard it
            while (true) {
                delay(POLL_MS)
                val decision = endpointer.onLevel(recorder.maxAmplitude, System.currentTimeMillis() - startedAt)
                if (decision != EndpointDecision.CONTINUE || stopRequested()) break
            }
            // A patient who tapped stop believes they answered, so a quiet voice is still sent.
            Recording(file, heardSpeech = endpointer.heardSpeech || stopRequested())
        } catch (error: Throwable) {
            file.delete()
            throw error
        } finally {
            // stop() throws if almost nothing was recorded; the file is then unusable anyway.
            if (started) runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private companion object {
        const val POLL_MS = 100L
    }
}
