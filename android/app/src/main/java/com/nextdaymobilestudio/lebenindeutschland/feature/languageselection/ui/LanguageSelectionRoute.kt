package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.LanguageSelectionViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage

@Composable
fun LanguageSelectionRoute(
    persistedLanguage: SupportedLanguage? = null,
    onContinue: (SupportedLanguage) -> Unit,
    viewModel: LanguageSelectionViewModel? = null,
) {
    val configuration = LocalConfiguration.current
    val currentLocaleTags = configuration.locales.toLanguageTags()
    val initialLanguage = remember(currentLocaleTags, persistedLanguage) {
        persistedLanguage ?: SupportedLanguage.fromLanguageTags(currentLocaleTags)
    }
    val actualViewModel = viewModel ?: composeViewModel<LanguageSelectionViewModel>(
        factory = LanguageSelectionViewModel.factory(initialLanguage),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    LanguageSelectionScreen(
        uiState = uiState,
        onSearchQueryChanged = actualViewModel::onSearchQueryChanged,
        onLanguageSelected = actualViewModel::onLanguageSelected,
        onContinue = {
            uiState.selectedLanguage?.let(onContinue)
        },
    )
}
