package com.sparklab.radio.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Timeline
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.sparklab.radio.MainActivity
import com.sparklab.radio.R
import com.sparklab.radio.RadioApp
import com.sparklab.radio.data.playback.toMediaItem
import com.sparklab.radio.data.playback.toStation
import com.sparklab.radio.data.remote.DefaultStations
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.usecase.stationSwitchQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope as KCScope
import okhttp3.OkHttpClient

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  RadioMediaService — Android Auto + phone playback in one service.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  MediaLibraryService is the modern Media3 replacement for the classic
 *  MediaBrowserService. It gives us:
 *    • a MediaSession → notification, lock-screen and car playback controls
 *    • onGetLibraryRoot / onGetChildren → the Android Auto browse templates
 *    • ExoPlayer playing our HTTP / Icecast / HLS radio streams
 *
 *  Browse tree exposed to the car:
 *
 *      ROOT
 *       ├── Favorites
 *       ├── All Stations
     *       ├── My Stations            (user added)
     *       ├── Countries
     *       │     ├── Bulgaria
     *       │     └── …  (alphabetical after Bulgaria)
     *       └── Genres
     *             ├── Pop
     *             └── …  (each contains its stations)
 *
 *  Playback from the car uses the same ExoPlayer queue that powers the phone,
 *  so next/previous skip between stations on BOTH surfaces.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class RadioMediaService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaLibrarySession? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isCarAutoStartLoading = false
    private val browseQueues = mutableMapOf<String, List<MediaItem>>()

    private val container by lazy { (application as RadioApp).container }
    private val repo by lazy { container.stationRepository }
    private val settingsRepo by lazy { container.settingsRepository }

    override fun onCreate() {
        super.onCreate()

        // A dedicated OkHttp client keeps connections alive & handles ICY radio.
        val httpDataSourceFactory = OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build(),
        ).setUserAgent("RadioSpark/1.6 Android Auto")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory),
            )
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true) // pause on headphone unplug
            .build()
        // Keep station switching available at both ends of the queue.
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val queue = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toStation() }
                if (queue.isNotEmpty()) scope.launch { settingsRepo.setPlaybackQueue(queue) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.takeIf { it.mediaId.isNotBlank() }?.let { item ->
                    scope.launch {
                        // Save first: Room/catalog work must not delay the resume snapshot.
                        settingsRepo.setLastStation(item.toStation())
                        repo.markPlayed(item.mediaId)
                    }
                }
            }
        })

        val stationPlayer = object : ForwardingPlayer(player) {
            // Generic Previous can seek to the live edge instead of changing station.
            override fun seekToPrevious() = player.seekToPreviousMediaItem()
            override fun seekToNext() = player.seekToNextMediaItem()
        }
        mediaSession = MediaLibrarySession.Builder(this, stationPlayer, LibraryCallback())
            .setBitmapLoader(androidx.media3.session.CacheBitmapLoader(StationArtworkLoader(this, scope)))
            .setSessionActivity(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            .setId("radiospark_session")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    private fun isCarController(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return normalized.contains("gearhead") ||
            normalized.contains("projection") ||
            normalized.contains("automotive") ||
            normalized.contains("android.car") ||
            normalized.contains("car.media")
    }

    private fun requestCarAutoStart() {
        if (isCarAutoStartLoading || player.isPlaying) return
        isCarAutoStartLoading = true
        scope.launch {
            try {
                if (!settingsRepo.current().autoStartLastStation) return@launch
                val resume = buildResumption()
                // A controller may have selected a station while restoration was loading.
                if (player.playWhenReady &&
                    (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY)
                ) return@launch
                if (player.mediaItemCount == 0 && resume.mediaItems.isNotEmpty()) {
                    player.setMediaItems(resume.mediaItems, resume.startIndex, C.TIME_UNSET)
                }
                if (player.mediaItemCount > 0) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("RadioMediaService", "Car playback resumption failed", error)
            } finally {
                isCarAutoStartLoading = false
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing when the user swipes the app away only if something is playing;
        // otherwise stop the service to save resources.
        if (player.playWhenReady && player.mediaItemCount > 0) return
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /* ══════════════════════════════════════════════════════════════════
     *  Browse tree + playback callbacks
     * ════════════════════════════════════════════════════════════════ */
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            super.onPostConnect(session, controller)
            if (isCarController(controller.packageName)) requestCarAutoStart()
        }

        /** Root of the car browse tree. */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Legacy MediaBrowser clients (including Auto) may never call onPostConnect.
            // Recent-media discovery must remain silent; only a car connection autoplays.
            if (params?.isRecent != true && isCarController(browser.packageName)) {
                requestCarAutoStart()
            }
            val root = MediaItem.Builder()
                .setMediaId(if (params?.isRecent == true) ID_RECENT else ID_ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        /** Children of any browsable node. */
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return scope.guavaFuture {
                val children: List<MediaItem> = when {
                    parentId == ID_RECENT -> listOfNotNull(settingsRepo.lastStation()?.toMediaItem())
                    parentId == ID_ROOT -> rootCategories()
                    parentId == ID_FAVORITES -> repo.getAllOnce().filter { it.isFavorite }.map { it.toBrowsableMediaItem() }
                    parentId == ID_ALL -> repo.getAllOnce().map { it.toBrowsableMediaItem() }
                    parentId == ID_MY -> repo.getAllOnce().filter { it.isUserEditable }.map { it.toBrowsableMediaItem() }
                    parentId == ID_COUNTRIES -> countries()
                    parentId.startsWith(PREFIX_COUNTRY) -> {
                        val country = parentId.removePrefix(PREFIX_COUNTRY)
                        repo.getAllOnce()
                            .filter { (it.country ?: "International").equals(country, ignoreCase = true) }
                            .map { it.toBrowsableMediaItem() }
                    }
                    parentId == ID_GENRES -> genres()
                    parentId.startsWith(PREFIX_GENRE) -> {
                        val genre = Genre.fromKey(parentId.removePrefix(PREFIX_GENRE))
                        repo.getAllOnce().filter { it.genre == genre }.map { it.toBrowsableMediaItem() }
                    }
                    else -> emptyList()
                }
                if (parentId != ID_RECENT && children.any { it.mediaMetadata.isPlayable == true }) {
                    children.forEach { browseQueues[it.mediaId] = children }
                }
                val start = (page.toLong().coerceAtLeast(0) * pageSize.coerceAtLeast(0))
                    .coerceAtMost(children.size.toLong()).toInt()
                val end = (start.toLong() + pageSize.coerceAtLeast(0))
                    .coerceAtMost(children.size.toLong()).toInt()
                LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(start, end)), params)
            }
        }

        /**
         * Called when the user chooses an item in the car.
         * For playable items we build a play queue (so next/previous work) and start playback.
         * Compose sends a "play station" command carrying the station id.
         */
        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return scope.guavaFuture { buildResumption() }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.guavaFuture {
            val item = settingsRepo.lastStation()?.takeIf { it.id == mediaId }?.toMediaItem()
                ?: browseQueues[mediaId]?.firstOrNull { it.mediaId == mediaId }
                ?: repo.getStation(mediaId)?.toMediaItem()
            if (item != null) LibraryResult.ofItem(item, null)
            else LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = scope.guavaFuture {
            resolvePlayableItems(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.guavaFuture {
            // Car hosts commonly submit browse results containing only mediaId.
            // Resolve every item to a stream URI before Media3 prepares playback.
            val playableItems = resolvePlayableItems(mediaItems)
            val selectedId = mediaItems.getOrNull(startIndex.takeIf { it >= 0 } ?: 0)?.mediaId
            val queue = if (playableItems.size == 1) {
                val selected = playableItems.first()
                browseQueues[selected.mediaId]
                    ?: stationSwitchQueue(selected.toStation(), settingsRepo.playbackQueue(), DefaultStations.all)
                        .map { it.toMediaItem() }
            } else playableItems
            MediaSession.MediaItemsWithStartPosition(
                queue,
                queue.indexOfFirst { it.mediaId == selectedId }.coerceAtLeast(0),
                C.TIME_UNSET,
            )
        }

        private suspend fun resolvePlayableItems(mediaItems: List<MediaItem>): List<MediaItem> =
            mediaItems.mapNotNull { item ->
                item.takeIf { it.localConfiguration?.uri != null }
                    ?: browseQueues[item.mediaId]?.firstOrNull { it.mediaId == item.mediaId }
                    ?: settingsRepo.lastStation()?.takeIf { it.id == item.mediaId }?.toMediaItem()
                    ?: repo.getStation(item.mediaId)?.toMediaItem()
            }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle,
        ): ListenableFuture<SessionResult> {
            // Phone UI uses this to hand over a whole queue for next/previous.
            if (customCommand.customAction == ACTION_SET_QUEUE) {
                val ids = args.getStringArrayList(KEY_IDS).orEmpty()
                scope.launch {
                    val stations = ids.mapNotNull { repo.getStation(it) }
                    val items = stations.map { it.toMediaItem() }
                    val index = args.getInt(KEY_INDEX, 0)
                    player.setMediaItems(items, index.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0)
                    player.prepare()
                    player.play()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    /* ────────────────────────── browse helpers ───────────────────────── */

    private fun rootCategories(): List<MediaItem> = listOf(
        category(ID_FAVORITES, getString(R.string.favorites_title)),
        category(ID_ALL, getString(R.string.stations_title)),
        category(ID_MY, "My Stations"),
        category(ID_COUNTRIES, "Countries"),
        category(ID_GENRES, getString(R.string.explore_title)),
    )

    private suspend fun countries(): List<MediaItem> = repo.getAllOnce()
        .map { it.country ?: "International" }
        .distinct()
        .map { country -> category(PREFIX_COUNTRY + country, country) }

    private fun genres(): List<MediaItem> = Genre.entries.map { genre ->
        category(PREFIX_GENRE + genre.key, genre.label)
    }

    private fun category(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()

    /** A station node in the car. Browsable=false, playable=true. */
    private fun Station.toBrowsableMediaItem(): MediaItem = toMediaItem()

    private suspend fun buildResumption(): MediaSession.MediaItemsWithStartPosition {
        val current = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        if (current.isNotEmpty()) {
            return MediaSession.MediaItemsWithStartPosition(current, player.currentMediaItemIndex, C.TIME_UNSET)
        }
        // Restore locally, even after process death, with no directory/network dependency.
        val saved = settingsRepo.playbackQueue()
        val last = settingsRepo.lastStation() ?: saved.firstOrNull() ?: DefaultStations.all.first()
        val queue = stationSwitchQueue(last, saved, DefaultStations.all).map { it.toMediaItem() }
        return MediaSession.MediaItemsWithStartPosition(
            queue, queue.indexOfFirst { it.mediaId == last.id }.coerceAtLeast(0), C.TIME_UNSET,
        )
    }
    companion object {
        const val ID_ROOT = "root"
        const val ID_RECENT = "recent"
        const val ID_FAVORITES = "favorites"
        const val ID_ALL = "all_stations"
        const val ID_MY = "my_stations"
        const val ID_COUNTRIES = "countries"
        const val ID_GENRES = "genres"
        const val PREFIX_COUNTRY = "country_"
        const val PREFIX_GENRE = "genre_"

        /** Custom command used by the phone UI to set a play queue. */
        const val ACTION_SET_QUEUE = "com.sparklab.radio.SET_QUEUE"
        const val KEY_IDS = "ids"
        const val KEY_INDEX = "index"
    }
}

/**
 * Run a suspending block and expose the result as a Guava [ListenableFuture],
 * which the Media3 session callback API expects. The block runs on the scope's
 * dispatcher (Main for this service) and any exception is propagated to the future.
 */
private fun <T> KCScope.guavaFuture(block: suspend KCScope.() -> T): ListenableFuture<T> {
    val settable = SettableFuture.create<T>()
    launch {
        try {
            settable.set(block())
        } catch (t: Throwable) {
            settable.setException(t)
        }
    }
    return settable
}
