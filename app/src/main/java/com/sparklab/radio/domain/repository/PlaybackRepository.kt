package com.sparklab.radio.domain.repository

import com.sparklab.radio.domain.model.PlaybackState
import com.sparklab.radio.domain.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Playback abstraction implemented by [com.sparklab.radio.domain.repository.PlaybackController].
 * The UI/ViewModels depend only on this interface, so audio can be swapped
 * (ExoPlayer, a remote cast, etc.) without touching the screens.
 */
interface PlaybackRepository {
    val state: Flow<PlaybackState>

    /** Start [station] and set [queue] as the play queue (for next/previous). */
    fun play(station: Station, queue: List<Station> = listOf(station))

    fun togglePlayPause()
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun stop()
}
