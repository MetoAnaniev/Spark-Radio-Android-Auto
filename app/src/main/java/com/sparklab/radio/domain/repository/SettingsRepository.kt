package com.sparklab.radio.domain.repository

import com.sparklab.radio.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Persisted user preferences (theme, quality, autostart, metered-network policy). */
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setQuality(quality: com.sparklab.radio.domain.model.StreamQuality)
    suspend fun setAutoStartLastStation(enabled: Boolean)
    suspend fun setWifiOnly(enabled: Boolean)
    suspend fun current(): UserSettings
}
