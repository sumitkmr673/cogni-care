package com.example.cognicare.core.audio

/**
 * How long to keep recording an answer for Whisper. Android's own recogniser stops by itself when
 * the patient goes quiet; a plain recording doesn't, so this decides it from the microphone level.
 */
data class EndpointConfig(
    /** Give up (nothing to upload) if no speech starts within this long. */
    val noSpeechTimeoutMs: Long,
    /** After speech, this much quiet means they have finished. */
    val trailingSilenceMs: Long,
    /** Hard stop, however long they keep talking. */
    val maxDurationMs: Long,
    /** Level (0..32767, MediaRecorder.getMaxAmplitude) that counts as speech even in a silent room. */
    val minSpeechLevel: Int = 1_500,
    /** Speech must also be this many times louder than the room's background noise. */
    val noiseMultiplier: Float = 3f,
    /** The first stretch of the recording measures the background noise. */
    val calibrationMs: Long = 300
) {
    companion object {
        /** A short answer to a question: a word or a few. Elderly speakers pause, so be patient. */
        val SHORT_ANSWER = EndpointConfig(noSpeechTimeoutMs = 7_000, trailingSilenceMs = 1_600, maxDurationMs = 15_000)

        /** Talking about their day: longer pauses between thoughts are normal. */
        val LONG_ANSWER = EndpointConfig(noSpeechTimeoutMs = 8_000, trailingSilenceMs = 3_000, maxDurationMs = 60_000)
    }
}

enum class EndpointDecision { CONTINUE, DONE, NO_SPEECH }

/** Fed the microphone level every ~100 ms; says when to stop. Pure, so it is unit-tested. */
class SilenceEndpointer(private val config: EndpointConfig) {

    var heardSpeech = false
        private set

    private var noiseFloor = 0
    private var lastSpeechAtMs = 0L

    fun onLevel(level: Int, elapsedMs: Long): EndpointDecision {
        if (elapsedMs <= config.calibrationMs && !heardSpeech) {
            noiseFloor = maxOf(noiseFloor, level)
        }
        val threshold = maxOf(config.minSpeechLevel, (noiseFloor * config.noiseMultiplier).toInt())
        if (level >= threshold && elapsedMs > config.calibrationMs) {
            heardSpeech = true
            lastSpeechAtMs = elapsedMs
        }
        return when {
            elapsedMs >= config.maxDurationMs -> if (heardSpeech) EndpointDecision.DONE else EndpointDecision.NO_SPEECH
            !heardSpeech && elapsedMs >= config.noSpeechTimeoutMs -> EndpointDecision.NO_SPEECH
            heardSpeech && elapsedMs - lastSpeechAtMs >= config.trailingSilenceMs -> EndpointDecision.DONE
            else -> EndpointDecision.CONTINUE
        }
    }
}
