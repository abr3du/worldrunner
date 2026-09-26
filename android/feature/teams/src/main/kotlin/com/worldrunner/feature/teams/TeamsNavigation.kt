package com.worldrunner.feature.teams

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TeamsRoute

@Serializable
data class TeamDetailRoute(val teamId: String)

fun NavGraphBuilder.teamsScreens(onTeamClick: (String) -> Unit, onBack: () -> Unit) {
    composable<TeamsRoute> { TeamsRoute(onTeamClick = onTeamClick) }
    composable<TeamDetailRoute> { TeamDetailRoute(onBack = onBack) }
}
