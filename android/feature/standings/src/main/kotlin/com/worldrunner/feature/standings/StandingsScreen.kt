package com.worldrunner.feature.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldrunner.core.designsystem.ZoneLabel
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.RouteProgress
import com.worldrunner.core.model.Standing
import com.worldrunner.feature.standings.map.WorldMapCard

@Composable
fun StandingsRoute(viewModel: StandingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state
    if (current?.view == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    StandingsScreen(current, onSelectTeam = viewModel::selectTeam, onHighlightTeam = viewModel::highlightTeam)
}

@Composable
fun StandingsScreen(state: StandingsUiState, onSelectTeam: (String) -> Unit, onHighlightTeam: (String) -> Unit = {}) {
    val view = state.view ?: return
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.myTeams.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.myTeams.forEach { team ->
                        FilterChip(
                            selected = team.id == state.selectedTeamId,
                            onClick = { onSelectTeam(team.id) },
                            label = { Text(team.name) },
                        )
                    }
                }
            }
        }
        item { RouteCard(view.routeProgress, state.unit) }
        item {
            WorldMapCard(view.league.standings, state.unit, state.highlightedTeamId, onSelectTeam = onHighlightTeam)
        }
        item {
            Text(view.league.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        items(view.league.standings, key = { it.team.id }) {
            StandingRow(it, state.unit, highlighted = it.team.id == state.highlightedTeamId, onClick = { onHighlightTeam(it.team.id) })
        }
    }
}

@Composable
private fun RouteCard(route: RouteProgress, unit: DistanceUnit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.route_progress), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { route.fraction }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.route_travelled, route.travelled.format(unit), route.routeLength.format(unit), route.nextPlace),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StandingRow(standing: Standing, unit: DistanceUnit, highlighted: Boolean, onClick: () -> Unit) {
    val mine = standing.team.isMine
    val background = if (mine) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = highlighted, onClick = onClick)
            .background(background)
            .then(if (highlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${standing.rank}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.weight(1f)) {
            Text(standing.team.name, fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal)
            if (mine) Text(stringResource(R.string.your_team), style = MaterialTheme.typography.labelMedium)
            ZoneLabel(standing.zone)
        }
        Text(standing.totalDistance.format(unit))
    }
}
