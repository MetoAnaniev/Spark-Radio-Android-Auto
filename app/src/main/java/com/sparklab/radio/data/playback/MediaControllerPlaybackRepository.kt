package com.sparklab.radio.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sparklab.radio.domain.model.PlaybackState
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.repository.PlaybackRepository
import com.sparklab.radio.playback.RadioMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges the UI to the Media3 [RadioMediaService] through a [MediaController].
 *
 * Responsibilities:
 *  - connect/disconnect the controller,
 *  - expose a [PlaybackState] flow for the Now Playing screen & mini-player,
 *  - translate Station objects into MediaItems (with metadata for Android Auto),
 *  - handle play/pause/next/previous and buffering/error states.
 *
 * The car UI talks to the SAME on-device session through the service's
 * MediaLibraryService + MediaSession, so both stay in sync automatically.
 */
class MediaControllerPlaybackRepository(
    private val context: Context,
) : PlaybackRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state = _state.asStateFlow()

    /** Call once from the Activity/Application when a UI session starts. */
    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, RadioMediaService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            scope.launch {
                val c = withContext(Dispatchers.Main) { future.await() }
                controller = c
                observeController(c)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun observeController(c: MediaController) {
        // Mirror Media3 state into our domain PlaybackState.
        scope.launch {
            c.isPlaying // touch to ensure listener already registered by builder
            while (true) {
                val mediaItem = c.currentMediaItem
                val station = mediaItem?.toStation()
                _state.value = PlaybackState(
                    current = station,
                    isPlaying = c.isPlaying,
                    isBuffering = c.playbackState == androidx.media3.common.Player.STATE_BUFFERING,
                    hasError = c.playbackState == androidx.media3.common.Player.STATE_IDLE &&
                        c.playerError != null,
                    errorMessage = c.playerError?.message,
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = c.duration.coerceAtLeast(0),
                )
                kotlinx.coroutines.delay(500)
            }
        }
        c.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val station = c.currentMediaItem?.toStation()
                _state.value = _state.value.copy(current = station)
            }
        })
    }

    override fun play(station: Station, queue: List<Station>) {
        val c = controller ?: return
        scope.launch {
            val items = queue.ifEmpty { listOf(station) }.map { it.toMediaItem() }
            c.setMediaItems(items, items.indexOfFirst { it.mediaId == station.id }.coerceAtLeast(0), 0)
            c.prepare()
            c.play()
        }
    }

    override fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else { c.prepare(); c.play() }
    }

    override fun pause() { controller?.pause() }
    override fun resume() { controller?.let { it.prepare(); it.play() } }
    override fun next() { controller?.seekToNextMediaItem() }
    override fun previous() { controller?.seekToPreviousMediaItem() }
    override fun stop() { controller?.stop() }
}

/* ---------------------------------------------------------------- Mapping */

/** Convert a [Station] into a Media3 [MediaItem] carrying car metadata. */
fun Station.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(genre.label)
        .setAlbumTitle(country ?: "RadioSpark")
        .setArtworkUri(logoUrl?.let(android.net.Uri::parse))
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl)
        .setMediaMetadata(metadata)
        .build()
}

/** Rebuild a [Station] from a MediaItem (car/notification -> UI sync). */
fun MediaItem.toStation(): Station {
    val md = mediaMetadata
    return Station(
        id = mediaId,
        name = md.title?.toString().orEmpty(),
        streamUrl = localConfiguration?.uri?.toString().orEmpty(),
        logoUrl = md.artworkUri?.toString(),
        description = md.subtitle?.toString().orEmpty(),
    )
}
