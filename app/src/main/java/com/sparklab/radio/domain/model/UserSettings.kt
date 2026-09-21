package com.sparklab.radio.domain.model

/** User-configurable options persisted in DataStore/SharedPreferences. */
data class UserSettings(
    val darkTheme: Boolean = true,
    val quality: StreamQuality = StreamQuality.HIGH,
    val autoStartLastStation: Boolean = true,
    val wifiOnly: Boolean = false,
)

/** Streaming quality hint. Applied as a bitrate cap / preference where relevant. */
enum class StreamQuality(val label: String, val maxBitrateKbps: Int) {
    LOW("Low", 64),
    MEDIUM("Medium", 128),
    HIGH("High", 320),
}
