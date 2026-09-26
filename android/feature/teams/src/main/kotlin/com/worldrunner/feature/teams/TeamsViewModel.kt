package com.worldrunner.feature.teams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.TeamDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TeamsUiState(val unit: DistanceUnit, val teams: List<TeamDetail>)

@HiltViewModel
class TeamsViewModel @Inject constructor(
    runnerRepository: RunnerRepository,
    teamRepository: TeamRepository,
) : ViewModel() {
    val uiState: StateFlow<TeamsUiState?> =
        combine(runnerRepository.runner, teamRepository.observeMyTeams()) { runner, teams -> TeamsUiState(runner.unit, teams) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class TeamDetailUiState(val unit: DistanceUnit, val detail: TeamDetail?)

@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    runnerRepository: RunnerRepository,
    teamRepository: TeamRepository,
) : ViewModel() {
    private val teamId = savedStateHandle.toRoute<TeamDetailRoute>().teamId

    val uiState: StateFlow<TeamDetailUiState?> =
        combine(runnerRepository.runner, teamRepository.observeTeam(teamId)) { runner, detail -> TeamDetailUiState(runner.unit, detail) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
