package com.sparklab.radio.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
 * Adaptive root navigation: bottom bar on touch devices and a persistent,
 * D-pad-friendly navigation rail on Google TV.
 */
@Composable
fun RadioAppScaffold(vm: RadioViewModel, isTelevision: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val navigateTo: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isTelevision) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RS",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                },
            ) {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateTo(destination) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
            RadioNavHost(
                navController = navController,
                vm = vm,
                isTelevision = true,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Destination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTo(destination) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            RadioNavHost(
                navController = navController,
                vm = vm,
                isTelevision = false,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RadioNavHost(
    navController: NavHostController,
    vm: RadioViewModel,
    isTelevision: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.NOW_PLAYING.route,
        modifier = modifier,
    ) {
        composable(Destination.NOW_PLAYING.route) {
            NowPlayingScreen(vm, isTelevision = isTelevision) {
                navController.navigate(Destination.STATIONS.route)
            }
        }
        composable(Destination.STATIONS.route) {
            StationsScreen(
                vm = vm,
                onAdd = { navController.navigate("edit") },
                onEdit = { stationId -> navController.navigate("edit/$stationId") },
                isTelevision = isTelevision,
            )
        }
        composable(Destination.EXPLORE.route) {
            ExploreScreen(
                vm = vm,
                onGenre = { genre -> navController.navigate("genre/${genre.key}") },
                isTelevision = isTelevision,
            )
        }
        composable(Destination.FAVORITES.route) {
            FavoritesScreen(vm = vm, isTelevision = isTelevision)
        }
        composable(Destination.SETTINGS.route) { SettingsScreen(vm) }

        composable("genre/{key}") { entry ->
            GenreStationsScreen(
                vm = vm,
                genreKey = entry.arguments?.getString("key"),
                onBack = { navController.popBackStack() },
                isTelevision = isTelevision,
            )
        }
        composable("edit") {
            EditStationScreen(vm, stationId = null, onBack = { navController.popBackStack() })
        }
        composable("edit/{id}") { entry ->
            EditStationScreen(
                vm,
                stationId = entry.arguments?.getString("id"),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
