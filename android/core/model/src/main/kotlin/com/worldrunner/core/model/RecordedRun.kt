package com.worldrunner.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * An on-foot session another app recorded (Garmin Connect, Strava, Samsung Health, ...),
 * read from Health Connect. Importing it logs a Run that remembers this recording.
 *
 * @param id the recording's Health Connect id, stable across reads.
 * @param date the local date the recording started, which decides its Week.
 */
data class RecordedRun(
    val id: String,
    val start: Instant,
    val end: Instant,
    val date: LocalDate,
    val distance: Distance,
) {
    fun overlaps(other: RecordedRun) = start < other.end && other.start < end
}

/** How an import went: Runs logged, and new recordings that failed validation (see [validateRun]). */
data class ImportResult(val imported: Int, val rejected: Int)

/**
 * The [recorded] runs still to import. A recording already imported as one of [runs] is skipped, and
 * recordings that overlap in time are one run recorded by two apps (say Garmin Connect, and Strava
 * copying it), so only the earliest-starting one is kept. Runs logged by hand carry no time of day,
 * so they cannot be matched and are never treated as duplicates.
 */
fun newRecordings(recorded: List<RecordedRun>, runs: List<Run>): List<RecordedRun> {
    val taken = runs.mapNotNull { it.recording }.toMutableList()
    return recorded.sortedWith(compareBy({ it.start }, { it.id })).filter { candidate ->
        val isNew = taken.none { it.id == candidate.id || it.overlaps(candidate) }
        if (isNew) taken += candidate
        isNew
    }
}
