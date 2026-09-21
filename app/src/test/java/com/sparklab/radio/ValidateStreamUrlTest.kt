package com.sparklab.radio

import com.sparklab.radio.domain.usecase.ValidateStreamUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the station URL rules used by the Add/Edit screen. */
class ValidateStreamUrlTest {

    @Test
    fun `accepts https stream url`() {
        assertNull(ValidateStreamUrl("https://stream.example.com/live.mp3"))
    }

    @Test
    fun `accepts icecast style url without extension`() {
        assertNull(ValidateStreamUrl("http://radio.example.com:8000/stream"))
    }

    @Test
    fun `accepts m3u and pls playlists`() {
        assertNull(ValidateStreamUrl("https://cdn.example.com/station.m3u"))
        assertNull(ValidateStreamUrl("https://cdn.example.com/station.pls"))
    }

    @Test
    fun `rejects empty url`() {
        assertEquals("Stream URL is required", ValidateStreamUrl("   "))
    }

    @Test
    fun `rejects non http scheme`() {
        assertEquals(
            "URL must start with http:// or https://",
            ValidateStreamUrl("ftp://example.com/stream"),
        )
    }

    @Test
    fun `rejects url without a valid host`() {
        assertEquals(
            "Enter a valid host, e.g. stream.example.com",
            ValidateStreamUrl("https://localhost/stream"),
        )
    }

    @Test
    fun `detects playlist files`() {
        assertTrue(ValidateStreamUrl.isPlaylist("https://x.com/a.m3u8"))
        assertTrue(ValidateStreamUrl.isPlaylist("https://x.com/a.pls?token=1"))
        assertTrue(!ValidateStreamUrl.isPlaylist("https://x.com/live.mp3"))
    }
}
