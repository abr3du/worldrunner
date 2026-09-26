package com.worldrunner.feature.teams

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeTeamRepository
import androidx.compose.ui.test.junit4.createComposeRule
import com.worldrunner.core.data.fake.FakeGameStore
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "h2000dp")
class TeamsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = FakeGameStore(Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC), TestScope())

    @Test
    fun listsMyTeamsWithRank() {
        val viewModel = TeamsViewModel(FakeRunnerRepository(store), FakeTeamRepository(store))
        compose.setContent { TeamsRoute(onTeamClick = {}, viewModel = viewModel) }

        compose.onNodeWithText("Trail Mix").assertIsDisplayed()
        compose.onNodeWithText("Lunch Break Runners").assertIsDisplayed()
    }

    @Test
    fun detailShowsTeammatesWeeklyTotalsIncludingMe() {
        val handle = SavedStateHandle(route = TeamDetailRoute("trail-mix"))
        val viewModel = TeamDetailViewModel(handle, FakeRunnerRepository(store), FakeTeamRepository(store))
        compose.setContent { TeamDetailRoute(onBack = {}, viewModel = viewModel) }

        compose.onNodeWithText("Robin").assertIsDisplayed()
        compose.onNodeWithText("Sam (you)").assertIsDisplayed()
    }
}
