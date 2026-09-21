package com.sparklab.radio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sparklab.radio.RadioApp
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.PlaybackState
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.repository.PlaybackRepository
import com.sparklab.radio.domain.repository.SettingsRepository
import com.sparklab.radio.domain.repository.StationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel shared by the main screens. Exposes catalog + playback state and
 * the actions the UI needs (play, favorite, queue navigation).
 *
 * AndroidViewModel so we can reach the [RadioApp] DI container without Hilt.
 */
class RadioViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as RadioApp).container
    val stationRepo: StationRepository = container.stationRepository
    val settingsRepo: SettingsRepository = container.settingsRepository
    private val playback: PlaybackRepository = container.playbackRepository

    /** Flat list of every station, kept hot for the Stations screen. */
    val allStations: StateFlow<List<Station>> = stationRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Station>> = stationRepo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStations: StateFlow<List<Station>> = stationRepo.observeUserStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Stations grouped by genre, ready for the Explore grid. */
    val stationsByGenre: StateFlow<Map<Genre, List<Station>>> =
        allStations.map { list -> list.groupBy { it.genre } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val playbackState: StateFlow<PlaybackState> = playback.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackState())

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Play [station] within [queue]. The queue drives next/previous on both the
     * phone and — because it is the same MediaSession — Android Auto.
     */
    fun play(station: Station, queue: List<Station>) {
        playback.play(station, queue)
        viewModelScope.launch { stationRepo.markPlayed(station.id) }
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun next() = playback.next()
    fun previous() = playback.previous()

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { stationRepo.setFavorite(station, !station.isFavorite) }
    }

    fun saveStation(station: Station, onDone: () -> Unit) {
        viewModelScope.launch {
            stationRepo.upsertUserStation(station)
            onDone()
        }
    }

    fun deleteStation(id: String) {
        viewModelScope.launch { stationRepo.deleteUserStation(id) }
    }

    fun stationsOf(genre: Genre): List<Station> = stationsByGenre.value[genre].orEmpty()
}
