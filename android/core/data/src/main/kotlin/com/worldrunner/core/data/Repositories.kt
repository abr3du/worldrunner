package com.worldrunner.core.data

import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.ImportResult
import com.worldrunner.core.model.League
import com.worldrunner.core.model.RecordedRun
import com.worldrunner.core.model.RouteProgress
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.StandingsSource
import com.worldrunner.core.model.Runner
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.Week
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface RunnerRepository {
    val runner: Flow<Runner>

    suspend fun setUnit(unit: DistanceUnit)
}

interface RunRepository {
    fun observeRuns(week: Week): Flow<List<Run>>

    /** Validates and queues a Run. It starts as Pending; the server result replaces that state. */
    suspend fun logRun(date: LocalDate, distance: Distance): RunValidation

    /** Logs each of [recorded] not imported before as a Run, under the same rules as [logRun]. */
    suspend fun importRuns(recorded: List<RecordedRun>): ImportResult
}

enum class RecordedRunAvailability { Available, NeedsUpdate, Unavailable }

/**
 * Runs other apps recorded (Garmin Connect, Strava, ...), read from Health Connect on this phone.
 * See docs/adr/0004-import-runs-from-health-connect.md.
 */
interface RecordedRunSource {
    fun availability(): RecordedRunAvailability

    /** The Health Connect permissions [read] needs; request them with PermissionController's contract. */
    val requiredPermissions: Set<String>

    suspend fun hasPermissions(): Boolean

    /** On-foot recordings that started at or after [since]. */
    suspend fun read(since: Instant): List<RecordedRun>
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
