package com.sparklab.radio.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The five phone sections. Android Auto uses its own browse tree instead. */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    NOW_PLAYING("now_playing", "Now Playing", Icons.Filled.PlayCircle),
    STATIONS("stations", "Stations", Icons.Filled.Radio),
    EXPLORE("explore", "Explore", Icons.Filled.Explore),
    FAVORITES("favorites", "Favorites", Icons.Filled.Favorite),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}
