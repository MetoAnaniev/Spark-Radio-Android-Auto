package com.sparklab.radio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sparklab.radio.data.local.dao.FavoriteDao
import com.sparklab.radio.data.local.dao.UserStationDao
import com.sparklab.radio.data.local.entity.FavoriteEntity
import com.sparklab.radio.data.local.entity.UserStationEntity

/**
 * Single Room database holding user stations and favorite flags.
 * Bump [version] and add a migration when you change a schema.
 */
@Database(
    entities = [UserStationEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun userStationDao(): UserStationDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var instance: RadioDatabase? = null

        fun get(context: Context): RadioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "radiospark.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
