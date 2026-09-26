package com.worldrunner.feature.standings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeStandingsRepository
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

/** Critical journey 4: League Standings and Route progress, own Team highlighted. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "h2000dp")
class StandingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = FakeGameStore(Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC), TestScope())
    private val viewModel = StandingsViewModel(FakeRunnerRepository(store), FakeTeamRepository(store), FakeStandingsRepository(store))

    @Test
    fun showsLeagueWithOwnTeamAndZonesLabelledInText() {
        compose.setContent { StandingsRoute(viewModel) }

        compose.onNodeWithText("Tier 3 · League B").assertIsDisplayed()
        compose.onNodeWithText("Route progress").assertIsDisplayed()
        compose.onNodeWithText("Your team").assertIsDisplayed()
        compose.onAllNodesWithText("Promotion zone").assertCountEquals(2)
        compose.onAllNodesWithText("Relegation zone").assertCountEquals(2)
    }

    @Test
    fun switchingTeamShowsThatTeamsLeague() {
        compose.setContent { StandingsRoute(viewModel) }

        compose.onNodeWithText("Lunch Break Runners").performClick()
        compose.onNodeWithText("Tier 4 · League F").assertIsDisplayed()
        // Lowest Tier: nobody is relegated.
        compose.onAllNodesWithText("Relegation zone").assertCountEquals(0)
    }
}
