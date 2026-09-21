package com.sparklab.radio.data.repository

import com.sparklab.radio.data.local.RadioDatabase
import com.sparklab.radio.data.local.entity.FavoriteEntity
import com.sparklab.radio.data.local.entity.UserStationEntity
import com.sparklab.radio.data.remote.DefaultStations
import com.sparklab.radio.data.remote.RemoteStationApi
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.repository.StationRepository
import com.sparklab.radio.domain.repository.StationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

/* ============================================================================
 * SOURCE 1 — Static catalog (defined in code, see DefaultStations)
 * ========================================================================== */
class StaticStationSource : StationSource {
    override fun observeStations(): Flow<List<Station>> = flowOf(DefaultStations.all)

    override suspend fun getStations(): List<Station> = DefaultStations.all
}

/* ============================================================================
 * SOURCE 2 — User stations (Room database)
 * ========================================================================== */
class UserStationSource(private val db: RadioDatabase) : StationSource {

    override fun observeStations(): Flow<List<Station>> =
        db.userStationDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getStations(): List<Station> =
        db.userStationDao().getAllOnce().map { it.toDomain() }

    private fun UserStationEntity.toDomain() = Station(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        genre = Genre.fromKey(genreKey),
        description = description,
        country = country,
        source = Station.Source.USER,
    )
}

/* ============================================================================
 * SOURCE 3 — Live Radio Browser public API with an offline-safe empty state.
 * ========================================================================== */
class RemoteStationSource(private val api: RemoteStationApi) : StationSource {
    override fun observeStations(): Flow<List<Station>> =
        flow {
            emit(emptyList())
            emit(api.getStations())
        }.catch { emit(emptyList()) }

    override suspend fun getStations(): List<Station> = api.getStations()
}

/* ============================================================================
 * Composed repository — merges every source and overlays favorites/usage.
 * ========================================================================== */
class StationRepositoryImpl(
    private val staticSource: StationSource,
    private val userSource: StationSource,
    private val remoteSource: StationSource,
    private val db: RadioDatabase,
) : StationRepository {

    /** All stations = static + user + remote, favorites merged in. */
    override fun observeAll(): Flow<List<Station>> =
        combine(
            staticSource.observeStations(),
            userSource.observeStations(),
            remoteSource.observeStations(),
            db.favoriteDao().observeAll(),
        ) { static, user, remote, favorites ->
            merge(user + remote + static, favorites)
        }

    override fun observeByGenre(genre: Genre): Flow<List<Station>> =
        observeAll().map { all -> all.filter { it.genre == genre } }

    override fun observeFavorites(): Flow<List<Station>> =
        combine(observeAll(), db.favoriteDao().observeFavorites()) { all, favs ->
            val ids = favs.map { it.stationId }.toSet()
            all.filter { it.id in ids }
                .sortedWith(
                    compareByDescending<Station> { it.sortOrder == 0 }
                        .thenBy { it.sortOrder }
                        .thenByDescending { it.lastPlayedAt },
                )
        }

    override fun observeUserStations(): Flow<List<Station>> = userSource.observeStations()

    override suspend fun getStation(id: String): Station? =
        getAllOnce().firstOrNull { it.id == id }

    override suspend fun getAllOnce(): List<Station> {
        val static = staticSource.getStations()
        val user = userSource.getStations()
        val remote = runCatching { remoteSource.getStations() }.getOrDefault(emptyList())
        val favorites = db.favoriteDao().getAllOnce()
        return merge(user + remote + static, favorites)
    }

    private fun merge(stations: List<Station>, favorites: List<FavoriteEntity>): List<Station> {
        val byId = favorites.associateBy { it.stationId }
        return stations
            .distinctBy { "${it.country.orEmpty().trim().lowercase()}|${it.name.trim().lowercase()}" }
            .map { station ->
                val favorite = byId[station.id]
                station.copy(
                    isFavorite = favorite?.isFavorite == true,
                    lastPlayedAt = favorite?.lastPlayedAt ?: 0L,
                    sortOrder = favorite?.sortOrder ?: 0,
                )
            }
            .sortedWith(
                compareBy<Station> { countrySortKey(it.country) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )
    }

    private fun countrySortKey(country: String?): String = when {
        country.equals("Bulgaria", ignoreCase = true) -> "0_bulgaria"
        country.isNullOrBlank() || country.equals("International", ignoreCase = true) -> "2_international"
        else -> "1_${country.lowercase()}"
    }

    override suspend fun upsertUserStation(station: Station): String {
        val id = station.id.ifBlank { "user_" + UUID.randomUUID().toString() }
        db.userStationDao().upsert(
            UserStationEntity(
                id = id,
                name = station.name.trim(),
                streamUrl = station.streamUrl.trim(),
                logoUrl = station.logoUrl?.trim()?.ifBlank { null },
                genreKey = station.genre.key,
                description = station.description.trim(),
                country = station.country?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    override suspend fun deleteUserStation(id: String) {
        db.userStationDao().deleteById(id)
        // also drop any favorite metadata for that station
        db.favoriteDao().upsert(FavoriteEntity(stationId = id, isFavorite = false))
    }

    override suspend fun setFavorite(station: Station, favorite: Boolean) {
        val existing = db.favoriteDao().getById(station.id)
        db.favoriteDao().upsert(
            FavoriteEntity(
                stationId = station.id,
                isFavorite = favorite,
                lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                sortOrder = existing?.sortOrder ?: 0,
            ),
        )
    }

    override suspend fun markPlayed(id: String) {
        // ensure a row exists then stamp the time
        val existing = db.favoriteDao().getById(id)
        if (existing == null) {
            db.favoriteDao().upsert(FavoriteEntity(stationId = id, lastPlayedAt = System.currentTimeMillis()))
        } else {
            db.favoriteDao().markPlayed(id, System.currentTimeMillis())
        }
    }
}
