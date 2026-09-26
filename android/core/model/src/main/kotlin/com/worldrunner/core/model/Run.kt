package com.worldrunner.core.model

import java.time.LocalDate

data class Run(
    val id: String,
    val date: LocalDate,
    val distance: Distance,
    val status: SyncStatus,
) {
    val week: Week get() = Week.containing(date)
}

/** What the Runner sees for a write: never silently confirmed. */
enum class SyncStatus { Pending, Confirmed, NeedsAttention }

/** Maximum distance for a single Run and for one Runner's Runs on one day. */
data class RunLimits(
    val perRun: Distance = Distance.kilometres(100.0),
    val perDay: Distance = Distance.kilometres(150.0),
)

sealed interface RunValidation {
    data object Valid : RunValidation
    data object NotPositive : RunValidation
    data class OverRunLimit(val limit: Distance) : RunValidation
    data class OverDayLimit(val limit: Distance) : RunValidation
    data object WeekClosed : RunValidation
    data object InFuture : RunValidation
}

/**
 * Local pre-check before a Run is queued. The server repeats these checks and wins.
 *
 * @param sameDayDistance distance of the Runner's other Runs on [date].
 */
fun validateRun(
    distance: Distance,
    date: LocalDate,
    today: LocalDate,
    now: java.time.Instant,
    sameDayDistance: Distance,
    limits: RunLimits = RunLimits(),
): RunValidation = when {
    distance.metres <= 0 -> RunValidation.NotPositive
    date.isAfter(today) -> RunValidation.InFuture
    !Week.containing(date).isOpenAt(now) -> RunValidation.WeekClosed
    distance > limits.perRun -> RunValidation.OverRunLimit(limits.perRun)
    distance + sameDayDistance > limits.perDay -> RunValidation.OverDayLimit(limits.perDay)
    else -> RunValidation.Valid
}
