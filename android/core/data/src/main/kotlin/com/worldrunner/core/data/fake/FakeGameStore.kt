package com.worldrunner.core.data.fake

import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.ImportResult
import com.worldrunner.core.model.League
import com.worldrunner.core.model.RecordedRun
import com.worldrunner.core.model.RouteProgress
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.Runner
import com.worldrunner.core.model.Standing
import com.worldrunner.core.model.SyncStatus
import com.worldrunner.core.model.Team
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.TeammateTotal
import com.worldrunner.core.model.Week
import com.worldrunner.core.model.WorldRoute
import com.worldrunner.core.model.newRecordings
import com.worldrunner.core.model.Zone
import com.worldrunner.core.model.sum
import com.worldrunner.core.model.validateRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * In-memory stand-in for the Worldrunner API, used until the backend exists.
 * It applies the same rules the server will, so the UI can be built and tested against it.
 */
class FakeGameStore(
    private val clock: Clock,
    private val syncScope: CoroutineScope,
    private val syncDelayMillis: Long = 1_500,
) {
    private val _runner = MutableStateFlow(Runner(displayName = "Sam", unit = DistanceUnit.Kilometres))
    val runner: StateFlow<Runner> = _runner

    private val _runs = MutableStateFlow(seedRuns())
    val runs: StateFlow<List<Run>> = _runs

    fun setUnit(unit: DistanceUnit) = _runner.update { it.copy(unit = unit) }

    fun logRun(date: LocalDate, distance: Distance, recording: RecordedRun? = null): RunValidation {
        val sameDay = _runs.value.filter { it.date == date }.map { it.distance }.sum()
        val result = validateRun(distance, date, LocalDate.now(clock), clock.instant(), sameDay)
        if (result != RunValidation.Valid) return result

        val id = UUID.randomUUID().toString()
        _runs.update { it + Run(id, date, distance, SyncStatus.Pending, recording) }
        syncScope.launch {
            delay(syncDelayMillis)
            val confirmed = if (Week.containing(date).isOpenAt(clock.instant())) SyncStatus.Confirmed else SyncStatus.NeedsAttention
            _runs.update { runs -> runs.map { if (it.id == id) it.copy(status = confirmed) else it } }
        }
        return result
    }

    /** Logs a Run for each of [recorded] not imported before; see [newRecordings]. */
    fun importRuns(recorded: List<RecordedRun>): ImportResult {
        val fresh = newRecordings(recorded, _runs.value)
        val imported = fresh.count { logRun(it.date, it.distance, it) == RunValidation.Valid }
        return ImportResult(imported, rejected = fresh.size - imported)
    }

    /** Builds the League containing [teamId] from the current Runs. */
    fun league(runs: List<Run>, teamId: String): League? {
        val seed = leagues.firstOrNull { l -> l.teams.any { it.id == teamId } } ?: return null
        val season = currentWeek().season
        val myConfirmed = runs.filter { it.status == SyncStatus.Confirmed && it.week.season == season }
        val myTotal = myConfirmed.map { it.distance }.sum()
        val rows = seed.teams.map { t ->
            val isMine = t.id in myTeamIds
            val total = t.seasonTotal + if (isMine) myTotal else Distance.Zero
            val active = t.activeRunners + if (isMine && myTotal.metres > 0) 1 else 0
            Triple(t, total, active)
        }.sortedWith(
            compareByDescending<Triple<SeedTeam, Distance, Int>> { it.second }
                .thenByDescending { it.third }
                .thenBy { it.first.createdOrder },
        )
        val standings = rows.mapIndexed { i, (t, total, active) ->
            val rank = i + 1
            val zone = when {
                seed.tier > 1 && rank <= PROMOTED -> Zone.Promotion
                seed.tier < LOWEST_TIER && rank > rows.size - RELEGATED -> Zone.Relegation
                else -> Zone.Safe
            }
            Standing(rank, Team(t.id, t.name, t.id in myTeamIds), total, active, zone)
        }
        return League(seed.id, seed.name, seed.tier, standings)
    }

    fun teamDetail(runs: List<Run>, teamId: String): TeamDetail? {
        val league = league(runs, teamId) ?: return null
        val standing = league.standings.first { it.team.id == teamId }
        val week = currentWeek()
        val myWeekly = runs.filter { it.status == SyncStatus.Confirmed && it.week == week }.map { it.distance }.sum()
        val roster = teammates.getValue(teamId).map { (name, km) -> TeammateTotal(name, Distance.kilometres(km), isMe = false) } +
            TeammateTotal(_runner.value.displayName, myWeekly, isMe = true)
        return TeamDetail(standing.team, league.name, standing, roster.sortedByDescending { it.weeklyTotal })
    }

    fun routeProgress(travelled: Distance): RouteProgress =
        RouteProgress(travelled, WorldRoute.length, WorldRoute.positionAt(travelled).next.name)

    fun currentWeek(): Week = Week.containing(LocalDate.now(clock))

    val myTeamIds = listOf("trail-mix", "lunch-break")

    private fun seedRuns(): List<Run> {
        val today = LocalDate.now(clock)
        return listOf(3L, 9L, 11L).map { daysAgo ->
            Run(UUID.randomUUID().toString(), today.minusDays(daysAgo), Distance.kilometres(6.4), SyncStatus.Confirmed)
        }
    }

    private class SeedTeam(
        val id: String,
        val name: String,
        val seasonTotal: Distance,
        val activeRunners: Int,
        val createdOrder: Int,
    )

    private class SeedLeague(val id: String, val name: String, val tier: Int, val teams: List<SeedTeam>)

    private val leagues = listOf(
        SeedLeague(
            id = "t3-b", name = "Tier 3 · League B", tier = 3,
            teams = listOf(
                "Trail Mix" to 1_180.0, "Harbour Striders" to 1_402.0, "Night Owls" to 1_356.5,
                "Hill Repeats" to 1_290.0, "Parkrun Regulars" to 1_244.0, "Couch Escapees" to 1_095.0,
                "Negative Splits" to 1_020.5, "The Pacers" to 988.0, "Sunday Long Run" to 941.0, "Slow & Steady" to 870.0,
            ).mapIndexed { i, (name, km) -> SeedTeam(name.slug(), name, Distance.kilometres(km), 6 + i % 4, i) },
        ),
        SeedLeague(
            id = "t4-f", name = "Tier 4 · League F", tier = 4,
            teams = listOf(
                "Lunch Break Runners" to 610.0, "Muddy Shoes" to 702.0, "Early Birds" to 655.0,
                "Km Collectors" to 590.0, "Rainy Day Club" to 540.0, "Stroller Squad" to 488.0,
                "Office Dash" to 452.0, "Tempo Tuesdays" to 431.0, "First Timers" to 380.0, "Last Mile" to 312.0,
            ).mapIndexed { i, (name, km) -> SeedTeam(name.slug(), name, Distance.kilometres(km), 4 + i % 3, i) },
        ),
    )

    private val teammates = mapOf(
        "trail-mix" to listOf("Robin" to 31.2, "Jo" to 24.0, "Kai" to 18.5, "Mika" to 12.0, "Ari" to 0.0),
        "lunch-break" to listOf("Priya" to 15.0, "Dev" to 9.8, "Lee" to 5.0),
    )

    private companion object {
        const val PROMOTED = 2
        const val RELEGATED = 2
        const val LOWEST_TIER = 4

        fun String.slug() = lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .let { if (it == "lunch-break-runners") "lunch-break" else it }
    }
}
