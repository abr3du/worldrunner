package com.worldrunner.core.data.fake

import com.worldrunner.core.data.LeagueState
import com.worldrunner.core.data.LeagueView
import com.worldrunner.core.data.RunRepository
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.StandingsRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.data.snapshot.StandingsSnapshot
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.ImportResult
import com.worldrunner.core.model.RecordedRun
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.Runner
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.Week
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class FakeRunnerRepository @Inject constructor(private val store: FakeGameStore) : RunnerRepository {
    override val runner: Flow<Runner> = store.runner

    override suspend fun setUnit(unit: DistanceUnit) = store.setUnit(unit)
}

class FakeRunRepository @Inject constructor(private val store: FakeGameStore) : RunRepository {
    override fun observeRuns(week: Week): Flow<List<Run>> =
        store.runs.map { runs -> runs.filter { it.week == week }.sortedByDescending { it.date } }

    override suspend fun logRun(date: LocalDate, distance: Distance): RunValidation = store.logRun(date, distance)

    override suspend fun importRuns(recorded: List<RecordedRun>): ImportResult = store.importRuns(recorded)
}

class FakeTeamRepository @Inject constructor(private val store: FakeGameStore) : TeamRepository {
    override fun observeMyTeams(): Flow<List<TeamDetail>> =
        combine(store.runs, store.runner) { runs, _ -> store.myTeamIds.mapNotNull { store.teamDetail(runs, it) } }

    override fun observeTeam(teamId: String): Flow<TeamDetail?> =
        combine(store.runs, store.runner) { runs, _ -> store.teamDetail(runs, teamId) }
}

class FakeStandingsRepository @Inject constructor(private val store: FakeGameStore) : StandingsRepository {
    override fun observeLeague(teamId: String): Flow<LeagueView?> = store.runs.map { runs ->
        store.league(runs, teamId)?.let { league ->
            val mine = league.standings.first { it.team.id == teamId }
            LeagueView(league, store.routeProgress(mine.totalDistance))
        }
    }

    override fun observeImportedLeague(): Flow<LeagueState> = flow {
        emit(LeagueState.Loading)
        val result = runCatching { withContext(Dispatchers.IO) { StandingsSnapshot.readBundled() } }
        emit(result.fold({ LeagueState.Loaded(it) }, { LeagueState.Failed(it.message ?: it.toString()) }))
    }
}
