/* Reusable Material date picker fields for ISO-formatted app forms. */
package com.teja.finflyiii.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.teja.finflyiii.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Displays an ISO date and updates it through the Material calendar dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
            readOnly = true,
            label = { Text(stringResource(label)) },
            trailingIcon = {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.choose_date))
            },
            singleLine = true,
        )
        Box(
            Modifier.matchParentSize().clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { showPicker = true },
            )
        )
    }
    if (showPicker) {
        val initialMillis = runCatching {
            LocalDate.parse(value.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

/** Picks only the calendar portion while preserving the current time text. */
@Composable
fun DateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val date = value.substringBefore(' ')
    val time = value.substringAfter(' ', "00:00")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DatePickerField(
            value = date,
            onValueChange = { selectedDate -> onValueChange("$selectedDate $time") },
            label = label,
            modifier = Modifier.weight(2f),
        )
        OutlinedTextField(
            value = time,
            onValueChange = { selectedTime -> onValueChange("$date $selectedTime") },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.time)) },
            singleLine = true,
        )
    }
}
