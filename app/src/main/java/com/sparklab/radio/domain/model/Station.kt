package com.sparklab.radio.domain.model

/**
 * A broadcast radio [Station] — the single core model used across the whole app.
 *
 * A station can come from three sources (see [Source]):
 *  - the built-in static catalog defined in code,
 *  - the user's own stations persisted in the Room database,
 *  - (optionally) a remote REST API once you plug one in.
 *
 * @param id            Stable unique id. For static stations this is a slug like
 *                      `static_rock_classics`; for user stations it is a UUID.
 * @param name          Human readable station name, shown big & bold.
 * @param streamUrl     The audio url. May be http/https (Icecast/Shoutcast),
 *                      or a playlist file (.m3u / .pls). Media3 resolves playlists.
 * @param logoUrl       Optional artwork/logo url. Falls back to a generated tile.
 * @param genre         Primary [Genre] used for grouping & Explore.
 * @param description   Short one-liner about the station (secondary text).
 * @param country       Optional language / country label, e.g. "Bulgaria".
 * @param isFavorite    Whether the user marked this station as a favorite.
 * @param source        Where this record originates from.
 * @param lastPlayedAt  Epoch millis of the last time it was played (0 = never).
 *                      Used to order the Favorites screen.
 * @param sortOrder     Manual ordering key for the Favorites screen.
 */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val genre: Genre = Genre.OTHER,
    val description: String = "",
    val country: String? = null,
    val isFavorite: Boolean = false,
    val source: Source = Source.STATIC,
    val lastPlayedAt: Long = 0L,
    val sortOrder: Int = 0,
) {
    /** True when this station can be edited/deleted by the user (only user-added ones). */
    val isUserEditable: Boolean get() = source == Source.USER

    enum class Source { STATIC, USER, REMOTE }
}
