package com.worldrunner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Launches the real app (Hilt graph, fake repositories) and walks the four top-level destinations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "h2000dp")
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationReachesEveryDestination() {
        compose.onNodeWithContentDescription("Log run").assertIsDisplayed()

        compose.onNodeWithText("Teams").performClick()
        compose.onNodeWithText("Your teams").assertIsDisplayed()
        compose.onNodeWithText("Trail Mix").performClick()
        compose.onNodeWithText("Sam (you)").assertIsDisplayed()

        compose.onNodeWithText("Standings").performClick()
        // Standings opens on the imported kmspiel snapshot, which loads off the main thread.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("kmspiel 4. Liga · season 2026-2").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("kmspiel 4. Liga · season 2026-2").assertIsDisplayed()

        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Miles").assertIsDisplayed()
    }
}
