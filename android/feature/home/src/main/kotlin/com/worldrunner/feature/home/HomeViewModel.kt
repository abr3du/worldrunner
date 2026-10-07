package com.worldrunner.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldrunner.core.data.RecordedRunAvailability
import com.worldrunner.core.data.RecordedRunSource
import com.worldrunner.core.data.RunRepository
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.data.TeamRepository
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.ImportResult
import com.worldrunner.core.model.Run
import com.worldrunner.core.model.RunValidation
import com.worldrunner.core.model.SyncStatus
import com.worldrunner.core.model.TeamDetail
import com.worldrunner.core.model.Week
import com.worldrunner.core.model.sum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val import: ImportState = ImportState(),
)

/**
 * The Health Connect import. [permissionRequest] asks the screen to request those permissions, and
 * [message] is the outcome to show once.
 */
data class ImportState(
    val busy: Boolean = false,
    val permissionRequest: Set<String>? = null,
    val message: ImportMessage? = null,
)

sealed interface ImportMessage {
    data class Done(val result: ImportResult) : ImportMessage
    data object Unavailable : ImportMessage
    data object NeedsUpdate : ImportMessage
    data object PermissionDenied : ImportMessage
    data object Failed : ImportMessage
}

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
    private val recordedRuns: RecordedRunSource,
    private val clock: Clock,
) : ViewModel() {
    private val week = Week.containing(LocalDate.now(clock))
    private val logRun = MutableStateFlow<LogRunState?>(null)
    private val import = MutableStateFlow(ImportState())
    private var unit = DistanceUnit.Kilometres

    val uiState: StateFlow<HomeUiState?> = combine(
        runnerRepository.runner,
        runRepository.observeRuns(week),
        teamRepository.observeMyTeams(),
        logRun,
        import,
    ) { runner, runs, teams, form, importState ->
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
            import = importState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The first day of the oldest Week not yet closed. */
    private fun earliestOpenDate(): LocalDate {
        val previous = week.previous()
        return if (previous.isOpenAt(clock.instant())) previous.monday else week.monday
    }

    fun openLogRun() {
        val today = LocalDate.now(clock)
        logRun.value = LogRunState(date = today, earliestDate = earliestOpenDate(), today = today)
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

    /** Imports the Runs other apps recorded in the Weeks still open, asking for permission first if needed. */
    fun importRuns() {
        if (import.value.busy) return
        when (recordedRuns.availability()) {
            RecordedRunAvailability.Unavailable -> showImport(ImportMessage.Unavailable)
            RecordedRunAvailability.NeedsUpdate -> showImport(ImportMessage.NeedsUpdate)
            RecordedRunAvailability.Available -> runImport { since ->
                if (recordedRuns.hasPermissions()) {
                    importSince(since)
                } else {
                    import.update { it.copy(permissionRequest = recordedRuns.requiredPermissions) }
                    null
                }
            }
        }
    }

    /** The screen launched the permission request in [ImportState.permissionRequest]. */
    fun onPermissionRequestLaunched() = import.update { it.copy(permissionRequest = null) }

    fun onPermissionsResult(granted: Set<String>) {
        if (granted.containsAll(recordedRuns.requiredPermissions)) {
            runImport(::importSince)
        } else {
            showImport(ImportMessage.PermissionDenied)
        }
    }

    fun onImportMessageShown() = import.update { it.copy(message = null) }

    private fun showImport(message: ImportMessage) = import.update { it.copy(message = message) }

    private suspend fun importSince(since: Instant): ImportMessage =
        ImportMessage.Done(runRepository.importRuns(recordedRuns.read(since)))

    /** Runs [block] while busy and shows the message it returns, if any. Health Connect errors become [ImportMessage.Failed]. */
    private fun runImport(block: suspend (since: Instant) -> ImportMessage?) {
        import.update { it.copy(busy = true) }
        viewModelScope.launch {
            val since = earliestOpenDate().atStartOfDay(clock.zone).toInstant()
            val message = try {
                block(since)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ImportMessage.Failed
            }
            import.update { it.copy(busy = false, message = message ?: it.message) }
        }
    }
}
