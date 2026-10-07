package com.worldrunner.core.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.worldrunner.core.data.RecordedRunAvailability
import com.worldrunner.core.data.RecordedRunSource
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.RecordedRun
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Reads on-foot exercise sessions from Health Connect, where Garmin Connect, Strava, Samsung Health
 * and others write the runs they record. Only reads: Worldrunner never writes health data.
 */
class HealthConnectRunSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RecordedRunSource {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability() = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> RecordedRunAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> RecordedRunAvailability.NeedsUpdate
        else -> RecordedRunAvailability.Unavailable
    }

    override val requiredPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    override suspend fun hasPermissions() =
        client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)

    override suspend fun read(since: Instant): List<RecordedRun> =
        readSessions(since).filter { it.exerciseType in ON_FOOT }.map { session ->
            RecordedRun(
                id = session.metadata.id,
                start = session.startTime,
                end = session.endTime,
                date = session.startTime.atZone(session.startZoneOffset ?: ZoneId.systemDefault()).toLocalDate(),
                distance = distanceOf(session),
            )
        }

    private suspend fun readSessions(since: Instant): List<ExerciseSessionRecord> {
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val page = client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.after(since), pageToken = pageToken),
            )
            sessions += page.records
            pageToken = page.pageToken
        } while (pageToken != null)
        return sessions
    }

    /**
     * Sessions carry no distance of their own: it is the distance the same app recorded during the
     * session. Limiting to that app keeps a run that two apps both wrote from counting twice.
     */
    private suspend fun distanceOf(session: ExerciseSessionRecord): Distance {
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                dataOriginFilter = setOf(session.metadata.dataOrigin),
            ),
        )
        val metres = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
        return Distance(metres.roundToLong())
    }

    private companion object {
        /** Running or walking, including treadmill: what counts as a Run. Hiking is walking. */
        val ON_FOOT = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        )
    }
}
