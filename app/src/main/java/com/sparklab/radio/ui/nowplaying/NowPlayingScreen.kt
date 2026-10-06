package com.sparklab.radio.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sparklab.radio.domain.model.PlaybackState
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.ui.components.StationArtwork
import com.sparklab.radio.ui.components.StatusPill
import com.sparklab.radio.ui.theme.HeartRed
import com.sparklab.radio.ui.theme.SuccessGreen
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/** Adaptive now-playing experience for phones and 10-foot Google TV screens. */
@Composable
fun NowPlayingScreen(
    vm: RadioViewModel,
    isTelevision: Boolean = false,
    onBrowse: () -> Unit,
) {
    val state by vm.playbackState.collectAsState()
    val station = state.current

    if (station == null) {
        EmptyNowPlaying(onBrowse = onBrowse)
        return
    }

    if (isTelevision) {
        TvNowPlaying(
            station = station,
            state = state,
            onPrevious = vm::previous,
            onPlayPause = vm::togglePlayPause,
            onNext = vm::next,
            onFavorite = { vm.toggleFavorite(station) },
        )
    } else {
        PhoneNowPlaying(
            station = station,
            state = state,
            onPrevious = vm::previous,
            onPlayPause = vm::togglePlayPause,
            onNext = vm::next,
            onFavorite = { vm.toggleFavorite(station) },
        )
    }
}

@Composable
private fun TvNowPlaying(
    station: Station,
    state: PlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 28.dp),
    ) {
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            StationArtwork(station = station, size = 300.dp, corner = 32)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                StationDetails(station, state.nowPlayingTitle)
                Spacer(Modifier.height(18.dp))
                ConnectionStatus(state)
                Spacer(Modifier.height(28.dp))
                PlaybackControls(
                    state = state,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                )
                Spacer(Modifier.height(16.dp))
                FavoriteControl(station = station, onFavorite = onFavorite)
            }
        }
    }
}

@Composable
private fun PhoneNowPlaying(
    station: Station,
    state: PlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        StationArtwork(station = station, size = 260.dp, corner = 32)
        Spacer(Modifier.height(24.dp))
        StationDetails(station, state.nowPlayingTitle)
        Spacer(Modifier.height(16.dp))
        ConnectionStatus(state)
        Spacer(Modifier.weight(1f))
        PlaybackControls(
            state = state,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
        )
        Spacer(Modifier.height(20.dp))
        FavoriteControl(station = station, onFavorite = onFavorite)
    }
}

@Composable
private fun StationDetails(station: Station, nowPlayingTitle: String?) {
    Text(
        text = station.name,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = station.genre.label + (station.country?.let { " · $it" } ?: ""),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    if (nowPlayingTitle != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = nowPlayingTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    } else if (station.description.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = station.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaybackControls(
    state: PlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(72.dp)) {
            Icon(
                Icons.Filled.SkipPrevious,
                "Previous station",
                Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.width(24.dp))
        Button(
            onClick = onPlayPause,
            shape = CircleShape,
            modifier = Modifier.size(96.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(24.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(72.dp)) {
            Icon(
                Icons.Filled.SkipNext,
                "Next station",
                Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun FavoriteControl(station: Station, onFavorite: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onFavorite, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (station.isFavorite) HeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (station.isFavorite) "In favorites" else "Add to favorites",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionStatus(state: PlaybackState) {
    val dot = when (state.status) {
        PlaybackState.Status.PLAYING -> SuccessGreen
        PlaybackState.Status.CONNECTING -> MaterialTheme.colorScheme.primary
        PlaybackState.Status.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (state.status) {
        PlaybackState.Status.PLAYING -> "Playing"
        PlaybackState.Status.CONNECTING -> "Connecting…"
        PlaybackState.Status.PAUSED -> "Paused"
        PlaybackState.Status.ERROR -> state.errorMessage ?: "Playback error"
        PlaybackState.Status.IDLE -> "Ready"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StatusPill(text = label, dotColor = dot)
        if (state.isBuffering) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun EmptyNowPlaying(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                null,
                Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Nothing playing",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a station to start listening",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBrowse, shape = MaterialTheme.shapes.large) {
            Text("Browse stations")
        }
    }
}
