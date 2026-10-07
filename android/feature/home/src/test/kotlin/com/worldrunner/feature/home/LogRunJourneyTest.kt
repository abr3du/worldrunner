package com.worldrunner.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
    private val viewModel = HomeViewModel(FakeRunnerRepository(store), FakeRunRepository(store), FakeTeamRepository(store), FakeRecordedRunSource(), clock)

    @Test
    fun loggedRunShowsPendingThenConfirmed() {
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithContentDescription("Log run").performClick()
        compose.onNodeWithText("Distance (km)").performTextInput("7.5")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("+ 7.5 km pending (provisional)").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("7.5 km"))
        compose.onNodeWithText("Pending").assertIsDisplayed()

        sync.testScheduler.advanceUntilIdle()
        compose.waitForIdle()
        compose.onNodeWithText("Confirmed").assertIsDisplayed()
        compose.onNodeWithText("7.5 km").assertIsDisplayed()
        // The weekly total, at the top, now includes the confirmed Run.
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onAllNodesWithText("7.5 km").onFirst().assertIsDisplayed()
    }

    @Test
    fun runOverTheLimitShowsAnErrorAndStaysOpen() {
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithContentDescription("Log run").performClick()
        compose.onNodeWithText("Distance (km)").performTextInput("120")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("A single run can be at most 100.0 km").assertIsDisplayed()
    }

    @Test
    fun limitErrorUsesTheRunnersUnit() {
        store.setUnit(DistanceUnit.Miles)
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithContentDescription("Log run").performClick()
        compose.onNodeWithText("Distance (mi)").performTextInput("70")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("A single run can be at most 62.1 mi").assertIsDisplayed()
    }

    @Test
    fun logRunButtonIsAnnouncedWithItsLabel() {
        compose.setContent { HomeRoute(onTeamClick = {}, viewModel = viewModel) }

        // The merged tree is what TalkBack reads: the clickable button itself must carry the label.
        compose.onNode(hasClickAction() and hasContentDescription("Log run")).assertIsDisplayed()
    }
}
