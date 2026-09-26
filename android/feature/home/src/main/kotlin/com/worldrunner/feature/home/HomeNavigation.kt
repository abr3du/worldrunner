package com.worldrunner.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(onTeamClick: (String) -> Unit) {
    composable<HomeRoute> { HomeRoute(onTeamClick = onTeamClick) }
}
