package com.worldrunner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        compose.onNodeWithText("Log run", useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithText("Teams").performClick()
        compose.onNodeWithText("Your teams").assertIsDisplayed()
        compose.onNodeWithText("Trail Mix").performClick()
        compose.onNodeWithText("Sam (you)").assertIsDisplayed()

        compose.onNodeWithText("Standings").performClick()
        compose.onNodeWithText("Route progress").assertIsDisplayed()

        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Miles").assertIsDisplayed()
    }
}
