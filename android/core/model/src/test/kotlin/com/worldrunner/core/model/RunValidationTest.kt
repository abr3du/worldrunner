package com.worldrunner.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RunValidationTest {
    private val today = LocalDate.of(2026, 9, 23) // Wednesday
    private val now = Instant.parse("2026-09-23T10:00:00Z")

    private fun validate(km: Double, date: LocalDate = today, sameDayKm: Double = 0.0) =
        validateRun(Distance.kilometres(km), date, today, now, Distance.kilometres(sameDayKm))

    @Test
    fun `ordinary run is valid`() = assertEquals(RunValidation.Valid, validate(5.0))

    @Test
    fun `zero distance is rejected`() = assertEquals(RunValidation.NotPositive, validate(0.0))

    @Test
    fun `run over the per-run limit is rejected`() =
        assertEquals(RunValidation.OverRunLimit(Distance.kilometres(100.0)), validate(100.1))

    @Test
    fun `run pushing the day over its limit is rejected`() =
        assertEquals(RunValidation.OverDayLimit(Distance.kilometres(150.0)), validate(60.0, sameDayKm = 95.0))

    @Test
    fun `run in a closed week is rejected`() =
        assertEquals(RunValidation.WeekClosed, validate(5.0, date = LocalDate.of(2026, 9, 20)))

    @Test
    fun `run in the future is rejected`() =
        assertEquals(RunValidation.InFuture, validate(5.0, date = today.plusDays(1)))
}
