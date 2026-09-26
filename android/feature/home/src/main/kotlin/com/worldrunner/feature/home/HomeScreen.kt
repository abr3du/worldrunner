package com.worldrunner.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldrunner.core.designsystem.SyncStatusLabel
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.TeamDetail
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeRoute(onTeamClick: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state ?: return
    HomeScreen(current, onLogRunClick = viewModel::openLogRun, onTeamClick = onTeamClick)
    current.logRun?.let { form ->
        LogRunSheet(
            state = form,
            unit = current.unit,
            onDistanceChange = viewModel::onDistanceChange,
            onDateChange = viewModel::onDateChange,
            onSave = viewModel::saveRun,
            onDismiss = viewModel::dismissLogRun,
        )
    }
}

@Composable
fun HomeScreen(state: HomeUiState, onLogRunClick: () -> Unit, onTeamClick: (String) -> Unit) {
    Scaffold(
        floatingActionButton = {
            val label = stringResource(R.string.log_run)
            // ExtendedFloatingActionButton hides its text from semantics, which leaves
            // TalkBack announcing an unnamed "Button"; label the button itself.
            ExtendedFloatingActionButton(
                onClick = onLogRunClick,
                modifier = Modifier.semantics { contentDescription = label },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(label) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { WeekSummary(state) }
            if (state.teams.isNotEmpty()) {
                item { SectionHeading(stringResource(R.string.team_impact)) }
                items(state.teams, key = { it.team.id }) { TeamImpactRow(it, state, onTeamClick) }
            }
            item { SectionHeading(stringResource(R.string.this_week_runs)) }
            if (state.runs.isEmpty()) {
                item { Text(stringResource(R.string.no_runs_yet), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.runs, key = { it.id }) { RunRow(it, state) }
        }
    }
}

@Composable
private fun WeekSummary(state: HomeUiState) {
    val closes = state.weekClosesAt.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.season_week, state.week.season.toString(), state.week.numberInSeason),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(state.weeklyTotal.format(state.unit), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.weekly_total), style = MaterialTheme.typography.bodyMedium)
            if (state.pendingTotal != Distance.Zero) {
                Text(
                    stringResource(R.string.pending_provisional, state.pendingTotal.format(state.unit)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(stringResource(R.string.week_closes, closes), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TeamImpactRow(detail: TeamDetail, state: HomeUiState, onTeamClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(detail.team.name) },
        supportingContent = { Text(stringResource(R.string.rank_in_league, detail.standing.rank, detail.leagueName)) },
        trailingContent = { Text(detail.standing.totalDistance.format(state.unit)) },
        modifier = Modifier.clickable { onTeamClick(detail.team.id) },
    )
}

@Composable
private fun RunRow(run: Run, state: HomeUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(run.distance.format(state.unit), style = MaterialTheme.typography.titleMedium)
            Text(
                run.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Box { SyncStatusLabel(run.status) }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
}
