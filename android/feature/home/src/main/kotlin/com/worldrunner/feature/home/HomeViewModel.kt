package com.worldrunner.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldrunner.core.data.RunRepository
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.SyncStatus
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.Week
import com.worldrunner.core.model.sum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val week: Week,
    val weekClosesAt: Instant,
    val unit: DistanceUnit,
    val weeklyTotal: Distance,
    val pendingTotal: Distance,
    val runs: List<Run>,
    val teams: List<TeamDetail>,
    val logRun: LogRunState?,
)

/** The open Log run form. [earliestDate] is the first day of the oldest Week not yet closed. */
data class LogRunState(
    val distanceText: String = "",
    val date: LocalDate,
    val earliestDate: LocalDate,
    val today: LocalDate,
    val error: RunValidation? = null,
    val invalidNumber: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    runnerRepository: RunnerRepository,
    private val runRepository: RunRepository,
    teamRepository: TeamRepository,
    private val clock: Clock,
) : ViewModel() {
    private val week = Week.containing(LocalDate.now(clock))
    private val logRun = MutableStateFlow<LogRunState?>(null)
    private var unit = DistanceUnit.Kilometres

    val uiState: StateFlow<HomeUiState?> = combine(
        runnerRepository.runner,
        runRepository.observeRuns(week),
        teamRepository.observeMyTeams(),
        logRun,
    ) { runner, runs, teams, form ->
        unit = runner.unit
        HomeUiState(
            week = week,
            weekClosesAt = week.closesAt,
            unit = runner.unit,
            weeklyTotal = runs.filter { it.status == SyncStatus.Confirmed }.map { it.distance }.sum(),
            pendingTotal = runs.filter { it.status == SyncStatus.Pending }.map { it.distance }.sum(),
            runs = runs,
            teams = teams,
            logRun = form,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openLogRun() {
        val today = LocalDate.now(clock)
        val previous = week.previous()
        val earliest = if (previous.isOpenAt(clock.instant())) previous.monday else week.monday
        logRun.value = LogRunState(date = today, earliestDate = earliest, today = today)
    }

    fun dismissLogRun() {
        logRun.value = null
    }

    fun onDistanceChange(text: String) = logRun.update { it?.copy(distanceText = text, error = null, invalidNumber = false) }

    fun onDateChange(date: LocalDate) = logRun.update { it?.copy(date = date, error = null) }

    fun saveRun() {
        val form = logRun.value ?: return
        val value = form.distanceText.replace(',', '.').toDoubleOrNull()
        if (value == null) {
            logRun.update { it?.copy(invalidNumber = true) }
            return
        }
        viewModelScope.launch {
            when (val result = runRepository.logRun(form.date, Distance.of(value, unit))) {
                RunValidation.Valid -> logRun.value = null
                else -> logRun.update { it?.copy(error = result) }
            }
        }
    }
}
