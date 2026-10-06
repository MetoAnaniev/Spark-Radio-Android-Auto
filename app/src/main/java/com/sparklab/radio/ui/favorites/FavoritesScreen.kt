package com.sparklab.radio.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sparklab.radio.ui.components.StationRow
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/** Favorites — only stations the user hearted, ordered by last played. */
@Composable
fun FavoritesScreen(vm: RadioViewModel, isTelevision: Boolean = false) {
    val favorites by vm.favorites.collectAsState()
    val playing by vm.playbackState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTelevision) 48.dp else 16.dp),
    ) {
        Text(
            "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text(
                    "No favorites yet.\nTap the heart on any station.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
