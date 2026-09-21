package com.sparklab.radio.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sparklab.radio.RadioApp
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.model.StreamQuality
import com.sparklab.radio.domain.model.UserSettings
import com.sparklab.radio.domain.usecase.ValidateStreamUrl

/** Form state + validation for adding/editing a custom station. */
data class StationFormState(
    val id: String = "",
    val name: String = "",
    val streamUrl: String = "",
    val logoUrl: String = "",
    val genre: Genre = Genre.POP,
    val country: String = "",
    val description: String = "",
    val urlError: String? = null,
    val nameError: String? = null,
) {
    val isValid: Boolean get() = name.isNotBlank() && ValidateStreamUrl(streamUrl) == null

    fun toStation() = Station(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl.ifBlank { null },
        genre = genre,
        description = description,
        country = country.ifBlank { null },
        source = Station.Source.USER,
    )

    companion object {
        fun from(station: Station) = StationFormState(
            id = station.id,
            name = station.name,
            streamUrl = station.streamUrl,
            logoUrl = station.logoUrl.orEmpty(),
            genre = station.genre,
            country = station.country.orEmpty(),
            description = station.description,
        )
    }
}

/** Factory used by the edit screen; kept tiny so screens stay declarative. */
object StationFormFactory {
    fun validation(state: StationFormState): StationFormState = state.copy(
        nameError = if (state.name.isBlank()) "Station name is required" else null,
        urlError = ValidateStreamUrl(state.streamUrl),
    )
}

/** Convenience: pick a quality label list for the Settings screen. */
val qualityOptions: List<StreamQuality> = StreamQuality.entries

/** Default settings used before the repository emits. */
val defaultSettings = UserSettings()

/** Provide the settings/repo to a simple ViewModel when needed. */
fun radioAppFactory(app: RadioApp) = viewModelFactory {
    initializer { RadioViewModel(app) }
}
