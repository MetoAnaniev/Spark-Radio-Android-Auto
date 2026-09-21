package com.sparklab.radio.domain.repository

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Read access to the radio catalog.
 *
 * The app ships three implementations, composed by [com.sparklab.radio.data.repository.StationRepository]:
 *  1. [com.sparklab.radio.data.repository.StaticStationSource]  — stations defined in code
 *  2. [com.sparklab.radio.data.repository.UserStationSource]    — user stations in Room
 *  3. [com.sparklab.radio.data.repository.RemoteStationSource]  — REST API (mock/real)
 *
 * To plug in a real API later, implement [RemoteStationSource] with Retrofit/Ktor/OkHttp
 * and swap it in the DI container. Nothing else in the app needs to change.
 */
interface StationSource {
    /** Emits the full list of stations for this source, reactively where possible. */
    fun observeStations(): Flow<List<Station>>

    /** One-shot fetch — used by the Android Auto browse tree and refresh actions. */
    suspend fun getStations(): List<Station>
}

/** Full-featured catalog repository used by the presentation & playback layers. */
interface StationRepository {

    /** All stations from every source, de-duplicated & favorites-merged. */
    fun observeAll(): Flow<List<Station>>

    /** Stations filtered by [genre]. */
    fun observeByGenre(genre: Genre): Flow<List<Station>>

    /** Stations the user marked as favorite, ordered by last played. */
    fun observeFavorites(): Flow<List<Station>>

    /** Stations the user created (editable). */
    fun observeUserStations(): Flow<List<Station>>

    suspend fun getStation(id: String): Station?

    suspend fun getAllOnce(): List<Station>

    /** Insert or update a user-created station. Returns the id. */
    suspend fun upsertUserStation(station: Station): String

    suspend fun deleteUserStation(id: String)

    /** Toggle favorite flag for any station (static, user or remote). */
    suspend fun setFavorite(station: Station, favorite: Boolean)

    /** Record that a station was played (updates ordering on the Favorites screen). */
    suspend fun markPlayed(id: String)
}
