package com.worldrunner.core.data

import com.worldrunner.core.data.fake.FakeGameStore
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.ImportResult
import com.worldrunner.core.model.RecordedRun
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class FakeGameStoreTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun `logged run is Pending then Confirmed`() = runTest {
        val store = FakeGameStore(clock, this)
        assertEquals(RunValidation.Valid, store.logRun(today, Distance.kilometres(5.0)))
        assertEquals(SyncStatus.Pending, store.runs.value.last().status)

        advanceUntilIdle()
        assertEquals(SyncStatus.Confirmed, store.runs.value.last().status)
    }

    @Test
    fun `invalid run is not queued`() = runTest {
        val store = FakeGameStore(clock, this)
        val before = store.runs.value.size
        assertEquals(RunValidation.WeekClosed, store.logRun(LocalDate.of(2026, 9, 1), Distance.kilometres(5.0)))
        assertEquals(before, store.runs.value.size)
    }

    @Test
    fun `confirmed run raises my team's total and only confirmed distance counts`() = runTest {
        val store = FakeGameStore(clock, this)
        val before = store.league(store.runs.value, "trail-mix")!!.standings.first { it.team.id == "trail-mix" }

        store.logRun(today, Distance.kilometres(50.0))
        val pending = store.league(store.runs.value, "trail-mix")!!.standings.first { it.team.id == "trail-mix" }
        assertEquals(before.totalDistance, pending.totalDistance)

        advanceUntilIdle()
        val after = store.league(store.runs.value, "trail-mix")!!.standings.first { it.team.id == "trail-mix" }
        assertEquals(before.totalDistance + Distance.kilometres(50.0), after.totalDistance)
        assertTrue(after.rank < before.rank)
    }

    @Test
    fun `promotion and relegation zones take two teams each`() = runTest {
        val league = FakeGameStore(clock, this).league(emptyList(), "trail-mix")!!
        assertEquals(2, league.standings.count { it.zone == com.worldrunner.core.model.Zone.Promotion })
        assertEquals(2, league.standings.count { it.zone == com.worldrunner.core.model.Zone.Relegation })
    }

    private fun recorded(id: String, date: LocalDate, km: Double) = RecordedRun(
        id, date.atTime(7, 0).toInstant(ZoneOffset.UTC), date.atTime(7, 45).toInstant(ZoneOffset.UTC), date, Distance.kilometres(km),
    )

    @Test
    fun `imported runs are logged once and remember their recording`() = runTest {
        val store = FakeGameStore(clock, this)
        val before = store.runs.value.size
        val recordings = listOf(recorded("a", today, 8.0), recorded("b", today.minusDays(1), 5.0))

        assertEquals(ImportResult(imported = 2, rejected = 0), store.importRuns(recordings))
        assertEquals(recordings.toSet(), store.runs.value.mapNotNull { it.recording }.toSet())

        assertEquals(ImportResult(imported = 0, rejected = 0), store.importRuns(recordings))
        assertEquals(before + 2, store.runs.value.size)
    }

    @Test
    fun `imported runs follow the run rules`() = runTest {
        val store = FakeGameStore(clock, this)
        val recordings = listOf(
            recorded("closed-week", LocalDate.of(2026, 9, 1), 5.0),
            recorded("ultra", today, 120.0),
            recorded("no-distance", today.minusDays(1), 0.0),
            recorded("ok", today.minusDays(2), 5.0),
        )
        assertEquals(ImportResult(imported = 1, rejected = 3), store.importRuns(recordings))
    }

}
