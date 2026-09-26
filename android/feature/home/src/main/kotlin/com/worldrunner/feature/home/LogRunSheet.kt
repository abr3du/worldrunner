package com.worldrunner.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.RunValidation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogRunSheet(
    state: LogRunState,
    unit: DistanceUnit,
    onDistanceChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pickingDate by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.log_run), style = MaterialTheme.typography.titleLarge)
            val error = errorText(state, unit)
            OutlinedTextField(
                value = state.distanceText,
                onValueChange = onDistanceChange,
                label = { Text(stringResource(R.string.distance_in, unit.symbol)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.run_date, state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))))
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
    if (pickingDate) {
        RunDatePicker(state, onPicked = { onDateChange(it); pickingDate = false }, onDismiss = { pickingDate = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunDatePicker(state: LogRunState, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = state.date.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = LocalDate.ofEpochDay(utcTimeMillis / MILLIS_PER_DAY)
                return !day.isBefore(state.earliestDate) && !day.isAfter(state.today)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onPicked(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) } }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    ) { DatePicker(picker) }
}

@Composable
private fun errorText(state: LogRunState, unit: DistanceUnit): String? = when {
    state.invalidNumber -> stringResource(R.string.error_not_a_number)
    else -> when (val e = state.error) {
        null, RunValidation.Valid -> null
        RunValidation.NotPositive -> stringResource(R.string.error_not_positive)
        RunValidation.InFuture -> stringResource(R.string.error_in_future)
        RunValidation.WeekClosed -> stringResource(R.string.error_week_closed)
        is RunValidation.OverRunLimit -> stringResource(R.string.error_over_run_limit, e.limit.format(unit))
        is RunValidation.OverDayLimit -> stringResource(R.string.error_over_day_limit, e.limit.format(unit))
    }
}
