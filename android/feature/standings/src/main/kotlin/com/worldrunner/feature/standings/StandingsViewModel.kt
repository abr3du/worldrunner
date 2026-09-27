package com.worldrunner.feature.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldrunner.core.data.LeagueState
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which League the Standings screen shows. */
sealed interface LeagueChoice {
    /** The kmspiel League imported as a snapshot; the default. */
    data object Imported : LeagueChoice

    /** The League of one of the Runner's own Teams. */
    data class MyTeam(val teamId: String) : LeagueChoice
}

data class StandingsUiState(
    val unit: DistanceUnit,
    val myTeams: List<Team>,
    val choice: LeagueChoice,
    /** Null while the League is loading or when it failed to load. */
    val view: LeagueView?,
    /** The Team identified on the map and in the list; null when none is picked. */
    val highlightedTeamId: String? = null,
    /** Why the League could not be loaded; null unless loading failed. */
    val loadError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StandingsViewModel @Inject constructor(
    runnerRepository: RunnerRepository,
    teamRepository: TeamRepository,
    standingsRepository: StandingsRepository,
) : ViewModel() {
    private val choice = MutableStateFlow<LeagueChoice>(LeagueChoice.Imported)
    private val highlighted = MutableStateFlow<String?>(null)
    private val myTeams = teamRepository.observeMyTeams()

    val uiState: StateFlow<StandingsUiState?> = combine(myTeams, choice, ::Pair)
        .flatMapLatest { (teams, choice) ->
            val league: Flow<LeagueState> = when (choice) {
                LeagueChoice.Imported -> standingsRepository.observeImportedLeague()
                is LeagueChoice.MyTeam -> standingsRepository.observeLeague(choice.teamId)
                    .map { view -> view?.let { LeagueState.Loaded(it) } ?: LeagueState.Loading }
            }
            combine(runnerRepository.runner, league, highlighted) { runner, state, highlight ->
                val view = (state as? LeagueState.Loaded)?.view
                val inLeague = highlight?.takeIf { h -> view?.league?.standings?.any { it.team.id == h } == true }
                StandingsUiState(runner.unit, teams.map { it.team }, choice, view, inLeague, (state as? LeagueState.Failed)?.reason)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectLeague(league: LeagueChoice) {
        choice.value = league
    }

    /** Identifies [teamId] on the map and in the list, from a marker tap or a row tap. */
    fun highlightTeam(teamId: String) {
        highlighted.value = teamId
    }
}
