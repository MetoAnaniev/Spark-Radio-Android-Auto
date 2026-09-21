package com.sparklab.radio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sparklab.radio.ui.edit.EditStationScreen
import com.sparklab.radio.ui.explore.ExploreScreen
import com.sparklab.radio.ui.explore.GenreStationsScreen
import com.sparklab.radio.ui.favorites.FavoritesScreen
import com.sparklab.radio.ui.nowplaying.NowPlayingScreen
import com.sparklab.radio.ui.settings.SettingsScreen
import com.sparklab.radio.ui.stations.StationsScreen
import com.sparklab.radio.ui.viewmodel.RadioViewModel

/**
 * Root scaffold: bottom navigation (phone) + NavHost.
 * The same ViewModel is shared across screens so playback/catalog state is unified.
 */
@Composable
fun RadioAppScaffold(vm: RadioViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                Destination.entries.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.NOW_PLAYING.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.NOW_PLAYING.route) { NowPlayingScreen(vm) { navController.navigate(Destination.STATIONS.route) } }
            composable(Destination.STATIONS.route) { StationsScreen(vm, onAdd = { navController.navigate("edit") }) }
            composable(Destination.EXPLORE.route) {
                ExploreScreen(vm, onGenre = { g -> navController.navigate("genre/${g.key}") })
            }
            composable(Destination.FAVORITES.route) { FavoritesScreen(vm) }
            composable(Destination.SETTINGS.route) { SettingsScreen(vm) }

            // Detail routes
            composable("genre/{key}") { entry ->
                GenreStationsScreen(
                    vm = vm,
                    genreKey = entry.arguments?.getString("key"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable("edit") { EditStationScreen(vm, stationId = null, onBack = { navController.popBackStack() }) }
            composable("edit/{id}") { entry ->
                EditStationScreen(vm, stationId = entry.arguments?.getString("id"), onBack = { navController.popBackStack() })
            }
        }
    }
}
