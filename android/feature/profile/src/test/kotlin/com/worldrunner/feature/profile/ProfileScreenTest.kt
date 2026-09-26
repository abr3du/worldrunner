package com.worldrunner.feature.profile

import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.model.DistanceUnit
import org.junit.Assert.assertEquals
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
@Config(sdk = [35])
class ProfileScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun choosingMilesChangesTheRunnersUnit() {
        val store = FakeGameStore(Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC), TestScope())
        val viewModel = ProfileViewModel(FakeRunnerRepository(store))
        compose.setContent { ProfileRoute(viewModel) }

        compose.onNodeWithText("Miles").performClick()
        compose.waitForIdle()
        assertEquals(DistanceUnit.Miles, store.runner.value.unit)
    }
}
