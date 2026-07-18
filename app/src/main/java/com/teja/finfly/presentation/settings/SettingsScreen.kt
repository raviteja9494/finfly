/* Presentation-layer Compose screen for configuring and testing Firefly III access. */
package com.teja.finfly.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        SettingsUiState.Loading -> LoadingState()
        SettingsUiState.Empty -> SettingsFormContent(SettingsForm(), viewModel)
        SettingsUiState.Error -> ErrorState()
        is SettingsUiState.Success -> SettingsFormContent(value.form, viewModel)
    }
}

@Composable
private fun SettingsFormContent(form: SettingsForm, viewModel: SettingsViewModel) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(FinFlyThemeTokens.radii.hero),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    OutlinedTextField(
                        value = form.serverUrl,
                        onValueChange = viewModel::updateServerUrl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.server_url_label)) },
                        placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                        leadingIcon = { Icon(Icons.Rounded.Cloud, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    OutlinedTextField(
                        value = form.bearerToken,
                        onValueChange = viewModel::updateBearerToken,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.bearer_token_label)) },
                        placeholder = { Text(stringResource(R.string.bearer_token_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (form.showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleTokenVisibility) {
                                Icon(
                                    if (form.showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(
                                        if (form.showToken) R.string.hide_token else R.string.show_token
                                    ),
                                )
                            }
                        },
                    )
                    Text(
                        text = stringResource(
                            R.string.last_sync_label,
                            form.lastSyncTime?.atZone(ZoneId.systemDefault())?.format(
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                            ) ?: stringResource(R.string.last_sync_never),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Feedback(form.feedback)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
                    ) {
                        OutlinedButton(onClick = viewModel::test, enabled = !form.isTesting && !form.isSaving) {
                            if (form.isTesting) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.test_connection))
                        }
                        Button(onClick = viewModel::save, enabled = !form.isSaving && !form.isTesting) {
                            if (form.isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.save_settings))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Feedback(feedback: SettingsFeedback?) {
    if (feedback == null) return
    val success = feedback == SettingsFeedback.CONNECTION_SUCCESS || feedback == SettingsFeedback.SAVED
    val message = when (feedback) {
        SettingsFeedback.CONNECTION_SUCCESS -> R.string.connection_success
        SettingsFeedback.CONNECTION_FAILED -> R.string.connection_failed
        SettingsFeedback.SAVED -> R.string.settings_saved
        SettingsFeedback.INVALID_URL -> R.string.settings_invalid_url
        SettingsFeedback.TOKEN_REQUIRED -> R.string.settings_token_required
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
            contentDescription = null,
            tint = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
        Text(
            text = if (message == R.string.connection_failed) stringResource(message, stringResource(R.string.error_generic))
            else stringResource(message),
            color = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
    }
}
