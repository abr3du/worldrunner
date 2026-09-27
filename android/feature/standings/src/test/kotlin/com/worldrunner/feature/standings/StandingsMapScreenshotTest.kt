package com.worldrunner.feature.standings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import com.worldrunner.core.data.fake.FakeGameStore
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeStandingsRepository
import com.worldrunner.core.data.fake.FakeTeamRepository
import com.worldrunner.core.designsystem.WorldrunnerTheme
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Renders the Standings map on a phone and a tablet and writes PNGs to `build/outputs/screenshots/` for review;
 * CI uploads them. It asserts only that rendering works: layout is checked by looking at the images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StandingsMapScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = FakeGameStore(Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC), TestScope())
    private val viewModel = StandingsViewModel(FakeRunnerRepository(store), FakeTeamRepository(store), FakeStandingsRepository(store))

    private fun capture(name: String, league: LeagueChoice, highlight: String) {
        viewModel.selectLeague(league)
        viewModel.highlightTeam(highlight)
        compose.setContent { WorldrunnerTheme(dynamicColor = false) { StandingsRoute(viewModel) } }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription("World map with the route", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        val dir = File("build/outputs/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "w393dp-h851dp-xxhdpi")
    fun phone() = capture("standings-map-phone", OWN, "night-owls")

    @Test
    @Config(sdk = [35], qualifiers = "w1024dp-h768dp-land-mdpi")
    fun tablet() = capture("standings-map-tablet", OWN, "night-owls")

    @Test
    @Config(sdk = [35], qualifiers = "w393dp-h851dp-xxhdpi")
    fun importedPhone() = capture("standings-map-kmspiel-phone", LeagueChoice.Imported, "kmspiel:LG Albatros Kiel")

    @Test
    @Config(sdk = [35], qualifiers = "w1024dp-h768dp-land-mdpi")
    fun importedTablet() = capture("standings-map-kmspiel-tablet", LeagueChoice.Imported, "kmspiel:LG Albatros Kiel")

    private companion object {
        val OWN = LeagueChoice.MyTeam("trail-mix")
    }
}
