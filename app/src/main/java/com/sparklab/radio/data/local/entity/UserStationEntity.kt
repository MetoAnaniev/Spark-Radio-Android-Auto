package com.sparklab.radio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A station the user added manually. Persisted in Room so it survives restarts.
 * Static (built-in) stations are NOT stored here — they live in code.
 */
@Entity(tableName = "user_stations")
data class UserStationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val genreKey: String,
    val description: String,
    val country: String?,
    val createdAt: Long,
)
