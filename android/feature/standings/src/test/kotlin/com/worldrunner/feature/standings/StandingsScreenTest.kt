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
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
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

    /** The Standings screen opens on the imported kmspiel League; the fake-data journeys use the Runner's own. */
    private fun showOwnLeague() {
        viewModel.selectLeague(LeagueChoice.MyTeam("trail-mix"))
        compose.setContent { StandingsRoute(viewModel) }
    }

    private fun list() = compose.onAllNodes(hasScrollToIndexAction()).onFirst()

    private fun showImportedLeague() {
        compose.setContent { StandingsRoute(viewModel) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("kmspiel 4. Liga", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun opensOnTheImportedLeagueWithItsSourceAndTimestamp() {
        showImportedLeague()
        awaitMap()

        compose.onNodeWithText("kmspiel 4. Liga · season 2026-2").assertIsDisplayed()
        compose.onNodeWithText("Snapshot of the standings on 2026-09-27, retrieved 2026-09-27 19:43 UTC", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Source: https://www.kmspiel.de/2018/km_liga.php?liga=4").assertIsDisplayed()
        compose.onNodeWithContentDescription("World map with the route and 35 team markers", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Österreich").assertExists()
        list().performScrollToNode(hasText("Musiker"))
        compose.onNodeWithText("Musiker").assertIsDisplayed()
    }

    @Test
    fun tappingAnImportedTeamShowsItsRankAndDistance() {
        showImportedLeague()
        awaitMap()

        list().performScrollToNode(hasText("LG Albatros Kiel"))
        compose.onNodeWithText("LG Albatros Kiel").performClick()
        list().performScrollToIndex(0)

        compose.onNodeWithText("#20 LG Albatros Kiel").assertExists()
        compose.onNodeWithText("2314.0 km · lap 1 ·", substring = true).assertExists()
    }

    @Test
    fun showsLeagueWithOwnTeamAndZonesLabelledInText() {
        showOwnLeague()

        compose.onNodeWithText("Tier 3 · League B").assertIsDisplayed()
        compose.onNodeWithText("Route progress").assertIsDisplayed()
        compose.onNodeWithText("Your team").assertIsDisplayed()
        compose.onAllNodesWithText("Promotion zone").assertCountEquals(2)
        compose.onAllNodesWithText("Relegation zone").assertCountEquals(2)
    }

    @Test
    fun switchingTeamShowsThatTeamsLeague() {
        showOwnLeague()

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
        showOwnLeague()
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
        showOwnLeague()
        awaitMap()

        compose.onNodeWithText("Night Owls").performClick()

        compose.onNodeWithText("#2 Night Owls").assertIsDisplayed()
        compose.onNodeWithText("1356.5 km · lap 1 · between Marseille and Milan").assertIsDisplayed()
    }

    @Test
    fun tappingOverlappingMarkersZoomsInUntilATeamIsSelected() {
        showOwnLeague()
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
