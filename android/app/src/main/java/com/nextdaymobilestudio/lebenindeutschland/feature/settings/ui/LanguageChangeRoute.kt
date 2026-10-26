package com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.LanguageChangeCoordinator
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.LanguageChangeUiState
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.LanguageChangeViewModel

/**
 * Language picker used from Settings. The selected language is reported through
 * [onLanguageReady] only after its content package is available locally; until then the
 * current language stays active and a blocking dialog shows the download progress.
 */
@Composable
fun LanguageChangeRoute(
    currentLanguage: SupportedLanguage,
    coordinator: LanguageChangeCoordinator,
    onLanguageReady: (SupportedLanguage) -> Unit,
    viewModel: LanguageChangeViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<LanguageChangeViewModel>(
        factory = LanguageChangeViewModel.factory(
            currentLanguage = currentLanguage,
            coordinator = coordinator,
        ),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        (uiState as? LanguageChangeUiState.Ready)?.let { ready ->
            onLanguageReady(ready.language)
        }
    }

    Box {
        LanguageSelectionRoute(
            persistedLanguage = currentLanguage,
            onContinue = actualViewModel::onLanguageConfirmed,
        )
        LanguageChangeDialog(
            uiState = uiState,
            onRetry = actualViewModel::retry,
            onCancel = actualViewModel::cancel,
        )
    }
}
