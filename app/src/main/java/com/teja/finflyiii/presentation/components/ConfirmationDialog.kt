/* Presentation-layer reusable confirmation for destructive and session actions. */
package com.teja.finflyiii.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teja.finflyiii.R

@Composable
fun ConfirmationDialog(
    @StringRes title: Int,
    message: String,
    @StringRes confirmLabel: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(confirmLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
