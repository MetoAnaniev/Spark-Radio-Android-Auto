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
        Station(
            id = "static_bg_radio", name = "BG Radio",
            streamUrl = "https://play.global.audio/bgradio128",
            genre = Genre.BULGARIAN, description = "Bulgarian music", country = "Bulgaria",
        ),
        Station(
            id = "static_energy_bg", name = "Energy NRJ Bulgaria",
            streamUrl = "https://play.global.audio/nrj128",
            genre = Genre.POP, description = "Hits and favorites", country = "Bulgaria",
        ),
        Station(
            id = "static_city_hiphop_bg", name = "City HipHop R&B",
            streamUrl = "https://play.global.audio/cityrabhi.aac",
            genre = Genre.POP, description = "Hip-hop and R&B", country = "Bulgaria",
        ),
        Station(
            id = "static_veronika_bg", name = "Radio Veronika",
            streamUrl = "https://play.global.audio/veronikahi.aac",
            genre = Genre.BULGARIAN, description = "Bulgarian and Balkan pop-folk", country = "Bulgaria",
        ),
        Station(
            id = "static_btv_radio", name = "bTV Radio",
            streamUrl = "https://cdn.bweb.bg/radio/btv-radio.mp3",
            genre = Genre.NEWS_TALK, description = "News and talk", country = "Bulgaria",
        ),
        Station(
            id = "static_veselina_bg", name = "Radio Veselina",
            streamUrl = "https://bss1.neterra.tv/veselina/veselina.m3u8",
            genre = Genre.BULGARIAN, description = "Bulgarian and Balkan music", country = "Bulgaria",
        ),
        Station(
            id = "static_bg_estrada", name = "BG Estrada",
            streamUrl = "https://play.global.audio/bgestrada",
            genre = Genre.BULGARIAN, description = "Bulgarian classics", country = "Bulgaria",
        ),
        Station(
            id = "static_energy_90s_bg", name = "Energy NRJ Bulgaria 90s Only",
            streamUrl = "https://play.global.audio/energy-90s",
            genre = Genre.POP, description = "Nonstop 90s hits", country = "Bulgaria",
        ),
        Station(
            id = "static_vanilla_bg", name = "Vanilla",
            streamUrl = "https://play.global.audio/vanillahi.aac",
            genre = Genre.CHILL, description = "Relaxed listening", country = "Bulgaria",
        ),
    )
}
