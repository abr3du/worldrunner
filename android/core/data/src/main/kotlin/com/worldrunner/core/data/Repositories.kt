package com.worldrunner.core.data

import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.League
import com.worldrunner.core.model.RouteProgress
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.StandingsSource
import com.worldrunner.core.model.Runner
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.Week
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RunnerRepository {
    val runner: Flow<Runner>

    suspend fun setUnit(unit: DistanceUnit)
}

interface RunRepository {
    fun observeRuns(week: Week): Flow<List<Run>>

    /** Validates and queues a Run. It starts as Pending; the server result replaces that state. */
    suspend fun logRun(date: LocalDate, distance: Distance): RunValidation
}

interface TeamRepository {
    /** The signed-in Runner's Teams this Season, with their current Standing. */
    fun observeMyTeams(): Flow<List<TeamDetail>>

    fun observeTeam(teamId: String): Flow<TeamDetail?>
}

/** A League with the highlighted Team's Route progress. [source] is set when the Standings were imported. */
data class LeagueView(val league: League, val routeProgress: RouteProgress, val source: StandingsSource? = null)

/** A League that may still be loading or may have failed to load. */
sealed interface LeagueState {
    data object Loading : LeagueState
    data class Loaded(val view: LeagueView) : LeagueState
    data class Failed(val reason: String) : LeagueState
}

interface StandingsRepository {
    /** The League that [teamId] competes in this Season. */
    fun observeLeague(teamId: String): Flow<LeagueView?>

    /** The imported kmspiel League snapshot; see docs/adr/0003-kmspiel-standings-prototype.md. */
    fun observeImportedLeague(): Flow<LeagueState>
}
