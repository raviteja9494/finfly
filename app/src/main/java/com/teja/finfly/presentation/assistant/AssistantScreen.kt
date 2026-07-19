/* Compose UI for private model setup and streamed finance chat. */
package com.teja.finfly.presentation.assistant

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.AiModelInfo
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.domain.model.ChatMessage
import com.teja.finfly.domain.model.ChatRole
import com.teja.finfly.domain.model.AssistantSuggestion
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.ai_copied)
    if (showInfo) {
        ModelInfoSheet(info = viewModel.modelInfo(), onDismiss = { showInfo = false })
    }
    Box(Modifier.fillMaxSize()) {
        when (val modelState = state.modelState) {
            is AiModelState.NotDownloaded -> ModelSetup(
                availableMb = modelState.availableMb,
                error = state.error,
                onDownload = viewModel::downloadModel,
            )
            is AiModelState.Downloading -> ModelDownloadProgress(modelState, viewModel::cancelDownload)
            is AiModelState.Error -> if (state.messages.isEmpty()) {
                ModelSetup(availableMb = 0, error = state.error, onDownload = viewModel::retry)
            } else {
                ChatContent(
                    state,
                    viewModel,
                    onShowInfo = { showInfo = true },
                    onCopy = { response ->
                        clipboard.setText(AnnotatedString(response))
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    },
                    onShare = { response -> context.shareAiResponse(response) },
                )
            }
            AiModelState.Loading,
            AiModelState.Ready,
            is AiModelState.Downloaded -> ChatContent(
                state,
                viewModel,
                onShowInfo = { showInfo = true },
                onCopy = { response ->
                    clipboard.setText(AnnotatedString(response))
                    scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                },
                onShare = { response -> context.shareAiResponse(response) },
            )
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ModelSetup(availableMb: Long, error: AssistantError?, onDownload: () -> Unit) {
    val spacing = FinFlyThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.hero),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(spacing.xLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Text(stringResource(R.string.ai_setup_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.ai_setup_description), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.ai_model_size_warning), style = MaterialTheme.typography.bodyMedium)
                if (availableMb > 0) Text(stringResource(R.string.ai_storage_available, availableMb))
                if (error != null) {
                    Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onDownload) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(stringResource(R.string.ai_download_model), Modifier.padding(start = spacing.small))
                }
                Text(stringResource(R.string.ai_private_notice), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ModelDownloadProgress(state: AiModelState.Downloading, onCancel: () -> Unit) {
    val spacing = FinFlyThemeTokens.spacing
    Column(
        Modifier.fillMaxSize().padding(spacing.xLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.ai_downloading_model), style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(
            progress = { state.progressPercent / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.large),
        )
        Text(stringResource(R.string.ai_download_progress, state.downloadedMb, state.totalMb, state.progressPercent))
        OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = spacing.medium)) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ChatContent(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    onShowInfo: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val spacing = FinFlyThemeTokens.spacing
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.ai_private_badge), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShowInfo) {
                Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.ai_model_info))
            }
            IconButton(onClick = viewModel::clearConversation, enabled = state.messages.isNotEmpty()) {
                Icon(Icons.Rounded.ClearAll, contentDescription = stringResource(R.string.ai_clear_chat))
            }
        }
        if (state.contextWasTruncated) {
            Text(
                stringResource(R.string.ai_context_truncated),
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.modelState == AiModelState.Loading) {
            Text(
                stringResource(R.string.ai_error_warming_up),
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.context?.let { context ->
            Text(
                stringResource(
                    R.string.ai_context_summary,
                    context.transactionCount,
                    context.dateRangeDays,
                    context.estimatedTokens,
                ),
                modifier = Modifier.padding(horizontal = spacing.medium),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.messages.isEmpty()) {
            AssistantWelcome(
                suggestions = state.suggestions,
                onSuggestion = viewModel::sendMessage,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                items(state.messages.asReversed(), key = ChatMessage::id) { message ->
                    ChatBubble(message, onCopy, onShare)
                }
            }
        }
        state.error?.let { error ->
            Text(
                stringResource(error.messageResource()),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = spacing.medium),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.ai_message_hint)) },
                enabled = !state.isGenerating,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
            )
            IconButton(
                onClick = if (state.isGenerating) viewModel::stopGenerating else ({ viewModel.sendMessage() }),
                enabled = state.isGenerating || state.input.isNotBlank(),
            ) {
                Icon(
                    if (state.isGenerating) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(
                        if (state.isGenerating) R.string.ai_stop_response else R.string.ai_send_message
                    ),
                )
            }
        }
    }
}

@Composable
private fun AssistantWelcome(
    suggestions: List<AssistantSuggestion>,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestionTexts = suggestions.map { stringResource(it.textResource()) }
    val spacing = FinFlyThemeTokens.spacing
    Column(
        modifier.fillMaxWidth().padding(spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.ai_welcome_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.ai_welcome_description), style = MaterialTheme.typography.bodyLarge)
        LazyRow(
            contentPadding = PaddingValues(top = spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            items(suggestionTexts) { suggestion ->
                AssistChip(onClick = { onSuggestion(suggestion) }, label = { Text(suggestion) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(message: ChatMessage, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    if (message.role == ChatRole.SYSTEM) return
    val user = message.role == ChatRole.USER
    val spacing = FinFlyThemeTokens.spacing
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .then(
                    if (user || message.content.isBlank()) Modifier
                    else Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onCopy(message.content) },
                    )
                ),
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
            colors = CardDefaults.cardColors(
                containerColor = if (user) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
                if (message.content.isBlank()) CircularProgressIndicator()
                else Text(message.content, style = MaterialTheme.typography.bodyLarge)
                if (!user && message.totalTokens != null && message.processingTimeMs != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.ai_response_metrics, message.totalTokens, message.processingTimeMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onShare(message.content) }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.ai_share_response))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelInfoSheet(info: AiModelInfo?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val spacing = FinFlyThemeTokens.spacing
        Column(
            Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(stringResource(R.string.ai_model_info), style = MaterialTheme.typography.headlineSmall)
            if (info == null) {
                Text(stringResource(R.string.ai_model_not_ready))
            } else {
                Text(info.name, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.ai_model_provider, info.provider))
                Text(stringResource(R.string.ai_model_size, info.sizeGb))
                Text(stringResource(R.string.ai_model_quantization, info.quantization))
                Text(stringResource(R.string.ai_model_location, info.filePath))
            }
            Text(stringResource(R.string.ai_private_notice), color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun AssistantError.messageResource(): Int = when (this) {
    AssistantError.DOWNLOAD -> R.string.ai_error_download
    AssistantError.MODEL_ACCESS -> R.string.ai_error_model_access
    AssistantError.STORAGE -> R.string.ai_error_storage
    AssistantError.WARMING_UP -> R.string.ai_error_warming_up
    AssistantError.MODEL_LOAD -> R.string.ai_error_model_load
    AssistantError.TIMEOUT -> R.string.ai_error_timeout
    AssistantError.OUT_OF_MEMORY -> R.string.ai_error_memory
    AssistantError.NO_TRANSACTIONS -> R.string.ai_error_no_transactions
    AssistantError.CONTEXT_TOO_LARGE -> R.string.ai_error_context_too_large
    AssistantError.GENERATION -> R.string.ai_error_generation
}

private fun AssistantSuggestion.textResource(): Int = when (this) {
    AssistantSuggestion.FOOD_THIS_MONTH -> R.string.ai_suggestion_food_month
    AssistantSuggestion.SPEND_TODAY -> R.string.ai_suggestion_spend_today
    AssistantSuggestion.BIG_EXPENSES_TODAY -> R.string.ai_suggestion_big_expenses_today
    AssistantSuggestion.MONTH_SUMMARY -> R.string.ai_suggestion_month_summary
    AssistantSuggestion.BALANCE -> R.string.ai_suggestion_balance
}

private fun android.content.Context.shareAiResponse(response: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, response)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.ai_share_response)))
}
