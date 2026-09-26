package com.worldrunner.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CalendarTest {
    @Test
    fun `week straddling July 1 belongs to the Season holding its Thursday`() {
        // 2026-06-29 is a Monday; that Week's Thursday is 2026-07-02.
        val week = Week.containing(LocalDate.of(2026, 6, 30))
        assertEquals(Season(2026, SeasonHalf.Second), week.season)
        assertEquals(1, week.numberInSeason)
    }

    @Test
    fun `week straddling January 1 can belong to the previous year's Season`() {
        // 2026-12-28 is a Monday; that Week's Thursday is 2026-12-31.
        val week = Week.containing(LocalDate.of(2027, 1, 2))
        assertEquals(Season(2026, SeasonHalf.Second), week.season)
    }

    @Test
    fun `first Season week starts on the Monday before the first Thursday`() {
        assertEquals(LocalDate.of(2026, 6, 29), Season(2026, SeasonHalf.Second).firstWeek.monday)
        assertEquals(LocalDate.of(2025, 12, 29), Season(2026, SeasonHalf.First).firstWeek.monday)
    }

    @Test
    fun `week number counts from the Season's first week`() {
        assertEquals(13, Week.containing(LocalDate.of(2026, 9, 26)).numberInSeason)
    }

    @Test
    fun `week closes Tuesday 23 59 UTC after it ends`() {
        val week = Week.containing(LocalDate.of(2026, 9, 16)) // Mon 14 – Sun 20 Sep
        assertEquals(Instant.parse("2026-09-22T23:59:00Z"), week.closesAt)
        assertTrue(week.isOpenAt(Instant.parse("2026-09-22T23:59:00Z")))
        assertFalse(week.isOpenAt(Instant.parse("2026-09-23T00:00:00Z")))
    }
}
