package com.sparklab.radio.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.ui.components.StationArtwork
import com.sparklab.radio.ui.components.StationRow
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/** Favorites playlist with a persistent, remote-friendly rearrangement mode. */
@Composable
fun FavoritesScreen(vm: RadioViewModel, isTelevision: Boolean = false) {
    val favorites by vm.favorites.collectAsState()
    val playing by vm.playbackState.collectAsState()
    var isReordering by remember { mutableStateOf(false) }
    var draftOrder by remember { mutableStateOf<List<Station>>(emptyList()) }

    LaunchedEffect(favorites, isReordering) {
        if (!isReordering) draftOrder = favorites
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTelevision) 48.dp else 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Favorites",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            if (favorites.size > 1) {
                TextButton(
                    onClick = {
                        if (!isReordering) draftOrder = favorites
                        isReordering = !isReordering
                    },
                ) {
                    Icon(Icons.Filled.SwapVert, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isReordering) "Done" else "Rearrange")
                }
            }
        }

        if (isReordering) {
            Text(
                "Use the arrows to set your preferred playlist order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    null,
                    Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No favorites yet.\nTap the heart on any station.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (isReordering) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = if (isTelevision) 32.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(draftOrder, key = { it.id }) { station ->
                    val index = draftOrder.indexOfFirst { it.id == station.id }
                    FavoriteReorderRow(
                        station = station,
                        position = index + 1,
                        canMoveUp = index > 0,
                        canMoveDown = index in 0 until draftOrder.lastIndex,
                        onMoveUp = {
                            val reordered = draftOrder.moved(index, index - 1)
                            draftOrder = reordered
                            vm.reorderFavorites(reordered.map(Station::id))
                        },
                        onMoveDown = {
                            val reordered = draftOrder.moved(index, index + 1)
                            draftOrder = reordered
                            vm.reorderFavorites(reordered.map(Station::id))
                        },
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = if (isTelevision) 32.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(favorites, key = { it.id }) { station ->
                    StationRow(
                        station = station,
                        isPlaying = playing.current?.id == station.id,
                        nowPlayingTitle = playing.nowPlayingTitle.takeIf {
                            playing.current?.id == station.id
                        },
                        onPlay = { vm.play(station, favorites) },
                        onToggleFavorite = { vm.toggleFavorite(station) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteReorderRow(
    station: Station,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(10.dp))
            StationArtwork(station = station, size = 52.dp, corner = 14)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    station.country ?: station.genre.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.ArrowUpward, "Move ${station.name} up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.ArrowDownward, "Move ${station.name} down")
            }
        }
    }
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }
}
