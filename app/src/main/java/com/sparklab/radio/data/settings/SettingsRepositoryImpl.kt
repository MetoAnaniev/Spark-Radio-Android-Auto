package com.sparklab.radio.data.settings

import android.content.Context
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.model.StreamQuality
import com.sparklab.radio.domain.model.UserSettings
import com.sparklab.radio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Settings backed by SharedPreferences. A MutableStateFlow mirrors the values
 * so Compose can observe changes reactively (theme, quality, autostart, wifi-only).
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("radiospark_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    override val settings: Flow<UserSettings> = _state.asStateFlow()

    private fun read() = UserSettings(
        darkTheme = prefs.getBoolean(KEY_DARK, true),
        quality = runCatching { StreamQuality.valueOf(prefs.getString(KEY_QUALITY, "HIGH")!!) }
            .getOrDefault(StreamQuality.HIGH),
        autoStartLastStation = prefs.getBoolean(KEY_AUTOSTART, true),
        wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
    )

    private fun update(block: (UserSettings) -> UserSettings) {
        val next = block(_state.value)
        prefs.edit()
            .putBoolean(KEY_DARK, next.darkTheme)
            .putString(KEY_QUALITY, next.quality.name)
            .putBoolean(KEY_AUTOSTART, next.autoStartLastStation)
            .putBoolean(KEY_WIFI_ONLY, next.wifiOnly)
            .apply()
        _state.value = next
    }

    override suspend fun setDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }
    override suspend fun setQuality(quality: StreamQuality) = update { it.copy(quality = quality) }
    override suspend fun setAutoStartLastStation(enabled: Boolean) = update { it.copy(autoStartLastStation = enabled) }
    override suspend fun setWifiOnly(enabled: Boolean) = update { it.copy(wifiOnly = enabled) }

    override suspend fun setLastStation(station: Station) {
        val json = JSONObject().apply {
            put("id", station.id)
            put("name", station.name)
            put("streamUrl", station.streamUrl)
            put("logoUrl", station.logoUrl ?: JSONObject.NULL)
            put("genre", station.genre.key)
            put("description", station.description)
            put("country", station.country ?: JSONObject.NULL)
        }
        prefs.edit().putString(KEY_LAST_STATION, json.toString()).apply()
    }

    override suspend fun lastStation(): Station? = prefs.getString(KEY_LAST_STATION, null)?.let { json ->
        runCatching {
            val value = JSONObject(json)
            Station(
                id = value.getString("id"),
                name = value.getString("name"),
                streamUrl = value.getString("streamUrl"),
                logoUrl = value.optString("logoUrl").takeIf { it.isNotBlank() && it != "null" },
                genre = Genre.fromKey(value.optString("genre")),
                description = value.optString("description"),
                country = value.optString("country").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrNull()
    }

    override suspend fun current(): UserSettings = _state.value

    companion object {
        private const val KEY_DARK = "dark_theme"
        private const val KEY_QUALITY = "quality"
        private const val KEY_AUTOSTART = "autostart"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_LAST_STATION = "last_station"
    }
}
