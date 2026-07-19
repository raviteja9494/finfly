/* Presentation-layer reusable loading, empty, and error screen treatments. */
package com.teja.finflyiii.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.teja.finflyiii.R

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val shimmer = shimmerBrush()
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(5) { index ->
            Box(
                Modifier.fillMaxWidth()
                    .height(if (index == 0) 96.dp else 112.dp)
                    .background(shimmer, RoundedCornerShape(24.dp))
            )
        }
    }
}

/** Renders a compact skeleton block for incremental list loading. */
@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(shimmerBrush(), RoundedCornerShape(24.dp)))
}

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "loading_shimmer")
    val progress = transition.animateFloat(
        initialValue = -600f,
        targetValue = 1_200f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
        label = "loading_shimmer_progress",
    ).value
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
        ),
        start = Offset(progress - 300f, 0f),
        end = Offset(progress, 300f),
    )
}

@Composable
fun EmptyState(@StringRes title: Int, @StringRes message: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Rounded.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(24.dp),
            )
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun ErrorState(onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    stringResource(R.string.error_generic),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.error_generic_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (onRetry != null) Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}
