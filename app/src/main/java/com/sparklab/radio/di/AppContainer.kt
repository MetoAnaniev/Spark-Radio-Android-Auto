package com.sparklab.radio.di

import android.content.Context
import com.sparklab.radio.data.local.RadioDatabase
import com.sparklab.radio.data.playback.MediaControllerPlaybackRepository
import com.sparklab.radio.data.remote.RemoteStationApi
import com.sparklab.radio.data.repository.RemoteStationSource
import com.sparklab.radio.data.repository.StaticStationSource
import com.sparklab.radio.data.repository.StationRepositoryImpl
import com.sparklab.radio.data.repository.UserStationSource
import com.sparklab.radio.data.settings.SettingsRepositoryImpl
import com.sparklab.radio.domain.repository.PlaybackRepository
import com.sparklab.radio.domain.repository.SettingsRepository
import com.sparklab.radio.domain.repository.StationRepository
import com.sparklab.radio.domain.usecase.GetStationsByGenre
import com.sparklab.radio.domain.usecase.ToggleFavorite

/**
 * Tiny hand-rolled DI container (no Hilt needed for an app this size).
 * Everything is a lazy singleton created from the Application context.
 *
 * ── To connect a real API ──────────────────────────────────────────────
 * Replace `RemoteStationApi.Mock()` below with:
 *     RemoteStationApi.Http("https://your.api.example.com")
 * and the whole app (UI + Android Auto) starts consuming remote stations.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val db: RadioDatabase = RadioDatabase.get(appContext)

    val stationRepository: StationRepository by lazy {
        StationRepositoryImpl(
            staticSource = StaticStationSource(),
            userSource = UserStationSource(db),
            remoteSource = RemoteStationSource(RemoteStationApi.Mock()),
            db = db,
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }

    /** UI-side playback bridge (MediaController -> RadioMediaService). */
    val playbackRepository: PlaybackRepository by lazy {
        MediaControllerPlaybackRepository(appContext)
    }

    val getStationsByGenre by lazy { GetStationsByGenre(stationRepository) }
    val toggleFavorite by lazy { ToggleFavorite(stationRepository) }
}
