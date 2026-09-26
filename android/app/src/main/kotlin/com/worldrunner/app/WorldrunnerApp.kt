package com.worldrunner.app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.worldrunner.feature.home.HomeRoute
import com.worldrunner.feature.home.homeScreen
import com.worldrunner.feature.profile.ProfileRoute
import com.worldrunner.feature.profile.profileScreen
import com.worldrunner.feature.standings.StandingsRoute
import com.worldrunner.feature.standings.standingsScreen
import com.worldrunner.feature.teams.TeamDetailRoute
import com.worldrunner.feature.teams.TeamsRoute
import com.worldrunner.feature.teams.teamsScreens
import kotlin.reflect.KClass

private enum class TopLevel(val route: Any, val routeClass: KClass<*>, val icon: ImageVector, @StringRes val label: Int) {
    Home(HomeRoute, HomeRoute::class, Icons.Filled.Home, R.string.tab_home),
    Teams(TeamsRoute, TeamsRoute::class, Icons.Filled.Face, R.string.tab_teams),
    Standings(StandingsRoute, StandingsRoute::class, Icons.Filled.Star, R.string.tab_standings),
    Profile(ProfileRoute, ProfileRoute::class, Icons.Filled.Person, R.string.tab_profile),
}

@Composable
fun WorldrunnerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevel.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            val openTeam: (String) -> Unit = { navController.navigate(TeamDetailRoute(it)) }
            homeScreen(onTeamClick = openTeam)
            teamsScreens(onTeamClick = openTeam, onBack = navController::popBackStack)
            standingsScreen()
            profileScreen()
        }
    }
}
