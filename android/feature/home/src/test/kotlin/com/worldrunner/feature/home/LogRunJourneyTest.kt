package com.worldrunner.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.worldrunner.core.data.fake.FakeGameStore
import com.worldrunner.core.data.fake.FakeRunRepository
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeTeamRepository
import com.worldrunner.core.model.DistanceUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Critical journey 2: log a Run from Home, see it go from Pending to Confirmed. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LogRunJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val clock = Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC)
    private val sync = TestScope(StandardTestDispatcher())
    private val store = FakeGameStore(clock, sync)
    private val viewModel = HomeViewModel(FakeRunnerRepository(store), FakeRunRepository(store), FakeTeamRepository(store), clock)

    @Test
    fun loggedRunShowsPendingThenConfirmed() {
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithText("Log run", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Distance (km)").performTextInput("7.5")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("+ 7.5 km pending (provisional)").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("7.5 km"))
        compose.onNodeWithText("Pending").assertIsDisplayed()

        sync.testScheduler.advanceUntilIdle()
        compose.waitForIdle()
        compose.onNodeWithText("Confirmed").assertIsDisplayed()
        // The Run row and the now-confirmed weekly total.
        compose.onAllNodesWithText("7.5 km").assertCountEquals(2)
    }

    @Test
    fun runOverTheLimitShowsAnErrorAndStaysOpen() {
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithText("Log run", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Distance (km)").performTextInput("120")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("A single run can be at most 100.0 km").assertIsDisplayed()
    }

    @Test
    fun limitErrorUsesTheRunnersUnit() {
        store.setUnit(DistanceUnit.Miles)
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithText("Log run", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Distance (mi)").performTextInput("70")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("A single run can be at most 62.1 mi").assertIsDisplayed()
    }
}
