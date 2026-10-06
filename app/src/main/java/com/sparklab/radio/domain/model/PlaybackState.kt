package com.sparklab.radio.domain.model

/**
 * Observable snapshot of the player, consumed by the UI (Now Playing screen,
 * mini-player) and used to drive the connection status text.
 */
data class PlaybackState(
    val current: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val nowPlayingTitle: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    /** Human status used for the connection pill on Now Playing. */
    val status: Status
        get() = when {
            hasError -> Status.ERROR
            isBuffering -> Status.CONNECTING
            isPlaying -> Status.PLAYING
            current != null -> Status.PAUSED
            else -> Status.IDLE
        }

    enum class Status { IDLE, CONNECTING, PLAYING, PAUSED, ERROR }
}
