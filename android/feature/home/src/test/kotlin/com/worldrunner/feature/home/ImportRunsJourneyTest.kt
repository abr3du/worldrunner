package com.worldrunner.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.worldrunner.core.data.RecordedRunAvailability
import com.worldrunner.core.data.fake.FakeGameStore
import com.worldrunner.core.data.fake.FakeRunRepository
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeTeamRepository
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.RecordedRun
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Import the Runs that Garmin Connect, Strava and others shared with Health Connect, with one button on Home. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImportRunsJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val clock = Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC)
    private val store = FakeGameStore(clock, TestScope(StandardTestDispatcher()))
    private val source = FakeRecordedRunSource()
    private val viewModel = HomeViewModel(
        FakeRunnerRepository(store), FakeRunRepository(store), FakeTeamRepository(store), source, clock,
    )

    private fun recorded(id: String, date: String, start: String, end: String, km: Double) = RecordedRun(
        id, Instant.parse("${date}T$start:00Z"), Instant.parse("${date}T$end:00Z"), LocalDate.parse(date), Distance.kilometres(km),
    )

    private val garminMorning = recorded("garmin-1", "2026-09-22", "07:00", "07:45", 8.4)
    private val stravaCopy = recorded("strava-1", "2026-09-22", "07:01", "07:46", 8.4)
    private val walk = recorded("garmin-2", "2026-09-21", "12:00", "12:30", 2.6)

    private fun showHome() = compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

    @Test
    fun oneTapImportsEachRunOnce() {
        source.recordings = listOf(garminMorning, stravaCopy, walk)
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()

        compose.onNodeWithText("Imported 2 runs.").assertIsDisplayed()
        compose.onNodeWithText("+ 11.0 km pending (provisional)").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("8.4 km"))
        // Only the Weeks still open are read: on Wednesday 23 September that is this Week alone.
        assertEquals(Instant.parse("2026-09-21T00:00:00Z"), source.readSince)
    }

    @Test
    fun importingAgainFindsNothingNew() {
        source.recordings = listOf(garminMorning)
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()
        compose.onNodeWithText("Imported 1 run.").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Imported 1 run.")).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Import runs from Health Connect").performClick()

        compose.onNodeWithText("No new runs to import.").assertIsDisplayed()
    }

    @Test
    fun runsOverTheLimitAreReportedAsSkipped() {
        source.recordings = listOf(walk, recorded("ultra", "2026-09-22", "05:00", "17:00", 110.0))
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()

        compose.onNodeWithText("Imported 1 run. 1 run was skipped: it has no distance or is over the run limits.")
            .assertIsDisplayed()
    }

    @Test
    fun grantingPermissionContinuesTheImport() {
        source.granted = false
        source.recordings = listOf(walk)
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()
        compose.waitForIdle()
        // Health Connect's own permission screen is outside the app; answer it as the Runner would.
        source.granted = true
        viewModel.onPermissionsResult(source.requiredPermissions)

        compose.onNodeWithText("Imported 1 run.").assertIsDisplayed()
    }

    @Test
    fun refusingPermissionExplainsWhatIsNeeded() {
        source.granted = false
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()
        compose.waitForIdle()
        viewModel.onPermissionsResult(emptySet())

        compose.onNodeWithText("Allow Worldrunner to read exercise and distance in Health Connect to import runs.")
            .assertIsDisplayed()
        compose.onNodeWithText("Open").assertIsDisplayed()
    }

    @Test
    fun phoneWithoutHealthConnectSaysSo() {
        source.availability = RecordedRunAvailability.Unavailable
        showHome()

        compose.onNodeWithText("Import runs from Health Connect").performClick()

        compose.onNodeWithText("Health Connect isn't available on this phone.").assertIsDisplayed()
    }
}
