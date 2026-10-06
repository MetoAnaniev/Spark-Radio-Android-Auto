package com.sparklab.radio.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.ui.components.StationRow
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/** Explore — a grid of genre tiles. Tapping opens the filtered stations list. */
@Composable
fun ExploreScreen(
    vm: RadioViewModel,
    onGenre: (Genre) -> Unit,
    isTelevision: Boolean = false,
) {
    val byGenre by vm.stationsByGenre.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTelevision) 48.dp else 16.dp),
    ) {
        Text(
            "Explore by Genre",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTelevision) 4 else 2),
            contentPadding = PaddingValues(bottom = if (isTelevision) 32.dp else 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Genre.entries.toList(), key = { it.key }) { genre ->
                GenreTile(
                    genre = genre,
                    count = byGenre[genre]?.size ?: 0,
                    onClick = { onGenre(genre) },
                    isTelevision = isTelevision,
                )
            }
        }
    }
}

@Composable
private fun GenreTile(
    genre: Genre,
    count: Int,
    onClick: () -> Unit,
    isTelevision: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = if (isFocused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 10.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .aspectRatio(if (isTelevision) 1.55f else 1.25f)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                val scale = if (isFocused) 1.04f else 1f
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(Modifier.align(Alignment.TopStart)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(genre.icon, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                Text(genre.label, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("$count stations", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Filtered station list for one genre, with a quick "Play first" action. */
@Composable
fun GenreStationsScreen(
    vm: RadioViewModel,
    genreKey: String?,
    onBack: () -> Unit,
    isTelevision: Boolean = false,
) {
    val genre = Genre.fromKey(genreKey)
    val byGenre by vm.stationsByGenre.collectAsState()
    val playing by vm.playbackState.collectAsState()
    val stations = byGenre[genre].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTelevision) 48.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(genre.label, style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
        }

        // Quick action: play the first station in the genre
        if (stations.isNotEmpty()) {
            Card(
                onClick = { vm.play(stations.first(), stations) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Play first · ${stations.first().name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = if (isTelevision) 32.dp else 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(stations, key = { it.id }) { station ->
                StationRow(
                    station = station,
                    isPlaying = playing.current?.id == station.id,
                    nowPlayingTitle = playing.nowPlayingTitle.takeIf {
                        playing.current?.id == station.id
                    },
                    onPlay = { vm.play(station, stations) },
                    onToggleFavorite = { vm.toggleFavorite(station) },
                )
            }
        }
    }
}
