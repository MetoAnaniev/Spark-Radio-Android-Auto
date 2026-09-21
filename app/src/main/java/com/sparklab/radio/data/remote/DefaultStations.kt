package com.sparklab.radio.data.remote

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station

/**
 * Default station catalog, defined in code.
 *
 * -------------------------------------------------------------------------
 * HOW TO ADD A NEW DEFAULT STATION
 * -------------------------------------------------------------------------
 * 1. Append a [Station] below with a unique `id` (prefix `static_`).
 * 2. Set `genre` to one of the [Genre] values so it appears under Explore.
 * 3. Replace `streamUrl` with a real Icecast/Shoutcast/HTTP(S) stream.
 *    `.m3u` / `.m3u8` / `.pls` playlists are supported by Media3.
 * 4. Optionally add a `logoUrl` (https image) — otherwise a styled tile is drawn.
 *
 * The URLs below are PLACEHOLDERS intended to be replaced with your own
 * licensed streams — see README → "Adding stations".
 * -------------------------------------------------------------------------
 */
object DefaultStations {

    val all: List<Station> = listOf(
        Station(
            id = "static_global_pop",
            name = "Global Pop Hits",
            streamUrl = "https://stream.example.com/global-pop",
            genre = Genre.POP,
            description = "Today's biggest pop tracks, non-stop.",
            country = "International",
        ),
        Station(
            id = "static_rock_classics",
            name = "Rock Classics 24/7",
            streamUrl = "https://stream.example.com/rock-classics",
            genre = Genre.ROCK,
            description = "Legendary rock anthems from the 70s to today.",
            country = "International",
        ),
        Station(
            id = "static_smooth_jazz",
            name = "Smooth Jazz Lounge",
            streamUrl = "https://stream.example.com/smooth-jazz",
            genre = Genre.JAZZ,
            description = "Relaxing saxophones and cool grooves.",
            country = "International",
        ),
        Station(
            id = "static_classical_focus",
            name = "Classical Focus",
            streamUrl = "https://stream.example.com/classical-focus",
            genre = Genre.CLASSICAL,
            description = "Timeless orchestral works for deep concentration.",
            country = "International",
        ),
        Station(
            id = "static_news_talk",
            name = "News & Talk Live",
            streamUrl = "https://stream.example.com/news-talk",
            genre = Genre.NEWS_TALK,
            description = "Headlines, interviews and live discussion.",
            country = "International",
        ),
        Station(
            id = "static_electronic_beats",
            name = "Electronic Beats Radio",
            streamUrl = "https://stream.example.com/electronic-beats",
            genre = Genre.ELECTRONIC,
            description = "House, techno and future bass around the clock.",
            country = "International",
        ),
        Station(
            id = "static_chill_lofi",
            name = "Chill & LoFi",
            streamUrl = "https://stream.example.com/chill-lofi",
            genre = Genre.CHILL,
            description = "Lo-fi beats and ambient textures to unwind.",
            country = "International",
        ),
        Station(
            id = "static_bulgarian_hits",
            name = "Bulgarian Hits Radio",
            streamUrl = "https://stream.example.com/bulgarian-hits",
            genre = Genre.BULGARIAN,
            description = "Най-добрите български хитове, денонощно.",
            country = "Bulgaria",
        ),
        Station(
            id = "static_greek_summer",
            name = "Greek Summer Radio",
            streamUrl = "https://stream.example.com/greek-summer",
            genre = Genre.GREEK,
            description = "Sunny Mediterranean hits and timeless Greek songs.",
            country = "Greece",
        ),
        Station(
            id = "static_kids_fun",
            name = "Kids Fun Radio",
            streamUrl = "https://stream.example.com/kids-fun",
            genre = Genre.KIDS,
            description = "Songs, stories and learning fun for children.",
            country = "International",
        ),
    )
}
