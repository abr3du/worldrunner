package com.worldrunner.feature.home

import com.worldrunner.core.data.RecordedRunAvailability
import com.worldrunner.core.data.RecordedRunSource
import com.worldrunner.core.model.RecordedRun
import java.time.Instant

/** Health Connect stand-in: [recordings] are what other apps wrote, readable once [granted]. */
class FakeRecordedRunSource(
    var availability: RecordedRunAvailability = RecordedRunAvailability.Available,
    var granted: Boolean = true,
    var recordings: List<RecordedRun> = emptyList(),
) : RecordedRunSource {
    var readSince: Instant? = null

    override fun availability() = availability

    override val requiredPermissions = setOf("android.permission.health.READ_EXERCISE", "android.permission.health.READ_DISTANCE")

    override suspend fun hasPermissions() = granted

    override suspend fun read(since: Instant): List<RecordedRun> {
        check(granted) { "Health Connect read without permission" }
        readSince = since
        return recordings.filter { !it.start.isBefore(since) }
    }
}
