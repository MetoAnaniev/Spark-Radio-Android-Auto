package com.sparklab.radio.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sparklab.radio.domain.model.Genre
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

/** Phone/TV playback bridge to the MediaSession owned by [RadioMediaService]. */
class MediaControllerPlaybackRepository(
    private val context: Context,
) : PlaybackRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: PlayRequest? = null
    private val knownStations = mutableMapOf<String, Station>()

    private val _state = MutableStateFlow(PlaybackState())
    override val state = _state.asStateFlow()

    /** Connect early, while preserving any play request made before Media3 is ready. */
    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, RadioMediaService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            scope.launch {
                try {
                    val connectedController = future.await()
                    controller = connectedController
                    observeController(connectedController)
                    pendingPlay?.also {
                        pendingPlay = null
                        startPlayback(connectedController, it)
                    }
                } catch (error: Throwable) {
                    controllerFuture = null
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        hasError = true,
                        errorMessage = "Player connection failed: ${error.message ?: "unknown error"}",
                    )
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
        pendingPlay = null
    }

    private fun observeController(mediaController: MediaController) {
        fun updateState(error: PlaybackException? = mediaController.playerError) {
            val mediaItem = mediaController.currentMediaItem
            val station = mediaItem?.let { knownStations[it.mediaId] ?: it.toStation() }
            _state.value = PlaybackState(
                current = station,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
                hasError = error != null,
                errorMessage = error?.message,
                positionMs = mediaController.currentPosition.coerceAtLeast(0),
                durationMs = mediaController.duration.coerceAtLeast(0),
            )
        }

        mediaController.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateState()
            override fun onPlaybackStateChanged(playbackState: Int) = updateState()
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
            override fun onPlayerError(error: PlaybackException) = updateState(error)
        })
        updateState()
    }

    override fun play(station: Station, queue: List<Station>) {
        val request = PlayRequest(station, queue.ifEmpty { listOf(station) })
        knownStations.putAll(request.queue.associateBy(Station::id))
        _state.value = PlaybackState(current = station, isBuffering = true)

        val activeController = controller
        if (activeController == null) {
            pendingPlay = request
            connect()
            return
        }
        startPlayback(activeController, request)
    }

    private fun startPlayback(mediaController: MediaController, request: PlayRequest) {
        val items = request.queue.map(Station::toMediaItem)
        val selectedIndex = items.indexOfFirst { it.mediaId == request.station.id }.coerceAtLeast(0)
        mediaController.setMediaItems(items, selectedIndex, 0)
        mediaController.prepare()
        mediaController.play()
    }

    override fun togglePlayPause() {
        val activeController = controller
        if (activeController == null) {
            connect()
            return
        }
        if (activeController.isPlaying) {
            activeController.pause()
        } else {
            activeController.prepare()
            activeController.play()
        }
    }

    override fun pause() {
        controller?.pause()
    }

    override fun resume() {
        controller?.let {
            it.prepare()
            it.play()
        } ?: connect()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun stop() {
        controller?.stop()
    }

    private data class PlayRequest(val station: Station, val queue: List<Station>)
}

/** Convert a station into a fully playable Media3 item with car metadata. */
fun Station.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setSubtitle(description)
        .setArtist(genre.label)
        .setAlbumTitle(country ?: "RadioSpark")
        .setArtworkUri(logoUrl?.let(android.net.Uri::parse))
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl)
        .setMediaMetadata(metadata)
        .build()
}

/** Rebuild a station from MediaSession metadata for phone/TV UI synchronization. */
fun MediaItem.toStation(): Station {
    val metadata = mediaMetadata
    val genreLabel = metadata.artist?.toString()
    return Station(
        id = mediaId,
        name = metadata.title?.toString().orEmpty(),
        streamUrl = localConfiguration?.uri?.toString().orEmpty(),
        logoUrl = metadata.artworkUri?.toString(),
        genre = Genre.entries.firstOrNull { it.label.equals(genreLabel, ignoreCase = true) } ?: Genre.OTHER,
        description = metadata.subtitle?.toString().orEmpty(),
        country = metadata.albumTitle?.toString(),
    )
}
