package com.sparklab.radio.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sparklab.radio.ui.components.StationRow
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/**
 * Stations — scrollable, card-based list of every station (static + user + remote).
 * Includes a search box and a FAB to add a custom station.
 */
@Composable
fun StationsScreen(vm: RadioViewModel, onAdd: () -> Unit) {
    val all by vm.allStations.collectAsState()
    val playing by vm.playbackState.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(all, query) {
        if (query.isBlank()) all
        else all.filter {
            it.name.contains(query, true) ||
                it.genre.label.contains(query, true) ||
                it.description.contains(query, true)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Add station")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Stations",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search stations") },
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { station ->
                    StationRow(
                        station = station,
                        isPlaying = playing.current?.id == station.id,
                        onPlay = { vm.play(station, filtered) },
                        onToggleFavorite = { vm.toggleFavorite(station) },
                    )
                }
            }
        }
    }
}
