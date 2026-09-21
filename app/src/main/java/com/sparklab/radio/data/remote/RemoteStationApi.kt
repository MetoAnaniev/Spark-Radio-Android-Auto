package com.sparklab.radio.data.remote

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * REST contract for a future radio directory service.
 *
 * GET /stations            -> all stations
 * GET /stations?genre=jazz -> stations of one genre
 * POST /stations           -> create a station
 *
 * Two implementations ship today:
 *  - [RemoteStationApi.Mock] : returns local sample data (works offline).
 *  - [RemoteStationApi.Http] : real HTTP call; parse the JSON in one place.
 *
 * Plug the real one in by changing DI (see AppContainer).
 */
interface RemoteStationApi {
    suspend fun getStations(genre: Genre? = null): List<Station>
    suspend fun createStation(station: Station): Station

    /** Offline-friendly default used by the app until a real endpoint is configured. */
    class Mock : RemoteStationApi {
        override suspend fun getStations(genre: Genre?): List<Station> =
            DefaultStations.all.filter { genre == null || it.genre == genre }
                .map { it.copy(source = Station.Source.REMOTE) }

        override suspend fun createStation(station: Station): Station = station
    }

    /**
     * Real HTTP client. The response schema is intentionally simple JSON:
     * [ { "id": "...", "name": "...", "streamUrl": "...", "genre": "rock",
     *     "description": "...", "logoUrl": "...", "country": "..." } ]
     */
    class Http(
        private val baseUrl: String,
        private val client: OkHttpClient = OkHttpClient(),
    ) : RemoteStationApi {

        override suspend fun getStations(genre: Genre?): List<Station> = withContext(Dispatchers.IO) {
            val url = buildString {
                append(baseUrl.trimEnd('/')).append("/stations")
                if (genre != null) append("?genre=").append(genre.key)
            }
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            response.use {
                if (!it.isSuccessful) error("HTTP ${it.code}")
                parseStations(it.body?.string().orEmpty())
            }
        }

        override suspend fun createStation(station: Station): Station = withContext(Dispatchers.IO) {
            // POST /stations with the station JSON. Kept minimal; extend as needed.
            val body = station.toJson().toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/stations")
                .post(body)
                .build()
            client.newCall(request).execute().use {
                if (!it.isSuccessful) error("HTTP ${it.code}")
                it.body?.string()?.let(::parseStation) ?: station
            }
        }

        private fun parseStations(json: String): List<Station> {
            val array = JSONArray(json)
            return (0 until array.length()).map { parseStation(array.getJSONObject(it).toString()) }
        }

        private fun parseStation(json: String): Station {
            val o = JSONObject(json)
            return Station(
                id = o.optString("id"),
                name = o.optString("name"),
                streamUrl = o.optString("streamUrl"),
                logoUrl = o.optString("logoUrl").ifBlank { null },
                genre = Genre.fromKey(o.optString("genre")),
                description = o.optString("description"),
                country = o.optString("country").ifBlank { null },
                source = Station.Source.REMOTE,
            )
        }
    }
}

/** Small helpers so we don't need the kotlinx-serialization dependency. */
private fun Station.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("streamUrl", streamUrl)
    put("logoUrl", logoUrl ?: JSONObject.NULL)
    put("genre", genre.key)
    put("description", description)
    put("country", country ?: JSONObject.NULL)
}
