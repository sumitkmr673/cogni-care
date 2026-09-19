package com.example.cognicare.repository

import com.example.cognicare.data.remote.voiceOrigin
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceServiceTest {

    @Test
    fun `the voice service follows the backend's host on port 8100`() {
        val origin = voiceOrigin("http://192.168.29.55:8000/".toHttpUrl(), override = null)
        assertEquals("192.168.29.55", origin.host)
        assertEquals(8100, origin.port)
    }

    @Test
    fun `an explicit voice URL wins`() {
        val origin = voiceOrigin("http://10.0.2.2:8000/".toHttpUrl(), "http://192.168.1.9:9000/".toHttpUrl())
        assertEquals("192.168.1.9", origin.host)
        assertEquals(9000, origin.port)
    }

    @Test
    fun `after a failure the service is skipped until the cooldown ends`() {
        var now = 1_000L
        val breaker = VoiceServiceBreaker(cooldownMs = 60_000L, clock = { now })
        assertTrue(breaker.shouldTry())

        breaker.recordFailure()
        now += 59_999L
        assertFalse(breaker.shouldTry())

        now += 1L
        assertTrue(breaker.shouldTry())
    }

    @Test
    fun `a success clears the cooldown`() {
        var now = 0L
        val breaker = VoiceServiceBreaker(cooldownMs = 60_000L, clock = { now })
        breaker.recordFailure()
        breaker.recordSuccess()
        assertTrue(breaker.shouldTry())
    }
}
