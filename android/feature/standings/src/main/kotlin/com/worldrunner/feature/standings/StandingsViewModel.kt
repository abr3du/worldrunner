package com.worldrunner.feature.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldrunner.core.data.LeagueView
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.StandingsRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StandingsUiState(
    val unit: DistanceUnit,
    val myTeams: List<Team>,
    val selectedTeamId: String,
    val view: LeagueView?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StandingsViewModel @Inject constructor(
    runnerRepository: RunnerRepository,
    teamRepository: TeamRepository,
    standingsRepository: StandingsRepository,
) : ViewModel() {
    private val selected = MutableStateFlow<String?>(null)
    private val myTeams = teamRepository.observeMyTeams()

    val uiState: StateFlow<StandingsUiState?> = combine(myTeams, selected) { teams, id -> teams to (id ?: teams.firstOrNull()?.team?.id) }
        .flatMapLatest { (teams, id) ->
            if (id == null) {
                flowOf(null)
            } else {
                combine(runnerRepository.runner, standingsRepository.observeLeague(id)) { runner, view ->
                    StandingsUiState(runner.unit, teams.map { it.team }, id, view)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectTeam(teamId: String) {
        selected.value = teamId
    }
}
