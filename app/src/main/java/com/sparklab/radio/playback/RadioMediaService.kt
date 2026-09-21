package com.sparklab.radio.playback

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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
import com.sparklab.radio.R
import com.sparklab.radio.RadioApp
import com.sparklab.radio.data.playback.toMediaItem
import com.sparklab.radio.data.playback.toStation
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.domain.model.Station
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
 *       └── Genres
 *             ├── Pop
 *             ├── Rock
 *             ├── …  (each contains its stations)
 *
 *  Playback from the car uses the same ExoPlayer queue that powers the phone,
 *  so next/previous skip between stations on BOTH surfaces.
 */
class RadioMediaService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaLibrarySession? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val repo by lazy { (application as RadioApp).container.stationRepository }

    override fun onCreate() {
        super.onCreate()

        // A dedicated OkHttp client keeps connections alive & handles ICY radio.
        val httpDataSourceFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory),
            )
            .setHandleAudioBecomingNoisy(true) // pause on headphone unplug
            .build()

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setId("radiospark_session")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

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

        /** Root of the car browse tree. */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ID_ROOT)
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
                    parentId == ID_ROOT -> rootCategories()
                    parentId == ID_FAVORITES -> repo.getAllOnce().filter { it.isFavorite }.map { it.toBrowsableMediaItem() }
                    parentId == ID_ALL -> repo.getAllOnce().map { it.toBrowsableMediaItem() }
                    parentId == ID_MY -> repo.getAllOnce().filter { it.isUserEditable }.map { it.toBrowsableMediaItem() }
                    parentId == ID_GENRES -> genres()
                    parentId.startsWith(PREFIX_GENRE) -> {
                        val genre = Genre.fromKey(parentId.removePrefix(PREFIX_GENRE))
                        repo.getAllOnce().filter { it.genre == genre }.map { it.toBrowsableMediaItem() }
                    }
                    else -> emptyList()
                }
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
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

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            // Android Auto sends us MediaItems with only a mediaId; resolve them
            // back into fully playable items (with stream URLs) from the repository.
            return scope.guavaFuture {
                mediaItems.map { item ->
                    val station = repo.getStation(item.mediaId)
                    station?.toMediaItem() ?: item
                }
            }
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
        category(ID_GENRES, getString(R.string.explore_title)),
    )

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

    private fun buildResumption(): MediaSession.MediaItemsWithStartPosition {
        val items = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        return MediaSession.MediaItemsWithStartPosition(items, player.currentMediaItemIndex, player.currentPosition)
    }
    companion object {
        const val ID_ROOT = "root"
        const val ID_FAVORITES = "favorites"
        const val ID_ALL = "all_stations"
        const val ID_MY = "my_stations"
        const val ID_GENRES = "genres"
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
