package com.sparklab.radio.domain.usecase

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.repository.StationRepository

/**
 * Small, testable use-cases that encapsulate app rules.
 * They keep ViewModels thin and make the business logic reusable (e.g. from Auto).
 */

/** Returns stations grouped by genre for the Explore grid. */
class GetStationsByGenre(private val repo: StationRepository) {
    operator fun invoke(genre: Genre) = repo.observeByGenre(genre)
}

/** Validates a user-supplied stream URL before saving. */
object ValidateStreamUrl {
    private val allowed = listOf(".m3u", ".m3u8", ".pls", ".mp3", ".aac", ".ogg", ".opus")

    /** Returns null when valid, or a human-readable error message. */
    operator fun invoke(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return "Stream URL is required"
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "URL must start with http:// or https://"
        }
        // Playlists and direct streams are all fine; servers without a file
        // extension (e.g. icecast /stream) are valid too, so we only warn
        // about obviously malformed hosts.
        val host = lower.removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (host.isBlank() || !host.contains('.')) return "Enter a valid host, e.g. stream.example.com"
        return null
    }

    /** True when the url points at a playlist file Media3 will expand. */
    fun isPlaylist(url: String): Boolean {
        val path = url.lowercase().substringBefore('?')
        return path.endsWith(".m3u") || path.endsWith(".m3u8") || path.endsWith(".pls")
    }
}

/** Toggles favorite on a station. */
class ToggleFavorite(private val repo: StationRepository) {
    suspend operator fun invoke(station: Station) =
        repo.setFavorite(station, !station.isFavorite)
}
