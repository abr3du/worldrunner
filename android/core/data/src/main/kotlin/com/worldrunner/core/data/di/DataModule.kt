package com.worldrunner.core.data.di

import com.worldrunner.core.data.RecordedRunSource
import com.worldrunner.core.data.RunRepository
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.StandingsRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.data.fake.FakeGameStore
import com.worldrunner.core.data.fake.FakeRunRepository
import com.worldrunner.core.data.fake.FakeRunnerRepository
import com.worldrunner.core.data.fake.FakeStandingsRepository
import com.worldrunner.core.data.fake.FakeTeamRepository
import com.worldrunner.core.data.health.HealthConnectRunSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

/** Binds fake repositories until the Worldrunner API exists, and Health Connect for recorded runs. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun runnerRepository(impl: FakeRunnerRepository): RunnerRepository
    @Binds abstract fun runRepository(impl: FakeRunRepository): RunRepository
    @Binds abstract fun teamRepository(impl: FakeTeamRepository): TeamRepository
    @Binds abstract fun standingsRepository(impl: FakeStandingsRepository): StandingsRepository
    @Binds abstract fun recordedRunSource(impl: HealthConnectRunSource): RecordedRunSource

    companion object {
        @Provides @Singleton
        fun clock(): Clock = Clock.systemDefaultZone()

        @Provides @Singleton
        fun fakeGameStore(clock: Clock): FakeGameStore =
            FakeGameStore(clock, CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
