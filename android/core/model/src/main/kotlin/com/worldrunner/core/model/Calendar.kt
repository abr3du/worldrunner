package com.worldrunner.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** An ISO week (Monday–Sunday), identified by its Monday. */
@JvmInline
value class Week private constructor(val monday: LocalDate) : Comparable<Week> {
    val sunday: LocalDate get() = monday.plusDays(6)
    val thursday: LocalDate get() = monday.plusDays(3)

    /** The Season owning this Week: decided by where its Thursday falls. */
    val season: Season get() = Season.containing(thursday)

    /** 1-based position of this Week within its Season. */
    val numberInSeason: Int get() = ChronoUnit.WEEKS.between(season.firstWeek.monday, monday).toInt() + 1

    /** Tuesday 23:59 UTC after the Week ends. */
    val closesAt: Instant get() = monday.plusDays(8).atTime(LocalTime.of(23, 59)).toInstant(ZoneOffset.UTC)

    fun isOpenAt(now: Instant): Boolean = !now.isAfter(closesAt)

    fun contains(date: LocalDate) = !date.isBefore(monday) && !date.isAfter(sunday)

    fun previous() = Week(monday.minusWeeks(1))

    override fun compareTo(other: Week) = monday.compareTo(other.monday)

    override fun toString() =
        "${monday.get(IsoFields.WEEK_BASED_YEAR)}-W%02d".format(monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    companion object {
        fun containing(date: LocalDate) = Week(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
}

enum class SeasonHalf { First, Second }

/** A half-year span made of whole Weeks, shared by every Team and League. */
data class Season(val year: Int, val half: SeasonHalf) {
    /** The first Week whose Thursday falls in this Season's half. */
    val firstWeek: Week
        get() {
            val start = LocalDate.of(year, if (half == SeasonHalf.First) 1 else 7, 1)
            val firstThursday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            return Week.containing(firstThursday)
        }

    override fun toString() = "${if (half == SeasonHalf.First) "Spring" else "Autumn"} $year"

    companion object {
        fun containing(date: LocalDate) =
            Season(date.year, if (date.monthValue <= 6) SeasonHalf.First else SeasonHalf.Second)
    }
}
