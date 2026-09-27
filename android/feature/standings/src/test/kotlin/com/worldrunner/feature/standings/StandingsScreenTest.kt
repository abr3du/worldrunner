package com.worldrunner.feature.standings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.WorldRoute
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

    private fun awaitMap() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription(MAP, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun mapIsLabelledAsAVisualisationWithLegendAndAttribution() {
        compose.setContent { StandingsRoute(viewModel) }
        awaitMap()

        compose.onNodeWithText("Team distance map").assertIsDisplayed()
        compose.onNodeWithText("These are not real locations", substring = true).assertIsDisplayed()
        compose.onNodeWithText("starting in Lisbon and heading east", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("World map with the route and 10 team markers", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Route").assertIsDisplayed()
        compose.onNodeWithText("Start (Lisbon)").assertIsDisplayed()
        compose.onNodeWithText("Natural Earth", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Zoom in").assertIsDisplayed()
        compose.onNodeWithContentDescription("Show whole world").assertIsDisplayed()
    }

    @Test
    fun tappingAStandingsRowIdentifiesThatTeamWithRankAndDistance() {
        compose.setContent { StandingsRoute(viewModel) }
        awaitMap()

        compose.onNodeWithText("Night Owls").performClick()

        compose.onNodeWithText("#2 Night Owls").assertIsDisplayed()
        compose.onNodeWithText("1356.5 km · lap 1 · between Marseille and Milan").assertIsDisplayed()
    }

    @Test
    fun tappingOverlappingMarkersZoomsInUntilATeamIsSelected() {
        compose.setContent { StandingsRoute(viewModel) }
        awaitMap()
        val map = compose.onNodeWithContentDescription(MAP, substring = true)
        val leader = WorldRoute.positionAt(Distance.kilometres(1_402.0)).position

        // The whole League is one group at world zoom. Each tap zooms in around the leader's marker, which stays
        // under the finger, until the leader can be picked on its own (or maximum zoom is reached).
        repeat(6) {
            if (compose.onAllNodesWithText("#1 Harbour Striders").fetchSemanticsNodes().isNotEmpty()) return@repeat
            map.performTouchInput {
                val worldHeight = width * (84 + 58) / 360f
                val x = ((leader.longitude + 180) / 360 * width).toFloat()
                val y = (height - worldHeight) / 2 + ((84 - leader.latitude) / 142 * worldHeight).toFloat()
                click(Offset(x, y))
            }
            compose.mainClock.advanceTimeBy(1_000)
        }
        compose.onNodeWithText("#1 Harbour Striders").assertIsDisplayed()
    }

    private companion object {
        const val MAP = "World map with the route"
    }
}
