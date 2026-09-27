package com.worldrunner.feature.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.worldrunner.core.model.StandingsSource
import com.worldrunner.feature.standings.map.WorldMapCard
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun StandingsRoute(viewModel: StandingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    StandingsScreen(current, onSelectLeague = viewModel::selectLeague, onHighlightTeam = viewModel::highlightTeam)
}

@Composable
fun StandingsScreen(state: StandingsUiState, onSelectLeague: (LeagueChoice) -> Unit, onHighlightTeam: (String) -> Unit = {}) {
    val view = state.view
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.choice == LeagueChoice.Imported,
                    onClick = { onSelectLeague(LeagueChoice.Imported) },
                    label = { Text(stringResource(R.string.imported_league)) },
                )
                state.myTeams.forEach { team ->
                    FilterChip(
                        selected = state.choice == LeagueChoice.MyTeam(team.id),
                        onClick = { onSelectLeague(LeagueChoice.MyTeam(team.id)) },
                        label = { Text(team.name) },
                    )
                }
            }
        }
        if (view == null) {
            item { LeagueStatus(state.loadError) }
            return@LazyColumn
        }
        view.source?.let { item { SourceCard(it) } }
        item { RouteCard(view.routeProgress, state.unit) }
        item {
            WorldMapCard(view.league.standings, state.unit, state.highlightedTeamId, onSelectTeam = onHighlightTeam)
        }
        item {
            Text(view.league.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        if (view.league.standings.isEmpty()) {
            item { Text(stringResource(R.string.league_empty), style = MaterialTheme.typography.bodyMedium) }
        }
        items(view.league.standings, key = { it.team.id }) {
            StandingRow(it, state.unit, highlighted = it.team.id == state.highlightedTeamId, onClick = { onHighlightTeam(it.team.id) })
        }
    }
}

/** Loading or failure of the selected League; the map and list need its Standings. */
@Composable
private fun LeagueStatus(error: String?) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (error == null) {
            CircularProgressIndicator()
            Text(stringResource(R.string.league_loading), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.league_error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Text(error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Where imported Standings came from and when, so a snapshot is never taken for live data. */
@Composable
private fun SourceCard(source: StandingsSource) {
    val retrieved = source.retrievedAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.source_title, source.competition, source.season), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.source_updated, source.asOf.toString(), retrieved), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.source_url, source.url), style = MaterialTheme.typography.bodySmall)
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
