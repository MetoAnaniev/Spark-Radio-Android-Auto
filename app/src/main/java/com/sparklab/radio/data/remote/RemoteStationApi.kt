package com.sparklab.radio.data.remote

import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Read-only contract for a public radio directory.
 *
 * [RadioBrowser] uses the free, open-source Radio Browser API. No API key or
 * account is required. It loads a strong Bulgarian selection plus popular
 * international stations and retries against a second public mirror.
 */
interface RemoteStationApi {
    suspend fun getStations(genre: Genre? = null): List<Station>
    suspend fun createStation(station: Station): Station

    /** Offline fallback, useful in tests. */
    class Mock : RemoteStationApi {
        override suspend fun getStations(genre: Genre?): List<Station> =
            DefaultStations.all.filter { genre == null || it.genre == genre }
                .map { it.copy(source = Station.Source.REMOTE) }

        override suspend fun createStation(station: Station): Station = station
    }

    class RadioBrowser(
        private val mirrors: List<String> = listOf(
            "https://de1.api.radio-browser.info",
            "https://de2.api.radio-browser.info",
        ),
        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build(),
    ) : RemoteStationApi {

        @Volatile
        private var cachedStations: List<Station> = emptyList()

        @Volatile
        private var cacheUpdatedAt: Long = 0L

        override suspend fun getStations(genre: Genre?): List<Station> = withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = cachedStations
            val stations = if (cached.isNotEmpty() && now - cacheUpdatedAt < CACHE_DURATION_MS) {
                cached
            } else {
                var lastError: Throwable? = null
                var loaded: List<Station>? = null
                for (mirror in mirrors.shuffled()) {
                    try {
                        loaded = fetchCatalog(mirror)
                        break
                    } catch (error: Throwable) {
                        lastError = error
                    }
                }
                val result = loaded ?: throw lastError
                    ?: IllegalStateException("No Radio Browser API mirror is available")
                cachedStations = result
                cacheUpdatedAt = now
                result
            }
            if (genre == null) stations else stations.filter { it.genre == genre }
        }

        override suspend fun createStation(station: Station): Station =
            throw UnsupportedOperationException("Radio Browser is a read-only public directory")

        private fun fetchCatalog(mirror: String): List<Station> {
            val bulgarian = requestStations(mirror, countryCode = "BG", limit = BULGARIAN_LIMIT)
            val international = requestStations(mirror, countryCode = null, limit = INTERNATIONAL_LIMIT)

            return (bulgarian + international)
                .distinctBy { it.id }
                .filter { it.streamUrl.startsWith("http://") || it.streamUrl.startsWith("https://") }
        }

        private fun requestStations(mirror: String, countryCode: String?, limit: Int): List<Station> {
            val url = mirror.toHttpUrl().newBuilder()
                .addPathSegments("json/stations/search")
                .addQueryParameter("hidebroken", "true")
                .addQueryParameter("order", "clickcount")
                .addQueryParameter("reverse", "true")
                .addQueryParameter("limit", limit.toString())
                .apply {
                    if (countryCode != null) {
                        addQueryParameter("countrycode", countryCode)
                        addQueryParameter("countrycodeExact", "true")
                    }
                }
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Radio Browser HTTP ${response.code}")
                parseStations(response.body?.string().orEmpty())
            }
        }

        private fun parseStations(json: String): List<Station> {
            val array = JSONArray(json)
            return buildList {
                for (index in 0 until array.length()) {
                    parseStation(array.getJSONObject(index))?.let(::add)
                }
            }
        }

        private fun parseStation(value: JSONObject): Station? {
            val id = value.optString("stationuuid").trim()
            val name = value.optString("name").trim()
            val resolvedUrl = value.optString("url_resolved").trim()
            val fallbackUrl = value.optString("url").trim()
            val streamUrl = resolvedUrl.ifBlank { fallbackUrl }
            if (id.isBlank() || name.isBlank() || streamUrl.isBlank()) return null

            val countryCode = value.optString("countrycode").trim().uppercase(Locale.ROOT)
            val tags = value.optString("tags")
            val codec = value.optString("codec").trim()
            val bitrate = value.optInt("bitrate", 0)
            val details = listOfNotNull(
                tags.split(',').firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
                codec.takeIf { it.isNotBlank() },
                bitrate.takeIf { it > 0 }?.let { "$it kbps" },
            ).joinToString(" · ")

            return Station(
                id = "rb_$id",
                name = name,
                streamUrl = streamUrl,
                logoUrl = value.optString("favicon").trim().takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                },
                genre = genreFrom(tags, countryCode),
                description = details.ifBlank { "Live radio" },
                country = countryName(countryCode),
                source = Station.Source.REMOTE,
            )
        }

        private fun genreFrom(tags: String, countryCode: String): Genre {
            val normalized = tags.lowercase(Locale.ROOT)
            return when {
                "rock" in normalized || "metal" in normalized -> Genre.ROCK
                "jazz" in normalized || "blues" in normalized -> Genre.JAZZ
                "classical" in normalized || "classic" in normalized -> Genre.CLASSICAL
                "news" in normalized || "talk" in normalized -> Genre.NEWS_TALK
                "electronic" in normalized || "dance" in normalized || "techno" in normalized -> Genre.ELECTRONIC
                "chill" in normalized || "lounge" in normalized || "ambient" in normalized -> Genre.CHILL
                "kids" in normalized || "children" in normalized -> Genre.KIDS
                "pop" in normalized || "hits" in normalized -> Genre.POP
                countryCode == "BG" -> Genre.BULGARIAN
                countryCode == "GR" -> Genre.GREEK
                else -> Genre.OTHER
            }
        }

        private fun countryName(countryCode: String): String {
            if (countryCode.length != 2) return "International"
            return runCatching {
                Locale.Builder().setRegion(countryCode).build().getDisplayCountry(Locale.ENGLISH)
            }.getOrDefault(countryCode).ifBlank { countryCode }
        }

        private companion object {
            const val USER_AGENT = "RadioSpark/1.1 (Android; contact: SparkLab Academy)"
            const val BULGARIAN_LIMIT = 100
            const val INTERNATIONAL_LIMIT = 200
            const val CACHE_DURATION_MS = 15 * 60 * 1000L
        }
    }
}
