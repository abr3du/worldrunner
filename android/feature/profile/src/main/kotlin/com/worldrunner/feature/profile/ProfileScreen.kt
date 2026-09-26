package com.worldrunner.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.worldrunner.core.data.RunnerRepository
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.Runner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileScreen() {
    composable<ProfileRoute> { ProfileRoute() }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(private val runnerRepository: RunnerRepository) : ViewModel() {
    val runner: StateFlow<Runner?> = runnerRepository.runner.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setUnit(unit: DistanceUnit) {
        viewModelScope.launch { runnerRepository.setUnit(unit) }
    }
}

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val runner by viewModel.runner.collectAsStateWithLifecycle()
    val current = runner ?: return
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(current.displayName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.units), style = MaterialTheme.typography.titleSmall)
        val units = DistanceUnit.entries
        SingleChoiceSegmentedButtonRow {
            units.forEachIndexed { i, unit ->
                SegmentedButton(
                    selected = current.unit == unit,
                    onClick = { viewModel.setUnit(unit) },
                    shape = SegmentedButtonDefaults.itemShape(i, units.size),
                    label = { Text(stringResource(if (unit == DistanceUnit.Kilometres) R.string.kilometres else R.string.miles)) },
                )
            }
        }
    }
}
