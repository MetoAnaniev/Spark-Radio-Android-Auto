package com.sparklab.radio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sparklab.radio.data.local.entity.FavoriteEntity
import com.sparklab.radio.data.local.entity.UserStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStationDao {
    @Query("SELECT * FROM user_stations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserStationEntity>>

    @Query("SELECT * FROM user_stations ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<UserStationEntity>

    @Query("SELECT * FROM user_stations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserStationEntity?

    @Upsert
    suspend fun upsert(entity: UserStationEntity)

    @Query("DELETE FROM user_stations WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE isFavorite = 1 ORDER BY sortOrder ASC, lastPlayedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getAllOnce(): List<FavoriteEntity>

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Query("UPDATE favorites SET lastPlayedAt = :ts WHERE stationId = :id")
    suspend fun markPlayed(id: String, ts: Long)

    @Query("SELECT * FROM favorites WHERE stationId = :id LIMIT 1")
    suspend fun getById(id: String): FavoriteEntity?

    @Query("UPDATE favorites SET sortOrder = :sortOrder WHERE stationId = :stationId")
    suspend fun updateSortOrder(stationId: String, sortOrder: Int)

    @Transaction
    suspend fun reorder(stationIds: List<String>) {
        stationIds.forEachIndexed { index, stationId ->
            updateSortOrder(stationId, index + 1)
        }
    }
}
