package com.worldrunner.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class NewRecordingsTest {
    private val day = LocalDate.of(2026, 9, 22)

    private fun recorded(id: String, start: String, end: String, km: Double = 5.0) = RecordedRun(
        id, Instant.parse("2026-09-22T${start}:00Z"), Instant.parse("2026-09-22T${end}:00Z"), day, Distance.kilometres(km),
    )

    private fun imported(recording: RecordedRun) =
        Run("run-${recording.id}", recording.date, recording.distance, SyncStatus.Confirmed, recording)

    @Test
    fun `separate recordings are all new`() {
        val morning = recorded("a", "07:00", "07:40")
        val evening = recorded("b", "18:00", "18:30")
        assertEquals(listOf(morning, evening), newRecordings(listOf(evening, morning), emptyList()))
    }

    @Test
    fun `a recording already imported is skipped`() {
        val morning = recorded("a", "07:00", "07:40")
        assertEquals(emptyList<RecordedRun>(), newRecordings(listOf(morning), listOf(imported(morning))))
    }

    @Test
    fun `one run recorded by two apps counts once, keeping the earliest start`() {
        val garmin = recorded("garmin", "07:00", "07:40", km = 8.02)
        val strava = recorded("strava", "07:01", "07:41", km = 8.0)
        assertEquals(listOf(garmin), newRecordings(listOf(strava, garmin), emptyList()))
    }

    @Test
    fun `a copy of a run imported earlier is skipped`() {
        val garmin = recorded("garmin", "07:00", "07:40")
        val strava = recorded("strava", "07:01", "07:41")
        assertEquals(emptyList<RecordedRun>(), newRecordings(listOf(strava), listOf(imported(garmin))))
    }

    @Test
    fun `back-to-back recordings are separate runs`() {
        val first = recorded("a", "07:00", "07:30")
        val second = recorded("b", "07:30", "08:00")
        assertEquals(listOf(first, second), newRecordings(listOf(first, second), emptyList()))
    }

    @Test
    fun `runs logged by hand never block an import`() {
        val manual = Run("manual", day, Distance.kilometres(5.0), SyncStatus.Confirmed)
        val morning = recorded("a", "07:00", "07:40")
        assertEquals(listOf(morning), newRecordings(listOf(morning), listOf(manual)))
    }
}
