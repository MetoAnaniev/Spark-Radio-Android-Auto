package com.sparklab.radio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favorite / usage metadata for ANY station id (static, user or remote).
 * Kept in its own table so a station can be favorited regardless of source.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val stationId: String,
    val isFavorite: Boolean = true,
    /** Epoch millis of last playback; used to order the Favorites list. */
    val lastPlayedAt: Long = 0L,
    /** Manual sort key (lower first). */
    val sortOrder: Int = 0,
)
