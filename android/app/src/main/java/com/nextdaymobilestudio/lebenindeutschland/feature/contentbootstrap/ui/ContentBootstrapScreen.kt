package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapUiState
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme

@Composable
fun ContentBootstrapScreen(
    uiState: ContentBootstrapUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ContentBootstrapTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                when (uiState) {
                    ContentBootstrapUiState.Loading -> LoadingContent()
                    is ContentBootstrapUiState.Ready -> ReadyContent(uiState)
                    ContentBootstrapUiState.Error -> ErrorContent(onRetry)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    StatusContent(
        modifier = Modifier.testTag(ContentBootstrapTestTags.LOADING),
        icon = {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
            )
        },
        title = stringResource(R.string.content_bootstrap_loading_title),
        description = stringResource(R.string.content_bootstrap_loading_description),
    )
}

@Composable
private fun ReadyContent(state: ContentBootstrapUiState.Ready) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag(ContentBootstrapTestTags.READY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.content_bootstrap_ready_title),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                R.string.content_bootstrap_ready_description,
                state.questionCount,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        if (state.isRefreshing) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(ContentBootstrapTestTags.REFRESHING),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 3.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.content_bootstrap_refreshing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.refreshFailed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(ContentBootstrapTestTags.REFRESH_WARNING),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.content_bootstrap_refresh_failed),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    StatusContent(
        modifier = Modifier.testTag(ContentBootstrapTestTags.ERROR),
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = stringResource(R.string.content_bootstrap_error_title),
        description = stringResource(R.string.content_bootstrap_error_description),
        action = {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag(ContentBootstrapTestTags.RETRY),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.content_bootstrap_retry),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

@Composable
private fun StatusContent(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(4.dp))
            action()
        }
    }
}

@Preview(
    name = "Content ready",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ContentBootstrapPreview() {
    LebenInDeutschlandTheme {
        ContentBootstrapScreen(
            uiState = ContentBootstrapUiState.Ready(
                releaseId = "schema-3",
                questionCount = 460,
                isRefreshing = false,
            ),
            onRetry = {},
        )
    }
}
