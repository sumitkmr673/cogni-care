package com.example.cognicare.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceEndpointerTest {

    private val config = EndpointConfig(noSpeechTimeoutMs = 3_000, trailingSilenceMs = 1_000, maxDurationMs = 8_000)

    /** Feeds one level per 100 ms from [fromMs] until [toMs], returning the last decision. */
    private fun SilenceEndpointer.feed(level: Int, fromMs: Long, toMs: Long): EndpointDecision {
        var decision = EndpointDecision.CONTINUE
        var t = fromMs
        while (t <= toMs && decision == EndpointDecision.CONTINUE) {
            decision = onLevel(level, t)
            t += 100
        }
        return decision
    }

    @Test
    fun `stops a moment after the patient finishes speaking`() {
        val endpointer = SilenceEndpointer(config)
        endpointer.feed(200, 100, 300)                       // quiet room
        assertEquals(EndpointDecision.CONTINUE, endpointer.feed(6_000, 400, 1_500)) // speaking
        assertEquals(EndpointDecision.CONTINUE, endpointer.onLevel(200, 2_000))    // short pause
        assertEquals(EndpointDecision.DONE, endpointer.onLevel(200, 2_500))
        assertTrue(endpointer.heardSpeech)
    }

    @Test
    fun `a pause shorter than the trailing silence does not cut them off`() {
        val endpointer = SilenceEndpointer(config)
        endpointer.feed(200, 100, 300)
        endpointer.feed(6_000, 400, 1_000)
        assertEquals(EndpointDecision.CONTINUE, endpointer.feed(200, 1_100, 1_800)) // 800 ms thinking
        endpointer.feed(6_000, 1_900, 2_500)                                        // carries on
        assertEquals(EndpointDecision.CONTINUE, endpointer.onLevel(200, 3_400))
    }

    @Test
    fun `nothing said means no speech, not an empty upload`() {
        val endpointer = SilenceEndpointer(config)
        assertEquals(EndpointDecision.NO_SPEECH, endpointer.feed(200, 100, 5_000))
        assertFalse(endpointer.heardSpeech)
    }

    @Test
    fun `background noise alone is not mistaken for speech`() {
        // A fan or TV: steady level well above the fixed minimum, but it is the room, not a voice.
        val endpointer = SilenceEndpointer(config)
        assertEquals(EndpointDecision.NO_SPEECH, endpointer.feed(2_500, 100, 5_000))
    }

    @Test
    fun `speech over background noise is still heard`() {
        val endpointer = SilenceEndpointer(config)
        endpointer.feed(2_500, 100, 300)
        endpointer.feed(12_000, 400, 1_000)
        assertTrue(endpointer.heardSpeech)
    }

    @Test
    fun `someone who keeps talking is stopped at the maximum length`() {
        val endpointer = SilenceEndpointer(config)
        endpointer.feed(200, 100, 300)
        assertEquals(EndpointDecision.DONE, endpointer.feed(6_000, 400, 9_000))
    }
}
