package com.worldrunner.feature.standings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object StandingsRoute

fun NavGraphBuilder.standingsScreen() {
    composable<StandingsRoute> { StandingsRoute() }
}
