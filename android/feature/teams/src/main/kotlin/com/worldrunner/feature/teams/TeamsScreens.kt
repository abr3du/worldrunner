package com.worldrunner.feature.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldrunner.core.designsystem.ZoneLabel

@Composable
fun TeamsRoute(onTeamClick: (String) -> Unit, viewModel: TeamsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state ?: return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Text(
                stringResource(R.string.your_teams),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp).semantics { heading() },
            )
        }
        items(current.teams, key = { it.team.id }) { detail ->
            ListItem(
                headlineContent = { Text(detail.team.name) },
                supportingContent = { Text(stringResource(R.string.rank_in_league, detail.standing.rank, detail.leagueName)) },
                trailingContent = { Text(detail.standing.totalDistance.format(current.unit)) },
                modifier = Modifier.clickable { onTeamClick(detail.team.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailRoute(onBack: () -> Unit, viewModel: TeamDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state?.detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.team?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (detail == null) return@Scaffold
        val unit = state!!.unit
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.rank_in_league, detail.standing.rank, detail.leagueName), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.season_total, detail.standing.totalDistance.format(unit)))
                    ZoneLabel(detail.standing.zone)
                }
            }
            item {
                Text(
                    stringResource(R.string.weekly_totals),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
                )
                HorizontalDivider()
            }
            items(detail.roster, key = { it.displayName }) { mate ->
                ListItem(
                    headlineContent = {
                        Text(if (mate.isMe) stringResource(R.string.you, mate.displayName) else mate.displayName)
                    },
                    trailingContent = { Text(mate.weeklyTotal.format(unit)) },
                )
            }
        }
    }
}
