package com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.LanguageChangeUiState

@Composable
fun LanguageChangeDialog(
    uiState: LanguageChangeUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    when (uiState) {
        is LanguageChangeUiState.Preparing -> AlertDialog(
            modifier = Modifier.testTag(SettingsTestTags.PREPARING_DIALOG),
            onDismissRequest = {},
            title = {
                Text(text = stringResource(R.string.language_change_preparing_title))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = stringResource(R.string.language_change_preparing_message))
                    CircularProgressIndicator()
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag(SettingsTestTags.CANCEL),
                ) {
                    Text(text = stringResource(R.string.language_change_cancel))
                }
            },
        )

        is LanguageChangeUiState.Failed -> AlertDialog(
            modifier = Modifier.testTag(SettingsTestTags.FAILED_DIALOG),
            onDismissRequest = onCancel,
            title = {
                Text(text = stringResource(R.string.language_change_failed_title))
            },
            text = {
                Text(text = stringResource(R.string.language_change_failed_message))
            },
            confirmButton = {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag(SettingsTestTags.RETRY),
                ) {
                    Text(text = stringResource(R.string.content_bootstrap_retry))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag(SettingsTestTags.CANCEL),
                ) {
                    Text(text = stringResource(R.string.language_change_cancel))
                }
            },
        )

        LanguageChangeUiState.Idle,
        is LanguageChangeUiState.Ready,
        -> Unit
    }
}
