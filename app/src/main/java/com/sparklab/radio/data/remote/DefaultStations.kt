package com.sparklab.radio.data.remote

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station

/**
 * Small verified fallback catalog available even when the public directory is offline.
 * The main catalog comes from the free Radio Browser API.
 */
object DefaultStations {

    val all: List<Station> = listOf(
        Station(
            id = "static_radio_nova_bg",
            name = "Radio Nova",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIO_NOVAAAC_H.aac?dist=WEBSITEBG",
            genre = Genre.ELECTRONIC,
            description = "Bulgarian electronic radio · AAC high quality",
            country = "Bulgaria",
        ),
    )
}
